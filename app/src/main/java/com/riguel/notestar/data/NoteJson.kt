package com.riguel.notestar.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * The one place that knows the note file format.
 *
 * Version 2: pages + layers + tags + optional PDF attachment (see
 * docs/DATA_MODEL.md). Version 1 (no "version" field, strokes/texts at the
 * root, written by NoteStar <= 0.3) is migrated on load.
 */
object NoteJson {

    const val CURRENT_VERSION = 2

    // ---- writing (always v2) ----

    fun toJson(doc: NoteDocument): JSONObject {
        val json = JSONObject()
        json.put("version", CURRENT_VERSION)
        json.put("id", doc.id)
        json.put("title", doc.title)
        json.put("tags", JSONArray(doc.tags))
        json.put("createdAt", doc.createdAt)
        json.put("updatedAt", doc.updatedAt)
        doc.coverColorValue?.let { json.put("cover", it.toLong()) }
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
        return NoteDocument(
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
            }
        )
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
                    fontFamilyName = t.optString("font", TextFonts.DEFAULT)
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
        return NoteDocument(
            id = json.getString("id"),
            title = json.optString("title", "Untitled"),
            tags = emptyList(),
            createdAt = updatedAt,
            updatedAt = updatedAt,
            pdf = null,
            pages = listOf(page)
        )
    }

    private fun parseTemplate(name: String): PageTemplate =
        try {
            PageTemplate.valueOf(name)
        } catch (e: Exception) {
            PageTemplate.BLANK
        }
}
