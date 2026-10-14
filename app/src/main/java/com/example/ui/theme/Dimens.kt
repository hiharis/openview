package com.example.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized dimensions and spacing tokens for OpenView.
 * Optimized for readability, accessibility, and elderly-friendly 56dp minimum touch targets.
 */
object Dimens {
  // Spacing (Generous spacing for clarity)
  val spaceNone: Dp = 0.dp
  val spaceExtraSmall: Dp = 4.dp
  val spaceSmall: Dp = 8.dp
  val spaceMedium: Dp = 16.dp
  val spaceLarge: Dp = 24.dp
  val spaceExtraLarge: Dp = 32.dp
  val spaceHuge: Dp = 48.dp

  // Component Corner Radii (16 to 24dp rounded corners)
  val cornerSmall: Dp = 12.dp
  val cornerMedium: Dp = 16.dp
  val cornerLarge: Dp = 20.dp
  val cornerExtraLarge: Dp = 24.dp
  val cornerPill: Dp = 32.dp

  // Border & Outlines
  val borderThin: Dp = 1.dp
  val borderFocus: Dp = 2.dp

  // Elevation (Subtle shadows for clear card depth)
  val elevationNone: Dp = 0.dp
  val elevationLow: Dp = 2.dp
  val elevationMedium: Dp = 4.dp

  // Elderly-Friendly Minimum Touch Target (at least 56dp)
  val minTouchTarget: Dp = 56.dp

  // Icon Sizes
  val iconSizeSmall: Dp = 20.dp
  val iconSizeMedium: Dp = 26.dp
  val iconSizeLarge: Dp = 32.dp
  val iconTileSize: Dp = 56.dp
  val heroIconSize: Dp = 64.dp

  // Maximum content width for responsive layouts (tablets/foldables)
  val maxContentWidth: Dp = 680.dp
}
