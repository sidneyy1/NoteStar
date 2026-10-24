package com.riguel.notestar.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.riguel.notestar.data.PageTemplate
import kotlin.math.floor

/** The geometric family a paper pattern belongs to. */
enum class PatternKind { BLANK, LINES, GRID, DOTS }

/**
 * One paper template in the registry: everything the picker, the editor
 * canvas and the thumbnails need. To add a template later: add a
 * [PageTemplate] value and one entry in [PaperTemplates.all] — it then
 * shows up in the picker (via its category list), the editor and exports
 * of the note data (the template id is stored in the note JSON).
 */
data class PaperTemplateDef(
    val template: PageTemplate,
    val label: String,
    val category: String,
    val kind: PatternKind,
    /** Pattern pitch in millimetres on an A4 page (0 for Blank). */
    val spacingMm: Float = 0f,
    /** Pattern stroke width in px at 1x zoom (dot radius for DOTS). */
    val strokeWidthPx: Float = 1f,
    val patternColor: Color = Color(0xFFD0D0D0),
    /** Ruled templates start their first line this far below the page top. */
    val topMarginMm: Float = 0f
)

object PaperTemplates {
    /** Page coordinates are ~150 dpi: 150 / 25.4 ≈ 5.9 units per mm. */
    const val UNITS_PER_MM: Float = 5.9f
    /** Points (1/72 inch) per millimetre — the note page space. */
    const val PT_PER_MM: Float = 72f / 25.4f

    /** Width of an A4 page in page-coordinate units. */
    const val A4_WIDTH_UNITS: Float = 210f * UNITS_PER_MM

    val all: List<PaperTemplateDef> = listOf(
        PaperTemplateDef(
            PageTemplate.BLANK, "Blank page", "Base", PatternKind.BLANK
        ),
        PaperTemplateDef(
            PageTemplate.THIN_LINE, "Thin line", "Base", PatternKind.LINES,
            spacingMm = 7f, strokeWidthPx = 1f,
            patternColor = Color(0xFFD0D0D0), topMarginMm = 5f
        ),
        PaperTemplateDef(
            PageTemplate.BOLD_LINE, "Bold line", "Base", PatternKind.LINES,
            spacingMm = 9f, strokeWidthPx = 1.5f,
            patternColor = Color(0xFFBDBDBD), topMarginMm = 5f
        ),
        PaperTemplateDef(
            PageTemplate.MINI_CHECK, "Mini check", "Base", PatternKind.GRID,
            spacingMm = 5f, strokeWidthPx = 0.75f,
            patternColor = Color(0xFFD9D9D9)
        ),
        PaperTemplateDef(
            PageTemplate.MID_CHECK, "Mid check", "Base", PatternKind.GRID,
            spacingMm = 8f, strokeWidthPx = 1f,
            patternColor = Color(0xFFD0D0D0)
        ),
        PaperTemplateDef(
            PageTemplate.DOT_GRID, "Dot grid", "Base", PatternKind.DOTS,
            spacingMm = 5f, strokeWidthPx = 1.5f,
            patternColor = Color(0xFFBDBDBD)
        ),
        // Long-standing templates (kept pixel-identical to their original
        // rendering: 72-unit ruling / 48-unit dot step, drawn specially in
        // drawPaperPattern).
        PaperTemplateDef(
            PageTemplate.LINED, "Lined page", "Study", PatternKind.LINES,
            spacingMm = 12f, strokeWidthPx = 2f,
            patternColor = Color(0xFFBBDEFB), topMarginMm = 12f
        ),
        PaperTemplateDef(
            PageTemplate.DOTTED, "Mind map (dot grid)", "Specialty", PatternKind.DOTS,
            spacingMm = 8f, strokeWidthPx = 2.5f,
            patternColor = Color(0xFFB0BEC5)
        ),
        PaperTemplateDef(
            PageTemplate.PLANNER, "Planner", "Plan", PatternKind.LINES,
            spacingMm = 12f, strokeWidthPx = 2f,
            patternColor = Color(0xFFBBDEFB), topMarginMm = 12f
        ),
        PaperTemplateDef(
            PageTemplate.LECTURE, "Lecture notes", "Study", PatternKind.LINES,
            spacingMm = 12f, strokeWidthPx = 2f,
            patternColor = Color(0xFFBBDEFB), topMarginMm = 28f
        )
    )

    fun def(template: PageTemplate): PaperTemplateDef =
        all.first { it.template == template }

    /** The Base category, in picker order. */
    val baseCategory: List<PageTemplate> = listOf(
        PageTemplate.BLANK,
        PageTemplate.THIN_LINE,
        PageTemplate.BOLD_LINE,
        PageTemplate.MINI_CHECK,
        PageTemplate.MID_CHECK,
        PageTemplate.DOT_GRID
    )
}

/**
 * Procedurally draws [template]'s pattern in screen space, following the
 * pan/zoom transform (page point p lands at p * scale + offset) — the same
 * convention the stroke renderer uses, so the vector pattern stays crisp at
 * any zoom. Spacing derives from the registry's millimetre values, so the
 * pattern scales to any page format. The pattern is page background only:
 * it belongs to no layer, and the eraser never touches it.
 */
fun DrawScope.drawPaperPattern(
    template: PageTemplate,
    scale: Float,
    offset: Offset,
    size: Size,
    unitsPerMm: Float = PaperTemplates.UNITS_PER_MM
) {
    // Legacy templates were laid out in ~150dpi page units; [u] converts
    // their constants into the caller's space (points for the bounded
    // editor, units for thumbnails).
    val u = unitsPerMm / PaperTemplates.UNITS_PER_MM
    if (template == PageTemplate.BLANK) return
    val left = -offset.x / scale
    val top = -offset.y / scale
    val right = left + size.width / scale
    val bottom = top + size.height / scale

    fun hLine(pageY: Float, color: Color, width: Float, fromX: Float = left, toX: Float = right) {
        val y = pageY * scale + offset.y
        drawLine(
            color = color,
            start = Offset(fromX * scale + offset.x, y),
            end = Offset(toX * scale + offset.x, y),
            strokeWidth = width
        )
    }

    fun vLine(pageX: Float, color: Color, width: Float) {
        val x = pageX * scale + offset.x
        drawLine(
            color = color,
            start = Offset(x, top * scale + offset.y),
            end = Offset(x, bottom * scale + offset.y),
            strokeWidth = width
        )
    }

    val def = PaperTemplates.def(template)
    when (def.kind) {
        PatternKind.BLANK -> Unit

        PatternKind.LINES -> {
            if (template == PageTemplate.LINED ||
                template == PageTemplate.PLANNER ||
                template == PageTemplate.LECTURE
            ) {
                // Legacy geometry, unchanged: 72-unit ruling + margin line.
                val lineColor = Color(0xFFBBDEFB)
                val startY = (if (template == PageTemplate.LECTURE) 168f else 72f) * u
                var y = floor((top - startY) / (72f * u)) * (72f * u) + startY
                if (y < startY) y = startY
                while (y <= bottom) {
                    if (y >= startY) hLine(y, lineColor, 2f)
                    y += 72f * u
                }
                vLine(72f * u, Color(0xFFF8BBD0), 2f)
                if (template == PageTemplate.PLANNER) {
                    // Time column + checkboxes down the left side
                    vLine(150f * u, lineColor, 2f)
                    var cy = startY
                    while (cy <= bottom) {
                        val topPx = (cy - 56f * u) * scale + offset.y
                        drawRect(
                            color = Color(0xFF90CAF9),
                            topLeft = Offset(92f * u * scale + offset.x, topPx),
                            size = Size(22f * u * scale, 22f * u * scale),
                            style = Stroke(width = 2f)
                        )
                        cy += 72f * u
                    }
                }
                if (template == PageTemplate.LECTURE) {
                    // Title / date header lines
                    hLine(64f * u, Color(0xFF90CAF9), 2f, fromX = 40f * u)
                    hLine(120f * u, Color(0xFF90CAF9), 2f, fromX = 40f * u)
                }
            } else {
                val step = def.spacingMm * unitsPerMm
                val startY = def.topMarginMm * unitsPerMm
                var y = floor((top - startY) / step) * step + startY
                if (y < startY) y = startY
                while (y <= bottom) {
                    if (y >= startY) hLine(y, def.patternColor, def.strokeWidthPx)
                    y += step
                }
            }
        }

        PatternKind.GRID -> {
            val step = def.spacingMm * unitsPerMm
            var gx = floor(left / step) * step
            while (gx <= right) {
                vLine(gx, def.patternColor, def.strokeWidthPx)
                gx += step
            }
            var gy = floor(top / step) * step
            while (gy <= bottom) {
                hLine(gy, def.patternColor, def.strokeWidthPx)
                gy += step
            }
        }

        PatternKind.DOTS -> {
            if (template == PageTemplate.DOTTED) {
                // Legacy mind-map dot grid (48-unit step), unchanged.
                val step = 48f * u
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
            } else {
                val step = def.spacingMm * unitsPerMm
                var gx = floor(left / step) * step
                while (gx <= right) {
                    var gy = floor(top / step) * step
                    while (gy <= bottom) {
                        drawCircle(
                            color = def.patternColor,
                            radius = def.strokeWidthPx,
                            center = Offset(gx * scale + offset.x, gy * scale + offset.y)
                        )
                        gy += step
                    }
                    gx += step
                }
            }
        }
    }
}

/**
 * Fixed geometry of the long-standing templates (Lined / Planner /
 * Lecture), in millimetres — the single source the page-space renderer
 * below and the PDF exporter both read, so screen and export agree.
 */
object LegacyPaper {
    /** Lined margin line distance from the page's left edge. */
    const val MARGIN_X_MM: Float = 12f
    /** Planner time-column line distance from the left edge. */
    const val PLANNER_COLUMN_MM: Float = 25.4f
    /** Planner checkbox left edge. */
    const val CHECKBOX_X_MM: Float = 15.6f
    /** Planner checkbox size (square). */
    const val CHECKBOX_SIZE_MM: Float = 3.7f
    /** Checkbox top sits this far above its ruling line. */
    const val CHECKBOX_LIFT_MM: Float = 9.5f
    /** Lecture header rule distances from the page top. */
    const val LECTURE_HEADER_1_MM: Float = 10.8f
    const val LECTURE_HEADER_2_MM: Float = 20.3f
    /** Lecture header rules start this far from the left edge. */
    const val LECTURE_HEADER_X_MM: Float = 6.8f
    /** One legacy page unit (at 5.9 units/mm) expressed in points. */
    const val UNIT_TO_PT: Float = 72f / (5.9f * 25.4f)
    /** Lined margin line color. */
    val MARGIN_COLOR: Color = Color(0xFFF8BBD0)
    /** Planner checkbox / lecture header color. */
    val ACCENT_COLOR: Color = Color(0xFF90CAF9)
}

/**
 * Draws [template]'s pattern in PAGE coordinates (points; origin at the
 * page's top-left, y down). The caller applies the page transform — this
 * renderer never sees screen size or viewport size, so the pattern is
 * glued to the page at every pan and zoom. Only the part inside
 * [visible] (page coordinates) is generated, which keeps high-zoom frames
 * cheap; everything is vector, re-drawn crisply each frame. The pattern
 * is background only: no layer, never erasable.
 */
fun DrawScope.drawPagePattern(
    template: PageTemplate,
    pageWidth: Float,
    pageHeight: Float,
    visible: androidx.compose.ui.geometry.Rect
) {
    if (template == PageTemplate.BLANK) return
    val def = PaperTemplates.def(template)
    if (def.spacingMm <= 0f) return
    val spacing = def.spacingMm * PaperTemplates.PT_PER_MM
    val widthPt = def.strokeWidthPx * LegacyPaper.UNIT_TO_PT
    val pt = PaperTemplates.PT_PER_MM

    fun firstAtOrAfter(start: Float, step: Float, bound: Float): Float {
        var v = start
        if (v < bound && step > 0f) {
            val n = floor((bound - v) / step).toInt()
            if (n > 0) v += n * step
            while (v < bound) v += step
        }
        return v
    }

    when (def.kind) {
        PatternKind.BLANK -> Unit

        PatternKind.LINES -> {
            val top = def.topMarginMm * pt
            val firstLine = if (top > 0f) top else spacing
            var y = firstAtOrAfter(firstLine, spacing, visible.top)
            while (y <= pageHeight && y <= visible.bottom) {
                drawLine(
                    def.patternColor,
                    Offset(0f, y), Offset(pageWidth, y),
                    strokeWidth = widthPt
                )
                y += spacing
            }
            if (template == PageTemplate.LINED) {
                val mx = LegacyPaper.MARGIN_X_MM * pt
                drawLine(
                    LegacyPaper.MARGIN_COLOR,
                    Offset(mx, 0f), Offset(mx, pageHeight),
                    strokeWidth = widthPt
                )
            }
            if (template == PageTemplate.PLANNER) {
                val cx = LegacyPaper.PLANNER_COLUMN_MM * pt
                drawLine(
                    def.patternColor,
                    Offset(cx, 0f), Offset(cx, pageHeight),
                    strokeWidth = widthPt
                )
                val sizePt = LegacyPaper.CHECKBOX_SIZE_MM * pt
                val bx = LegacyPaper.CHECKBOX_X_MM * pt
                val lift = LegacyPaper.CHECKBOX_LIFT_MM * pt
                var ry = firstAtOrAfter(firstLine, spacing, visible.top - lift)
                while (ry <= pageHeight && ry - lift <= visible.bottom) {
                    val boxTop = ry - lift
                    if (boxTop + sizePt >= visible.top) {
                        drawRect(
                            color = LegacyPaper.ACCENT_COLOR,
                            topLeft = Offset(bx, boxTop),
                            size = Size(sizePt, sizePt),
                            style = Stroke(width = widthPt)
                        )
                    }
                    ry += spacing
                }
            }
            if (template == PageTemplate.LECTURE) {
                val fromX = LegacyPaper.LECTURE_HEADER_X_MM * pt
                for (headerMm in listOf(
                    LegacyPaper.LECTURE_HEADER_1_MM,
                    LegacyPaper.LECTURE_HEADER_2_MM
                )) {
                    val hy = headerMm * pt
                    drawLine(
                        LegacyPaper.ACCENT_COLOR,
                        Offset(fromX, hy), Offset(pageWidth, hy),
                        strokeWidth = widthPt
                    )
                }
            }
        }

        PatternKind.GRID -> {
            var x = firstAtOrAfter(spacing, spacing, visible.left)
            while (x < pageWidth && x <= visible.right) {
                drawLine(
                    def.patternColor,
                    Offset(x, 0f), Offset(x, pageHeight),
                    strokeWidth = widthPt
                )
                x += spacing
            }
            var y = firstAtOrAfter(spacing, spacing, visible.top)
            while (y < pageHeight && y <= visible.bottom) {
                drawLine(
                    def.patternColor,
                    Offset(0f, y), Offset(pageWidth, y),
                    strokeWidth = widthPt
                )
                y += spacing
            }
        }

        PatternKind.DOTS -> {
            val radius = def.strokeWidthPx * LegacyPaper.UNIT_TO_PT
            var gx = firstAtOrAfter(spacing, spacing, visible.left)
            while (gx < pageWidth && gx <= visible.right) {
                var gy = firstAtOrAfter(spacing, spacing, visible.top)
                while (gy < pageHeight && gy <= visible.bottom) {
                    drawCircle(
                        color = def.patternColor,
                        radius = radius,
                        center = Offset(gx, gy)
                    )
                    gy += spacing
                }
                gx += spacing
            }
        }
    }
}
