package com.icpx.android.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.icpx.android.R;
import com.icpx.android.database.TargetDAO;
import com.icpx.android.firebase.FirebaseManager;
import com.icpx.android.service.CodeforcesService;

import org.json.JSONObject;

/**
 * Fragment showing all-time statistics
 */
public class StatsAllTimeFragment extends Fragment {

    private TextView allTimeSolvedValue;
    private TextView allTimeTotalValue;
    private TextView allTimePendingValue;
    private TextView maxCfRatingValue;
    
    private TargetDAO targetDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_all_time, container, false);
        
        allTimeSolvedValue = view.findViewById(R.id.allTimeSolvedValue);
        allTimeTotalValue = view.findViewById(R.id.allTimeTotalValue);
        allTimePendingValue = view.findViewById(R.id.allTimePendingValue);
        maxCfRatingValue = view.findViewById(R.id.maxCfRatingValue);
        
        targetDAO = new TargetDAO(requireContext());
        
        loadData();
        
        return view;
    }

    private void loadData() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || firebaseUser.getEmail() == null) {
            return;
        }
        final String userEmail = firebaseUser.getEmail();
        
        new Thread(() -> {
            // Get all-time statistics
            int uniqueSolved = targetDAO.getUniqueSolvedCount(userEmail);
            int totalProblems = targetDAO.getAllTargets(userEmail).size();
            int pendingProblems = targetDAO.getCountByStatus("pending", userEmail);
            int maxRating = targetDAO.getMaxCfRating(userEmail);
            
            // Get Codeforces handle for max rating comparison
            String prefKey = "user_prefs_" + userEmail;
            android.content.SharedPreferences userPrefs = requireActivity().getSharedPreferences(prefKey, 0);
            String cfHandle = userPrefs.getString("codeforces_handle", null);
            
            // If not in local prefs, try loading from Firebase
            if (cfHandle == null || cfHandle.isEmpty()) {
                try {
                    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    final String[] firebaseHandle = new String[1];
                    
                    FirebaseManager.getInstance().getUserData(firebaseUser.getUid(),
                            new FirebaseManager.DataCallback() {
                                @Override
                                public void onSuccess(java.util.List<com.google.firebase.firestore.DocumentSnapshot> documents) {
                                    if (!documents.isEmpty()) {
                                        com.google.firebase.firestore.DocumentSnapshot doc = documents.get(0);
                                        firebaseHandle[0] = doc.getString("codeforcesHandle");
                                        if (firebaseHandle[0] != null && !firebaseHandle[0].isEmpty()) {
                                            userPrefs.edit()
                                                    .putString("codeforces_handle", firebaseHandle[0])
                                                    .apply();
                                        }
                                    }
                                    latch.countDown();
                                }
                                
                                @Override
                                public void onFailure(Exception e) {
                                    android.util.Log.e("StatsAllTimeFragment", "Failed to load handle", e);
                                    latch.countDown();
                                }
                            });
                    
                    latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
                    if (firebaseHandle[0] != null) {
                        cfHandle = firebaseHandle[0];
                    }
                } catch (Exception e) {
                    android.util.Log.e("StatsAllTimeFragment", "Error loading handle from Firebase", e);
                }
            }
            
            final String finalHandle = cfHandle;
            String maxRatingText = "N/A";
            
            if (finalHandle != null && !finalHandle.isEmpty()) {
                try {
                    CodeforcesService cfService = new CodeforcesService();
                    JSONObject userInfo = cfService.fetchUserInfo(finalHandle);
                    if (userInfo != null && userInfo.has("maxRating")) {
                        int cfMaxRating = userInfo.getInt("maxRating");
                        maxRatingText = String.valueOf(cfMaxRating);
                    }
                } catch (Exception e) {
                    android.util.Log.e("StatsAllTimeFragment", "Error fetching CF rating", e);
                }
            }

            final String finalMaxRating = maxRatingText;
            
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                allTimeSolvedValue.setText(String.valueOf(uniqueSolved));
                allTimeTotalValue.setText(String.valueOf(totalProblems));
                allTimePendingValue.setText(String.valueOf(pendingProblems));
                maxCfRatingValue.setText(finalMaxRating);
                try {
                    int r = Integer.parseInt(finalMaxRating);
                    maxCfRatingValue.setTextColor(getRatingColor(r));
                } catch (NumberFormatException e) {
                    // Default color if N/A
                    maxCfRatingValue.setTextColor(android.graphics.Color.GRAY);
                }
            });
        }).start();
    }

    private int getRatingColor(int rating) {
        if (rating < 1200) return android.graphics.Color.GRAY;
        if (rating < 1400) return android.graphics.Color.parseColor("#008000"); // Green
        if (rating < 1600) return android.graphics.Color.parseColor("#03A89E"); // Cyan
        if (rating < 1900) return android.graphics.Color.BLUE;
        if (rating < 2100) return android.graphics.Color.parseColor("#AA00AA"); // Violet
        if (rating < 2400) return android.graphics.Color.parseColor("#FF8C00"); // Orange
        return android.graphics.Color.RED;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
}
