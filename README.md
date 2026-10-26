# 🥾 RandoGPX

Navigation sur tracés GPX pour la randonnée : suivi GPS temps réel,
alerte hors-tracé, stats de parcours, waypoints, cartes
OpenStreetMap / OpenTopoMap / IGN, mode hors-ligne, bibliothèque de GPX
et journaux de rando avec météo.

## Données utilisateur

- GPX sauvegardés dans Documents/RandoGPX/ (réutilisables par toute application)
- Journaux dans Documents/RandoGPX/logs/ (format JSON lisible)
- Tuiles de carte en cache dans le dossier de l'application

## Télécharger l'APK

Onglet **Actions** → dernier run **Build APK** → artefact **RandoGPX-APK**.

## Compiler dans GitHub Codespaces

Dans le terminal du Codespace :

    gradle wrapper --gradle-version 8.7
    ./gradlew assembleDebug

APK obtenu dans app/build/outputs/apk/debug/app-debug.apk.
