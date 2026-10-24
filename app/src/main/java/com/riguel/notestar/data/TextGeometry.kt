package com.riguel.notestar.data

/**
 * Pure page-geometry helpers shared by the normal-note editor and the
 * normal-note PDF exporter. Nothing here touches Android, so the JVM unit
 * tests can check the exact numbers both renderers must agree on.
 *
 * The v0.13 text bug, in one line: the editor rendered a stored point size
 * as `(size * viewScale).sp`, and Compose turns sp into pixels by
 * multiplying by scaledDensity (density x fontScale), so on screen the text
 * was scaledDensity times larger than the stored points — while the
 * exporter wrote the stored size straight to the PDF. The exporter was
 * right; the editor was wrong. The fix is [spValueForPagePt]: divide the
 * wanted pixel size back by scaledDensity before handing it to Compose,
 * so the pixels on screen are exactly `sizePt * viewScale`.
 */
object TextGeometry {

    /** Fallback ratios, used only when real font metrics are unavailable. */
    const val FALLBACK_ASCENT_RATIO: Float = 0.8f
    const val FALLBACK_LINE_HEIGHT_RATIO: Float = 1.25f

    /**
     * The sp value Compose must be given so the glyphs land at
     * `fontSizePt * viewScalePxPerPt` pixels. Inverse of [pxFromSp].
     */
    fun spValueForPagePt(
        fontSizePt: Float,
        viewScalePxPerPt: Float,
        scaledDensity: Float
    ): Float {
        require(scaledDensity > 0f) { "scaledDensity must be positive" }
        return fontSizePt * viewScalePxPerPt / scaledDensity
    }

    /** Pixels Compose produces for an sp value at this scaledDensity. */
    fun pxFromSp(sp: Float, scaledDensity: Float): Float = sp * scaledDensity

    /** Line height in points: the stored override, else the font's ratio. */
    fun lineHeightPt(element: TextElement, lineHeightRatio: Float): Float =
        element.lineHeightPt ?: (element.fontSizeSp * lineHeightRatio)

    /**
     * Baseline of the first line, in page points, y-down from the page top.
     * [boxTopPt] is the box's stored top edge; glyphs start [paddingPt]
     * below it, and the first baseline sits one ascent below the glyph top.
     * The ascent ratio must come from the font's real metrics (TextMetrics),
     * never from the old 0.8em estimate.
     */
    fun baselinePt(
        boxTopPt: Float,
        paddingPt: Float,
        ascentRatio: Float,
        fontSizePt: Float
    ): Float = boxTopPt + paddingPt + ascentRatio * fontSizePt

    /**
     * Page size in screen pixels at [viewScale] (pixels per point).
     * The editor's text layer must be exactly this big: it is the page
     * rectangle in screen space, and the only clip text may ever see.
     */
    fun pageSizePx(
        pageWidthPt: Float,
        pageHeightPt: Float,
        viewScale: Float
    ): Pair<Float, Float> =
        Pair(pageWidthPt * viewScale, pageHeightPt * viewScale)

    /**
     * Screen x of a text box's left edge: the shared PageTransform
     * (uniform scale + pan), nothing else. The stored box is in page
     * points, so its on-screen left edge is viewOffset + x * viewScale.
     */
    fun textScreenLeftPx(
        xPt: Float,
        viewScale: Float,
        viewOffsetXPx: Float
    ): Float = viewOffsetXPx + xPt * viewScale

    /**
     * Screen x of a text box's right edge, same transform: left edge +
     * wrap width in points * viewScale. For a box that runs to the page
     * edge this is exactly the page's right edge in screen space, so
     * the box is contained in the page clip at every zoom — it is
     * never clipped to a smaller rectangle.
     */
    fun textScreenRightPx(
        element: TextElement,
        pageWidthPt: Float,
        viewScale: Float,
        viewOffsetXPx: Float
    ): Float = viewOffsetXPx +
        (element.x + wrapWidthPt(element, pageWidthPt)) * viewScale

    /** Screen x of the page clip's right edge, same transform. */
    fun pageClipRightPx(
        pageWidthPt: Float,
        viewScale: Float,
        viewOffsetXPx: Float
    ): Float = viewOffsetXPx + pageWidthPt * viewScale

    /** Wrap width in points for a box on a page of [pageWidthPt]. */
    fun wrapWidthPt(element: TextElement, pageWidthPt: Float): Float {
        val toEdge = (pageWidthPt - element.x).coerceAtLeast(10f)
        val stored = element.widthPt
        return if (stored == null) toEdge else stored.coerceIn(10f, toEdge)
    }

    /**
     * Greedy word wrap at [wrapWidthPt], measuring with [measureWidthPt]
     * (a function of (text, fontSizePt) -> width in points, e.g. the PDF
     * font's string width or Paint.measureText). The editor's Compose Text
     * wraps at the same box width, so line breaks must agree: words are
     * kept whole, a word longer than the box is broken by character, and
     * explicit "\n" in the source always breaks. Empty paragraphs produce
     * one empty line (a blank line keeps its line height).
     */
    fun wrapLines(
        text: String,
        wrapWidthPt: Float,
        fontSizePt: Float,
        measureWidthPt: (String, Float) -> Float
    ): List<String> {
        if (text.isEmpty()) return listOf("")
        val out = mutableListOf<String>()
        for (paragraph in text.split("\n")) {
            if (paragraph.isEmpty()) {
                out.add("")
                continue
            }
            val words = paragraph.split(" ")
            var line = ""
            fun lineWidth(candidate: String): Float =
                measureWidthPt(candidate, fontSizePt)
            for (word in words) {
                if (word.isEmpty()) {
                    // Preserve a double space's width inside a line.
                    if (line.isNotEmpty()) line += " "
                    continue
                }
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (lineWidth(candidate) <= wrapWidthPt || line.isEmpty()) {
                    // A single over-long word still has to be broken.
                    if (line.isEmpty() && lineWidth(word) > wrapWidthPt) {
                        var rest = word
                        while (rest.isNotEmpty()) {
                            var cut = rest.length
                            while (cut > 1 && lineWidth(rest.substring(0, cut)) > wrapWidthPt) {
                                cut--
                            }
                            out.add(rest.substring(0, cut))
                            rest = rest.substring(cut)
                        }
                        line = ""
                    } else {
                        line = candidate
                    }
                } else {
                    out.add(line)
                    line = word
                }
            }
            if (line.isNotEmpty() || words.all { it.isEmpty() }) out.add(line)
        }
        return out
    }
}
