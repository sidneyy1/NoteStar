package com.riguel.notestar.data

import kotlin.math.hypot
import kotlin.math.max

/**
 * Pure stroke math: smoothing and the handwriting beautifier.
 * No Android dependencies, so it can be unit-tested on the JVM.
 */
object StrokeGeometry {

    private fun distance(a: StrokePoint, b: StrokePoint): Float =
        hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

    private fun lerp(a: StrokePoint, b: StrokePoint, t: Float): StrokePoint =
        StrokePoint(
            x = a.x + (b.x - a.x) * t,
            y = a.y + (b.y - a.y) * t,
            pressure = a.pressure + (b.pressure - a.pressure) * t
        )

    /** Drop points that are very close together (stylus jitter). */
    fun filterJitter(points: List<StrokePoint>, minDistance: Float = 1.5f): List<StrokePoint> {
        if (points.size < 3) return points
        val result = mutableListOf(points.first())
        for (point in points.drop(1).dropLast(1)) {
            if (distance(result.last(), point) >= minDistance) {
                result.add(point)
            }
        }
        result.add(points.last())
        return result
    }

    /**
     * Chaikin corner cutting: replaces each segment with two points at 1/4
     * and 3/4, which rounds corners while keeping the stroke's shape and
     * endpoints. More iterations = smoother.
     */
    fun chaikin(points: List<StrokePoint>, iterations: Int = 2): List<StrokePoint> {
        var current = points
        repeat(iterations) {
            if (current.size < 3) return current
            val next = mutableListOf(current.first())
            for (i in 0 until current.size - 1) {
                val a = current[i]
                val b = current[i + 1]
                next.add(lerp(a, b, 0.25f))
                next.add(lerp(a, b, 0.75f))
            }
            next.add(current.last())
            current = next
        }
        return current
    }

    /** Smooth rendering polyline for a raw stroke. */
    fun smoothed(points: List<StrokePoint>): List<StrokePoint> =
        chaikin(filterJitter(points), iterations = 2)

    /**
     * True when the stroke stays close to the straight line between its
     * endpoints (max deviation below [toleranceRatio] of the length).
     */
    fun isNearStraight(points: List<StrokePoint>, toleranceRatio: Float = 0.04f): Boolean {
        if (points.size < 3) return false
        val start = points.first()
        val end = points.last()
        val length = distance(start, end)
        if (length < 40f) return false
        val dx = end.x - start.x
        val dy = end.y - start.y
        var maxDeviation = 0f
        for (point in points) {
            // Perpendicular distance to the chord = |cross product| / length
            val cross = ((point.x - start.x) * dy - (point.y - start.y) * dx)
            val deviation = kotlin.math.abs(cross) / length
            if (deviation > maxDeviation) maxDeviation = deviation
        }
        return maxDeviation <= max(length * toleranceRatio, 6f)
    }

    /**
     * The beautifier: smooth the stroke and, when it is nearly a straight
     * line, align it to a clean straight segment. Meaning is never changed:
     * curves stay curves, only jitter and near-lines are cleaned up.
     */
    fun beautify(points: List<StrokePoint>): List<StrokePoint> {
        if (points.size < 3) return points
        if (isNearStraight(points)) {
            return listOf(points.first(), points.last())
        }
        return smoothed(points)
    }
}
