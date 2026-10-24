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

/** A block of typed text placed on the page, with simple rich formatting. */
data class TextElement(
    val id: String,
    val x: Float,
    val y: Float,
    val text: String,
    val colorValue: ULong,
    val fontSizeSp: Float,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontFamilyName: String = TextFonts.DEFAULT
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
    val coverColorValue: ULong? = null
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
