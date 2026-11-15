package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.model.AppEntry
import com.example.model.ContactEntry
import com.example.model.FileEntry
import com.example.model.OmniSearchResult
import com.example.model.SearchCategory
import com.example.model.SysConfig
import com.example.ui.theme.LocalChronoPalette
import kotlinx.coroutines.delay

@Composable
fun OmniSearchTray(
  searchResult: OmniSearchResult,
  sysConfig: SysConfig,
  onQueryChange: (String) -> Unit,
  onSelectCategory: (SearchCategory) -> Unit,
  onLaunchApp: (AppEntry) -> Unit,
  onTogglePinApp: (String) -> Unit,
  onToggleHideApp: (String) -> Unit,
  onInspectFile: (FileEntry) -> Unit,
  onPickSafFiles: () -> Unit,
  onSyncContactsPermission: () -> Unit,
  onOpenAddContactModal: () -> Unit,
  onDialContact: (ContactEntry) -> Unit,
  onSmsContact: (ContactEntry) -> Unit,
  onWhatsAppContact: (ContactEntry) -> Unit,
  onCloseTray: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onCloseTray)

  val palette = LocalChronoPalette.current
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current
  val chamfer = sysConfig.chamferRadiusDp
  val edgePad = sysConfig.edgeHorizonPaddingDp.dp

  LaunchedEffect(Unit) {
    delay(80L)
    runCatching {
      focusRequester.requestFocus()
      keyboardController?.show()
    }
  }

  Box(
    contentAlignment = Alignment.TopCenter,
    modifier = modifier
      .fillMaxSize()
      .background(palette.background)
      .testTag("omni_search_tray")
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 520.dp)
        .padding(horizontal = edgePad, vertical = 16.dp)
    ) {
      // 1. Minimal Search Bar + Close Icon
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = searchResult.query,
          onValueChange = onQueryChange,
          singleLine = true,
          textStyle = MaterialTheme.typography.titleLarge.copy(color = palette.onBackground),
          placeholder = {
            Text(
              text = "SEARCH APPS, FILES, CONTACTS...",
              style = MaterialTheme.typography.bodyMedium,
              color = palette.dimText,
            )
          },
          trailingIcon = {
            if (searchResult.query.isNotEmpty()) {
              IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.testTag("clear_search_query_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Clear search",
                  tint = palette.dimText,
                )
              }
            } else {
              Text(
                text = searchResult.formattedLatencyMs,
                style = MaterialTheme.typography.labelSmall,
                color = palette.dimText,
                modifier = Modifier.padding(end = 10.dp)
              )
            }
          },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
          keyboardActions = KeyboardActions(
            onGo = {
              val topApp = searchResult.exactMatchApp ?: searchResult.apps.firstOrNull()
              if (topApp != null) onLaunchApp(topApp)
            }
          ),
          shape = CutCornerShape(chamfer.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.primary,
            unfocusedBorderColor = palette.border,
            focusedContainerColor = palette.surface,
            unfocusedContainerColor = palette.surface,
            cursorColor = palette.accent,
          ),
          modifier = Modifier
            .weight(1f)
            .focusRequester(focusRequester)
            .testTag("omni_search_input")
        )

        IconButton(
          onClick = onCloseTray,
          modifier = Modifier
            .size(48.dp)
            .testTag("close_omni_tray_button")
        ) {
          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Close Tray",
            tint = palette.dimText,
          )
        }
      }

      // 2. Minimal Category Filter Pills (ALL, APPS, FILES, CONTACTS)
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        SearchCategory.entries.forEach { category ->
          val isSelected = searchResult.category == category
          val pillShape = CutCornerShape((chamfer * 0.6f).dp)

          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .clip(pillShape)
              .background(
                if (isSelected) palette.primary else palette.surface,
                pillShape
              )
              .border(
                width = 1.dp,
                color = if (isSelected) palette.primary else palette.border.copy(alpha = 0.5f),
                shape = pillShape
              )
              .clickable { onSelectCategory(category) }
              .padding(vertical = 8.dp)
              .testTag("filter_pill_${category.name.lowercase()}")
          ) {
            Text(
              text = category.label,
              style = MaterialTheme.typography.labelSmall,
              color = if (isSelected) palette.background else palette.dimText,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      // 3. Clean Single-Pass Results Stream
      LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("omni_results_list")
      ) {
        // APPS ([APP])
        itemsIndexed(
          items = searchResult.apps,
          key = { _, app -> "app_${app.id}" }
        ) { index, app ->
          StaggeredCascadeItem(index = index) {
            MinimalAppRow(
              app = app,
              onLaunch = { onLaunchApp(app) },
            )
          }
        }

        // FILES ([FILE])
        itemsIndexed(
          items = searchResult.files,
          key = { _, file -> "file_${file.id}" }
        ) { index, file ->
          StaggeredCascadeItem(index = index) {
            MinimalFileRow(
              file = file,
              onInspect = { onInspectFile(file) },
            )
          }
        }

        // CONTACTS ([CNT])
        itemsIndexed(
          items = searchResult.contacts,
          key = { _, cnt -> "cnt_${cnt.id}" }
        ) { index, contact ->
          StaggeredCascadeItem(index = index) {
            MinimalContactRow(
              contact = contact,
              onCall = { onDialContact(contact) },
              onSms = { onSmsContact(contact) },
              onWhatsApp = { onWhatsAppContact(contact) },
            )
          }
        }

        if (searchResult.totalMatches == 0) {
          item(key = "empty_state") {
            Text(
              text = "NO MATCHES",
              style = MaterialTheme.typography.labelLarge,
              color = palette.dimText,
              modifier = Modifier.padding(vertical = 24.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StaggeredCascadeItem(
  index: Int,
  content: @Composable () -> Unit,
) {
  var visible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    val delayMs = (index.coerceAtMost(8) * 12L)
    if (delayMs > 0) delay(delayMs)
    visible = true
  }

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(animationSpec = tween(durationMillis = 120)) +
      slideInVertically(
        initialOffsetY = { it / 5 },
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f)
      )
  ) {
    content()
  }
}

@Composable
private fun MinimalAppRow(
  app: AppEntry,
  onLaunch: () -> Unit,
) {
  val palette = LocalChronoPalette.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onLaunch)
      .testTag("app_result_${app.id}")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 13.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = "[APP]",
          style = MaterialTheme.typography.labelSmall,
          color = palette.dimText,
        )
        FuzzyHighlightedText(
          text = app.label,
          matchedIndices = app.matchedIndices,
          style = MaterialTheme.typography.titleMedium,
          normalColor = palette.onBackground,
          highlightColor = palette.accent,
        )
      }
    }
    HorizontalDivider(thickness = 0.5.dp, color = palette.border.copy(alpha = 0.28f))
  }
}

@Composable
private fun MinimalFileRow(
  file: FileEntry,
  onInspect: () -> Unit,
) {
  val palette = LocalChronoPalette.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onInspect)
      .testTag("file_result_${file.displayName}")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 13.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = "[FILE]",
          style = MaterialTheme.typography.labelSmall,
          color = palette.secondary,
        )
        FuzzyHighlightedText(
          text = file.displayName,
          matchedIndices = file.matchedIndices,
          style = MaterialTheme.typography.titleMedium,
          normalColor = palette.onBackground,
          highlightColor = palette.accent,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = file.formattedSize(),
        style = MaterialTheme.typography.labelSmall,
        color = palette.dimText,
      )
    }
    HorizontalDivider(thickness = 0.5.dp, color = palette.border.copy(alpha = 0.28f))
  }
}

@Composable
private fun MinimalContactRow(
  contact: ContactEntry,
  onCall: () -> Unit,
  onSms: () -> Unit,
  onWhatsApp: () -> Unit,
) {
  val palette = LocalChronoPalette.current
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("contact_result_${contact.id}")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 11.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        Text(
          text = "[CNT]",
          style = MaterialTheme.typography.labelSmall,
          color = palette.accent,
        )
        FuzzyHighlightedText(
          text = contact.displayName,
          matchedIndices = contact.matchedIndices,
          style = MaterialTheme.typography.titleMedium,
          normalColor = palette.onBackground,
          highlightColor = palette.accent,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        TacticalActionChip(
          label = "CALL",
          onClick = onCall,
          accentBorder = true,
          testTag = "contact_call_${contact.id}",
        )
        TacticalActionChip(
          label = "SMS",
          onClick = onSms,
          testTag = "contact_sms_${contact.id}",
        )
        TacticalActionChip(
          label = "WA",
          onClick = onWhatsApp,
          testTag = "contact_wa_${contact.id}",
        )
      }
    }
    HorizontalDivider(thickness = 0.5.dp, color = palette.border.copy(alpha = 0.28f))
  }
}
