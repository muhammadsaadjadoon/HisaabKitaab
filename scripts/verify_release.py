#!/usr/bin/env python3
"""Source/archive verification without an Android SDK. Not a reproducible build test."""
from pathlib import Path
import hashlib
import sys
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
APK = ROOT / "release" / "HisaabKitaab-v6.0.apk"
HTML = ROOT / "app/src/main/assets/index.html"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
EXPECTED_SHA256 = "bc7368a6ca0657378988b9d31cedad518cd2c06a4d71b466b1c880fc86069bc7"


def check(label, ok, detail=""):
    print(f"[{'PASS' if ok else 'FAIL'}] {label}" + (f" — {detail}" if detail else ""))
    return bool(ok)


def main():
    if not APK.is_file() or not HTML.is_file() or not MANIFEST.is_file():
        print("[FAIL] Missing release APK, HTML asset or manifest")
        return 1
    ok = True
    sha = hashlib.sha256(APK.read_bytes()).hexdigest()
    ok &= check("Supplied APK SHA-256", sha == EXPECTED_SHA256, sha)
    root = ET.parse(MANIFEST).getroot()
    ns = "{http://schemas.android.com/apk/res/android}"
    ok &= check("Source package", root.attrib.get("package") == "com.saadjadoon.gharhisab")
    ok &= check("Source version", root.attrib.get(ns + "versionCode") == "6" and root.attrib.get(ns + "versionName") == "6.0")
    try:
        with zipfile.ZipFile(APK) as z:
            ok &= check("APK ZIP CRC and integrity", z.testzip() is None)
            names = set(z.namelist())
            ok &= check("APK contains AndroidManifest.xml and classes.dex", {"AndroidManifest.xml", "classes.dex"} <= names)
            ok &= check("Embedded UI exactly matches source asset", z.read("assets/index.html") == HTML.read_bytes())
            dex = z.read("classes.dex")
            ok &= check("DEX contains app class and native bridge references", b"MainActivity" in dex and b"AndroidBridge" in dex)
            ok &= check("APK includes signing metadata", any(n.startswith("META-INF/") and n.endswith(".RSA") for n in names))
    except (OSError, zipfile.BadZipFile, KeyError) as exc:
        print(f"[FAIL] Could not inspect APK: {exc}")
        return 1
    print("\nScope: STATIC consistency checks only; source rebuild and device tests NOT performed.")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
