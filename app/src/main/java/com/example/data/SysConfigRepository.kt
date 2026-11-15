package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ContactEntry
import com.example.model.SysConfig
import com.example.ui.theme.ChronoPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chronoDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "chrono_os_70_prefs"
)

class SysConfigRepository(private val context: Context) {

  private object Keys {
    val PALETTE_ID = stringPreferencesKey("palette_id")
    val USE_SHARE_TECH = booleanPreferencesKey("use_share_tech")
    val FONT_SCALE = floatPreferencesKey("font_scale")
    val SCANLINE_OPACITY = floatPreferencesKey("scanline_opacity")
    val CHAMFER_RADIUS = floatPreferencesKey("chamfer_radius_dp")
    val SPRING_STIFFNESS = floatPreferencesKey("spring_stiffness")
    val SPRING_DAMPING = floatPreferencesKey("spring_damping")
    val FOCUS_MODE = booleanPreferencesKey("focus_mode_enabled")
    val AUTO_LAUNCH_EXACT = booleanPreferencesKey("auto_launch_exact")
    val EDGE_PADDING = floatPreferencesKey("edge_padding_dp")
    val MAX_DOCK_SLOTS = intPreferencesKey("max_dock_slots")
    val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
    val EDGE_GLOW_ENABLED = booleanPreferencesKey("edge_glow_enabled")
    val PINNED_APP_IDS = stringPreferencesKey("pinned_app_ids_csv")
    val HIDDEN_APP_IDS = stringSetPreferencesKey("hidden_app_ids")
    val INDEXED_SAF_FILES = stringSetPreferencesKey("indexed_saf_files")
    val CUSTOM_CONTACTS = stringSetPreferencesKey("custom_speed_contacts")
  }

  val sysConfigFlow: Flow<SysConfig> = context.chronoDataStore.data.map { prefs ->
    val defaultPinned = listOf(
      "cmd.phone",
      "cmd.terminal",
      "cmd.poweramp",
      "cmd.browser",
      "cmd.messages",
    )
    val pinnedCsv = prefs[Keys.PINNED_APP_IDS]
    val pinnedList = if (pinnedCsv.isNullOrBlank()) {
      defaultPinned
    } else {
      pinnedCsv.split("|").filter { it.isNotBlank() }
    }

    SysConfig(
      palette = ChronoPalette.fromId(prefs[Keys.PALETTE_ID] ?: ChronoPalette.AMBER_PHOSPHOR.id),
      useShareTechDisplay = prefs[Keys.USE_SHARE_TECH] ?: true,
      fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
      scanlineOpacity = prefs[Keys.SCANLINE_OPACITY] ?: 0.12f,
      chamferRadiusDp = prefs[Keys.CHAMFER_RADIUS] ?: 8f,
      springStiffness = prefs[Keys.SPRING_STIFFNESS] ?: 400f,
      springDamping = prefs[Keys.SPRING_DAMPING] ?: 28f,
      focusModeEnabled = prefs[Keys.FOCUS_MODE] ?: false,
      autoLaunchExactMatch = prefs[Keys.AUTO_LAUNCH_EXACT] ?: false,
      edgeHorizonPaddingDp = prefs[Keys.EDGE_PADDING] ?: 24f,
      maxDockSlots = (prefs[Keys.MAX_DOCK_SLOTS] ?: 5).coerceIn(5, 7),
      hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
      edgeGlowEnabled = prefs[Keys.EDGE_GLOW_ENABLED] ?: false,
      pinnedAppIds = pinnedList,
      hiddenAppIds = prefs[Keys.HIDDEN_APP_IDS] ?: emptySet(),
    )
  }

  val indexedSafEntriesFlow: Flow<Set<String>> = context.chronoDataStore.data.map { prefs ->
    prefs[Keys.INDEXED_SAF_FILES] ?: emptySet()
  }

  val customContactsFlow: Flow<List<ContactEntry>> = context.chronoDataStore.data.map { prefs ->
    val rawSet = prefs[Keys.CUSTOM_CONTACTS] ?: emptySet()
    rawSet.mapNotNull { serialized ->
      val parts = serialized.split("|")
      if (parts.size >= 4) {
        ContactEntry(
          id = parts[0],
          displayName = parts[1],
          phoneNumber = parts[2],
          roleTag = parts[3],
          isSystemContact = false,
        )
      } else {
        null
      }
    }
  }

  suspend fun setPalette(palette: ChronoPalette) {
    context.chronoDataStore.edit { it[Keys.PALETTE_ID] = palette.id }
  }

  suspend fun setUseShareTech(useShareTech: Boolean) {
    context.chronoDataStore.edit { it[Keys.USE_SHARE_TECH] = useShareTech }
  }

  suspend fun setFontScale(scale: Float) {
    context.chronoDataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.25f) }
  }

  suspend fun setScanlineOpacity(opacity: Float) {
    context.chronoDataStore.edit { it[Keys.SCANLINE_OPACITY] = opacity.coerceIn(0f, 0.65f) }
  }

  suspend fun setChamferRadius(radiusDp: Float) {
    context.chronoDataStore.edit { it[Keys.CHAMFER_RADIUS] = radiusDp.coerceIn(0f, 22f) }
  }

  suspend fun setSpringPhysics(stiffness: Float, damping: Float) {
    context.chronoDataStore.edit {
      it[Keys.SPRING_STIFFNESS] = stiffness.coerceIn(180f, 900f)
      it[Keys.SPRING_DAMPING] = damping.coerceIn(14f, 48f)
    }
  }

  suspend fun setFocusMode(enabled: Boolean) {
    context.chronoDataStore.edit { it[Keys.FOCUS_MODE] = enabled }
  }

  suspend fun setAutoLaunchExactMatch(enabled: Boolean) {
    context.chronoDataStore.edit { it[Keys.AUTO_LAUNCH_EXACT] = enabled }
  }

  suspend fun setEdgePadding(paddingDp: Float) {
    context.chronoDataStore.edit { it[Keys.EDGE_PADDING] = paddingDp.coerceIn(12f, 36f) }
  }

  suspend fun setMaxDockSlots(slots: Int) {
    context.chronoDataStore.edit { it[Keys.MAX_DOCK_SLOTS] = slots.coerceIn(5, 7) }
  }

  suspend fun setHapticEnabled(enabled: Boolean) {
    context.chronoDataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
  }

  suspend fun setEdgeGlowEnabled(enabled: Boolean) {
    context.chronoDataStore.edit { it[Keys.EDGE_GLOW_ENABLED] = enabled }
  }

  suspend fun togglePinApp(appId: String, maxSlots: Int) {
    context.chronoDataStore.edit { prefs ->
      val currentCsv = prefs[Keys.PINNED_APP_IDS]
      val current = if (currentCsv.isNullOrBlank()) {
        mutableListOf(
          "cmd.phone",
          "cmd.terminal",
          "cmd.poweramp",
          "cmd.browser",
          "cmd.messages",
          "cmd.sysconfig",
        )
      } else {
        currentCsv.split("|").filter { it.isNotBlank() }.toMutableList()
      }
      if (current.contains(appId)) {
        if (current.size > 1) current.remove(appId)
      } else {
        if (current.size >= maxSlots) {
          current.removeAt(current.lastIndex)
        }
        current.add(appId)
      }
      prefs[Keys.PINNED_APP_IDS] = current.joinToString("|")
    }
  }

  suspend fun toggleHideApp(appId: String) {
    context.chronoDataStore.edit { prefs ->
      val current = (prefs[Keys.HIDDEN_APP_IDS] ?: emptySet()).toMutableSet()
      if (current.contains(appId)) {
        current.remove(appId)
      } else {
        current.add(appId)
      }
      prefs[Keys.HIDDEN_APP_IDS] = current
    }
  }

  suspend fun clearHiddenApps() {
    context.chronoDataStore.edit { prefs ->
      prefs[Keys.HIDDEN_APP_IDS] = emptySet()
    }
  }

  suspend fun addIndexedSafEntry(serializedEntry: String) {
    context.chronoDataStore.edit { prefs ->
      val current = (prefs[Keys.INDEXED_SAF_FILES] ?: emptySet()).toMutableSet()
      current.add(serializedEntry)
      prefs[Keys.INDEXED_SAF_FILES] = current
    }
  }

  suspend fun addCustomContact(name: String, phone: String, role: String) {
    context.chronoDataStore.edit { prefs ->
      val current = (prefs[Keys.CUSTOM_CONTACTS] ?: emptySet()).toMutableSet()
      val id = "cnt.custom.${System.currentTimeMillis()}"
      val cleanName = name.replace("|", " ").trim()
      val cleanPhone = phone.replace("|", " ").trim()
      val cleanRole = role.replace("|", " ").trim().uppercase()
      current.add("$id|$cleanName|$cleanPhone|$cleanRole")
      prefs[Keys.CUSTOM_CONTACTS] = current
    }
  }

  suspend fun resetToFactoryDefaults() {
    context.chronoDataStore.edit { it.clear() }
  }
}
