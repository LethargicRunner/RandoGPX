# Changelog RandoGPX

Chaque nouvelle version = une section `## vX.Y` en haut de ce fichier.
Le workflow publie automatiquement la première section en notes de release
(le tag et le nom viennent de `versionName` dans app/build.gradle.kts).

## v1.13

Sécurité & fiabilité (livraison 1 de la refonte V2) :
• Correctif : l'affichage restait bloqué sur « Enregistrement en cours »
  après avoir terminé une rando
• SMS de retour : après « Terminer », l'app propose de prévenir votre
  contact d'urgence que vous êtes revenu de rando
• Nom, difficulté (étoiles) et note libre saisis en fin de rando, écrits
  directement dans le GPX (<name>, <keywords>, <desc>) et le journal
• Le fichier GPX créé est annoncé avec son emplacement exact
• Boussole de visée : aiguille fixe, couronne tournante, pin orange vers
  l'objectif — alignez-le sous la flèche rouge pour vous diriger

## v1.12

Nouveautés v1.12 :
• Mode d'emploi complet intégré dans l'app : bouton ⓘ → « Mode d'emploi »
• 8 chapitres (démarrage, écran, enregistrement, navigation, cartes
  hors-ligne, mode nuit, outils, sécurité) + FAQ de 12 questions
• Copie GitHub : MODE_EMPLOI.md

## v1.11

Nouveautés v1.11 :
• Menu escamotable plus sûr : quand le menu est masqué, 🆘 Urgence
  (pleine largeur), ▶ Départ/Pause et ■ Terminer restent visibles

## v1.10.1

Correctif : l'app s'arrêtait systématiquement au lancement.
• CompassRose (mini-boussole) : ajout du constructeur XML requis
  par l'inflateur de layout (Context, AttributeSet)

## v1.10

🥾 RandoGPX v1.10

Navigation GPX pour la randonnée.

Nouveautés v1.10 :
• Menu escamotable : bouton « ▾ Masquer » pour libérer la carte
• Rose de boussole marine : aiguille rouge (nord) / bleue (sud),
  graduations, points cardinaux
• Mini-boussole permanente en haut à droite de l'écran
• Trajet quasi vide (< 50 m) : demande avant sauvegarde

v1.9 (rappel) :
• Mode nuit automatique au coucher du soleil (filtre sombre
  sur la carte, interface ambrée — fonctionne hors-ligne)
• Refonte de la palette de couleurs

v1.8 (rappel) :
• Export GPX automatique du parcours réel en fin de rando
• Trace du parcours dessinée en direct, même en usage libre
• Déplacer la carte coupe le suivi ; « Suivi » recentre

📲 Téléchargez un APK ci-dessous et installez-le
(autoriser les applications inconnues sur le téléphone).

## v1.9

Nouveautés v1.9 :
• Mode nuit automatique au coucher et au lever du soleil
  (calcul NOAA hors-ligne, carte assombrie, tons ambrés)
• Bascule manuelle : Cartes → 🌙 Mode nuit (auto / forcé jour / forcé nuit)
• Refonte de la palette de couleurs (verts rando, boutons teintés)

## v1.8

Nouveautés v1.8 :
• Export GPX automatique du parcours réel en fin de rando
• Trace du parcours dessinée en direct, même en usage libre
• Nouveau marqueur de position (pastille bleue anneau blanc)
• Déplacer la carte coupe le suivi automatiquement
