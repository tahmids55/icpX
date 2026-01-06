package com.icpx.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.icpx.android.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Custom view to display activity heatmap similar to GitHub/Codeforces
 */
public class ActivityHeatmapView extends View {

    private static final int WEEKS_TO_SHOW = 53; // Show ~1 year
    private static final int DAYS_PER_WEEK = 7;
    private static final float CELL_SIZE_DP = 12f;
    private static final float CELL_MARGIN_DP = 2f;
    
    private Paint cellPaint;
    private Paint textPaint;
    private float cellSize;
    private float cellMargin;
    
    // Activity data: date -> activity type (0=none, 1=problem, 2=topic, 3=both)
    private Map<String, Integer> activityData = new HashMap<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public ActivityHeatmapView(Context context) {
        super(context);
        init();
    }

    public ActivityHeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ActivityHeatmapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellPaint.setStyle(Paint.Style.FILL);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(dpToPx(10));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.textSecondary));
        
        cellSize = dpToPx(CELL_SIZE_DP);
        cellMargin = dpToPx(CELL_MARGIN_DP);
    }

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    /**
     * Set activity data for the heatmap
     * @param problemDates List of dates when problems were solved
     * @param topicDates List of dates when topics were learned
     */
    public void setActivityData(Map<String, Integer> problemCount, Map<String, Integer> topicCount) {
        activityData.clear();
        
        // Combine data: 1=problem only, 2=topic only, 3=both
        for (String date : problemCount.keySet()) {
            activityData.put(date, 1);
        }
        
        for (String date : topicCount.keySet()) {
            if (activityData.containsKey(date)) {
                activityData.put(date, 3); // Both problem and topic
            } else {
                activityData.put(date, 2); // Topic only
            }
        }
        
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = (int) ((cellSize + cellMargin) * DAYS_PER_WEEK + dpToPx(20)); // +20 for month labels
        setMeasuredDimension(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, -(WEEKS_TO_SHOW - 1));
        
        // Start from Sunday of that week
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        
        float startX = dpToPx(40); // Leave space for day labels
        float startY = dpToPx(20); // Leave space for month labels
        
        String lastMonth = "";
        float monthLabelX = startX;
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        
        // Draw cells
        for (int week = 0; week < WEEKS_TO_SHOW; week++) {
            String currentMonth = "";
            for (int day = 0; day < DAYS_PER_WEEK; day++) {
                String dateStr = dateFormat.format(cal.getTime());
                
                // Check for month boundary
                currentMonth = monthFormat.format(cal.getTime());
                if (!currentMonth.equals(lastMonth) && day == 0) {
                    canvas.drawText(currentMonth, monthLabelX, startY - dpToPx(5), textPaint);
                    lastMonth = currentMonth;
                }
                
                float x = startX + week * (cellSize + cellMargin);
                float y = startY + day * (cellSize + cellMargin);
                
                // Determine color based on activity
                int activityType = activityData.getOrDefault(dateStr, 0);
                cellPaint.setColor(getColorForActivity(activityType));
                
                RectF rect = new RectF(x, y, x + cellSize, y + cellSize);
                canvas.drawRoundRect(rect, dpToPx(2), dpToPx(2), cellPaint);
                
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            if (week == 0 || (week > 0 && !currentMonth.equals(lastMonth))) {
                monthLabelX = startX + week * (cellSize + cellMargin);
            }
        }
        
        // Draw day labels (Mon, Wed, Fri)
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Mon", startX - dpToPx(5), startY + (cellSize + cellMargin) * 1 + cellSize / 2 + dpToPx(3), textPaint);
        canvas.drawText("Wed", startX - dpToPx(5), startY + (cellSize + cellMargin) * 3 + cellSize / 2 + dpToPx(3), textPaint);
        canvas.drawText("Fri", startX - dpToPx(5), startY + (cellSize + cellMargin) * 5 + cellSize / 2 + dpToPx(3), textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    /**
     * Get color based on activity type
     * 0 = No activity (light gray)
     * 1 = Problem solved (green)
     * 2 = Topic learned (red)
     * 3 = Both (orange/mixed)
     */
    private int getColorForActivity(int activityType) {
        switch (activityType) {
            case 1: // Problem solved
                return ContextCompat.getColor(getContext(), R.color.heatmapGreen);
            case 2: // Topic learned
                return ContextCompat.getColor(getContext(), R.color.heatmapRed);
            case 3: // Both
                return ContextCompat.getColor(getContext(), R.color.heatmapOrange);
            default: // No activity
                return ContextCompat.getColor(getContext(), R.color.heatmapEmpty);
        }
    }
}
