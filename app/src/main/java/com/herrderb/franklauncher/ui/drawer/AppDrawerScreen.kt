package com.herrderb.franklauncher.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.herrderb.franklauncher.data.AppInfo
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    allApps: List<AppInfo>,
    favoritePackages: List<String>,
    showIcons: Boolean = false,
    onAppLaunch: (AppInfo) -> Unit,
    onAddFavorite: (AppInfo) -> Unit,
    onBack: () -> Unit,
    isFullyVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showSnackbar by remember { mutableStateOf<String?>(null) }

    val filteredApps = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    LaunchedEffect(isFullyVisible) {
        if (isFullyVisible) {
            delay(50)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar != null) {
            delay(1500)
            showSnackbar = null
        }
    }

    var totalDrag by remember { mutableFloatStateOf(0f) }
    val draggableState = rememberDraggableState { delta ->
        totalDrag += delta
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStarted = { totalDrag = 0f },
                onDragStopped = {
                    if (totalDrag > 50f) {
                        onBack()
                    }
                    totalDrag = 0f
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search apps…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App count
            Text(
                text = "${filteredApps.size} apps",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // App list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isFavorite = app.packageName in favoritePackages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onAppLaunch(app) },
                                onLongClick = {
                                    if (!isFavorite) {
                                        onAddFavorite(app)
                                        showSnackbar = "★ ${app.label} added to favorites"
                                    }
                                }
                            )
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showIcons && app.icon != null) {
                            val bitmap = remember(app.packageName) {
                                app.icon.toBitmap(48, 48).asImageBitmap()
                            }
                            Image(
                                painter = BitmapPainter(bitmap),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = app.label,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        if (isFavorite) {
                            Text(
                                text = "★",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Snackbar
        if (showSnackbar != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    text = showSnackbar!!,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
