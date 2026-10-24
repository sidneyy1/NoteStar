package com.riguel.notestar.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens for the NoteStar redesign: clean, minimal, light theme with
 * a single teal-blue accent. Tweak values here to restyle the whole app.
 */
object DesignTokens {
    // Colors (light theme)
    val Background = Color(0xFFFAFAFA)
    val SidebarBackground = Color(0xFFF4F4F2)
    val Card = Color.White
    val Accent = Color(0xFF1E8FB5)
    val AccentTint = Color(0xFFE1F1F7)
    val BannerBackground = Color(0xFFE8F4F9)
    val TextPrimary = Color(0xFF222222)
    val TextSecondary = Color(0xFF999999)
    val Divider = Color(0xFFE5E5E5)
    val TabInactive = Color(0xFFEEEEEE)
    val TabBarBackground = Color(0xFFF0F0EE)

    // Editor (document-viewer style)
    val CanvasBackground = Color(0xFFF2F2F2)
    val IconGray = Color(0xFF444444)
    val PageSurface = Color.White

    // Cover colors offered when creating a note (also used as list thumbnails).
    val CoverColors: List<Pair<String, Color>> = listOf(
        "Sky" to Color(0xFF8ECAE6),
        "Teal" to Color(0xFF1E8FB5),
        "Mint" to Color(0xFF95D5B2),
        "Lemon" to Color(0xFFFFE066),
        "Peach" to Color(0xFFFFB4A2),
        "Rose" to Color(0xFFFFC8DD),
        "Lavender" to Color(0xFFCDB4DB),
        "Graphite" to Color(0xFF5A5A5A),
        "Ink" to Color(0xFF222222),
        "Paper" to Color(0xFFF1EAD8)
    )

    // Shape + spacing
    val CardRadius = 16.dp
    val ItemRadius = 12.dp
    val PillRadius = 20.dp
    val ScreenPadding = 16.dp
}

/** A dashed rounded-rect border, used by the Create card and unselected pickers. */
fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp = 12.dp,
    strokeWidth: Dp = 1.5.dp
): Modifier = drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
    )
    drawRoundRect(
        color = color,
        size = Size(size.width, size.height),
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
        style = stroke
    )
}
