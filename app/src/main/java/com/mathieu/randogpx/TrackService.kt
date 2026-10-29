package com.mathieu.randogpx

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

/**
 * Service GPS au premier plan (V1.3) :
 * - l'enregistrement continue même app en arrière-plan / écran éteint
 * - GPS ADAPTATIF : 5 s à l'approche d'un virage du tracé, jusqu'à 60 s en ligne droite
 * - sauvegarde automatique toutes les 5 minutes
 * - GPS coupé pendant les pauses
 */
class TrackService : Service() {

    enum class State { IDLE, RECORDING, PAUSED }

    companion object {
        var state = State.IDLE
        var logger: HikeLogger? = null
        var trackName = "rando"
        var weather: Weather.Current? = null
        var lastSummary: String? = null

        /** V1.10 : vrai = la prochaine finalisation est jetée (trajet quasi vide). */
        var discardNextSave = false

        /** V1.13 : nom, difficulté (étoiles) et note libre saisis en fin de rando. */
        var pendingMeta: Triple<String, Int, String>? = null

        /** Tracé GPX prévu (pour détecter virages et bifurcations à venir). */
        var plannedTrack: List<TrackPoint> = emptyList()

        val stateListeners = mutableListOf<(State, String?) -> Unit>()

        const val ACTION_START = "com.mathieu.randogpx.START"
        const val ACTION_PAUSE = "com.mathieu.randogpx.PAUSE"
        const val ACTION_RESUME = "com.mathieu.randogpx.RESUME"
        const val ACTION_STOP = "com.mathieu.randogpx.STOP"

        private const val CHANNEL_ID = "rando_recording"
        private const val NOTIF_ID = 1
        private const val AUTO_SAVE_INTERVAL_MS = 5 * 60 * 1000L

        // ----- GPS adaptatif -----
        const val GPS_FAST_MS = 5_000L       // virage proche (< 200 m)
        const val GPS_NORMAL_MS = 15_000L    // virage à l'horizon (< 600 m) ou pas de tracé
        const val GPS_SLOW_MS = 60_000L      // longue ligne droite
        private const val TURN_ANGLE_DEG = 30.0
        private const val TURN_LOOKAHEAD_M = 600.0
        private const val TURN_NEAR_M = 200.0
        private const val CHUNK_M = 40.0

        private fun notifyState(summary: String? = null) {
            stateListeners.forEach { it(state, summary) }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var locationListener: android.location.LocationListener? = null
    private var currentIntervalMs = GPS_NORMAL_MS

    private val autoSaveRunnable = object : Runnable {
        override fun run() {
            logger?.autosave(this@TrackService)
            handler.postDelayed(this, AUTO_SAVE_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_PAUSE -> {
                logger?.pause()
                state = State.PAUSED
                stopLocationUpdates()   // économie batterie : GPS coupé en pause
                updateNotification()
                notifyState()
            }
            ACTION_RESUME -> {
                logger?.resume()
                state = State.RECORDING
                currentIntervalMs = GPS_NORMAL_MS
                startLocationUpdates()
                updateNotification()
                notifyState()
            }
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    private fun startRecording() {
        logger = HikeLogger(trackName, weather)
        state = State.RECORDING
        startForegroundCompat()
        currentIntervalMs = GPS_NORMAL_MS
        startLocationUpdates()
        handler.removeCallbacks(autoSaveRunnable)
        handler.postDelayed(autoSaveRunnable, AUTO_SAVE_INTERVAL_MS)
        notifyState()
    }

    private fun stopRecording() {
        val discard = discardNextSave
        discardNextSave = false
        val meta = pendingMeta
        pendingMeta = null
        lastSummary = try {
            if (discard) {
                logger?.discard(this)
                "Sortie abandonnée : rien n'a été sauvegardé"
            } else {
                logger?.finish(this, meta)
            }
        } catch (e: Exception) {
            "Erreur lors de l'enregistrement : " + e.message
        }
        handler.removeCallbacks(autoSaveRunnable)
        stopLocationUpdates()
        logger = null
        state = State.IDLE
        stopForeground(true)
        stopSelf()
        notifyState(lastSummary)
    }

    // ---------- GPS adaptatif ----------

    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val l = android.location.LocationListener { loc ->
            // V1.14 : même filtre anti-glitch pour l'enregistrement
            if (GpsFilter.accept(loc)) {
                logger?.add(
                    loc.latitude, loc.longitude,
                    if (loc.hasAltitude()) loc.altitude else null
                )
                adaptGpsInterval(loc.latitude, loc.longitude)
            }
        }
        locationListener = l
        try {
            lm.requestLocationUpdates(
                android.location.LocationManager.GPS_PROVIDER, currentIntervalMs, 3f, l
            )
        } catch (_: Exception) { }
    }

    private fun stopLocationUpdates() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        locationListener?.let { lm.removeUpdates(it) }
        locationListener = null
    }

    /**
     * Choisit la fréquence GPS selon la route à venir :
     * - virage du tracé à moins de 200 m  -> 5 s
     * - virage à moins de 600 m          -> 15 s
     * - longue ligne droite (> 600 m)    -> 60 s
     * - pas de tracé GPX chargé           -> 15 s fixe
     */
    private fun adaptGpsInterval(lat: Double, lon: Double) {
        val desired = if (plannedTrack.size < 3) {
            GPS_NORMAL_MS
        } else {
            val turnD = distanceToNextTurn(lat, lon)
            when {
                turnD == null -> GPS_SLOW_MS
                turnD < TURN_NEAR_M -> GPS_FAST_MS
                else -> GPS_NORMAL_MS
            }
        }
        if (desired != currentIntervalMs) {
            stopLocationUpdates()
            currentIntervalMs = desired
            startLocationUpdates()
        }
    }

    /**
     * Distance (en mètres) jusqu'au prochain changement de cap significatif
     * du tracé prévu, en regardant jusqu'à TURN_LOOKAHEAD_M devant soi.
     * Retourne null si la route est rectiligne sur toute la fenêtre.
     */
    private fun distanceToNextTurn(lat: Double, lon: Double): Double? {
        val pts = plannedTrack
        if (pts.size < 3) return null

        // Point du tracé le plus proche de la position actuelle
        var bestIdx = 0
        var bestD = Double.MAX_VALUE
        for (i in pts.indices) {
            val d = GpxUtils.distance(lat, lon, pts[i].lat, pts[i].lon)
            if (d < bestD) { bestD = d; bestIdx = i }
        }

        // Avance par tronçons de ~CHUNK_M et compare les caps successifs
        var idx = bestIdx
        var distFromNearest = 0.0
        var prevBearing: Double? = null
        while (idx < pts.size - 1 && distFromNearest < TURN_LOOKAHEAD_M) {
            var chunk = 0.0
            val start = idx
            while (idx < pts.size - 1 && chunk < CHUNK_M) {
                chunk += GpxUtils.distance(
                    pts[idx].lat, pts[idx].lon, pts[idx + 1].lat, pts[idx + 1].lon
                )
                idx++
            }
            if (chunk < 1.0) break
            val b = GpxUtils.bearing(
                pts[start].lat, pts[start].lon, pts[idx].lat, pts[idx].lon
            )
            val pb = prevBearing
            if (pb != null) {
                val delta = kotlin.math.abs(((b - pb + 540.0) % 360.0) - 180.0)
                if (delta > TURN_ANGLE_DEG) return distFromNearest + bestD
            }
            prevBearing = b
            distFromNearest += chunk
        }
        return null
    }

    // ---------- Notification ----------

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun updateNotification() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Enregistrement de randonnée",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
        val text = when (state) {
            State.RECORDING -> "Enregistrement — " + trackName + " (GPS adaptatif)"
            State.PAUSED -> "En pause (GPS coupé)"
            else -> ""
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("RandoGPX")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
