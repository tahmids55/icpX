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
 * Fragment showing daily statistics
 */
public class StatsDailyFragment extends Fragment {

    private TextView dailyAddedValue;
    private TextView dailySolvedValue;
    private TextView dailyPendingValue;
    private TextView currentCfRatingValue;
    
    private TargetDAO targetDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_daily, container, false);
        
        dailyAddedValue = view.findViewById(R.id.dailyAddedValue);
        dailySolvedValue = view.findViewById(R.id.dailySolvedValue);
        dailyPendingValue = view.findViewById(R.id.dailyPendingValue);
        currentCfRatingValue = view.findViewById(R.id.currentCfRatingValue);
        
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
            // Get daily statistics
            int dailyAdded = targetDAO.getDailyAddedCount(userEmail);
            int dailySolved = targetDAO.getDailySolvedCount(userEmail);
            int dailyPending = targetDAO.getCountByStatus("pending", userEmail);
            
            // Get Codeforces handle for current rating
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
                                    android.util.Log.e("StatsDailyFragment", "Failed to load handle", e);
                                    latch.countDown();
                                }
                            });
                    
                    latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
                    if (firebaseHandle[0] != null) {
                        cfHandle = firebaseHandle[0];
                    }
                } catch (Exception e) {
                    android.util.Log.e("StatsDailyFragment", "Error loading handle from Firebase", e);
                }
            }
            
            final String finalHandle = cfHandle;
            String ratingText = "N/A";
            
            if (finalHandle != null && !finalHandle.isEmpty()) {
                try {
                    CodeforcesService cfService = new CodeforcesService();
                    JSONObject userInfo = cfService.fetchUserInfo(finalHandle);
                    if (userInfo != null && userInfo.has("rating")) {
                        int rating = userInfo.getInt("rating");
                        ratingText = String.valueOf(rating);
                    }
                } catch (Exception e) {
                    android.util.Log.e("StatsDailyFragment", "Error fetching CF rating", e);
                }
            }

            final String finalRating = ratingText;
            
            requireActivity().runOnUiThread(() -> {
                dailyAddedValue.setText(String.valueOf(dailyAdded));
                dailySolvedValue.setText(String.valueOf(dailySolved));
                dailyPendingValue.setText(String.valueOf(dailyPending));
                currentCfRatingValue.setText(finalRating);
                try {
                    int r = Integer.parseInt(finalRating);
                    currentCfRatingValue.setTextColor(getRatingColor(r));
                } catch (NumberFormatException e) {
                    currentCfRatingValue.setTextColor(android.graphics.Color.GRAY);
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
