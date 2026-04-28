package com.jarserver;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel para gerenciar os logs do servidor.
 * Utiliza LiveData para comunicação reativa entre Service e UI.
 */
public class LogViewModel extends ViewModel {
    private final MutableLiveData<String> logsLiveData = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isServerRunning = new MutableLiveData<>(false);
    private final MutableLiveData<String> selectedFolderPath = new MutableLiveData<>("");

    public LiveData<String> getLogs() {
        return logsLiveData;
    }

    public LiveData<Boolean> getIsServerRunning() {
        return isServerRunning;
    }

    public LiveData<String> getSelectedFolderPath() {
        return selectedFolderPath;
    }

    public void addLog(String message) {
        String currentLogs = logsLiveData.getValue() != null ? logsLiveData.getValue() : "";
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        String newLog = "[" + timestamp + "] " + message + "\n";
        logsLiveData.setValue(currentLogs + newLog);
    }

    public void clearLogs() {
        logsLiveData.setValue("");
    }

    public void setServerRunning(boolean running) {
        isServerRunning.setValue(running);
    }

    public void setSelectedFolderPath(String path) {
        selectedFolderPath.setValue(path);
    }
}
