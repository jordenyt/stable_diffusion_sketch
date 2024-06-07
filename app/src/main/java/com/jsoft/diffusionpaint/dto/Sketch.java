package com.jsoft.diffusionpaint.dto;

import static java.lang.Math.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

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
    private String style;

    private String exif;
    private List<Sketch> children;
    public static final int customModeCount = 12;
    public static final String CN_MODE_IMG_SCRIBBLE = "scribble";
    public static final String CN_MODE_TXT = "txt";
    public static final String CN_MODE_TXT_SDXL = "txtSDXL";
    public static final String CN_MODE_TXT_SDXL_TURBO = "txtSDXLTurbo";
    public static final String CN_MODE_REFINER = "imgSDXL";
    public static final String CN_MODE_TXT_CANNY = "txtCanny";
    public static final String CN_MODE_TXT_SCRIBBLE = "txtScribble";
    public static final String CN_MODE_INPAINT = "inpaintNoise";
    public static final String CN_MODE_INPAINT_SKETCH = "inpaintColor";
    public static final String CN_MODE_PARTIAL_INPAINT = "inpaintPartial";
    public static final String CN_MODE_PARTIAL_INPAINT_SKETCH = "inpaintPartialSketch";
    public static final String CN_MODE_PARTIAL_REFINER = "inpaintPartialRefiner";
    public static final String CN_MODE_OUTPAINT = "outpaint";
    public static final String CN_MODE_OUTPAINT_H = CN_MODE_OUTPAINT + "H";
    public static final String CN_MODE_OUTPAINT_H_LEFT = CN_MODE_OUTPAINT + "HL";
    public static final String CN_MODE_OUTPAINT_H_RIGHT = CN_MODE_OUTPAINT + "HR";
    public static final String CN_MODE_OUTPAINT_V = CN_MODE_OUTPAINT + "V";
    public static final String CN_MODE_OUTPAINT_V_TOP = CN_MODE_OUTPAINT + "VT";
    public static final String CN_MODE_OUTPAINT_V_BOTTOM = CN_MODE_OUTPAINT + "VB";
    public static final String CN_MODE_INPAINT_MERGE = "mergeReference";
    public static final String CN_MODE_CUSTOM = "custom";
    public static final String CN_MODE_ORIGIN = "original";
    public static final String CN_MODE_SUPIR = "supir";
    public static final String CN_MODE_SUPIR_PARTIAL = "supirPartial";
    public static final String CN_MODE_VTRON = "vtron";
    public static final String CN_MODE_ICLIGHT_TEXT = "iclightText";
    public static final String CN_MODE_ICLIGHT_RELIGHT = "iclightRelight";
    public static final String CN_MODE_ICLIGHT_BG = "iclightBG";
    public static final String ASPECT_RATIO_LANDSCAPE = "landscape";
    public static final String ASPECT_RATIO_PORTRAIT = "portrait";
    public static final String ASPECT_RATIO_SQUARE = "square";
    public static final String ASPECT_RATIO_WIDE = "wide";
    public static final Map<String, String> defaultJSON;
    static {
        Map<String, String> json = new LinkedHashMap<>();
        json.put(CN_MODE_IMG_SCRIBBLE, "{\"baseImage\":\"sketch\", \"cn\":[{\"cnInputImage\":\"sketch\", \"cnModelKey\":\"cnScribbleModel\", \"cnModule\":\"none\", \"cnWeight\":0.7}], \"denoise\":0.75, \"type\":\"img2img\"}");
        json.put(CN_MODE_TXT_CANNY, "{\"cn\":[{\"cnInputImage\":\"sketch\", \"cnModelKey\":\"cnCannyModel\", \"cnModule\":\"canny\", \"cnWeight\":1.0}], \"type\":\"txt2img\"}");
        json.put(CN_MODE_TXT_SCRIBBLE, "{\"cn\":[{\"cnInputImage\":\"sketch\", \"cnModelKey\":\"cnScribbleModel\", \"cnModule\":\"scribble_hed\", \"cnWeight\":0.7}], \"type\":\"txt2img\"}");
        json.put(CN_MODE_TXT, "{\"type\":\"txt2img\"}");
        json.put(CN_MODE_TXT_SDXL, "{\"type\":\"txt2img\", \"sdSize\":1280}");
        json.put(CN_MODE_REFINER, "{\"type\":\"img2img\", \"denoise\":0.5, \"model\":\"sdxlBase\", \"baseImage\":\"background\", \"sdSize\":1280}");
        json.put(CN_MODE_TXT_SDXL_TURBO, "{\"type\":\"txt2img\", \"sdSize\":1024, \"cfgScale\":2.0, \"steps\":6, \"sampler\":\"DPM++ SDE\"}");
        json.put(CN_MODE_INPAINT, "{\"baseImage\":\"background\", \"denoise\":1.0, \"inpaintFill\":2, \"type\":\"inpaint\"}");
        json.put(CN_MODE_INPAINT_SKETCH, "{\"baseImage\":\"sketch\", \"denoise\":0.5, \"inpaintFill\":1, \"type\":\"inpaint\"}");
        json.put(CN_MODE_PARTIAL_INPAINT, "{\"baseImage\":\"background\", \"denoise\":1.0, \"inpaintFill\":2, \"inpaintPartial\":1, \"type\":\"inpaint\", \"model\":\"sdxlInpaint\", \"sdSize\":1280}");
        json.put(CN_MODE_PARTIAL_INPAINT_SKETCH, "{\"baseImage\":\"sketch\", \"denoise\":0.6, \"inpaintFill\":1, \"inpaintPartial\":1, \"type\":\"inpaint\", \"model\":\"sdxlInpaint\", \"sdSize\":1280}");
        json.put(CN_MODE_PARTIAL_REFINER, "{\"baseImage\":\"background\", \"denoise\":0.6, \"inpaintFill\":1, \"inpaintPartial\":1, \"type\":\"inpaint\", \"model\":\"sdxlInpaint\", \"sdSize\":1280}");
        json.put(CN_MODE_OUTPAINT, "{\"baseImage\":\"background\", \"denoise\":1.0, \"inpaintFill\":2, \"type\":\"inpaint\", \"cfgScale\":10.0}");
        json.put(CN_MODE_INPAINT_MERGE, "{\"baseImage\":\"background\", \"denoise\":0.5, \"inpaintPartial\":1, \"inpaintFill\":1, \"type\":\"inpaint\"}");
        json.put(CN_MODE_CUSTOM, "{\"type\":\"txt2img\"}");
        json.put(CN_MODE_SUPIR_PARTIAL, "{\"baseImage\":\"background\", \"denoise\":1.0, \"inpaintFill\":2, \"inpaintPartial\":1, \"type\":\"inpaint\", \"sdSize\":768}");
        json.put(CN_MODE_VTRON, "{\"baseImage\":\"background\", \"denoise\":1.0, \"inpaintFill\":2, \"inpaintPartial\":1, \"type\":\"inpaint\", \"sdSize\":1024}");
        defaultJSON = Collections.unmodifiableMap(json);
    }

    public static final Map<String, String> cnModeMap;
    static {
        Map<String, String> cnMode = new LinkedHashMap<>();
        cnMode.put("img2img(sketch) + Scribble(sketch)", CN_MODE_IMG_SCRIBBLE);
        cnMode.put("txt2img + Canny(sketch)", CN_MODE_TXT_CANNY);
        cnMode.put("txt2img + Scribble(sketch)", CN_MODE_TXT_SCRIBBLE);
        cnMode.put("txt2img with v1.5 model", CN_MODE_TXT);
        cnMode.put("txt2img with SDXL", CN_MODE_TXT_SDXL);
        cnMode.put("txt2img with SDXL Turbo/Lightning", CN_MODE_TXT_SDXL_TURBO);
        cnMode.put("Inpainting (background)", CN_MODE_INPAINT);
        cnMode.put("Inpainting (sketch)", CN_MODE_INPAINT_SKETCH);
        cnMode.put("Refiner", CN_MODE_REFINER);
        cnMode.put("Partial Inpainting (background)", CN_MODE_PARTIAL_INPAINT);
        cnMode.put("Partial Inpainting (sketch)", CN_MODE_PARTIAL_INPAINT_SKETCH);
        cnMode.put("Partial Refiner", CN_MODE_PARTIAL_REFINER);
        cnMode.put("Outpainting Horizontally", CN_MODE_OUTPAINT_H);
        cnMode.put("Outpainting on Left", CN_MODE_OUTPAINT_H_LEFT);
        cnMode.put("Outpainting on Right", CN_MODE_OUTPAINT_H_RIGHT);
        cnMode.put("Outpainting Vertically", CN_MODE_OUTPAINT_V);
        cnMode.put("Outpainting on Top", CN_MODE_OUTPAINT_V_TOP);
        cnMode.put("Outpainting on Bottom", CN_MODE_OUTPAINT_V_BOTTOM);
        cnMode.put("Original / Fill with Reference", CN_MODE_ORIGIN);
        cnMode.put("Merge with Reference", CN_MODE_INPAINT_MERGE);
        cnMode.put("SupIR", CN_MODE_SUPIR);
        cnMode.put("Partial SupIR", CN_MODE_SUPIR_PARTIAL);
        cnMode.put("Virtual Try-On (IDM-VTRON)", CN_MODE_VTRON);
        cnMode.put("IC-Light Text", CN_MODE_ICLIGHT_TEXT);
        cnMode.put("IC-Light Relight", CN_MODE_ICLIGHT_RELIGHT);
        cnMode.put("IC-Light Background", CN_MODE_ICLIGHT_BG);
        for (int i=1; i<=customModeCount; i++) {
            cnMode.put("Custom Mode " + i, CN_MODE_CUSTOM + i);
        }
        cnModeMap = Collections.unmodifiableMap(cnMode);
    }

    public static final Map<String, String> txt2imgModeMap;
    static {
        Map<String, String> mode = new LinkedHashMap<>();
        mode.put("txt2img with SDXL Turbo/Lightning", CN_MODE_TXT_SDXL_TURBO);
        mode.put("txt2img with SDXL", CN_MODE_TXT_SDXL);
        mode.put("txt2img with v1.5 model", CN_MODE_TXT);
        txt2imgModeMap = Collections.unmodifiableMap(mode);
    }

    public Sketch() {
        this.id = -1;
        this.createDate = new Date();
        this.lastUpdateDate = new Date();
        this.prompt = "";
        this.negPrompt = "";
        this.style = "";
        this.imgPreview = null;
        this.cnMode = CN_MODE_IMG_SCRIBBLE;
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

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

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

    public Bitmap getImgReference(int sdSize) {
        double scale = 1.0;
        if (imgReference.getWidth() > imgReference.getHeight()) {
            scale = (double)sdSize / imgReference.getWidth();
        } else {
            scale = (double)sdSize / imgReference.getHeight();
        }
        return Bitmap.createScaledBitmap(imgReference, (int)round(imgReference.getWidth() * scale), (int)round(imgReference.getHeight() * scale), true);
    }

    public Bitmap getImgBackground(int sdSize) {
        double scale = 1.0;
        if (imgBackground.getWidth() > imgBackground.getHeight()) {
            scale = (double)sdSize / imgBackground.getWidth();
        } else {
            scale = (double)sdSize / imgBackground.getHeight();
        }
        return Bitmap.createScaledBitmap(imgBackground, (int)round(imgBackground.getWidth() * scale), (int)round(imgBackground.getHeight() * scale), true);
    }

    private RectF getInpaintRect(int sdSize) {
        int inpaintMargin = max(imgBackground.getWidth(), imgBackground.getHeight()) / 24;
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
        int inpaintWidth = x2 - x1;
        if (x2 + inpaintMargin < imgBackground.getWidth()) inpaintWidth += inpaintMargin;
        if (x1 - inpaintMargin > 0) inpaintWidth += inpaintMargin;
        if (inpaintWidth > imgBackground.getWidth()) inpaintWidth = imgBackground.getWidth();
        int inpaintHeight = y2 - y1;
        if (y2 + inpaintMargin < imgBackground.getHeight()) inpaintHeight += inpaintMargin;
        if (y1 - inpaintMargin > 0) inpaintHeight += inpaintMargin;
        if (inpaintHeight > imgBackground.getHeight()) inpaintHeight = imgBackground.getHeight();

        if (inpaintWidth >= inpaintHeight) {
            if (inpaintWidth > sdSize) {
                scale = (double) (inpaintWidth) / (sdSize);
            } else if (imgBackground.getWidth() < sdSize) {
                scale = (double) (imgBackground.getWidth()) / (sdSize);
            }
        } else {
            if (inpaintHeight > sdSize) {
                scale = (double) (inpaintHeight) / (sdSize);
            } else if (imgBackground.getHeight() < sdSize) {
                scale = (double) (imgBackground.getHeight()) / (sdSize);
            }
        }

        double blockWidth = sdBlockSize * scale;
        if (inpaintWidth >= inpaintHeight) {
            inpaintWidth = (int)round(sdSize * scale);
            if (inpaintHeight < (int)round(2d / 3d * inpaintWidth)) {
                inpaintHeight = min(imgBackground.getHeight(), (int)round(2d / 3d * inpaintWidth));
            }
            double heightBlock = ceil(inpaintHeight / blockWidth);
            inpaintHeight = (int)round(blockWidth * heightBlock);
            if (inpaintHeight > imgBackground.getHeight()) {
                inpaintHeight = imgBackground.getHeight();
                inpaintWidth = (int)round(sdSize * inpaintHeight / heightBlock / sdBlockSize);
            }
        } else {
            inpaintHeight = (int)round(sdSize * scale);
            if (inpaintWidth < (int)round(2d / 3d * inpaintHeight)) {
                inpaintWidth = min(imgBackground.getWidth(), (int)round(2d / 3d * inpaintHeight));
            }
            double widthBlock = ceil(inpaintWidth / blockWidth);
            inpaintWidth = (int)round(blockWidth * widthBlock);
            if (inpaintWidth > imgBackground.getWidth()) {
                inpaintWidth = imgBackground.getWidth();
                inpaintHeight = (int)round(sdSize * inpaintWidth / widthBlock / sdBlockSize);
            }
        }

        double left = (x1 + (x2-x1)/2d) - (inpaintWidth)/2d;
        double right = left + inpaintWidth;
        double top = (y1 + (y2-y1)/2d) - (inpaintHeight)/ 2d;
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
