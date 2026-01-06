package com.icpx.android.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.icpx.android.R;
import com.icpx.android.model.StatCard;

import java.util.List;

/**
 * Adapter for displaying stat cards
 */
public class StatCardAdapter extends RecyclerView.Adapter<StatCardAdapter.ViewHolder> {

    private List<StatCard> statCards;

    public StatCardAdapter(List<StatCard> statCards) {
        this.statCards = statCards;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stat_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StatCard statCard = statCards.get(position);
        
        holder.titleTextView.setText(statCard.getTitle());
        holder.valueTextView.setText(statCard.getValue());
        holder.iconTextView.setText(statCard.getIcon());
        
        // Set icon background color
        int color = holder.itemView.getContext().getColor(statCard.getColorResId());
        holder.iconTextView.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return statCards.size();
    }

    public void updateData(List<StatCard> newStatCards) {
        this.statCards = newStatCards;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView iconTextView;
        TextView valueTextView;
        TextView titleTextView;

        ViewHolder(View itemView) {
            super(itemView);
            iconTextView = itemView.findViewById(R.id.iconTextView);
            valueTextView = itemView.findViewById(R.id.valueTextView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
        }
    }
}
