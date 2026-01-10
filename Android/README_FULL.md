# icpX Android Project (Full Documentation)

## Overview
icpX is a comprehensive Android application for competitive programming practice, tracking, and statistics. It integrates local and cloud storage, Codeforces API, and advanced UI/UX features to help users manage problems, track progress, and sync data across devices.

---

## Features

### 1. User Authentication & Account Management
- Google Sign-In integration for secure authentication.
- Local user preferences and session management.
- Option to require login on startup.
- Password management (future feature).

### 2. Problem/Target Management
- Add, edit, delete, and soft-delete problems and topics.
- Each problem (target) includes: ID, name, type (problem/topic), problem link, topic name, website URL, status (pending/achieved/failed), rating, description, creation date, and deleted flag.
- Duplicate prevention by problem link.
- Problem details auto-fetched from Codeforces API.
- Problem verification via Codeforces handle.

### 3. History & Statistics
- History: List of all solved problems (with problem ID and link), stored locally and in Firebase as a subcollection.
- All-time solve: Count of unique solved problems.
- All-time history: List of all solved problems (problem ID and link).
- Daily solved count, unique solved count, max Codeforces rating, average rating, heatmap/activity by date.
- Contest reminders and tracking.

### 4. Cloud Sync (Firebase Firestore)
- Manual and automatic sync of problems, history, and stats.
- Data stored per user in Firestore:
  - `users/{userId}/targets/{targetId}`: All problems/targets.
  - `users/{userId}/history/{historyId}`: All solved problems (history).
  - `users/{userId}`: Global stats (all_time_solve, codeforcesHandle, etc.).
- Sync logic ensures no duplicates and merges local/cloud data.
- Google Sign-In required for cloud sync.

### 5. UI/UX & Navigation
- Material Design with custom color themes.
- Fragments for settings, history, statistics, and problem management.
- RecyclerViews for lists, swipe-to-refresh, clickable links.
- Notifications for sync, reminders, and contest events.
- Button color and text size customization.
- Error and status messages via Toasts and notifications.

### 6. Database Schema & Local Storage
- SQLite database with three main tables:
  - `users`: User info, handle, password, creation date.
  - `targets`: Problems/topics with all metadata.
  - `settings`: App-wide stats (all_time_solve, etc.).
- Soft delete for targets (deleted flag).
- Efficient queries for history, stats, and activity.

### 7. Background Services & Workers
- WorkManager for scheduled auto-sync and contest reminders.
- Periodic sync every 24 hours (if enabled).
- Background threads for database and network operations.

### 8. Codeforces API Integration
- Fetch problem details (name, rating) from Codeforces URLs.
- Verify problem solved status via Codeforces handle.
- Fetch user info (max rating) for statistics.

### 9. Error Handling & Edge Cases
- Null safety and adapter position checks in UI.
- Duplicate prevention in problem creation.
- Sync error handling and notifications.
- Edge case handling for login, network, and database failures.

### 10. Additional Features
- Contest reminders with customizable time (24/48 hours).
- Topic management and learning tracking.
- Heatmap/activity visualization (by date).
- Password protection (future feature).

---

## Build & Run Instructions

1. **Requirements:**
   - Android Studio (latest recommended)
   - JDK 8+ (Java 8 source/target, but newer JDKs supported)
   - Firebase project with Firestore enabled
   - Google Services JSON (`app/google-services.json`)

2. **Setup:**
   - Clone the repository.
   - Place your `google-services.json` in the `app/` directory.
   - Open in Android Studio.
   - Sync Gradle and build the project.

3. **Run:**
   - Connect an Android device or start an emulator.
   - Run `./run.sh` or use Android Studio's run button.
   - Sign in with Google to enable cloud sync.

4. **Dependencies:**
   - Firebase Auth & Firestore
   - Google Sign-In
   - AndroidX, Material Components
   - WorkManager
   - OkHttp, JSON libraries

---

## File/Folder Structure
- `app/src/main/java/com/icpx/android/`:
  - `ui/fragments/`: All UI fragments (Settings, History, Stats, etc.)
  - `model/`: Data models (Target, User, etc.)
  - `database/`: SQLite helpers and DAOs
  - `firebase/`: Firebase sync and manager classes
  - `service/`: Codeforces API integration
  - `worker/`: Background workers
- `app/src/main/res/`: Layouts, colors, drawables
- `build.gradle`, `settings.gradle`: Project configuration
- `google-services.json`: Firebase config

---

## Extending the Project
- Add new problem sources by extending `TargetDAO` and sync logic.
- Add new statistics or visualizations in `StatsAllTimeFragment`.
- Integrate additional contest platforms via new service classes.
- Customize UI themes in `res/values/colors.xml` and layouts.

---

## Contact & Contribution
For questions, issues, or contributions, please open an issue or pull request on the repository.

---

## License
This project is licensed for educational and personal use. See LICENSE file for details.
