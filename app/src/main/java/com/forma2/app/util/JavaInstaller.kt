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

    suspend fun downloadAndExtractJre(
        context: Context,
        onProgress: (Float) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val jreDir = File(context.filesDir, "jre")
        // Se já existir libjvm.so, retorna o caminho
        if (jreDir.exists() && jreDir.walkTopDown().any { it.isFile && it.name == "libjvm.so" }) {
            return@withContext jreDir.absolutePath
        }

        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .build()
        // Endpoint da JRE do Adoptium para aarch64 Linux
        val apiUrl = "https://api.adoptium.net/v3/binary/latest/17/ga/linux/aarch64/jre/hotspot/normal/eclipse?project=jdk"
        val request = Request.Builder().url(apiUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download falhou: ${response.code}")

        val body = response.body ?: throw Exception("Resposta vazia")
        val contentLength = body.contentLength()
        var downloadedBytes = 0L

        val tempFile = File(context.cacheDir, "jre.tar.gz")
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

        // Limpa e extrai
        jreDir.deleteRecursively()
        jreDir.mkdirs()
        tempFile.inputStream().use { fileStream ->
            GZIPInputStream(fileStream).use { gzStream ->
                TarArchiveInputStream(gzStream).use { tarInput ->
                    var entry = tarInput.nextTarEntry
                    while (entry != null) {
                        val entryFile = File(jreDir, entry.name)
                        if (entry.isDirectory) {
                            entryFile.mkdirs()
                        } else {
                            entryFile.parentFile?.mkdirs()
                            entryFile.outputStream().use { out ->
                                tarInput.copyTo(out)
                            }
                        }
                        entry = tarInput.nextTarEntry
                    }
                }
            }
        }
        tempFile.delete()

        // Procura o diretório raiz da JRE (onde está lib/libjvm.so)
        val jreRoot = jreDir.walkTopDown().firstOrNull { it.isDirectory && File(it, "lib/libjvm.so").exists() }
        jreRoot?.absolutePath
    }
}