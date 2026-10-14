package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core.FileType
import com.example.core.OpenFileSession
import com.example.core.OpenFileSessionState
import com.example.ui.about.AboutScreen
import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen
import com.example.viewers.code.CodeViewerScreen
import com.example.viewers.docx.DocxViewerScreen
import com.example.viewers.pdf.PdfViewerScreen
import com.example.viewers.text.TextViewerScreen
import com.example.viewers.unsupported.UnsupportedFileScreen

@Composable
fun OpenViewNavGraph(
  modifier: Modifier = Modifier,
  navController: NavHostController = rememberNavController(),
  startDestination: String = Screen.Home.route,
) {
  val sessionState by OpenFileSession.sessionState.collectAsStateWithLifecycle()

  // React to file session state changes (from SAF picker or external VIEW intents)
  LaunchedEffect(sessionState) {
    when (val state = sessionState) {
      is OpenFileSessionState.Ready -> {
        val targetRoute = when (state.fileInfo.fileType) {
          FileType.TEXT -> Screen.TextViewer.route
          FileType.CODE -> Screen.CodeViewer.route
          FileType.PDF -> Screen.PdfViewer.route
          FileType.DOCX -> Screen.DocxViewer.route
          FileType.LEGACY_DOC, FileType.UNSUPPORTED -> Screen.Unsupported.route
        }
        if (navController.currentDestination?.route != targetRoute) {
          navController.navigate(targetRoute)
        }
      }
      is OpenFileSessionState.Error -> {
        if (navController.currentDestination?.route != Screen.Unsupported.route) {
          navController.navigate(Screen.Unsupported.route)
        }
      }
      else -> Unit
    }
  }

  NavHost(
    navController = navController,
    startDestination = startDestination,
    modifier = modifier,
  ) {
    composable(route = Screen.Home.route) {
      HomeScreen(
        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
        onNavigateToAbout = { navController.navigate(Screen.About.route) },
      )
    }

    composable(route = Screen.Settings.route) {
      SettingsScreen(
        onNavigateUp = { navController.navigateUp() },
        onNavigateToAbout = { navController.navigate(Screen.About.route) },
      )
    }

    composable(route = Screen.About.route) {
      AboutScreen(
        onNavigateUp = { navController.navigateUp() },
      )
    }

    composable(route = Screen.TextViewer.route) {
      TextViewerScreen(
        onNavigateUp = {
          OpenFileSession.reset()
          navController.navigateUp()
        },
      )
    }

    composable(route = Screen.CodeViewer.route) {
      CodeViewerScreen(
        onNavigateUp = {
          OpenFileSession.reset()
          navController.navigateUp()
        },
      )
    }

    composable(route = Screen.PdfViewer.route) {
      PdfViewerScreen(
        onNavigateUp = {
          OpenFileSession.reset()
          navController.navigateUp()
        },
      )
    }

    composable(route = Screen.DocxViewer.route) {
      DocxViewerScreen(
        onNavigateUp = {
          OpenFileSession.reset()
          navController.navigateUp()
        },
      )
    }

    composable(route = Screen.Unsupported.route) {
      UnsupportedFileScreen(
        onNavigateUp = {
          OpenFileSession.reset()
          navController.navigateUp()
        },
      )
    }
  }
}
