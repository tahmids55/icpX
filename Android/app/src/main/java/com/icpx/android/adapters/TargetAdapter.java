package com.icpx.android.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.icpx.android.R;
import com.icpx.android.model.Target;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying targets
 */
public class TargetAdapter extends RecyclerView.Adapter<TargetAdapter.ViewHolder> {

    private List<Target> targets;
    private OnTargetClickListener listener;
    private OnPendingClickListener pendingClickListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnTargetClickListener {
        void onTargetClick(Target target);
    }

    public interface OnPendingClickListener {
        void onPendingClick(Target target);
    }

    public TargetAdapter(List<Target> targets, OnTargetClickListener listener) {
        this.targets = targets;
        this.listener = listener;
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
        holder.dateTextView.setText(dateFormat.format(target.getCreatedAt()));
        
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
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTargetClick(target);
            }
        });

        holder.pendingButton.setOnClickListener(v -> {
            if (pendingClickListener != null) {
                pendingClickListener.onPendingClick(target);
            }
        });
    }

    @Override
    public int getItemCount() {
        return targets.size();
    }

    public void updateData(List<Target> newTargets) {
        this.targets = newTargets;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View statusIndicator;
        TextView nameTextView;
        TextView typeTextView;
        TextView ratingTextView;
        TextView dateTextView;
        Chip statusChip;
        MaterialButton pendingButton;

        ViewHolder(View itemView) {
            super(itemView);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            ratingTextView = itemView.findViewById(R.id.ratingTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            statusChip = itemView.findViewById(R.id.statusChip);
            pendingButton = itemView.findViewById(R.id.pendingButton);
        }
    }
}
