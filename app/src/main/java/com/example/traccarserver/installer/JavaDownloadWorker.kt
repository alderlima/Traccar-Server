package com.example.traccarserver.installer

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import androidx.core.app.NotificationCompat
import com.example.traccarserver.server.ServerService
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class JavaDownloadWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    private val envManager = EnvironmentManager(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.MINUTES)
        .build()

    companion object {
        const val KEY_PROGRESS = "PROGRESS"
        const val KEY_STATUS = "STATUS"
        // URL para o Java 17 (OpenJDK) para Android/Linux aarch64 (exemplo do Termux/Adoptium)
        // Nota: O usuário deve fornecer uma URL válida para o tar.gz compatível com Android
        const val JAVA_URL = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%2B7/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.10_7.tar.gz"
    }

    override suspend fun doWork(): Result {
        val notificationId = 2001
        try {
            setForeground(createForegroundInfo("Baixando Java 17...", notificationId))
        } catch (e: Exception) {
            Log.e("JavaDownloadWorker", "Erro ao definir foreground: ${e.message}")
        }

        val tempFile = File(applicationContext.cacheDir, "java17.tar.gz")
        
        try {
            updateStatus("Iniciando download...")
            Log.d("JavaDownloadWorker", "Baixando de: $JAVA_URL")
            downloadFile(JAVA_URL, tempFile)
            
            updateStatus("Extraindo Java...")
            Log.d("JavaDownloadWorker", "Extraindo para: ${envManager.javaDir.absolutePath}")
            if (envManager.javaDir.exists()) envManager.javaDir.deleteRecursively()
            envManager.javaDir.mkdirs()
            
            extractFromFile(tempFile, envManager.javaDir)
            
            if (envManager.javaExecutable.exists()) {
                // Tenta definir permissão de execução via Java API
                envManager.javaExecutable.setExecutable(true, false)
                File(envManager.javaDir, "bin/keytool").setExecutable(true, false)
                
                // Tenta forçar via Shell (mais robusto em alguns dispositivos Android)
                try {
                    Runtime.getRuntime().exec("chmod 755 ${envManager.javaExecutable.absolutePath}").waitFor()
                    val binDir = File(envManager.javaDir, "bin")
                    Runtime.getRuntime().exec("chmod -R 755 ${binDir.absolutePath}").waitFor()
                } catch (e: Exception) {
                    Log.e("JavaDownloadWorker", "Erro ao dar chmod via shell: ${e.message}")
                }
                
                updateStatus("Java instalado com sucesso!")
                Log.d("JavaDownloadWorker", "Instalação concluída com sucesso.")
                return Result.success()
            } else {
                Log.e("JavaDownloadWorker", "Binário não encontrado em: ${envManager.javaExecutable.absolutePath}")
                return Result.failure(workDataOf(KEY_STATUS to "Erro: Binário java não encontrado em ${envManager.javaExecutable.absolutePath}"))
            }
        } catch (e: Exception) {
            Log.e("JavaDownloadWorker", "Erro durante o processo: ${e.message}", e)
            return Result.failure(workDataOf(KEY_STATUS to "Erro: ${e.message}"))
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private suspend fun downloadFile(url: String, targetFile: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Falha no download: $response")
            
            val body = response.body ?: throw IOException("Corpo da resposta vazio")
            val contentLength = body.contentLength()
            
            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            val progress = (totalBytesRead * 100 / contentLength).toInt()
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                        }
                    }
                }
            }
        }
    }

    private fun extractFromFile(file: File, destinationDir: File) {
        java.io.FileInputStream(file).use { fis ->
            val gzipIn = org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream(fis)
            val tarIn = org.apache.commons.compress.archivers.tar.TarArchiveInputStream(gzipIn)
            var entry = tarIn.nextTarEntry
            while (entry != null) {
                // Remove o primeiro componente do caminho (ex: jdk-17.0.10+7/)
                val name = entry.name
                val parts = name.split("/")
                if (parts.size > 1) {
                    val strippedName = parts.drop(1).joinToString("/")
                    if (strippedName.isNotEmpty()) {
                        val outputFile = File(destinationDir, strippedName)
                        if (entry.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs()
                            FileOutputStream(outputFile).use { fos ->
                                tarIn.copyTo(fos)
                            }
                        }
                    }
                }
                entry = tarIn.nextTarEntry
            }
        }
    }

    private suspend fun updateStatus(status: String) {
        setProgress(workDataOf(KEY_STATUS to status))
    }

    private fun createForegroundInfo(progressText: String, notificationId: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, ServerService.CHANNEL_ID)
            .setContentTitle("Instalador Java")
            .setTicker("Baixando Java")
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        return ForegroundInfo(notificationId, notification)
    }
}
