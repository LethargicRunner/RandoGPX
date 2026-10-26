package com.mathieu.randogpx

object TrackStats {

    data class Stats(
        val distanceKm: Double,
        val durationMin: Long?,
        val ascentM: Int,
        val descentM: Int,
        val elevMinM: Int?,
        val elevMaxM: Int?,
        val avgSpeedKmh: Double?
    )

    fun compute(track: List<TrackPoint>): Stats {
        var dist = 0.0
        for (i in 1 until track.size) {
            dist += GpxUtils.distance(
                track[i - 1].lat, track[i - 1].lon, track[i].lat, track[i].lon
            )
        }

        val elevs = track.mapNotNull { it.ele }
        var ascent = 0.0; var descent = 0.0
        if (elevs.size > 1) {
            val filtered = smooth(elevs, 3.0)
            for (i in 1 until filtered.size) {
                val d = filtered[i] - filtered[i - 1]
                if (d > 0) ascent += d else descent -= d
            }
        }

        val times = track.mapNotNull { it.time }
        val duration = if (times.size > 1) times.last() - times.min() else null
        val avgSpeed = duration?.takeIf { it > 0 }?.let { (dist / 1000.0) / (it / 3600000.0) }

        return Stats(
            distanceKm = dist / 1000.0,
            durationMin = duration?.let { it / 60000 },
            ascentM = ascent.toInt(),
            descentM = descent.toInt(),
            elevMinM = elevs.minOrNull()?.toInt(),
            elevMaxM = elevs.maxOrNull()?.toInt(),
            avgSpeedKmh = avgSpeed
        )
    }

    private fun smooth(values: List<Double>, threshold: Double): List<Double> {
        val out = mutableListOf(values.first())
        var ref = values.first()
        for (v in values.drop(1)) {
            if (kotlin.math.abs(v - ref) >= threshold) {
                out += v
                ref = v
            }
        }
        return out
    }
}
