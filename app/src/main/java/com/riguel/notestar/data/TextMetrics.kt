package com.riguel.notestar.data

import android.graphics.Paint
import android.graphics.Typeface

/**
 * Real font metrics for the fonts Compose renders on screen, via the same
 * underlying system Typefaces (sans-serif = Roboto on most Android
 * devices, serif = Noto Serif, monospace = Roboto Mono, cursive = the
 * system cursive font). Both the normal-note editor and the normal-note
 * exporter call this, so the first baseline and the line spacing are the
 * same numbers on both sides — the old exporter's fixed 0.8em ascent and
 * 1.25 line height were estimates, and they are what pushed exported text
 * above its highlight.
 *
 * Ratios are size-independent (metrics scale linearly with text size), so
 * they are cached per (family, bold, italic). On any failure the fallback
 * ratios from TextGeometry are returned, never a crash.
 */
object TextMetrics {

    data class Ratios(val ascent: Float, val lineHeight: Float)

    private val cache = mutableMapOf<String, Ratios>()

    fun ratios(
        fontFamilyName: String,
        bold: Boolean,
        italic: Boolean
    ): Ratios {
        val key = "$fontFamilyName|$bold|$italic"
        cache[key]?.let { return it }
        val result = try {
            val family = when (fontFamilyName) {
                TextFonts.SERIF -> "serif"
                TextFonts.MONO -> "monospace"
                TextFonts.CURSIVE -> "cursive"
                else -> "sans-serif"
            }
            val style = when {
                bold && italic -> Typeface.BOLD_ITALIC
                bold -> Typeface.BOLD
                italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            val paint = Paint().apply {
                typeface = Typeface.create(family, style)
                textSize = 100f
            }
            val fm = paint.fontMetrics
            val ascent = (-fm.ascent / 100f).coerceIn(0.5f, 1.1f)
            val lineHeight = ((fm.descent - fm.ascent + fm.leading) / 100f)
                .coerceIn(1.0f, 1.6f)
            Ratios(ascent, lineHeight)
        } catch (e: Exception) {
            Ratios(
                TextGeometry.FALLBACK_ASCENT_RATIO,
                TextGeometry.FALLBACK_LINE_HEIGHT_RATIO
            )
        }
        cache[key] = result
        return result
    }

    /** Ascent in points for a block of this font at [fontSizePt]. */
    fun ascentPt(element: TextElement): Float =
        ratios(element.fontFamilyName, element.bold, element.italic).ascent *
            element.fontSizeSp

    /** Line height in points: stored override, else the font's ratio. */
    fun lineHeightPt(element: TextElement): Float =
        element.lineHeightPt ?: (
            ratios(element.fontFamilyName, element.bold, element.italic).lineHeight *
                element.fontSizeSp
            )
}
