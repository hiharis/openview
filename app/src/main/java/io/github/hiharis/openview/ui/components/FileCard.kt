// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import io.github.hiharis.openview.R
import io.github.hiharis.openview.ui.theme.Dimens
import io.github.hiharis.openview.ui.theme.OpenViewTheme

@Composable
fun FileCard(
  fileName: String,
  fileDetails: String,
  extension: String,
  onClick: () -> Unit,
  onRemoveFromRecents: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val fileTypeColors = OpenViewTheme.fileTypeColors

  val (icon: ImageVector, iconColor: Color, containerColor: Color) = when (extension.lowercase()) {
    "pdf" -> Triple(Icons.Default.PictureAsPdf, fileTypeColors.pdf, fileTypeColors.pdfContainer)
    "doc", "docx", "rtf", "odt" -> Triple(Icons.Default.Description, fileTypeColors.word, fileTypeColors.wordContainer)
    "py", "kt", "kts", "java", "cpp", "c", "h", "hpp", "js", "ts", "html", "css", "json", "xml", "md", "sh" ->
      Triple(Icons.Default.Code, fileTypeColors.code, fileTypeColors.codeContainer)
    else -> Triple(Icons.Default.Description, fileTypeColors.text, fileTypeColors.textContainer)
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(Dimens.cornerLarge))
      .clickable(onClick = onClick)
      .testTag("file_card_$fileName"),
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
      // Colored File-Type Icon Tile
      Surface(
        shape = RoundedCornerShape(Dimens.cornerMedium),
        color = containerColor,
        modifier = Modifier.size(Dimens.iconTileSize),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = fileName,
            tint = iconColor,
            modifier = Modifier.size(Dimens.iconSizeLarge),
          )
        }
      }

      Spacer(modifier = Modifier.width(Dimens.spaceMedium))

      // File Name and Details
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = fileName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = fileDetails,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(top = Dimens.spaceExtraSmall),
        )
      }

      // Three-Dot Menu
      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier
            .size(Dimens.minTouchTarget)
            .testTag("file_card_menu_$fileName"),
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(id = R.string.home_more_options),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimens.iconSizeMedium),
          )
        }

        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
        ) {
          DropdownMenuItem(
            text = {
              Text(
                text = stringResource(id = R.string.home_remove_from_recents),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
              )
            },
            onClick = {
              menuExpanded = false
              onRemoveFromRecents()
            },
            modifier = Modifier.testTag("remove_from_recents_item"),
          )
        }
      }
    }
  }
}
