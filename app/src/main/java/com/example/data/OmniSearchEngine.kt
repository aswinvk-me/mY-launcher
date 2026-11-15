package com.example.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.example.model.AppEntry
import com.example.model.BuiltInModule
import com.example.model.ContactEntry
import com.example.model.FileEntry
import com.example.model.OmniSearchResult
import com.example.model.SearchCategory
import java.io.File
import kotlin.system.measureNanoTime

class OmniSearchEngine(private val context: Context) {

  private val workspaceDir: File by lazy {
    File(context.filesDir, "chrono_workspace").apply {
      if (!exists()) mkdirs()
    }
  }

  init {
    ensureWorkspaceFilesSeeded()
  }

  fun ensureWorkspaceFilesSeeded() {
    val starterFiles = mapOf(
      "edge70_fusion_telemetry.spec" to """
        // MOTO EDGE 70 FUSION // HARDWARE TELEMETRY SPEC
        DISPLAY_PANEL : 6.7" pOLED 144Hz Curved Edge
        COLOR_DEPTH   : 10-bit Phosphor Calibrated
        SPRING_ENGINE : Stiffness=400.0 | Damping=28.0
        POWER_RAIL    : Li-Po 5000mAh | 68W TurboPower
        OMNI_INDEXER  : Single-Pass Sub-10ms Fuzzy Trie
      """.trimIndent(),
      "chrono_os_kernel_v17.kt" to """
        package chrono.kernel.v17
        // Zero-bloat Android 17 Launcher Kernel
        object ChronoScheduler {
          const val TARGET_REFRESH_HZ = 144
          const val FRAME_BUDGET_MS = 6.94f
          fun verifyRealtimeLock(): Boolean = true
        }
      """.trimIndent(),
      "poweramp_eq_poled.dsp" to """
        [POWERAMP_DSP_PRESET // SYNTHWAVE_84]
        PREAMP_DB = +2.5
        BAND_31HZ = +4.5dB (Sub-Bass Warmth)
        BAND_62HZ = +3.0dB (Analog Punch)
        BAND_4KHZ = +1.8dB (Crisp Lead Synth)
        BAND_16KHZ = +3.2dB (Air / Tape Shimmer)
      """.trimIndent(),
      "tactical_hotkey_manual.md" to """
        # CHRONO-OS // 70 OPERATOR MANUAL
        1. SWIPE UP anywhere on Home Screen to engage OMNI-SEARCH.
        2. Type "pwr" to fuzzy-match Poweramp // Audio Deck.
        3. Toggle FOCUS.LOCK on Home to suppress notifications & dim non-essentials.
        4. OpenCHRONO.SHELL to run live diagnostic and file creation commands.
      """.trimIndent(),
      "field_deployment_log.txt" to """
        LOG_STAMP: 2026-11-14 // SECTOR 70
        STATUS: ALL TELEMETRY NOMINAL
        LATENCY: 0.34ms SINGLE-PASS QUERY
        BATTERY: 4.185V // 31.2C THERMALS OPTIMAL
      """.trimIndent(),
    )

    starterFiles.forEach { (fileName, body) ->
      val target = File(workspaceDir, fileName)
      if (!target.exists()) {
        runCatching { target.writeText(body) }
      }
    }
  }

  fun createWorkspaceFile(fileName: String, content: String): FileEntry? {
    val cleanName = fileName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
      .ifBlank { "note_${System.currentTimeMillis()}.txt" }
    val finalName = if (cleanName.contains(".")) cleanName else "$cleanName.txt"
    val target = File(workspaceDir, finalName)
    return runCatching {
      target.writeText(content)
      fileToEntry(target)
    }.getOrNull()
  }

  fun loadInstalledAndCoreApps(): List<AppEntry> {
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
      addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val resolvedList = runCatching {
      pm.queryIntentActivities(mainIntent, 0)
    }.getOrDefault(emptyList())

    val ownPkg = context.packageName
    val installedApps = resolvedList
      .mapNotNull { info ->
        val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
        if (pkg == ownPkg) return@mapNotNull null
        val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
        if (label.isBlank()) return@mapNotNull null

        val lower = label.lowercase()
        val isEssential = lower.contains("phone") ||
          lower.contains("dial") ||
          lower.contains("message") ||
          lower.contains("camera") ||
          lower.contains("settings") ||
          lower.contains("clock")

        AppEntry(
          id = "pkg:$pkg",
          label = label,
          packageName = pkg,
          categoryCode = inferCategoryTag(label, pkg),
          isEssential = isEssential,
          badgeCount = (pkg.hashCode().and(0x7FFFFFFF) % 4),
          builtInModule = BuiltInModule.NONE,
          isRealInstalledApp = true,
        )
      }
      .distinctBy { it.packageName }

    // Core Command Dock & Tactical Built-in / Hybrid Modules
    val coreCommands = listOf(
      AppEntry(
        id = "cmd.phone",
        label = "Phone // Comms Dialer",
        packageName = "chrono.module.dialer",
        categoryCode = "COMMS",
        isEssential = true,
        badgeCount = 0,
        builtInModule = BuiltInModule.DIAL_PAD,
        isRealInstalledApp = false,
      ),
      AppEntry(
        id = "cmd.terminal",
        label = "Terminal // Chrono Shell",
        packageName = "chrono.module.shell",
        categoryCode = "SHELL",
        isEssential = true,
        badgeCount = 0,
        builtInModule = BuiltInModule.TERMINAL_SHELL,
        isRealInstalledApp = false,
      ),
      AppEntry(
        id = "cmd.poweramp",
        label = "Poweramp // Audio Deck",
        packageName = "com.maxmpz.audioplayer",
        categoryCode = "AUDIO",
        isEssential = true,
        badgeCount = 2,
        builtInModule = BuiltInModule.AUDIO_DECK,
        isRealInstalledApp = false,
      ),
      AppEntry(
        id = "cmd.browser",
        label = "Browser // Net Uplink",
        packageName = "chrono.module.browser",
        categoryCode = "NET",
        isEssential = false,
        badgeCount = 3,
        builtInModule = BuiltInModule.BROWSER_INTENT,
        isRealInstalledApp = false,
      ),
      AppEntry(
        id = "cmd.messages",
        label = "Messages // SMS Relay",
        packageName = "chrono.module.messages",
        categoryCode = "SMS",
        isEssential = true,
        badgeCount = 1,
        builtInModule = BuiltInModule.MESSAGES_INTENT,
        isRealInstalledApp = false,
      ),
      AppEntry(
        id = "cmd.sysconfig",
        label = "SYS.CONFIG // Engine",
        packageName = "chrono.module.sysconfig",
        categoryCode = "KERNEL",
        isEssential = true,
        badgeCount = 0,
        builtInModule = BuiltInModule.SYS_CONFIG,
        isRealInstalledApp = false,
      ),
    )

    return (coreCommands + installedApps).sortedBy { it.label.lowercase() }
  }

  fun loadLocalAndSafFiles(serializedSafEntries: Set<String>): List<FileEntry> {
    ensureWorkspaceFilesSeeded()
    val workspaceFiles = workspaceDir.listFiles()
      ?.filter { it.isFile }
      ?.map { fileToEntry(it) }
      .orEmpty()

    val safFiles = serializedSafEntries.mapNotNull { entry ->
      val parts = entry.split("|")
      if (parts.size >= 4) {
        val uriStr = parts[0]
        val name = parts[1]
        val size = parts[2].toLongOrNull() ?: 1024L
        val ext = parts[3]
        FileEntry(
          id = "saf:$uriStr",
          displayName = name,
          pathDisplay = "SAF://EXTERNAL/$name",
          sizeBytes = size,
          extensionTag = ext,
          uriString = uriStr,
          previewSnippet = "External Storage Access Framework Document ($ext)",
          lastModifiedEpochMs = System.currentTimeMillis(),
        )
      } else {
        null
      }
    }

    return (workspaceFiles + safFiles).sortedBy { it.displayName.lowercase() }
  }

  fun resolveSafUriMetadata(uri: Uri): String? {
    return runCatching {
      var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "document.dat"
      var sizeBytes = 4096L
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
          if (nameIdx >= 0) {
            displayName = cursor.getString(nameIdx) ?: displayName
          }
          if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
            sizeBytes = cursor.getLong(sizeIdx)
          }
        }
      }
      val ext = displayName.substringAfterLast('.', "DAT").uppercase().take(6)
      "$uri|$displayName|$sizeBytes|$ext"
    }.getOrNull()
  }

  fun loadContacts(customContacts: List<ContactEntry>): List<ContactEntry> {
    val systemContacts = mutableListOf<ContactEntry>()
    val hasPerm = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    if (hasPerm) {
      runCatching {
        val projection = arrayOf(
          ContactsContract.CommonDataKinds.Phone._ID,
          ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
          ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        context.contentResolver.query(
          ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
          projection,
          null,
          null,
          "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
          val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
          val nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
          val numCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
          while (cursor.moveToNext() && systemContacts.size < 150) {
            val id = if (idCol >= 0) cursor.getString(idCol).orEmpty() else ""
            val name = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
            val number = if (numCol >= 0) cursor.getString(numCol).orEmpty() else ""
            if (name.isNotBlank() && number.isNotBlank()) {
              systemContacts.add(
                ContactEntry(
                  id = "sys:$id:$number",
                  displayName = name,
                  phoneNumber = number,
                  roleTag = "DEVICE.CNT",
                  isSystemContact = true,
                )
              )
            }
          }
        }
      }
    }

    val starterTacticalContacts = listOf(
      ContactEntry(
        id = "cnt.ops.1",
        displayName = "Aria Vance",
        phoneNumber = "+1-555-0144",
        roleTag = "ENG",
        isSystemContact = false,
      ),
      ContactEntry(
        id = "cnt.ops.2",
        displayName = "Kaelen Voss",
        phoneNumber = "+1-555-0170",
        roleTag = "DSP",
        isSystemContact = false,
      ),
      ContactEntry(
        id = "cnt.ops.3",
        displayName = "Soren Mercer",
        phoneNumber = "+1-555-0199",
        roleTag = "OPS",
        isSystemContact = false,
      ),
    )

    return (systemContacts + customContacts + starterTacticalContacts)
      .distinctBy { "${it.displayName.lowercase()}|${it.phoneNumber}" }
  }

  /**
   * Single-pass indexing & fuzzy filter across Apps, Files, and Contacts with sub-10ms latency.
   */
  fun executeSinglePassSearch(
    rawQuery: String,
    category: SearchCategory,
    allApps: List<AppEntry>,
    allFiles: List<FileEntry>,
    allContacts: List<ContactEntry>,
    hiddenAppIds: Set<String>,
  ): OmniSearchResult {
    val q = rawQuery.trim()
    var matchedApps = emptyList<AppEntry>()
    var matchedFiles = emptyList<FileEntry>()
    var matchedContacts = emptyList<ContactEntry>()
    var exactApp: AppEntry? = null

    val elapsedNs = measureNanoTime {
      val visibleApps = allApps.filterNot { hiddenAppIds.contains(it.id) }

      if (q.isEmpty()) {
        if (category == SearchCategory.ALL || category == SearchCategory.APPS) {
          matchedApps = visibleApps
        }
        if (category == SearchCategory.ALL || category == SearchCategory.FILES) {
          matchedFiles = allFiles
        }
        if (category == SearchCategory.ALL || category == SearchCategory.CONTACTS) {
          matchedContacts = allContacts
        }
      } else {
        if (category == SearchCategory.ALL || category == SearchCategory.APPS) {
          matchedApps = visibleApps.mapNotNull { app ->
            val primaryMatch = computeFuzzyMatch(q, app.label)
            val secondaryMatch = computeFuzzyMatch(q, app.categoryCode)
            val bestScore = maxOf(primaryMatch.score, secondaryMatch.score - 15)
            if (bestScore > 0) {
              app.copy(
                matchScore = bestScore,
                matchedIndices = if (primaryMatch.score >= secondaryMatch.score - 15) {
                  primaryMatch.indices
                } else {
                  emptyList()
                }
              )
            } else {
              null
            }
          }.sortedByDescending { it.matchScore }

          exactApp = matchedApps.firstOrNull { app ->
            val primaryName = app.label.substringBefore("//").trim()
            primaryName.equals(q, ignoreCase = true) ||
              app.label.equals(q, ignoreCase = true) ||
              (q.equals("pwr", ignoreCase = true) && app.id == "cmd.poweramp") ||
              (q.equals("term", ignoreCase = true) && app.id == "cmd.terminal")
          }
        }

        if (category == SearchCategory.ALL || category == SearchCategory.FILES) {
          matchedFiles = allFiles.mapNotNull { file ->
            val nameMatch = computeFuzzyMatch(q, file.displayName)
            val extMatch = computeFuzzyMatch(q, file.extensionTag)
            val bestScore = maxOf(nameMatch.score, extMatch.score - 10)
            if (bestScore > 0) {
              file.copy(
                matchScore = bestScore,
                matchedIndices = nameMatch.indices
              )
            } else {
              null
            }
          }.sortedByDescending { it.matchScore }
        }

        if (category == SearchCategory.ALL || category == SearchCategory.CONTACTS) {
          matchedContacts = allContacts.mapNotNull { cnt ->
            val nameMatch = computeFuzzyMatch(q, cnt.displayName)
            val numMatch = computeFuzzyMatch(q, cnt.phoneNumber)
            val roleMatch = computeFuzzyMatch(q, cnt.roleTag)
            val bestScore = maxOf(nameMatch.score, numMatch.score - 5, roleMatch.score - 12)
            if (bestScore > 0) {
              cnt.copy(
                matchScore = bestScore,
                matchedIndices = nameMatch.indices
              )
            } else {
              null
            }
          }.sortedByDescending { it.matchScore }
        }
      }
    }

    return OmniSearchResult(
      query = q,
      category = category,
      apps = matchedApps,
      files = matchedFiles,
      contacts = matchedContacts,
      executionTimeMicros = (elapsedNs / 1000L).coerceAtLeast(18L),
      exactMatchApp = exactApp,
    )
  }

  private data class FuzzyScore(val score: Int, val indices: List<Int>)

  /**
   * Computes subsequence fuzzy score and character highlight indices.
   * Example: query "pwr" against "Poweramp // Audio Deck" -> matches indices [0, 2, 4] with high score.
   */
  private fun computeFuzzyMatch(query: String, target: String): FuzzyScore {
    if (query.isBlank() || target.isBlank()) return FuzzyScore(0, emptyList())
    val qLower = query.lowercase()
    val tLower = target.lowercase()

    // Direct prefix match
    if (tLower.startsWith(qLower)) {
      return FuzzyScore(200 + (40 - target.length).coerceAtLeast(0), (qLower.indices).toList())
    }

    // Direct substring match
    val subIndex = tLower.indexOf(qLower)
    if (subIndex >= 0) {
      val bonus = if (subIndex == 0 || !tLower[subIndex - 1].isLetterOrDigit()) 150 else 115
      return FuzzyScore(bonus, (subIndex until (subIndex + qLower.length)).toList())
    }

    // Subsequence fuzzy match (e.g. "pwr" -> P-o-w-e-r-a-m-p)
    val indices = ArrayList<Int>(qLower.length)
    var qIdx = 0
    var score = 60
    var prevMatchIdx = -2

    for (tIdx in tLower.indices) {
      if (qIdx < qLower.length && tLower[tIdx] == qLower[qIdx]) {
        indices.add(tIdx)
        // Consecutive character bonus
        if (tIdx == prevMatchIdx + 1) {
          score += 18
        }
        // Word boundary / camelCase / initial letter bonus
        if (tIdx == 0 || !tLower[tIdx - 1].isLetterOrDigit()) {
          score += 24
        }
        // Compactness penalty for wide gaps
        if (prevMatchIdx >= 0 && (tIdx - prevMatchIdx) > 4) {
          score -= (tIdx - prevMatchIdx)
        }
        prevMatchIdx = tIdx
        qIdx++
      }
    }

    return if (qIdx == qLower.length) {
      FuzzyScore(score.coerceAtLeast(15), indices)
    } else {
      FuzzyScore(0, emptyList())
    }
  }

  private fun fileToEntry(file: File): FileEntry {
    val ext = file.extension.uppercase().ifBlank { "DAT" }
    val snippet = runCatching {
      file.readLines().take(6).joinToString("\n")
    }.getOrDefault("")
    return FileEntry(
      id = "file:${file.name}",
      displayName = file.name,
      pathDisplay = "/chrono_workspace/${file.name}",
      sizeBytes = file.length().coerceAtLeast(128L),
      extensionTag = ext,
      localFilePath = file.absolutePath,
      previewSnippet = snippet,
      lastModifiedEpochMs = file.lastModified(),
    )
  }

  private fun inferCategoryTag(label: String, pkg: String): String {
    val combined = "$label $pkg".lowercase()
    return when {
      combined.contains("music") || combined.contains("audio") || combined.contains("poweramp") -> "AUDIO"
      combined.contains("phone") || combined.contains("dial") || combined.contains("contact") -> "COMMS"
      combined.contains("message") || combined.contains("sms") || combined.contains("chat") -> "MSG"
      combined.contains("browser") || combined.contains("chrome") || combined.contains("web") -> "NET"
      combined.contains("camera") || combined.contains("photo") || combined.contains("gallery") -> "OPTIC"
      combined.contains("setting") || combined.contains("system") -> "SYS"
      combined.contains("file") || combined.contains("doc") -> "FS"
      else -> "EXEC"
    }
  }
}
