package com.riguel.notestar

import com.riguel.notestar.data.StrokeGeometry
import com.riguel.notestar.data.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class StrokeGeometryTest {

    @Test
    fun chaikinKeepsEndpoints() {
        val points = listOf(
            StrokePoint(0f, 0f),
            StrokePoint(50f, 80f),
            StrokePoint(100f, 0f)
        )
        val result = StrokeGeometry.chaikin(points, iterations = 1)
        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
        assertTrue(result.size > points.size)
    }

    @Test
    fun filterJitterRemovesClosePoints() {
        val points = listOf(
            StrokePoint(0f, 0f),
            StrokePoint(0.5f, 0.5f),
            StrokePoint(1f, 1f),
            StrokePoint(50f, 50f)
        )
        val result = StrokeGeometry.filterJitter(points, minDistance = 5f)
        assertEquals(2, result.size)
        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
    }

    @Test
    fun beautifyStraightensNearStraightLine() {
        // A long line with tiny wobbles should become a clean 2-point segment.
        val points = (0..10).map { i ->
            StrokePoint(i * 20f, i * 20f + if (i % 2 == 0) 1f else -1f)
        }
        val result = StrokeGeometry.beautify(points)
        assertEquals(2, result.size)
        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
    }

    @Test
    fun beautifyKeepsCurvesCurved() {
        // A quarter circle arc must not be straightened away.
        val points = (0..12).map { i ->
            val angle = (i / 12.0) * (PI / 2)
            StrokePoint(
                x = (100 * cos(angle)).toFloat(),
                y = (100 * sin(angle)).toFloat()
            )
        }
        val result = StrokeGeometry.beautify(points)
        assertTrue(result.size > 2)
    }
}
