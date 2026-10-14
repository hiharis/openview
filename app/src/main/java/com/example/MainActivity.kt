package com.example

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
import com.example.core.OpenFileSession
import com.example.data.settings.SettingsManager
import com.example.navigation.OpenViewNavGraph
import com.example.ui.settings.AppThemeMode
import com.example.ui.theme.OpenViewTheme

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
