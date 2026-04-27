package com.example.traccarserver.ui

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.traccarserver.installer.EnvironmentManager
import com.example.traccarserver.installer.JavaDownloadWorker
import com.example.traccarserver.server.ServerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val envManager = EnvironmentManager(application)
    private val workManager = WorkManager.getInstance(application)
    
    val isServerRunning = mutableStateOf(ServerService.isRunning)
    val traccarPath = mutableStateOf(envManager.traccarDirPath ?: "Nenhuma pasta selecionada")
    val isTraccarReady = mutableStateOf(envManager.isTraccarReady())
    val isJavaReady = mutableStateOf(envManager.isJavaReady())
    val downloadProgress = mutableStateOf<Int?>(null)
    val downloadStatus = mutableStateOf<String?>(null)
    val serverLogs = mutableStateListOf<String>()

    init {
        serverLogs.addAll(ServerService.logLines)
        ServerService.onLogAdded = { log ->
            serverLogs.add(log)
            if (serverLogs.size > 1000) serverLogs.removeAt(0)
        }
        ServerService.onStatusChanged = { running ->
            isServerRunning.value = running
        }
        
        checkJavaStatus()
        observeDownload()
    }

    fun checkJavaStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            envManager.ensureJavaInstalled()
            withContext(Dispatchers.Main) {
                isJavaReady.value = envManager.isJavaReady()
            }
        }
    }

    private fun observeDownload() {
        workManager.getWorkInfosByTagLiveData("java_download").observeForever { infos ->
            val info = infos.firstOrNull() ?: return@observeForever
            
            val progress = info.progress.getInt(JavaDownloadWorker.KEY_PROGRESS, -1)
            if (progress != -1) downloadProgress.value = progress
            
            val status = info.progress.getString(JavaDownloadWorker.KEY_STATUS)
            if (status != null) downloadStatus.value = status

            if (info.state.isFinished) {
                downloadProgress.value = null
                checkJavaStatus()
                if (info.state == WorkInfo.State.SUCCEEDED) {
                    downloadStatus.value = "Java instalado!"
                } else {
                    val error = info.outputData.getString(JavaDownloadWorker.KEY_STATUS)
                    downloadStatus.value = error ?: "Falha no download"
                }
            }
        }
    }

    fun startJavaDownload() {
        val request = OneTimeWorkRequestBuilder<JavaDownloadWorker>()
            .addTag("java_download")
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueueUniqueWork("java_download", ExistingWorkPolicy.KEEP, request)
    }

    fun updateTraccarPath(path: String) {
        envManager.traccarDirPath = path
        traccarPath.value = path
        isTraccarReady.value = envManager.isTraccarReady()
    }

    fun startServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Garante que o Java esteja lá antes de tentar iniciar o serviço
                envManager.ensureJavaInstalled()
                
                withContext(Dispatchers.Main) {
                    val intent = Intent(getApplication(), ServerService::class.java).apply {
                        action = ServerService.ACTION_START
                    }
                    getApplication<Application>().startForegroundService(intent)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    serverLogs.add("Erro ao iniciar: ${e.message}")
                }
            }
        }
    }

    fun stopServer() {
        val intent = Intent(getApplication(), ServerService::class.java).apply {
            action = ServerService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }
}
