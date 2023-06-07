package com.jsoft.diffusionpaint.dto;

public class CnParam {

    public String cnInputImage; //Background, Sketch
    public String cnModule;
    public String cnModelKey;
    public int cnControlMode;
    public double cnWeight;
    public double cnModuleParamA;
    public double cnModuleParamB;

    public static final String CN_MODE_BALANCED = "Balanced";
    public static final String CN_MODE_PROMPT = "My prompt is more important";
    public static final String CN_MODE_CONTROLNET = "ControlNet is more important";

}
