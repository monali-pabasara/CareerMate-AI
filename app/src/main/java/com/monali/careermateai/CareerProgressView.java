package com.monali.careermateai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CareerProgressView extends View {

    private int progress = 0;

    private Paint backgroundPaint;
    private Paint progressPaint;
    private Paint textPaint;

    public CareerProgressView(Context context) {
        super(context);
        init();
    }

    public CareerProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CareerProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.parseColor("#E3EAF3"));
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(dpToPx(12));
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.parseColor("#185FA5"));
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(dpToPx(12));
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#1A1A2E"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(dpToPx(26));
        textPaint.setFakeBoldText(true);
    }

    public void setProgress(int progress) {
        if (progress < 0) {
            this.progress = 0;
        } else if (progress > 100) {
            this.progress = 100;
        } else {
            this.progress = progress;
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int size = Math.min(getWidth(), getHeight());
        int padding = dpToPx(14);

        RectF rectF = new RectF(
                padding,
                padding,
                size - padding,
                size - padding
        );

        canvas.drawArc(rectF, -90, 360, false, backgroundPaint);
        canvas.drawArc(rectF, -90, progress * 3.6f, false, progressPaint);

        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        float centerY = getHeight() / 2f - (fontMetrics.ascent + fontMetrics.descent) / 2;

        canvas.drawText(progress + "%", getWidth() / 2f, centerY, textPaint);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}