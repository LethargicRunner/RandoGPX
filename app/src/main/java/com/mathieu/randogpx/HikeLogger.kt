package com.mathieu.randogpx

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HikeLogger(
    private val trackName: String,
    private val weatherAtStart: Weather.Current?
) {
    data class RecPoint(val lat: Double, val lon: Double, val ele: Double?, val time: Long)

    private val startTime = System.currentTimeMillis()
    private val points = mutableListOf<RecPoint>()

    fun add(lat: Double, lon: Double, ele: Double?) {
        if (points.isEmpty() || GpxUtils.distance(
                points.last().lat, points.last().lon, lat, lon
            ) > 10
        ) points += RecPoint(lat, lon, ele, System.currentTimeMillis())
    }

    val recorded get() = points.toList()

    fun finish(context: Context): String {
        val endTime = System.currentTimeMillis()
        var dist = 0.0
        for (i in 1 until points.size) {
            dist += GpxUtils.distance(
                points[i - 1].lat, points[i - 1].lon, points[i].lat, points[i].lon
            )
        }
        val asc = TrackStats.compute(
            points.map { TrackPoint(it.lat, it.lon, it.ele, it.time) }
        ).ascentM

        val json = JSONObject().apply {
            put("rando", trackName)
            put("debut", iso(startTime))
            put("fin", iso(endTime))
            put("duree_min", (endTime - startTime) / 60000)
            put("distance_km", Math.round(dist / 10.0) / 100.0)
            put("denivele_positif_m", asc)
            put("points_enregistres", points.size)
            weatherAtStart?.let {
                put("meteo_depart", JSONObject().apply {
                    put("temperature_c", it.temperatureC)
                    put("vent_kmh", it.windKmh)
                    put("description", it.description)
                })
            }
            put("parcours", JSONArray().apply {
                points.forEach { p ->
                    put(JSONObject().apply {
                        put("lat", p.lat); put("lon", p.lon)
                        p.ele?.let { e -> put("ele", e) }
                        put("t", p.time)
                    })
                }
            })
        }

        val fileName = "log_" +
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.FRANCE)
                    .format(Date(startTime)) + "_" +
                trackName.substringBeforeLast('.').replace(' ', '_') + ".json"

        val bytes = json.toString(2).toByteArray()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/" + GpxRepository.FOLDER + "/logs")
            }
            context.contentResolver.insert(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values
            )?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            }
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                GpxRepository.FOLDER + "/logs"
            ).apply { mkdirs() }
            File(dir, fileName).writeBytes(bytes)
        }

        return "Rando enregistrée : %.1f km, %d min, D+ %d m%s".format(
            dist / 1000,
            (endTime - startTime) / 60000,
            asc,
            weatherAtStart?.let { " | " + it.description + ", " + it.temperatureC + "°C" } ?: ""
        )
    }

    private fun iso(t: Long): String = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US
    ).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(t))
}
