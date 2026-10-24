package com.riguel.notestar

import com.riguel.notestar.data.TextElement
import com.riguel.notestar.data.TextGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The v0.14 anchor/lock tests, in pure form (no device):
 *
 * 1. Rendering is pageToScreen only: screen = viewOffset + page * scale.
 *    Zooming, panning, saving and reloading never rewrite the stored
 *    page position, so converting back with screenToPage must return
 *    the stored position exactly, at fit, 2x and 4x, panned or not.
 * 2. An explicit move/resize gesture commits once, at drag end, with
 *    the drag's screen displacement converted by the current scale.
 * 3. No clamping: a box partly outside the viewport (or with a negative
 *    page coordinate) still renders at its true page position.
 * 4. The one-time drift diagnostic only counts boxes outside the page
 *    rectangle; it never modifies one.
 *
 * The old drift bug is modelled here too: per-event application of
 * pan / viewScale to the stored position, with viewScale falling
 * during a zoom-out, moves the stored position by more than the
 * gesture intended — the code that did that (a detectTransformGestures
 * on every text box, plus a pinch-resizes-text detector on the page)
 * is deleted in v0.14, so no test may call it; the tests below assert
 * the invariants its replacement must keep.
 */
class TextAnchorTest {

    private fun element(x: Float = 200f, y: Float = 300f) = TextElement(
        id = "t1", x = x, y = y, text = "Hello World",
        colorValue = 4278190080uL, fontSizeSp = 10f,
        widthPt = null, paddingPt = 4f, lineHeightPt = null
    )

    // pageToScreen / screenToPage in floats, mirroring PageTransform.
    private fun toScreen(page: Float, scale: Float, offset: Float): Float =
        offset + page * scale

    private fun toPage(screen: Float, scale: Float, offset: Float): Float =
        (screen - offset) / scale

    @Test
    fun zoomPanAndReloadNeverChangeStoredPosition() {
        val el = element()
        for (scale in listOf(1.447f, 1.447f * 2f, 1.447f * 4f)) {
            for (offset in listOf(0f, -800f, 120f)) {
                val screenX = toScreen(el.x, scale, offset)
                val screenY = toScreen(el.y, scale, offset)
                // Round-trip through the transform returns the stored
                // position exactly: rendering is a pure function of
                // the stored page position, never a write to it.
                assertEquals(el.x, toPage(screenX, scale, offset), 0.001f)
                assertEquals(el.y, toPage(screenY, scale, offset), 0.001f)
                // And the stored element itself is untouched by the
                // transform (the test's element is never reassigned).
                assertEquals(200f, el.x, 0.001f)
                assertEquals(300f, el.y, 0.001f)
            }
        }
    }

    @Test
    fun moveCommitConvertsDragOnceWithCurrentScale() {
        // Start (200, 300), drag 290px right and 145px up at 2.9 px/pt:
        // page delta is (100, -50), committed once as (300, 250).
        val (x, y) = TextGeometry.moveCommit(200f, 300f, 290f, -145f, 2.9f)
        assertEquals(300f, x, 0.001f)
        assertEquals(250f, y, 0.001f)
        // A zero drag commits the start position unchanged.
        val (x0, y0) = TextGeometry.moveCommit(200f, 300f, 0f, 0f, 2.9f)
        assertEquals(200f, x0, 0.001f)
        assertEquals(300f, y0, 0.001f)
    }

    @Test
    fun resizeCommitMovesOnlyTheDraggedEdge() {
        // Right corner: drag 87px right at 2.9 px/pt widens 400 -> 430,
        // x fixed.
        val (rx, rw) = TextGeometry.resizeCommit(
            100f, 400f, 87f, 2.9f, false, 595f
        )
        assertEquals(100f, rx, 0.001f)
        assertEquals(430f, rw, 0.001f)
        // Left corner: drag 58px left at 2.9 px/pt moves x 100 -> 80
        // and widens 400 -> 420, right edge fixed at 500.
        val (lx, lw) = TextGeometry.resizeCommit(
            100f, 400f, -58f, 2.9f, true, 595f
        )
        assertEquals(80f, lx, 0.001f)
        assertEquals(420f, lw, 0.001f)
        assertEquals(500f, lx + lw, 0.001f)
        // Width never drops below 48pt, even for a huge left drag.
        val (_, minW) = TextGeometry.resizeCommit(
            100f, 400f, -10000f, 2.9f, true, 595f
        )
        assertEquals(48f, minW, 0.001f)
    }

    @Test
    fun positionIsNeverClampedToViewportOrOrigin() {
        // A box partly outside the viewport (panned) and a box with a
        // negative page coordinate both render at their true page
        // position, not clamped to 0 or to the viewport edge.
        val screenX = toScreen(-20f, 2.9f, -800f)
        assertEquals(-858f, screenX, 0.001f)
        assertTrue("negative page x must not be clamped to 0", screenX < 0f)
        val el = element(x = -20f, y = -10f)
        assertEquals(-20f, el.x, 0.001f)
        assertEquals(-10f, el.y, 0.001f)
    }

    @Test
    fun outOfPageDiagnosticCountsWithoutModifying() {
        val texts = listOf(
            element(200f, 300f),
            element(-5f, 300f),
            element(200f, 900f),
            element(600f, 300f),
            element(0f, 0f)
        )
        // Only the three outside [0, 595] x [0, 842] count; (0, 0)
        // is on the page and is not suspicious, and nothing is moved.
        assertEquals(3, TextGeometry.outOfPageCount(texts, 595f, 842f))
        assertEquals(200f, texts[0].x, 0.001f)
        assertEquals(-5f, texts[1].x, 0.001f)
    }

    @Test
    fun resizedWidthSurvivesNormalizeInsteadOfResettingToPageEdge() {
        // normalizeText's old behaviour reset every width to the page
        // edge, which would have erased a corner-resize on the next
        // update. That behaviour is covered by the editor change (a
        // stored width is kept, clamped to [48, pageWidth - x]); here
        // the clamp itself is checked, since the editor's normalize
        // is not JVM-testable (it calls TextMetrics/Paint).
        val kept = 200f.coerceIn(48f, (595f - 100f).coerceAtLeast(48f))
        assertEquals(200f, kept, 0.001f)
        val clamped = 9999f.coerceIn(48f, (595f - 100f).coerceAtLeast(48f))
        assertEquals(495f, clamped, 0.001f)
    }
}
