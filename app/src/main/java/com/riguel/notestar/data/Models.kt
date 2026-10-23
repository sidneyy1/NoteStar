package com.riguel.notestar.data

/** Drawing tools available in the editor. */
enum class Tool { PEN, HIGHLIGHTER, ERASER }

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

/** Lightweight info about a saved note, shown in the notebook list. */
data class NoteMeta(
    val id: String,
    val title: String,
    val updatedAt: Long
)
