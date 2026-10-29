package com.mathieu.randogpx

/**
 * Filtre anti-glitch GPS (V1.14).
 *
 * En ville, le GPS "saute" parfois de plusieurs centaines de mètres à plusieurs
 * kilomètres (effet canyon urbain : les signaux rebondissent sur les immeubles).
 * Ces fixes aberrants arrivaient jusqu'à l'écran : pastille de position
 * téléportée, trace orange qui gonfle, distance au tracé qui délire.
 *
 * Ce filtre est appliqué à l'entrée de TOUTE l'app (écran ET enregistrement) :
 * - un fix dont la précision annoncée dépasse 30 m est ignoré ;
 * - un déplacement impossible à pied (plus de ~16 km/h sur plus de 20 m
 *   depuis le dernier fix fiable) est ignoré ;
 * - en dessous de 20 m, on garde (bruit normal de stationnaire).
 *
 * Le fix douteux est simplement écarté : la position affichée reste sur le
 * dernier point fiable en attendant le prochain fix correct.
 */
object GpsFilter {

    const val MAX_ACCURACY_M = 30f     // précision annoncée maximale acceptée
    const val MAX_SPEED_MPS = 4.5      // ~16 km/h : impossible en marchant
    const val MIN_JUMP_M = 20.0        // saut minimum pour appliquer le test de vitesse

    private var lastGoodLat = 0.0
    private var lastGoodLon = 0.0
    private var lastGoodTime = 0L
    private var hasLast = false

    /** true = ce fix est fiable, on peut l'utiliser partout (écran + journal). */
    @Synchronized fun accept(loc: android.location.Location): Boolean {
        // 1. Précision annoncée par le chipset trop médiocre -> rejet
        if (loc.hasAccuracy() && loc.accuracy > MAX_ACCURACY_M) return false

        // 2. Déplacement physiquement impossible depuis le dernier fix fiable
        if (hasLast) {
            val d = GpxUtils.distance(lastGoodLat, lastGoodLon, loc.latitude, loc.longitude)
            val dt = (loc.time - lastGoodTime) / 1000.0
            if (d > MIN_JUMP_M && (dt <= 0.0 || d / dt > MAX_SPEED_MPS)) return false
        }

        lastGoodLat = loc.latitude
        lastGoodLon = loc.longitude
        lastGoodTime = loc.time
        hasLast = true
        return true
    }

    /** Repart de zéro (nouvelle sortie, reprise après longue pause). */
    @Synchronized fun reset() {
        hasLast = false
    }
}
