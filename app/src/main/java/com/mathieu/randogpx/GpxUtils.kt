package com.mathieu.randogpx

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GpxUtils {
    private const val EARTH_R = 6371000.0

    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_R * atan2(sqrt(a), sqrt(1 - a))
    }

    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
        val dl = Math.toRadians(lon2 - lon1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun projectOnTrack(lat: Double, lon: Double, track: List<TrackPoint>):
            ProjectionResult {
        if (track.size < 2) return ProjectionResult(0, 0.0, 0.0, 0.0)

        val cum = DoubleArray(track.size)
        for (i in 1 until track.size) {
            cum[i] = cum[i - 1] + distance(
                track[i - 1].lat, track[i - 1].lon, track[i].lat, track[i].lon
            )
        }

        var bestSeg = 0
        var bestDist = Double.MAX_VALUE
        var bestAlong = 0.0

        for (i in 0 until track.size - 1) {
            val (d, along) = distanceToSegment(
                lat, lon,
                track[i].lat, track[i].lon,
                track[i + 1].lat, track[i + 1].lon
            )
            if (d < bestDist) { bestDist = d; bestSeg = i; bestAlong = along }
        }
        return ProjectionResult(bestSeg, bestDist, cum[bestSeg] + bestAlong, cum.last())
    }

    private fun distanceToSegment(
        pLat: Double, pLon: Double,
        aLat: Double, aLon: Double,
        bLat: Double, bLon: Double
    ): Pair<Double, Double> {
        val kx = 111320.0 * cos(Math.toRadians(pLat))
        val ky = 110540.0
        val px = pLon * kx; val py = pLat * ky
        val ax = aLon * kx; val ay = aLat * ky
        val bx = bLon * kx; val by = bLat * ky
        val dx = bx - ax; val dy = by - ay
        val lenSq = dx * dx + dy * dy
        val t = if (lenSq == 0.0) 0.0
        else (((px - ax) * dx + (py - ay) * dy) / lenSq).coerceIn(0.0, 1.0)
        val cx = ax + t * dx; val cy = ay + t * dy
        return sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy)) to t * sqrt(lenSq)
    }
}

data class ProjectionResult(
    val segmentIndex: Int,
    val distanceToTrack: Double,
    val distanceCovered: Double,
    val totalDistance: Double
)
