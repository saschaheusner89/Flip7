# Flip 7 – Android-App (APK)

Schlanke WebView-Hülle um `../index.html`. Die Seite läuft komplett offline aus der App
(`https://appassets.androidplatform.net/index.html`), Schriften sind eingebaut.

Die Hülle bringt gegenüber der reinen HTML:

- Display bleibt an
- kein Neuladen bei Splitscreen/Drehen
- Zurück-Taste schließt erst offene Fenster (`window.flip7Back`)
- „Teilen“ für die Datensicherung (`Flip7Native.share`)
- Tastatur zurückholen: Wurde sie weggewischt, bringt sie ein Tipp auf eine Spieler:in wieder (`Flip7Native.keyboard`)

## Damit Updates die Statistik behalten – die 4 Regeln

Android behält die Daten (Statistik, Medaillen, Namen, Farben) nur, wenn die neue APK als
**Update** über die installierte App geht und die Seite ihren Speicher wiederfindet:

| # | Was | Wert | Wenn falsch … |
|---|-----|------|---------------|
| 1 | Paket-Name | `io.github.saschaheusner89.flip7` | andere App → Statistik nicht da |
| 2 | Signaturschlüssel | `flip7-release.p12` (Fingerabdruck in `signing-cert.sha256`) | Android verweigert das Update |
| 3 | Lade-Adresse | `https://appassets.androidplatform.net/index.html` | App findet ihren Speicher nicht |
| 4 | versionCode | muss steigen | Android verweigert das „Downgrade“ |

`build.sh` prüft 1–3 bei jedem Build und **bricht ab**, statt eine unpassende APK zu erzeugen.
Nie die alte App deinstallieren, um ein Update zu erzwingen – dabei wäre die Statistik weg.
Vorher immer **Statistik → Daten übertragen → Sichern**.

## Normaler Weg: automatisch auf GitHub bauen (kein PC nötig)

Bei jeder Änderung an `index.html` auf `main` baut GitHub Actions
(`.github/workflows/build-apk.yml`) die APK mit dem richtigen Schlüssel.
Die fertige APK liegt dann immer unter derselben Adresse:

**https://github.com/saschaheusner89/Flip7/releases/latest/download/flip7.apk**

1. `index.html` ändern und im `<title>` die Version hochzählen, z. B. `Score v34`.
2. Auf `main` committen (geht auch im GitHub-Web-Editor am Handy).
3. Nach ca. 2–3 Minuten den Link oben am Handy öffnen und installieren.
   Das ist ein Update, die Statistik bleibt.

Der versionCode wird automatisch gesetzt (1000 + Build-Nummer) und steigt immer.

### Einmalige Einrichtung

In GitHub unter **Settings → Secrets and variables → Actions → New repository secret** zwei Einträge anlegen:

- **`FLIP7_KEYSTORE_B64`**: der Schlüssel als Text (Base64 von `flip7-release.p12`).
  Unter Windows erzeugt man ihn in PowerShell so:
  `[Convert]::ToBase64String([IO.File]::ReadAllBytes("flip7-release.p12"))`
- **`FLIP7_KS_PASS`**: das Passwort des Schlüssels.

Fehlen die Secrets, bricht der Build mit einer klaren Meldung ab. Er erzeugt nie einen neuen Schlüssel.

## Selbst bauen (Linux/WSL/Git-Bash, ohne Android-SDK)

```bash
KS_PASS='<passwort>' VERSION_CODE=<höher als installiert> ./build.sh   # → build/flip7-v<Version>.apk
```

Die Werkzeuge lädt `build.sh` selbst von Maven Central bzw. GitHub nach `.tools/`:

- aapt2 aus apktool
- dx
- apksig
- android-all als API-35-Klassen

Den Schlüssel `flip7-release.p12` neben `build.sh` legen.

## Anders bauen (z. B. Android Studio)?

Das geht, solange alle 4 Regeln eingehalten werden:

- `MainActivity.java`, `AndroidManifest.xml`, `res/` und `assets/fonts/` aus diesem Ordner übernehmen.
- `index.html` nach `assets/` legen.
- Mit `flip7-release.p12` signieren (Typ PKCS12, Alias `flip7`, Schlüssel-Passwort = Keystore-Passwort).
- Den Fingerabdruck des Zertifikats mit `signing-cert.sha256` vergleichen.

## Sicherheitsnetz

Unter **Statistik → Daten übertragen** lässt sich alles als Text sichern und in einer anderen App bzw. auf
einem anderen Handy wieder laden. Ab und zu **Sichern → Teilen** (z. B. per Mail an sich selbst) schadet nie.

## Signaturschlüssel

`flip7-release.p12` und das Passwort **nie ins Git** – das Repo ist öffentlich.
Beides sicher aufbewahren, z. B. im Passwort-Manager oder in der Cloud.
Geht der Schlüssel verloren, sind keine Updates mehr möglich. Dann bleibt nur der Umzug per „Daten übertragen“ in eine neu signierte App.

## Web-App (iPhone und Browser)

GitHub Pages veröffentlicht `main` unter **https://saschaheusner89.github.io/Flip7/**.
Dazu gehören `index.html`, `manifest.webmanifest`, `sw.js` (Offline-Speicher), `icons/` und `.nojekyll`.

- iPhone: Link in Safari öffnen → Teilen → „Zum Home-Bildschirm“. Dann Vollbild, offline, eigener Ziffernblock.
- Updates: `index.html` auf `main` ändern. Die Web-App holt die neue Version im Hintergrund, sie gilt ab dem nächsten Start.
- Das Repo nicht umbenennen: Die Adresse ist Teil der installierten Web-App.
- Auf dem iPhone löscht das Entfernen des Home-Bildschirm-Symbols auch die Daten der Web-App.
