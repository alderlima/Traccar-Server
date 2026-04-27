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
        
        // Configura o WakeLock para manter a CPU ativa com baixo consumo
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TraccarServer::WakeLock")
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
        
        // Ativa o WakeLock para garantir que o servidor não durma
        wakeLock?.acquire(10*60*1000L /* 10 minutos ou até o processo terminar */)

        val notification = createNotification("Servidor Traccar Ativo")
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            try {
                val traccarDir = envManager.getTraccarDir() ?: throw IOException("Diretório do Traccar não selecionado")
                val jarFile = envManager.getJarFile() ?: throw IOException("Arquivo .jar do Traccar não encontrado")
                val traccarJar = jarFile.absolutePath
                val configPath = File(traccarDir, "conf/traccar.xml").absolutePath

                addLog("Iniciando Traccar via DalvikVM...")
                addLog("Diretório: ${traccarDir.absolutePath}")
                
                // Garante que a pasta de dados exista para o H2
                val dataDir = File(traccarDir, "data")
                if (!dataDir.exists()) dataDir.mkdirs()

                // Monta o Classpath com o JAR principal e todas as libs
                val libDir = File(traccarDir, "lib")
                val classpath = StringBuilder(traccarJar)
                if (libDir.exists() && libDir.isDirectory) {
                    libDir.listFiles { _, name -> name.endsWith(".jar") }?.forEach {
                        classpath.append(":").append(it.absolutePath)
                    }
                }

                // Tenta usar o Java externo se ele existir e for executável
                val javaExec = envManager.javaExecutable.absolutePath
                val canUseExternalJava = envManager.javaExecutable.exists() && envManager.javaExecutable.canExecute()

                val processBuilder = if (canUseExternalJava) {
                    addLog("Usando Java 17 (Ambiente Termux)...")
                    ProcessBuilder(
                        javaExec,
                        "-Xms128m",
                        "-Xmx512m",
                        "-Djava.net.preferIPv4Stack=true",
                        "-jar",
                        traccarJar,
                        configPath
                    )
                } else {
                    addLog("Usando DalvikVM (Nativo)...")
                    ProcessBuilder(
                        "dalvikvm",
                        "-Xmx512m",
                        "-cp", classpath.toString(),
                        "org.traccar.Main",
                        configPath
                    )
                }
                
                // Configuração de Ambiente Estilo Termux
                val env = processBuilder.environment()
                env["JAVA_HOME"] = envManager.javaHome.absolutePath
                env["PATH"] = "${envManager.binDir.absolutePath}:/system/bin:/system/xbin"
                env["LD_LIBRARY_PATH"] = "${envManager.libDir.absolutePath}:${File(envManager.libDir, "server").absolutePath}"
                env["HOME"] = filesDir.absolutePath
                env["TMPDIR"] = envManager.tmpDir.absolutePath
                env["ANDROID_DATA"] = File(filesDir, "android_data").apply { mkdirs() }.absolutePath
                
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
            
            // Libera o WakeLock
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            
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
