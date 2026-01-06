package com.icpx.android.adapters;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.icpx.android.R;
import com.icpx.android.model.Contest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for displaying contests
 */
public class ContestAdapter extends RecyclerView.Adapter<ContestAdapter.ContestViewHolder> {

    private List<Contest> contests;
    private OnContestClickListener listener;
    private Handler handler = new Handler();

    public interface OnContestClickListener {
        void onContestClick(Contest contest);
    }

    public ContestAdapter(List<Contest> contests, OnContestClickListener listener) {
        this.contests = contests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contest, parent, false);
        return new ContestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContestViewHolder holder, int position) {
        Contest contest = contests.get(position);
        holder.bind(contest);
    }

    @Override
    public int getItemCount() {
        return contests.size();
    }

    public void updateData(List<Contest> newContests) {
        this.contests = newContests;
        notifyDataSetChanged();
    }

    class ContestViewHolder extends RecyclerView.ViewHolder {
        private TextView contestName;
        private TextView contestType;
        private TextView contestDate;
        private TextView contestTimer;
        private TextView contestStatus;
        private Runnable timerRunnable;

        public ContestViewHolder(@NonNull View itemView) {
            super(itemView);
            contestName = itemView.findViewById(R.id.contestName);
            contestType = itemView.findViewById(R.id.contestType);
            contestDate = itemView.findViewById(R.id.contestDate);
            contestTimer = itemView.findViewById(R.id.contestTimer);
            contestStatus = itemView.findViewById(R.id.contestStatus);
        }

        public void bind(Contest contest) {
            contestName.setText(contest.getName());
            contestType.setText(contest.getType());

            // Format date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy  HH:mm", Locale.getDefault());
            Date startDate = new Date(contest.getStartTimeSeconds() * 1000);
            contestDate.setText(dateFormat.format(startDate));

            // Set status
            if (contest.isUpcoming()) {
                contestStatus.setText("Upcoming");
                contestStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.statOrange));
                
                // Show timer for upcoming contests
                startCountdownTimer(contest);
                contestTimer.setVisibility(View.VISIBLE);
            } else if (contest.isRunning()) {
                contestStatus.setText("Running");
                contestStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.statGreen));
                
                // Show remaining time
                startCountdownTimer(contest);
                contestTimer.setVisibility(View.VISIBLE);
            } else {
                contestStatus.setText("Finished");
                contestStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.textSecondary));
                contestTimer.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onContestClick(contest);
                }
            });
        }

        private void startCountdownTimer(Contest contest) {
            // Remove previous timer if exists
            if (timerRunnable != null) {
                handler.removeCallbacks(timerRunnable);
            }

            timerRunnable = new Runnable() {
                @Override
                public void run() {
                    long currentTime = System.currentTimeMillis() / 1000;
                    long timeDiff;
                    String prefix;

                    if (contest.isUpcoming()) {
                        timeDiff = contest.getStartTimeSeconds() - currentTime;
                        prefix = "Starts in: ";
                    } else {
                        // Running contest - show remaining time
                        long endTime = contest.getStartTimeSeconds() + contest.getDurationSeconds();
                        timeDiff = endTime - currentTime;
                        prefix = "Ends in: ";
                    }

                    if (timeDiff > 0) {
                        long days = TimeUnit.SECONDS.toDays(timeDiff);
                        long hours = TimeUnit.SECONDS.toHours(timeDiff) % 24;
                        long minutes = TimeUnit.SECONDS.toMinutes(timeDiff) % 60;
                        long seconds = timeDiff % 60;

                        String timeString;
                        if (days > 0) {
                            timeString = String.format(Locale.getDefault(), 
                                    "%dd %02dh %02dm", days, hours, minutes);
                        } else if (hours > 0) {
                            timeString = String.format(Locale.getDefault(), 
                                    "%02dh %02dm %02ds", hours, minutes, seconds);
                        } else {
                            timeString = String.format(Locale.getDefault(), 
                                    "%02dm %02ds", minutes, seconds);
                        }

                        contestTimer.setText(prefix + timeString);
                        
                        // Update every second
                        handler.postDelayed(this, 1000);
                    } else {
                        contestTimer.setText("Contest started / ended");
                    }
                }
            };

            handler.post(timerRunnable);
        }
    }
}
