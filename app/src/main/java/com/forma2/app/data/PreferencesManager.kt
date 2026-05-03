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
