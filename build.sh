#!/bin/bash
# Build script for NexusMsg Android APK (arm64-v8a only)
# Requires: Android SDK, JDK 17

set -e

echo "============================================"
echo "  NexusMsg APK Builder (arm64-v8a only)"
echo "============================================"

# Check for ANDROID_HOME
if [ -z "$ANDROID_HOME" ]; then
    echo "ERROR: ANDROID_HOME not set. Please set it to your Android SDK path."
    echo "  Example: export ANDROID_HOME=$HOME/Android/Sdk"
    exit 1
fi

echo ""
echo "Building debug APK..."
./gradlew assembleDebug

echo ""
echo "Building release APK..."
./gradlew assembleRelease

echo ""
echo "============================================"
echo "  Build Complete!"
echo "============================================"
echo ""
echo "APK files:"
echo "  Debug:   app/build/outputs/apk/debug/app-arm64-v8a-debug.apk"
echo "  Release: app/build/outputs/apk/release/app-arm64-v8a-release.apk"
echo ""
echo "To install on device:"
echo "  adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk"
