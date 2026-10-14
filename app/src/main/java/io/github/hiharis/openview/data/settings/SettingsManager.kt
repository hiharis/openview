// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "openview_settings")

class SettingsManager(private val context: Context) {
  companion object {
    val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    val KEY_FONT_SIZE = intPreferencesKey("font_size_sp")
  }

  val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[KEY_THEME_MODE] ?: "SYSTEM"
  }

  val fontSizeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
    preferences[KEY_FONT_SIZE] ?: 14
  }

  suspend fun saveThemeMode(mode: String) {
    context.dataStore.edit { preferences ->
      preferences[KEY_THEME_MODE] = mode
    }
  }

  suspend fun saveFontSize(sizeSp: Int) {
    context.dataStore.edit { preferences ->
      preferences[KEY_FONT_SIZE] = sizeSp
    }
  }
}
