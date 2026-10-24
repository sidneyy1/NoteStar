package com.riguel.notestar.ui
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import com.riguel.notestar.data.EditorMode
import kotlinx.coroutines.delay

import android.graphics.Bitmap
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Redo
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
import androidx.compose.material3.Surface
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
import com.riguel.notestar.data.StrokeGeometry
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
    onEnterPip: () -> Unit,
    tabs: List<DocTab>,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit
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
    val redoStrokes = remember { mutableStateListOf<NoteStroke>() }

    fun workPageIndex(): Int =
        pagesState.indexOfFirst { it.pdfPageIndex == currentPdfPage }

    fun loadWork() {
        strokes.clear()
        texts.clear()
        redoStrokes.clear()
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
    // The colors + size row is part of the ribbon: show it by default here
    // (the palette icon in the tools row still hides it).

    var tool by remember { mutableStateOf(Tool.PEN) }
    var penType by remember { mutableStateOf(PenTypes.PEN) }
    var inkColor by remember { mutableStateOf(Color.Black) }
    var thickness by remember { mutableStateOf(5f) }
    var textSize by remember { mutableStateOf(16f) }
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var showRename by remember { mutableStateOf(false) }
    var readerMode by remember { mutableStateOf(false) }
    var showAnnotations by remember { mutableStateOf(true) }
    var showOutline by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    // Declared up here because switchPdfPage (below) resets them.
    var pdfZoom by remember { mutableStateOf(1f) }
    var pdfPan by remember { mutableStateOf(Offset.Zero) }
    var pdfMode by remember { mutableStateOf(EditorMode.DRAW) }
    var popoverOpen by remember { mutableStateOf(false) }
    var toolsCollapsed by remember { mutableStateOf(false) }
    var beautifyOn by remember { mutableStateOf(true) }
    var pagePillFaded by remember { mutableStateOf(false) }
    var showGoToPage by remember { mutableStateOf(false) }
    var goToText by remember { mutableStateOf("") }
    var hexInput by remember { mutableStateOf("") }
    LaunchedEffect(currentPdfPage, pdfPan) {
        pagePillFaded = false
        delay(2000)
        pagePillFaded = true
    }
    val pagePillAlpha by animateFloatAsState(
        targetValue = if (pagePillFaded) 0.25f else 1f,
        animationSpec = tween(300),
        label = "pdfPagePillAlpha"
    )
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
        popoverOpen = false
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

    // Page view zoom/pan: pdfZoom/pdfPan drive a graphicsLayer transform
    // on the whole page (bitmap + ink + text together), so annotations stay
    // glued to the PDF at any zoom. Inside the page, everything is drawn in
    // local coordinates at scaleFactor (px per PDF point).
    // (pdfZoom/pdfPan themselves are declared earlier, before switchPdfPage.)
    fun clampPdfPan(pan: Offset, zoom: Float): Offset {
        val w = canvasSize.width.toFloat()
        val h = canvasSize.height.toFloat()
        if (w <= 0f || h <= 0f) return pan
        return Offset(
            pan.x.coerceIn(-w * zoom, w * 0.5f),
            pan.y.coerceIn(-h * zoom, h * 0.5f)
        )
    }
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
                    val newZoom = (pdfZoom * zoomChange).coerceIn(1f, 4f)
                    val factor = if (pdfZoom == 0f) 1f else newZoom / pdfZoom
                    pdfPan = clampPdfPan(
                        Offset(
                            centroid.x - (centroid.x - pdfPan.x) * factor + panChange.x,
                            centroid.y - (centroid.y - pdfPan.y) * factor + panChange.y
                        ),
                        newZoom
                    )
                    pdfZoom = newZoom
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

    /** Tool switching, exactly as the old ribbon did it. */
    fun applyTool(t: Tool) {
        commitEditing()
        tool = t
        if (t != Tool.TEXT) selectedTextId = null
        if (t == Tool.HIGHLIGHTER && inkColor !in pdfHighlighterColors) {
            inkColor = pdfHighlighterColors.first()
        }
        if ((t == Tool.PEN || t == Tool.TEXT) && inkColor in pdfHighlighterColors) {
            inkColor = Color.Black
        }
    }

    fun applyHexColor() {
        val parsed = parseHexColor(hexInput) ?: return
        inkColor = parsed
        if (tool == Tool.TEXT) {
            texts.firstOrNull { it.id == (editingTextId ?: selectedTextId) }
                ?.let { updateText(it.copy(colorValue = parsed.value)) }
        }
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

    // Auto-save when leaving the note.
    BackHandler {
        saveAll()
        onBack()
    }

    // Switching tabs / leaving composition: persist the latest state too.
    DisposableEffect(Unit) {
        onDispose { saveAll() }
    }

    // Voice input: dictated text becomes a new text annotation.
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            val element = TextElement(
                id = "t" + System.nanoTime().toString(),
                x = 54f,
                y = 108f,
                text = spoken,
                colorValue = inkColor.value,
                fontSizeSp = textSize
            )
            texts.add(element)
            selectedTextId = element.id
        }
    }

    fun launchSpeech() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to add a text note")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            // No speech recognizer on this device.
        }
    }

    fun eraseAt(point: StrokePoint) {
        redoStrokes.clear()
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
        // Document tabs + slim toolbar (redesign)
        DocumentTabs(
            tabs = tabs,
            activeTabId = initialDoc.id,
            onSelect = onTabSelect,
            onClose = onTabClose
        )
        EditorToolbar(
            onBack = {
                saveAll()
                onBack()
            },
            onMic = { launchSpeech() },
            onReader = { readerMode = !readerMode },
            readerActive = readerMode,
            onLayers = { showAnnotations = !showAnnotations },
            layersActive = showAnnotations,
            onAddPage = null,
            onSidePanel = { showOutline = true },
            onAdjust = { popoverOpen = !popoverOpen },
            adjustActive = popoverOpen,
            onRename = {
                renameText = title
                showRename = true
            },
            onSave = { saveAll() },
            onEnterPip = onEnterPip
        )

        // Page surface: the page is fitted to the available width (16dp
        // margins, capped at 900dp) at the top of the gray canvas. Pinch
        // zooms (up to 4x); the whole page (bitmap + ink + text) moves as
        // one graphicsLayer, so annotations stay glued to the PDF.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(DesignTokens.CanvasBackground)
                .clipToBounds(),
            contentAlignment = Alignment.TopCenter
        ) {
            val bitmap = pageBitmap
            if (bitmap == null || pageWidthPt == 0) {
                Text(
                    text = "Loading PDF page…",
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                // Fit to width: 16dp margins on both sides, and the page
                // is never wider than 900dp (comfortable on tablets).
                val fitScale = min(
                    (maxWidth.value - 32f) / pageWidthPt.toFloat(),
                    900f / pageWidthPt.toFloat()
                ).coerceAtLeast(0.05f)
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
                        .graphicsLayer {
                            scaleX = pdfZoom
                            scaleY = pdfZoom
                            translationX = pdfPan.x
                            translationY = pdfPan.y
                            transformOrigin = TransformOrigin(0f, 0f)
                            shadowElevation = 12f
                        }
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

                    val gestureModifier = when (pdfMode) {
                        EditorMode.HAND -> Modifier.pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                pdfPan = clampPdfPan(pdfPan + dragAmount, pdfZoom)
                            }
                        }
                        EditorMode.SELECT -> Modifier
                        EditorMode.DRAW -> when (tool) {
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
                                        // Beautifier: smooths the stroke and snaps
                                        // near-straight lines to a clean straight
                                        // segment (same as the note editor).
                                        strokes.add(
                                            NoteStroke(
                                                points = if (beautifyOn) {
                                                    StrokeGeometry.beautify(rawPoints)
                                                } else {
                                                    rawPoints
                                                },
                                                colorValue = inkColor.value,
                                                thickness = thickness,
                                                highlighter = tool == Tool.HIGHLIGHTER,
                                                penType = penType
                                            )
                                        )
                                        redoStrokes.clear()
                                    }
                                    currentPoints = emptyList()
                                }
                            )
                        }
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
                            for (stroke in if (showAnnotations) strokes else emptyList()) {
                                drawStrokeSmooth(stroke)
                            }
                            if (currentPoints.isNotEmpty() && tool != Tool.ERASER &&
                                tool != Tool.TEXT && showAnnotations
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

                    for (element in if (showAnnotations) texts else emptyList()) {
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
                                            if ((tool == Tool.TEXT || pdfMode == EditorMode.SELECT) && editingTextId == null) {
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
            // Floating tools — the same shell as the note editor:
            // toolbar pill + options popover + page pill over the canvas.
            if (!readerMode && pageBitmap != null) {
                val popoverHasContent = pdfMode == EditorMode.SELECT ||
                    (pdfMode == EditorMode.DRAW && tool != Tool.ERASER)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = popoverOpen && !toolsCollapsed && popoverHasContent,
                        enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                    ) {
                        ToolOptionsPopover(modifier = Modifier.fillMaxWidth()) {
                            // Pen types, same as the note editor's Draw options
                            if (pdfMode == EditorMode.DRAW && tool == Tool.PEN) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PenTypes.ALL.forEach { type ->
                                        FilterChip(
                                            selected = penType == type,
                                            onClick = { penType = type },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = DesignTokens.AccentTint,
                                                selectedLabelColor = DesignTokens.TextPrimary
                                            ),
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
                            if (pdfMode == EditorMode.DRAW && tool != Tool.ERASER) {
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
                                                    color = if (c == inkColor) DesignTokens.Accent else Color.Gray,
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
                                        // Live stroke preview
                                        Box(
                                            modifier = Modifier
                                                .width(40.dp)
                                                .height(thickness.dp.coerceIn(1.dp, 24.dp))
                                                .clip(CircleShape)
                                                .background(inkColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
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
                                            onDone = { applyHexColor() }
                                        ),
                                        modifier = Modifier.width(150.dp)
                                    )
                                    IconButton(onClick = { applyHexColor() }) {
                                        Icon(Icons.Filled.Check, contentDescription = "Apply hex color")
                                    }
                                    Text(text = "Hex color", style = MaterialTheme.typography.bodySmall)
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
                                                selected = formatTarget.fontFamilyName == font,
                                                onClick = {
                                                    updateText(formatTarget.copy(fontFamilyName = font))
                                                },
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
                            if (pdfMode == EditorMode.SELECT) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tap a text block to select it, then drag to move it. Ink can't be selected on a PDF.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (selectedTextId != null) {
                                        IconButton(
                                            onClick = {
                                                texts.removeAll { it.id == selectedTextId }
                                                selectedTextId = null
                                            }
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete selected text"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (popoverOpen && !toolsCollapsed && popoverHasContent) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (!toolsCollapsed) {
                        FloatingToolbar(
                            mode = pdfMode,
                            onModeTap = { m ->
                                if (m == pdfMode) {
                                    if (m != EditorMode.HAND) popoverOpen = !popoverOpen
                                } else {
                                    commitEditing()
                                    pdfMode = m
                                    popoverOpen = false
                                }
                            },
                            tool = tool,
                            onToolTap = { t ->
                                if (pdfMode == EditorMode.DRAW && t == tool) {
                                    popoverOpen = !popoverOpen
                                } else {
                                    if (pdfMode != EditorMode.DRAW) {
                                        commitEditing()
                                        pdfMode = EditorMode.DRAW
                                    }
                                    applyTool(t)
                                }
                            },
                            magicActive = beautifyOn,
                            onMagicTap = { beautifyOn = !beautifyOn },
                            inkColor = inkColor,
                            onColorTap = {
                                if (pdfMode != EditorMode.DRAW) {
                                    commitEditing()
                                    pdfMode = EditorMode.DRAW
                                }
                                if (tool == Tool.ERASER) applyTool(Tool.PEN)
                                popoverOpen = !popoverOpen
                            },
                            onUndo = {
                                if (strokes.isNotEmpty()) {
                                    redoStrokes.add(strokes.removeAt(strokes.lastIndex))
                                }
                            },
                            onRedo = {
                                if (redoStrokes.isNotEmpty()) {
                                    strokes.add(redoStrokes.removeAt(redoStrokes.lastIndex))
                                }
                            },
                            onDelete = {
                                if (selectedTextId != null) {
                                    texts.removeAll { it.id == selectedTextId }
                                    selectedTextId = null
                                } else {
                                    strokes.clear()
                                    texts.clear()
                                    redoStrokes.clear()
                                }
                            },
                            deleteEnabled = selectedTextId != null ||
                                strokes.isNotEmpty() || texts.isNotEmpty(),
                            onCollapse = {
                                toolsCollapsed = true
                                popoverOpen = false
                            }
                        )
                    }
                }
                if (toolsCollapsed) {
                    IconButton(
                        onClick = { toolsCollapsed = false },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, DesignTokens.Divider, CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Show toolbar",
                            tint = DesignTokens.IconGray
                        )
                    }
                }
                // Page pill: bottom right, fades when idle. Tapping the
                // page count opens Go to page.
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 64.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    if (pdfZoom != 1f || pdfPan != Offset.Zero) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White,
                            shadowElevation = 6.dp
                        ) {
                            TextButton(
                                onClick = {
                                    pdfZoom = 1f
                                    pdfPan = Offset.Zero
                                }
                            ) { Text("Reset zoom") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Box(modifier = Modifier.alpha(pagePillAlpha)) {
                        PageIndicatorPill(
                            label = "${currentPdfPage + 1} / $totalPages",
                            onPrev = { switchPdfPage(currentPdfPage - 1) },
                            onNext = { switchPdfPage(currentPdfPage + 1) },
                            onLabelClick = {
                                goToText = ""
                                showGoToPage = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showGoToPage) {
        AlertDialog(
            onDismissRequest = { showGoToPage = false },
            title = { Text("Go to page") },
            text = {
                OutlinedTextField(
                    value = goToText,
                    onValueChange = { goToText = it.filter { c -> c.isDigit() } },
                    label = { Text("Page (1 to $totalPages)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            goToText.toIntOrNull()?.let {
                                switchPdfPage((it - 1).coerceIn(0, totalPages - 1))
                            }
                            showGoToPage = false
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        goToText.toIntOrNull()?.let {
                            switchPdfPage((it - 1).coerceIn(0, totalPages - 1))
                        }
                        showGoToPage = false
                    }
                ) { Text("Go") }
            },
            dismissButton = {
                TextButton(onClick = { showGoToPage = false }) { Text("Cancel") }
            }
        )
    }

    if (showOutline) {
        AlertDialog(
            onDismissRequest = { showOutline = false },
            title = { Text("Outline") },
            text = {
                val items = texts.filter { it.text.isNotBlank() }
                if (items.isEmpty()) {
                    Text("No text on this page yet.")
                } else {
                    Column {
                        items.forEach { item ->
                            Text(
                                text = "• " + item.text,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOutline = false }) { Text("Close") }
            }
        )
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


/** Parses "#RRGGBB" into a Color, or null when malformed. */
private fun parseHexColor(input: String): Color? {
    val t = input.trim().removePrefix("#")
    if (t.length != 6) return null
    val v = t.toLongOrNull(16) ?: return null
    return Color(0xFF000000.toInt() or v.toInt())
}