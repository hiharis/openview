// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hiharis.openview.R
import io.github.hiharis.openview.core.OpenFileSession
import io.github.hiharis.openview.ui.components.FileCard
import io.github.hiharis.openview.ui.components.OpenViewTopAppBar
import io.github.hiharis.openview.ui.theme.Dimens
import io.github.hiharis.openview.ui.theme.OpenViewTheme

@Composable
fun HomeScreen(
  onNavigateToSettings: () -> Unit,
  onNavigateToAbout: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val fileTypeColors = OpenViewTheme.fileTypeColors

  val openDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri: Uri? ->
    if (uri != null) {
      OpenFileSession.openUri(context, uri)
    }
  }

  Scaffold(
    topBar = {
      OpenViewTopAppBar(
        title = stringResource(id = R.string.app_name),
        actions = {
          IconButton(
            onClick = onNavigateToAbout,
            modifier = Modifier
              .size(Dimens.minTouchTarget)
              .testTag("home_about_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = stringResource(id = R.string.action_info),
              tint = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.size(Dimens.iconSizeMedium),
            )
          }
          IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
              .size(Dimens.minTouchTarget)
              .testTag("home_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = stringResource(id = R.string.action_settings),
              tint = MaterialTheme.colorScheme.onBackground,
              modifier = Modifier.size(Dimens.iconSizeMedium),
            )
          }
        },
      )
    },
    bottomBar = {
      // Elderly-friendly NavigationBar with large touch targets and permanent text labels
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = Dimens.elevationLow,
        modifier = Modifier.fillMaxWidth().testTag("home_bottom_bar"),
      ) {
        val tabs = listOf(
          Triple(HomeTab.ALL, stringResource(id = R.string.tab_all), Icons.AutoMirrored.Filled.LibraryBooks),
          Triple(HomeTab.PDF, stringResource(id = R.string.tab_pdf), Icons.Default.PictureAsPdf),
          Triple(HomeTab.WORD, stringResource(id = R.string.tab_word), Icons.Default.Description),
          Triple(HomeTab.TEXT, stringResource(id = R.string.tab_text), Icons.Default.Description),
          Triple(HomeTab.CODE, stringResource(id = R.string.tab_code), Icons.Default.Code),
        )

        tabs.forEach { (tab, label, icon) ->
          val isSelected = uiState.selectedTab == tab
          val indicatorColor = when (tab) {
            HomeTab.ALL -> MaterialTheme.colorScheme.primaryContainer
            HomeTab.PDF -> fileTypeColors.pdfContainer
            HomeTab.WORD -> fileTypeColors.wordContainer
            HomeTab.TEXT -> fileTypeColors.textContainer
            HomeTab.CODE -> fileTypeColors.codeContainer
          }
          val activeIconColor = when (tab) {
            HomeTab.ALL -> MaterialTheme.colorScheme.primary
            HomeTab.PDF -> fileTypeColors.pdf
            HomeTab.WORD -> fileTypeColors.word
            HomeTab.TEXT -> fileTypeColors.text
            HomeTab.CODE -> fileTypeColors.code
          }

          NavigationBarItem(
            selected = isSelected,
            onClick = { viewModel.selectTab(tab) },
            icon = {
              Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(Dimens.iconSizeMedium),
              )
            },
            label = {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              )
            },
            alwaysShowLabel = true,
            modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = activeIconColor,
              selectedTextColor = MaterialTheme.colorScheme.onSurface,
              indicatorColor = indicatorColor,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
          )
        }
      }
    },
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      AnimatedContent(
        targetState = uiState.selectedTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "tab_content_animation",
      ) { currentTab ->
        TabContent(
          tab = currentTab,
          files = uiState.filteredFiles,
          onOpenFile = {
            openDocumentLauncher.launch(
              arrayOf(
                "application/pdf",
                "image/*",
                "text/*",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/msword",
                "application/rtf",
                "application/x-rtf",
                "text/rtf",
                "application/vnd.oasis.opendocument.text",
                "application/json",
                "application/xml",
                "text/xml",
                "text/html",
                "text/css",
                "text/javascript",
                "application/javascript",
                "text/markdown",
                "text/x-markdown",
                "text/csv",
                "application/octet-stream",
                "*/*",
              ),
            )
          },
          onFileClick = { fileItem ->
            OpenFileSession.openUri(context, Uri.parse(fileItem.fileUri))
          },
          onRemoveRecent = { fileItem ->
            viewModel.removeRecent(fileItem.id)
          },
        )
      }
    }
  }
}

@Composable
private fun TabContent(
  tab: HomeTab,
  files: List<RecentFileItem>,
  onOpenFile: () -> Unit,
  onFileClick: (RecentFileItem) -> Unit,
  onRemoveRecent: (RecentFileItem) -> Unit,
) {
  val title = when (tab) {
    HomeTab.ALL -> stringResource(id = R.string.home_title_all)
    HomeTab.PDF -> stringResource(id = R.string.home_title_pdf)
    HomeTab.WORD -> stringResource(id = R.string.home_title_word)
    HomeTab.TEXT -> stringResource(id = R.string.home_title_text)
    HomeTab.CODE -> stringResource(id = R.string.home_title_code)
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .widthIn(max = Dimens.maxContentWidth)
      .padding(horizontal = Dimens.spaceMedium),
    contentPadding = PaddingValues(vertical = Dimens.spaceMedium),
    verticalArrangement = Arrangement.spacedBy(Dimens.spaceMedium),
  ) {
    // Header & Prominent "Open File" button
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = Dimens.spaceSmall),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Dimens.spaceMedium))

        // Prominent Open File Button (56dp minimum touch target)
        Button(
          onClick = onOpenFile,
          modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.minTouchTarget)
            .testTag("open_file_button"),
          shape = RoundedCornerShape(Dimens.cornerLarge),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
          elevation = ButtonDefaults.buttonElevation(defaultElevation = Dimens.elevationLow),
        ) {
          Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconSizeMedium),
          )
          Spacer(modifier = Modifier.width(Dimens.spaceSmall))
          Text(
            text = stringResource(id = R.string.home_open_file_button),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    if (files.isNotEmpty()) {
      item {
        Text(
          text = stringResource(id = R.string.home_recent_files_header),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = Dimens.spaceSmall),
        )
      }

      items(files, key = { it.id }) { fileItem ->
        FileCard(
          fileName = fileItem.fileName,
          fileDetails = fileItem.lastOpenedFormatted,
          extension = fileItem.extension,
          onClick = { onFileClick(fileItem) },
          onRemoveFromRecents = { onRemoveRecent(fileItem) },
        )
      }
    } else {
      item {
        TabEmptyState(tab = tab)
      }
    }
  }
}

@Composable
private fun TabEmptyState(tab: HomeTab) {
  val (titleRes, descRes, icon) = when (tab) {
    HomeTab.ALL -> Triple(R.string.empty_all_title, R.string.empty_all_desc, Icons.AutoMirrored.Outlined.InsertDriveFile)
    HomeTab.PDF -> Triple(R.string.empty_pdf_title, R.string.empty_pdf_desc, Icons.Default.PictureAsPdf)
    HomeTab.WORD -> Triple(R.string.empty_word_title, R.string.empty_word_desc, Icons.Default.Description)
    HomeTab.TEXT -> Triple(R.string.empty_text_title, R.string.empty_text_desc, Icons.Default.Description)
    HomeTab.CODE -> Triple(R.string.empty_code_title, R.string.empty_code_desc, Icons.Default.Code)
  }

  val fileTypeColors = OpenViewTheme.fileTypeColors
  val iconTint = when (tab) {
    HomeTab.ALL -> MaterialTheme.colorScheme.onSurfaceVariant
    HomeTab.PDF -> fileTypeColors.pdf
    HomeTab.WORD -> fileTypeColors.word
    HomeTab.TEXT -> fileTypeColors.text
    HomeTab.CODE -> fileTypeColors.code
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("empty_tab_card"),
    shape = RoundedCornerShape(Dimens.cornerExtraLarge),
    border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLow),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          horizontal = Dimens.spaceLarge,
          vertical = Dimens.spaceExtraLarge,
        ),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(Dimens.heroIconSize),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(Dimens.iconSizeLarge),
          )
        }
      }

      Spacer(modifier = Modifier.height(Dimens.spaceMedium))

      Text(
        text = stringResource(id = titleRes),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(Dimens.spaceSmall))

      Text(
        text = stringResource(id = descRes),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}
