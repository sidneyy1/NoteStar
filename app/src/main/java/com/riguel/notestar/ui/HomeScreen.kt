package com.riguel.notestar.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.riguel.notestar.data.NoteMeta
import com.riguel.notestar.data.PageTemplate
import java.io.InputStream
import java.text.DateFormat
import java.util.Date

private fun templateLabel(template: PageTemplate): String = when (template) {
    PageTemplate.BLANK -> "Blank page"
    PageTemplate.LINED -> "Lined page"
    PageTemplate.DOTTED -> "Mind map (dot grid)"
    PageTemplate.PLANNER -> "Planner"
    PageTemplate.LECTURE -> "Lecture notes"
}

@Composable
fun HomeScreen(
    notes: List<NoteMeta>,
    darkMode: Boolean,
    accentColors: List<Color>,
    accentIndex: Int,
    onToggleDarkMode: () -> Unit,
    onAccentSelected: (Int) -> Unit,
    onOpen: (NoteMeta) -> Unit,
    onCreate: (PageTemplate, String) -> Unit,
    onDelete: (NoteMeta) -> Unit,
    onEditTags: (NoteMeta, List<String>) -> Unit,
    onImportPdf: (InputStream, String) -> Unit
) {
    var showTemplatePicker by remember { mutableStateOf(false) }
    var pendingTemplate by remember { mutableStateOf<PageTemplate?>(null) }
    var newTitle by remember { mutableStateOf("") }
    var noteToDelete by remember { mutableStateOf<NoteMeta?>(null) }
    var noteForTags by remember { mutableStateOf<NoteMeta?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
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
    val visibleNotes = if (selectedTag == null) {
        notes
    } else {
        notes.filter { selectedTag in it.tags }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showTemplatePicker = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "NoteStar",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                // Accent color choices (custom theme)
                accentColors.forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (index == accentIndex) 3.dp else 1.dp,
                                color = if (index == accentIndex) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    Color.Gray
                                },
                                shape = CircleShape
                            )
                            .clickable { onAccentSelected(index) }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                IconButton(onClick = onToggleDarkMode) {
                    Icon(
                        if (darkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "Toggle dark theme"
                    )
                }
            }
            Text(
                text = "Your notebooks — stored only on this device.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }) {
                Text("Import PDF to annotate")
            }

            // Tag filter
            if (allTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { selectedTag = null },
                        label = { Text("All") }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    allTags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = if (selectedTag == tag) null else tag },
                            label = { Text(tag) }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (visibleNotes.isEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (notes.isEmpty()) {
                        "No notes yet. Tap + to create your first note, or import a PDF."
                    } else {
                        "No notes with this tag."
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(visibleNotes) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onOpen(note) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.title.ifBlank { "Untitled" },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = if (note.isPdf) {
                                            "PDF · ${note.pageCount} pages"
                                        } else {
                                            "${templateLabel(note.template)} · ${note.pageCount} page(s)"
                                        },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = DateFormat.getDateTimeInstance(
                                            DateFormat.MEDIUM,
                                            DateFormat.SHORT
                                        ).format(Date(note.updatedAt)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (note.tags.isNotEmpty()) {
                                        Text(
                                            text = note.tags.joinToString("  ") { "#$it" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                IconButton(onClick = { noteForTags = note }) {
                                    Icon(Icons.Filled.Label, contentDescription = "Edit tags")
                                }
                                IconButton(onClick = { noteToDelete = note }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete note")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("New note") },
            text = {
                Column {
                    Text("Choose a page template:")
                    Spacer(modifier = Modifier.height(8.dp))
                    PageTemplate.entries.forEach { template ->
                        TextButton(onClick = {
                            showTemplatePicker = false
                            pendingTemplate = template
                            newTitle = ""
                        }) {
                            Text(templateLabel(template))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTemplatePicker = false }) { Text("Cancel") }
            }
        )
    }

    val templateForTitle = pendingTemplate
    if (templateForTitle != null) {
        AlertDialog(
            onDismissRequest = { pendingTemplate = null },
            title = { Text("Name your note") },
            text = {
                Column {
                    Text("Template: ${templateLabel(templateForTitle)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = { Text("Untitled") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreate(templateForTitle, newTitle.trim())
                    pendingTemplate = null
                }) { Text("Create note") }
            },
            dismissButton = {
                TextButton(onClick = { pendingTemplate = null }) { Text("Cancel") }
            }
        )
    }

    val deleteTarget = noteToDelete
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete note?") },
            text = {
                Text("\"${deleteTarget.title.ifBlank { "Untitled" }}\" will be permanently deleted.")
            },
            confirmButton = {
                Button(onClick = {
                    onDelete(deleteTarget)
                    noteToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Cancel") }
            }
        )
    }

    val tagsTarget = noteForTags
    if (tagsTarget != null) {
        var tagsText by remember(tagsTarget.id) {
            mutableStateOf(tagsTarget.tags.joinToString(", "))
        }
        AlertDialog(
            onDismissRequest = { noteForTags = null },
            title = { Text("Tags") },
            text = {
                Column {
                    Text("Separate tags with commas, e.g. school, exam")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tagsText,
                        onValueChange = { tagsText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = tagsText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    onEditTags(tagsTarget, parsed)
                    noteForTags = null
                }) { Text("Save tags") }
            },
            dismissButton = {
                TextButton(onClick = { noteForTags = null }) { Text("Cancel") }
            }
        )
    }
}
