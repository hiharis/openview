package com.example.viewers.docx

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.OpenFileSession
import com.example.core.OpenFileSessionState
import com.example.ui.components.OpenViewTopAppBar
import com.example.ui.components.ViewerErrorView

@Composable
fun DocxViewerScreen(
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: DocxViewerViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val sessionState by OpenFileSession.sessionState.collectAsStateWithLifecycle()

  LaunchedEffect(sessionState) {
    when (val state = sessionState) {
      is OpenFileSessionState.Ready -> {
        viewModel.loadDocx(state.fileInfo.displayName, emptyList())
      }
      is OpenFileSessionState.Error -> {
        viewModel.setError(state.errorMessage)
      }
      else -> Unit
    }
  }

  val title = when (val state = uiState) {
    is DocxUiState.Content -> state.fileName
    else -> "DOCX Viewer"
  }

  Scaffold(
    topBar = {
      OpenViewTopAppBar(
        title = title,
        canNavigateBack = true,
        onNavigateUp = onNavigateUp,
      )
    },
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    when (val state = uiState) {
      is DocxUiState.Loading -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
        }
      }
      is DocxUiState.Content -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Ready for format implementation (Stage B: DOCX viewer)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
      is DocxUiState.Error -> {
        ViewerErrorView(
          errorMessage = state.message,
          onNavigateUp = onNavigateUp,
          modifier = Modifier.padding(innerPadding),
        )
      }
    }
  }
}
