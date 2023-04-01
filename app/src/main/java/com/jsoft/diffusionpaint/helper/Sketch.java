package com.jsoft.diffusionpaint.helper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.io.Serializable;
import java.util.Date;

public class Sketch implements Serializable {
    private Date createDate;
    private Date lastUpdateDate;
    private int id;
    private String prompt;
    private Bitmap imgPreview;
    private Bitmap imgBackground;
    private Bitmap imgPaint;
    private Bitmap imgInpaint;

    private String cnMode;

    public static final String CN_MODE_SCRIBBLE = "scribble";
    public static final String CN_MODE_DEPTH = "depth";
    public static final String CN_MODE_POSE = "pose";
    public static final String CN_MODE_TXT = "txt";
    public static final String CN_MODE_TXT_CANNY = "txtCanny";
    public static final String CN_MODE_TXT_DEPTH = "txtDepth";
    public static final String CN_MODE_TXT_SCRIBBLE = "txtScribble";
    public static final String CN_MODE_INPAINT = "inpaintNoise";
    public static final String CN_MODE_INPAINT_COLOR = "inpaintColor";
    public static final String CN_MODE_INPAINT_DEPTH = "inpaintDepth";
    public static final String ASPECT_RATIO_LANDSCAPE = "landscape";
    public static final String ASPECT_RATIO_PORTRAIT = "portrait";
    public static final String ASPECT_RATIO_SQUARE = "square";


    public Sketch() {
        this.id = -1;
        this.createDate = new Date();
        this.lastUpdateDate = new Date();
        this.prompt = "";
        this.imgPreview = null;
        this.cnMode = CN_MODE_SCRIBBLE;
    }

    public Bitmap getImgPreview() {
        return imgPreview;
    }

    public void setImgPreview(Bitmap imgPreview) {
        this.imgPreview = imgPreview;
    }

    public Bitmap getImgBackground() { return imgBackground;}

    public void setImgBackground(Bitmap imgBackground) { this.imgBackground = imgBackground; }

    public Bitmap getImgPaint() { return imgPaint; }

    public void setImgPaint(Bitmap imgPaint) { this.imgPaint = imgPaint; }

    public Bitmap getImgInpaint() { return imgInpaint; }

    public void setImgInpaint(Bitmap imgInpaint) { this.imgInpaint = imgInpaint; }

    public String getCnMode() {
        return cnMode;
    }

    public void setCnMode(String cnMode) {
        this.cnMode = cnMode;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(Date lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public static Bitmap getInpaintMaskFromPaint(Sketch s) {
        Bitmap originalBitmap = s.getImgPaint();
        // Create a new Bitmap with the same dimensions and a black background
        Bitmap newBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);

        // Iterate over each pixel in the original Bitmap and set the color value in the new Bitmap
        for (int x = 0; x < originalBitmap.getWidth(); x++) {
            for (int y = 0; y < originalBitmap.getHeight(); y++) {
                int color = originalBitmap.getPixel(x,y);
                if (Color.alpha(color) != 0) {
                    newBitmap.setPixel(x,y,Color.WHITE);
                } else {
                    newBitmap.setPixel(x, y, 0x00000000);
                }
            }
        }

        int expandPixel=10;
        Bitmap bmMask = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cvMask = new Canvas(bmMask);
        Paint mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.BLACK);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        cvMask.drawRect(0, 0, bmMask.getWidth(), bmMask.getHeight(), mBackgroundPaint);
        for (int i=-expandPixel;i<=expandPixel;i++) {
            for (int j=-expandPixel;j<=expandPixel;j++) {
                if (Math.sqrt(i^2 + j^2) <= expandPixel) {
                    cvMask.drawBitmap(newBitmap, i, j, null);
                }
            }
        }
        return bmMask;
    }
}
