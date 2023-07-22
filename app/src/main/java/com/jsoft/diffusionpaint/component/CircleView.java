package com.jsoft.diffusionpaint.component;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.jsoft.diffusionpaint.R;

public class CircleView extends View {
    private Paint fillPaint;
    private Paint strokePaint;
    private int color;
    private float radius;

    public CircleView(Context context) {
        super(context);
        init();
    }

    public CircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        // Initialize the Paint object with antialiasing enabled.
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        fillPaint.setColor(Color.valueOf(0f, 0f, 0f).toArgb());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Set the Paint color to the specified color.
        fillPaint.setColor(color);

        // Draw a circle with the specified radius at the center of the View.
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        canvas.drawCircle(centerX, centerY, radius, fillPaint);
        canvas.drawCircle(centerX, centerY, radius + 1, strokePaint);
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }

    public void setRadius(float radius) {
        this.radius = radius;
        invalidate();
    }
}