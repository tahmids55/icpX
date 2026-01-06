package com.icpx.android.ui.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.icpx.android.R;
import com.icpx.android.database.TargetDAO;
import com.icpx.android.model.Target;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for viewing solved problems history
 */
public class HistoryFragment extends Fragment {

    private RecyclerView historyRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private HistoryAdapter adapter;
    private TargetDAO targetDAO;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        
        targetDAO = new TargetDAO(requireContext());
        
        historyRecyclerView = view.findViewById(R.id.historyRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter();
        historyRecyclerView.setAdapter(adapter);
        
        swipeRefreshLayout.setOnRefreshListener(this::loadHistory);
        
        loadHistory();
        
        return view;
    }

    private void loadHistory() {
        new Thread(() -> {
            List<Target> allTargets = targetDAO.getTargetsByStatus("achieved");
            // Filter to show only problems (not topics)
            List<Target> problems = new ArrayList<>();
            for (Target target : allTargets) {
                if ("problem".equals(target.getType())) {
                    problems.add(target);
                }
            }
            requireActivity().runOnUiThread(() -> {
                adapter.setTargets(problems);
                swipeRefreshLayout.setRefreshing(false);
            });
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistory();
    }

    private int getRatingColor(int rating) {
        if (rating < 1200) return Color.parseColor("#808080"); // Gray - Newbie
        else if (rating < 1400) return Color.parseColor("#008000"); // Green - Pupil
        else if (rating < 1600) return Color.parseColor("#03A89E"); // Cyan - Specialist
        else if (rating < 1900) return Color.parseColor("#0000FF"); // Blue - Expert
        else if (rating < 2100) return Color.parseColor("#AA00AA"); // Purple - Candidate Master
        else if (rating < 2400) return Color.parseColor("#FF8C00"); // Orange - Master/IM
        else return Color.parseColor("#FF0000"); // Red - GM/IGM/LGM
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        
        private List<Target> targets = new ArrayList<>();
        private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        void setTargets(List<Target> targets) {
            this.targets = targets;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Target target = targets.get(position);
            
            // Set problem name
            holder.problemNameText.setText(target.getName());
            
            // Set date
            if (target.getCreatedAt() != null) {
                holder.problemDateText.setText(dateFormat.format(target.getCreatedAt()));
            } else {
                holder.problemDateText.setText("");
            }
            
            // Set rating with color
            if (target.getRating() > 0) {
                holder.ratingTextView.setText(String.valueOf(target.getRating()));
                holder.ratingTextView.setTextColor(getRatingColor(target.getRating()));
                holder.ratingTextView.setVisibility(View.VISIBLE);
            } else {
                holder.ratingTextView.setVisibility(View.GONE);
            }

            // Handle click to open problem link
            holder.itemView.setOnClickListener(v -> {
                if (target.getProblemLink() != null && !target.getProblemLink().isEmpty()) {
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle(target.getName())
                            .setMessage("Do you want to visit this problem on Codeforces?")
                            .setPositiveButton("Visit", (dialog, which) -> {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse(target.getProblemLink()));
                                startActivity(browserIntent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return targets.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView problemNameText;
            TextView problemDateText;
            TextView ratingTextView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                problemNameText = itemView.findViewById(R.id.problemNameText);
                problemDateText = itemView.findViewById(R.id.problemDateText);
                ratingTextView = itemView.findViewById(R.id.ratingTextView);
            }
        }
    }
}
