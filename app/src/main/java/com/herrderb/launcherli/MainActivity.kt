package com.herrderb.launcherli

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.herrderb.launcherli.ui.drawer.AppDrawerScreen
import com.herrderb.launcherli.ui.home.HomeScreen
import com.herrderb.launcherli.ui.home.HomeViewModel
import com.herrderb.launcherli.ui.settings.SettingsScreen
import com.herrderb.launcherli.ui.theme.LauncherliTheme

class MainActivity : ComponentActivity() {

    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ask for location on a fresh start only, not on every recreate (theme or
        // configuration change); weather and hydro degrade silently without it.
        if (savedInstanceState == null &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }

        setContent {
            val viewModel: HomeViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            var showDefaultLauncherPrompt by remember { mutableStateOf(false) }
            var defaultLauncherChecked by rememberSaveable { mutableStateOf(false) }

            // Check once per launcher start whether we're the default home app.
            LaunchedEffect(Unit) {
                if (defaultLauncherChecked) return@LaunchedEffect
                defaultLauncherChecked = true
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                val currentDefault = resolveInfo?.activityInfo?.packageName
                if (currentDefault != packageName) {
                    showDefaultLauncherPrompt = true
                }
            }

            // Prompt to set as default launcher
            if (showDefaultLauncherPrompt) {
                AlertDialog(
                    onDismissRequest = { showDefaultLauncherPrompt = false },
                    title = { Text("Set as Default Launcher") },
                    text = { Text("Launcherli is not your default home app. Would you like to set it up?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showDefaultLauncherPrompt = false
                            val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
                            startActivity(intent)
                        }) { Text("Open Settings") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDefaultLauncherPrompt = false }) {
                            Text("Later")
                        }
                    }
                )
            }

            LauncherliTheme(themeMode = uiState.themeMode) {
                var currentScreen by remember { mutableStateOf(Screen.HOME) }
                // Drawer offset as a fraction of its width: 1 = hidden off the right
                // edge, 0 = fully open. Only the drawer's graphicsLayer reads the value,
                // so dragging and settling move a layer without recomposing anything.
                val drawerOffset = remember { Animatable(1f) }
                val scope = rememberCoroutineScope()
                val drawerShown by remember { derivedStateOf { drawerOffset.value < 1f } }
                val drawerSettledOpen by remember { derivedStateOf { drawerOffset.value == 0f } }
                val settleDrawer: (Boolean) -> Unit = { open ->
                    currentScreen = if (open) Screen.DRAWER else Screen.HOME
                    scope.launch {
                        drawerOffset.animateTo(
                            if (open) 0f else 1f,
                            tween(250, easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f))
                        )
                    }
                }

                BackHandler(enabled = currentScreen != Screen.HOME) {
                    if (currentScreen == Screen.DRAWER) settleDrawer(false) else currentScreen = Screen.HOME
                }

                // On the home screen, fade the status bar out after a moment for a
                // cleaner look; show it again on the drawer and settings screens.
                val view = LocalView.current
                LaunchedEffect(currentScreen) {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    if (currentScreen == Screen.HOME) {
                        delay(2000)
                        controller.hide(WindowInsetsCompat.Type.statusBars())
                    } else {
                        controller.show(WindowInsetsCompat.Type.statusBars())
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Reserve the system-bar space even while the bar is hidden,
                        // so hiding the status bar fades it out without shifting content.
                        .windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility)
                ) {
                    val smoothEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

                    // Home + Settings via AnimatedContent
                    AnimatedContent(
                        targetState = if (currentScreen == Screen.SETTINGS) Screen.SETTINGS else Screen.HOME,
                        transitionSpec = {
                            when (targetState) {
                                Screen.SETTINGS -> (slideInVertically(
                                    animationSpec = tween(200, easing = smoothEasing)
                                ) { it / 4 } + fadeIn(animationSpec = tween(150))) togetherWith
                                    fadeOut(animationSpec = tween(100))
                                else -> (slideInVertically(
                                    animationSpec = tween(200, easing = smoothEasing)
                                ) { -it / 4 } + fadeIn(animationSpec = tween(150))) togetherWith
                                    fadeOut(animationSpec = tween(100))
                            }
                        },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            Screen.HOME, Screen.DRAWER -> HomeScreen(
                                uiState = uiState,
                                onAppLaunch = { viewModel.launchApp(it) },
                                onSwipeLeft = { },
                                onDragDrawer = { progress ->
                                    scope.launch { drawerOffset.snapTo(1f - progress) }
                                },
                                onDragDrawerEnd = { progress -> settleDrawer(progress > 0.3f) },
                                onToggleLock = { viewModel.toggleHomescreenLock() },
                                onOpenSettings = { currentScreen = Screen.SETTINGS },
                                onWeatherClick = {
                                    val meteoSwissPkg = uiState.allApps
                                        .firstOrNull { it.label.contains("meteoswiss", ignoreCase = true) }
                                        ?.packageName
                                    val intent = meteoSwissPkg?.let {
                                        packageManager.getLaunchIntentForPackage(it)
                                    } ?: android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://www.meteoswiss.admin.ch/")
                                    )
                                    try { startActivity(intent) } catch (_: Exception) {}
                                },
                                onHydroClick = {
                                    uiState.hydro?.let { hydro ->
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(hydro.url)
                                        )
                                        try { startActivity(intent) } catch (_: Exception) {}
                                    }
                                },
                                onRemoveFavorite = { viewModel.removeFavoriteApp(it) },
                                onReorderFavorites = { apps ->
                                    viewModel.reorderFavorites(apps)
                                }
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                currentTheme = uiState.themeMode,
                                favoriteTextSize = uiState.favoriteTextSize,
                                favoriteAlignment = uiState.favoriteAlignment,
                                showDrawerIcons = uiState.showDrawerIcons,
                                showWidgetLabels = uiState.showWidgetLabels,
                                showMostUsedApps = uiState.showMostUsedApps,
                                contactSearchEnabled = uiState.contactSearchEnabled,
                                calendarIcsUrl = uiState.calendarIcsUrl,
                                allApps = uiState.allApps,
                                onThemeChange = { viewModel.setThemeMode(it) },
                                onFavoriteTextSizeChange = { viewModel.setFavoriteTextSize(it) },
                                onFavoriteAlignmentChange = { viewModel.setFavoriteAlignment(it) },
                                onShowDrawerIconsChange = { viewModel.setShowDrawerIcons(it) },
                                onShowWidgetLabelsChange = { viewModel.setShowWidgetLabels(it) },
                                onShowMostUsedAppsChange = { viewModel.setShowMostUsedApps(it) },
                                onResetMostUsedApps = { viewModel.resetAppUsage() },
                                onContactSearchEnabledChange = { viewModel.setContactSearchEnabled(it) },
                                onCalendarIcsUrlChange = { viewModel.setCalendarIcsUrl(it) },
                                onBack = { currentScreen = Screen.HOME },
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                                )
                            )
                        }
                    }

                    // Drawer overlay - slides from right following gesture
                    if (currentScreen == Screen.DRAWER || drawerShown) {
                        val favoriteKeys = remember(uiState.favoriteApps) {
                            uiState.favoriteApps.map { it.key }
                        }
                        AppDrawerScreen(
                            allApps = uiState.allApps,
                            favoriteKeys = favoriteKeys,
                            showIcons = uiState.showDrawerIcons,
                            mostUsedApps = uiState.mostUsedApps,
                            showMostUsed = uiState.showMostUsedApps,
                            contactSearchEnabled = uiState.contactSearchEnabled,
                            onAppLaunch = {
                                viewModel.launchApp(it, countUsage = true)
                                settleDrawer(false)
                            },
                            onAddFavorite = { viewModel.addFavoriteApp(it) },
                            onRemoveFavorite = { viewModel.removeFavoriteApp(it) },
                            onClearUsage = { viewModel.clearAppUsage(it) },
                            onBack = { settleDrawer(false) },
                            isFullyVisible = currentScreen == Screen.DRAWER && drawerSettledOpen,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { translationX = size.width * drawerOffset.value }
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                        )
                    }
                }
            }
        }
    }
}

enum class Screen {
    HOME, DRAWER, SETTINGS
}
