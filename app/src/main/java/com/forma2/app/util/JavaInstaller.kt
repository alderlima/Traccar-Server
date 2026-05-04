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

    private const val BASE_URL = "https://packages.termux.dev/apt/termux-main/pool/main/o"
    private const val JDK_DEB = "openjdk-17_17.0-31_aarch64.deb"
    private const val JDK_X_DEB = "openjdk-17-x_17.0-31_aarch64.deb"

    suspend fun downloadAndExtractJre(
        context: Context,
        onProgress: (Float) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val jdkDir = File(context.filesDir, "jdk-17")
        val javaBin = File(jdkDir, "bin/java")
        if (javaBin.exists()) {
            makeExecutable(javaBin)
            return@withContext jdkDir.absolutePath
        }

        val client = OkHttpClient.Builder().followRedirects(true).build()

        val mainDeb = downloadFile(client, "$BASE_URL/openjdk-17/$JDK_DEB", context)
        val xDeb = downloadFile(client, "$BASE_URL/openjdk-17-x/$JDK_X_DEB", context)
        onProgress(0.5f) // primeira metade concluída

        jdkDir.deleteRecursively()
        jdkDir.mkdirs()

        extractDeb(mainDeb, jdkDir)
        extractDeb(xDeb, jdkDir)

        mainDeb.delete()
        xDeb.delete()

        // Tornar binários executáveis
        jdkDir.walkTopDown().filter { it.isFile && (it.name == "java" || it.name == "keytool") }.forEach {
            makeExecutable(it)
        }
        // Criar wrapper que carrega libandroid-shmem.so
        setupWrapper(jdkDir)

        onProgress(1f)
        javaBin.parentFile?.parentFile?.absolutePath
    }

    private suspend fun downloadFile(client: OkHttpClient, url: String, context: Context): File {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download falhou (${response.code}): $url")
        val file = File(context.cacheDir, url.substringAfterLast('/'))
        FileOutputStream(file).use { fos ->
            response.body!!.byteStream().use { it.copyTo(fos) }
        }
        return file
    }

    private fun extractDeb(debFile: File, destDir: File) {
        ArArchiveInputStream(debFile.inputStream()).use { ar ->
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
                                    outFile.outputStream().use { tar.copyTo(it) }
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