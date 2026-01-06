# icpX - Android Version

An Android application for tracking competitive programming progress and managing training goals. This app helps competitive programmers monitor their practice, set targets, and track their improvement on platforms like Codeforces.

## 🎯 Features

### Core Features
- **User Authentication**: Secure login with BCrypt password hashing
- **Dashboard**: Overview of statistics and recent activity
- **Target Management**: Add, track, and manage programming problems
- **Auto-Fetch**: Automatically fetch problem details from Codeforces API
- **History Tracking**: View all solved problems with ratings
- **Statistics**: Track total problems, solved count, pending tasks, and average rating

### Technical Features
- Material Design UI with custom themes
- SQLite database for local storage
- RecyclerView with smooth animations
- Swipe-to-refresh functionality
- Navigation drawer for easy navigation
- Custom dialogs and fragments
- Responsive layouts for all screen sizes

## 📱 Screenshots

*Add your screenshots here*

## 🛠️ Technology Stack

- **Language**: Java
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM-like with DAOs
- **Database**: SQLite
- **UI Components**: Material Design Components
- **Networking**: OkHttp
- **JSON Parsing**: Gson
- **Password Hashing**: jBCrypt
- **Charts**: MPAndroidChart (for future features)
- **Animations**: Lottie, XML animations

## 📂 Project Structure

```
android_app/
├── app/
│   ├── src/main/
│   │   ├── java/com/icpx/android/
│   │   │   ├── adapters/           # RecyclerView adapters
│   │   │   ├── database/           # SQLite helpers and DAOs
│   │   │   ├── model/              # Data models
│   │   │   ├── service/            # API services
│   │   │   ├── ui/                 # Activities and fragments
│   │   │   │   ├── dialogs/        # Custom dialogs
│   │   │   │   └── fragments/      # Fragment components
│   │   │   └── util/               # Utility classes
│   │   └── res/
│   │       ├── anim/               # Animations
│   │       ├── drawable/           # Icons and backgrounds
│   │       ├── layout/             # XML layouts
│   │       ├── menu/               # Navigation menu
│   │       └── values/             # Strings, colors, styles
│   └── build.gradle
└── build.gradle
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 8 or higher
- Android SDK with minimum API level 24

### Installation

1. **Clone the repository**
   ```bash
   cd android_app
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `android_app` folder

3. **Sync Gradle**
   - Wait for Gradle to sync dependencies
   - Resolve any dependency issues if prompted

4. **Run the app**
   - Connect an Android device or start an emulator
   - Click the "Run" button in Android Studio

### Build Configuration

The app requires these dependencies (already in `build.gradle`):
```gradle
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'org.mindrot:jbcrypt:0.4'
implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
implementation 'com.airbnb.android:lottie:6.1.0'
```

## 📖 Usage

### First Time Setup
1. Launch the app
2. Create an account with username and password
3. Optionally add your Codeforces handle
4. Choose whether to enable startup password

### Adding Problems
1. Navigate to Targets section
2. Tap the floating action button (+)
3. Paste a Codeforces problem URL
4. App automatically fetches problem name and rating
5. Tap "Add Problem"

### Tracking Progress
- Mark problems as "Solved" or "Failed"
- View statistics on the dashboard
- Check history for all solved problems
- Monitor your average problem rating

## 🎨 Customization

### Themes
Edit colors in `res/values/colors.xml`:
- Primary color: `colorPrimary`
- Accent color: `colorAccent`
- Background: `backgroundColor`

### Animations
Modify animations in `res/anim/`:
- `slide_up_fade_in.xml`
- `scale_in.xml`
- `fade_in.xml`

### Styles
Customize UI in `res/values/styles.xml`:
- Button styles
- Card styles
- Text styles

## 🔐 Security

- Passwords are hashed using BCrypt
- SQLite database is stored locally on device
- No sensitive data transmitted over network
- API calls use HTTPS

## 🌟 Key Differences from Desktop Version

While maintaining the same core concept, the Android version features:
- Native Android Material Design UI
- Touch-optimized interactions
- Mobile-friendly navigation drawer
- Swipe gestures for refresh
- Optimized for portrait and landscape modes
- Background threading for smooth performance
- Android-specific animations and transitions

## 🔮 Future Enhancements

- [ ] Charts and graphs for progress visualization
- [ ] Virtual contest mode
- [ ] Problem recommendations
- [ ] Multi-platform support (AtCoder, LeetCode)
- [ ] Dark theme support
- [ ] Backup and restore
- [ ] Cloud sync
- [ ] Notifications for contest reminders

## 📝 API Integration

The app integrates with Codeforces API:
- **Endpoint**: `https://codeforces.com/api/`
- **Methods Used**:
  - `problemset.problems` - Fetch problem details
  - `user.info` - Get user information

## 🐛 Known Issues

- API rate limiting may affect auto-fetch
- Network errors are not fully handled
- Some edge cases in problem URL parsing

## 📄 License

This project is created for educational purposes.

## 👨‍💻 Author

Adapted from the JavaFX desktop version of icpX

## 🙏 Acknowledgments

- Codeforces for providing the API
- Material Design Components team
- Open source library contributors

---

**Note**: This is an Android adaptation of the icpX desktop application, maintaining the same concept but redesigned for mobile platforms with native Android features and Material Design guidelines.
