package com.herrderb.franklauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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

        setContent {
            val viewModel: HomeViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            FrankLauncherTheme(themeMode = uiState.themeMode) {
                var currentScreen by remember { mutableStateOf(Screen.HOME) }

                BackHandler(enabled = currentScreen != Screen.HOME) {
                    currentScreen = Screen.HOME
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            when (targetState) {
                                Screen.DRAWER -> slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                                Screen.SETTINGS -> slideInVertically { it } togetherWith slideOutVertically { -it }
                                Screen.HOME -> slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                            }
                        },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            Screen.HOME -> HomeScreen(
                                uiState = uiState,
                                onAppLaunch = { viewModel.launchApp(it) },
                                onSwipeLeft = { currentScreen = Screen.DRAWER },
                                onToggleLock = { viewModel.toggleHomescreenLock() },
                                onOpenSettings = { currentScreen = Screen.SETTINGS }
                            )
                            Screen.DRAWER -> AppDrawerScreen(
                                allApps = uiState.allApps,
                                onAppLaunch = { viewModel.launchApp(it) },
                                onBack = { currentScreen = Screen.HOME },
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                                )
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                currentTheme = uiState.themeMode,
                                widgetHeightFraction = uiState.widgetHeightFraction,
                                onThemeChange = { viewModel.setThemeMode(it) },
                                onWidgetHeightChange = { viewModel.setWidgetHeightFraction(it) },
                                onBack = { currentScreen = Screen.HOME },
                                modifier = Modifier.background(
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class Screen {
    HOME, DRAWER, SETTINGS
}
