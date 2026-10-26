package com.mathieu.randogpx

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Weather {

    data class Current(
        val temperatureC: Double,
        val windKmh: Double,
        val description: String
    )

    /** Récupère la météo actuelle. Retourne null si pas de réseau. Thread secondaire requis. */
    fun fetch(lat: Double, lon: Double): Current? = try {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                "&current=temperature_2m,weather_code,wind_speed_10m"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val current = JSONObject(body).getJSONObject("current")
        Current(
            temperatureC = current.getDouble("temperature_2m"),
            windKmh = current.getDouble("wind_speed_10m"),
            description = describe(current.optInt("weather_code", -1))
        )
    } catch (_: Exception) { null }

    fun describe(code: Int): String = when (code) {
        0 -> "ciel dégagé"
        1, 2 -> "peu nuageux"
        3 -> "nuageux"
        45, 48 -> "brouillard"
        in 51..57 -> "bruine"
        in 61..67 -> "pluie"
        in 71..77 -> "neige"
        in 80..82 -> "averses"
        85, 86 -> "averses de neige"
        95, 96, 99 -> "orage"
        else -> "météo indisponible"
    }
}
