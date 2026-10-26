package com.mathieu.randogpx

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.PI
import kotlin.math.tan

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var map: MapView
    private lateinit var tvStatus: TextView
    private lateinit var tvInfo: TextView
    private lateinit var arrow: ImageView
    private lateinit var btnFollow: ToggleButton

    private var track: List<TrackPoint> = emptyList()
    private var waypoints: List<Waypoint> = emptyList()
    private var trackName = "rando"
    private var trackStats: TrackStats.Stats? = null

    private var userMarker: Marker? = null
    private var trackLine: Polyline? = null
    private var nextWp: Waypoint? = null

    private var currentLat: Double? = null
    private var currentLon: Double? = null
    private var currentAlt: Double? = null
    private var lastSpeed = 0f
    private var lastAlertTime = 0L
    private var followMode = true

    private var hikeLogger: HikeLogger? = null
    private var weather: Weather.Current? = null

    private var azimuth = 0f
    private var targetBearing: Double? = null
    private lateinit var sensorManager: SensorManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it.values.any { granted -> granted }) startLocationUpdates()
            else Toast.makeText(this, "Permission localisation refusée", Toast.LENGTH_LONG).show()
        }

    private val openGpx =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importAndSaveGpx(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(getExternalFilesDir(null), "osmdroid")
        }
        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
        tvStatus = findViewById(R.id.tvStatus)
        tvInfo = findViewById(R.id.tvInfo)
        arrow = findViewById(R.id.compassArrow)
        btnFollow = findViewById(R.id.btnFollow)

        map.setTileSource(TileSources.OPENSTREETMAP)
        map.setMultiTouchControls(true)
        map.controller.setZoom(16.0)

        findViewById<Button>(R.id.btnImport).setOnClickListener {
            openGpx.launch(arrayOf("application/gpx+xml", "application/xml", "text/xml", "*/*"))
        }
        findViewById<Button>(R.id.btnLibrary).setOnClickListener { showLibrary() }
        findViewById<Button>(R.id.btnTiles).setOnClickListener { showTileSourceDialog() }
        findViewById<Button>(R.id.btnOffline).setOnClickListener { downloadOfflineArea() }
        findViewById<Button>(R.id.btnStats).setOnClickListener { showStats() }
        findViewById<Button>(R.id.btnWaypoints).setOnClickListener { showWaypointDialog() }
        findViewById<Button>(R.id.btnEndHike).setOnClickListener { confirmEndHike() }
        btnFollow.setOnCheckedChangeListener { _, checked ->
            followMode = checked
            if (checked) recentreOnUser()
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "RandoGPX:keepScreenOn")

        if (!hasLocationPermission()) requestPermissions.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        wakeLock?.acquire(10 * 60 * 60 * 1000L)
        if (hasLocationPermission()) startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    private fun showStats() {
        val s = trackStats
            ?: run { Toast.makeText(this, "Importez d'abord un tracé GPX", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this)
            .setTitle("📊 Stats — " + trackName)
            .setMessage(
                "Distance : %.2f km\n".format(s.distanceKm) +
                ("Durée du tracé : " + (s.durationMin ?: "?") + " min\n") +
                "Dénivelé positif : " + s.ascentM + " m\n" +
                "Dénivelé négatif : " + s.descentM + " m\n" +
                "Altitude : " + (s.elevMinM ?: "?") + " m → " + (s.elevMaxM ?: "?") + " m\n" +
                "Vitesse moyenne : " + (s.avgSpeedKmh?.let { "%.1f km/h".format(it) } ?: "?") +
                "\n\nPoints : " + track.size
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLibrary() {
        val files = GpxRepository.list(this)
        if (files.isEmpty()) {
            Toast.makeText(this, "Aucun GPX dans Documents/RandoGPX", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("📁 Mes GPX (" + files.size + ")")
            .setItems(files.map { it.first }.toTypedArray()) { _, which ->
                val (_, uri) = files[which]
                loadGpx(uri, files[which].first)
            }
            .setNegativeButton("Fermer", null)
            .show()
    }

    private fun showTileSourceDialog() {
        val sources = arrayOf(
            TileSources.OPENSTREETMAP,
            TileSources.OPENTOPOMAP,
            TileSources.IGN_PLAN
        )
        val names = sources.map { TileSources.nameOf(it) }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Fond de carte")
            .setItems(names) { _, which ->
                map.setTileSource(sources[which])
                map.invalidate()
                Toast.makeText(this, "Carte : " + names[which], Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun downloadOfflineArea() {
        if (track.size < 2) {
            Toast.makeText(this, "Importez d'abord un tracé GPX", Toast.LENGTH_SHORT).show()
            return
        }
        val bbox = BoundingBox.fromGeoPoints(track.map { GeoPoint(it.lat, it.lon) })
            .increaseByScale(1.15f)

        val zoomMin = 14
        val zoomMax = 18
        val possible = estimateTiles(bbox, zoomMin, zoomMax)

        AlertDialog.Builder(this)
            .setTitle("Téléchargement hors-ligne")
            .setMessage(
                "Environ " + possible + " tuiles vont être téléchargées pour la carte " +
                    TileSources.nameOf(map.tileProvider.tileSource) + " (zooms 14–18).\n\n" +
                    "Restez connecté au Wi-Fi jusqu'à la fin. Continuer ?"
            )
            .setPositiveButton("Télécharger") { _, _ ->
                Toast.makeText(this, "Téléchargement démarré…", Toast.LENGTH_LONG).show()
                val cm = CacheManager(map)
                cm.downloadAreaAsync(
                    this, bbox, zoomMin, zoomMax,
                    object : CacheManager.CacheManagerCallback {
                        override fun onTaskComplete() {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "✅ Zone hors-ligne prête ! Vous pouvez couper le réseau.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        override fun onTaskFailed(errors: Int) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "⚠ Terminé avec " + errors + " erreurs : vérifiez votre connexion puis relancez.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {
                            runOnUiThread {
                                tvInfo.text = "Tuiles : " + progress + " (zoom " + currentZoomLevel + "/" + zoomMax + ")"
                            }
                        }
                        override fun downloadStarted() {}
                        override fun setPossibleTilesInArea(total: Int) {}
                    }
                )
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /** Estimation du nombre de tuiles couvrant une zone pour des zooms donnés. */
    private fun estimateTiles(bbox: BoundingBox, zMin: Int, zMax: Int): Long {
        var total = 0.0
        for (z in zMin..zMax) {
            val x1 = lonToTileX(bbox.lonWest, z)
            val x2 = lonToTileX(bbox.lonEast, z)
            val y1 = latToTileY(bbox.latNorth, z)
            val y2 = latToTileY(bbox.latSouth, z)
            total += (abs(x2 - x1) + 1) * (abs(y2 - y1) + 1)
        }
        return total.toLong()
    }

    private fun lonToTileX(lon: Double, zoom: Int): Double =
        (lon + 180.0) / 360.0 * (1 shl zoom)

    private fun latToTileY(lat: Double, zoom: Int): Double {
        val latRad = Math.toRadians(lat)
        return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
    }

    private fun importAndSaveGpx(uri: android.net.Uri) {
        val name = GpxRepository.displayName(this, uri)
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null) {
            Toast.makeText(this, "Impossible de lire ce fichier", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val saved = GpxRepository.save(this, name, bytes)
            Toast.makeText(this, "Copié dans Documents/RandoGPX/" + saved, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Copie impossible : " + e.message, Toast.LENGTH_LONG).show()
        }
        loadGpx(uri, name)
    }

    private fun loadGpx(uri: android.net.Uri, name: String) {
        try {
            val gpx = contentResolver.openInputStream(uri)?.use { GpxParser.parse(it) } ?: return
            track = gpx.track
            waypoints = gpx.waypoints
            trackName = name.substringBeforeLast('.')
            trackStats = if (track.size > 1) TrackStats.compute(track) else null

            trackLine?.let { map.overlays.remove(it) }
            map.overlays.removeAll { it is Marker && it.id == "wp" }

            if (track.isNotEmpty()) {
                val line = Polyline(map).apply {
                    setPoints(track.map { GeoPoint(it.lat, it.lon) })
                    outlinePaint.color = android.graphics.Color.rgb(0, 120, 255)
                    outlinePaint.strokeWidth = 8f
                }
                trackLine = line
                map.overlays.add(line)
                map.post {
                    map.zoomToBoundingBox(
                        BoundingBox.fromGeoPoints(track.map { GeoPoint(it.lat, it.lon) })
                            .increaseByScale(1.2f), false
                    )
                }
            }

            waypoints.forEach { wp ->
                val m = Marker(map).apply {
                    id = "wp"
                    position = GeoPoint(wp.lat, wp.lon)
                    title = wp.name
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(m)
            }

            nextWp = waypoints.firstOrNull()
            tvStatus.text = "Tracé : %.1f km, D+ %d m".format(
                trackStats?.distanceKm ?: 0.0, trackStats?.ascentM ?: 0
            )
            tvStatus.setTextColor(android.graphics.Color.WHITE)
            map.invalidate()

            hikeLogger = HikeLogger(trackName, null)
            if (track.isNotEmpty()) fetchWeather(track.first().lat, track.first().lon)

            Toast.makeText(
                this,
                "Journal démarré — bouton « Fin rando » pour enregistrer",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur de lecture du GPX : " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        Thread {
            val w = Weather.fetch(lat, lon)
            runOnUiThread {
                weather = w
                w?.let {
                    hikeLogger = HikeLogger(trackName, it)
                    tvInfo.text = "Météo : " + it.description + ", " + it.temperatureC + "°C, vent " + it.windKmh + " km/h"
                } ?: Toast.makeText(
                    this, "Météo indisponible (pas de réseau)", Toast.LENGTH_SHORT
                ).show()
            }
        }.start()
    }

    private fun confirmEndHike() {
        val logger = hikeLogger
        if (logger == null || logger.recorded.isEmpty()) {
            Toast.makeText(this, "Aucune rando en cours à enregistrer", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Terminer la rando ?")
            .setMessage("Le journal (parcours réel + stats + météo) sera sauvegardé dans Documents/RandoGPX/logs/")
            .setPositiveButton("Enregistrer") { _, _ ->
                try {
                    val summary = logger.finish(this)
                    Toast.makeText(this, summary, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Erreur sauvegarde : " + e.message, Toast.LENGTH_LONG).show()
                }
                hikeLogger = null
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val listener = android.location.LocationListener { loc -> onNewLocation(loc) }
        val providers = listOf(
            android.location.LocationManager.GPS_PROVIDER,
            android.location.LocationManager.NETWORK_PROVIDER
        ).filter { lm.isProviderEnabled(it) }
        providers.forEach {
            try {
                lm.requestLocationUpdates(it, 2000L, 2f, listener, mainLooper)
            } catch (_: SecurityException) { }
        }
    }

    private fun onNewLocation(loc: android.location.Location) {
        currentLat = loc.latitude
        currentLon = loc.longitude
        currentAlt = if (loc.hasAltitude()) loc.altitude else currentAlt
        lastSpeed = if (loc.hasSpeed()) loc.speed else lastSpeed

        hikeLogger?.add(loc.latitude, loc.longitude, currentAlt)

        if (userMarker == null) {
            userMarker = Marker(map).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ContextCompat.getDrawable(
                    this@MainActivity, android.R.drawable.ic_menu_mylocation
                )
                map.overlays.add(this)
            }
        }
        userMarker?.position = GeoPoint(loc.latitude, loc.longitude)
        if (followMode) recentreOnUser()
        map.invalidate()

        updateNavigation()
    }

    private fun recentreOnUser() {
        val lat = currentLat ?: return
        val lon = currentLon ?: return
        map.post { map.controller.animateTo(GeoPoint(lat, lon)) }
    }

    private fun updateNavigation() {
        val lat = currentLat ?: return
        val lon = currentLon ?: return
        if (track.size < 2) return

        val proj = GpxUtils.projectOnTrack(lat, lon, track)
        val remaining = proj.totalDistance - proj.distanceCovered
        val speed = if (lastSpeed > 0.5) lastSpeed else 1.1f
        val etaMin = (remaining / speed) / 60.0

        val wpInfo = nextWp?.let { wp ->
            val d = GpxUtils.distance(lat, lon, wp.lat, wp.lon)
            targetBearing = GpxUtils.bearing(lat, lon, wp.lat, wp.lon)
            " | → " + wp.name + " : " + d.toKm()
        } ?: ""

        tvInfo.text = "Reste : %.1f km (%d min)%s".format(remaining / 1000, etaMin.toInt(), wpInfo)

        if (proj.distanceToTrack > 50) {
            tvStatus.text = "⚠ HORS TRACÉ (%d m) — suivez la flèche"
                .format(proj.distanceToTrack.toInt())
            tvStatus.setTextColor(android.graphics.Color.rgb(255, 80, 80))
            targetBearing = GpxUtils.bearing(
                lat, lon,
                track[proj.segmentIndex].lat, track[proj.segmentIndex].lon
            )
            arrow.visibility = View.VISIBLE
            if (System.currentTimeMillis() - lastAlertTime > 10000) {
                lastAlertTime = System.currentTimeMillis()
                alert()
            }
        } else {
            tvStatus.text = "✔ Sur le tracé"
            tvStatus.setTextColor(android.graphics.Color.rgb(80, 255, 120))
            arrow.visibility = if (nextWp != null) View.VISIBLE else View.GONE
        }
    }

    private fun alert() {
        val vib = getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
        else @Suppress("DEPRECATION") vib.vibrate(600)
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            .startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
    }

    private fun showWaypointDialog() {
        if (waypoints.isEmpty()) {
            Toast.makeText(this, "Aucun waypoint dans ce GPX", Toast.LENGTH_SHORT).show()
            return
        }
        val names = waypoints.mapIndexed { i, wp -> (i + 1).toString() + ". " + wp.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Prochain waypoint")
            .setItems(names) { _, which ->
                nextWp = waypoints[which]
                updateNavigation()
                Toast.makeText(this, "Objectif : " + nextWp?.name, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Aucun") { _, _ ->
                nextWp = null
                arrow.visibility = View.GONE
            }
            .show()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val r = FloatArray(9); val orientation = FloatArray(3)
        SensorManager.getRotationMatrixFromVector(r, event.values)
        SensorManager.getOrientation(r, orientation)
        azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()

        val bearing = targetBearing ?: return
        val rotation = ((bearing - azimuth + 360) % 360).toFloat()
        arrow.animate().rotation(rotation).setDuration(120).start()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun Double.toKm() =
        if (this >= 1000) "%.1f km".format(this / 1000) else this.toInt().toString() + " m"
}
