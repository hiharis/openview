// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.viewers.image

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hiharis.openview.R
import io.github.hiharis.openview.core.OpenFileSession
import io.github.hiharis.openview.core.OpenFileSessionState
import io.github.hiharis.openview.ui.components.OpenViewTopAppBar
import io.github.hiharis.openview.ui.theme.Dimens

@Composable
fun ImageViewerScreen(
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ImageViewerViewModel = viewModel(),
) {
  val context = LocalContext.current
  val sessionState by OpenFileSession.sessionState.collectAsStateWithLifecycle()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val fileInfo = (sessionState as? OpenFileSessionState.Ready)?.fileInfo
  val uri = fileInfo?.uri
  val fileName = fileInfo?.displayName ?: stringResource(id = R.string.unsupported_default_filename)

  LaunchedEffect(uri) {
    if (uri != null) {
      viewModel.loadImage(context, uri, fileName)
    }
  }

  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  BackHandler {
    if (scale > 1.1f) {
      scale = 1f
      offset = Offset.Zero
    } else {
      onNavigateUp()
    }
  }

  Scaffold(
    topBar = {
      OpenViewTopAppBar(
        title = fileName,
        canNavigateBack = true,
        onNavigateUp = onNavigateUp,
      )
    },
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.Center,
    ) {
      when (val state = uiState) {
        is ImageUiState.Loading -> {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.testTag("image_loading_indicator"))
          }
        }
        is ImageUiState.Error -> {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Dimens.spaceMedium),
          ) {
            Text(
              text = stringResource(id = R.string.viewer_error_image_invalid),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(Dimens.spaceSmall))
            Text(
              text = state.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        is ImageUiState.Content -> {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .testTag("image_viewer_content")
              .pointerInput(Unit) {
                detectTapGestures(
                  onDoubleTap = {
                    if (scale > 1.1f) {
                      scale = 1f
                      offset = Offset.Zero
                    } else {
                      scale = 2.5f
                      offset = Offset.Zero
                    }
                  },
                )
              }
              .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                  val newScale = (scale * zoom).coerceIn(1f, 6f)
                  if (newScale <= 1f) {
                    scale = 1f
                    offset = Offset.Zero
                  } else {
                    scale = newScale
                    offset += pan
                  }
                }
              },
            contentAlignment = Alignment.Center,
          ) {
            Image(
              bitmap = state.bitmap.asImageBitmap(),
              contentDescription = fileName,
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                  scaleX = scale,
                  scaleY = scale,
                  translationX = offset.x,
                  translationY = offset.y,
                ),
            )
          }
        }
      }
    }
  }
}
