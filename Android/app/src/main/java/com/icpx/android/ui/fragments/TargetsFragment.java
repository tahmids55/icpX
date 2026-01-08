package com.icpx.android.ui.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.icpx.android.R;
import com.icpx.android.adapters.TargetAdapter;
import com.icpx.android.database.TargetDAO;
import com.icpx.android.firebase.FirebaseManager;
import com.icpx.android.model.Target;
import com.icpx.android.service.CodeforcesService;
import com.icpx.android.ui.dialogs.AddProblemDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fragment for managing targets
 */
public class TargetsFragment extends Fragment {

    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView targetsRecyclerView;
    private FloatingActionButton fab;
    
    // Selection Toolbar
    private android.widget.LinearLayout selectionToolbar;
    private android.widget.CheckBox selectAllCheckBox;
    private android.widget.TextView selectionCountText;
    private android.widget.ImageButton closeSelectionButton;
    private android.widget.ImageButton deleteSelectedButton;
    
    private TargetAdapter targetAdapter;
    private TargetDAO targetDAO;
    private String currentFilter = "all";
    private CodeforcesService codeforcesService;
    private android.app.ProgressDialog progressDialog;
    private boolean duplicatesRemoved = false; // Track if duplicates have been removed

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_targets, container, false);
        
        initViews(view);
        targetDAO = new TargetDAO(requireContext());
        codeforcesService = new CodeforcesService();
        
        setupTabLayout();
        setupRecyclerView();
        setupFab();
        loadTargets();
        
        return view;
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tabLayout);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        targetsRecyclerView = view.findViewById(R.id.targetsRecyclerView);
        fab = view.findViewById(R.id.fab);
        
        selectionToolbar = view.findViewById(R.id.selectionToolbar);
        selectAllCheckBox = view.findViewById(R.id.selectAllCheckBox);
        selectionCountText = view.findViewById(R.id.selectionCountText);
        closeSelectionButton = view.findViewById(R.id.closeSelectionButton);
        deleteSelectedButton = view.findViewById(R.id.deleteSelectedButton);
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Exit selection mode when switching tabs
                if (targetAdapter != null) {
                    targetAdapter.stopSelectionMode();
                }
                
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "pending";
                        break;
                    case 2:
                        currentFilter = "achieved";
                        break;
                }
                loadTargets();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSelectionListeners() {
        // Selection Mode Listener
        targetAdapter.setOnSelectionModeListener((isSelectionMode, selectedCount) -> {
            if (isSelectionMode) {
                selectionToolbar.setVisibility(View.VISIBLE);
                fab.hide();
                selectionCountText.setText(selectedCount + " selected");
            } else {
                selectionToolbar.setVisibility(View.GONE);
                fab.show();
                selectAllCheckBox.setChecked(false);
            }
        });
        
        // Toolbar Listeners
        selectAllCheckBox.setOnClickListener(v -> {
            if (selectAllCheckBox.isChecked()) {
                targetAdapter.selectAllAchieved();
            } else {
                if (targetAdapter != null) {
                   targetAdapter.stopSelectionMode(); 
                   targetAdapter.startSelectionMode(); 
                }
            }
        });
        
        closeSelectionButton.setOnClickListener(v -> targetAdapter.stopSelectionMode());
        
        deleteSelectedButton.setOnClickListener(v -> {
            java.util.Set<Integer> selectedIds = targetAdapter.getSelectedItems();
            if (selectedIds.isEmpty()) return;
            
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Selected")
                .setMessage("Delete " + selectedIds.size() + " items?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    new Thread(() -> {
                        for (int id : selectedIds) {
                             Target t = targetDAO.getTargetById(id);
                             if (t != null) {
                                // Delete associated PDF file
                                deletePdfFile(t.getName());
                                
                                // Soft or Hard delete
                                if ("achieved".equals(t.getStatus())) {
                                    t.setDeleted(true);
                                    targetDAO.updateTarget(t);
                                } else {
                                    targetDAO.deleteTarget(t.getId());
                                }
                                
                                // Sync deletion
                                if (currentUser != null) {
                                    FirebaseManager.getInstance().deleteTarget(currentUser.getUid(), 
                                            String.valueOf(t.getId()), 
                                            new FirebaseManager.FirestoreCallback() {
                                                @Override
                                                public void onSuccess() {}
                                                
                                                @Override
                                                public void onFailure(Exception e) {}
                                            });
                                }
                             }
                        }
                        
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Selected items deleted", Toast.LENGTH_SHORT).show();
                            targetAdapter.stopSelectionMode();
                            loadTargets();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }

    private void setupRecyclerView() {
        targetsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        targetAdapter = new TargetAdapter(new ArrayList<>(), this::onTargetClick);
        targetAdapter.setOnPendingClickListener(this::onPendingClick);
        targetsRecyclerView.setAdapter(targetAdapter);
        
        setupSelectionListeners();
        
        swipeRefreshLayout.setOnRefreshListener(this::loadTargets);
    }

    private void setupFab() {
        fab.setOnClickListener(v -> showAddProblemDialog());
    }

    private void loadTargets() {
        swipeRefreshLayout.setRefreshing(true);
        
        // Get current user email
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            requireActivity().runOnUiThread(() -> {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(requireContext(), "Please log in", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        
        final String userEmail = currentUser.getEmail();
        android.util.Log.d("TargetsFragment", "Loading targets for user: " + userEmail);
        
        new Thread(() -> {
            // Always remove duplicates on every load
            int removed = targetDAO.removeDuplicateTargets(userEmail);
            if (removed > 0) {
                android.util.Log.i("TargetsFragment", "Removed " + removed + " duplicate targets");
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(requireContext(), "Cleaned up " + removed + " duplicate entries", Toast.LENGTH_LONG).show()
                );
            }
            
            List<Target> targets;
            
            if ("all".equals(currentFilter)) {
                targets = targetDAO.getAllTargets(userEmail);
                android.util.Log.d("TargetsFragment", "Loaded " + targets.size() + " targets for " + userEmail);
            } else {
                targets = targetDAO.getTargetsByStatus(currentFilter, userEmail);
            }

            requireActivity().runOnUiThread(() -> {
                targetAdapter.updateData(targets);
                swipeRefreshLayout.setRefreshing(false);
            });
        }).start();
    }

    private void onTargetClick(Target target) {
        // Show simple dialog with Delete and Open Link
        String[] options;
        if (target.getProblemLink() != null && !target.getProblemLink().isEmpty()) {
            options = new String[]{"Delete", "Open Link"};
        } else {
            options = new String[]{"Delete"};
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(target.getName())
                .setItems(options, (dialog, which) -> {
                    if (options[which].equals("Delete")) {
                        deleteTarget(target);
                    } else if (options[which].equals("Open Link")) {
                        android.content.Intent browserIntent = new android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(target.getProblemLink()));
                        startActivity(browserIntent);
                    }
                })
                .show();
    }

    private void deleteTarget(Target target) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Target")
                .setMessage("Are you sure you want to delete \"" + target.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(() -> {
                        // Delete associated PDF file
                        deletePdfFile(target.getName());
                        
                        // Use soft delete for achieved targets to keep them in history
                        if ("achieved".equals(target.getStatus())) {
                            target.setDeleted(true);
                            targetDAO.updateTarget(target);
                        } else {
                            // Hard delete for non-achieved targets
                            targetDAO.deleteTarget(target.getId());
                        }
                        
                        // Auto-sync deletion to Firebase
                        if (currentUser != null) {
                            FirebaseManager.getInstance().deleteTarget(currentUser.getUid(), 
                                    String.valueOf(target.getId()), 
                                    new FirebaseManager.FirestoreCallback() {
                                        @Override
                                        public void onSuccess() {}
                                        
                                        @Override
                                        public void onFailure(Exception e) {}
                                    });
                        }
                        
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Target deleted", Toast.LENGTH_SHORT).show();
                            loadTargets();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deletePdfFile(String problemName) {
        try {
            File dir = new File(requireContext().getFilesDir(), "problems");
            if (!dir.exists()) return;
            
            String sanitizedName = (problemName != null ? problemName : "problem")
                .replaceAll("[^a-zA-Z0-9.-]", "_");
            File pdfFile = new File(dir, sanitizedName + ".pdf");
            
            if (pdfFile.exists()) {
                boolean deleted = pdfFile.delete();
                android.util.Log.d("TargetsFragment", "PDF deleted: " + deleted + " - " + pdfFile.getName());
            }
        } catch (Exception e) {
            android.util.Log.e("TargetsFragment", "Error deleting PDF file", e);
        }
    }

    private void onPendingClick(Target target) {
        // This does the Codeforces verification (old onTargetClick behavior)
        // Skip topics, only check problems
        if (!"problem".equals(target.getType())) {
            Toast.makeText(requireContext(), "This is a topic, not a problem", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get Codeforces handle from user-specific SharedPreferences
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(requireContext(), "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }
        String prefKey = "user_prefs_" + currentUser.getEmail();
        String cfHandle = requireActivity().getSharedPreferences(prefKey, 0)
                .getString("codeforces_handle", "");

        if (cfHandle.isEmpty()) {
            // Ask for Codeforces handle
            showCodeforcesHandleDialog(target);
            return;
        }

        // Auto-check if problem is solved
        checkProblemStatus(target, cfHandle);
    }

    private void showCodeforcesHandleDialog(Target target) {
        android.view.View dialogView = LayoutInflater.from(requireContext())
                .inflate(android.R.layout.simple_list_item_1, null);
        
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Enter your Codeforces handle");
        input.setPadding(50, 30, 50, 30);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Codeforces Handle Required")
                .setMessage("To automatically check if problems are solved, please enter your Codeforces handle:")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String handle = input.getText().toString().trim();
                    if (!handle.isEmpty()) {
                        // Save handle
                        requireActivity().getSharedPreferences("user_prefs", 0)
                                .edit()
                                .putString("codeforces_handle", handle)
                                .apply();
                        // Check problem
                        checkProblemStatus(target, handle);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkProblemStatus(Target target, String cfHandle) {
        // Extract contest ID and problem index from link
        String link = target.getProblemLink();
        if (link == null || link.isEmpty()) {
            Toast.makeText(requireContext(), "No problem link available", Toast.LENGTH_SHORT).show();
            return;
        }

        Pattern pattern = Pattern.compile("codeforces\\.com/(?:problemset/problem|contest)/(\\d+)/(?:problem/)?([A-Z]\\d*)");
        Matcher matcher = pattern.matcher(link);

        if (!matcher.find()) {
            Toast.makeText(requireContext(), "Invalid Codeforces link", Toast.LENGTH_SHORT).show();
            return;
        }

        String contestId = matcher.group(1);
        String problemIndex = matcher.group(2);

        // Show progress dialog
        progressDialog = new android.app.ProgressDialog(requireContext());
        progressDialog.setMessage("Checking Codeforces...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Check in background
        new Thread(() -> {
            boolean isSolved = codeforcesService.isProblemSolved(cfHandle, contestId, problemIndex);

            requireActivity().runOnUiThread(() -> {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                if (isSolved) {
                    // Automatically mark as solved
                    if (!"achieved".equals(target.getStatus())) {
                        updateTargetStatus(target, "achieved");
                        Toast.makeText(requireContext(), "✓ Problem is solved on Codeforces! Auto-marked as achieved.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "✓ Already marked as solved!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Not solved yet - just show message, no popup
                    Toast.makeText(requireContext(), "❌ Problem not solved yet on Codeforces", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showAddProblemDialog() {
        AddProblemDialog dialog = new AddProblemDialog(requireContext(), target -> {
            // Add target to database
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(requireContext(), "Please log in", Toast.LENGTH_SHORT).show();
                return;
            }
            
            final String userEmail = currentUser.getEmail();
            android.util.Log.d("TargetsFragment", "Creating target for user: " + userEmail);
            new Thread(() -> {
                long id = targetDAO.createTarget(target, userEmail);
                
                final boolean isDuplicate = (id > 0 && targetDAO.getTargetById((int)id) != null);
                
                requireActivity().runOnUiThread(() -> {
                    if (id > 0) {
                        if (isDuplicate) {
                            Toast.makeText(requireContext(), "This problem already exists in your targets", 
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), R.string.target_added, 
                                    Toast.LENGTH_SHORT).show();
                        }
                        loadTargets();
                        
                        // Auto-sync to Firebase only if not duplicate
                        if (!isDuplicate) {
                            syncTargetToFirebase(target, currentUser.getUid());
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to add target", 
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });
        dialog.show();
    }

    private void updateTargetStatus(Target target, String newStatus) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        new Thread(() -> {
            target.setStatus(newStatus);
            targetDAO.updateTarget(target);
            
            // Auto-sync to Firebase
            if (currentUser != null) {
                syncTargetToFirebase(target, currentUser.getUid());
            }
            
            requireActivity().runOnUiThread(() -> {
                String message = "";
                switch (newStatus) {
                    case "achieved":
                        message = "Marked as Solved! 🎉";
                        break;
                    case "failed":
                        message = "Marked as Failed";
                        break;
                    case "pending":
                        message = "Marked as Pending";
                        break;
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                loadTargets();
            });
        }).start();
    }
    
    /**
     * Helper method to sync a target to Firebase
     */
    private void syncTargetToFirebase(Target target, String userId) {
        java.util.Map<String, Object> targetData = new java.util.HashMap<>();
        targetData.put("id", String.valueOf(target.getId()));
        targetData.put("type", target.getType());
        targetData.put("name", target.getName());
        targetData.put("problemLink", target.getProblemLink());
        targetData.put("topicName", target.getTopicName());
        targetData.put("websiteUrl", target.getWebsiteUrl());
        targetData.put("status", target.getStatus());
        targetData.put("rating", target.getRating());
        targetData.put("deleted", target.isDeleted());
        targetData.put("createdAt", target.getCreatedAt().getTime());
        
        FirebaseManager.getInstance().saveTarget(userId, targetData, 
            new FirebaseManager.FirestoreCallback() {
                @Override
                public void onSuccess() {
                    // Silently succeed
                }
                
                @Override
                public void onFailure(Exception e) {
                    // Silently fail, user can manually sync later if needed
                }
            });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTargets();
    }
}
