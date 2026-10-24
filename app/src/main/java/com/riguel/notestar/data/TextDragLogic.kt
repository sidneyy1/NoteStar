package com.riguel.notestar.data

import kotlin.math.abs

/**
 * Pure, UI-free math for the Select-mode text drag (v0.15.4), kept
 * here — with no Compose or Android imports — so the unit test can
 * exercise the exact functions the editor uses. All values are in
 * page units (pt) unless a name says otherwise; screen px only ever
 * enter through [screenToPageCoord].
 *
 * The drag rule is absolute, not delta-based: on drag start the
 * editor stores grab = touchPage - boxTopLeft (with the touch taken
 * at the moment the slop is passed, so the slop distance is absorbed
 * into the grab and the box does not move at drag start). Every move
 * event then previews touchPage - grab, so the box stays glued to
 * the finger at every zoom, and the stored position is written only
 * on release. Cancelling just drops the preview — the stored
 * position was never touched, so there is nothing to restore.
 */

/** A text box's geometry, for guide computation. */
data class BoxGeom(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float
)

/** Screen px -> page units through the shared transform M. */
fun screenToPageCoord(
    screen: Float,
    offset: Float,
    scale: Float
): Float = (screen - offset) / scale

/** The drag preview: where the box's top-left sits for a touch. */
fun dragPreviewCoord(touchPage: Float, grab: Float): Float =
    touchPage - grab

/**
 * Clamp a dragged position so at least [visibleMin] pt of the box
 * stays inside the page on that axis. The box may hang off the
 * page, but never further than its size minus [visibleMin].
 */
fun clampVisibleCoord(
    pos: Float,
    size: Float,
    pageSize: Float,
    visibleMin: Float
): Float = pos.coerceIn(-(size - visibleMin), pageSize - visibleMin)

/**
 * Snap one axis of a dragged box to the nearest guide. The box's
 * leading edge, center, and trailing edge are all candidates; the
 * returned position makes the winning candidate sit exactly on its
 * guide, and the second value is that guide (null when nothing is
 * within [threshold]). Ties go to the first guide in list order.
 */
fun snapAxisCoord(
    pos: Float,
    size: Float,
    guides: List<Float>,
    threshold: Float
): Pair<Float, Float?> {
    var bestPos = pos
    var bestGuide: Float? = null
    var bestDist = threshold
    val candidates = listOf(
        pos to 0f,
        pos + size / 2f to size / 2f,
        pos + size to size
    )
    for (guide in guides) {
        for ((candidate, edgeOffset) in candidates) {
            val dist = abs(candidate - guide)
            if (dist <= bestDist) {
                bestDist = dist
                bestPos = guide - edgeOffset
                bestGuide = guide
            }
        }
    }
    return bestPos to bestGuide
}

/** Vertical guides: page edges + center + every other box's edges. */
fun verticalGuides(pageWidth: Float, others: List<BoxGeom>): List<Float> =
    listOf(0f, pageWidth / 2f, pageWidth) +
        others.flatMap { listOf(it.x, it.x + it.w / 2f, it.x + it.w) }

/** Horizontal guides: page edges + center + every other box's edges. */
fun horizontalGuides(pageHeight: Float, others: List<BoxGeom>): List<Float> =
    listOf(0f, pageHeight / 2f, pageHeight) +
        others.flatMap { listOf(it.y, it.y + it.h / 2f, it.y + it.h) }

/**
 * Resize preview for a right-corner drag: the dragged edge follows
 * the finger (minus the grab taken at slop-pass), the left edge
 * stays put, width clamped to [48, pageWidth - x].
 */
fun resizeRightPreview(
    startX: Float,
    startWidth: Float,
    edgePage: Float,
    pageWidth: Float
): Float = (edgePage - startX).coerceIn(
    48f, (pageWidth - startX).coerceAtLeast(48f)
)

/**
 * Resize preview for a left-corner drag: the dragged edge follows
 * the finger (minus the grab taken at slop-pass), the right edge
 * stays put. Returns (newX, newWidth).
 */
fun resizeLeftPreview(
    startX: Float,
    startWidth: Float,
    edgePage: Float,
    pageWidth: Float
): Pair<Float, Float> {
    val rightEdge = startX + startWidth
    val newX = edgePage.coerceIn(0f, rightEdge - 48f)
    val newWidth = (rightEdge - newX).coerceIn(
        48f, (pageWidth - newX).coerceAtLeast(48f)
    )
    return newX to newWidth
}
