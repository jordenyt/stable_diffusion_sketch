package com.jsoft.diffusionpaint.component;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CircleView extends View {
    private Paint paint;
    private int color;
    private float radius;
    private float blur;
    BlurMaskFilter filter;

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
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        if (blur > 0) {
            filter = new BlurMaskFilter(blur / 20 * radius, BlurMaskFilter.Blur.NORMAL);
        } else {
            filter = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Set the Paint color to the specified color.
        paint.setColor(color);
        paint.setStrokeWidth(radius);
        paint.setMaskFilter(filter);

        // Draw a circle with the specified radius at the center of the View.
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        canvas.drawCircle(centerX, centerY, radius / 2, paint);
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }

    public void setRadius(float radius) {
        this.radius = radius;
        if (this.blur > 0) {
            filter = new BlurMaskFilter(blur / 20 * radius, BlurMaskFilter.Blur.NORMAL);
        } else {
            filter = null;
        }
        invalidate();
    }

    public void setBlur(float blur) {
        this.blur = blur;
        if (this.blur > 0) {
            filter = new BlurMaskFilter(blur / 20 * radius, BlurMaskFilter.Blur.NORMAL);
        } else {
            filter = null;
        }
        invalidate();
    }
}