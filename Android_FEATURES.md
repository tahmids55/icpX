# Android Directory Features

## Top-Level Features
- Gradle-based Android project
- Cloud sync implementation (CLOUD_SYNC_IMPLEMENTATION.md)
- Build scripts and properties (build.gradle, gradle.properties, settings.gradle, gradlew, gradlew.bat)
- App module with source code and resources
- Documentation (README.md, README_FULL.md, BUILD_GUIDE.md, promt.txt)
- Shell script for running (run.sh)

## app/
- Android app module
- build.gradle for app-specific dependencies
- google-services.json for Firebase integration
- proguard-rules.pro for code obfuscation
- Source code and resources in `src/main`

### app/src/main
- AndroidManifest.xml: App manifest
- java/: Application source code (package: com.icpx.android)
- res/: Android resources (layouts, drawables, values, etc.)

### app/src/main/res
- anim/: Animation resources
- drawable/: Image and shape resources
- layout/: UI layout XML files
- menu/: Menu XMLs
- values/: Strings, colors, styles, etc.
- xml/: Miscellaneous XML resources

## build/
- Build outputs and reports

## gradle/
- Gradle wrapper for build consistency

---

**Key Features:**
- Android app with Firebase integration
- Modular code structure
- Resource-rich UI (layouts, drawables, animations)
- Gradle build system
- Cloud sync and documentation
