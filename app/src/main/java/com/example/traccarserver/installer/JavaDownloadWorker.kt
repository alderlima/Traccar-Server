package com.example.traccarserver.installer

import android.app.*
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
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
        const val JAVA_URL = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%2B7/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.10_7.tar.gz"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "traccar_installer_channel"
    }

    override suspend fun doWork(): Result {
        // Cria o canal de notificação (necessário para API 26+)
        createNotificationChannel()

        // Define foreground ANTES de qualquer operação suspensa
        val foregroundInfo = createForegroundInfo("Preparando ambiente...")
        setForeground(foregroundInfo)

        return try {
            executeInstallation()
        } catch (e: Exception) {
            Log.e("JavaDownloadWorker", "Erro fatal: ${e.message}", e)
            Result.failure(workDataOf(KEY_STATUS to "Erro: ${e.message}"))
        }
    }

    private suspend fun executeInstallation(): Result {
        val tempFile = File(applicationContext.cacheDir, "java17.tar.gz")

        try {
            updateStatus("Baixando Java 17 (AArch64)...")
            downloadFile(JAVA_URL, tempFile)

            updateStatus("Limpando ambiente anterior...")
            if (envManager.prefixDir.exists()) envManager.prefixDir.deleteRecursively()
            envManager.prefixDir.mkdirs()
            envManager.binDir.mkdirs()
            envManager.libDir.mkdirs()
            envManager.tmpDir.mkdirs()

            updateStatus("Extraindo arquivos...")
            extractViaShell(tempFile, envManager.prefixDir)

            // Localiza o binário java recursivamente
            val javaBin = findJavaBinary(envManager.prefixDir)
            if (javaBin != null && javaBin.exists()) {
                updateStatus("Configurando permissões...")
                applyPermissions(javaBin.parentFile ?: envManager.binDir)

                // Atualiza o EnvironmentManager com o caminho real
                saveJavaPath(javaBin)

                updateStatus("Java 17 instalado com sucesso!")
                return Result.success()
            } else {
                return Result.failure(workDataOf(KEY_STATUS to "Binário java não encontrado após extração"))
            }
        } catch (e: Exception) {
            Log.e("JavaDownloadWorker", "Erro durante instalação: ${e.message}", e)
            return Result.failure(workDataOf(KEY_STATUS to "Erro: ${e.message}"))
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private suspend fun downloadFile(url: String, targetFile: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Falha no download: ${response.code}")
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
        // Tenta com --strip-components=1
        val command = "tar -xzf ${file.absolutePath} -C ${destinationDir.absolutePath} --strip-components=1"
        try {
            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()
            if (exitCode == 0) return
        } catch (e: Exception) {
            Log.w("JavaDownloadWorker", "Falha no comando com strip, tentando sem...")
        }

        // Fallback: extrair completo
        val fallbackCommand = "tar -xzf ${file.absolutePath} -C ${destinationDir.absolutePath}"
        val fallbackProcess = Runtime.getRuntime().exec(fallbackCommand)
        if (fallbackProcess.waitFor() != 0) {
            val error = fallbackProcess.errorStream.bufferedReader().readText()
            throw IOException("Falha na extração: $error")
        }
    }

    private fun findJavaBinary(dir: File): File? {
        if (!dir.exists() || !dir.isDirectory) return null
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                findJavaBinary(file)?.let { return it }
            } else if (file.name == "java" && file.parentFile?.name == "bin") {
                return file
            }
        }
        return null
    }

    private fun applyPermissions(binDir: File) {
        try {
            // Torna todos os arquivos dentro de binDir executáveis
            binDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.setExecutable(true, false)
                }
            }
            Runtime.getRuntime().exec("chmod -R 755 ${binDir.absolutePath}").waitFor()
        } catch (e: Exception) {
            Log.e("JavaDownloadWorker", "Erro ao aplicar permissões: ${e.message}")
        }
    }

    private fun saveJavaPath(javaBin: File) {
        val prefs = applicationContext.getSharedPreferences("traccar_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("java_executable_path", javaBin.absolutePath).apply()
    }

    private suspend fun updateStatus(status: String) {
        setProgress(workDataOf(KEY_STATUS to status))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Instalador Traccar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificações de download e instalação do Java"
            }
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundInfo(text: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Instalador Traccar")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}