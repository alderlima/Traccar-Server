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
    val traccarDir = File(baseDir, "traccar")
    val javaDir = File(baseDir, "java")
    val javaExecutable = File(javaDir, "bin/java")

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
        if (isJavaReady()) return

        // Tenta extrair dos assets apenas se o arquivo existir
        val assetExists = try {
            context.assets.open("java17.tar.gz").close()
            true
        } catch (e: IOException) {
            false
        }

        if (assetExists) {
            Log.d("EnvironmentManager", "Extraindo Java 17 interno...")
            val extractor = AssetExtractor(context)
            if (javaDir.exists()) javaDir.deleteRecursively()
            javaDir.mkdirs()
            try {
                extractor.extractTarGz("java17.tar.gz", javaDir)
                if (javaExecutable.exists()) {
                    javaExecutable.setExecutable(true, false)
                    Log.d("EnvironmentManager", "Java 17 pronto.")
                }
            } catch (e: Exception) {
                Log.e("EnvironmentManager", "Falha na extração do Java: ${e.message}")
            }
        } else {
            Log.d("EnvironmentManager", "Java 17 não encontrado nos assets. Necessário download.")
        }
    }
}
