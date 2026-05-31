package com.herrderb.franklauncher.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.herrderb.franklauncher.ui.theme.ThemeMode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    favoriteTextSize: Float,
    favoriteAlignment: String,
    showDrawerIcons: Boolean,
    weatherApp: String,
    weatherAppInternational: String,
    allApps: List<com.herrderb.franklauncher.data.AppInfo>,
    onThemeChange: (ThemeMode) -> Unit,
    onFavoriteTextSizeChange: (Float) -> Unit,
    onFavoriteAlignmentChange: (String) -> Unit,
    onShowDrawerIconsChange: (Boolean) -> Unit,
    onWeatherAppChange: (String) -> Unit,
    onWeatherAppInternationalChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(top = 48.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- Appearance ---
        SectionHeader("Appearance")

        Text(
            text = "Theme",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = currentTheme == mode,
                    onClick = { onThemeChange(mode) },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

        // --- Favorites ---
        SectionHeader("Favorites")

        // Text Size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Text Size",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onFavoriteTextSizeChange(14f) }) {
                    Text(
                        text = "Small",
                        color = if (favoriteTextSize == 14f)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
                TextButton(onClick = { onFavoriteTextSizeChange(18f) }) {
                    Text(
                        text = "Medium",
                        color = if (favoriteTextSize == 18f)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
                TextButton(onClick = { onFavoriteTextSizeChange(24f) }) {
                    Text(
                        text = "Large",
                        color = if (favoriteTextSize == 24f)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Alignment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alignment",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onFavoriteAlignmentChange("left") }
                ) {
                    Text(
                        text = "Left",
                        color = if (favoriteAlignment == "left")
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
                TextButton(
                    onClick = { onFavoriteAlignmentChange("centered") }
                ) {
                    Text(
                        text = "Centered",
                        color = if (favoriteAlignment == "centered")
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

        // --- Weather ---
        SectionHeader("Weather")

        var showAppPickerCh by remember { mutableStateOf(false) }
        val selectedAppNameCh = allApps.find { it.packageName == weatherApp }?.label ?: "Not set"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { showAppPickerCh = true }),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Weather app (Switzerland)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Text(
                text = selectedAppNameCh,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (showAppPickerCh) {
            AlertDialog(
                onDismissRequest = { showAppPickerCh = false },
                title = { Text("Select weather app (Switzerland)") },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(allApps.size) { index ->
                            val app = allApps[index]
                            TextButton(
                                onClick = {
                                    onWeatherAppChange(app.packageName)
                                    showAppPickerCh = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = app.label,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (app.packageName == weatherApp)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAppPickerCh = false }) { Text("Cancel") }
                }
            )
        }

        var showAppPickerIntl by remember { mutableStateOf(false) }
        val selectedAppNameIntl = allApps.find { it.packageName == weatherAppInternational }?.label ?: "Not set"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { showAppPickerIntl = true }),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Weather app (International)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Text(
                text = selectedAppNameIntl,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (showAppPickerIntl) {
            AlertDialog(
                onDismissRequest = { showAppPickerIntl = false },
                title = { Text("Select weather app (International)") },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(allApps.size) { index ->
                            val app = allApps[index]
                            TextButton(
                                onClick = {
                                    onWeatherAppInternationalChange(app.packageName)
                                    showAppPickerIntl = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = app.label,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (app.packageName == weatherAppInternational)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAppPickerIntl = false }) { Text("Cancel") }
                }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

        // --- App Drawer ---
        SectionHeader("App Drawer")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show app icons",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Switch(
                checked = showDrawerIcons,
                onCheckedChange = onShowDrawerIconsChange
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("← Back", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SettingSlider(
    label: String,
    value: String,
    sliderValue: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
    Slider(
        value = sliderValue,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = Modifier.fillMaxWidth()
    )
}
