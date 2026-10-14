package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.Dimens

@Composable
fun SettingsRow(
  title: String,
  modifier: Modifier = Modifier,
  description: String? = null,
  icon: ImageVector? = null,
  showChevron: Boolean = false,
  trailingContent: @Composable (() -> Unit)? = null,
  onClick: (() -> Unit)? = null,
) {
  val clickModifier = if (onClick != null) {
    Modifier
      .clip(RoundedCornerShape(Dimens.cornerMedium))
      .clickable(onClick = onClick)
  } else {
    Modifier
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .then(clickModifier)
      .defaultMinSize(minHeight = Dimens.minTouchTarget)
      .testTag("settings_row_$title"),
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(Dimens.cornerMedium),
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
      if (icon != null) {
        // Soft accent-tinted circle for leading icon
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(Dimens.iconTileSize),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(Dimens.iconSizeMedium),
            )
          }
        }
        Spacer(modifier = Modifier.width(Dimens.spaceMedium))
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (description != null) {
          Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Dimens.spaceExtraSmall),
          )
        }
      }

      if (trailingContent != null) {
        Spacer(modifier = Modifier.width(Dimens.spaceSmall))
        trailingContent()
      } else if (showChevron) {
        Spacer(modifier = Modifier.width(Dimens.spaceSmall))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(Dimens.iconSizeMedium),
        )
      }
    }
  }
}
