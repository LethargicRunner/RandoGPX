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
 * Service GPS au premier plan (V2) :
 * l'enregistrement de la randonnée continue même si l'app est en arrière-plan
 * ou si l'écran s'éteint. Sauvegarde automatique toutes les 5 minutes.
 */
class TrackService : Service() {

    enum class State { IDLE, RECORDING, PAUSED }

    companion object {
        var state = State.IDLE
        var logger: HikeLogger? = null
        var trackName = "rando"
        var weather: Weather.Current? = null
        var lastSummary: String? = null

        val stateListeners = mutableListOf<(State, String?) -> Unit>()

        const val ACTION_START = "com.mathieu.randogpx.START"
        const val ACTION_PAUSE = "com.mathieu.randogpx.PAUSE"
        const val ACTION_RESUME = "com.mathieu.randogpx.RESUME"
        const val ACTION_STOP = "com.mathieu.randogpx.STOP"

        private const val CHANNEL_ID = "rando_recording"
        private const val NOTIF_ID = 1

        private fun notifyState(summary: String? = null) {
            stateListeners.forEach { it(state, summary) }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var locationListener: android.location.LocationListener? = null

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
                updateNotification()
                notifyState()
            }
            ACTION_RESUME -> {
                logger?.resume()
                state = State.RECORDING
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
        startLocationUpdates()
        handler.removeCallbacks(autoSaveRunnable)
        handler.postDelayed(autoSaveRunnable, AUTO_SAVE_INTERVAL_MS)
        notifyState()
    }

    private fun stopRecording() {
        lastSummary = try {
            logger?.finish(this)
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

    @Suppress("MissingPermission")
    private fun startLocationUpdates() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val l = android.location.LocationListener { loc ->
            logger?.add(
                loc.latitude, loc.longitude,
                if (loc.hasAltitude()) loc.altitude else null
            )
        }
        locationListener = l
        try {
            lm.requestLocationUpdates(
                android.location.LocationManager.GPS_PROVIDER, 5000L, 5f, l
            )
        } catch (_: Exception) { }
    }

    private fun stopLocationUpdates() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        locationListener?.let { lm.removeUpdates(it) }
        locationListener = null
    }

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
            State.RECORDING -> "Enregistrement en cours — " + trackName
            State.PAUSED -> "En pause"
            else -> ""
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("🥾 RandoGPX")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        private const val AUTO_SAVE_INTERVAL_MS = 5 * 60 * 1000L
    }
}
