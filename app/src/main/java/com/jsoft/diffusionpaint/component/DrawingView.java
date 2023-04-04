package com.jsoft.diffusionpaint.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.jsoft.diffusionpaint.helper.Sketch;

import java.util.ArrayList;

public class DrawingView extends View
{
	private Path mDrawPath;
	private Paint mBackgroundPaint;
	private Paint mDrawPaint;
	private Canvas mDrawCanvas;
	private Bitmap mCanvasBitmap; //member of mDrawCanvas
	private Bitmap mBaseBitmap; //Input Background


	private Bitmap mPaintBitmap; //Input Paint from save data
	private ArrayList<Path> mPaths = new ArrayList<>();
	private ArrayList<Paint> mPaints = new ArrayList<>();
	private ArrayList<Path> mUndonePaths = new ArrayList<>();
	private ArrayList<Paint> mUndonePaints = new ArrayList<>();

	// Set default values
	private int mBackgroundColor = 0xFFFFFFFF;
	private int mPaintColor = 0xFF666666;
	private int mStrokeWidth = 10;
	private boolean isEyedropper = false;
	private boolean isEraser = false;
	private DrawingViewListener listener;

	public DrawingView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init();
	}

	public void setListener(DrawingViewListener listener) {
		this.listener = listener;
	}

	public void setEyedropper(boolean eyedropper) {
		isEyedropper = eyedropper;
	}

	public boolean getIsEraserMode() {
		return this.isEraser;
	}
	private void init()
	{
		mDrawPath = new Path();
		mBackgroundPaint = new Paint();
		mBaseBitmap = null;
		initPaint();
	}

	private void initPaint()
	{
		mDrawPaint = new Paint();
		mDrawPaint.setColor(mPaintColor);
		mDrawPaint.setAntiAlias(true);
		mDrawPaint.setStrokeWidth(mStrokeWidth);
		mDrawPaint.setStyle(Paint.Style.STROKE);
		mDrawPaint.setStrokeJoin(Paint.Join.ROUND);
		mDrawPaint.setStrokeCap(Paint.Cap.ROUND);
		if (isEraser) {
			mDrawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		}
	}

	private void drawBackground(Canvas canvas) {
		if (mBaseBitmap == null) {
			mBackgroundPaint.setColor(mBackgroundColor);
			mBackgroundPaint.setStyle(Paint.Style.FILL);
			canvas.drawRect(0, 0, this.getWidth(), this.getHeight(), mBackgroundPaint);
		} else {
			Bitmap bitmap = mBaseBitmap;
			float bitmapWidth = bitmap.getWidth();
			float bitmapHeight = bitmap.getHeight();
			float canvasWidth = canvas.getWidth();
			float canvasHeight = canvas.getHeight();

			// Calculate the scale factor to fit the bitmap into the canvas while maintaining its aspect ratio
			float scaleFactor = Math.max(canvasWidth / bitmapWidth, canvasHeight / bitmapHeight);

			// Calculate the cropped bitmap dimensions
			float croppedWidth = canvasWidth / scaleFactor;
			float croppedHeight = canvasHeight / scaleFactor;
			float xOffset = (bitmapWidth - croppedWidth) / 2;
			float yOffset = (bitmapHeight - croppedHeight) / 2;

			// Create a matrix to crop and scale the bitmap
			Matrix matrix = new Matrix();
			matrix.postScale(scaleFactor, scaleFactor);
			matrix.postTranslate(-xOffset, -yOffset);

			// Apply the matrix to the bitmap
			Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, (int)xOffset, (int)yOffset, (int)croppedWidth, (int)croppedHeight, matrix, true);

			// Draw the cropped bitmap onto the canvas
			canvas.drawBitmap(croppedBitmap, 0, 0, null);
		}
	}

	private void drawPaths(Canvas canvas) {
		drawPaths(canvas, null, null);
	}

	private void drawPaths(Canvas canvas, Path path, Paint paint) {
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		if (width == 0) return;
		Bitmap pathBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas pathCanvas = new Canvas(pathBitmap);
		if (mPaintBitmap != null) { pathCanvas.drawBitmap(mPaintBitmap, 0, 0, null); }
		int i = 0;
		for (Path p : mPaths) {
			pathCanvas.drawPath(p, mPaints.get(i));
			i++;
		}
		if (path != null && paint != null) {
			pathCanvas.drawPath(path, paint);
		}
		canvas.drawBitmap(pathBitmap, 0, 0, null);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		drawBackground(canvas);
		drawPaths(canvas, mDrawPath, mDrawPaint);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		mDrawCanvas = new Canvas(mCanvasBitmap);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		float touchX = event.getX();
		float touchY = event.getY();

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (!isEyedropper) {
					mDrawPath.moveTo(touchX, touchY);
				}
				//mDrawPath.addCircle(touchX, touchY, mStrokeWidth/10, Path.Direction.CW);
				break;
			case MotionEvent.ACTION_MOVE:
				if (!isEyedropper) {
					mDrawPath.lineTo(touchX, touchY);
				}
				break;
			case MotionEvent.ACTION_UP:
				if (!isEyedropper) {
					mDrawPath.lineTo(touchX, touchY);
					mPaths.add(mDrawPath);
					mPaints.add(mDrawPaint);
					mDrawPath = new Path();
					initPaint();
				} else {
					int eyedropperColor = getPreview().getPixel((int)touchX, (int)touchY);
					listener.onEyedropperResult(eyedropperColor);
				}
				break;
			default:
				return false;
		}

		invalidate();
		return true;
	}

	public void clearCanvas() {
		mPaths.clear();
		mPaints.clear();
		mUndonePaths.clear();
		mUndonePaints.clear();
		mDrawCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
		invalidate();
	}

	public void setPaintColor(int color) {
		mPaintColor = color;
		mDrawPaint.setColor(mPaintColor);
	}

	public void setPaintStrokeWidth(int strokeWidth) {
		mStrokeWidth = strokeWidth;
		mDrawPaint.setStrokeWidth(mStrokeWidth);
	}

	public void setEraserMode() {
		isEraser = true;
		mDrawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
	}

	public void setPenMode() {
		isEraser = false;
		mDrawPaint.setXfermode(null);
	}

	public void setBackgroundColor(int color) {
		mBackgroundColor = color;
		mBackgroundPaint.setColor(mBackgroundColor);
		invalidate();
	}



	public void undo() {
		if (mPaths.size() > 0) {
			mUndonePaths.add(mPaths.remove(mPaths.size() - 1));
			mUndonePaints.add(mPaints.remove(mPaints.size() - 1));
			invalidate();
		}
	}

	public void redo() {
		if (mUndonePaths.size() > 0)
		{
			mPaths.add(mUndonePaths.remove(mUndonePaths.size() - 1));
			mPaints.add(mUndonePaints.remove(mUndonePaints.size() - 1));
			invalidate();
		}
	}

	public Sketch prepareBitmap(Sketch s) {
		s.setImgPreview(getPreview());
		s.setImgBackground(getBgBitmap());
		s.setImgPaint(getPaintBitmap());
		return s;
	}

	public Bitmap getPreview() {
		drawBackground(mDrawCanvas);
		drawPaths(mDrawCanvas);
		return mCanvasBitmap;
	}

	public Bitmap getBgBitmap() {
		Bitmap bmBackground = Bitmap.createBitmap(mDrawCanvas.getWidth(), mDrawCanvas.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvasBackground = new Canvas(bmBackground);
		drawBackground(canvasBackground);
		return bmBackground;
	}

	public Bitmap getPaintBitmap() {
		Bitmap bmOverlay = Bitmap.createBitmap(mDrawCanvas.getWidth(), mDrawCanvas.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvasOverlay = new Canvas(bmOverlay);
		drawPaths(canvasOverlay);
		return bmOverlay;
	}

	public void setmBaseBitmap(Bitmap bitmap) {
		this.mBaseBitmap = bitmap;
	}

	public void setmPaintBitmap(Bitmap mPaintBitmap) {
		this.mPaintBitmap = mPaintBitmap;
	}

	public boolean isEmpty() {
		return (mPaths.size() == 0);
	}

}
