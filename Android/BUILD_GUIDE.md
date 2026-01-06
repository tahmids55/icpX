# icpX Android App

## Project Overview
This is an Android application for tracking Codeforces problems and targets.

## Recent Fixes Applied

1. **Fixed Repository Configuration**
   - Added JitPack repository to settings.gradle for MPAndroidChart dependency
   - Updated dependency resolution management

2. **Fixed Resource Errors**
   - Fixed `drawablePadding` attribute in styles.xml (changed to `android:drawablePadding`)
   - Updated launcher icon references from mipmap to drawable
   - Created ic_launcher.xml in drawable folder
   - Fixed splash_background.xml to use drawable instead of mipmap

3. **Fixed AndroidManifest**
   - Removed non-existent activity declarations (TargetActivity, HistoryActivity, SettingsActivity, TopicHistoryActivity)
   - App uses fragments instead of separate activities for these features

4. **Build Configuration**
   - Added gradle properties to suppress SDK warnings
   - Configured build types properly

## Building the Project

### Using Android Studio (Recommended)
1. Open the project in Android Studio
2. Let Android Studio download and sync dependencies
3. Click Run or Build > Make Project
4. Select a device/emulator and run the app

### Using Command Line
```bash
# Clean and build debug APK
./gradlew clean assembleDebug

# Install on connected device
./gradlew installDebug

# Build and run
./gradlew clean build
```

Note: There may be JVM compatibility issues with the current Gradle daemon. If you encounter crashes, try:
- Using Android Studio which handles the build process better
- Using a different Java version (Java 17 is recommended for Android development)
- Adding `--no-daemon` flag to gradle commands

## Running the App

### On Physical Device
1. Enable Developer Options and USB Debugging on your Android device
2. Connect via USB
3. Run: `./gradlew installDebug` or use Android Studio's Run button

### On Emulator
1. Create an Android Virtual Device (AVD) in Android Studio
2. Start the emulator
3. Run the app from Android Studio or use `./gradlew installDebug`

## App Structure
- **UI Activities**: SplashActivity, SetupActivity, LoginActivity, MainActivity
- **Fragments**: DashboardFragment, TargetsFragment, HistoryFragment, SettingsFragment
- **Services**: CodeforcesService for API integration
- **Database**: SQLite with DAO pattern (UserDAO, TargetDAO)
- **Models**: User, Target, StatCard

## Dependencies
- AndroidX libraries
- Material Components
- Gson for JSON parsing
- OkHttp for networking
- JBcrypt for password hashing
- MPAndroidChart for charts
- Lottie for animations

## Known Issues
- The Gradle daemon may crash during build on some systems due to JVM compatibility
- Solution: Use Android Studio or configure JAVA_HOME to point to Java 17

## APK Location
After successful build, find the APK at:
```
app/build/outputs/apk/debug/app-debug.apk
```

You can install this APK directly on an Android device.

