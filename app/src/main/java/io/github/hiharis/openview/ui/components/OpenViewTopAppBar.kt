// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import io.github.hiharis.openview.R
import io.github.hiharis.openview.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenViewTopAppBar(
  title: String,
  modifier: Modifier = Modifier,
  canNavigateBack: Boolean = false,
  onNavigateUp: () -> Unit = {},
  actions: @Composable RowScope.() -> Unit = {},
) {
  TopAppBar(
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
      )
    },
    modifier = modifier.testTag("top_app_bar"),
    navigationIcon = {
      if (canNavigateBack) {
        IconButton(
          onClick = onNavigateUp,
          modifier = Modifier
            .size(Dimens.minTouchTarget)
            .testTag("nav_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(id = R.string.nav_back),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(Dimens.iconSizeMedium),
          )
        }
      }
    },
    actions = actions,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.background,
      titleContentColor = MaterialTheme.colorScheme.onBackground,
    ),
  )
}
