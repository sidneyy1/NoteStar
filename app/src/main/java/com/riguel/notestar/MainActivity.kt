package com.riguel.notestar

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.riguel.notestar.data.BackupManager
import com.riguel.notestar.data.ExportCancelled
import com.riguel.notestar.data.ExportException
import com.riguel.notestar.data.ExportOptions
import com.riguel.notestar.data.PdfExportService
import com.riguel.notestar.data.NoteDocument
import com.riguel.notestar.data.NoteStorage
import com.riguel.notestar.data.Page
import com.riguel.notestar.data.PageTemplate
import com.riguel.notestar.ui.CreateNoteScreen
import com.riguel.notestar.ui.DocTab
import com.riguel.notestar.ui.EditorScreen
import com.riguel.notestar.ui.ExportDoneDialog
import com.riguel.notestar.ui.ExportOptionsSheet
import com.riguel.notestar.ui.ExportProgressDialog
import com.riguel.notestar.ui.HomeScreen
import com.riguel.notestar.ui.PdfNoteScreen
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Accent choices for the custom theme; the design accent is the first one. */
val ACCENT_COLORS = listOf(
    Color(0xFF1E8FB5), // teal-blue (design accent)
    Color(0xFF00897B), // sea green
    Color(0xFF6A1B9A), // purple
    Color(0xFFEF6C00), // orange
    Color(0xFFAD1457)  // pink
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        setContent {
            var darkMode by remember { mutableStateOf(prefs.getBoolean("darkMode", false)) }
            var accentIndex by remember {
                mutableStateOf(prefs.getInt("accentIndex", 0))
            }
            val accent = ACCENT_COLORS[accentIndex % ACCENT_COLORS.size]
            val scheme = if (darkMode) {
                darkColorScheme(primary = accent)
            } else {
                lightColorScheme(primary = accent)
            }
            MaterialTheme(colorScheme = scheme) {
                NoteStarApp(
                    darkMode = darkMode,
                    accentIndex = accentIndex,
                    onToggleDarkMode = {
                        darkMode = !darkMode
                        prefs.edit().putBoolean("darkMode", darkMode).apply()
                    },
                    onAccentSelected = { index ->
                        accentIndex = index
                        prefs.edit().putInt("accentIndex", index).apply()
                    },
                    onEnterPip = { enterPip() }
                )
            }
        }
    }

    /** Keep the current note visible in a floating window over other apps. */
    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                enterPictureInPictureMode(PictureInPictureParams.Builder().build())
            } catch (e: Exception) {
                // Some devices/ROMs refuse PiP; failing silently is fine.
            }
        }
    }
}

@Composable
fun NoteStarApp(
    darkMode: Boolean,
    accentIndex: Int,
    onToggleDarkMode: () -> Unit,
    onAccentSelected: (Int) -> Unit,
    onEnterPip: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Open documents live in browser-style tabs above the editors.
    var openTabs by remember { mutableStateOf<List<NoteDocument>>(emptyList()) }
    var activeTabId by remember { mutableStateOf<String?>(null) }
    var showHome by remember { mutableStateOf(true) }
    var showCreate by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    val notes = remember(refresh) { NoteStorage.listNotes(context) }

    val activeDoc = openTabs.firstOrNull { it.id == activeTabId }
    val docTabs = openTabs.map { DocTab(it.id, it.title, it.pdf != null) }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    // ---- Export as PDF ----
    var exportDoc by remember { mutableStateOf<NoteDocument?>(null) }
    var exportCurrentPage by remember { mutableStateOf(-1) }
    var exportProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var exportResult by remember { mutableStateOf<File?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var exportCancelledFlag by remember { mutableStateOf(false) }

    fun startExport(doc: NoteDocument, currentPage: Int) {
        exportCurrentPage = currentPage
        exportDoc = doc
    }

    fun runExport(options: ExportOptions) {
        val doc = exportDoc ?: return
        exportCancelledFlag = false
        exportProgress = 0 to 1
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    PdfExportService.export(
                        context = context,
                        doc = doc,
                        options = options,
                        currentPage = exportCurrentPage,
                        isCancelled = { exportCancelledFlag },
                        onProgress = { done, total ->
                            exportProgress = done to total
                        }
                    )
                }
                exportProgress = null
                exportDoc = null
                exportResult = file
            } catch (e: ExportCancelled) {
                exportProgress = null
                exportDoc = null
            } catch (e: ExportException) {
                exportProgress = null
                exportDoc = null
                exportError = e.message ?: "Export failed. Tap to retry"
            } catch (e: java.io.IOException) {
                exportProgress = null
                exportDoc = null
                exportError =
                    "Export failed — the file could not be written (storage may be full)."
            } catch (e: Exception) {
                exportProgress = null
                exportDoc = null
                exportError = "Export failed. Tap to retry"
            }
        }
    }

    fun shareExport(file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    }

    fun openExport(file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { notify("No PDF viewer app found") }
    }

    val exportSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val file = exportResult
        if (uri != null && file != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        file.inputStream().use { input -> input.copyTo(out) }
                    }
                }
            }
            notify("Saved ${file.name}")
        }
    }

    fun openInTab(doc: NoteDocument) {
        if (openTabs.none { it.id == doc.id }) {
            openTabs = openTabs + doc
        }
        activeTabId = doc.id
        showHome = false
    }

    fun closeTab(id: String) {
        val wasActive = id == activeTabId
        openTabs = openTabs.filterNot { it.id == id }
        if (wasActive) {
            val next = openTabs.lastOrNull()
            if (next != null) {
                activeTabId = next.id
            } else {
                activeTabId = null
                showHome = true
            }
        }
    }

    fun saveDoc(updated: NoteDocument, message: String) {
        NoteStorage.saveNote(context, updated)
        openTabs = openTabs.map { if (it.id == updated.id) updated else it }
        notify(message)
        refresh++
    }

    // Backup to shared storage (Documents/NoteStar). Android 8/9 needs the
    // storage permission first; Android 10+ needs none.
    val backupPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val count = BackupManager.backupAll(context)
            notify("Backed up $count note(s) to Documents/NoteStar")
        } else {
            notify("Storage permission is needed to save backups")
        }
    }
    LaunchedEffect(Unit) {
        // One-time: back up notes that already existed before backups shipped.
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("backup_migrated_v1", false)) {
            if (BackupManager.needsLegacyPermission(context)) {
                backupPermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                BackupManager.backupAll(context)
            }
            prefs.edit().putBoolean("backup_migrated_v1", true).apply()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            when {
                showCreate -> {
                    CreateNoteScreen(
                        onCancel = { showCreate = false },
                        onCreate = { template, title, tags, cover ->
                            val now = System.currentTimeMillis()
                            val doc = NoteDocument(
                                id = NoteStorage.newId(),
                                title = title.ifBlank { "Untitled" },
                                tags = tags,
                                createdAt = now,
                                updatedAt = now,
                                pages = listOf(Page.create(template)),
                                coverColorValue = cover
                            )
                            NoteStorage.saveNote(context, doc)
                            showCreate = false
                            openInTab(doc)
                            refresh++
                        }
                    )
                }
                !showHome && activeDoc != null -> {
                    key(activeDoc.id) {
                        if (activeDoc.pdf != null) {
                            PdfNoteScreen(
                                initialDoc = activeDoc,
                                pdfFile = NoteStorage.pdfFile(
                                    context, activeDoc.pdf.fileName
                                ),
                                onBack = {
                                    showHome = true
                                    refresh++
                                },
                                onSave = { updated -> saveDoc(updated, "Annotations saved") },
                                onExportRequest = { page ->
                                    NoteStorage.loadNote(context, activeDoc.id)?.let {
                                        startExport(it, page)
                                    }
                                },
                                onEnterPip = onEnterPip,
                                tabs = docTabs,
                                onTabSelect = { id -> activeTabId = id },
                                onTabClose = { id -> closeTab(id) }
                            )
                        } else {
                            EditorScreen(
                                initialDoc = activeDoc,
                                onBack = {
                                    showHome = true
                                    refresh++
                                },
                                onSave = { updated -> saveDoc(updated, "Note saved") },
                                onExportRequest = { page ->
                                    NoteStorage.loadNote(context, activeDoc.id)?.let {
                                        startExport(it, page)
                                    }
                                },
                                onExcerptPage = { excerptTitle, page ->
                                    val now = System.currentTimeMillis()
                                    val excerpt = NoteDocument(
                                        id = NoteStorage.newId(),
                                        title = excerptTitle,
                                        createdAt = now,
                                        updatedAt = now,
                                        pages = listOf(
                                            page.copy(
                                                id = "p" + System.nanoTime().toString()
                                            )
                                        )
                                    )
                                    NoteStorage.saveNote(context, excerpt)
                                    notify("Page saved as a new note: $excerptTitle")
                                    refresh++
                                },
                                onEnterPip = onEnterPip,
                                tabs = docTabs,
                                onTabSelect = { id -> activeTabId = id },
                                onTabClose = { id -> closeTab(id) }
                            )
                        }
                    }
                }
                else -> {
                    HomeScreen(
                        notes = notes,
                        darkMode = darkMode,
                        accentColors = ACCENT_COLORS,
                        accentIndex = accentIndex,
                        onToggleDarkMode = onToggleDarkMode,
                        onAccentSelected = onAccentSelected,
                        onOpen = { meta ->
                            NoteStorage.loadNote(context, meta.id)?.let { openInTab(it) }
                        },
                        onShowCreate = { showCreate = true },
                        onQuickCreate = {
                            val now = System.currentTimeMillis()
                            val doc = NoteDocument(
                                id = NoteStorage.newId(),
                                title = "",
                                createdAt = now,
                                updatedAt = now,
                                pages = listOf(Page.create(PageTemplate.BLANK))
                            )
                            NoteStorage.saveNote(context, doc)
                            openInTab(doc)
                            refresh++
                        },
                        onDelete = { meta ->
                            NoteStorage.deleteNote(context, meta.id)
                            openTabs = openTabs.filterNot { it.id == meta.id }
                            if (activeTabId == meta.id) {
                                activeTabId = openTabs.lastOrNull()?.id
                            }
                            refresh++
                        },
                        onEditTags = { meta, tags ->
                            val loaded = NoteStorage.loadNote(context, meta.id)
                            if (loaded != null) {
                                val updated = loaded.copy(tags = tags)
                                NoteStorage.saveNote(context, updated)
                                openTabs = openTabs.map {
                                    if (it.id == updated.id) updated else it
                                }
                                refresh++
                            }
                        },
                        onExportNote = { meta ->
                            NoteStorage.loadNote(context, meta.id)?.let {
                                startExport(it, -1)
                            }
                        },
                        onImportPdf = { input, displayName ->
                            val imported = NoteStorage.importPdf(context, input, displayName)
                            if (imported != null) {
                                notify("Imported \"${imported.title}\" — tap it to annotate")
                                refresh++
                            } else {
                                notify("That file could not be read as a PDF")
                            }
                        },
                        onBackupNow = {
                            if (BackupManager.needsLegacyPermission(context)) {
                                backupPermLauncher.launch(
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            } else {
                                val count = BackupManager.backupAll(context)
                                notify("Backed up $count note(s) to Documents/NoteStar")
                            }
                        },
                        onRestoreBackup = {
                            val count = BackupManager.restoreAll(context)
                            if (count > 0) {
                                notify("Restored $count note(s) from the backup")
                            } else {
                                notify("No new notes found in the backup")
                            }
                            refresh++
                        }
                    )
                }
            }
        }

        // ---- Export as PDF UI ----
        val sheetDoc = exportDoc
        if (sheetDoc != null && exportProgress == null && exportResult == null) {
            ExportOptionsSheet(
                docTitle = sheetDoc.title.ifBlank { "Untitled" },
                pageCount = sheetDoc.pdf?.pageCount ?: sheetDoc.pages.size,
                hasCurrentPage = exportCurrentPage >= 0,
                onDismiss = { exportDoc = null },
                onExport = { options -> runExport(options) }
            )
        }
        exportProgress?.let { (done, total) ->
            ExportProgressDialog(
                done = done,
                total = total,
                onCancel = { exportCancelledFlag = true }
            )
        }
        exportResult?.let { file ->
            ExportDoneDialog(
                fileName = file.name,
                onShare = { shareExport(file) },
                onOpen = { openExport(file) },
                onSaveToDevice = { exportSaveLauncher.launch(file.name) },
                onDismiss = { exportResult = null }
            )
        }
        exportError?.let { message ->
            AlertDialog(
                onDismissRequest = { exportError = null },
                title = { Text("Export as PDF") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { exportError = null }) { Text("OK") }
                }
            )
        }
    }
}
