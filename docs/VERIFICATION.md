# APK and source verification report

**Audit date:** 19 November 2026  
**Files supplied:** `HisaabKitaab.zip` and a separate `HisaabKitaab.apk`.

## Results

| Test | Result | Evidence / limitation |
|---|---|---|
| ZIP source project extracted | **PASS** | Source, images, build script and embedded release present |
| Separately uploaded APK vs ZIP release APK | **PASS — byte-identical** | SHA-256 below matches for both |
| Supplied APK ZIP integrity / CRC | **PASS** | Every archived entry passed ZIP integrity check |
| UI asset matches source | **PASS — exact bytes** | `assets/index.html` equals `app/src/main/assets/index.html` (92,418 bytes) |
| APK includes Android runtime content | **PASS** | `AndroidManifest.xml`, `classes.dex`, bundled HTML, resources |
| DEX reference spot-check | **PASS** | `MainActivity` and bridge method indicators found |
| Build script syntax | **PASS** | `bash -n build.sh` |
| Clean compilation from source | **NOT VERIFIED** | Android SDK platform 34 and build-tools 34.0.0 unavailable in the audit environment |
| Build with original release signing identity | **NOT VERIFIED** | Original signing keystore is not provided |
| App runtime / exports / UI on a phone | **NOT TESTED** | No Android device/emulator test performed |
| APK signature verification with `apksigner` | **NOT VERIFIED** | Android SDK build-tools unavailable; signing files present in APK, but not cryptographically verified |

## Exact supplied APK hash

```text
SHA-256: bc7368a6ca0657378988b9d31cedad518cd2c06a4d71b466b1c880fc86069bc7
```

This hash applies to both the separately uploaded `HisaabKitaab.apk` and `release/HisaabKitaab-v6.0.apk` in the provided ZIP. The source repository's embedded `index.html` also matches the APK's embedded page byte-for-byte.

**Interpretation:** The ZIP and submitted binary clearly belong together at the asset/package-artifact level. This **does not prove** that rebuilding the source using the supplied script reproduces the APK, that compiled DEX is exactly derived from the supplied Java, or that the app passes functional/security tests.

## Reproduce the static checks

```bash
python3 scripts/verify_release.py
bash -n build.sh
```

## Complete a real compilation + signature audit locally

Set up JDK 17 and Android SDK platform 34 / build-tools 34.0.0 (README has commands). Set `ANDROID_SDK_ROOT`, original `KEYSTORE_FILE`, `KEYSTORE_PASS` and `KEY_PASS`, then run:

```bash
bash build.sh
"$ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner" verify --verbose --print-certs release/HisaabKitaab-v6.0.apk
"$ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner" verify --verbose --print-certs dist/HisaabKitaab.apk
"$ANDROID_SDK_ROOT/build-tools/34.0.0/aapt" dump badging release/HisaabKitaab-v6.0.apk
"$ANDROID_SDK_ROOT/build-tools/34.0.0/aapt" dump badging dist/HisaabKitaab.apk
sha256sum release/HisaabKitaab-v6.0.apk dist/HisaabKitaab.apk
```

Compare version, package, signing certificate fingerprints and functional behavior. Equal SHA-256 hashes would prove identical APK files; different hashes can occur because of toolchain/signing differences. Install and perform the README manual-QA checklist on a suitable Android device.

**Caution:** The original keystore was not supplied. Generating a new signing key will **not** reproduce the original app's signing identity and may prevent updating an existing installation without data loss.
