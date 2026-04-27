package com.example.traccarserver.installer

import android.content.Context
import android.util.Log
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.*
import java.util.zip.ZipInputStream

class AssetExtractor(private val context: Context) {

    @Throws(IOException::class)
    fun extractTarGz(assetName: String, destinationDir: File) {
        // Verifica se o asset existe antes de tentar abrir
        val assets = context.assets.list("") ?: emptyArray()
        if (!assets.contains(assetName)) {
            throw IOException("O arquivo '$assetName' não foi encontrado na pasta 'assets' do projeto Android. Por favor, adicione o arquivo 'java17.tar.gz' em 'app/src/main/assets/'.")
        }

        if (!destinationDir.exists()) destinationDir.mkdirs()

        context.assets.open(assetName).use { inputStream ->
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
                            // Tenta manter permissões de execução para binários
                            if (entry.name.contains("bin/")) {
                                outputFile.setExecutable(true, false)
                            }
                        }
                        entry = tarIn.nextTarEntry
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    fun extractZip(assetName: String, destinationDir: File) {
        val assets = context.assets.list("") ?: emptyArray()
        if (!assets.contains(assetName)) {
            Log.w("AssetExtractor", "Aviso: Asset '$assetName' não encontrado. Pulando extração.")
            return
        }

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
