package com.jsoft.diffusionpaint.dto;

import com.jsoft.diffusionpaint.helper.SdApiHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SdParam {
    public String name;
    public String type; //txt2img, img2img, inpaint
    public String prompt = "";
    public String negPrompt = "";
    public String baseImage; //for img2img and inpaint, Background, Sketch
    public double denoise;
    public int steps;
    public double cfgScale;
    public int inpaintFill; //for inpaint, Original / Noise
    public int inpaintPartial = 0;
    public int sdSize;
    public int maskBlur = -1;

    public static final String SD_MODE_TYPE_TXT2IMG = "txt2img";
    public static final String SD_MODE_TYPE_IMG2IMG = "img2img";
    public static final String SD_MODE_TYPE_INPAINT = "inpaint";
    public static final String SD_INPUT_IMAGE_BACKGROUND = "background";
    public static final String SD_INPUT_IMAGE_SKETCH = "sketch";
    public static final String SD_INPUT_IMAGE_REF = "reference";
    public static final String SD_INPUT_IMAGE_BG_REF = "bg_ref";
    public static final int SD_INPAINT_FILL_ORIGINAL = 1;
    public static final int SD_INPAINT_FILL_NOISE = 2;
    public static final int INPAINT_FULL = 0;
    public static final int INPAINT_PARTIAL = 1;

    public static List<String> getModelKeyList() {
        List<String> paramList = new ArrayList<>(modeKeyList);
        return paramList;
    }

    public static final List<String> modeKeyList = new ArrayList<>(Arrays.asList(
            "\"name\":\"Custom Mode\"",
            "\"type\":\"txt2img\"",
            "\"type\":\"img2img\"",
            "\"type\":\"inpaint\"",
            "{\"type\":\"txt2img\"",
            "{\"type\":\"img2img\", \"denoise\":0.75, \"baseImage\":\"background\"",
            "{\"type\":\"inpaint\", \"denoise\":0.75, \"baseImage\":\"background\"",
            "\"steps\":30",
            "\"cfgScale\":7.0",
            "\"denoise\":0.75",
            "\"baseImage\":\"background\"",
            "\"baseImage\":\"sketch\"",
            "\"inpaintPartial\":1",
            "\"sdSize\":768",
            "\"sdSize\":1024",
            "\"sdSize\":1280",
            "\"maskBlur\":10",
            "\"prompt\":\"\"",
            "\"negPrompt\":\"\""
    ));


}
