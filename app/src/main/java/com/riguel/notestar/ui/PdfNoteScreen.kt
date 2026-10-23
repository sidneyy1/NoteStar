package com.riguel.notestar.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.riguel.notestar.data.Layer
import com.riguel.notestar.data.NoteDocument
import com.riguel.notestar.data.Page
import com.riguel.notestar.data.StrokePoint
import com.riguel.notestar.data.TextElement
import com.riguel.notestar.data.Tool
import com.riguel.notestar.data.Stroke as NoteStroke
import java.io.File
import kotlin.math.hypot
import kotlin.math.roundToInt

private val pdfPenColors = listOf(
    Color.Black, Color(0xFF1565C0), Color(0xFF2E7D32),
    Color(0xFFC62828), Color(0xFFAD1457), Color(0xFFEF6C00)
)

private val pdfHighlighterColors = listOf(
    Color(0xFFFFEB3B), Color(0xFF69F0AE), Color(0xFF81D4FA), Color(0xFFF48FB1)
)

/**
 * Annotate an imported PDF. The PDF is rendered read-only with PdfRenderer;
 * ink and text are stored in the note JSON (in PDF point coordinates), so
 * the original file bytes are never touched.
 */
@Composable
fun PdfNoteScreen(
    initialDoc: NoteDocument,
    pdfFile: File,
    onBack: () -> Unit,
    onSave: (NoteDocument) -> Unit,
    onEnterPip: () -> Unit
) {
    val totalPages = initialDoc.pdf?.pageCount ?: initialDoc.pages.size
    var title by remember { mutableStateOf(initialDoc.title) }
    val pagesState = remember {
        mutableStateListOf<Page>().apply { addAll(initialDoc.pages) }
    }
    var currentPdfPage by remember { mutableStateOf(0) }

    // Working copy of the current PDF page's annotation layer.
    val strokes = remember { mutableStateListOf<NoteStroke>() }
    val texts = remember { mutableStateListOf<TextElement>() }

    fun workPageIndex(): Int =
        pagesState.indexOfFirst { it.pdfPageIndex == currentPdfPage }

    fun loadWork() {
        strokes.clear()
        texts.clear()
        val idx = workPageIndex()
        if (idx >= 0) {
            val layer = pagesState[idx].layers.firstOrNull()
            if (layer != null) {
                strokes.addAll(layer.strokes)
                texts.addAll(layer.texts)
            }
        }
    }

    fun snapshotWork() {
        val idx = workPageIndex()
        if (idx >= 0) {
            val page = pagesState[idx]
            val first = page.layers.firstOrNull() ?: Layer.create("Annotations")
            val updatedFirst = first.copy(
                strokes = strokes.toList(),
                texts = texts.toList()
            )
            pagesState[idx] = page.copy(
                layers = listOf(updatedFirst) + page.layers.drop(1)
            )
        }
    }

    // Compact chrome on phones so the PDF page stays visible.
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val btnSize = if (compact) 38.dp else 48.dp
    val iconSize = if (compact) 20.dp else 24.dp
    var paletteVisible by remember { mutableStateOf(!compact) }

    var tool by remember { mutableStateOf(Tool.PEN) }
    var inkColor by remember { mutableStateOf(Color.Black) }
    var thickness by remember { mutableStateOf(5f) }
    var textSize by remember { mutableStateOf(16f) }
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var editingTextId by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    var initialized by remember { mutableStateOf(false) }
    if (!initialized) {
        loadWork()
        initialized = true
    }

    fun switchPdfPage(index: Int) {
        if (index == currentPdfPage || index !in 0 until totalPages) return
        snapshotWork()
        currentPdfPage = index
        loadWork()
        selectedTextId = null
        editingTextId = null
    }

    // --- PDF rendering ---
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    DisposableEffect(pdfFile) {
        val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val opened = try {
            PdfRenderer(fd)
        } catch (e: Exception) {
            null
        }
        renderer = opened
        onDispose {
            opened?.close()
            fd.close()
        }
    }
    var pageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var pageWidthPt by remember { mutableStateOf(0) }
    var pageHeightPt by remember { mutableStateOf(0) }
    LaunchedEffect(currentPdfPage, renderer) {
        val r = renderer ?: return@LaunchedEffect
        try {
            val page = r.openPage(currentPdfPage)
            val w = page.width
            val h = page.height
            val bitmap = Bitmap.createBitmap(w * 2, h * 2, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            pageWidthPt = w
            pageHeightPt = h
            pageBitmap = bitmap.asImageBitmap()
        } catch (e: Exception) {
            pageBitmap = null
        }
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val scaleFactor: Float =
        if (pageWidthPt > 0 && canvasSize.width > 0) {
            canvasSize.width.toFloat() / pageWidthPt.toFloat()
        } else {
            1f
        }

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

    fun eraseAt(point: StrokePoint) {
        strokes.removeAll { stroke ->
            stroke.points.any { p ->
                hypot((p.x - point.x).toDouble(), (p.y - point.y).toDouble()) <
                    20.0 / scaleFactor
            }
        }
    }

    LaunchedEffect(editingTextId) {
        if (editingTextId != null) focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
            IconButton(onClick = onEnterPip) {
                Icon(Icons.Filled.PictureInPicture, contentDescription = "Picture in picture")
            }
            IconButton(onClick = {
                commitEditing()
                snapshotWork()
                onSave(
                    initialDoc.copy(
                        title = title.ifBlank { "Untitled" },
                        pages = pagesState.toList(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }) {
                Icon(Icons.Filled.Save, contentDescription = "Save annotations")
            }
        }

        // Page navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { switchPdfPage(currentPdfPage - 1) }) {
                Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous PDF page")
            }
            Text("PDF page ${currentPdfPage + 1} / $totalPages")
            IconButton(onClick = { switchPdfPage(currentPdfPage + 1) }) {
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next PDF page")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Annotations save separately — the PDF is unchanged.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Tools
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
                    onClick = {
                        commitEditing()
                        tool = t
                        if (t != Tool.TEXT) selectedTextId = null
                        if (t == Tool.HIGHLIGHTER && inkColor !in pdfHighlighterColors) {
                            inkColor = pdfHighlighterColors.first()
                        }
                        if ((t == Tool.PEN || t == Tool.TEXT) && inkColor in pdfHighlighterColors) {
                            inkColor = Color.Black
                        }
                    },
                    modifier = Modifier
                        .size(btnSize)
                        .background(
                            if (t == tool) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(icon, contentDescription = description, modifier = Modifier.size(iconSize))
                }
            }
            IconButton(
                onClick = { paletteVisible = !paletteVisible },
                modifier = Modifier
                    .size(btnSize)
                    .background(
                        if (paletteVisible) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Filled.Palette,
                    contentDescription = "Show / hide colors",
                    modifier = Modifier.size(iconSize)
                )
            }
            IconButton(
                modifier = Modifier.size(btnSize),
                onClick = {
                    if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    modifier = Modifier.size(iconSize)
                )
            }
            IconButton(
                modifier = Modifier.size(btnSize),
                onClick = { strokes.clear(); texts.clear() }
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Clear page annotations",
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        if (paletteVisible && tool != Tool.ERASER) {
            val palette = if (tool == Tool.HIGHLIGHTER) pdfHighlighterColors else pdfPenColors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                            .clickable {
                                inkColor = c
                                if (tool == Tool.TEXT) {
                                    texts.firstOrNull { it.id == (editingTextId ?: selectedTextId) }
                                        ?.let { updateText(it.copy(colorValue = c.value)) }
                                }
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (tool == Tool.TEXT) {
                    Slider(
                        value = texts.firstOrNull { it.id == (editingTextId ?: selectedTextId) }
                            ?.fontSizeSp ?: textSize,
                        onValueChange = { v ->
                            val target = texts.firstOrNull {
                                it.id == (editingTextId ?: selectedTextId)
                            }
                            if (target != null) updateText(target.copy(fontSizeSp = v)) else textSize = v
                        },
                        valueRange = 8f..60f,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Slider(
                        value = thickness,
                        onValueChange = { thickness = it },
                        valueRange = 2f..24f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Page surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFEEEEEE)),
            contentAlignment = Alignment.TopCenter
        ) {
            val bitmap = pageBitmap
            if (bitmap == null || pageWidthPt == 0) {
                Text(
                    text = "Loading PDF page…",
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(pageWidthPt.toFloat() / pageHeightPt.toFloat())
                        .background(Color.White)
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "PDF page ${currentPdfPage + 1}",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )

                    fun toPdf(screen: Offset, pressure: Float = 1f): StrokePoint = StrokePoint(
                        x = screen.x / scaleFactor,
                        y = screen.y / scaleFactor,
                        pressure = pressure
                    )

                    val gestureModifier = when (tool) {
                        Tool.TEXT -> Modifier.pointerInput(tool) {
                            detectTapGestures { screen ->
                                when {
                                    editingTextId != null -> commitEditing()
                                    selectedTextId != null -> selectedTextId = null
                                    else -> {
                                        val point = toPdf(screen)
                                        val element = TextElement(
                                            id = System.nanoTime().toString(),
                                            x = point.x,
                                            y = point.y,
                                            text = "",
                                            colorValue = inkColor.value,
                                            fontSizeSp = textSize
                                        )
                                        texts.add(element)
                                        selectedTextId = element.id
                                        editingTextId = element.id
                                    }
                                }
                            }
                        }
                        else -> Modifier.pointerInput(tool, inkColor, thickness) {
                            detectDragGestures(
                                onDragStart = { screen ->
                                    val point = toPdf(screen)
                                    if (tool == Tool.ERASER) {
                                        eraseAt(point)
                                    } else {
                                        currentPoints = listOf(point)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val point = toPdf(
                                        change.position,
                                        change.pressure.coerceIn(0.1f, 1f)
                                    )
                                    if (tool == Tool.ERASER) {
                                        eraseAt(point)
                                    } else if (currentPoints.isNotEmpty()) {
                                        currentPoints = if (currentPoints.size == 1) {
                                            listOf(
                                                currentPoints.first()
                                                    .copy(pressure = point.pressure),
                                                point
                                            )
                                        } else {
                                            currentPoints + point
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (tool != Tool.ERASER && currentPoints.isNotEmpty()) {
                                        val rawPoints = if (currentPoints.size == 1) {
                                            val only = currentPoints.first()
                                            currentPoints + only.copy(x = only.x + 0.1f)
                                        } else {
                                            currentPoints
                                        }
                                        strokes.add(
                                            NoteStroke(
                                                points = rawPoints,
                                                colorValue = inkColor.value,
                                                thickness = thickness,
                                                highlighter = tool == Tool.HIGHLIGHTER
                                            )
                                        )
                                    }
                                    currentPoints = emptyList()
                                }
                            )
                        }
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { canvasSize = it }
                            .then(gestureModifier)
                    ) {
                        withTransform({
                            scale(scaleFactor, scaleFactor, Offset.Zero)
                        }) {
                            for (stroke in strokes) {
                                drawStrokeSmooth(stroke)
                            }
                            if (currentPoints.isNotEmpty() && tool != Tool.ERASER &&
                                tool != Tool.TEXT
                            ) {
                                drawStrokeSmooth(
                                    NoteStroke(
                                        points = currentPoints,
                                        colorValue = inkColor.value,
                                        thickness = thickness,
                                        highlighter = tool == Tool.HIGHLIGHTER
                                    )
                                )
                            }
                        }
                    }

                    for (element in texts) {
                        key(element.id) {
                            val isEditing = element.id == editingTextId
                            val isSelected = element.id == selectedTextId
                            val position = Modifier.offset {
                                IntOffset(
                                    (element.x * scaleFactor).roundToInt(),
                                    (element.y * scaleFactor).roundToInt()
                                )
                            }
                            // TextElement stores size in PDF points here.
                            val displayElement = element.copy(
                                fontSizeSp = element.fontSizeSp * scaleFactor
                            )
                            if (isEditing) {
                                BasicTextField(
                                    value = element.text,
                                    onValueChange = { updateText(element.copy(text = it)) },
                                    textStyle = textStyleFor(displayElement),
                                    cursorBrush = SolidColor(Color(0xFF1565C0)),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = { commitEditing() }
                                    ),
                                    modifier = position
                                        .focusRequester(focusRequester)
                                        .background(
                                            Color(0x143D5AFE),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .defaultMinSize(minWidth = 120.dp, minHeight = 32.dp)
                                        .padding(4.dp)
                                )
                            } else {
                                Text(
                                    text = element.text,
                                    style = textStyleFor(displayElement),
                                    modifier = position
                                        .then(
                                            if (tool == Tool.TEXT && editingTextId == null) {
                                                Modifier
                                                    .background(
                                                        if (isSelected) {
                                                            Color(0x143D5AFE)
                                                        } else {
                                                            Color.Transparent
                                                        },
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .pointerInput(element.id, scaleFactor) {
                                                        // pan/zoom are per-event changes;
                                                        // apply them to the freshest state.
                                                        detectTransformGestures { _, pan, zoom, _ ->
                                                            val base = texts.firstOrNull {
                                                                it.id == element.id
                                                            } ?: element
                                                            updateText(
                                                                base.copy(
                                                                    x = base.x + pan.x / scaleFactor,
                                                                    y = base.y + pan.y / scaleFactor,
                                                                    fontSizeSp = (base.fontSizeSp * zoom)
                                                                        .coerceIn(6f, 90f)
                                                                )
                                                            )
                                                        }
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
                                        .padding(3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
