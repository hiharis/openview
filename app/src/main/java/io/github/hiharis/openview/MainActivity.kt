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
import io.github.hiharis.openview.core.OpenFileSession
import io.github.hiharis.openview.data.settings.SettingsManager
import io.github.hiharis.openview.navigation.OpenViewNavGraph
import io.github.hiharis.openview.ui.settings.AppThemeMode
import io.github.hiharis.openview.ui.theme.OpenViewTheme

class MainActivity : ComponentActivity() {
  private val settingsManager by lazy { SettingsManager(applicationContext) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Handle incoming VIEW intent on cold start
    handleIncomingIntent(intent)

    setContent {
      val themePref by settingsManager.themeModeFlow.collectAsStateWithLifecycle(initialValue = "SYSTEM")
      val isSystemDark = isSystemInDarkTheme()
      val darkTheme = when (themePref) {
        AppThemeMode.LIGHT.name -> false
        AppThemeMode.DARK.name -> true
        else -> isSystemDark
      }

      OpenViewTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
          OpenViewNavGraph()
        }
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
