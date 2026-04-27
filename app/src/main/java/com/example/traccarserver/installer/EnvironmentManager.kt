package com.example.traccarserver.installer

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException

class EnvironmentManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("traccar_prefs", Context.MODE_PRIVATE)
    // Estrutura estilo Termux: /data/data/com.example.traccarserver/files/usr/
    private val usrDir: File = File(context.filesDir, "usr")
    val binDir = File(usrDir, "bin")
    val libDir = File(usrDir, "lib")
    val tmpDir = File(context.cacheDir, "tmp")
    
    val javaExecutable = File(binDir, "java")
    val javaHome = usrDir // No estilo Termux, o prefixo costuma ser o JAVA_HOME

    var traccarDirPath: String?
        get() = prefs.getString("traccar_path", null)
        set(value) = prefs.edit().putString("traccar_path", value).apply()

    fun getTraccarDir(): File? {
        val path = traccarDirPath ?: return null
        return File(path)
    }

    /**
     * Tenta converter o URI do SAF para um caminho de arquivo absoluto.
     * Adicionado tratamento de erro para evitar crashes.
     */
    fun resolveUriToPath(uri: Uri): String? {
        return try {
            val uriString = uri.toString()
            Log.d("EnvironmentManager", "Resolvendo URI: $uriString")

            when {
                uriString.contains("primary:") -> {
                    val split = uriString.split("primary:")[1].replace("%2F", "/")
                    File(Environment.getExternalStorageDirectory(), split).absolutePath
                }
                uriString.contains("document/raw:") -> {
                    uriString.split("document/raw:")[1].replace("%2F", "/")
                }
                else -> {
                    // Tenta extrair o ID do documento
                    val docId = uri.path?.split(":")?.lastOrNull()?.replace("%2F", "/")
                    if (docId != null) {
                        File(Environment.getExternalStorageDirectory(), docId).absolutePath
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("EnvironmentManager", "Erro ao resolver URI: ${e.message}")
            null
        }
    }

    fun isJavaReady(): Boolean {
        // Agora usamos o Java nativo do Android (dalvikvm), que sempre está pronto.
        return true
    }

    fun isTraccarReady(): Boolean {
        val dir = getTraccarDir() ?: return false
        return getJarFile() != null
    }

    fun getJarFile(): File? {
        val dir = getTraccarDir() ?: return null
        // Procura pelos nomes comuns do JAR do Traccar
        val possibleNames = listOf("tracker-server.jar", "traccar-server.jar", "traccar.jar")
        for (name in possibleNames) {
            val file = File(dir, name)
            if (file.exists()) return file
        }
        return null
    }

    @Throws(IOException::class)
    fun ensureJavaInstalled() {
        // Agora usamos o Java nativo do Android (dalvikvm) ou o baixado via Worker.
        // Este método pode ser mantido vazio ou removido se não for mais chamado.
    }
}
