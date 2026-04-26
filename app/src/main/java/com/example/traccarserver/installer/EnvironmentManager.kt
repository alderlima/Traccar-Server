package com.example.traccarserver.installer

import android.content.Context
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

    fun isJavaReady(): Boolean {
        return javaExecutable.exists() && javaExecutable.canExecute()
    }

    fun isTraccarReady(): Boolean {
        val dir = getTraccarDir() ?: return false
        return File(dir, "traccar.jar").exists()
    }

    @Throws(IOException::class)
    fun ensureJavaInstalled() {
        if (isJavaReady()) return

        Log.d("EnvironmentManager", "Java não encontrado ou não executável. Iniciando extração silenciosa...")
        val extractor = AssetExtractor(context)
        
        if (javaDir.exists()) javaDir.deleteRecursively()
        javaDir.mkdirs()

        extractor.extractTarGz("java17.tar.gz", javaDir)

        if (javaExecutable.exists()) {
            javaExecutable.setExecutable(true, false)
            Log.d("EnvironmentManager", "Java 17 extraído e configurado com sucesso.")
        } else {
            throw IOException("Falha ao localizar binário Java após extração.")
        }
    }

    fun getTraccarConfigPath(): String? {
        val dir = getTraccarDir() ?: return null
        return File(dir, "conf/traccar.xml").absolutePath
    }
}
