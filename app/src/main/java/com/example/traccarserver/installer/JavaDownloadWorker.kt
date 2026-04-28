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
        .readTimeout(15, java.util.concurrent.TimeUnit.MINUTES)
        .build()

    companion object {
        const val KEY_PROGRESS = "PROGRESS"
        const val KEY_STATUS = "STATUS"
        // URL para o Java 17 (OpenJDK) para Android/Linux aarch64
        const val JAVA_URL = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%2B7/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.10_7.tar.gz"
    }

    override suspend fun doWork(): Result {
        val notificationId = 2001
        try {
            setForeground(createForegroundInfo("Preparando ambiente Termux...", notificationId))
        } catch (e: Exception) {
            Log.e("JavaDownloadWorker", "Erro ao definir foreground: ${e.message}")
        }

        val tempFile = File(applicationContext.cacheDir, "java17.tar.gz")
        
        try {
            updateStatus("Baixando Java 17...")
            downloadFile(JAVA_URL, tempFile)
            
            updateStatus("Limpando ambiente antigo...")
            if (envManager.prefixDir.exists()) envManager.prefixDir.deleteRecursively()
            envManager.prefixDir.mkdirs()
            envManager.binDir.mkdirs()
            envManager.libDir.mkdirs()
            envManager.tmpDir.mkdirs()
            
            updateStatus("Extraindo Java (Estilo Termux)...")
            extractViaShell(tempFile, envManager.prefixDir)
            
            if (envManager.javaExecutable.exists()) {
                updateStatus("Configurando permissões de execução...")
                applyPermissions(envManager.binDir)
                
                updateStatus("Java instalado com sucesso!")
                return Result.success()
            } else {
                return Result.failure(workDataOf(KEY_STATUS to "Erro: Binário java não encontrado após extração."))
            }
        } catch (e: Exception) {
            Log.e("JavaDownloadWorker", "Erro: ${e.message}", e)
            return Result.failure(workDataOf(KEY_STATUS to "Erro: ${e.message}"))
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private suspend fun downloadFile(url: String, targetFile: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Falha no download: $response")
            val body = response.body ?: throw IOException("Corpo vazio")
            val contentLength = body.contentLength()
            
            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(16384)
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

    private fun extractViaShell(file: File, destinationDir: File) {
        // Usa o comando tar nativo com --strip-components=1 para extrair o conteúdo da pasta raiz do JDK
        val command = "tar -xzf ${file.absolutePath} -C ${destinationDir.absolutePath} --strip-components=1"
        try {
            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().readText()
                Log.e("JavaDownloadWorker", "Erro no tar: $error")
                throw IOException("Falha na extração via shell: $error")
            }
        } catch (e: Exception) {
            throw IOException("Erro ao executar comando de extração: ${e.message}")
        }
    }

    private fun applyPermissions(dir: File) {
        try {
            // Aplica permissão de execução recursivamente na pasta bin (estilo Termux)
            Runtime.getRuntime().exec("chmod -R 755 ${dir.absolutePath}").waitFor()
        } catch (e: Exception) {
            Log.e("JavaDownloadWorker", "Erro ao aplicar permissões: ${e.message}")
        }
    }

    private suspend fun updateStatus(status: String) {
        setProgress(workDataOf(KEY_STATUS to status))
    }

    private fun createForegroundInfo(text: String, id: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, ServerService.CHANNEL_ID)
            .setContentTitle("Instalador Traccar")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        return ForegroundInfo(id, notification)
    }
}
