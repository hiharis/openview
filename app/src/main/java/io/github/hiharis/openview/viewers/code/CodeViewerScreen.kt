// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.viewers.code

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material.icons.outlined.Code
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
import androidx.compose.ui.text.font.FontFamily
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
import io.github.hiharis.openview.ui.theme.Dimens
import io.github.hiharis.openview.ui.theme.OpenViewTheme
import io.github.hiharis.openview.ui.theme.SyntaxColors

@Composable
fun CodeViewerScreen(
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: CodeViewerViewModel = viewModel(),
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val sessionState by OpenFileSession.sessionState.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()

  LaunchedEffect(sessionState) {
    when (val state = sessionState) {
      is OpenFileSessionState.Ready -> {
        viewModel.loadFile(
          context = context,
          uri = state.fileInfo.uri,
          fileName = state.fileInfo.displayName,
          sizeBytes = state.fileInfo.sizeBytes,
        )
      }
      is OpenFileSessionState.Error -> {
        viewModel.setError(state.errorMessage)
      }
      else -> Unit
    }
  }

  val currentContent = uiState as? CodeUiState.Content
  LaunchedEffect(currentContent?.currentMatchIndex) {
    val content = currentContent ?: return@LaunchedEffect
    if (content.currentMatchIndex in content.searchMatches.indices) {
      val match = content.searchMatches[content.currentMatchIndex]
      listState.animateScrollToItem(match.lineIndex)
    }
  }

  val title = when (val state = uiState) {
    is CodeUiState.Content -> state.fileName
    else -> stringResource(id = R.string.viewer_title_code)
  }

  Scaffold(
    topBar = {
      OpenViewTopAppBar(
        title = title,
        canNavigateBack = true,
        onNavigateUp = onNavigateUp,
        actions = {
          if (uiState is CodeUiState.Content) {
            IconButton(
              onClick = { viewModel.toggleSearch() },
              modifier = Modifier
                .size(Dimens.minTouchTarget)
                .testTag("code_action_search"),
            ) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(id = R.string.action_search),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(Dimens.iconSizeMedium),
              )
            }
            IconButton(
              onClick = { viewModel.toggleWordWrap() },
              modifier = Modifier
                .size(Dimens.minTouchTarget)
                .testTag("code_action_wrap"),
            ) {
              Icon(
                imageVector = Icons.Default.WrapText,
                contentDescription = stringResource(id = R.string.action_wrap_text),
                tint = if (currentContent?.wordWrap == true) {
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
      when (val state = uiState) {
        is CodeUiState.Loading -> {
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

        is CodeUiState.Content -> {
          Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            if (state.isSearchOpen) {
              CodeSearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                matchCount = state.searchMatches.size,
                currentMatchIndex = state.currentMatchIndex,
                onPrevMatch = { viewModel.prevMatch() },
                onNextMatch = { viewModel.nextMatch() },
                onCloseSearch = { viewModel.toggleSearch() },
              )
            }

            // Language & File Meta Bar
            CodeInfoBar(
              language = state.language,
              lineCount = state.totalLines,
              encoding = state.encoding,
            )

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
                      imageVector = Icons.Outlined.Code,
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
              val effectiveFontSizeSp = (state.fontSizeSp * state.zoomScale * zoomFactor).coerceIn(11f, 32f)
              val syntaxColors = OpenViewTheme.syntaxColors
              val currentMatchLine = if (state.currentMatchIndex in state.searchMatches.indices) {
                state.searchMatches[state.currentMatchIndex].lineIndex
              } else {
                -1
              }

              val horizontalScrollState = rememberScrollState()
              val lineCountDigits = state.totalLines.toString().length.coerceAtLeast(2)

              Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                  state = listState,
                  modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                      detectTransformGestures { _, _, zoom, _ ->
                        zoomFactor = (zoomFactor * zoom).coerceIn(0.8f, 2.5f)
                      }
                    },
                ) {
                  itemsIndexed(state.lines, key = { index, _ -> index }) { lineIndex, lineText ->
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.Top,
                    ) {
                      // Line number gutter
                      Box(
                        modifier = Modifier
                          .width(Dimens.codeGutterWidth + (lineCountDigits * 4).sp.value.toInt().coerceAtLeast(0).let { Dimens.spaceSmall })
                          .background(syntaxColors.gutterBackground)
                          .padding(horizontal = Dimens.codeGutterPadding, vertical = Dimens.spaceNone),
                        contentAlignment = Alignment.TopEnd,
                      ) {
                        Text(
                          text = (lineIndex + 1).toString(),
                          fontFamily = FontFamily.Monospace,
                          fontSize = (effectiveFontSizeSp * 0.85f).sp,
                          lineHeight = (effectiveFontSizeSp * 1.4f).sp,
                          color = syntaxColors.gutterText,
                          textAlign = TextAlign.End,
                        )
                      }

                      // Vertical Divider
                      Box(
                        modifier = Modifier
                          .width(Dimens.dividerThickness)
                          .height((effectiveFontSizeSp * 1.4f).sp.value.toInt().coerceAtLeast(20).let { Dimens.codeLineHeight })
                          .background(MaterialTheme.colorScheme.outlineVariant),
                      )

                      // Code Line Text
                      val lineModifier = if (state.wordWrap) {
                        Modifier
                          .weight(1f)
                          .padding(horizontal = Dimens.spaceSmall)
                      } else {
                        Modifier
                          .weight(1f)
                          .horizontalScroll(horizontalScrollState)
                          .padding(horizontal = Dimens.spaceSmall)
                      }

                      Box(modifier = lineModifier) {
                        val annotated = remember(lineText, state.searchQuery, state.currentMatchIndex, syntaxColors) {
                          highlightCodeLine(
                            lineText = lineText,
                            lineIndex = lineIndex,
                            syntaxColors = syntaxColors,
                            searchQuery = state.searchQuery,
                            currentMatchLine = currentMatchLine,
                            currentMatchIndex = state.currentMatchIndex,
                            searchMatches = state.searchMatches,
                          )
                        }

                        Text(
                          text = annotated,
                          fontFamily = FontFamily.Monospace,
                          fontSize = effectiveFontSizeSp.sp,
                          lineHeight = (effectiveFontSizeSp * 1.4f).sp,
                          color = MaterialTheme.colorScheme.onBackground,
                        )
                      }
                    }
                  }

                  if (!state.isFullLoadComplete) {
                    item {
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(Dimens.spaceMedium),
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

                // Draggable fast scrollbar on right edge
                DraggableFastScrollBar(
                  listState = listState,
                  totalItems = state.lines.size,
                  modifier = Modifier.align(Alignment.CenterEnd),
                )
              }
            }
          }
        }

        is CodeUiState.Error -> {
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
private fun CodeInfoBar(
  language: String,
  lineCount: Int,
  encoding: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.spaceMedium, vertical = Dimens.badgePaddingVertical),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Surface(
        shape = RoundedCornerShape(Dimens.cornerSmall),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Text(
          text = language,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(
            horizontal = Dimens.badgePaddingHorizontal,
            vertical = Dimens.badgePaddingVertical,
          ),
        )
      }

      Text(
        text = stringResource(id = R.string.file_meta_info, language, encoding, lineCount),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CodeSearchBar(
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
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = Dimens.elevationLow,
    shadowElevation = Dimens.elevationLow,
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceSmall),
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
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        ),
        shape = RoundedCornerShape(Dimens.searchBarCorner),
        modifier = Modifier
          .weight(1f)
          .height(Dimens.searchBarHeight)
          .testTag("code_search_query_input"),
      )

      Spacer(modifier = Modifier.width(Dimens.spaceSmall))

      if (query.isNotEmpty()) {
        Text(
          text = if (matchCount > 0) {
            stringResource(id = R.string.search_match_counter, currentMatchIndex + 1, matchCount)
          } else {
            stringResource(id = R.string.search_no_matches)
          },
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = Dimens.spaceSmall),
        )

        IconButton(
          onClick = onPrevMatch,
          enabled = matchCount > 0,
          modifier = Modifier
            .size(Dimens.minTouchTarget)
            .testTag("code_search_prev_button"),
        ) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = stringResource(id = R.string.action_search_prev),
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }

        IconButton(
          onClick = onNextMatch,
          enabled = matchCount > 0,
          modifier = Modifier
            .size(Dimens.minTouchTarget)
            .testTag("code_search_next_button"),
        ) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = stringResource(id = R.string.action_search_next),
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      IconButton(
        onClick = onCloseSearch,
        modifier = Modifier
          .size(Dimens.minTouchTarget)
          .testTag("code_search_close_button"),
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = stringResource(id = R.string.action_search_close),
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

private val KEYWORDS_REGEX = Regex(
  "\\b(fun|val|var|class|interface|object|enum|def|import|package|from|return|" +
    "if|else|for|while|do|break|continue|when|switch|case|try|catch|finally|throw|" +
    "new|public|private|protected|internal|static|final|override|abstract|const|" +
    "let|async|await|yield|function|typeof|null|true|false|nil|None|self|this|" +
    "super|struct|typedef|include|define)\\b"
)
private val STRING_LITERAL_REGEX = Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'")
private val COMMENT_REGEX = Regex("//.*|/\\*[\\s\\S]*?\\*/|#.*")
private val NUMBER_REGEX = Regex("\\b\\d+(\\.\\d+)?([fFdDlL])?\\b|0x[0-9a-fA-F]+")

private fun highlightCodeLine(
  lineText: String,
  lineIndex: Int,
  syntaxColors: SyntaxColors,
  searchQuery: String,
  currentMatchLine: Int,
  currentMatchIndex: Int,
  searchMatches: List<CodeSearchMatch>,
): AnnotatedString {
  return buildAnnotatedString {
    append(lineText)

    // Numbers
    NUMBER_REGEX.findAll(lineText).forEach { match ->
      addStyle(
        SpanStyle(color = syntaxColors.number),
        match.range.first,
        match.range.last + 1,
      )
    }

    // Keywords
    KEYWORDS_REGEX.findAll(lineText).forEach { match ->
      addStyle(
        SpanStyle(color = syntaxColors.keyword, fontWeight = FontWeight.Bold),
        match.range.first,
        match.range.last + 1,
      )
    }

    // Strings
    STRING_LITERAL_REGEX.findAll(lineText).forEach { match ->
      addStyle(
        SpanStyle(color = syntaxColors.string),
        match.range.first,
        match.range.last + 1,
      )
    }

    // Comments override syntax
    COMMENT_REGEX.findAll(lineText).forEach { match ->
      addStyle(
        SpanStyle(color = syntaxColors.comment),
        match.range.first,
        match.range.last + 1,
      )
    }

    // Search matches on top
    if (searchQuery.isNotBlank() && lineText.contains(searchQuery, ignoreCase = true)) {
      val lowerLine = lineText.lowercase()
      val lowerQuery = searchQuery.lowercase()
      var start = 0
      while (start < lineText.length) {
        val found = lowerLine.indexOf(lowerQuery, start)
        if (found == -1) break

        val isCurrent = lineIndex == currentMatchLine &&
          currentMatchIndex in searchMatches.indices &&
          searchMatches[currentMatchIndex].charIndex == found

        addStyle(
          SpanStyle(
            background = if (isCurrent) syntaxColors.searchHighlightCurrent else syntaxColors.searchHighlight,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
          ),
          found,
          found + searchQuery.length,
        )
        start = found + searchQuery.length
      }
    }
  }
}
