package com.icpx.android.ui.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.tabs.TabLayout;
import com.icpx.android.R;
import com.icpx.android.adapters.ContestAdapter;
import com.icpx.android.model.Contest;
import com.icpx.android.service.CodeforcesService;
import com.icpx.android.service.ContestReminderWorker;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fragment for viewing Codeforces contests
 */
public class ContestsFragment extends Fragment {

    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView contestsRecyclerView;
    
    private ContestAdapter contestAdapter;
    private CodeforcesService codeforcesService;
    private String currentFilter = "upcoming";
    private List<Contest> allContests = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contests, container, false);
        
        initViews(view);
        codeforcesService = new CodeforcesService();
        
        setupTabLayout();
        setupRecyclerView();
        loadContests();
        
        return view;
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        contestsRecyclerView = view.findViewById(R.id.contestsRecyclerView);
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        currentFilter = "upcoming";
                        break;
                    case 1:
                        currentFilter = "past";
                        break;
                }
                filterContests();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        contestsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        contestAdapter = new ContestAdapter(new ArrayList<>(), this::onContestClick);
        contestsRecyclerView.setAdapter(contestAdapter);
        
        swipeRefreshLayout.setOnRefreshListener(this::loadContests);
    }

    private void loadContests() {
        swipeRefreshLayout.setRefreshing(true);
        
        new Thread(() -> {
            try {
                JSONArray contestsJson = codeforcesService.fetchContests(false);
                allContests.clear();
                
                long currentTime = System.currentTimeMillis() / 1000; // Current time in seconds
                
                for (int i = 0; i < contestsJson.length(); i++) {
                    JSONObject contestJson = contestsJson.getJSONObject(i);
                    
                    Contest contest = new Contest();
                    contest.setId(contestJson.getInt("id"));
                    contest.setName(contestJson.getString("name"));
                    contest.setType(contestJson.optString("type", ""));
                    contest.setPhase(contestJson.getString("phase"));
                    contest.setDurationSeconds(contestJson.getLong("durationSeconds"));
                    contest.setStartTimeSeconds(contestJson.optLong("startTimeSeconds", 0));
                    contest.setRelativeTimeSeconds(contestJson.optLong("relativeTimeSeconds", 0));
                    
                    allContests.add(contest);
                }
                
                requireActivity().runOnUiThread(() -> {
                    filterContests();
                    scheduleRemindersForUpcomingContests();
                    swipeRefreshLayout.setRefreshing(false);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to load contests: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        }).start();
    }

    private void filterContests() {
        List<Contest> filtered = new ArrayList<>();
        
        for (Contest contest : allContests) {
            if ("upcoming".equals(currentFilter) && (contest.isUpcoming() || contest.isRunning())) {
                filtered.add(contest);
            } else if ("past".equals(currentFilter) && contest.isFinished()) {
                filtered.add(contest);
            }
        }
        
        // Sort by start time
        Collections.sort(filtered, new Comparator<Contest>() {
            @Override
            public int compare(Contest c1, Contest c2) {
                if ("upcoming".equals(currentFilter)) {
                    // Upcoming: earliest first
                    return Long.compare(c1.getStartTimeSeconds(), c2.getStartTimeSeconds());
                } else {
                    // Past: most recent first
                    return Long.compare(c2.getStartTimeSeconds(), c1.getStartTimeSeconds());
                }
            }
        });
        
        contestAdapter.updateData(filtered);
    }

    private void onContestClick(Contest contest) {
        if (contest.isFinished()) {
            // For past contests, show options including import
            String[] options = {"Visit", "Import Contest", "Cancel"};
            new AlertDialog.Builder(requireContext())
                    .setTitle(contest.getName())
                    .setItems(options, (dialog, which) -> {
                        switch (which) {
                            case 0: // Visit
                                Intent visitIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(contest.getContestUrl()));
                                startActivity(visitIntent);
                                break;
                            case 1: // Import Contest
                                showImportContestDialog(contest);
                                break;
                            case 2: // Cancel
                                dialog.dismiss();
                                break;
                        }
                    })
                    .show();
        } else {
            // For upcoming/running contests, just show visit option
            new AlertDialog.Builder(requireContext())
                    .setTitle(contest.getName())
                    .setMessage("Do you want to visit this contest on Codeforces?")
                    .setPositiveButton("Visit", (dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(contest.getContestUrl()));
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void showImportContestDialog(Contest contest) {
        com.icpx.android.ui.dialogs.AddProblemDialog dialog = 
                new com.icpx.android.ui.dialogs.AddProblemDialog(requireContext(), target -> {
            // Add target to database
            com.google.firebase.auth.FirebaseUser currentUser = 
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(requireContext(), "Please log in", Toast.LENGTH_SHORT).show();
                return;
            }
            
            final String userEmail = currentUser.getEmail();
            android.util.Log.d("ContestsFragment", "Creating target for user: " + userEmail);
            
            new Thread(() -> {
                com.icpx.android.database.TargetDAO targetDAO = 
                        new com.icpx.android.database.TargetDAO(requireContext());
                long id = targetDAO.createTarget(target, userEmail);
                android.util.Log.d("ContestsFragment", "Created target with ID: " + id + " for user: " + userEmail);
                
                requireActivity().runOnUiThread(() -> {
                    if (id > 0) {
                        Toast.makeText(requireContext(), "Problem added to targets", 
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to add target", 
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        }, String.valueOf(contest.getId()));
        
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh contests when fragment is resumed
        if (contestAdapter != null) {
            contestAdapter.notifyDataSetChanged();
        }
    }

    private void scheduleRemindersForUpcomingContests() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", 0);
        boolean remindersEnabled = prefs.getBoolean("contest_reminders_enabled", false);
        
        if (!remindersEnabled) {
            // Cancel all existing reminders
            WorkManager.getInstance(requireContext()).cancelAllWorkByTag("contest_reminder");
            return;
        }
        
        int reminderHours = prefs.getInt("contest_reminder_hours", 24);
        long currentTime = System.currentTimeMillis() / 1000;
        
        for (Contest contest : allContests) {
            if (contest.isUpcoming()) {
                long reminderTime = contest.getStartTimeSeconds() - (reminderHours * 60 * 60);
                long delay = reminderTime - currentTime;
                
                if (delay > 0) {
                    scheduleReminder(contest, delay, reminderHours);
                }
            }
        }
    }

    private void scheduleReminder(Contest contest, long delaySeconds, int hours) {
        Data inputData = new Data.Builder()
                .putInt("contestId", contest.getId())
                .putString("contestName", contest.getName())
                .putLong("startTime", contest.getStartTimeSeconds())
                .putInt("hours", hours)
                .build();

        OneTimeWorkRequest reminderWork = new OneTimeWorkRequest.Builder(ContestReminderWorker.class)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setInputData(inputData)
                .addTag("contest_reminder")
                .addTag("contest_" + contest.getId())
                .build();

        WorkManager.getInstance(requireContext()).enqueue(reminderWork);
    }
}
