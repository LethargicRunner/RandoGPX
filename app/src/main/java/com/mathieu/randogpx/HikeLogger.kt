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

/**
 * Journal de randonnée V2 :
 * - gestion de la pause (durée nette, points uniquement en enregistrement)
 * - sauvegarde automatique périodique (survit à un crash / batterie vide)
 * - finalisation dans Documents/RandoGPX/logs/ en JSON lisible
 */
class HikeLogger(
    private val trackName: String,
    private val weatherAtStart: Weather.Current?
) {
    data class RecPoint(val lat: Double, val lon: Double, val ele: Double?, val time: Long)

    private val startTime = System.currentTimeMillis()
    private var pausedMs = 0L
    private var pauseStart: Long? = null
    private val points = mutableListOf<RecPoint>()

    fun pause() { if (pauseStart == null) pauseStart = System.currentTimeMillis() }
    fun resume() { pauseStart?.let { pausedMs += System.currentTimeMillis() - it }; pauseStart = null }

    fun add(lat: Double, lon: Double, ele: Double?) {
        if (pauseStart != null) return   // pas de points pendant la pause
        if (points.isEmpty() || GpxUtils.distance(
                points.last().lat, points.last().lon, lat, lon
            ) > 10
        ) points += RecPoint(lat, lon, ele, System.currentTimeMillis())
    }

    val recorded get() = points.toList()
    val isPaused get() = pauseStart != null

    private fun elapsedMs(): Long {
        val end = pauseStart ?: System.currentTimeMillis()
        return end - startTime - pausedMs
    }

    private fun buildJson(meta: Triple<String, Int, String>? = null): JSONObject {
        var dist = 0.0
        for (i in 1 until points.size) {
            dist += GpxUtils.distance(
                points[i - 1].lat, points[i - 1].lon, points[i].lat, points[i].lon
            )
        }
        val asc = TrackStats.compute(
            points.map { TrackPoint(it.lat, it.lon, it.ele, it.time) }
        ).ascentM

        return JSONObject().apply {
            put("rando", trackName)
            put("debut", iso(startTime))
            meta?.let {
                if (it.first.isNotBlank()) put("rando", it.first)
                if (it.second > 0) put("difficulte_etoiles", it.second)
                if (it.third.isNotBlank()) put("note", it.third)
            }
            put("duree_min", elapsedMs() / 60000)
            put("distance_km", Math.round(dist / 10.0) / 100.0)
            put("denivele_positif_m", asc)
            put("points_enregistres", points.size)
            put("en_pause", pauseStart != null)
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
    }

    /** Export GPX standard du parcours réel (gpx.studio, Visorando…). */
    private fun buildGpx(hikeName: String, stars: Int, note: String): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"RandoGPX\" ")
            .append("xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <metadata>\n    <name>")
            .append(xmlEscape(hikeName)).append("</name>\n")
        if (note.isNotBlank())
            sb.append("    <desc>").append(xmlEscape(note)).append("</desc>\n")
        if (stars > 0)
            sb.append("    <keywords>difficulte=").append(stars).append("</keywords>\n")
        sb.append("    <time>")
            .append(iso(startTime)).append("</time>\n  </metadata>\n")
        sb.append("  <trk>\n    <name>")
            .append(xmlEscape(hikeName)).append("</name>\n    <trkseg>\n")
        points.forEach { p ->
            sb.append("      <trkpt lat=\"").append(p.lat).append("\" lon=\"")
                .append(p.lon).append("\">")
            p.ele?.let { sb.append("<ele>").append(it).append("</ele>") }
            sb.append("<time>").append(iso(p.time)).append("</time>")
            sb.append("</trkpt>\n")
        }
        sb.append("    </trkseg>\n  </trk>\n</gpx>\n")
        return sb.toString()
    }

    private fun xmlEscape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    /** Sauvegarde automatique (toutes les 5 min) : fichier privé, écrasé à chaque fois. */
    fun autosave(context: Context) {
        try {
            File(context.filesDir, AUTOSAVE).writeText(buildJson().toString(2))
        } catch (_: Exception) { }
    }

    /** Abandonne la rando sans rien écrire (trajet quasi vide). */
    fun discard(context: Context) {
        File(context.filesDir, AUTOSAVE).delete()
    }

    /** Termine la rando : journal final dans Documents/RandoGPX/logs/, autosave supprimé. */
    fun finish(context: Context, meta: Triple<String, Int, String>? = null): String {
        val hikeName = meta?.first?.takeIf { it.isNotBlank() }
            ?: trackName.substringBeforeLast('.')
        val stars = meta?.second ?: 0
        val note = meta?.third ?: ""

        val json = buildJson(meta)
        val endTime = System.currentTimeMillis()

        val fileName = "log_" +
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.FRANCE)
                    .format(Date(startTime)) + "_" +
                hikeName.replace(' ', '_') + ".json"

        writePublic(context, fileName, json.toString(2).toByteArray())
        File(context.filesDir, AUTOSAVE).delete()

        // Export GPX du parcours réel : rangé dans Documents/RandoGPX,
        // visible dans « Mes GPX », partageable et réimportable
        var gpxFile = ""
        if (points.size >= 2) {
            try {
                val gpxName = "rando_" +
                    SimpleDateFormat("yyyyMMdd_HHmm", Locale.FRANCE).format(Date(startTime)) +
                    "_" + hikeName.replace(' ', '_')
                val saved = GpxRepository.save(context, gpxName, buildGpx(hikeName, stars, note).toByteArray())
                gpxFile = "Documents/RandoGPX/" + saved
            } catch (_: Exception) { }
        }

        var dist = 0.0
        for (i in 1 until points.size) {
            dist += GpxUtils.distance(
                points[i - 1].lat, points[i - 1].lon, points[i].lat, points[i].lon
            )
        }
        return "Rando enregistrée : %.1f km, %d min, D+ %d m%s".format(
            dist / 1000,
            elapsedMs() / 60000,
            TrackStats.compute(points.map { TrackPoint(it.lat, it.lon, it.ele, it.time) }).ascentM,
            weatherAtStart?.let { " | " + it.description + ", " + it.temperatureC + "°C" } ?: ""
        ) + (if (gpxFile.isNotEmpty()) "\n📁 GPX : " + gpxFile else "")
    }

    companion object {
        private const val AUTOSAVE = "autosave_rando.json"

        /** Une rando interrompue (crash/batterie) attend-elle d'être sauvée ? */
        fun pendingAutosave(context: Context): Boolean =
            File(context.filesDir, AUTOSAVE).exists()

        /** Sauvegarde l'auto-recovery dans les journaux publics. Retourne le nom du fichier. */
        fun finalizePending(context: Context): String? {
            val f = File(context.filesDir, AUTOSAVE)
            if (!f.exists()) return null
            val name = "log_recupere_" +
                SimpleDateFormat("yyyyMMdd_HHmm", Locale.FRANCE).format(Date()) + ".json"
            writePublic(context, name, f.readText().toByteArray())
            f.delete()
            return name
        }

        fun discardPending(context: Context) { File(context.filesDir, AUTOSAVE).delete() }

        private fun writePublic(context: Context, fileName: String, bytes: ByteArray) {
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
        }
    }

    private fun iso(t: Long): String = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US
    ).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(t))
}
