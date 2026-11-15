package com.example.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalChronoPalette = staticCompositionLocalOf { ChronoPalette.AMBER_PHOSPHOR }

@Composable
fun ChronoOSTheme(
  palette: ChronoPalette = ChronoPalette.AMBER_PHOSPHOR,
  useShareTechDisplay: Boolean = true,
  fontScale: Float = 1.0f,
  chamferRadiusDp: Float = 10f,
  content: @Composable () -> Unit,
) {
  val colorScheme = darkColorScheme(
    primary = palette.primary,
    onPrimary = palette.background,
    primaryContainer = palette.surfaceElevated,
    onPrimaryContainer = palette.primary,
    secondary = palette.secondary,
    onSecondary = palette.background,
    secondaryContainer = palette.surface,
    onSecondaryContainer = palette.secondary,
    tertiary = palette.accent,
    onTertiary = palette.background,
    background = palette.background,
    onBackground = palette.onBackground,
    surface = palette.surface,
    onSurface = palette.onBackground,
    surfaceVariant = palette.surfaceElevated,
    onSurfaceVariant = palette.dimText,
    outline = palette.border,
    error = palette.statusAlert,
  )

  val cut = chamferRadiusDp.coerceIn(0f, 24f).dp
  val smallCut = (chamferRadiusDp * 0.6f).coerceIn(0f, 16f).dp
  val largeCut = (chamferRadiusDp * 1.4f).coerceIn(0f, 28f).dp

  val shapes = Shapes(
    extraSmall = CutCornerShape(smallCut),
    small = CutCornerShape(smallCut),
    medium = CutCornerShape(cut),
    large = CutCornerShape(largeCut),
    extraLarge = CutCornerShape(largeCut),
  )

  CompositionLocalProvider(LocalChronoPalette provides palette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = buildChronoTypography(
        useShareTechDisplay = useShareTechDisplay,
        fontScale = fontScale,
      ),
      shapes = shapes,
      content = content,
    )
  }
}

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  ChronoOSTheme(content = content)
}
