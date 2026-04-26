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
    
    val isServerRunning = mutableStateOf(ServerService.isRunning)
    val traccarPath = mutableStateOf(envManager.traccarDirPath ?: "Nenhuma pasta selecionada")
    val isTraccarReady = mutableStateOf(envManager.isTraccarReady())
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
        
        // Tenta garantir que o Java esteja pronto em background no início
        viewModelScope.launch(Dispatchers.IO) {
            try {
                envManager.ensureJavaInstalled()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    serverLogs.add("Erro crítico: Falha ao preparar Java interno: ${e.message}")
                }
            }
        }
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
