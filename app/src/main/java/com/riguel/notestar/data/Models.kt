package com.riguel.notestar.data

/** Tools available in the editor. */
enum class Tool { PEN, HIGHLIGHTER, TEXT, ERASER }

/** Page background chosen when a note is created. */
enum class PageTemplate { BLANK, LINED }

/** One point along a stroke, in canvas coordinates. */
data class StrokePoint(val x: Float, val y: Float)

/**
 * A single drawn stroke.
 * [colorValue] is the raw ULong value of a Compose Color, stored as-is so a
 * note round-trips exactly through JSON.
 */
data class Stroke(
    val points: List<StrokePoint>,
    val colorValue: ULong,
    val thickness: Float,
    val highlighter: Boolean
)

/** A block of typed text placed on the page. */
data class TextElement(
    val id: String,
    val x: Float,
    val y: Float,
    val text: String,
    val colorValue: ULong,
    val fontSizeSp: Float
)

/** Lightweight info about a saved note, shown in the notebook list. */
data class NoteMeta(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val template: PageTemplate = PageTemplate.BLANK
)

/** A fully loaded note: metadata plus everything drawn/typed on the page. */
data class LoadedNote(
    val meta: NoteMeta,
    val strokes: List<Stroke>,
    val texts: List<TextElement>
)
