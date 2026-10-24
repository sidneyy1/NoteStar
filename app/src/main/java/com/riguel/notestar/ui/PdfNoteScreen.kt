package com.riguel.notestar.ui
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import com.riguel.notestar.data.EditorMode
import kotlinx.coroutines.delay

import android.graphics.Bitmap
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.riguel.notestar.data.ToolPrefs
import com.riguel.notestar.data.StrokeGeometry
import com.riguel.notestar.data.StrokePoint
import com.riguel.notestar.data.TextElement
import com.riguel.notestar.data.TextFonts
import com.riguel.notestar.data.Tool
import com.riguel.notestar.data.Stroke as NoteStroke
import java.io.File
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Prefs key for a tool's remembered color/size (see ToolPrefs). */
private fun pdfToolKey(t: Tool, pt: String): String = when (t) {
    Tool.PEN -> "pen_$pt"
    Tool.HIGHLIGHTER -> "highlighter"
    Tool.ERASER -> "eraser"
    Tool.TEXT -> "text"
    else -> "pen_pen"
}

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
    onExportRequest: (Int) -> Unit,
    onEnterPip: () -> Unit,
    tabs: List<DocTab>,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit
) {
    val totalPages = initialDoc.pdf?.pageCount ?: initialDoc.pages.size
    val context = LocalContext.current
    // Page fit: null = automatic (fit width on portrait devices, fit page
    // on landscape ones); the "..." menu toggles it and the choice is
    // remembered per document.
    var fitMode by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(initialDoc.id) {
        fitMode = context.getSharedPreferences("pdf_view", Context.MODE_PRIVATE)
            .getString("fit_${initialDoc.id}", null)
    }
    val isLandscapeDevice = LocalConfiguration.current.orientation ==
        Configuration.ORIENTATION_LANDSCAPE
    val fitIsWidth = when (fitMode) {
        "width" -> true
        "page" -> false
        else -> !isLandscapeDevice
    }
    fun toggleFitMode() {
        val next = if (fitIsWidth) "page" else "width"
        fitMode = next
        context.getSharedPreferences("pdf_view", Context.MODE_PRIVATE)
            .edit().putString("fit_${initialDoc.id}", next).apply()
    }
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
    var penType by remember {
        mutableStateOf(ToolPrefs.penType(context, PenTypes.PEN))
    }
    // Per-tool memory, shared with the note editor (v0.15.0).
    var inkColor by remember {
        mutableStateOf(
            ToolPrefs.colorForTool(context, "pen_$penType", Color.Black)
        )
    }
    var thickness by remember {
        mutableStateOf(
            ToolPrefs.sizeForTool(context, "pen_$penType", 5f)
        )
    }
    var textSize by remember {
        mutableStateOf(ToolPrefs.sizeForTool(context, "text", 16f))
    }
    var eraserSize by remember {
        mutableStateOf(ToolPrefs.sizeForTool(context, "eraser", 20f))
    }
    var pressureEnabled by remember {
        mutableStateOf(ToolPrefs.pressureEnabled(context))
    }
    var sheetView by remember { mutableStateOf(ToolSheetView.MAIN) }
    var sheetEntryView by remember { mutableStateOf(ToolSheetView.MAIN) }
    var recentColors by remember {
        mutableStateOf(ToolPrefs.recentColors(context))
    }
    var customColors by remember {
        mutableStateOf(ToolPrefs.customColors(context))
    }
    var brushRingVisible by remember { mutableStateOf(false) }
    var brushRingSession by remember { mutableStateOf(0) }
    val pdfScope = rememberCoroutineScope()
    LaunchedEffect(inkColor, tool, penType) {
        ToolPrefs.saveColorForTool(
            context, pdfToolKey(tool, penType), inkColor
        )
    }
    LaunchedEffect(thickness, tool, penType) {
        if (tool != Tool.TEXT && tool != Tool.ERASER) {
            ToolPrefs.saveSizeForTool(
                context, pdfToolKey(tool, penType), thickness
            )
        }
    }
    LaunchedEffect(textSize) {
        ToolPrefs.saveSizeForTool(context, "text", textSize)
    }
    LaunchedEffect(eraserSize) {
        ToolPrefs.saveSizeForTool(context, "eraser", eraserSize)
    }
    LaunchedEffect(penType) {
        ToolPrefs.savePenType(context, penType)
    }
    LaunchedEffect(pressureEnabled) {
        ToolPrefs.setPressureEnabled(context, pressureEnabled)
    }
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
    LaunchedEffect(currentPdfPage, pdfPan, pdfZoom) {
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
    var renderScale by remember { mutableStateOf(2f) }
    var pageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var pageWidthPt by remember { mutableStateOf(0) }
    var pageHeightPt by remember { mutableStateOf(0) }
    LaunchedEffect(currentPdfPage, renderer, renderScale) {
        val r = renderer ?: return@LaunchedEffect
        try {
            val page = r.openPage(currentPdfPage)
            val w = page.width
            val h = page.height
            // Render at displayed size x pixel ratio: the bitmap aspect
            // always equals the page aspect (one uniform scale for both
            // dimensions), capped so a huge zoom can't allocate forever.
            val rs = min(renderScale, min(3200f / w, 3200f / h))
            val bmpW = (w * rs).toInt().coerceAtLeast(1)
            val bmpH = (h * rs).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            pageWidthPt = w
            pageHeightPt = h
            pageBitmap = bitmap.asImageBitmap()
            // Debug assertion: bitmap aspect must match the page's.
            val pageAspect = w.toFloat() / h.toFloat()
            val bmpAspect = bmpW.toFloat() / bmpH.toFloat()
            if (abs(bmpAspect - pageAspect) > pageAspect * 0.005f) {
                Log.w("PdfNoteScreen", "Bitmap aspect $bmpAspect != page $pageAspect")
            }
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
            var downTime = 0L
            var downPos = Offset.Zero
            var lastTapTime = 0L
            var lastTapPos = Offset.Zero
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
                } else if (event.changes.size == 1 && pdfMode != EditorMode.DRAW) {
                    // Double-tap toggles fit <-> 200%, anchored at the tap
                    // (Hand/Select only — in Draw mode a tap is ink).
                    val change = event.changes.first()
                    if (change.changedToDown()) {
                        downTime = change.uptimeMillis
                        downPos = change.position
                    } else if (change.changedToUp()) {
                        val quick = change.uptimeMillis - downTime < 250
                        val still = (change.position - downPos).getDistance() < 24f
                        if (quick && still) {
                            val now = change.uptimeMillis
                            val nearLast = (change.position - lastTapPos)
                                .getDistance() < 48f
                            if (now - lastTapTime < 350 && nearLast) {
                                if (pdfZoom > 1.05f) {
                                    pdfZoom = 1f
                                    pdfPan = Offset.Zero
                                } else {
                                    val nz = 2f
                                    val f = nz / pdfZoom
                                    pdfPan = clampPdfPan(
                                        Offset(
                                            change.position.x -
                                                (change.position.x - pdfPan.x) * f,
                                            change.position.y -
                                                (change.position.y - pdfPan.y) * f
                                        ),
                                        nz
                                    )
                                    pdfZoom = nz
                                }
                                lastTapTime = 0L
                            } else {
                                lastTapTime = now
                                lastTapPos = change.position
                            }
                        }
                    }
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

    /** Tool switching: restores the tool's last color and size. */
    fun applyTool(t: Tool) {
        commitEditing()
        tool = t
        if (t != Tool.TEXT) selectedTextId = null
        val key = pdfToolKey(t, penType)
        inkColor = ToolPrefs.colorForTool(
            context, key,
            if (t == Tool.HIGHLIGHTER) HighlighterPalette.first()
            else Color.Black
        )
        when (t) {
            Tool.TEXT -> textSize =
                ToolPrefs.sizeForTool(context, key, 16f)
            Tool.ERASER -> eraserSize =
                ToolPrefs.sizeForTool(context, key, 20f)
            else -> thickness =
                ToolPrefs.sizeForTool(context, key, 5f)
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
                    eraserSize.toDouble() / scaleFactor.toDouble()
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
            onEnterPip = onEnterPip,
            onExport = {
                saveAll()
                onExportRequest(currentPdfPage)
            },
            onFitToggle = { toggleFitMode() },
            fitIsWidth = fitIsWidth
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
                .padding(top = 16.dp)
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
                // Fit modes: "width" fits the page width (16dp margins,
                // 900dp cap); "page" also fits the height so one whole
                // page is visible. pdfZoom multiplies from there.
                val widthFit = min(
                    (maxWidth.value - 32f) / pageWidthPt.toFloat(),
                    900f / pageWidthPt.toFloat()
                )
                val heightFit = maxHeight.value / pageHeightPt.toFloat()
                val fitScale = (
                    if (fitIsWidth) widthFit else min(widthFit, heightFit)
                ).coerceAtLeast(0.05f)
                // Sharp re-render: during a pinch the layer scales the
                // current bitmap; 300ms after the zoom settles, re-render
                // at the new scale (px per point = fit x density x zoom).
                val densityNow = LocalDensity.current.density
                LaunchedEffect(fitScale, pdfZoom) {
                    delay(300)
                    val target = (fitScale * densityNow * pdfZoom)
                        .coerceIn(1f, 6f)
                    val capped = min(
                        target,
                        min(3200f / pageWidthPt, 3200f / pageHeightPt)
                    )
                    if (abs(capped - renderScale) > 0.01f) {
                        renderScale = capped
                    }
                }
                Box(
                    modifier = Modifier
                        // requiredSize, not size: size() is coerced by the
                        // parent's constraints, so a page taller than the
                        // viewport got its height clamped while the width
                        // stayed — squashing the bitmap (the "stretch").
                        // requiredSize keeps the true aspect; the parent
                        // clips the overflow and pan/zoom handles the rest.
                        .requiredSize(
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
                        contentScale = ContentScale.Fit,
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
                        else -> Modifier.pointerInput(tool, inkColor, thickness, pressureEnabled) {
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
                                        if (pressureEnabled) {
                                            change.pressure.coerceIn(0.1f, 1f)
                                        } else {
                                            1f
                                        }
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
                // Every draw tool has sheet content now (eraser
                // gets its size control), as does Select mode.
                val popoverHasContent = pdfMode == EditorMode.SELECT ||
                    pdfMode == EditorMode.DRAW
                // Outside-tap scrim: while the sheet is open, a tap
                // anywhere but the bottom column dismisses it.
                if (popoverOpen && !toolsCollapsed && popoverHasContent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { popoverOpen = false }
                    )
                }
                // Brush ring: real on-screen size at current zoom,
                // centered on the canvas, while the size slider drags
                // and for 500ms after release.
                if (brushRingVisible) {
                    val ringSizePt = when (tool) {
                        Tool.ERASER -> eraserSize
                        Tool.TEXT -> texts.firstOrNull {
                            it.id == (editingTextId ?: selectedTextId)
                        }?.fontSizeSp ?: textSize
                        else -> thickness
                    }
                    val ringDiameter = with(LocalDensity.current) {
                        (ringSizePt * scaleFactor).toDp()
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(ringDiameter.coerceAtLeast(8.dp))
                            .clip(CircleShape)
                            .border(2.dp, DesignTokens.Accent, CircleShape)
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = popoverOpen && !toolsCollapsed && popoverHasContent,
                        enter = fadeIn(tween(180)) + expandVertically(tween(180)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                    ) {
                        ToolOptionsSheet(
                            view = sheetView,
                            entryView = sheetEntryView,
                            onViewChange = { sheetView = it },
                            onDismiss = { popoverOpen = false },
                            isHighlighter = tool == Tool.HIGHLIGHTER,
                            showTransparent = false,
                            showPenTypes = pdfMode == EditorMode.DRAW &&
                                tool == Tool.PEN,
                            penType = penType,
                            onPenTypeChange = { newType ->
                                penType = newType
                                thickness = ToolPrefs.sizeForTool(
                                    context, "pen_$newType", 5f
                                )
                                inkColor = ToolPrefs.colorForTool(
                                    context, "pen_$newType", Color.Black
                                )
                            },
                            showSize = pdfMode == EditorMode.DRAW,
                            size = when (tool) {
                                Tool.TEXT -> texts.firstOrNull {
                                    it.id == (editingTextId ?: selectedTextId)
                                }?.fontSizeSp ?: textSize
                                Tool.ERASER -> eraserSize
                                else -> thickness
                            },
                            sizeRange = when (tool) {
                                Tool.TEXT -> 6f..90f
                                else -> sizeRangeFor(tool, penType)
                            },
                            sizeUnit = "px",
                            previewKind = when (tool) {
                                Tool.HIGHLIGHTER -> PreviewKind.HIGHLIGHTER
                                Tool.TEXT -> PreviewKind.TEXT
                                else -> PreviewKind.STROKE
                            },
                            onSizeChange = { v ->
                                when (tool) {
                                    Tool.TEXT -> {
                                        val target = texts.firstOrNull {
                                            it.id == (editingTextId
                                                ?: selectedTextId)
                                        }
                                        if (target != null) {
                                            updateText(
                                                target.copy(fontSizeSp = v)
                                            )
                                        } else {
                                            textSize = v
                                        }
                                    }
                                    Tool.ERASER -> eraserSize = v
                                    else -> thickness = v
                                }
                            },
                            onSizeDragStart = {
                                brushRingSession++
                                brushRingVisible = true
                            },
                            onSizeDragEnd = {
                                val session = brushRingSession
                                pdfScope.launch {
                                    delay(500)
                                    if (session == brushRingSession) {
                                        brushRingVisible = false
                                    }
                                }
                            },
                            showColorRow = pdfMode == EditorMode.DRAW &&
                                tool != Tool.ERASER,
                            inkColor = inkColor,
                            onColorLive = { c ->
                                inkColor = c
                                if (tool == Tool.TEXT) {
                                    texts.firstOrNull {
                                        it.id == (editingTextId
                                            ?: selectedTextId)
                                    }?.let {
                                        updateText(
                                            it.copy(colorValue = c.value)
                                        )
                                    }
                                }
                            },
                            onColorCommit = { c ->
                                inkColor = c
                                if (tool == Tool.TEXT) {
                                    texts.firstOrNull {
                                        it.id == (editingTextId
                                            ?: selectedTextId)
                                    }?.let {
                                        updateText(
                                            it.copy(colorValue = c.value)
                                        )
                                    }
                                }
                                ToolPrefs.pushRecent(context, c)
                                recentColors =
                                    ToolPrefs.recentColors(context)
                            },
                            recentColors = recentColors,
                            customColors = customColors,
                            onSaveCustom = { c ->
                                ToolPrefs.addCustom(context, c)
                                customColors =
                                    ToolPrefs.customColors(context)
                            },
                            onDeleteCustom = { c ->
                                ToolPrefs.removeCustom(context, c)
                                customColors =
                                    ToolPrefs.customColors(context)
                            },
                            selectColorMode = pdfMode == EditorMode.SELECT &&
                                selectedTextId != null,
                            showPressureToggle = pdfMode == EditorMode.DRAW,
                            pressureEnabled = pressureEnabled,
                            onPressureChange = { pressureEnabled = it },
                            extraTopContent = {
                                // Text formatting for the selected text
                                // (unchanged from the old popover).
                                if (tool == Tool.TEXT) {
                                    val formatTarget = texts.firstOrNull {
                                        it.id == (editingTextId
                                            ?: selectedTextId)
                                    }
                                    if (formatTarget != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(
                                                    rememberScrollState()
                                                )
                                                .padding(
                                                    horizontal = 8.dp,
                                                    vertical = 2.dp
                                                ),
                                            verticalAlignment = Alignment
                                                .CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    updateText(
                                                        formatTarget.copy(
                                                            bold = !formatTarget
                                                                .bold
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.size(
                                                    btnSize
                                                )
                                            ) {
                                                Icon(
                                                    Icons.Filled.FormatBold,
                                                    contentDescription =
                                                        "Bold",
                                                    tint = if (formatTarget
                                                        .bold
                                                    ) {
                                                        MaterialTheme
                                                            .colorScheme
                                                            .primary
                                                    } else {
                                                        LocalContentColor
                                                            .current
                                                    },
                                                    modifier = Modifier.size(
                                                        iconSize
                                                    )
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    updateText(
                                                        formatTarget.copy(
                                                            italic =
                                                                !formatTarget
                                                                    .italic
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.size(
                                                    btnSize
                                                )
                                            ) {
                                                Icon(
                                                    Icons.Filled.FormatItalic,
                                                    contentDescription =
                                                        "Italic",
                                                    tint = if (formatTarget
                                                        .italic
                                                    ) {
                                                        MaterialTheme
                                                            .colorScheme
                                                            .primary
                                                    } else {
                                                        LocalContentColor
                                                            .current
                                                    },
                                                    modifier = Modifier.size(
                                                        iconSize
                                                    )
                                                )
                                            }
                                            Spacer(
                                                modifier = Modifier.width(8.dp)
                                            )
                                            TextFonts.ALL.forEach { font ->
                                                FilterChip(
                                                    selected = formatTarget
                                                        .fontFamilyName ==
                                                        font,
                                                    onClick = {
                                                        updateText(
                                                            formatTarget.copy(
                                                                fontFamilyName =
                                                                    font
                                                            )
                                                        )
                                                    },
                                                    label = {
                                                        Text(
                                                            when (font) {
                                                                TextFonts.SERIF ->
                                                                    "Serif"
                                                                TextFonts.SANS ->
                                                                    "Sans"
                                                                TextFonts.MONO ->
                                                                    "Mono"
                                                                TextFonts.CURSIVE ->
                                                                    "Cursive"
                                                                else -> "Default"
                                                            }
                                                        )
                                                    }
                                                )
                                                Spacer(
                                                    modifier = Modifier.width(
                                                        6.dp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                // Select mode actions (unchanged).
                                if (pdfMode == EditorMode.SELECT) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 12.dp,
                                                vertical = 4.dp
                                            ),
                                        verticalAlignment = Alignment
                                            .CenterVertically
                                    ) {
                                        Text(
                                            text = "Tap a text block to select it, then drag to move it. Ink can't be selected on a PDF.",
                                            style = MaterialTheme.typography
                                                .bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (selectedTextId != null) {
                                            IconButton(
                                                onClick = {
                                                    texts.removeAll {
                                                        it.id ==
                                                            selectedTextId
                                                    }
                                                    selectedTextId = null
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription =
                                                        "Delete selected text"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                    if (popoverOpen && !toolsCollapsed && popoverHasContent) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (!toolsCollapsed) {
                        FloatingToolbar(
                            mode = pdfMode,
                            onModeTap = { m ->
                                if (m == pdfMode) {
                                    if (m != EditorMode.HAND) {
                                        if (!popoverOpen) {
                                            if (m == EditorMode.SELECT &&
                                                selectedTextId != null
                                            ) {
                                                sheetView =
                                                    ToolSheetView.COLOR
                                                sheetEntryView =
                                                    ToolSheetView.COLOR
                                            } else {
                                                sheetView =
                                                    ToolSheetView.MAIN
                                                sheetEntryView =
                                                    ToolSheetView.MAIN
                                            }
                                        }
                                        popoverOpen = !popoverOpen
                                    }
                                } else {
                                    commitEditing()
                                    pdfMode = m
                                    popoverOpen = false
                                }
                            },
                            tool = tool,
                            onToolTap = { t ->
                                if (pdfMode == EditorMode.DRAW && t == tool) {
                                    if (!popoverOpen) {
                                        sheetView = ToolSheetView.MAIN
                                        sheetEntryView = ToolSheetView.MAIN
                                    }
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
                                // The color button always shows the
                                // swatch grid.
                                sheetView = ToolSheetView.COLOR
                                if (!popoverOpen) {
                                    sheetEntryView = ToolSheetView.COLOR
                                }
                                popoverOpen = true
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
                        .padding(end = 16.dp, bottom = 64.dp),
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
                    // Hidden while the sheet is open (no overlap).
                    if (!(popoverOpen && !toolsCollapsed))
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


