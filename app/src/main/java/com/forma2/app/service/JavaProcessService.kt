package com.forma2.app.service

import com.forma2.app.util.LogManager
import com.forma2.app.util.ServiceState
import kotlinx.coroutines.*
import java.io.File

object JavaProcessService {
    private var job: Job? = null

    fun start(jarPath: String, workingDir: String, javaHome: String) {
        job = CoroutineScope(Dispatchers.IO).launch {
            ServiceState.setRunning(true)
            LogManager.appendLog("Iniciando JVM dentro do app...")
            try {
                // Carrega a libjvm.so (extraída junto com a JRE)
                val libJvmPath = File(javaHome, "lib/libjvm.so").absolutePath
                System.load(libJvmPath)
                LogManager.appendLog("libjvm.so carregada de $libJvmPath")

                // Chama o método nativo que cria a JVM e executa o JAR
                startJvmAndRunJar(jarPath, workingDir, javaHome)
            } catch (e: Exception) {
                LogManager.appendLog("Erro JVM: ${e.message}")
                e.printStackTrace()
            } finally {
                ServiceState.setRunning(false)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    // Método nativo implementado em cpp/jvm-bridge.cpp
    @JvmStatic
    private external fun startJvmAndRunJar(jarPath: String, workingDir: String, javaHome: String)
}