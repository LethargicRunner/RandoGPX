package com.mathieu.randogpx

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
    private lateinit var btnStartPause: Button
    private lateinit var btnFinish: Button
    private lateinit var btnAbout: Button
    private var compassDial: ImageView? = null
    private var compassText: TextView? = null
    private val CARDINALS = arrayOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")

    // Panneau de navigation
    private lateinit var navPanel: View
    private lateinit var navContent: View
    private lateinit var tvNavArrow: TextView
    private lateinit var tvNavInstruction: TextView
    private lateinit var tvNavDist: TextView
    private lateinit var btnFinishHere: Button
    private lateinit var btnNavCollapse: Button
    private var navCollapsed = false

    private var track: List<TrackPoint> = emptyList()
    private var waypoints: List<Waypoint> = emptyList()
    private var trackName = "rando"
    private var trackStats: TrackStats.Stats? = null

    private var userMarker: Marker? = null
    private var trackLine: Polyline? = null
    private var returnLine: Polyline? = null
    private var nextWp: Waypoint? = null

    private var currentLat: Double? = null
    private var currentLon: Double? = null
    private var currentAlt: Double? = null
    private var lastSpeed = 0f
    private var lastAlertTime = 0L
    private var followMode = true

    // Navigation turn-by-turn
    private var plannedPairs: List<Pair<Double, Double>> = emptyList()
    private val walkedPoints = mutableListOf<Pair<Double, Double>>()
    private var returnMode = false
    private var returnPairs: List<Pair<Double, Double>> = emptyList()
    private var lastTurnKey = -1

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

    private val stateListener: (TrackService.State, String?) -> Unit = { st, summary ->
        runOnUiThread {
            syncControls()
            summary?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }
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
        btnStartPause = findViewById(R.id.btnStartPause)
        btnFinish = findViewById(R.id.btnFinish)

        navPanel = findViewById(R.id.navPanel)
        navContent = findViewById(R.id.navContent)
        tvNavArrow = findViewById(R.id.tvNavArrow)
        tvNavInstruction = findViewById(R.id.tvNavInstruction)
        tvNavDist = findViewById(R.id.tvNavDist)
        btnFinishHere = findViewById(R.id.btnFinishHere)
        btnNavCollapse = findViewById(R.id.btnNavCollapse)

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
        btnStartPause.setOnClickListener { onStartPauseClicked() }
        btnFinish.setOnClickListener { onFinishClicked() }
        btnAbout = findViewById(R.id.btnAbout)
        btnAbout.setOnClickListener { showAbout() }
        findViewById<Button>(R.id.btnCompass).setOnClickListener { showCompass() }
        findViewById<Button>(R.id.btnWeather).setOnClickListener { showForecast() }
        findViewById<Button>(R.id.btnEphemeris).setOnClickListener { showEphemeris() }
        btnFinishHere.setOnClickListener { confirmFinishHere() }
        btnNavCollapse.setOnClickListener {
            navCollapsed = !navCollapsed
            syncNavPanel()
        }
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

        TrackService.stateListeners.add(stateListener)
        syncControls()

        // Une rando interrompue (crash / batterie vide) attend-elle d'être sauvée ?
        if (HikeLogger.pendingAutosave(this)) {
            AlertDialog.Builder(this)
                .setTitle("🥾 Randonnée non terminée")
                .setMessage("Un enregistrement interrompu a été retrouvé (sauvegarde automatique).\n\nVoulez-vous le sauvegarder dans vos journaux ?")
                .setPositiveButton("Sauvegarder") { _, _ ->
                    val name = HikeLogger.finalizePending(this)
                    Toast.makeText(this, "Journal récupéré : " + name, Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("Supprimer", null)
                .show()
        }

        if (!hasLocationPermission()) requestPermissions.launch(locationPermissions())
    }

    private fun locationPermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        return perms.toTypedArray()
    }

    // ---------- Contrôles d'enregistrement ----------

    private fun onStartPauseClicked() {
        val intent = Intent(this, TrackService::class.java)
        when (TrackService.state) {
            TrackService.State.IDLE -> {
                TrackService.trackName = if (track.isNotEmpty()) trackName else "rando_libre"
                TrackService.weather = weather
                intent.action = TrackService.ACTION_START
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(intent) else startService(intent)
            }
            TrackService.State.RECORDING -> {
                intent.action = TrackService.ACTION_PAUSE
                startService(intent)
            }
            TrackService.State.PAUSED -> {
                intent.action = TrackService.ACTION_RESUME
                startService(intent)
            }
        }
    }

    private fun onFinishClicked() {
        if (TrackService.state == TrackService.State.IDLE) {
            Toast.makeText(this, "Aucune rando en cours", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Terminer la rando ?")
            .setMessage("Le journal (parcours réel + stats + météo) sera sauvegardé dans Documents/RandoGPX/logs/")
            .setPositiveButton("Enregistrer") { _, _ ->
                val intent = Intent(this, TrackService::class.java)
                intent.action = TrackService.ACTION_STOP
                startService(intent)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /**
     * « Finir ici » : arrête l'enregistrement puis guide le retour au point de
     * départ le long du chemin réellement parcouru (tracé inversé).
     */
    private fun confirmFinishHere() {
        AlertDialog.Builder(this)
            .setTitle("Terminer ici et revenir au départ ?")
            .setMessage(
                "La rando sera clôturée à cet endroit, puis l'app vous guidera " +
                    "pas à pas jusqu'au point de départ en suivant votre chemin aller."
            )
            .setPositiveButton("Terminer et revenir") { _, _ ->
                if (TrackService.state != TrackService.State.IDLE) {
                    val intent = Intent(this, TrackService::class.java)
                    intent.action = TrackService.ACTION_STOP
                    startService(intent)
                }
                startReturnGuidance()
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun startReturnGuidance() {
        if (walkedPoints.size < 2) {
            Toast.makeText(
                this,
                "Pas assez de points enregistrés pour le guidage retour — suivez le tracé orange sur la carte",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        returnMode = true
        lastTurnKey = -1
        returnPairs = walkedPoints.toList().asReversed()

        returnLine?.let { map.overlays.remove(it) }
        val line = Polyline(map).apply {
            setPoints(returnPairs.map { GeoPoint(it.first, it.second) })
            outlinePaint.color = android.graphics.Color.rgb(255, 152, 0)
            outlinePaint.strokeWidth = 7f
        }
        returnLine = line
        map.overlays.add(line)
        map.invalidate()

        navPanel.visibility = View.VISIBLE
        syncNavPanel()
        tvNavInstruction.text = "Retour au départ — suivez le tracé orange"
        tvNavDist.text = ""
        beep()
        Toast.makeText(this, "🧭 Guidage retour activé", Toast.LENGTH_LONG).show()
    }

    private fun syncNavPanel() {
        navContent.visibility = if (navCollapsed) View.GONE else View.VISIBLE
        btnNavCollapse.text = if (navCollapsed) "▸" else "▾"
    }

    private fun syncControls() {
        btnStartPause.text = when (TrackService.state) {
            TrackService.State.IDLE -> "▶ Départ"
            TrackService.State.RECORDING -> "⏸ Pause"
            TrackService.State.PAUSED -> "▶ Reprendre"
        }
        btnFinish.isEnabled = TrackService.state != TrackService.State.IDLE
        when (TrackService.state) {
            TrackService.State.RECORDING ->
                tvInfo.text = "🔴 Enregistrement en cours (" + TrackService.trackName + ")"
            TrackService.State.PAUSED -> tvInfo.text = "⏸ Rando en pause"
            TrackService.State.IDLE -> { }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        wakeLock?.acquire(10 * 60 * 60 * 1000L)
        if (hasLocationPermission()) startLocationUpdates()
        syncControls()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    private fun showCompass() {
        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }
        val dial = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_compass)
            layoutParams = android.widget.LinearLayout.LayoutParams(
                (220 * density).toInt(), (220 * density).toInt()
            )
        }
        val text = TextView(this).apply {
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            setPadding(0, pad, 0, 0)
        }
        container.addView(dial)
        container.addView(text)
        compassDial = dial
        compassText = text
        val dialog = AlertDialog.Builder(this)
            .setTitle("🧭 Boussole")
            .setView(container)
            .setPositiveButton("Fermer", null)
            .show()
        dialog.setOnDismissListener {
            compassDial = null
            compassText = null
        }
        updateCompassViews()
    }

    private fun updateCompassViews() {
        val heading = ((azimuth % 360f) + 360f) % 360f
        compassDial?.rotation = -heading
        val card = CARDINALS[(Math.round(heading / 45f).toInt() % 8 + 8) % 8]
        val lat = currentLat
        val lon = currentLon
        val wp = nextWp
        val wpLine = if (wp != null && lat != null && lon != null) {
            val b = GpxUtils.bearing(lat, lon, wp.lat, wp.lon)
            val d = GpxUtils.distance(lat, lon, wp.lat, wp.lon)
            "\nVers " + wp.name + " : " + "%.0f°".format(java.util.Locale.US, b) + " — " + d.toKm()
        } else ""
        compassText?.text =
            "%.0f° (%s)".format(java.util.Locale.US, heading.toDouble(), card) + wpLine
    }

    private fun showForecast() {
        val lat = currentLat ?: track.firstOrNull()?.lat
        val lon = currentLon ?: track.firstOrNull()?.lon
        if (lat == null || lon == null) {
            Toast.makeText(this, "Position indisponible — dehors ou avec un GPX", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Chargement des prévisions…", Toast.LENGTH_SHORT).show()
        Thread {
            val days = Weather.fetchForecast(lat, lon)
            runOnUiThread {
                if (days == null) {
                    Toast.makeText(this, "Météo indisponible (pas de réseau)", Toast.LENGTH_LONG).show()
                    return@runOnUiThread
                }
                val todayIso = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .format(java.util.Date())
                val sb = StringBuilder()
                days.forEach { d ->
                    val label = if (d.date == todayIso) "Aujourd'hui" else formatDateShort(d.date)
                    sb.append(label).append(" : ")
                        .append(Weather.codeEmoji(d.code)).append(" ")
                        .append(d.tmin.toInt()).append("° / ").append(d.tmax.toInt()).append("°")
                        .append(", pluie ").append(d.precipProb).append("%\n")
                }
                AlertDialog.Builder(this)
                    .setTitle("☁️ Prévisions météo (7 jours)")
                    .setMessage(sb.toString().trim() + "\n\nSource : Open-Meteo")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }.start()
    }

    private fun formatDateShort(iso: String): String = try {
        val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(iso)
        val out = java.text.SimpleDateFormat("EEE d", java.util.Locale.FRENCH)
        out.format(parsed)
    } catch (_: Exception) { iso }

    private fun showEphemeris() {
        val lat = currentLat ?: track.firstOrNull()?.lat
        val lon = currentLon ?: track.firstOrNull()?.lon
        if (lat == null || lon == null) {
            Toast.makeText(this, "Position indisponible — dehors ou avec un GPX", Toast.LENGTH_SHORT).show()
            return
        }
        val now = System.currentTimeMillis()
        val (sunrise, sunset) = Ephemeris.sunTimes(lat, lon, now)
        val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val sb = StringBuilder()
        sb.append("🌅 Lever du soleil : ")
            .append(if (sunrise != null) fmt.format(java.util.Date(sunrise)) else "—").append("\n")
        sb.append("🌇 Coucher du soleil : ")
            .append(if (sunset != null) fmt.format(java.util.Date(sunset)) else "—").append("\n")
        if (sunrise != null && sunset != null) {
            sb.append("⏱️ Durée du jour : ").append(formatDuration(sunset - sunrise)).append("\n")
            if (now >= sunrise && now <= sunset) {
                sb.append("\n✅ Il reste ").append(formatDuration(sunset - now)).append(" de lumière")
            }
        }
        val phase = Ephemeris.moonPhase(now)
        sb.append("\n🌙 ").append(Ephemeris.moonName(phase))
            .append(" (").append(Ephemeris.moonIllumination(phase)).append("% éclairée)")
        AlertDialog.Builder(this)
            .setTitle("🌙 Éphémérides du jour")
            .setMessage(sb.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun formatDuration(ms: Long): String {
        val totalMin = ms / 60000
        return (totalMin / 60).toString() + " h " + (totalMin % 60).toString().padStart(2, '0')
    }

    private fun showAbout() {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) { "?" }
        AlertDialog.Builder(this)
            .setTitle("À propos — RandoGPX v" + version)
            .setMessage(
                "Navigation GPX pour la randonnée — par Mathieu Bouvier.\n\n" +
                    "https://github.com/LethargicRunner/RandoGPX\n\n" +
                    "FONDS DE CARTE\n" +
                    "• OpenStreetMap — © les contributeurs OpenStreetMap (ODbL)\n" +
                    "• OpenTopoMap — © OpenTopoMap (CC-BY-SA)\n" +
                    "• Plan IGN — © IGN, via la Géoplateforme\n\n" +
                    "Le téléchargement massif de tuiles respecte les politiques " +
                    "d'usage des fournisseurs : il est limité à OpenTopoMap et IGN Plan.\n\n" +
                    "MÉTÉO\n" +
                    "• Open-Meteo (https://open-meteo.com), sous licence CC BY 4.0\n\n" +
                    "REMERCIEMENTS\n" +
                    "• Bibliothèque de carte : osmdroid (Apache 2.0)"
            )
            .setPositiveButton("OK", null)
            .show()
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
            .setNegativeButton("Fermer", null)
            .show()
    }

    private fun downloadOfflineArea() {
        if (track.size < 2) {
            Toast.makeText(this, "Importez d'abord un tracé GPX", Toast.LENGTH_SHORT).show()
            return
        }
        // OpenStreetMap standard interdit le téléchargement massif (politique d'usage des tuiles OSM).
        // Seuls OpenTopoMap et IGN Plan l'autorisent dans cette app.
        val srcName = map.tileProvider.tileSource.name()
        if (srcName != "OpenTopoMap" && srcName != "IGN Plan") {
            AlertDialog.Builder(this)
                .setTitle("Carte non téléchargeable")
                .setMessage(
                    "OpenStreetMap standard interdit le téléchargement massif de tuiles " +
                        "(politique d'OpenStreetMap).\n\nUtilisez OpenTopoMap ou IGN Plan " +
                        "pour la carte hors-ligne.")
                .setPositiveButton("Basculer sur OpenTopoMap") { _, _ ->
                    map.setTileSource(TileSources.OPENTOPOMAP)
                    map.invalidate()
                    Toast.makeText(this, "Carte OpenTopoMap — relancez Hors-ligne", Toast.LENGTH_LONG).show()
                }
                .setNeutralButton("Basculer sur IGN Plan") { _, _ ->
                    map.setTileSource(TileSources.IGN_PLAN)
                    map.invalidate()
                    Toast.makeText(this, "Carte IGN Plan — relancez Hors-ligne", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("Annuler", null)
                .show()
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
                try {
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
                } catch (e: Exception) {
                    Toast.makeText(
                        this, "Téléchargement impossible : " + e.message, Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

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
            TrackService.plannedTrack = track   // GPS adaptatif : le service connaît la route à venir
            plannedPairs = track.map { it.lat to it.lon }
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
                    setAnchor(0.5f, 1.0f)
                }
                map.overlays.add(m)
            }

            nextWp = waypoints.firstOrNull()
            tvStatus.text = "Tracé : %.1f km, D+ %d m".format(
                trackStats?.distanceKm ?: 0.0, trackStats?.ascentM ?: 0
            )
            tvStatus.setTextColor(android.graphics.Color.WHITE)
            navPanel.visibility = if (track.size >= 2) View.VISIBLE else View.GONE
            map.invalidate()

            if (track.isNotEmpty()) fetchWeather(track.first().lat, track.first().lon)
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
                    tvInfo.text = "Météo au départ : " + it.description + ", " + it.temperatureC + "°C, vent " + it.windKmh + " km/h"
                } ?: Toast.makeText(
                    this, "Météo indisponible (pas de réseau)", Toast.LENGTH_SHORT
                ).show()
            }
        }.start()
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

        // Mémorise le chemin réellement parcouru (pour le guidage retour)
        if (TrackService.state == TrackService.State.RECORDING) {
            val last = walkedPoints.lastOrNull()
            if (last == null ||
                GpxUtils.distance(last.first, last.second, loc.latitude, loc.longitude) > 5.0
            ) {
                walkedPoints.add(loc.latitude to loc.longitude)
            }
        }

        if (userMarker == null) {
            userMarker = Marker(map).apply {
                setAnchor(0.5f, 0.5f)
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

        // Mode guidage retour au point de départ
        if (returnMode) {
            updateReturnNav(lat, lon)
            return
        }

        if (track.size < 2) return

        // Panneau de navigation : actif même pendant l'enregistrement
        updateNavPanel(lat, lon)

        // Barre d'info : pendant l'enregistrement, elle affiche l'état de la rando
        if (TrackService.state != TrackService.State.IDLE) return

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

    /** Panneau turn-by-turn : ↰ gauche, ↱ droite, ↑ tout droit, ⤺ demi-tour hors tracé. */
    private fun updateNavPanel(lat: Double, lon: Double) {
        val proj = GpxUtils.projectOnTrack(lat, lon, track)
        if (proj.distanceToTrack > 50) {
            tvNavArrow.text = "⤺"
            tvNavInstruction.text = "Demi-tour — revenez sur le tracé"
            tvNavDist.text = proj.distanceToTrack.toInt().toString() + " m hors tracé"
            btnFinishHere.visibility = View.VISIBLE
            return
        }
        btnFinishHere.visibility = View.GONE

        val turn = GpxUtils.nextTurn(lat, lon, plannedPairs)
        if (turn == null) {
            tvNavArrow.text = "↑"
            tvNavInstruction.text = "Tout droit"
            tvNavDist.text = ""
        } else {
            val dir = if (turn.directionRight) "droite" else "gauche"
            tvNavArrow.text = if (turn.directionRight) "↱" else "↰"
            tvNavInstruction.text = "Virage à " + dir + " dans " + turn.distanceM.toInt() + " m"
            tvNavDist.text = if (turn.distanceM < 150) "⚠ Imminent" else "Prochain virage"
            if (turn.distanceM < 150 && turn.turnIndex != lastTurnKey) {
                lastTurnKey = turn.turnIndex
                beep()
            }
        }
    }

    /** Guidage retour : suit le chemin aller inversé jusqu'au point de départ. */
    private fun updateReturnNav(lat: Double, lon: Double) {
        val start = returnPairs.lastOrNull() ?: return
        val d = GpxUtils.distance(lat, lon, start.first, start.second)

        if (d < 40) {
            returnMode = false
            returnLine?.let { map.overlays.remove(it); returnLine = null }
            walkedPoints.clear()
            lastTurnKey = -1
            map.invalidate()
            tvNavArrow.text = "🏁"
            tvNavInstruction.text = "Retour au point de départ terminé !"
            tvNavDist.text = ""
            beep()
            Toast.makeText(this, "🎉 Vous êtes revenu au point de départ", Toast.LENGTH_LONG).show()
            return
        }

        val turn = GpxUtils.nextTurn(lat, lon, returnPairs)
        if (turn == null || turn.distanceM > 150) {
            tvNavArrow.text = "↑"
            tvNavInstruction.text = "Retour au départ"
            tvNavDist.text = "%.1f km restants".format(d / 1000)
        } else {
            val dir = if (turn.directionRight) "droite" else "gauche"
            tvNavArrow.text = if (turn.directionRight) "↱" else "↰"
            tvNavInstruction.text = "Retour : à " + dir + " dans " + turn.distanceM.toInt() + " m"
            tvNavDist.text = "%.1f km restants".format(d / 1000)
            if (turn.turnIndex != lastTurnKey) {
                lastTurnKey = turn.turnIndex
                beep()
            }
        }
    }

    private fun beep() {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                .startTone(ToneGenerator.TONE_PROP_BEEP, 300)
        } catch (_: Exception) { }
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

        updateCompassViews()

        val bearing = targetBearing ?: return
        val rotation = ((bearing - azimuth + 360) % 360).toFloat()
        arrow.animate().rotation(rotation).setDuration(120).start()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun Double.toKm() =
        if (this >= 1000) "%.1f km".format(this / 1000) else this.toInt().toString() + " m"
}
