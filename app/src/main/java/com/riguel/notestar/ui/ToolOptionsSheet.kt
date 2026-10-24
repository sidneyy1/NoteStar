package com.riguel.notestar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riguel.notestar.data.Tool
import com.riguel.notestar.data.selectedPresetIndex
import kotlin.math.roundToInt

// =====================================================================
// Palettes and color math
// =====================================================================

/** The basic palette, in the spec's order (row 1, row 2, row 3 lead). */
val BasicPalette: List<Color> = listOf(
    // Row 1 (transparent is prepended separately, only where supported)
    Color.White, Color(0xFFBDBDBD), Color(0xFF757575), Color.Black,
    Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF42A5F5),
    // Row 2 — teal is first (the app accent family), then the spectrum
    Color(0xFF00897B), Color(0xFF2E7D32), Color(0xFF66BB6A),
    Color(0xFFFFEB3B), Color(0xFFEF6C00), Color(0xFFE53935),
    Color(0xFF880E4F), Color(0xFFD81B60),
    // Row 3 lead
    Color(0xFF7E57C2)
)

/** Highlighter-friendly palette, same grid style, same "+" Custom. */
val HighlighterPalette: List<Color> = listOf(
    Color(0xFFFFEB3B), Color(0xFFCDDC39), Color(0xFF69F0AE),
    Color(0xFF4DD0E1), Color(0xFF81D4FA), Color(0xFFF48FB1),
    Color(0xFFFFB74D), Color(0xFFCE93D8)
)

private val RainbowColors = listOf(
    Color.Red, Color(0xFFFF9800), Color.Yellow, Color.Green,
    Color(0xFF2196F3), Color(0xFF3F51B5), Color(0xFF9C27B0), Color.Red
)

/**
 * HSV + alpha, kept as separate components so the hue survives
 * saturation = 0 or brightness = 0 (where RGB has no hue to recover).
 * Conversions go through android.graphics.Color, the only API that
 * round-trips HSV without a Compose Color dependency.
 */
private data class HsvColor(
    val h: Float, // 0..360
    val s: Float, // 0..1
    val v: Float, // 0..1
    val a: Float // 0..1
) {
    fun toColor(): Color {
        val argb = android.graphics.Color.HSVToColor(
            (a * 255f).roundToInt().coerceIn(0, 255),
            floatArrayOf(h, s, v)
        )
        return Color(argb)
    }

    companion object {
        fun from(color: Color): HsvColor {
            val hsv = FloatArray(3)
            android.graphics.Color.RGBToHSV(
                (color.red * 255f).roundToInt(),
                (color.green * 255f).roundToInt(),
                (color.blue * 255f).roundToInt(),
                hsv
            )
            return HsvColor(hsv[0], hsv[1], hsv[2], color.alpha)
        }
    }
}

/** Hex formatting: #RRGGBB, or #RRGGBBAA when translucent. */
private fun Color.toHexString(): String {
    val argb = toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    val a = (argb ushr 24) and 0xFF
    return if (a < 255) {
        "#%02X%02X%02X%02X".format(r, g, b, a)
    } else {
        "#%02X%02X%02X".format(r, g, b)
    }
}

/**
 * Parses #RGB, #RRGGBB and #RRGGBBAA (alpha last, per spec).
 * Returns null for anything else — the caller shows an error and
 * leaves the current color untouched.
 */
private fun parseHexInput(input: String): Color? {
    val hex = input.trim().removePrefix("#")
    if (hex.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
        return null
    }
    return when (hex.length) {
        3 -> {
            val r = "${hex[0]}${hex[0]}".toInt(16)
            val g = "${hex[1]}${hex[1]}".toInt(16)
            val b = "${hex[2]}${hex[2]}".toInt(16)
            Color(r, g, b)
        }
        6 -> {
            val v = hex.toLongOrNull(16) ?: return null
            Color(0xFF000000.toInt() or v.toInt())
        }
        8 -> {
            val v = hex.toLongOrNull(16) ?: return null
            val r = ((v shr 24) and 0xFF).toInt()
            val g = ((v shr 16) and 0xFF).toInt()
            val b = ((v shr 8) and 0xFF).toInt()
            val a = (v and 0xFF).toInt()
            Color(r, g, b, a)
        }
        else -> null
    }
}

// =====================================================================
// Small shared pieces
// =====================================================================

/** The checkerboard behind the transparent swatch and alpha track. */
@Composable
private fun CheckerboardBox(modifier: Modifier) {
    Canvas(modifier = modifier) {
        val cell = size.minDimension / 4f
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = 0f
            var col = 0
            while (x < size.width) {
                if ((row + col) % 2 == 0) {
                    drawRect(
                        color = Color(0xFFCCCCCC),
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(cell, cell)
                    )
                }
                x += cell
                col++
            }
            y += cell
            row++
        }
    }
}

/**
 * A "-" or "+" stepper: one step on tap, repeating on long-press
 * (400ms delay, then every 90ms). Touch target is 48dp; the icon
 * itself is 24dp.
 */
@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        kotlinx.coroutines.delay(400)
        while (pressed) {
            onStep()
            kotlinx.coroutines.delay(90)
        }
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .semantics {
                contentDescription = description
                onClick { onStep(); true }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    pressed = true
                    onStep()
                    // Wait for lift; the repeat loop above runs while
                    // pressed, and stops when this sets it false.
                    do {
                        val event = awaitPointerEvent()
                        val up = event.changes.firstOrNull {
                            it.id == down.id
                        }
                        // pressed == false covers both lift and
                        // cancel (changedToUpOrCanceled does not exist
                        // on this Compose version).
                        if (up == null || !up.pressed) break
                    } while (true)
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = DesignTokens.TextPrimary)
    }
}

/**
 * A horizontal drag track: tap anywhere to jump, drag to scrub.
 * The thumb is drawn by [thumb] at the current [fraction]; the track
 * itself is drawn by [track]. Uses rememberUpdatedState so a
 * recomposition mid-drag (the value changing is the whole point)
 * never restarts the gesture.
 */
@Composable
private fun FractionTrack(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    onDragStart: (() -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    track: @Composable () -> Unit,
    thumb: @Composable () -> Unit
) {
    val latestOnFraction by rememberUpdatedState(onFractionChange)
    val latestOnStart by rememberUpdatedState(onDragStart)
    val latestOnEnd by rememberUpdatedState(onDragEnd)
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val thumbHalfPx = with(density) { 14.dp.toPx() }
        Box(modifier = Modifier.fillMaxWidth()) { track() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        latestOnStart?.invoke()
                        latestOnFraction(
                            (down.position.x / widthPx).coerceIn(0f, 1f)
                        )
                        drag(down.id) { change ->
                            change.consume()
                            latestOnFraction(
                                (change.position.x / widthPx).coerceIn(0f, 1f)
                            )
                        }
                        latestOnEnd?.invoke()
                    }
                }
        ) {
            Box(
                modifier = Modifier.offset {
                    androidx.compose.ui.unit.IntOffset(
                        ((fraction * widthPx) - thumbHalfPx).roundToInt(),
                        0
                    )
                }
            ) { thumb() }
        }
    }
}

// =====================================================================
// Color grid (basic view)
// =====================================================================

/**
 * One swatch cell: a 36dp rounded swatch (8dp radius, 1dp darker
 * border) centered in its touch box, a teal ring + check when
 * selected, and a long-press delete for saved custom colors only.
 * The row width is measured (BoxWithConstraints) so 8 cells always
 * fit without clipping: on narrow phones the swatches shrink a
 * little below 36dp instead of overflowing (8 x 36dp + 7 x 8dp gaps
 * = 344dp, wider than a 360dp phone's usable width).
 */
// combinedClickable is still experimental in this Compose version
// (BOM 2024.09.00); the opt-in is the standard way to use it.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwatchCell(
    color: Color,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?,
    cellWidth: Dp
) {
    val haptics = LocalHapticFeedback.current
    val swatchSize = minOf(36.dp, cellWidth - 4.dp).coerceAtLeast(20.dp)
    Box(
        modifier = Modifier
            .width(cellWidth)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {
                    haptics.performHapticFeedback(
                        HapticFeedbackType.TextHandleMove
                    )
                    onSelect()
                },
                onLongClick = onDelete
            ),
        contentAlignment = Alignment.Center
    ) {
        if (color == Color.Transparent) {
            Box(
                modifier = Modifier
                    .size(swatchSize)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF999999), RoundedCornerShape(8.dp))
            ) {
                CheckerboardBox(modifier = Modifier.fillMaxWidth())
            }
        } else {
            Box(
                modifier = Modifier
                    .size(swatchSize)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) {
                            DesignTokens.Accent
                        } else {
                            Color.Black.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (color.luminance() > 0.5f) {
                            DesignTokens.Accent
                        } else {
                            Color.White
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (selected && color == Color.Transparent) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = DesignTokens.Accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * The swatch grid: row 1 (optional transparent + 7), row 2 (8),
 * then purple + recents + saved customs, chunked into rows of 8,
 * and finally the "+" Custom swatch (rainbow ring) that opens the
 * advanced picker. Tapping a swatch commits it (caller applies,
 * records it as recent, and closes the sheet by default).
 */
@Composable
fun ColorGrid(
    selected: Color?,
    isHighlighter: Boolean,
    recentColors: List<Color>,
    customColors: List<Color>,
    showTransparent: Boolean,
    onSelect: (Color) -> Unit,
    onDeleteCustom: (Color) -> Unit,
    onCustomPlus: () -> Unit,
    showCustomPlus: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = if (isHighlighter) HighlighterPalette else BasicPalette
    // Spec rows, preserved whether or not the transparent swatch is
    // shown: row 1 = transparent (if supported) + the first 7,
    // row 2 = the next 8, row 3+ = purple + recents + customs.
    // (BasicPalette is ordered [7, 8, purple]; the highlighter
    // palette is a single row of 8, so its rows are [8] + tail.)
    // De-duplicated by exact RGBA, palette first: a color that is
    // both a preset and a recent (or a custom) appears exactly once,
    // so exact-equality selection can never check two cells. Each
    // cell draws its own stored color.
    val shownRecents = recentColors.filter { it !in palette }
    val shownCustoms = customColors.filter {
        it !in palette && it !in shownRecents
    }
    val rows: List<List<Color>> = if (isHighlighter) {
        // One row of 8, then recents + customs.
        listOf(palette) +
            (shownRecents + shownCustoms).chunked(8)
                .filter { it.isNotEmpty() }
    } else {
        val row1 = (if (showTransparent) {
            listOf(Color.Transparent)
        } else {
            emptyList()
        }) + palette.take(7)
        val row2 = palette.drop(7).take(8)
        val tail = palette.drop(15) + shownRecents + shownCustoms
        listOf(row1, row2) +
            tail.chunked(8).filter { it.isNotEmpty() }
    }

    Column(modifier = modifier) {
        Text(
            text = "Color palette",
            style = MaterialTheme.typography.labelLarge,
            color = DesignTokens.TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        BoxWithConstraints {
            val cellWidth = maxWidth / 8
            for (row in rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (color in row) {
                        key(color) {
                            SwatchCell(
                                color = color,
                                selected = selected == color,
                                onSelect = { onSelect(color) },
                                onDelete = if (color in customColors) {
                                    { onDeleteCustom(color) }
                                } else {
                                    null
                                },
                                cellWidth = cellWidth
                            )
                        }
                    }
                    // Pad a short last row so cells keep their width.
                    repeat(8 - row.size) {
                        Spacer(modifier = Modifier.width(cellWidth))
                    }
                }
            }
            // The "+" Custom swatch: rainbow ring, Add icon, labeled
            // "Custom" underneath, on its own row so the label fits.
            // Hidden inside the advanced view, where it would be a
            // no-op (the user is already there).
            if (showCustomPlus) Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(cellWidth)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCustomPlus() }
                        .padding(vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 2.dp,
                                brush = Brush.sweepGradient(RainbowColors),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = DesignTokens.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Custom",
                        style = MaterialTheme.typography.labelSmall,
                        color = DesignTokens.TextSecondary
                    )
                }
            }
        }
    }
}

// =====================================================================
// Advanced color picker
// =====================================================================

/** Saturation/brightness square: hue -> white horizontally, -> black vertically. */
@Composable
private fun SaturationBrightnessPanel(
    hsv: HsvColor,
    onChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnChange by rememberUpdatedState(onChange)
    val hueColor = HsvColor(hsv.h, 1f, 1f, 1f).toColor()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(hueColor)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    latestOnChange(
                        (down.position.x / size.width).coerceIn(0f, 1f),
                        1f - (down.position.y / size.height).coerceIn(0f, 1f)
                    )
                    drag(down.id) { change ->
                        change.consume()
                        latestOnChange(
                            (change.position.x / size.width).coerceIn(0f, 1f),
                            1f - (change.position.y / size.height)
                                .coerceIn(0f, 1f)
                        )
                    }
                }
            }
    ) {
        // White -> transparent (saturation), then transparent -> black
        // (brightness). Two overlay boxes, same size as the panel.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.White, Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black)
                    )
                )
        )
        // Thumb: white 2dp outline, subtle shadow, at (s, 1 - v).
        // Note: the panel height is fixed at 160dp, so the Y math
        // uses that, not constraints.maxHeight — inside a scrolling
        // column the height constraint can be infinite.
        val density = LocalDensity.current
        val panelHeightPx = with(density) { 160.dp.toPx() }
        val thumbHalfPx = with(density) { 12.dp.toPx() }
        Box(
            modifier = Modifier.offset {
                val x = (hsv.s * constraints.maxWidth - thumbHalfPx)
                    .roundToInt()
                val y = ((1f - hsv.v) * panelHeightPx - thumbHalfPx)
                    .roundToInt()
                androidx.compose.ui.unit.IntOffset(x, y)
            }
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(hsv.toColor())
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun RoundThumb(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .shadow(4.dp, CircleShape)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color.White, CircleShape)
    )
}

/**
 * The advanced picker: SB square, hue slider, alpha slider, hex
 * field, the preset grid, and the old/new + Save + Done footer.
 * Every control writes the same [HsvColor] state, so all of them
 * stay in sync live; [onColorChange] applies each new color to the
 * tool immediately, and Done commits it (recent + close) via the
 * caller's [onDone].
 */
@Composable
fun AdvancedColorPicker(
    initialColor: Color,
    isHighlighter: Boolean,
    recentColors: List<Color>,
    customColors: List<Color>,
    onColorChange: (Color) -> Unit,
    onSaveCustom: (Color) -> Unit,
    onDeleteCustom: (Color) -> Unit,
    onDone: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    // Fresh state on entry (this composable only exists while the
    // advanced view is open); never re-read from initialColor
    // mid-drag, so hue can't jump from RGB round-tripping.
    var hsv by remember { mutableStateOf(HsvColor.from(initialColor)) }
    val oldColor = remember { initialColor }
    val current = hsv.toColor()
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current

    fun apply(next: HsvColor) {
        hsv = next
        onColorChange(next.toColor())
    }

    Column(modifier = modifier) {
        SaturationBrightnessPanel(
            hsv = hsv,
            onChange = { s, v -> apply(hsv.copy(s = s, v = v)) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Hue slider: rainbow track, thumb filled with the hue.
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(
                icon = Icons.Filled.Remove,
                description = "Decrease hue",
                onStep = {
                    apply(hsv.copy(h = (hsv.h - 1f + 360f) % 360f))
                }
            )
            FractionTrack(
                fraction = hsv.h / 360f,
                onFractionChange = { f ->
                    apply(hsv.copy(h = f * 360f))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                track = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(RainbowColors)
                            )
                    )
                },
                thumb = { RoundThumb(HsvColor(hsv.h, 1f, 1f, 1f).toColor()) }
            )
            StepButton(
                icon = Icons.Filled.Add,
                description = "Increase hue",
                onStep = {
                    apply(hsv.copy(h = (hsv.h + 1f) % 360f))
                }
            )
        }
        // Opacity slider: checkerboard fading into the current color.
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(
                icon = Icons.Filled.Remove,
                description = "Decrease opacity",
                onStep = {
                    apply(hsv.copy(a = (hsv.a - 0.01f).coerceIn(0f, 1f)))
                }
            )
            FractionTrack(
                fraction = hsv.a,
                onFractionChange = { f -> apply(hsv.copy(a = f)) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                track = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        CheckerboardBox(modifier = Modifier.fillMaxWidth())
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            HsvColor(hsv.h, hsv.s, hsv.v, 1f)
                                                .toColor()
                                        )
                                    )
                                )
                        )
                    }
                },
                thumb = { RoundThumb(current) }
            )
            StepButton(
                icon = Icons.Filled.Add,
                description = "Increase opacity",
                onStep = {
                    apply(hsv.copy(a = (hsv.a + 0.01f).coerceIn(0f, 1f)))
                }
            )
        }
        // Hex field: validates as typed, selects all on tap, copies.
        var hexError by remember { mutableStateOf(false) }
        var fieldFocused by remember { mutableStateOf(false) }
        var fieldValue by remember {
            mutableStateOf(TextFieldValue(current.toHexString()))
        }
        // Keep the field in step with sliders/square while it is
        // not being edited; while editing, only errors change.
        LaunchedEffect(current) {
            if (!fieldFocused) {
                fieldValue = TextFieldValue(current.toHexString())
                hexError = false
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = fieldValue,
                onValueChange = { next ->
                    val cleaned = next.text.filter { c ->
                        c.isDigit() || c in 'a'..'f' || c in 'A'..'F' ||
                            c == '#'
                    }.take(9)
                    fieldValue = TextFieldValue(
                        cleaned, TextRange(cleaned.length)
                    )
                    val parsed = parseHexInput(cleaned)
                    if (parsed != null) {
                        hexError = false
                        hsv = HsvColor.from(parsed)
                        onColorChange(parsed)
                    }
                    // While typing, an incomplete value is not an
                    // error yet — the error state appears on blur
                    // (below) if the value still doesn't parse.
                },
                isError = hexError,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                supportingText = if (hexError) {
                    { Text("Invalid hex — use #RGB, #RRGGBB or #RRGGBBAA") }
                } else {
                    null
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { state ->
                        fieldFocused = state.isFocused
                        if (state.isFocused) {
                            fieldValue = fieldValue.copy(
                                selection = TextRange(
                                    0, fieldValue.text.length
                                )
                            )
                        } else {
                            // Blur with an unparsable, non-empty value
                            // keeps the text and shows the error; the
                            // current color is left untouched, and the
                            // field resyncs on the next color change.
                            val parsed = parseHexInput(fieldValue.text)
                            if (parsed == null &&
                                fieldValue.text.isNotBlank() &&
                                fieldValue.text != "#"
                            ) {
                                hexError = true
                            } else {
                                hexError = false
                                fieldValue = TextFieldValue(
                                    current.toHexString()
                                )
                            }
                        }
                    }
            )
            IconButton(onClick = {
                clipboard.setText(AnnotatedString(current.toHexString()))
            }) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy hex color"
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // The same preset grid, for jumping back to a preset: taps
        // here only move the advanced controls (live), they don't
        // commit or close.
        ColorGrid(
            selected = current,
            isHighlighter = isHighlighter,
            recentColors = recentColors,
            customColors = customColors,
            showTransparent = false,
            onSelect = { chosen ->
                haptics.performHapticFeedback(
                    HapticFeedbackType.TextHandleMove
                )
                hsv = HsvColor.from(chosen)
                onColorChange(chosen)
            },
            onDeleteCustom = onDeleteCustom,
            onCustomPlus = { /* already in advanced */ },
            showCustomPlus = false
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Footer: old vs new preview, Save color, Done.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(oldColor)
                    .border(1.dp, Color.Black.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp))
            )
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = null,
                tint = DesignTokens.TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(current)
                    .border(1.dp, Color.Black.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { onSaveCustom(current) }) {
                Text("Save color")
            }
            Button(
                onClick = { onDone(current) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DesignTokens.Accent
                )
            ) {
                Text("Done")
            }
        }
    }
}

// =====================================================================
// Brush size control
// =====================================================================

enum class PreviewKind { STROKE, HIGHLIGHTER, TEXT }

/** Per-tool size ranges (spec Part 2). */
fun sizeRangeFor(tool: Tool, penType: String): ClosedFloatingPointRange<Float> =
    when (tool) {
        Tool.HIGHLIGHTER -> 4f..80f
        Tool.ERASER -> 2f..100f
        Tool.PEN -> when (penType) {
            "pencil" -> 0.5f..30f
            "calligraphy" -> 1f..60f
            else -> 0.5f..50f
        }
        else -> 0.5f..50f
    }

/** Perceptual slider mapping: fine control at small sizes. */
private fun sizeToFraction(size: Float, range: ClosedFloatingPointRange<Float>): Float {
    val t = ((size - range.start) / (range.endInclusive - range.start))
        .coerceIn(0f, 1f)
    return kotlin.math.sqrt(t)
}

private fun fractionToSize(f: Float, range: ClosedFloatingPointRange<Float>): Float =
    range.start + (range.endInclusive - range.start) * f * f

/** 0.5 steps below 10, 1.0 steps at and above. */
private fun sizeStep(size: Float): Float = if (size < 10f) 0.5f else 1f

/** Live preview: a curved stroke (or "Aa" for text) at the size. */
@Composable
private fun SizePreview(
    brushSize: Float,
    color: Color,
    kind: PreviewKind,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F2F2)),
        contentAlignment = Alignment.Center
    ) {
        if (kind == PreviewKind.TEXT) {
            Text(
                text = "Aa",
                color = color,
                fontSize = brushSize.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            val density = LocalDensity.current
            Canvas(modifier = Modifier.fillMaxWidth()) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.62f)
                    quadraticBezierTo(w * 0.4f, h * 0.15f, w * 0.62f, h * 0.55f)
                    quadraticBezierTo(w * 0.78f, h * 0.85f, w * 0.88f, h * 0.4f)
                }
                val strokePx = with(density) {
                    brushSize.dp.toPx().coerceIn(1f, 56.dp.toPx())
                }
                drawPath(
                    path = path,
                    color = if (kind == PreviewKind.HIGHLIGHTER) {
                        color.copy(alpha = 0.55f)
                    } else {
                        color
                    },
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

/** A size exactly, with decimals (sizes step by 0.5). */
@Composable
private fun DecimalSizeField(
    value: Float,
    onApply: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) {
        mutableStateOf(
            if (value % 1f == 0f) value.roundToInt().toString()
            else value.toString()
        )
    }
    OutlinedTextField(
        value = text,
        onValueChange = { next ->
            val filtered = next.filter { c -> c.isDigit() || c == '.' }
            // One decimal point only.
            if (filtered.count { it == '.' } <= 1) text = filtered
        },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = androidx.compose.ui.text.input.ImeAction.Done
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = { text.toFloatOrNull()?.let { onApply(it) } }
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.width(84.dp)
    )
}

private val SizePresetLabels = listOf("XS", "S", "M", "L", "XL")

/**
 * Preview + perceptual slider + steppers + numeric field + preset
 * chips. The slider itself is a FractionTrack (tap to jump, drag
 * to scrub) with a 28dp thumb on a 6dp teal track.
 *
 * Preset selection is radio-style and single-choice by
 * construction: [selectedPresetIndex] (one state value, the size)
 * returns at most one index, and each chip only reads whether it
 * is that index. Chips carry stable keys (their labels), so list
 * reuse or recomposition can't leave a stale highlight.
 */
@Composable
fun BrushSizeControl(
    size: Float,
    range: ClosedFloatingPointRange<Float>,
    presets: List<Float>,
    unit: String,
    color: Color,
    kind: PreviewKind,
    onSizeChange: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = sizeToFraction(size, range)
    Column(modifier = modifier) {
        SizePreview(brushSize = size, color = color, kind = kind)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(
                icon = Icons.Filled.Remove,
                description = "Decrease size",
                onStep = {
                    onSizeChange(
                        (size - sizeStep(size)).coerceIn(range)
                    )
                }
            )
            FractionTrack(
                fraction = fraction,
                onFractionChange = { f ->
                    onSizeChange(fractionToSize(f, range))
                },
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                track = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(DesignTokens.Accent)
                    )
                },
                thumb = { RoundThumb(DesignTokens.Accent) }
            )
            StepButton(
                icon = Icons.Filled.Add,
                description = "Increase size",
                onStep = {
                    onSizeChange(
                        (size + sizeStep(size)).coerceIn(range)
                    )
                }
            )
            DecimalSizeField(
                value = size,
                onApply = { v -> onSizeChange(v.coerceIn(range)) }
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyMedium,
                color = DesignTokens.TextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        // Preset chips: radio-style, single selection from the one
        // size value (see selectedPresetIndex). Horizontally
        // scrollable as a fallback, but the padding is reduced so
        // all five fit on a 360dp phone without scrolling.
        val selectedPreset = selectedPresetIndex(size, presets)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SizePresetLabels.forEachIndexed { i, label ->
                val preset = presets.getOrNull(i) ?: return@forEachIndexed
                val selected = i == selectedPreset
                val dotSize = preset.coerceIn(2f, 16f).dp
                key(label) {
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (selected) DesignTokens.AccentTint
                                else Color(0xFFF2F2F2)
                            )
                            .border(
                                width = if (selected) 1.dp else 0.dp,
                                color = if (selected) DesignTokens.Accent
                                else Color.Transparent,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable {
                                onSizeChange(preset.coerceIn(range))
                            }
                            .semantics {
                                role = Role.RadioButton
                                this.selected = selected
                            }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) DesignTokens.Accent
                                else DesignTokens.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Pen type segmented control
// =====================================================================

/** Pencil / Pen / Calligraphy as one segmented control, icons + labels. */
@Composable
fun PenTypeSegmentedControl(
    penType: String,
    onPenTypeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple("pencil", "Pencil", Icons.Filled.Create),
        Triple("pen", "Pen", Icons.Filled.Edit),
        Triple("calligraphy", "Calligraphy", Icons.Filled.Brush)
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F2F2))
            .padding(4.dp)
    ) {
        for ((key, label, icon) in items) {
            val selected = penType == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selected) DesignTokens.Accent else Color.Transparent
                    )
                    .clickable { onPenTypeChange(key) },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selected) Color.White
                        else DesignTokens.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White
                        else DesignTokens.TextPrimary
                    )
                }
            }
        }
    }
}

// =====================================================================
// The sheet itself
// =====================================================================

enum class ToolSheetView { MAIN, COLOR, ADVANCED }

/**
 * The shared tool options sheet for the normal-note editor and the
 * imported-PDF editor: one component, same behaviour in both.
 *
 * Views: MAIN (pen types, brush size, color row), COLOR (swatch
 * grid only — no sliders, no hex), ADVANCED (square, hue/alpha
 * sliders, hex, grid, footer). [entryView] is the view the sheet
 * opened with; Back from COLOR returns to MAIN only if the sheet
 * opened on MAIN, otherwise it dismisses. Back from ADVANCED
 * returns to COLOR, and Back from MAIN dismisses.
 *
 * Layout: on a phone it fills the width (16dp side margins from the
 * caller) and caps at 50% of screen height on MAIN, 60% on the
 * color views, scrolling if taller; on a tablet/landscape it caps
 * at 360dp wide and centers (the caller's column centers it).
 * The drag handle swipes down to dismiss; [onDismiss] is also
 * called by the caller's outside-tap scrim. Chrome is explicit,
 * not themed: sheet white (#FFFFFF), preview and unselected chips
 * light gray (#F2F2F2) — the theme surface colors in this app
 * resolve to Material3's lavender defaults, which is where the
 * old tint came from. Selection is always the teal accent.
 */
@Composable
fun ToolOptionsSheet(
    view: ToolSheetView,
    entryView: ToolSheetView,
    onViewChange: (ToolSheetView) -> Unit,
    onDismiss: () -> Unit,
    isHighlighter: Boolean,
    showTransparent: Boolean,
    showPenTypes: Boolean,
    penType: String,
    onPenTypeChange: (String) -> Unit,
    showSize: Boolean,
    size: Float,
    sizeRange: ClosedFloatingPointRange<Float>,
    presets: List<Float>,
    sizeUnit: String,
    previewKind: PreviewKind,
    onSizeChange: (Float) -> Unit,
    onSizeDragStart: () -> Unit,
    onSizeDragEnd: () -> Unit,
    showColorRow: Boolean,
    inkColor: Color,
    onColorLive: (Color) -> Unit,
    onColorCommit: (Color) -> Unit,
    recentColors: List<Color>,
    customColors: List<Color>,
    onSaveCustom: (Color) -> Unit,
    onDeleteCustom: (Color) -> Unit,
    selectColorMode: Boolean,
    showPressureToggle: Boolean,
    pressureEnabled: Boolean,
    onPressureChange: (Boolean) -> Unit,
    extraTopContent: @Composable ColumnScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    fun goBack() {
        when (view) {
            ToolSheetView.ADVANCED -> onViewChange(ToolSheetView.COLOR)
            ToolSheetView.COLOR -> {
                if (selectColorMode || entryView == ToolSheetView.COLOR) {
                    onDismiss()
                } else {
                    onViewChange(ToolSheetView.MAIN)
                }
            }
            ToolSheetView.MAIN -> onDismiss()
        }
    }

    BackHandler { goBack() }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = screenHeight * if (view == ToolSheetView.MAIN) 0.5f else 0.6f

    Surface(
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        // Explicit white (v0.15.2): the theme surface color in this
        // app resolves to Material3's default pale lavender
        // (#FEF7FF), which is the tint the sheet used to show.
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column {
            // Drag handle: swipe down to dismiss.
            var dragTotal by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 4.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E5E5))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragTotal > 60f) onDismiss()
                                dragTotal = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                dragTotal += dragAmount
                            }
                        )
                    }
            )
            Column(
                modifier = Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                when (view) {
                    ToolSheetView.MAIN -> {
                        extraTopContent()
                        if (showPenTypes) {
                            PenTypeSegmentedControl(
                                penType = penType,
                                onPenTypeChange = onPenTypeChange
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        if (showSize) {
                            BrushSizeControl(
                                size = size,
                                range = sizeRange,
                                presets = presets,
                                unit = sizeUnit,
                                color = inkColor,
                                kind = previewKind,
                                onSizeChange = onSizeChange,
                                onDragStart = onSizeDragStart,
                                onDragEnd = onSizeDragEnd
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        if (showColorRow) {
                            // Current swatch + 6 most recent + More.
                            Text(
                                text = "Color",
                                style = MaterialTheme.typography.labelLarge,
                                color = DesignTokens.TextSecondary,
                                modifier = Modifier.padding(
                                    start = 4.dp, bottom = 4.dp
                                )
                            )
                            // Single check, by construction: the
                            // first cell is always the current color
                            // (checked), and recents exclude it, so
                            // no second cell can ever match it. Each
                            // cell draws its own stored color.
                            val rowRecents = recentColors
                                .filter { it != inkColor }
                                .take(6)
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                key(inkColor) {
                                    SwatchCell(
                                        color = inkColor,
                                        selected = true,
                                        onSelect = {},
                                        onDelete = null,
                                        cellWidth = 44.dp
                                    )
                                }
                                for (recent in rowRecents) {
                                    key(recent) {
                                        SwatchCell(
                                            color = recent,
                                            selected = false,
                                            onSelect = {
                                                onColorCommit(recent)
                                                onDismiss()
                                            },
                                            onDelete = null,
                                            cellWidth = 44.dp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(
                                    onClick = {
                                        onViewChange(ToolSheetView.COLOR)
                                    }
                                ) {
                                    Text("More colors")
                                }
                            }
                        }
                        if (showPressureToggle) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Pressure sensitivity",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = pressureEnabled,
                                    onCheckedChange = onPressureChange
                                )
                            }
                        }
                    }
                    ToolSheetView.COLOR -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { goBack() }) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            Text(
                                text = "Colors",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        ColorGrid(
                            selected = inkColor,
                            isHighlighter = isHighlighter,
                            recentColors = recentColors,
                            customColors = customColors,
                            showTransparent = showTransparent,
                            onSelect = { chosen ->
                                onColorCommit(chosen)
                                onDismiss()
                            },
                            onDeleteCustom = onDeleteCustom,
                            onCustomPlus = {
                                onViewChange(ToolSheetView.ADVANCED)
                            }
                        )
                    }
                    ToolSheetView.ADVANCED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { goBack() }) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            Text(
                                text = "Custom color",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        AdvancedColorPicker(
                            initialColor = inkColor,
                            isHighlighter = isHighlighter,
                            recentColors = recentColors,
                            customColors = customColors,
                            onColorChange = onColorLive,
                            onSaveCustom = onSaveCustom,
                            onDeleteCustom = onDeleteCustom,
                            onDone = { chosen ->
                                onColorCommit(chosen)
                                onDismiss()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
