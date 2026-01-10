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
 * Custom view to display monthly activity heatmap
 * Shows current month by default, 7 columns (Fri-Thu), 4-5 rows
 */
public class ActivityHeatmapView extends View {

    private static final int DAYS_PER_WEEK = 7;
    private static final float CELL_SIZE_DP = 26f;
    private static final float CELL_MARGIN_DP = 5f;
    
    private Paint cellPaint;
    private Paint textPaint;
    private float cellSize;
    private float cellMargin;
    
    // Activity data: date -> activity type (0=none, 1=problem, 2=topic, 3=both)
    private Map<String, Integer> activityData = new HashMap<>();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    
    // Current month offset (0 = current month, -1 = last month, etc.)
    private int monthOffset = 0;

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
        textPaint.setTextSize(dpToPx(12));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.textSecondary));
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        cellSize = dpToPx(CELL_SIZE_DP);
        cellMargin = dpToPx(CELL_MARGIN_DP);
    }

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    /**
     * Set activity data for the heatmap
     * @param problemCount Map of dates when problems were solved
     * @param topicCount Map of dates when topics were learned
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
    
    /**
     * Set month offset (0 = current month, -1 = last month, etc.)
     */
    public void setMonthOffset(int offset) {
        this.monthOffset = offset;
        invalidate();
    }
    
    /**
     * Get current month offset
     */
    public int getMonthOffset() {
        return monthOffset;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Height for 5 rows max + day labels + spacing
        int height = (int) ((cellSize + cellMargin) * 5 + dpToPx(40));
        // Use full width for centering
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Get the first day of the target month
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, monthOffset);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        
        // Find the first Friday before or on the 1st of the month
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        
        // Calculate total grid width and center it
        float gridWidth = (cellSize + cellMargin) * DAYS_PER_WEEK - cellMargin;
        float startX = (getWidth() - gridWidth) / 2;
        float startY = dpToPx(20); // Space for header
        
        // Draw day labels (Fri, Sat, Sun, Mon, Tue, Wed, Thu)
        String[] dayLabels = {"Fri", "Sat", "Sun", "Mon", "Tue", "Wed", "Thu"};
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < DAYS_PER_WEEK; i++) {
            float x = startX + i * (cellSize + cellMargin) + cellSize / 2;
            canvas.drawText(dayLabels[i], x, startY, textPaint);
        }
        
        startY += dpToPx(8); // Add spacing after labels
        
        // Draw cells for approximately 5 weeks (35 days)
        int totalDays = 35;
        int currentRow = 0;
        int currentCol = 0;
        
        Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        datePaint.setTextSize(dpToPx(11));
        datePaint.setTextAlign(Paint.Align.CENTER);
        
        for (int i = 0; i < totalDays; i++) {
            String dateStr = dateFormat.format(cal.getTime());
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            
            float x = startX + currentCol * (cellSize + cellMargin);
            float y = startY + currentRow * (cellSize + cellMargin);
            
            // Determine color based on activity
            int activityType = activityData.getOrDefault(dateStr, 0);
            cellPaint.setColor(getColorForActivity(activityType));
            
            RectF rect = new RectF(x, y, x + cellSize, y + cellSize);
            canvas.drawRoundRect(rect, dpToPx(2), dpToPx(2), cellPaint);
            
            // Draw date number inside the cell
            // Use dark text for light backgrounds, light text for dark backgrounds
            if (activityType == 0) {
                datePaint.setColor(ContextCompat.getColor(getContext(), R.color.textPrimary));
            } else {
                datePaint.setColor(0xFFFFFFFF); // White text for colored cells
            }
            
            float textX = x + cellSize / 2;
            float textY = y + cellSize / 2 + dpToPx(3); // Center vertically
            canvas.drawText(String.valueOf(dayOfMonth), textX, textY, datePaint);
            
            currentCol++;
            if (currentCol >= DAYS_PER_WEEK) {
                currentCol = 0;
                currentRow++;
            }
            
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
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
