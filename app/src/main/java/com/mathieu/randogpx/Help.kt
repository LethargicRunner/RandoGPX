package com.mathieu.randogpx

/**
 * Aide RandoGPX (V2.0.1) :
 * - manual() : prise en main en 8 chapitres (bouton ❓ → Prise en main)
 * - faq()    : questions fréquentes (bouton ❓ → FAQ)
 */
object Help {
    fun manual(): String = MANUAL
    fun faq(): String = FAQ

    private val MANUAL =
"""🥾 RANDOGPX — PRISE EN MAIN

═══════════════════════════════
1. DÉMARRER EN 30 SECONDES
═══════════════════════════════
• Au lancement, l'écran d'accueil vous demande : « Suivre un GPX » ou « Rando libre » ?
• Avec un tracé : « Suivre un GPX » → choisissez un fichier .gpx (créé sur gpx.studio, Visorando, IGN Rando…). Il est copié dans Documents/RandoGPX et redevient disponible via 📂 → « Mes GPX ».
• Sans tracé : « Rando libre », puis « ▶ Départ » et marchez. Votre parcours se dessine en orange sur la carte.
• « ⏸ Pause » pour une pause, « ▶ Reprendre » pour continuer, « ■ Terminer » à la fin.

═══════════════════════════════
2. COMPRENDRE L'ÉCRAN
═══════════════════════════════
• En haut : bandeau discret — nom du tracé, distance restante, état de la rando.
• En haut à droite : bouton « ❓ » (aide en 3 sections : prise en main, FAQ, à propos) et mini-boussole (le rouge indique le nord).
• Sur la carte : pastille bleue = votre position ; tracé bleu = itinéraire prévu ; tracé orange = votre parcours réel ; points = waypoints du GPX.
• En bas : « ▾ Masquer » replie les menus pour libérer la carte — ▶ Départ/Pause, ■ Terminer et 🆘 restent toujours visibles.
• 📂 GPX & cartes : importer, Mes GPX, fond de carte, hors-ligne.
• 🧰 Outils : boussole, ciel du jour (météo + éphémérides), stats, waypoints, suivi carte, journaux, sauvegarde cloud, aide.

═══════════════════════════════
3. ENREGISTRER UNE RANDO
═══════════════════════════════
• « ▶ Départ » déclenche le rappel « avez-vous prévenu un ami ? », puis l'enregistrement démarre. Il continue en arrière-plan, même écran éteint ou app réduite (notification « Enregistrement »).
• GPS ADAPTATIF : mesure toutes les 5 s à l'approche d'un virage (< 200 m), 15 s à moyenne distance, jusqu'à 60 s en ligne droite → la batterie dure beaucoup plus longtemps.
• FILTRE ANTI-GLITCH (v1.14) : un fix GPS douteux (précision > 30 m ou déplacement impossible à pied) est ignoré — fini les sauts de kilomètres en ville.
• En pause, le GPS est totalement coupé.
• Sauvegarde automatique toutes les 5 minutes : si batterie vide ou crash, l'app propose de récupérer la rando au prochain lancement.
• « ■ Terminer » enregistre le journal (JSON dans Documents/RandoGPX/logs) ET un fichier GPX de votre parcours réel. Le résumé propose directement « 📤 Partager le GPX ».
• Si vous avez bougé de moins de 50 m, l'app vous demande si ça vaut le coup de sauvegarder.

═══════════════════════════════
4. NAVIGATION PAS À PAS
═══════════════════════════════
• Panneau vert en haut de la barre : ↰ virage à gauche, ↱ à droite, ↑ tout droit, avec la distance. Un bip sonore retentit à moins de 150 m de la bifurcation.
• Si vous vous écartez de plus de 50 m du tracé : vibration + alerte, panneau ⤺ « demi-tour », et un bouton « 🏁 Finir ici » apparaît.
• « 🏁 Finir ici » : clôture la rando à cet endroit puis vous guide au retour en suivant VOTRE chemin aller, à l'envers (tracé orange).
• Déplacer la carte à la main coupe automatiquement le suivi — réactivez-le dans 🧰 Outils → « 🎯 Suivi carte ».

═══════════════════════════════
5. CARTES ET HORS-LIGNE
═══════════════════════════════
• 📂 → Fond de carte : OpenStreetMap (standard), OpenTopoMap (relief), IGN Plan, et le mode nuit (auto → nuit forcée → jour forcé).
• 📂 → Hors-ligne : télécharge les tuiles autour de votre tracé (zooms 14 à 18) pour naviguer sans réseau. À faire AVANT la rando, en Wi-Fi.
• Le téléchargement massif est bloqué sur OpenStreetMap (politique d'usage des serveurs OSM) : l'app propose de basculer sur OpenTopoMap ou IGN, qui l'autorisent.

═══════════════════════════════
6. MODE NUIT 🌙
═══════════════════════════════
• Bascule AUTOMATIQUE au coucher et au lever du soleil (calcul hors-ligne, dès que vous avez une position).
• Carte assombrie et interface ambrée : préserve la vision nocturne.
• Forçage possible : 📂 → Fond de carte → 🌙.

═══════════════════════════════
7. BOUSSOLE, MÉTÉO, ÉPHÉMÉRIDES
═══════════════════════════════
• 🧭 Boussole : rose marine (rouge = nord, bleu = sud), cap en degrés, direction du prochain waypoint.
• 🌤️ Ciel du jour : un seul écran — météo du jour en haut (Open-Meteo, réseau ponctuel), éphémérides en dessous (lever/coucher, lumière restante, lune — 100 % hors-ligne), et un bouton « 📅 Prévisions à 5 jours ».

═══════════════════════════════
8. SÉCURITÉ 🆘
═══════════════════════════════
• AVANT de partir : définissez votre contact d'urgence (🆘 → contact) et envoyez-lui le SMS de procédure.
• Le menu 🆘 : SMS SOS pré-rempli avec votre position et un lien carte (VOUS appuyez toujours sur Envoyer), partage de position, appel 112, coordonnées en grand à dicter aux secours.
• ⚠ Honnêteté : un SOS par SMS suppose d'avoir du réseau. En zone blanche, AUCUNE application ne remplace une balise PLB ou un satellite messenger — n'en partez pas pour une rando engagée sans l'un des deux."""

    private val FAQ =
"""❓ RANDOGPX — FAQ

Q. L'app continue-t-elle vraiment écran éteint ?
R. Oui, c'est un service GPS « au premier plan ». Sur Xiaomi/MIUI, pensez à paramètres Batterie → RandoGPX → « Aucune restriction », sinon le système peut fermer l'app en arrière-plan.

Q. Où sont mes données ?
R. Tout est local, dans Documents/RandoGPX sur votre téléphone. Pas de compte, pas de pub, aucune position envoyée nulle part — sauf ce que vous partagez vous-même, et la sauvegarde cloud que VOUS configurez.

Q. C'est quoi la sauvegarde cloud ?
R. 🧰 Outils → « ☁️ Sauvegarde cloud » : vos GPX sont envoyés à la fin de chaque rando dans un dépôt GitHub PRIVÉ que vous créez (ex. RandoGPX-data), via un jeton fine-grained stocké uniquement dans votre téléphone. Sans réseau, les envois attendent en file et repartent automatiquement. Vous pouvez couper cette fonctionnalité à tout moment en effaçant les champs.

Q. Pourquoi l'écran d'accueil à chaque lancement ?
R. Pour un choix explicite : suivre un tracé ou partir libre. Vous ne partez jamais « par erreur » dans le mauvais mode, et un lancement accidentel n'ouvre pas directement le GPS.

Q. Comment partager ma rando ?
R. Trois façons : à la fin, bouton « 📤 Partager le GPX » dans le résumé ; 📂 → « Mes GPX » → « 📤 Partager… » ; 🧰 → « Journaux » → partagez le journal JSON complet (stats + météo + parcours).

Q. Ma trace orange sautait de plusieurs kilomètres en ville !
R. Corrigé en v1.14 : tout fix GPS dont la précision annoncée dépasse 30 m, ou impliquant une vitesse impossible à pied (> 16 km/h sur plus de 20 m), est ignoré partout : écran, trace en direct et enregistrement.

Q. Pourquoi je ne peux pas télécharger OpenStreetMap en hors-ligne ?
R. La politique d'usage des serveurs OSM interdit le téléchargement massif de tuiles. OpenTopoMap et IGN l'autorisent, d'où la bascule proposée.

Q. La boussole indique n'importe quoi !
R. Les capteurs magnétiques se dérèglent près du métal ou d'aimants. Faites un « 8 » avec le téléphone à la main, à l'air libre.

Q. Ma position met du temps à apparaître.
R. Le premier fix GPS peut prendre 1 à 2 minutes, surtout en forêt ou en ville. Restez à l'air libre. L'altitude du GPS reste indicative (± 10–20 m, plus en effet canyon urbain).

Q. La carte ne se recentre plus sur moi !
R. Vous avez déplacé la carte à la main : le suivi s'est coupé volontairement. Réactivez-le dans 🧰 Outils → « 🎯 Suivi carte ».

Q. Comment créer mon propre GPX ?
R. Deux façons : marchez avec « ▶ Départ » puis terminez — le GPX s'exporte tout seul ; ou dessinez l'itinéraire sur gpx.studio (gratuit) et importez-le.

Q. Comment mettre à jour l'app ?
R. Téléchargez le dernier APK sur github.com/LethargicRunner/RandoGPX (section Releases) et installez-le par-dessus l'ancien — vos données sont conservées.

Bonne rando ! 🥾"""
}
