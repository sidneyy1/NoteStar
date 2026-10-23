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

    fun listNotes(context: Context): List<NoteMeta> {
        val files = notesDir(context).listFiles { f -> f.extension == "json" }
            ?: return emptyList()
        return files.mapNotNull { file ->
            try {
                val json = JSONObject(file.readText())
                NoteMeta(
                    id = json.getString("id"),
                    title = json.optString("title", "Untitled"),
                    updatedAt = json.optLong("updatedAt", file.lastModified())
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.updatedAt }
    }

    fun loadNote(context: Context, id: String): Pair<NoteMeta, List<Stroke>>? {
        val file = noteFile(context, id)
        if (!file.exists()) return null
        return try {
            val json = JSONObject(file.readText())
            val meta = NoteMeta(
                id = json.getString("id"),
                title = json.optString("title", "Untitled"),
                updatedAt = json.optLong("updatedAt", file.lastModified())
            )
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
            meta to strokes
        } catch (e: Exception) {
            null
        }
    }

    fun saveNote(context: Context, meta: NoteMeta, strokes: List<Stroke>) {
        val json = JSONObject()
        json.put("id", meta.id)
        json.put("title", meta.title)
        json.put("updatedAt", meta.updatedAt)
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
        noteFile(context, meta.id).writeText(json.toString())
    }

    fun deleteNote(context: Context, id: String) {
        val file = noteFile(context, id)
        if (file.exists()) file.delete()
    }
}
