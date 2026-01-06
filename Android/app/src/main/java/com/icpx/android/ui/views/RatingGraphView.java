package com.icpx.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.icpx.android.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Custom view to display Codeforces-style rating graph
 */
public class RatingGraphView extends View {

    private static final float PADDING_DP = 40f;
    private static final float POINT_RADIUS_DP = 4f;
    
    private Paint linePaint;
    private Paint pointPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint axisPaint;
    
    private List<RatingPoint> ratingHistory = new ArrayList<>();
    private int minRating = 0;
    private int maxRating = 2400;
    
    public static class RatingPoint {
        public long timestamp;
        public int rating;
        public String contestName;
        
        public RatingPoint(long timestamp, int rating, String contestName) {
            this.timestamp = timestamp;
            this.rating = rating;
            this.contestName = contestName;
        }
    }

    public RatingGraphView(Context context) {
        super(context);
        init();
    }

    public RatingGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RatingGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dpToPx(2));
        
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setStyle(Paint.Style.FILL);
        
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dpToPx(1));
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.divider));
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(dpToPx(12));
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.textSecondary));
        
        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(dpToPx(2));
        axisPaint.setColor(ContextCompat.getColor(getContext(), R.color.textPrimary));
    }

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    /**
     * Set rating history data
     */
    public void setRatingHistory(List<RatingPoint> history) {
        this.ratingHistory = new ArrayList<>(history);
        
        if (!ratingHistory.isEmpty()) {
            // Calculate min and max ratings for scaling
            minRating = Integer.MAX_VALUE;
            maxRating = Integer.MIN_VALUE;
            
            for (RatingPoint point : ratingHistory) {
                minRating = Math.min(minRating, point.rating);
                maxRating = Math.max(maxRating, point.rating);
            }
            
            // Round to nearest 100 and add padding
            minRating = (minRating / 100) * 100 - 100;
            maxRating = ((maxRating / 100) + 1) * 100 + 100;
            
            // Ensure minimum range
            if (maxRating - minRating < 400) {
                int mid = (maxRating + minRating) / 2;
                minRating = mid - 200;
                maxRating = mid + 200;
            }
        }
        
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) dpToPx(280); // Increased height for date labels
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (ratingHistory.isEmpty()) {
            // Draw empty state
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No rating history available", 
                    getWidth() / 2f, 
                    getHeight() / 2f, 
                    textPaint);
            textPaint.setTextAlign(Paint.Align.LEFT);
            return;
        }

        float padding = dpToPx(PADDING_DP);
        float graphWidth = getWidth() - 2 * padding;
        float graphHeight = getHeight() - 2 * padding;

        // Draw rating threshold colored bands FIRST (background)
        drawRatingThresholds(canvas, padding, graphWidth, graphHeight);

        // Draw axes
        canvas.drawLine(padding, padding, padding, getHeight() - padding, axisPaint);
        canvas.drawLine(padding, getHeight() - padding, getWidth() - padding, getHeight() - padding, axisPaint);

        // Draw rating line
        drawRatingLine(canvas, padding, graphWidth, graphHeight);

        // Draw points
        drawRatingPoints(canvas, padding, graphWidth, graphHeight);

        // Draw Y-axis labels (ratings)
        drawYAxisLabels(canvas, padding, graphHeight);
        
        // Draw X-axis labels (dates)
        drawXAxisLabels(canvas, padding, graphWidth, graphHeight);
    }

    private void drawRatingThresholds(Canvas canvas, float padding, float graphWidth, float graphHeight) {
        // Define rating bands with their colors
        int[][] ratingBands = {
                {0, 1200, R.color.ratingNewbie},
                {1200, 1400, R.color.ratingPupil},
                {1400, 1600, R.color.ratingSpecialist},
                {1600, 1900, R.color.ratingExpert},
                {1900, 2100, R.color.ratingCandidateMaster},
                {2100, 2400, R.color.ratingMaster},
                {2400, 4000, R.color.ratingGrandmaster}
        };
        
        Paint bandPaint = new Paint();
        bandPaint.setStyle(Paint.Style.FILL);
        
        for (int[] band : ratingBands) {
            int bandMin = band[0];
            int bandMax = band[1];
            int colorRes = band[2];
            
            // Only draw if band intersects with visible range
            if (bandMax > minRating && bandMin < maxRating) {
                int visibleMin = Math.max(bandMin, minRating);
                int visibleMax = Math.min(bandMax, maxRating);
                
                float yTop = padding + graphHeight - ((visibleMax - minRating) / (float)(maxRating - minRating)) * graphHeight;
                float yBottom = padding + graphHeight - ((visibleMin - minRating) / (float)(maxRating - minRating)) * graphHeight;
                
                bandPaint.setColor(ContextCompat.getColor(getContext(), colorRes));
                bandPaint.setAlpha(40); // Semi-transparent background
                canvas.drawRect(padding, yTop, padding + graphWidth, yBottom, bandPaint);
                
                // Draw separator line at top of band
                if (bandMin >= minRating && bandMin <= maxRating) {
                    float y = padding + graphHeight - ((bandMin - minRating) / (float)(maxRating - minRating)) * graphHeight;
                    Paint separatorPaint = new Paint(gridPaint);
                    separatorPaint.setColor(ContextCompat.getColor(getContext(), colorRes));
                    separatorPaint.setAlpha(150);
                    canvas.drawLine(padding, y, padding + graphWidth, y, separatorPaint);
                }
            }
        }
    }

    private void drawRatingLine(Canvas canvas, float padding, float graphWidth, float graphHeight) {
        if (ratingHistory.size() < 2) return;

        long minTime = ratingHistory.get(0).timestamp;
        long maxTime = ratingHistory.get(ratingHistory.size() - 1).timestamp;
        long timeRange = maxTime - minTime;
        if (timeRange == 0) timeRange = 1;

        for (int i = 0; i < ratingHistory.size() - 1; i++) {
            RatingPoint current = ratingHistory.get(i);
            RatingPoint next = ratingHistory.get(i + 1);

            float x1 = padding + ((current.timestamp - minTime) / (float) timeRange) * graphWidth;
            float y1 = padding + graphHeight - ((current.rating - minRating) / (float)(maxRating - minRating)) * graphHeight;
            
            float x2 = padding + ((next.timestamp - minTime) / (float) timeRange) * graphWidth;
            float y2 = padding + graphHeight - ((next.rating - minRating) / (float)(maxRating - minRating)) * graphHeight;

            // Set line color based on rating
            linePaint.setColor(getRatingColor(next.rating));
            canvas.drawLine(x1, y1, x2, y2, linePaint);
        }
    }

    private void drawRatingPoints(Canvas canvas, float padding, float graphWidth, float graphHeight) {
        if (ratingHistory.isEmpty()) return;

        long minTime = ratingHistory.get(0).timestamp;
        long maxTime = ratingHistory.get(ratingHistory.size() - 1).timestamp;
        long timeRange = maxTime - minTime;
        if (timeRange == 0) timeRange = 1;

        float pointRadius = dpToPx(POINT_RADIUS_DP);

        for (RatingPoint point : ratingHistory) {
            float x = padding + ((point.timestamp - minTime) / (float) timeRange) * graphWidth;
            float y = padding + graphHeight - ((point.rating - minRating) / (float)(maxRating - minRating)) * graphHeight;

            pointPaint.setColor(getRatingColor(point.rating));
            canvas.drawCircle(x, y, pointRadius, pointPaint);
            
            // Draw white border
            Paint borderPaint = new Paint(pointPaint);
            borderPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(dpToPx(1.5f));
            canvas.drawCircle(x, y, pointRadius, borderPaint);
        }
    }

    private void drawYAxisLabels(Canvas canvas, float padding, float graphHeight) {
        textPaint.setTextAlign(Paint.Align.RIGHT);
        
        int step = 200;
        for (int rating = ((minRating / step) * step); rating <= maxRating; rating += step) {
            if (rating >= minRating && rating <= maxRating) {
                float y = padding + graphHeight - ((rating - minRating) / (float)(maxRating - minRating)) * graphHeight;
                canvas.drawText(String.valueOf(rating), padding - dpToPx(8), y + dpToPx(4), textPaint);
            }
        }
        
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawXAxisLabels(Canvas canvas, float padding, float graphWidth, float graphHeight) {
        if (ratingHistory.isEmpty()) return;

        long minTime = ratingHistory.get(0).timestamp;
        long maxTime = ratingHistory.get(ratingHistory.size() - 1).timestamp;
        long timeRange = maxTime - minTime;
        if (timeRange == 0) return;

        textPaint.setTextAlign(Paint.Align.CENTER);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        
        // Draw date labels at start, middle, and end
        float y = getHeight() - padding + dpToPx(20);
        
        // Start date
        canvas.drawText(dateFormat.format(new Date(minTime)), 
                padding, y, textPaint);
        
        // Middle date
        if (ratingHistory.size() > 2) {
            long midTime = (minTime + maxTime) / 2;
            float midX = padding + graphWidth / 2;
            canvas.drawText(dateFormat.format(new Date(midTime)), 
                    midX, y, textPaint);
        }
        
        // End date
        canvas.drawText(dateFormat.format(new Date(maxTime)), 
                padding + graphWidth, y, textPaint);
        
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    /**
     * Get Codeforces rating color based on rating value
     */
    private int getRatingColor(int rating) {
        if (rating < 1200) {
            return ContextCompat.getColor(getContext(), R.color.ratingNewbie);        // Gray
        } else if (rating < 1400) {
            return ContextCompat.getColor(getContext(), R.color.ratingPupil);         // Green
        } else if (rating < 1600) {
            return ContextCompat.getColor(getContext(), R.color.ratingSpecialist);    // Cyan
        } else if (rating < 1900) {
            return ContextCompat.getColor(getContext(), R.color.ratingExpert);        // Blue
        } else if (rating < 2100) {
            return ContextCompat.getColor(getContext(), R.color.ratingCandidateMaster); // Violet
        } else if (rating < 2400) {
            return ContextCompat.getColor(getContext(), R.color.ratingMaster);        // Orange
        } else {
            return ContextCompat.getColor(getContext(), R.color.ratingGrandmaster);   // Red
        }
    }
}
