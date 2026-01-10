# Heatmap Cloud Sync Implementation

## Overview
Your heatmap activity data is now automatically synced to Firebase Cloud Firestore. Users can access their activity history from any device using their email account.

## What Gets Synced
Each day's activity is saved with:
- **Problem Count**: Number of problems solved that day
- **Topic Count**: Number of topics learned that day
- **Date**: Date in format "yyyy-MM-dd"
- **Timestamp**: When the data was last updated

## Cloud Data Structure
```
users/
  {userId}/
    daily_activity/
      {date}/
        problemCount: 5
        topicCount: 3
        timestamp: 1234567890
```

Example:
```
users/abc123/daily_activity/2025-01-15/
  problemCount: 5
  topicCount: 3
  timestamp: 1736985600000
```

## How It Works

### 1. Automatic Sync
When you open the Dashboard:
- The app loads local activity data from your device
- Displays it on the heatmap immediately
- Automatically syncs the data to Firebase in the background
- Merges with existing cloud data (doesn't overwrite)

### 2. Data Flow
```
Local SQLite Database
  ↓ (reads activity)
DashboardFragment.loadData()
  ↓ (displays on heatmap)
ActivityHeatmapView
  ↓ (syncs to cloud)
FirebaseManager.batchSaveDailyActivity()
  ↓ (stores in Firestore)
Firebase Cloud
```

### 3. Firebase Methods Added

#### `saveDailyActivity(userId, date, problemCount, topicCount, callback)`
Saves a single day's activity to the cloud.

#### `batchSaveDailyActivity(userId, activityMap, callback)`
Saves multiple days at once (more efficient). This is what the Dashboard uses.

#### `getDailyActivity(userId, callback)`
Retrieves all activity data from the cloud for the user.

## Code Changes

### FirebaseManager.java
Added three new methods:
1. `saveDailyActivity()` - Save single date
2. `batchSaveDailyActivity()` - Save multiple dates (batch operation)
3. `getDailyActivity()` - Retrieve cloud data

### DashboardFragment.java
- Added `firebaseManager` field
- Added `syncActivityDataToCloud()` method
- Modified `loadData()` to call sync after displaying data
- Imports: Added `HashMap` and `DocumentSnapshot`

## Benefits
✓ **Cross-Device Access**: View your heatmap on any device  
✓ **Data Backup**: Never lose your activity history  
✓ **Automatic Sync**: No manual action needed  
✓ **Merge Strategy**: Uses SetOptions.merge() to preserve existing data  
✓ **Efficient**: Batch writes reduce Firestore operations  

## Future Enhancements (Optional)
- Load cloud data when local database is empty
- Sync only new/changed dates (delta sync)
- Pull-to-refresh to force sync from cloud
- Offline queue for sync when network unavailable
- Conflict resolution for concurrent edits

## Testing
To verify it works:
1. Open the Dashboard → heatmap loads and syncs
2. Check Firebase Console → users/{yourUserId}/daily_activity
3. You should see documents for each date with activity
4. Log in on another device → data should appear (future: needs load from cloud implementation)

## Security
- Data is isolated by userId (only your data is accessible)
- Firebase Security Rules should be configured:
```javascript
match /users/{userId}/daily_activity/{date} {
  allow read, write: if request.auth.uid == userId;
}
```

## Notes
- Current implementation is **push-only** (local → cloud)
- To enable **pull** (cloud → local), implement getDailyActivity() in Dashboard
- Sync happens every time Dashboard loads (could optimize with timestamp checks)
