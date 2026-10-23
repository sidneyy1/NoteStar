package com.riguel.notestar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.riguel.notestar.data.NoteMeta
import com.riguel.notestar.data.PageTemplate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    notes: List<NoteMeta>,
    onCreate: (PageTemplate) -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    var showTemplatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "NoteStar", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "Handwritten and typed notes, saved on this device",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { showTemplatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("New note")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (notes.isEmpty()) {
            Text("No notes yet. Create your first note to start writing.")
        } else {
            LazyColumn {
                items(notes, key = { it.id }) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onOpen(note.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = (if (note.template == PageTemplate.LINED) "Lined page" else "Blank page") +
                                        " · " + dateFormat.format(Date(note.updatedAt)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = { onDelete(note.id) }) {
                                Text("Delete")
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
            title = { Text("Choose a page style") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showTemplatePicker = false
                            onCreate(PageTemplate.BLANK)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Blank page — clean white page")
                    }
                    TextButton(
                        onClick = {
                            showTemplatePicker = false
                            onCreate(PageTemplate.LINED)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Lined page — horizontal lines with a margin")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTemplatePicker = false }) { Text("Cancel") }
            }
        )
    }
}
