package com.mathieu.randogpx

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Éphémérides calculées localement (sans réseau) :
 * lever/coucher du soleil (algorithme NOAA), phase de lune.
 */
object Ephemeris {

    /**
     * Lever et coucher du soleil en ms epoch, ou null si aucun événement
     * (nuit polaire ou soleil de minuit).
     */
    fun sunTimes(lat: Double, lon: Double, nowMs: Long): Pair<Long?, Long?> {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = nowMs
        val dayStart = utc.clone() as Calendar
        dayStart.set(Calendar.HOUR_OF_DAY, 0)
        dayStart.set(Calendar.MINUTE, 0)
        dayStart.set(Calendar.SECOND, 0)
        dayStart.set(Calendar.MILLISECOND, 0)
        val doy = utc.get(Calendar.DAY_OF_YEAR)

        val gamma = 2.0 * PI / 365.0 * (doy - 1 + 0.5)
        val eqTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma)
                - 0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
        val decl = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma)
                - 0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma)
                - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        val latR = Math.toRadians(lat)
        val zenithR = Math.toRadians(90.833)
        val cosHa = cos(zenithR) / (cos(latR) * cos(decl)) - tan(latR) * tan(decl)
        if (cosHa < -1.0 || cosHa > 1.0) return null to null
        val ha = Math.toDegrees(acos(cosHa))
        val sunriseMin = 720.0 - 4.0 * (lon + ha) - eqTime
        val sunsetMin = 720.0 - 4.0 * (lon - ha) - eqTime
        return (dayStart.timeInMillis + sunriseMin * 60000.0).toLong() to
                (dayStart.timeInMillis + sunsetMin * 60000.0).toLong()
    }

    /** Phase lunaire : 0 = nouvelle lune, 0.5 = pleine lune. */
    fun moonPhase(nowMs: Long): Double {
        val jd = nowMs / 86400000.0 + 2440587.5
        val phase = ((jd - 2451550.1) / 29.530588853) % 1.0
        return if (phase < 0) phase + 1.0 else phase
    }

    fun moonName(phase: Double): String = when {
        phase < 0.03 || phase > 0.97 -> "Nouvelle lune"
        phase < 0.22 -> "Premier croissant"
        phase < 0.28 -> "Premier quartier"
        phase < 0.47 -> "Gibbeuse croissante"
        phase < 0.53 -> "Pleine lune"
        phase < 0.72 -> "Gibbeuse décroissante"
        phase < 0.78 -> "Dernier quartier"
        else -> "Dernier croissant"
    }

    fun moonIllumination(phase: Double): Int =
        ((1.0 - cos(2.0 * PI * phase)) / 2.0 * 100).toInt()
}