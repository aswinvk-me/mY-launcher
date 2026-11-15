package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val JetBrainsMonoFamily = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal),
  Font(R.font.jetbrains_mono, FontWeight.Medium),
  Font(R.font.jetbrains_mono, FontWeight.Bold),
)

val ShareTechMonoFamily = FontFamily(
  Font(R.font.share_tech_mono, FontWeight.Normal),
  Font(R.font.share_tech_mono, FontWeight.Medium),
  Font(R.font.share_tech_mono, FontWeight.Bold),
)

fun buildChronoTypography(
  useShareTechDisplay: Boolean = true,
  fontScale: Float = 1.0f,
): Typography {
  val displayFamily = if (useShareTechDisplay) ShareTechMonoFamily else JetBrainsMonoFamily
  val bodyFamily = JetBrainsMonoFamily
  val s = fontScale.coerceIn(0.85f, 1.30f)

  return Typography(
    displayLarge = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (54f * s).sp,
      lineHeight = (58f * s).sp,
      letterSpacing = 1.5.sp,
    ),
    displayMedium = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (36f * s).sp,
      lineHeight = (40f * s).sp,
      letterSpacing = 1.2.sp,
    ),
    headlineLarge = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (24f * s).sp,
      lineHeight = (28f * s).sp,
      letterSpacing = 1.2.sp,
    ),
    headlineMedium = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.Medium,
      fontSize = (18f * s).sp,
      lineHeight = (22f * s).sp,
      letterSpacing = 1.sp,
    ),
    titleLarge = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (16f * s).sp,
      lineHeight = (20f * s).sp,
      letterSpacing = 1.1.sp,
    ),
    titleMedium = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (14f * s).sp,
      lineHeight = (18f * s).sp,
      letterSpacing = 0.8.sp,
    ),
    bodyLarge = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (14f * s).sp,
      lineHeight = (20f * s).sp,
      letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Normal,
      fontSize = (12f * s).sp,
      lineHeight = (16f * s).sp,
      letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
      fontFamily = displayFamily,
      fontWeight = FontWeight.Bold,
      fontSize = (12f * s).sp,
      lineHeight = (16f * s).sp,
      letterSpacing = 1.2.sp,
    ),
    labelSmall = TextStyle(
      fontFamily = bodyFamily,
      fontWeight = FontWeight.Medium,
      fontSize = (10f * s).sp,
      lineHeight = (13f * s).sp,
      letterSpacing = 1.sp,
    ),
  )
}
