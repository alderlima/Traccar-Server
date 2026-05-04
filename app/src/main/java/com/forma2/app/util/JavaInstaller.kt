package com.forma2.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.*
import java.util.concurrent.TimeUnit

object JavaInstaller {

    // Lista de mirrors oficiais do Termux
    private val MIRRORS = listOf(
        "https://packages.termux.dev/apt/termux-main/pool/main/o",
        "https://mirror.mwt.me/termux/main/pool/main/o",
        "https://mirrors.cqupt.edu.cn/termux/apt/termux-main/pool/main/o",
        "https://mirrors.aliyun.com/termux/termux-main/pool/main/o"
    )

    // Nomes dos pacotes na versão atual (17.0.19)
    private const val JDK_DEB = "openjdk-17_17.0.19_aarch64.deb"
    private const val JDK_X_DEB = "openjdk-17-x_17.0.19_aarch64.deb"

    suspend fun downloadAndExtractJre(
        context: Context,
        onProgress: (Float) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val jdkDir = File(context.applicationInfo.nativeLibraryDir, "jdk-17")
        val javaBin = File(jdkDir, "bin/java")
        if (javaBin.exists()) {
            makeExecutable(javaBin)
            return@withContext jdkDir.absolutePath
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        val mainDeb = downloadFileWithFallback(client, JDK_DEB, context)
        onProgress(0.3f)
        val xDeb = downloadFileWithFallback(client, JDK_X_DEB, context)
        onProgress(0.6f)

        // Verifica se os arquivos não estão vazios
        if (mainDeb.length() == 0L || xDeb.length() == 0L) {
            throw IOException("Download do JDK falhou: arquivo vazio.")
        }

        val tempDir = File(context.cacheDir, "jdk-temp")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        try {
            extractDeb(mainDeb, tempDir)
            extractDeb(xDeb, tempDir)
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            throw IOException("Falha na extração dos pacotes .deb: ${e.message}", e)
        } finally {
            mainDeb.delete()
            xDeb.delete()
        }

        // Localiza a raiz do JDK dentro da estrutura extraída
        val extractedJavaHome = tempDir.walkTopDown().firstOrNull { file ->
            file.isFile && file.name == "java" && file.parentFile?.name == "bin"
        }?.parentFile?.parentFile

        if (extractedJavaHome == null) {
            tempDir.deleteRecursively()
            throw IOException("Estrutura do JDK não encontrada na extração.")
        }

        jdkDir.deleteRecursively()
        jdkDir.mkdirs()
        extractedJavaHome.copyRecursively(jdkDir, overwrite = true)
        tempDir.deleteRecursively()

        // Torna binários executáveis
        jdkDir.walkTopDown().filter { it.isFile && (it.name == "java" || it.name == "keytool") }.forEach {
            makeExecutable(it)
        }
        setupWrapper(jdkDir)

        onProgress(1f)
        jdkDir.absolutePath
    }

    private suspend fun downloadFileWithFallback(
        client: OkHttpClient,
        fileName: String,
        context: Context
    ): File {
        for (baseUrl in MIRRORS) {
            val url = "$baseUrl/openjdk-17/$fileName"
            // Para o pacote -x, ajusta a URL
            val finalUrl = if (fileName.contains("-x")) {
                "$baseUrl/openjdk-17-x/$fileName"
            } else {
                url
            }
            try {
                return downloadFile(client, finalUrl, context)
            } catch (e: IOException) {
                continue // tenta o próximo mirror
            }
        }
        throw IOException("Todos os mirrors falharam para $fileName")
    }

    private suspend fun downloadFile(
        client: OkHttpClient,
        url: String,
        context: Context
    ): File {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("HTTP ${response.code} para $url")
        val body = response.body ?: throw IOException("Corpo da resposta vazio para $url")
        val file = File(context.cacheDir, url.substringAfterLast('/'))
        FileOutputStream(file).use { fos ->
            body.byteStream().use { input ->
                input.copyTo(fos)
            }
        }
        val contentLength = body.contentLength()
        if (contentLength > 0 && file.length() != contentLength) {
            file.delete()
            throw IOException("Tamanho do arquivo baixado não confere com Content-Length")
        }
        return file
    }

    private fun extractDeb(debFile: File, destDir: File) {
        BufferedInputStream(debFile.inputStream()).use { bufferedIn ->
            ArArchiveInputStream(bufferedIn).use { ar ->
                var entry = ar.nextArEntry
                while (entry != null) {
                    if (entry.name == "data.tar.xz") {
                        XZCompressorInputStream(ar).use { xzIn ->
                            TarArchiveInputStream(xzIn).use { tar ->
                                var te = tar.nextTarEntry
                                while (te != null) {
                                    val outFile = File(destDir, te.name)
                                    if (te.isDirectory) outFile.mkdirs()
                                    else {
                                        outFile.parentFile?.mkdirs()
                                        outFile.outputStream().use { out ->
                                            tar.copyTo(out)
                                        }
                                    }
                                    te = tar.nextTarEntry
                                }
                            }
                        }
                    }
                    entry = ar.nextArEntry
                }
            }
        }
    }

    private fun makeExecutable(file: File) {
        file.setReadable(true, false)
        file.setExecutable(true, false)
    }

    private fun setupWrapper(jdkDir: File) {
        val wrapper = File(jdkDir, "bin/java-wrapper")
        wrapper.writeText("""#!/system/bin/sh
export LD_PRELOAD="${jdkDir.absolutePath}/lib/libandroid-shmem.so"
exec ${jdkDir.absolutePath}/bin/java "\$@"
""")
        makeExecutable(wrapper)
    }
}