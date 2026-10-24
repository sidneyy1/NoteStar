package com.riguel.notestar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riguel.notestar.data.PageTemplate

private enum class CreateKind { COVER, PAPER }

private val PAPER_CATEGORIES: List<Pair<String, List<PageTemplate>>> = listOf(
    "Base" to PaperTemplates.baseCategory,
    "Study" to listOf(PageTemplate.LECTURE, PageTemplate.LINED, PageTemplate.BLANK),
    "Plan" to listOf(PageTemplate.PLANNER, PageTemplate.DOTTED, PageTemplate.LINED),
    "Work" to listOf(PageTemplate.LINED, PageTemplate.LECTURE, PageTemplate.BLANK),
    "Life" to listOf(PageTemplate.PLANNER, PageTemplate.LINED, PageTemplate.DOTTED),
    "Write" to listOf(PageTemplate.LINED, PageTemplate.BLANK, PageTemplate.DOTTED),
    "Specialty" to listOf(PageTemplate.DOTTED, PageTemplate.LECTURE, PageTemplate.PLANNER)
)

private val COVER_CATEGORIES = listOf(
    "Simple", "Graph", "Fruit", "Illustration", "Black and white", "Yearly"
)

fun templateLabel(template: PageTemplate): String =
    PaperTemplates.def(template).label

/** Mini paper preview drawn to match each template's look. */
@Composable
fun PaperThumbnail(template: PageTemplate, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 62.dp, height = 82.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, DesignTokens.Divider, RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val line = Color(0xFFB9CDD6)
            val faint = Color(0xFFD8E2E8)
            when (template) {
                // Pattern templates render their real pattern, scaled to
                // the thumbnail (an A4-width page fitted to the card width).
                PageTemplate.BLANK, PageTemplate.LINED, PageTemplate.DOTTED,
                PageTemplate.THIN_LINE, PageTemplate.BOLD_LINE,
                PageTemplate.MINI_CHECK, PageTemplate.MID_CHECK,
                PageTemplate.DOT_GRID -> drawPaperPattern(
                    template,
                    scale = size.width / PaperTemplates.A4_WIDTH_UNITS,
                    offset = Offset.Zero,
                    size = size
                )
                PageTemplate.PLANNER -> {
                    drawRect(faint, Offset(6.dp.toPx(), 6.dp.toPx()),
                        androidx.compose.ui.geometry.Size(size.width - 12.dp.toPx(), 12.dp.toPx()))
                    val cellW = (size.width - 16.dp.toPx()) / 2f
                    val cellH = 16.dp.toPx()
                    for (r in 0..2) {
                        for (c in 0..1) {
                            drawRect(
                                faint,
                                Offset(6.dp.toPx() + c * (cellW + 4.dp.toPx()), 24.dp.toPx() + r * (cellH + 4.dp.toPx())),
                                androidx.compose.ui.geometry.Size(cellW, cellH),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(1f)
                            )
                        }
                    }
                }
                PageTemplate.LECTURE -> {
                    drawRect(faint, Offset(6.dp.toPx(), 6.dp.toPx()),
                        androidx.compose.ui.geometry.Size(16.dp.toPx(), size.height - 12.dp.toPx()))
                    var y = 14.dp.toPx()
                    while (y < size.height - 4.dp.toPx()) {
                        drawLine(line, Offset(26.dp.toPx(), y), Offset(size.width - 6.dp.toPx(), y), 1f)
                        y += 10.dp.toPx()
                    }
                }
            }
        }
    }
}

@Composable
private fun KindCard(
    title: String,
    selected: Boolean,
    coverColor: Color?,
    onClick: () -> Unit
) {
    val base = Modifier
        .size(width = 104.dp, height = 128.dp)
        .clip(RoundedCornerShape(14.dp))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = (if (selected) {
                base.background(DesignTokens.AccentTint)
                    .border(2.dp, DesignTokens.Accent, RoundedCornerShape(14.dp))
            } else {
                base.background(Color.White).dashedBorder(DesignTokens.TextSecondary, 14.dp)
            }).clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (title == "Cover") {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 76.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(coverColor ?: DesignTokens.Accent)
                )
            } else {
                PaperThumbnail(PageTemplate.LINED)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = if (selected) DesignTokens.Accent else DesignTokens.TextPrimary,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun CategoryChips(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp)
    ) {
        categories.forEach { category ->
            val active = category == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(DesignTokens.PillRadius))
                    .background(if (active) DesignTokens.AccentTint else Color.White)
                    .border(
                        1.dp,
                        if (active) DesignTokens.Accent else DesignTokens.Divider,
                        RoundedCornerShape(DesignTokens.PillRadius)
                    )
                    .clickable { onSelect(category) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = category,
                    color = if (active) DesignTokens.Accent else DesignTokens.TextPrimary,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

/**
 * Template picker section: header with cover-color swatches, category chips,
 * and a horizontally scrolling grid of templates / covers with a teal check
 * badge on the selected one.
 */
@Composable
fun TemplatePicker(
    kindIsCover: Boolean,
    paperCategory: String,
    coverCategory: String,
    onPaperCategory: (String) -> Unit,
    onCoverCategory: (String) -> Unit,
    template: PageTemplate,
    onTemplate: (PageTemplate) -> Unit,
    coverColor: Color?,
    onCoverColor: (Color) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (kindIsCover) "Cover template" else "Paper template",
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = DesignTokens.TextPrimary
            )
            Spacer(modifier = Modifier.width(12.dp))
            DesignTokens.CoverColors.forEach { (_, color) ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(
                            if (coverColor == color) 2.dp else 1.dp,
                            if (coverColor == color) DesignTokens.Accent else DesignTokens.Divider,
                            CircleShape
                        )
                        .padding(3.dp)
                        .background(color, CircleShape)
                        .clickable { onCoverColor(color) }
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
        if (kindIsCover) {
            CategoryChips(COVER_CATEGORIES, coverCategory, onCoverCategory)
            Text(
                text = coverCategory,
                color = DesignTokens.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                DesignTokens.CoverColors.forEach { (name, color) ->
                    val selected = coverColor == color
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(width = 62.dp, height = 82.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color)
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) DesignTokens.Accent else DesignTokens.Divider,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onCoverColor(color) }
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(18.dp)
                                        .background(DesignTokens.Accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            color = if (selected) DesignTokens.Accent else DesignTokens.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
        } else {
            CategoryChips(
                PAPER_CATEGORIES.map { it.first },
                paperCategory,
                onPaperCategory
            )
            val templates = PAPER_CATEGORIES.firstOrNull { it.first == paperCategory }?.second
                ?: emptyList()
            Text(
                text = paperCategory,
                color = DesignTokens.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                templates.forEach { item ->
                    val selected = template == item
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            PaperThumbnail(
                                item,
                                modifier = Modifier
                                    .border(
                                        if (selected) 2.dp else 0.dp,
                                        if (selected) DesignTokens.Accent else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onTemplate(item) }
                            )
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(18.dp)
                                        .background(DesignTokens.Accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = templateLabel(item),
                            fontSize = 12.sp,
                            color = if (selected) DesignTokens.Accent else DesignTokens.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
        }
    }
}

/**
 * Full-screen "Create note" modal: Cancel / title / Create top bar, Cover vs
 * Paper preview cards, a details card (title, tags, cover toggle, format),
 * the template picker, and the three-item bottom tab bar.
 */
@Composable
fun CreateNoteScreen(
    onCancel: () -> Unit,
    onCreate: (PageTemplate, String, List<String>, ULong?) -> Unit
) {
    var kind by remember { mutableStateOf(CreateKind.PAPER) }
    var title by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>() }
    var coverEnabled by remember { mutableStateOf(true) }
    var coverColor by remember { mutableStateOf<Color?>(null) }
    var template by remember { mutableStateOf(PageTemplate.BLANK) }
    var paperCategory by remember { mutableStateOf("Base") }
    var coverCategory by remember { mutableStateOf("Simple") }
    var bottomTab by remember { mutableStateOf(0) }
    var showTagDialog by remember { mutableStateOf(false) }
    var tagInput by remember { mutableStateOf("") }
    var showFormatMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignTokens.Background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = DesignTokens.Accent, fontSize = 15.sp)
            }
            Text(
                text = "Create note",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = DesignTokens.TextPrimary,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = {
                    onCreate(
                        template,
                        title.trim(),
                        tags.toList(),
                        if (coverEnabled) coverColor?.value else null
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Accent),
                shape = RoundedCornerShape(DesignTokens.PillRadius)
            ) {
                Text("Create", color = Color.White)
            }
        }
        HorizontalDivider(color = DesignTokens.Divider)

        if (bottomTab == 0) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(DesignTokens.ScreenPadding)
            ) {
                // Cover / Paper cards + details card
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val wide = maxWidth >= 600.dp
                    val previews: @Composable () -> Unit = {
                        Row {
                            KindCard("Cover", kind == CreateKind.COVER, coverColor) {
                                kind = CreateKind.COVER
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            KindCard("Paper", kind == CreateKind.PAPER, coverColor) {
                                kind = CreateKind.PAPER
                            }
                        }
                    }
                    val details: @Composable () -> Unit = {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(DesignTokens.CardRadius))
                                .background(Color.White)
                                .border(1.dp, DesignTokens.Divider, RoundedCornerShape(DesignTokens.CardRadius))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = { Text("Enter note title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text("Tag", color = DesignTokens.TextPrimary, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                tags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(DesignTokens.AccentTint, RoundedCornerShape(DesignTokens.PillRadius))
                                            .clickable { tags.remove(tag) }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("$tag  ×", color = DesignTokens.Accent, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                IconButton(
                                    onClick = { showTagDialog = true },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add tag", tint = DesignTokens.Accent)
                                }
                            }
                            HorizontalDivider(color = DesignTokens.Divider)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 6.dp)
                            ) {
                                Text(
                                    "Cover",
                                    color = DesignTokens.TextPrimary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = coverEnabled,
                                    onCheckedChange = { coverEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = DesignTokens.Accent,
                                        checkedThumbColor = Color.White
                                    )
                                )
                            }
                            HorizontalDivider(color = DesignTokens.Divider)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    "Format",
                                    color = DesignTokens.TextPrimary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Box {
                                    Row(
                                        modifier = Modifier.clickable { showFormatMenu = true },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Portrait, A4", color = DesignTokens.TextSecondary, fontSize = 14.sp)
                                        Icon(
                                            Icons.Filled.ExpandMore,
                                            contentDescription = "Format",
                                            tint = DesignTokens.TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showFormatMenu,
                                        onDismissRequest = { showFormatMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Portrait, A4") },
                                            onClick = { showFormatMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (wide) {
                        Row {
                            previews()
                            Spacer(modifier = Modifier.width(16.dp))
                            Box(modifier = Modifier.weight(1f)) { details() }
                        }
                    } else {
                        Column {
                            previews()
                            Spacer(modifier = Modifier.height(14.dp))
                            details()
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                TemplatePicker(
                    kindIsCover = kind == CreateKind.COVER,
                    paperCategory = paperCategory,
                    coverCategory = coverCategory,
                    onPaperCategory = { paperCategory = it },
                    onCoverCategory = { coverCategory = it },
                    template = template,
                    onTemplate = { template = it },
                    coverColor = coverColor,
                    onCoverColor = {
                        coverColor = it
                        coverEnabled = true
                    }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (bottomTab == 1) {
                        "No custom templates yet."
                    } else {
                        "Nothing added yet. Templates you add will appear here."
                    },
                    color = DesignTokens.TextSecondary
                )
            }
        }

        // Bottom tab bar
        HorizontalDivider(color = DesignTokens.Divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 8.dp)
        ) {
            val items = listOf(
                Triple(Icons.Filled.AutoAwesome, "Paper/Cover template", 0),
                Triple(Icons.Filled.Favorite, "Custom paper/cover", 1),
                Triple(Icons.Filled.Download, "The added paper/cover", 2)
            )
            items.forEach { (icon, label, index) ->
                val active = bottomTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { bottomTab = index },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (active) DesignTokens.Accent else DesignTokens.TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (active) DesignTokens.Accent else DesignTokens.TextSecondary
                    )
                }
            }
        }
    }

    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Add tag") },
            text = {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val value = tagInput.trim()
                        if (value.isNotEmpty() && value !in tags) tags.add(value)
                        tagInput = ""
                        showTagDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Accent)
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) { Text("Cancel") }
            }
        )
    }
}
