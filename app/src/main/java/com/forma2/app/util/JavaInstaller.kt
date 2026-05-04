package com.forma2.app.util

import android.content.Context
import android.util.Log
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

    private const val TAG = "JavaInstaller"

    // Mirrors testados e funcionais
    private val MIRRORS = listOf(
        "https://mirror.mwt.me/termux/main/pool/main/o",   // mais estável
        "https://packages.termux.dev/apt/termux-main/pool/main/o",
        "https://mirrors.cqupt.edu.cn/termux/apt/termux-main/pool/main/o",
        "https://mirrors.aliyun.com/termux/termux-main/pool/main/o"
    )

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
            LogManager.appendLog("JDK já instalado em $jdkDir")
            return@withContext jdkDir.absolutePath
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        // Baixa com tentativas e verificação
        val mainDeb = downloadWithRetry(client, JDK_DEB, false, context)
        onProgress(0.3f)
        val xDeb = downloadWithRetry(client, JDK_X_DEB, true, context)
        onProgress(0.6f)

        // Validação básica de tamanho (pelo menos 1MB cada)
        if (mainDeb.length() < 1_000_000 || xDeb.length() < 100_000) {
            throw IOException("Arquivo baixado parece inválido (tamanho insuficiente).")
        }

        LogManager.appendLog("Pacotes baixados (${mainDeb.length()} bytes, ${xDeb.length()} bytes). Extraindo...")

        val tempDir = File(context.cacheDir, "jdk-temp")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        try {
            extractDeb(mainDeb, tempDir)
            LogManager.appendLog("Pacote principal extraído.")
            extractDeb(xDeb, tempDir)
            LogManager.appendLog("Pacote complementar extraído.")
        } catch (e: Exception) {
            LogManager.appendLog("Erro na extração: ${e.message}")
            tempDir.deleteRecursively()
            throw IOException("Falha na extração: ${e.message}", e)
        } finally {
            // Mantemos os .deb para inspeção manual, mas podem ser apagados depois
            // mainDeb.delete(); xDeb.delete()
        }

        val extractedJavaHome = tempDir.walkTopDown().firstOrNull { file ->
            file.isFile && file.name == "java" && file.parentFile?.name == "bin"
        }?.parentFile?.parentFile

        if (extractedJavaHome == null) {
            tempDir.deleteRecursively()
            throw IOException("Estrutura do JDK não encontrada.")
        }

        jdkDir.deleteRecursively()
        jdkDir.mkdirs()
        extractedJavaHome.copyRecursively(jdkDir, overwrite = true)
        tempDir.deleteRecursively()

        jdkDir.walkTopDown().filter { it.isFile && (it.name == "java" || it.name == "keytool") }.forEach {
            makeExecutable(it)
        }
        setupWrapper(jdkDir)

        onProgress(1f)
        LogManager.appendLog("JDK 17 instalado com sucesso.")
        jdkDir.absolutePath
    }

    private suspend fun downloadWithRetry(
        client: OkHttpClient,
        fileName: String,
        isXPackage: Boolean,
        context: Context
    ): File {
        var lastException: Exception? = null
        for (mirror in MIRRORS) {
            val basePath = if (isXPackage) "$mirror/openjdk-17-x" else "$mirror/openjdk-17"
            val url = "$basePath/$fileName"
            repeat(3) { attempt ->
                try {
                    LogManager.appendLog("Baixando $url (tentativa ${attempt + 1})")
                    val file = downloadFile(client, url, context)
                    if (file.length() > 0) return file
                } catch (e: Exception) {
                    lastException = e
                    LogManager.appendLog("Falha no download: ${e.message}")
                }
            }
        }
        throw IOException("Todos os mirrors falharam para $fileName. Último erro: ${lastException?.message}")
    }

    private suspend fun downloadFile(client: OkHttpClient, url: String, context: Context): File {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
        val body = response.body ?: throw IOException("Corpo vazio")
        val file = File(context.cacheDir, url.substringAfterLast('/'))
        FileOutputStream(file).use { fos ->
            body.byteStream().use { input ->
                input.copyTo(fos)
            }
        }
        // Verifica tamanho contra Content-Length
        val contentLength = body.contentLength()
        if (contentLength > 0 && file.length() != contentLength) {
            file.delete()
            throw IOException("Download incompleto: esperado $contentLength bytes, recebido ${file.length()}")
        }
        return file
    }

    private fun extractDeb(debFile: File, destDir: File) {
        // Usa BufferedInputStream com buffer maior
        BufferedInputStream(FileInputStream(debFile), 65536).use { bis ->
            ArArchiveInputStream(bis).use { ar ->
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