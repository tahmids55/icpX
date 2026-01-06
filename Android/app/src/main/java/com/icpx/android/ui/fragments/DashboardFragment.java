package com.icpx.android.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.icpx.android.R;
import com.icpx.android.adapters.StatCardAdapter;
import com.icpx.android.adapters.TargetAdapter;
import com.icpx.android.database.TargetDAO;
import com.icpx.android.database.UserDAO;
import com.icpx.android.firebase.FirebaseManager;
import com.icpx.android.model.StatCard;
import com.icpx.android.model.Target;
import com.icpx.android.model.User;
import com.icpx.android.service.CodeforcesService;
import com.icpx.android.ui.views.ActivityHeatmapView;
import com.icpx.android.ui.views.RatingGraphView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dashboard fragment showing statistics and recent activity
 */
public class DashboardFragment extends Fragment {

    private RecyclerView statsRecyclerView;
    private RecyclerView recentTargetsRecyclerView;
    private TextView emptyStateText;
    private MaterialButton viewAllButton;
    private ActivityHeatmapView activityHeatmap;
    private RatingGraphView ratingGraph;
    
    private StatCardAdapter statCardAdapter;
    private TargetAdapter targetAdapter;
    private TargetDAO targetDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        
        initViews(view);
        targetDAO = new TargetDAO(requireContext());
        
        setupStatsRecyclerView();
        setupRecentTargetsRecyclerView();
        loadData();
        
        return view;
    }

    private void initViews(View view) {
        statsRecyclerView = view.findViewById(R.id.statsRecyclerView);
        recentTargetsRecyclerView = view.findViewById(R.id.recentTargetsRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        viewAllButton = view.findViewById(R.id.viewAllButton);
        activityHeatmap = view.findViewById(R.id.activityHeatmap);
        ratingGraph = view.findViewById(R.id.ratingGraph);
        
        // Set click listener for View All button
        viewAllButton.setOnClickListener(v -> navigateToTargets());
    }

    private void setupStatsRecyclerView() {
        statsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        statCardAdapter = new StatCardAdapter(new ArrayList<>());
        statsRecyclerView.setAdapter(statCardAdapter);
    }

    private void setupRecentTargetsRecyclerView() {
        recentTargetsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        targetAdapter = new TargetAdapter(new ArrayList<>(), this::onTargetClick);
        recentTargetsRecyclerView.setAdapter(targetAdapter);
    }

    private void loadData() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || firebaseUser.getEmail() == null) {
            return;
        }
        final String userEmail = firebaseUser.getEmail();
        
        new Thread(() -> {
            // Get statistics for current user
            int totalProblems = targetDAO.getAllTargets(userEmail).size();
            int solvedProblems = targetDAO.getCountByStatus("achieved", userEmail);
            int pendingProblems = targetDAO.getCountByStatus("pending", userEmail);
            
            // Get Codeforces handle from user-specific SharedPreferences
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
                                        // Save to local prefs for faster access next time
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
                                    android.util.Log.e("DashboardFragment", "Failed to load handle from Firebase", e);
                                    latch.countDown();
                                }
                            });
                    
                    latch.await(3, java.util.concurrent.TimeUnit.SECONDS);
                    if (firebaseHandle[0] != null) {
                        cfHandle = firebaseHandle[0];
                    }
                } catch (Exception e) {
                    android.util.Log.e("DashboardFragment", "Error loading handle from Firebase", e);
                }
            }
            
            final String finalHandle = cfHandle;
            
            String ratingText = "N/A";
            int ratingColor = R.color.statPurple;
            
            if (finalHandle != null && !finalHandle.isEmpty()) {
                try {
                    CodeforcesService cfService = new CodeforcesService();
                    JSONObject userInfo = cfService.fetchUserInfo(finalHandle);
                    if (userInfo != null && userInfo.has("rating")) {
                        int rating = userInfo.getInt("rating");
                        ratingText = String.valueOf(rating);
                        ratingColor = getRatingColor(rating);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // Keep default values on error
                }
            }

            // Create stat cards
            List<StatCard> statCards = new ArrayList<>();
            statCards.add(new StatCard("Total Problems", String.valueOf(totalProblems), 
                    "📊", R.color.statBlue));
            statCards.add(new StatCard("Solved", String.valueOf(solvedProblems), 
                    "✅", R.color.statGreen));
            statCards.add(new StatCard("Pending", String.valueOf(pendingProblems), 
                    "⏳", R.color.statOrange));
            statCards.add(new StatCard("CF Rating", ratingText, 
                    "", ratingColor));

            // Get recent targets (limit to 5)
            List<Target> allTargets = targetDAO.getAllTargets(userEmail);
            List<Target> recentTargets = allTargets.subList(0, Math.min(5, allTargets.size()));

            // Get activity data for heatmap
            Map<String, Integer> problemActivity = targetDAO.getProblemActivityByDate(userEmail);
            Map<String, Integer> topicActivity = targetDAO.getTopicActivityByDate(userEmail);

            // Sample rating history for demonstration (replace with actual mechanism later)
            List<RatingGraphView.RatingPoint> ratingHistory = new ArrayList<>();
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 365*24*60*60*1000L, 800, "Contest 1"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 330*24*60*60*1000L, 950, "Contest 2"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 300*24*60*60*1000L, 1050, "Contest 3"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 270*24*60*60*1000L, 1150, "Contest 4"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 240*24*60*60*1000L, 1100, "Contest 5"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 210*24*60*60*1000L, 1250, "Contest 6"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 180*24*60*60*1000L, 1200, "Contest 7"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 150*24*60*60*1000L, 1320, "Contest 8"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 120*24*60*60*1000L, 1420, "Contest 9"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 90*24*60*60*1000L, 1380, "Contest 10"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 60*24*60*60*1000L, 1520, "Contest 11"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 30*24*60*60*1000L, 1480, "Contest 12"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis() - 15*24*60*60*1000L, 1610, "Contest 13"));
            ratingHistory.add(new RatingGraphView.RatingPoint(System.currentTimeMillis(), 1580, "Contest 14"));

            requireActivity().runOnUiThread(() -> {
                statCardAdapter.updateData(statCards);
                targetAdapter.updateData(recentTargets);
                activityHeatmap.setActivityData(problemActivity, topicActivity);
                ratingGraph.setRatingHistory(ratingHistory);
                
                if (recentTargets.isEmpty()) {
                    emptyStateText.setVisibility(View.VISIBLE);
                    recentTargetsRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateText.setVisibility(View.GONE);
                    recentTargetsRecyclerView.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }
    
    /**
     * Get Codeforces rating color based on rating value
     */
    private int getRatingColor(int rating) {
        if (rating < 1200) {
            return R.color.ratingNewbie;        // Gray
        } else if (rating < 1400) {
            return R.color.ratingPupil;         // Green
        } else if (rating < 1600) {
            return R.color.ratingSpecialist;    // Cyan
        } else if (rating < 1900) {
            return R.color.ratingExpert;        // Blue
        } else if (rating < 2100) {
            return R.color.ratingCandidateMaster; // Violet
        } else if (rating < 2400) {
            return R.color.ratingMaster;        // Orange
        } else {
            return R.color.ratingGrandmaster;   // Red
        }
    }

    private void onTargetClick(Target target) {
        // Handle target click - can open detail dialog or navigate
    }
    
    /**
     * Navigate to Targets section
     */
    private void navigateToTargets() {
        if (getActivity() != null) {
            NavigationView navigationView = getActivity().findViewById(R.id.navigationView);
            if (navigationView != null) {
                navigationView.setCheckedItem(R.id.nav_targets);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.contentContainer, new TargetsFragment())
                        .commit();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData(); // Refresh data when returning to fragment
    }
}
