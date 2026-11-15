package com.example

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ActiveOverlayModal
import com.example.ui.ChronoViewModel
import com.example.ui.components.CrtScanlineAndEdgeOverlay
import com.example.ui.components.HomeCommandScreen
import com.example.ui.components.OmniSearchTray
import com.example.ui.components.SysConfigDrawer
import com.example.ui.components.TacticalOverlayModalHost
import com.example.ui.theme.ChronoOSTheme
import com.example.ui.theme.LocalChronoPalette

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ChronoLauncherApp()
    }
  }
}

@Composable
fun ChronoLauncherApp(
  viewModel: ChronoViewModel = viewModel(),
) {
  val sysConfig by viewModel.sysConfig.collectAsStateWithLifecycle()
  val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
  val pinnedApps by viewModel.pinnedDockApps.collectAsStateWithLifecycle()
  val allApps by viewModel.allApps.collectAsStateWithLifecycle()
  val searchResult by viewModel.omniSearchResult.collectAsStateWithLifecycle()
  val isAppTrayOpen by viewModel.isAppTrayOpen.collectAsStateWithLifecycle()
  val isSysConfigOpen by viewModel.isSysConfigOpen.collectAsStateWithLifecycle()
  val activeModal by viewModel.activeModal.collectAsStateWithLifecycle()
  val inspectedFile by viewModel.inspectedFile.collectAsStateWithLifecycle()
  val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
  val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
  val isAudioPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
  val currentTrackIndex by viewModel.currentTrackIndex.collectAsStateWithLifecycle()

  val context = LocalContext.current

  // Runtime Permission Launcher for ContactsContract query
  val contactsPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { granted ->
      viewModel.onContactsPermissionResult(granted)
    }
  )

  // Storage Access Framework (SAF) Multi-Document Picker for indexing external files
  val safDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments(),
    onResult = { uris ->
      uris.forEach { uri ->
        runCatching {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
          )
        }
      }
      viewModel.onSafFilesSelected(uris)
    }
  )

  ChronoOSTheme(
    palette = sysConfig.palette,
    useShareTechDisplay = sysConfig.useShareTechDisplay,
    fontScale = sysConfig.fontScale,
    chamferRadiusDp = sysConfig.chamferRadiusDp,
  ) {
    val palette = LocalChronoPalette.current
    val dampingRatio = (sysConfig.springDamping / 36f).coerceIn(0.45f, 1.0f)
    val stiff = sysConfig.springStiffness.coerceIn(180f, 900f)

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(palette.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
      // 1. Distraction-Free Home Command Screen
      HomeCommandScreen(
        sysConfig = sysConfig,
        telemetry = telemetry,
        pinnedApps = pinnedApps,
        statusBanner = statusBanner,
        onSelectPalette = viewModel::setPalette,
        onToggleFocusMode = viewModel::setFocusMode,
        onLaunchApp = viewModel::launchAppEntry,
        onOpenOmniTray = viewModel::openAppTray,
        onOpenSysConfig = viewModel::openSysConfig,
        onOpenDockEditor = { viewModel.openModal(ActiveOverlayModal.DOCK_SLOT_EDITOR) },
        onOpenTerminalQuick = { viewModel.openModal(ActiveOverlayModal.TERMINAL_SHELL) },
      )

      // 2. 144Hz Spring-Animated Unified App Tray & Instant Omni-Search
      AnimatedVisibility(
        visible = isAppTrayOpen,
        enter = slideInVertically(
          initialOffsetY = { fullHeight -> fullHeight },
          animationSpec = spring(stiffness = stiff, dampingRatio = dampingRatio)
        ) + fadeIn(animationSpec = spring(stiffness = stiff)),
        exit = slideOutVertically(
          targetOffsetY = { fullHeight -> fullHeight },
          animationSpec = spring(stiffness = stiff, dampingRatio = dampingRatio)
        ) + fadeOut(),
      ) {
        OmniSearchTray(
          searchResult = searchResult,
          sysConfig = sysConfig,
          onQueryChange = viewModel::updateSearchQuery,
          onSelectCategory = viewModel::selectCategory,
          onLaunchApp = viewModel::launchAppEntry,
          onTogglePinApp = viewModel::togglePinApp,
          onToggleHideApp = viewModel::toggleHideApp,
          onInspectFile = viewModel::inspectOrOpenFile,
          onPickSafFiles = {
            safDocumentLauncher.launch(arrayOf("*/*"))
          },
          onSyncContactsPermission = {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
          },
          onOpenAddContactModal = {
            viewModel.openModal(ActiveOverlayModal.ADD_CONTACT)
          },
          onDialContact = viewModel::dialContact,
          onSmsContact = viewModel::smsContact,
          onWhatsAppContact = viewModel::messageWhatsAppContact,
          onCloseTray = viewModel::closeAppTray,
        )
      }

      // 3. Extreme Customization Engine (SYS.CONFIG Drawer)
      AnimatedVisibility(
        visible = isSysConfigOpen,
        enter = slideInHorizontally(
          initialOffsetX = { fullWidth -> fullWidth },
          animationSpec = spring(stiffness = stiff, dampingRatio = dampingRatio)
        ) + fadeIn(),
        exit = slideOutHorizontally(
          targetOffsetX = { fullWidth -> fullWidth },
          animationSpec = spring(stiffness = stiff, dampingRatio = dampingRatio)
        ) + fadeOut(),
      ) {
        SysConfigDrawer(
          sysConfig = sysConfig,
          onSelectPalette = viewModel::setPalette,
          onToggleShareTechFont = viewModel::setUseShareTech,
          onFontScaleChange = viewModel::setFontScale,
          onScanlineOpacityChange = viewModel::setScanlineOpacity,
          onChamferRadiusChange = viewModel::setChamferRadius,
          onSpringPhysicsChange = viewModel::setSpringPhysics,
          onEdgePaddingChange = viewModel::setEdgePadding,
          onMaxDockSlotsChange = viewModel::setMaxDockSlots,
          onAutoLaunchExactChange = viewModel::setAutoLaunchExactMatch,
          onHapticEnabledChange = viewModel::setHapticEnabled,
          onEdgeGlowEnabledChange = viewModel::setEdgeGlowEnabled,
          onRestoreHiddenApps = viewModel::clearHiddenApps,
          onOpenDefaultHomeSelector = viewModel::openSystemHomeSettings,
          onResetDefaults = viewModel::resetFactoryDefaults,
          onClose = viewModel::closeSysConfig,
        )
      }

      // 4. Built-in Tactical Modals (Chrono Shell, Poweramp Audio Deck, File Inspector, Dock Editor)
      TacticalOverlayModalHost(
        activeModal = activeModal,
        chamferDp = sysConfig.chamferRadiusDp,
        terminalLogs = terminalLogs,
        onExecuteTerminalCommand = viewModel::executeTerminalCommand,
        isAudioPlaying = isAudioPlaying,
        currentTrackIndex = currentTrackIndex,
        onToggleAudioPlay = viewModel::toggleAudioPlayback,
        onCycleTrack = viewModel::cycleAudioTrack,
        inspectedFile = inspectedFile,
        onSaveFileContent = viewModel::saveEditedFileContent,
        allApps = allApps,
        pinnedAppIds = sysConfig.pinnedAppIds,
        maxDockSlots = sysConfig.maxDockSlots,
        onTogglePinApp = viewModel::togglePinApp,
        onAddSpeedContact = viewModel::addSpeedDialContact,
        onCloseModal = viewModel::closeActiveModal,
      )

      // 5. Top-level Non-Intrusive CRT Scanline & Moto Edge 70 Fusion Curved Horizon Overlay
      CrtScanlineAndEdgeOverlay(
        scanlineOpacity = sysConfig.scanlineOpacity,
        edgeGlowEnabled = sysConfig.edgeGlowEnabled,
        focusModeActive = sysConfig.focusModeEnabled,
      )
    }
  }
}
