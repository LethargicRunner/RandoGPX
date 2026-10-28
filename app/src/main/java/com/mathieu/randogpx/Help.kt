package com.mathieu.randogpx

/**
 * Mode d'emploi complet + FAQ de RandoGPX (affiché dans l'app via ⓘ).
 */
object Help {
    fun manual(): String = MANUAL

    private val MANUAL =
```🥾 RANDOGPX — MODE D'EMPLOI

═══════════════════════════════
1. DÉMARRER EN 30 SECONDES
═══════════════════════════════
• Avec un tracé : bouton « GPX » → choisissez un fichier .gpx (créé sur gpx.studio, Visorando, IGN Rando…). Il est copié dans Documents/RandoGPX et redevient disponible via « Mes GPX ».
• Sans tracé (usage libre) : appuyez simplement sur « ▶ Départ » et marchez. Votre parcours se dessine en orange sur la carte.
• « ⏸ Pause » pour une pause, « ▶ Reprendre » pour continuer, « ■ Terminer » à la fin.

═══════════════════════════════
2. COMPRENDRE L'ÉCRAN
═══════════════════════════════
• En haut : nom du tracé, distance restante, durée estimée, état de la rando.
• En haut à droite : mini-boussole (le rouge indique le nord).
• Sur la carte : pastille bleue = votre position ; tracé bleu = itinéraire prévu ; tracé orange = votre parcours réel ; points = waypoints du GPX.
• En bas : rangées de boutons. « ▾ Masquer » les replie pour libérer la carte — 🆘 Urgence, ▶ Départ/Pause et ■ Terminer restent toujours visibles.

═══════════════════════════════
3. ENREGISTRER UNE RANDO
═══════════════════════════════
• « ▶ Départ » déclenche le rappel « avez-vous prévenu un ami ? », puis l'enregistrement démarre. Il continue en arrière-plan, même écran éteint ou app réduite (notification « Enregistrement »).
• GPS ADAPTATIF : mesure toutes les 5 s à l'approche d'un virage (< 200 m), 15 s à moyenne distance, jusqu'à 60 s en ligne droite → la batterie dure beaucoup plus longtemps.
• En pause, le GPS est totalement coupé.
• Sauvegarde automatique toutes les 5 minutes : si batterie vide ou crash, l'app propose de récupérer la rando au prochain lancement.
• « ■ Terminer » enregistre le journal (JSON dans Documents/RandoGPX/logs) ET un fichier GPX de votre parcours réel (dans Documents/RandoGPX, visible dans « Mes GPX », partageable et réimportable).
• Si vous avez bougé de moins de 50 m, l'app vous demande si ça vaut le coup de sauvegarder.

═══════════════════════════════
4. NAVIGATION PAS À PAS
═══════════════════════════════
• Panneau vert en haut de la barre : ↰ virage à gauche, ↱ à droite, ↑ tout droit, avec la distance. Un bip sonore retentit à moins de 150 m de la bifurcation.
• Si vous vous écartez de plus de 50 m du tracé : vibration + alerte, panneau ⤺ « demi-tour », et un bouton « 🏁 Finir ici » apparaît.
• « 🏁 Finir ici » : clôture la rando à cet endroit puis vous guide au retour en suivant VOTRE chemin aller, à l'envers (tracé orange).
• Le bouton « Suivi » (toggle) : déplacer la carte à la main coupe automatiquement le suivi — vous pouvez explorer librement. Réactivez « Suivi » pour vous recentrer.

═══════════════════════════════
5. CARTES ET HORS-LIGNE
═══════════════════════════════
• « Cartes » : OpenStreetMap (standard), OpenTopoMap (relief), IGN Plan.
• « Hors-ligne » : télécharge les tuiles autour de votre tracé (zooms 14 à 18) pour naviguer sans réseau. À faire AVANT la rando, en Wi-Fi.
• Le téléchargement massif est bloqué sur OpenStreetMap (politique d'usage des serveurs OSM) : l'app propose de basculer sur OpenTopoMap ou IGN, qui l'autorisent.

═══════════════════════════════
6. MODE NUIT 🌙
═══════════════════════════════
• Bascule AUTOMATIQUE au coucher du soleil et au lever du jour (calcul hors-ligne, dès que vous avez une position).
• Carte assombrie et interface ambrée : préserve la vision nocturne.
• Forçage possible : Cartes → « 🌙 Mode nuit » (auto → nuit forcée → jour forcé).

═══════════════════════════════
7. BOUSSOLE, MÉTÉO, ÉPHÉMÉRIDES
═══════════════════════════════
• 🧭 Boussole : rose marine (rouge = nord, bleu = sud), cap en degrés, direction du prochain waypoint.
• ☁️ Météo : prévisions sur 7 jours (Open-Meteo) — nécessite un accès réseau ponctuel.
• 🌙 Éphémérides : lever/coucher du soleil, lumière restante, phase de lune — 100 % hors-ligne.

═══════════════════════════════
8. SÉCURITÉ 🆘
═══════════════════════════════
• AVANT de partir : définissez votre contact d'urgence (Urgence → « Définir le contact ») et envoyez-lui le SMS de procédure qui s'ouvre automatiquement.
• Le menu Urgence : SMS SOS pré-rempli avec votre position et un lien carte (VOUS appuyez toujours sur Envoyer), partage de position, appel 112, coordonnées en grand à dicter aux secours.
• ⚠ Honnêteté : un SOS par SMS suppose d'avoir du réseau. En zone blanche, AUCUNE application ne remplace une balise PLB ou un satellite messenger — n'en partez pas pour une rando engagée sans l'un des deux.

═══════════════════════════════
FAQ — QUESTIONS FRÉQUENTES
═══════════════════════════════
Q. L'app continue-t-elle vraiment écran éteint ?
R. Oui, c'est un service GPS « au premier plan ». Sur Xiaomi/MIUI, pensez à paramètres Batterie → RandoGPX → « Aucune restriction », sinon le système peut fermer l'app en arrière-plan.

Q. Où sont mes données ?
R. Tout est local, dans Documents/RandoGPX sur votre téléphone. Pas de compte, pas de pub, aucune position envoyée nulle part (sauf ce que vous partagez vous-même).

Q. Pourquoi je ne peux pas télécharger OpenStreetMap en hors-ligne ?
R. La politique d'usage des serveurs OSM interdit le téléchargement massif de tuiles. OpenTopoMap et IGN l'autorisent, d'où la bascule proposée.

Q. La boussole indique n'importe quoi !
R. Les capteurs magnétiques se dérèglent près du métal ou d'aimants. Faites un « 8 » avec le téléphone à la main, à l'air libre. Si ça persiste, éloignez-vous de tout objet métallique.

Q. Ma position met du temps à apparaître.
R. Le premier fix GPS peut prendre 1 à 2 minutes, surtout en forêt ou en ville. Restez à l'air libre. L'altitude du GPS reste indicative (± 10–20 m).

Q. La carte ne se recentre plus sur moi !
R. Vous avez déplacé la carte à la main : le suivi s'est coupé volontairement. Appuyez sur « Suivi ».

Q. Le mode nuit s'active alors que je suis en after-ski bien éclairé.
R. Il suit le vrai coucher du soleil de votre position. Pour forcer : Cartes → 🌙 Mode nuit.

Q. Comment créer mon propre GPX ?
R. Deux façons : marchez avec « ▶ Départ » puis terminez — le GPX s'exporte tout seul ; ou dessinez l'itinéraire sur gpx.studio (gratuit) et importez-le.

Q. Comment partager ma rando à un ami ?
R. À la fin, le fichier rando_….gpx est dans Documents/RandoGPX (et « Mes GPX ») : partagez-le depuis n'importe quelle app, ou envoyez-le-lui pour qu'il l'importe dans la sienne.

Q. La météo ne s'affiche pas.
R. Elle nécessite un accès réseau ponctuel (Open-Meteo). Sans réseau : les éphémérides, elles, marchent toujours.

Q. Comment mettre à jour l'app ?
R. Téléchargez le dernier APK sur github.com/LethargicRunner/RandoGPX (section Releases) et installez-le par-dessus l'ancien — vos données sont conservées.

Q. Puis-je couper le réseau pendant la rando ?
R. Oui, après avoir téléchargé la zone en hors-ligne. GPS, navigation, boussole, éphémérides et enregistrement fonctionnent sans réseau ; seule la météo en a besoin.

Bonne rando ! 🥾```
}
