package com.riguel.notestar

import com.riguel.notestar.data.BoxGeom
import com.riguel.notestar.data.clampVisibleCoord
import com.riguel.notestar.data.dragPreviewCoord
import com.riguel.notestar.data.resizeLeftPreview
import com.riguel.notestar.data.resizeRightPreview
import com.riguel.notestar.data.screenToPageCoord
import com.riguel.notestar.data.snapAxisCoord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * v0.15.4 live-drag tests, in pure form (no device). They simulate
 * the editor's drag exactly: the grab is taken at the slop-pass
 * touch, and every later preview is touchPage - grab, so:
 *
 * 1. At the slop-pass touch itself the preview equals the stored
 *    position — no jump on the first move event.
 * 2. Dragging from screen point A to screen point B shifts the
 *    box's top-left by exactly (B - A) / zoom in page units, at
 *    1x, 2x and 4x, with pan — the box follows the finger 1:1.
 * 3. Snap picks the nearest guide within threshold and moves the
 *    winning edge/center exactly onto it; nothing snaps outside
 *    the threshold.
 * 4. The 24dp-visible clamp keeps at least that much of the box
 *    inside the page on each axis.
 * 5. Resize previews move only the dragged edge, anchored on the
 *    opposite corner, clamped to the page.
 */
class TextDragLogicTest {

    private val eps = 0.001f

    @Test
    fun noJumpOnFirstMoveEvent() {
        // Stored box at (200, 300); the slop-pass touch lands on
        // its page point (260, 340) at 2x with pan (40, 80).
        val scale = 2f
        val panX = 40f
        val panY = 80f
        val originX = 200f
        val originY = 300f
        // Screen point of the slop-pass touch: M(origin + grab).
        val grabX = 60f
        val grabY = 40f
        val touchAX = panX + (originX + grabX) * scale
        val touchAY = panY + (originY + grabY) * scale
        // The editor stores grab = touchPage - origin at slop-pass.
        val storedGrabX = screenToPageCoord(touchAX, panX, scale) - originX
        val storedGrabY = screenToPageCoord(touchAY, panY, scale) - originY
        assertEquals(grabX, storedGrabX, eps)
        assertEquals(grabY, storedGrabY, eps)
        // The first preview (at that same touch) is the origin.
        assertEquals(
            originX,
            dragPreviewCoord(
                screenToPageCoord(touchAX, panX, scale), storedGrabX
            ),
            eps
        )
        assertEquals(
            originY,
            dragPreviewCoord(
                screenToPageCoord(touchAY, panY, scale), storedGrabY
            ),
            eps
        )
    }

    @Test
    fun dragShiftsByScreenDeltaOverZoom() {
        val originX = 200f
        val originY = 300f
        for (scale in listOf(1f, 2f, 4f)) {
            for ((panX, panY) in listOf(0f to 0f, 40f to 80f, -120f to 60f)) {
                // Finger starts on the box (grab 60, 40), then moves
                // by (+290, -145) screen px.
                val grabX = 60f
                val grabY = 40f
                val touchAX = panX + (originX + grabX) * scale
                val touchAY = panY + (originY + grabY) * scale
                val touchBX = touchAX + 290f
                val touchBY = touchAY - 145f
                val storedGrabX =
                    screenToPageCoord(touchAX, panX, scale) - originX
                val storedGrabY =
                    screenToPageCoord(touchAY, panY, scale) - originY
                val previewX = dragPreviewCoord(
                    screenToPageCoord(touchBX, panX, scale), storedGrabX
                )
                val previewY = dragPreviewCoord(
                    screenToPageCoord(touchBY, panY, scale), storedGrabY
                )
                assertEquals(
                    "scale $scale pan ($panX, $panY) x",
                    290f / scale, previewX - originX, eps
                )
                assertEquals(
                    "scale $scale pan ($panX, $panY) y",
                    -145f / scale, previewY - originY, eps
                )
            }
        }
    }

    @Test
    fun snapPicksNearestGuideWithinThreshold() {
        // Box at x = 100, width 50: left 100, center 125, right 150.
        val (snapped, guide) = snapAxisCoord(
            100f, 50f, listOf(0f, 297.5f, 595f, 123f, 130f), 6f
        )
        // Center 125 is 2 from guide 123 (nearest); 130 is 5 away.
        assertEquals(98f, snapped, eps)
        assertEquals(123f, guide)
        // Nothing within threshold: position and guide unchanged.
        val (plain, none) = snapAxisCoord(
            100f, 50f, listOf(0f, 595f), 6f
        )
        assertEquals(100f, plain, eps)
        assertNull(none)
    }

    @Test
    fun clampKeeps24dpVisible() {
        val page = 595f
        val visibleMin = 24f
        // Box 100 wide dragged far left: at most 76 off-page.
        assertEquals(-76f, clampVisibleCoord(-200f, 100f, page, visibleMin), eps)
        // Dragged far right: at most page - 24.
        assertEquals(571f, clampVisibleCoord(900f, 100f, page, visibleMin), eps)
        // Inside: untouched.
        assertEquals(200f, clampVisibleCoord(200f, 100f, page, visibleMin), eps)
    }

    @Test
    fun resizeMovesOnlyDraggedEdge() {
        // Right corner: left stays 200, width follows the edge.
        assertEquals(
            150f, resizeRightPreview(200f, 100f, 350f, 595f), eps
        )
        // Right corner clamped to the page edge (min width 48).
        assertEquals(
            395f, resizeRightPreview(200f, 100f, 900f, 595f), eps
        )
        // Left corner: right edge stays 300, x follows the finger.
        val (lx, lw) = resizeLeftPreview(200f, 100f, 150f, 595f)
        assertEquals(150f, lx, eps)
        assertEquals(150f, lw, eps)
        // Left corner cannot cross the right edge minus 48.
        val (cx, cw) = resizeLeftPreview(200f, 100f, 290f, 595f)
        assertEquals(252f, cx, eps)
        assertEquals(48f, cw, eps)
    }

    @Test
    fun guidesIncludePageAndOtherBoxes() {
        // Sanity: another box contributes its edges/center, and the
        // dragged box itself is the caller's job to exclude.
        val others = listOf(BoxGeom(100f, 100f, 50f, 40f))
        val guides = others.flatMap {
            listOf(it.x, it.x + it.w / 2f, it.x + it.w)
        }
        assertEquals(listOf(100f, 125f, 150f), guides)
    }
}
