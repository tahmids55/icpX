
# icpX Android App

## Overview

icpX is a comprehensive Android application designed for competitive programmers to manage, track, and analyze their problem-solving progress, with deep integration for Codeforces and cloud sync via Firebase. The app provides robust features for user authentication, problem/target management, statistics, cloud sync, and more, all wrapped in a modern Material Design UI.

---

## 🎯 App Purpose & Main Features

- **User Authentication**: Secure login and account management using BCrypt password hashing and Firebase Auth (email/password & Google Sign-In).
- **Target/Problem Management**: Add, edit, delete, and solve programming problems ("targets"). Problems can be fetched automatically from Codeforces API by pasting a URL.
- **History & Statistics**: Track all solved problems, view history, ratings, heatmap, and progress charts. Dashboard summarizes total solves, pending tasks, and average rating.
- **Cloud Sync**: Manual and automatic sync of user data (problems, status, ratings, handle) between local SQLite and Firebase Firestore. Google Sign-In supported for account linking.
- **UI/UX**: Material Design with custom themes, navigation drawer, fragments, activities, responsive layouts, notifications, and smooth animations.
- **Database**: Local SQLite database for offline-first experience. Tables for users, targets/problems, history, and statistics.
- **Background Services**: Uses WorkManager for scheduled syncs and reminders. Handles contest tracking and notifications.
- **Codeforces API Integration**: Fetches problem details and verifies user handles using official Codeforces API endpoints.
- **Error Handling**: Graceful handling of network errors, API rate limits, sync conflicts, and edge cases in problem parsing.
- **Other Features**: Reminders for contests, offline WebView for cached problem statements, backup/restore, and more.

---

## 🧑‍💻 User Authentication & Account Management

- **Account Creation**: Users sign up with email/password (min 6 chars) and optional Codeforces handle. Firebase Auth creates account and stores user data in Firestore.
- **Login**: Email/password authentication via Firebase. Google Sign-In supported (OAuth2 flow).
- **Local Auth**: BCrypt password hashing for local login. Optionally, startup password can be enabled for extra security.
- **Account Data**: Firestore stores email, handle, creation date, and targets. Each user has a unique Firebase UID.

---

## 📚 Problem/Target Management

- **Add Problem**: Paste Codeforces URL, app fetches name/rating via API. Manual entry also supported.
- **Edit/Delete**: Problems can be edited or removed from the list.
- **Solve/History**: Mark problems as solved/failed. History view shows all attempts, ratings, and timestamps.
- **Sync**: Problems and their status are synced to Firestore under `users/{userId}/targets/{targetId}`.

---

## 📊 Statistics & Visualization

- **Dashboard**: Shows total problems, solved count, pending, average rating.
- **Charts**: MPAndroidChart used for progress graphs and heatmaps.
- **History**: Full history of solved problems, sortable by date/rating.
- **Ratings**: Tracks problem ratings, user performance over time.

---

## ☁️ Cloud Sync (Firebase Firestore & Google Sign-In)

- **Manual Sync**: User can trigger upload/download from cloud in Settings.
- **Auto Sync**: Optionally syncs on login or after changes.
- **Bidirectional**: Syncs both local → cloud and cloud → local, with conflict resolution.
   - All targets/problems
   - Status (pending/achieved)
   - Ratings
   - Creation dates
- **Firebase Structure**:
   ```
            └── targets/
                  ├── {targetId}/
- **Google Sign-In**: OAuth2 flow, links Firebase account, enables cloud sync.

## 🖌️ UI/UX Design

- **Navigation**: Navigation drawer, fragments for dashboard, targets, settings, history, etc.
- **Themes**: Light/dark theme support, custom color palette.
- **Notifications**: Contest reminders, sync status, error alerts.
   - `users`: id, email, codeforcesHandle, passwordHash, createdAt
   - `targets`: id, name, type, status, rating, createdAt, userId
- **Relationships**:
   - One user → many targets
   - One target → many history entries
---

- **FirebaseSyncService**: Handles bidirectional sync, conflict resolution, error handling.
- **Reminders**: Contest tracking, custom reminders via Settings.
## 🌐 Codeforces API Integration

   - Auto-fetch problem name/rating
   - Verify Codeforces handle
---
## ⚠️ Error Handling & Edge Cases

- **Network Errors**: Graceful fallback, retry logic, offline cache
- **API Rate Limiting**: Notifies user, backs off requests
- **Sync Conflicts**: Resolves using timestamps, user choice
- **Invalid Data**: Validates URLs, handles missing fields
- **Security**: Passwords never synced, Firestore rules enforced

---

## 🛠️ Other Features

- **Reminders**: Customizable contest reminders
- **Contest Tracking**: Tracks upcoming contests, notifies user
- **Offline WebView**: Caches problem statements for offline viewing
- **Backup/Restore**: Planned for future releases

---

## ⚙️ Build & Run Instructions

### Prerequisites
- Android Studio (latest recommended)
- JDK 11+
- Firebase project setup (see FIREBASE_SETUP.md & GOOGLE_SIGNIN_SETUP.md)

### Build Steps
2. Open in Android Studio
3. Ensure `google-services.json` is present in `app/`
5. Configure Firebase (see setup guides)

### Dependencies
- Firebase Auth, Firestore, Realtime Database
- Google Sign-In
- MPAndroidChart
- WorkManager
---

- `google-services.json` - Firebase config

---

## 📝 Notes
- Firebase integration is modular and can be extended
- Security: passwords are local-only, Firestore rules recommended
- See FIREBASE_TESTING.md for testing cloud sync

---

## 📄 License
This project is created for educational purposes.

## 👨‍💻 Author
Adapted from the JavaFX desktop version of icpX

---

## 🔮 Future Enhancements
- Charts/graphs for progress
- Virtual contest mode
- Problem recommendations
- Multi-platform support (AtCoder, LeetCode)
- Backup/restore
- Notifications
- Dark theme

---

## References
- [FIREBASE_SETUP.md](../FIREBASE_SETUP.md)
- [GOOGLE_SIGNIN_SETUP.md](../GOOGLE_SIGNIN_SETUP.md)
- [FIREBASE_TESTING.md](../FIREBASE_TESTING.md)

---

For any questions or contributions, please open an issue or pull request.
