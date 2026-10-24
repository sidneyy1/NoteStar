package com.riguel.notestar.data

import androidx.compose.ui.geometry.Offset

/**
 * The one coordinate bridge for normal notes. Page space is PDF points
 * (origin at the page's top-left, y down); screen space is pixels. There is
 * exactly one uniform scale — x and y are never scaled independently.
 *
 * The editor's renderer and touch input go through these helpers; the PDF
 * exporter works in page space directly (scale 1, offset zero), so the
 * exported geometry is the screen geometry at 100%.
 */
object PageTransform {

    fun toScreen(page: Offset, scale: Float, offset: Offset): Offset =
        Offset(offset.x + page.x * scale, offset.y + page.y * scale)

    fun toPage(screen: Offset, scale: Float, offset: Offset): Offset =
        Offset((screen.x - offset.x) / scale, (screen.y - offset.y) / scale)
}
