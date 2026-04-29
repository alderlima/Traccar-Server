package com.jarserver;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.jarserver.service.ServerService;

/**
 * MainActivity - Interface principal do aplicativo.
 * Gerencia a seleção de pasta, inicialização do servidor e exibição de logs.
 */
public class MainActivity extends AppCompatActivity implements ServerService.ServerServiceListener {
    private static final String PREFS_NAME = "jar_server_prefs";
    private static final String PREF_FOLDER_URI = "folder_uri";
    private static final String PREF_JAR_NAME = "jar_name";

    private LogViewModel viewModel;
    private SharedPreferences preferences;

    // UI Components
    private Button selectFolderBtn;
    private Button startServerBtn;
    private Button stopServerBtn;
    private Button clearLogsBtn;
    private TextView statusText;
    private TextView folderPathText;
    private TextView logsText;
    private ScrollView logsScrollView;

    // Launcher para seleção de pasta
    private ActivityResultLauncher<Uri> folderPickerLauncher;

    // Variáveis de estado
    private String selectedFolderUri = "";
    private String selectedJarName = "";
    private boolean isServerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar ViewModel
        viewModel = new ViewModelProvider(this).get(LogViewModel.class);

        // Inicializar SharedPreferences
        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Inicializar componentes de UI
        initializeUI();

        // Configurar listeners
        setupListeners();

        // Configurar folder picker launcher
        setupFolderPickerLauncher();

        // Restaurar estado anterior
        restorePreviousState();

        // Solicitar permissões necessárias
        requestNecessaryPermissions();

        // Observar mudanças no ViewModel
        observeViewModel();
    }

    private void initializeUI() {
        selectFolderBtn = findViewById(R.id.selectFolderBtn);
        startServerBtn = findViewById(R.id.startServerBtn);
        stopServerBtn = findViewById(R.id.stopServerBtn);
        clearLogsBtn = findViewById(R.id.clearLogsBtn);
        statusText = findViewById(R.id.statusText);
        folderPathText = findViewById(R.id.folderPathText);
        logsText = findViewById(R.id.logsText);
        logsScrollView = findViewById(R.id.logsText).getParent().getParent() instanceof ScrollView ?
                (ScrollView) findViewById(R.id.logsText).getParent().getParent() : null;
    }

    private void setupListeners() {
        selectFolderBtn.setOnClickListener(v -> openFolderPicker());
        startServerBtn.setOnClickListener(v -> startServer());
        stopServerBtn.setOnClickListener(v -> stopServer());
        clearLogsBtn.setOnClickListener(v -> clearLogs());
    }

    private void setupFolderPickerLauncher() {
        folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                uri -> {
                    if (uri != null) {
                        selectedFolderUri = uri.toString();
                        preferences.edit().putString(PREF_FOLDER_URI, selectedFolderUri).apply();
                        updateFolderDisplay();
                        viewModel.addLog("Pasta selecionada: " + uri.getPath());
                    }
                }
        );
    }

    private void openFolderPicker() {
        folderPickerLauncher.launch(null);
    }

    private void startServer() {
        if (selectedFolderUri.isEmpty()) {
            Toast.makeText(this, R.string.error_no_folder, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedJarName.isEmpty()) {
            showJarFileDialog();
            return;
        }

        isServerRunning = true;
        startServerBtn.setEnabled(false);
        stopServerBtn.setEnabled(true);
        statusText.setText(R.string.status_running);

        Intent serviceIntent = ServerService.getStartIntent(
                this, selectedFolderUri, selectedJarName
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        viewModel.addLog("Iniciando servidor...");
        viewModel.setServerRunning(true);
    }

    private void stopServer() {
        isServerRunning = false;
        startServerBtn.setEnabled(true);
        stopServerBtn.setEnabled(false);
        statusText.setText(R.string.status_stopped);

        Intent serviceIntent = ServerService.getStopIntent(this);
        stopService(serviceIntent);

        viewModel.addLog("Parando servidor...");
        viewModel.setServerRunning(false);
    }

    private void clearLogs() {
        logsText.setText("");
        viewModel.clearLogs();
    }

    private void showJarFileDialog() {
        // Diálogo simples para selecionar arquivo JAR
        // Por enquanto, usar o primeiro .jar encontrado
        viewModel.addLog("Procurando por arquivos .jar...");
        // Implementação simplificada - em produção, usar um diálogo real
        selectedJarName = "";
    }

    private void updateFolderDisplay() {
        if (!selectedFolderUri.isEmpty()) {
            folderPathText.setText(getString(R.string.folder_selected));
        } else {
            folderPathText.setText(getString(R.string.no_folder_selected));
        }
    }

    private void restorePreviousState() {
        selectedFolderUri = preferences.getString(PREF_FOLDER_URI, "");
        selectedJarName = preferences.getString(PREF_JAR_NAME, "");
        updateFolderDisplay();
    }

    private void requestNecessaryPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void observeViewModel() {
        viewModel.getLogs().observe(this, logs -> {
            logsText.setText(logs);
            // Auto-scroll para o final
            if (logsScrollView != null) {
                logsScrollView.post(() -> logsScrollView.fullScroll(ScrollView.FOCUS_DOWN));
            }
        });

        viewModel.getIsServerRunning().observe(this, running -> {
            isServerRunning = running;
            startServerBtn.setEnabled(!running);
            stopServerBtn.setEnabled(running);
            statusText.setText(running ? R.string.status_running : R.string.status_idle);
        });

        viewModel.getSelectedFolderPath().observe(this, path -> {
            if (!path.isEmpty()) {
                folderPathText.setText(path);
            }
        });
    }

    @Override
    public void onLogMessage(String message) {
        runOnUiThread(() -> viewModel.addLog(message));
    }

    @Override
    public void onServerStarted() {
        runOnUiThread(() -> {
            viewModel.setServerRunning(true);
            Toast.makeText(this, "Servidor iniciado com sucesso", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onServerStopped() {
        runOnUiThread(() -> {
            viewModel.setServerRunning(false);
            Toast.makeText(this, "Servidor parado", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            viewModel.addLog("ERRO: " + error);
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Não parar o serviço ao destruir a activity
        // O serviço continua rodando em background
    }
}
