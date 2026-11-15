package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OmniSearchEngine
import com.example.data.SysConfigRepository
import com.example.data.TelemetryMonitor
import com.example.model.AppEntry
import com.example.model.BuiltInModule
import com.example.model.ContactEntry
import com.example.model.FileEntry
import com.example.model.OmniSearchResult
import com.example.model.SearchCategory
import com.example.model.SysConfig
import com.example.model.SysTelemetry
import com.example.ui.theme.ChronoPalette
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ActiveOverlayModal {
  NONE,
  TERMINAL_SHELL,
  AUDIO_DECK,
  FILE_INSPECTOR,
  ADD_CONTACT,
  DOCK_SLOT_EDITOR,
}

class ChronoViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = SysConfigRepository(application)
  private val telemetryMonitor = TelemetryMonitor(application)
  private val searchEngine = OmniSearchEngine(application)

  val sysConfig: StateFlow<SysConfig> = repository.sysConfigFlow.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = SysConfig()
  )

  val telemetry: StateFlow<SysTelemetry> = telemetryMonitor.observeTelemetry().stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = telemetryMonitor.readSnapshot()
  )

  private val _isAppTrayOpen = MutableStateFlow(false)
  val isAppTrayOpen: StateFlow<Boolean> = _isAppTrayOpen.asStateFlow()

  private val _isSysConfigOpen = MutableStateFlow(false)
  val isSysConfigOpen: StateFlow<Boolean> = _isSysConfigOpen.asStateFlow()

  private val _activeModal = MutableStateFlow(ActiveOverlayModal.NONE)
  val activeModal: StateFlow<ActiveOverlayModal> = _activeModal.asStateFlow()

  private val _inspectedFile = MutableStateFlow<FileEntry?>(null)
  val inspectedFile: StateFlow<FileEntry?> = _inspectedFile.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategory = MutableStateFlow(SearchCategory.ALL)
  val selectedCategory: StateFlow<SearchCategory> = _selectedCategory.asStateFlow()

  private val _allApps = MutableStateFlow<List<AppEntry>>(emptyList())
  val allApps: StateFlow<List<AppEntry>> = _allApps.asStateFlow()

  private val _allFiles = MutableStateFlow<List<FileEntry>>(emptyList())
  private val _allContacts = MutableStateFlow<List<ContactEntry>>(emptyList())

  private val _statusBannerMessage = MutableStateFlow("SYSTEM READY // 144HZ LOCKED")
  val statusBannerMessage: StateFlow<String> = _statusBannerMessage.asStateFlow()

  // Terminal Shell History State
  private val _terminalLogs = MutableStateFlow(
    listOf(
      "CHRONO-OS // 70 KERNEL v17.4 INITIALIZED",
      "CPU ARCH: ARMv9 // DISPLAY: 144Hz pOLED",
      "TYPE 'help' FOR AVAILABLE TACTICAL COMMANDS.",
    )
  )
  val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

  // Audio Deck State (for built-in Poweramp // Synth Deck module)
  private val _isAudioPlaying = MutableStateFlow(false)
  val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

  private val _currentTrackIndex = MutableStateFlow(0)
  val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

  val omniSearchResult: StateFlow<OmniSearchResult> = combine(
    _searchQuery,
    _selectedCategory,
    _allApps,
    _allFiles,
    combine(_allContacts, sysConfig) { contacts, cfg -> contacts to cfg }
  ) { query, category, apps, files, (contacts, cfg) ->
    searchEngine.executeSinglePassSearch(
      rawQuery = query,
      category = category,
      allApps = apps,
      allFiles = files,
      allContacts = contacts,
      hiddenAppIds = cfg.hiddenAppIds,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = OmniSearchResult()
  )

  val pinnedDockApps: StateFlow<List<AppEntry>> = combine(_allApps, sysConfig) { apps, cfg ->
    val byId = apps.associateBy { it.id }
    val resolved = cfg.pinnedAppIds.mapNotNull { byId[it] }.take(cfg.maxDockSlots)
    if (resolved.isNotEmpty()) {
      resolved
    } else {
      apps.take(cfg.maxDockSlots)
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  init {
    refreshDataStreams()
    viewModelScope.launch {
      repository.indexedSafEntriesFlow.collect { safSet ->
        _allFiles.value = searchEngine.loadLocalAndSafFiles(safSet)
      }
    }
    viewModelScope.launch {
      repository.customContactsFlow.collect { customList ->
        _allContacts.value = searchEngine.loadContacts(customList)
      }
    }
  }

  fun refreshDataStreams() {
    viewModelScope.launch(Dispatchers.IO) {
      _allApps.value = searchEngine.loadInstalledAndCoreApps()
      _allFiles.value = searchEngine.loadLocalAndSafFiles(emptySet())
      _allContacts.value = searchEngine.loadContacts(emptyList())
    }
  }

  fun onContactsPermissionResult(granted: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      val custom = repository.customContactsFlow
      _allContacts.value = searchEngine.loadContacts(emptyList())
      postStatus(if (granted) "CONTACTS.DB SYNCED // LIVE" else "CONTACTS PERMISSION DENIED // USING LOCAL SPEED-DIAL")
    }
  }

  fun openAppTray() {
    triggerHapticTick(isHeavy = false)
    _isAppTrayOpen.value = true
  }

  fun closeAppTray() {
    triggerHapticTick(isHeavy = false)
    _isAppTrayOpen.value = false
    _searchQuery.value = ""
  }

  fun openSysConfig() {
    triggerHapticTick(isHeavy = true)
    _isSysConfigOpen.value = true
  }

  fun closeSysConfig() {
    triggerHapticTick(isHeavy = false)
    _isSysConfigOpen.value = false
  }

  fun closeActiveModal() {
    triggerHapticTick(isHeavy = false)
    _activeModal.value = ActiveOverlayModal.NONE
    _inspectedFile.value = null
  }

  fun openModal(modal: ActiveOverlayModal) {
    triggerHapticTick(isHeavy = false)
    _activeModal.value = modal
  }

  fun updateSearchQuery(newQuery: String) {
    _searchQuery.value = newQuery
    triggerHapticTick(isHeavy = false)

    val cfg = sysConfig.value
    if (cfg.autoLaunchExactMatch && newQuery.trim().length >= 3) {
      val result = searchEngine.executeSinglePassSearch(
        rawQuery = newQuery,
        category = _selectedCategory.value,
        allApps = _allApps.value,
        allFiles = _allFiles.value,
        allContacts = _allContacts.value,
        hiddenAppIds = cfg.hiddenAppIds,
      )
      result.exactMatchApp?.let { exact ->
        postStatus("AUTO-EXEC EXACT MATCH // ${exact.label.uppercase()}")
        launchAppEntry(exact)
      }
    }
  }

  fun selectCategory(category: SearchCategory) {
    triggerHapticTick(isHeavy = false)
    _selectedCategory.value = category
  }

  fun launchAppEntry(app: AppEntry) {
    triggerHapticTick(isHeavy = true)
    val ctx = getApplication<Application>()

    when (app.builtInModule) {
      BuiltInModule.TERMINAL_SHELL -> {
        _activeModal.value = ActiveOverlayModal.TERMINAL_SHELL
        postStatus("EXEC // CHRONO.SHELL v17.4")
        return
      }
      BuiltInModule.AUDIO_DECK -> {
        // Check if real Poweramp is installed; otherwise open the built-in Poweramp // Synth Deck
        val launchIntent = ctx.packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
          launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          runCatching { ctx.startActivity(launchIntent) }
        } else {
          _activeModal.value = ActiveOverlayModal.AUDIO_DECK
          postStatus("EXEC // POWERAMP SYNTH DECK")
        }
        return
      }
      BuiltInModule.SYS_CONFIG -> {
        _isSysConfigOpen.value = true
        postStatus("EXEC // SYS.CONFIG ENGINE")
        return
      }
      BuiltInModule.DIAL_PAD -> {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
          ctx.startActivity(dialIntent)
          postStatus("EXEC // SYSTEM DIALER")
        }.onFailure {
          postStatus("DIALER INTENT DISPATCHED")
        }
        return
      }
      BuiltInModule.BROWSER_INTENT -> {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
          ctx.startActivity(webIntent)
          postStatus("EXEC // NET UPLINK BROWSER")
        }.onFailure {
          postStatus("BROWSER UPLINK READY")
        }
        return
      }
      BuiltInModule.MESSAGES_INTENT -> {
        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
          ctx.startActivity(smsIntent)
          postStatus("EXEC // SMS RELAY")
        }.onFailure {
          postStatus("SMS RELAY DISPATCHED")
        }
        return
      }
      BuiltInModule.NONE -> {
        val intent = ctx.packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          runCatching {
            ctx.startActivity(intent)
            postStatus("LAUNCHED // ${app.label.uppercase()}")
          }.onFailure {
            postStatus("ERR LAUNCHING ${app.packageName}")
          }
        } else {
          postStatus("MODULE READY // ${app.label.uppercase()}")
        }
      }
    }
  }

  fun inspectOrOpenFile(file: FileEntry) {
    triggerHapticTick(isHeavy = false)
    if (file.localFilePath != null) {
      val diskFile = File(file.localFilePath)
      val fullText = runCatching { diskFile.readText() }.getOrDefault(file.previewSnippet)
      _inspectedFile.value = file.copy(previewSnippet = fullText)
      _activeModal.value = ActiveOverlayModal.FILE_INSPECTOR
      postStatus("OPENED FILE // ${file.displayName}")
    } else if (file.uriString != null) {
      val ctx = getApplication<Application>()
      val uri = Uri.parse(file.uriString)
      val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        data = uri
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      runCatching {
        ctx.startActivity(viewIntent)
        postStatus("EXTERNAL SAF // ${file.displayName}")
      }.onFailure {
        _inspectedFile.value = file
        _activeModal.value = ActiveOverlayModal.FILE_INSPECTOR
      }
    }
  }

  fun saveEditedFileContent(file: FileEntry, updatedContent: String) {
    val path = file.localFilePath ?: return
    viewModelScope.launch(Dispatchers.IO) {
      runCatching {
        File(path).writeText(updatedContent)
      }
      val safSet = repository.indexedSafEntriesFlow
      _allFiles.value = searchEngine.loadLocalAndSafFiles(emptySet())
      _inspectedFile.value = file.copy(
        previewSnippet = updatedContent,
        sizeBytes = updatedContent.toByteArray().size.toLong()
      )
      postStatus("SAVED TO DISK // ${file.displayName}")
    }
  }

  fun onSafFilesSelected(uris: List<Uri>) {
    if (uris.isEmpty()) return
    viewModelScope.launch(Dispatchers.IO) {
      var indexedCount = 0
      uris.forEach { uri ->
        searchEngine.resolveSafUriMetadata(uri)?.let { serialized ->
          repository.addIndexedSafEntry(serialized)
          indexedCount++
        }
      }
      postStatus("INDEXED $indexedCount SAF DOCUMENT(S)")
    }
  }

  fun dialContact(contact: ContactEntry) {
    triggerHapticTick(isHeavy = true)
    val ctx = getApplication<Application>()
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(contact.phoneNumber)}")).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
      ctx.startActivity(intent)
      postStatus("CALLING // ${contact.displayName.uppercase()}")
    }.onFailure {
      postStatus("DIAL READY // ${contact.phoneNumber}")
    }
  }

  fun smsContact(contact: ContactEntry) {
    triggerHapticTick(isHeavy = false)
    val ctx = getApplication<Application>()
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(contact.phoneNumber)}")).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
      ctx.startActivity(intent)
      postStatus("SMS // ${contact.displayName.uppercase()}")
    }.onFailure {
      postStatus("SMS RELAY // ${contact.phoneNumber}")
    }
  }

  fun messageWhatsAppContact(contact: ContactEntry) {
    triggerHapticTick(isHeavy = false)
    val ctx = getApplication<Application>()
    val digits = contact.phoneNumber.filter { it.isDigit() }
    val waUri = Uri.parse("https://wa.me/$digits")
    val intent = Intent(Intent.ACTION_VIEW, waUri).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
      ctx.startActivity(intent)
      postStatus("WA RELAY // ${contact.displayName.uppercase()}")
    }.onFailure {
      postStatus("MSG CHANNEL // ${contact.phoneNumber}")
    }
  }

  fun addSpeedDialContact(name: String, phone: String, role: String) {
    if (name.isBlank() || phone.isBlank()) return
    viewModelScope.launch {
      repository.addCustomContact(name, phone, role.ifBlank { "FIELD.OPS" })
      _activeModal.value = ActiveOverlayModal.NONE
      postStatus("ADDED CONTACT // ${name.uppercase()}")
    }
  }

  fun executeTerminalCommand(rawCmd: String) {
    val cmd = rawCmd.trim()
    if (cmd.isEmpty()) return
    triggerHapticTick(isHeavy = false)

    val currentLogs = _terminalLogs.value.toMutableList()
    currentLogs.add("chrono@edge70:~$ $cmd")

    val parts = cmd.split(" ")
    val action = parts.first().lowercase()
    val arg = parts.drop(1).joinToString(" ")

    when (action) {
      "help" -> {
        currentLogs.add("COMMANDS:")
        currentLogs.add("  status          - Display hardware telemetry")
        currentLogs.add("  theme <id>      - Set palette (amber|matrix|synthwave|industrial)")
        currentLogs.add("  focus <on|off>  - Toggle FOCUS.LOCK mode")
        currentLogs.add("  ls              - List indexed workspace files")
        currentLogs.add("  touch <name>    - Create new local file in workspace")
        currentLogs.add("  clear           - Clear terminal buffer")
      }
      "status" -> {
        val t = telemetry.value
        currentLogs.add("CPU: ${t.cpuLoadPercent}% | RAM: ${t.ramUsedMb}/${t.ramTotalMb}MB")
        currentLogs.add("BAT: ${t.batteryPercent}% (${t.formattedVoltageV}) | TEMP: ${t.formattedTempC}")
        currentLogs.add("DISPLAY: ${t.displayRefreshHz}Hz | UPTIME: ${t.uptimeFormatted}")
      }
      "theme" -> {
        val targetPalette = ChronoPalette.fromId(arg.lowercase())
        setPalette(targetPalette)
        currentLogs.add("PALETTE UPDATED -> ${targetPalette.displayName}")
      }
      "focus" -> {
        val enable = arg.lowercase() == "on" || arg == "1" || arg.lowercase() == "true"
        setFocusMode(enable)
        currentLogs.add("FOCUS.LOCK -> ${if (enable) "ENGAGED" else "STANDBY"}")
      }
      "ls" -> {
        _allFiles.value.forEach { f ->
          currentLogs.add("  [${f.extensionTag}] ${f.displayName} (${f.formattedSize()})")
        }
      }
      "touch" -> {
        val created = searchEngine.createWorkspaceFile(
          fileName = arg.ifBlank { "log_${System.currentTimeMillis() % 1000}.txt" },
          content = "// CREATED VIA CHRONO.SHELL\nTIMESTAMP: ${System.currentTimeMillis()}\n"
        )
        _allFiles.value = searchEngine.loadLocalAndSafFiles(emptySet())
        currentLogs.add("CREATED FILE -> ${created?.displayName ?: "ERR"}")
      }
      "clear" -> {
        _terminalLogs.value = listOf("BUFFER CLEARED // READY.")
        return
      }
      else -> {
        currentLogs.add("UNKNOWN DIRECTIVE '$action'. TYPE 'help' FOR SYNTAX.")
      }
    }
    _terminalLogs.value = currentLogs.takeLast(40)
  }

  fun openSystemHomeSettings() {
    val ctx = getApplication<Application>()
    val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
      ctx.startActivity(intent)
      postStatus("OPENED ANDROID DEFAULT HOME SELECTOR")
    }.onFailure {
      val fallback = Intent(Settings.ACTION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      runCatching { ctx.startActivity(fallback) }
    }
  }

  // SYS.CONFIG Mutators
  fun setPalette(palette: ChronoPalette) {
    triggerHapticTick(isHeavy = true)
    viewModelScope.launch {
      repository.setPalette(palette)
      postStatus("PALETTE // ${palette.displayName}")
    }
  }

  fun setUseShareTech(useShareTech: Boolean) {
    triggerHapticTick(isHeavy = false)
    viewModelScope.launch { repository.setUseShareTech(useShareTech) }
  }

  fun setFontScale(scale: Float) {
    viewModelScope.launch { repository.setFontScale(scale) }
  }

  fun setScanlineOpacity(opacity: Float) {
    viewModelScope.launch { repository.setScanlineOpacity(opacity) }
  }

  fun setChamferRadius(radiusDp: Float) {
    viewModelScope.launch { repository.setChamferRadius(radiusDp) }
  }

  fun setSpringPhysics(stiffness: Float, damping: Float) {
    viewModelScope.launch { repository.setSpringPhysics(stiffness, damping) }
  }

  fun setFocusMode(enabled: Boolean) {
    triggerHapticTick(isHeavy = true)
    viewModelScope.launch {
      repository.setFocusMode(enabled)
      postStatus(
        if (enabled) "FOCUS.LOCK ENGAGED // BADGES SUPPRESSED"
        else "FOCUS.LOCK RELEASED // ALL CHANNELS ACTIVE"
      )
    }
  }

  fun setAutoLaunchExactMatch(enabled: Boolean) {
    triggerHapticTick(isHeavy = false)
    viewModelScope.launch { repository.setAutoLaunchExactMatch(enabled) }
  }

  fun setEdgePadding(paddingDp: Float) {
    viewModelScope.launch { repository.setEdgePadding(paddingDp) }
  }

  fun setMaxDockSlots(slots: Int) {
    triggerHapticTick(isHeavy = false)
    viewModelScope.launch { repository.setMaxDockSlots(slots) }
  }

  fun setHapticEnabled(enabled: Boolean) {
    viewModelScope.launch { repository.setHapticEnabled(enabled) }
  }

  fun setEdgeGlowEnabled(enabled: Boolean) {
    viewModelScope.launch { repository.setEdgeGlowEnabled(enabled) }
  }

  fun togglePinApp(appId: String) {
    triggerHapticTick(isHeavy = true)
    viewModelScope.launch {
      repository.togglePinApp(appId, sysConfig.value.maxDockSlots)
      postStatus("UPDATED PRIMARY COMMAND DOCK")
    }
  }

  fun toggleHideApp(appId: String) {
    triggerHapticTick(isHeavy = false)
    viewModelScope.launch {
      repository.toggleHideApp(appId)
      postStatus("UPDATED HIDDEN APP REGISTRY")
    }
  }

  fun clearHiddenApps() {
    triggerHapticTick(isHeavy = false)
    viewModelScope.launch {
      repository.clearHiddenApps()
      postStatus("RESTORED ALL HIDDEN APPS")
    }
  }

  fun resetFactoryDefaults() {
    triggerHapticTick(isHeavy = true)
    viewModelScope.launch {
      repository.resetToFactoryDefaults()
      postStatus("SYS.CONFIG RESET TO FACTORY DEFAULTS")
    }
  }

  fun toggleAudioPlayback() {
    triggerHapticTick(isHeavy = true)
    _isAudioPlaying.value = !_isAudioPlaying.value
  }

  fun cycleAudioTrack(delta: Int) {
    triggerHapticTick(isHeavy = false)
    val next = (_currentTrackIndex.value + delta + 4) % 4
    _currentTrackIndex.value = next
  }

  fun postStatus(msg: String) {
    _statusBannerMessage.value = msg
  }

  fun triggerHapticTick(isHeavy: Boolean = false) {
    if (!sysConfig.value.hapticFeedbackEnabled) return
    val ctx = getApplication<Application>()
    runCatching {
      val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      }

      if (vibrator?.hasVibrator() == true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val effectId = if (isHeavy) {
            VibrationEffect.EFFECT_CLICK
          } else {
            VibrationEffect.EFFECT_TICK
          }
          vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator.vibrate(
            VibrationEffect.createOneShot(if (isHeavy) 18L else 8L, VibrationEffect.DEFAULT_AMPLITUDE)
          )
        }
      }
    }
  }
}
