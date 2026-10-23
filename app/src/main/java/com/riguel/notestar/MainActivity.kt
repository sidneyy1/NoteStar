package com.riguel.notestar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.riguel.notestar.data.NoteMeta
import com.riguel.notestar.data.NoteStorage
import com.riguel.notestar.data.PageTemplate
import com.riguel.notestar.ui.EditorScreen
import com.riguel.notestar.ui.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NoteStarApp()
            }
        }
    }
}

@Composable
fun NoteStarApp() {
    val context = LocalContext.current
    var editingId by remember { mutableStateOf<String?>(null) }
    var newTemplate by remember { mutableStateOf<PageTemplate?>(null) }
    var refresh by remember { mutableStateOf(0) }

    val notes = remember(refresh) { NoteStorage.listNotes(context) }

    if (newTemplate != null || editingId != null) {
        val loaded = remember(editingId) {
            editingId?.let { NoteStorage.loadNote(context, it) }
        }
        val template = loaded?.meta?.template ?: newTemplate ?: PageTemplate.BLANK
        EditorScreen(
            initialTitle = loaded?.meta?.title ?: "",
            initialTemplate = template,
            initialStrokes = loaded?.strokes ?: emptyList(),
            initialTexts = loaded?.texts ?: emptyList(),
            onBack = {
                newTemplate = null
                editingId = null
                refresh++
            },
            onSave = { title, strokes, texts ->
                val id = editingId ?: NoteStorage.newId()
                NoteStorage.saveNote(
                    context,
                    NoteMeta(
                        id = id,
                        title = title,
                        updatedAt = System.currentTimeMillis(),
                        template = template
                    ),
                    strokes,
                    texts
                )
                newTemplate = null
                editingId = null
                refresh++
            }
        )
    } else {
        HomeScreen(
            notes = notes,
            onCreate = { template -> newTemplate = template },
            onOpen = { id -> editingId = id },
            onDelete = { id ->
                NoteStorage.deleteNote(context, id)
                refresh++
            }
        )
    }
}
