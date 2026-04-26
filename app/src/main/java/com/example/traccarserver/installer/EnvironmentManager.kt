package com.example.traccarserver.installer

import android.content.Context
import java.io.File
import java.io.IOException

class EnvironmentManager(private val context: Context) {

    private val baseDir: File = File(context.filesDir, "server")
    val javaDir = File(baseDir, "java")
    val traccarDir = File(baseDir, "traccar")
    val javaExecutable = File(javaDir, "bin/java")

    fun isInstalled(): Boolean {
        return javaExecutable.exists() && File(traccarDir, "traccar.jar").exists()
    }

    @Throws(IOException::class)
    fun install(onProgress: (String) -> Unit) {
        val extractor = AssetExtractor(context)

        onProgress("Limpando diretórios antigos...")
        baseDir.deleteRecursively()
        baseDir.mkdirs()

        onProgress("Extraindo Java 17 (JRE)...")
        extractor.extractTarGz("java17.tar.gz", javaDir)

        onProgress("Extraindo arquivos do Traccar...")
        extractor.extractZip("traccar.zip", traccarDir)

        onProgress("Configurando permissões do binário Java...")
        if (javaExecutable.exists()) {
            javaExecutable.setExecutable(true, false)
        } else {
            throw IOException("Binário Java não encontrado após extração em: ${javaExecutable.absolutePath}")
        }

        onProgress("Ambiente instalado com sucesso!")
    }

    fun getTraccarConfigPath(): String {
        return File(traccarDir, "conf/traccar.xml").absolutePath
    }
}
