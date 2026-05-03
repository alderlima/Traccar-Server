package com.forma2.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.forma2.app.data.PreferencesManager
import com.forma2.app.service.JavaProcessService
import com.forma2.app.util.JavaInstaller
import com.forma2.app.util.LogManager
import com.forma2.app.util.ServiceState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)

    data class UiState(
        val jdkInstalled: Boolean = false,
        val jdkPath: String = "",
        val installProgress: Float = 0f,
        val installing: Boolean = false,
        val selectedDirectoryUri: Uri? = null,
        val jarFiles: List<DocumentFile> = emptyList(),
        val selectedJar: DocumentFile? = null,
        val selectedJarPath: String = "",
        val processRunning: Boolean = false,
        val logs: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
        observeLogs()
        observeServiceState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val jdkPath = prefs.jdkPath.first()
            val dirUriStr = prefs.selectedDirectoryUri.first()
            val dirUri = if (dirUriStr.isNotEmpty()) Uri.parse(dirUriStr) else null
            _uiState.update {
                it.copy(
                    jdkInstalled = jdkPath.isNotEmpty() && File(jdkPath).exists(),
                    jdkPath = jdkPath,
                    selectedDirectoryUri = dirUri
                )
            }
            if (dirUri != null) {
                refreshJarList(dirUri)
            }
        }
    }

    private fun observeLogs() {
        viewModelScope.launch {
            LogManager.logs.collect { log ->
                _uiState.update { it.copy(logs = log) }
            }
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            ServiceState.isRunning.collect { running ->
                _uiState.update { it.copy(processRunning = running) }
            }
        }
    }

    fun installJava() {
        viewModelScope.launch {
            if (_uiState.value.installing) return@launch
            _uiState.update { it.copy(installing = true, installProgress = 0f) }
            try {
                val jdkPath = JavaInstaller.downloadAndExtract(
                    context = getApplication(),
                    onProgress = { progress ->
                        _uiState.update { it.copy(installProgress = progress) }
                    }
                )
                if (jdkPath != null) {
                    prefs.saveJdkPath(jdkPath)
                    _uiState.update {
                        it.copy(
                            jdkInstalled = true,
                            jdkPath = jdkPath,
                            installing = false,
                            installProgress = 1f
                        )
                    }
                    LogManager.appendLog("Java 17 instalado em $jdkPath")
                } else {
                    _uiState.update { it.copy(installing = false) }
                    LogManager.appendLog("Falha na instalação do Java")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(installing = false) }
                LogManager.appendLog("Erro na instalação: ${e.message}")
            }
        }
    }

    fun pickDirectory(uri: Uri) {
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}

        viewModelScope.launch {
            prefs.saveSelectedDirectoryUri(uri.toString())
            _uiState.update { it.copy(selectedDirectoryUri = uri) }
            refreshJarList(uri)
        }
    }

    private fun refreshJarList(dirUri: Uri) {
        viewModelScope.launch {
            val docFile = DocumentFile.fromTreeUri(getApplication(), dirUri)
            val jarFiles = docFile?.listFiles()?.filter {
                it.isFile && it.name?.endsWith(".jar", true) == true
            } ?: emptyList()
            _uiState.update { it.copy(jarFiles = jarFiles) }
        }
    }

    fun selectJar(docFile: DocumentFile) {
        viewModelScope.launch {
            val cacheDir = File(getApplication().cacheDir, "jars").also { it.mkdirs() }
            val destFile = File(cacheDir, docFile.name ?: "app.jar")
            try {
                getApplication().contentResolver.openInputStream(docFile.uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val jarPath = destFile.absolutePath
                _uiState.update { it.copy(selectedJar = docFile, selectedJarPath = jarPath) }
                LogManager.appendLog("Jar copiado para $jarPath")
            } catch (e: Exception) {
                LogManager.appendLog("Erro ao copiar jar: ${e.message}")
            }
        }
    }

    fun startProcess() {
        val state = _uiState.value
        if (state.processRunning) return
        val jarPath = state.selectedJarPath
        val jdkPath = state.jdkPath
        if (jarPath.isNotEmpty() && jdkPath.isNotEmpty()) {
            val workingDir = File(jarPath).parent ?: getApplication().filesDir.absolutePath
            JavaProcessService.start(
                context = getApplication(),
                jarPath = jarPath,
                workingDir = workingDir,
                javaHome = jdkPath
            )
        }
    }

    fun stopProcess() {
        JavaProcessService.stop(getApplication())
    }

    fun clearLogs() {
        LogManager.clearLogs()
    }
}
