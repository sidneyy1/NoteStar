package com.riguel.notestar.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riguel.notestar.data.NoteMeta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatNoteDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMillis))

/** Small page thumbnail: cover color (or a soft tint) with page lines / a PDF tag. */
@Composable
fun NoteThumbnail(note: NoteMeta, large: Boolean = false) {
    val bg = note.coverColorValue?.let { Color(it) }
        ?: if (note.isPdf) Color(0xFFF6E3E3) else DesignTokens.AccentTint
    Box(
        modifier = Modifier
            .size(width = if (large) 74.dp else 46.dp, height = if (large) 96.dp else 58.dp)
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 8.dp)
    ) {
        if (note.isPdf) {
            Text(
                text = "PDF",
                color = Color(0xFFB5413C),
                fontSize = if (large) 13.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column {
                repeat(if (large) 6 else 4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(1.dp))
                    )
                    Spacer(modifier = Modifier.height(if (large) 7.dp else 5.dp))
                }
            }
        }
    }
}

/**
 * One row of the All notes list: thumbnail, title, date in small gray text,
 * and a chevron that opens the note's actions (Open / Edit tags / Delete).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListItem(
    note: NoteMeta,
    selectMode: Boolean,
    selected: Boolean,
    darkMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditTags: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val primary = if (darkMode) Color.White else DesignTokens.TextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(checkedColor = DesignTokens.Accent)
            )
        }
        NoteThumbnail(note)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title.ifBlank { "Untitled" },
                color = primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatNoteDate(note.updatedAt) +
                    " · " + note.pageCount + (if (note.pageCount == 1) " page" else " pages") +
                    if (note.tags.isNotEmpty()) " · " + note.tags.joinToString(", ") else "",
                color = DesignTokens.TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Note actions",
                    tint = DesignTokens.TextSecondary
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Open") },
                    onClick = {
                        showMenu = false
                        onClick()
                    },
                    leadingIcon = { Icon(Icons.Filled.OpenInNew, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Edit tags") },
                    onClick = {
                        showMenu = false
                        onEditTags()
                    },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }
                )
            }
        }
    }
}

/** Card variant used by the grid view of the All notes screen. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteGridItem(
    note: NoteMeta,
    selectMode: Boolean,
    selected: Boolean,
    darkMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val primary = if (darkMode) Color.White else DesignTokens.TextPrimary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignTokens.ItemRadius))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = if (darkMode) Color(0xFF262628) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                NoteThumbnail(note, large = true)
                if (selectMode) {
                    Spacer(modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onClick() },
                        colors = CheckboxDefaults.colors(checkedColor = DesignTokens.Accent)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = note.title.ifBlank { "Untitled" },
                color = primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatNoteDate(note.updatedAt),
                color = DesignTokens.TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
