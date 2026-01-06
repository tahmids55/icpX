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

import com.icpx.android.R;
import com.icpx.android.adapters.StatCardAdapter;
import com.icpx.android.adapters.TargetAdapter;
import com.icpx.android.database.TargetDAO;
import com.icpx.android.model.StatCard;
import com.icpx.android.model.Target;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard fragment showing statistics and recent activity
 */
public class DashboardFragment extends Fragment {

    private RecyclerView statsRecyclerView;
    private RecyclerView recentTargetsRecyclerView;
    private TextView emptyStateText;
    
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
        new Thread(() -> {
            // Get statistics
            int totalProblems = targetDAO.getAllTargets().size();
            int solvedProblems = targetDAO.getCountByStatus("achieved");
            int pendingProblems = targetDAO.getCountByStatus("pending");
            double avgRating = targetDAO.getAverageRating();

            // Create stat cards
            List<StatCard> statCards = new ArrayList<>();
            statCards.add(new StatCard("Total Problems", String.valueOf(totalProblems), 
                    "📊", R.color.statBlue));
            statCards.add(new StatCard("Solved", String.valueOf(solvedProblems), 
                    "✅", R.color.statGreen));
            statCards.add(new StatCard("Pending", String.valueOf(pendingProblems), 
                    "⏳", R.color.statOrange));
            statCards.add(new StatCard("Avg Rating", avgRating > 0 ? String.format("%.0f", avgRating) : "N/A", 
                    "⭐", R.color.statPurple));

            // Get recent targets (limit to 5)
            List<Target> allTargets = targetDAO.getAllTargets();
            List<Target> recentTargets = allTargets.subList(0, Math.min(5, allTargets.size()));

            requireActivity().runOnUiThread(() -> {
                statCardAdapter.updateData(statCards);
                targetAdapter.updateData(recentTargets);
                
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

    private void onTargetClick(Target target) {
        // Handle target click - can open detail dialog or navigate
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData(); // Refresh data when returning to fragment
    }
}
