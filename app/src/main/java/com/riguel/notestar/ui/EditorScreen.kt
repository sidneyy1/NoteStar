package com.riguel.notestar.ui
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Surface
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.delay

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
import androidx.compose.foundation.layout.height
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
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riguel.notestar.data.EditorMode
import com.riguel.notestar.data.Layer
import com.riguel.notestar.data.NoteDocument
import com.riguel.notestar.data.Page
import com.riguel.notestar.data.PageFormat
import com.riguel.notestar.data.PageTemplate
import com.riguel.notestar.data.PageTransform
import com.riguel.notestar.data.PenTypes
import com.riguel.notestar.data.StrokeGeometry
import com.riguel.notestar.data.StrokePoint
import com.riguel.notestar.data.TextElement
import com.riguel.notestar.data.TextFonts
import com.riguel.notestar.data.Tool
import com.riguel.notestar.data.Stroke as NoteStroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val penColors = listOf(
    Color.Black, Color(0xFF424242), Color(0xFF1565C0), Color(0xFF039BE5),
    Color(0xFF00897B), Color(0xFF2E7D32), Color(0xFF7CB342), Color(0xFFEF6C00),
    Color(0xFFC62828), Color(0xFFAD1457), Color(0xFF6A1B9A), Color(0xFF4E342E)
)

private val highlighterColors = listOf(
    Color(0xFFFFEB3B), Color(0xFFCDDC39), Color(0xFF69F0AE), Color(0xFF4DD0E1),
    Color(0xFF81D4FA), Color(0xFFF48FB1), Color(0xFFFFB74D), Color(0xFFCE93D8)
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

private fun penTypeLabel(name: String): String = when (name) {
    PenTypes.PENCIL -> "Pencil"
    PenTypes.CALLIGRAPHY -> "Calligraphy"
    else -> "Pen"
}

fun textStyleFor(element: TextElement, scale: Float = 1f): TextStyle = TextStyle(
    color = Color(element.colorValue),
    fontSize = (element.fontSizeSp * scale).sp,
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

/**
 * A small numeric field for setting a size exactly (type "10" = 10px),
 * for when a slider is fiddly — e.g. a phone in portrait.
 */
@Composable
internal fun NumericSizeField(
    value: Float,
    onApply: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.roundToInt().toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it.filter { c -> c.isDigit() } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { text.toFloatOrNull()?.let { onApply(it) } }
        ),
        modifier = modifier.width(68.dp)
    )
}

/** Editable working copy of a [Layer] while its page is open. */
private class LayerWork(source: Layer) {
    val id: String = source.id
    var name by mutableStateOf(source.name)
    var visible by mutableStateOf(source.visible)
    var locked by mutableStateOf(source.locked)
    val strokes: SnapshotStateList<NoteStroke> =
        mutableStateListOf<NoteStroke>().apply { addAll(source.strokes) }
    val texts: SnapshotStateList<TextElement> =
        mutableStateListOf<TextElement>().apply { addAll(source.texts) }

    fun toLayer(): Layer = Layer(
        id = id, name = name, visible = visible, locked = locked,
        strokes = strokes.toList(), texts = texts.toList()
    )
}

@Composable
fun EditorScreen(
    initialDoc: NoteDocument,
    onBack: () -> Unit,
    onSave: (NoteDocument) -> Unit,
    onExcerptPage: (String, Page) -> Unit,
    onExportRequest: (Int) -> Unit,
    onEnterPip: () -> Unit,
    tabs: List<DocTab>,
    onTabSelect: (String) -> Unit,
    onTabClose: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialDoc.title) }
    // Phones get compact chrome so the page stays visible; tablets keep
    // full-size controls.
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val btnSize = if (compact) 38.dp else 48.dp
    val iconSize = if (compact) 20.dp else 24.dp

    val pagesState = remember {
        mutableStateListOf<Page>().apply { addAll(initialDoc.pages) }
    }
    var currentPageIndex by remember { mutableStateOf(0) }
    var currentTemplate by remember { mutableStateOf(initialDoc.pages.first().template) }
    val layersWork = remember { mutableStateListOf<LayerWork>() }

    var activeLayerId by remember { mutableStateOf<String?>(null) }
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var selectedStroke by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var viewScale by remember { mutableStateOf(1f) }
    var viewOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current.density
    var pageFormat by remember { mutableStateOf(initialDoc.pageFormat) }
    var viewReady by remember { mutableStateOf(false) }
    var showZoomBadge by remember { mutableStateOf(false) }
    var showPageSize by remember { mutableStateOf(false) }

    // Bounded page model: geometry lives in points on a page of
    // pageFormat.widthPt x pageFormat.heightPt. The minimum zoom is the
    // fit scale below; the maximum is 4x it (400%).
    val fitScaleNow: Float = if (canvasSize.width > 0) {
        val marginPx = 32f * density
        val capPx = 900f * density
        val wFit = min(
            (canvasSize.width - marginPx) / pageFormat.widthPt,
            capPx / pageFormat.widthPt
        )
        val hFit = (canvasSize.height - marginPx) / pageFormat.heightPt
        if (canvasSize.width > canvasSize.height) min(wFit, hFit) else wFit
    } else {
        1f
    }

    fun fitOffset(scale: Float): Offset {
        if (canvasSize.width == 0) return Offset.Zero
        val pw = pageFormat.widthPt * scale
        val ph = pageFormat.heightPt * scale
        val x = (canvasSize.width - pw) / 2f
        val y = if (ph <= canvasSize.height) {
            (canvasSize.height - ph) / 2f
        } else {
            16f * density
        }
        return Offset(x, y)
    }

    /** Pan clamp: at least 48dp of the page always stays on screen. */
    fun clampViewOffset(o: Offset, scale: Float): Offset {
        if (canvasSize.width == 0) return o
        val keep = 48f * density
        val pw = pageFormat.widthPt * scale
        val ph = pageFormat.heightPt * scale
        val xRange = (keep - pw)..(canvasSize.width - keep)
        val yRange = (keep - ph)..(canvasSize.height - keep)
        return Offset(
            x = if (xRange.start <= xRange.endInclusive) {
                o.x.coerceIn(xRange)
            } else {
                (canvasSize.width - pw) / 2f
            },
            y = if (yRange.start <= yRange.endInclusive) {
                o.y.coerceIn(yRange)
            } else {
                (canvasSize.height - ph) / 2f
            }
        )
    }

    fun resetViewToFit() {
        if (canvasSize.width > 0) {
            viewScale = fitScaleNow
            viewOffset = fitOffset(fitScaleNow)
        }
    }

    fun loadWork(page: Page) {
        layersWork.clear()
        page.layers.forEach { layersWork.add(LayerWork(it)) }
        activeLayerId = layersWork.firstOrNull()?.id
    }

    fun snapshotCurrentPage(): Page {
        val base = pagesState[currentPageIndex]
        return Page(
            id = base.id,
            template = currentTemplate,
            pdfPageIndex = base.pdfPageIndex,
            layers = layersWork.map { it.toLayer() }
        )
    }

    /** True when any stored content falls outside [fmt]'s page. */
    fun contentOutsidePage(fmt: PageFormat): Boolean {
        val pages = pagesState.mapIndexed { i, p ->
            if (i == currentPageIndex) snapshotCurrentPage() else p
        }
        return pages.any { page ->
            page.layers.any { layer ->
                layer.strokes.any { stroke ->
                    stroke.points.any { pt ->
                        pt.x < 0f || pt.y < 0f ||
                            pt.x > fmt.widthPt || pt.y > fmt.heightPt
                    }
                } || layer.texts.any { text ->
                    text.x < 0f || text.y < 0f ||
                        text.x > fmt.widthPt || text.y > fmt.heightPt
                }
            }
        }
    }

    // One-time initial load
    var initialized by remember { mutableStateOf(false) }
    if (!initialized) {
        loadWork(initialDoc.pages.first())
        initialized = true
    }

    // Frame the page (fit) as soon as the canvas size is known.
    LaunchedEffect(canvasSize) {
        if (canvasSize.width > 0 && !viewReady) {
            resetViewToFit()
            viewReady = true
        }
    }

    // Brief zoom-percentage badge while the zoom changes.
    LaunchedEffect(viewScale) {
        if (viewReady) {
            showZoomBadge = true
            delay(1200)
            showZoomBadge = false
        }
    }

    fun activeLayer(): LayerWork? =
        layersWork.firstOrNull { it.id == activeLayerId } ?: layersWork.firstOrNull()

    fun switchPage(index: Int) {
        if (index == currentPageIndex || index !in pagesState.indices) return
        pagesState[currentPageIndex] = snapshotCurrentPage()
        currentPageIndex = index
        currentTemplate = pagesState[index].template
        loadWork(pagesState[index])
        selectedTextId = null
        editingTextId = null
        selectedStroke = null
        resetViewToFit()
    }

    fun addPage() {
        pagesState[currentPageIndex] = snapshotCurrentPage()
        val newPage = Page.create(currentTemplate)
        pagesState.add(newPage)
        switchPage(pagesState.lastIndex)
    }

    var mode by remember { mutableStateOf(EditorMode.DRAW) }
    var tool by remember { mutableStateOf(Tool.PEN) }
    var penType by remember { mutableStateOf(PenTypes.PEN) }
    var inkColor by remember { mutableStateOf(Color.Black) }
    var thickness by remember { mutableStateOf(3f) }
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }

    // Two-finger page gestures (see the interceptor on the page area):
    // while one is running — and until every finger lifts — drawing input
    // is suppressed, and whatever the first finger had started is undone.
    var suppressDrawing by remember { mutableStateOf(false) }
    var eraserBackup by remember {
        mutableStateOf<Pair<String, List<NoteStroke>>?>(null)
    }
    var selectionDragBackup by remember {
        mutableStateOf<Triple<String, Int, List<StrokePoint>>?>(null)
    }
    val editorScope = rememberCoroutineScope()

    /** Discards whatever a half-finished one-finger action changed. */
    fun cancelInProgressGesture() {
        currentPoints = emptyList()
        eraserBackup?.let { (layerId, strokes) ->
            val layer = layersWork.firstOrNull { it.id == layerId }
            if (layer != null) {
                layer.strokes.clear()
                layer.strokes.addAll(strokes)
            }
        }
        eraserBackup = null
        selectionDragBackup?.let { (layerId, index, points) ->
            val layer = layersWork.firstOrNull { it.id == layerId }
            val stroke = layer?.strokes?.getOrNull(index)
            if (layer != null && stroke != null) {
                layer.strokes[index] = stroke.copy(points = points)
            }
        }
        selectionDragBackup = null
    }
    var beautifyOn by remember { mutableStateOf(false) }
    var chromeVisible by remember { mutableStateOf(true) }
    var popoverOpen by remember { mutableStateOf(false) }
    var toolsCollapsed by remember { mutableStateOf(false) }
    var pagePillFaded by remember { mutableStateOf(false) }
    LaunchedEffect(currentPageIndex, pagesState.size) {
        pagePillFaded = false
        delay(2000)
        pagePillFaded = true
    }
    val pagePillAlpha by animateFloatAsState(
        targetValue = if (pagePillFaded) 0.25f else 1f,
        animationSpec = tween(300),
        label = "pagePillAlpha"
    )
    var showLayers by remember { mutableStateOf(false) }
    var showOutline by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var newTextSize by remember { mutableStateOf(10f) }
    var newTextBold by remember { mutableStateOf(false) }
    var newTextItalic by remember { mutableStateOf(false) }
    var newTextFont by remember { mutableStateOf(TextFonts.DEFAULT) }
    var hexInput by remember { mutableStateOf(colorToHex(Color.Black)) }
    val focusRequester = remember { FocusRequester() }

    val undoStack = remember { mutableStateListOf<NoteStroke>() }
    val redoStack = remember { mutableStateListOf<NoteStroke>() }

    fun activeText(): TextElement? {
        val layer = activeLayer() ?: return null
        return layer.texts.firstOrNull { it.id == (editingTextId ?: selectedTextId) }
    }

    fun updateText(updated: TextElement) {
        for (layer in layersWork) {
            val i = layer.texts.indexOfFirst { it.id == updated.id }
            if (i >= 0) {
                layer.texts[i] = updated
                return
            }
        }
    }

    fun commitEditing() {
        val id = editingTextId ?: return
        for (layer in layersWork) {
            val el = layer.texts.firstOrNull { it.id == id }
            if (el != null && el.text.isBlank()) {
                layer.texts.remove(el)
                if (selectedTextId == id) selectedTextId = null
            }
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
        if (newTool != Tool.TEXT) selectedTextId = null
        when (newTool) {
            Tool.HIGHLIGHTER -> if (inkColor !in highlighterColors) applyColor(highlighterColors.first())
            Tool.PEN, Tool.TEXT -> if (inkColor in highlighterColors) applyColor(Color.Black)
            else -> Unit
        }
    }

    fun setMode(newMode: EditorMode) {
        commitEditing()
        mode = newMode
        if (newMode != EditorMode.SELECT) selectedStroke = null
        if (newMode == EditorMode.HAND) selectedTextId = null
    }

    fun eraseAt(point: StrokePoint) {
        val layer = activeLayer() ?: return
        if (layer.locked) return
        layer.strokes.removeAll { stroke ->
            stroke.points.any { p ->
                hypot(
                    (p.x - point.x).toDouble(),
                    (p.y - point.y).toDouble()
                ) < 24.0 / viewScale
            }
        }
    }

    fun deleteSelected() {
        val sel = selectedStroke
        if (sel != null) {
            val layer = layersWork.firstOrNull { it.id == sel.first }
            if (layer != null && sel.second in layer.strokes.indices) {
                layer.strokes.removeAt(sel.second)
            }
            selectedStroke = null
            return
        }
        val textId = selectedTextId
        if (textId != null) {
            for (layer in layersWork) {
                layer.texts.removeAll { it.id == textId }
            }
            selectedTextId = null
        }
    }

    fun buildDocument(): NoteDocument {
        pagesState[currentPageIndex] = snapshotCurrentPage()
        return initialDoc.copy(
            title = title.ifBlank { "Untitled" },
            pages = pagesState.toList(),
            pageFormat = pageFormat,
            updatedAt = System.currentTimeMillis()
        )
    }

    // Voice input: dictated text becomes a new text block on the page.
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            val layer = activeLayer()
            if (layer != null && !layer.locked) {
                val element = TextElement(
                    id = System.nanoTime().toString(),
                    x = 48f,
                    y = 96f,
                    text = spoken,
                    colorValue = inkColor.value,
                    fontSizeSp = newTextSize,
                    bold = newTextBold,
                    italic = newTextItalic,
                    fontFamilyName = newTextFont
                )
                layer.texts.add(element)
                selectedTextId = element.id
            }
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

    LaunchedEffect(editingTextId) {
        if (editingTextId != null) focusRequester.requestFocus()
    }

    // Auto-save when leaving the note.
    BackHandler {
        commitEditing()
        onSave(buildDocument())
        onBack()
    }

    // Switching tabs / leaving composition: persist the latest state too.
    DisposableEffect(Unit) {
        onDispose { onSave(buildDocument()) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (chromeVisible) {
            // Document tabs + slim toolbar (redesign)
            DocumentTabs(
                tabs = tabs,
                activeTabId = initialDoc.id,
                onSelect = onTabSelect,
                onClose = onTabClose
            )
            EditorToolbar(
                onBack = {
                    commitEditing()
                    onSave(buildDocument())
                    onBack()
                },
                onMic = { launchSpeech() },
                onReader = { chromeVisible = false },
                readerActive = false,
                onLayers = { showLayers = true },
                onAddPage = { addPage() },
                onSidePanel = { showOutline = true },
                onAdjust = { beautifyOn = !beautifyOn },
                adjustActive = beautifyOn,
                onRename = {
                    renameText = title
                    showRename = true
                },
                onSave = {
                    commitEditing()
                    onSave(buildDocument())
                },
                onEnterPip = onEnterPip,
                onExcerpt = {
                    commitEditing()
                    onExcerptPage(title.ifBlank { "Untitled" }, snapshotCurrentPage())
                },
                onExport = {
                    commitEditing()
                    onSave(buildDocument())
                    onExportRequest(currentPageIndex)
                },
                onFitToggle = { resetViewToFit() },
                fitIsWidth = true,
                onPageSize = { showPageSize = true }
            )

            val layerNow = activeLayer()
            if (layerNow != null && layerNow.locked) {
                Text(
                    text = "Layer \"${layerNow.name}\" is locked — unlock it in Layers to edit.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }

    // Two-finger pinch/pan for every mode except Hand (which has its own
    // transform detector). Runs on the Initial pass, ahead of the drawing
    // detectors: the moment a second finger lands, the in-progress action
    // is discarded, the gesture's events are consumed, and drawing stays
    // suppressed until every finger has lifted. A stylus counts as
    // drawing, never as a pinch finger; one touch beside a stylus is
    // treated as a palm and ignored.
    val multiTouchPageGestures = if (mode != EditorMode.HAND) {
        Modifier.pointerInput(mode, fitScaleNow, pageFormat) {
            fun clampPinchOffset(o: Offset, scale: Float): Offset {
                val cs = canvasSize
                if (cs.width == 0) return o
                val keep = 48f * density
                val pw = pageFormat.widthPt * scale
                val ph = pageFormat.heightPt * scale
                val xRange = (keep - pw)..(cs.width - keep)
                val yRange = (keep - ph)..(cs.height - keep)
                return Offset(
                    x = if (xRange.start <= xRange.endInclusive) {
                        o.x.coerceIn(xRange)
                    } else {
                        (cs.width - pw) / 2f
                    },
                    y = if (yRange.start <= yRange.endInclusive) {
                        o.y.coerceIn(yRange)
                    } else {
                        (cs.height - ph) / 2f
                    }
                )
            }

            awaitPointerEventScope {
                var pinching = false
                var prevCentroid = Offset.Zero
                var prevDistance = 0f
                var velocity = Offset.Zero
                var lastEventMs = 0L
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val touches = event.changes.filter {
                        it.type == PointerType.Touch && it.pressed
                    }
                    val stylusDown = event.changes.any {
                        it.type == PointerType.Stylus && it.pressed
                    }
                    if (touches.size >= 2 && !stylusDown) {
                        var centroid = Offset.Zero
                        for (change in touches) centroid += change.position
                        centroid /= touches.size.toFloat()
                        var distance = 0f
                        for (i in touches.indices) {
                            for (j in i + 1 until touches.size) {
                                distance = maxOf(
                                    distance,
                                    (touches[i].position - touches[j].position)
                                        .getDistance()
                                )
                            }
                        }
                        if (!pinching) {
                            pinching = true
                            suppressDrawing = true
                            cancelInProgressGesture()
                            prevCentroid = centroid
                            prevDistance = distance
                            velocity = Offset.Zero
                            lastEventMs = touches.first().uptimeMillis
                        } else {
                            val zoom = if (prevDistance > 0f) {
                                distance / prevDistance
                            } else {
                                1f
                            }
                            val pan = centroid - prevCentroid
                            // Rubber-band past the limits; the release
                            // handler snaps the zoom back into range.
                            val raw = viewScale * zoom
                            val clamped = raw.coerceIn(
                                fitScaleNow, fitScaleNow * 4f
                            )
                            val banded = when {
                                raw > clamped ->
                                    clamped + (raw - clamped) * 0.35f
                                raw < clamped ->
                                    clamped - (clamped - raw) * 0.35f
                                else -> raw
                            }
                            val factor = banded / viewScale
                            viewOffset = clampPinchOffset(
                                Offset(
                                    x = centroid.x -
                                        (centroid.x - viewOffset.x) * factor +
                                        pan.x,
                                    y = centroid.y -
                                        (centroid.y - viewOffset.y) * factor +
                                        pan.y
                                ),
                                banded
                            )
                            viewScale = banded
                            val nowMs = touches.first().uptimeMillis
                            val dt = (nowMs - lastEventMs).toFloat()
                            if (dt > 0f) {
                                val v = pan / dt * 1000f
                                velocity = velocity * 0.6f + v * 0.4f
                            }
                            lastEventMs = nowMs
                            prevCentroid = centroid
                            prevDistance = distance
                        }
                        event.changes.forEach {
                            if (it.pressed && it.type == PointerType.Touch) {
                                it.consume()
                            }
                        }
                    } else if (pinching) {
                        pinching = false
                        // Snap a rubber-banded zoom back into range,
                        // anchored at the last pinch center.
                        val target = viewScale.coerceIn(
                            fitScaleNow, fitScaleNow * 4f
                        )
                        if (target != viewScale) {
                            val f = target / viewScale
                            viewOffset = clampPinchOffset(
                                Offset(
                                    x = prevCentroid.x -
                                        (prevCentroid.x - viewOffset.x) * f,
                                    y = prevCentroid.y -
                                        (prevCentroid.y - viewOffset.y) * f
                                ),
                                target
                            )
                            viewScale = target
                        }
                        // Fling: keep the two-finger pan gliding with a
                        // natural exponential decay, clamped to the page.
                        if (velocity.getDistance() > 300f) {
                            val flingVelocity = velocity
                            editorScope.launch {
                                var v = flingVelocity
                                var last = -1L
                                while (v.getDistance() > 40f) {
                                    val frame = withFrameMillis { it }
                                    if (last >= 0) {
                                        val dt = (frame - last) / 1000f
                                        if (dt > 0f) {
                                            val proposed = Offset(
                                                viewOffset.x + v.x * dt,
                                                viewOffset.y + v.y * dt
                                            )
                                            val clampedNow = clampPinchOffset(
                                                proposed, viewScale
                                            )
                                            viewOffset = clampedNow
                                            if (clampedNow != proposed) break
                                            v *= kotlin.math.exp(-3.2f * dt)
                                        }
                                    }
                                    last = frame
                                }
                            }
                        }
                    }
                    if (event.changes.none { it.pressed }) {
                        suppressDrawing = false
                        pinching = false
                    }
                }
            }
        }
    } else {
        Modifier
    }

        // Page area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(DesignTokens.CanvasBackground)
                .onSizeChanged { canvasSize = it }
                .clipToBounds()
                .then(multiTouchPageGestures)
        ) {
            fun toPage(screen: Offset, pressure: Float = 1f): StrokePoint {
                val p = PageTransform.toPage(screen, viewScale, viewOffset)
                return StrokePoint(x = p.x, y = p.y, pressure = pressure)
            }

            val gestureModifier = when (mode) {
                EditorMode.HAND -> Modifier
                    .pointerInput(mode, fitScaleNow) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            // Zoom clamps to [fit, 400% of fit]; pan keeps
                            // at least 48dp of the page on screen.
                            val newScale = (viewScale * zoom)
                                .coerceIn(fitScaleNow, fitScaleNow * 4f)
                            val factor = newScale / viewScale
                            viewOffset = clampViewOffset(
                                Offset(
                                    x = centroid.x - (centroid.x - viewOffset.x) * factor + pan.x,
                                    y = centroid.y - (centroid.y - viewOffset.y) * factor + pan.y
                                ),
                                newScale
                            )
                            viewScale = newScale
                        }
                    }
                    .pointerInput(mode, fitScaleNow) {
                        detectTapGestures(
                            onDoubleTap = { pos ->
                                if (viewScale > fitScaleNow * 1.05f) {
                                    resetViewToFit()
                                } else {
                                    val target = fitScaleNow * 2f
                                    val f = target / viewScale
                                    viewOffset = clampViewOffset(
                                        Offset(
                                            pos.x - (pos.x - viewOffset.x) * f,
                                            pos.y - (pos.y - viewOffset.y) * f
                                        ),
                                        target
                                    )
                                    viewScale = target
                                }
                            }
                        )
                    }
                EditorMode.SELECT -> Modifier.pointerInput(mode) {
                    coroutineScope {
                        launch {
                            detectTapGestures { screen ->
                                if (suppressDrawing) return@detectTapGestures
                                val pagePoint = toPage(screen)
                                var found: Pair<String, Int>? = null
                                for (layer in layersWork.asReversed()) {
                                    if (!layer.visible) continue
                                    for (i in layer.strokes.indices.reversed()) {
                                        val s = layer.strokes[i]
                                        val hit = s.points.any { p ->
                                            hypot(
                                                (p.x - pagePoint.x).toDouble(),
                                                (p.y - pagePoint.y).toDouble()
                                            ) < 28.0 / viewScale
                                        }
                                        if (hit) {
                                            found = layer.id to i
                                            break
                                        }
                                    }
                                    if (found != null) break
                                }
                                selectedStroke = found
                                if (found != null) {
                                    selectedTextId = null
                                    activeLayerId = found.first
                                } else {
                                    selectedTextId = null
                                }
                            }
                        }
                        launch {
                            detectDragGestures(
                                onDragStart = {
                                    val sel = selectedStroke
                                    if (sel != null) {
                                        val layer = layersWork
                                            .firstOrNull { it.id == sel.first }
                                        val s = layer?.strokes
                                            ?.getOrNull(sel.second)
                                        if (layer != null && s != null) {
                                            selectionDragBackup = Triple(
                                                layer.id, sel.second, s.points
                                            )
                                        }
                                    }
                                },
                                onDragEnd = { selectionDragBackup = null },
                                onDragCancel = { cancelInProgressGesture() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val sel = if (suppressDrawing) {
                                        null
                                    } else {
                                        selectedStroke
                                    }
                                    if (sel != null) {
                                        val layer = layersWork.firstOrNull { it.id == sel.first }
                                        val idx = sel.second
                                        if (layer != null && idx in layer.strokes.indices) {
                                            val s = layer.strokes[idx]
                                            val dx = dragAmount.x / viewScale
                                            val dy = dragAmount.y / viewScale
                                            layer.strokes[idx] = s.copy(
                                                points = s.points.map { p ->
                                                    p.copy(x = p.x + dx, y = p.y + dy)
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                EditorMode.DRAW -> when (tool) {
                    Tool.TEXT -> Modifier.pointerInput(tool, selectedTextId, editingTextId) {
                        coroutineScope {
                            launch {
                                detectTapGestures { screen ->
                                    if (suppressDrawing) return@detectTapGestures
                                    when {
                                        editingTextId != null -> commitEditing()
                                        selectedTextId != null -> selectedTextId = null
                                        else -> {
                                            val layer = activeLayer()
                                            if (layer != null && !layer.locked) {
                                                val point = toPage(screen)
                                                if (point.x in 0f..pageFormat.widthPt && point.y in 0f..pageFormat.heightPt) {
                                                val element = TextElement(
                                                    id = System.nanoTime().toString(),
                                                    x = point.x,
                                                    y = point.y,
                                                    text = "",
                                                    colorValue = inkColor.value,
                                                    fontSizeSp = newTextSize,
                                                    bold = newTextBold,
                                                    italic = newTextItalic,
                                                    fontFamilyName = newTextFont
                                                )
                                                layer.texts.add(element)
                                                selectedTextId = element.id
                                                editingTextId = element.id
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            launch {
                                // Pinch anywhere on the page zooms the selected text.
                                detectTransformGestures { _, _, zoom, _ ->
                                    if (zoom != 1f && editingTextId == null) {
                                        val target = activeText()
                                        if (target != null) {
                                            updateText(
                                                target.copy(
                                                    fontSizeSp = (target.fontSizeSp * zoom)
                                                        .coerceIn(5f, 58f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> Modifier.pointerInput(tool, inkColor, thickness, penType, pageFormat) {
                        detectDragGestures(
                            onDragStart = { screen ->
                                val layer = activeLayer()
                                if (!suppressDrawing && layer != null && !layer.locked) {
                                    val point = toPage(screen)
                                    if (tool == Tool.ERASER) {
                                        if (eraserBackup == null) {
                                            eraserBackup =
                                                layer.id to layer.strokes.toList()
                                        }
                                        eraseAt(point)
                                    } else if (
                                        point.x in 0f..pageFormat.widthPt &&
                                        point.y in 0f..pageFormat.heightPt
                                    ) {
                                        currentPoints = listOf(point)
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val layer = activeLayer()
                                if (!suppressDrawing && layer != null && !layer.locked) {
                                    val point = toPage(
                                        change.position,
                                        change.pressure.coerceIn(0.1f, 1f)
                                    )
                                    if (tool == Tool.ERASER) {
                                        eraseAt(point)
                                    } else if (currentPoints.isNotEmpty()) {
                                        // Give the first point a real pressure too.
                                        currentPoints = if (currentPoints.size == 1) {
                                            listOf(
                                                currentPoints.first().copy(pressure = point.pressure),
                                                point
                                            )
                                        } else {
                                            currentPoints + point
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                val layer = activeLayer()
                                if (tool != Tool.ERASER && layer != null && !layer.locked &&
                                    currentPoints.isNotEmpty()
                                ) {
                                    val rawPoints = if (currentPoints.size == 1) {
                                        val only = currentPoints.first()
                                        currentPoints + only.copy(x = only.x + 0.1f)
                                    } else {
                                        currentPoints
                                    }
                                    val finalPoints = if (beautifyOn) {
                                        StrokeGeometry.beautify(rawPoints)
                                    } else {
                                        rawPoints
                                    }
                                    layer.strokes.add(
                                        NoteStroke(
                                            points = finalPoints,
                                            colorValue = inkColor.value,
                                            thickness = thickness,
                                            highlighter = tool == Tool.HIGHLIGHTER,
                                            penType = if (tool == Tool.PEN) {
                                                penType
                                            } else {
                                                PenTypes.PEN
                                            }
                                        )
                                    )
                                    redoStack.clear()
                                    undoStack.clear()
                                }
                                currentPoints = emptyList()
                                eraserBackup = null
                            },
                            onDragCancel = { cancelInProgressGesture() }
                        )
                    }
                }
            }

            val selectionColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(gestureModifier)
            ) {
                // The bounded page sheet: soft shadow + white paper. The
                // template pattern is clipped to the page rectangle.
                val sheetSize = androidx.compose.ui.geometry.Size(
                    pageFormat.widthPt * viewScale,
                    pageFormat.heightPt * viewScale
                )
                drawRoundRect(
                    color = Color(0x22000000),
                    topLeft = Offset(viewOffset.x, viewOffset.y + 6f),
                    size = sheetSize,
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRect(
                    color = Color.White,
                    topLeft = viewOffset,
                    size = sheetSize
                )
                // The part of the page currently on screen, in page
                // coordinates: the pattern generator draws only inside it.
                val visiblePageRect = androidx.compose.ui.geometry.Rect(
                    left = maxOf(0f, -viewOffset.x / viewScale),
                    top = maxOf(0f, -viewOffset.y / viewScale),
                    right = minOf(
                        pageFormat.widthPt,
                        (size.width - viewOffset.x) / viewScale
                    ),
                    bottom = minOf(
                        pageFormat.heightPt,
                        (size.height - viewOffset.y) / viewScale
                    )
                )
                withTransform({
                    translate(viewOffset.x, viewOffset.y)
                    scale(viewScale, viewScale, Offset.Zero)
                }) {
                    // Clip ink to the page through the raw canvas
                    // (DrawScope.clipRect is unavailable in this Compose
                    // version); the transform makes these page coords.
                    drawContext.canvas.save()
                    drawContext.canvas.clipRect(
                        androidx.compose.ui.geometry.Rect(
                            0f, 0f, pageFormat.widthPt, pageFormat.heightPt
                        )
                    )
                    try {
                        // Paper template: generated in page coordinates,
                        // inside the same transform and page clip as the
                        // ink, so it can never drift from the page or leak
                        // onto the gray surround.
                        drawPagePattern(
                            template = currentTemplate,
                            pageWidth = pageFormat.widthPt,
                            pageHeight = pageFormat.heightPt,
                            visible = visiblePageRect
                        )
                    for (layer in layersWork) {
                        if (!layer.visible) continue
                        for (stroke in layer.strokes) {
                            drawStrokeSmooth(stroke)
                        }
                    }
                    if (currentPoints.isNotEmpty() && mode == EditorMode.DRAW &&
                        tool != Tool.ERASER && tool != Tool.TEXT
                    ) {
                        drawStrokeSmooth(
                            NoteStroke(
                                points = currentPoints,
                                colorValue = inkColor.value,
                                thickness = thickness,
                                highlighter = tool == Tool.HIGHLIGHTER,
                                penType = if (tool == Tool.PEN) penType else PenTypes.PEN
                            )
                        )
                    }
                    // Selection box around the selected stroke (Select mode)
                    if (mode == EditorMode.SELECT) {
                        val sel = selectedStroke
                        if (sel != null) {
                            val layer = layersWork.firstOrNull { it.id == sel.first }
                            val s = layer?.strokes?.getOrNull(sel.second)
                            if (s != null && s.points.isNotEmpty()) {
                                val minX = s.points.minOf { it.x } - 14f
                                val minY = s.points.minOf { it.y } - 14f
                                val maxX = s.points.maxOf { it.x } + 14f
                                val maxY = s.points.maxOf { it.y } + 14f
                                drawRect(
                                    color = selectionColor,
                                    topLeft = Offset(minX, minY),
                                    size = androidx.compose.ui.geometry.Size(
                                        maxX - minX,
                                        maxY - minY
                                    ),
                                    style = Stroke(width = 2f / viewScale)
                                )
                            }
                        }
                    }
                    } finally {
                        drawContext.canvas.restore()
                    }
                }
            }

            // Text overlays live in a page-sized box pinned to the
            // sheet and clipped to its edge; positions inside are
            // page-local (the same pattern as the PDF page box).
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            viewOffset.x.roundToInt(),
                            viewOffset.y.roundToInt()
                        )
                    }
                    .size(
                        width = (pageFormat.widthPt * viewScale / density).dp,
                        height = (pageFormat.heightPt * viewScale / density).dp
                    )
                    .clipToBounds()
            ) {
            // Typed text elements (visible layers only)
            for (layer in layersWork) {
                if (!layer.visible) continue
                val isActiveLayer = layer.id == activeLayer()?.id
                for (element in layer.texts) {
                    key(element.id) {
                        val isEditing = element.id == editingTextId
                        val isSelected = element.id == selectedTextId
                        val position = Modifier.offset {
                            IntOffset(
                                (element.x * viewScale).roundToInt(),
                                (element.y * viewScale).roundToInt()
                            )
                        }
                        if (isEditing) {
                            BasicTextField(
                                value = element.text,
                                onValueChange = { newValue ->
                                    updateText(element.copy(text = newValue))
                                },
                                textStyle = textStyleFor(element, viewScale),
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
                            val textInteractive = editingTextId == null && !layer.locked &&
                                (
                                    mode == EditorMode.SELECT ||
                                        (mode == EditorMode.DRAW && tool == Tool.TEXT && isActiveLayer)
                                    )
                            Text(
                                text = element.text,
                                style = textStyleFor(element, viewScale),
                                modifier = position
                                    .then(
                                        if (textInteractive) {
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
                                                .pointerInput(element.id, viewScale) {
                                                    // pan/zoom are per-event changes, so
                                                    // apply them to the freshest state.
                                                    detectTransformGestures { _, pan, zoom, _ ->
                                                        if (suppressDrawing) {
                                                            return@detectTransformGestures
                                                        }
                                                        val base = layersWork
                                                            .firstOrNull { it.id == layer.id }
                                                            ?.texts
                                                            ?.firstOrNull { it.id == element.id }
                                                            ?: element
                                                        updateText(
                                                            base.copy(
                                                                x = base.x + pan.x / viewScale,
                                                                y = base.y + pan.y / viewScale,
                                                                fontSizeSp = (base.fontSizeSp * zoom)
                                                                    .coerceIn(5f, 58f)
                                                            )
                                                        )
                                                    }
                                                }
                                                .clickable {
                                                    if (mode == EditorMode.SELECT) {
                                                        selectedTextId = element.id
                                                        activeLayerId = layer.id
                                                        selectedStroke = null
                                                    } else if (selectedTextId == element.id) {
                                                        editingTextId = element.id
                                                    } else {
                                                        selectedTextId = element.id
                                                        activeLayerId = layer.id
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

            // Zoom percentage, shown briefly while zooming.
            if (showZoomBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White,
                        shadowElevation = 6.dp
                    ) {
                        Text(
                            text = "${(viewScale / fitScaleNow * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = DesignTokens.TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }


            // Fullscreen toggle (always available)
            IconButton(
                onClick = { chromeVisible = !chromeVisible },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        CircleShape
                    )
            ) {
                Icon(
                    if (chromeVisible) Icons.Filled.Fullscreen else Icons.Filled.FullscreenExit,
                    contentDescription = "Toggle full screen"
                )
            }
            // Floating tools (redesign): options popover + toolbar pill,
            // docked at the bottom center of the canvas. The drawing rows
            // that used to sit under the top toolbar live here now.
            if (chromeVisible) {
                val popoverHasContent = mode == EditorMode.SELECT ||
                    (mode == EditorMode.DRAW && tool != Tool.ERASER)
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
                    // Pen type picker (Pen tool only)
                    if (mode == EditorMode.DRAW && tool == Tool.PEN) {
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
                                    label = { Text(penTypeLabel(type)) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    }
                // Rich text formatting bar
                if (mode == EditorMode.DRAW && tool == Tool.TEXT) {
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
                                if (boldActive) DesignTokens.AccentTint else Color.Transparent,
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
                                if (italicActive) DesignTokens.AccentTint else Color.Transparent,
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
                                for (layer in layersWork) {
                                    layer.texts.removeAll { it.id == selectedTextId }
                                }
                                selectedTextId = null
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete selected text")
                            }
                        }
                    }
                    if (!compact) {
                        Text(
                            text = "Tap the page to type. Tap text to select, tap again to edit. Pinch to resize it, or pinch the page to zoom a selected text.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
                // Palette + size + hex (toggle with the palette button; also
                // hidden while erasing)
                if (mode == EditorMode.DRAW && tool != Tool.ERASER) {
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
                                            color = if (c == inkColor) DesignTokens.Accent else Color.Gray,
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
                                valueRange = 5f..58f,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            NumericSizeField(
                                value = target?.fontSizeSp ?: newTextSize,
                                onApply = { v ->
                                    val clamped = v.coerceIn(5f, 58f)
                                    if (target != null) {
                                        updateText(target.copy(fontSizeSp = clamped))
                                    } else {
                                        newTextSize = clamped
                                    }
                                }
                            )
                            Text("pt", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Slider(
                                value = thickness,
                                onValueChange = { thickness = it },
                                valueRange = 0.5f..29f,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            NumericSizeField(
                                value = thickness,
                                onApply = { v -> thickness = v.coerceIn(0.5f, 29f) }
                            )
                            Text("pt", style = MaterialTheme.typography.bodySmall)
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
                                onDone = { parseHexColor(hexInput)?.let { applyColor(it) } }
                            ),
                            modifier = Modifier.width(150.dp)
                        )
                        IconButton(onClick = {
                            parseHexColor(hexInput)?.let { applyColor(it) }
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = "Apply hex color")
                        }
                        Text(text = "Hex color", style = MaterialTheme.typography.bodySmall)
                    }
                }
                // Select mode actions
                if (mode == EditorMode.SELECT) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap ink or text to select it, then drag to move it.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { deleteSelected() },
                            modifier = Modifier.size(btnSize),
                            enabled = selectedStroke != null || selectedTextId != null
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete selection",
                                modifier = Modifier.size(iconSize)
                            )
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
                            mode = mode,
                            onModeTap = { m ->
                                if (m == mode) {
                                    if (m != EditorMode.HAND) popoverOpen = !popoverOpen
                                } else {
                                    setMode(m)
                                    popoverOpen = false
                                }
                            },
                            tool = tool,
                            onToolTap = { t ->
                                if (mode == EditorMode.DRAW && t == tool) {
                                    popoverOpen = !popoverOpen
                                } else {
                                    if (mode != EditorMode.DRAW) setMode(EditorMode.DRAW)
                                    selectTool(t)
                                }
                            },
                            magicActive = beautifyOn,
                            onMagicTap = { beautifyOn = !beautifyOn },
                            inkColor = inkColor,
                            onColorTap = {
                                if (mode != EditorMode.DRAW) setMode(EditorMode.DRAW)
                                if (tool == Tool.ERASER) selectTool(Tool.PEN)
                                popoverOpen = !popoverOpen
                            },
                            onUndo = {
                                val layer = activeLayer()
                                if (layer != null && !layer.locked && layer.strokes.isNotEmpty()) {
                                    redoStack.add(layer.strokes.removeAt(layer.strokes.lastIndex))
                                }
                            },
                            onRedo = {
                                val layer = activeLayer()
                                if (layer != null && !layer.locked && redoStack.isNotEmpty()) {
                                    layer.strokes.add(redoStack.removeAt(redoStack.lastIndex))
                                }
                            },
                            onDelete = {
                                if (selectedStroke != null || selectedTextId != null) {
                                    deleteSelected()
                                } else {
                                    val layer = activeLayer()
                                    if (layer != null && !layer.locked) {
                                        layer.strokes.clear()
                                        redoStack.clear()
                                    }
                                }
                            },
                            deleteEnabled = selectedStroke != null || selectedTextId != null ||
                                (activeLayer()?.strokes?.isNotEmpty() == true),
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
                // Page navigation pill: bottom right, fades when idle.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 64.dp)
                        .alpha(pagePillAlpha)
                ) {
                    PageIndicatorPill(
                        label = "Page ${currentPageIndex + 1} / ${pagesState.size}",
                        onPrev = { switchPage(currentPageIndex - 1) },
                        onNext = { switchPage(currentPageIndex + 1) },
                        onAdd = { addPage() }
                    )
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
                    commitEditing()
                    onSave(buildDocument())
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            }
        )
    }

    if (showPageSize) {
        PageSizePickerDialog(
            current = pageFormat,
            clipsContent = { candidate -> contentOutsidePage(candidate) },
            onDismiss = { showPageSize = false },
            onPick = { picked ->
                pageFormat = picked
                showPageSize = false
                resetViewToFit()
                onSave(buildDocument())
            }
        )
    }

    if (showLayers) {
        LayersDialog(
            layers = layersWork,
            activeLayerId = activeLayer()?.id,
            onSelect = { id ->
                activeLayerId = id
                selectedTextId = null
                editingTextId = null
            },
            onAdd = {
                val layer = LayerWork(Layer.create("Layer ${layersWork.size + 1}"))
                layersWork.add(layer)
                activeLayerId = layer.id
            },
            onDelete = { work ->
                if (layersWork.size > 1) {
                    layersWork.remove(work)
                    if (activeLayerId == work.id) {
                        activeLayerId = layersWork.firstOrNull()?.id
                    }
                }
            },
            onMove = { work, direction ->
                val index = layersWork.indexOf(work)
                val target = index + direction
                if (index >= 0 && target in layersWork.indices) {
                    layersWork.removeAt(index)
                    layersWork.add(target, work)
                }
            },
            onDismiss = { showLayers = false }
        )
    }

    if (showOutline) {
        val outlineItems = layersWork.flatMap { layer ->
            layer.texts.map { layer to it }
        }.sortedBy { it.second.y }
        AlertDialog(
            onDismissRequest = { showOutline = false },
            title = { Text("Outline") },
            text = {
                Column {
                    if (outlineItems.isEmpty()) {
                        Text("No text on this page yet. Text blocks you add will appear here, nested by size.")
                    } else {
                        outlineItems.forEach { (layer, element) ->
                            val indent = when {
                                element.fontSizeSp >= 30f -> ""
                                element.fontSizeSp >= 22f -> "    "
                                else -> "        "
                            }
                            TextButton(onClick = {
                                activeLayerId = layer.id
                                selectedTextId = element.id
                                if (mode != EditorMode.DRAW) mode = EditorMode.DRAW
                                if (tool != Tool.TEXT) tool = Tool.TEXT
                                showOutline = false
                            }) {
                                Text(
                                    text = indent + element.text.lineSequence()
                                        .firstOrNull().orEmpty()
                                        .ifBlank { "(empty text)" },
                                    fontWeight = if (element.fontSizeSp >= 30f) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOutline = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun LayersDialog(
    layers: List<LayerWork>,
    activeLayerId: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (LayerWork) -> Unit,
    onMove: (LayerWork, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var renameTarget by remember { mutableStateOf<LayerWork?>(null) }
    var renameText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Layers") },
        text = {
            Column {
                Text(
                    text = "Bottom layers draw first. Tap a layer to draw on it, tap its name to rename.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.size(8.dp))
                // Show top-most first in the list, like most editors.
                layers.asReversed().forEach { layer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (layer.id == activeLayerId) {
                                    DesignTokens.AccentTint
                                } else {
                                    Color.Transparent
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelect(layer.id) }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = layer.name,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    renameTarget = layer
                                    renameText = layer.name
                                }
                        )
                        IconButton(onClick = { layer.visible = !layer.visible }) {
                            Icon(
                                if (layer.visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Toggle visibility"
                            )
                        }
                        IconButton(onClick = { layer.locked = !layer.locked }) {
                            Icon(
                                if (layer.locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = "Toggle lock"
                            )
                        }
                        IconButton(onClick = { onMove(layer, 1) }) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Move layer up")
                        }
                        IconButton(onClick = { onMove(layer, -1) }) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Move layer down")
                        }
                        IconButton(onClick = { onDelete(layer) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete layer")
                        }
                    }
                }
                TextButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Add layer")
                }
                val target = renameTarget
                if (target != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (renameText.isNotBlank()) target.name = renameText
                            renameTarget = null
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = "Rename layer")
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

/** Draw one stroke as smooth segments, shaped by its pen type. */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokeSmooth(stroke: NoteStroke) {
    val isPencil = stroke.penType == PenTypes.PENCIL && !stroke.highlighter
    val isCalligraphy = stroke.penType == PenTypes.CALLIGRAPHY && !stroke.highlighter
    val points = if (isPencil) {
        // Pencil keeps its grain: only light jitter filtering, no smoothing.
        StrokeGeometry.filterJitter(stroke.points, minDistance = 0.8f)
    } else {
        StrokeGeometry.smoothed(stroke.points)
    }
    if (points.size < 2) return
    val baseColor = Color(stroke.colorValue)
    if (stroke.highlighter) {
        // Draw the highlight as ONE path with Multiply blending: overlapping
        // segments can't stack up into an opaque blob, and dark text under
        // the highlight stays fully readable (like a real highlighter).
        val path = Path()
        path.moveTo(points.first().x, points.first().y)
        for (p in points.drop(1)) path.lineTo(p.x, p.y)
        drawPath(
            path = path,
            color = baseColor.copy(alpha = 0.55f),
            style = Stroke(
                width = stroke.thickness.coerceAtLeast(0.5f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            ),
            blendMode = BlendMode.Multiply
        )
        return
    }
    val drawColor = when {
        isPencil -> baseColor.copy(alpha = 0.85f)
        else -> baseColor
    }
    val nibAngle = PI / 4.0 // 45-degree broad nib for calligraphy
    for (i in 0 until points.size - 1) {
        val a = points[i]
        val b = points[i + 1]
        val avgPressure = (a.pressure + b.pressure) / 2f
        val width = when {
            stroke.highlighter -> stroke.thickness
            isPencil -> stroke.thickness * 0.55f * (0.4f + 0.6f * avgPressure)
            isCalligraphy -> {
                val angle = atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())
                val f = 0.15f + 0.85f * abs(sin(angle - nibAngle)).toFloat()
                stroke.thickness * 1.7f * f
            }
            else -> stroke.thickness * (0.3f + 0.7f * avgPressure)
        }
        drawLine(
            color = drawColor,
            start = Offset(a.x, a.y),
            end = Offset(b.x, b.y),
            strokeWidth = width.coerceAtLeast(0.5f),
            cap = StrokeCap.Round
        )
    }
}
