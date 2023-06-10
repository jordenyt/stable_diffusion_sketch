package com.jsoft.diffusionpaint.dto;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

import com.jsoft.diffusionpaint.helper.Utils;

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
    public static final String CN_MODE_CUSTOM_5 = "custom5";
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

    public RectF getRectInpaint(int sdSize) {
        if (rectInpaint == null) {
            rectInpaint = getInpaintRect(sdSize);
        }
        return rectInpaint;
    }

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

    public Bitmap getResizedImgBgRef() {
        if (imgReference != null) {
            Bitmap imgBg = getResizedImgBackground();
            Bitmap imgRef = getResizedImgReference();
            Bitmap result = imgBg.copy(Bitmap.Config.ARGB_8888, true);
            for (int x = 0; x < imgBg.getWidth(); x++) {
                for (int y = 0; y < imgBg.getHeight(); y++) {
                    if (Color.alpha(imgPaint.getPixel(x, y)) != 0) {
                        result.setPixel(x, y, imgRef.getPixel(x, y));
                    }
                }
            }
            return result;
        } else {
            return getResizedImgBackground();
        }
    }

    public Bitmap getImgBgRef() {
        if (imgReference != null) {
            Bitmap imgRef = Bitmap.createScaledBitmap(imgReference, imgBackground.getWidth(), imgBackground.getHeight(), true);
            Bitmap imgPaintR = Bitmap.createScaledBitmap(imgPaint, imgBackground.getWidth(), imgBackground.getHeight(), true);
            Bitmap result = imgRef.copy(Bitmap.Config.ARGB_8888, true);
            for (int x = 0; x < imgRef.getWidth(); x++) {
                for (int y = 0; y < imgRef.getHeight(); y++) {
                    if (Color.alpha(imgPaintR.getPixel(x, y)) == 0) {
                        result.setPixel(x, y, imgBackground.getPixel(x, y));
                    }
                }
            }
            return result;
        } else {
            return imgBackground;
        }
    }

    private RectF getInpaintRect(int sdSize) {
        int inpaintMargin = 64;
        int sdBlockSize = 64;
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

        double scale = 1d;
        int inpaintWidth = Math.min(imgBackground.getWidth(), x2 - x1 + 1 + 2 * inpaintMargin);
        int inpaintHeight = Math.min(imgBackground.getHeight(), y2 - y1 + 1 + 2 * inpaintMargin);
        if ((inpaintWidth > sdSize) && (x2-x1 >= y2-y1)) {
            scale = (inpaintWidth-1d) / (sdSize-1d);
        } else if ((inpaintHeight > sdSize) && (y2-y1 >= x2-x1)) {
            scale = (inpaintHeight-1d) / (sdSize-1d);
        }

        double blockWidth = sdBlockSize * scale;
        if (x2-x1 >= y2-y1) {
            inpaintWidth = (int)Math.round(sdSize * scale);
            inpaintHeight = (int)Math.round(blockWidth * Math.ceil(inpaintHeight / blockWidth));
        } else {
            inpaintHeight = (int)Math.round(sdSize * scale);
            inpaintWidth = (int)Math.round(blockWidth * Math.ceil(inpaintWidth / blockWidth));
        }

        double left = (x1 + (x2-x1)/2d) - (inpaintWidth-1)/2d;
        double right = left + (inpaintWidth-1);
        double top = (y1 + (y2-y1)/2d) - (inpaintHeight-1)/ 2d;
        double bottom = top + (inpaintHeight-1);

        if (left < 0) {
            right = right - left;
            left = 0;
        } else if (right > imgBackground.getWidth()) {
            left = left - (right - imgBackground.getWidth());
            right = imgBackground.getWidth();
        }

        if (top < 0) {
            bottom = bottom - top;
            top = 0;
        } else if (bottom > imgBackground.getHeight()) {
            top = top - (bottom - imgBackground.getHeight() );
            bottom = imgBackground.getHeight();
        }

        return new RectF(Math.round(left), Math.round(top), Math.round(right), Math.round(bottom));
    }
}
