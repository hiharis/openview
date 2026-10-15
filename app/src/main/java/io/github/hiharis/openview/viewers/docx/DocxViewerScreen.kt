// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.viewers.docx

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Description
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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
import io.github.hiharis.openview.ui.theme.OpenViewTheme

@Composable
fun DocxViewerScreen(
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: DocxViewerViewModel = viewModel(),
) {
  val context = LocalContext.current
  val sessionState by OpenFileSession.sessionState.collectAsStateWithLifecycle()
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()

  LaunchedEffect(sessionState) {
    if (sessionState is OpenFileSessionState.Ready) {
      val ready = sessionState as OpenFileSessionState.Ready
      viewModel.loadDocx(context, ready.fileInfo.uri, ready.fileInfo.displayName)
    }
  }

  Scaffold(
    topBar = {
      OpenViewTopAppBar(
        title = when (val s = state) {
          is DocxUiState.Content -> s.fileName
          else -> stringResource(id = R.string.viewer_title_docx)
        },
        canNavigateBack = true,
        onNavigateUp = onNavigateUp,
        actions = {
          if (state is DocxUiState.Content) {
            val contentState = state as DocxUiState.Content
            IconButton(
              onClick = { viewModel.toggleSearch() },
              modifier = Modifier
                .size(Dimens.minTouchTarget)
                .testTag("docx_action_search"),
            ) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(id = R.string.action_search),
                tint = if (contentState.isSearchOpen) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onBackground
                },
                modifier = Modifier.size(Dimens.iconSizeMedium),
              )
            }
          }
        },
      )
    },
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
    ) {
      when (val state = state) {
        is DocxUiState.Loading -> {
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

        is DocxUiState.Content -> {
          Column(modifier = Modifier.fillMaxSize()) {
            if (state.isSearchOpen) {
              DocxSearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                matchCount = state.searchMatches.size,
                currentMatchIndex = state.currentMatchIndex,
                onPrevMatch = { viewModel.prevMatch() },
                onNextMatch = { viewModel.nextMatch() },
                onCloseSearch = { viewModel.toggleSearch() },
              )
            }

            if (state.isEmpty) {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(Dimens.spaceLarge),
                contentAlignment = Alignment.Center,
              ) {
                Card(
                  shape = RoundedCornerShape(Dimens.cornerLarge),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLow),
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spaceMedium),
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(Dimens.spaceLarge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Description,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(Dimens.heroIconSize),
                    )
                    Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                    Text(
                      text = stringResource(id = R.string.viewer_empty_file),
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                      textAlign = TextAlign.Center,
                    )
                  }
                }
              }
            } else {
              var zoomFactor by remember { mutableFloatStateOf(1.0f) }

              LaunchedEffect(zoomFactor) {
                val index = listState.firstVisibleItemIndex
                val offset = listState.firstVisibleItemScrollOffset
                listState.scrollToItem(index, offset)
              }

              val syntaxColors = OpenViewTheme.syntaxColors
              val currentMatch = if (state.searchMatches.isNotEmpty() && state.currentMatchIndex in state.searchMatches.indices) {
                state.searchMatches[state.currentMatchIndex]
              } else {
                null
              }

              Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                  state = listState,
                  modifier = Modifier
                    .fillMaxSize()
                    .pinchZoom(
                      onZoom = { zoomChange, _ ->
                        zoomFactor = (zoomFactor * zoomChange).coerceIn(0.5f, 4.0f)
                      },
                      onDoubleTap = {
                        zoomFactor = if (zoomFactor < 1.5f) 2.0f else 1.0f
                      },
                    )
                    .padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceSmall),
                ) {
                  itemsIndexed(state.elements, key = { index, _ -> index }) { index, element ->
                    when (element) {
                      is DocxElement.Heading -> {
                        val baseFontSize = when (element.level) {
                          1 -> 24f
                          2 -> 20f
                          else -> 18f
                        }
                        val fontSizeSp = (baseFontSize * zoomFactor).sp
                        val topSpacing = when (element.level) {
                          1 -> Dimens.heading1Spacing
                          2 -> Dimens.heading2Spacing
                          else -> Dimens.heading3Spacing
                        }
                        val highlighted = highlightText(
                          text = element.text,
                          elementIndex = index,
                          query = state.searchQuery,
                          currentMatch = currentMatch,
                          highlightColor = syntaxColors.searchHighlight,
                          currentHighlightColor = syntaxColors.searchHighlightCurrent,
                        )
                        Text(
                          text = highlighted,
                          fontSize = fontSizeSp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onBackground,
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = topSpacing, bottom = Dimens.spaceSmall),
                        )
                      }

                      is DocxElement.Paragraph -> {
                        val highlighted = highlightAnnotatedText(
                          annotatedText = element.text,
                          elementIndex = index,
                          query = state.searchQuery,
                          currentMatch = currentMatch,
                          highlightColor = syntaxColors.searchHighlight,
                          currentHighlightColor = syntaxColors.searchHighlightCurrent,
                        )
                        Text(
                          text = highlighted,
                          fontSize = (16f * zoomFactor).sp,
                          lineHeight = (24f * zoomFactor).sp,
                          color = MaterialTheme.colorScheme.onBackground,
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.paragraphSpacing),
                        )
                      }

                      is DocxElement.ListItem -> {
                        val highlighted = highlightAnnotatedText(
                          annotatedText = element.text,
                          elementIndex = index,
                          query = state.searchQuery,
                          currentMatch = currentMatch,
                          highlightColor = syntaxColors.searchHighlight,
                          currentHighlightColor = syntaxColors.searchHighlightCurrent,
                        )
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.listItemSpacing),
                          verticalAlignment = Alignment.Top,
                        ) {
                          Text(
                            text = "•",
                            fontSize = (18f * zoomFactor).sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = Dimens.spaceSmall),
                          )
                          Text(
                            text = highlighted,
                            fontSize = (16f * zoomFactor).sp,
                            lineHeight = (24f * zoomFactor).sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                          )
                        }
                      }

                      is DocxElement.Table -> {
                        DocxTableCard(
                          table = element,
                          elementIndex = index,
                          searchQuery = state.searchQuery,
                          currentMatch = currentMatch,
                          highlightColor = syntaxColors.searchHighlight,
                          currentHighlightColor = syntaxColors.searchHighlightCurrent,
                          zoomFactor = zoomFactor,
                        )
                      }
                    }
                  }
                }

                // Draggable fast scrollbar on right edge
                DraggableFastScrollBar(
                  listState = listState,
                  totalItems = state.elements.size,
                  modifier = Modifier.align(Alignment.CenterEnd),
                )
              }
            }
          }
        }

        is DocxUiState.Error -> {
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
private fun DocxTableCard(
  table: DocxElement.Table,
  elementIndex: Int,
  searchQuery: String,
  currentMatch: DocxSearchMatch?,
  highlightColor: Color,
  currentHighlightColor: Color,
  zoomFactor: Float,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = Dimens.tableSpacing)
      .testTag("docx_table_card"),
    shape = RoundedCornerShape(Dimens.tableCornerRadius),
    border = BorderStroke(Dimens.tableBorderWidth, MaterialTheme.colorScheme.outline),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLow),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
    ) {
      Column(modifier = Modifier.padding(Dimens.tableCellPadding)) {
        table.rows.forEachIndexed { rowIndex, row ->
          val isHeaderRow = rowIndex == 0
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                if (isHeaderRow) {
                  MaterialTheme.colorScheme.surfaceVariant
                } else {
                  MaterialTheme.colorScheme.surface
                },
              )
              .padding(vertical = Dimens.spaceSmall),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            row.forEach { cellText ->
              val highlighted = highlightAnnotatedText(
                annotatedText = cellText,
                elementIndex = elementIndex,
                query = searchQuery,
                currentMatch = currentMatch,
                highlightColor = highlightColor,
                currentHighlightColor = currentHighlightColor,
              )
              Box(
                modifier = Modifier
                  .width((Dimens.jumpDialogWidth / 2) * zoomFactor)
                  .padding(Dimens.tableCellPadding),
              ) {
                Text(
                  text = highlighted,
                  fontSize = (14f * zoomFactor).sp,
                  fontWeight = if (isHeaderRow) FontWeight.Bold else FontWeight.Normal,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }

          if (rowIndex < table.rows.size - 1) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.dividerThickness)
                .background(MaterialTheme.colorScheme.outlineVariant),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DocxSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  matchCount: Int,
  currentMatchIndex: Int,
  onPrevMatch: () -> Unit,
  onNextMatch: () -> Unit,
  onCloseSearch: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shadowElevation = Dimens.elevationLow,
    modifier = modifier
      .fillMaxWidth()
      .testTag("docx_search_bar"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceSmall),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
    ) {
      OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
          Text(
            text = stringResource(id = R.string.search_placeholder),
            style = MaterialTheme.typography.bodyMedium,
          )
        },
        singleLine = true,
        shape = RoundedCornerShape(Dimens.searchBarCorner),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
          .weight(1f)
          .testTag("docx_search_input"),
      )

      if (query.isNotEmpty()) {
        Text(
          text = if (matchCount > 0) {
            stringResource(id = R.string.search_match_counter, currentMatchIndex + 1, matchCount)
          } else {
            stringResource(id = R.string.search_no_matches)
          },
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        IconButton(
          onClick = onPrevMatch,
          enabled = matchCount > 0,
          modifier = Modifier.size(Dimens.minTouchTarget),
        ) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = stringResource(id = R.string.action_search_prev),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        IconButton(
          onClick = onNextMatch,
          enabled = matchCount > 0,
          modifier = Modifier.size(Dimens.minTouchTarget),
        ) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = stringResource(id = R.string.action_search_next),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      IconButton(
        onClick = onCloseSearch,
        modifier = Modifier.size(Dimens.minTouchTarget),
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = stringResource(id = R.string.action_search_close),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

private fun highlightAnnotatedText(
  annotatedText: AnnotatedString,
  elementIndex: Int,
  query: String,
  currentMatch: DocxSearchMatch?,
  highlightColor: Color,
  currentHighlightColor: Color,
): AnnotatedString {
  if (query.isBlank()) return annotatedText
  val rawText = annotatedText.text
  val builder = AnnotatedString.Builder(annotatedText)
  var start = 0
  while (start < rawText.length) {
    val idx = rawText.indexOf(query, start, ignoreCase = true)
    if (idx == -1) break
    val isCurrent = currentMatch != null && currentMatch.elementIndex == elementIndex && currentMatch.startIndex == idx
    val bg = if (isCurrent) currentHighlightColor else highlightColor
    builder.addStyle(SpanStyle(background = bg), idx, idx + query.length)
    start = idx + query.length.coerceAtLeast(1)
  }
  return builder.toAnnotatedString()
}

private fun highlightText(
  text: String,
  elementIndex: Int,
  query: String,
  currentMatch: DocxSearchMatch?,
  highlightColor: Color,
  currentHighlightColor: Color,
): AnnotatedString {
  val builder = AnnotatedString.Builder(text)
  if (query.isBlank()) return builder.toAnnotatedString()
  var start = 0
  while (start < text.length) {
    val idx = text.indexOf(query, start, ignoreCase = true)
    if (idx == -1) break
    val isCurrent = currentMatch != null && currentMatch.elementIndex == elementIndex && currentMatch.startIndex == idx
    val bg = if (isCurrent) currentHighlightColor else highlightColor
    builder.addStyle(SpanStyle(background = bg), idx, idx + query.length)
    start = idx + query.length.coerceAtLeast(1)
  }
  return builder.toAnnotatedString()
}
