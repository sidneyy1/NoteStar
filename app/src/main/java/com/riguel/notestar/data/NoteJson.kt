package com.riguel.notestar.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * The one place that knows the note file format.
 *
 * Version 2: pages + layers + tags + optional PDF attachment (see
 * docs/DATA_MODEL.md). Version 1 (no "version" field, strokes/texts at the
 * root, written by NoteStar <= 0.3) is migrated on load.
 *
 * Version 3: normal notes store geometry in PDF POINTS and carry a
 * pageFormat. Pre-v3 normal notes used ~150dpi page units (5.9 per mm);
 * they are converted once on load with a single uniform factor. PDF notes
 * always stored PDF points and are never converted.
 */
object NoteJson {

    const val CURRENT_VERSION = 3

    // ---- writing (always v3) ----

    fun toJson(doc: NoteDocument): JSONObject {
        val json = JSONObject()
        json.put("version", CURRENT_VERSION)
        json.put("id", doc.id)
        json.put("title", doc.title)
        json.put("tags", JSONArray(doc.tags))
        json.put("createdAt", doc.createdAt)
        json.put("updatedAt", doc.updatedAt)
        doc.coverColorValue?.let { json.put("cover", it.toLong()) }
        val pf = JSONObject()
        pf.put("w", doc.pageFormat.widthPt.toDouble())
        pf.put("h", doc.pageFormat.heightPt.toDouble())
        pf.put("preset", doc.pageFormat.presetId)
        json.put("pageFormat", pf)
        doc.pdf?.let { pdf ->
            val p = JSONObject()
            p.put("fileName", pdf.fileName)
            p.put("pageCount", pdf.pageCount)
            json.put("pdf", p)
        }
        val pages = JSONArray()
        for (page in doc.pages) {
            pages.put(pageToJson(page))
        }
        json.put("pages", pages)
        return json
    }

    private fun pageToJson(page: Page): JSONObject {
        val json = JSONObject()
        json.put("id", page.id)
        json.put("template", page.template.name)
        if (page.pdfPageIndex != null) {
            json.put("pdfPageIndex", page.pdfPageIndex)
        }
        val layers = JSONArray()
        for (layer in page.layers) {
            layers.put(layerToJson(layer))
        }
        json.put("layers", layers)
        return json
    }

    private fun layerToJson(layer: Layer): JSONObject {
        val json = JSONObject()
        json.put("id", layer.id)
        json.put("name", layer.name)
        json.put("visible", layer.visible)
        json.put("locked", layer.locked)
        val strokes = JSONArray()
        for (stroke in layer.strokes) {
            strokes.put(strokeToJson(stroke))
        }
        json.put("strokes", strokes)
        val texts = JSONArray()
        for (text in layer.texts) {
            texts.put(textToJson(text))
        }
        json.put("texts", texts)
        return json
    }

    internal fun strokeToJson(stroke: Stroke): JSONObject {
        val json = JSONObject()
        json.put("color", stroke.colorValue.toString())
        json.put("thickness", stroke.thickness.toDouble())
        json.put("highlighter", stroke.highlighter)
        json.put("pen", stroke.penType)
        val points = JSONArray()
        for (point in stroke.points) {
            val p = JSONArray()
            p.put(point.x.toDouble())
            p.put(point.y.toDouble())
            p.put(point.pressure.toDouble())
            points.put(p)
        }
        json.put("points", points)
        return json
    }

    internal fun textToJson(text: TextElement): JSONObject {
        val json = JSONObject()
        json.put("id", text.id)
        json.put("x", text.x.toDouble())
        json.put("y", text.y.toDouble())
        json.put("text", text.text)
        json.put("color", text.colorValue.toString())
        json.put("size", text.fontSizeSp.toDouble())
        json.put("bold", text.bold)
        json.put("italic", text.italic)
        json.put("font", text.fontFamilyName)
        // v0.13 box geometry, all in page points. Old readers ignore
        // unknown keys, so these are additive, not a version bump.
        text.widthPt?.let { json.put("width", it.toDouble()) }
        json.put("padding", text.paddingPt.toDouble())
        text.lineHeightPt?.let { json.put("lineHeight", it.toDouble()) }
        json.put("underline", text.underline)
        return json
    }

    // ---- reading (v2 + v1 migration) ----

    fun fromJson(json: JSONObject): NoteDocument {
        val version = json.optInt("version", 1)
        return if (version >= 2) readV2(json) else migrateV1(json)
    }

    private fun readV2(json: JSONObject): NoteDocument {
        val pdfJson = json.optJSONObject("pdf")
        val pdf = pdfJson?.let {
            PdfAttachment(
                fileName = it.getString("fileName"),
                pageCount = it.optInt("pageCount", 0)
            )
        }
        val pagesJson = json.optJSONArray("pages") ?: JSONArray()
        val pages = mutableListOf<Page>()
        for (i in 0 until pagesJson.length()) {
            pages.add(pageFromJson(pagesJson.getJSONObject(i)))
        }
        if (pages.isEmpty()) {
            pages.add(Page.create(PageTemplate.BLANK))
        }
        val tagsJson = json.optJSONArray("tags") ?: JSONArray()
        val tags = mutableListOf<String>()
        for (i in 0 until tagsJson.length()) {
            tags.add(tagsJson.getString(i))
        }
        val pfJson = json.optJSONObject("pageFormat")
        val pageFormat = if (pfJson != null) {
            PageFormat(
                widthPt = pfJson.optDouble("w", 595.0).toFloat(),
                heightPt = pfJson.optDouble("h", 842.0).toFloat(),
                presetId = pfJson.optString("preset", "custom")
            )
        } else {
            PageFormats.A4
        }
        val doc = NoteDocument(
            id = json.getString("id"),
            title = json.optString("title", "Untitled"),
            tags = tags,
            createdAt = json.optLong("createdAt", 0L),
            updatedAt = json.optLong("updatedAt", 0L),
            pdf = pdf,
            pages = pages,
            coverColorValue = if (json.has("cover")) {
                json.getLong("cover").toULong()
            } else {
                null
            },
            pageFormat = pageFormat
        )
        // Pre-v3 normal notes: convert legacy page units to points, once.
        val version = json.optInt("version", 1)
        return if (version < CURRENT_VERSION && pdf == null) {
            convertLegacyUnits(doc)
        } else {
            doc
        }
    }

    private fun pageFromJson(json: JSONObject): Page {
        val layersJson = json.optJSONArray("layers") ?: JSONArray()
        val layers = mutableListOf<Layer>()
        for (i in 0 until layersJson.length()) {
            layers.add(layerFromJson(layersJson.getJSONObject(i)))
        }
        if (layers.isEmpty()) {
            layers.add(Layer.create("Layer 1"))
        }
        return Page(
            id = json.optString("id", "p" + System.nanoTime().toString()),
            template = parseTemplate(json.optString("template", PageTemplate.BLANK.name)),
            pdfPageIndex = if (json.has("pdfPageIndex")) json.getInt("pdfPageIndex") else null,
            layers = layers
        )
    }

    private fun layerFromJson(json: JSONObject): Layer {
        return Layer(
            id = json.optString("id", "l" + System.nanoTime().toString()),
            name = json.optString("name", "Layer"),
            visible = json.optBoolean("visible", true),
            locked = json.optBoolean("locked", false),
            strokes = strokesFromJson(json.optJSONArray("strokes")),
            texts = textsFromJson(json.optJSONArray("texts"))
        )
    }

    internal fun strokesFromJson(array: JSONArray?): List<Stroke> {
        if (array == null) return emptyList()
        val strokes = mutableListOf<Stroke>()
        for (i in 0 until array.length()) {
            val s = array.getJSONObject(i)
            val pointsJson = s.getJSONArray("points")
            val points = mutableListOf<StrokePoint>()
            for (j in 0 until pointsJson.length()) {
                val p = pointsJson.getJSONArray(j)
                points.add(
                    StrokePoint(
                        x = p.getDouble(0).toFloat(),
                        y = p.getDouble(1).toFloat(),
                        // v1 points have no pressure element
                        pressure = if (p.length() > 2) p.getDouble(2).toFloat() else 1f
                    )
                )
            }
            strokes.add(
                Stroke(
                    points = points,
                    colorValue = s.getString("color").toULong(),
                    thickness = s.getDouble("thickness").toFloat(),
                    highlighter = s.optBoolean("highlighter", false),
                    penType = s.optString("pen", PenTypes.PEN)
                )
            )
        }
        return strokes
    }

    internal fun textsFromJson(array: JSONArray?): List<TextElement> {
        if (array == null) return emptyList()
        val texts = mutableListOf<TextElement>()
        for (i in 0 until array.length()) {
            val t = array.getJSONObject(i)
            texts.add(
                TextElement(
                    id = t.getString("id"),
                    x = t.getDouble("x").toFloat(),
                    y = t.getDouble("y").toFloat(),
                    text = t.optString("text", ""),
                    colorValue = t.getString("color").toULong(),
                    fontSizeSp = t.optDouble("size", 20.0).toFloat(),
                    bold = t.optBoolean("bold", false),
                    italic = t.optBoolean("italic", false),
                    fontFamilyName = t.optString("font", TextFonts.DEFAULT),
                    widthPt = if (t.has("width")) {
                        t.optDouble("width").toFloat()
                    } else {
                        null
                    },
                    paddingPt = t.optDouble("padding", 4.0).toFloat(),
                    lineHeightPt = if (t.has("lineHeight")) {
                        t.optDouble("lineHeight").toFloat()
                    } else {
                        null
                    },
                    underline = t.optBoolean("underline", false)
                )
            )
        }
        return texts
    }

    /** v1: strokes/texts/template at the root -> one page with one layer. */
    private fun migrateV1(json: JSONObject): NoteDocument {
        val layer = Layer(
            id = "l" + System.nanoTime().toString(),
            name = "Layer 1",
            strokes = strokesFromJson(json.optJSONArray("strokes")),
            texts = textsFromJson(json.optJSONArray("texts"))
        )
        val page = Page(
            id = "p" + System.nanoTime().toString(),
            template = parseTemplate(json.optString("template", PageTemplate.BLANK.name)),
            layers = listOf(layer)
        )
        val updatedAt = json.optLong("updatedAt", 0L)
        // v1 files are normal notes in legacy page units -> convert too.
        return convertLegacyUnits(
            NoteDocument(
                id = json.getString("id"),
                title = json.optString("title", "Untitled"),
                tags = emptyList(),
                createdAt = updatedAt,
                updatedAt = updatedAt,
                pdf = null,
                pages = listOf(page)
            )
        )
    }

    /**
     * Pre-v3 normal notes stored geometry in ~150dpi page units (5.9 per
     * mm, so an A4 page was 1239 units wide). Convert to points with one
     * uniform factor — 72 / (5.9 x 25.4) — covering points, stroke widths
     * and font sizes, so the layout on the point-based A4 page is exactly
     * the layout the user drew. The note becomes an A4 document; content
     * outside that page stays in the data (hidden, not deleted).
     */
    private fun convertLegacyUnits(doc: NoteDocument): NoteDocument {
        val k = 72f / (5.9f * 25.4f)
        val pages = doc.pages.map { page ->
            page.copy(
                layers = page.layers.map { layer ->
                    layer.copy(
                        strokes = layer.strokes.map { stroke ->
                            stroke.copy(
                                points = stroke.points.map { point ->
                                    StrokePoint(
                                        x = point.x * k,
                                        y = point.y * k,
                                        pressure = point.pressure
                                    )
                                },
                                thickness = stroke.thickness * k
                            )
                        },
                        texts = layer.texts.map { text ->
                            text.copy(
                                x = text.x * k,
                                y = text.y * k,
                                fontSizeSp = text.fontSizeSp * k
                            )
                        }
                    )
                }
            )
        }
        return doc.copy(pages = pages, pageFormat = PageFormats.A4)
    }

    private fun parseTemplate(name: String): PageTemplate =
        try {
            PageTemplate.valueOf(name)
        } catch (e: Exception) {
            PageTemplate.BLANK
        }
}
