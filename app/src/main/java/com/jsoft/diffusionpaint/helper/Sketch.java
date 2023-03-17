package com.jsoft.diffusionpaint.helper;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Path;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class Sketch implements Serializable {
    private Date createDate;
    private Date lastUpdateDate;
    private int id;
    private String prompt;
    private Bitmap imgPreview;

    public Sketch(int id, Date createDate, Date lastUpdateDate, String prompt, Bitmap imgPreview) {
        this.id = id;
        this.createDate = createDate;
        this.lastUpdateDate = lastUpdateDate;
        this.prompt = prompt;
        this.imgPreview = imgPreview;
    }

    public Sketch() {
        this.id = -1;
        this.createDate = new Date();
        this.lastUpdateDate = new Date();
        this.prompt = "";
        this.imgPreview = null;
    }

    public static String sketch2Json(Sketch s) {
        Gson gson = new Gson();
        String j = gson.toJson(s);
        return j;
    }

    public static Sketch json2Sketch(String jsonString) {
        Gson gson = new Gson();
        Sketch s = gson.fromJson(jsonString, Sketch.class);
        return s;
    }

    public Bitmap getImgPreview() {
        return imgPreview;
    }

    public void setImgPreview(Bitmap imgPreview) {
        this.imgPreview = imgPreview;
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

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
