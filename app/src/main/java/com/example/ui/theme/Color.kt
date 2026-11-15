package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class ChronoPalette(
  val id: String,
  val displayName: String,
  val codeName: String,
  val primary: Color,
  val secondary: Color,
  val accent: Color,
  val background: Color,
  val surface: Color,
  val surfaceElevated: Color,
  val onBackground: Color,
  val dimText: Color,
  val border: Color,
  val statusAlert: Color,
) {
  AMBER_PHOSPHOR(
    id = "amber",
    displayName = "AMBER PHOSPHOR",
    codeName = "P3-CRT // #FFB000",
    primary = Color(0xFFFFB000),
    secondary = Color(0xFFFF8800),
    accent = Color(0xFFFFD54F),
    background = Color(0xFF050505),
    surface = Color(0xFF0E0B05),
    surfaceElevated = Color(0xFF181207),
    onBackground = Color(0xFFFFB000),
    dimText = Color(0xFF9E6D00),
    border = Color(0xFF5A3E00),
    statusAlert = Color(0xFFFF3D00),
  ),
  CYBER_MATRIX(
    id = "matrix",
    displayName = "CYBER MATRIX",
    codeName = "VT-100 // #00FF66",
    primary = Color(0xFF00FF66),
    secondary = Color(0xFF00C853),
    accent = Color(0xFF69F0AE),
    background = Color(0xFF040805),
    surface = Color(0xFF09140D),
    surfaceElevated = Color(0xFF0F2216),
    onBackground = Color(0xFF00FF66),
    dimText = Color(0xFF008E39),
    border = Color(0xFF005221),
    statusAlert = Color(0xFFFFEA00),
  ),
  SYNTHWAVE_HORIZON(
    id = "synthwave",
    displayName = "SYNTHWAVE HORIZON",
    codeName = "OUTRUN // #00F0FF",
    primary = Color(0xFF00F0FF),
    secondary = Color(0xFFFF0055),
    accent = Color(0xFFFF0055),
    background = Color(0xFF05040A),
    surface = Color(0xFF0D0A1A),
    surfaceElevated = Color(0xFF17112B),
    onBackground = Color(0xFFE0FCFF),
    dimText = Color(0xFF0095A8),
    border = Color(0xFF3E1854),
    statusAlert = Color(0xFFFF0055),
  ),
  NOTHING_INDUSTRIAL(
    id = "industrial",
    displayName = "NOTHING / INDUSTRIAL",
    codeName = "TE-01 // #F5F5F5",
    primary = Color(0xFFF5F5F5),
    secondary = Color(0xFFBDBDBD),
    accent = Color(0xFFFF2A2A),
    background = Color(0xFF050505),
    surface = Color(0xFF111111),
    surfaceElevated = Color(0xFF1C1C1C),
    onBackground = Color(0xFFF5F5F5),
    dimText = Color(0xFF8C8C8C),
    border = Color(0xFF333333),
    statusAlert = Color(0xFFFF2A2A),
  );

  companion object {
    fun fromId(id: String): ChronoPalette = entries.find { it.id == id } ?: AMBER_PHOSPHOR
  }
}
