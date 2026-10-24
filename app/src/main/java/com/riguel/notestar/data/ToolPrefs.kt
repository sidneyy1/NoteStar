package com.riguel.notestar.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * App preferences for the shared tool options sheet (v0.15.0):
 * recent colors, saved custom colors, the last color and size per
 * tool, and the pressure-sensitivity toggle. Stored in one
 * SharedPreferences file ("tool_options") shared by the normal-note
 * editor and the imported-PDF editor, so both behave the same.
 *
 * Colors are stored as ARGB ints (Color.toArgb), lists as
 * comma-separated ints. Nothing here touches the note JSON format:
 * this is UI state only, and a missing or corrupt entry falls back
 * to the caller's default.
 */
object ToolPrefs {
    private const val FILE = "tool_options"
    private const val KEY_RECENTS = "recent_colors"
    private const val KEY_CUSTOMS = "custom_colors"
    private const val KEY_PRESSURE = "pressure_enabled"
    const val MAX_RECENTS = 8
    const val MAX_CUSTOMS = 16

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun readList(context: Context, key: String): List<Color> {
        val raw = prefs(context).getString(key, null) ?: return emptyList()
        return raw.split(",").mapNotNull { it.toIntOrNull()?.let { v -> Color(v) } }
    }

    private fun writeList(context: Context, key: String, colors: List<Color>) {
        prefs(context).edit()
            .putString(key, colors.joinToString(",") { it.toArgb().toString() })
            .apply()
    }

    /** Last 8 colors the user committed (swatch tap or Done), newest first. */
    fun recentColors(context: Context): List<Color> =
        readList(context, KEY_RECENTS)

    /** Prepends [color], deduped, capped at [MAX_RECENTS]. */
    fun pushRecent(context: Context, color: Color) {
        val updated = (listOf(color) + readList(context, KEY_RECENTS))
            .distinct()
            .take(MAX_RECENTS)
        writeList(context, KEY_RECENTS, updated)
    }

    /** User-saved custom colors, oldest first, capped at [MAX_CUSTOMS]. */
    fun customColors(context: Context): List<Color> =
        readList(context, KEY_CUSTOMS)

    /** Adds [color] unless already saved; drops the oldest if full. */
    fun addCustom(context: Context, color: Color) {
        val current = readList(context, KEY_CUSTOMS)
        if (color in current) return
        val updated = (current + color).takeLast(MAX_CUSTOMS)
        writeList(context, KEY_CUSTOMS, updated)
    }

    fun removeCustom(context: Context, color: Color) {
        writeList(context, KEY_CUSTOMS, readList(context, KEY_CUSTOMS) - color)
    }

    // --- Per-tool memory -------------------------------------------------
    // Keys: "pen_pencil", "pen_pen", "pen_calligraphy",
    // "highlighter", "eraser", "text". A tool's last color/size is
    // restored when the user switches back to it.

    fun colorForTool(context: Context, key: String, default: Color): Color {
        val raw = prefs(context).getInt("color_$key", Int.MIN_VALUE)
        return if (raw == Int.MIN_VALUE) default else Color(raw)
    }

    fun saveColorForTool(context: Context, key: String, color: Color) {
        prefs(context).edit().putInt("color_$key", color.toArgb()).apply()
    }

    fun sizeForTool(context: Context, key: String, default: Float): Float {
        return prefs(context).getFloat("size_$key", Float.NaN).let {
            if (it.isNaN()) default else it
        }
    }

    fun saveSizeForTool(context: Context, key: String, size: Float) {
        prefs(context).edit().putFloat("size_$key", size).apply()
    }

    /** The last pen sub-type (pencil / pen / calligraphy). */
    fun penType(context: Context, default: String): String =
        prefs(context).getString("pen_type", null) ?: default

    fun savePenType(context: Context, penType: String) {
        prefs(context).edit().putString("pen_type", penType).apply()
    }

    fun pressureEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PRESSURE, true)

    fun setPressureEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PRESSURE, enabled).apply()
    }
}
