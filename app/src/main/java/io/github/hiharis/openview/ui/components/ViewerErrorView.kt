// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.ui.components

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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.hiharis.openview.R
import io.github.hiharis.openview.ui.theme.Dimens

@Composable
fun ViewerErrorView(
  errorMessage: String,
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(Dimens.spaceLarge),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = Dimens.maxContentWidth)
        .testTag("viewer_error_card"),
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
              imageVector = Icons.Outlined.ErrorOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(Dimens.iconSizeLarge),
            )
          }
        }

        Spacer(modifier = Modifier.height(Dimens.spaceMedium))

        Text(
          text = "Unable to open file",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Dimens.spaceSmall))

        Text(
          text = errorMessage,
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
            .testTag("viewer_error_back_button"),
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
