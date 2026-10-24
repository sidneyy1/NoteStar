package com.riguel.notestar.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.riguel.notestar.ui.LegacyPaper
import com.riguel.notestar.ui.PaperTemplates
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.blend.BlendMode
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import java.io.File
import kotlin.math.abs
import kotlin.math.sin

/** Export quality. The overlay pipeline is vector, so this only documents
 * intent; it is kept for the strokes that would need rasterizing. */
enum class ExportQuality { STANDARD, HIGH }

enum class ExportPageChoice { ALL, CURRENT, CUSTOM }

data class ExportOptions(
    val includeAnnotations: Boolean = true,
    val pageChoice: ExportPageChoice = ExportPageChoice.ALL,
    val customRange: String = "",
    val quality: ExportQuality = ExportQuality.STANDARD,
    val fileName: String = "export"
)

/** User-facing export failure (bad range, password-protected PDF, ...). */
class ExportException(message: String) : Exception(message)

/** Thrown when the user cancels mid-export; handled silently by the UI. */
class ExportCancelled : Exception()

/** Parses "1-3, 5" into sorted 0-based page indices, or null when invalid. */
fun parsePageRange(input: String, pageCount: Int): List<Int>? {
    if (input.isBlank()) return null
    val result = sortedSetOf<Int>()
    for (part in input.split(",")) {
        val p = part.trim()
        if (p.isEmpty()) return null
        if ("-" in p) {
            val bits = p.split("-")
            if (bits.size != 2) return null
            val a = bits[0].trim().toIntOrNull() ?: return null
            val b = bits[1].trim().toIntOrNull() ?: return null
            if (a < 1 || b < a || b > pageCount) return null
            for (i in a..b) result.add(i - 1)
        } else {
            val n = p.toIntOrNull() ?: return null
            if (n < 1 || n > pageCount) return null
            result.add(n - 1)
        }
    }
    return result.toList()
}

fun resolveExportPages(
    options: ExportOptions,
    pageCount: Int,
    currentPage: Int
): List<Int>? = when (options.pageChoice) {
    ExportPageChoice.ALL -> (0 until pageCount).toList()
    ExportPageChoice.CURRENT ->
        listOf(currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)))
    ExportPageChoice.CUSTOM -> parsePageRange(options.customRange, pageCount)
}

/**
 * "Export as PDF". Imported PDFs are rebuilt with PDFBox by copying the
 * original pages (text stays selectable, sizes/rotation/CropBox preserved)
 * and appending the annotations as a vector content stream; the imported
 * file itself is never touched. Plain notes are rendered as A4 pages with
 * the paper template, ink and text drawn as vectors. Everything is done one
 * page at a time on the caller's background thread.
 */
object PdfExportService {

    @Volatile
    private var initialized = false

    fun export(
        context: Context,
        doc: NoteDocument,
        options: ExportOptions,
        currentPage: Int,
        isCancelled: () -> Boolean,
        onProgress: (done: Int, total: Int) -> Unit
    ): File {
        if (!initialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            initialized = true
        }
        val pageCount = doc.pdf?.pageCount ?: doc.pages.size
        val pageIndices = resolveExportPages(options, pageCount, currentPage)
            ?: throw ExportException("That page range doesn't look right.")
        val outFile = uniqueFile(context, sanitize(options.fileName))
        try {
            if (doc.pdf != null) {
                exportPdfNote(context, doc, pageIndices, options, isCancelled, onProgress, outFile)
            } else {
                exportPlainNote(doc, pageIndices, options, isCancelled, onProgress, outFile)
            }
        } catch (e: Exception) {
            outFile.delete()
            throw e
        }
        return outFile
    }

    private fun exportPdfNote(
        context: Context,
        doc: NoteDocument,
        pageIndices: List<Int>,
        options: ExportOptions,
        isCancelled: () -> Boolean,
        onProgress: (Int, Int) -> Unit,
        outFile: File
    ) {
        val srcFile = NoteStorage.pdfFile(context, doc.pdf!!.fileName)
        val source = try {
            PDDocument.load(srcFile)
        } catch (e: Exception) {
            throw ExportException(
                "This PDF is password protected or unreadable, so it can't be exported."
            )
        }
        source.use { src ->
            val target = PDDocument()
            target.use { out ->
                pageIndices.forEachIndexed { outIndex, srcIndex ->
                    if (isCancelled()) throw ExportCancelled()
                    val imported = out.importPage(src.getPage(srcIndex))
                    if (options.includeAnnotations) {
                        val notePage = doc.pages.firstOrNull {
                            it.pdfPageIndex == srcIndex
                        }
                        if (notePage != null) {
                            AnnotationRenderer.drawPdfAnnotations(out, imported, notePage)
                        }
                    }
                    onProgress(outIndex + 1, pageIndices.size)
                }
                out.save(outFile)
            }
        }
    }

    private fun exportPlainNote(
        doc: NoteDocument,
        pageIndices: List<Int>,
        options: ExportOptions,
        isCancelled: () -> Boolean,
        onProgress: (Int, Int) -> Unit,
        outFile: File
    ) {
        val target = PDDocument()
        target.use { out ->
            pageIndices.forEachIndexed { outIndex, srcIndex ->
                if (isCancelled()) throw ExportCancelled()
                val notePage = doc.pages[srcIndex]
                // The PDF page is exactly the note's page size, and page
                // coordinates ARE PDF points: the export transform is
                // scale 1 (plus the Y flip), so widths, font sizes and
                // positions are identical to the editor at 100%.
                val rect = PDRectangle(
                    doc.pageFormat.widthPt, doc.pageFormat.heightPt
                )
                val pdPage = PDPage(rect)
                out.addPage(pdPage)
                PDPageContentStream(out, pdPage).use { stream ->
                    // Page color (white sheet).
                    stream.setNonStrokingColor(1f, 1f, 1f)
                    stream.addRect(0f, 0f, rect.width, rect.height)
                    stream.fill()
                    stream.transform(Matrix(1f, 0f, 0f, -1f, 0f, rect.height))
                    // Clip everything to the page rectangle.
                    stream.addRect(0f, 0f, rect.width, rect.height)
                    stream.clip()
                    AnnotationRenderer.drawTemplate(
                        stream, notePage.template,
                        rect.width, rect.height,
                        PaperTemplates.PT_PER_MM
                    )
                    if (options.includeAnnotations) {
                        AnnotationRenderer.drawNoteContent(
                            stream, notePage, highlightsFirst = true
                        )
                    }
                }
                onProgress(outIndex + 1, pageIndices.size)
            }
            out.save(outFile)
        }
    }

    private fun sanitize(name: String): String {
        val cleaned = name.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return cleaned.ifEmpty { "note" }
    }

    /** cacheDir/exports/<name>.pdf, appending " (1)", " (2)", ... as needed. */
    private fun uniqueFile(context: Context, baseName: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        var candidate = File(dir, "$baseName.pdf")
        var n = 1
        while (candidate.exists()) {
            candidate = File(dir, "$baseName ($n).pdf")
            n++
        }
        return candidate
    }
}

/**
 * Draws stored annotations into a PDF content stream. The caller sets up the
 * stream transform so the current user space is the app's view space:
 * top-left origin, y pointing DOWN, units = PDF points for PDF pages or
 * editor page units for plain notes.
 */
object AnnotationRenderer {

    private fun resetState(): PDExtendedGraphicsState =
        PDExtendedGraphicsState().apply {
            setBlendMode(BlendMode.NORMAL)
            setStrokingAlphaConstant(1f)
            setNonStrokingAlphaConstant(1f)
        }

    private fun setColor(stream: PDPageContentStream, color: Color, stroke: Boolean) {
        if (stroke) {
            stream.setStrokingColor(color.red, color.green, color.blue)
        } else {
            stream.setNonStrokingColor(color.red, color.green, color.blue)
        }
    }

    /** Appends one PDF page's annotations, honouring page rotation/CropBox. */
    fun drawPdfAnnotations(doc: PDDocument, page: PDPage, notePage: Page) {
        val crop = page.cropBox
        val w = crop.width
        val h = crop.height
        val llx = crop.lowerLeftX
        val lly = crop.lowerLeftY
        PDPageContentStream(
            doc, page, PDPageContentStream.AppendMode.APPEND, true
        ).use { stream ->
            // Stored coords live in the rotated view space (top-left origin,
            // y down, PDF points). Map them into the page's user space.
            val matrix = when (page.rotation) {
                90 -> Matrix(0f, 1f, 1f, 0f, llx, lly)
                180 -> Matrix(-1f, 0f, 0f, -1f, w + llx, h + lly)
                270 -> Matrix(0f, -1f, -1f, 0f, w + llx, h + lly)
                else -> Matrix(1f, 0f, 0f, -1f, llx, h + lly)
            }
            stream.transform(matrix)
            drawNoteContent(stream, notePage)
        }
    }

    /** Strokes of all visible layers (in order), then their text blocks —
     * the same z-order as on screen. */
    fun drawNoteContent(
        stream: PDPageContentStream,
        page: Page,
        highlightsFirst: Boolean = false
    ) {
        val visible = page.layers.filter { it.visible }
        stream.setLineCapStyle(1)
        stream.setLineJoinStyle(1)
        if (highlightsFirst) {
            // Normal-note export order: paper, highlights, ink, text.
            for (layer in visible) {
                for (stroke in layer.strokes) {
                    if (stroke.highlighter) drawStroke(stream, stroke)
                }
            }
            for (layer in visible) {
                for (stroke in layer.strokes) {
                    if (!stroke.highlighter) drawStroke(stream, stroke)
                }
            }
        } else {
            for (layer in visible) {
                for (stroke in layer.strokes) drawStroke(stream, stroke)
            }
        }
        stream.setGraphicsStateParameters(resetState())
        for (layer in visible) {
            for (element in layer.texts) drawText(stream, element)
        }
    }

    private fun drawStroke(stream: PDPageContentStream, stroke: Stroke) {
        val color = Color(stroke.colorValue)
        if (stroke.highlighter) {
            // One constant-width path, multiply blend at 0.55 alpha — the
            // same look as on screen, words stay readable underneath.
            val pts = StrokeGeometry.smoothed(stroke.points)
            if (pts.isEmpty()) return
            stream.setGraphicsStateParameters(
                PDExtendedGraphicsState().apply {
                    setBlendMode(BlendMode.MULTIPLY)
                    setStrokingAlphaConstant(0.55f)
                    setNonStrokingAlphaConstant(0.55f)
                }
            )
            setColor(stream, color, stroke = true)
            stream.setLineWidth(stroke.thickness)
            stream.moveTo(pts.first().x, pts.first().y)
            if (pts.size == 1) {
                stream.lineTo(pts.first().x + 0.01f, pts.first().y)
            } else {
                for (p in pts.drop(1)) stream.lineTo(p.x, p.y)
            }
            stream.stroke()
            stream.setGraphicsStateParameters(resetState())
            return
        }
        val pts = if (stroke.penType == PenTypes.PENCIL) {
            StrokeGeometry.filterJitter(stroke.points, 0.8f)
        } else {
            StrokeGeometry.smoothed(stroke.points)
        }
        if (pts.isEmpty()) return
        if (stroke.penType == PenTypes.PENCIL) {
            stream.setGraphicsStateParameters(
                PDExtendedGraphicsState().apply { setStrokingAlphaConstant(0.85f) }
            )
        }
        setColor(stream, color, stroke = true)
        if (pts.size == 1) {
            val p = pts.first()
            stream.setLineWidth(widthFor(stroke, p.pressure, 0f))
            stream.moveTo(p.x, p.y)
            stream.lineTo(p.x + 0.01f, p.y)
            stream.stroke()
        } else {
            for (i in 0 until pts.size - 1) {
                val a = pts[i]
                val b = pts[i + 1]
                val angle = kotlin.math.atan2(b.y - a.y, b.x - a.x)
                stream.setLineWidth(widthFor(stroke, b.pressure, angle))
                stream.moveTo(a.x, a.y)
                stream.lineTo(b.x, b.y)
                stream.stroke()
            }
        }
        if (stroke.penType == PenTypes.PENCIL) {
            stream.setGraphicsStateParameters(resetState())
        }
    }

    /** Per-segment width, mirroring the on-screen renderer. */
    private fun widthFor(stroke: Stroke, pressure: Float, angleRad: Float): Float =
        when (stroke.penType) {
            PenTypes.PENCIL ->
                stroke.thickness * 0.55f * (0.4f + 0.6f * pressure)
            PenTypes.CALLIGRAPHY -> {
                val nib = abs(sin(angleRad - Math.PI / 4)).toFloat()
                stroke.thickness * 1.7f * (0.15f + 0.85f * nib)
            }
            else -> stroke.thickness * (0.3f + 0.7f * pressure)
        }

    private fun fontFor(element: TextElement): PDType1Font = when (element.fontFamilyName) {
        TextFonts.SERIF -> when {
            element.bold && element.italic -> PDType1Font.TIMES_BOLD_ITALIC
            element.bold -> PDType1Font.TIMES_BOLD
            element.italic -> PDType1Font.TIMES_ITALIC
            else -> PDType1Font.TIMES_ROMAN
        }
        TextFonts.MONO -> when {
            element.bold && element.italic -> PDType1Font.COURIER_BOLD_OBLIQUE
            element.bold -> PDType1Font.COURIER_BOLD
            element.italic -> PDType1Font.COURIER_OBLIQUE
            else -> PDType1Font.COURIER
        }
        else -> when {
            element.bold && element.italic -> PDType1Font.HELVETICA_BOLD_OBLIQUE
            element.bold -> PDType1Font.HELVETICA_BOLD
            element.italic -> PDType1Font.HELVETICA_OBLIQUE
            else -> PDType1Font.HELVETICA
        }
    }

    private fun drawText(stream: PDPageContentStream, element: TextElement) {
        val color = Color(element.colorValue)
        setColor(stream, color, stroke = false)
        val font = fontFor(element)
        val size = element.fontSizeSp
        val lines = element.text.split("\n")
        lines.forEachIndexed { i, line ->
            if (line.isEmpty()) return@forEachIndexed
            stream.beginText()
            stream.setFont(font, size)
            // The stream space is y-down (the caller's transform flipped Y),
            // so flip the text matrix to keep glyphs upright. Baseline sits
            // about 0.8em below the stored top edge, like on screen.
            val baseline = element.y + size * 0.8f + i * size * 1.25f
            stream.setTextMatrix(Matrix(1f, 0f, 0f, -1f, element.x, baseline))
            stream.showText(line)
            stream.endText()
        }
    }

    /** The paper template as vector lines/dots, from the shared registry. */
    fun drawTemplate(
        stream: PDPageContentStream,
        template: PageTemplate,
        widthUnits: Float,
        heightUnits: Float,
        unitsPerMm: Float
    ) {
        val def = PaperTemplates.def(template)
        val spacing = def.spacingMm * unitsPerMm
        val top = def.topMarginMm * unitsPerMm
        val c = def.patternColor
        when (def.kind.name) {
            "LINES" -> {
                setColor(stream, c, stroke = true)
                stream.setLineWidth(def.strokeWidthPx * LegacyPaper.UNIT_TO_PT)
                var y = if (top > 0f) top else spacing
                while (y < heightUnits) {
                    stream.moveTo(0f, y)
                    stream.lineTo(widthUnits, y)
                    stream.stroke()
                    y += spacing
                }
                if (template == PageTemplate.LINED) {
                    // The editor's margin line (shared LegacyPaper value).
                    val mx = LegacyPaper.MARGIN_X_MM * unitsPerMm
                    setColor(stream, Color(0xFFF8BBD0), stroke = true)
                    stream.moveTo(mx, 0f)
                    stream.lineTo(mx, heightUnits)
                    stream.stroke()
                }
                if (template == PageTemplate.PLANNER) {
                    // Time column + checkboxes, same geometry as the
                    // editor's page-space generator (LegacyPaper).
                    val cx = LegacyPaper.PLANNER_COLUMN_MM * unitsPerMm
                    setColor(stream, c, stroke = true)
                    stream.moveTo(cx, 0f)
                    stream.lineTo(cx, heightUnits)
                    stream.stroke()
                    val boxSize = LegacyPaper.CHECKBOX_SIZE_MM * unitsPerMm
                    val bx = LegacyPaper.CHECKBOX_X_MM * unitsPerMm
                    val lift = LegacyPaper.CHECKBOX_LIFT_MM * unitsPerMm
                    setColor(stream, Color(0xFF90CAF9), stroke = true)
                    var ry = if (top > 0f) top else spacing
                    while (ry < heightUnits) {
                        stream.addRect(bx, ry - lift, boxSize, boxSize)
                        stream.stroke()
                        ry += spacing
                    }
                }
                if (template == PageTemplate.LECTURE) {
                    // Header rules, same LegacyPaper geometry as the editor.
                    val fromX = LegacyPaper.LECTURE_HEADER_X_MM * unitsPerMm
                    setColor(stream, Color(0xFF90CAF9), stroke = true)
                    for (headerMm in listOf(
                        LegacyPaper.LECTURE_HEADER_1_MM,
                        LegacyPaper.LECTURE_HEADER_2_MM
                    )) {
                        val hy = headerMm * unitsPerMm
                        stream.moveTo(fromX, hy)
                        stream.lineTo(widthUnits, hy)
                        stream.stroke()
                    }
                }
            }
            "GRID" -> {
                setColor(stream, c, stroke = true)
                stream.setLineWidth(def.strokeWidthPx * LegacyPaper.UNIT_TO_PT)
                var x = spacing
                while (x < widthUnits) {
                    stream.moveTo(x, 0f)
                    stream.lineTo(x, heightUnits)
                    stream.stroke()
                    x += spacing
                }
                var y = spacing
                while (y < heightUnits) {
                    stream.moveTo(0f, y)
                    stream.lineTo(widthUnits, y)
                    stream.stroke()
                    y += spacing
                }
            }
            "DOTS" -> {
                setColor(stream, c, stroke = false)
                val r = def.strokeWidthPx * LegacyPaper.UNIT_TO_PT
                var y = spacing
                while (y < heightUnits) {
                    var x = spacing
                    while (x < widthUnits) {
                        addDot(stream, x, y, r)
                        x += spacing
                    }
                    y += spacing
                }
            }
            else -> Unit // BLANK
        }
    }

    /** A small filled circle via four bezier arcs. */
    private fun addDot(
        stream: PDPageContentStream,
        cx: Float,
        cy: Float,
        r: Float
    ) {
        val k = 0.5523f * r
        stream.moveTo(cx + r, cy)
        stream.curveTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r)
        stream.curveTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy)
        stream.curveTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r)
        stream.curveTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy)
        stream.fill()
    }
}
