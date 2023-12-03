package com.jsoft.diffusionpaint.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.jsoft.diffusionpaint.dto.Sketch;

import java.util.ArrayList;

public class DrawingView extends View
{
	private Path mDrawPath;
	private Paint mDrawPaint;
	private Canvas mViewCanvas;
	private Bitmap mViewBitmap; //member of mDrawCanvas
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

	private int maxImgSize = 2560;
	private String aspectRatio;

	private double defaultScale = 1.0;
	private double curScale = 1.0;
	private double curTop = 0;
	private double curLeft = 0;
	private boolean isTranslate = false;
	ScaleGestureDetector mScaleDetector;
	GestureDetector mGestureDetector;

	public DrawingView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		super.setClickable(true);
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		mGestureDetector = new GestureDetector(context, new DrawingView.GestureListener());
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
	public boolean getIsTranslate() {
		return this.isTranslate;
	}
	public void setIsTranslate(boolean isTranslate) {
		this.isTranslate = isTranslate;
	}
	private void init() {
		mDrawPath = new Path();
		mBaseBitmap = null;
		initPaint();
	}

	private void initPaint() {
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

	private void drawBackground(Canvas viewCanvas) {
		drawBackground(viewCanvas, curLeft, curTop, curScale);
	}

	private void drawBackground(Canvas viewCanvas, double offsetX, double offsetY, double viewScale) {
		if (mBaseBitmap == null) {
			//Create new mBaseBitmap for new Drawing
			Paint mBackgroundPaint = new Paint();
			mBackgroundPaint.setColor(mBackgroundColor);
			mBackgroundPaint.setStyle(Paint.Style.FILL);
			double baseWidth, baseHeight;
			if (aspectRatio.equals(Sketch.ASPECT_RATIO_SQUARE)) {
				baseWidth = baseHeight = Math.min(viewCanvas.getWidth(), (double)viewCanvas.getHeight());
			} else if (aspectRatio.equals(Sketch.ASPECT_RATIO_PORTRAIT)) {
				double ratio = (double) viewCanvas.getHeight() / (double) viewCanvas.getWidth();
				baseWidth = (ratio >= 4d / 3d) ? viewCanvas.getWidth() : viewCanvas.getHeight() * 3d / 4d;
				baseHeight = (ratio >= 4d / 3d) ? viewCanvas.getWidth() * 4d / 3d : viewCanvas.getHeight();
			} else {
				double ratio = (double) viewCanvas.getWidth() / (double) viewCanvas.getHeight();
				baseWidth = (ratio >= 4d / 3d) ? viewCanvas.getHeight() * 4d / 3d : viewCanvas.getWidth();
				baseHeight = (ratio >= 4d / 3d) ? viewCanvas.getHeight() : viewCanvas.getWidth() * 3d / 4d;
			}
			mBaseBitmap = Bitmap.createBitmap((int)Math.round(baseWidth), (int)Math.round(baseHeight), Bitmap.Config.ARGB_8888);
			Canvas baseCanvas = new Canvas(mBaseBitmap);
			baseCanvas.drawRect(0f,0f,(int)Math.round(baseWidth), (int)Math.round(baseHeight), mBackgroundPaint);

			curLeft = (viewCanvas.getWidth() - baseCanvas.getWidth()) / 2d;
			curTop = (viewCanvas.getHeight() - baseCanvas.getHeight()) / 2d;

			defaultScale = 1.0;
			curScale = 1.0;
			mDrawPaint.setStrokeWidth((int)Math.round((double)mStrokeWidth / curScale));
			drawBackground(viewCanvas, curLeft, curTop, curScale);
		} else {
			Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBaseBitmap, (int)Math.round(mBaseBitmap.getWidth() * viewScale),  (int)Math.round(mBaseBitmap.getHeight() * viewScale), true);
			viewCanvas.drawBitmap(resizedBitmap, (int)Math.round(offsetX), (int)Math.round(offsetY), null);
		}
	}

	private void drawPaths(Canvas canvas) {
		drawPaths(canvas, null, null, curLeft, curTop, curScale);
	}

	private void drawPaths(Canvas canvas, Path path, Paint paint) {
		drawPaths(canvas, path, paint, curLeft, curTop, curScale);
	}

	private void drawPaths(Canvas canvas, double offsetX, double offsetY, double viewScale) {
		drawPaths(canvas, null, null, offsetX, offsetY, viewScale);
	}

	private void drawPaths(Canvas canvas, Path path, Paint paint, double offsetX, double offsetY, double viewScale) {
		int width = mBaseBitmap.getWidth();
		int height = mBaseBitmap.getHeight();
		if (width == 0) return;
		Bitmap pathBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas pathCanvas = new Canvas(pathBitmap);
		if (mPaintBitmap != null) { pathCanvas.drawBitmap(mPaintBitmap, null, new RectF(0,0,width, height), null); }
		int i = 0;
		for (Path p : mPaths) {
			pathCanvas.drawPath(p, mPaints.get(i));
			i++;
		}
		if (path != null && paint != null) {
			pathCanvas.drawPath(path, paint);
		}
		canvas.drawBitmap(pathBitmap, null, new RectF((float)offsetX, (float)offsetY,(float)(offsetX + width * viewScale), (float)(offsetY + height * viewScale)), null);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		mViewCanvas = canvas;
		drawBackground(canvas);
		drawPaths(canvas, mDrawPath, mDrawPaint);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mViewBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		mViewCanvas = new Canvas(mViewBitmap);
		resetView();
	}

	private void resetView() {
		double w = mViewCanvas.getWidth();
		double h = mViewCanvas.getHeight();
		if (mBaseBitmap != null) {
			defaultScale = Math.min(w / (double)mBaseBitmap.getWidth(), h / (double)mBaseBitmap.getHeight());
			curScale = defaultScale;
			curLeft = (w - mBaseBitmap.getWidth() * curScale) / 2d;
			curTop = (h - mBaseBitmap.getHeight() * curScale) / 2d;
			mDrawPaint.setStrokeWidth((int)Math.round((double)mStrokeWidth / curScale));
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if (!isTranslate) {
			float touchX = event.getX();
			float touchY = event.getY();
			float realX = (float) ((touchX - curLeft) / curScale);
			float realY = (float) ((touchY - curTop) / curScale);

			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (!isEyedropper) {
						mDrawPath.moveTo(realX, realY);
					}
					//mDrawPath.addCircle(touchX, touchY, mStrokeWidth/10, Path.Direction.CW);
					break;
				case MotionEvent.ACTION_MOVE:
					if (!isEyedropper) {
						mDrawPath.lineTo(realX, realY);
					}
					break;
				case MotionEvent.ACTION_UP:
					if (!isEyedropper) {
						mDrawPath.lineTo(realX, realY);
						mPaths.add(mDrawPath);
						mPaints.add(mDrawPaint);
						mDrawPath = new Path();
						initPaint();
					} else {
						Log.e("diffusionpaint", "isEyedropper 1");
						Bitmap viewBM = getViewBitmap();
						Log.e("diffusionpaint", "isEyedropper 2");
						int eyedropperColor = viewBM.getPixel((int) touchX, (int) touchY);
						listener.onEyedropperResult(eyedropperColor);
					}
					break;
				default:
					return false;
			}
		} else {
			mScaleDetector.onTouchEvent(event);
			mGestureDetector.onTouchEvent(event);
		}

		invalidate();
		return true;
	}
	private class ScaleListener extends
			ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			//scaleImage(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
			double realX =  ((detector.getFocusX() - curLeft) / curScale);
			double realY = ((detector.getFocusY() - curTop) / curScale);
			double originalScale = curScale;
			Log.e("diffusionpaint", "onScale(" + detector.getScaleFactor() + ", " + detector.getFocusX()+ ", " +detector.getFocusY()+")");
			curScale = curScale * detector.getScaleFactor();
			if (curScale > 1.0) {
				curScale = 1.0;
			} else if (curScale < defaultScale) {
				curScale = defaultScale;
			}

			curTop = curTop - (curScale / originalScale - 1) * realY;
			curLeft = curLeft - (curScale / originalScale - 1) * realX;
			fixTrans();

			mDrawPaint.setStrokeWidth((int)Math.round((double)mStrokeWidth / curScale));

			return true;
		}
	}

	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (curScale >= 1.0) {
				resetView();
			} else {
				curScale = 1.5 * curScale;
				mDrawPaint.setStrokeWidth((int)Math.round((double)mStrokeWidth / curScale));
			}

			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			//Log.e("diffusionpaint", "onScroll(" + distanceX + ", " + distanceY+")");
			curTop -= distanceY;
			curLeft -= distanceX;
			fixTrans();
			return true;
		}
	}

	public void fixTrans() {
		if (mViewCanvas.getHeight() > mBaseBitmap.getHeight() * curScale) {
			if (curTop < 0) {
				curTop = 0;
			} else if (curTop + mBaseBitmap.getHeight() * curScale > mViewCanvas.getHeight()) {
				curTop = mViewCanvas.getHeight() - mBaseBitmap.getHeight() * curScale;
			}
		}  else {
			if (curTop + mBaseBitmap.getHeight() * curScale < mViewCanvas.getHeight()) {
				curTop = mViewCanvas.getHeight() - mBaseBitmap.getHeight() * curScale;
			} else if (curTop > 0) {
				curTop = 0;
			}
		}

		if (mViewCanvas.getWidth() > mBaseBitmap.getWidth() * curScale) {
			if (curLeft < 0) {
				curLeft = 0;
			} else if (curLeft + mBaseBitmap.getWidth() * curScale > mViewCanvas.getWidth()) {
				curLeft = mViewCanvas.getWidth() - mBaseBitmap.getWidth() * curScale;
			}
		}  else {
			if (curLeft + mBaseBitmap.getWidth() * curScale < mViewCanvas.getWidth()) {
				curLeft = mViewCanvas.getWidth() - mBaseBitmap.getWidth() * curScale;
			} else if (curLeft > 0) {
				curLeft = 0;
			}
		}
	}

	public void clearCanvas() {
		mPaths.clear();
		mPaints.clear();
		mUndonePaths.clear();
		mUndonePaints.clear();
		mViewCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
		invalidate();
	}

	public void setPaintColor(int color) {
		mPaintColor = color;
		mDrawPaint.setColor(mPaintColor);
	}

	public void setPaintStrokeWidth(int strokeWidth) {
		mStrokeWidth = strokeWidth;
		mDrawPaint.setStrokeWidth((int)Math.round((double)mStrokeWidth / curScale));
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
		//mBackgroundPaint.setColor(mBackgroundColor);
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

	public Sketch prepareBitmap(Sketch s, Bitmap bmRef) {
		s.setImgPreview(getPreview());
		s.setImgReference(getCroppedBitmap(bmRef));
		s.setImgBackground(getCroppedBitmap(mBaseBitmap));
		s.setImgPaint(getCroppedBitmap(getPaintBitmap()));
		return s;
	}

	public Bitmap getPreview() {
		Bitmap previewBitmap = Bitmap.createBitmap((int)Math.round(mBaseBitmap.getWidth()*defaultScale), (int)Math.round(mBaseBitmap.getHeight()*defaultScale), Bitmap.Config.ARGB_8888);
		Canvas previewCanvas = new Canvas(previewBitmap);
		drawBackground(previewCanvas,0,0, defaultScale);
		drawPaths(previewCanvas,0,0, defaultScale);
		return previewBitmap;
	}

	public Bitmap getViewBitmap() {
		mViewBitmap = Bitmap.createBitmap(mViewCanvas.getWidth(), mViewCanvas.getHeight(), Bitmap.Config.ARGB_8888);
		mViewCanvas = new Canvas(mViewBitmap);
		drawBackground(mViewCanvas);
		drawPaths(mViewCanvas);
		return mViewBitmap;
	}

	public Bitmap getCroppedBitmap(Bitmap bm) {
		if (bm != null) {
			double bitmapWidth = bm.getWidth();
			double bitmapHeight = bm.getHeight();

			double scale = Math.min(maxImgSize / bitmapWidth, maxImgSize / bitmapHeight);
			if (scale > 1d) {
				scale = 1d;
			}
			return Bitmap.createScaledBitmap(bm, (int)Math.round(bitmapWidth * scale), (int)Math.round(bitmapHeight * scale), true);
		}
		return null;
	}

	public Bitmap getPaintBitmap() {
		Bitmap paintBitmap = Bitmap.createBitmap(mBaseBitmap.getWidth(), mBaseBitmap.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas paintCanvas = new Canvas(paintBitmap);
		drawPaths(paintCanvas, 0, 0, 1);
		return paintBitmap;
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

	public void setCanvasSize(int canvasSize) { this.maxImgSize = canvasSize; }

	public void setAspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; }

}
