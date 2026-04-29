package com.jarserver.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.documentfile.provider.DocumentFile;

import com.jarserver.MainActivity;
import com.jarserver.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground Service que executa um arquivo .jar em background.
 * Mantém o servidor rodando mesmo quando o app é minimizado.
 */
public class ServerService extends Service {
    private static final String CHANNEL_ID = "jar_server_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String ACTION_START = "com.jarserver.START";
    private static final String ACTION_STOP = "com.jarserver.STOP";
    private static final String EXTRA_FOLDER_URI = "folder_uri";
    private static final String EXTRA_JAR_NAME = "jar_name";

    private Process jarProcess;
    private ExecutorService executorService;
    private ServerServiceListener listener;

    public interface ServerServiceListener {
        void onLogMessage(String message);
        void onServerStarted();
        void onServerStopped();
        void onError(String error);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newFixedThreadPool(2);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                startJarServer(intent);
            } else if (ACTION_STOP.equals(action)) {
                stopJarServer();
            }
        }
        return START_STICKY;
    }

    private void startJarServer(Intent intent) {
        String folderUri = intent.getStringExtra(EXTRA_FOLDER_URI);
        String jarName = intent.getStringExtra(EXTRA_JAR_NAME);

        if (folderUri == null || jarName == null) {
            notifyError("Pasta ou arquivo JAR não especificado");
            return;
        }

        executorService.execute(() -> {
            try {
                Uri uri = Uri.parse(folderUri);
                DocumentFile folder = DocumentFile.fromTreeUri(this, uri);

                if (folder == null || !folder.exists()) {
                    notifyError("Pasta não encontrada");
                    return;
                }

                // Procurar pelo arquivo .jar na pasta
                DocumentFile jarFile = findJarFile(folder, jarName);
                if (jarFile == null) {
                    notifyError("Arquivo .jar não encontrado: " + jarName);
                    return;
                }

                notifyLog("Iniciando servidor com: " + jarFile.getName());

                // Copiar o arquivo .jar para cache (necessário para executar)
                File cacheJar = new File(getCacheDir(), jarFile.getName());
                copyUriToFile(jarFile.getUri(), cacheJar);

                if (!cacheJar.exists()) {
                    notifyError("Falha ao copiar arquivo JAR");
                    return;
                }

                // Iniciar o processo Java
                startJavaProcess(cacheJar);

            } catch (Exception e) {
                notifyError("Erro ao iniciar servidor: " + e.getMessage());
            }
        });
    }

    private DocumentFile findJarFile(DocumentFile folder, String jarName) {
        for (DocumentFile file : folder.listFiles()) {
            if (file.isFile() && file.getName() != null && file.getName().endsWith(".jar")) {
                if (jarName.isEmpty() || file.getName().equals(jarName)) {
                    return file;
                }
            }
        }
        return null;
    }

    private void copyUriToFile(Uri uri, File destination) throws IOException {
        try (java.io.InputStream input = getContentResolver().openInputStream(uri);
             java.io.OutputStream output = new java.io.FileOutputStream(destination)) {
            if (input != null) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
            }
        }
    }

    private void startJavaProcess(File jarFile) throws IOException {
        String javaPath = System.getProperty("java.home");
        if (javaPath == null) {
            notifyError("Java não encontrado no sistema");
            return;
        }

        ProcessBuilder pb = new ProcessBuilder(
                javaPath + "/bin/java",
                "-Xmx256m",
                "-jar",
                jarFile.getAbsolutePath()
        );

        pb.redirectErrorStream(true);
        jarProcess = pb.start();

        notifyLog("Servidor iniciado com PID: " + jarProcess.pid());
        notifyServerStarted();

        // Ler output do processo
        readProcessOutput();
    }

    private void readProcessOutput() {
        executorService.execute(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(jarProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null && jarProcess != null && jarProcess.isAlive()) {
                    notifyLog(line);
                }
            } catch (IOException e) {
                if (jarProcess != null && jarProcess.isAlive()) {
                    notifyError("Erro ao ler output: " + e.getMessage());
                }
            }
        });
    }

    private void stopJarServer() {
        if (jarProcess != null && jarProcess.isAlive()) {
            try {
                jarProcess.destroy();
                notifyLog("Servidor parado");
                notifyServerStopped();
            } catch (Exception e) {
                notifyError("Erro ao parar servidor: " + e.getMessage());
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "JAR Server",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Notificações do servidor JAR");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.server_notification_title))
                .setContentText(getString(R.string.server_notification_text))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void notifyLog(String message) {
        if (listener != null) {
            listener.onLogMessage(message);
        }
    }

    private void notifyError(String error) {
        if (listener != null) {
            listener.onError(error);
        }
    }

    private void notifyServerStarted() {
        showNotification();
        if (listener != null) {
            listener.onServerStarted();
        }
    }

    private void notifyServerStopped() {
        if (listener != null) {
            listener.onServerStopped();
        }
    }

    public void setListener(ServerServiceListener listener) {
        this.listener = listener;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopJarServer();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    public static Intent getStartIntent(Context context, String folderUri, String jarName) {
        Intent intent = new Intent(context, ServerService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_FOLDER_URI, folderUri);
        intent.putExtra(EXTRA_JAR_NAME, jarName);
        return intent;
    }

    public static Intent getStopIntent(Context context) {
        Intent intent = new Intent(context, ServerService.class);
        intent.setAction(ACTION_STOP);
        return intent;
    }
}
