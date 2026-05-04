package com.forma2.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.forma2.app.ui.screen.HomeScreen
import com.forma2.app.ui.theme.Forma2Theme
import com.forma2.app.viewmodel.MainViewModel
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {

    private val crashLogFile by lazy {
        File(filesDir, "crash.log")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Define o handler global de exceções não capturadas
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val crashLog = "FATAL CRASH\nThread: ${thread.name}\n${sw}"
            try {
                crashLogFile.writeText(crashLog)
            } catch (_: Exception) {}
            // Reabre o app na próxima vez (opcional)
            // Para garantir que o log seja visto, reinicie o processo
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        val viewModel: MainViewModel by viewModels()

        // Carrega o último crash log se existir
        if (crashLogFile.exists()) {
            viewModel.loadCrashLog(crashLogFile.readText())
            crashLogFile.delete()
        }

        setContent {
            Forma2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}