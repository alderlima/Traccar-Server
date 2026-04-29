package com.example.traccarserver.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
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
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "traccar_server_channel"
        
        var isRunning = false
        val logLines = mutableListOf<String>()
        var onLogAdded: ((String) -> Unit)? = null
        var onStatusChanged: ((Boolean) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        envManager = EnvironmentManager(this)
        createNotificationChannel()
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TraccarServer::WakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startServer()
            ACTION_STOP -> stopServer()
        }
        return START_STICKY
    }

    private fun startServer() {
        if (isRunning) return
        
        wakeLock?.acquire(24*60*60*1000L /* 24 horas */)

        val notification = createNotification("Servidor Traccar Ativo")
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            try {
                val traccarDir = envManager.getTraccarDir() ?: throw IOException("Pasta do Traccar não selecionada ou inacessível")
                val jarFile = envManager.getJarFile() ?: throw IOException("Arquivo .jar não encontrado na pasta selecionada")
                
                // O Traccar geralmente precisa de um arquivo de configuração
                val configPath = File(traccarDir, "conf/traccar.xml")
                val args = mutableListOf<String>()
                args.add(envManager.javaExecutable.absolutePath)
                args.add("-Xms128m")
                args.add("-Xmx512m")
                args.add("-Djava.net.preferIPv4Stack=true")
                args.add("-jar")
                args.add(jarFile.absolutePath)
                
                if (configPath.exists()) {
                    args.add(configPath.absolutePath)
                    addLog("Usando configuração: ${configPath.absolutePath}")
                } else {
                    addLog("Aviso: conf/traccar.xml não encontrado. Tentando iniciar sem config específica.")
                }

                if (!envManager.isJavaInstalled()) {
                    throw IOException("Java não instalado ou sem permissão de execução.")
                }

                addLog("Iniciando processo Java...")
                addLog("Comando: java -jar ${jarFile.name}")
                
                val processBuilder = ProcessBuilder(args)

                // CONFIGURAÇÃO DE AMBIENTE ESTILO TERMUX ($PREFIX)
                val env = processBuilder.environment()
                env["PREFIX"] = envManager.prefixDir.absolutePath
                env["HOME"] = filesDir.absolutePath
                env["JAVA_HOME"] = envManager.javaHome.absolutePath
                env["PATH"] = "${envManager.binDir.absolutePath}:/system/bin:/system/xbin"
                env["LD_LIBRARY_PATH"] = "${envManager.libDir.absolutePath}:${File(envManager.libDir, "server").absolutePath}"
                env["TMPDIR"] = envManager.tmpDir.absolutePath
                
                processBuilder.directory(traccarDir)
                processBuilder.redirectErrorStream(true)

                serverProcess = processBuilder.start()
                isRunning = true
                withContext(Dispatchers.Main) { onStatusChanged?.invoke(true) }

                val reader = BufferedReader(InputStreamReader(serverProcess?.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { addLog(it) }
                }

                val exitCode = serverProcess?.waitFor()
                addLog("Servidor finalizado (Código: $exitCode)")
            } catch (e: Exception) {
                addLog("ERRO CRÍTICO: ${e.message}")
                Log.e("ServerService", "Falha no servidor", e)
            } finally {
                stopServer()
            }
        }
    }

    private fun stopServer() {
        serverProcess?.destroy()
        serverProcess = null
        isRunning = false
        
        if (wakeLock?.isHeld == true) wakeLock?.release()
        
        serviceScope.launch(Dispatchers.Main) { onStatusChanged?.invoke(false) }
        stopForeground(true)
        stopSelf()
    }

    private fun addLog(line: String) {
        Log.d("TraccarLog", line)
        synchronized(logLines) {
            logLines.add(line)
            if (logLines.size > 1000) logLines.removeAt(0)
        }
        serviceScope.launch(Dispatchers.Main) { onLogAdded?.invoke(line) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Traccar Server", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Traccar Server")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopServer()
        serviceScope.cancel()
        super.onDestroy()
    }
}
