package com.riguel.notestar.data

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import java.io.File

/**
 * Embedded fonts for NORMAL-note text export only. The imported-PDF
 * export path does not use this: it keeps its built-in-font text path
 * exactly as before.
 *
 * The editor renders with the device's system Typefaces, so the exporter
 * embeds those same font files when it can find them under /system/fonts
 * (Roboto for Default/Sans, Noto Serif for Serif, Roboto Mono/Droid Sans
 * Mono for Mono, Dancing Script/Coming Soon for Cursive). A file that is
 * found is loaded with PDType0Font.load, which embeds and subsets it —
 * never a Standard 14 built-in font. When no system file is found for a
 * family/style, the bundled Roboto fallback in assets/fonts/ (Apache 2.0)
 * is embedded instead: still a real embedded font, never Helvetica.
 *
 * Glyph fallback: before drawing a run, [hasGlyph] checks the main font
 * with Android Paint (same font family/style as the Typeface). A run
 * whose glyphs the main font lacks is drawn with the bundled fallback
 * font, which is also embedded. Paint is used for the check because
 * PDFont.hasGlyph's code parameter differs between PDFBox versions;
 * Paint.hasGlyph is stable on minSdk 26.
 */
class ExportFontSet(
    private val document: PDDocument,
    private val context: Context
) {
    private val cache = mutableMapOf<String, PDFont>()
    private val fallbackCache = mutableMapOf<Boolean, PDFont>()

    private fun systemCandidates(
        family: String,
        bold: Boolean,
        italic: Boolean
    ): List<String> {
        val suffix = when {
            bold && italic -> "BoldItalic"
            bold -> "Bold"
            italic -> "Italic"
            else -> "Regular"
        }
        val monoSuffix = when {
            bold && italic -> "BoldOblique"
            bold -> "Bold"
            italic -> "Oblique"
            else -> "Regular"
        }
        return when (family) {
            TextFonts.SERIF -> listOf(
                "/system/fonts/NotoSerif-$suffix.ttf",
                "/system/fonts/NotoSerifDisplay-$suffix.ttf"
            )
            TextFonts.MONO -> listOf(
                "/system/fonts/RobotoMono-$suffix.ttf",
                "/system/fonts/DroidSansMono-$monoSuffix.ttf",
                "/system/fonts/NotoSansMono-$suffix.ttf"
            )
            TextFonts.CURSIVE -> listOf(
                "/system/fonts/DancingScript-$suffix.ttf",
                "/system/fonts/ComingSoon.ttf"
            )
            else -> listOf("/system/fonts/Roboto-$suffix.ttf")
        }
    }

    private fun fallback(bold: Boolean): PDFont {
        fallbackCache[bold]?.let { return it }
        val asset = if (bold) "fonts/Roboto-Bold.ttf" else "fonts/Roboto-Regular.ttf"
        val loaded = try {
            context.assets.open(asset).use { input ->
                PDType0Font.load(document, input)
            }
        } catch (e: Exception) {
            throw ExportException(
                "The export font could not be loaded, so text can't be exported."
            )
        }
        fallbackCache[bold] = loaded
        return loaded
    }

    /** Embedded font for this family/style, cached per document. */
    fun fontFor(element: TextElement): PDFont {
        val key = "${element.fontFamilyName}|${element.bold}|${element.italic}"
        cache[key]?.let { return it }
        var loaded: PDFont? = null
        for (path in systemCandidates(
            element.fontFamilyName, element.bold, element.italic
        )) {
            try {
                val file = File(path)
                if (file.exists()) {
                    loaded = PDType0Font.load(document, file)
                    break
                }
            } catch (e: Exception) {
                // Try the next candidate; the bundled fallback is last.
            }
        }
        if (loaded == null) loaded = fallback(element.bold)
        cache[key] = loaded
        return loaded
    }

    fun fallbackFont(bold: Boolean = false): PDFont = fallback(bold)

    /** True when the main font for [element] can render [text]. */
    fun hasGlyph(element: TextElement, text: String): Boolean {
        return try {
            val family = when (element.fontFamilyName) {
                TextFonts.SERIF -> "serif"
                TextFonts.MONO -> "monospace"
                TextFonts.CURSIVE -> "cursive"
                else -> "sans-serif"
            }
            val style = when {
                element.bold && element.italic -> Typeface.BOLD_ITALIC
                element.bold -> Typeface.BOLD
                element.italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            val paint = Paint().apply {
                typeface = Typeface.create(family, style)
            }
            paint.hasGlyph(text)
        } catch (e: Exception) {
            true
        }
    }

    /** Width of [text] in points at [fontSizePt], via the embedded font. */
    fun stringWidthPt(font: PDFont, text: String, fontSizePt: Float): Float {
        return try {
            font.getStringWidth(text) / 1000f * fontSizePt
        } catch (e: Exception) {
            // Fall back to a rough average width rather than failing the
            // whole export over one unmeasurable string.
            text.length * fontSizePt * 0.5f
        }
    }
}
