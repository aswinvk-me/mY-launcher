package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.model.AppEntry
import com.example.model.FileEntry
import com.example.ui.ActiveOverlayModal
import com.example.ui.theme.LocalChronoPalette

@Composable
fun TacticalOverlayModalHost(
  activeModal: ActiveOverlayModal,
  chamferDp: Float,
  terminalLogs: List<String>,
  onExecuteTerminalCommand: (String) -> Unit,
  isAudioPlaying: Boolean,
  currentTrackIndex: Int,
  onToggleAudioPlay: () -> Unit,
  onCycleTrack: (Int) -> Unit,
  inspectedFile: FileEntry?,
  onSaveFileContent: (FileEntry, String) -> Unit,
  allApps: List<AppEntry>,
  pinnedAppIds: List<String>,
  maxDockSlots: Int,
  onTogglePinApp: (String) -> Unit,
  onAddSpeedContact: (String, String, String) -> Unit,
  onCloseModal: () -> Unit,
) {
  if (activeModal == ActiveOverlayModal.NONE) return
  BackHandler(onBack = onCloseModal)

  val palette = LocalChronoPalette.current

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .fillMaxSize()
      .background(palette.background.copy(alpha = 0.92f))
      .padding(20.dp)
      .testTag("tactical_overlay_modal")
  ) {
    ChamferedHudCard(
      chamferDp = chamferDp,
      borderColor = palette.primary,
      containerColor = palette.surface,
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 560.dp)
    ) {
      when (activeModal) {
        ActiveOverlayModal.TERMINAL_SHELL -> {
          ChronoShellTerminalModal(
            logs = terminalLogs,
            onExecute = onExecuteTerminalCommand,
            onClose = onCloseModal,
          )
        }
        ActiveOverlayModal.AUDIO_DECK -> {
          PowerampSynthDeckModal(
            isPlaying = isAudioPlaying,
            trackIndex = currentTrackIndex,
            onTogglePlay = onToggleAudioPlay,
            onCycleTrack = onCycleTrack,
            onClose = onCloseModal,
          )
        }
        ActiveOverlayModal.FILE_INSPECTOR -> {
          if (inspectedFile != null) {
            FileInspectorModal(
              file = inspectedFile,
              onSave = { updated -> onSaveFileContent(inspectedFile, updated) },
              onClose = onCloseModal,
            )
          }
        }
        ActiveOverlayModal.DOCK_SLOT_EDITOR -> {
          DockSlotEditorModal(
            allApps = allApps,
            pinnedAppIds = pinnedAppIds,
            maxSlots = maxDockSlots,
            onTogglePin = onTogglePinApp,
            onClose = onCloseModal,
          )
        }
        ActiveOverlayModal.ADD_CONTACT -> {
          AddSpeedContactModal(
            onSave = onAddSpeedContact,
            onClose = onCloseModal,
          )
        }
        ActiveOverlayModal.NONE -> Unit
      }
    }
  }
}

@Composable
private fun ChronoShellTerminalModal(
  logs: List<String>,
  onExecute: (String) -> Unit,
  onClose: () -> Unit,
) {
  val palette = LocalChronoPalette.current
  var cmdInput by remember { mutableStateOf("") }

  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    ModalHeader(title = "CHRONO.SHELL // CLI TERMINAL", onClose = onClose)

    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(220.dp)
        .background(palette.background, CutCornerShape(6.dp))
        .border(1.dp, palette.border, CutCornerShape(6.dp))
        .padding(10.dp)
    ) {
      items(logs) { line ->
        Text(
          text = line,
          style = MaterialTheme.typography.bodyMedium,
          color = if (line.startsWith("chrono@")) palette.accent else palette.primary,
        )
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      listOf("status", "ls", "theme matrix", "theme amber", "focus on").forEach { preset ->
        TacticalActionChip(
          label = preset.uppercase(),
          onClick = { onExecute(preset) },
          testTag = "term_preset_${preset.replace(" ", "_")}",
          modifier = Modifier.weight(1f)
        )
      }
    }

    OutlinedTextField(
      value = cmdInput,
      onValueChange = { cmdInput = it },
      singleLine = true,
      placeholder = {
        Text(
          text = "Enter command (help, status, ls, touch note.md)...",
          style = MaterialTheme.typography.bodyMedium,
          color = palette.dimText,
        )
      },
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
      keyboardActions = KeyboardActions(
        onSend = {
          onExecute(cmdInput)
          cmdInput = ""
        }
      ),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = palette.primary,
        unfocusedBorderColor = palette.border,
        focusedTextColor = palette.onBackground,
        unfocusedTextColor = palette.onBackground,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("terminal_command_input")
    )
  }
}

@Composable
private fun PowerampSynthDeckModal(
  isPlaying: Boolean,
  trackIndex: Int,
  onTogglePlay: () -> Unit,
  onCycleTrack: (Int) -> Unit,
  onClose: () -> Unit,
) {
  val palette = LocalChronoPalette.current
  val tracks = listOf(
    "01 // PHOSPHOR HORIZON (144 BPM) - DSP HI-RES FLAC",
    "02 // CASSETTE FUTURISM 1984 - ANALOG TAPE MASTER",
    "03 // EDGE 70 OVERDRIVE - SUB-BASS TELEMETRY",
    "04 // TERMINAL VELOCITY - ZERO LATENCY MIX",
  )

  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    ModalHeader(title = "POWERAMP // TACTICAL AUDIO DECK", onClose = onClose)

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(palette.background, CutCornerShape(8.dp))
        .border(1.dp, palette.border, CutCornerShape(8.dp))
        .padding(14.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = if (isPlaying) "STATE // ACTIVE DSP STREAM [24-BIT / 96KHZ]" else "STATE // DECK PAUSED",
          style = MaterialTheme.typography.labelSmall,
          color = palette.accent,
        )
        Text(
          text = tracks[trackIndex % tracks.size],
          style = MaterialTheme.typography.titleMedium,
          color = palette.primary,
        )
        Text(
          text = "EQ PRESET: POLED WARMTH (+4.5dB 31Hz | +3.2dB 16kHz)",
          style = MaterialTheme.typography.labelSmall,
          color = palette.dimText,
        )
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      TacticalActionChip(
        label = "<< PREV",
        onClick = { onCycleTrack(-1) },
        testTag = "audio_prev_button",
        modifier = Modifier.weight(1f)
      )
      TacticalActionChip(
        label = if (isPlaying) "|| PAUSE" else ">> ENGAGE PLAY",
        onClick = onTogglePlay,
        accentBorder = true,
        testTag = "audio_play_pause_button",
        modifier = Modifier.weight(1.4f)
      )
      TacticalActionChip(
        label = "NEXT >>",
        onClick = { onCycleTrack(1) },
        testTag = "audio_next_button",
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
private fun FileInspectorModal(
  file: FileEntry,
  onSave: (String) -> Unit,
  onClose: () -> Unit,
) {
  val palette = LocalChronoPalette.current
  var editedContent by remember(file.id) { mutableStateOf(file.previewSnippet) }

  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    ModalHeader(title = "FILE.INSPECT // ${file.displayName}", onClose = onClose)

    Text(
      text = "PATH: ${file.pathDisplay} // SIZE: ${file.formattedSize()}",
      style = MaterialTheme.typography.labelSmall,
      color = palette.dimText,
    )

    OutlinedTextField(
      value = editedContent,
      onValueChange = { editedContent = it },
      textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.onBackground),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = palette.primary,
        unfocusedBorderColor = palette.border,
        focusedContainerColor = palette.background,
        unfocusedContainerColor = palette.background,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
        .testTag("file_inspector_editor")
    )

    Row(
      horizontalArrangement = Arrangement.End,
      modifier = Modifier.fillMaxWidth()
    ) {
      if (file.localFilePath != null) {
        TacticalActionChip(
          label = "WRITE TO DISK",
          onClick = {
            onSave(editedContent)
            onClose()
          },
          accentBorder = true,
          testTag = "save_file_button",
        )
      }
    }
  }
}

@Composable
private fun DockSlotEditorModal(
  allApps: List<AppEntry>,
  pinnedAppIds: List<String>,
  maxSlots: Int,
  onTogglePin: (String) -> Unit,
  onClose: () -> Unit,
) {
  val palette = LocalChronoPalette.current
  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    ModalHeader(
      title = "PRIMARY DOCK // PINNED SLOTS (${pinnedAppIds.size}/$maxSlots)",
      onClose = onClose
    )

    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(280.dp)
    ) {
      items(allApps, key = { it.id }) { app ->
        val pinned = pinnedAppIds.contains(app.id)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .background(palette.surfaceElevated, CutCornerShape(6.dp))
            .border(
              1.dp,
              if (pinned) palette.primary else palette.border,
              CutCornerShape(6.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = app.label,
              style = MaterialTheme.typography.titleMedium,
              color = palette.onBackground,
            )
            Text(
              text = "[${app.categoryCode}] // ${app.packageName}",
              style = MaterialTheme.typography.labelSmall,
              color = palette.dimText,
            )
          }
          TacticalActionChip(
            label = if (pinned) "PINNED" else "+ PIN",
            onClick = { onTogglePin(app.id) },
            accentBorder = pinned,
            testTag = "dock_editor_toggle_${app.id}",
          )
        }
      }
    }
  }
}

@Composable
private fun AddSpeedContactModal(
  onSave: (String, String, String) -> Unit,
  onClose: () -> Unit,
) {
  val palette = LocalChronoPalette.current
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var role by remember { mutableStateOf("FIELD.OPS") }

  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    ModalHeader(title = "+ REGISTER TACTICAL CONTACT", onClose = onClose)

    OutlinedTextField(
      value = name,
      onValueChange = { name = it },
      singleLine = true,
      label = { Text("OPERATOR NAME") },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("new_contact_name_input")
    )
    OutlinedTextField(
      value = phone,
      onValueChange = { phone = it },
      singleLine = true,
      label = { Text("COMMS NUMBER (E.G. +1-555-0188)") },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("new_contact_phone_input")
    )
    OutlinedTextField(
      value = role,
      onValueChange = { role = it },
      singleLine = true,
      label = { Text("SECTOR / ROLE TAG") },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("new_contact_role_input")
    )

    Row(
      horizontalArrangement = Arrangement.End,
      modifier = Modifier.fillMaxWidth()
    ) {
      TacticalActionChip(
        label = "COMMIT TO DIRECTORY",
        onClick = { onSave(name, phone, role) },
        accentBorder = true,
        testTag = "commit_contact_button",
      )
    }
  }
}

@Composable
private fun ModalHeader(
  title: String,
  onClose: () -> Unit,
) {
  val palette = LocalChronoPalette.current
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = palette.primary,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.weight(1f)
    )
    Spacer(modifier = Modifier.width(8.dp))
    IconButton(
      onClick = onClose,
      modifier = Modifier
        .minimumInteractiveComponentSize()
        .clip(CutCornerShape(6.dp))
        .background(palette.surfaceElevated)
        .border(1.dp, palette.border, CutCornerShape(6.dp))
        .testTag("close_modal_button")
    ) {
      Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Close modal",
        tint = palette.primary,
      )
    }
  }
}
