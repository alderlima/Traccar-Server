package com.example.traccarserver.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.traccarserver.MainActivity
import com.example.traccarserver.installer.EnvironmentManager
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class ServerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverProcess: Process? = null
    private lateinit var envManager: EnvironmentManager

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "traccar_server_channel"
        
        // Simulação simples de barramento de eventos para logs e status
        var isRunning = false
        val logLines = mutableListOf<String>()
        var onLogAdded: ((String) -> Unit)? = null
        var onStatusChanged: ((Boolean) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        envManager = EnvironmentManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startServer()
            ACTION_STOP -> stopServer()
        }
        return START_NOT_STICKY
    }

    private fun startServer() {
        if (isRunning) return

        val notification = createNotification("Iniciando servidor Traccar...")
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            try {
                val javaExec = envManager.javaExecutable.absolutePath
                val traccarDir = envManager.getTraccarDir() ?: throw IOException("Diretório do Traccar não selecionado")
                val jarFile = envManager.getJarFile() ?: throw IOException("Arquivo .jar do Traccar não encontrado")
                val traccarJar = jarFile.absolutePath
                val configPath = File(traccarDir, "conf/traccar.xml").absolutePath

                addLog("Iniciando: ${jarFile.name}")
                addLog("Diretório: ${traccarDir.absolutePath}")
                
                // Garante que a pasta de dados exista para o H2
                val dataDir = File(traccarDir, "data")
                if (!dataDir.exists()) dataDir.mkdirs()

                val processBuilder = ProcessBuilder(
                    javaExec,
                    "-Xms128m",
                    "-Xmx256m",
                    "-Djava.net.preferIPv4Stack=true",
                    "-jar",
                    traccarJar,
                    configPath
                )
                
                // Configura variáveis de ambiente essenciais (Estilo Termux)
                val env = processBuilder.environment()
                val javaBinDir = File(envManager.javaDir, "bin").absolutePath
                val javaLibDir = File(envManager.javaDir, "lib").absolutePath
                val javaServerLibDir = File(envManager.javaDir, "lib/server").absolutePath
                
                // Limpa variáveis que podem interferir e define as novas
                env["JAVA_HOME"] = envManager.javaDir.absolutePath
                env["PATH"] = "$javaBinDir:/system/bin:/system/xbin"
                env["LD_LIBRARY_PATH"] = "$javaLibDir:$javaServerLibDir"
                env["LANG"] = "en_US.UTF-8"
                env["LC_ALL"] = "en_US.UTF-8"
                env["HOME"] = filesDir.absolutePath
                env["TMPDIR"] = cacheDir.absolutePath
                
                processBuilder.directory(traccarDir)
                processBuilder.redirectErrorStream(true)

                serverProcess = processBuilder.start()
                isRunning = true
                withContext(Dispatchers.Main) { onStatusChanged?.invoke(true) }

                updateNotification("Servidor Traccar em execução")

                val reader = BufferedReader(InputStreamReader(serverProcess?.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { addLog(it) }
                }

                val exitCode = serverProcess?.waitFor()
                addLog("Servidor parado com código: $exitCode")
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Erro desconhecido"
                addLog("Erro ao iniciar servidor: $errorMsg")
                if (errorMsg.contains("Permission denied")) {
                    addLog("Dica: Tente reinstalar o Java ou verifique as permissões do app.")
                }
                Log.e("ServerService", "Erro no processo", e)
            } finally {
                isRunning = false
                withContext(Dispatchers.Main) { onStatusChanged?.invoke(false) }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun stopServer() {
        serviceScope.launch {
            serverProcess?.destroy()
            isRunning = false
            withContext(Dispatchers.Main) { onStatusChanged?.invoke(false) }
            addLog("Solicitação de parada enviada.")
            delay(1000)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun addLog(line: String) {
        Log.d("TraccarLog", line)
        logLines.add(line)
        if (logLines.size > 1000) logLines.removeAt(0)
        serviceScope.launch(Dispatchers.Main) {
            onLogAdded?.invoke(line)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Traccar Server Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Traccar Local Server")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serverProcess?.destroy()
        serviceScope.cancel()
        isRunning = false
        super.onDestroy()
    }
}
