package com.riguel.notestar.data

/** Tools available in the editor. */
enum class Tool { PEN, HIGHLIGHTER, TEXT, ERASER, MOVE }

/** The three editor modes: select things, roam the canvas, or draw. */
enum class EditorMode { SELECT, HAND, DRAW }

/** Pen nib styles for the Pen tool. Stored by name in the note JSON. */
object PenTypes {
    const val PENCIL = "pencil"
    const val PEN = "pen"
    const val CALLIGRAPHY = "calligraphy"
    val ALL = listOf(PENCIL, PEN, CALLIGRAPHY)
}

/** Page background. Built-in templates; user templates reuse these styles. */
enum class PageTemplate {
    BLANK, LINED, DOTTED, PLANNER, LECTURE,
    THIN_LINE, BOLD_LINE, MINI_CHECK, MID_CHECK, DOT_GRID
}

/** One point along a stroke, in page coordinates. Pressure is 0..1. */
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f
)

/**
 * A single drawn stroke.
 * [colorValue] is the raw ULong value of a Compose Color, stored as-is so a
 * note round-trips exactly through JSON.
 */
data class Stroke(
    val points: List<StrokePoint>,
    val colorValue: ULong,
    val thickness: Float,
    val highlighter: Boolean,
    val penType: String = PenTypes.PEN
)

/** Font choices for typed text. Stored by name in the note JSON. */
object TextFonts {
    const val DEFAULT = "default"
    const val SERIF = "serif"
    const val SANS = "sans"
    const val MONO = "mono"
    const val CURSIVE = "cursive"
    val ALL = listOf(DEFAULT, SERIF, SANS, MONO, CURSIVE)
}

/**
 * A block of typed text placed on the page, with simple rich formatting.
 *
 * Geometry is in PAGE POINTS (1/72 inch), the same units strokes use:
 * [x]/[y] are the text box's top-left corner, [fontSizeSp] is the font
 * size in points despite its legacy name (it predates the v0.11 point
 * model; the JSON key is still "size", so old files keep working).
 * [widthPt] is the box's wrap width in points; null means "to the page's
 * right edge" (x -> pageWidth), which is how unwrapped old notes behaved.
 * [paddingPt] is the inset from the box edge to the glyphs, in points —
 * the same value on screen and in export (v0.13: 4pt; previously the
 * editor used 4dp/6dp screen insets that export could not reproduce).
 * [lineHeightPt] is the line height in points; null means "this font's
 * real line height" (see TextMetrics), which is what both renderers use.
 */
data class TextElement(
    val id: String,
    val x: Float,
    val y: Float,
    val text: String,
    val colorValue: ULong,
    val fontSizeSp: Float,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontFamilyName: String = TextFonts.DEFAULT,
    val widthPt: Float? = null,
    val paddingPt: Float = 4f,
    val lineHeightPt: Float? = null,
    val underline: Boolean = false
)

/** A named layer of ink + text. List order is back-to-front. */
data class Layer(
    val id: String,
    val name: String,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val strokes: List<Stroke> = emptyList(),
    val texts: List<TextElement> = emptyList()
) {
    companion object {
        fun create(name: String): Layer =
            Layer(id = "l" + System.nanoTime().toString(), name = name)
    }
}

/** One page of a note. For PDF notes, [pdfPageIndex] links to a PDF page. */
data class Page(
    val id: String,
    val template: PageTemplate = PageTemplate.BLANK,
    val pdfPageIndex: Int? = null,
    val layers: List<Layer> = listOf(Layer.create("Layer 1"))
) {
    companion object {
        fun create(template: PageTemplate): Page =
            Page(id = "p" + System.nanoTime().toString(), template = template)
    }
}

/** An imported PDF stored in the app's private files. Never modified. */
data class PdfAttachment(
    val fileName: String,
    val pageCount: Int
)

/**
 * A normal note's page size, in PDF points (1/72 inch). All geometry of a
 * normal note (stroke points and widths, text positions and font sizes) is
 * stored in these page coordinates, so the screen and the PDF export share
 * one coordinate system. Imported PDFs ignore this — they keep the PDF's
 * own page sizes.
 */
data class PageFormat(
    val widthPt: Float,
    val heightPt: Float,
    val presetId: String = "custom"
)

/** Units offered by the custom page-size editor. */
enum class SizeUnit { PX, MM, CM, IN }

/** Built-in page sizes (portrait, points) and unit conversions. */
object PageFormats {
    val A3 = PageFormat(842f, 1191f, "a3")
    val A4 = PageFormat(595f, 842f, "a4")
    val A5 = PageFormat(420f, 595f, "a5")
    val A6 = PageFormat(297f, 420f, "a6")
    val B5 = PageFormat(499f, 709f, "b5")
    val LETTER = PageFormat(612f, 792f, "letter")
    val LEGAL = PageFormat(612f, 1008f, "legal")
    val SQUARE = PageFormat(595f, 595f, "square")
    val PRESETS = listOf(A3, A4, A5, A6, B5, LETTER, LEGAL, SQUARE)

    fun label(presetId: String): String = when (presetId) {
        "a3" -> "A3"
        "a4" -> "A4"
        "a5" -> "A5"
        "a6" -> "A6"
        "b5" -> "B5"
        "letter" -> "Letter"
        "legal" -> "Legal"
        "square" -> "Square"
        else -> "Custom"
    }

    /** Converts an editor value to points. 1 px = 1/96 inch. */
    fun toPt(value: Float, unit: SizeUnit): Float = when (unit) {
        SizeUnit.PX -> value * 72f / 96f
        SizeUnit.MM -> value * 72f / 25.4f
        SizeUnit.CM -> value * 72f / 2.54f
        SizeUnit.IN -> value * 72f
    }

    /** Converts points back to an editor value. */
    fun fromPt(pt: Float, unit: SizeUnit): Float = when (unit) {
        SizeUnit.PX -> pt * 96f / 72f
        SizeUnit.MM -> pt * 25.4f / 72f
        SizeUnit.CM -> pt * 2.54f / 72f
        SizeUnit.IN -> pt / 72f
    }

    /** The value in px (1/96 inch), used for the custom-size limits. */
    fun toPx(pt: Float): Float = pt * 96f / 72f
}

/** A complete note: the unit saved as one JSON file. */
data class NoteDocument(
    val id: String,
    val title: String,
    val tags: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val pdf: PdfAttachment? = null,
    val pages: List<Page> = listOf(Page.create(PageTemplate.BLANK)),
    /** Optional cover color (ARGB as a Compose Color value), chosen at creation. */
    val coverColorValue: ULong? = null,
    /** Page size for normal notes (points). Unused for imported PDFs. */
    val pageFormat: PageFormat = PageFormats.A4
)

/** Lightweight info about a saved note, shown in the notebook list. */
data class NoteMeta(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val template: PageTemplate = PageTemplate.BLANK,
    val tags: List<String> = emptyList(),
    val isPdf: Boolean = false,
    val pageCount: Int = 1,
    val coverColorValue: ULong? = null
)
