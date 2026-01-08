# Firebase Testing Guide - icpX Android

## ✅ Setup Complete!

Firebase is now fully integrated and ready to test. You have 3 sync buttons in Settings:

1. **⬆️ Upload to Cloud** - Send local data to Firebase
2. **⬇️ Download from Cloud** - Get data from Firebase  
3. **🔄 Full Sync** - Bidirectional sync

---

## 🧪 How to Test Firebase Integration

### Step 1: Install the App
```bash
./run.sh
```

### Step 2: Create Some Test Data
1. Open the app
2. Go to **Targets** section
3. Add 2-3 problems or topics
4. Mark some as "achieved"

### Step 3: Sync to Firebase
1. Go to **Settings** (from navigation drawer)
2. Scroll down to **Cloud Sync** section
3. Click **⬆️ Upload to Cloud**
4. Wait for success message

### Step 4: Verify in Firebase Console
1. Go to: https://console.firebase.google.com/project/icpx-efa50
2. Click **Firestore Database** in left menu
3. You should see:
   ```
   users/
     └── {some-user-id}/
         ├── username: "..."
         ├── codeforcesHandle: "..."
         └── targets/
             ├── 1/
             │   ├── name: "Problem Name"
             │   ├── rating: 1200
             │   └── status: "achieved"
             └── 2/
                 └── ...
   ```

### Step 5: Test Download (Optional)
1. Clear app data: Settings → Apps → icpX → Clear Data
2. Reinstall or just restart app
3. Login again
4. Go to Settings → **⬇️ Download from Cloud**
5. Your targets should reappear!

---

## 🔍 How to Check If It's Working

### Method 1: Check Logcat
```bash
adb logcat | grep -i firebase
```

Look for:
- ✅ `Firebase initialization successful`
- ✅ `Firestore document written`
- ❌ `FirebaseException` (if there's an error)

### Method 2: Firebase Console
Visit: https://console.firebase.google.com/project/icpx-efa50/firestore

You should see data appear after clicking Upload.

### Method 3: Check Toast Messages
The app shows:
- ✓ "Synced X targets" on success
- ✗ "Sync failed: ..." on error

---

## 🐛 Troubleshooting

### "User not authenticated" Error
**What it means:** Firebase Auth hasn't completed  
**Fix:** The app auto-signs in with test credentials. Wait 2-3 seconds and try again.

### "Permission denied" Error
**What it means:** Firestore rules block access  
**Fix:** Go to Firebase Console → Firestore → Rules, paste:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### "No internet connection" Error
**What it means:** Device is offline  
**Fix:** Connect to WiFi or mobile data

### Nothing appears in Firebase Console
**Check:**
1. Click "Upload to Cloud" button (not just Full Sync)
2. Wait for "Synced X targets" message
3. Refresh Firebase Console page
4. Make sure you're in correct project: icpx-efa50

---

## 📊 What Gets Synced

**Uploaded to Firebase:**
- All targets (problems & topics)
- Target status (pending/achieved)
- Problem ratings
- Creation dates
- User's Codeforces handle

**NOT synced:**
- Login password (local only for security)
- App settings (notifications, etc.)

---

## 🔐 Security Note

Current implementation uses a test account (`test@icpx.com`) for all users. This is fine for testing but not production.

For production, you'd want:
- Each user has their own Firebase account
- Proper authentication flow
- Secure Firestore rules

---

## ⚡ Quick Test Command

After installing app and adding data:

1. Open Settings → Cloud Sync
2. Click **⬆️ Upload to Cloud**
3. Open browser: https://console.firebase.google.com/project/icpx-efa50/firestore
4. Look for `users/{userId}/targets/` - your data should be there!

That's it! 🎉
