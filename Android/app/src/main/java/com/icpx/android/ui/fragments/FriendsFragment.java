package com.icpx.android.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.icpx.android.R;
import com.icpx.android.database.FriendsDAO;

import java.util.List;
import java.util.Map;

/**
 * Fragment for Friends feature
 * Allows users to add friends and view their ratings/activity
 */
public class FriendsFragment extends Fragment {

    private EditText friendEmailInput;
    private Button addFriendButton;
    private Button refreshButton;
    private LinearLayout friendsContainer;
    private ProgressBar progressBar;
    private TextView statusText;
    
    private FriendsDAO friendsDAO;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        friendsDAO = new FriendsDAO(requireContext());
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);
        
        friendEmailInput = view.findViewById(R.id.friendEmailInput);
        addFriendButton = view.findViewById(R.id.addFriendButton);
        refreshButton = view.findViewById(R.id.refreshButton);
        friendsContainer = view.findViewById(R.id.friendsContainer);
        progressBar = view.findViewById(R.id.progressBar);
        statusText = view.findViewById(R.id.statusText);
        
        addFriendButton.setOnClickListener(v -> addFriend());
        refreshButton.setOnClickListener(v -> refreshFriends());
        
        loadFriends();
        
        return view;
    }

    private void addFriend() {
        String email = friendEmailInput.getText().toString().trim();
        
        if (email.isEmpty()) {
            statusText.setText("Please enter an email address");
            return;
        }
        
        if (!email.contains("@")) {
            statusText.setText("Please enter a valid email address");
            return;
        }
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            statusText.setText("Please sign in first");
            return;
        }
        
        String currentEmail = currentUser.getEmail();
        if (email.equalsIgnoreCase(currentEmail)) {
            statusText.setText("You cannot add yourself as a friend");
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        addFriendButton.setEnabled(false);
        
        // Add locally
        boolean localSuccess = friendsDAO.addFriend(currentEmail, email);
        
        if (localSuccess) {
            String uid = currentUser.getUid();
            String friendEmailLower = email.toLowerCase();
            
            java.util.Map<String, Object> friendData = new java.util.HashMap<>();
            friendData.put("friendEmail", friendEmailLower);
            friendData.put("addedAt", System.currentTimeMillis());
            
            // Try to look up friend's UID from userProfiles first
            String emailKey = friendEmailLower.replace(".", "_").replace("@", "_at_");
            db.collection("userProfiles").document(emailKey).get()
              .addOnSuccessListener(profileDoc -> {
                  if (profileDoc.exists()) {
                      String friendUid = profileDoc.getString("uid");
                      Double friendRating = profileDoc.getDouble("rating");
                      if (friendUid != null) friendData.put("friendUid", friendUid);
                      if (friendRating != null) friendData.put("friendRating", friendRating);
                  }
                  
                  // Save friend data with or without UID
                  saveFriendData(uid, friendEmailLower, friendData, email);
              })
              .addOnFailureListener(e -> {
                  // userProfiles lookup failed, try users collection
                  db.collection("users").whereEqualTo("email", friendEmailLower).get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                            friendData.put("friendUid", userDoc.getId());
                            Double rating = userDoc.getDouble("rating");
                            if (rating != null) friendData.put("friendRating", rating);
                        }
                        saveFriendData(uid, friendEmailLower, friendData, email);
                    })
                    .addOnFailureListener(e2 -> saveFriendData(uid, friendEmailLower, friendData, email));
              });
        } else {
            progressBar.setVisibility(View.GONE);
            addFriendButton.setEnabled(true);
            statusText.setText("Friend already exists");
        }
    }
    
    private void saveFriendData(String uid, String friendEmailLower, java.util.Map<String, Object> friendData, String displayEmail) {
        db.collection("users").document(uid)
          .collection("friends").document(friendEmailLower)
          .set(friendData)
          .addOnSuccessListener(aVoid -> {
              requireActivity().runOnUiThread(() -> {
                  progressBar.setVisibility(View.GONE);
                  addFriendButton.setEnabled(true);
                  statusText.setText("Friend added: " + displayEmail);
                  friendEmailInput.setText("");
                  loadFriends();
              });
          })
          .addOnFailureListener(e -> {
              requireActivity().runOnUiThread(() -> {
                  progressBar.setVisibility(View.GONE);
                  addFriendButton.setEnabled(true);
                  statusText.setText("Added locally, sync failed");
                  loadFriends();
              });
          });
    }

    private void refreshFriends() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        refreshButton.setEnabled(false);
        
        String uid = currentUser.getUid();
        String currentEmail = currentUser.getEmail();
        
        db.collection("users").document(uid)
          .collection("friends")
          .get()
          .addOnSuccessListener(querySnapshot -> {
              for (QueryDocumentSnapshot doc : querySnapshot) {
                  String friendEmail = doc.getString("friendEmail");
                  if (friendEmail != null && currentEmail != null) {
                      friendsDAO.addFriend(currentEmail.toLowerCase(), friendEmail.toLowerCase());
                  }
              }
              
              requireActivity().runOnUiThread(() -> {
                  progressBar.setVisibility(View.GONE);
                  refreshButton.setEnabled(true);
                  statusText.setText("Synced " + querySnapshot.size() + " friends");
                  loadFriends();
              });
          })
          .addOnFailureListener(e -> {
              requireActivity().runOnUiThread(() -> {
                  progressBar.setVisibility(View.GONE);
                  refreshButton.setEnabled(true);
                  statusText.setText("Sync failed: " + e.getMessage());
              });
          });
    }

    private void loadFriends() {
        friendsContainer.removeAllViews();
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            TextView noLoginText = new TextView(requireContext());
            noLoginText.setText("Sign in to view friends");
            noLoginText.setPadding(16, 16, 16, 16);
            friendsContainer.addView(noLoginText);
            return;
        }
        
        List<String> friends = friendsDAO.getFriends(currentUser.getEmail());
        
        if (friends.isEmpty()) {
            TextView emptyText = new TextView(requireContext());
            emptyText.setText("No friends added yet.\nAdd a friend by their email address above!");
            emptyText.setPadding(32, 32, 32, 32);
            friendsContainer.addView(emptyText);
            return;
        }
        
        for (String friendEmail : friends) {
            View card = createFriendCard(friendEmail);
            friendsContainer.addView(card);
        }
    }

    private View createFriendCard(String friendEmail) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 16, 24, 16);
        card.setBackgroundResource(R.drawable.card_background);
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);
        
        // Header row
        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        TextView emailText = new TextView(requireContext());
        emailText.setText("📧 " + friendEmail);
        emailText.setTextSize(16);
        emailText.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ));
        
        Button viewBtn = new Button(requireContext());
        viewBtn.setText("View");
        viewBtn.setTextSize(12);
        viewBtn.setOnClickListener(v -> viewFriendStats(friendEmail, card));
        
        Button removeBtn = new Button(requireContext());
        removeBtn.setText("Remove");
        removeBtn.setTextSize(12);
        removeBtn.setOnClickListener(v -> removeFriend(friendEmail));
        
        headerRow.addView(emailText);
        headerRow.addView(viewBtn);
        headerRow.addView(removeBtn);
        
        card.addView(headerRow);
        
        // Stats container (will be populated on view click)
        LinearLayout statsContainer = new LinearLayout(requireContext());
        statsContainer.setOrientation(LinearLayout.VERTICAL);
        statsContainer.setTag("stats_" + friendEmail);
        card.addView(statsContainer);
        
        return card;
    }

    private void viewFriendStats(String friendEmail, View card) {
        // Find stats container in card
        LinearLayout statsContainer = card.findViewWithTag("stats_" + friendEmail);
        if (statsContainer == null) return;
        
        statsContainer.removeAllViews();
        
        ProgressBar loading = new ProgressBar(requireContext());
        statsContainer.addView(loading);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            statsContainer.removeAllViews();
            TextView errorText = new TextView(requireContext());
            errorText.setText("Please sign in first");
            statsContainer.addView(errorText);
            return;
        }
        
        String currentUid = currentUser.getUid();
        String friendEmailLower = friendEmail.toLowerCase();
        
        // FIRST: Check if we have stored friend UID in our friends collection
        db.collection("users").document(currentUid)
          .collection("friends").document(friendEmailLower)
          .get()
          .addOnSuccessListener(friendDoc -> {
              if (friendDoc.exists() && friendDoc.getString("friendUid") != null) {
                  // We have the friend's UID stored
                  String friendUid = friendDoc.getString("friendUid");
                  Double storedRating = friendDoc.getDouble("friendRating");
                  double rating = storedRating != null ? storedRating : 5.0;
                  displayFriendStats(statsContainer, friendEmail, friendUid, rating);
              } else {
                  // No stored UID, try userProfiles
                  tryUserProfilesLookup(statsContainer, friendEmail, friendEmailLower);
              }
          })
          .addOnFailureListener(e -> {
              // Couldn't read our own friends collection, try userProfiles
              tryUserProfilesLookup(statsContainer, friendEmail, friendEmailLower);
          });
    }
    
    private void tryUserProfilesLookup(LinearLayout statsContainer, String friendEmail, String friendEmailLower) {
        String emailKey = friendEmailLower.replace(".", "_").replace("@", "_at_");
        
        db.collection("userProfiles").document(emailKey)
          .get()
          .addOnSuccessListener(profileDoc -> {
              if (profileDoc.exists()) {
                  String friendUid = profileDoc.getString("uid");
                  Double rating = profileDoc.getDouble("rating");
                  if (rating == null) rating = 5.0;
                  displayFriendStats(statsContainer, friendEmail, friendUid, rating);
              } else {
                  // Fallback: try querying users collection
                  tryUsersQueryLookup(statsContainer, friendEmail, friendEmailLower);
              }
          })
          .addOnFailureListener(e -> tryUsersQueryLookup(statsContainer, friendEmail, friendEmailLower));
    }
    
    private void tryUsersQueryLookup(LinearLayout statsContainer, String friendEmail, String friendEmailLower) {
        db.collection("users")
          .whereEqualTo("email", friendEmailLower)
          .get()
          .addOnSuccessListener(querySnapshot -> {
              if (querySnapshot.isEmpty()) {
                  requireActivity().runOnUiThread(() -> {
                      statsContainer.removeAllViews();
                      TextView errorText = new TextView(requireContext());
                      errorText.setText("User not found. They need to open the app and sign in first.");
                      errorText.setPadding(8, 8, 8, 8);
                      statsContainer.addView(errorText);
                  });
                  return;
              }
              
              DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
              String friendUid = userDoc.getId();
              Double rating = userDoc.getDouble("rating");
              if (rating == null) rating = 5.0;
              
              displayFriendStats(statsContainer, friendEmail, friendUid, rating);
          })
          .addOnFailureListener(e -> {
              requireActivity().runOnUiThread(() -> {
                  statsContainer.removeAllViews();
                  TextView errorText = new TextView(requireContext());
                  errorText.setText("Error: " + e.getMessage());
                  errorText.setPadding(8, 8, 8, 8);
                  statsContainer.addView(errorText);
              });
          });
    }
    
    private void displayFriendStats(LinearLayout statsContainer, String friendEmail, String friendUid, double rating) {
        // Get total solved count if we have UID
        if (friendUid != null) {
            db.collection("users").document(friendUid)
              .collection("targets")
              .whereEqualTo("status", "achieved")
              .get()
              .addOnSuccessListener(targetsQuery -> {
                  int totalSolved = targetsQuery.size();
                  showStatsUI(statsContainer, rating, totalSolved);
              })
              .addOnFailureListener(e -> {
                  // Permission denied for targets, just show rating
                  showStatsUI(statsContainer, rating, -1);
              });
        } else {
            showStatsUI(statsContainer, rating, -1);
        }
    }
    
    private void showStatsUI(LinearLayout statsContainer, double rating, int totalSolved) {
        requireActivity().runOnUiThread(() -> {
            statsContainer.removeAllViews();
            
            // Rating text
            TextView ratingText = new TextView(requireContext());
            ratingText.setText(String.format(java.util.Locale.US, "🏆 Rating: %.1f / 10", rating));
            ratingText.setTextSize(18);
            ratingText.setPadding(0, 8, 0, 4);
            statsContainer.addView(ratingText);
            
            // Rating bar chart (visual representation)
            LinearLayout barContainer = new LinearLayout(requireContext());
            barContainer.setOrientation(LinearLayout.HORIZONTAL);
            barContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            barContainer.setPadding(0, 4, 0, 8);
            
            // Calculate bar widths (max 300dp, scale 1-10)
            int maxBarWidth = 300;
            double clampedRating = Math.min(10.0, Math.max(0.0, rating));
            int fillWidth = (int) (clampedRating / 10.0 * maxBarWidth);
            int emptyWidth = maxBarWidth - fillWidth;
            
            // Filled portion (green gradient effect)
            View filledBar = new View(requireContext());
            LinearLayout.LayoutParams filledParams = new LinearLayout.LayoutParams(
                (int) (fillWidth * getResources().getDisplayMetrics().density), 
                (int) (16 * getResources().getDisplayMetrics().density)
            );
            filledBar.setLayoutParams(filledParams);
            filledBar.setBackgroundColor(0xFF4CAF50); // Green
            barContainer.addView(filledBar);
            
            // Empty portion (gray)
            View emptyBar = new View(requireContext());
            LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(
                (int) (emptyWidth * getResources().getDisplayMetrics().density),
                (int) (16 * getResources().getDisplayMetrics().density)
            );
            emptyBar.setLayoutParams(emptyParams);
            emptyBar.setBackgroundColor(0xFF333333); // Dark gray
            barContainer.addView(emptyBar);
            
            statsContainer.addView(barContainer);
            
            // Total solved (if available)
            if (totalSolved >= 0) {
                TextView solvedText = new TextView(requireContext());
                solvedText.setText("✓ Total Solved: " + totalSolved);
                solvedText.setTextSize(14);
                solvedText.setPadding(0, 4, 0, 8);
                statsContainer.addView(solvedText);
            } else {
                TextView noteText = new TextView(requireContext());
                noteText.setText("(Detailed stats not available)");
                noteText.setTextSize(12);
                noteText.setPadding(0, 4, 0, 8);
                statsContainer.addView(noteText);
            }
        });
    }

    private void removeFriend(String friendEmail) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) return;
        
        new AlertDialog.Builder(requireContext())
            .setTitle("Remove Friend")
            .setMessage("Remove " + friendEmail + "?")
            .setPositiveButton("Remove", (dialog, which) -> {
                String currentEmail = currentUser.getEmail();
                
                // Remove locally
                friendsDAO.removeFriend(currentEmail, friendEmail);
                
                // Remove from Firebase
                String uid = currentUser.getUid();
                db.collection("users").document(uid)
                  .collection("friends").document(friendEmail.toLowerCase())
                  .delete()
                  .addOnSuccessListener(aVoid -> {
                      requireActivity().runOnUiThread(() -> {
                          statusText.setText("Friend removed: " + friendEmail);
                          loadFriends();
                      });
                  })
                  .addOnFailureListener(e -> {
                      requireActivity().runOnUiThread(() -> {
                          statusText.setText("Removed locally, sync failed");
                          loadFriends();
                      });
                  });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
