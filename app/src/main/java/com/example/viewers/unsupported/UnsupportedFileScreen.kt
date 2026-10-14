package com.example.viewers.unsupported

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.FileType
import com.example.core.OpenFileSession
import com.example.core.OpenFileSessionState
import com.example.ui.components.OpenViewTopAppBar
import com.example.ui.theme.Dimens

@Composable
fun UnsupportedFileScreen(
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  fileName: String = "",
  isLegacyDoc: Boolean = false,
) {
  val sessionState by OpenFileSession.sessionState.collectAsStateWithLifecycle()
  val resolvedFileName = when (val state = sessionState) {
    is OpenFileSessionState.Ready -> state.fileInfo.displayName
    else -> fileName.ifEmpty { "File" }
  }
  val resolvedIsLegacyDoc = when (val state = sessionState) {
    is OpenFileSessionState.Ready -> state.fileInfo.fileType == FileType.LEGACY_DOC
    else -> isLegacyDoc
  }

  Scaffold(
    topBar = {
      OpenViewTopAppBar(
        title = stringResource(id = R.string.status_unsupported),
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
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = Dimens.maxContentWidth)
          .padding(Dimens.spaceLarge)
          .testTag("unsupported_file_card"),
        shape = RoundedCornerShape(Dimens.cornerLarge),
        border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationNone),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.spaceLarge),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Surface(
            shape = CircleShape,
            border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(Dimens.heroIconSize),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(Dimens.iconSizeLarge),
              )
            }
          }

          Spacer(modifier = Modifier.height(Dimens.spaceMedium))

          Text(
            text = if (resolvedIsLegacyDoc) {
              "Legacy .doc Format"
            } else {
              "Unsupported File Format"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(Dimens.spaceSmall))

          Text(
            text = if (resolvedIsLegacyDoc) {
              "\"$resolvedFileName\" is in legacy Microsoft Word .doc format. OpenView supports modern .docx format. You can convert this file to .docx or .pdf to open it in OpenView."
            } else {
              "\"$resolvedFileName\" format cannot be opened yet. OpenView currently supports TXT, Source Code, PDF, and DOCX files."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(Dimens.spaceLarge))

          Button(
            onClick = onNavigateUp,
            modifier = Modifier
              .fillMaxWidth()
              .height(Dimens.minTouchTarget)
              .testTag("unsupported_back_button"),
            shape = RoundedCornerShape(Dimens.cornerMedium),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          ) {
            Text(text = stringResource(id = R.string.nav_back))
          }
        }
      }
    }
  }
}
