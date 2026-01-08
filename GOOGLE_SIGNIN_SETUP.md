# Firebase Authentication Setup - icpX Android

## ✅ Email/Password Auth Integrated!

Users now sign up with **email and password** when they first open the app. Their data is automatically saved to Firebase under their unique user ID.

---

## 📱 How It Works Now

### New User Flow:
1. **First Launch** → Setup screen appears
2. **User enters:**
   - Email address
   - Password (min 6 characters)
   - Codeforces handle (optional)
3. **Clicks "Create Account"**
4. ✓ Firebase account created
5. ✓ User data saved to Firestore: `users/{userId}/`
6. ✓ Auto signed-in
7. → Goes to Dashboard

### Returning User Flow:
1. **App Launch** → Login screen
2. **User enters email + password**
3. ✓ Firebase authenticates
4. → Goes to Dashboard with their data

### Data Storage:
- **Each user has unique Firebase UID**
- **Firestore structure:**
  ```
  users/
    └── {firebaseUserId}/
        ├── email: "user@example.com"
        ├── codeforcesHandle: "user_handle"
        ├── createdAt: 1704567890
        └── targets/
            ├── 1/
            │   ├── name: "Problem A"
            │   ├── rating: 1200
            │   └── status: "achieved"
            └── 2/
                └── ...
  ```

---

## 🔧 Firebase Console Setup (Required)

### Step 1: Enable Email/Password Authentication

1. Go to **Firebase Console**: https://console.firebase.google.com/project/icpx-efa50
2. Click **Authentication** in left sidebar
3. Click **Get Started** (if first time) or **Sign-in method** tab
4. Click **Email/Password** provider
5. Click **Enable** toggle for "Email/Password"
6. Click **Save**

### Step 2: (Optional) Enable Google Sign-In

For users who want to sign in with Google as well:

1. Still in **Authentication → Sign-in method**
2. Click **Google** provider
3. Click **Enable** toggle
4. Select **Project support email** from dropdown
5. Click **Save**
6. Copy the **Web client ID** 
7. Open `app/src/main/res/values/strings.xml`
8. Replace:
   ```xml
   <string name="default_web_client_id">YOUR_ACTUAL_WEB_CLIENT_ID_HERE</string>
   ```

### Step 3: Add SHA-1 Fingerprint (For Google Sign-In Only)

Google Sign-In requires your app's SHA-1 certificate:

#### Option A: Debug Certificate (For Testing)
```bash
cd ~/
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Look for **SHA1** line, copy the fingerprint (like: `AA:BB:CC:...`)

#### Option B: Quick Command
```bash
keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android | grep SHA1
```

#### Add to Firebase:
1. Go to **Project Settings** (⚙️ icon) in Firebase Console
2. Scroll to **Your apps** section
3. Find your Android app (`com.icpx.android`)
4. Click **Add fingerprint**
5. Paste the SHA-1
6. Click **Save**

### Step 4: Download Updated google-services.json

After adding SHA-1:
1. Still in **Project Settings → Your apps**
2. Scroll down, click **Download google-services.json**
3. Replace the file in `app/google-services.json`

---

## 📱 How It Works

### User Experience:

1. **First Time:**
   - User goes to Settings
   - Clicks "Upload to Cloud" or any sync button
   - Sees dialog: "Sign in Required"
   - Clicks "Sign in with Google"
   - Google account picker appears
   - Selects account
   - ✓ Signed in! Data syncs to their account

2. **After Sign-in:**
   - Data automatically syncs to **their** Firebase account
   - Each user has separate data
   - Can access from any device with same Google account

3. **Shown in Settings:**
   - "Signed in as: user@gmail.com"
   - Last sync time
   - Sync buttons work without re-authentication

---

## 🧪 Testing

### Quick Test:
```bash
./run.sh
```

Then in the app:
1. Open **Settings** from menu
2. Scroll to **Cloud Sync**
3. Click **⬆️ Upload to Cloud**
4. Dialog appears: "Sign in Required"
5. Click **Sign in with Google**
6. Select your Google account
7. Should see: "✓ Signed in as your@email.com"
8. Click sync button again - uploads to Firebase!

### Verify in Firebase:
1. https://console.firebase.google.com/project/icpx-efa50/firestore
2. Click **users** collection
3. Find document with your Google user ID
4. Should see your targets inside!

---

## 🔐 Security Benefits

**Before (Test Account):**
- Everyone used `test@icpx.com`
- All data mixed together
- No privacy

**After (Google Sign-In):**
- Each user has unique Google account
- Data separated by user ID
- Firestore rules can enforce: users only see their own data
- Can revoke access anytime

---

## 📝 Firestore Security Rules

Update rules for better security:

1. Go to **Firestore Database → Rules** tab
2. Paste this:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only read/write their own data
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```
3. Click **Publish**

Now users can ONLY access their own data!

---

## 🚀 Quick Start Checklist

- [ ] Enable Google Sign-In in Firebase Console
- [ ] Copy Web Client ID to `strings.xml`
- [ ] Get SHA-1 fingerprint
- [ ] Add SHA-1 to Firebase Project Settings
- [ ] Download new `google-services.json`
- [ ] Build and test: `./run.sh`
- [ ] Try signing in with Google
- [ ] Upload data and verify in Firestore

---

## ❓ Troubleshooting

### "12500: Sign in failed"
- **Cause:** SHA-1 fingerprint not added
- **Fix:** Follow Step 3 above

### "Web client ID not found"  
- **Cause:** Wrong client ID in strings.xml
- **Fix:** Copy actual Web Client ID from Firebase Console

### "No account picker appears"
- **Cause:** Google Play Services not updated
- **Fix:** Update Google Play Services on device

### "Permission denied in Firestore"
- **Cause:** Security rules too strict or not signed in
- **Fix:** Check Firebase Auth shows user, update rules

---

Once you complete the Firebase Console setup, the app will be ready to use Google Sign-In! 🎉
