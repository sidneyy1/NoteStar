package com.riguel.notestar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.riguel.notestar.data.EditorMode
import com.riguel.notestar.data.Tool

/** One icon button inside the floating toolbar pill. */
@Composable
private fun PillButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
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
            modifier = Modifier.size(21.dp),
            tint = if (active) DesignTokens.Accent else DesignTokens.IconGray
        )
    }
}

@Composable
private fun PillDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(24.dp)
            .background(DesignTokens.Divider)
    )
}

/**
 * The drawing tools as a compact floating pill, docked at the bottom
 * center of the canvas: mode buttons (Select / Hand / Draw), the Draw
 * sub-tools, beautifier ("magic"), ink color, undo / redo, delete, and a
 * collapse button that hides the pill for distraction-free work.
 * Tapping the already-active mode or tool asks the caller to toggle the
 * options popover (see EditorScreen).
 */
@Composable
fun FloatingToolbar(
    mode: EditorMode,
    onModeTap: (EditorMode) -> Unit,
    tool: Tool,
    onToolTap: (Tool) -> Unit,
    magicActive: Boolean,
    onMagicTap: () -> Unit,
    inkColor: Color,
    onColorTap: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDelete: () -> Unit,
    deleteEnabled: Boolean,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillButton(
                Icons.Filled.NearMe, "Select",
                active = mode == EditorMode.SELECT
            ) { onModeTap(EditorMode.SELECT) }
            PillButton(
                Icons.Filled.PanTool, "Hand",
                active = mode == EditorMode.HAND
            ) { onModeTap(EditorMode.HAND) }
            PillButton(
                Icons.Filled.Brush, "Draw",
                active = mode == EditorMode.DRAW
            ) { onModeTap(EditorMode.DRAW) }
            PillDivider()
            PillButton(
                Icons.Filled.Gesture, "Pen",
                active = mode == EditorMode.DRAW && tool == Tool.PEN
            ) { onToolTap(Tool.PEN) }
            PillButton(
                Icons.Filled.FormatColorFill, "Highlighter",
                active = mode == EditorMode.DRAW && tool == Tool.HIGHLIGHTER
            ) { onToolTap(Tool.HIGHLIGHTER) }
            PillButton(
                Icons.Filled.TextFields, "Text",
                active = mode == EditorMode.DRAW && tool == Tool.TEXT
            ) { onToolTap(Tool.TEXT) }
            PillButton(
                Icons.Filled.Backspace, "Eraser",
                active = mode == EditorMode.DRAW && tool == Tool.ERASER
            ) { onToolTap(Tool.ERASER) }
            PillDivider()
            PillButton(
                Icons.Filled.AutoFixHigh, "Handwriting beautifier",
                active = magicActive, onClick = onMagicTap
            )
            // Color: the button face is the current ink color.
            IconButton(onClick = onColorTap, modifier = Modifier.size(40.dp)) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(inkColor)
                        .border(1.dp, DesignTokens.Divider, CircleShape)
                )
            }
            PillButton(
                Icons.AutoMirrored.Filled.Undo, "Undo",
                active = false, onClick = onUndo
            )
            PillButton(
                Icons.AutoMirrored.Filled.Redo, "Redo",
                active = false, onClick = onRedo
            )
            PillButton(
                Icons.Filled.Delete, "Delete selection / clear ink",
                active = false, enabled = deleteEnabled, onClick = onDelete
            )
            PillDivider()
            PillButton(
                Icons.Filled.KeyboardArrowDown, "Hide toolbar",
                active = false, onClick = onCollapse
            )
        }
    }
}

/**
 * The white card that floats above [FloatingToolbar] holding the active
 * tool's options (pen types, colors + size, text formatting, select hint).
 */
@Composable
fun ToolOptionsPopover(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            content = content
        )
    }
}

/**
 * Small floating page-navigation pill ("Page 1 / 3", previous / next /
 * add page), docked at the bottom right of the canvas. The caller fades
 * it out when the user hasn't navigated for a while.
 */
@Composable
fun PageIndicatorPill(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous page",
                    tint = DesignTokens.IconGray
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = DesignTokens.TextPrimary
            )
            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Next page",
                    tint = DesignTokens.IconGray
                )
            }
            IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add page",
                    tint = DesignTokens.IconGray
                )
            }
        }
    }
}
