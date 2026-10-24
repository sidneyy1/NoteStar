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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.riguel.notestar.data.PageTemplate
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
    onEnterPip: () -> Unit
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

    // One-time initial load
    var initialized by remember { mutableStateOf(false) }
    if (!initialized) {
        loadWork(initialDoc.pages.first())
        initialized = true
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
        viewScale = 1f
        viewOffset = Offset.Zero
    }

    var mode by remember { mutableStateOf(EditorMode.DRAW) }
    var tool by remember { mutableStateOf(Tool.PEN) }
    var penType by remember { mutableStateOf(PenTypes.PEN) }
    var inkColor by remember { mutableStateOf(Color.Black) }
    var thickness by remember { mutableStateOf(6f) }
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    var beautifyOn by remember { mutableStateOf(false) }
    var chromeVisible by remember { mutableStateOf(true) }
    var paletteVisible by remember { mutableStateOf(!compact) }
    var showLayers by remember { mutableStateOf(false) }
    var showOutline by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var newTextSize by remember { mutableStateOf(20f) }
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
            updatedAt = System.currentTimeMillis()
        )
    }

    LaunchedEffect(editingTextId) {
        if (editingTextId != null) focusRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (chromeVisible) {
            // Header
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
                IconButton(
                    onClick = {
                        commitEditing()
                        onSave(buildDocument())
                    },
                    modifier = Modifier.size(btnSize)
                ) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = "Save note",
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            // Mode row: Select / Hand / Draw
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val modes = listOf(
                    Triple(EditorMode.SELECT, Icons.Filled.NearMe, "Select"),
                    Triple(EditorMode.HAND, Icons.Filled.PanTool, "Hand"),
                    Triple(EditorMode.DRAW, Icons.Filled.Brush, "Draw")
                )
                modes.forEach { (m, icon, label) ->
                    Row(
                        modifier = Modifier
                            .background(
                                if (m == mode) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                                RoundedCornerShape(50)
                            )
                            .clickable { setMode(m) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(iconSize))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = label, style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            // Draw sub-tools
            if (mode == EditorMode.DRAW) {
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
                            modifier = Modifier
                                .size(btnSize)
                                .background(
                                    if (t == tool) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                icon,
                                contentDescription = description,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                    IconButton(
                        onClick = { beautifyOn = !beautifyOn },
                        modifier = Modifier
                            .size(btnSize)
                            .background(
                                if (beautifyOn) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Filled.AutoFixHigh,
                            contentDescription = "Handwriting beautifier",
                            modifier = Modifier.size(iconSize)
                        )
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
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        modifier = Modifier.size(btnSize),
                        onClick = {
                            val layer = activeLayer()
                            if (layer != null && !layer.locked && layer.strokes.isNotEmpty()) {
                                redoStack.add(layer.strokes.removeAt(layer.strokes.lastIndex))
                            }
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
                        onClick = {
                            val layer = activeLayer()
                            if (layer != null && !layer.locked && redoStack.isNotEmpty()) {
                                layer.strokes.add(redoStack.removeAt(redoStack.lastIndex))
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    IconButton(
                        modifier = Modifier.size(btnSize),
                        onClick = {
                            val layer = activeLayer()
                            if (layer != null && !layer.locked) {
                                layer.strokes.clear()
                                redoStack.clear()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Clear ink on layer",
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }

                // Pen type picker (Pen tool only)
                if (tool == Tool.PEN) {
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
                                label = { Text(penTypeLabel(type)) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
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

            // Page bar: layers, outline, excerpt, page navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showLayers = true }, modifier = Modifier.size(btnSize)) {
                    Icon(
                        Icons.Filled.Layers,
                        contentDescription = "Layers",
                        modifier = Modifier.size(iconSize)
                    )
                }
                IconButton(onClick = { showOutline = true }, modifier = Modifier.size(btnSize)) {
                    Icon(
                        Icons.Filled.FormatListBulleted,
                        contentDescription = "Outline",
                        modifier = Modifier.size(iconSize)
                    )
                }
                IconButton(
                    onClick = {
                        commitEditing()
                        onExcerptPage(title.ifBlank { "Untitled" }, snapshotCurrentPage())
                    },
                    modifier = Modifier.size(btnSize)
                ) {
                    Icon(
                        Icons.Filled.ContentCut,
                        contentDescription = "Excerpt page into new note",
                        modifier = Modifier.size(iconSize)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { switchPage(currentPageIndex - 1) },
                    modifier = Modifier.size(btnSize)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous page",
                        modifier = Modifier.size(iconSize)
                    )
                }
                Text("Page ${currentPageIndex + 1} / ${pagesState.size}")
                IconButton(
                    onClick = { switchPage(currentPageIndex + 1) },
                    modifier = Modifier.size(btnSize)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Next page",
                        modifier = Modifier.size(iconSize)
                    )
                }
                IconButton(
                    onClick = {
                        pagesState[currentPageIndex] = snapshotCurrentPage()
                        val newPage = Page.create(currentTemplate)
                        pagesState.add(newPage)
                        switchPage(pagesState.lastIndex)
                    },
                    modifier = Modifier.size(btnSize)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add page",
                        modifier = Modifier.size(iconSize)
                    )
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
            if (mode == EditorMode.DRAW && paletteVisible && tool != Tool.ERASER) {
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
                            valueRange = 10f..120f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        NumericSizeField(
                            value = target?.fontSizeSp ?: newTextSize,
                            onApply = { v ->
                                val clamped = v.coerceIn(10f, 120f)
                                if (target != null) {
                                    updateText(target.copy(fontSizeSp = clamped))
                                } else {
                                    newTextSize = clamped
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

            val layerNow = activeLayer()
            if (layerNow != null && layerNow.locked) {
                Text(
                    text = "Layer \"${layerNow.name}\" is locked — unlock it in Layers to edit.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }

        // Page area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White)
                .clipToBounds()
        ) {
            fun toPage(screen: Offset, pressure: Float = 1f): StrokePoint = StrokePoint(
                x = (screen.x - viewOffset.x) / viewScale,
                y = (screen.y - viewOffset.y) / viewScale,
                pressure = pressure
            )

            val gestureModifier = when (mode) {
                EditorMode.HAND -> Modifier.pointerInput(mode) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (viewScale * zoom).coerceIn(0.3f, 4f)
                        val factor = newScale / viewScale
                        viewOffset = Offset(
                            x = centroid.x - (centroid.x - viewOffset.x) * factor + pan.x,
                            y = centroid.y - (centroid.y - viewOffset.y) * factor + pan.y
                        )
                        viewScale = newScale
                    }
                }
                EditorMode.SELECT -> Modifier.pointerInput(mode) {
                    coroutineScope {
                        launch {
                            detectTapGestures { screen ->
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
                                onDragStart = { },
                                onDragEnd = { },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val sel = selectedStroke
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
                                    when {
                                        editingTextId != null -> commitEditing()
                                        selectedTextId != null -> selectedTextId = null
                                        else -> {
                                            val layer = activeLayer()
                                            if (layer != null && !layer.locked) {
                                                val point = toPage(screen)
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
                            launch {
                                // Pinch anywhere on the page zooms the selected text.
                                detectTransformGestures { _, _, zoom, _ ->
                                    if (zoom != 1f && editingTextId == null) {
                                        val target = activeText()
                                        if (target != null) {
                                            updateText(
                                                target.copy(
                                                    fontSizeSp = (target.fontSizeSp * zoom)
                                                        .coerceIn(10f, 120f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> Modifier.pointerInput(tool, inkColor, thickness, penType) {
                        detectDragGestures(
                            onDragStart = { screen ->
                                val layer = activeLayer()
                                if (layer != null && !layer.locked) {
                                    val point = toPage(screen)
                                    if (tool == Tool.ERASER) {
                                        eraseAt(point)
                                    } else {
                                        currentPoints = listOf(point)
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val layer = activeLayer()
                                if (layer != null && !layer.locked) {
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
                            }
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
                drawTemplate(currentTemplate, viewScale, viewOffset, size)
                withTransform({
                    translate(viewOffset.x, viewOffset.y)
                    scale(viewScale, viewScale, Offset.Zero)
                }) {
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
                }
            }

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
                                (viewOffset.x + element.x * viewScale).roundToInt(),
                                (viewOffset.y + element.y * viewScale).roundToInt()
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
                                                                    .coerceIn(10f, 120f)
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

            // Fullscreen toggle (always available)
            IconButton(
                onClick = { chromeVisible = !chromeVisible },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
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
                                    MaterialTheme.colorScheme.primaryContainer
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
    val drawColor = when {
        stroke.highlighter -> baseColor.copy(alpha = 0.35f)
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

/** Draw the page template in screen space, following the pan/zoom transform. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTemplate(
    template: PageTemplate,
    scale: Float,
    offset: Offset,
    size: androidx.compose.ui.geometry.Size
) {
    if (template == PageTemplate.BLANK) return
    val left = -offset.x / scale
    val top = -offset.y / scale
    val right = left + size.width / scale
    val bottom = top + size.height / scale

    fun hLine(pageY: Float, color: Color, fromX: Float = left, toX: Float = right) {
        val y = pageY * scale + offset.y
        drawLine(
            color = color,
            start = Offset(fromX * scale + offset.x, y),
            end = Offset(toX * scale + offset.x, y),
            strokeWidth = 2f
        )
    }

    fun vLine(pageX: Float, color: Color) {
        val x = pageX * scale + offset.x
        drawLine(
            color = color,
            start = Offset(x, top * scale + offset.y),
            end = Offset(x, bottom * scale + offset.y),
            strokeWidth = 2f
        )
    }

    val lineColor = Color(0xFFBBDEFB)
    when (template) {
        PageTemplate.LINED, PageTemplate.PLANNER, PageTemplate.LECTURE -> {
            val startY = if (template == PageTemplate.LECTURE) 168f else 72f
            var y = floor((top - startY) / 72f) * 72f + startY
            if (y < startY) y = startY
            while (y <= bottom) {
                if (y >= startY) hLine(y, lineColor)
                y += 72f
            }
            vLine(72f, Color(0xFFF8BBD0))
            if (template == PageTemplate.PLANNER) {
                // Time column + checkboxes down the left side
                vLine(150f, lineColor)
                var cy = startY
                while (cy <= bottom) {
                    val topPx = (cy - 56f) * scale + offset.y
                    drawRect(
                        color = Color(0xFF90CAF9),
                        topLeft = Offset(92f * scale + offset.x, topPx),
                        size = androidx.compose.ui.geometry.Size(22f * scale, 22f * scale),
                        style = Stroke(width = 2f)
                    )
                    cy += 72f
                }
            }
            if (template == PageTemplate.LECTURE) {
                // Title / date header lines
                hLine(64f, Color(0xFF90CAF9), fromX = 40f)
                hLine(120f, Color(0xFF90CAF9), fromX = 40f)
            }
        }
        PageTemplate.DOTTED -> {
            // Mind-map friendly dot grid
            val step = 48f
            var gx = floor(left / step) * step
            while (gx <= right) {
                var gy = floor(top / step) * step
                while (gy <= bottom) {
                    drawCircle(
                        color = Color(0xFFB0BEC5),
                        radius = 2.5f,
                        center = Offset(gx * scale + offset.x, gy * scale + offset.y)
                    )
                    gy += step
                }
                gx += step
            }
        }
        PageTemplate.BLANK -> Unit
    }
}
