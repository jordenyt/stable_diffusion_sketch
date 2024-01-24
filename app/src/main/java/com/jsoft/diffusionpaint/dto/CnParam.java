package com.jsoft.diffusionpaint.dto;

public class CnParam {

    public String cnInputImage; //Background, Sketch
    public String cnModule;
    public String cnModelKey;
    public int cnResizeMode = -1;
    public int cnControlMode;
    public double cnWeight;
    public double cnModuleParamA;
    public double cnModuleParamB;
    public double cnStart = 0.0;
    public double cnEnd = 1.0;

    public static final String CN_RESIZE_MODE_RESIZE = "Just Resize";
    public static final String CN_RESIZE_MODE_CROP = "Crop and Resize";
    public static final String CN_RESIZE_MODE_FILL = "Resize and Fill";
}
