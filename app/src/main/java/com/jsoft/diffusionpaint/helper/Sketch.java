package com.jsoft.diffusionpaint.helper;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Sketch implements Serializable {
    private Date createDate;
    private Date lastUpdateDate;
    private int id;
    private int parentId;
    private String prompt;
    private Bitmap imgPreview;
    private Bitmap imgBackground;
    private Bitmap imgPaint;
    private Bitmap imgInpaintMask;
    private Bitmap imgReference;

    private String cnMode;
    private RectF rectInpaint;

    private List<Sketch> children;

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
    public static final String CN_MODE_CUSTOM_1 = "custom1";
    public static final String CN_MODE_CUSTOM_2 = "custom2";
    public static final String CN_MODE_CUSTOM_3 = "custom3";
    public static final String CN_MODE_CUSTOM_4 = "custom4";
    public static final String CN_MODE_ORIGIN = "original";
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

    public Bitmap getImgInpaintMask() { return imgInpaintMask; }

    public void setImgInpaintMask(Bitmap imgInpaintMask) { this.imgInpaintMask = imgInpaintMask; }

    public Bitmap getImgReference() { return imgReference; }

    public void setImgReference(Bitmap imgReference) { this.imgReference = imgReference; }

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

    public int getParentId() { return parentId; }

    public void setParentId(int parentId) { this.parentId = parentId; }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void setRectInpaint(RectF rectInpaint) { this.rectInpaint = rectInpaint; }

    public RectF getRectInpaint() { return rectInpaint; }

    public List<Sketch> getChildren() { return children; }

    public void setChildren(List<Sketch> children) { this.children = children; }

    public static Bitmap getInpaintMaskFromPaint(Sketch s) {
        return Utils.getDilationMask(s.getImgPaint(), 0);
    }

    public Bitmap getResizedImgBackground() {
        if (imgBackground != null) {
            return Bitmap.createScaledBitmap(imgBackground, imgPreview.getWidth(), imgPreview.getHeight(), true);
        } else {
            return null;
        }
    }

    public Bitmap getResizedImgReference() {
        if (imgReference != null) {
            return Bitmap.createScaledBitmap(imgReference, imgPreview.getWidth(), imgPreview.getHeight(), true);
        } else {
            return null;
        }
    }

    public RectF getInpaintRect(int sdSize) {
        int inpaintMargin = 64;
        if (imgBackground == null) {
            return null;
        }
        int x1 = imgBackground.getWidth();
        int y1 = imgBackground.getHeight();
        int x2 = -1;
        int y2 = -1;
        Bitmap resizedImgPaint = Bitmap.createScaledBitmap(imgPaint, imgBackground.getWidth(), imgBackground.getHeight(), false);
        for (int x=0;x<imgBackground.getWidth();x++) {
            for (int y=0;y<imgBackground.getHeight();y++) {
                int color = resizedImgPaint.getPixel(x,y);
                if (Color.alpha(color) != 0) {
                    if (x < x1) x1 = x;
                    if (y < y1) y1 = y;
                    if (x > x2) x2 = x;
                    if (y > y2) y2 = y;
                }
            }
        }
        String aspectRatio = Utils.getAspectRatio(imgBackground);
        int minWidth = sdSize;
        int minHeight = sdSize;
        if (aspectRatio.equals(Sketch.ASPECT_RATIO_PORTRAIT)) {
            minWidth = sdSize * 3 / 4;
        } else if (aspectRatio.equals(Sketch.ASPECT_RATIO_LANDSCAPE)) {
            minHeight = sdSize * 3 / 4;
        }

        int inpaintWidth = Math.max(minWidth, x2 - x1 + 1 + 2 * inpaintMargin);
        int inpaintHeight =  Math.max(minHeight, y2 - y1 + 1 + 2 * inpaintMargin);
        float scale = Math.max((float)inpaintWidth/imgBackground.getWidth(), (float)inpaintHeight/imgBackground.getHeight());
        if (scale > 1.0) scale = 1;

        int left = (int)Math.round((x1 + (x2-x1)/2.0) - scale * imgBackground.getWidth() / 2.0);
        int right = (int)Math.round(left + scale * imgBackground.getWidth());
        int top = (int)Math.round((y1 + (y2-y1)/2.0) - scale * imgBackground.getHeight() / 2.0);
        int bottom = (int)Math.round(top + scale * imgBackground.getHeight());

        if (left < 0) {
            left = 0;
            right = (int)Math.round(scale * imgBackground.getWidth());
        } else if (right > imgBackground.getWidth()) {
            left = (int) Math.max((1-scale)*imgBackground.getWidth(),  0);
            right = imgBackground.getWidth();
        }

        if (top < 0) {
            bottom = (int)Math.round(scale * imgBackground.getHeight());
            top = 0;
        } else if (bottom > imgBackground.getHeight()) {
            top = (int) Math.max((1-scale)*imgBackground.getHeight(),  0);
            bottom = imgBackground.getHeight();
        }

        return new RectF(left,top,right,bottom);
    }
}
