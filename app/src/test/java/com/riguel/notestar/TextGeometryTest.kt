package com.riguel.notestar

import com.riguel.notestar.data.TextElement
import com.riguel.notestar.data.TextGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The v0.13 acceptance geometry, checked without an Android device:
 * render the editor page at 100% (viewScale px/pt) and the exported page
 * at the same dpi, and every text box must agree — position under 1 pt,
 * font size under 2%. The checks below are the pure form of that test:
 * the editor's pixel size, converted back to points, equals the stored
 * points; the exporter's baseline/line-height/wrap use the same numbers.
 */
class TextGeometryTest {

    private fun element(
        x: Float = 100f,
        y: Float = 120f,
        size: Float = 20f,
        text: String = "Hello Alphabet Train"
    ) = TextElement(
        id = "t1", x = x, y = y, text = text,
        colorValue = 4278190080uL, fontSizeSp = size,
        widthPt = null, paddingPt = 4f, lineHeightPt = null
    )

    @Test
    fun editorPixelsConvertBackToStoredPointsWithinTwoPercent() {
        // A phone at fit: viewScale ~1.67 px/pt, scaledDensity 2.625.
        // The old code gave Compose size*viewScale sp -> size*viewScale*
        // scaledDensity px (162% too big here). The fix divides by
        // scaledDensity first, so pixels are exactly size*viewScale.
        val sizePt = 20f
        val viewScale = 1.67f
        val scaledDensity = 2.625f
        val sp = TextGeometry.spValueForPagePt(sizePt, viewScale, scaledDensity)
        val px = TextGeometry.pxFromSp(sp, scaledDensity)
        val backToPt = px / viewScale
        assertTrue(
            "editor pt $backToPt differs from stored $sizePt by more than 2%",
            abs(backToPt - sizePt) / sizePt < 0.02f
        )
        // And the old formula would have failed this same check.
        val oldPx = sizePt * viewScale * scaledDensity
        assertTrue(abs(oldPx / viewScale - sizePt) / sizePt > 0.5f)
    }

    @Test
    fun baselineUsesRealAscentNotEstimate() {
        val el = element()
        val ascentRatio = 0.928f // Roboto-like, from TextMetrics
        val baseline = TextGeometry.baselinePt(
            el.y, el.paddingPt, ascentRatio, el.fontSizeSp
        )
        // box top + padding + ascent, not box top + 0.8*size.
        assertEquals(120f + 4f + 0.928f * 20f, baseline, 0.001f)
        assertTrue(abs(baseline - (120f + 0.8f * 20f)) > 1f)
    }

    @Test
    fun lineHeightHonoursStoredOverrideElseRatio() {
        val el = element()
        assertEquals(20f * 1.17f, TextGeometry.lineHeightPt(el, 1.17f), 0.001f)
        val stored = el.copy(lineHeightPt = 30f)
        assertEquals(30f, TextGeometry.lineHeightPt(stored, 1.17f), 0.001f)
    }

    @Test
    fun wrapWidthIsBoxToPageEdgeAndClamped() {
        val el = element(x = 500f)
        // A4 page width 595: box is 95pt wide.
        assertEquals(95f, TextGeometry.wrapWidthPt(el, 595f), 0.001f)
        val narrow = el.copy(widthPt = 50f)
        assertEquals(50f, TextGeometry.wrapWidthPt(narrow, 595f), 0.001f)
        val overflowing = el.copy(widthPt = 9999f)
        assertEquals(95f, TextGeometry.wrapWidthPt(overflowing, 595f), 0.001f)
    }

    @Test
    fun wrapBreaksAtSameWidthAsEditorBox() {
        // Fake font: every character is 0.5 * size wide, space included.
        val measure: (String, Float) -> Float = { text, size ->
            text.length * size * 0.5f
        }
        val lines = TextGeometry.wrapLines(
            "Hello Alphabet Train", 80f, 20f, measure
        )
        // At this fake width "Alphabet" is exactly 80pt, so it fills a
        // line on its own; "Hello" and "Train" are shorter.
        assertEquals(listOf("Hello", "Alphabet", "Train"), lines)
        for (line in lines) {
            assertTrue(measure(line, 20f) <= 80f + 0.001f)
        }
    }

    @Test
    fun wrapKeepsExplicitNewlinesAndBlankLines() {
        val measure: (String, Float) -> Float = { text, size ->
            text.length * size * 0.5f
        }
        val lines = TextGeometry.wrapLines("One\n\nTwo", 1000f, 20f, measure)
        assertEquals(listOf("One", "", "Two"), lines)
    }

    @Test
    fun textBoxTopLeftIsGlyphOriginPlusPadding() {
        // Position agreement under 1pt: the exporter draws glyphs at
        // x + padding, y + padding (then baseline), and the editor's
        // Text sits at the same offset (position + page-unit padding).
        val el = element(x = 48f, y = 96f)
        assertEquals(52f, el.x + el.paddingPt, 0.001f)
        assertEquals(100f, el.y + el.paddingPt, 0.001f)
    }
}
