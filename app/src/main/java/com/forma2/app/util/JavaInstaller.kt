package com.forma2.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.File
import java.io.FileOutputStream

object JavaInstaller {

    // Espelhos dos pacotes oficiais do Termux (versão 17.0.31, aarch64)
    private const val BASE_URL = "https://mirrors.cqupt.edu.cn/termux/apt/termux-main/pool/main/o"
    private const val JDK_DEB = "openjdk-17_17.0-31_aarch64.deb"
    private const val JDK_X_DEB = "openjdk-17-x_17.0-31_aarch64.deb"

    suspend fun downloadAndExtractJre(
        context: Context,
        onProgress: (Float) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val jdkDir = File(context.filesDir, "jdk-17")
        val javaBin = File(jdkDir, "bin/java")
        if (javaBin.exists()) {
            // Garantir permissão de execução
            makeExecutable(javaBin)
            return@withContext jdkDir.absolutePath
        }

        val client = OkHttpClient.Builder().followRedirects(true).build()

        // Baixar e extrair o pacote principal
        val mainDeb = downloadFile(client, "$BASE_URL/openjdk-17/$JDK_DEB", context, 0.5f, onProgress)
        val xDeb = downloadFile(client, "$BASE_URL/openjdk-17-x/$JDK_X_DEB", context, 0.5f, onProgress)

        jdkDir.deleteRecursively()
        jdkDir.mkdirs()

        extractDeb(mainDeb, jdkDir)
        extractDeb(xDeb, jdkDir)

        mainDeb.delete()
        xDeb.delete()

        // Tornar executáveis os binários e bibliotecas
        makeExecutable(javaBin)
        jdkDir.walkTopDown()
            .filter { it.isFile && it.extension in listOf("so", "dylib") }
            .forEach { makeExecutable(it) }

        // Configurar um pequeno wrapper para LD_PRELOAD (opcional, mas recomendado)
        setupLdPreload(jdkDir)

        javaBin.parentFile?.parentFile?.absolutePath
    }

    private suspend fun downloadFile(
        client: OkHttpClient,
        url: String,
        context: Context,
        weight: Float,
        onProgress: (Float) -> Unit
    ): File {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download falhou: ${response.code}")

        val body = response.body ?: throw Exception("Resposta vazia")
        val file = File(context.cacheDir, url.substringAfterLast('/'))
        FileOutputStream(file).use { output ->
            body.byteStream().use { input ->
                val buffer = ByteArray(8192)
                var len: Int
                while (input.read(buffer).also { len = it } != -1) {
                    output.write(buffer, 0, len)
                }
            }
        }
        onProgress(weight)
        return file
    }

    private fun extractDeb(debFile: File, destDir: File) {
        // .deb é um archive ar, contendo control.tar.xz e data.tar.xz
        ArArchiveInputStream(debFile.inputStream()).use { ar ->
            var entry = ar.nextArEntry
            while (entry != null) {
                val name = entry.name
                if (name == "data.tar.xz") {
                    // Extrair data.tar.xz para destDir
                    XZCompressorInputStream(ar).use { xzIn ->
                        TarArchiveInputStream(xzIn).use { tar ->
                            var tarEntry = tar.nextTarEntry
                            while (tarEntry != null) {
                                val outFile = File(destDir, tarEntry.name)
                                if (tarEntry.isDirectory) outFile.mkdirs()
                                else {
                                    outFile.parentFile?.mkdirs()
                                    outFile.outputStream().use { tar.copyTo(it) }
                                }
                                tarEntry = tar.nextTarEntry
                            }
                        }
                    }
                }
                entry = ar.nextArEntry
            }
        }
    }

    private fun makeExecutable(file: File) {
        file.setReadable(true, false)
        file.setExecutable(true, false)
    }

    private fun setupLdPreload(jdkDir: File) {
        // O binário java do Termux precisa de libandroid-shmem.so
        // que já estará no diretório lib/ após extrair openjdk-17-x.
        // Criamos um script wrapper que define LD_PRELOAD.
        val wrapper = File(jdkDir, "bin/java-wrapper")
        wrapper.writeText("""#!/system/bin/sh
export LD_PRELOAD="${jdkDir.absolutePath}/lib/libandroid-shmem.so"
exec ${jdkDir.absolutePath}/bin/java "\$@"
""")
        makeExecutable(wrapper)
    }
}