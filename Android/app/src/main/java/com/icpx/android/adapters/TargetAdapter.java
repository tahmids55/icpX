package com.icpx.android.adapters;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.icpx.android.R;
import com.icpx.android.model.Target;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapter for displaying targets
 */
public class TargetAdapter extends RecyclerView.Adapter<TargetAdapter.ViewHolder> {

    private List<Target> targets;
    private OnTargetClickListener listener;
    private OnPendingClickListener pendingClickListener;
    private OnSelectionModeListener selectionModeListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    private boolean isSelectionMode = false;
    private Set<Integer> selectedItems = new HashSet<>();
    
    private static final long LONG_PRESS_DURATION = 750; // 750ms for long press

    public interface OnTargetClickListener {
        void onTargetClick(Target target);
    }

    public interface OnPendingClickListener {
        void onPendingClick(Target target);
    }
    
    public interface OnSelectionModeListener {
        void onSelectionModeChanged(boolean isSelectionMode, int selectedCount);
    }

    public TargetAdapter(List<Target> targets, OnTargetClickListener listener) {
        this.targets = targets;
        this.listener = listener;
    }
    
    public void setOnSelectionModeListener(OnSelectionModeListener listener) {
        this.selectionModeListener = listener;
    }

    public void setOnPendingClickListener(OnPendingClickListener pendingClickListener) {
        this.pendingClickListener = pendingClickListener;
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_target, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Target target = targets.get(position);
        
        holder.nameTextView.setText(target.getName());
        holder.typeTextView.setText(target.getType());
        holder.dateTextView.setText(target.getCreatedAt() != null ? dateFormat.format(target.getCreatedAt()) : "");
        
        // Set rating with color if available
        if (target.getRating() != null && target.getRating() > 0) {
            holder.ratingTextView.setText(String.valueOf(target.getRating()));
            holder.ratingTextView.setTextColor(getRatingColor(target.getRating()));
            holder.ratingTextView.setVisibility(View.VISIBLE);
        } else {
            holder.ratingTextView.setVisibility(View.GONE);
        }
        
        // Set status
        holder.statusChip.setText(target.getStatus());
        int statusColor;
        switch (target.getStatus()) {
            case "achieved":
                statusColor = holder.itemView.getContext().getColor(R.color.statusAchieved);
                break;
            case "failed":
                statusColor = holder.itemView.getContext().getColor(R.color.statusFailed);
                break;
            default:
                statusColor = holder.itemView.getContext().getColor(R.color.statusPending);
        }
        holder.statusChip.setChipBackgroundColor(
                android.content.res.ColorStateList.valueOf(statusColor));
        holder.statusIndicator.setBackgroundColor(statusColor);
        
        // Set deadline countdown for pending items
        if (target.getDeadline() != null && !"achieved".equals(target.getStatus())) {
            long now = System.currentTimeMillis();
            long deadline = target.getDeadline().getTime();
            
            if (now < deadline) {
                // Time remaining
                long diffMs = deadline - now;
                long hours = diffMs / (1000 * 60 * 60);
                long minutes = (diffMs / (1000 * 60)) % 60;
                
                String timeText;
                int bgColor;
                if (hours >= 12) {
                    timeText = "⏰ " + hours + "h left";
                    bgColor = Color.parseColor("#4CAF50"); // Green
                } else if (hours >= 1) {
                    timeText = "⏰ " + hours + "h " + minutes + "m left";
                    bgColor = Color.parseColor("#FF9800"); // Orange
                } else {
                    timeText = "⏰ " + minutes + "m left";
                    bgColor = Color.parseColor("#F44336"); // Red
                }
                
                holder.deadlineTextView.setText(timeText);
                holder.deadlineTextView.getBackground().setTint(bgColor);
                holder.deadlineTextView.setVisibility(View.VISIBLE);
            } else {
                // Overdue
                long diffMs = now - deadline;
                long hours = diffMs / (1000 * 60 * 60);
                long minutes = (diffMs / (1000 * 60)) % 60;
                
                String timeText;
                double penalty = (hours * 60 + minutes) * 0.01;
                if (hours >= 1) {
                    timeText = String.format("⚠ %dh %dm late (-%.2f)", hours, minutes, penalty);
                } else {
                    timeText = String.format("⚠ %dm late (-%.2f)", minutes, penalty);
                }
                
                holder.deadlineTextView.setText(timeText);
                holder.deadlineTextView.getBackground().setTint(Color.parseColor("#B71C1C")); // Dark red
                holder.deadlineTextView.setVisibility(View.VISIBLE);
            }
        } else {
            holder.deadlineTextView.setVisibility(View.GONE);
        }
        
        // Selection Mode Logic
        if (isSelectionMode && "achieved".equals(target.getStatus())) {
            holder.selectionCheckBox.setVisibility(View.VISIBLE);
            holder.selectionCheckBox.setChecked(selectedItems.contains(target.getId()));
            holder.selectionCheckBox.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition < targets.size()) {
                    toggleSelection(targets.get(adapterPosition).getId());
                }
            });
        } else {
            holder.selectionCheckBox.setVisibility(View.GONE);
        }
        
        // Touch Listener Logic
        if ("achieved".equals(target.getStatus())) {
            // New Logic for Achieved: Normal click opens problem, Long Press -> Selection Mode
            holder.itemView.setOnTouchListener(null); // Clear previous touch listener
            
            holder.itemView.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= targets.size()) {
                    return;
                }
                Target currentTarget = targets.get(adapterPosition);
                
                if (isSelectionMode) {
                    toggleSelection(currentTarget.getId());
                } else {
                    // Open problem in offline/browser mode
                    if (currentTarget.getProblemLink() != null && !currentTarget.getProblemLink().isEmpty()) {
                        try {
                            android.content.Intent intent = new android.content.Intent(
                                v.getContext(), 
                                com.icpx.android.ui.ProblemTabbedActivity.class
                            );
                            intent.putExtra(
                                com.icpx.android.ui.ProblemTabbedActivity.EXTRA_PROBLEM_URL, 
                                currentTarget.getProblemLink()
                            );
                            intent.putExtra(
                                com.icpx.android.ui.ProblemTabbedActivity.EXTRA_PROBLEM_NAME, 
                                currentTarget.getName()
                            );
                            v.getContext().startActivity(intent);
                        } catch (Exception e) {
                            android.widget.Toast.makeText(v.getContext(), 
                                "Error opening problem: " + e.getMessage(), 
                                android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
            
            holder.itemView.setOnLongClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= targets.size()) {
                    return false;
                }
                Target currentTarget = targets.get(adapterPosition);
                
                if (!isSelectionMode) {
                    startSelectionMode();
                    toggleSelection(currentTarget.getId()); // Select the long-pressed item
                    return true;
                }
                return false;
            });
            
            // Allow checkbox clicking even if row click is handled
            holder.selectionCheckBox.setClickable(false); // Let row click handle it
        } else {
            // Default Logic for Pending/Wait: Maintain old behavior (Open/Delete Dialog)
            setupDefaultTouchListener(holder);
        }

        holder.pendingButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && adapterPosition < targets.size()) {
                if (pendingClickListener != null) {
                    pendingClickListener.onPendingClick(targets.get(adapterPosition));
                }
            }
        });
    }
    
    private void setupDefaultTouchListener(ViewHolder holder) {
        // Simple touch handling with long press (750ms)
        final Handler longPressHandler = new Handler(Looper.getMainLooper());
        final boolean[] isLongPressTriggered = {false};
        
        final Runnable longPressRunnable = new Runnable() {
            @Override
            public void run() {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= targets.size()) {
                    return;
                }
                Target currentTarget = targets.get(adapterPosition);
                
                isLongPressTriggered[0] = true;
                
                // Vibrate for feedback
                try {
                    android.os.Vibrator vibrator = (android.os.Vibrator) holder.itemView.getContext()
                        .getSystemService(android.content.Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(50);
                    }
                } catch (Exception e) {
                    // Ignore vibration errors
                }
                
                // Trigger the action
                if (listener != null) {
                    listener.onTargetClick(currentTarget);
                }
            }
        };
        
        holder.itemView.setOnTouchListener(new View.OnTouchListener() {
            private float startX, startY;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isLongPressTriggered[0] = false;
                        startX = event.getX();
                        startY = event.getY();
                        
                        // Simple alpha feedback
                        v.setAlpha(0.7f);
                        
                        // Start long press timer
                        longPressHandler.postDelayed(longPressRunnable, LONG_PRESS_DURATION);
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        // Cancel if finger moved too far
                        float deltaX = Math.abs(event.getX() - startX);
                        float deltaY = Math.abs(event.getY() - startY);
                        if (deltaX > 50 || deltaY > 50) {
                            longPressHandler.removeCallbacks(longPressRunnable);
                            v.setAlpha(1.0f);
                        }
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        longPressHandler.removeCallbacks(longPressRunnable);
                        v.setAlpha(1.0f);
                        
                        // Only open problem viewer if it was a normal tap
                        if (!isLongPressTriggered[0]) {
                            int adapterPosition = holder.getAdapterPosition();
                            if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= targets.size()) {
                                return true;
                            }
                            Target currentTarget = targets.get(adapterPosition);
                            
                            if (currentTarget.getProblemLink() != null && !currentTarget.getProblemLink().isEmpty()) {
                                try {
                                    android.util.Log.d("TargetAdapter", "Opening problem: " + currentTarget.getName() + " URL: " + currentTarget.getProblemLink());
                                    android.content.Intent intent = new android.content.Intent(
                                        v.getContext(), 
                                        com.icpx.android.ui.ProblemTabbedActivity.class
                                    );
                                    intent.putExtra(
                                        com.icpx.android.ui.ProblemTabbedActivity.EXTRA_PROBLEM_URL, 
                                        currentTarget.getProblemLink()
                                    );
                                    intent.putExtra(
                                        com.icpx.android.ui.ProblemTabbedActivity.EXTRA_PROBLEM_NAME, 
                                        currentTarget.getName()
                                    );
                                    v.getContext().startActivity(intent);
                                    android.util.Log.d("TargetAdapter", "Activity started successfully");
                                } catch (Exception e) {
                                    android.util.Log.e("TargetAdapter", "Error opening problem", e);
                                    android.widget.Toast.makeText(v.getContext(), 
                                        "Error opening problem: " + e.getMessage(), 
                                        android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                        return true;
                        
                    case MotionEvent.ACTION_CANCEL:
                        longPressHandler.removeCallbacks(longPressRunnable);
                        v.setAlpha(1.0f);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return targets != null ? targets.size() : 0;
    }
    
    public void startSelectionMode() {
        isSelectionMode = true;
        notifyDataSetChanged();
        if (selectionModeListener != null) {
            selectionModeListener.onSelectionModeChanged(true, selectedItems.size());
        }
    }
    
    public void stopSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        notifyDataSetChanged();
        if (selectionModeListener != null) {
            selectionModeListener.onSelectionModeChanged(false, 0);
        }
    }
    
    public void toggleSelection(int targetId) {
        if (selectedItems.contains(targetId)) {
            selectedItems.remove(targetId);
        } else {
            selectedItems.add(targetId);
        }
        notifyDataSetChanged();
        if (selectionModeListener != null) {
            selectionModeListener.onSelectionModeChanged(isSelectionMode, selectedItems.size());
        }
    }
    
    public void selectAllAchieved() {
        selectedItems.clear();
        if (targets != null) {
            for (Target t : targets) {
                if ("achieved".equals(t.getStatus())) {
                    selectedItems.add(t.getId());
                }
            }
        }
        notifyDataSetChanged();
        if (selectionModeListener != null) {
            selectionModeListener.onSelectionModeChanged(isSelectionMode, selectedItems.size());
        }
    }
    
    public Set<Integer> getSelectedItems() {
        return new HashSet<>(selectedItems);
    }
    
    public void deleteSelectedItemsAndExitMode() {
        stopSelectionMode();
    }

    public void updateData(List<Target> newTargets) {
        this.targets = newTargets != null ? newTargets : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View statusIndicator;
        TextView nameTextView;
        TextView typeTextView;
        TextView ratingTextView;
        TextView dateTextView;
        TextView deadlineTextView;
        Chip statusChip;
        MaterialButton pendingButton;
        CheckBox selectionCheckBox;

        ViewHolder(View itemView) {
            super(itemView);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            ratingTextView = itemView.findViewById(R.id.ratingTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            deadlineTextView = itemView.findViewById(R.id.deadlineTextView);
            statusChip = itemView.findViewById(R.id.statusChip);
            pendingButton = itemView.findViewById(R.id.pendingButton);
            selectionCheckBox = itemView.findViewById(R.id.selectionCheckBox);
        }
    }
}
