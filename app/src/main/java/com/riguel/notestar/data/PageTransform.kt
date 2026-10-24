package com.riguel.notestar.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

/**
 * The single source of truth for the normal-note editor's view (v0.14.1):
 * one matrix M = translate(offset) x scale(scale), owned here and read by
 * every layer — paper template, highlights, ink, canvas text, selection
 * frame, handles and the editing field. No layer keeps its own copy of
 * zoom or pan, and no layer recomputes a transform from zoom alone: a
 * pinch changes scale AND offset together (focal-point pan), and every
 * reader sees both, in the same frame, because both are state on this
 * one object. Screen = M(page) = offset + page * scale; page = M^-1.
 */
class PageViewState {
    var scale by mutableStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    fun toScreen(page: Offset): Offset =
        PageTransform.toScreen(page, scale, offset)

    fun toPage(screen: Offset): Offset =
        PageTransform.toPage(screen, scale, offset)
}
