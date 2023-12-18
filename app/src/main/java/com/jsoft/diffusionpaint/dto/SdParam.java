package com.jsoft.diffusionpaint.dto;

import java.util.List;

public class SdParam {
    public String type; //txt2img, img2img, inpaint
    public String baseImage; //for img2img and inpaint, Background, Sketch
    public double denoise;
    public int steps;
    public double cfgScale;
    public int inpaintFill; //for inpaint, Original / Noise
    public int inpaintPartial = 0;
    public int sdSize;
    public String model;

    public List<CnParam> cn;
    public static final String SD_MODEL_V1 = "v1Model";
    public static final String SD_MODEL_INPAINT = "v1Inpaint";
    public static final String SD_MODEL_SDXL_BASE = "sdxlBase";
    public static final String SD_MODEL_SDXL_REFINER = "sdxlRefiner";
    public static final String SD_MODEL_SDXL_TURBO = "sdxlTurbo";
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




}
