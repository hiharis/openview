// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import io.github.hiharis.openview.core.OpenFileSession
import io.github.hiharis.openview.data.settings.SettingsManager
import io.github.hiharis.openview.data.update.UpdateCheckResult
import io.github.hiharis.openview.data.update.UpdateDialog
import io.github.hiharis.openview.data.update.UpdateInfo
import io.github.hiharis.openview.data.update.UpdateManager
import io.github.hiharis.openview.navigation.OpenViewNavGraph
import io.github.hiharis.openview.ui.settings.AppThemeMode
import io.github.hiharis.openview.ui.theme.OpenViewTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
  private val settingsManager by lazy { SettingsManager(applicationContext) }
  private val autoUpdateInfo = mutableStateOf<UpdateInfo?>(null)

  companion object {
    private const val TEN_DAYS_MILLIS = 10L * 24 * 60 * 60 * 1000L
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Clean up leftover updates from previous sessions
    UpdateManager.cleanOldUpdates(applicationContext)

    // Handle incoming VIEW intent on cold start
    handleIncomingIntent(intent)

    // Background automatic check for updates (at most once every 10 days)
    checkAutomaticUpdates()

    setContent {
      val themePref by settingsManager.themeModeFlow.collectAsStateWithLifecycle(initialValue = "DARK")
      val isSystemDark = isSystemInDarkTheme()
      val selectedTheme = when (themePref) {
        AppThemeMode.LIGHT.name -> AppThemeMode.LIGHT
        AppThemeMode.DARK.name -> AppThemeMode.DARK
        AppThemeMode.RED.name -> AppThemeMode.RED
        AppThemeMode.FOREST.name -> AppThemeMode.FOREST
        else -> if (isSystemDark) AppThemeMode.DARK else AppThemeMode.LIGHT
      }

      OpenViewTheme(themeMode = selectedTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
          OpenViewNavGraph()

          // Show update dialog if available
          autoUpdateInfo.value?.let { info ->
            UpdateDialog(
              info = info,
              onDismiss = {
                autoUpdateInfo.value = null
                lifecycleScope.launch(Dispatchers.IO) {
                  settingsManager.saveLastUpdateCheckMillis(System.currentTimeMillis())
                }
              },
            )
          }
        }
      }
    }
  }

  private fun checkAutomaticUpdates() {
    lifecycleScope.launch(Dispatchers.IO) {
      try {
        val lastCheck = settingsManager.getLastUpdateCheckMillis()
        val now = System.currentTimeMillis()
        if (now - lastCheck >= TEN_DAYS_MILLIS) {
          val currentVersion = UpdateManager.getInstalledVersionName(applicationContext)
          val result = UpdateManager.checkForUpdates(currentVersion)
          when (result) {
            is UpdateCheckResult.UpdateAvailable -> {
              settingsManager.saveLastUpdateCheckMillis(now)
              withContext(Dispatchers.Main) {
                autoUpdateInfo.value = result.info
              }
            }
            is UpdateCheckResult.UpToDate -> {
              settingsManager.saveLastUpdateCheckMillis(now)
            }
            is UpdateCheckResult.Error -> {
              // Silently ignore network/parsing errors; do not save timestamp so it can retry next launch
            }
          }
        }
      } catch (_: Exception) {
        // Silently swallow all background update check exceptions
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    // Handle incoming VIEW intent when app is already open
    handleIncomingIntent(intent)
  }

  private fun handleIncomingIntent(intent: Intent?) {
    if (intent == null) return
    val action = intent.action
    val uri = intent.data

    if (action == Intent.ACTION_VIEW && uri != null) {
      Log.d("FileOpen", "VIEW intent received in MainActivity: scheme=${uri.scheme}, authority=${uri.authority}")
      OpenFileSession.openUri(this, uri)
    }
  }
}
