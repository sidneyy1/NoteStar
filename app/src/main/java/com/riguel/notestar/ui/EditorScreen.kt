package com.riguel.notestar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.riguel.notestar.data.StrokePoint
import com.riguel.notestar.data.Tool
import com.riguel.notestar.data.Stroke as NoteStroke

private val inkColors = listOf(
    Color.Black,
    Color(0xFF1565C0),
    Color(0xFFC62828),
    Color(0xFF2E7D32),
    Color(0xFF6A1B9A)
)

@Composable
fun EditorScreen(
    initialTitle: String,
    initialStrokes: List<NoteStroke>,
    onBack: () -> Unit,
    onSave: (String, List<NoteStroke>) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    val strokes = remember { mutableStateListOf<NoteStroke>().apply { addAll(initialStrokes) } }
    val redoStack = remember { mutableStateListOf<NoteStroke>() }
    var tool by remember { mutableStateOf(Tool.PEN) }
    var inkColor by remember { mutableStateOf(Color.Black) }
    var thickness by remember { mutableStateOf(6f) }
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }

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
            Button(onClick = { onSave(title.ifBlank { "Untitled" }, strokes.toList()) }) {
                Text("Save")
            }
        }

        // Tool picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tool.values().forEach { t ->
                if (t == tool) {
                    Button(onClick = { tool = t }) { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) }
                } else {
                    TextButton(onClick = { tool = t }) { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) }
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
            ) { Text("Clear") }
        }

        // Colors and thickness (hidden while erasing)
        if (tool != Tool.ERASER) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                inkColors.forEach { c ->
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
                Slider(
                    value = thickness,
                    onValueChange = { thickness = it },
                    valueRange = 2f..28f,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Drawing canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
                .pointerInput(tool, inkColor, thickness) {
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
        ) {
            val allStrokes = if (currentPoints.isNotEmpty() && tool != Tool.ERASER) {
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
    }
}
