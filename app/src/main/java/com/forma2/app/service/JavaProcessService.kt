package com.forma2.app.service

import android.content.Context
import com.forma2.app.util.LogManager
import com.forma2.app.util.ServiceState
import kotlinx.coroutines.*

class JavaProcessService {

    companion object {
        private var job: Job? = null
        private var jvmLoaded = false

        fun start(context: Context, jarPath: String, workingDir: String, javaHome: String) {
            // javaHome agora é o caminho da JRE extraída (ex.: /data/user/0/.../files/jre/jdk-17...-jre)
            job = CoroutineScope(Dispatchers.IO).launch {
                ServiceState.setRunning(true)
                LogManager.appendLog("Iniciando JVM dentro do app...")
                try {
                    // Carrega a JVM via JNI
                    if (!jvmLoaded) {
                        System.load(File(javaHome, "lib/libjvm.so").absolutePath)
                        jvmLoaded = true
                        LogManager.appendLog("libjvm.so carregada com sucesso")
                    }

                    // Usa a API JNI para iniciar a JVM e chamar o método main
                    startJvmAndRunJar(jarPath, workingDir, javaHome)
                } catch (e: Exception) {
                    LogManager.appendLog("Erro JVM: ${e.message}")
                    e.printStackTrace()
                } finally {
                    ServiceState.setRunning(false)
                }
            }
        }

        fun stop(context: Context) {
            job?.cancel()
            // A JVM não pode ser desligada facilmente sem matar o processo
            // Apenas interrompemos a corrotina
        }

        private external fun startJvmAndRunJar(jarPath: String, workingDir: String, javaHome: String)
    }
}