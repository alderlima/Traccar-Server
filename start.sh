#!/bin/bash

# ========================================================
# SCRIPT GERADOR DO PROJETO FORMA 2 - JVM DENTRO DO APP
# Cria toda a estrutura de pastas e arquivos para Android
# ========================================================

PROJETO="Forma2"
PACOTE="com/forma2/app"
PACOTE_PATH="com.forma2.app"

echo "Criando projeto $PROJETO..."

# -------------------------------
# 1. Estrutura de diretórios
# -------------------------------
mkdir -p $PROJETO/app/src/main/java/$PACOTE/ui/theme
mkdir -p $PROJETO/app/src/main/java/$PACOTE/ui/screen
mkdir -p $PROJETO/app/src/main/java/$PACOTE/ui/components
mkdir -p $PROJETO/app/src/main/java/$PACOTE/viewmodel
mkdir -p $PROJETO/app/src/main/java/$PACOTE/service
mkdir -p $PROJETO/app/src/main/java/$PACOTE/util
mkdir -p $PROJETO/app/src/main/java/$PACOTE/data
mkdir -p $PROJETO/app/src/main/res/values
mkdir -p $PROJETO/app/src/main/res/drawable
mkdir -p $PROJETO/gradle/wrapper

# -------------------------------
# 2. Arquivos de build raiz
# -------------------------------
cat > $PROJETO/settings.gradle.kts << 'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Forma2"
include(":app")
EOF

cat > $PROJETO/build.gradle.kts << 'EOF'
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
EOF

cat > $PROJETO/gradle.properties << 'EOF'
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
EOF

# -------------------------------
# 3. Gradle wrapper properties
# -------------------------------
cat > $PROJETO/gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

# -------------------------------
# 4. App build.gradle.kts
# -------------------------------
cat > $PROJETO/app/build.gradle.kts << 'EOF'
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.forma2.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.forma2.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.coroutines.android)
    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
}
EOF

# -------------------------------
# 5. Catálogo de dependências
# -------------------------------
cat > $PROJETO/gradle/libs.versions.toml << 'EOF'
[versions]
agp = "8.2.2"
kotlin = "1.9.22"
compose-bom = "2024.06.00"
activity-compose = "1.9.0"
lifecycle = "2.7.0"
datastore = "1.1.1"
okhttp = "4.12.0"
coroutines = "1.8.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.13.1" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
EOF

# -------------------------------
# 6. AndroidManifest.xml
# -------------------------------
cat > $PROJETO/app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Forma2"
        android:usesCleartextTraffic="true">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Forma2">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.JavaProcessService"
            android:foregroundServiceType="specialUse"
            android:exported="false">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="java_process_runner" />
        </service>
    </application>
</manifest>
EOF

# -------------------------------
# 7. Recursos
# -------------------------------
cat > $PROJETO/app/src/main/res/values/strings.xml << 'EOF'
<resources>
    <string name="app_name">Forma 2</string>
</resources>
EOF

cat > $PROJETO/app/src/main/res/values/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Forma2" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
EOF

cat > $PROJETO/app/src/main/res/drawable/ic_notification.xml << 'EOF'
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M12,12m-10,0a10,10 0,1 1,20 0a10,10 0,1 1,-20 0"/>
</vector>
EOF

# -------------------------------
# 8. Código Kotlin
# -------------------------------

# MainActivity
cat > $PROJETO/app/src/main/java/$PACOTE/MainActivity.kt << 'EOF'
package com.forma2.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.forma2.app.ui.screen.HomeScreen
import com.forma2.app.ui.theme.Forma2Theme
import com.forma2.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Forma2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
EOF

# Theme
cat > $PROJETO/app/src/main/java/$PACOTE/ui/theme/Color.kt << 'EOF'
package com.forma2.app.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
EOF

cat > $PROJETO/app/src/main/java/$PACOTE/ui/theme/Type.kt << 'EOF'
package com.forma2.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
EOF

cat > $PROJETO/app/src/main/java/$PACOTE/ui/theme/Theme.kt << 'EOF'
package com.forma2.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun Forma2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
EOF

# HomeScreen
cat > $PROJETO/app/src/main/java/$PACOTE/ui/screen/HomeScreen.kt << 'EOF'
package com.forma2.app.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forma2.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.startProcess()
    }

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.pickDirectory(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forma 2 - JVM") },
                actions = {
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpar Logs")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Instalar Java
            Button(
                onClick = { viewModel.installJava() },
                enabled = !uiState.installing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.installing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instalando Java...")
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.jdkInstalled) "Java 17 Instalado" else "Instalar Java 17")
                }
            }

            if (uiState.installing) {
                LinearProgressIndicator(
                    progress = { uiState.installProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Escolher pasta
            Button(
                onClick = { dirPickerLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escolher Pasta com .jar")
            }

            uiState.selectedDirectoryUri?.let {
                Text(
                    text = "Pasta: ${it.lastPathSegment}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (uiState.jarFiles.isNotEmpty()) {
                Text(
                    "Arquivos .jar encontrados:",
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(top = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                ) {
                    items(uiState.jarFiles) { jar ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectJar(jar) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = jar.name ?: "unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.selectedJar?.uri == jar.uri) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selecionado",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.selectedJarPath.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Jar selecionado: ${uiState.selectedJar?.name}")
                        Text("Caminho: ${uiState.selectedJarPath}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Botões Iniciar/Parar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                viewModel.startProcess()
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            viewModel.startProcess()
                        }
                    },
                    enabled = !uiState.processRunning && uiState.selectedJarPath.isNotEmpty() && uiState.jdkInstalled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Iniciar")
                }
                Button(
                    onClick = { viewModel.stopProcess() },
                    enabled = uiState.processRunning,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Parar")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logs
            Text("Logs:", style = MaterialTheme.typography.subtitle1)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = uiState.logs.ifEmpty { "Nenhum log..." },
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
EOF

# ViewModel
cat > $PROJETO/app/src/main/java/$PACOTE/viewmodel/MainViewModel.kt << 'EOF'
package com.forma2.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.forma2.app.data.PreferencesManager
import com.forma2.app.service.JavaProcessService
import com.forma2.app.util.JavaInstaller
import com.forma2.app.util.LogManager
import com.forma2.app.util.ServiceState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)

    data class UiState(
        val jdkInstalled: Boolean = false,
        val jdkPath: String = "",
        val installProgress: Float = 0f,
        val installing: Boolean = false,
        val selectedDirectoryUri: Uri? = null,
        val jarFiles: List<DocumentFile> = emptyList(),
        val selectedJar: DocumentFile? = null,
        val selectedJarPath: String = "",
        val processRunning: Boolean = false,
        val logs: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
        observeLogs()
        observeServiceState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val jdkPath = prefs.jdkPath.first()
            val dirUriStr = prefs.selectedDirectoryUri.first()
            val dirUri = if (dirUriStr.isNotEmpty()) Uri.parse(dirUriStr) else null
            _uiState.update {
                it.copy(
                    jdkInstalled = jdkPath.isNotEmpty() && File(jdkPath).exists(),
                    jdkPath = jdkPath,
                    selectedDirectoryUri = dirUri
                )
            }
            if (dirUri != null) {
                refreshJarList(dirUri)
            }
        }
    }

    private fun observeLogs() {
        viewModelScope.launch {
            LogManager.logs.collect { log ->
                _uiState.update { it.copy(logs = log) }
            }
        }
    }

    private fun observeServiceState() {
        viewModelScope.launch {
            ServiceState.isRunning.collect { running ->
                _uiState.update { it.copy(processRunning = running) }
            }
        }
    }

    fun installJava() {
        viewModelScope.launch {
            if (_uiState.value.installing) return@launch
            _uiState.update { it.copy(installing = true, installProgress = 0f) }
            try {
                val jdkPath = JavaInstaller.downloadAndExtract(
                    context = getApplication(),
                    onProgress = { progress ->
                        _uiState.update { it.copy(installProgress = progress) }
                    }
                )
                if (jdkPath != null) {
                    prefs.saveJdkPath(jdkPath)
                    _uiState.update {
                        it.copy(
                            jdkInstalled = true,
                            jdkPath = jdkPath,
                            installing = false,
                            installProgress = 1f
                        )
                    }
                    LogManager.appendLog("Java 17 instalado em $jdkPath")
                } else {
                    _uiState.update { it.copy(installing = false) }
                    LogManager.appendLog("Falha na instalação do Java")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(installing = false) }
                LogManager.appendLog("Erro na instalação: ${e.message}")
            }
        }
    }

    fun pickDirectory(uri: Uri) {
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}

        viewModelScope.launch {
            prefs.saveSelectedDirectoryUri(uri.toString())
            _uiState.update { it.copy(selectedDirectoryUri = uri) }
            refreshJarList(uri)
        }
    }

    private fun refreshJarList(dirUri: Uri) {
        viewModelScope.launch {
            val docFile = DocumentFile.fromTreeUri(getApplication(), dirUri)
            val jarFiles = docFile?.listFiles()?.filter {
                it.isFile && it.name?.endsWith(".jar", true) == true
            } ?: emptyList()
            _uiState.update { it.copy(jarFiles = jarFiles) }
        }
    }

    fun selectJar(docFile: DocumentFile) {
        viewModelScope.launch {
            val cacheDir = File(getApplication().cacheDir, "jars").also { it.mkdirs() }
            val destFile = File(cacheDir, docFile.name ?: "app.jar")
            try {
                getApplication().contentResolver.openInputStream(docFile.uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val jarPath = destFile.absolutePath
                _uiState.update { it.copy(selectedJar = docFile, selectedJarPath = jarPath) }
                LogManager.appendLog("Jar copiado para $jarPath")
            } catch (e: Exception) {
                LogManager.appendLog("Erro ao copiar jar: ${e.message}")
            }
        }
    }

    fun startProcess() {
        val state = _uiState.value
        if (state.processRunning) return
        val jarPath = state.selectedJarPath
        val jdkPath = state.jdkPath
        if (jarPath.isNotEmpty() && jdkPath.isNotEmpty()) {
            val workingDir = File(jarPath).parent ?: getApplication().filesDir.absolutePath
            JavaProcessService.start(
                context = getApplication(),
                jarPath = jarPath,
                workingDir = workingDir,
                javaHome = jdkPath
            )
        }
    }

    fun stopProcess() {
        JavaProcessService.stop(getApplication())
    }

    fun clearLogs() {
        LogManager.clearLogs()
    }
}
EOF

# Serviço de foreground
cat > $PROJETO/app/src/main/java/$PACOTE/service/JavaProcessService.kt << 'EOF'
package com.forma2.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.forma2.app.MainActivity
import com.forma2.app.R
import com.forma2.app.util.LogManager
import com.forma2.app.util.ServiceState
import kotlinx.coroutines.*
import java.io.File

class JavaProcessService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var process: Process? = null

    companion object {
        const val EXTRA_JAR_PATH = "jar_path"
        const val EXTRA_WORKING_DIR = "working_dir"
        const val EXTRA_JAVA_HOME = "java_home"
        const val CHANNEL_ID = "java_process_channel"
        const val NOTIFICATION_ID = 101

        fun start(context: Context, jarPath: String, workingDir: String, javaHome: String) {
            val intent = Intent(context, JavaProcessService::class.java).apply {
                putExtra(EXTRA_JAR_PATH, jarPath)
                putExtra(EXTRA_WORKING_DIR, workingDir)
                putExtra(EXTRA_JAVA_HOME, javaHome)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, JavaProcessService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("Executando processo Java...")
        startForeground(NOTIFICATION_ID, notification)

        intent?.let {
            val jarPath = it.getStringExtra(EXTRA_JAR_PATH) ?: return START_NOT_STICKY
            val workingDir = it.getStringExtra(EXTRA_WORKING_DIR) ?: return START_NOT_STICKY
            val javaHome = it.getStringExtra(EXTRA_JAVA_HOME) ?: return START_NOT_STICKY
            runJava(jarPath, workingDir, javaHome)
        }

        return START_STICKY
    }

    private fun runJava(jarPath: String, workingDir: String, javaHome: String) {
        serviceScope.launch {
            ServiceState.setRunning(true)
            LogManager.appendLog("Iniciando processo Java...")
            LogManager.appendLog("JAR: $jarPath")
            LogManager.appendLog("Diretório: $workingDir")

            val javaBinary = File(javaHome, "bin/java").absolutePath
            val command = listOf(javaBinary, "-jar", jarPath)

            try {
                val processBuilder = ProcessBuilder(command)
                    .directory(File(workingDir))
                    .redirectErrorStream(true)
                process = processBuilder.start()
                val reader = process!!.inputStream.bufferedReader()

                reader.useLines { lines ->
                    lines.forEach { line ->
                        LogManager.appendLog(line)
                    }
                }

                val exitCode = process?.waitFor() ?: -1
                LogManager.appendLog("Processo encerrado com código: $exitCode")
            } catch (e: Exception) {
                LogManager.appendLog("Erro: ${e.message}")
            } finally {
                process = null
                ServiceState.setRunning(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        process?.destroy()
        super.onDestroy()
        ServiceState.setRunning(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Processo Java",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Forma 2")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
EOF

# Instalador do Java
cat > $PROJETO/app/src/main/java/$PACOTE/util/JavaInstaller.kt << 'EOF'
package com.forma2.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

object JavaInstaller {

    suspend fun downloadAndExtract(
        context: Context,
        onProgress: (Float) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        val jdkDir = File(context.filesDir, "jdk-17")
        if (jdkDir.exists() && File(jdkDir, "bin/java").exists()) {
            return@withContext jdkDir.absolutePath
        }

        val client = OkHttpClient.Builder()
            .followRedirects(true)
            .build()
        val apiUrl = "https://api.adoptium.net/v3/binary/latest/17/ga/linux/aarch64/jdk/hotspot/normal/eclipse?project=jdk"
        val request = Request.Builder().url(apiUrl).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Download falhou: ${response.code}")

        val body = response.body ?: throw Exception("Resposta vazia")
        val contentLength = body.contentLength()
        var downloadedBytes = 0L

        val tempFile = File(context.cacheDir, "jdk.tar.gz")
        body.byteStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8192)
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    output.write(buffer, 0, bytes)
                    downloadedBytes += bytes
                    if (contentLength > 0) {
                        onProgress(downloadedBytes.toFloat() / contentLength.toFloat())
                    }
                }
            }
        }

        // Extração
        jdkDir.mkdirs()
        tempFile.inputStream().use { fileStream ->
            GZIPInputStream(fileStream).use { gzStream ->
                TarArchiveInputStream(gzStream).use { tarInput ->
                    var entry = tarInput.nextTarEntry
                    while (entry != null) {
                        val entryFile = File(jdkDir, entry.name)
                        if (entry.isDirectory) {
                            entryFile.mkdirs()
                        } else {
                            entryFile.parentFile?.mkdirs()
                            entryFile.outputStream().use { out ->
                                tarInput.copyTo(out)
                            }
                            if (entry.name.contains("bin/java")) {
                                entryFile.setExecutable(true)
                            }
                        }
                        entry = tarInput.nextTarEntry
                    }
                }
            }
        }

        tempFile.delete()

        // O tar pode vir com diretório raiz, localizar java
        val javaBinary = jdkDir.walkTopDown().find { it.isFile && it.name == "java" }
        if (javaBinary != null) {
            javaBinary.setExecutable(true)
            javaBinary.parentFile?.parentFile?.absolutePath
        } else null
    }
}
EOF

# Gerenciadores de estado (singletons)
cat > $PROJETO/app/src/main/java/$PACOTE/util/LogManager.kt << 'EOF'
package com.forma2.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LogManager {
    private val _logs = MutableStateFlow("")
    val logs: StateFlow<String> = _logs.asStateFlow()

    fun appendLog(message: String) {
        _logs.value = _logs.value + message + "\n"
    }

    fun clearLogs() {
        _logs.value = ""
    }
}
EOF

cat > $PROJETO/app/src/main/java/$PACOTE/util/ServiceState.kt << 'EOF'
package com.forma2.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServiceState {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun setRunning(running: Boolean) {
        _isRunning.value = running
    }
}
EOF

# Preferences DataStore
cat > $PROJETO/app/src/main/java/$PACOTE/data/PreferencesManager.kt << 'EOF'
package com.forma2.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_JDK_PATH = stringPreferencesKey("jdk_path")
        val KEY_SELECTED_DIR_URI = stringPreferencesKey("selected_dir_uri")
    }

    val jdkPath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_JDK_PATH] ?: ""
    }

    val selectedDirectoryUri: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_DIR_URI] ?: ""
    }

    suspend fun saveJdkPath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_JDK_PATH] = path
        }
    }

    suspend fun saveSelectedDirectoryUri(uri: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_DIR_URI] = uri
        }
    }
}
EOF

echo ""
echo "✅ Projeto '$PROJETO' criado com sucesso!"
echo ""
echo "📌 Para compilar, abra a pasta '$PROJETO' no Android Studio ou execute:"
echo "   cd $PROJETO"
echo "   gradle wrapper --gradle-version 8.5   # (apenas uma vez)"
echo "   ./gradlew assembleDebug"
echo ""
echo "Depois instale o APK gerado em app/build/outputs/apk/debug/"