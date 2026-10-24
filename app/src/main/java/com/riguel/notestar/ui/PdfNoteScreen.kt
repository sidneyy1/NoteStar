package com.riguel.notestar.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.riguel.notestar.data.Layer
import com.riguel.notestar.data.NoteDocument
import com.riguel.notestar.data.Page
import com.riguel.notestar.data.PenTypes
import com.riguel.notestar.data.StrokePoint
import com.riguel.notestar.data.TextElement
import com.riguel.notestar.data.TextFonts
import com.riguel.notestar.data.Tool
import com.riguel.notestar.data.Stroke as NoteStroke
import java.io.File
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
    var penType by remember { mutableStateOf(PenTypes.PEN) }
    var inkColor by remember { mutableStateOf(Color.Black) }
    var thickness by remember { mutableStateOf(5f) }
    var textSize by remember { mutableStateOf(16f) }
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    // Declared up here because switchPdfPage (below) resets them.
    var pdfZoom by remember { mutableStateOf(1f) }
    var pdfPan by remember { mutableStateOf(Offset.Zero) }
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
        pdfZoom = 1f
        pdfPan = Offset.Zero
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

    // Page view zoom/pan: two-finger pinch zooms the whole PDF page (1 = the
    // page fitted entirely into the available space), two-finger drag pans.
    // (pdfZoom/pdfPan themselves are declared earlier, before switchPdfPage.)
    val totalScale = scaleFactor * pdfZoom
    val pageZoomInterceptor = Modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.size > 1) {
                    val zoomChange = event.calculateZoom()
                    val panChange = event.calculatePan()
                    val centroid = event.changes.fold(Offset.Zero) { acc, change ->
                        acc + change.position
                    } / event.changes.size.toFloat()
                    val newZoom = (pdfZoom * zoomChange).coerceIn(1f, 5f)
                    val factor = if (pdfZoom == 0f) 1f else newZoom / pdfZoom
                    pdfPan = Offset(
                        centroid.x - (centroid.x - pdfPan.x) * factor + panChange.x,
                        centroid.y - (centroid.y - pdfPan.y) * factor + panChange.y
                    )
                    pdfZoom = newZoom
                    if (pdfZoom == 1f) pdfPan = Offset.Zero
                    event.changes.forEach { it.consume() }
                }
            }
        }
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

    fun saveAll() {
        commitEditing()
        snapshotWork()
        onSave(
            initialDoc.copy(
                title = title.ifBlank { "Untitled" },
                pages = pagesState.toList(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun eraseAt(point: StrokePoint) {
        strokes.removeAll { stroke ->
            stroke.points.any { p ->
                hypot((p.x - point.x).toDouble(), (p.y - point.y).toDouble()) <
                    20.0 / totalScale
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
            Text(
                text = title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(btnSize)
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Menu",
                        modifier = Modifier.size(iconSize)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            showMenu = false
                            renameText = title
                            showRename = true
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                        }
                    )
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
            IconButton(onClick = { saveAll() }, modifier = Modifier.size(btnSize)) {
                Icon(
                    Icons.Filled.Save,
                    contentDescription = "Save annotations",
                    modifier = Modifier.size(iconSize)
                )
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
            if (pdfZoom != 1f) {
                TextButton(onClick = {
                    pdfZoom = 1f
                    pdfPan = Offset.Zero
                }) { Text("Reset zoom") }
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

        // Pen types, same as the note editor's Draw options
        if (tool == Tool.PEN) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pen:", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(6.dp))
                PenTypes.ALL.forEach { type ->
                    FilterChip(
                        selected = penType == type,
                        onClick = { penType = type },
                        label = {
                            Text(
                                when (type) {
                                    PenTypes.PENCIL -> "Pencil"
                                    PenTypes.CALLIGRAPHY -> "Calligraphy"
                                    else -> "Pen"
                                }
                            )
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
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
                    val target = texts.firstOrNull { it.id == (editingTextId ?: selectedTextId) }
                    Slider(
                        value = target?.fontSizeSp ?: textSize,
                        onValueChange = { v ->
                            if (target != null) {
                                updateText(target.copy(fontSizeSp = v))
                            } else {
                                textSize = v
                            }
                        },
                        valueRange = 6f..90f,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    NumericSizeField(
                        value = target?.fontSizeSp ?: textSize,
                        onApply = { v ->
                            val clamped = v.coerceIn(6f, 90f)
                            if (target != null) {
                                updateText(target.copy(fontSizeSp = clamped))
                            } else {
                                textSize = clamped
                            }
                        }
                    )
                    Text("px", style = MaterialTheme.typography.bodySmall)
                } else {
                    Slider(
                        value = thickness,
                        onValueChange = { thickness = it },
                        valueRange = 1f..60f,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    NumericSizeField(
                        value = thickness,
                        onApply = { v -> thickness = v.coerceIn(1f, 60f) }
                    )
                    Text("px", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Text formatting for the selected text, same as the note editor
        if (tool == Tool.TEXT) {
            val formatTarget = texts.firstOrNull {
                it.id == (editingTextId ?: selectedTextId)
            }
            if (formatTarget != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { updateText(formatTarget.copy(bold = !formatTarget.bold)) },
                        modifier = Modifier.size(btnSize)
                    ) {
                        Icon(
                            Icons.Filled.FormatBold,
                            contentDescription = "Bold",
                            tint = if (formatTarget.bold) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                LocalContentColor.current
                            },
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    IconButton(
                        onClick = {
                            updateText(formatTarget.copy(italic = !formatTarget.italic))
                        },
                        modifier = Modifier.size(btnSize)
                    ) {
                        Icon(
                            Icons.Filled.FormatItalic,
                            contentDescription = "Italic",
                            tint = if (formatTarget.italic) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                LocalContentColor.current
                            },
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextFonts.ALL.forEach { font ->
                        FilterChip(
                            selected = formatTarget.font == font,
                            onClick = { updateText(formatTarget.copy(font = font)) },
                            label = {
                                Text(
                                    when (font) {
                                        TextFonts.SERIF -> "Serif"
                                        TextFonts.SANS -> "Sans"
                                        TextFonts.MONO -> "Mono"
                                        TextFonts.CURSIVE -> "Cursive"
                                        else -> "Default"
                                    }
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }
        }

        // Page surface: the whole page is fitted into the available space
        // (never taller/wider than it), and two-finger pinch zooms/pans it.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFEEEEEE))
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = pageBitmap
            if (bitmap == null || pageWidthPt == 0) {
                Text(
                    text = "Loading PDF page…",
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                val fitScale = min(
                    maxWidth.value / pageWidthPt.toFloat(),
                    maxHeight.value / pageHeightPt.toFloat()
                )
                Box(
                    modifier = Modifier
                        .size(
                            width = (pageWidthPt * fitScale).dp,
                            height = (pageHeightPt * fitScale).dp
                        )
                        .background(Color.White)
                        .then(
                            if (tool != Tool.TEXT) pageZoomInterceptor else Modifier
                        )
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "PDF page ${currentPdfPage + 1}",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )

                    fun toPdf(screen: Offset, pressure: Float = 1f): StrokePoint = StrokePoint(
                        x = (screen.x - pdfPan.x) / totalScale,
                        y = (screen.y - pdfPan.y) / totalScale,
                        pressure = pressure
                    )

                    val gestureModifier = when (tool) {
                        Tool.TEXT -> Modifier.pointerInput(tool, selectedTextId, editingTextId) {
                            coroutineScope {
                                launch {
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
                                launch {
                                    // Pinch anywhere on the page zooms the selected text.
                                    detectTransformGestures { _, _, zoom, _ ->
                                        if (zoom != 1f && editingTextId == null) {
                                            val target = texts.firstOrNull {
                                                it.id == selectedTextId
                                            }
                                            if (target != null) {
                                                updateText(
                                                    target.copy(
                                                        fontSizeSp = (target.fontSizeSp * zoom)
                                                            .coerceIn(6f, 90f)
                                                    )
                                                )
                                            }
                                        }
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
                                                highlighter = tool == Tool.HIGHLIGHTER,
                                                penType = penType
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
                            translate(pdfPan.x, pdfPan.y)
                            scale(totalScale, totalScale, Offset.Zero)
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
                                        highlighter = tool == Tool.HIGHLIGHTER,
                                        penType = penType
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
                                    (pdfPan.x + element.x * totalScale).roundToInt(),
                                    (pdfPan.y + element.y * totalScale).roundToInt()
                                )
                            }
                            // TextElement stores size in PDF points here.
                            val displayElement = element.copy(
                                fontSizeSp = element.fontSizeSp * totalScale
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
                                                    .pointerInput(element.id, totalScale) {
                                                        // pan/zoom are per-event changes;
                                                        // apply them to the freshest state.
                                                        detectTransformGestures { _, pan, zoom, _ ->
                                                            val base = texts.firstOrNull {
                                                                it.id == element.id
                                                            } ?: element
                                                            updateText(
                                                                base.copy(
                                                                    x = base.x + pan.x / totalScale,
                                                                    y = base.y + pan.y / totalScale,
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

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename note") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    placeholder = { Text("Untitled") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    title = renameText
                    showRename = false
                    saveAll()
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            }
        )
    }
}
