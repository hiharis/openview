// Copyright 2026 hiharis | Licensed under Apache 2.0 | github.com/hiharis/openview
package io.github.hiharis.openview.ui.theme

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

@Immutable
data class SyntaxColors(
  val keyword: Color,
  val string: Color,
  val comment: Color,
  val number: Color,
  val gutterBackground: Color,
  val gutterText: Color,
  val searchHighlight: Color,
  val searchHighlightCurrent: Color,
)

val LocalSyntaxColors = staticCompositionLocalOf {
  SyntaxColors(
    keyword = SyntaxKeywordLight,
    string = SyntaxStringLight,
    comment = SyntaxCommentLight,
    number = SyntaxNumberLight,
    gutterBackground = GutterBackgroundLight,
    gutterText = GutterTextLight,
    searchHighlight = SearchHighlightLight,
    searchHighlightCurrent = SearchHighlightCurrentLight,
  )
}

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

private val OpenViewRedColorScheme = lightColorScheme(
  primary = AccentRed,
  onPrimary = Color.White,
  primaryContainer = AccentRedLight,
  onPrimaryContainer = Color(0xFFB71C1C),
  secondary = AccentRed,
  onSecondary = Color.White,
  secondaryContainer = AccentRedLight,
  onSecondaryContainer = Color(0xFFB71C1C),
  background = BackgroundRed,
  onBackground = TextPrimaryRed,
  surface = SurfaceRed,
  onSurface = TextPrimaryRed,
  surfaceVariant = SurfaceVariantRed,
  onSurfaceVariant = TextSecondaryRed,
  outline = OutlineRed,
  outlineVariant = OutlineVariantRed,
)

private val OpenViewForestColorScheme = darkColorScheme(
  primary = AccentForest,
  onPrimary = Color(0xFF022C22),
  primaryContainer = AccentForestContainer,
  onPrimaryContainer = Color(0xFFA7F3D0),
  secondary = Color(0xFF34D399),
  onSecondary = Color(0xFF022C22),
  secondaryContainer = Color(0xFF065F46),
  onSecondaryContainer = Color(0xFFD1FAE5),
  background = BackgroundForest,
  onBackground = TextPrimaryForest,
  surface = SurfaceForest,
  onSurface = TextPrimaryForest,
  surfaceVariant = SurfaceVariantForest,
  onSurfaceVariant = TextSecondaryForest,
  outline = OutlineForest,
  outlineVariant = OutlineVariantForest,
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

  val syntaxColors: SyntaxColors
    @Composable
    get() = LocalSyntaxColors.current
}

@Composable
fun OpenViewTheme(
  themeMode: io.github.hiharis.openview.ui.settings.AppThemeMode? = null,
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val activeMode = themeMode ?: if (darkTheme) {
    io.github.hiharis.openview.ui.settings.AppThemeMode.DARK
  } else {
    io.github.hiharis.openview.ui.settings.AppThemeMode.LIGHT
  }

  val colorScheme: ColorScheme = when (activeMode) {
    io.github.hiharis.openview.ui.settings.AppThemeMode.LIGHT -> OpenViewLightColorScheme
    io.github.hiharis.openview.ui.settings.AppThemeMode.DARK -> OpenViewDarkColorScheme
    io.github.hiharis.openview.ui.settings.AppThemeMode.RED -> OpenViewRedColorScheme
    io.github.hiharis.openview.ui.settings.AppThemeMode.FOREST -> OpenViewForestColorScheme
  }

  val isDarkSurface = activeMode == io.github.hiharis.openview.ui.settings.AppThemeMode.DARK ||
    activeMode == io.github.hiharis.openview.ui.settings.AppThemeMode.FOREST

  val fileTypeColors = if (isDarkSurface) {
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

  val syntaxColors = if (isDarkSurface) {
    SyntaxColors(
      keyword = SyntaxKeywordDark,
      string = SyntaxStringDark,
      comment = SyntaxCommentDark,
      number = SyntaxNumberDark,
      gutterBackground = if (activeMode == io.github.hiharis.openview.ui.settings.AppThemeMode.FOREST) SurfaceForest else GutterBackgroundDark,
      gutterText = if (activeMode == io.github.hiharis.openview.ui.settings.AppThemeMode.FOREST) TextSecondaryForest else GutterTextDark,
      searchHighlight = SearchHighlightDark,
      searchHighlightCurrent = SearchHighlightCurrentDark,
    )
  } else {
    SyntaxColors(
      keyword = SyntaxKeywordLight,
      string = SyntaxStringLight,
      comment = SyntaxCommentLight,
      number = SyntaxNumberLight,
      gutterBackground = if (activeMode == io.github.hiharis.openview.ui.settings.AppThemeMode.RED) SurfaceVariantRed else GutterBackgroundLight,
      gutterText = if (activeMode == io.github.hiharis.openview.ui.settings.AppThemeMode.RED) TextSecondaryRed else GutterTextLight,
      searchHighlight = SearchHighlightLight,
      searchHighlightCurrent = SearchHighlightCurrentLight,
    )
  }

  CompositionLocalProvider(
    LocalFileTypeColors provides fileTypeColors,
    LocalSyntaxColors provides syntaxColors,
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = OpenViewShapes,
      content = content,
    )
  }
}
