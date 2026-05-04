package com.forma2.app.service

import com.forma2.app.util.LogManager
import com.forma2.app.util.ServiceState
import kotlinx.coroutines.*
import java.io.File

object JavaProcessService {
    private var job: Job? = null
    private var currentProcess: Process? = null

    fun start(jarPath: String, workingDir: String, javaHome: String) {
        job = CoroutineScope(Dispatchers.IO).launch {
            ServiceState.setRunning(true)
            LogManager.appendLog("Iniciando JVM (modelo Termux)...")
            try {
                val javaBin = File(javaHome, "bin/java").absolutePath
                // Usar wrapper se disponível, senão java puro
                val wrapper = File(javaHome, "bin/java-wrapper")
                val executable = if (wrapper.exists()) wrapper.absolutePath else javaBin

                val command = arrayOf(
                    executable,
                    "-jar", jarPath
                )

                val env = System.getenv().toMutableMap()
                env["JAVA_HOME"] = javaHome
                env["PATH"] = "${javaHome}/bin:${System.getenv("PATH")}"
                val ldPath = arrayOf(
                    "${javaHome}/lib",
                    "${javaHome}/lib/jli",
                    "${javaHome}/lib/server"
                ).joinToString(":")
                env["LD_LIBRARY_PATH"] = if (env.containsKey("LD_LIBRARY_PATH")) {
                    "$ldPath:${env["LD_LIBRARY_PATH"]}"
                } else {
                    ldPath
                }

                val pb = ProcessBuilder(command)
                    .directory(File(workingDir))
                    .apply { environment().putAll(env) }
                    .redirectErrorStream(true)

                currentProcess = pb.start()
                LogManager.appendLog("PID: ${currentProcess?.pid()}")

                val reader = currentProcess!!.inputStream.bufferedReader()
                reader.useLines { lines ->
                    lines.forEach { line ->
                        LogManager.appendLog(line)
                    }
                }

                val exitCode = currentProcess?.waitFor() ?: -1
                LogManager.appendLog("Processo encerrado com código: $exitCode")
            } catch (e: Exception) {
                LogManager.appendLog("Erro: ${e.message}")
            } finally {
                currentProcess = null
                ServiceState.setRunning(false)
            }
        }
    }

    fun stop() {
        currentProcess?.destroy()
        job?.cancel()
    }
}