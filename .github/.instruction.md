# CAHIER DES CHARGES : PROJET TRANSITO-LIBRE (ANDROID 7)

## 1. OBJECTIF GLOBAL
Créer une application Android native capable de fonctionner hors-ligne (Offline First), affichant une carte libre et permettant de consulter les horaires de transport (GTFS) et de calculer des itinéraires.

## 2. STACK TECHNIQUE IMPOSÉE
* **OS Cible :** Android 7.0 (API Level 24) minimum.
* **Langage :** Java 8 (ou Kotlin configuré pour JVM 1.8).
* **Architecture :** MVVM (Model-View-ViewModel) obligatoire.
* **Carte :** MapLibre Native SDK for Android.
* **Base de Données :** Room Database (pour parser et stocker le GTFS).
* **Réseau :** Retrofit (pour télécharger les tuiles ou mises à jour GTFS).
* **Injection de dépendance :** Koin ou Dagger Hilt (léger).

## 3. STRUCTURE DE DONNÉES (GTFS -> SQL)
L'application doit parser un fichier .zip GTFS et peupler une base SQLite locale.
Tables requises (Mapping 1:1 avec GTFS) :
* `Agency` (agency_id, name)
* `Stops` (stop_id, name, lat, lon) -> **Indexé pour la recherche spatiale.**
* `Routes` (route_id, short_name, long_name, color)
* `Trips` (trip_id, service_id, headsign)
* `StopTimes` (trip_id, arrival_time, stop_id, stop_sequence) -> **Très volumineux, optimisation requise.**

## 4. UI / UX (DÉTAILS DES ÉCRANS)
### A. Écran Principal (MapFragment)
* Plein écran avec MapLibre.
* Bouton FAB "Ma position" (boussole).
* Barre de recherche en haut (Searchview) connectée à la base `Stops` et à l'API Photon (Geocoding).
* Overlay : Les arrêts de bus proches s'affichent sous forme de petits cercles cliquables.

### B. Feuille de détail (BottomSheet)
* Au clic sur un arrêt, un panneau glisse du bas.
* Affiche : Nom de l'arrêt + Liste des prochains passages (requête SQL sur `StopTimes`).

### C. Écran Itinéraire
* Deux champs : Départ (GPS actuel par défaut) / Arrivée.
* Appel API vers GraphHopper.
* Résultat : Tracé Polyline sur la carte + Instructions textuelles étape par étape.

## 5. CONTRAINTES DE QUALITÉ
* **Gestion d'erreur :** Si le GPS est coupé, l'app ne doit pas crasher (afficher un Toast).
* **Performance :** Le parsing du GTFS (qui peut prendre 2 minutes) doit se faire dans un `Worker` (WorkManager) avec une notification de progression.
* **Tests :** Chaque ViewModel doit avoir un test unitaire JUnit.
