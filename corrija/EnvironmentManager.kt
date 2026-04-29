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
    
    // O binário java dentro do ambiente Termux
    val javaExecutable = File(binDir, "java")
    val javaHome = prefixDir

    var traccarDirPath: String?
        get() = prefs.getString("traccar_path", null)
        set(value) = prefs.edit().putString("traccar_path", value).apply()

    fun getTraccarDir(): File? {
        val path = traccarDirPath ?: return null
        val dir = File(path)
        return if (dir.exists() && dir.isDirectory) dir else null
    }

    /**
     * Tenta resolver a URI do SAF para um caminho de arquivo real.
     * Nota: No Android moderno, o acesso direto via File pode ser restrito.
     * Esta função tenta lidar com os formatos mais comuns de URI de árvore.
     */
    fun resolveUriToPath(uri: Uri): String? {
        return try {
            val path = uri.path ?: return null
            
            // Lidar com URIs do provedor de armazenamento externo
            if (path.contains("primary:")) {
                val split = path.split("primary:")[1]
                File(Environment.getExternalStorageDirectory(), split).absolutePath
            } else if (path.contains("/tree/")) {
                // Formato comum: /tree/primary:Documents/Traccar
                val parts = path.split(":")
                if (parts.size > 1) {
                    File(Environment.getExternalStorageDirectory(), parts[1]).absolutePath
                } else {
                    null
                }
            } else {
                // Fallback para caminhos diretos se possível
                val file = File(path)
                if (file.exists()) file.absolutePath else null
            }
        } catch (e: Exception) {
            Log.e("EnvironmentManager", "Erro ao resolver URI: ${e.message}")
            null
        }
    }

    fun isJavaInstalled(): Boolean {
        // Verifica se o binário existe e se temos permissão de execução
        return javaExecutable.exists() && (javaExecutable.canExecute() || trySetExecutable(javaExecutable))
    }
    
    private fun trySetExecutable(file: File): Boolean {
        return try {
            file.setExecutable(true)
        } catch (e: Exception) {
            false
        }
    }

    fun isTraccarReady(): Boolean {
        return getJarFile() != null
    }

    fun getJarFile(): File? {
        val dir = getTraccarDir() ?: return null
        // Lista de nomes comuns para o JAR do Traccar
        val possibleNames = listOf("tracker-server.jar", "traccar-server.jar", "traccar.jar")
        
        // Primeiro tenta encontrar na raiz da pasta selecionada
        for (name in possibleNames) {
            val file = File(dir, name)
            if (file.exists() && file.isFile) return file
        }
        
        // Se não encontrar, tenta procurar em subpastas comuns (como target/ se for build manual)
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
