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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import com.riguel.notestar.data.BoxGeom
import com.riguel.notestar.data.EditorMode
import com.riguel.notestar.data.Layer
import com.riguel.notestar.data.clampVisibleCoord
import com.riguel.notestar.data.dragPreviewCoord
import com.riguel.notestar.data.horizontalGuides
import com.riguel.notestar.data.resizeLeftPreview
import com.riguel.notestar.data.resizeRightPreview
import com.riguel.notestar.data.snapAxisCoord
import com.riguel.notestar.data.verticalGuides
import com.riguel.notestar.data.NoteDocument
import com.riguel.notestar.data.Page
import com.riguel.notestar.data.PageFormat
import com.riguel.notestar.data.PageTemplate
import com.riguel.notestar.data.PageViewState
import com.riguel.notestar.data.ToolPrefs
import com.riguel.notestar.data.PenTypes
import com.riguel.notestar.data.StrokeGeometry
import com.riguel.notestar.data.presetsForTool
import com.riguel.notestar.data.StrokePoint
import com.riguel.notestar.data.TextElement
import com.riguel.notestar.data.TextGeometry
import com.riguel.notestar.data.TextMetrics
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
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

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

/** Prefs key for a tool's remembered color/size (see ToolPrefs). */
private fun toolKey(t: Tool, pt: String): String = when (t) {
    Tool.PEN -> "pen_$pt"
    Tool.HIGHLIGHTER -> "highlighter"
    Tool.ERASER -> "eraser"
    Tool.TEXT -> "text"
    else -> "pen_pen"
}

private fun penTypeLabel(name: String): String = when (name) {
    PenTypes.PENCIL -> "Pencil"
    PenTypes.CALLIGRAPHY -> "Calligraphy"
    else -> "Pen"
}

/**
 * Legacy style used ONLY by the imported-PDF editor (PdfNoteScreen).
 * It treats the stored size as sp and is left exactly as it was, so the
 * PDF editor does not change in v0.13.
 */
fun textStyleFor(element: TextElement, scale: Float = 1f): TextStyle = TextStyle(
    color = Color(element.colorValue),
    fontSize = (element.fontSizeSp * scale).sp,
    fontWeight = if (element.bold) FontWeight.Bold else FontWeight.Normal,
    fontStyle = if (element.italic) FontStyle.Italic else FontStyle.Normal,
    fontFamily = fontFamilyFor(element.fontFamilyName)
)

/**
 * Normal-note style (v0.13). The stored size is in PAGE POINTS, and
 * [scale] is pixels per point, so the wanted pixel size is size*scale —
 * but Compose sp values are multiplied by scaledDensity on their way to
 * pixels, so the sp value must be size*scale/scaledDensity (see
 * TextGeometry.spValueForPagePt). Giving Compose size*scale directly was
 * the v0.13 text-size bug: on-screen text was scaledDensity times too
 * big, while the exporter wrote the stored points unchanged. Line height
 * is the font's real metrics (TextMetrics), not an estimate, and
 * alignment is always Start, matching the exporter.
 */
fun textStyleForPage(
    element: TextElement,
    scale: Float,
    scaledDensity: Float
): TextStyle {
    val ratios = TextMetrics.ratios(
        element.fontFamilyName, element.bold, element.italic
    )
    val lineHeightPt = TextGeometry.lineHeightPt(element, ratios.lineHeight)
    return TextStyle(
        color = Color(element.colorValue),
        fontSize = TextGeometry.spValueForPagePt(
            element.fontSizeSp, scale, scaledDensity
        ).sp,
        lineHeight = TextGeometry.spValueForPagePt(
            lineHeightPt, scale, scaledDensity
        ).sp,
        fontWeight = if (element.bold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (element.italic) FontStyle.Italic else FontStyle.Normal,
        fontFamily = fontFamilyFor(element.fontFamilyName),
        textAlign = TextAlign.Start,
        textDecoration = if (element.underline) {
            TextDecoration.Underline
        } else {
            TextDecoration.None
        }
    )
}

/**
 * Draw one text box on the page Canvas, inside the caller's
 * withTransform + page clip (v0.14.1, fixed in v0.14.2). This is the
 * ONLY display path for non-editing text: the old separate
 * Text-composable overlay is gone, so text cannot diverge from ink —
 * both go through the same matrix M (translate + uniform scale,
 * pivot at the page origin) and the same page-rectangle clip.
 *
 * How it stays in page units: the layout is measured with a TextStyle
 * whose sp values are the page-unit sizes divided by scaledDensity
 * (the same trick as textStyleForPage), so the measured layout is
 * exactly fontSizePt wide/tall in "pixels" — i.e. in page units.
 * Drawn inside the transform, the matrix then scales it to
 * fontSize x zoom on screen, so text stays the same size relative to
 * the page at every zoom and stays crisp (vector glyphs, never a
 * bitmap). The layout wraps at the stored width, its top is
 * boxTop + padding, and its line height is the real line height —
 * the same numbers the exporter uses. Emoji renders through Compose
 * text layout's system font fallback on the same matrix, not as a
 * separate view.
 *
 * v0.14.2 note: the first version of this function used
 * drawContext.canvas.nativeCanvas, which does not resolve on this
 * Compose version (BOM 2024.09.00) — that was the v0.14.1 build
 * failure. The documented Compose API for canvas text is
 * TextMeasurer + DrawScope.drawText, used here instead.
 */
@OptIn(ExperimentalTextApi::class)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPageText(
    textMeasurer: TextMeasurer,
    element: TextElement,
    pageWidthPt: Float,
    scaledDensity: Float
) {
    if (element.text.isEmpty()) return
    val wrapWidth = TextGeometry.wrapWidthPt(element, pageWidthPt)
    val lineHeightPt = TextMetrics.lineHeightPt(element)
    val style = TextStyle(
        color = Color(element.colorValue),
        fontSize = (element.fontSizeSp / scaledDensity).sp,
        lineHeight = (lineHeightPt / scaledDensity).sp,
        fontWeight = if (element.bold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (element.italic) FontStyle.Italic else FontStyle.Normal,
        fontFamily = fontFamilyFor(element.fontFamilyName),
        textAlign = TextAlign.Start,
        textDecoration = if (element.underline) {
            TextDecoration.Underline
        } else {
            TextDecoration.None
        }
    )
    val layout = textMeasurer.measure(
        text = AnnotatedString(element.text),
        style = style,
        constraints = Constraints(
            maxWidth = wrapWidth.toInt(),
            maxHeight = Constraints.Infinity
        )
    )
    drawText(
        layout,
        topLeft = Offset(
            element.x + element.paddingPt,
            element.y + element.paddingPt
        )
    )
}

/**
 * One undoable action (v0.15.4). Strokes were the only undoable
 * thing before, and the old undo button just popped the layer's
 * last stroke; text moves now record here too, so one undo step
 * restores exactly one drag's (or one nudge burst's) previous
 * geometry. Width is stored because a resize changes it alongside x.
 */
private sealed interface EditorUndo {
    data class Stroke(val layerId: String, val stroke: NoteStroke) : EditorUndo
    data class TextGeom(
        val layerId: String,
        val textId: String,
        val beforeX: Float,
        val beforeY: Float,
        val beforeWidth: Float?,
        val afterX: Float,
        val afterY: Float,
        val afterWidth: Float?
    ) : EditorUndo
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

@OptIn(ExperimentalTextApi::class)
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
    // Live-drag state for an explicit Select-mode move/resize on
    // the selected text box (v0.15.4). The stored element is never
    // written during a drag: the text, its frame and its handles
    // all draw from the preview below, and the stored position is
    // written exactly once, on release. Cancelling (pinch, tool
    // change, system cancel) just drops the preview — the stored
    // position was never touched, so there is nothing to restore
    // and no jump. Each drag is keyed by element id, so a stale
    // drag can never apply to a different box.
    //
    // Move: grab is the page-unit offset between the touch point
    // (taken when the 8dp slop is passed, so the slop distance is
    // absorbed and the box does not move at drag start) and the
    // box's top-left; preview = touchPage - grab, every move event.
    var textDragId by remember { mutableStateOf<String?>(null) }
    var textDragOrigin by remember { mutableStateOf(Offset.Zero) }
    var textDragGrab by remember { mutableStateOf(Offset.Zero) }
    var textDragPreview by remember { mutableStateOf<Offset?>(null) }
    // Resize: preview (x, widthPt) for the corner being dragged.
    var textResizeId by remember { mutableStateOf<String?>(null) }
    var textResizeStartX by remember { mutableStateOf(0f) }
    var textResizeStartWidth by remember { mutableStateOf(0f) }
    var textResizePreview by remember {
        mutableStateOf<Pair<Float, Float>?>(null)
    }
    var textResizeLeftCorner by remember { mutableStateOf(false) }
    var textResizeGrabDx by remember { mutableStateOf(0f) }
    // Shared by both drags: last touch in page-area screen px (edge
    // auto-pan reads it) and the guides currently snapped to.
    var textDragTouch by remember { mutableStateOf<Offset?>(null) }
    var textDragGuidesV by remember { mutableStateOf<List<Float>>(emptyList()) }
    var textDragGuidesH by remember { mutableStateOf<List<Float>>(emptyList()) }
    // Alignment snap, off in Settings ("snap_to_guides", default on).
    // LocalContext directly: toolContext is declared later in this
    // composable, and a local must be declared before it is used.
    val snapToGuides = remember {
        LocalContext.current.getSharedPreferences(
            "settings", Context.MODE_PRIVATE
        ).getBoolean("snap_to_guides", true)
    }
    // One nudge burst (press to release) records one undo step.
    var nudgeBurst by remember {
        mutableStateOf<Triple<String, Float, Float>?>(null)
    }
    var selectedStroke by remember { mutableStateOf<Pair<String, Int>?>(null) }
    // The single view transform (v0.14.1): every layer below reads
    // scale and offset from this one object, never from a local copy.
    val pageViewState = remember { PageViewState() }
    // Measures canvas text layouts (v0.14.2): one measurer for the
    // whole editor, reused for every box on every frame.
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current.density
    // sp -> pixels multiplier (density x fontScale). The page-text style
    // divides by this, so a stored point size lands at size*pageViewState.scale px.
    val scaledDensity = LocalDensity.current.density * LocalDensity.current.fontScale
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
            pageViewState.scale = fitScaleNow
            pageViewState.offset = fitOffset(fitScaleNow)
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
    LaunchedEffect(pageViewState.scale) {
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

    val toolContext = LocalContext.current
    var mode by remember { mutableStateOf(EditorMode.DRAW) }
    var tool by remember { mutableStateOf(Tool.PEN) }
    var penType by remember {
        mutableStateOf(ToolPrefs.penType(toolContext, PenTypes.PEN))
    }
    // Tool settings are remembered per tool in ToolPrefs and restored
    // when switching back (v0.15.0); the old code forced highlighter
    // to yellow and pen/text to black on every switch instead.
    var inkColor by remember {
        mutableStateOf(
            ToolPrefs.colorForTool(toolContext, "pen_$penType", Color.Black)
        )
    }
    var thickness by remember {
        mutableStateOf(
            ToolPrefs.sizeForTool(toolContext, "pen_$penType", 3f)
        )
    }
    var eraserSize by remember {
        mutableStateOf(ToolPrefs.sizeForTool(toolContext, "eraser", 24f))
    }
    var pressureEnabled by remember {
        mutableStateOf(ToolPrefs.pressureEnabled(toolContext))
    }
    // Shared tool options sheet state (v0.15.0).
    var sheetView by remember { mutableStateOf(ToolSheetView.MAIN) }
    var sheetEntryView by remember { mutableStateOf(ToolSheetView.MAIN) }
    var recentColors by remember {
        mutableStateOf(ToolPrefs.recentColors(toolContext))
    }
    var customColors by remember {
        mutableStateOf(ToolPrefs.customColors(toolContext))
    }
    // On-canvas brush ring: shown while the size slider drags, and
    // for 500ms after release (session guards against a stale hide).
    var brushRingVisible by remember { mutableStateOf(false) }
    var brushRingSession by remember { mutableStateOf(0) }
    LaunchedEffect(inkColor, tool, penType) {
        ToolPrefs.saveColorForTool(
            toolContext, toolKey(tool, penType), inkColor
        )
    }
    LaunchedEffect(thickness, tool, penType) {
        if (tool != Tool.TEXT && tool != Tool.ERASER) {
            ToolPrefs.saveSizeForTool(
                toolContext, toolKey(tool, penType), thickness
            )
        }
    }
    LaunchedEffect(eraserSize) {
        ToolPrefs.saveSizeForTool(toolContext, "eraser", eraserSize)
    }
    LaunchedEffect(penType) {
        ToolPrefs.savePenType(toolContext, penType)
    }
    LaunchedEffect(pressureEnabled) {
        ToolPrefs.setPressureEnabled(toolContext, pressureEnabled)
    }
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

    /** Drops any live text-drag preview without writing anything. */
    fun clearTextDragState() {
        textDragId = null
        textDragPreview = null
        textDragTouch = null
        textDragGuidesV = emptyList()
        textDragGuidesH = emptyList()
        textResizeId = null
        textResizePreview = null
    }

    /** Discards whatever a half-finished one-finger action changed. */
    fun cancelInProgressGesture() {
        currentPoints = emptyList()
        // A pinch mid-drag cancels the move/resize: the preview is
        // dropped and the stored position was never written, so the
        // box is back where it was. The gesture's own lift handler
        // is guarded on textDragId, so it cannot commit afterwards.
        clearTextDragState()
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
    // Edge auto-pan (v0.15.4): while a text drag is active and the
    // finger sits within 48dp of a viewport edge, the page pans
    // slowly that way (~120dp/s). The preview is recomputed from
    // the current touch through M on every move event, so panning
    // cannot move the box off the finger — its screen position
    // depends only on the touch and the grab, never on the offset.
    // The existing 48dp pan clamp keeps the page on screen.
    LaunchedEffect(textDragId, textResizeId) {
        if (textDragId == null && textResizeId == null) {
            return@LaunchedEffect
        }
        while (textDragId != null || textResizeId != null) {
            val touch = textDragTouch
            if (touch != null && canvasSize.width > 0) {
                val edge = 48f * density
                val speed = 2f * density
                var dx = 0f
                var dy = 0f
                if (touch.x < edge) {
                    dx = speed
                } else if (touch.x > canvasSize.width - edge) {
                    dx = -speed
                }
                if (touch.y < edge) {
                    dy = speed
                } else if (touch.y > canvasSize.height - edge) {
                    dy = -speed
                }
                if (dx != 0f || dy != 0f) {
                    pageViewState.offset = clampViewOffset(
                        pageViewState.offset + Offset(dx, dy),
                        pageViewState.scale
                    )
                }
            }
            delay(16)
        }
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
    var newTextSize by remember {
        mutableStateOf(ToolPrefs.sizeForTool(toolContext, "text", 10f))
    }
    LaunchedEffect(newTextSize) {
        ToolPrefs.saveSizeForTool(toolContext, "text", newTextSize)
    }
    var newTextBold by remember { mutableStateOf(false) }
    var newTextItalic by remember { mutableStateOf(false) }
    var newTextFont by remember { mutableStateOf(TextFonts.DEFAULT) }
    var newTextUnderline by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val undoStack = remember { mutableStateListOf<EditorUndo>() }
    val redoStack = remember { mutableStateListOf<EditorUndo>() }

    fun activeText(): TextElement? {
        val layer = activeLayer() ?: return null
        return layer.texts.firstOrNull { it.id == (editingTextId ?: selectedTextId) }
    }

    /**
     * Fill in the box geometry every text update derives from, so the
     * stored record always matches what the editor and the exporter draw.
     * Line height = this font/size's real metrics. Padding is kept as
     * stored (default 4pt, page units — never dp). The wrap width is
     * kept as stored when the user set one (corner-resize handle);
     * only a box with no stored width (old notes, or a freshly created
     * box) defaults to the page's right edge. normalizeText never
     * touches x or y: zoom, pan, rotation, tab switches, saving and
     * reloading must never write a text box's position — only an
     * explicit move/resize gesture (which commits once, at drag end)
     * or an explicit edit may change it.
     */
    fun normalizeText(element: TextElement): TextElement {
        val ratios = TextMetrics.ratios(
            element.fontFamilyName, element.bold, element.italic
        )
        val width = element.widthPt?.coerceIn(
            48f, (pageFormat.widthPt - element.x).coerceAtLeast(48f)
        ) ?: (pageFormat.widthPt - element.x).coerceAtLeast(48f)
        return element.copy(
            widthPt = width,
            lineHeightPt = element.fontSizeSp * ratios.lineHeight
        )
    }

    fun updateText(updated: TextElement) {
        val normalized = normalizeText(updated)
        for (layer in layersWork) {
            val i = layer.texts.indexOfFirst { it.id == normalized.id }
            if (i >= 0) {
                layer.texts[i] = normalized
                return
            }
        }
    }

    fun layerIdOfText(id: String): String? {
        for (layer in layersWork) {
            if (layer.texts.any { it.id == id }) return layer.id
        }
        return null
    }

    /**
     * What the canvas, the frame and the handles all draw for a box:
     * the live preview while it is being moved/resized, the stored
     * element otherwise. One function feeds every layer, so text,
     * frame and handles can never disagree mid-drag.
     */
    fun displayText(el: TextElement): TextElement {
        if (el.id == textDragId) {
            val preview = textDragPreview ?: return el
            return el.copy(x = preview.x, y = preview.y)
        }
        if (el.id == textResizeId) {
            val preview = textResizePreview ?: return el
            return el.copy(x = preview.first, widthPt = preview.second)
        }
        return el
    }

    /** True while this box is drawn from a drag preview. */
    fun isPreviewing(id: String): Boolean =
        textDragId == id || textResizeId == id

    /**
     * Snap a move preview to the nearest guide (page edges/center,
     * other boxes' edges/centers), then clamp so at least 24dp of
     * the box stays inside the page. Guides that survive the final
     * clamp are kept for drawing; a guide the clamp pushed off is
     * dropped, so a drawn guide is always exact.
     */
    fun snapAndClampPreview(
        raw: Offset,
        w: Float,
        h: Float,
        draggedId: String
    ): Offset {
        var snapped = raw
        var guideV: Float? = null
        var guideH: Float? = null
        if (snapToGuides) {
            val threshold = 6f * density / pageViewState.scale
            val others = layersWork.flatMap { layer ->
                if (!layer.visible) {
                    emptyList()
                } else {
                    layer.texts.filter { it.id != draggedId }.map {
                        BoxGeom(
                            it.x, it.y,
                            TextGeometry.wrapWidthPt(it, pageFormat.widthPt),
                            TextMetrics.boxHeightPt(it, pageFormat.widthPt)
                        )
                    }
                }
            }
            val (sx, vg) = snapAxisCoord(
                raw.x, w,
                verticalGuides(pageFormat.widthPt, others), threshold
            )
            val (sy, hg) = snapAxisCoord(
                raw.y, h,
                horizontalGuides(pageFormat.heightPt, others), threshold
            )
            snapped = Offset(sx, sy)
            guideV = vg
            guideH = hg
        }
        val visibleMin = 24f * density / pageViewState.scale
        val clamped = Offset(
            clampVisibleCoord(
                snapped.x, w, pageFormat.widthPt, visibleMin
            ),
            clampVisibleCoord(
                snapped.y, h, pageFormat.heightPt, visibleMin
            )
        )
        if (clamped.x != snapped.x) guideV = null
        if (clamped.y != snapped.y) guideH = null
        textDragGuidesV = guideV?.let { listOf(it) } ?: emptyList()
        textDragGuidesH = guideH?.let { listOf(it) } ?: emptyList()
        return clamped
    }

    /** Records one text-geometry undo step; a new action clears redo. */
    fun pushTextUndo(entry: EditorUndo.TextGeom) {
        undoStack.add(entry)
        redoStack.clear()
    }

    /** Writes a finished move drag: exactly one stored write. */
    fun commitTextDrag() {
        val id = textDragId ?: return
        val preview = textDragPreview ?: return
        val layerId = layerIdOfText(id) ?: return
        val el = layersWork.firstNotNullOfOrNull { layer ->
            layer.texts.firstOrNull { it.id == id }
        } ?: return
        updateText(el.copy(x = preview.x, y = preview.y))
        pushTextUndo(
            EditorUndo.TextGeom(
                layerId = layerId,
                textId = id,
                beforeX = textDragOrigin.x,
                beforeY = textDragOrigin.y,
                beforeWidth = el.widthPt,
                afterX = preview.x,
                afterY = preview.y,
                afterWidth = el.widthPt
            )
        )
    }

    /** Writes a finished resize drag: exactly one stored write. */
    fun commitTextResize() {
        val id = textResizeId ?: return
        val preview = textResizePreview ?: return
        val layerId = layerIdOfText(id) ?: return
        val el = layersWork.firstNotNullOfOrNull { layer ->
            layer.texts.firstOrNull { it.id == id }
        } ?: return
        updateText(el.copy(x = preview.first, widthPt = preview.second))
        pushTextUndo(
            EditorUndo.TextGeom(
                layerId = layerId,
                textId = id,
                beforeX = textResizeStartX,
                beforeY = el.y,
                beforeWidth = textResizeStartWidth,
                afterX = preview.first,
                afterY = el.y,
                afterWidth = preview.second
            )
        )
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
        // In Draw/Text this recolors the box being edited; in Select
        // it recolors the selected box (activeText covers both).
        activeText()?.let { updateText(it.copy(colorValue = c.value)) }
    }

    /** Restores the tool's last color and size from ToolPrefs. */
    fun loadToolSettings(t: Tool) {
        val key = toolKey(t, penType)
        inkColor = ToolPrefs.colorForTool(
            toolContext, key,
            if (t == Tool.HIGHLIGHTER) HighlighterPalette.first()
            else Color.Black
        )
        when (t) {
            Tool.TEXT -> newTextSize =
                ToolPrefs.sizeForTool(toolContext, key, 10f)
            Tool.ERASER -> eraserSize =
                ToolPrefs.sizeForTool(toolContext, key, 24f)
            else -> thickness =
                ToolPrefs.sizeForTool(toolContext, key, 3f)
        }
    }

    fun selectTool(newTool: Tool) {
        commitEditing()
        tool = newTool
        if (newTool != Tool.TEXT) selectedTextId = null
        loadToolSettings(newTool)
    }

    fun setMode(newMode: EditorMode) {
        commitEditing()
        mode = newMode
        if (newMode != EditorMode.SELECT) selectedStroke = null
        // A text selection belongs to the Select tool only: leaving
        // Select (Hand, or back to Draw) clears it, so the Draw tools
        // and their popover can never modify a box the user did not
        // just select.
        if (newMode != EditorMode.SELECT) selectedTextId = null
    }

    fun eraseAt(point: StrokePoint) {
        val layer = activeLayer() ?: return
        if (layer.locked) return
        layer.strokes.removeAll { stroke ->
            stroke.points.any { p ->
                hypot(
                    (p.x - point.x).toDouble(),
                    (p.y - point.y).toDouble()
                ) < eraserSize.toDouble() / pageViewState.scale.toDouble()
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
                val element = normalizeText(
                    TextElement(
                        id = System.nanoTime().toString(),
                        x = 48f,
                        y = 96f,
                        text = spoken,
                        colorValue = inkColor.value,
                        fontSizeSp = newTextSize,
                        bold = newTextBold,
                        italic = newTextItalic,
                        fontFamilyName = newTextFont,
                        underline = newTextUnderline
                    )
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
                            val raw = pageViewState.scale * zoom
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
                            val factor = banded / pageViewState.scale
                            pageViewState.offset = clampPinchOffset(
                                Offset(
                                    x = centroid.x -
                                        (centroid.x - pageViewState.offset.x) * factor +
                                        pan.x,
                                    y = centroid.y -
                                        (centroid.y - pageViewState.offset.y) * factor +
                                        pan.y
                                ),
                                banded
                            )
                            pageViewState.scale = banded
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
                        val target = pageViewState.scale.coerceIn(
                            fitScaleNow, fitScaleNow * 4f
                        )
                        if (target != pageViewState.scale) {
                            val f = target / pageViewState.scale
                            pageViewState.offset = clampPinchOffset(
                                Offset(
                                    x = prevCentroid.x -
                                        (prevCentroid.x - pageViewState.offset.x) * f,
                                    y = prevCentroid.y -
                                        (prevCentroid.y - pageViewState.offset.y) * f
                                ),
                                target
                            )
                            pageViewState.scale = target
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
                                                pageViewState.offset.x + v.x * dt,
                                                pageViewState.offset.y + v.y * dt
                                            )
                                            val clampedNow = clampPinchOffset(
                                                proposed, pageViewState.scale
                                            )
                                            pageViewState.offset = clampedNow
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
            // Placement and hit-testing go through the same M: a tap
            // stores screenToPage(tap) with the full transform, pan
            // included — never tapPoint / zoom.
            fun toPage(screen: Offset, pressure: Float = 1f): StrokePoint {
                val p = pageViewState.toPage(screen)
                return StrokePoint(x = p.x, y = p.y, pressure = pressure)
            }

            // Hit-test for the Draw/Text tap handler: is this page
            // point inside an existing visible text box? Used so a
            // tap on existing text with the Text tool creates nothing
            // — text is locked unless selected with the Select tool,
            // so tapping it in Draw must not start editing it, and
            // must not stack a new empty box on top of it.
            // Topmost visible text box containing a page point, with
            // its layer id (text is drawn over ink, so text wins a
            // tap that lands on both). Used by the SELECT tap handler
            // and the Draw/Text tap handler; both work in page coords
            // via toPage (full M, including pan), never in screen px.
            fun textAtPagePoint(p: StrokePoint): Pair<String, TextElement>? {
                for (layer in layersWork.asReversed()) {
                    if (!layer.visible) continue
                    for (el in layer.texts.asReversed()) {
                        val w = TextGeometry.wrapWidthPt(el, pageFormat.widthPt)
                        val h = TextMetrics.boxHeightPt(el, pageFormat.widthPt)
                        if (p.x in el.x..(el.x + w) && p.y in el.y..(el.y + h)) {
                            return layer.id to el
                        }
                    }
                }
                return null
            }

            val gestureModifier = when (mode) {
                EditorMode.HAND -> Modifier
                    .pointerInput(mode, fitScaleNow) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            // Zoom clamps to [fit, 400% of fit]; pan keeps
                            // at least 48dp of the page on screen.
                            val newScale = (pageViewState.scale * zoom)
                                .coerceIn(fitScaleNow, fitScaleNow * 4f)
                            val factor = newScale / pageViewState.scale
                            pageViewState.offset = clampViewOffset(
                                Offset(
                                    x = centroid.x - (centroid.x - pageViewState.offset.x) * factor + pan.x,
                                    y = centroid.y - (centroid.y - pageViewState.offset.y) * factor + pan.y
                                ),
                                newScale
                            )
                            pageViewState.scale = newScale
                        }
                    }
                    .pointerInput(mode, fitScaleNow) {
                        detectTapGestures(
                            onDoubleTap = { pos ->
                                if (pageViewState.scale > fitScaleNow * 1.05f) {
                                    resetViewToFit()
                                } else {
                                    val target = fitScaleNow * 2f
                                    val f = target / pageViewState.scale
                                    pageViewState.offset = clampViewOffset(
                                        Offset(
                                            pos.x - (pos.x - pageViewState.offset.x) * f,
                                            pos.y - (pos.y - pageViewState.offset.y) * f
                                        ),
                                        target
                                    )
                                    pageViewState.scale = target
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
                                // Text is drawn on the Canvas, so it is
                                // hit-tested here too, in page coords:
                                // a tap on canvas text selects it (text
                                // is drawn over ink, so it wins over a
                                // stroke underneath). Locked-layer text
                                // is not selectable and falls through
                                // to the stroke search below.
                                val textHit = textAtPagePoint(pagePoint)
                                if (textHit != null) {
                                    val hitLayer = layersWork.firstOrNull {
                                        it.id == textHit.first
                                    }
                                    if (hitLayer != null && !hitLayer.locked) {
                                        selectedTextId = textHit.second.id
                                        activeLayerId = textHit.first
                                        selectedStroke = null
                                        return@detectTapGestures
                                    }
                                }
                                var found: Pair<String, Int>? = null
                                for (layer in layersWork.asReversed()) {
                                    if (!layer.visible) continue
                                    for (i in layer.strokes.indices.reversed()) {
                                        val s = layer.strokes[i]
                                        val hit = s.points.any { p ->
                                            hypot(
                                                (p.x - pagePoint.x).toDouble(),
                                                (p.y - pagePoint.y).toDouble()
                                            ) < 28.0 / pageViewState.scale
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
                                            val dx = dragAmount.x / pageViewState.scale
                                            val dy = dragAmount.y / pageViewState.scale
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
                                                // Tapping existing text with the
                                                // Draw/Text tool does nothing:
                                                // text is locked unless selected
                                                // with the Select tool, so this
                                                // must neither edit it nor stack
                                                // a new box on top of it.
                                                if (textAtPagePoint(point) != null) {
                                                    return@detectTapGestures
                                                }
                                                if (point.x in 0f..pageFormat.widthPt && point.y in 0f..pageFormat.heightPt) {
                                                val element = normalizeText(
                                                    TextElement(
                                                        id = System.nanoTime().toString(),
                                                        x = point.x,
                                                        y = point.y,
                                                        text = "",
                                                        colorValue = inkColor.value,
                                                        fontSizeSp = newTextSize,
                                                        bold = newTextBold,
                                                        italic = newTextItalic,
                                                        fontFamilyName = newTextFont,
                                                        underline = newTextUnderline
                                                    )
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
                            // Deliberately no pinch handler here (v0.14):
                            // the old "pinch anywhere zooms the selected
                            // text" detector wrote a text box's font size
                            // on every pinch event, so a page zoom wrote
                            // to stored text data. Pinch now only zooms
                            // the page (page interceptor / Hand detector);
                            // text is resized only by an explicit corner
                            // handle drag or the size controls, on a
                            // selected box.
                        }
                    }
                    else -> Modifier.pointerInput(tool, inkColor, thickness, penType, pageFormat, pressureEnabled) {
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
                                        if (pressureEnabled) {
                                            change.pressure.coerceIn(0.1f, 1f)
                                        } else {
                                            1f
                                        }
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
                                    val finishedStroke = NoteStroke(
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
                                    layer.strokes.add(finishedStroke)
                                    // A new stroke is an undoable action
                                    // too, and it clears redo like any
                                    // other action after an undo.
                                    undoStack.add(
                                        EditorUndo.Stroke(
                                            layer.id, finishedStroke
                                        )
                                    )
                                    redoStack.clear()
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
                    pageFormat.widthPt * pageViewState.scale,
                    pageFormat.heightPt * pageViewState.scale
                )
                drawRoundRect(
                    color = Color(0x22000000),
                    topLeft = Offset(pageViewState.offset.x, pageViewState.offset.y + 6f),
                    size = sheetSize,
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRect(
                    color = Color.White,
                    topLeft = pageViewState.offset,
                    size = sheetSize
                )
                // The part of the page currently on screen, in page
                // coordinates: the pattern generator draws only inside it.
                val visiblePageRect = androidx.compose.ui.geometry.Rect(
                    left = maxOf(0f, -pageViewState.offset.x / pageViewState.scale),
                    top = maxOf(0f, -pageViewState.offset.y / pageViewState.scale),
                    right = minOf(
                        pageFormat.widthPt,
                        (size.width - pageViewState.offset.x) / pageViewState.scale
                    ),
                    bottom = minOf(
                        pageFormat.heightPt,
                        (size.height - pageViewState.offset.y) / pageViewState.scale
                    )
                )
                withTransform({
                    translate(pageViewState.offset.x, pageViewState.offset.y)
                    scale(pageViewState.scale, pageViewState.scale, Offset.Zero)
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
                    // Text, on this same canvas, inside the same
                    // transform and page clip as template and ink
                    // (v0.14.1): screen = M(page) for every glyph, so
                    // text can never sit anywhere but on the page, at
                    // its stored page position, over its highlight.
                    // The box being edited is skipped here — it is
                    // drawn once, by its BasicTextField below, so it
                    // is never double-drawn with two renderers.
                    for (layer in layersWork) {
                        if (!layer.visible) continue
                        for (element in layer.texts) {
                            if (element.id != editingTextId) {
                                // The dragged/resized box draws from
                                // its preview (same frame as the
                                // frame and handles below), at 90%
                                // opacity while lifted.
                                val disp = displayText(element)
                                if (isPreviewing(disp.id)) {
                                    val base = Color(disp.colorValue)
                                    val faded = base.copy(
                                        alpha = base.alpha * 0.9f
                                    )
                                    drawPageText(
                                        textMeasurer,
                                        disp.copy(
                                            colorValue = faded.value
                                        ),
                                        pageFormat.widthPt, scaledDensity
                                    )
                                } else {
                                    drawPageText(
                                        textMeasurer, disp,
                                        pageFormat.widthPt, scaledDensity
                                    )
                                }
                            }
                        }
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
                                    style = Stroke(width = 2f / pageViewState.scale)
                                )
                            }
                        }
                    }
                    // Selection frame around the selected text box
                    // (Select mode, not editing): drawn in page coords
                    // on this same canvas, so it sits exactly on the
                    // canvas text. The move/resize handles are the
                    // composables below, positioned from the same M.
                    if (mode == EditorMode.SELECT && selectedTextId != null &&
                        editingTextId == null
                    ) {
                        for (layer in layersWork) {
                            if (layer.locked) continue
                            val el = layer.texts.firstOrNull {
                                it.id == selectedTextId
                            } ?: continue
                            val disp = displayText(el)
                            val w = TextGeometry.wrapWidthPt(
                                disp, pageFormat.widthPt
                            )
                            val h = TextMetrics.boxHeightPt(
                                disp, pageFormat.widthPt
                            )
                            if (isPreviewing(disp.id)) {
                                // Ghost of the original place (20%),
                                // soft shadow under the lifted box,
                                // and a teal dashed outline at the
                                // preview — all in page coords, so
                                // they track the page like the text.
                                val origW = TextGeometry.wrapWidthPt(
                                    el, pageFormat.widthPt
                                )
                                val origH = TextMetrics.boxHeightPt(
                                    el, pageFormat.widthPt
                                )
                                drawRect(
                                    color = DesignTokens.Accent.copy(
                                        alpha = 0.2f
                                    ),
                                    topLeft = Offset(el.x, el.y),
                                    size = androidx.compose.ui.geometry.Size(
                                        origW, origH
                                    ),
                                    style = Stroke(
                                        width = 1f / pageViewState.scale
                                    )
                                )
                                drawRoundRect(
                                    color = Color.Black.copy(alpha = 0.15f),
                                    topLeft = Offset(
                                        disp.x,
                                        disp.y + 2f * density /
                                            pageViewState.scale
                                    ),
                                    size = androidx.compose.ui.geometry.Size(
                                        w, h
                                    ),
                                    cornerRadius = CornerRadius(
                                        4f / pageViewState.scale
                                    )
                                )
                                drawRoundRect(
                                    color = DesignTokens.Accent,
                                    topLeft = Offset(disp.x, disp.y),
                                    size = androidx.compose.ui.geometry.Size(
                                        w, h
                                    ),
                                    cornerRadius = CornerRadius(
                                        4f / pageViewState.scale
                                    ),
                                    style = Stroke(
                                        width = 2f / pageViewState.scale,
                                        pathEffect = PathEffect
                                            .dashPathEffect(
                                                floatArrayOf(
                                                    8f / pageViewState.scale,
                                                    6f / pageViewState.scale
                                                ),
                                                0f
                                            )
                                    )
                                )
                                // Alignment guides currently snapped
                                // to: thin teal lines across the page.
                                for (gx in textDragGuidesV) {
                                    drawLine(
                                        color = DesignTokens.Accent,
                                        start = Offset(gx, 0f),
                                        end = Offset(
                                            gx, pageFormat.heightPt
                                        ),
                                        strokeWidth =
                                            1f / pageViewState.scale
                                    )
                                }
                                for (gy in textDragGuidesH) {
                                    drawLine(
                                        color = DesignTokens.Accent,
                                        start = Offset(0f, gy),
                                        end = Offset(
                                            pageFormat.widthPt, gy
                                        ),
                                        strokeWidth =
                                            1f / pageViewState.scale
                                    )
                                }
                            } else {
                                drawRect(
                                    color = selectionColor,
                                    topLeft = Offset(disp.x, disp.y),
                                    size = androidx.compose.ui.geometry.Size(
                                        w, h
                                    ),
                                    style = Stroke(
                                        width = 2f / pageViewState.scale
                                    )
                                )
                            }
                            break
                        }
                    }
                    } finally {
                        drawContext.canvas.restore()
                    }
                }
            }

            // Brush ring (v0.15.0): while the size slider drags (and
            // 500ms after release), a ring at the center of the canvas
            // shows the real on-screen brush size at current zoom:
            // diameter = size (pt) x page scale (px per pt). It has
            // no input handling, so it never blocks drawing.
            if (brushRingVisible) {
                val ringSizePt = when (tool) {
                    Tool.ERASER -> eraserSize
                    Tool.TEXT -> activeText()?.fontSizeSp ?: newTextSize
                    else -> thickness
                }
                val ringDiameter = with(LocalDensity.current) {
                    (ringSizePt * pageViewState.scale).toDp()
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(ringDiameter.coerceAtLeast(8.dp))
                        .clip(CircleShape)
                        .border(2.dp, DesignTokens.Accent, CircleShape)
                )
            }

            // Interaction overlay (v0.14.1): this section draws NO
            // text. Display text lives on the Canvas above, inside M.
            // What lives here is interaction only, and every position
            // is computed from the same PageViewState M, in screen px,
            // on every composition — so a pinch frame that moves the
            // page moves the editing field and the handles in the
            // same frame, with no stale offset and no dp layout box
            // in between:
            //  - the active editing field (the ONLY native view used
            //    for text), at M(left, top), width = wrapWidth x scale;
            //  - the Select move/resize handles for the selected box,
            //    at M(box corners / top-center), fixed dp size so they
            //    stay tappable at any zoom.
            // Selection itself is hit-tested on the Canvas (the SELECT
            // tap handler, via textAtPagePoint), not by a view.
            val editingElement: TextElement? = editingTextId?.let { id ->
                layersWork.firstNotNullOfOrNull { layer ->
                    layer.texts.firstOrNull { it.id == id }
                }
            }
            val selectedElement: TextElement? =
                if (mode == EditorMode.SELECT && editingTextId == null) {
                    selectedTextId?.let { id ->
                        layersWork.firstNotNullOfOrNull { layer ->
                            if (layer.locked) null
                            else layer.texts.firstOrNull { it.id == id }
                        }
                    }
                } else {
                    null
                }
            if (editingElement != null) {
                val element = editingElement
                // M applied to (left, top), recomputed every frame:
                // while editing, a pinch re-positions this field from
                // the same M the Canvas text uses, so the caret never
                // sits anywhere but on the box it edits.
                val editScreen = pageViewState.toScreen(
                    Offset(element.x, element.y)
                )
                val editPaddingDp =
                    (element.paddingPt * pageViewState.scale / density).dp
                BasicTextField(
                    value = element.text,
                    onValueChange = { newValue ->
                        updateText(element.copy(text = newValue))
                    },
                    textStyle = textStyleForPage(
                        element, pageViewState.scale, scaledDensity
                    ),
                    cursorBrush = SolidColor(Color(0xFF1565C0)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { commitEditing() }
                    ),
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                editScreen.x.roundToInt(),
                                editScreen.y.roundToInt()
                            )
                        }
                        .requiredWidth(
                            (TextGeometry.wrapWidthPt(
                                element, pageFormat.widthPt
                            ) * pageViewState.scale / density).dp
                        )
                        .focusRequester(focusRequester)
                        .background(Color(0x143D5AFE), RoundedCornerShape(4.dp))
                        .defaultMinSize(minWidth = 140.dp, minHeight = 40.dp)
                        .padding(editPaddingDp)
                )
            }
            if (selectedElement != null) {
                // Every position below is the DISPLAY geometry
                // (preview while dragging, stored otherwise), from
                // the same M — so the frame, the text and the
                // handles move as one, in the same frame.
                val element = displayText(selectedElement)
                val primary = MaterialTheme.colorScheme.primary
                val topLeft = pageViewState.toScreen(
                    Offset(element.x, element.y)
                )
                val boxWpx = TextGeometry.wrapWidthPt(
                    element, pageFormat.widthPt
                ) * pageViewState.scale
                val boxHpx = TextMetrics.boxHeightPt(
                    element, pageFormat.widthPt
                ) * pageViewState.scale
                // Move handle: top-center of the box, from M.
                // Live drag (v0.15.4): an 8dp slop tells a tap from
                // a drag; the grab is taken when the slop is passed,
                // so the box does not move at drag start; every move
                // event previews touchPage - grab through M, and the
                // stored position is written exactly once, on lift.
                val moveHandleOriginState = rememberUpdatedState(
                    Offset(
                        topLeft.x + boxWpx / 2f - 14f * density,
                        topLeft.y - 7f * density
                    )
                )
                Box(
                    Modifier
                        .offset {
                            IntOffset(
                                (topLeft.x + boxWpx / 2f - 14f * density)
                                    .roundToInt(),
                                (topLeft.y - 7f * density).roundToInt()
                            )
                        }
                        .size(28.dp)
                        .background(primary, CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                        .pointerInput(element.id) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val slopPx = 8f * density
                                var slopTouch: Offset? = null
                                while (slopTouch == null) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes
                                        .firstOrNull { it.id == down.id }
                                        ?: continue
                                    if (!change.pressed) break
                                    change.consume()
                                    if ((change.position - down.position)
                                            .getDistance() >= slopPx
                                    ) {
                                        slopTouch = change.position
                                    }
                                }
                                // Lift before the slop: a tap, the
                                // selection is unchanged, no drag.
                                if (slopTouch == null) {
                                    return@awaitEachGesture
                                }
                                val origin = Offset(
                                    element.x, element.y
                                )
                                val touchPage = pageViewState.toPage(
                                    moveHandleOriginState.value + slopTouch
                                )
                                textDragId = element.id
                                textDragOrigin = origin
                                textDragGrab = touchPage - origin
                                textDragPreview = origin
                                textDragTouch =
                                    moveHandleOriginState.value + slopTouch
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes
                                            .firstOrNull {
                                                it.id == down.id
                                            } ?: continue
                                        if (!change.pressed) {
                                            // Lift: commit only if a
                                            // pinch or tool change did
                                            // not already cancel this
                                            // drag (cancel clears the
                                            // id, so this is skipped).
                                            if (textDragId == element.id &&
                                                !suppressDrawing
                                            ) {
                                                commitTextDrag()
                                            }
                                            break
                                        }
                                        change.consume()
                                        if (suppressDrawing) continue
                                        val handleOrigin =
                                            moveHandleOriginState.value
                                        textDragTouch =
                                            handleOrigin + change.position
                                        val page = pageViewState.toPage(
                                            handleOrigin + change.position
                                        )
                                        val w = TextGeometry.wrapWidthPt(
                                            element, pageFormat.widthPt
                                        )
                                        val h = TextMetrics.boxHeightPt(
                                            element, pageFormat.widthPt
                                        )
                                        val raw = Offset(
                                            dragPreviewCoord(
                                                page.x, textDragGrab.x
                                            ),
                                            dragPreviewCoord(
                                                page.y, textDragGrab.y
                                            )
                                        )
                                        textDragPreview =
                                            snapAndClampPreview(
                                                raw, w, h, element.id
                                            )
                                    }
                                } finally {
                                    // System cancel: drop the preview,
                                    // never write the stored position.
                                    clearTextDragState()
                                }
                            }
                        }
                        .clickable {}
                )
                // Corner resize handles: at M(box corners), fixed
                // size. Same live-preview, no-jump rule as the move
                // handle, with the opposite corner as the anchor.
                listOf(
                    Triple(0f, 0f, true),
                    Triple(boxWpx, 0f, false),
                    Triple(0f, boxHpx, true),
                    Triple(boxWpx, boxHpx, false)
                ).forEach { (dxPx, dyPx, left) ->
                    val cornerOriginState = rememberUpdatedState(
                        Offset(
                            topLeft.x + dxPx - 12f * density,
                            topLeft.y + dyPx - 12f * density
                        )
                    )
                    Box(
                        Modifier
                            .offset {
                                IntOffset(
                                    (topLeft.x + dxPx - 12f * density)
                                        .roundToInt(),
                                    (topLeft.y + dyPx - 12f * density)
                                        .roundToInt()
                                )
                            }
                            .size(24.dp)
                            .background(primary, CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                            .pointerInput(element.id, left) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    val slopPx = 8f * density
                                    var slopTouch: Offset? = null
                                    while (slopTouch == null) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes
                                            .firstOrNull {
                                                it.id == down.id
                                            } ?: continue
                                        if (!change.pressed) break
                                        change.consume()
                                        if ((change.position - down.position)
                                                .getDistance() >= slopPx
                                        ) {
                                            slopTouch = change.position
                                        }
                                    }
                                    if (slopTouch == null) {
                                        return@awaitEachGesture
                                    }
                                    val startX = element.x
                                    val startWidth =
                                        TextGeometry.wrapWidthPt(
                                            element, pageFormat.widthPt
                                        )
                                    val touchPageX = pageViewState.toPage(
                                        cornerOriginState.value + slopTouch
                                    ).x
                                    val draggedEdge = if (left) {
                                        startX
                                    } else {
                                        startX + startWidth
                                    }
                                    textResizeId = element.id
                                    textResizeStartX = startX
                                    textResizeStartWidth = startWidth
                                    textResizeLeftCorner = left
                                    textResizeGrabDx =
                                        touchPageX - draggedEdge
                                    textResizePreview =
                                        startX to startWidth
                                    textDragTouch =
                                        cornerOriginState.value + slopTouch
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes
                                                .firstOrNull {
                                                    it.id == down.id
                                                } ?: continue
                                            if (!change.pressed) {
                                                if (textResizeId ==
                                                    element.id &&
                                                    !suppressDrawing
                                                ) {
                                                    commitTextResize()
                                                }
                                                break
                                            }
                                            change.consume()
                                            if (suppressDrawing) continue
                                            val cornerOrigin =
                                                cornerOriginState.value
                                            textDragTouch =
                                                cornerOrigin +
                                                    change.position
                                            val pageX = pageViewState.toPage(
                                                cornerOrigin +
                                                    change.position
                                            ).x
                                            val edge = pageX -
                                                textResizeGrabDx
                                            textResizePreview = if (left) {
                                                resizeLeftPreview(
                                                    startX, startWidth,
                                                    edge,
                                                    pageFormat.widthPt
                                                )
                                            } else {
                                                startX to resizeRightPreview(
                                                    startX, startWidth,
                                                    edge,
                                                    pageFormat.widthPt
                                                )
                                            }
                                        }
                                    } finally {
                                        clearTextDragState()
                                    }
                                }
                            }
                            .clickable {}
                    )
                }
                // Position label while dragging (page units, 1dp).
                // Non-interactive: it never consumes touches.
                val dragLabel: String? =
                    if (textDragId == element.id &&
                        textDragPreview != null
                    ) {
                        val p = textDragPreview!!
                        "%.1f, %.1f pt".format(
                            Locale.US, p.x, p.y
                        )
                    } else if (textResizeId == element.id &&
                        textResizePreview != null
                    ) {
                        "%.1f pt".format(
                            Locale.US, textResizePreview!!.second
                        )
                    } else {
                        null
                    }
                if (dragLabel != null) {
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    topLeft.x.roundToInt(),
                                    (topLeft.y - 30f * density)
                                        .roundToInt()
                                )
                            }
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(
                                horizontal = 8.dp, vertical = 4.dp
                            )
                    ) {
                        Text(
                            text = dragLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
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
                            text = "${(pageViewState.scale / fitScaleNow * 100).roundToInt()}%",
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
                // Every draw tool has sheet content now (eraser
                // gets its size control), as does Select mode.
                val popoverHasContent = mode == EditorMode.SELECT ||
                    mode == EditorMode.DRAW
                // Outside-tap scrim: while the sheet is open, a tap
                // anywhere but the bottom column dismisses it. The
                // column itself is composed after this, so the sheet
                // and the toolbar pill stay tappable above it.
                if (popoverOpen && !toolsCollapsed && popoverHasContent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { popoverOpen = false }
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
                        showPenTypes = mode == EditorMode.DRAW &&
                            tool == Tool.PEN,
                        penType = penType,
                        onPenTypeChange = { newType ->
                            penType = newType
                            thickness = ToolPrefs.sizeForTool(
                                toolContext, "pen_$newType", 3f
                            )
                            inkColor = ToolPrefs.colorForTool(
                                toolContext, "pen_$newType", Color.Black
                            )
                        },
                        showSize = mode == EditorMode.DRAW,
                        size = when (tool) {
                            Tool.TEXT -> activeText()?.fontSizeSp
                                ?: newTextSize
                            Tool.ERASER -> eraserSize
                            else -> thickness
                        },
                        sizeRange = when (tool) {
                            Tool.TEXT -> 5f..58f
                            else -> sizeRangeFor(tool, penType)
                        },
                        presets = presetsForTool(tool, penType),
                        sizeUnit = "pt",
                        previewKind = when (tool) {
                            Tool.HIGHLIGHTER -> PreviewKind.HIGHLIGHTER
                            Tool.TEXT -> PreviewKind.TEXT
                            else -> PreviewKind.STROKE
                        },
                        onSizeChange = { v ->
                            when (tool) {
                                Tool.TEXT -> {
                                    val target = activeText()
                                    if (target != null) {
                                        updateText(
                                            target.copy(fontSizeSp = v)
                                        )
                                    } else {
                                        newTextSize = v
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
                            editorScope.launch {
                                delay(500)
                                if (session == brushRingSession) {
                                    brushRingVisible = false
                                }
                            }
                        },
                        showColorRow = mode == EditorMode.DRAW &&
                            tool != Tool.ERASER,
                        inkColor = inkColor,
                        onColorLive = { applyColor(it) },
                        onColorCommit = { c ->
                            applyColor(c)
                            ToolPrefs.pushRecent(toolContext, c)
                            recentColors =
                                ToolPrefs.recentColors(toolContext)
                        },
                        recentColors = recentColors,
                        customColors = customColors,
                        onSaveCustom = { c ->
                            ToolPrefs.addCustom(toolContext, c)
                            customColors =
                                ToolPrefs.customColors(toolContext)
                        },
                        onDeleteCustom = { c ->
                            ToolPrefs.removeCustom(toolContext, c)
                            customColors =
                                ToolPrefs.customColors(toolContext)
                        },
                        selectColorMode = mode == EditorMode.SELECT &&
                            selectedTextId != null,
                        showPressureToggle = mode == EditorMode.DRAW,
                        pressureEnabled = pressureEnabled,
                        onPressureChange = { pressureEnabled = it },
                        extraTopContent = {
                            // Rich text formatting bar (unchanged).
                            if (mode == EditorMode.DRAW &&
                                tool == Tool.TEXT
                            ) {
                                val target = activeText()
                                val boldActive = target?.bold ?: newTextBold
                                val italicActive = target?.italic
                                    ?: newTextItalic
                                val underlineActive = target?.underline
                                    ?: newTextUnderline
                                val fontActive = target?.fontFamilyName
                                    ?: newTextFont
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(
                                            rememberScrollState()
                                        )
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (target != null) {
                                                updateText(
                                                    target.copy(
                                                        bold = !target.bold
                                                    )
                                                )
                                            } else {
                                                newTextBold = !newTextBold
                                            }
                                        },
                                        modifier = Modifier.background(
                                            if (boldActive) {
                                                DesignTokens.AccentTint
                                            } else {
                                                Color.Transparent
                                            },
                                            CircleShape
                                        )
                                    ) {
                                        Icon(
                                            Icons.Filled.FormatBold,
                                            contentDescription = "Bold"
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (target != null) {
                                                updateText(
                                                    target.copy(
                                                        italic = !target.italic
                                                    )
                                                )
                                            } else {
                                                newTextItalic = !newTextItalic
                                            }
                                        },
                                        modifier = Modifier.background(
                                            if (italicActive) {
                                                DesignTokens.AccentTint
                                            } else {
                                                Color.Transparent
                                            },
                                            CircleShape
                                        )
                                    ) {
                                        Icon(
                                            Icons.Filled.FormatItalic,
                                            contentDescription = "Italic"
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (target != null) {
                                                updateText(
                                                    target.copy(
                                                        underline =
                                                            !target.underline
                                                    )
                                                )
                                            } else {
                                                newTextUnderline =
                                                    !newTextUnderline
                                            }
                                        },
                                        modifier = Modifier.background(
                                            if (underlineActive) {
                                                DesignTokens.AccentTint
                                            } else {
                                                Color.Transparent
                                            },
                                            CircleShape
                                        )
                                    ) {
                                        Icon(
                                            Icons.Filled.FormatUnderlined,
                                            contentDescription = "Underline"
                                        )
                                    }
                                    Spacer(
                                        modifier = Modifier.width(8.dp)
                                    )
                                    TextFonts.ALL.forEach { fontName ->
                                        TextButton(onClick = {
                                            if (target != null) {
                                                updateText(
                                                    target.copy(
                                                        fontFamilyName =
                                                            fontName
                                                    )
                                                )
                                            } else {
                                                newTextFont = fontName
                                            }
                                        }) {
                                            Text(
                                                text = fontLabel(fontName),
                                                fontFamily = fontFamilyFor(
                                                    fontName
                                                ),
                                                fontWeight = if (fontName ==
                                                    fontActive
                                                ) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Normal
                                                },
                                                color = if (fontName ==
                                                    fontActive
                                                ) {
                                                    MaterialTheme.colorScheme
                                                        .primary
                                                } else {
                                                    MaterialTheme.colorScheme
                                                        .onSurface
                                                }
                                            )
                                        }
                                    }
                                    if (selectedTextId != null &&
                                        editingTextId == null
                                    ) {
                                        Spacer(
                                            modifier = Modifier.width(8.dp)
                                        )
                                        IconButton(onClick = {
                                            for (layer in layersWork) {
                                                layer.texts.removeAll {
                                                    it.id == selectedTextId
                                                }
                                            }
                                            selectedTextId = null
                                        }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription =
                                                    "Delete selected text"
                                            )
                                        }
                                    }
                                }
                                if (!compact) {
                                    Text(
                                        text = "Tap the page to type. Existing text is locked — switch to Select to move, resize or edit it. Pinch zooms the page, never the text.",
                                        style = MaterialTheme.typography
                                            .bodySmall,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp
                                        )
                                    )
                                }
                            }
                            // Select mode actions (unchanged).
                            if (mode == EditorMode.SELECT) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 8.dp,
                                            vertical = 2.dp
                                        ),
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tap ink or text to select it. Drag a selected text box by its top handle, resize with a corner handle; drag selected ink to move it.",
                                        style = MaterialTheme.typography
                                            .bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { deleteSelected() },
                                        modifier = Modifier.size(btnSize),
                                        enabled = selectedStroke != null ||
                                            selectedTextId != null
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription =
                                                "Delete selection",
                                            modifier = Modifier.size(iconSize)
                                        )
                                    }
                                }
                                // Nudge pad (v0.15.4): 1 pt per tap,
                                // long-press repeats. One press is one
                                // undo step, recorded on release. Only
                                // for text on an unlocked layer (a
                                // locked layer's text cannot be
                                // selected, so this is just a guard).
                                val nudgeLayer = selectedTextId?.let { id ->
                                    layersWork.firstOrNull { layer ->
                                        !layer.locked && layer.texts.any {
                                            it.id == id
                                        }
                                    }
                                }
                                if (nudgeLayer != null) {
                                    fun nudgeBy(dx: Float, dy: Float) {
                                        val id = selectedTextId ?: return
                                        val el = layersWork
                                            .firstNotNullOfOrNull { layer ->
                                                layer.texts.firstOrNull {
                                                    it.id == id
                                                }
                                            } ?: return
                                        updateText(
                                            el.copy(
                                                x = el.x + dx,
                                                y = el.y + dy
                                            )
                                        )
                                    }
                                    fun nudgeDown() {
                                        val id = selectedTextId ?: return
                                        val el = layersWork
                                            .firstNotNullOfOrNull { layer ->
                                                layer.texts.firstOrNull {
                                                    it.id == id
                                                }
                                            } ?: return
                                        nudgeBurst = Triple(id, el.x, el.y)
                                    }
                                    fun nudgeUp() {
                                        val burst = nudgeBurst
                                        nudgeBurst = null
                                        if (burst == null) return
                                        val el = layersWork
                                            .firstNotNullOfOrNull { layer ->
                                                layer.texts.firstOrNull {
                                                    it.id == burst.first
                                                }
                                            } ?: return
                                        if (el.x != burst.second ||
                                            el.y != burst.third
                                        ) {
                                            val layerId =
                                                layerIdOfText(burst.first)
                                                    ?: return
                                            pushTextUndo(
                                                EditorUndo.TextGeom(
                                                    layerId = layerId,
                                                    textId = burst.first,
                                                    beforeX = burst.second,
                                                    beforeY = burst.third,
                                                    beforeWidth = el.widthPt,
                                                    afterX = el.x,
                                                    afterY = el.y,
                                                    afterWidth = el.widthPt
                                                )
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 8.dp,
                                                vertical = 2.dp
                                            ),
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Nudge (1 pt)",
                                            style = MaterialTheme.typography
                                                .bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        NudgeButton(
                                            icon = Icons.Filled
                                                .KeyboardArrowLeft,
                                            description = "Nudge left",
                                            onDown = { nudgeDown() },
                                            onStep = { nudgeBy(-1f, 0f) },
                                            onUp = { nudgeUp() }
                                        )
                                        NudgeButton(
                                            icon = Icons.Filled
                                                .KeyboardArrowUp,
                                            description = "Nudge up",
                                            onDown = { nudgeDown() },
                                            onStep = { nudgeBy(0f, -1f) },
                                            onUp = { nudgeUp() }
                                        )
                                        NudgeButton(
                                            icon = Icons.Filled
                                                .KeyboardArrowDown,
                                            description = "Nudge down",
                                            onDown = { nudgeDown() },
                                            onStep = { nudgeBy(0f, 1f) },
                                            onUp = { nudgeUp() }
                                        )
                                        NudgeButton(
                                            icon = Icons.Filled
                                                .KeyboardArrowRight,
                                            description = "Nudge right",
                                            onDown = { nudgeDown() },
                                            onStep = { nudgeBy(1f, 0f) },
                                            onUp = { nudgeUp() }
                                        )
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
                            mode = mode,
                            onModeTap = { m ->
                                if (m == mode) {
                                    if (m != EditorMode.HAND) {
                                        if (!popoverOpen) {
                                            // Select with a text box
                                            // selected opens straight
                                            // on its colors; else MAIN.
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
                                    setMode(m)
                                    popoverOpen = false
                                }
                            },
                            tool = tool,
                            onToolTap = { t ->
                                if (mode == EditorMode.DRAW && t == tool) {
                                    if (!popoverOpen) {
                                        sheetView = ToolSheetView.MAIN
                                        sheetEntryView = ToolSheetView.MAIN
                                    }
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
                                // The color button always shows the
                                // swatch grid: opening goes to COLOR,
                                // and tapping it while open switches
                                // there instead of closing.
                                sheetView = ToolSheetView.COLOR
                                if (!popoverOpen) {
                                    sheetEntryView = ToolSheetView.COLOR
                                }
                                popoverOpen = true
                            },
                            onUndo = {
                                // One step, most recent action first:
                                // a stroke comes off its layer, a text
                                // move/resize returns to its previous
                                // geometry. A locked or missing layer
                                // drops its entry instead of editing
                                // through the lock.
                                val entry = undoStack.removeLastOrNull()
                                    ?: return@FloatingToolbar
                                when (entry) {
                                    is EditorUndo.Stroke -> {
                                        val layer = layersWork.firstOrNull {
                                            it.id == entry.layerId
                                        }
                                        if (layer != null && !layer.locked &&
                                            layer.strokes.contains(entry.stroke)
                                        ) {
                                            layer.strokes.remove(entry.stroke)
                                            redoStack.add(entry)
                                        }
                                    }
                                    is EditorUndo.TextGeom -> {
                                        val layer = layersWork.firstOrNull {
                                            it.id == entry.layerId
                                        }
                                        val el = layer?.texts?.firstOrNull {
                                            it.id == entry.textId
                                        }
                                        if (layer != null && !layer.locked &&
                                            el != null
                                        ) {
                                            updateText(
                                                el.copy(
                                                    x = entry.beforeX,
                                                    y = entry.beforeY,
                                                    widthPt = entry.beforeWidth
                                                )
                                            )
                                            redoStack.add(entry)
                                        }
                                    }
                                }
                            },
                            onRedo = {
                                val entry = redoStack.removeLastOrNull()
                                    ?: return@FloatingToolbar
                                when (entry) {
                                    is EditorUndo.Stroke -> {
                                        val layer = layersWork.firstOrNull {
                                            it.id == entry.layerId
                                        }
                                        if (layer != null && !layer.locked) {
                                            layer.strokes.add(entry.stroke)
                                            undoStack.add(entry)
                                        }
                                    }
                                    is EditorUndo.TextGeom -> {
                                        val layer = layersWork.firstOrNull {
                                            it.id == entry.layerId
                                        }
                                        val el = layer?.texts?.firstOrNull {
                                            it.id == entry.textId
                                        }
                                        if (layer != null && !layer.locked &&
                                            el != null
                                        ) {
                                            updateText(
                                                el.copy(
                                                    x = entry.afterX,
                                                    y = entry.afterY,
                                                    widthPt = entry.afterWidth
                                                )
                                            )
                                            undoStack.add(entry)
                                        }
                                    }
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
                // Page navigation pill: bottom right, fades when idle,
                // and hidden entirely while the sheet is open so
                // nothing overlaps it.
                if (!(popoverOpen && !toolsCollapsed)) Box(
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

/**
 * A button that fires once per tap and repeats while held (400ms
 * delay, then every 90ms). onDown/onUp bracket one press, so the
 * caller can record one undo step per burst. Used by the Select
 * nudge pad (v0.15.4).
 */
@Composable
private fun NudgeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onDown: () -> Unit,
    onStep: () -> Unit,
    onUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(DesignTokens.AccentTint)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    try {
                        onDown()
                        onStep()
                        var nextRepeat = System.currentTimeMillis() + 400
                        while (true) {
                            val timeout = (nextRepeat -
                                System.currentTimeMillis()
                                ).coerceAtLeast(0)
                            val event = withTimeoutOrNull(timeout) {
                                awaitPointerEvent()
                            }
                            if (event == null) {
                                onStep()
                                nextRepeat =
                                    System.currentTimeMillis() + 90
                                continue
                            }
                            val change = event.changes.firstOrNull {
                                it.id == down.id
                            } ?: continue
                            if (!change.pressed) break
                            change.consume()
                        }
                    } finally {
                        onUp()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = DesignTokens.Accent
        )
    }
}
