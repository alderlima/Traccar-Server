package com.example.traccarserver.ui

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.traccarserver.installer.EnvironmentManager
import com.example.traccarserver.server.ServerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val envManager = EnvironmentManager(application)
    
    val isInstalled = mutableStateOf(envManager.isInstalled())
    val isServerRunning = mutableStateOf(ServerService.isRunning)
    val installationLogs = mutableStateOf("")
    val isInstalling = mutableStateOf(false)
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
    }

    fun installEnvironment() {
        viewModelScope.launch {
            isInstalling.value = true
            try {
                withContext(Dispatchers.IO) {
                    envManager.install { progress ->
                        viewModelScope.launch(Dispatchers.Main) {
                            installationLogs.value = progress
                        }
                    }
                }
                isInstalled.value = true
            } catch (e: Exception) {
                installationLogs.value = "Erro: ${e.message}"
            } finally {
                isInstalling.value = false
            }
        }
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
