package com.herrderb.franklauncher.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herrderb.franklauncher.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    widgetHeightFraction: Float,
    onThemeChange: (ThemeMode) -> Unit,
    onWidgetHeightChange: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(top = 48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Theme selection
        Text(
            text = "Theme",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = currentTheme == mode,
                    onClick = { onThemeChange(mode) },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        // Widget height slider
        Text(
            text = "Widget Area Height: ${(widgetHeightFraction * 100).toInt()}%",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Slider(
            value = widgetHeightFraction,
            onValueChange = onWidgetHeightChange,
            valueRange = 0.2f..0.8f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Back", fontSize = 16.sp)
        }
    }
}
