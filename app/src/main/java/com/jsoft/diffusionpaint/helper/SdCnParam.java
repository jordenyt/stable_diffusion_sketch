package com.jsoft.diffusionpaint.helper;

/*import org.json.JSONException;
import org.json.JSONObject;*/

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
    public double cnWeight;

    public static final String SD_MODE_TYPE_TXT2IMG = "txt2img";
    public static final String SD_MODE_TYPE_IMG2IMG = "img2img";
    public static final String SD_MODE_TYPE_INPAINT = "inpaint";
    public static final String SD_INPUT_IMAGE_BACKGROUND = "background";
    public static final String SD_INPUT_IMAGE_SKETCH = "sketch";
    public static final String SD_INPUT_IMAGE_MASK = "mask";
    public static final int SD_INPAINT_FILL_ORIGINAL = 1;
    public static final int SD_INPAINT_FILL_NOISE = 2;

    /*public void setParam(JSONObject jsonObject) throws JSONException {
        this.name = jsonObject.getString("name");
        this.type = jsonObject.getString("type");
        this.baseImage = jsonObject.getString("baseImage");
        this.denoise = jsonObject.getDouble("denoise");
        this.inpaintFill = jsonObject.getInt("inpaintFill");
        this.cnInputImage = jsonObject.getString("cnInputImage");
        this.cnModule = jsonObject.getString("cnModule");
        this.cnModelKey = jsonObject.getString("cnModelKey");
        this.cnWeight = jsonObject.getDouble("cnWeight");
    }*/

}
