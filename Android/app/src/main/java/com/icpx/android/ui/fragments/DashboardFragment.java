package com.icpx.android.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.icpx.android.R;
import com.icpx.android.adapters.StatsPagerAdapter;
import com.icpx.android.adapters.TargetAdapter;
import com.icpx.android.database.TargetDAO;
import com.icpx.android.model.Target;
import com.icpx.android.ui.views.ActivityHeatmapView;
import com.icpx.android.ui.views.PersonalRatingBarView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dashboard fragment showing statistics and recent activity
 */
public class DashboardFragment extends Fragment {

    private ViewPager2 statsViewPager;
    private TabLayout statsTabLayout;
    private RecyclerView recentTargetsRecyclerView;
    private TextView emptyStateText;
    private MaterialButton viewAllButton;
    private ActivityHeatmapView activityHeatmap;
    private PersonalRatingBarView personalRatingBar;
    
    private StatsPagerAdapter statsPagerAdapter;
    private TargetAdapter targetAdapter;
    private TargetDAO targetDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        
        initViews(view);
        targetDAO = new TargetDAO(requireContext());
        
        setupStatsViewPager();
        setupRecentTargetsRecyclerView();
        loadData();
        
        return view;
    }

    private void initViews(View view) {
        statsViewPager = view.findViewById(R.id.statsViewPager);
        statsTabLayout = view.findViewById(R.id.statsTabLayout);
        recentTargetsRecyclerView = view.findViewById(R.id.recentTargetsRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        viewAllButton = view.findViewById(R.id.viewAllButton);
        activityHeatmap = view.findViewById(R.id.activityHeatmap);
        personalRatingBar = view.findViewById(R.id.personalRatingBar);
        
        // Set click listener for View All button
        viewAllButton.setOnClickListener(v -> navigateToTargets());
    }

    private void setupStatsViewPager() {
        statsPagerAdapter = new StatsPagerAdapter(this);
        statsViewPager.setAdapter(statsPagerAdapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(statsTabLayout, statsViewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("All Time");
                            break;
                        case 1:
                            tab.setText("Daily");
                            break;
                    }
                }
        ).attach();
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

            // Get recent targets (limit to 5)
            List<Target> allTargets = targetDAO.getAllTargets(userEmail);
            List<Target> recentTargets = allTargets.subList(0, Math.min(5, allTargets.size()));

            // Get activity data for heatmap
            Map<String, Integer> problemActivity = targetDAO.getProblemActivityByDate(userEmail);
            Map<String, Integer> topicActivity = targetDAO.getTopicActivityByDate(userEmail);

            // Calculate personal rating based on target achievement
            float personalRating = calculatePersonalRating(totalProblems, solvedProblems, pendingProblems);

            requireActivity().runOnUiThread(() -> {
                targetAdapter.updateData(recentTargets);
                activityHeatmap.setActivityData(problemActivity, topicActivity);
                personalRatingBar.setRating(personalRating);
                
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
     * Calculate personal rating (0-10) based on target achievement
     * Increases when targets are achieved, decreases when targets are not met
     */
    private float calculatePersonalRating(int totalProblems, int solvedProblems, int pendingProblems) {
        // Load previous rating from SharedPreferences
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || firebaseUser.getEmail() == null) {
            return 5.0f; // Default starting rating
        }
        
        String prefKey = "user_prefs_" + firebaseUser.getEmail();
        android.content.SharedPreferences userPrefs = requireActivity().getSharedPreferences(prefKey, 0);
        float previousRating = userPrefs.getFloat("personal_rating", 5.0f);
        int previousSolved = userPrefs.getInt("previous_solved", 0);
        int previousPending = userPrefs.getInt("previous_pending", 0);
        
        // Calculate change
        float rating = previousRating;
        
        // Increase for newly solved problems
        int newlySolved = solvedProblems - previousSolved;
        if (newlySolved > 0) {
            rating += newlySolved * 0.15f; // +0.15 per solved problem
        }
        
        // Decrease for increase in pending problems (targets not being met)
        int pendingIncrease = pendingProblems - previousPending;
        if (pendingIncrease > 0) {
            rating -= pendingIncrease * 0.05f; // -0.05 per new pending
        }
        
        // Clamp between 0 and 10
        rating = Math.max(0f, Math.min(10f, rating));
        
        // Save current state for next calculation
        userPrefs.edit()
            .putFloat("personal_rating", rating)
            .putInt("previous_solved", solvedProblems)
            .putInt("previous_pending", pendingProblems)
            .apply();
        
        return rating;
    }

    private void onTargetClick(Target target) {
        // Open ProblemTabbedActivity with the problem URL and name
        if (target.getProblemLink() != null && !target.getProblemLink().isEmpty()) {
            android.content.Intent intent = new android.content.Intent(requireActivity(), 
                    com.icpx.android.ui.ProblemTabbedActivity.class);
            intent.putExtra("EXTRA_PROBLEM_URL", target.getProblemLink());
            intent.putExtra("EXTRA_PROBLEM_NAME", target.getName());
            startActivity(intent);
        } else {
            android.widget.Toast.makeText(requireContext(), 
                    "No problem link available", 
                    android.widget.Toast.LENGTH_SHORT).show();
        }
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
