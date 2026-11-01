# Changelog RandoGPX

Chaque nouvelle version = une section `## vX.Y` en haut de ce fichier.
Le workflow publie automatiquement la première section en notes de release
(le tag et le nom viennent de `versionName` dans app/build.gradle.kts).

## v2.4

Bandeau live pendant la rando :
• Temps écoulé et distance parcourue affichés en permanence
  pendant l'enregistrement (ex. « 🔴 ⏱ 1 h 05 • 4,5 km parcourus »)
• Avec un GPX chargé, la distance restante s'ajoute au bandeau
  (ex. « • reste 12,3 km »)
• En pause, le bandeau garde le résumé (le chrono est cumulé,
  les pauses ne comptent pas)
• En rando libre, temps + distance uniquement (pas de « reste »)

## v2.3

Cartes prêtes avant de quitter le réseau :
• Au moment d'appuyer sur « ▶ Départ » avec un GPX chargé, l'app
  propose de télécharger toutes les tuiles couvrant la zone du
  tracé (zooms 14–18, OpenTopoMap ou IGN Plan)
• Le téléchargement tourne en arrière-plan : la rando démarre
  sans attendre — un message confirme quand la zone est prête
• Avec OpenStreetMap standard (qui interdit le téléchargement
  massif), l'app propose de basculer sur OpenTopoMap ou IGN Plan
• « ⬇️ Cartes hors-ligne » du menu 📂 garde le même comportement
  et réutilise le même téléchargement

## v2.2

Le tracé prend les choses en main :
• Marqueur « 🏁 Départ » sur la carte dès qu'un GPX est chargé
• 📂 → « 🚗 Aller au point de départ » : ouvre votre app de
  navigation (Google Maps, Waze…) en mode itinéraire vers le
  départ de la rando
• Flèches vertes espacées le long du tracé : la direction générale
  se lit d'un coup d'œil (espacement auto, max ~25 flèches)
• Direction générale affichée en permanence dans le bandeau
  (ex. « ↗ direction générale 47° »)
• Flèche orange clignotante semi-transparente au bas de l'écran :
  indique la direction du prochain tronçon, suit le cap du téléphone
• La mini-boussole s'ouvre en GRANDE boussole d'un simple tap
  (entrée retirée du menu 🧰)
• « Mes GPX » affiche plus clairement : nom + taille + date, trié
  du plus récent au plus ancien

## v2.1

Menus encore plus simples :
• 🌤️ « Ciel du jour » fusionne la météo et les éphémérides en un seul
  écran — la prévision du jour s'affiche AU-DESSUS du lever/coucher du
  soleil, de la lumière restante et de la phase de lune
• Bouton « 📅 Prévisions à 5 jours » (au lieu de 7) depuis ce même écran
• Le menu 🧰 Outils gagne en lisibilité : une ligne de moins
• Fonctionne aussi en rando libre (position GPS) et sans réseau :
  les éphémérides restent affichées, seule la météo est absente

## v2.0.1

La grande refonte V2 (livraison 2) :
• Écran d'accueil à chaque lancement : « Bienvenue, où allons-nous
  aujourd'hui ? » avec 2 grandes cartes — Suivre un GPX / Rando libre
• Barre du bas allégée : 📂 GPX & cartes et 🧰 Outils en menus
  déroulants ; ▶ Départ, ■ Terminer et 🆘 toujours accessibles
• Bouton « ❓ » rond en haut à droite : aide en 3 sections
  (Prise en main, FAQ, À propos)
• Palette « ville/nature » : ardoise/blanc, accents vert forêt et
  orange ; le panneau noir du haut devient un bandeau discret
• Accès direct aux fichiers : journaux avec Ouvrir/Partager, partage
  GPX depuis « Mes GPX », bouton « 📤 Partager le GPX » dans le
  résumé de fin de rando
• Sauvegarde cloud GitHub : dépôt privé + jeton fine-grained collé
  dans l'app (🧰 Outils → ☁️), envoi immédiat en fin de rando et file
  d'attente automatique sans réseau
• Conserve le filtre GPS anti-glitch de la v1.14

## v1.14

Fiabilité GPS en ville :
• Nouveau filtre anti-glitch appliqué à TOUTE l'app : un fix GPS
  douteux (précision annoncée > 30 m, ou déplacement impossible à
  pied : plus de ~16 km/h sur plus de 20 m) est ignoré
• Fini les sauts de plusieurs kilomètres : pastille de position,
  trace orange en direct et distance au tracé restent sur la
  dernière position fiable
• L'enregistrement GPX bénéficie du même filtre (double protection)
• Rappel : l'altitude GPS reste indicative en ville (effet canyon
  urbain), ne vous y fiez pas sur pied

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
