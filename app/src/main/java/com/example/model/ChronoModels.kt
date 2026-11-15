package com.example.model

import com.example.ui.theme.ChronoPalette

enum class SearchCategory(val label: String, val tag: String) {
  ALL("ALL", "[ALL]"),
  APPS("APPS", "[APP]"),
  FILES("FILES", "[FILE]"),
  CONTACTS("CONTACTS", "[CNT]"),
}

enum class BuiltInModule {
  NONE,
  TERMINAL_SHELL,
  AUDIO_DECK,
  SYS_CONFIG,
  DIAL_PAD,
  BROWSER_INTENT,
  MESSAGES_INTENT,
}

data class AppEntry(
  val id: String,
  val label: String,
  val packageName: String,
  val categoryCode: String,
  val isEssential: Boolean = false,
  val badgeCount: Int = 0,
  val builtInModule: BuiltInModule = BuiltInModule.NONE,
  val isRealInstalledApp: Boolean = false,
  val matchScore: Int = 0,
  val matchedIndices: List<Int> = emptyList(),
)

data class FileEntry(
  val id: String,
  val displayName: String,
  val pathDisplay: String,
  val sizeBytes: Long,
  val extensionTag: String,
  val uriString: String? = null,
  val localFilePath: String? = null,
  val previewSnippet: String = "",
  val lastModifiedEpochMs: Long = 0L,
  val matchScore: Int = 0,
  val matchedIndices: List<Int> = emptyList(),
) {
  fun formattedSize(): String {
    return when {
      sizeBytes >= 1024 * 1024 -> String.format("%.2f MB", sizeBytes / (1024f * 1024f))
      sizeBytes >= 1024 -> String.format("%.1f KB", sizeBytes / 1024f)
      else -> "$sizeBytes B"
    }
  }
}

data class ContactEntry(
  val id: String,
  val displayName: String,
  val phoneNumber: String,
  val roleTag: String,
  val isSystemContact: Boolean = false,
  val matchScore: Int = 0,
  val matchedIndices: List<Int> = emptyList(),
)

data class OmniSearchResult(
  val query: String = "",
  val category: SearchCategory = SearchCategory.ALL,
  val apps: List<AppEntry> = emptyList(),
  val files: List<FileEntry> = emptyList(),
  val contacts: List<ContactEntry> = emptyList(),
  val executionTimeMicros: Long = 0L,
  val exactMatchApp: AppEntry? = null,
) {
  val totalMatches: Int
    get() = apps.size + files.size + contacts.size

  val formattedLatencyMs: String
    get() = String.format("%.2f ms", executionTimeMicros / 1000f)
}

data class SysTelemetry(
  val cpuLoadPercent: Int = 14,
  val cpuHistory: List<Int> = listOf(12, 15, 11, 18, 14, 16, 13, 15),
  val ramUsedMb: Long = 3840L,
  val ramTotalMb: Long = 8192L,
  val batteryPercent: Int = 88,
  val batteryVoltageMv: Int = 4185,
  val batteryTempTenthsC: Int = 312,
  val isCharging: Boolean = false,
  val displayRefreshHz: Int = 144,
  val uptimeFormatted: String = "04:18:22",
  val storageFreeGb: Float = 112.4f,
) {
  val formattedTempC: String
    get() = String.format("%.1f°C", batteryTempTenthsC / 10f)

  val formattedVoltageV: String
    get() = String.format("%.3fV (%dmV)", batteryVoltageMv / 1000f, batteryVoltageMv)

  val ramUsagePercent: Int
    get() = if (ramTotalMb > 0) ((ramUsedMb * 100L) / ramTotalMb).toInt().coerceIn(1, 99) else 45
}

data class SysConfig(
  val palette: ChronoPalette = ChronoPalette.AMBER_PHOSPHOR,
  val useShareTechDisplay: Boolean = true,
  val fontScale: Float = 1.0f,
  val scanlineOpacity: Float = 0.12f,
  val chamferRadiusDp: Float = 8f,
  val springStiffness: Float = 400f,
  val springDamping: Float = 28f,
  val focusModeEnabled: Boolean = false,
  val autoLaunchExactMatch: Boolean = false,
  val edgeHorizonPaddingDp: Float = 24f,
  val maxDockSlots: Int = 5,
  val hapticFeedbackEnabled: Boolean = true,
  val edgeGlowEnabled: Boolean = false,
  val pinnedAppIds: List<String> = listOf(
    "cmd.phone",
    "cmd.terminal",
    "cmd.poweramp",
    "cmd.browser",
    "cmd.messages",
  ),
  val hiddenAppIds: Set<String> = emptySet(),
)
