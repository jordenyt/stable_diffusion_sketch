package com.jsoft.diffusionpaint.dto;

import static java.lang.Math.*;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.jsoft.diffusionpaint.helper.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    public static JSONArray comfyuiModes;
    public static final String CN_MODE_COMFYUI = "comfyui";
    public static final String CN_MODE_ORIGIN = "original";
    public static final String ASPECT_RATIO_LANDSCAPE = "landscape";
    public static final String ASPECT_RATIO_PORTRAIT = "portrait";
    public static final String ASPECT_RATIO_SQUARE = "square";
    public static final String ASPECT_RATIO_WIDE = "wide";
    public static final Map<String, String> defaultJSON;
    static {
        Map<String, String> json = new LinkedHashMap<>();
        json.put(CN_MODE_ORIGIN, "{\"type\":\"img2img\"}");
        defaultJSON = Collections.unmodifiableMap(json);
    }

    public static  Map<String, String> cnModeMap() {
        Map<String, String> cnMode = new LinkedHashMap<>();

        cnMode.put("Original / Fill with Reference", CN_MODE_ORIGIN);

        if (comfyuiModes != null) {
            for (int i = 0; i < comfyuiModes.length(); i++) {
                try {
                    JSONObject cfMode = comfyuiModes.getJSONObject(i);
                    if (cfMode.getBoolean("show")) {
                        cnMode.put(cfMode.getString("title"), CN_MODE_COMFYUI + cfMode.getString("name"));
                    }
                } catch (JSONException ignored) {
                }
            }
        }
        return Collections.unmodifiableMap(cnMode);
    }

    public static Map<String, String> txt2imgModeMap()  {
        Map<String, String> mode = new LinkedHashMap<>();

        if (comfyuiModes != null) {
            for (int i = 0; i < comfyuiModes.length(); i++) {
                try {
                    JSONObject cfMode = comfyuiModes.getJSONObject(i);
                    if (cfMode.getBoolean("showT2I")) {
                        mode.put(cfMode.getString("title"), CN_MODE_COMFYUI + cfMode.getString("name"));
                    }
                } catch (JSONException ignored) {
                }
            }
        }
        return Collections.unmodifiableMap(mode);
    }

    public Sketch() {
        this.id = -1;
        this.createDate = new Date();
        this.lastUpdateDate = new Date();
        this.prompt = "";
        this.negPrompt = "";
        this.imgPreview = null;
        this.cnMode = "";
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

    public static Bitmap getInpaintMaskFromPaint(Sketch s, int expandPixel, boolean blurBoundary) {
        Bitmap resizedBm = Bitmap.createScaledBitmap(s.getImgPaint(), s.getImgBackground().getWidth(), s.getImgBackground().getHeight(), false);
        return Utils.getDilationMask(resizedBm, expandPixel, blurBoundary);
    }

    public Bitmap getImgBgRefPreview() {
        Bitmap previewBitmap = Bitmap.createBitmap(imgBackground.getWidth(), imgBackground.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cvPreview = new Canvas(previewBitmap);
        cvPreview.drawBitmap(imgBackground, 0, 0, null);
        cvPreview.drawBitmap(imgPaint, 0, 0, null);
        return previewBitmap;
    }

    public Bitmap getImgBgRefPaint(int boundaryWidth) {
        Bitmap sketchBitmap = Bitmap.createScaledBitmap(imgPaint, imgBackground.getWidth(), imgBackground.getHeight(), false);
        int[] sketchPixels = new int[imgBackground.getWidth() * imgBackground.getHeight()];
        sketchBitmap.getPixels(sketchPixels, 0, sketchBitmap.getWidth(), 0, 0, sketchBitmap.getWidth(), sketchBitmap.getHeight());

        Bitmap paintBitmap = Bitmap.createBitmap(imgBackground.getWidth(), imgBackground.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cvPaint = new Canvas(paintBitmap);

        Paint boundaryPaint = new Paint();
        boundaryPaint.setColor(Color.argb(127,0,0,255));
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

    public Bitmap getImgBgRef(int blurBoundary) {
        return getImgBgMerge(Utils.outerFit(imgReference, imgBackground), 0, blurBoundary);
    }

    public Bitmap getImgBgMerge(Bitmap bmMerge, int boundary, int blurBoundary) {
        if (bmMerge != null) {
            Bitmap resizedImgPaint = Bitmap.createScaledBitmap(imgPaint, imgBackground.getWidth(), imgBackground.getHeight(), false);
            Bitmap imgDilatedMask = Utils.getDilationMask(resizedImgPaint, boundary, false);
            if (blurBoundary > 0) {
                imgDilatedMask = Utils.getDilationMask(Utils.changeBlackPixelsToTransparent(imgDilatedMask), blurBoundary, true);
            }

            try {
                return Utils.mergeBitmaps(imgBackground, bmMerge, imgDilatedMask);
            } catch  (IllegalArgumentException e) {
                return bmMerge;
            }
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
        int sdBlockSize = 8;
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
