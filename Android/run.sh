#!/bin/bash

# icpX Android App - Build and Run Script
# This script builds, installs, and launches the app on a connected Android device

echo "🔍 Checking for connected devices..."
DEVICES=$(adb devices | grep -w "device" | wc -l)

if [ $DEVICES -eq 0 ]; then
    echo "❌ No Android device connected!"
    echo "Please connect your device via USB and enable USB debugging."
    exit 1
fi

echo "✅ Device found!"
echo ""

echo "🔨 Building and installing app..."
./gradlew installDebug

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo ""
echo "🚀 Launching app..."
adb shell am start -n com.icpx.android/.ui.SplashActivity

echo ""
echo "✅ App launched successfully!"
