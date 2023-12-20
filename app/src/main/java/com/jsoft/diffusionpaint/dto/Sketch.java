package com.jsoft.diffusionpaint.dto;

import static java.lang.Math.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.exifinterface.media.ExifInterface;

import com.jsoft.diffusionpaint.helper.Utils;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Sketch implements Serializable {
    private Date createDate;
    private Date lastUpdateDate;
    private int id;
    private int parentId;
    private String prompt;
    private String negPrompt;
    private Bitmap imgPreview;
    private Bitmap imgBackground;
    private Bitmap imgPaint;
    private Bitmap imgInpaintMask;
    private Bitmap imgReference;
    private String cnMode;
    private RectF rectInpaint;

    private String exif;
    private List<Sketch> children;
    public static final int customModeCount = 8;

    public static final String CN_MODE_SCRIBBLE = "scribble";
    public static final String CN_MODE_TXT = "txt";
    public static final String CN_MODE_TXT_SDXL = "txtSDXL";
    public static final String CN_MODE_TXT_SDXL_TURBO = "txtSDXLTurbo";
    public static final String CN_MODE_IMG_SDXL = "imgSDXL";
    public static final String CN_MODE_TXT_CANNY = "txtCanny";
    public static final String CN_MODE_TXT_SCRIBBLE = "txtScribble";
    public static final String CN_MODE_INPAINT = "inpaintNoise";
    public static final String CN_MODE_INPAINT_COLOR = "inpaintColor";
    public static final String CN_MODE_INPAINT_PARTIAL = "inpaintPartial";
    public static final String CN_MODE_INPAINT_PARTIAL_SKETCH = "inpaintPartialSketch";
    public static final String CN_MODE_OUTPAINT = "outpaint";
    public static final String CN_MODE_OUTPAINT_H = "outpaintH";
    public static final String CN_MODE_OUTPAINT_H_LEFT = "outpaintHL";
    public static final String CN_MODE_OUTPAINT_H_RIGHT = "outpaintHR";
    public static final String CN_MODE_OUTPAINT_V = "outpaintV";
    public static final String CN_MODE_OUTPAINT_V_TOP = "outpaintVT";
    public static final String CN_MODE_OUTPAINT_V_BOTTOM = "outpaintVB";
    public static final String CN_MODE_MERGE = "mergeReference";
    public static final String CN_MODE_CUSTOM = "custom";
    public static final String CN_MODE_ORIGIN = "original";
    public static final String ASPECT_RATIO_LANDSCAPE = "landscape";
    public static final String ASPECT_RATIO_PORTRAIT = "portrait";
    public static final String ASPECT_RATIO_SQUARE = "square";
    public static final String ASPECT_RATIO_WIDE = "wide";
    public static final Map<String, String> defaultJSON;
    static {
        Map<String, String> json = new LinkedHashMap<>();
        json.put(CN_MODE_SCRIBBLE, "{\"baseImage\":\"sketch\", \"cn\":[{\"cnInputImage\":\"sketch\", \"cnModelKey\":\"cnScribbleModel\", \"cnModule\":\"none\", \"cnWeight\":0.7}], \"denoise\":0.75, \"type\":\"img2img\"}");
        json.put(CN_MODE_TXT_CANNY, "{\"cn\":[{\"cnInputImage\":\"sketch\", \"cnModelKey\":\"cnCannyModel\", \"cnModule\":\"canny\", \"cnWeight\":1.0}], \"type\":\"txt2img\"}");
        json.put(CN_MODE_TXT_SCRIBBLE, "{\"cn\":[{\"cnInputImage\":\"sketch\", \"cnModelKey\":\"cnScribbleModel\", \"cnModule\":\"scribble_hed\", \"cnWeight\":0.7}], \"type\":\"txt2img\"}");
        json.put(CN_MODE_TXT, "{\"type\":\"txt2img\"}");
        json.put(CN_MODE_TXT_SDXL, "{\"type\":\"txt2img\", \"sdSize\":1280}");
        json.put(CN_MODE_IMG_SDXL, "{\"type\":\"img2img\", \"denoise\":0.5, \"model\":\"sdxlBase\", \"baseImage\":\"background\", \"sdSize\":1280}");
        json.put(CN_MODE_TXT_SDXL_TURBO, "{\"type\":\"txt2img\", \"sdSize\":768, \"cfgScale\":2.0, \"steps\":5, \"sampler\":\"DPM++ SDE Karras\"}");
        json.put(CN_MODE_INPAINT, "{\"baseImage\":\"background\", \"denoise\":1.0, \"inpaintFill\":2, \"type\":\"inpaint\"}");
        json.put(CN_MODE_INPAINT_COLOR, "{\"baseImage\":\"sketch\", \"denoise\":0.5, \"inpaintFill\":1, \"type\":\"inpaint\"}");
        json.put(CN_MODE_INPAINT_PARTIAL, "{\"baseImage\":\"background\", \"denoise\":1.0, \"inpaintFill\":2, \"inpaintPartial\":1, \"type\":\"inpaint\"}");
        json.put(CN_MODE_INPAINT_PARTIAL_SKETCH, "{\"baseImage\":\"sketch\", \"denoise\":0.5, \"inpaintFill\":1, \"inpaintPartial\":1, \"type\":\"inpaint\"}");
        json.put(CN_MODE_OUTPAINT, "{\"baseImage\":\"background\", \"denoise\":1.0, \"inpaintFill\":2, \"type\":\"inpaint\", \"cfgScale\":10.0}");
        json.put(CN_MODE_MERGE, "{\"baseImage\":\"background\", \"denoise\":0.75, \"inpaintFill\":1, \"type\":\"inpaint\"}");
        json.put(CN_MODE_CUSTOM, "{\"type\":\"txt2img\"}");
        defaultJSON = Collections.unmodifiableMap(json);
    }

    public static final Map<String, String> cnModeMap;
    static {
        Map<String, String> cnMode = new LinkedHashMap<>();
        cnMode.put("img2img(sketch) + Scribble(sketch)", CN_MODE_SCRIBBLE);
        cnMode.put("txt2img + Canny(sketch)", CN_MODE_TXT_CANNY);
        cnMode.put("txt2img + Scribble(sketch)", CN_MODE_TXT_SCRIBBLE);
        cnMode.put("txt2img", CN_MODE_TXT);
        cnMode.put("SDXL txt2img", CN_MODE_TXT_SDXL);
        cnMode.put("SDXL Turbo txt2img", CN_MODE_TXT_SDXL_TURBO);
        cnMode.put("SDXL img2img", CN_MODE_IMG_SDXL);
        cnMode.put("Inpainting (background)", CN_MODE_INPAINT);
        cnMode.put("Inpainting (sketch)", CN_MODE_INPAINT_COLOR);
        cnMode.put("Partial Inpainting (background)", CN_MODE_INPAINT_PARTIAL);
        cnMode.put("Partial Inpainting (sketch)", CN_MODE_INPAINT_PARTIAL_SKETCH);
        cnMode.put("Outpainting Horizontally", CN_MODE_OUTPAINT_H);
        cnMode.put("Outpainting on Left", CN_MODE_OUTPAINT_H_LEFT);
        cnMode.put("Outpainting on Right", CN_MODE_OUTPAINT_H_RIGHT);
        cnMode.put("Outpainting Vertically", CN_MODE_OUTPAINT_V);
        cnMode.put("Outpainting on Top", CN_MODE_OUTPAINT_V_TOP);
        cnMode.put("Outpainting on Bottom", CN_MODE_OUTPAINT_V_BOTTOM);
        cnMode.put("Original / Fill with Reference", CN_MODE_ORIGIN);
        cnMode.put("Merge with Reference", CN_MODE_MERGE);
        for (int i=1; i<=customModeCount; i++) {
            cnMode.put("Custom Mode " + i, CN_MODE_CUSTOM + i);
        }
        cnModeMap = Collections.unmodifiableMap(cnMode);
    }

    public static final Map<String, String> txt2imgModeMap;
    static {
        Map<String, String> mode = new LinkedHashMap<>();
        mode.put("SDXL txt2img", CN_MODE_TXT_SDXL);
        mode.put("SDXL Turbo txt2img", CN_MODE_TXT_SDXL_TURBO);
        mode.put("txt2img", CN_MODE_TXT);
        txt2imgModeMap = Collections.unmodifiableMap(mode);
    }

    public Sketch() {
        this.id = -1;
        this.createDate = new Date();
        this.lastUpdateDate = new Date();
        this.prompt = "";
        this.negPrompt = "";
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

    public String getNegPrompt() { return negPrompt; }

    public void setNegPrompt(String negPrompt) { this.negPrompt = negPrompt; }

    public String getExif() { return exif; }

    public void setExif(String exif) { this.exif = exif; }

    public RectF getRectInpaint(int sdSize) {
        if (rectInpaint == null) {
            rectInpaint = getInpaintRect(sdSize);
        }
        return rectInpaint;
    }

    public List<Sketch> getChildren() { return children; }

    public void setChildren(List<Sketch> children) { this.children = children; }

    public static Bitmap getInpaintMaskFromPaint(Sketch s, int expandPixel) {
        return Utils.getDilationMask(s.getImgPaint(), expandPixel);
    }

    public Bitmap getImgBgRefPreview() {
        Bitmap previewBitmap = Bitmap.createBitmap(imgBackground.getWidth(), imgBackground.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cvPreview = new Canvas(previewBitmap);
        cvPreview.drawBitmap(imgBackground, 0, 0, null);
        cvPreview.drawBitmap(imgPaint, 0, 0, null);
        return previewBitmap;
    }

    public Bitmap getImgBgRefPaint(int boundaryWidth) {
        Bitmap sketchBitmap = Bitmap.createScaledBitmap(imgPaint, imgBackground.getWidth(), imgBackground.getHeight(), true);
        int[] sketchPixels = new int[imgBackground.getWidth() * imgBackground.getHeight()];
        sketchBitmap.getPixels(sketchPixels, 0, sketchBitmap.getWidth(), 0, 0, sketchBitmap.getWidth(), sketchBitmap.getHeight());

        Bitmap paintBitmap = Bitmap.createBitmap(imgBackground.getWidth(), imgBackground.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cvPaint = new Canvas(paintBitmap);

        Paint boundaryPaint = new Paint();
        boundaryPaint.setColor(Color.BLUE);
        boundaryPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < sketchPixels.length; i++) {
            if (Color.alpha(sketchPixels[i]) != 0) {
                boolean isBoundary = false;
                for (int dx=-1; dx<=1; dx++) {
                    for (int dy=-1; dy<=1; dy++) {
                        if (!(dx==0 && dy==0)) {
                            int x = i % sketchBitmap.getWidth() + dx;
                            int y = i / sketchBitmap.getWidth() + dy;
                            if (x >= 0 && x < sketchBitmap.getWidth() && y >= 0 && y < sketchBitmap.getHeight()) {
                                if (Color.alpha(sketchPixels[x + y * sketchBitmap.getWidth()]) == 0) {
                                    isBoundary = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (isBoundary) break;
                }
                if (isBoundary) {
                    int x = i % sketchBitmap.getWidth();
                    int y = i / sketchBitmap.getWidth();
                    cvPaint.drawCircle(x, y, boundaryWidth, boundaryPaint);
                }
            }
        }

        return paintBitmap;
    }

    public Bitmap getImgBgRef() {
        return getImgBgMerge(imgReference, 0);
    }

    public Bitmap getImgBgMerge(Bitmap bmMerge, int boundary) {
        if (bmMerge != null) {


            Bitmap imgMerge = Bitmap.createBitmap(imgBackground.getWidth(), imgBackground.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas cvMerge = new Canvas(imgMerge);
            cvMerge.drawBitmap(imgBackground, 0, 0, null);
            double ratio = min((double)imgBackground.getWidth() / (double) bmMerge.getWidth(), (double)imgBackground.getHeight() / (double) bmMerge.getHeight());
            Bitmap bmMergeScaled = Bitmap.createScaledBitmap(bmMerge, (int)round(bmMerge.getWidth() * ratio), (int)round(bmMerge.getHeight() * ratio), true);
            cvMerge.drawBitmap(bmMergeScaled, (imgBackground.getWidth() - bmMergeScaled.getWidth()) / 2f, (imgBackground.getHeight() - bmMergeScaled.getHeight()) / 2f, null);

            Bitmap imgDilatedMask = Utils.getDilationMask(imgPaint, boundary);
            Bitmap imgPaintR = Bitmap.createScaledBitmap(imgDilatedMask, imgBackground.getWidth(), imgBackground.getHeight(), true);

            int[] mergePixels = new int[imgMerge.getWidth() * imgMerge.getHeight()];
            int[] paintPixels = new int[imgPaintR.getWidth() * imgPaintR.getHeight()];
            int[] backgroundPixels = new int[imgBackground.getWidth() * imgBackground.getHeight()];

            imgMerge.getPixels(mergePixels, 0, imgMerge.getWidth(), 0, 0, imgMerge.getWidth(), imgMerge.getHeight());
            imgPaintR.getPixels(paintPixels, 0, imgPaintR.getWidth(), 0, 0, imgPaintR.getWidth(), imgPaintR.getHeight());
            imgBackground.getPixels(backgroundPixels, 0, imgBackground.getWidth(), 0, 0, imgBackground.getWidth(), imgBackground.getHeight());

            for (int i = 0; i < mergePixels.length; i++) {
                if (paintPixels[i] == Color.BLACK) {
                    mergePixels[i] = backgroundPixels[i];
                }
            }

            return Bitmap.createBitmap(mergePixels, imgMerge.getWidth(), imgMerge.getHeight(), Bitmap.Config.ARGB_8888);
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
        int[] paintPixel = new int[resizedImgPaint.getWidth() * resizedImgPaint.getHeight()];
        resizedImgPaint.getPixels(paintPixel, 0, resizedImgPaint.getWidth(), 0, 0, resizedImgPaint.getWidth(), resizedImgPaint.getHeight());
        for (int x=0;x<imgBackground.getWidth();x++) {
            for (int y=0;y<imgBackground.getHeight();y++) {
                if (Color.alpha(paintPixel[y * resizedImgPaint.getWidth() + x]) != 0) {
                    if (x < x1) x1 = x;
                    if (y < y1) y1 = y;
                    if (x > x2) x2 = x;
                    if (y > y2) y2 = y;
                }
            }
        }

        double scale = 1d;
        int inpaintWidth = min(imgBackground.getWidth(), x2 - x1 + 1 + 2 * inpaintMargin);
        int inpaintHeight = min(imgBackground.getHeight(), y2 - y1 + 1 + 2 * inpaintMargin);
        if (x2-x1 >= y2-y1) {
            if (inpaintWidth > sdSize) {
                scale = (inpaintWidth - 1d) / (sdSize - 1d);
            } else if (imgBackground.getWidth() < sdSize) {
                scale = (imgBackground.getWidth() - 1d) / (sdSize - 1d);
            }
        } else if (y2-y1 >= x2-x1) {
            if (inpaintHeight > sdSize) {
                scale = (inpaintHeight - 1d) / (sdSize - 1d);
            } else if (imgBackground.getHeight() < sdSize) {
                scale = (imgBackground.getHeight() - 1d) / (sdSize - 1d);
            }
        }

        double blockWidth = sdBlockSize * scale;
        if (x2-x1 >= y2-y1) {
            inpaintWidth = (int)round(sdSize * scale);
            if (inpaintHeight < (int)round(2d / 3d * inpaintWidth)) {
                inpaintHeight = min(imgBackground.getHeight(), (int)round(2d / 3d * inpaintWidth));
            }
            inpaintHeight = (int)round(blockWidth * ceil(inpaintHeight / blockWidth));
            if (inpaintHeight > imgBackground.getHeight()) {
                inpaintHeight -= blockWidth;
            }
        } else {
            inpaintHeight = (int)round(sdSize * scale);
            if (inpaintWidth < (int)round(2d / 3d * inpaintHeight)) {
                inpaintWidth = min(imgBackground.getWidth(), (int)round(2d / 3d * inpaintHeight));
            }
            inpaintWidth = (int)round(blockWidth * ceil(inpaintWidth / blockWidth));
            if (inpaintWidth > imgBackground.getWidth()) {
                inpaintWidth -= blockWidth;
            }
        }

        double left = (x1 + (x2-x1)/2d) - (inpaintWidth-1)/2d;
        double right = left + inpaintWidth;
        double top = (y1 + (y2-y1)/2d) - (inpaintHeight-1)/ 2d;
        double bottom = top + inpaintHeight;

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

        return new RectF(max(0, round(left)), max(0, round(top)), min(imgBackground.getWidth(), round(right)), min(imgBackground.getHeight(), round(bottom)));
    }
}
