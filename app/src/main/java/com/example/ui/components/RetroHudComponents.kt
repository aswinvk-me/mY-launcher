package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ChronoPalette
import com.example.ui.theme.LocalChronoPalette

@Composable
fun CrtScanlineAndEdgeOverlay(
  scanlineOpacity: Float,
  edgeGlowEnabled: Boolean,
  focusModeActive: Boolean,
  modifier: Modifier = Modifier,
) {
  val palette = LocalChronoPalette.current
  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    // 1. Subtle CRT horizontal scanline overlay
    if (scanlineOpacity > 0.01f) {
      val lineStepPx = 4.dp.toPx().coerceAtLeast(3f)
      val strokePx = 1.dp.toPx().coerceAtLeast(1f)
      val scanColor = Color.Black.copy(alpha = (scanlineOpacity * 0.55f).coerceIn(0f, 0.45f))
      val phosphorTint = palette.primary.copy(alpha = (scanlineOpacity * 0.06f).coerceIn(0f, 0.08f))

      var y = 0f
      while (y < h) {
        drawLine(
          color = scanColor,
          start = Offset(0f, y),
          end = Offset(w, y),
          strokeWidth = strokePx,
        )
        y += lineStepPx
      }
      drawRect(color = phosphorTint)
    }

    // 2. Moto Edge 70 Fusion Curved Display Horizon Glow
    if (edgeGlowEnabled) {
      val edgeWidth = 14.dp.toPx()
      val glowColor = if (focusModeActive) {
        palette.accent.copy(alpha = 0.22f)
      } else {
        palette.primary.copy(alpha = 0.18f)
      }

      drawRect(
        brush = Brush.horizontalGradient(
          colors = listOf(glowColor, Color.Transparent),
          startX = 0f,
          endX = edgeWidth,
        ),
        topLeft = Offset(0f, 0f),
        size = Size(edgeWidth, h),
      )
      drawRect(
        brush = Brush.horizontalGradient(
          colors = listOf(Color.Transparent, glowColor),
          startX = w - edgeWidth,
          endX = w,
        ),
        topLeft = Offset(w - edgeWidth, 0f),
        size = Size(edgeWidth, h),
      )
    }
  }
}

@Composable
fun ChamferedHudCard(
  modifier: Modifier = Modifier,
  chamferDp: Float = 10f,
  borderColor: Color = LocalChronoPalette.current.border,
  containerColor: Color = LocalChronoPalette.current.surface,
  onClick: (() -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val shape = CutCornerShape(chamferDp.coerceIn(0f, 24f).dp)
  val baseModifier = modifier
    .clip(shape)
    .background(containerColor, shape)
    .border(BorderStroke(1.dp, borderColor), shape)

  val clickableModifier = if (onClick != null) {
    baseModifier.clickable(onClick = onClick)
  } else {
    baseModifier
  }

  Box(modifier = clickableModifier) {
    content()
  }
}

@Composable
fun HardwareToggleSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  label: String,
  statusOn: String = "ENGAGED",
  statusOff: String = "STANDBY",
  testTag: String = "hardware_toggle_switch",
  modifier: Modifier = Modifier,
) {
  val palette = LocalChronoPalette.current
  val thumbOffset by animateDpAsState(
    targetValue = if (checked) 26.dp else 2.dp,
    animationSpec = spring(
      stiffness = Spring.StiffnessMediumLow,
      dampingRatio = Spring.DampingRatioNoBouncy
    ),
    label = "toggle_thumb_offset"
  )
  val trackBorder by animateColorAsState(
    targetValue = if (checked) palette.accent else palette.border,
    label = "toggle_border"
  )
  val thumbColor by animateColorAsState(
    targetValue = if (checked) palette.accent else palette.dimText,
    label = "toggle_thumb_color"
  )

  Surface(
    color = palette.surfaceElevated,
    shape = CutCornerShape(6.dp),
    border = BorderStroke(1.dp, trackBorder),
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clip(CutCornerShape(6.dp))
      .clickable { onCheckedChange(!checked) }
      .testTag(testTag)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Column(modifier = Modifier.weight(1f, fill = false)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          color = palette.primary,
        )
        Text(
          text = if (checked) statusOn else statusOff,
          style = MaterialTheme.typography.labelSmall,
          color = if (checked) palette.accent else palette.dimText,
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      // Physical hardware rail slot
      Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
          .width(52.dp)
          .height(24.dp)
          .clip(CutCornerShape(4.dp))
          .background(palette.background)
          .border(1.dp, trackBorder, CutCornerShape(4.dp))
      ) {
        Box(
          modifier = Modifier
            .offset(x = thumbOffset)
            .size(width = 22.dp, height = 18.dp)
            .clip(CutCornerShape(3.dp))
            .background(thumbColor)
        )
      }
    }
  }
}

@Composable
fun FuzzyHighlightedText(
  text: String,
  matchedIndices: List<Int>,
  style: TextStyle = MaterialTheme.typography.titleMedium,
  normalColor: Color = LocalChronoPalette.current.onBackground,
  highlightColor: Color = LocalChronoPalette.current.accent,
  maxLines: Int = 1,
  modifier: Modifier = Modifier,
) {
  if (matchedIndices.isEmpty()) {
    Text(
      text = text,
      style = style,
      color = normalColor,
      maxLines = maxLines,
      overflow = TextOverflow.Ellipsis,
      modifier = modifier,
    )
    return
  }

  val matchSet = matchedIndices.toHashSet()
  val annotated = buildAnnotatedString {
    text.forEachIndexed { idx, ch ->
      if (matchSet.contains(idx)) {
        withStyle(
          SpanStyle(
            color = highlightColor,
            fontWeight = FontWeight.ExtraBold,
            textDecoration = TextDecoration.Underline,
            background = highlightColor.copy(alpha = 0.18f),
          )
        ) {
          append(ch)
        }
      } else {
        withStyle(SpanStyle(color = normalColor)) {
          append(ch)
        }
      }
    }
  }

  Text(
    text = annotated,
    style = style,
    maxLines = maxLines,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier,
  )
}

@Composable
fun PaletteQuickSwitcherBar(
  activePalette: ChronoPalette,
  onSelectPalette: (ChronoPalette) -> Unit,
  chamferDp: Float,
  modifier: Modifier = Modifier,
) {
  val palette = LocalChronoPalette.current
  Row(
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.fillMaxWidth()
  ) {
    ChronoPalette.entries.forEach { option ->
      val isSelected = option == activePalette
      val shape = CutCornerShape((chamferDp * 0.6f).coerceIn(2f, 12f).dp)
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .weight(1f)
          .height(36.dp)
          .clip(shape)
          .background(
            if (isSelected) option.primary.copy(alpha = 0.2f) else palette.surface
          )
          .border(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) option.primary else palette.border,
            shape = shape,
          )
          .clickable { onSelectPalette(option) }
          .testTag("palette_chip_${option.id}")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.padding(horizontal = 4.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(option.primary, CutCornerShape(2.dp))
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = option.id.uppercase().take(6),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) option.primary else palette.dimText,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
fun TacticalActionChip(
  label: String,
  onClick: () -> Unit,
  testTag: String,
  accentBorder: Boolean = false,
  chamferDp: Dp = 6.dp,
  modifier: Modifier = Modifier,
) {
  val palette = LocalChronoPalette.current
  val shape = CutCornerShape(chamferDp)
  val borderCol = if (accentBorder) palette.accent else palette.border
  val textCol = if (accentBorder) palette.accent else palette.primary

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .minimumInteractiveComponentSize()
      .clip(shape)
      .background(palette.surfaceElevated, shape)
      .border(1.dp, borderCol, shape)
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag(testTag)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = textCol,
      fontWeight = FontWeight.Bold,
    )
  }
}
