// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.hiharis.openview.R
import io.github.hiharis.openview.ui.components.OpenViewTopAppBar
import io.github.hiharis.openview.ui.theme.Dimens

data class LibraryLicenseItem(
  val nameRes: Int,
  val licenseRes: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val repoUrl = stringResource(id = R.string.app_repo_url)
  val githubFollowUrl = stringResource(id = R.string.about_follow_github_url)
  val instagramFollowUrl = stringResource(id = R.string.about_follow_instagram_url)

  val libraries = listOf(
    LibraryLicenseItem(R.string.about_lib_compose_name, R.string.about_lib_compose_license),
    LibraryLicenseItem(R.string.about_lib_navigation_name, R.string.about_lib_navigation_license),
    LibraryLicenseItem(R.string.about_lib_datastore_name, R.string.about_lib_datastore_license),
    LibraryLicenseItem(R.string.about_lib_room_name, R.string.about_lib_room_license),
    LibraryLicenseItem(R.string.about_lib_coroutines_name, R.string.about_lib_coroutines_license),
  )

  Scaffold(
    topBar = {
      OpenViewTopAppBar(
        title = stringResource(id = R.string.about_title),
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
        // App Identity Header
        item {
          Card(
            shape = RoundedCornerShape(Dimens.cornerExtraLarge),
            border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLow),
            modifier = Modifier.fillMaxWidth().testTag("about_identity_card"),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spaceLarge),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(Dimens.heroIconSize),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.iconSizeLarge),
                  )
                }
              }

              Spacer(modifier = Modifier.height(Dimens.spaceMedium))

              Text(
                text = stringResource(id = R.string.about_app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )

              Spacer(modifier = Modifier.height(Dimens.spaceExtraSmall))

              Text(
                text = stringResource(id = R.string.app_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )

              Spacer(modifier = Modifier.height(Dimens.spaceMedium))

              Card(
                shape = RoundedCornerShape(Dimens.cornerLarge),
                border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationNone),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("about_support_card"),
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spaceMedium),
                ) {
                  Text(
                    text = stringResource(id = R.string.about_support_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )

                  Spacer(modifier = Modifier.height(Dimens.spaceSmall))

                  Text(
                    text = stringResource(id = R.string.about_support_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )

                  Spacer(modifier = Modifier.height(Dimens.spaceMedium))

                  Button(
                    onClick = {
                      try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubFollowUrl))
                        context.startActivity(intent)
                      } catch (_: ActivityNotFoundException) {
                      }
                    },
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = Dimens.minTouchTarget)
                      .testTag("about_follow_github"),
                    shape = RoundedCornerShape(Dimens.cornerMedium),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary,
                      contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = stringResource(id = R.string.about_follow_github_label),
                        modifier = Modifier.size(Dimens.iconSizeSmall),
                      )
                      Spacer(modifier = Modifier.width(Dimens.spaceSmall))
                      Text(
                        text = stringResource(id = R.string.about_follow_github_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(Dimens.spaceMedium))

                  OutlinedButton(
                    onClick = {
                      try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(instagramFollowUrl))
                        context.startActivity(intent)
                      } catch (_: ActivityNotFoundException) {
                      }
                    },
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(min = Dimens.minTouchTarget)
                      .testTag("about_follow_instagram"),
                    shape = RoundedCornerShape(Dimens.cornerMedium),
                    border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                      contentColor = MaterialTheme.colorScheme.primary,
                    ),
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.Center,
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = stringResource(id = R.string.about_follow_instagram_label),
                        modifier = Modifier.size(Dimens.iconSizeSmall),
                      )
                      Spacer(modifier = Modifier.width(Dimens.spaceSmall))
                      Text(
                        text = stringResource(id = R.string.about_follow_instagram_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(Dimens.spaceMedium))

              Text(
                text = stringResource(id = R.string.about_created_by),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
              )
            }
          }
        }

        // Details Section (Version, Author, License, Repo)
        item {
          Card(
            shape = RoundedCornerShape(Dimens.cornerExtraLarge),
            border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLow),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(Dimens.spaceMedium)) {
              InfoRow(
                label = stringResource(id = R.string.about_version_label),
                value = stringResource(id = R.string.about_version_value),
              )
              HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimens.spaceSmall),
                thickness = Dimens.borderThin,
                color = MaterialTheme.colorScheme.outlineVariant,
              )
              InfoRow(
                label = stringResource(id = R.string.about_author_label),
                value = stringResource(id = R.string.about_created_by),
              )
              HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimens.spaceSmall),
                thickness = Dimens.borderThin,
                color = MaterialTheme.colorScheme.outlineVariant,
              )
              InfoRow(
                label = stringResource(id = R.string.about_license_label),
                value = stringResource(id = R.string.about_license_value),
              )
              HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimens.spaceSmall),
                thickness = Dimens.borderThin,
                color = MaterialTheme.colorScheme.outlineVariant,
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    try {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl))
                      context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                  }
                  .padding(vertical = Dimens.spaceSmall)
                  .testTag("about_github_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = stringResource(id = R.string.about_source_code_github),
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = stringResource(id = R.string.app_author),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(modifier = Modifier.width(Dimens.spaceExtraSmall))
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.iconSizeSmall),
                  )
                }
              }
            }
          }
        }

        // Mission & Privacy Statement
        item {
          Card(
            shape = RoundedCornerShape(Dimens.cornerExtraLarge),
            border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLow),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(Dimens.spaceLarge)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Security,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(Dimens.iconSizeMedium),
                )
                Spacer(modifier = Modifier.width(Dimens.spaceSmall))
                Text(
                  text = stringResource(id = R.string.about_mission_title),
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
              Spacer(modifier = Modifier.height(Dimens.spaceSmall))
              Text(
                text = stringResource(id = R.string.about_mission_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // Open Source Libraries List
        item {
          Text(
            text = stringResource(id = R.string.about_libraries_header),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Spacer(modifier = Modifier.height(Dimens.spaceSmall))
          Card(
            shape = RoundedCornerShape(Dimens.cornerExtraLarge),
            border = BorderStroke(Dimens.borderThin, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLow),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(Dimens.spaceMedium)) {
              libraries.forEachIndexed { index, lib ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.spaceSmall),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Default.Code,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(Dimens.iconSizeSmall),
                    )
                    Spacer(modifier = Modifier.width(Dimens.spaceSmall))
                    Text(
                      text = stringResource(id = lib.nameRes),
                      style = MaterialTheme.typography.bodyLarge,
                      fontWeight = FontWeight.Medium,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                  Text(
                    text = stringResource(id = lib.licenseRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (index < libraries.lastIndex) {
                  HorizontalDivider(
                    thickness = Dimens.borderThin,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = Dimens.spaceExtraSmall),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = Dimens.spaceSmall),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
