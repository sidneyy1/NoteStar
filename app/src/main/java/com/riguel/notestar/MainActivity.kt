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
    var creatingNew by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }

    val notes = remember(refresh) { NoteStorage.listNotes(context) }

    if (creatingNew || editingId != null) {
        val loaded = remember(editingId) {
            editingId?.let { NoteStorage.loadNote(context, it) }
        }
        EditorScreen(
            initialTitle = loaded?.first?.title ?: "",
            initialStrokes = loaded?.second ?: emptyList(),
            onBack = {
                creatingNew = false
                editingId = null
                refresh++
            },
            onSave = { title, strokes ->
                val id = editingId ?: NoteStorage.newId()
                NoteStorage.saveNote(
                    context,
                    NoteMeta(id = id, title = title, updatedAt = System.currentTimeMillis()),
                    strokes
                )
                creatingNew = false
                editingId = null
                refresh++
            }
        )
    } else {
        HomeScreen(
            notes = notes,
            onCreate = { creatingNew = true },
            onOpen = { id -> editingId = id },
            onDelete = { id ->
                NoteStorage.deleteNote(context, id)
                refresh++
            }
        )
    }
}
