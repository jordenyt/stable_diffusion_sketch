package com.jsoft.diffusionpaint.dto;

public class SdCnParam {
    public String type; //txt2img, img2img, inpaint
    public String baseImage; //for img2img and inpaint, Background, Sketch
    public double denoise;
    public int steps;
    public double cfgScale;
    public int inpaintFill; //for inpaint, Original / Noise
    public String cnInputImage; //Background, Sketch
    public String cnModule;
    public String cnModelKey;
    public int cnControlMode;
    public double cnWeight;
    public int inpaintPartial = 0;
    public int sdSize;

    public static final String SD_MODE_TYPE_TXT2IMG = "txt2img";
    public static final String SD_MODE_TYPE_IMG2IMG = "img2img";
    public static final String SD_MODE_TYPE_INPAINT = "inpaint";
    public static final String SD_INPUT_IMAGE_BACKGROUND = "background";
    public static final String SD_INPUT_IMAGE_SKETCH = "sketch";
    public static final String SD_INPUT_IMAGE_REF = "reference";
    public static final int SD_INPAINT_FILL_ORIGINAL = 1;
    public static final int SD_INPAINT_FILL_NOISE = 2;
    public static final int INPAINT_FULL = 0;
    public static final int INPAINT_PARTIAL = 1;

    public static final String CN_MODE_BALANCED = "Balanced";
    public static final String CN_MODE_PROMPT = "My prompt is more important";
    public static final String CN_MODE_CONTROLNET = "ControlNet is more important";


}
