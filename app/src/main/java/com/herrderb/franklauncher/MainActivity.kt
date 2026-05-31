package com.herrderb.franklauncher

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.herrderb.franklauncher.ui.drawer.AppDrawerScreen
import com.herrderb.franklauncher.ui.home.HomeScreen
import com.herrderb.franklauncher.ui.home.HomeViewModel
import com.herrderb.franklauncher.ui.settings.SettingsScreen
import com.herrderb.franklauncher.ui.theme.FrankLauncherTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request location permission for weather station selection
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 100)
        }

        setContent {
            val viewModel: HomeViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            var showDefaultLauncherPrompt by remember { mutableStateOf(false) }

            // Check if we're the default launcher
            LaunchedEffect(Unit) {
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

            FrankLauncherTheme(themeMode = uiState.themeMode) {
                var currentScreen by remember { mutableStateOf(Screen.HOME) }
                // Drawer gesture state: 0f = closed, 1f = fully open
                var drawerProgress by remember { mutableFloatStateOf(0f) }
                var isDraggingDrawer by remember { mutableStateOf(false) }
                val drawerOffsetAnim by animateFloatAsState(
                    targetValue = if (currentScreen == Screen.DRAWER && !isDraggingDrawer) 0f
                        else if (currentScreen == Screen.HOME && !isDraggingDrawer) 1f
                        else 1f - drawerProgress,
                    animationSpec = tween(
                        durationMillis = if (isDraggingDrawer) 0 else 250,
                        easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
                    ),
                    label = "drawer_offset",
                    finishedListener = {
                        if (it == 0f) currentScreen = Screen.DRAWER
                        if (it == 1f) currentScreen = Screen.HOME
                    }
                )

                BackHandler(enabled = currentScreen != Screen.HOME) {
                    currentScreen = Screen.HOME
                    drawerProgress = 0f
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
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
                                    isDraggingDrawer = true
                                    drawerProgress = progress
                                },
                                onDragDrawerEnd = { progress ->
                                    isDraggingDrawer = false
                                    if (progress > 0.3f) {
                                        currentScreen = Screen.DRAWER
                                        drawerProgress = 1f
                                    } else {
                                        currentScreen = Screen.HOME
                                        drawerProgress = 0f
                                    }
                                },
                                onToggleLock = { viewModel.toggleHomescreenLock() },
                                onOpenSettings = { currentScreen = Screen.SETTINGS },
                                onWeatherClick = {
                                    val weatherPkg = if (uiState.isInSwitzerland)
                                        uiState.weatherApp
                                    else
                                        uiState.weatherAppInternational
                                    if (weatherPkg.isNotBlank()) {
                                        val intent = packageManager.getLaunchIntentForPackage(weatherPkg)
                                        if (intent != null) startActivity(intent)
                                    }
                                },
                                onHydroClick = {
                                    uiState.hydro?.let { hydro ->
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(hydro.url)
                                        )
                                        startActivity(intent)
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
                                weatherApp = uiState.weatherApp,
                                weatherAppInternational = uiState.weatherAppInternational,
                                allApps = uiState.allApps,
                                onThemeChange = { viewModel.setThemeMode(it) },
                                onFavoriteTextSizeChange = { viewModel.setFavoriteTextSize(it) },
                                onFavoriteAlignmentChange = { viewModel.setFavoriteAlignment(it) },
                                onShowDrawerIconsChange = { viewModel.setShowDrawerIcons(it) },
                                onWeatherAppChange = { viewModel.setWeatherApp(it) },
                                onWeatherAppInternationalChange = { viewModel.setWeatherAppInternational(it) },
                                onBack = { currentScreen = Screen.HOME },
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                                )
                            )
                        }
                    }

                    // Drawer overlay - slides from right following gesture
                    val drawerOffsetFraction = if (isDraggingDrawer) (1f - drawerProgress) else drawerOffsetAnim
                    if (currentScreen == Screen.DRAWER || drawerProgress > 0f || drawerOffsetAnim < 1f) {
                        val favoritePackagesList = remember(uiState.favoriteApps) {
                            uiState.favoriteApps.map { it.packageName }
                        }
                        AppDrawerScreen(
                            allApps = uiState.allApps,
                            favoritePackages = favoritePackagesList,
                            showIcons = uiState.showDrawerIcons,
                            onAppLaunch = { viewModel.launchApp(it) },
                            onAddFavorite = { viewModel.addFavoriteApp(it) },
                            onBack = {
                                currentScreen = Screen.HOME
                                drawerProgress = 0f
                            },
                            isFullyVisible = currentScreen == Screen.DRAWER && !isDraggingDrawer && drawerOffsetFraction == 0f,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = size.width * drawerOffsetFraction
                                }
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
