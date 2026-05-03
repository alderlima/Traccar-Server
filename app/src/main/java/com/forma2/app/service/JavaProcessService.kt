package com.forma2.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.forma2.app.MainActivity
import com.forma2.app.R
import com.forma2.app.util.LogManager
import com.forma2.app.util.ServiceState
import kotlinx.coroutines.*
import java.io.File

class JavaProcessService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var process: Process? = null

    companion object {
        const val EXTRA_JAR_PATH = "jar_path"
        const val EXTRA_WORKING_DIR = "working_dir"
        const val EXTRA_JAVA_HOME = "java_home"
        const val CHANNEL_ID = "java_process_channel"
        const val NOTIFICATION_ID = 101

        fun start(context: Context, jarPath: String, workingDir: String, javaHome: String) {
            val intent = Intent(context, JavaProcessService::class.java).apply {
                putExtra(EXTRA_JAR_PATH, jarPath)
                putExtra(EXTRA_WORKING_DIR, workingDir)
                putExtra(EXTRA_JAVA_HOME, javaHome)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, JavaProcessService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("Executando processo Java...")
        startForeground(NOTIFICATION_ID, notification)

        intent?.let {
            val jarPath = it.getStringExtra(EXTRA_JAR_PATH) ?: return START_NOT_STICKY
            val workingDir = it.getStringExtra(EXTRA_WORKING_DIR) ?: return START_NOT_STICKY
            val javaHome = it.getStringExtra(EXTRA_JAVA_HOME) ?: return START_NOT_STICKY
            runJava(jarPath, workingDir, javaHome)
        }

        return START_STICKY
    }

    private fun runJava(jarPath: String, workingDir: String, javaHome: String) {
        serviceScope.launch {
            ServiceState.setRunning(true)
            LogManager.appendLog("Iniciando processo Java...")
            LogManager.appendLog("JAR: $jarPath")
            LogManager.appendLog("Diretório: $workingDir")

            val javaBinary = File(javaHome, "bin/java").absolutePath
            val command = listOf(javaBinary, "-jar", jarPath)

            try {
                val processBuilder = ProcessBuilder(command)
                    .directory(File(workingDir))
                    .redirectErrorStream(true)
                process = processBuilder.start()
                val reader = process!!.inputStream.bufferedReader()

                reader.useLines { lines ->
                    lines.forEach { line ->
                        LogManager.appendLog(line)
                    }
                }

                val exitCode = process?.waitFor() ?: -1
                LogManager.appendLog("Processo encerrado com código: $exitCode")
            } catch (e: Exception) {
                LogManager.appendLog("Erro: ${e.message}")
            } finally {
                process = null
                ServiceState.setRunning(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        process?.destroy()
        super.onDestroy()
        ServiceState.setRunning(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Processo Java",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Forma 2")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
