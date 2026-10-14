package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class FileTypeColors(
  val pdf: Color,
  val pdfContainer: Color,
  val word: Color,
  val wordContainer: Color,
  val text: Color,
  val textContainer: Color,
  val code: Color,
  val codeContainer: Color,
)

val LocalFileTypeColors = staticCompositionLocalOf {
  FileTypeColors(
    pdf = FileTypePdfLight,
    pdfContainer = FileTypePdfContainerLight,
    word = FileTypeWordLight,
    wordContainer = FileTypeWordContainerLight,
    text = FileTypeTextLight,
    textContainer = FileTypeTextContainerLight,
    code = FileTypeCodeLight,
    codeContainer = FileTypeCodeContainerLight,
  )
}

private val OpenViewLightColorScheme = lightColorScheme(
  primary = AccentBlue,
  onPrimary = Color.White,
  primaryContainer = AccentBlueLight,
  onPrimaryContainer = Color(0xFF0F3275),
  secondary = AccentBlue,
  onSecondary = Color.White,
  secondaryContainer = AccentBlueLight,
  onSecondaryContainer = Color(0xFF0F3275),
  background = BackgroundLight,
  onBackground = TextPrimaryLight,
  surface = SurfaceLight,
  onSurface = TextPrimaryLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = TextSecondaryLight,
  outline = OutlineLight,
  outlineVariant = OutlineVariantLight,
)

private val OpenViewDarkColorScheme = darkColorScheme(
  primary = Color(0xFF6B9DFE),
  onPrimary = Color(0xFF0B1F44),
  primaryContainer = AccentBlueDark,
  onPrimaryContainer = Color(0xFFD2E3FC),
  secondary = Color(0xFF6B9DFE),
  onSecondary = Color(0xFF0B1F44),
  secondaryContainer = AccentBlueDark,
  onSecondaryContainer = Color(0xFFD2E3FC),
  background = BackgroundDark,
  onBackground = TextPrimaryDark,
  surface = SurfaceDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = TextSecondaryDark,
  outline = OutlineDark,
  outlineVariant = OutlineVariantDark,
)

private val OpenViewShapes = Shapes(
  small = RoundedCornerShape(Dimens.cornerSmall),
  medium = RoundedCornerShape(Dimens.cornerMedium),
  large = RoundedCornerShape(Dimens.cornerLarge),
  extraLarge = RoundedCornerShape(Dimens.cornerExtraLarge),
)

object OpenViewTheme {
  val fileTypeColors: FileTypeColors
    @Composable
    get() = LocalFileTypeColors.current
}

@Composable
fun OpenViewTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme: ColorScheme = if (darkTheme) OpenViewDarkColorScheme else OpenViewLightColorScheme

  val fileTypeColors = if (darkTheme) {
    FileTypeColors(
      pdf = FileTypePdfDark,
      pdfContainer = FileTypePdfContainerDark,
      word = FileTypeWordDark,
      wordContainer = FileTypeWordContainerDark,
      text = FileTypeTextDark,
      textContainer = FileTypeTextContainerDark,
      code = FileTypeCodeDark,
      codeContainer = FileTypeCodeContainerDark,
    )
  } else {
    FileTypeColors(
      pdf = FileTypePdfLight,
      pdfContainer = FileTypePdfContainerLight,
      word = FileTypeWordLight,
      wordContainer = FileTypeWordContainerLight,
      text = FileTypeTextLight,
      textContainer = FileTypeTextContainerLight,
      code = FileTypeCodeLight,
      codeContainer = FileTypeCodeContainerLight,
    )
  }

  CompositionLocalProvider(LocalFileTypeColors provides fileTypeColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = OpenViewShapes,
      content = content,
    )
  }
}
