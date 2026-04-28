package com.example.traccarserver.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.traccarserver.installer.EnvironmentManager
import com.example.traccarserver.installer.JavaDownloadWorker
import com.example.traccarserver.server.ServerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val envManager = EnvironmentManager(application)
    private val workManager = WorkManager.getInstance(application)
    
    private val _isServerRunning = MutableStateFlow(ServerService.isRunning)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning

    private val _traccarPath = MutableStateFlow(envManager.traccarDirPath)
    val traccarPath: StateFlow<String?> = _traccarPath

    private val _isTraccarReady = MutableStateFlow(envManager.isTraccarReady())
    val isTraccarReady: StateFlow<Boolean> = _isTraccarReady

    private val _isJavaReady = MutableStateFlow(envManager.isJavaInstalled())
    val isJavaReady: StateFlow<Boolean> = _isJavaReady

    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus: StateFlow<String?> = _downloadStatus

    private val _serverLogs = MutableStateFlow<List<String>>(emptyList())
    val serverLogs: StateFlow<List<String>> = _serverLogs

    init {
        viewModelScope.launch {
            while (true) {
                _isServerRunning.value = ServerService.isRunning
                _serverLogs.value = ServerService.logLines.toList()
                _isJavaReady.value = envManager.isJavaInstalled()
                _isTraccarReady.value = envManager.isTraccarReady()
                delay(1000)
            }
        }
        observeDownload()
    }

    private fun observeDownload() {
        workManager.getWorkInfosByTagLiveData("java_download").observeForever { infos ->
            val info = infos.firstOrNull() ?: return@observeForever
            
            val progress = info.progress.getInt(JavaDownloadWorker.KEY_PROGRESS, -1)
            if (progress != -1) _downloadProgress.value = progress
            
            val status = info.progress.getString(JavaDownloadWorker.KEY_STATUS)
            if (status != null) _downloadStatus.value = status

            if (info.state.isFinished) {
                _isJavaReady.value = envManager.isJavaInstalled()
                if (info.state == WorkInfo.State.SUCCEEDED) {
                    _downloadStatus.value = "Java instalado!"
                    _downloadProgress.value = 100
                } else {
                    val error = info.outputData.getString(JavaDownloadWorker.KEY_STATUS)
                    _downloadStatus.value = error ?: "Falha no download"
                }
            }
        }
    }

    fun startJavaDownload() {
        val request = OneTimeWorkRequestBuilder<JavaDownloadWorker>()
            .addTag("java_download")
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueueUniqueWork("java_download", ExistingWorkPolicy.REPLACE, request)
    }

    fun updateTraccarPath(path: String) {
        envManager.traccarDirPath = path
        _traccarPath.value = path
        _isTraccarReady.value = envManager.isTraccarReady()
    }

    fun resolveUri(uri: Uri): String? {
        return envManager.resolveUriToPath(uri)
    }

    fun startServer() {
        val intent = Intent(getApplication(), ServerService::class.java).apply {
            action = ServerService.ACTION_START
        }
        getApplication<Application>().startForegroundService(intent)
    }

    fun stopServer() {
        val intent = Intent(getApplication(), ServerService::class.java).apply {
            action = ServerService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }
}
