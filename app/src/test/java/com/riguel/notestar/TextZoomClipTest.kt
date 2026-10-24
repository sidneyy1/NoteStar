package com.riguel.notestar

import com.riguel.notestar.data.TextElement
import com.riguel.notestar.data.TextGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The v0.13.1 zoom-clip regression test, in pure form (no device):
 * the editor's text layer is a page-sized box positioned by the shared
 * PageTransform (viewOffset + x * viewScale), clipped only to the page
 * rectangle. The bug was that the box used size() instead of
 * requiredSize(), so Compose coerced it to the viewport's constraints:
 * at zoom above fit, the box was viewport-sized and clipToBounds cut
 * text on the right, then hid it entirely once x * viewScale exceeded
 * the viewport width — while the Canvas ink/highlight, drawn with the
 * full transform, stayed visible.
 *
 * These checks run the same transform the editor now uses at zoom
 * factors 1x, 2x, 3.37x and 4x of fit, with a panned (negative)
 * viewOffset, and assert: the rendered box's left and right edges equal
 * the stored box transformed by the scale within 1 px, and the box is
 * contained in the page clip (never clipped to a smaller rectangle).
 * A viewport-sized clip (the old calculation) is also shown to fail
 * at 3.37x and 4x, so the test would have caught the original bug.
 */
class TextZoomClipTest {

    private val pageWidthPt = 595f // A4 portrait
    private val pageHeightPt = 842f
    private val fitScale = 1.447f // phone portrait at fit, px/pt
    private val canvasWidthPx = 945f // viewport width, px
    private val viewOffsetXPx = -800f // panned right, px

    private fun element() = TextElement(
        id = "t1", x = 150f, y = 200f, text = "Hello World",
        colorValue = 4278190080uL, fontSizeSp = 10f,
        widthPt = null, paddingPt = 4f, lineHeightPt = null
    )

    @Test
    fun textBoxEdgesEqualStoredBoxTimesScaleAtEveryZoom() {
        val el = element()
        for (factor in listOf(1f, 2f, 3.37f, 4f)) {
            val viewScale = fitScale * factor
            val left = TextGeometry.textScreenLeftPx(
                el.x, viewScale, viewOffsetXPx
            )
            val right = TextGeometry.textScreenRightPx(
                el, pageWidthPt, viewScale, viewOffsetXPx
            )
            val clipRight = TextGeometry.pageClipRightPx(
                pageWidthPt, viewScale, viewOffsetXPx
            )
            // Left edge: stored x * scale, within 1 px.
            assertEquals(
                "left edge at ${factor}x",
                viewOffsetXPx + el.x * viewScale, left, 1f
            )
            // Right edge: stored box (to page edge) * scale, within 1 px.
            assertEquals(
                "right edge at ${factor}x",
                viewOffsetXPx + pageWidthPt * viewScale, right, 1f
            )
            // Contained in the page clip, never in a smaller rectangle.
            assertTrue(
                "box extends past page clip at ${factor}x: $right > $clipRight",
                right <= clipRight + 1f
            )
            // The box is on screen (its screen position overlaps the
            // viewport) at every zoom, including 4x.
            assertTrue(
                "box off screen at ${factor}x: left $left, right $right",
                left < canvasWidthPx && right > 0f
            )
        }
    }

    @Test
    fun viewportSizedClipWouldCutTextAtHighZoom() {
        // The old calculation: the text box was coerced to the viewport
        // width, so its clip right edge was viewOffset + canvas width.
        // At 3.37x and 4x that edge falls inside the text box, cutting
        // it on the right; at 4x the box's local offset (x * viewScale)
        // exceeds the viewport width, so the box is hidden entirely.
        // (x = 200pt here so the 4x local offset clears the viewport;
        // the first test's x = 150pt box is cut but not yet hidden.)
        val el = element().copy(x = 200f)
        for (factor in listOf(3.37f, 4f)) {
            val viewScale = fitScale * factor
            val oldClipRight = viewOffsetXPx + canvasWidthPx
            val right = TextGeometry.textScreenRightPx(
                el, pageWidthPt, viewScale, viewOffsetXPx
            )
            assertTrue(
                "old clip should cut at ${factor}x",
                oldClipRight < right - 1f
            )
            val localOffset = el.x * viewScale
            if (factor == 4f) {
                assertTrue(
                    "old clip hides the box at 4x: local offset $localOffset " +
                        "exceeds viewport $canvasWidthPx",
                    localOffset > canvasWidthPx
                )
            }
        }
    }

    @Test
    fun pageBoxSizeIsPageTimesScaleNotViewport() {
        val (w, h) = TextGeometry.pageSizePx(
            pageWidthPt, pageHeightPt, fitScale * 4f
        )
        assertEquals(pageWidthPt * fitScale * 4f, w, 0.001f)
        assertEquals(pageHeightPt * fitScale * 4f, h, 0.001f)
        assertTrue("page box must be wider than the viewport at 4x", w > canvasWidthPx)
        assertTrue(abs(w - canvasWidthPx) > 1f)
    }
}
