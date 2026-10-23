package com.riguel.notestar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Saves notes as JSON files in the app's private storage:
 *   files/notes/<id>.json
 * No account or network needed; everything stays on the device.
 */
object NoteStorage {

    private fun notesDir(context: Context): File {
        val dir = File(context.filesDir, "notes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun noteFile(context: Context, id: String): File =
        File(notesDir(context), "$id.json")

    fun newId(): String = System.currentTimeMillis().toString()

    private fun readMeta(json: JSONObject, fallbackTime: Long): NoteMeta {
        val template = try {
            PageTemplate.valueOf(json.optString("template", PageTemplate.BLANK.name))
        } catch (e: Exception) {
            PageTemplate.BLANK
        }
        return NoteMeta(
            id = json.getString("id"),
            title = json.optString("title", "Untitled"),
            updatedAt = json.optLong("updatedAt", fallbackTime),
            template = template
        )
    }

    fun listNotes(context: Context): List<NoteMeta> {
        val files = notesDir(context).listFiles { f -> f.extension == "json" }
            ?: return emptyList()
        return files.mapNotNull { file ->
            try {
                readMeta(JSONObject(file.readText()), file.lastModified())
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.updatedAt }
    }

    fun loadNote(context: Context, id: String): LoadedNote? {
        val file = noteFile(context, id)
        if (!file.exists()) return null
        return try {
            val json = JSONObject(file.readText())
            val meta = readMeta(json, file.lastModified())

            val strokesJson = json.optJSONArray("strokes") ?: JSONArray()
            val strokes = mutableListOf<Stroke>()
            for (i in 0 until strokesJson.length()) {
                val s = strokesJson.getJSONObject(i)
                val pointsJson = s.getJSONArray("points")
                val points = mutableListOf<StrokePoint>()
                for (j in 0 until pointsJson.length()) {
                    val p = pointsJson.getJSONArray(j)
                    points.add(StrokePoint(p.getDouble(0).toFloat(), p.getDouble(1).toFloat()))
                }
                strokes.add(
                    Stroke(
                        points = points,
                        colorValue = s.getString("color").toULong(),
                        thickness = s.getDouble("thickness").toFloat(),
                        highlighter = s.optBoolean("highlighter", false)
                    )
                )
            }

            val textsJson = json.optJSONArray("texts") ?: JSONArray()
            val texts = mutableListOf<TextElement>()
            for (i in 0 until textsJson.length()) {
                val t = textsJson.getJSONObject(i)
                texts.add(
                    TextElement(
                        id = t.getString("id"),
                        x = t.getDouble("x").toFloat(),
                        y = t.getDouble("y").toFloat(),
                        text = t.optString("text", ""),
                        colorValue = t.getString("color").toULong(),
                        fontSizeSp = t.optDouble("size", 20.0).toFloat()
                    )
                )
            }

            LoadedNote(meta = meta, strokes = strokes, texts = texts)
        } catch (e: Exception) {
            null
        }
    }

    fun saveNote(
        context: Context,
        meta: NoteMeta,
        strokes: List<Stroke>,
        texts: List<TextElement>
    ) {
        val json = JSONObject()
        json.put("id", meta.id)
        json.put("title", meta.title)
        json.put("updatedAt", meta.updatedAt)
        json.put("template", meta.template.name)

        val strokesJson = JSONArray()
        for (stroke in strokes) {
            val s = JSONObject()
            s.put("color", stroke.colorValue.toString())
            s.put("thickness", stroke.thickness.toDouble())
            s.put("highlighter", stroke.highlighter)
            val pointsJson = JSONArray()
            for (point in stroke.points) {
                val p = JSONArray()
                p.put(point.x.toDouble())
                p.put(point.y.toDouble())
                pointsJson.put(p)
            }
            s.put("points", pointsJson)
            strokesJson.put(s)
        }
        json.put("strokes", strokesJson)

        val textsJson = JSONArray()
        for (element in texts) {
            val t = JSONObject()
            t.put("id", element.id)
            t.put("x", element.x.toDouble())
            t.put("y", element.y.toDouble())
            t.put("text", element.text)
            t.put("color", element.colorValue.toString())
            t.put("size", element.fontSizeSp.toDouble())
            textsJson.put(t)
        }
        json.put("texts", textsJson)

        noteFile(context, meta.id).writeText(json.toString())
    }

    fun deleteNote(context: Context, id: String) {
        val file = noteFile(context, id)
        if (file.exists()) file.delete()
    }
}
