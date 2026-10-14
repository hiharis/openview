// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.hiharis.openview.data.settings.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
}

enum class FontSizePreference(val title: String, val sizeSp: Int) {
  SMALL("Small (12sp)", 12),
  MEDIUM("Medium (14sp)", 14),
  LARGE("Large (18sp)", 18),
}

data class SettingsUiState(
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val fontSize: FontSizePreference = FontSizePreference.MEDIUM,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
  private val settingsManager = SettingsManager(application)

  val uiState: StateFlow<SettingsUiState> = combine(
    settingsManager.themeModeFlow,
    settingsManager.fontSizeFlow,
  ) { themeStr, fontSp ->
    val mode = try {
      AppThemeMode.valueOf(themeStr)
    } catch (_: Exception) {
      AppThemeMode.SYSTEM
    }
    val fontSize = FontSizePreference.values().find { it.sizeSp == fontSp } ?: FontSizePreference.MEDIUM
    SettingsUiState(themeMode = mode, fontSize = fontSize)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = SettingsUiState(),
  )

  fun setThemeMode(mode: AppThemeMode) {
    viewModelScope.launch {
      settingsManager.saveThemeMode(mode.name)
    }
  }

  fun setFontSize(fontSize: FontSizePreference) {
    viewModelScope.launch {
      settingsManager.saveFontSize(fontSize.sizeSp)
    }
  }
}
