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

    data class ForecastDay(
        val date: String,
        val tmin: Double,
        val tmax: Double,
        val precipProb: Int,
        val code: Int
    )

    fun codeEmoji(code: Int): String = when (code) {
        0 -> "☀️"
        1 -> "🌤️"
        2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..67 -> "🌦️"
        in 71..77 -> "🌨️"
        in 80..82 -> "🌧️"
        in 95..99 -> "⛈️"
        else -> "•"
    }

    /** Prévisions sur 7 jours. Thread secondaire requis. Retourne null si pas de réseau. */
    fun fetchForecast(lat: Double, lon: Double): List<ForecastDay>? = try {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&timezone=auto&forecast_days=7"
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val daily = JSONObject(body).getJSONObject("daily")
        val n = daily.getJSONArray("time").length()
        (0 until n).map { i ->
            ForecastDay(
                daily.getJSONArray("time").getString(i),
                daily.getJSONArray("temperature_2m_min").getDouble(i),
                daily.getJSONArray("temperature_2m_max").getDouble(i),
                daily.getJSONArray("precipitation_probability_max").optInt(i, 0),
                daily.getJSONArray("weather_code").getInt(i)
            )
        }
    } catch (e: Exception) { null }
}