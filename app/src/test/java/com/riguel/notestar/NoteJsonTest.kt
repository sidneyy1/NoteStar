package com.riguel.notestar

import com.riguel.notestar.data.Layer
import com.riguel.notestar.data.NoteDocument
import com.riguel.notestar.data.NoteJson
import com.riguel.notestar.data.Page
import com.riguel.notestar.data.PageTemplate
import com.riguel.notestar.data.PdfAttachment
import com.riguel.notestar.data.Stroke
import com.riguel.notestar.data.StrokePoint
import com.riguel.notestar.data.TextElement
import com.riguel.notestar.data.TextFonts
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteJsonTest {

    private fun sampleDoc(): NoteDocument {
        val stroke = Stroke(
            points = listOf(
                StrokePoint(10.5f, 20.5f, 0.5f),
                StrokePoint(30f, 40f, 0.75f)
            ),
            colorValue = 4283215696uL,
            thickness = 6f,
            highlighter = false
        )
        val text = TextElement(
            id = "t1",
            x = 100f,
            y = 120f,
            text = "Hello",
            colorValue = 4278190080uL,
            fontSizeSp = 20f,
            bold = true,
            italic = false,
            fontFamilyName = TextFonts.SERIF
        )
        val layer = Layer(
            id = "l1",
            name = "Layer 1",
            visible = true,
            locked = false,
            strokes = listOf(stroke),
            texts = listOf(text)
        )
        val hiddenLayer = Layer(id = "l2", name = "Draft", visible = false, locked = true)
        return NoteDocument(
            id = "n1",
            title = "Biology",
            tags = listOf("school", "exam"),
            createdAt = 1000L,
            updatedAt = 2000L,
            pdf = PdfAttachment(fileName = "123.pdf", pageCount = 2),
            pages = listOf(
                Page(
                    id = "p1",
                    template = PageTemplate.LINED,
                    pdfPageIndex = 0,
                    layers = listOf(layer, hiddenLayer)
                ),
                Page(id = "p2", template = PageTemplate.BLANK, pdfPageIndex = 1)
            )
        )
    }

    @Test
    fun versionFieldIsWritten() {
        val json = NoteJson.toJson(sampleDoc())
        assertEquals(NoteJson.CURRENT_VERSION, json.getInt("version"))
    }

    @Test
    fun roundTripPreservesEverything() {
        val doc = sampleDoc()
        val parsed = NoteJson.fromJson(NoteJson.toJson(doc))
        assertEquals(doc, parsed)
    }

    @Test
    fun migratesV1Format() {
        // Shape written by NoteStar <= 0.3: no version, content at the root,
        // points are [x, y] pairs without pressure.
        val v1 = JSONObject(
            """
            {
              "id": "old1",
              "title": "Old note",
              "updatedAt": 555,
              "template": "LINED",
              "strokes": [
                { "color": "4278190080", "thickness": 4.0, "highlighter": false,
                  "points": [[1.0, 2.0], [3.0, 4.0]] }
              ],
              "texts": [
                { "id": "t9", "x": 5.0, "y": 6.0, "text": "legacy",
                  "color": "4278190080", "size": 18.0 }
              ]
            }
            """.trimIndent()
        )
        val doc = NoteJson.fromJson(v1)
        assertEquals("old1", doc.id)
        assertEquals("Old note", doc.title)
        assertEquals(1, doc.pages.size)
        assertEquals(PageTemplate.LINED, doc.pages[0].template)
        assertEquals(1, doc.pages[0].layers.size)
        val layer = doc.pages[0].layers[0]
        assertEquals(1, layer.strokes.size)
        assertEquals(1f, layer.strokes[0].points[0].pressure)
        assertEquals(1, layer.texts.size)
        assertEquals("legacy", layer.texts[0].text)
        assertTrue(doc.pdf == null)
    }
}
