package com.riguel.notestar.ui

import android.content.Context
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riguel.notestar.data.NoteMeta
import com.riguel.notestar.data.NotificationCenter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStream

private enum class NoteSort { NEWEST, OLDEST, TITLE }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    notes: List<NoteMeta>,
    darkMode: Boolean,
    accentColors: List<Color>,
    accentIndex: Int,
    onToggleDarkMode: () -> Unit,
    onAccentSelected: (Int) -> Unit,
    onOpen: (NoteMeta) -> Unit,
    onShowCreate: () -> Unit,
    onQuickCreate: () -> Unit,
    onDelete: (NoteMeta) -> Unit,
    onEditTags: (NoteMeta, List<String>) -> Unit,
    onExportNote: (NoteMeta) -> Unit,
    onImportPdf: (InputStream, String) -> Unit,
    onBackupNow: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }
    var selection by remember { mutableStateOf(SidebarKeys.ALL) }
    var sidebarOverride by remember { mutableStateOf<Boolean?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var segment by remember { mutableStateOf(0) } // 0 ALL, 1 NOTE, 2 PDF
    var gridView by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(NoteSort.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var bannerDismissed by remember {
        mutableStateOf(prefs.getBoolean("backup_banner_dismissed", false))
    }
    var showSaveConfirmations by remember {
        mutableStateOf(prefs.getBoolean("show_save_confirmations", true))
    }
    var snapToGuides by remember {
        mutableStateOf(prefs.getBoolean("snap_to_guides", true))
    }
    // The menu owns the ONLY SnackbarHost for quiet messages. Editors
    // never host one, so nothing can flash while an editor is on top.
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    // Drain on every notes refresh (import/backup from the menu) and on
    // first composition (returning from an editor). Each message shows
    // about 2 seconds and is swipe-dismissible (SnackbarHost default).
    androidx.compose.runtime.LaunchedEffect(notes) {
        val events = NotificationCenter.drain(
            System.currentTimeMillis(), showSaveConfirmations
        )
        for (event in events) {
            val job = snackbarScope.launch {
                snackbarHostState.showSnackbar(
                    event.text, duration = SnackbarDuration.Short
                )
            }
            snackbarScope.launch {
                delay(2000)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            job.join()
        }
    }
    var showSettings by remember { mutableStateOf(false) }
    var showBackup by remember { mutableStateOf(false) }
    var showStore by remember { mutableStateOf(false) }
    var showFolderHint by remember { mutableStateOf(false) }
    var showTagHint by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<NoteMeta?>(null) }
    var showDeleteSelected by remember { mutableStateOf(false) }
    var noteForTags by remember { mutableStateOf<NoteMeta?>(null) }
    var tagsInput by remember { mutableStateOf("") }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var displayName = "Document.pdf"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) {
                        displayName = cursor.getString(index) ?: displayName
                    }
                }
            } catch (e: Exception) {
                // Keep the fallback name.
            }
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    onImportPdf(input, displayName)
                }
            } catch (e: Exception) {
                // Import failures are reported by the caller via snackbar.
            }
        }
    }

    val allTags = notes.flatMap { it.tags }.distinct().sorted()
    val tagCounts = allTags.associateWith { tag -> notes.count { tag in it.tags } }

    val baseNotes = when {
        selection == SidebarKeys.TRASH -> emptyList()
        selection == SidebarKeys.SEARCH -> notes.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }
        selection == SidebarKeys.UNTAGGED -> notes.filter { it.tags.isEmpty() }
        selection.startsWith("tag:") -> {
            val tag = selection.removePrefix("tag:")
            notes.filter { tag in it.tags }
        }
        else -> notes
    }
    val segmented = when (segment) {
        1 -> baseNotes.filter { !it.isPdf }
        2 -> baseNotes.filter { it.isPdf }
        else -> baseNotes
    }
    val visibleNotes = when (sort) {
        NoteSort.NEWEST -> segmented.sortedByDescending { it.updatedAt }
        NoteSort.OLDEST -> segmented.sortedBy { it.updatedAt }
        NoteSort.TITLE -> segmented.sortedBy { it.title.lowercase() }
    }

    val headerTitle = when {
        selection == SidebarKeys.TRASH -> "Trash"
        selection == SidebarKeys.SEARCH -> "Search"
        selection == SidebarKeys.UNTAGGED -> "Untagged"
        selection.startsWith("tag:") -> selection.removePrefix("tag:")
        else -> "All notes"
    }
    val primaryText = if (darkMode) Color.White else DesignTokens.TextPrimary
    val background = if (darkMode) Color(0xFF121214) else DesignTokens.Background

    fun toggleSelected(note: NoteMeta) {
        selectedIds = if (note.id in selectedIds) {
            selectedIds - note.id
        } else {
            selectedIds + note.id
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        // Hoisted: maxWidth (a BoxWithConstraints receiver value) cannot be
        // read inside nested layout lambdas below.
        val containerWidth = maxWidth
        val collapsed = sidebarOverride ?: (containerWidth < 600.dp)
        Row(modifier = Modifier.fillMaxSize()) {
            // ---- Sidebar ----
            Box(modifier = Modifier.width(if (collapsed) 56.dp else (containerWidth * 0.24f).coerceIn(210.dp, 300.dp))) {
                if (collapsed) {
                    SidebarRail(
                        selection = selection,
                        darkMode = darkMode,
                        onExpand = { sidebarOverride = false },
                        onSelectAll = { selection = SidebarKeys.ALL },
                        onSelectTrash = { selection = SidebarKeys.TRASH },
                        onSelectSearch = { selection = SidebarKeys.SEARCH },
                        onSettings = { showSettings = true }
                    )
                } else {
                    Sidebar(
                        totalNotes = notes.size,
                        untaggedCount = notes.count { it.tags.isEmpty() },
                        tags = allTags,
                        tagCounts = tagCounts,
                        selection = selection,
                        darkMode = darkMode,
                        onSelectAll = { selection = SidebarKeys.ALL },
                        onSelectUntagged = { selection = SidebarKeys.UNTAGGED },
                        onSelectTrash = { selection = SidebarKeys.TRASH },
                        onSelectSearch = { selection = SidebarKeys.SEARCH },
                        onSelectTag = { selection = SidebarKeys.tag(it) },
                        onCollapse = { sidebarOverride = true },
                        onSettings = { showSettings = true },
                        onStore = { showStore = true },
                        onAddFolder = { showFolderHint = true },
                        onAddTag = { showTagHint = true }
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(if (darkMode) Color(0xFF2A2A2C) else DesignTokens.Divider)
            )

            // ---- Main content ----
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = DesignTokens.ScreenPadding, vertical = 12.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = headerTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { gridView = !gridView }) {
                        Icon(
                            if (gridView) Icons.Filled.ViewList else Icons.Filled.GridView,
                            contentDescription = "Toggle list / grid view",
                            tint = primaryText
                        )
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort", tint = primaryText)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Newest first") },
                                onClick = { sort = NoteSort.NEWEST; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Oldest first") },
                                onClick = { sort = NoteSort.OLDEST; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Title A–Z") },
                                onClick = { sort = NoteSort.TITLE; showSortMenu = false }
                            )
                        }
                    }
                    IconButton(onClick = {
                        selectMode = !selectMode
                        if (!selectMode) selectedIds = emptySet()
                    }) {
                        Icon(
                            Icons.Filled.CheckBox,
                            contentDescription = "Multi-select",
                            tint = if (selectMode) DesignTokens.Accent else primaryText
                        )
                    }
                }

                if (selectMode) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${selectedIds.size} selected",
                            color = DesignTokens.TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { showDeleteSelected = true },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Text("Delete selected", color = Color(0xFFC62828))
                        }
                        TextButton(onClick = {
                            selectMode = false
                            selectedIds = emptySet()
                        }) { Text("Done", color = DesignTokens.Accent) }
                    }
                }

                // Backup info banner
                if (!bannerDismissed && selection == SidebarKeys.ALL && !selectMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(DesignTokens.ItemRadius))
                            .background(
                                if (darkMode) Color(0xFF17323D) else DesignTokens.BannerBackground
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = DesignTokens.Accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notes are backed up on this device.",
                            color = primaryText,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showBackup = true }) {
                            Text("Go to Backup >", color = DesignTokens.Accent, fontSize = 13.sp)
                        }
                        IconButton(
                            onClick = {
                                bannerDismissed = true
                                prefs.edit().putBoolean("backup_banner_dismissed", true).apply()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Dismiss",
                                tint = DesignTokens.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Segmented tabs ALL / NOTE / PDF
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("ALL", "NOTE", "PDF").forEachIndexed { index, label ->
                        Column(
                            modifier = Modifier
                                .clickable { segment = index }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (segment == index) FontWeight.Medium else FontWeight.Normal,
                                color = if (segment == index) DesignTokens.Accent else DesignTokens.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .height(2.dp)
                                    .background(
                                        if (segment == index) DesignTokens.Accent else Color.Transparent
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }) {
                        Text("Import PDF", color = DesignTokens.Accent, fontSize = 13.sp)
                    }
                }
                HorizontalDivider(color = if (darkMode) Color(0xFF2A2A2C) else DesignTokens.Divider)

                if (selection == SidebarKeys.SEARCH) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search notes by title") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selection == SidebarKeys.TRASH) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Trash is empty.\nDeleted notes are removed permanently.",
                            color = DesignTokens.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    // Create card
                    if (!selectMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(DesignTokens.CardRadius))
                                .background(if (darkMode) Color(0xFF1D1D1F) else Color.White)
                                .dashedBorder(DesignTokens.Accent, DesignTokens.CardRadius)
                                .combinedClickable(
                                    onClick = onShowCreate,
                                    onDoubleClick = onQuickCreate
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .dashedBorder(DesignTokens.Accent, 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Create note",
                                    tint = DesignTokens.Accent
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Create",
                                    color = DesignTokens.Accent,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "Tap twice to create a quick note",
                                    color = DesignTokens.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (visibleNotes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (notes.isEmpty()) {
                                    "No notes yet. Create your first note above, or import a PDF."
                                } else {
                                    "Nothing matches here."
                                },
                                color = DesignTokens.TextSecondary
                            )
                        }
                    } else if (gridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (containerWidth >= 900.dp) 3 else 2),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(visibleNotes) { note ->
                                Box(modifier = Modifier.padding(6.dp)) {
                                    NoteGridItem(
                                        note = note,
                                        selectMode = selectMode,
                                        selected = note.id in selectedIds,
                                        darkMode = darkMode,
                                        onClick = {
                                            if (selectMode) toggleSelected(note) else onOpen(note)
                                        },
                                        onLongClick = {
                                            selectMode = true
                                            toggleSelected(note)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(visibleNotes) { note ->
                                NoteListItem(
                                    note = note,
                                    selectMode = selectMode,
                                    selected = note.id in selectedIds,
                                    darkMode = darkMode,
                                    onClick = {
                                        if (selectMode) toggleSelected(note) else onOpen(note)
                                    },
                                    onLongClick = {
                                        selectMode = true
                                        toggleSelected(note)
                                    },
                                    onEditTags = {
                                        noteForTags = note
                                        tagsInput = note.tags.joinToString(", ")
                                    },
                                    onExport = { onExportNote(note) },
                                    onDelete = { noteToDelete = note }
                                )
                                HorizontalDivider(
                                    color = if (darkMode) Color(0xFF2A2A2C) else DesignTokens.Divider,
                                    modifier = Modifier.padding(start = 62.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // ---- Dialogs ----
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete this note?") },
            text = { Text("\"${note.title.ifBlank { "Untitled" }}\" will be permanently deleted, including its backup copy.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(note)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteSelected) {
        AlertDialog(
            onDismissRequest = { showDeleteSelected = false },
            title = { Text("Delete ${selectedIds.size} note(s)?") },
            text = { Text("The selected notes will be permanently deleted, including their backup copies.") },
            confirmButton = {
                Button(
                    onClick = {
                        notes.filter { it.id in selectedIds }.forEach { onDelete(it) }
                        selectedIds = emptySet()
                        selectMode = false
                        showDeleteSelected = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelected = false }) { Text("Cancel") }
            }
        )
    }

    noteForTags?.let { note ->
        AlertDialog(
            onDismissRequest = { noteForTags = null },
            title = { Text("Edit tags") },
            text = {
                Column {
                    Text("Separate tags with commas.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tags = tagsInput.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                        onEditTags(note, tags)
                        noteForTags = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Accent)
                ) { Text("Save tags") }
            },
            dismissButton = {
                TextButton(onClick = { noteForTags = null }) { Text("Cancel") }
            }
        )
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Settings") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark mode", modifier = Modifier.weight(1f))
                        Switch(
                            checked = darkMode,
                            onCheckedChange = { onToggleDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = DesignTokens.Accent,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Notifications", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Show save confirmations",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = showSaveConfirmations,
                            onCheckedChange = { on ->
                                showSaveConfirmations = on
                                prefs.edit().putBoolean(
                                    "show_save_confirmations", on
                                ).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = DesignTokens.Accent,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                    Text(
                        "On: 'Note saved' appears only on the All notes " +
                            "screen, after returning from an edited note. " +
                            "Off: never. Failures and export results " +
                            "always show, wherever you are.",
                        color = DesignTokens.TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Snap dragged text to guides",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = snapToGuides,
                            onCheckedChange = { on ->
                                snapToGuides = on
                                prefs.edit().putBoolean(
                                    "snap_to_guides", on
                                ).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = DesignTokens.Accent,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                    Text(
                        "On: while dragging a text box, its edges and " +
                            "center snap to the page center, page edges " +
                            "and other boxes, showing thin teal guides. " +
                            "Off: the box follows the finger freely.",
                        color = DesignTokens.TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Accent color")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        accentColors.forEachIndexed { index, color ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (index == accentIndex) 3.dp else 1.dp,
                                        color = if (index == accentIndex) {
                                            DesignTokens.TextPrimary
                                        } else {
                                            Color.Gray
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable { onAccentSelected(index) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Backups live in Documents/NoteStar on this device.",
                        color = DesignTokens.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) { Text("Done") }
            }
        )
    }

    if (showBackup) {
        AlertDialog(
            onDismissRequest = { showBackup = false },
            title = { Text("Backup") },
            text = {
                Text(
                    "Every save is copied to Documents/NoteStar, so notes survive " +
                        "uninstalling the app. After a reinstall, restore them here."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackup = false
                        onBackupNow()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Accent)
                ) { Text("Backup now") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackup = false
                    onRestoreBackup()
                }) { Text("Restore backup") }
            }
        )
    }

    if (showStore) {
        AlertDialog(
            onDismissRequest = { showStore = false },
            title = { Text("Store") },
            text = { Text("NoteStar is free and works fully offline — there's no store yet, everything is already included.") },
            confirmButton = {
                TextButton(onClick = { showStore = false }) { Text("OK") }
            }
        )
    }

    if (showFolderHint) {
        AlertDialog(
            onDismissRequest = { showFolderHint = false },
            title = { Text("Folders") },
            text = { Text("Folders aren't available yet. All notes live under All notes — use tags to organize them.") },
            confirmButton = {
                TextButton(onClick = { showFolderHint = false }) { Text("OK") }
            }
        )
    }

    if (showTagHint) {
        AlertDialog(
            onDismissRequest = { showTagHint = false },
            title = { Text("Tags") },
            text = { Text("Add tags when creating a note, or from a note's chevron menu in the list (Edit tags).") },
            confirmButton = {
                TextButton(onClick = { showTagHint = false }) { Text("OK") }
            }
        )
    }
}
