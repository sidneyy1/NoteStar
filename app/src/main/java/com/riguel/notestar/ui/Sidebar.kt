package com.riguel.notestar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Which sidebar entry is currently driving the main area. */
object SidebarKeys {
    const val ALL = "all"
    const val UNTAGGED = "untagged"
    const val TRASH = "trash"
    const val SEARCH = "search"
    fun tag(name: String) = "tag:$name"
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    darkMode: Boolean,
    count: Int? = null,
    indent: Boolean = false,
    trailingChevron: Boolean = false,
    onClick: () -> Unit
) {
    val primary = if (darkMode) Color.White else DesignTokens.TextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) DesignTokens.AccentTint else Color.Transparent)
            .clickable { onClick() }
            .padding(
                start = if (indent) 34.dp else 10.dp,
                end = 10.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) DesignTokens.Accent else primary,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = if (active) DesignTokens.Accent else primary,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(text = count.toString(), color = DesignTokens.TextSecondary, fontSize = 13.sp)
        }
        if (trailingChevron) {
            Icon(
                Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = DesignTokens.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    emptyLabel: String?,
    darkMode: Boolean,
    onAdd: () -> Unit
) {
    val primary = if (darkMode) Color.White else DesignTokens.TextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 4.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if (emptyLabel != null) {
            Text(text = emptyLabel, color = DesignTokens.TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(1.dp, DesignTokens.TextSecondary, CircleShape)
                .clickable { onAdd() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Add $title",
                tint = DesignTokens.TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * The "All notes" sidebar: sparkle avatar + window buttons on top, Search /
 * All notes (with Uncategorized + Untagged sub-items) / Trash, then Folder
 * and Tag sections. The active item gets the light teal highlight.
 */
@Composable
fun Sidebar(
    totalNotes: Int,
    untaggedCount: Int,
    tags: List<String>,
    tagCounts: Map<String, Int>,
    selection: String,
    darkMode: Boolean,
    onSelectAll: () -> Unit,
    onSelectUntagged: () -> Unit,
    onSelectTrash: () -> Unit,
    onSelectSearch: () -> Unit,
    onSelectTag: (String) -> Unit,
    onCollapse: () -> Unit,
    onSettings: () -> Unit,
    onStore: () -> Unit,
    onAddFolder: () -> Unit,
    onAddTag: () -> Unit
) {
    val bg = if (darkMode) Color(0xFF1D1D1F) else DesignTokens.SidebarBackground
    val primary = if (darkMode) Color.White else DesignTokens.TextPrimary
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top row: sparkle avatar + collapse / settings / store
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(DesignTokens.AccentTint, CircleShape)
                    .clickable { onSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = "Assistant",
                    tint = DesignTokens.Accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onCollapse, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Filled.MenuOpen,
                    contentDescription = "Collapse sidebar",
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onSettings, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onStore, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Filled.Storefront,
                    contentDescription = "Store",
                    tint = primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        SidebarItem(
            icon = Icons.Filled.Search,
            label = "Search",
            active = selection == SidebarKeys.SEARCH,
            darkMode = darkMode,
            onClick = onSelectSearch
        )
        SidebarItem(
            icon = Icons.Filled.Description,
            label = "All notes",
            active = selection == SidebarKeys.ALL || selection == SidebarKeys.UNTAGGED ||
                selection.startsWith("tag:"),
            darkMode = darkMode,
            trailingChevron = true,
            onClick = onSelectAll
        )
        SidebarItem(
            icon = Icons.Filled.Description,
            label = "Uncategorized",
            active = selection == SidebarKeys.ALL,
            darkMode = darkMode,
            count = totalNotes,
            indent = true,
            onClick = onSelectAll
        )
        SidebarItem(
            icon = Icons.Filled.Label,
            label = "Untagged",
            active = selection == SidebarKeys.UNTAGGED,
            darkMode = darkMode,
            count = untaggedCount,
            indent = true,
            onClick = onSelectUntagged
        )
        SidebarItem(
            icon = Icons.Filled.Delete,
            label = "Trash",
            active = selection == SidebarKeys.TRASH,
            darkMode = darkMode,
            onClick = onSelectTrash
        )

        SectionHeader(
            title = "Folder",
            emptyLabel = "No folder yet",
            darkMode = darkMode,
            onAdd = onAddFolder
        )

        SectionHeader(
            title = "Tag",
            emptyLabel = if (tags.isEmpty()) "No tag yet" else null,
            darkMode = darkMode,
            onAdd = onAddTag
        )
        tags.forEach { tag ->
            SidebarItem(
                icon = Icons.Filled.Label,
                label = tag,
                active = selection == SidebarKeys.tag(tag),
                darkMode = darkMode,
                count = tagCounts[tag],
                indent = true,
                onClick = { onSelectTag(tag) }
            )
        }
    }
}

/** Slim icon rail shown when the sidebar is collapsed. */
@Composable
fun SidebarRail(
    selection: String,
    darkMode: Boolean,
    onExpand: () -> Unit,
    onSelectAll: () -> Unit,
    onSelectTrash: () -> Unit,
    onSelectSearch: () -> Unit,
    onSettings: () -> Unit
) {
    val bg = if (darkMode) Color(0xFF1D1D1F) else DesignTokens.SidebarBackground
    val primary = if (darkMode) Color.White else DesignTokens.TextPrimary
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(bg)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onExpand) {
            Icon(Icons.Filled.Menu, contentDescription = "Expand sidebar", tint = primary)
        }
        RailIcon(
            Icons.Filled.AutoAwesome, "Assistant",
            selection == "assistant", onSettings
        )
        RailIcon(
            Icons.Filled.Search, "Search",
            selection == SidebarKeys.SEARCH, onSelectSearch
        )
        RailIcon(
            Icons.Filled.Description, "All notes",
            selection == SidebarKeys.ALL || selection == SidebarKeys.UNTAGGED,
            onSelectAll
        )
        RailIcon(
            Icons.Filled.Delete, "Trash",
            selection == SidebarKeys.TRASH, onSelectTrash
        )
        RailIcon(Icons.Filled.Settings, "Settings", false, onSettings)
    }
}

@Composable
private fun RailIcon(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(
                if (active) DesignTokens.AccentTint else Color.Transparent,
                CircleShape
            )
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (active) DesignTokens.Accent else DesignTokens.TextSecondary,
            modifier = Modifier.size(21.dp)
        )
    }
}
