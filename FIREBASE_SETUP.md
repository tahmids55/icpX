# Firebase Integration - icpX Android App

## ✅ Setup Complete

### What's Been Configured:
1. **Firebase Dependencies** - Added to Gradle files
2. **google-services.json** - Configured and placed in `app/` directory
3. **Package Name** - Updated to `com.icpx.android`
4. **Firebase Services** - Auth, Firestore, Realtime Database enabled

---

## 📦 Files Created

### 1. **FirebaseManager.java**
Location: `app/src/main/java/com/icpx/android/firebase/FirebaseManager.java`

**Features:**
- User authentication (sign in, sign up, sign out)
- Firestore CRUD operations
- Singleton pattern for easy access

**Usage Example:**
```java
FirebaseManager firebaseManager = FirebaseManager.getInstance();

// Sign in
firebaseManager.signIn(email, password, new FirebaseManager.AuthCallback() {
    @Override
    public void onSuccess(FirebaseUser user) {
        // User signed in successfully
    }
    
    @Override
    public void onFailure(Exception e) {
        // Handle error
    }
});

// Save data
Map<String, Object> data = new HashMap<>();
data.put("field", "value");
firebaseManager.saveTarget(userId, data, callback);
```

### 2. **FirebaseSyncService.java**
Location: `app/src/main/java/com/icpx/android/firebase/FirebaseSyncService.java`

**Features:**
- Sync SQLite ↔ Firebase
- Bidirectional data synchronization
- Conflict resolution

**Usage Example:**
```java
FirebaseSyncService syncService = new FirebaseSyncService(context);

// Sync local data to cloud
syncService.syncTargetsToFirebase(new FirebaseSyncService.SyncCallback() {
    @Override
    public void onSuccess(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onFailure(Exception e) {
        Toast.makeText(context, "Sync failed: " + e.getMessage(), 
                      Toast.LENGTH_SHORT).show();
    }
});

// Full bidirectional sync
syncService.performFullSync(callback);
```

---

## 🚀 How to Use Firebase in Your App

### Option 1: Manual Sync (Recommended to Start)
Add a sync button in your settings:

```java
// In SettingsFragment.java
FirebaseSyncService syncService = new FirebaseSyncService(requireContext());
syncButton.setOnClickListener(v -> {
    syncService.performFullSync(new FirebaseSyncService.SyncCallback() {
        @Override
        public void onSuccess(String message) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void onFailure(Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), 
                          Toast.LENGTH_SHORT).show();
        }
    });
});
```

### Option 2: Auto Sync on Login
Add to your LoginActivity/Fragment:

```java
FirebaseManager.getInstance().signIn(email, password, new FirebaseManager.AuthCallback() {
    @Override
    public void onSuccess(FirebaseUser user) {
        // Sync data after successful login
        new FirebaseSyncService(LoginActivity.this).syncTargetsFromFirebase(callback);
        // Navigate to main activity
    }
    
    @Override
    public void onFailure(Exception e) {
        // Handle login error
    }
});
```

### Option 3: Real-time Sync
Add listeners to sync on every change:

```java
// After creating/updating a target
targetDAO.createTarget(target);

// Immediately sync to Firebase
if (FirebaseManager.getInstance().getCurrentUser() != null) {
    Map<String, Object> targetData = /* convert target to map */;
    FirebaseManager.getInstance().saveTarget(userId, targetData, callback);
}
```

---

## 📊 Firebase Structure

Your data will be organized in Firestore like this:

```
users/
  └── {userId}/
      ├── username: "john_doe"
      ├── codeforcesHandle: "johnd"
      └── targets/
          ├── {targetId}/
          │   ├── name: "Problem 1A"
          │   ├── type: "problem"
          │   ├── status: "achieved"
          │   ├── rating: 1200
          │   └── ...
          └── {targetId}/
              └── ...
```

---

## 🔐 Authentication Flow

Current app uses local SQLite auth. To integrate Firebase Auth:

1. **Keep Local Auth** (easier, recommended)
   - Users log in with local credentials
   - Automatically sign into Firebase anonymously or with a fixed account
   - Data syncs in background

2. **Replace with Firebase Auth** (more secure)
   - Replace LoginActivity with Firebase auth
   - Users sign up/login with email/password through Firebase
   - Data automatically associated with their Firebase account

---

## ⚙️ Next Steps

1. **Add Sync Button to Settings**
   - Let users manually trigger sync
   - Show sync status/progress

2. **Test Firebase Console**
   - Go to: https://console.firebase.google.com
   - Select project: icpx-30c92
   - Check Firestore Database to see synced data

3. **Enable Offline Persistence** (optional)
```java
// In your Application class or MainActivity onCreate
FirebaseFirestore.getInstance()
    .getFirestoreSettings()
    .setPersistenceEnabled(true);
```

4. **Add Network Detection**
   - Only sync when connected to internet
   - Queue changes for later sync when offline

---

## 🧪 Testing

1. Build and install: `./run.sh`
2. Create some targets in the app
3. Call sync method
4. Check Firebase Console → Firestore Database
5. Delete app data and reinstall
6. Sync from Firebase - your data should reappear!

---

## 📝 Notes

- Firebase is configured but NOT actively used yet
- You need to add UI buttons/triggers to call sync methods
- Data currently stays in SQLite (as before)
- Firebase sync is opt-in when you implement the calls
- No breaking changes to existing functionality
