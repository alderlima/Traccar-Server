package com.example.traccarserver

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.traccarserver.ui.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") { MainScreen(navController, viewModel) }
        composable("logs") { LogsScreen(navController, viewModel) }
        composable("web") { WebViewScreen(navController) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val directoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Resolve o URI do SAF para o caminho físico real
            val manager = com.example.traccarserver.installer.EnvironmentManager(context)
            val realPath = manager.resolveUriToPath(it)
            if (realPath != null) {
                viewModel.updateTraccarPath(realPath)
            } else {
                viewModel.updateTraccarPath(it.path ?: "")
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Traccar Local Server") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(viewModel.isTraccarReady.value, viewModel.isServerRunning.value, viewModel.traccarPath.value)

            Button(
                onClick = { directoryLauncher.launch(null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isServerRunning.value
            ) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Selecionar Pasta do Traccar")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.startServer() },
                    modifier = Modifier.weight(1f),
                    enabled = viewModel.isTraccarReady.value && !viewModel.isServerRunning.value,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("Iniciar")
                }
                Button(
                    onClick = { viewModel.stopServer() },
                    modifier = Modifier.weight(1f),
                    enabled = viewModel.isServerRunning.value,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Text("Parar")
                }
            }

            Divider()

            MenuButton(Icons.Default.List, "Ver Logs") { navController.navigate("logs") }
            MenuButton(Icons.Default.Public, "Abrir Painel Web") { navController.navigate("web") }
        }
    }
}

@Composable
fun StatusCard(ready: Boolean, running: Boolean, path: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (running) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Status do Sistema", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            StatusRow("Pasta:", path, MaterialTheme.colorScheme.onSurfaceVariant)
            StatusRow("Traccar:", if (ready) "Encontrado" else "Não encontrado", if (ready) Color(0xFF4CAF50) else Color(0xFFF44336))
            StatusRow("Servidor:", if (running) "Ativo" else "Inativo", if (running) Color(0xFF4CAF50) else Color(0xFFF44336))
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, color: Color) {
    Row {
        Text(label, modifier = Modifier.width(80.dp), fontSize = 14.sp)
        Text(value, color = color, style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp)
    }
}

@Composable
fun MenuButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(navController: NavHostController, viewModel: MainViewModel) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(viewModel.serverLogs.size) {
        if (viewModel.serverLogs.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.serverLogs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs do Servidor") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
                .padding(8.dp)
        ) {
            items(viewModel.serverLogs) { log ->
                Text(
                    text = log,
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Painel Traccar") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.padding(padding).fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    loadUrl("http://127.0.0.1:8082")
                }
            }
        )
    }
}
