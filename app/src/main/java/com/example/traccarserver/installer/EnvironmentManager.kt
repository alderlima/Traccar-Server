package com.example.traccarserver.installer

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File

class EnvironmentManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("traccar_prefs", Context.MODE_PRIVATE)

    // Estrutura idêntica ao Termux ($PREFIX)
    val prefixDir: File = File(context.filesDir, "usr")
    val binDir = File(prefixDir, "bin")
    val libDir = File(prefixDir, "lib")
    val tmpDir = File(context.filesDir, "tmp")

    // Caminhos do Java – serão atualizados dinamicamente se encontrados
    var javaExecutable: File = File(binDir, "java")
        private set
    var javaHome: File = prefixDir
        private set

    var traccarDirPath: String?
        get() = prefs.getString("traccar_path", null)
        set(value) = prefs.edit().putString("traccar_path", value).apply()

    init {
        // Tenta localizar o Java usando cache ou busca recursiva
        locateJava()
    }

    /**
     * Localiza o binário java dentro do prefixDir.
     * Prioriza o último caminho salvo e, se não existir, faz busca recursiva.
     */
    private fun locateJava() {
        val cachedPath = prefs.getString("java_executable_path", null)
        if (cachedPath != null) {
            val cachedFile = File(cachedPath)
            if (cachedFile.exists() && cachedFile.canExecute()) {
                javaExecutable = cachedFile
                javaHome = cachedFile.parentFile?.parentFile ?: prefixDir
                return
            }
        }

        // Busca recursiva por .../bin/java
        val found = findJavaBinary(prefixDir)
        if (found != null) {
            javaExecutable = found
            javaHome = found.parentFile?.parentFile ?: prefixDir
            // Salva para uso futuro
            prefs.edit().putString("java_executable_path", found.absolutePath).apply()
        } else {
            // Fallback para o caminho padrão (pode não existir)
            javaExecutable = File(binDir, "java")
            javaHome = prefixDir
        }
    }

    /**
     * Busca recursivamente em 'dir' por um arquivo chamado 'java' cujo pai seja 'bin'.
     */
    private fun findJavaBinary(dir: File): File? {
        if (!dir.exists() || !dir.isDirectory) return null
        val files = dir.listFiles() ?: return null
        for (file in files) {
            if (file.isDirectory) {
                val found = findJavaBinary(file)
                if (found != null) return found
            } else if (file.name == "java" && file.parentFile?.name == "bin") {
                return file
            }
        }
        return null
    }

    /**
     * Verifica se o Java está instalado e executável.
     * Tenta localizar novamente a cada chamada (garante detecção após instalação).
     */
    fun isJavaInstalled(): Boolean {
        // Reavalia dinamicamente (caso o worker tenha extraído depois da criação do manager)
        locateJava()
        val exists = javaExecutable.exists()
        val canExec = exists && (javaExecutable.canExecute() || trySetExecutable(javaExecutable))
        if (!canExec && exists) {
            Log.w("EnvironmentManager", "Java existe mas não é executável: ${javaExecutable.absolutePath}")
        }
        return canExec
    }

    private fun trySetExecutable(file: File): Boolean {
        return try {
            file.setExecutable(true, false) // false = owner, group, others
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Força uma nova busca pelo Java (útil após instalação manual ou erro).
     */
    fun refreshJava() {
        prefs.edit().remove("java_executable_path").apply()
        locateJava()
    }

    fun getTraccarDir(): File? {
        val path = traccarDirPath ?: return null
        val dir = File(path)
        return if (dir.exists() && dir.isDirectory) dir else null
    }

    fun resolveUriToPath(uri: Uri): String? {
        return try {
            val path = uri.path ?: return null
            when {
                path.contains("primary:") -> {
                    val split = path.split("primary:")[1]
                    File(Environment.getExternalStorageDirectory(), split).absolutePath
                }
                path.contains("/tree/") -> {
                    val parts = path.split(":")
                    if (parts.size > 1) {
                        File(Environment.getExternalStorageDirectory(), parts[1]).absolutePath
                    } else null
                }
                else -> {
                    val file = File(path)
                    if (file.exists()) file.absolutePath else null
                }
            }
        } catch (e: Exception) {
            Log.e("EnvironmentManager", "Erro ao resolver URI: ${e.message}")
            null
        }
    }

    fun isTraccarReady(): Boolean {
        return getJarFile() != null
    }

    fun getJarFile(): File? {
        val dir = getTraccarDir() ?: return null
        val possibleNames = listOf("tracker-server.jar", "traccar-server.jar", "traccar.jar")
        for (name in possibleNames) {
            val file = File(dir, name)
            if (file.exists() && file.isFile) return file
        }
        val targetDir = File(dir, "target")
        if (targetDir.exists() && targetDir.isDirectory) {
            for (name in possibleNames) {
                val file = File(targetDir, name)
                if (file.exists() && file.isFile) return file
            }
        }
        return null
    }
}