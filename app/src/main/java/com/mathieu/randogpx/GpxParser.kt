package com.mathieu.randogpx

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class TrackPoint(
    val lat: Double,
    val lon: Double,
    val ele: Double? = null,
    val time: Long? = null
)

data class Waypoint(val name: String, val lat: Double, val lon: Double)

data class GpxData(val track: List<TrackPoint>, val waypoints: List<Waypoint>)

object GpxParser {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parse(inputStream: InputStream): GpxData {
        val track = mutableListOf<TrackPoint>()
        val waypoints = mutableListOf<Waypoint>()

        val parser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }.newPullParser()
        parser.setInput(inputStream, null)

        var event = parser.eventType
        var currentWptName: String? = null
        var inWaypoint = false
        var inTrkpt = false
        var curEle: Double? = null
        var curTime: Long? = null
        var pendingLatLon: Pair<Double, Double>? = null

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "wpt" -> {
                        inWaypoint = true
                        currentWptName = null
                    }
                    "trkpt" -> {
                        inTrkpt = true
                        curEle = null
                        curTime = null
                        val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        pendingLatLon = if (lat != null && lon != null) lat to lon else null
                    }
                    "name" -> if (inWaypoint) currentWptName = parser.nextText()
                    "ele" -> if (inTrkpt) curEle = parser.nextText().toDoubleOrNull()
                    "time" -> if (inTrkpt) curTime = parseTime(parser.nextText())
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "trkpt" -> {
                        pendingLatLon?.let { (lat, lon) ->
                            track += TrackPoint(lat, lon, curEle, curTime)
                        }
                        pendingLatLon = null
                        inTrkpt = false
                    }
                    "wpt" -> {
                        val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        if (lat != null && lon != null) {
                            waypoints += Waypoint(currentWptName ?: "WP" + (waypoints.size + 1), lat, lon)
                        }
                        inWaypoint = false
                    }
                }
            }
            event = parser.next()
        }
        return GpxData(track, waypoints)
    }

    private fun parseTime(raw: String): Long? = try {
        timeFormat.parse(raw.substringBefore('Z').substringBefore('.'))?.time
    } catch (_: Exception) { null }
}
