package com.example.traccarserver.installer

import android.content.Context
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.*
import java.util.zip.ZipInputStream

class AssetExtractor(private val context: Context) {

    @Throws(IOException::class)
    fun extractTarGz(assetName: String, destinationDir: File) {
        context.assets.open(assetName).use { inputStream ->
            extractTarGz(inputStream, destinationDir)
        }
    }

    @Throws(IOException::class)
    fun extractTarGz(inputStream: InputStream, destinationDir: File) {
        if (!destinationDir.exists()) destinationDir.mkdirs()

        GzipCompressorInputStream(inputStream).use { gzipIn ->
            TarArchiveInputStream(gzipIn).use { tarIn ->
                var entry = tarIn.nextTarEntry
                while (entry != null) {
                    val outputFile = File(destinationDir, entry.name)
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { fos ->
                            tarIn.copyTo(fos)
                        }
                    }
                    entry = tarIn.nextTarEntry
                }
            }
        }
    }

    @Throws(IOException::class)
    fun extractZip(assetName: String, destinationDir: File) {
        if (!destinationDir.exists()) destinationDir.mkdirs()

        context.assets.open(assetName).use { inputStream ->
            ZipInputStream(inputStream).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val outputFile = File(destinationDir, entry.name)
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()
                        FileOutputStream(outputFile).use { fos ->
                            zipIn.copyTo(fos)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
        }
    }
}
