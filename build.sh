#!/bin/bash
# Hisaab Kitaab - build script (manual Android pipeline, no Gradle)
# Builds a signed release APK at dist/HisaabKitaab.apk
#
# Requirements: JDK 17, Android SDK (platform android-34, build-tools 34.0.0), zip
# Signing: set the keystore passwords as environment variables before running:
#   export KEYSTORE_PASS='your-store-password'
#   export KEY_PASS='your-key-password'
# If gharhisab-release.jks does not exist yet, it is generated on first run.
# NEVER commit the keystore file or its passwords to Git (see .gitignore).
set -e

# ---- Version (keep in sync with AndroidManifest.xml) ----
VERSION_CODE=6
VERSION_NAME=6.0

# ---- Paths (relative to this script, so the project can live anywhere) ----
ROOT="$(cd "$(dirname "$0")" && pwd)"
PROJ="$ROOT/app/src/main"
BUILD="$ROOT/build"
DIST="$ROOT/dist"

# ---- Java ----
if [ -z "$JAVA_HOME" ]; then
  for d in /usr/lib/jvm/java-17-openjdk-amd64 /usr/lib/jvm/java-17-openjdk /usr/lib/jvm/java-17; do
    [ -d "$d" ] && JAVA_HOME="$d" && break
  done
fi
[ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH"
java -version >/dev/null 2>&1 || { echo "ERROR: Java not found. Install JDK 17 and/or set JAVA_HOME."; exit 1; }

# ---- Android SDK ----
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK" ]; then
  for d in "$HOME/workspace/tools/android-sdk" "$HOME/Android/Sdk" "$HOME/Library/Android/sdk"; do
    [ -d "$d" ] && SDK="$d" && break
  done
fi
[ -d "$SDK" ] || { echo "ERROR: Android SDK not found. Set ANDROID_SDK_ROOT to your SDK path."; exit 1; }
BT="$SDK/build-tools/34.0.0"
PLAT="$SDK/platforms/android-34/android.jar"
[ -f "$BT/aapt2" ] || { echo "ERROR: build-tools 34.0.0 not found in $SDK"; exit 1; }
[ -f "$PLAT" ] || { echo "ERROR: platform android-34 not found in $SDK"; exit 1; }

# ---- Keystore passwords (from environment, never hardcoded) ----
KS="${KEYSTORE_FILE:-$ROOT/gharhisab-release.jks}"
if [ -z "$KEYSTORE_PASS" ]; then
  read -r -s -p "Keystore password: " KEYSTORE_PASS; echo
fi
KEY_PASS="${KEY_PASS:-$KEYSTORE_PASS}"

rm -rf "$BUILD"; mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$DIST"

echo "== aapt2 compile =="
"$BT/aapt2" compile --dir "$PROJ/res" -o "$BUILD/compiled_res.zip"

echo "== aapt2 link =="
"$BT/aapt2" link -o "$BUILD/base.apk" -I "$PLAT" \
  --manifest "$PROJ/AndroidManifest.xml" \
  -R "$BUILD/compiled_res.zip" -A "$PROJ/assets" \
  --auto-add-overlay --java "$BUILD/gen" \
  --min-sdk-version 24 --target-sdk-version 34 \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME"

echo "== javac =="
find "$PROJ/java" "$BUILD/gen" -name "*.java" > "$BUILD/sources.txt"
javac -encoding UTF-8 -source 8 -target 8 -cp "$PLAT" -d "$BUILD/classes" @"$BUILD/sources.txt"

echo "== d8 =="
"$BT/d8" --lib "$PLAT" --min-api 24 --output "$BUILD/dex" $(find "$BUILD/classes" -name "*.class")

echo "== add dex =="
( cd "$BUILD/dex" && zip -q "$BUILD/base.apk" classes.dex )

echo "== zipalign =="
"$BT/zipalign" -f 4 "$BUILD/base.apk" "$BUILD/aligned.apk"

echo "== keystore =="
if [ ! -f "$KS" ]; then
  echo "No keystore found at $KS - generating a new one (keep it safe, never share it)."
  keytool -genkeypair -keystore "$KS" -alias gharhisab -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass "$KEYSTORE_PASS" -keypass "$KEY_PASS" \
    -dname "CN=Muhammad Saad Jadoon, O=Hisaab Kitaab, C=PK"
fi

echo "== apksigner =="
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$KEYSTORE_PASS" --key-pass "pass:$KEY_PASS" \
  --out "$DIST/HisaabKitaab.apk" "$BUILD/aligned.apk"

echo "== verify =="
"$BT/apksigner" verify "$DIST/HisaabKitaab.apk"

echo "== DONE =="
ls -lh "$DIST/HisaabKitaab.apk"
echo "Install on a phone with: adb install -r \"$DIST/HisaabKitaab.apk\""
