package com.icpx.android.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.icpx.android.R;

/**
 * Custom view to display a personal rating bar from 0-10
 * with Codeforces-style colored background bands
 */
public class PersonalRatingBarView extends View {

    private static final float PADDING_DP = 16f;
    private static final float BAR_WIDTH_DP = 60f;
    private static final float LABEL_TEXT_SIZE_DP = 12f;
    private static final float RATING_TEXT_SIZE_DP = 16f;
    
    private Paint backgroundPaint;
    private Paint barPaint;
    private Paint labelPaint;
    private Paint ratingTextPaint;
    private Paint axisPaint;
    
    private float currentRating = 5.0f; // Default rating
    
    // Rating bands: min, max, color
    private static class RatingBand {
        float min;
        float max;
        int color;
        String label;
        
        RatingBand(float min, float max, int color, String label) {
            this.min = min;
            this.max = max;
            this.color = color;
            this.label = label;
        }
    }
    
    private RatingBand[] ratingBands;

    public PersonalRatingBarView(Context context) {
        super(context);
        init();
    }

    public PersonalRatingBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PersonalRatingBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Initialize paints
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);
        
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(dpToPx(LABEL_TEXT_SIZE_DP));
        labelPaint.setColor(ContextCompat.getColor(getContext(), R.color.textSecondary));
        
        ratingTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ratingTextPaint.setTextSize(dpToPx(RATING_TEXT_SIZE_DP));
        ratingTextPaint.setColor(ContextCompat.getColor(getContext(), R.color.textPrimary));
        ratingTextPaint.setTextAlign(Paint.Align.CENTER);
        ratingTextPaint.setFakeBoldText(true);
        
        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setStyle(Paint.Style.STROKE);
        axisPaint.setStrokeWidth(dpToPx(2));
        axisPaint.setColor(ContextCompat.getColor(getContext(), R.color.textPrimary));
        
        // Initialize rating bands
        ratingBands = new RatingBand[]{
            new RatingBand(0f, 2f, ContextCompat.getColor(getContext(), R.color.ratingBarNewbie), "Newbie"),
            new RatingBand(2f, 4f, ContextCompat.getColor(getContext(), R.color.ratingBarPupil), "Pupil"),
            new RatingBand(4f, 6f, ContextCompat.getColor(getContext(), R.color.ratingBarSpecialist), "Specialist"),
            new RatingBand(6f, 7f, ContextCompat.getColor(getContext(), R.color.ratingBarExpert), "Expert"),
            new RatingBand(7f, 8f, ContextCompat.getColor(getContext(), R.color.ratingBarCandidateMaster), "Candidate Master"),
            new RatingBand(8f, 8.5f, ContextCompat.getColor(getContext(), R.color.ratingBarMaster), "Master"),
            new RatingBand(8.5f, 9f, ContextCompat.getColor(getContext(), R.color.ratingBarGrandMaster), "Grand Master"),
            new RatingBand(9f, 9.5f, ContextCompat.getColor(getContext(), R.color.ratingBarKing), "King"),
            new RatingBand(9.5f, 10f, ContextCompat.getColor(getContext(), R.color.ratingBarEternity), "Eternity")
        };
    }

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    /**
     * Set the current rating (0-10)
     */
    public void setRating(float rating) {
        this.currentRating = Math.max(0f, Math.min(10f, rating));
        invalidate();
    }
    
    /**
     * Get the current rating
     */
    public float getRating() {
        return currentRating;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) dpToPx(400); // Fixed height for the bar
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float padding = dpToPx(PADDING_DP);
        float barWidth = dpToPx(BAR_WIDTH_DP);
        float graphHeight = getHeight() - 2 * padding;
        
        // Center the bar horizontally
        float barLeft = (getWidth() - barWidth) / 2f;
        float barRight = barLeft + barWidth;

        // Draw colored background bands
        drawBackgroundBands(canvas, barLeft, barRight, padding, graphHeight);
        
        // Draw Y-axis with labels
        drawYAxis(canvas, barLeft - dpToPx(80), padding, graphHeight);
        
        // Draw the rating bar
        drawRatingBar(canvas, barLeft, barRight, padding, graphHeight);
        
        // Draw rating labels on the right
        drawRatingLabels(canvas, barRight + dpToPx(12), padding, graphHeight);
        
        // Draw current rating value on top of bar
        drawCurrentRatingText(canvas, barLeft, barRight, padding, graphHeight);
    }

    private void drawBackgroundBands(Canvas canvas, float barLeft, float barRight, 
                                      float padding, float graphHeight) {
        for (RatingBand band : ratingBands) {
            float yTop = padding + graphHeight - (band.max / 10f) * graphHeight;
            float yBottom = padding + graphHeight - (band.min / 10f) * graphHeight;
            
            // Special gradient for Eternity band
            if (band.label.equals("Eternity")) {
                Paint gradientPaint = new Paint(backgroundPaint);
                LinearGradient gradient = new LinearGradient(
                    barLeft, yTop, barLeft, yBottom,
                    new int[]{
                        ContextCompat.getColor(getContext(), R.color.ratingBarEternity),
                        ContextCompat.getColor(getContext(), R.color.ratingBarEternityGradient),
                        Color.parseColor("#9D00FF"), // Purple
                        Color.parseColor("#FF00FF")  // Magenta
                    },
                    null,
                    Shader.TileMode.CLAMP
                );
                gradientPaint.setShader(gradient);
                gradientPaint.setAlpha(60); // Semi-transparent
                canvas.drawRect(barLeft, yTop, barRight, yBottom, gradientPaint);
            } else {
                backgroundPaint.setColor(band.color);
                backgroundPaint.setAlpha(60); // Semi-transparent
                canvas.drawRect(barLeft, yTop, barRight, yBottom, backgroundPaint);
            }
            
            // Draw separator line
            Paint separatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            separatorPaint.setColor(band.color);
            separatorPaint.setStrokeWidth(dpToPx(1));
            separatorPaint.setAlpha(200);
            canvas.drawLine(barLeft, yTop, barRight, yTop, separatorPaint);
        }
    }

    private void drawYAxis(Canvas canvas, float x, float padding, float graphHeight) {
        // Draw Y-axis line
        canvas.drawLine(x, padding, x, padding + graphHeight, axisPaint);
        
        // Draw tick marks and labels for each rating value
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        
        for (int i = 0; i <= 10; i++) {
            float y = padding + graphHeight - (i / 10f) * graphHeight;
            
            // Draw tick mark
            canvas.drawLine(x - dpToPx(5), y, x, y, axisPaint);
            
            // Draw label
            canvas.drawText(String.valueOf(i), x - dpToPx(10), y + dpToPx(4), labelPaint);
        }
        
        labelPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawRatingBar(Canvas canvas, float barLeft, float barRight, 
                                float padding, float graphHeight) {
        // Calculate bar height based on current rating
        float barHeight = (currentRating / 10f) * graphHeight;
        float barTop = padding + graphHeight - barHeight;
        float barBottom = padding + graphHeight;
        
        // Determine which band the current rating falls into
        RatingBand currentBand = ratingBands[0];
        for (RatingBand band : ratingBands) {
            if (currentRating >= band.min && currentRating <= band.max) {
                currentBand = band;
                break;
            }
        }
        
        // Draw the bar with gradient effect
        if (currentBand.label.equals("Eternity")) {
            // Special stunning gradient for Eternity
            LinearGradient gradient = new LinearGradient(
                barLeft, barBottom, barLeft, barTop,
                new int[]{
                    Color.parseColor("#FF00FF"), // Magenta
                    Color.parseColor("#9D00FF"), // Purple  
                    ContextCompat.getColor(getContext(), R.color.ratingBarEternityGradient), // Cyan
                    Color.parseColor("#FFD700")  // Gold
                },
                null,
                Shader.TileMode.CLAMP
            );
            barPaint.setShader(gradient);
        } else {
            barPaint.setShader(null);
            barPaint.setColor(currentBand.color);
        }
        
        RectF barRect = new RectF(barLeft, barTop, barRight, barBottom);
        canvas.drawRect(barRect, barPaint);
        
        // Draw bar border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(2));
        borderPaint.setColor(ContextCompat.getColor(getContext(), R.color.textPrimary));
        canvas.drawRect(barRect, borderPaint);
    }

    private void drawRatingLabels(Canvas canvas, float x, float padding, float graphHeight) {
        Paint labelTextPaint = new Paint(labelPaint);
        labelTextPaint.setTextSize(dpToPx(10));
        labelTextPaint.setTextAlign(Paint.Align.LEFT);
        
        for (RatingBand band : ratingBands) {
            // Position label at the middle of the band
            float midPoint = (band.min + band.max) / 2f;
            float y = padding + graphHeight - (midPoint / 10f) * graphHeight;
            
            // Use band color for label
            labelTextPaint.setColor(band.color);
            labelTextPaint.setFakeBoldText(true);
            
            canvas.drawText(band.label, x, y + dpToPx(3), labelTextPaint);
        }
    }

    private void drawCurrentRatingText(Canvas canvas, float barLeft, float barRight, 
                                        float padding, float graphHeight) {
        float barHeight = (currentRating / 10f) * graphHeight;
        float barTop = padding + graphHeight - barHeight;
        
        // Draw rating value above the bar
        String ratingText = String.format("%.1f", currentRating);
        float textY = barTop - dpToPx(10);
        
        // Ensure text doesn't go off screen
        if (textY < padding) {
            textY = barTop + dpToPx(20);
        }
        
        canvas.drawText(ratingText, (barLeft + barRight) / 2f, textY, ratingTextPaint);
    }
}
