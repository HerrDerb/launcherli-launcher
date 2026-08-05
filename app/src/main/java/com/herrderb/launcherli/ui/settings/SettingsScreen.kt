package com.herrderb.launcherli.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.herrderb.launcherli.ui.theme.ThemeMode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeMode,
    favoriteTextSize: Float,
    favoriteAlignment: String,
    showDrawerIcons: Boolean,
    showWidgetLabels: Boolean,
    showMostUsedApps: Boolean,
    contactSearchEnabled: Boolean,
    calendarIcsUrl: String,
    allApps: List<com.herrderb.launcherli.data.AppInfo>,
    onThemeChange: (ThemeMode) -> Unit,
    onFavoriteTextSizeChange: (Float) -> Unit,
    onFavoriteAlignmentChange: (String) -> Unit,
    onShowDrawerIconsChange: (Boolean) -> Unit,
    onShowWidgetLabelsChange: (Boolean) -> Unit,
    onShowMostUsedAppsChange: (Boolean) -> Unit,
    onResetMostUsedApps: () -> Unit,
    onContactSearchEnabledChange: (Boolean) -> Unit,
    onCalendarIcsUrlChange: (String) -> Unit,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show station labels",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Switch(
                checked = showWidgetLabels,
                onCheckedChange = onShowWidgetLabelsChange
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

        // --- Calendar ---
        SectionHeader("Calendar")

        Text(
            text = "Paste a public calendar link (.ics). Most calendar apps " +
                "offer this under a \"Share\" or \"Public/subscription link\" option.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        val hasSavedLink = calendarIcsUrl.isNotBlank()
        // Resets to placeholder view whenever the saved value changes (e.g. after Save).
        var editingLink by remember(calendarIcsUrl) { mutableStateOf(false) }

        if (hasSavedLink && !editingLink) {
            // Link is stored encrypted and never shown again — only a placeholder.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Link saved ••••••••",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Row {
                    TextButton(onClick = { editingLink = true }) { Text("Change") }
                    TextButton(onClick = { onCalendarIcsUrlChange("") }) { Text("Clear") }
                }
            }
        } else {
            var urlText by remember { mutableStateOf("") }
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("Calendar link (.ics)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (hasSavedLink) {
                    TextButton(onClick = { editingLink = false }) { Text("Cancel") }
                }
                TextButton(
                    onClick = { onCalendarIcsUrlChange(urlText.trim()) },
                    enabled = urlText.isNotBlank()
                ) { Text("Save") }
            }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Most used apps",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = "Show your most-launched apps above the list.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            Switch(
                checked = showMostUsedApps,
                onCheckedChange = onShowMostUsedAppsChange
            )
        }

        val context = LocalContext.current
        var showContactsPermissionHint by remember { mutableStateOf(false) }
        val contactsPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                showContactsPermissionHint = false
                onContactSearchEnabledChange(true)
            } else {
                showContactsPermissionHint = true
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Contact search",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = "Search device contacts from the drawer search bar.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            Switch(
                checked = contactSearchEnabled,
                onCheckedChange = { wantOn ->
                    when {
                        !wantOn -> onContactSearchEnabledChange(false)
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED ->
                            onContactSearchEnabledChange(true)
                        else -> contactsPermissionLauncher
                            .launch(Manifest.permission.READ_CONTACTS)
                    }
                }
            )
        }

        if (showContactsPermissionHint) {
            Text(
                text = "Contacts permission was denied. If the dialog no longer " +
                    "appears, allow Contacts for Launcherli in system settings.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Open app settings", fontSize = 13.sp)
            }
        }

        var showResetConfirm by remember { mutableStateOf(false) }
        TextButton(
            onClick = { showResetConfirm = true },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("Reset usage counts", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        }

        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("Reset most used apps?") },
                text = { Text("This clears all app launch counts. The most used list starts over.") },
                confirmButton = {
                    TextButton(onClick = {
                        onResetMostUsedApps()
                        showResetConfirm = false
                    }) { Text("Reset") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
                }
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
