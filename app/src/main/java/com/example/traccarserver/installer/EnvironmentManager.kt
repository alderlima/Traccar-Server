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
        return File(path)
    }

    fun resolveUriToPath(uri: Uri): String? {
        return try {
            val uriString = uri.toString()
            when {
                uriString.contains("primary:") -> {
                    val split = uriString.split("primary:")[1].replace("%2F", "/")
                    File(Environment.getExternalStorageDirectory(), split).absolutePath
                }
                uriString.contains("document/raw:") -> {
                    uriString.split("document/raw:")[1].replace("%2F", "/")
                }
                else -> {
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

    fun isJavaInstalled(): Boolean {
        return javaExecutable.exists() && javaExecutable.canExecute()
    }

    fun isTraccarReady(): Boolean {
        return getJarFile()?.exists() == true
    }

    fun getJarFile(): File? {
        val dir = getTraccarDir() ?: return null
        val possibleNames = listOf("tracker-server.jar", "traccar-server.jar", "traccar.jar")
        for (name in possibleNames) {
            val file = File(dir, name)
            if (file.exists()) return file
        }
        return null
    }
}
