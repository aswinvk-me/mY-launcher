package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppEntry
import com.example.model.SysConfig
import com.example.model.SysTelemetry
import com.example.ui.theme.ChronoPalette
import com.example.ui.theme.LocalChronoPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun HomeCommandScreen(
  sysConfig: SysConfig,
  telemetry: SysTelemetry,
  pinnedApps: List<AppEntry>,
  statusBanner: String,
  onSelectPalette: (ChronoPalette) -> Unit,
  onToggleFocusMode: (Boolean) -> Unit,
  onLaunchApp: (AppEntry) -> Unit,
  onOpenOmniTray: () -> Unit,
  onOpenSysConfig: () -> Unit,
  onOpenDockEditor: () -> Unit,
  onOpenTerminalQuick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalChronoPalette.current
  val edgePad = sysConfig.edgeHorizonPaddingDp.dp
  val chamfer = sysConfig.chamferRadiusDp

  var currentTimeMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      currentTimeMs = System.currentTimeMillis()
      delay(1000L)
    }
  }

  val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }
  val dateFormat = remember { SimpleDateFormat("EEE · dd MMM", Locale.US) }
  val formattedTime = remember(currentTimeMs / 1000L) { timeFormat.format(Date(currentTimeMs)) }
  val formattedDate = remember(currentTimeMs / 60000L) {
    dateFormat.format(Date(currentTimeMs)).uppercase()
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .fillMaxSize()
      .pointerInput(Unit) {
        var cumulativeUpDrag = 0f
        detectVerticalDragGestures(
          onDragStart = { cumulativeUpDrag = 0f },
          onVerticalDrag = { change, dragAmount ->
            if (dragAmount < 0f) {
              cumulativeUpDrag += -dragAmount
              if (cumulativeUpDrag > 48f) {
                change.consume()
                cumulativeUpDrag = 0f
                onOpenOmniTray()
              }
            }
          }
        )
      }
      .testTag("home_command_screen")
  ) {
    Column(
      verticalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 520.dp)
        .padding(horizontal = edgePad, vertical = 20.dp)
    ) {
      // 1. MINIMAL TOP HEADER: Digital Clock + Single-Line Telemetry + Focus Toggle
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = formattedDate,
            style = MaterialTheme.typography.labelLarge,
            color = palette.dimText,
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Minimal Hardware Focus Toggle Pill
            val focusShape = CutCornerShape((chamfer * 0.6f).dp)
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(focusShape)
                .background(
                  if (sysConfig.focusModeEnabled) palette.accent.copy(alpha = 0.16f)
                  else palette.surface
                )
                .border(
                  1.dp,
                  if (sysConfig.focusModeEnabled) palette.accent else palette.border,
                  focusShape
                )
                .clickable { onToggleFocusMode(!sysConfig.focusModeEnabled) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("focus_mode_toggle")
            ) {
              Text(
                text = if (sysConfig.focusModeEnabled) "FOCUS · ON" else "FOCUS · OFF",
                style = MaterialTheme.typography.labelSmall,
                color = if (sysConfig.focusModeEnabled) palette.accent else palette.dimText,
                fontWeight = FontWeight.Bold,
              )
            }

            IconButton(
              onClick = onOpenSysConfig,
              modifier = Modifier
                .size(36.dp)
                .testTag("open_sys_config_button")
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Open SYS.CONFIG",
                tint = palette.dimText,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        Text(
          text = formattedTime,
          style = MaterialTheme.typography.displayLarge,
          color = palette.primary,
          modifier = Modifier.testTag("retro_digital_clock")
        )

        // Single-line compact hardware telemetry whisper
        Text(
          text = "${telemetry.batteryPercent}% (${telemetry.batteryVoltageMv}mV · ${telemetry.formattedTempC})  ·  CPU ${telemetry.cpuLoadPercent}%  ·  RAM ${telemetry.ramUsagePercent}%",
          style = MaterialTheme.typography.labelSmall,
          color = palette.dimText,
        )
      }

      // 2. SPARSE PRIMARY COMMAND DOCK (Pure Text, Zero Clutter)
      Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        pinnedApps.forEachIndexed { index, app ->
          MinimalDockRow(
            slotNumber = index + 1,
            app = app,
            focusModeActive = sysConfig.focusModeEnabled,
            onClick = { onLaunchApp(app) },
          )
        }
      }

      // 3. MINIMAL BOTTOM OMNI-SEARCH TRIGGER
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .minimumInteractiveComponentSize()
          .clip(CutCornerShape(chamfer.dp))
          .border(1.dp, palette.border.copy(alpha = 0.6f), CutCornerShape(chamfer.dp))
          .clickable(onClick = onOpenOmniTray)
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .testTag("open_omni_search_bar")
      ) {
        Text(
          text = "OMNI.SEARCH",
          style = MaterialTheme.typography.labelLarge,
          color = palette.dimText,
        )
        Icon(
          imageVector = Icons.Default.KeyboardArrowUp,
          contentDescription = "Swipe up for Omni-Search",
          tint = palette.dimText,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

@Composable
private fun MinimalDockRow(
  slotNumber: Int,
  app: AppEntry,
  focusModeActive: Boolean,
  onClick: () -> Unit,
) {
  val palette = LocalChronoPalette.current
  val dimmed = focusModeActive && !app.isEssential
  val rowAlpha by animateFloatAsState(
    targetValue = if (dimmed) 0.32f else 1.0f,
    label = "minimal_dock_alpha"
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .alpha(rowAlpha)
      .clickable(onClick = onClick)
      .testTag("dock_slot_$slotNumber")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 14.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = String.format("0%d", slotNumber),
          style = MaterialTheme.typography.labelSmall,
          color = palette.dimText,
        )
        Text(
          text = app.label.uppercase(),
          style = MaterialTheme.typography.headlineMedium,
          color = palette.onBackground,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (!focusModeActive && app.badgeCount > 0) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(palette.accent, CutCornerShape(2.dp))
        )
      }
    }
    HorizontalDivider(
      thickness = 0.5.dp,
      color = palette.border.copy(alpha = 0.35f)
    )
  }
}
