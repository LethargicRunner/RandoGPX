package com.mathieu.randogpx

import android.content.Context

/**
 * Contact d'urgence et messages de sécurité.
 * Tout est stocké localement (SharedPreferences), aucun envoi automatique :
 * les SMS s'ouvrent pré-remplis dans l'app Messages, l'utilisateur contrôle l'envoi.
 */
object Emergency {
    private const val PREFS = "emergency"
    private const val KEY_NAME = "contactName"
    private const val KEY_PHONE = "contactPhone"
    private const val KEY_USER = "userName"

    fun load(context: Context): Pair<String, String>? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = p.getString(KEY_NAME, null) ?: return null
        val phone = p.getString(KEY_PHONE, null) ?: return null
        return name to phone
    }

    fun save(context: Context, contactName: String, phone: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_NAME, contactName)
            .putString(KEY_PHONE, phone)
            .apply()
    }

    fun loadUserName(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_USER, "Moi") ?: "Moi"

    fun saveUserName(context: Context, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_USER, name).apply()
    }

    fun mapsLink(lat: Double, lon: Double): String =
        "https://maps.google.com/?q=" + "%.5f".format(java.util.Locale.US, lat) +
            "," + "%.5f".format(java.util.Locale.US, lon)

    /** Message SOS : dernière position connue + consignes pour le contact. */
    fun sosMessage(myName: String, lat: Double?, lon: Double?, alt: Double?): String {
        val pos = if (lat != null && lon != null) {
            "Dernière position : " + "%.5f".format(java.util.Locale.US, lat) +
                ", " + "%.5f".format(java.util.Locale.US, lon) +
                (alt?.let { " (altitude " + it.toInt() + " m)" } ?: "") + "\n" + mapsLink(lat, lon)
        } else "Position GPS indisponible"
        return "🆘 URGENCE de " + myName + " (RandoGPX).\n" + pos + "\n\n" +
            "Essayez de me joindre. Sans réponse sous 15 min : appelez le 112 " +
            "(secours) en indiquant ces coordonnées."
    }

    /** SMS de procédure envoyé une fois au contact désigné. */
    fun procedureMessage(myName: String, contactName: String): String =
        "Bonjour " + contactName + ", c'est " + myName + ".\n\n" +
        "Je vous désigne comme personne à contacter en cas d'urgence pendant " +
        "mes randonnées (app RandoGPX). 🥾\n\n" +
        "Si vous recevez mon message 🆘 URGENCE avec des coordonnées GPS :\n" +
        "1) Essayez de me joindre\n" +
        "2) Sans réponse sous 15 min, appelez le 112 en donnant " +
        "les coordonnées du message\n\n" +
        "Merci d'être ma balise de sécurité !"
}