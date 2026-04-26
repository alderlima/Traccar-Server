package com.example.traccarserver.installer

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException

class EnvironmentManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("traccar_prefs", Context.MODE_PRIVATE)
    private val baseDir: File = File(context.filesDir, "server")
    val javaDir = File(baseDir, "java")
    val javaExecutable = File(javaDir, "bin/java")

    var traccarDirPath: String?
        get() = prefs.getString("traccar_path", null)
        set(value) = prefs.edit().putString("traccar_path", value).apply()

    fun getTraccarDir(): File? {
        val path = traccarDirPath ?: return null
        return File(path)
    }

    // Resolve o URI do SAF para um caminho de arquivo real
    fun resolveUriToPath(uri: Uri): String? {
        val uriString = uri.toString()
        return if (uriString.contains("primary")) {
            val split = uriString.split("primary:")[1].replace("%2F", "/")
            Environment.getExternalStorageDirectory().absolutePath + "/" + split
        } else {
            // Fallback simples para o caminho do URI se não for primary
            uri.path?.split(":")?.lastOrNull()?.replace("%2F", "/")?.let {
                Environment.getExternalStorageDirectory().absolutePath + "/" + it
            }
        }
    }

    fun isJavaReady(): Boolean {
        return javaExecutable.exists() && javaExecutable.canExecute()
    }

    fun isTraccarReady(): Boolean {
        val dir = getTraccarDir() ?: return false
        // Verifica tanto 'tracker-server.jar' quanto 'traccar.jar' por compatibilidade
        return File(dir, "tracker-server.jar").exists() || File(dir, "traccar.jar").exists()
    }

    fun getJarFile(): File? {
        val dir = getTraccarDir() ?: return null
        val trackerServer = File(dir, "tracker-server.jar")
        if (trackerServer.exists()) return trackerServer
        val traccar = File(dir, "traccar.jar")
        if (traccar.exists()) return traccar
        return null
    }

    @Throws(IOException::class)
    fun ensureJavaInstalled() {
        if (isJavaReady()) return

        Log.d("EnvironmentManager", "Preparando Java 17 interno...")
        val extractor = AssetExtractor(context)
        
        if (javaDir.exists()) javaDir.deleteRecursively()
        javaDir.mkdirs()

        extractor.extractTarGz("java17.tar.gz", javaDir)

        if (javaExecutable.exists()) {
            javaExecutable.setExecutable(true, false)
        } else {
            throw IOException("Erro ao configurar binário Java.")
        }
    }
}
