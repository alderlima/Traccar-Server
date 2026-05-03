package com.forma2.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

object JavaInstaller {

    suspend fun downloadAndExtract(
        context: Context,
        onProgress: (Float) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val jdkDir = File(context.filesDir, "jdk-17")
        if (jdkDir.exists() && File(jdkDir, "bin/java").exists()) {
            return@withContext jdkDir.absolutePath
        }

        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .build()
        val apiUrl = "https://api.adoptium.net/v3/binary/latest/17/ga/linux/aarch64/jdk/hotspot/normal/eclipse?project=jdk"
        val request = Request.Builder().url(apiUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download falhou: ${response.code}")

        val body = response.body ?: throw Exception("Resposta vazia")
        val contentLength = body.contentLength()
        var downloadedBytes = 0L

        val tempFile = File(context.cacheDir, "jdk.tar.gz")
        body.byteStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8192)
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    output.write(buffer, 0, bytes)
                    downloadedBytes += bytes
                    if (contentLength > 0) {
                        onProgress(downloadedBytes.toFloat() / contentLength.toFloat())
                    }
                }
            }
        }

        // Extração
        jdkDir.mkdirs()
        tempFile.inputStream().use { fileStream ->
            GZIPInputStream(fileStream).use { gzStream ->
                TarArchiveInputStream(gzStream).use { tarInput ->
                    var entry = tarInput.nextTarEntry
                    while (entry != null) {
                        val entryFile = File(jdkDir, entry.name)
                        if (entry.isDirectory) {
                            entryFile.mkdirs()
                        } else {
                            entryFile.parentFile?.mkdirs()
                            entryFile.outputStream().use { out ->
                                tarInput.copyTo(out)
                            }
                            if (entry.name.contains("bin/java")) {
                                entryFile.setExecutable(true)
                            }
                        }
                        entry = tarInput.nextTarEntry
                    }
                }
            }
        }

        tempFile.delete()

        // O tar pode vir com diretório raiz, localizar java
        val javaBinary = jdkDir.walkTopDown().find { it.isFile && it.name == "java" }
        if (javaBinary != null) {
            javaBinary.setExecutable(true)
            javaBinary.parentFile?.parentFile?.absolutePath
        } else null
    }
}
