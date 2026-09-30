# Flip 7 – Android-App (APK)

Schlanke WebView-Hülle um `../index.html`. Die Seite läuft komplett offline aus der App
(`https://appassets.androidplatform.net/index.html`), Schriften sind eingebaut.

Die Hülle bringt gegenüber der reinen HTML:

- Display bleibt an
- kein Neuladen bei Splitscreen/Drehen
- Zurück-Taste schließt erst offene Fenster (`window.flip7Back`)

## Bauen (ohne Android-SDK)

```bash
KS_PASS='<passwort>' ./build.sh                       # → build/flip7-v33.apk
VERSION_CODE=34 VERSION_NAME=34 KS_PASS='…' ./build.sh
```

Die Werkzeuge lädt `build.sh` selbst von Maven Central bzw. GitHub nach `.tools/`:

- aapt2 aus apktool
- dx
- apksig
- android-all als API-35-Klassen

## Signaturschlüssel

`flip7-release.p12` (+ Passwort) **nie ins Git** – das Repo ist öffentlich.
Updates lassen sich nur mit demselben Schlüssel über die installierte App installieren.
Mit einem anderen Schlüssel müsste man die App erst deinstallieren, und dabei gehen Statistik und Medaillen verloren.
Fehlt der Schlüssel, erzeugt `build.sh` einen neuen.
