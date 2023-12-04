package com.jsoft.diffusionpaint.component;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.jsoft.diffusionpaint.DrawingActivity;
import com.jsoft.diffusionpaint.dto.Sketch;

public class DrawingView extends View
{
	private Path mDrawPath;
	private Paint mDrawPaint;
	private Canvas mViewCanvas;
	private Bitmap mViewBitmap; //member of mDrawCanvas
	private Bitmap mBaseBitmap; //Input Background
	private Bitmap mPaintBitmap; //Input Paint from save data
	private Bitmap mTranslateBitmap;

	// Set default values
	private int mBackgroundColor = 0xFFFFFFFF;
	private int mPaintColor = 0xFF666666;
	private int mStrokeWidth = 10;
	private boolean isEyedropper = false;
	private boolean isEraser = false;
	private DrawingViewListener listener;

	private int maxImgSize = 2560;
	private String aspectRatio;

	private double minScale = 1.0;
	private double curScale = 1.0;
	private double curTop = 0;
	private double curLeft = 0;
	private double maxScale = 1.0;
	private final int maxResolution = 5000;
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
		if (isTranslate) {
			mTranslateBitmap = Bitmap.createBitmap(mBaseBitmap.getWidth(), mBaseBitmap.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas translateCanvas = new Canvas(mTranslateBitmap);
			drawBackground(translateCanvas, 0, 0, 1.0);
			drawPaths(translateCanvas, 0, 0, 1.0);
		} else {
			mTranslateBitmap = null;
		}
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

			minScale = 1.0;
			maxScale = (double)maxResolution / Math.max(baseWidth, baseHeight);
			curScale = 1.0;
			drawBackground(viewCanvas, curLeft, curTop, curScale);
		} else {
			drawBitmapOnCanvas(mBaseBitmap, viewCanvas, offsetX, offsetY, viewScale);
		}
	}

	private void drawBitmapOnCanvas(Bitmap bm, Canvas viewCanvas, double offsetX, double offsetY, double viewScale) {
		double srcTop = Math.max(0d, -offsetY/viewScale);
		double srcLeft = Math.max(0d, -offsetX/viewScale);
		double srcHeight = (viewScale * bm.getHeight() <= viewCanvas.getHeight()) ? bm.getHeight() : (double)viewCanvas.getHeight() / viewScale;
		double srcWidth = (viewScale * bm.getWidth() <= viewCanvas.getWidth()) ? bm.getWidth() : (double)viewCanvas.getWidth() / viewScale;
		Rect srcRect = new Rect((int)Math.round(srcLeft), (int)Math.round(srcTop), (int)Math.round(srcLeft + srcWidth), (int)Math.round(srcTop+srcHeight));
		double dstTop = (offsetY > 0d) ? offsetY : 0;
		double dstLeft = (offsetX > 0d) ? offsetX : 0;
		double dstHeight = (viewScale * bm.getHeight() <= viewCanvas.getHeight()) ? bm.getHeight() * viewScale: (double)viewCanvas.getHeight();
		double dstWidth = (viewScale * bm.getWidth() <= viewCanvas.getWidth()) ? bm.getWidth() * viewScale: (double)viewCanvas.getWidth();
		Rect dstRect = new Rect((int)Math.round(dstLeft), (int)Math.round(dstTop), (int)Math.round(dstLeft + dstWidth), (int)Math.round(dstTop+dstHeight));
		viewCanvas.drawBitmap(bm, srcRect, dstRect, null);
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
		for (Path p : DrawingActivity.mPaths) {
			pathCanvas.drawPath(p, DrawingActivity.mPaints.get(i));
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
		if (!isTranslate) {
			drawBackground(canvas);
			drawPaths(canvas, mDrawPath, mDrawPaint);
		} else {
			drawBitmapOnCanvas(mTranslateBitmap,canvas,curLeft,curTop,curScale);
		}
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
			minScale = Math.min(w / (double)mBaseBitmap.getWidth(), h / (double)mBaseBitmap.getHeight());
			maxScale = (double)maxResolution / (double)Math.max(mBaseBitmap.getWidth(), mBaseBitmap.getHeight());
			curScale = minScale;
			curLeft = (w - mBaseBitmap.getWidth() * curScale) / 2d;
			curTop = (h - mBaseBitmap.getHeight() * curScale) / 2d;
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
						mDrawPaint.setStrokeWidth((int)Math.round((double)mStrokeWidth / curScale));
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
						DrawingActivity.mPaths.add(mDrawPath);
						DrawingActivity.mPaints.add(mDrawPaint);
						mDrawPath = new Path();
						initPaint();
					} else {
						Bitmap viewBM = getViewBitmap();
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
			scaleView(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
			return true;
		}
	}

	private void scaleView(float factor, float focusX, float focusY) {
		double realX =  ((focusX - curLeft) / curScale);
		double realY = ((focusY - curTop) / curScale);
		double originalScale = curScale;

		curScale = curScale * factor;
		if (curScale > maxScale) {
			curScale = maxScale;
		} else if (curScale < minScale) {
			curScale = minScale;
		}

		curTop = curTop - (curScale / originalScale - 1) * realY * originalScale;
		curLeft = curLeft - (curScale / originalScale - 1) * realX * originalScale;
		fixTrans();
	}

	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (curScale >= maxScale) {
				curScale=minScale;
				fixTrans();
			} else {
				scaleView(1.5f, e.getX(), e.getY());
			}
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			curTop -= distanceY;
			curLeft -= distanceX;
			fixTrans();
			return true;
		}
	}

	public void fixTrans() {
		if (mViewCanvas.getHeight() > mBaseBitmap.getHeight() * curScale) {
			curTop = (mViewCanvas.getHeight() - mBaseBitmap.getHeight() * curScale) / 2d;
		}  else {
			if (curTop + mBaseBitmap.getHeight() * curScale < mViewCanvas.getHeight()) {
				curTop = mViewCanvas.getHeight() - mBaseBitmap.getHeight() * curScale;
			} else if (curTop > 0) {
				curTop = 0;
			}
		}

		if (mViewCanvas.getWidth() > mBaseBitmap.getWidth() * curScale) {
			curLeft = (mViewCanvas.getWidth() - mBaseBitmap.getWidth() * curScale) / 2d;
		}  else {
			if (curLeft + mBaseBitmap.getWidth() * curScale < mViewCanvas.getWidth()) {
				curLeft = mViewCanvas.getWidth() - mBaseBitmap.getWidth() * curScale;
			} else if (curLeft > 0) {
				curLeft = 0;
			}
		}
	}

	public void setPaintColor(int color) {
		mPaintColor = color;
		mDrawPaint.setColor(mPaintColor);
	}

	public void setPaintStrokeWidth(int strokeWidth) {
		mStrokeWidth = strokeWidth;
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
		if (DrawingActivity.mPaths.size() > 0) {
			DrawingActivity.mUndonePaths.add(DrawingActivity.mPaths.remove(DrawingActivity.mPaths.size() - 1));
			DrawingActivity.mUndonePaints.add(DrawingActivity.mPaints.remove(DrawingActivity.mPaints.size() - 1));
			invalidate();
		}
	}

	public void redo() {
		if (DrawingActivity.mUndonePaths.size() > 0)
		{
			DrawingActivity.mPaths.add(DrawingActivity.mUndonePaths.remove(DrawingActivity.mUndonePaths.size() - 1));
			DrawingActivity.mPaints.add(DrawingActivity.mUndonePaints.remove(DrawingActivity.mUndonePaints.size() - 1));
			invalidate();
		}
	}

	public Sketch prepareBitmap(Sketch s, Bitmap bmRef) {
		s.setImgPreview(getPreview());
		s.setImgReference(getCroppedBitmap(bmRef));
		s.setImgBackground(mBaseBitmap);
		s.setImgPaint(getPaintBitmap());
		return s;
	}

	public Bitmap getPreview() {
		Bitmap previewBitmap = Bitmap.createBitmap((int)Math.round(mBaseBitmap.getWidth()* minScale), (int)Math.round(mBaseBitmap.getHeight()* minScale), Bitmap.Config.ARGB_8888);
		Canvas previewCanvas = new Canvas(previewBitmap);
		drawBackground(previewCanvas,0,0, minScale);
		drawPaths(previewCanvas,0,0, minScale);
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
		this.mBaseBitmap = getCroppedBitmap(bitmap);
	}

	public void setmPaintBitmap(Bitmap mPaintBitmap) {
		this.mPaintBitmap = getCroppedBitmap(mPaintBitmap);
	}

	public boolean isEmpty() {
		return (DrawingActivity.mPaths.size() == 0);
	}

	public void setCanvasSize(int canvasSize) { this.maxImgSize = canvasSize; }

	public void setAspectRatio(String aspectRatio) { this.aspectRatio = aspectRatio; }

}
