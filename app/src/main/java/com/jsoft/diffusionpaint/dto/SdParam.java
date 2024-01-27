package com.jsoft.diffusionpaint.dto;

import java.util.ArrayList;
import java.util.Arrays;
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
    public int clipSkip;
    public String model;
    public String sampler;

    public List<CnParam> cn;
    public static final String SD_MODEL_V1 = "v1Model";
    public static final String SD_MODEL_INPAINT = "v1Inpaint";
    public static final String SD_MODEL_SDXL_BASE = "sdxlBase";
    public static final String SD_MODEL_SDXL_TURBO = "sdxlTurbo";
    public static final String SD_MODEL_SDXL_INPAINT = "sdxlInpaint";
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
    public static List<String> cnModelList = null;
    public static List<String> cnModuleList = null;
    public static List<String> samplerList = null;

    public static List<String> getModelKeyList() {
        List<String> paramList = new ArrayList<>(modeKeyList);
        if (cnModelList != null) {
            for (String s : cnModelList) {
                paramList.add("\"cnModel\":\"" + s + "\"");
            }
        }
        if (cnModuleList != null) {
            for (String s : cnModuleList) {
                paramList.add("\"cnModule\":\"" + s + "\"");
            }
        }
        if (samplerList != null) {
            for (String s : samplerList) {
                paramList.add("\"sampler\":\"" + s + "\"");
            }
        }
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
            "\"model\":\"v1Model\"",
            "\"model\":\"v1Inpaint\"",
            "\"model\":\"sdxlBase\"",
            "\"model\":\"sdxlTurbo\"",
            "\"model\":\"sdxlInpaint\"",
            "\"denoise\":0.75",
            "\"baseImage\":\"background\"",
            "\"baseImage\":\"sketch\"",
            "\"inpaintFill\":0",
            "\"inpaintFill\":1",
            "\"inpaintFill\":2",
            "\"inpaintFill\":3",
            "\"inpaintPartial\":1",
            "\"sdSize\":768",
            "\"sdSize\":1024",
            "\"sdSize\":1280",
            "\"clipSkip\":1",
            "\"clipSkip\":2",
            "\"cn\":[{\"cnInputImage\":\"background\"",
            "\"cn\":[{\"cnInputImage\":\"sketch\"",
            "\"cn\":[{\"cnInputImage\":\"reference\"",
            "{\"cnInputImage\":\"background\"",
            "{\"cnInputImage\":\"sketch\"",
            "{\"cnInputImage\":\"reference\"",
            "\"cnInputImage\":\"background\"",
            "\"cnInputImage\":\"sketch\"",
            "\"cnInputImage\":\"reference\"",
            "\"cnModelKey\":\"cnTileModel\"",
            "\"cnModelKey\":\"cnPoseModel\"",
            "\"cnModelKey\":\"cnCannyModel\"",
            "\"cnModelKey\":\"cnScribbleModel\"",
            "\"cnModelKey\":\"cnDepthModel\"",
            "\"cnModelKey\":\"cnDepthModel\"",
            "\"cnModelKey\":\"cnNormalModel\"",
            "\"cnModelKey\":\"cnMlsdModel\"",
            "\"cnModelKey\":\"cnLineartModel\"",
            "\"cnModelKey\":\"cnSoftedgeModel\"",
            "\"cnModelKey\":\"cnSegModel\"",
            "\"cnModelKey\":\"cnIPAdapterModel\"",
            "\"cnModelKey\":\"cnxlIPAdapterModel\"",
            "\"cnModelKey\":\"cnOther1Model\"",
            "\"cnModelKey\":\"cnOther2Model\"",
            "\"cnModelKey\":\"cnOther3Model\"",
            "\"cnModuleParamA\":8.0",
            "\"cnModuleParamB\":1.0",
            "\"cnControlMode\":0",
            "\"cnControlMode\":1",
            "\"cnControlMode\":2",
            "\"cnWeight\":1.0",
            "\"cnResizeMode\":0",
            "\"cnResizeMode\":1",
            "\"cnResizeMode\":2",
            "\"cnStart\":0.0",
            "\"cnEnd\":1.0",
            "\"prompt\":\"\"",
            "\"negPrompt\":\"\""
    ));


}
