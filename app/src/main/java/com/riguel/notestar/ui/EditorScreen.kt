package com.riguel.notestar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riguel.notestar.data.PageTemplate
import com.riguel.notestar.data.StrokePoint
import com.riguel.notestar.data.TextElement
import com.riguel.notestar.data.TextFonts
import com.riguel.notestar.data.Tool
import com.riguel.notestar.data.Stroke as NoteStroke
import kotlin.math.roundToInt

private val penColors = listOf(
    Color.Black,
    Color(0xFF424242),
    Color(0xFF1565C0),
    Color(0xFF039BE5),
    Color(0xFF00897B),
    Color(0xFF2E7D32),
    Color(0xFF7CB342),
    Color(0xFFEF6C00),
    Color(0xFFC62828),
    Color(0xFFAD1457),
    Color(0xFF6A1B9A),
    Color(0xFF4E342E)
)

private val highlighterColors = listOf(
    Color(0xFFFFEB3B),
    Color(0xFFCDDC39),
    Color(0xFF69F0AE),
    Color(0xFF4DD0E1),
    Color(0xFF81D4FA),
    Color(0xFFF48FB1),
    Color(0xFFFFB74D),
    Color(0xFFCE93D8)
)

private fun fontFamilyFor(name: String): FontFamily = when (name) {
    TextFonts.SERIF -> FontFamily.Serif
    TextFonts.SANS -> FontFamily.SansSerif
    TextFonts.MONO -> FontFamily.Monospace
    TextFonts.CURSIVE -> FontFamily.Cursive
    else -> FontFamily.Default
}

private fun fontLabel(name: String): String = when (name) {
    TextFonts.SERIF -> "Serif"
    TextFonts.SANS -> "Sans"
    TextFonts.MONO -> "Mono"
    TextFonts.CURSIVE -> "Cursive"
    else -> "Default"
}

private fun textStyleFor(element: TextElement): TextStyle = TextStyle(
    color = Color(element.colorValue),
    fontSize = element.fontSizeSp.sp,
    fontWeight = if (element.bold) FontWeight.Bold else FontWeight.Normal,
    fontStyle = if (element.italic) FontStyle.Italic else FontStyle.Normal,
    fontFamily = fontFamilyFor(element.fontFamilyName)
)

private fun colorToHex(c: Color): String = String.format(
    "#%02X%02X%02X",
    (c.red * 255).roundToInt(),
    (c.green * 255).roundToInt(),
    (c.blue * 255).roundToInt()
)

private fun parseHexColor(input: String): Color? {
    val t = input.trim().removePrefix("#")
    if (t.length != 6) return null
    val v = t.toLongOrNull(16) ?: return null
    return Color(0xFF000000.toInt() or v.toInt())
}

@Composable
fun EditorScreen(
    initialTitle: String,
    initialTemplate: PageTemplate,
    initialStrokes: List<NoteStroke>,
    initialTexts: List<TextElement>,
    onBack: () -> Unit,
    onSave: (String, List<NoteStroke>, List<TextElement>) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    val strokes = remember { mutableStateListOf<NoteStroke>().apply { addAll(initialStrokes) } }
    val texts = remember { mutableStateListOf<TextElement>().apply { addAll(initialTexts) } }
    val redoStack = remember { mutableStateListOf<NoteStroke>() }
    var tool by remember { mutableStateOf(Tool.PEN) }
    var inkColor by remember { mutableStateOf(Color.Black) }
    var thickness by remember { mutableStateOf(6f) }
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }

    // Inline text editing state (no dialogs): tap the page and type right there.
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var editingTextId by remember { mutableStateOf<String?>(null) }
    // Style defaults applied to newly created text.
    var newTextSize by remember { mutableStateOf(20f) }
    var newTextBold by remember { mutableStateOf(false) }
    var newTextItalic by remember { mutableStateOf(false) }
    var newTextFont by remember { mutableStateOf(TextFonts.DEFAULT) }

    var hexInput by remember { mutableStateOf(colorToHex(Color.Black)) }
    val focusRequester = remember { FocusRequester() }

    fun activeText(): TextElement? =
        texts.firstOrNull { it.id == (editingTextId ?: selectedTextId) }

    fun updateText(updated: TextElement) {
        val i = texts.indexOfFirst { it.id == updated.id }
        if (i >= 0) texts[i] = updated
    }

    fun commitEditing() {
        val id = editingTextId ?: return
        val el = texts.firstOrNull { it.id == id }
        if (el != null && el.text.isBlank()) {
            texts.remove(el)
            if (selectedTextId == id) selectedTextId = null
        }
        editingTextId = null
    }

    fun applyColor(c: Color) {
        inkColor = c
        hexInput = colorToHex(c)
        if (tool == Tool.TEXT) {
            activeText()?.let { updateText(it.copy(colorValue = c.value)) }
        }
    }

    fun selectTool(newTool: Tool) {
        commitEditing()
        tool = newTool
        if (newTool != Tool.TEXT) {
            selectedTextId = null
        }
        when (newTool) {
            Tool.HIGHLIGHTER -> if (inkColor !in highlighterColors) applyColor(highlighterColors.first())
            Tool.PEN, Tool.TEXT -> if (inkColor in highlighterColors) applyColor(Color.Black)
            Tool.ERASER -> Unit
        }
    }

    fun eraseAt(point: StrokePoint) {
        strokes.removeAll { stroke ->
            stroke.points.any { p ->
                kotlin.math.hypot(
                    (p.x - point.x).toDouble(),
                    (p.y - point.y).toDouble()
                ) < 24.0
            }
        }
    }

    LaunchedEffect(editingTextId) {
        if (editingTextId != null) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header: back icon, title, save icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Untitled") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                commitEditing()
                onSave(title.ifBlank { "Untitled" }, strokes.toList(), texts.toList())
            }) {
                Icon(Icons.Filled.Save, contentDescription = "Save note")
            }
        }

        // Ribbon: tool icons + history icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val toolIcons = listOf(
                Triple(Tool.PEN, Icons.Filled.Gesture, "Pen"),
                Triple(Tool.HIGHLIGHTER, Icons.Filled.FormatColorFill, "Highlighter"),
                Triple(Tool.TEXT, Icons.Filled.TextFields, "Text"),
                Triple(Tool.ERASER, Icons.Filled.Backspace, "Eraser")
            )
            toolIcons.forEach { (t, icon, description) ->
                IconButton(
                    onClick = { selectTool(t) },
                    modifier = Modifier.background(
                        if (t == tool) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(icon, contentDescription = description)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = {
                if (strokes.isNotEmpty()) {
                    redoStack.add(strokes.removeAt(strokes.lastIndex))
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = {
                if (redoStack.isNotEmpty()) {
                    strokes.add(redoStack.removeAt(redoStack.lastIndex))
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
            }
            IconButton(onClick = {
                strokes.clear()
                redoStack.clear()
            }) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear ink")
            }
        }

        // Rich text formatting bar (Text tool only)
        if (tool == Tool.TEXT) {
            val target = activeText()
            val boldActive = target?.bold ?: newTextBold
            val italicActive = target?.italic ?: newTextItalic
            val fontActive = target?.fontFamilyName ?: newTextFont
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (target != null) updateText(target.copy(bold = !target.bold))
                        else newTextBold = !newTextBold
                    },
                    modifier = Modifier.background(
                        if (boldActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Filled.FormatBold, contentDescription = "Bold")
                }
                IconButton(
                    onClick = {
                        if (target != null) updateText(target.copy(italic = !target.italic))
                        else newTextItalic = !newTextItalic
                    },
                    modifier = Modifier.background(
                        if (italicActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        CircleShape
                    )
                ) {
                    Icon(Icons.Filled.FormatItalic, contentDescription = "Italic")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextFonts.ALL.forEach { fontName ->
                    TextButton(onClick = {
                        if (target != null) updateText(target.copy(fontFamilyName = fontName))
                        else newTextFont = fontName
                    }) {
                        Text(
                            text = fontLabel(fontName),
                            fontFamily = fontFamilyFor(fontName),
                            fontWeight = if (fontName == fontActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (fontName == fontActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
                if (selectedTextId != null && editingTextId == null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        texts.removeAll { it.id == selectedTextId }
                        selectedTextId = null
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete selected text")
                    }
                }
            }
            Text(
                text = "Tap the page to type. Tap text to select, tap again to edit. Pinch a text to resize it.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Palette + size slider + hex selector (hidden while erasing)
        if (tool != Tool.ERASER) {
            val palette = if (tool == Tool.HIGHLIGHTER) highlighterColors else penColors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    palette.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (c == inkColor) 3.dp else 1.dp,
                                    color = if (c == inkColor) Color(0xFFFFC107) else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { applyColor(c) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                val target = activeText()
                if (tool == Tool.TEXT) {
                    Slider(
                        value = target?.fontSizeSp ?: newTextSize,
                        onValueChange = { v ->
                            if (target != null) updateText(target.copy(fontSizeSp = v))
                            else newTextSize = v
                        },
                        valueRange = 10f..80f,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Slider(
                        value = thickness,
                        onValueChange = { thickness = it },
                        valueRange = 2f..28f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // Hex color selector: any color you want, e.g. #1E88E5
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(inkColor)
                        .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { hexInput = it },
                    placeholder = { Text("#RRGGBB") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { parseHexColor(hexInput)?.let { applyColor(it) } }
                    ),
                    modifier = Modifier.width(150.dp)
                )
                IconButton(onClick = {
                    parseHexColor(hexInput)?.let { applyColor(it) }
                }) {
                    Icon(Icons.Filled.Check, contentDescription = "Apply hex color")
                }
                Text(
                    text = "Hex color",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Page: canvas (template + ink) with typed text on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
        ) {
            val gestureModifier = if (tool == Tool.TEXT) {
                Modifier.pointerInput(tool) {
                    detectTapGestures { offset ->
                        when {
                            editingTextId != null -> commitEditing()
                            selectedTextId != null -> selectedTextId = null
                            else -> {
                                val element = TextElement(
                                    id = System.nanoTime().toString(),
                                    x = offset.x,
                                    y = offset.y,
                                    text = "",
                                    colorValue = inkColor.value,
                                    fontSizeSp = newTextSize,
                                    bold = newTextBold,
                                    italic = newTextItalic,
                                    fontFamilyName = newTextFont
                                )
                                texts.add(element)
                                selectedTextId = element.id
                                editingTextId = element.id
                            }
                        }
                    }
                }
            } else {
                Modifier.pointerInput(tool, inkColor, thickness) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val start = StrokePoint(offset.x, offset.y)
                            if (tool == Tool.ERASER) {
                                eraseAt(start)
                            } else {
                                currentPoints = listOf(start)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val point = StrokePoint(change.position.x, change.position.y)
                            if (tool == Tool.ERASER) {
                                eraseAt(point)
                            } else {
                                currentPoints = currentPoints + point
                            }
                        },
                        onDragEnd = {
                            if (tool != Tool.ERASER && currentPoints.isNotEmpty()) {
                                strokes.add(
                                    NoteStroke(
                                        points = currentPoints,
                                        colorValue = inkColor.value,
                                        thickness = thickness,
                                        highlighter = tool == Tool.HIGHLIGHTER
                                    )
                                )
                                redoStack.clear()
                            }
                            currentPoints = emptyList()
                        }
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(gestureModifier)
            ) {
                if (initialTemplate == PageTemplate.LINED) {
                    var y = 72f
                    while (y < size.height) {
                        drawLine(
                            color = Color(0xFFBBDEFB),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2f
                        )
                        y += 72f
                    }
                    drawLine(
                        color = Color(0xFFF8BBD0),
                        start = Offset(72f, 0f),
                        end = Offset(72f, size.height),
                        strokeWidth = 2f
                    )
                }

                val allStrokes = if (currentPoints.isNotEmpty() && tool != Tool.ERASER && tool != Tool.TEXT) {
                    strokes + NoteStroke(
                        points = currentPoints,
                        colorValue = inkColor.value,
                        thickness = thickness,
                        highlighter = tool == Tool.HIGHLIGHTER
                    )
                } else {
                    strokes
                }
                for (stroke in allStrokes) {
                    if (stroke.points.isEmpty()) continue
                    val path = Path().apply {
                        moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (point in stroke.points.drop(1)) {
                            lineTo(point.x, point.y)
                        }
                        if (stroke.points.size == 1) {
                            lineTo(stroke.points.first().x + 0.1f, stroke.points.first().y + 0.1f)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(stroke.colorValue).copy(
                            alpha = if (stroke.highlighter) 0.35f else 1f
                        ),
                        style = Stroke(
                            width = stroke.thickness,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            // Typed text elements on top of the ink
            texts.forEach { element ->
                key(element.id) {
                    val isEditing = element.id == editingTextId
                    val isSelected = element.id == selectedTextId
                    val position = Modifier.offset {
                        IntOffset(element.x.roundToInt(), element.y.roundToInt())
                    }
                    if (isEditing) {
                        BasicTextField(
                            value = element.text,
                            onValueChange = { newValue ->
                                updateText(element.copy(text = newValue))
                            },
                            textStyle = textStyleFor(element),
                            cursorBrush = SolidColor(Color(0xFF1565C0)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { commitEditing() }
                            ),
                            modifier = position
                                .focusRequester(focusRequester)
                                .background(Color(0x143D5AFE), RoundedCornerShape(4.dp))
                                .defaultMinSize(minWidth = 140.dp, minHeight = 40.dp)
                                .padding(6.dp)
                        )
                    } else {
                        Text(
                            text = element.text,
                            style = textStyleFor(element),
                            modifier = position
                                .then(
                                    if (tool == Tool.TEXT && editingTextId == null) {
                                        Modifier
                                            .background(
                                                if (isSelected) Color(0x143D5AFE) else Color.Transparent,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 1.dp else 0.dp,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    Color.Transparent
                                                },
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .pointerInput(element.id) {
                                                var acc: TextElement? = null
                                                detectTransformGestures(
                                                    onGestureStart = {
                                                        acc = texts.firstOrNull { it.id == element.id }
                                                            ?: element
                                                    },
                                                    onGesture = { _, pan, zoom, _ ->
                                                        val base = acc ?: element
                                                        val updated = base.copy(
                                                            x = base.x + pan.x,
                                                            y = base.y + pan.y,
                                                            fontSizeSp = (base.fontSizeSp * zoom)
                                                                .coerceIn(10f, 120f)
                                                        )
                                                        acc = updated
                                                        updateText(updated)
                                                    },
                                                    onGestureEnd = { acc = null }
                                                )
                                            }
                                            .clickable {
                                                if (selectedTextId == element.id) {
                                                    editingTextId = element.id
                                                } else {
                                                    selectedTextId = element.id
                                                }
                                            }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
