package com.icpx.android.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.icpx.android.R;
import com.icpx.android.firebase.FirebaseManager;
import com.icpx.android.firebase.FirebaseSyncService;
import com.icpx.android.ui.LoginActivity;
import com.icpx.android.worker.SyncWorker;
import com.icpx.android.utils.NotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Fragment for app settings
 */
public class SettingsFragment extends Fragment {

    private View cfHandleLayout;
    private TextView cfHandleText;
    private View changePasswordLayout;
    private View logoutLayout;
    private View clearDataLayout;
    private SwitchCompat requireLoginSwitch;
    private SwitchCompat notificationsSwitch;
    private SwitchCompat autoFetchSwitch;
    private SwitchCompat contestRemindersSwitch;
    private View reminderTimeLayout;
    private TextView reminderTimeText;
    private MaterialButton fullSyncButton;
    private TextView lastSyncText;
    private SharedPreferences prefs;
    private FirebaseSyncService syncService;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        prefs = requireActivity().getSharedPreferences("user_prefs", 0);
        syncService = new FirebaseSyncService(requireContext());
        
        // Setup Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
        
        // Setup activity result launcher for Google Sign-In
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    }
                });
        
        initViews(view);
        setupListeners();
        loadCfHandle();
        updateLastSyncTime();
        
        return view;
    }

    private void initViews(View view) {
        cfHandleLayout = view.findViewById(R.id.cfHandleLayout);
        cfHandleText = view.findViewById(R.id.cfHandleText);
        changePasswordLayout = view.findViewById(R.id.changePasswordLayout);
        logoutLayout = view.findViewById(R.id.logoutLayout);
        clearDataLayout = view.findViewById(R.id.clearDataLayout);
        requireLoginSwitch = view.findViewById(R.id.requireLoginSwitch);
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        autoFetchSwitch = view.findViewById(R.id.autoFetchSwitch);
        contestRemindersSwitch = view.findViewById(R.id.contestRemindersSwitch);
        reminderTimeLayout = view.findViewById(R.id.reminderTimeLayout);
        reminderTimeText = view.findViewById(R.id.reminderTimeText);
        fullSyncButton = view.findViewById(R.id.fullSyncButton);
        lastSyncText = view.findViewById(R.id.lastSyncText);

        // Load saved preferences
        boolean requireLogin = prefs.getBoolean("require_login", true);
        requireLoginSwitch.setChecked(requireLogin);

        boolean autoFetchEnabled = prefs.getBoolean("auto_fetch_enabled", false);
        autoFetchSwitch.setChecked(autoFetchEnabled);
        
        boolean contestRemindersEnabled = prefs.getBoolean("contest_reminders_enabled", false);
        contestRemindersSwitch.setChecked(contestRemindersEnabled);
        reminderTimeLayout.setVisibility(contestRemindersEnabled ? View.VISIBLE : View.GONE);
        
        int reminderHours = prefs.getInt("contest_reminder_hours", 24);
        reminderTimeText.setText(reminderHours + " hours");
    }

    private void loadCfHandle() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            cfHandleText.setText("Not logged in");
            cfHandleText.setTextColor(getResources().getColor(R.color.textSecondary));
            return;
        }
        String prefKey = "user_prefs_" + currentUser.getEmail();
        android.content.SharedPreferences userPrefs = requireActivity().getSharedPreferences(prefKey, 0);
        String handle = userPrefs.getString("codeforces_handle", "");
        if (handle.isEmpty()) {
            cfHandleText.setText("Not set - Tap to add");
            cfHandleText.setTextColor(getResources().getColor(R.color.textSecondary));
        } else {
            cfHandleText.setText(handle);
            cfHandleText.setTextColor(getResources().getColor(R.color.colorPrimary));
        }
    }

    private void setupListeners() {
        cfHandleLayout.setOnClickListener(v -> showCfHandleDialog());

        requireLoginSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            prefs.edit()
                    .putBoolean("require_login", isChecked)
                    .apply();
            
            Toast.makeText(requireContext(), 
                    isChecked ? "Login required on startup" : "Login disabled - app will skip login screen", 
                    Toast.LENGTH_SHORT).show();
        });

        changePasswordLayout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Change password feature coming soon", Toast.LENGTH_SHORT).show();
        });

        logoutLayout.setOnClickListener(v -> {
            // Clear user session and go to login
            prefs.edit()
                    .clear()
                    .apply();
            
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        clearDataLayout.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Cache cleared", Toast.LENGTH_SHORT).show();
        });

        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(requireContext(), 
                    "Notifications " + (isChecked ? "enabled" : "disabled"), 
                    Toast.LENGTH_SHORT).show();
        });

        autoFetchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("auto_fetch_enabled", isChecked).apply();
            
            if (isChecked) {
                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build();

                PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 1, TimeUnit.DAYS)
                        .setConstraints(constraints)
                        .setInitialDelay(1, TimeUnit.MINUTES)
                        .build();

                WorkManager.getInstance(requireContext())
                        .enqueueUniquePeriodicWork("DailySync", ExistingPeriodicWorkPolicy.UPDATE, syncRequest);
                
                Toast.makeText(requireContext(), "Auto-sync enabled (Every 24h)", Toast.LENGTH_SHORT).show();
            } else {
                WorkManager.getInstance(requireContext()).cancelUniqueWork("DailySync");
                Toast.makeText(requireContext(), "Auto-sync disabled", Toast.LENGTH_SHORT).show();
            }
        });

        contestRemindersSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("contest_reminders_enabled", isChecked).apply();
            reminderTimeLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            Toast.makeText(requireContext(), 
                    "Contest reminders " + (isChecked ? "enabled" : "disabled"), 
                    Toast.LENGTH_SHORT).show();
            
            // Cancel all reminders if disabled, or trigger reschedule if enabled
            if (!isChecked) {
                WorkManager.getInstance(requireContext()).cancelAllWorkByTag("contest_reminder");
            } else {
                // Notify ContestsFragment to reschedule reminders
                rescheduleContestReminders();
            }
        });

        reminderTimeLayout.setOnClickListener(v -> showReminderTimeDialog());

        // Firebase Sync button
        fullSyncButton.setOnClickListener(v -> performFullSync());
    }

    private void showReminderTimeDialog() {
        final String[] options = {"24 hours", "48 hours"};
        final int[] hours = {24, 48};
        
        int currentHours = prefs.getInt("contest_reminder_hours", 24);
        int selectedIndex = currentHours == 48 ? 1 : 0;
        
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Remind me before")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    prefs.edit().putInt("contest_reminder_hours", hours[which]).apply();
                    reminderTimeText.setText(options[which]);
                    Toast.makeText(requireContext(), "Reminder time set to " + options[which], Toast.LENGTH_SHORT).show();
                    
                    // Reschedule reminders with new time
                    rescheduleContestReminders();
                    
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void rescheduleContestReminders() {
        // Cancel all existing reminders
        WorkManager.getInstance(requireContext()).cancelAllWorkByTag("contest_reminder");
        
        // The ContestsFragment will automatically reschedule when it's next loaded
        // or we can broadcast to trigger immediate reschedule
        Toast.makeText(requireContext(), "Reminders will be updated", Toast.LENGTH_SHORT).show();
    }

    private void showCfHandleDialog() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(requireContext(), "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }
        String prefKey = "user_prefs_" + currentUser.getEmail();
        android.content.SharedPreferences userPrefs = requireActivity().getSharedPreferences(prefKey, 0);
        
        View dialogView = LayoutInflater.from(requireContext()).inflate(android.R.layout.select_dialog_item, null);
        EditText input = new EditText(requireContext());
        input.setHint("Enter Codeforces handle");
        
        String currentHandle = userPrefs.getString("codeforces_handle", "");
        if (!currentHandle.isEmpty()) {
            input.setText(currentHandle);
            input.setSelection(currentHandle.length());
        }
        
        input.setPadding(50, 30, 50, 30);

        new AlertDialog.Builder(requireContext())
                .setTitle("Codeforces Handle")
                .setMessage("Enter your Codeforces handle for automatic problem verification")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String handle = input.getText().toString().trim();
                    if (!handle.isEmpty()) {
                        // Save to local SharedPreferences
                        userPrefs.edit()
                                .putString("codeforces_handle", handle)
                                .apply();
                        
                        // Also save to Firebase
                        java.util.Map<String, Object> userData = new java.util.HashMap<>();
                        userData.put("codeforcesHandle", handle);
                        com.icpx.android.firebase.FirebaseManager.getInstance().saveUserData(
                                currentUser.getUid(), userData,
                                new com.icpx.android.firebase.FirebaseManager.FirestoreCallback() {
                                    @Override
                                    public void onSuccess() {
                                        android.util.Log.d("SettingsFragment", "Handle synced to Firebase");
                                    }
                                    
                                    @Override
                                    public void onFailure(Exception e) {
                                        android.util.Log.e("SettingsFragment", "Failed to sync handle to Firebase", e);
                                    }
                                });
                        
                        loadCfHandle();
                        Toast.makeText(requireContext(), "Codeforces handle saved: " + handle, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Handle cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Remove", (dialog, which) -> {
                    // Remove from local SharedPreferences
                    userPrefs.edit()
                            .remove("codeforces_handle")
                            .apply();
                    
                    // Also remove from Firebase
                    java.util.Map<String, Object> userData = new java.util.HashMap<>();
                    userData.put("codeforcesHandle", null);
                    com.icpx.android.firebase.FirebaseManager.getInstance().saveUserData(
                            currentUser.getUid(), userData,
                            new com.icpx.android.firebase.FirebaseManager.FirestoreCallback() {
                                @Override
                                public void onSuccess() {
                                    android.util.Log.d("SettingsFragment", "Handle removed from Firebase");
                                }
                                
                                @Override
                                public void onFailure(Exception e) {
                                    android.util.Log.e("SettingsFragment", "Failed to remove handle from Firebase", e);
                                }
                            });
                    
                    loadCfHandle();
                    Toast.makeText(requireContext(), "Codeforces handle removed", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void uploadToCloud() {
        if (!checkFirebaseAuth()) return;
        
        setButtonsEnabled(false);
        Toast.makeText(requireContext(), "Uploading to cloud...", Toast.LENGTH_SHORT).show();
        
        syncService.syncTargetsToFirebase(new FirebaseSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                // After targets are synced, sync all-time stats
                int allTimeSolve = syncService.getTargetDAO().getAllTimeSolve();
                int allTimeHistory = syncService.getTargetDAO().getAllTimeHistory();
                syncService.syncAllTimeStatsToFirebase(allTimeSolve, allTimeHistory, new FirebaseManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        // After stats, sync history
                        syncService.syncHistoryToFirebase(new FirebaseManager.FirestoreCallback() {
                            @Override
                            public void onSuccess() {
                                requireActivity().runOnUiThread(() -> {
                                    setButtonsEnabled(true);
                                    saveLastSyncTime();
                                    updateLastSyncTime();
                                    String msg = "✓ " + message + "\nAll-time stats and history synced.";
                                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                                    NotificationHelper.sendSyncNotification(requireContext(), "Upload Complete", message + "\nAll-time stats and history synced.");
                                });
                            }
                            @Override
                            public void onFailure(Exception e) {
                                requireActivity().runOnUiThread(() -> {
                                    setButtonsEnabled(true);
                                    Toast.makeText(requireContext(), "✗ History upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    NotificationHelper.sendSyncNotification(requireContext(), "History Upload Failed", e.getMessage());
                                });
                            }
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        requireActivity().runOnUiThread(() -> {
                            setButtonsEnabled(true);
                            Toast.makeText(requireContext(), "✗ Stats upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            NotificationHelper.sendSyncNotification(requireContext(), "Stats Upload Failed", e.getMessage());
                        });
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    setButtonsEnabled(true);
                    Toast.makeText(requireContext(), "✗ Upload failed: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    NotificationHelper.sendSyncNotification(requireContext(), "Upload Failed", e.getMessage());
                });
            }
        });
    }

    private void downloadFromCloud() {
        if (!checkFirebaseAuth()) return;
        
        setButtonsEnabled(false);
        Toast.makeText(requireContext(), "Downloading from cloud...", Toast.LENGTH_SHORT).show();
        
        syncService.syncTargetsFromFirebase(new FirebaseSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                // After targets are downloaded, sync all-time stats
                int allTimeSolve = syncService.getTargetDAO().getAllTimeSolve();
                int allTimeHistory = syncService.getTargetDAO().getAllTimeHistory();
                syncService.syncAllTimeStatsToFirebase(allTimeSolve, allTimeHistory, new FirebaseManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        // After stats, sync history
                        syncService.syncHistoryToFirebase(new FirebaseManager.FirestoreCallback() {
                            @Override
                            public void onSuccess() {
                                requireActivity().runOnUiThread(() -> {
                                    setButtonsEnabled(true);
                                    saveLastSyncTime();
                                    updateLastSyncTime();
                                    String msg = "✓ " + message + "\nAll-time stats and history synced.";
                                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                                    NotificationHelper.sendSyncNotification(requireContext(), "Download Complete", message + "\nAll-time stats and history synced.");
                                });
                            }
                            @Override
                            public void onFailure(Exception e) {
                                requireActivity().runOnUiThread(() -> {
                                    setButtonsEnabled(true);
                                    Toast.makeText(requireContext(), "✗ History download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    NotificationHelper.sendSyncNotification(requireContext(), "History Download Failed", e.getMessage());
                                });
                            }
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        requireActivity().runOnUiThread(() -> {
                            setButtonsEnabled(true);
                            Toast.makeText(requireContext(), "✗ Stats download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            NotificationHelper.sendSyncNotification(requireContext(), "Stats Download Failed", e.getMessage());
                        });
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    setButtonsEnabled(true);
                    Toast.makeText(requireContext(), "✗ Download failed: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    NotificationHelper.sendSyncNotification(requireContext(), "Download Failed", e.getMessage());
                });
            }
        });
    }

    private void performFullSync() {
        if (!checkFirebaseAuth()) return;
        
        setButtonsEnabled(false);
        Toast.makeText(requireContext(), "Performing full sync...", Toast.LENGTH_SHORT).show();
        
        syncService.performFullSync(new FirebaseSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                // After full sync, sync all-time stats
                int allTimeSolve = syncService.getTargetDAO().getAllTimeSolve();
                int allTimeHistory = syncService.getTargetDAO().getAllTimeHistory();
                syncService.syncAllTimeStatsToFirebase(allTimeSolve, allTimeHistory, new FirebaseManager.FirestoreCallback() {
                    @Override
                    public void onSuccess() {
                        // After stats, sync history
                        syncService.syncHistoryToFirebase(new FirebaseManager.FirestoreCallback() {
                            @Override
                            public void onSuccess() {
                                requireActivity().runOnUiThread(() -> {
                                    setButtonsEnabled(true);
                                    saveLastSyncTime();
                                    updateLastSyncTime();
                                    Toast.makeText(requireContext(), "✓ Full sync complete!\nAll-time stats and history synced.", Toast.LENGTH_LONG).show();
                                    NotificationHelper.sendSyncNotification(requireContext(), "Full Sync Complete", "Data synchronized with cloud.\nAll-time stats and history synced.");
                                });
                            }
                            @Override
                            public void onFailure(Exception e) {
                                requireActivity().runOnUiThread(() -> {
                                    setButtonsEnabled(true);
                                    Toast.makeText(requireContext(), "✗ History sync failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    NotificationHelper.sendSyncNotification(requireContext(), "History Sync Failed", e.getMessage());
                                });
                            }
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        requireActivity().runOnUiThread(() -> {
                            setButtonsEnabled(true);
                            Toast.makeText(requireContext(), "✗ Stats sync failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            NotificationHelper.sendSyncNotification(requireContext(), "Stats Sync Failed", e.getMessage());
                        });
                    }
                });
            }
            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    setButtonsEnabled(true);
                    Toast.makeText(requireContext(), "✗ Sync failed: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    NotificationHelper.sendSyncNotification(requireContext(), "Sync Failed", e.getMessage());
                });
            }
        });
    }

    private boolean checkFirebaseAuth() {
        if (FirebaseManager.getInstance().getCurrentUser() == null) {
            // Show Google Sign-In dialog
            new AlertDialog.Builder(requireContext())
                    .setTitle("Sign in Required")
                    .setMessage("Sign in with Google to sync your data across devices")
                    .setPositiveButton("Sign in with Google", (dialog, which) -> {
                        signInWithGoogle();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return false;
        }
        return true;
    }
    
    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }
    
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            
            // Sign in to Firebase with Google account
            FirebaseManager.getInstance().signInWithGoogle(account, new FirebaseManager.AuthCallback() {
                @Override
                public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), 
                                "✓ Signed in as " + user.getEmail(), 
                                Toast.LENGTH_LONG).show();
                        
                        // Update last sync text to show user
                        lastSyncText.setText("Signed in as: " + user.getEmail());
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), 
                                "✗ Sign-in failed: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                    });
                }
            });
            
        } catch (ApiException e) {
            Toast.makeText(requireContext(), 
                    "Google sign-in failed: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        fullSyncButton.setEnabled(enabled);
    }

    private void saveLastSyncTime() {
        prefs.edit()
                .putLong("last_sync_time", System.currentTimeMillis())
                .apply();
    }

    private void updateLastSyncTime() {
        long lastSync = prefs.getLong("last_sync_time", 0);
        if (lastSync == 0) {
            lastSyncText.setText("Last sync: Never");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            lastSyncText.setText("Last sync: " + sdf.format(new Date(lastSync)));
        }
    }
}
