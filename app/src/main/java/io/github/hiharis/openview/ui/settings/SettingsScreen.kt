// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.hiharis.openview.R
import io.github.hiharis.openview.ui.components.OpenViewTopAppBar
import io.github.hiharis.openview.ui.components.SettingsRow
import io.github.hiharis.openview.ui.theme.BackgroundLight
import io.github.hiharis.openview.ui.theme.Dimens
import io.github.hiharis.openview.ui.theme.SurfaceDark
import io.github.hiharis.openview.ui.theme.TextPrimaryDark
import io.github.hiharis.openview.ui.theme.TextPrimaryLight
import io.github.hiharis.openview.ui.theme.Transparent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onNavigateUp: () -> Unit,
  onNavigateToAbout: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      OpenViewTopAppBar(
        title = stringResource(id = R.string.settings_title),
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
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = Dimens.maxContentWidth)
          .padding(horizontal = Dimens.spaceMedium),
        contentPadding = PaddingValues(vertical = Dimens.spaceMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLarge),
      ) {
        // 1. APPEARANCE SECTION
        item {
          SectionHeader(title = stringResource(id = R.string.settings_section_appearance))
          Spacer(modifier = Modifier.height(Dimens.spaceSmall))
          Card(
            shape = RoundedCornerShape(Dimens.cornerExtraLarge),
            border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLow),
          ) {
            Column(modifier = Modifier.padding(Dimens.spaceMedium)) {
              Text(
                text = stringResource(id = R.string.settings_theme_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Spacer(modifier = Modifier.height(Dimens.spaceMedium))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
              ) {
                ThemePreviewCard(
                  title = stringResource(id = R.string.settings_theme_system),
                  isSelected = uiState.themeMode == AppThemeMode.SYSTEM,
                  onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM) },
                  previewBgColor = MaterialTheme.colorScheme.surfaceVariant,
                  previewTextColor = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f).testTag("theme_card_system"),
                )
                ThemePreviewCard(
                  title = stringResource(id = R.string.settings_theme_light),
                  isSelected = uiState.themeMode == AppThemeMode.LIGHT,
                  onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                  previewBgColor = BackgroundLight,
                  previewTextColor = TextPrimaryLight,
                  modifier = Modifier.weight(1f).testTag("theme_card_light"),
                )
                ThemePreviewCard(
                  title = stringResource(id = R.string.settings_theme_dark),
                  isSelected = uiState.themeMode == AppThemeMode.DARK,
                  onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
                  previewBgColor = SurfaceDark,
                  previewTextColor = TextPrimaryDark,
                  modifier = Modifier.weight(1f).testTag("theme_card_dark"),
                )
              }
            }
          }
        }

        // 2. READING SECTION (Font size with live preview)
        item {
          SectionHeader(title = stringResource(id = R.string.settings_section_reading))
          Spacer(modifier = Modifier.height(Dimens.spaceSmall))
          Card(
            shape = RoundedCornerShape(Dimens.cornerExtraLarge),
            border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLow),
          ) {
            Column(modifier = Modifier.padding(Dimens.spaceMedium)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = stringResource(id = R.string.settings_font_size_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = stringResource(id = R.string.settings_font_size_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                Surface(
                  shape = RoundedCornerShape(Dimens.cornerMedium),
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.padding(start = Dimens.spaceSmall),
                ) {
                  Text(
                    text = stringResource(id = R.string.settings_font_size_value, uiState.fontSize.sizeSp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceSmall),
                  )
                }
              }

              Spacer(modifier = Modifier.height(Dimens.spaceLarge))

              // Stepper controls for elderly-friendly precision
              val sizes = FontSizePreference.values()
              val currentIndex = sizes.indexOf(uiState.fontSize).coerceAtLeast(0)

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                IconButton(
                  onClick = {
                    if (currentIndex > 0) {
                      viewModel.setFontSize(sizes[currentIndex - 1])
                    }
                  },
                  enabled = currentIndex > 0,
                  modifier = Modifier
                    .size(Dimens.minTouchTarget)
                    .clip(CircleShape)
                    .background(
                      if (currentIndex > 0) MaterialTheme.colorScheme.surfaceVariant else Transparent,
                    )
                    .testTag("font_size_decrease"),
                ) {
                  Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = stringResource(id = R.string.settings_font_size_decrease),
                    tint = if (currentIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                  )
                }

                Slider(
                  value = currentIndex.toFloat(),
                  onValueChange = { floatVal ->
                    val index = floatVal.toInt().coerceIn(0, sizes.size - 1)
                    viewModel.setFontSize(sizes[index])
                  },
                  valueRange = 0f..(sizes.size - 1).toFloat(),
                  steps = sizes.size - 2,
                  modifier = Modifier.weight(1f).padding(horizontal = Dimens.spaceMedium),
                  colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                  ),
                )

                IconButton(
                  onClick = {
                    if (currentIndex < sizes.size - 1) {
                      viewModel.setFontSize(sizes[currentIndex + 1])
                    }
                  },
                  enabled = currentIndex < sizes.size - 1,
                  modifier = Modifier
                    .size(Dimens.minTouchTarget)
                    .clip(CircleShape)
                    .background(
                      if (currentIndex < sizes.size - 1) MaterialTheme.colorScheme.surfaceVariant else Transparent,
                    )
                    .testTag("font_size_increase"),
                ) {
                  Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.settings_font_size_increase),
                    tint = if (currentIndex < sizes.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                  )
                }
              }

              Spacer(modifier = Modifier.height(Dimens.spaceMedium))

              // Live Text Preview Card
              Surface(
                shape = RoundedCornerShape(Dimens.cornerLarge),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(Dimens.spaceMedium)) {
                  Text(
                    text = stringResource(id = R.string.settings_preview_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(modifier = Modifier.height(Dimens.spaceSmall))
                  Text(
                    text = stringResource(id = R.string.settings_preview_sample),
                    fontSize = uiState.fontSize.sizeSp.sp,
                    lineHeight = (uiState.fontSize.sizeSp + 8).sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
        }

        // 3. ABOUT SECTION & SHORT PRIVACY CARD
        item {
          SectionHeader(title = stringResource(id = R.string.settings_section_about))
          Spacer(modifier = Modifier.height(Dimens.spaceSmall))
          Card(
            shape = RoundedCornerShape(Dimens.cornerExtraLarge),
            border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLow),
          ) {
            Column(modifier = Modifier.padding(Dimens.spaceSmall)) {
              SettingsRow(
                title = stringResource(id = R.string.settings_about_app_title),
                description = stringResource(id = R.string.settings_about_app_desc),
                icon = Icons.Default.Info,
                showChevron = true,
                onClick = onNavigateToAbout,
              )
            }
          }

          Spacer(modifier = Modifier.height(Dimens.spaceMedium))

          // Short Privacy Facts Card
          Card(
            shape = RoundedCornerShape(Dimens.cornerLarge),
            border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationNone),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(Dimens.spaceMedium),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconSizeLarge),
              )
              Spacer(modifier = Modifier.width(Dimens.spaceMedium))
              Column {
                Text(
                  text = stringResource(id = R.string.settings_privacy_badge_title),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = stringResource(id = R.string.settings_privacy_badge_desc),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ThemePreviewCard(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  previewBgColor: Color,
  previewTextColor: Color,
  modifier: Modifier = Modifier,
) {
  val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
  val borderWidth = if (isSelected) Dimens.borderFocus else Dimens.borderThin

  Card(
    modifier = modifier
      .clip(RoundedCornerShape(Dimens.cornerLarge))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(Dimens.cornerLarge),
    border = BorderStroke(borderWidth, borderColor),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) Dimens.elevationMedium else Dimens.elevationNone),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(Dimens.spaceMedium),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Mini Representation Box
      Surface(
        shape = RoundedCornerShape(Dimens.cornerSmall),
        color = previewBgColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp),
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.spaceSmall),
          verticalArrangement = Arrangement.Center,
        ) {
          Box(
            modifier = Modifier
              .width(28.dp)
              .height(4.dp)
              .background(previewTextColor, RoundedCornerShape(2.dp)),
          )
          Spacer(modifier = Modifier.height(4.dp))
          Box(
            modifier = Modifier
              .width(44.dp)
              .height(4.dp)
              .background(previewTextColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
          )
        }
      }

      Spacer(modifier = Modifier.height(Dimens.spaceSmall))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        if (isSelected) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = stringResource(id = R.string.content_desc_selected),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(Dimens.spaceExtraSmall))
        }
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.primary,
  )
}
