// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hiharis.openview.R
import io.github.hiharis.openview.core.OpenFileSession
import io.github.hiharis.openview.data.files.DeviceDocument
import io.github.hiharis.openview.ui.components.FileCard
import io.github.hiharis.openview.ui.components.OpenViewTopAppBar
import io.github.hiharis.openview.ui.theme.Dimens
import io.github.hiharis.openview.ui.theme.OpenViewTheme
import java.util.Locale

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

  val manageStorageLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) {
    viewModel.checkAndRefreshPermission()
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { isGranted ->
    viewModel.updatePermissionStatus(isGranted)
  }

  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.checkAndRefreshPermission()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val requestStoragePermission = {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      try {
        val intent = Intent(
          Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
          Uri.parse("package:${context.packageName}"),
        )
        manageStorageLauncher.launch(intent)
      } catch (_: Exception) {
        try {
          val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
          manageStorageLauncher.launch(intent)
        } catch (_: Exception) {
        }
      }
    } else {
      permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
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
          recentFiles = uiState.filteredFiles,
          deviceDocuments = uiState.filteredDeviceDocuments,
          isScanningDocuments = uiState.isScanningDocuments,
          hasStoragePermission = uiState.hasStoragePermission,
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
          onDeviceDocumentClick = { doc ->
            OpenFileSession.openUri(context, doc.uri)
          },
          onRemoveRecent = { fileItem ->
            viewModel.removeRecent(fileItem.id)
          },
          onRequestPermission = requestStoragePermission,
        )
      }
    }
  }
}

@Composable
private fun TabContent(
  tab: HomeTab,
  recentFiles: List<RecentFileItem>,
  deviceDocuments: List<DeviceDocument>,
  isScanningDocuments: Boolean,
  hasStoragePermission: Boolean,
  onOpenFile: () -> Unit,
  onFileClick: (RecentFileItem) -> Unit,
  onDeviceDocumentClick: (DeviceDocument) -> Unit,
  onRemoveRecent: (RecentFileItem) -> Unit,
  onRequestPermission: () -> Unit,
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

    // Permission banner if storage access is not granted
    if (!hasStoragePermission) {
      item {
        StoragePermissionBanner(
          onRequestPermission = onRequestPermission,
        )
      }
    }

    // Documents in Download and Documents folders
    if (deviceDocuments.isNotEmpty()) {
      item {
        Text(
          text = stringResource(id = R.string.home_device_documents_header),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = Dimens.spaceSmall),
        )
      }

      items(deviceDocuments, key = { it.id }) { document ->
        DeviceDocumentCard(
          document = document,
          onClick = { onDeviceDocumentClick(document) },
        )
      }
    } else if (isScanningDocuments) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("scanning_documents_card"),
          shape = RoundedCornerShape(Dimens.cornerLarge),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
          ),
          border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(Dimens.spaceMedium),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = stringResource(id = R.string.home_scanning_documents),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // Recent files section
    if (recentFiles.isNotEmpty()) {
      item {
        Text(
          text = stringResource(id = R.string.home_recent_files_header),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = Dimens.spaceSmall),
        )
      }

      items(recentFiles, key = { it.id }) { fileItem ->
        FileCard(
          fileName = fileItem.fileName,
          fileDetails = fileItem.lastOpenedFormatted,
          extension = fileItem.extension,
          onClick = { onFileClick(fileItem) },
          onRemoveFromRecents = { onRemoveRecent(fileItem) },
        )
      }
    } else if (deviceDocuments.isEmpty() && !isScanningDocuments) {
      item {
        TabEmptyState(tab = tab)
      }
    }
  }
}

@Composable
private fun DeviceDocumentCard(
  document: DeviceDocument,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val fileTypeColors = OpenViewTheme.fileTypeColors
  val (icon: ImageVector, iconColor: Color, containerColor: Color) = when (document.extension.lowercase(Locale.ROOT)) {
    "pdf" -> Triple(Icons.Default.PictureAsPdf, fileTypeColors.pdf, fileTypeColors.pdfContainer)
    "doc", "docx", "rtf", "odt" -> Triple(Icons.Default.Description, fileTypeColors.word, fileTypeColors.wordContainer)
    "py", "kt", "kts", "java", "cpp", "c", "h", "hpp", "js", "ts", "html", "css", "json", "xml", "md", "sh", "sql" ->
      Triple(Icons.Default.Code, fileTypeColors.code, fileTypeColors.codeContainer)
    else -> Triple(Icons.Default.Description, fileTypeColors.text, fileTypeColors.textContainer)
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Dimens.cornerLarge))
      .clickable(onClick = onClick)
      .testTag("device_document_card_${document.name}"),
    shape = RoundedCornerShape(Dimens.cornerLarge),
    border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLow),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          horizontal = Dimens.spaceMedium,
          vertical = Dimens.spaceMedium,
        ),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(Dimens.cornerMedium),
        color = containerColor,
        modifier = Modifier.size(Dimens.iconTileSize),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = document.name,
            tint = iconColor,
            modifier = Modifier.size(Dimens.iconSizeLarge),
          )
        }
      }

      Spacer(modifier = Modifier.width(Dimens.spaceMedium))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = document.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${document.folderName} • ${document.formattedSize} • ${document.lastModifiedFormatted}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = Dimens.spaceExtraSmall),
        )
      }
    }
  }
}

@Composable
private fun StoragePermissionBanner(
  onRequestPermission: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("storage_permission_banner"),
    shape = RoundedCornerShape(Dimens.cornerLarge),
    border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(Dimens.spaceMedium),
    ) {
      Text(
        text = stringResource(id = R.string.home_storage_permission_title),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(Dimens.spaceExtraSmall))
      Text(
        text = stringResource(id = R.string.home_storage_permission_desc),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(Dimens.spaceSmall))
      Button(
        onClick = onRequestPermission,
        modifier = Modifier
          .fillMaxWidth()
          .height(Dimens.minTouchTarget)
          .testTag("grant_storage_permission_button"),
        shape = RoundedCornerShape(Dimens.cornerMedium),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
      ) {
        Text(
          text = stringResource(id = R.string.home_storage_permission_button),
          fontWeight = FontWeight.Bold,
        )
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
