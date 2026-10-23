package com.riguel.notestar

import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.riguel.notestar.data.NoteDocument
import com.riguel.notestar.data.NoteStorage
import com.riguel.notestar.data.Page
import com.riguel.notestar.data.PageTemplate
import com.riguel.notestar.ui.EditorScreen
import com.riguel.notestar.ui.HomeScreen
import com.riguel.notestar.ui.PdfNoteScreen
import kotlinx.coroutines.launch

/** Accent choices for the custom theme. */
val ACCENT_COLORS = listOf(
    Color(0xFF1565C0), // blue
    Color(0xFF00897B), // teal
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

    var openDoc by remember { mutableStateOf<NoteDocument?>(null) }
    var newTemplate by remember { mutableStateOf<PageTemplate?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val notes = remember(refresh) { NoteStorage.listNotes(context) }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val doc = openDoc
    val creating = newTemplate != null

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
    when {
        creating || doc != null -> {
            val initialDoc = remember(doc?.id, newTemplate) {
                doc ?: NoteDocument(
                    id = NoteStorage.newId(),
                    title = "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    pages = listOf(Page.create(newTemplate ?: PageTemplate.BLANK))
                )
            }
            if (initialDoc.pdf != null) {
                PdfNoteScreen(
                    initialDoc = initialDoc,
                    pdfFile = NoteStorage.pdfFile(context, initialDoc.pdf.fileName),
                    onBack = {
                        openDoc = null
                        newTemplate = null
                        refresh++
                    },
                    onSave = { updated ->
                        NoteStorage.saveNote(context, updated)
                        notify("Annotations saved")
                        refresh++
                    },
                    onEnterPip = onEnterPip
                )
            } else {
                EditorScreen(
                    initialDoc = initialDoc,
                    onBack = {
                        openDoc = null
                        newTemplate = null
                        refresh++
                    },
                    onSave = { updated ->
                        NoteStorage.saveNote(context, updated)
                        if (openDoc == null) openDoc = updated
                        notify("Note saved")
                        refresh++
                    },
                    onExcerptPage = { excerptTitle, page ->
                        val now = System.currentTimeMillis()
                        val excerpt = NoteDocument(
                            id = NoteStorage.newId(),
                            title = excerptTitle,
                            createdAt = now,
                            updatedAt = now,
                            pages = listOf(
                                page.copy(id = "p" + System.nanoTime().toString())
                            )
                        )
                        NoteStorage.saveNote(context, excerpt)
                        notify("Page saved as a new note: $excerptTitle")
                        refresh++
                    },
                    onEnterPip = onEnterPip
                )
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
                            openDoc = NoteStorage.loadNote(context, meta.id)
                        },
                        onCreate = { template -> newTemplate = template },
                        onDelete = { meta ->
                            NoteStorage.deleteNote(context, meta.id)
                            refresh++
                        },
                        onEditTags = { meta, tags ->
                            val loaded = NoteStorage.loadNote(context, meta.id)
                            if (loaded != null) {
                                NoteStorage.saveNote(context, loaded.copy(tags = tags))
                                refresh++
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
                        }
                    )
                }
            }
        }
    }
}
