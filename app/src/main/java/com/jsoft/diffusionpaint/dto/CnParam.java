package com.jsoft.diffusionpaint.dto;

public class CnParam {
    public String cnInputImage; //Background, Sketch
    public String cnModule;
    public String cnModelKey;
    public String cnModel;
    public int cnResizeMode = -1;
    public int cnControlMode;
    public double cnWeight;
    public double cnModuleParamA = Double.NaN;
    public double cnModuleParamB = Double.NaN;
    public double cnStart = 0.0;
    public double cnEnd = 1.0;

    public static final String[] CN_RESIZE_MODE = {"Just Resize", "Crop and Resize", "Resize and Fill"};

    public static final String[] CN_CONTROL_MODE = {"Balanced", "My prompt is more important", "ControlNet is more important"};
}
