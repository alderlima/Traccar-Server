package com.forma2.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LogManager {
    private val _logs = MutableStateFlow("")
    val logs: StateFlow<String> = _logs.asStateFlow()

    fun appendLog(message: String) {
        _logs.value = _logs.value + message + "\n"
    }

    fun clearLogs() {
        _logs.value = ""
    }
}
