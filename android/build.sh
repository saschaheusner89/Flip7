#!/usr/bin/env bash
# Baut die Flip-7-APK aus ../index.html – ganz ohne Android-SDK/Gradle.
# Werkzeuge kommen von Maven Central bzw. GitHub (aapt2 steckt in apktool).
#
#   ./build.sh                    → build/flip7-v<VERSION>.apk
#   VERSION_CODE=34 VERSION_NAME=34 KS_PASS=… ./build.sh
#
# WICHTIG: Updates lassen sich nur über die alte App installieren, wenn sie mit
# DEMSELBEN Schlüssel signiert sind (KEYSTORE). Der Schlüssel gehört nicht ins Git.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
TOOLS="${TOOLS:-$HERE/.tools}"
OUT="$HERE/build"
KEYSTORE="${KEYSTORE:-$HERE/flip7-release.p12}"   # nicht ins Git (Repo ist öffentlich)!
KS_PASS="${KS_PASS:-}"
VERSION_CODE="${VERSION_CODE:-33}"
VERSION_NAME="${VERSION_NAME:-33}"
MIN_SDK=24
TARGET_SDK=35

mkdir -p "$TOOLS"
fetch() { [ -s "$TOOLS/$1" ] || { echo "Lade $1 …"; curl -fsSL --retry 3 -o "$TOOLS/$1" "$2"; }; }
fetch apktool.jar     https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar
fetch dx.jar          https://repo1.maven.org/maven2/com/jakewharton/android/repackaged/dalvik-dx/16.0.1/dalvik-dx-16.0.1.jar
fetch apksig.jar      https://repo1.maven.org/maven2/com/android/tools/build/apksig/2.3.0/apksig-2.3.0.jar
fetch android-all.jar https://repo1.maven.org/maven2/org/robolectric/android-all/15-robolectric-13954326/android-all-15-robolectric-13954326.jar
if [ ! -x "$TOOLS/aapt2" ]; then
  (cd "$TOOLS" && unzip -o -q -j apktool.jar prebuilt/linux/aapt2 prebuilt/android-framework.jar && chmod +x aapt2)
fi

rm -rf "$OUT" && mkdir -p "$OUT/classes" "$OUT/assets/fonts"

echo "1/5 Assets (HTML + Schriften)"
cp "$HERE/../index.html" "$OUT/assets/index.html"
cp "$HERE/assets/fonts/"* "$OUT/assets/fonts/"

echo "2/5 Java → classes.dex"
javac --release 8 -nowarn -Xlint:-options -encoding UTF-8 -cp "$TOOLS/android-all.jar" -d "$OUT/classes" \
  $(find "$HERE/src" -name '*.java')
java -cp "$TOOLS/dx.jar" com.android.dx.command.Main --dex --min-sdk-version=$MIN_SDK \
  --output="$OUT/classes.dex" "$OUT/classes"

echo "3/5 Ressourcen + Manifest (aapt2)"
"$TOOLS/aapt2" compile --dir "$HERE/res" -o "$OUT/res.zip"
"$TOOLS/aapt2" link -o "$OUT/linked.apk" -I "$TOOLS/android-framework.jar" \
  --manifest "$HERE/AndroidManifest.xml" -A "$OUT/assets" "$OUT/res.zip" \
  --min-sdk-version $MIN_SDK --target-sdk-version $TARGET_SDK \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME"

echo "4/5 Packen + Ausrichten"
python3 "$HERE/tools/package.py" build "$OUT/linked.apk" "$OUT/classes.dex" "$OUT/unsigned.apk"

echo "5/5 Signieren (APK Signature Scheme v2)"
if [ ! -f "$KEYSTORE" ]; then
  KS_PASS="${KS_PASS:-$(python3 -c 'import secrets; print(secrets.token_urlsafe(12))')}"
  keytool -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -alias flip7 -keyalg RSA -keysize 3072 -validity 36500 -dname "CN=Flip 7 Score Tracker" >/dev/null 2>&1
  echo "NEUER Schlüssel: $KEYSTORE  Passwort: $KS_PASS  → beides sicher aufbewahren (für alle Updates nötig)!"
fi
[ -n "$KS_PASS" ] || { echo "Bitte KS_PASS (Passwort von $KEYSTORE) setzen"; exit 1; }
APK="$OUT/flip7-v$VERSION_NAME.apk"
# apksig 2.3.0 (neueste Version auf Maven Central) braucht beim Laden dieses JDK-Interna
java --add-exports java.base/sun.security.x509=ALL-UNNAMED -cp "$TOOLS/apksig.jar" "$HERE/tools/Sign.java" "$OUT/unsigned.apk" "$APK" "$KEYSTORE" "$KS_PASS"
python3 "$HERE/tools/package.py" check "$APK"
echo "Fertig: $APK ($(du -h "$APK" | cut -f1))"
