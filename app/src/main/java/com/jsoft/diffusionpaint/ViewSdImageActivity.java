package com.jsoft.diffusionpaint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jsoft.diffusionpaint.helper.PaintDb;
import com.jsoft.diffusionpaint.helper.SdApiHelper;
import com.jsoft.diffusionpaint.helper.SdApiResponseListener;
import com.jsoft.diffusionpaint.helper.Sketch;
import com.jsoft.diffusionpaint.helper.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ViewSdImageActivity extends AppCompatActivity implements SdApiResponseListener {

    private Sketch mCurrentSketch;
    private PaintDb db;
    private ImageView sdImage;
    private LinearLayout spinner_bg;
    private FloatingActionButton sdButton;
    private FloatingActionButton saveButton;
    private FloatingActionButton backButton;
    private Bitmap mBitmap;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault());
    private String baseUrl;
    private SharedPreferences sharedPreferences;
    private String aspectRatio;
    private SdApiHelper sdApiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Intent i = getIntent();
        int sketchId = i.getIntExtra("sketchId", -1);
        String cnMode = i.getStringExtra("cnMode");
        this.sdApiHelper = new SdApiHelper(this, this);

        db = new PaintDb(this);
        baseUrl = sharedPreferences.getString("sdServerAddress", "");

        if (sketchId >= 0) {
            Sketch dbSketch = db.getSketch(sketchId);
            if (dbSketch != null) {
                mCurrentSketch = dbSketch;
                mCurrentSketch.setCnMode(cnMode);
                mBitmap = mCurrentSketch.getImgPreview();
                if (mCurrentSketch.getImgPreview().getWidth() > mCurrentSketch.getImgPreview().getHeight()) {
                    aspectRatio = "landscape";
                } else if (mCurrentSketch.getImgPreview().getWidth() < mCurrentSketch.getImgPreview().getHeight()) {
                    aspectRatio = "portrait";
                } else {
                    aspectRatio = "square";
                }
            }
        }
        if (mCurrentSketch==null) {
            mCurrentSketch=new Sketch();
        }
        setScreenRotation();

        setContentView(R.layout.activity_view_sd_image);
        sdImage = findViewById(R.id.sd_image);
        spinner_bg = findViewById(R.id.spinner_bg);
        sdButton = findViewById(R.id.fab_stable_diffusion2);
        saveButton = findViewById(R.id.fab_save2);
        backButton = findViewById(R.id.fab_back);

        if (mBitmap != null) {
            sdImage.setImageBitmap(mBitmap);
        }

        sdButton.setOnClickListener(view -> callSD(mCurrentSketch.getCnMode()));

        backButton.setOnClickListener(view -> this.onBackPressed());

        saveButton.setOnClickListener(view -> {
            Utils.saveBitmapToExternalStorage(this,mBitmap,"sdsketch_" + mCurrentSketch.getId() + "_" + dateFormat.format(new Date()) + ".jpg");
            saveButton.setVisibility(View.GONE);
        });

        callSD(mCurrentSketch.getCnMode());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Lock the orientation to portrait
        setScreenRotation();
    }

    public void setScreenRotation() {
        if (aspectRatio.equals("portrait")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else if (aspectRatio.equals("landscape")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private void callSD(String cnMode) {
        spinner_bg.setVisibility(View.VISIBLE);
        sdButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);

        if (cnMode.startsWith("txt")) {
            JSONObject jsonObject = sdApiHelper.getControlnetTxt2imgJSON(mCurrentSketch.getPrompt(), cnMode, mCurrentSketch, aspectRatio);
            sdApiHelper.sendPostRequest("cntxt2img", baseUrl + "/sdapi/v1/txt2img", jsonObject);
        } else {
            JSONObject jsonObject = sdApiHelper.getControlnetImg2imgJSON(mCurrentSketch.getPrompt(), cnMode, mCurrentSketch, aspectRatio);
            sdApiHelper.sendPostRequest("cnimg2img", baseUrl + "/sdapi/v1/img2img", jsonObject);
        }
    }

    @Override
    public void onSdApiFailure() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Call Stable Diffusion API failed")
                .setTitle("Magic Failed")
                .setPositiveButton("OK", (dialog, id) -> ViewSdImageActivity.this.onBackPressed());
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onSdApiResponse(String requestType, String responseBody) {
        if ("cnimg2img".equals(requestType) || "cntxt2img".equals(requestType)) {
            try {
                JSONObject jsonObject = new JSONObject(responseBody);
                JSONArray images = jsonObject.getJSONArray("images");
                if (images.length() > 0) {
                    mBitmap = Utils.base64String2Bitmap((String) images.get(0));
                    sdImage.setImageBitmap(mBitmap);
                }
                spinner_bg.setVisibility(View.GONE);
                sdButton.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.VISIBLE);
                backButton.setVisibility(View.VISIBLE);
            } catch (JSONException e) {
                e.printStackTrace();
                onSdApiFailure();
            }
        }
    }
}