package com.riguel.notestar

import com.riguel.notestar.data.PenTypes
import com.riguel.notestar.data.Tool
import com.riguel.notestar.data.presetsForTool
import com.riguel.notestar.data.pushRecentColors
import com.riguel.notestar.data.selectedPresetIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * v0.15.2 selection-state tests, in pure form (no device):
 *
 * 1. Size presets are single-choice: for each size in the spec's
 *    list, at most one preset is selected — and 8.75 pt selects
 *    none, because no pen preset is within 0.25 pt of it.
 * 2. Tapping each chip in turn selects exactly that chip, and
 *    dragging off a preset clears the highlight.
 * 3. Recents built from [A, B, A, C, A] equal [A, C, B] — most
 *    recent first, no duplicates — and picking the same color
 *    again adds no second copy.
 */
class ToolSelectionTest {

    private val penPresets = listOf(1f, 2.5f, 5f, 10f, 20f)

    @Test
    fun penPresetSelectionAtMostOne() {
        val cases: List<Pair<Float, Int?>> = listOf(
            0.5f to null,
            1f to 0,
            2.5f to 1,
            5f to 2,
            8.75f to null,
            10f to 3,
            20f to 4
        )
        for ((size, expected) in cases) {
            // A single index or null by construction — never two.
            assertEquals(
                "size $size",
                expected,
                selectedPresetIndex(size, penPresets)
            )
        }
        // The table itself has no duplicates that could tie.
        assertEquals(penPresets, penPresets.distinct())
    }

    @Test
    fun tappingChipsSelectsExactlyOne() {
        for (i in penPresets.indices) {
            assertEquals(
                "tapping preset $i",
                i,
                selectedPresetIndex(penPresets[i], penPresets)
            )
        }
        // Dragging the slider off a preset clears the highlight.
        assertNull(selectedPresetIndex(8.75f, penPresets))
        assertNull(selectedPresetIndex(7.3f, penPresets))
    }

    @Test
    fun penTableMatchesSpec() {
        assertEquals(
            penPresets,
            presetsForTool(Tool.PEN, PenTypes.PEN)
        )
    }

    @Test
    fun recentsDedupeAndOrder() {
        val a = 0xFF000000.toInt()
        val b = 0xFF0000FF.toInt()
        val c = 0xFFFF0000.toInt()
        var recents = emptyList<Int>()
        for (color in listOf(a, b, a, c, a)) {
            recents = pushRecentColors(recents, color)
        }
        assertEquals(listOf(a, c, b), recents)
        assertEquals(recents, recents.distinct())
        // Picking the same color again adds no duplicate.
        assertEquals(listOf(a, c, b), pushRecentColors(recents, a))
        // Cap respected.
        var long = emptyList<Int>()
        for (i in 0 until 12) long = pushRecentColors(long, i)
        assertEquals(8, long.size)
        assertEquals(long, long.distinct())
    }
}
