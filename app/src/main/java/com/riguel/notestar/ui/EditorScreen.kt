package com.riguel.notestar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riguel.notestar.data.PageTemplate
import com.riguel.notestar.data.StrokePoint
import com.riguel.notestar.data.TextElement
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
    var textSize by remember { mutableStateOf(20f) }
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    // The text element currently being created/edited in the dialog, if any.
    var editingText by remember { mutableStateOf<TextElement?>(null) }

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

    fun selectTool(newTool: Tool) {
        tool = newTool
        // Give each tool a sensible default color the first time you switch to it.
        when (newTool) {
            Tool.HIGHLIGHTER -> if (inkColor !in highlighterColors) inkColor = highlighterColors.first()
            Tool.PEN, Tool.TEXT -> if (inkColor in highlighterColors) inkColor = Color.Black
            Tool.ERASER -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("Back") }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Untitled") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                onSave(title.ifBlank { "Untitled" }, strokes.toList(), texts.toList())
            }) {
                Text("Save")
            }
        }

        // Tool picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tool.values().forEach { t ->
                val label = t.name.lowercase().replaceFirstChar { it.uppercase() }
                if (t == tool) {
                    Button(onClick = { selectTool(t) }) { Text(label) }
                } else {
                    TextButton(onClick = { selectTool(t) }) { Text(label) }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = {
                    if (strokes.isNotEmpty()) {
                        redoStack.add(strokes.removeAt(strokes.lastIndex))
                    }
                }
            ) { Text("Undo") }
            TextButton(
                onClick = {
                    if (redoStack.isNotEmpty()) {
                        strokes.add(redoStack.removeAt(redoStack.lastIndex))
                    }
                }
            ) { Text("Redo") }
            TextButton(
                onClick = {
                    strokes.clear()
                    redoStack.clear()
                }
            ) { Text("Clear ink") }
        }
        if (tool == Tool.TEXT) {
            Text(
                text = "Tap anywhere on the page to add text. Tap a text to edit it, drag it to move it.",
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // Color palette + size slider (hidden while erasing)
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
                                .clickable { inkColor = c }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (tool == Tool.TEXT) {
                    Slider(
                        value = textSize,
                        onValueChange = { textSize = it },
                        valueRange = 14f..44f,
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
                        editingText = TextElement(
                            id = System.nanoTime().toString(),
                            x = offset.x,
                            y = offset.y,
                            text = "",
                            colorValue = inkColor.value,
                            fontSizeSp = textSize
                        )
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
                // Page template: ruled lines + margin for lined pages
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
                    Text(
                        text = element.text,
                        color = Color(element.colorValue),
                        fontSize = element.fontSizeSp.sp,
                        modifier = Modifier
                            .offset {
                                IntOffset(element.x.roundToInt(), element.y.roundToInt())
                            }
                            .then(
                                if (tool == Tool.TEXT) {
                                    Modifier
                                        .pointerInput(element.id) {
                                            var current = element
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                current = current.copy(
                                                    x = current.x + dragAmount.x,
                                                    y = current.y + dragAmount.y
                                                )
                                                val i = texts.indexOfFirst { it.id == current.id }
                                                if (i >= 0) texts[i] = current
                                            }
                                        }
                                        .clickable { editingText = element }
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

    // Create / edit text dialog
    val target = editingText
    if (target != null) {
        var draft by remember(target.id) { mutableStateOf(target.text) }
        val alreadyOnPage = texts.any { it.id == target.id }
        AlertDialog(
            onDismissRequest = { editingText = null },
            title = { Text(if (alreadyOnPage) "Edit text" else "Add text") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Type here") },
                    minLines = 2
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (draft.isNotBlank()) {
                        val saved = target.copy(text = draft)
                        if (alreadyOnPage) {
                            val i = texts.indexOfFirst { it.id == target.id }
                            if (i >= 0) texts[i] = saved
                        } else {
                            texts.add(saved)
                        }
                    }
                    editingText = null
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    if (alreadyOnPage) {
                        TextButton(onClick = {
                            texts.removeAll { it.id == target.id }
                            editingText = null
                        }) { Text("Delete") }
                    }
                    TextButton(onClick = { editingText = null }) { Text("Cancel") }
                }
            }
        )
    }
}
