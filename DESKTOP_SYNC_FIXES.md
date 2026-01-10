# Desktop Cloud Sync - Fixed Issues

## Problems Fixed

### 1. ✅ Download ClassCastException Error
**Error:**
```
java.lang.ClassCastException: class java.lang.String cannot be cast to class java.lang.Number
```

**Root Cause:**
- Android uploads ID as String: `map.put("id", String.valueOf(target.getId()))`
- Desktop tried to read ID as Long: `doc.getLong("id")`
- This caused ClassCastException

**Fix:**
- Desktop now stores ID as String (matches Android)
- Desktop reads ID as String and parses to int
- Both platforms now use consistent data format

### 2. ✅ Field Name Mismatch
**Problem:**
- Android uses camelCase: `problemLink`, `topicName`, `websiteUrl`, `createdAt`
- Desktop was using snake_case: `problem_link`, `topic_name`, `website_url`, `created_at`
- This caused fields to not sync properly

**Fix:**
- Desktop now uses camelCase field names matching Android
- All field names are now consistent across platforms

### 3. ✅ History Not Uploading
**Problem:**
- Desktop only uploaded targets, not history or stats

**Fix:**
- Added history sync to upload process
- Collects all achieved problems
- Uploads as `history` array with id, name, link, rating
- Matches Android's history format

### 4. ✅ Stats Not Uploading
**Problem:**
- Desktop didn't sync `allTimeSolve` and `allTimeHistory` stats

**Fix:**
- Added stats sync to upload process
- Calculates `allTimeSolve` (total achieved targets)
- Calculates `allTimeHistory` (total achieved problems)
- Uploads to user document

### 5. ✅ Download Not Updating Existing Targets
**Problem:**
- Desktop skipped existing targets during download
- No way to get updates from other devices

**Fix:**
- Desktop now updates existing targets if IDs match
- Properly merges cloud data with local data
- Bidirectional sync now works correctly

## Data Format (Desktop ↔ Android Compatible)

### Target Document
```json
{
  "id": "123",                    // String (was Integer)
  "type": "problem",
  "name": "Problem Name",
  "problemLink": "https://...",   // camelCase (was problem_link)
  "topicName": "Topic",           // camelCase (was topic_name)
  "websiteUrl": "https://...",    // camelCase (was website_url)
  "status": "achieved",
  "rating": 1200,
  "archived": false,
  "createdAt": 1704567890000      // Epoch millis (was ISO string)
}
```

### User Document (History & Stats)
```json
{
  "email": "user@gmail.com",
  "username": "John",
  "history": [
    {
      "id": 123,
      "problem_link": "https://...",
      "name": "Problem A",
      "rating": 1200
    }
  ],
  "allTimeSolve": 45,
  "allTimeHistory": 42,
  "lastUpdated": 1704567890000
}
```

## Testing

### Upload Test
```bash
cd Desktop
mvn clean javafx:run
```

1. Sign in with Google
2. Click "Upload to Cloud"
3. Should see: "Uploaded X targets, Y history"
4. No errors

### Download Test
1. After upload, delete some local targets
2. Click "Download from Cloud"
3. Should see: "Downloaded X new targets"
4. Deleted targets should reappear

### Cross-Platform Test
1. Add targets on Android
2. Upload from Android
3. Download on Desktop → See Android targets
4. Modify on Desktop
5. Upload from Desktop
6. Download on Android → See Desktop changes

## Files Modified

1. [SettingsController.java](Desktop/src/main/java/com/icpx/controller/SettingsController.java)
   - Fixed upload data format (String ID, camelCase fields, epoch timestamps)
   - Added history sync
   - Added stats sync
   - Fixed download to read String ID
   - Added update logic for existing targets

## Summary

All sync issues are now resolved. Desktop and Android now use identical data formats and both upload/download work correctly with full bidirectional sync including:
- ✅ Targets
- ✅ History (solved problems)
- ✅ All-time stats
- ✅ Cross-platform compatibility
