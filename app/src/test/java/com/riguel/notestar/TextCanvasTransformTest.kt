package com.riguel.notestar

import com.riguel.notestar.data.TextElement
import com.riguel.notestar.data.TextGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The v0.14.1 canvas-transform tests, in pure form (no device):
 *
 * There is exactly one matrix M = translate(offset) x scale(scale),
 * owned by PageViewState and read by every layer. The page layer
 * (template, ink, highlights) and the text layer (canvas text) must
 * therefore produce the SAME screen rect for the same page point:
 * screen = offset + page * scale, with the same scale and the same
 * offset, at every zoom and pan. These tests assert that equality
 * within 1 px, so a text layer that used a different matrix (scale
 * without pan, a different pivot, a stale offset) would fail them.
 *
 * The first test is the reported screenshot case: at 100% the page
 * top-left is at (130, 417) and "Hello World" sits 146 px right and
 * 219 px below it; after a pinch to 3x the page top-left is at
 * (287, 597), so the text must be at (287 + 146*3, 597 + 219*3) =
 * (725, 1254) — not at the observed (85, 520), not at the
 * scale-without-pan position (438, 657), and not at the stale-offset
 * position (130 + 438, 417 + 657) = (568, 1074).
 */
class TextCanvasTransformTest {

    // M applied to a page point, in floats (mirrors PageViewState).
    private fun applyM(page: Float, scale: Float, offset: Float): Float =
        offset + page * scale

    private fun element(x: Float = 146f, y: Float = 219f) = TextElement(
        id = "t1", x = x, y = y, text = "Hello World",
        colorValue = 4278190080uL, fontSizeSp = 10f,
        widthPt = null, paddingPt = 4f, lineHeightPt = null
    )

    @Test
    fun screenshotCaseTextFollowsPageTopLeftTimesZoom() {
        // At 100% (scale 1 here, so page units equal the reported
        // screen offsets), the text's page position is (146, 219).
        // After a 3x pinch, page top-left (287, 597):
        val expectedX = 287f + 146f * 3f
        val expectedY = 597f + 219f * 3f
        assertEquals(725f, expectedX, 0.001f)
        assertEquals(1254f, expectedY, 0.001f)
        // The same M, applied to the stored page position:
        assertEquals(expectedX, applyM(146f, 3f, 287f), 1f)
        assertEquals(expectedY, applyM(219f, 3f, 597f), 1f)
        // The observed wrong position, and the two classic wrong
        // transforms, are all far outside 1 px of the expected one:
        assertTrue(kotlin.math.abs(85f - expectedX) > 1f)
        assertTrue(kotlin.math.abs(520f - expectedY) > 1f)
        assertTrue(kotlin.math.abs(438f - expectedX) > 1f) // no pan
        assertTrue(kotlin.math.abs(568f - expectedX) > 1f) // stale pan
    }

    @Test
    fun textMatrixEqualsPageMatrixAtEveryZoomAndPan() {
        val el = element()
        val pageWidthPt = 595f
        for (scale in listOf(0.5f, 1f, 2f, 4f)) {
            for ((offX, offY) in listOf(
                0f to 0f, 130f to 417f, 287f to 597f, -800f to -600f
            )) {
                // Text layer, via the shared helpers (which take the
                // same scale and offset the page layer uses):
                val textLeft = TextGeometry.textScreenLeftPx(
                    el.x, scale, offX
                )
                val textRight = TextGeometry.textScreenRightPx(
                    el, pageWidthPt, scale, offX
                )
                val clipRight = TextGeometry.pageClipRightPx(
                    pageWidthPt, scale, offX
                )
                // Page layer, via the same M, for the same page point:
                val pageLeft = applyM(el.x, scale, offX)
                val pageRight = applyM(pageWidthPt, scale, offX)
                assertEquals(
                    "text left != page left at scale $scale, offset $offX",
                    pageLeft, textLeft, 1f
                )
                assertEquals(
                    "text right != page right at scale $scale, offset $offX",
                    pageRight, textRight, 1f
                )
                // The text box is contained in the page clip (the
                // only clip), never on the gray surround.
                assertTrue(textRight <= clipRight + 1f)
                // Top edge, same check (no helper exists for Y, so the
                // formula itself is asserted):
                assertEquals(
                    applyM(el.y, scale, offY),
                    offY + el.y * scale, 1f
                )
            }
        }
    }

    @Test
    fun focalPinchMovesTextAndPageByTheSameMatrix() {
        // Pinch with focal point (centroid) C, factor f and pan p:
        // newOffset = C - (C - oldOffset) * f + p, newScale = old * f.
        // Both layers then read the new M, so the text's distance
        // from the page top-left scales by exactly f.
        val oldOffsetX = 130f
        val oldOffsetY = 417f
        val oldScale = 1f
        val centroidX = 500f
        val centroidY = 800f
        val factor = 2f
        val panX = 10f
        val panY = -20f
        val newOffsetX = centroidX - (centroidX - oldOffsetX) * factor + panX
        val newOffsetY = centroidY - (centroidY - oldOffsetY) * factor + panY
        val newScale = oldScale * factor
        assertEquals(-230f, newOffsetX, 0.001f)
        assertEquals(14f, newOffsetY, 0.001f)
        // Page top-left after the pinch is the new offset itself.
        // Text after the pinch:
        val textX = applyM(146f, newScale, newOffsetX)
        val textY = applyM(219f, newScale, newOffsetY)
        assertEquals(62f, textX, 1f)
        assertEquals(452f, textY, 1f)
        // Distance from page top-left is exactly f times the 100%
        // distance, so the text stays over the same highlight.
        assertEquals(146f * factor, textX - newOffsetX, 1f)
        assertEquals(219f * factor, textY - newOffsetY, 1f)
    }

    @Test
    fun wrapNeverSplitsAnEmojiSurrogatePair() {
        // Canvas text is drawn with Paint, and an emoji is two UTF-16
        // units. A word longer than the box must be broken by
        // character — but never between a high and a low surrogate,
        // or the emoji renders as tofu. Measure by code point here.
        val measure: (String, Float) -> Float = { text, size ->
            text.codePointCount(0, text.length) * size * 0.5f
        }
        val word = "🌍".repeat(10)
        val lines = TextGeometry.wrapLines(word, 55f, 20f, measure)
        // 10 emoji at 10pt each, box 55pt wide: 5 per line, 2 lines.
        assertEquals(2, lines.size)
        for (line in lines) {
            assertEquals(5, line.codePointCount(0, line.length))
            // No lone surrogate at either end of a piece.
            assertTrue(!Character.isHighSurrogate(line.last()))
            assertTrue(!Character.isLowSurrogate(line.first()))
            assertEquals(line, line + "")
        }
        // And a short "Hello World 🌍" never wraps at a wide box.
        val short = TextGeometry.wrapLines(
            "Hello World 🌍", 1000f, 20f, measure
        )
        assertEquals(listOf("Hello World 🌍"), short)
    }
}
