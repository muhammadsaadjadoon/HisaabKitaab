<div align="center">

# Hisaab Kitaab
### Offline Android Home Expense Manager

**Track income • Manage expenses • Monitor savings • Export your records**

![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white) ![Version](https://img.shields.io/badge/Version-6.0-17654A) ![Java](https://img.shields.io/badge/Java-Android_WebView-orange) ![License](https://img.shields.io/badge/License-MIT-blue) ![Offline](https://img.shields.io/badge/Offline-Yes-brightgreen)

**Developed by Muhammad Saad Jadoon**

</div>

Hisaab Kitaab is an offline-first household finance tracker for Android. Log everyday transactions, review each month's balance, analyze spending, and export reports without an account or cloud service. The native Android shell is written in Java; the interface is packaged as HTML, CSS and JavaScript inside the APK.

## App preview

| Dashboard | Reports | Calendar | Menu |
|:---:|:---:|:---:|:---:|
| ![Dashboard](screenshots/dashboard.jpg) | ![Reports](screenshots/reports.jpg) | ![Calendar](screenshots/calendar.jpg) | ![Menu](screenshots/menu-drawer.jpg) |

## Features

- **Income, expenses, savings and deductions:** Create, edit and delete transactions; add relevant categories, notes, sources, destinations and reasons.
- **Dashboard:** Balance, current-month totals, spending insights and recent activity.
- **Monthly records and calendar:** Browse dated transactions and previous months.
- **Reports:** Monthly breakdowns, category charts and savings information.
- **Exports:** PDF, Excel (`.xlsx`), CSV and JSON backup/restore.
- **Profile and preferences:** Personal information, date format, week start and amount-hiding mode.
- **On-device storage:** WebView local storage mirrored to native Android SharedPreferences; no account required.
- **Notifications:** Optional month-end summary reminders.

## Technical overview

| Item | Detail |
|---|---|
| Application ID | `com.saadjadoon.gharhisab` |
| Version | `6.0` / version code `6` |
| Minimum Android | Android 7.0 / API 24 |
| Target / compile SDK | API 34 |
| Native | Java `MainActivity` and JavaScript bridge |
| Frontend | `app/src/main/assets/index.html` (bundled HTML/CSS/JS) |
| Build system | `build.sh` (Android SDK command-line tools; **not Gradle**) |
| Signing | Android keystore (not included in this repository) |
| License | MIT, see [LICENSE](LICENSE) |

The Android WebView loads the bundled local page at `file:///android_asset/index.html`. The native bridge handles local preferences, notifications and file exports. There are no external frontend dependencies referenced in the bundled HTML.

## Get the app

The APK included with the provided project is at [`release/HisaabKitaab-v6.0.apk`](release/HisaabKitaab-v6.0.apk). Install the downloaded APK on a supported Android device after checking the source and trustworthiness of the release. **This APK was supplied with the project; it has not been reproduced from source in this audit environment.** See [verification results](docs/VERIFICATION.md).

For Android SDK installations with `adb`:

```bash
adb devices
adb install -r release/HisaabKitaab-v6.0.apk
```

If Android reports a signing-certificate mismatch, **do not uninstall the existing app without backing up your data**. Android updates require the same application ID and a compatible signing certificate.

## Project structure

```text
HisaabKitaab/
├── app/src/main/
│   ├── AndroidManifest.xml            # package, version and permissions
│   ├── assets/index.html              # complete offline UI
│   ├── java/com/saadjadoon/gharhisab/
│   │   └── MainActivity.java          # WebView, bridge and exports
│   └── res/                            # styles, app label and icons
├── screenshots/                       # sample app screens
├── release/HisaabKitaab-v6.0.apk      # supplied binary release
├── scripts/verify_release.py          # archive/source consistency tests
├── docs/VERIFICATION.md               # audit and rebuild limitations
├── build.sh                            # command-line APK builder
├── icon_master.png
├── CHANGELOG.md
├── LICENSE
└── README.md
```

## Prerequisites for building

Install these on **Linux, macOS, or Windows with Git Bash/WSL**:

1. **JDK 17** (recommended for the project's build pipeline); `java`, `javac`, `keytool` on PATH.
2. **Android SDK Platform 34**, **Android SDK Build-Tools 34.0.0** (`aapt2`, `d8`, `zipalign`, `apksigner`) and optionally **Android SDK Platform-Tools** (`adb`).
3. `bash`, `zip`, `unzip` and Python 3 (only for optional verification).

With Android SDK Command-line Tools already installed and `sdkmanager` available:

```bash
sdkmanager --licenses
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
java -version
javac -version
```

Set the SDK root. Example for Linux:

```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
```

Example for macOS:

```bash
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
```

Example for Windows Git Bash (change username/path for your machine):

```bash
export ANDROID_SDK_ROOT="/c/Users/YOUR_USERNAME/AppData/Local/Android/Sdk"
```

For Windows, use **Git Bash or WSL**, not PowerShell directly to execute the `.sh` script. If WSL is used, the installed SDK tools must match the WSL/Linux environment; Windows executables are not a drop-in replacement for the script.

## Build from source

Clone your repository after publishing it:

```bash
git clone https://github.com/YOUR_USERNAME/HisaabKitaab.git
cd HisaabKitaab
```

### Important: signing identity

The supplied ZIP contains **no original signing keystore**. To build an APK that can update the supplied release on an existing phone, obtain the original keystore and passwords privately. Never commit or share them.

```bash
export KEYSTORE_FILE="$PWD/gharhisab-release.jks"
export KEYSTORE_PASS='YOUR_PRIVATE_STORE_PASSWORD'
export KEY_PASS='YOUR_PRIVATE_KEY_PASSWORD'
chmod +x build.sh
./build.sh
```

The script creates `dist/HisaabKitaab.apk`. If the keystore file doesn't exist, `build.sh` **generates a different new signing key**. That APK will not install as an update over the supplied APK. For a first independent install or fork only, a new key is appropriate. Securely back up the new keystore.

To inspect the build and install it:

```bash
"$ANDROID_SDK_ROOT/build-tools/34.0.0/apksigner" verify --verbose --print-certs dist/HisaabKitaab.apk
"$ANDROID_SDK_ROOT/build-tools/34.0.0/aapt" dump badging dist/HisaabKitaab.apk
adb install -r dist/HisaabKitaab.apk
```

### Build stages

`build.sh` invokes `aapt2 compile` → `aapt2 link` → `javac` → `d8` → `zip` → `zipalign` → `apksigner sign` → `apksigner verify`. The script checks the Android SDK paths before building.

### Verify the supplied binary and sources

Run the included **stdlib-only** automated checks:

```bash
python3 scripts/verify_release.py
```

They check the APK archive, matching source asset, version strings, included DEX file and known SHA-256. They **do not replace** a clean compile, cryptographic certificate verification or runtime tests. See [the complete audit](docs/VERIFICATION.md).

To compare a freshly built APK with the supplied binary:

```bash
sha256sum release/HisaabKitaab-v6.0.apk dist/HisaabKitaab.apk
```

Signed APKs can differ byte-for-byte due to signing key, toolchain, timestamps, ordering and build configuration, even when the app is functionally equivalent. Matching APK hashes is strong binary identity evidence; mismatching hashes alone does not imply different app behavior.

## Manual QA checklist

After installing on a real phone: (1) launch and finish onboarding; (2) add each transaction type; (3) edit/delete an entry; (4) inspect monthly totals and calendar; (5) export PDF/XLSX/CSV and open the saved files; (6) backup and restore JSON; (7) close/reopen and verify data persists; (8) test navigation, privacy mode and notification permission; (9) update an existing install using a build signed by the same key and confirm records remain.

These are recommended tests, **not** claims that device testing was completed.

## Data and privacy

Transaction data is stored locally using WebView storage and native preferences. No cloud account is required. Exported files and JSON backups can contain sensitive financial information, so treat them securely. Storage guarantees depend on Android backup, user actions and device behavior; clearing app data or uninstalling the app can remove records. Keep separate backups before updating or switching phones.

The manifest requests notification permission and legacy storage write permission (Android 9 and below). See the manifest and code for precise behavior. The source is made available for inspection; an independent full security assessment has not been performed.

## Versioning and publishing

For a new release, update `VERSION_CODE` and `VERSION_NAME` in `build.sh` **and** `android:versionCode` and `android:versionName` in the manifest. Update [CHANGELOG.md](CHANGELOG.md), compile, verify, test, and publish a new release artifact. Keep private keystores out of Git and CI logs.

**Google Play:** The manual script currently outputs an **APK only**; there is no `.aab` build pipeline in this repository. Building a Play Store-ready Android App Bundle requires additional configuration/tooling. Do not assume `./build.sh` produces an AAB.

## Upload to GitHub

Create an **empty** repository named `HisaabKitaab` on GitHub (do not pre-add another README). From the extracted project root:

```bash
git init
git branch -M main
git add .
git status
git commit -m "Initial release: Hisaab Kitaab v6.0"
git remote add origin https://github.com/YOUR_USERNAME/HisaabKitaab.git
git push -u origin main
```

Replace `YOUR_USERNAME` with your actual GitHub username. Authenticate using GitHub's supported authentication method (not an account password in the command). Optional release steps: GitHub → repository → **Releases** → **Draft a new release** → tag `v6.0` → attach the supplied `release/HisaabKitaab-v6.0.apk`.

## Contributing and license

Open an issue with clear reproduction steps; do not share personal transaction records in issues. The project is distributed under [MIT](LICENSE). See [CHANGELOG.md](CHANGELOG.md) for release history.

---

<div align="center">Made by <strong>Muhammad Saad Jadoon</strong> · <strong>Hisaab Kitaab v6.0</strong></div>
