package com.riguel.notestar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewSidebar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** One open document, shown in the browser-style tab bar of the editors. */
data class DocTab(
    val id: String,
    val title: String,
    val isPdf: Boolean
)

/**
 * Browser-style tabs for open documents: the active tab is white, inactive
 * tabs are light gray, each with a close button, plus a "N v" overflow menu
 * listing every open document.
 */
@Composable
fun DocumentTabs(
    tabs: List<DocTab>,
    activeTabId: String,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit
) {
    var showOverflow by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DesignTokens.TabBarBackground)
            .padding(start = 8.dp, top = 6.dp, end = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom
        ) {
            tabs.forEach { tab ->
                val active = tab.id == activeTabId
                Row(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = 0.dp,
                                bottomEnd = 0.dp
                            )
                        )
                        .background(if (active) Color.White else DesignTokens.TabInactive)
                        .clickable { onSelect(tab.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tab.title.ifBlank { "Untitled" },
                        color = if (active) {
                            DesignTokens.TextPrimary
                        } else {
                            DesignTokens.TextSecondary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 130.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close tab",
                        tint = DesignTokens.TextSecondary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onClose(tab.id) }
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        Box {
            Row(
                modifier = Modifier
                    .clickable { showOverflow = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = tabs.size.toString(), color = DesignTokens.TextSecondary)
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = "All open documents",
                    tint = DesignTokens.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = showOverflow,
                onDismissRequest = { showOverflow = false }
            ) {
                tabs.forEach { tab ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                tab.title.ifBlank { "Untitled" } +
                                    if (tab.isPdf) " (PDF)" else ""
                            )
                        },
                        onClick = {
                            showOverflow = false
                            onSelect(tab.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    description: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(
                if (active) DesignTokens.AccentTint else Color.Transparent,
                CircleShape
            )
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (active) DesignTokens.Accent else DesignTokens.TextPrimary,
            modifier = Modifier.size(21.dp)
        )
    }
}

/**
 * The slim editor toolbar under the tab bar: back + microphone on the left;
 * reader mode, layers, add page, side panel, adjust and the "..." menu
 * (Rename / Save / Picture in picture) on the right.
 */
@Composable
fun EditorToolbar(
    onBack: () -> Unit,
    onMic: () -> Unit,
    onReader: () -> Unit,
    readerActive: Boolean,
    onLayers: () -> Unit,
    layersActive: Boolean = false,
    onAddPage: (() -> Unit)? = null,
    onSidePanel: () -> Unit,
    onAdjust: () -> Unit,
    adjustActive: Boolean = false,
    onRename: () -> Unit,
    onSave: () -> Unit,
    onEnterPip: () -> Unit,
    onExcerpt: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    onFitToggle: (() -> Unit)? = null,
    fitIsWidth: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onClick = onBack)
        ToolbarButton(Icons.Filled.Mic, "Voice input", onClick = onMic)
        Spacer(modifier = Modifier.weight(1f))
        ToolbarButton(
            Icons.Filled.MenuBook,
            "Reader mode",
            active = readerActive,
            onClick = onReader
        )
        ToolbarButton(
            Icons.Filled.Layers,
            "Layers",
            active = layersActive,
            onClick = onLayers
        )
        if (onAddPage != null) {
            ToolbarButton(Icons.Filled.NoteAdd, "Add page", onClick = onAddPage)
        }
        ToolbarButton(Icons.Filled.ViewSidebar, "Side panel / outline", onClick = onSidePanel)
        ToolbarButton(
            Icons.Filled.Tune,
            "Adjust",
            active = adjustActive,
            onClick = onAdjust
        )
        Box {
            ToolbarButton(Icons.Filled.MoreVert, "More", onClick = { showMenu = true })
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        showMenu = false
                        onRename()
                    },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Save") },
                    onClick = {
                        showMenu = false
                        onSave()
                    },
                    leadingIcon = { Icon(Icons.Filled.Save, contentDescription = null) }
                )
                if (onExport != null) {
                    DropdownMenuItem(
                        text = { Text("Export as PDF") },
                        onClick = {
                            showMenu = false
                            onExport()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        }
                    )
                }
                if (onFitToggle != null) {
                    DropdownMenuItem(
                        text = { Text(if (fitIsWidth) "Fit page" else "Fit width") },
                        onClick = {
                            showMenu = false
                            onFitToggle()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.FitScreen, contentDescription = null)
                        }
                    )
                }
                if (onExcerpt != null) {
                    DropdownMenuItem(
                        text = { Text("Save page as new note") },
                        onClick = {
                            showMenu = false
                            onExcerpt()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.ContentCut, contentDescription = null)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Picture in picture") },
                    onClick = {
                        showMenu = false
                        onEnterPip()
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.PictureInPicture, contentDescription = null)
                    }
                )
            }
        }
    }
}
