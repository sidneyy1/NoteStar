package com.riguel.notestar.data

import kotlin.math.abs

/**
 * Pure, UI-free logic for the tool options sheet (v0.15.2), kept
 * here — with no Compose or Android imports — so the unit test can
 * exercise the exact functions the sheet uses. The sheet itself only
 * reads these results; it stores no "selected" boolean of its own.
 */

/**
 * Single-choice preset selection: the chip whose preset is nearest
 * to [size], and only if it is within [tolerance] (0.25 pt per
 * spec). Returns the preset's index, or null when no preset is
 * close enough — so the highlight clears when the value is dragged
 * or typed off a preset. Never a range compare against a preset,
 * so the same size can never match two chips: the nearest is unique
 * (ties go to the first, and the tables below have no duplicates).
 */
fun selectedPresetIndex(
    size: Float,
    presets: List<Float>,
    tolerance: Float = 0.25f
): Int? {
    if (presets.isEmpty()) return null
    var best = 0
    var bestDiff = abs(size - presets[0])
    for (i in 1 until presets.size) {
        val diff = abs(size - presets[i])
        if (diff < bestDiff) {
            best = i
            bestDiff = diff
        }
    }
    return if (bestDiff <= tolerance) best else null
}

/**
 * One preset table per tool (pt). The pen table is the spec's
 * exact table; the others are scaled to their own ranges (all
 * values inside the tool's range, no duplicates, so the nearest
 * rule above can never tie on two chips).
 */
fun presetsForTool(tool: Tool, penType: String): List<Float> = when (tool) {
    Tool.PEN -> when (penType) {
        PenTypes.PENCIL -> listOf(1f, 2f, 4f, 8f, 15f)
        PenTypes.CALLIGRAPHY -> listOf(2f, 4f, 8f, 15f, 30f)
        else -> listOf(1f, 2.5f, 5f, 10f, 20f)
    }
    Tool.HIGHLIGHTER -> listOf(8f, 15f, 25f, 40f, 60f)
    Tool.ERASER -> listOf(5f, 10f, 20f, 40f, 80f)
    Tool.TEXT -> listOf(8f, 12f, 16f, 24f, 36f)
    else -> listOf(1f, 2.5f, 5f, 10f, 20f)
}

/**
 * Recent colors, stored as ARGB ints: prepend [color], de-duplicate
 * by exact RGBA, most recent first, capped at [max]. Pushing a
 * color that is already present moves it to the front without
 * adding a second copy — so picking the same color again never
 * grows the list.
 */
fun pushRecentColors(
    recents: List<Int>,
    color: Int,
    max: Int = 8
): List<Int> = (listOf(color) + recents).distinct().take(max)
