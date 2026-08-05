package com.herrderb.launcherli.ui.drawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.herrderb.launcherli.data.AppInfo
import com.herrderb.launcherli.data.AppRepository
import com.herrderb.launcherli.data.ContactInfo
import com.herrderb.launcherli.data.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    allApps: List<AppInfo>,
    favoritePackages: List<String>,
    showIcons: Boolean = false,
    mostUsedApps: List<AppInfo> = emptyList(),
    showMostUsed: Boolean = false,
    contactSearchEnabled: Boolean = false,
    onAppLaunch: (AppInfo) -> Unit,
    onAddFavorite: (AppInfo) -> Unit,
    onRemoveFavorite: (AppInfo) -> Unit,
    onBack: () -> Unit,
    isFullyVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showSnackbar by remember { mutableStateOf<String?>(null) }

    val filteredApps = remember(searchQuery, allApps) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    val favoriteSet = remember(favoritePackages) { favoritePackages.toHashSet() }

    val contactsRepository = remember { ContactsRepository(context) }
    var contactResults by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }

    LaunchedEffect(searchQuery, contactSearchEnabled) {
        if (!contactSearchEnabled || searchQuery.isBlank()) {
            contactResults = emptyList()
            return@LaunchedEffect
        }
        // Debounce: the effect restarts on every keystroke, cancelling the
        // pending delay and abandoning any stale in-flight query.
        delay(150)
        contactResults = contactsRepository.search(searchQuery)
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
            val showMostUsedSection = showMostUsed && searchQuery.isBlank() && mostUsedApps.isNotEmpty()
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (showMostUsedSection) {
                    item(key = "most_used_header") {
                        Text(
                            text = "MOST USED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    items(mostUsedApps, key = { "mu_${it.packageName}" }) { app ->
                        DrawerAppRow(
                            app = app,
                            isFavorite = app.packageName in favoriteSet,
                            showIcons = showIcons,
                            context = context,
                            onLaunch = {
                                focusManager.clearFocus()
                                onAppLaunch(app)
                            },
                            onAddFavorite = { onAddFavorite(app) },
                            onRemoveFavorite = { onRemoveFavorite(app) },
                            onLongPress = { focusManager.clearFocus() }
                        )
                    }
                    item(key = "most_used_divider") {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                items(filteredApps, key = { it.packageName }) { app ->
                    DrawerAppRow(
                        app = app,
                        isFavorite = app.packageName in favoriteSet,
                        showIcons = showIcons,
                        context = context,
                        onLaunch = {
                            focusManager.clearFocus()
                            onAppLaunch(app)
                        },
                        onAddFavorite = { onAddFavorite(app) },
                        onRemoveFavorite = { onRemoveFavorite(app) },
                        onLongPress = { focusManager.clearFocus() },
                        modifier = Modifier.animateItem()
                    )
                }
                if (contactResults.isNotEmpty()) {
                    item(key = "contacts_divider") {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    item(key = "contacts_header") {
                        Text(
                            text = "CONTACTS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    items(contactResults, key = { "contact_${it.contactId}" }) { contact ->
                        ContactRow(
                            contact = contact,
                            onCall = {
                                focusManager.clearFocus()
                                contact.phoneNumber?.let { contactsRepository.dial(it) }
                            },
                            onSms = {
                                focusManager.clearFocus()
                                contact.phoneNumber?.let { contactsRepository.sms(it) }
                            },
                            onWhatsApp = {
                                focusManager.clearFocus()
                                contact.whatsAppDataId?.let { contactsRepository.openWhatsApp(it) }
                            },
                            onOpenCard = {
                                focusManager.clearFocus()
                                contactsRepository.openContactCard(contact)
                            },
                            modifier = Modifier.animateItem()
                        )
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

/** A contact search hit with quick actions. Call/SMS need a phone number,
 * WhatsApp needs a synced WhatsApp profile row; the contact card is always
 * reachable via the trailing button or the row itself. */
@Composable
private fun ContactRow(
    contact: ContactInfo,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onWhatsApp: () -> Unit,
    onOpenCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenCard() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = contact.displayName,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        val actionTint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        if (contact.phoneNumber != null) {
            IconButton(onClick = onCall) {
                Icon(Icons.Outlined.Call, contentDescription = "Call", tint = actionTint)
            }
            IconButton(onClick = onSms) {
                Icon(
                    Icons.AutoMirrored.Outlined.Message,
                    contentDescription = "SMS",
                    tint = actionTint
                )
            }
        }
        if (contact.whatsAppDataId != null) {
            IconButton(onClick = onWhatsApp) {
                Icon(Icons.Filled.Whatsapp, contentDescription = "WhatsApp", tint = actionTint)
            }
        }
        IconButton(onClick = onOpenCard) {
            Icon(Icons.Outlined.Person, contentDescription = "Open contact", tint = actionTint)
        }
    }
}

/** A single tappable app entry with its long-press menu. Shared by the
 * "most used" section and the full alphabetical list. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppRow(
    app: AppInfo,
    isFavorite: Boolean,
    showIcons: Boolean,
    context: Context,
    onLaunch: () -> Unit,
    onAddFavorite: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(app.packageName) {
                    detectTapGestures(
                        onTap = { onLaunch() },
                        onLongPress = {
                            onLongPress()
                            showMenu = true
                        }
                    )
                }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showIcons) {
                // Decode the icon lazily on IO, only for this visible row.
                val iconBitmap by produceState<ImageBitmap?>(null, app.packageName) {
                    value = withContext(Dispatchers.IO) {
                        AppRepository(context).loadIcon(app.packageName, app.activityName)
                            ?.toBitmap(48, 48)?.asImageBitmap()
                    }
                }
                iconBitmap?.let { bitmap ->
                    Image(
                        painter = BitmapPainter(bitmap),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
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

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = DpOffset(48.dp, (-100).dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = app.label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (isFavorite) "Remove from favorites" else "Add to favorites") },
                onClick = {
                    showMenu = false
                    if (isFavorite) onRemoveFavorite() else onAddFavorite()
                }
            )
            DropdownMenuItem(
                text = { Text("App info") },
                onClick = {
                    showMenu = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}
