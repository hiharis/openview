// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.viewers.pdf

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hiharis.openview.R
import io.github.hiharis.openview.core.OpenFileSession
import io.github.hiharis.openview.core.OpenFileSessionState
import io.github.hiharis.openview.ui.components.DraggableFastScrollBar
import io.github.hiharis.openview.ui.components.OpenViewTopAppBar
import io.github.hiharis.openview.ui.components.ViewerErrorView
import io.github.hiharis.openview.ui.theme.Dimens
import kotlinx.coroutines.launch

@Composable
fun PdfViewerScreen(
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PdfViewerViewModel = viewModel(),
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val sessionState by OpenFileSession.sessionState.collectAsStateWithLifecycle()
  val pageBitmaps by viewModel.pageBitmaps.collectAsStateWithLifecycle()

  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  var showJumpDialog by remember { mutableStateOf(false) }

  // Inverted color filter for night mode
  val invertColorFilter = remember {
    ColorFilter.colorMatrix(
      ColorMatrix(
        floatArrayOf(
          -1f, 0f, 0f, 0f, 255f,
          0f, -1f, 0f, 0f, 255f,
          0f, 0f, -1f, 0f, 255f,
          0f, 0f, 0f, 1f, 0f,
        ),
      ),
    )
  }

  // Load PDF on ready session
  LaunchedEffect(sessionState) {
    when (val state = sessionState) {
      is OpenFileSessionState.Ready -> {
        viewModel.loadPdf(
          context = context,
          uri = state.fileInfo.uri,
          fileName = state.fileInfo.displayName,
        )
      }
      is OpenFileSessionState.Error -> {
        viewModel.setError(state.errorMessage)
      }
      else -> Unit
    }
  }

  // Update current page when scrolled
  val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
  LaunchedEffect(firstVisibleIndex) {
    viewModel.updateCurrentPage(firstVisibleIndex + 1)
  }

  val title = when (val state = uiState) {
    is PdfUiState.Content -> state.fileName
    else -> stringResource(id = R.string.viewer_title_pdf)
  }

  val isNightModeActive = (uiState as? PdfUiState.Content)?.isNightMode == true

  Scaffold(
    topBar = {
      OpenViewTopAppBar(
        title = title,
        canNavigateBack = true,
        onNavigateUp = onNavigateUp,
        actions = {
          if (uiState is PdfUiState.Content) {
            val contentState = uiState as PdfUiState.Content

            // Night Mode Toggle (Invert Colors)
            IconButton(
              onClick = { viewModel.toggleNightMode() },
              modifier = Modifier
                .size(Dimens.minTouchTarget)
                .testTag("pdf_action_night_mode"),
            ) {
              Icon(
                imageVector = if (contentState.isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = stringResource(id = R.string.action_night_mode),
                tint = if (contentState.isNightMode) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onBackground
                },
                modifier = Modifier.size(Dimens.iconSizeMedium),
              )
            }

            // Fit Mode Toggle (Fit Width vs Fit Page)
            IconButton(
              onClick = { viewModel.toggleFitMode() },
              modifier = Modifier
                .size(Dimens.minTouchTarget)
                .testTag("pdf_action_fit_mode"),
            ) {
              Icon(
                imageVector = Icons.Default.FitScreen,
                contentDescription = stringResource(id = R.string.action_fit_mode),
                tint = if (contentState.fitMode == PdfFitMode.FIT_PAGE) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onBackground
                },
                modifier = Modifier.size(Dimens.iconSizeMedium),
              )
            }

            // Jump to page dialog
            IconButton(
              onClick = { showJumpDialog = true },
              modifier = Modifier
                .size(Dimens.minTouchTarget)
                .testTag("pdf_action_jump"),
            ) {
              Icon(
                imageVector = Icons.Default.FormatListNumbered,
                contentDescription = stringResource(id = R.string.action_jump_to_page),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(Dimens.iconSizeMedium),
              )
            }
          }
        },
      )
    },
    modifier = modifier.fillMaxSize(),
    containerColor = if (isNightModeActive) Color(0xFF121212) else MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      when (val state = uiState) {
        is PdfUiState.Loading -> {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(Dimens.heroIconSize),
            )
          }
        }

        is PdfUiState.Content -> {
          BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidthPx = with(density) { maxWidth.toPx().toInt() }
            val viewportHeightDp = maxHeight - Dimens.spaceMedium

            LazyColumn(
              state = listState,
              modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                  detectTapGestures(
                    onDoubleTap = {
                      viewModel.toggleDoubleTapZoom()
                    },
                  )
                }
                .pointerInput(Unit) {
                  detectTransformGestures { _, _, zoom, _ ->
                    viewModel.setZoomScale(state.zoomScale * zoom)
                  }
                }
                .graphicsLayer(
                  scaleX = state.zoomScale,
                  scaleY = state.zoomScale,
                  transformOrigin = TransformOrigin(0.5f, 0f),
                )
                .padding(vertical = Dimens.spaceSmall),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              items(count = state.totalPages, key = { it }) { pageIndex ->
                PdfPageItem(
                  pageIndex = pageIndex,
                  bitmap = pageBitmaps[pageIndex],
                  targetWidthPx = screenWidthPx,
                  fitMode = state.fitMode,
                  viewportHeightDp = viewportHeightDp,
                  colorFilter = if (state.isNightMode) invertColorFilter else null,
                  isNightMode = state.isNightMode,
                  onRequestBitmap = { viewModel.requestPageBitmap(pageIndex, screenWidthPx) },
                )
              }
            }

            // Draggable Fast Scrollbar on right edge
            DraggableFastScrollBar(
              listState = listState,
              totalItems = state.totalPages,
              modifier = Modifier.align(Alignment.CenterEnd),
            )

            // Floating Page Counter Pill
            Surface(
              shape = RoundedCornerShape(Dimens.cornerPill),
              color = if (state.isNightMode) Color(0xFF2C2C2C) else MaterialTheme.colorScheme.surfaceVariant,
              shadowElevation = Dimens.elevationLow,
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Dimens.spaceMedium),
            ) {
              Text(
                text = stringResource(
                  id = R.string.pdf_page_counter,
                  state.currentPage.coerceIn(1, state.totalPages),
                  state.totalPages,
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (state.isNightMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                  horizontal = Dimens.spaceMedium,
                  vertical = Dimens.pageIndicatorPadding,
                ),
              )
            }
          }

          // Jump to Page Dialog
          if (showJumpDialog) {
            JumpToPageDialog(
              totalPages = state.totalPages,
              currentPage = state.currentPage,
              onDismiss = { showJumpDialog = false },
              onPageSelected = { targetPage ->
                showJumpDialog = false
                coroutineScope.launch {
                  listState.animateScrollToItem(targetPage - 1)
                }
              },
            )
          }
        }

        is PdfUiState.Error -> {
          ViewerErrorView(
            errorMessage = state.message,
            onNavigateUp = onNavigateUp,
          )
        }
      }
    }
  }
}

@Composable
private fun PdfPageItem(
  pageIndex: Int,
  bitmap: Bitmap?,
  targetWidthPx: Int,
  fitMode: PdfFitMode,
  viewportHeightDp: Dp,
  colorFilter: ColorFilter?,
  isNightMode: Boolean,
  onRequestBitmap: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LaunchedEffect(pageIndex) {
    if (bitmap == null) {
      onRequestBitmap()
    }
  }

  val cardBg = if (isNightMode) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surface

  val itemModifier = when (fitMode) {
    PdfFitMode.FIT_WIDTH -> {
      modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.spaceSmall, vertical = Dimens.spaceSmall)
    }
    PdfFitMode.FIT_PAGE -> {
      val aspect = if (bitmap != null) {
        bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
      } else {
        1f / 1.414f
      }
      modifier
        .height(viewportHeightDp)
        .aspectRatio(aspect)
        .padding(horizontal = Dimens.spaceSmall, vertical = Dimens.spaceSmall)
    }
  }

  Card(
    modifier = itemModifier,
    shape = RoundedCornerShape(Dimens.cornerMedium),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLow),
  ) {
    if (bitmap != null) {
      val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = if (fitMode == PdfFitMode.FIT_WIDTH) {
          Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
        } else {
          Modifier.fillMaxSize()
        },
        contentScale = if (fitMode == PdfFitMode.FIT_WIDTH) ContentScale.FillWidth else ContentScale.Fit,
        colorFilter = colorFilter,
      )
    } else {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f / 1.414f)
          .background(if (isNightMode) Color(0xFF262626) else MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(
          modifier = Modifier.size(Dimens.iconSizeLarge),
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

@Composable
private fun JumpToPageDialog(
  totalPages: Int,
  currentPage: Int,
  onDismiss: () -> Unit,
  onPageSelected: (Int) -> Unit,
) {
  var pageInputText by remember { mutableStateOf(currentPage.toString()) }
  val pageNum = pageInputText.toIntOrNull()
  val isValid = pageNum != null && pageNum in 1..totalPages

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(id = R.string.dialog_jump_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column {
        OutlinedTextField(
          value = pageInputText,
          onValueChange = { pageInputText = it.filter { ch -> ch.isDigit() } },
          label = {
            Text(stringResource(id = R.string.dialog_jump_hint, totalPages))
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          keyboardActions = KeyboardActions(
            onDone = {
              if (isValid && pageNum != null) {
                onPageSelected(pageNum)
              }
            },
          ),
          singleLine = true,
          isError = pageInputText.isNotEmpty() && !isValid,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_jump_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (isValid && pageNum != null) {
            onPageSelected(pageNum)
          }
        },
        enabled = isValid,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.testTag("pdf_jump_confirm"),
      ) {
        Text(stringResource(id = R.string.dialog_jump_go))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("pdf_jump_cancel"),
      ) {
        Text(stringResource(id = R.string.dialog_cancel))
      }
    },
    shape = RoundedCornerShape(Dimens.cornerLarge),
    containerColor = MaterialTheme.colorScheme.surface,
  )
}
