# Desktop Google Sign-In Setup Guide

## Why Google Was Blocking You

Your previous implementation used a **Web Client ID** without a **client secret**. For Desktop applications, Google requires:
1. A **Desktop/Native OAuth Client** (not Web or Android)
2. Both **Client ID** and **Client Secret**

---

## Step-by-Step Setup

### Step 1: Create Desktop OAuth Credentials

1. Go to **Google Cloud Console**: https://console.cloud.google.com/apis/credentials?project=icpx-efa50

2. Click **+ CREATE CREDENTIALS** → **OAuth client ID**

3. Select Application type: **Desktop app**

4. Name it: `icpX Desktop` (or any name you prefer)

5. Click **Create**

6. **IMPORTANT**: You'll see a popup with:
   - Client ID: `xxx.apps.googleusercontent.com`
   - Client secret: `GOCSPX-xxxxx`
   
7. Click **Download JSON** button

### Step 2: Configure Your Desktop App

1. Open the downloaded JSON file. It should look like:
   ```json
   {
     "installed": {
       "client_id": "924198752370-xxxxxxxx.apps.googleusercontent.com",
       "project_id": "icpx-efa50",
       "auth_uri": "https://accounts.google.com/o/oauth2/auth",
       "token_uri": "https://oauth2.googleapis.com/token",
       "client_secret": "GOCSPX-xxxxxxxx",
       ...
     }
   }
   ```

2. Copy the entire content and replace in:
   ```
   Desktop/src/main/resources/desktop_oauth_credentials.json
   ```

   **OR** simply move/rename the downloaded file to:
   ```
   Desktop/src/main/resources/desktop_oauth_credentials.json
   ```

### Step 3: Configure OAuth Consent Screen (If Not Done)

1. Go to: https://console.cloud.google.com/apis/credentials/consent?project=icpx-efa50

2. If in **Testing** mode:
   - Add your email to **Test users**
   - This is fine for personal use

3. For production, you'll need to verify the app

### Step 4: Add Authorized Redirect URI

1. Go back to **Credentials**: https://console.cloud.google.com/apis/credentials?project=icpx-efa50

2. Click on your newly created **Desktop OAuth client**

3. Under **Authorized redirect URIs**, add:
   ```
   http://localhost:8888/Callback
   ```

4. Click **Save**

---

## Testing the Sign-In

1. Build and run your Desktop app:
   ```bash
   cd Desktop
   mvn clean javafx:run
   ```

2. Click "Sign in with Google"

3. Your browser will open with Google's sign-in page

4. Select your Google account

5. You'll be redirected back to the app (browser will show "Received verification code")

6. The app should now show you as signed in!

---

## Troubleshooting

### Error: "Access blocked: This app's request is invalid"
- Make sure you created a **Desktop app** type, not Web or Android
- Ensure you have both client_id AND client_secret in the JSON

### Error: "redirect_uri_mismatch"
- Add `http://localhost:8888/Callback` to Authorized redirect URIs
- Wait a few minutes for Google to propagate changes

### Error: "This app is blocked"
- Go to OAuth consent screen
- Add your email to Test users (if in Testing mode)
- Make sure the app is not in "Needs verification" state

### Error: "File not found: desktop_oauth_credentials.json"
- Ensure the file is in `src/main/resources/` folder
- Check the JSON format is correct (starts with `"installed"`)

---

## Security Notes

⚠️ **Keep your `desktop_oauth_credentials.json` private!**
- Add it to `.gitignore`
- Never commit client secrets to public repositories

Add to your `.gitignore`:
```
desktop_oauth_credentials.json
```

---

## How It Works Now

1. **First Sign-In**: Opens browser → User signs in → Tokens stored locally
2. **Subsequent Uses**: Uses stored tokens (no browser needed unless expired)
3. **Sign Out**: Clears all stored tokens

Tokens are stored in: `~/.icpx/tokens/`
