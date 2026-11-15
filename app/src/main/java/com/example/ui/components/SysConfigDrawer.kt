package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.SysConfig
import com.example.ui.theme.ChronoPalette
import com.example.ui.theme.LocalChronoPalette
import kotlin.math.roundToInt

@Composable
fun SysConfigDrawer(
  sysConfig: SysConfig,
  onSelectPalette: (ChronoPalette) -> Unit,
  onToggleShareTechFont: (Boolean) -> Unit,
  onFontScaleChange: (Float) -> Unit,
  onScanlineOpacityChange: (Float) -> Unit,
  onChamferRadiusChange: (Float) -> Unit,
  onSpringPhysicsChange: (Float, Float) -> Unit,
  onEdgePaddingChange: (Float) -> Unit,
  onMaxDockSlotsChange: (Int) -> Unit,
  onAutoLaunchExactChange: (Boolean) -> Unit,
  onHapticEnabledChange: (Boolean) -> Unit,
  onEdgeGlowEnabledChange: (Boolean) -> Unit,
  onRestoreHiddenApps: () -> Unit,
  onOpenDefaultHomeSelector: () -> Unit,
  onResetDefaults: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onClose)

  val palette = LocalChronoPalette.current
  val chamfer = sysConfig.chamferRadiusDp

  Box(
    contentAlignment = Alignment.TopCenter,
    modifier = modifier
      .fillMaxSize()
      .background(palette.background)
      .testTag("sys_config_drawer")
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 520.dp)
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .verticalScroll(rememberScrollState())
    ) {
      // Minimal Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "SYS.CONFIG",
          style = MaterialTheme.typography.headlineMedium,
          color = palette.primary,
        )
        IconButton(
          onClick = onClose,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("close_sys_config_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close SYS.CONFIG",
            tint = palette.dimText,
          )
        }
      }

      HorizontalDivider(thickness = 0.5.dp, color = palette.border)

      // 1. Hot-Swappable Palette Bar
      Text(
        text = "PALETTE",
        style = MaterialTheme.typography.labelSmall,
        color = palette.dimText,
      )
      PaletteQuickSwitcherBar(
        activePalette = sysConfig.palette,
        onSelectPalette = onSelectPalette,
        chamferDp = chamfer,
      )

      HorizontalDivider(thickness = 0.5.dp, color = palette.border)

      // 2. Core Minimal Sliders
      ConfigSliderControl(
        label = "SPRING STIFFNESS",
        valueText = "${sysConfig.springStiffness.roundToInt()}",
        value = sysConfig.springStiffness,
        valueRange = 200f..800f,
        onValueChange = { onSpringPhysicsChange(it, sysConfig.springDamping) },
      )

      ConfigSliderControl(
        label = "CRT SCANLINES",
        valueText = "${(sysConfig.scanlineOpacity * 100).roundToInt()}%",
        value = sysConfig.scanlineOpacity,
        valueRange = 0f..0.50f,
        onValueChange = onScanlineOpacityChange,
      )

      ConfigSliderControl(
        label = "CHAMFER CUT",
        valueText = "${sysConfig.chamferRadiusDp.roundToInt()} DP",
        value = sysConfig.chamferRadiusDp,
        valueRange = 0f..18f,
        onValueChange = onChamferRadiusChange,
      )

      ConfigSliderControl(
        label = "FONT SCALE",
        valueText = String.format("%.2fx", sysConfig.fontScale),
        value = sysConfig.fontScale,
        valueRange = 0.85f..1.20f,
        onValueChange = onFontScaleChange,
      )

      HorizontalDivider(thickness = 0.5.dp, color = palette.border)

      // 3. Minimal Toggles
      HardwareToggleSwitch(
        checked = sysConfig.autoLaunchExactMatch,
        onCheckedChange = onAutoLaunchExactChange,
        label = "AUTO-LAUNCH EXACT MATCH",
        statusOn = "ON",
        statusOff = "OFF",
        testTag = "toggle_auto_launch",
        modifier = Modifier.fillMaxWidth()
      )

      HardwareToggleSwitch(
        checked = sysConfig.hapticFeedbackEnabled,
        onCheckedChange = onHapticEnabledChange,
        label = "HAPTIC TICKS",
        statusOn = "ON",
        statusOff = "OFF",
        testTag = "toggle_haptics",
        modifier = Modifier.fillMaxWidth()
      )

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        TacticalActionChip(
          label = "SET DEFAULT HOME",
          onClick = onOpenDefaultHomeSelector,
          accentBorder = true,
          testTag = "set_default_home_button",
          modifier = Modifier.weight(1f)
        )
        TacticalActionChip(
          label = "RESET",
          onClick = onResetDefaults,
          testTag = "reset_sys_config_button",
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun ConfigSliderControl(
  label: String,
  valueText: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
) {
  val palette = LocalChronoPalette.current
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = palette.dimText,
      )
      Text(
        text = valueText,
        style = MaterialTheme.typography.labelSmall,
        color = palette.primary,
        fontWeight = FontWeight.Bold,
      )
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      colors = SliderDefaults.colors(
        thumbColor = palette.accent,
        activeTrackColor = palette.primary,
        inactiveTrackColor = palette.border,
      ),
      modifier = Modifier.fillMaxWidth()
    )
  }
}
