// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.viewers.pdf

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hiharis.openview.R
import io.github.hiharis.openview.core.OpenFileSession
import io.github.hiharis.openview.core.OpenFileSessionState
import io.github.hiharis.openview.ui.components.DraggableFastScrollBar
import io.github.hiharis.openview.ui.components.OpenViewTopAppBar
import io.github.hiharis.openview.ui.components.ViewerErrorView
import io.github.hiharis.openview.ui.components.pinchZoom
import io.github.hiharis.openview.ui.theme.Dimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PdfViewerScreen(
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PdfViewerViewModel = viewModel(),
) {
  val context = LocalContext.current
  val sessionState by OpenFileSession.sessionState.collectAsStateWithLifecycle()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val pageBitmaps by viewModel.pageBitmaps.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val density = LocalDensity.current

  var showJumpDialog by remember { mutableStateOf(false) }

  LaunchedEffect(sessionState) {
    if (sessionState is OpenFileSessionState.Ready) {
      val ready = sessionState as OpenFileSessionState.Ready
      viewModel.loadPdf(context, ready.fileInfo.uri, ready.fileInfo.displayName)
    }
  }

  // Update current page in ViewModel based on first visible item in LazyColumn
  val visiblePage by remember {
    derivedStateOf {
      listState.firstVisibleItemIndex + 1
    }
  }

  LaunchedEffect(visiblePage) {
    viewModel.updateCurrentPage(visiblePage)
  }

  val isNightModeActive = (uiState as? PdfUiState.Content)?.isNightMode == true

  // Invert color matrix for night mode PDF rendering
  val invertColorFilter = remember {
    val matrix = ColorMatrix(
      floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
      ),
    )
    ColorFilter.colorMatrix(matrix)
  }

  Scaffold(
    topBar = {
      OpenViewTopAppBar(
        title = when (val s = uiState) {
          is PdfUiState.Content -> s.fileName
          else -> stringResource(id = R.string.viewer_title_pdf)
        },
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
            val viewportWidth = maxWidth
            val screenWidthPx = with(density) { maxWidth.toPx().toInt() }
            val viewportHeightDp = maxHeight - Dimens.spaceMedium
            val hScrollState = rememberScrollState()
            var zoomScale by remember { mutableFloatStateOf(1.0f) }
            var panFraction by remember { mutableFloatStateOf(0.5f) }

            val isZoomed = zoomScale > 1.0f
            val contentWidth = if (isZoomed) viewportWidth * zoomScale else viewportWidth

            // Keep panFraction in sync with manual user horizontal scroll when zoomed
            LaunchedEffect(hScrollState.value, hScrollState.maxValue) {
              if (hScrollState.maxValue > 0) {
                panFraction = (hScrollState.value.toFloat() / hScrollState.maxValue).coerceIn(0f, 1f)
              }
            }

            LaunchedEffect(zoomScale) {
              if (zoomScale <= 1.0f) {
                hScrollState.scrollTo(0)
                panFraction = 0.5f
              } else {
                val maxScroll = (screenWidthPx * (zoomScale - 1f)).roundToInt()
                if (maxScroll > 0) {
                  val targetScroll = (maxScroll * panFraction).roundToInt().coerceIn(0, maxScroll)
                  hScrollState.scrollTo(targetScroll)
                }
              }
            }

            Box(
              modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(
                  state = hScrollState,
                  enabled = isZoomed,
                )
                .pinchZoom(
                  onZoom = { zoomChange, centroid ->
                    val oldScale = zoomScale
                    val newScale = (zoomScale * zoomChange).coerceIn(1.0f, 4.0f)
                    if (newScale != oldScale) {
                      // When beginning zoom from 1.0x, anchor to pinch focal point or center
                      if (oldScale <= 1.01f) {
                        val focalX = if (centroid.x.isFinite() && centroid.x > 0f) centroid.x else screenWidthPx / 2f
                        panFraction = (focalX / screenWidthPx.toFloat()).coerceIn(0.2f, 0.8f)
                      }
                      zoomScale = newScale
                      val maxScroll = screenWidthPx * (newScale - 1f)
                      if (maxScroll > 0f) {
                        val targetScroll = (maxScroll * panFraction).roundToInt().coerceIn(0, maxScroll.roundToInt())
                        coroutineScope.launch {
                          hScrollState.scrollTo(targetScroll)
                        }
                      }
                    }
                  },
                  onDoubleTap = {
                    if (zoomScale < 1.5f) {
                      panFraction = 0.5f
                      val targetScale = 2.0f
                      zoomScale = targetScale
                      coroutineScope.launch {
                        val maxScroll = (screenWidthPx * (targetScale - 1f)).roundToInt()
                        hScrollState.animateScrollTo(maxScroll / 2)
                      }
                    } else {
                      zoomScale = 1.0f
                      panFraction = 0.5f
                      coroutineScope.launch {
                        hScrollState.animateScrollTo(0)
                      }
                    }
                  },
                ),
            ) {
              LazyColumn(
                state = listState,
                modifier = Modifier
                  .width(contentWidth)
                  .fillMaxHeight()
                  .padding(vertical = Dimens.spaceSmall),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                items(count = state.totalPages, key = { it }) { pageIndex ->
                  PdfPageItem(
                    pageIndex = pageIndex,
                    bitmap = pageBitmaps[pageIndex],
                    targetWidthPx = screenWidthPx,
                    zoomScale = zoomScale,
                    fitMode = state.fitMode,
                    viewportHeightDp = viewportHeightDp,
                    colorFilter = if (state.isNightMode) invertColorFilter else null,
                    isNightMode = state.isNightMode,
                    onRequestBitmap = { widthPx -> viewModel.requestPageBitmap(pageIndex, widthPx) },
                  )
                }
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
  zoomScale: Float,
  fitMode: PdfFitMode,
  viewportHeightDp: Dp,
  colorFilter: ColorFilter?,
  isNightMode: Boolean,
  onRequestBitmap: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  // Re-render bitmap debounced 300ms at new zoom level so it stays sharp
  LaunchedEffect(zoomScale) {
    delay(300)
    val zoomedWidth = (targetWidthPx * zoomScale).toInt().coerceIn(1, 4096)
    onRequestBitmap(zoomedWidth)
  }

  LaunchedEffect(pageIndex) {
    if (bitmap == null) {
      val initialWidth = (targetWidthPx * zoomScale).toInt().coerceIn(1, 4096)
      onRequestBitmap(initialWidth)
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
        .height(viewportHeightDp * zoomScale)
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
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(cardBg),
      contentAlignment = Alignment.Center,
    ) {
      if (bitmap != null) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = null,
          colorFilter = colorFilter,
          contentScale = ContentScale.FillWidth,
          modifier = Modifier.fillMaxWidth(),
        )
      } else {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.heroIconSize * 3),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
            modifier = Modifier.size(Dimens.iconSizeMedium),
            color = MaterialTheme.colorScheme.primary,
          )
        }
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
  var textInput by remember { mutableStateOf(currentPage.toString()) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(Dimens.cornerLarge),
      modifier = Modifier
        .width(Dimens.jumpDialogWidth)
        .padding(Dimens.spaceMedium),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(
        modifier = Modifier.padding(Dimens.spaceLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = stringResource(id = R.string.action_jump_to_page),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(Dimens.spaceMedium))

        OutlinedTextField(
          value = textInput,
          onValueChange = { textInput = it.filter { ch -> ch.isDigit() } },
          label = { Text(stringResource(id = R.string.pdf_page_counter, currentPage, totalPages)) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_jump_input"),
        )

        Spacer(modifier = Modifier.height(Dimens.spaceLarge))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) {
            Text(stringResource(id = R.string.action_close))
          }
          Spacer(modifier = Modifier.width(Dimens.spaceSmall))
          Button(
            onClick = {
              val pageNum = textInput.toIntOrNull() ?: currentPage
              onPageSelected(pageNum.coerceIn(1, totalPages))
            },
            modifier = Modifier.testTag("pdf_jump_submit"),
          ) {
            Text(stringResource(id = R.string.action_open))
          }
        }
      }
    }
  }
}
