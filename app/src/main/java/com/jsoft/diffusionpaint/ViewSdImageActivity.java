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

public class ViewSdImageActivity extends AppCompatActivity {

    private Sketch mCurrentSketch;
    private PaintDb db;
    private ImageView sdImage;
    private LinearLayout spinner_bg;
    private FloatingActionButton sdButton;
    private FloatingActionButton saveButton;
    private FloatingActionButton backButton;
    private Bitmap mBitmap;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault());
    OkHttpClient client = new OkHttpClient();
    private String baseUrl;
    private SharedPreferences sharedPreferences;
    private String aspectRatio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Intent i = getIntent();
        int sketchId = i.getIntExtra("sketchId", -1);
        String cnMode = i.getStringExtra("cnMode");


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

        if (mBitmap != null) {
            sdImage.setImageBitmap(mBitmap);
        }

        spinner_bg = findViewById(R.id.spinner_bg);
        sdButton = findViewById(R.id.fab_stable_diffusion2);
        saveButton = findViewById(R.id.fab_save2);
        backButton = findViewById(R.id.fab_back);

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
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (aspectRatio.equals("landscape")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void callSD(String cnMode) {
        spinner_bg.setVisibility(View.VISIBLE);
        sdButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);

        JSONObject jsonObject = getControlnetImg2imgJSON(mCurrentSketch.getPrompt(), cnMode);
        sendPostRequest("cnimg2img", baseUrl + "/sdapi/v1/img2img", jsonObject);
    }

    private JSONObject getControlnetImg2imgJSON(String prompt, String cnMode) {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray init_images = new JSONArray();
            init_images.put(Utils.bitmap2Base64String(mCurrentSketch.getImgPreview()));
            jsonObject.put("init_images", init_images);
            jsonObject.put("resize_mode", 1);
            jsonObject.put("denoising_strength", 0.8);
            jsonObject.put("image_cfg_scale", 7);
            //jsonObject.put("mask", "string");
            jsonObject.put("mask_blur", 4);
            jsonObject.put("inpainting_fill", 0);
            jsonObject.put("inpaint_full_res", true);
            jsonObject.put("inpaint_full_res_padding", 0);
            jsonObject.put("inpainting_mask_invert", 0);
            //jsonObject.put("initial_noise_multiplier", 0);
            jsonObject.put("prompt", sharedPreferences.getString("promptPrefix", "") + " " + prompt + ", " + sharedPreferences.getString("promptPostfix", ""));
            jsonObject.put("styles", new JSONArray());
            jsonObject.put("seed", -1);
            jsonObject.put("subseed", -1);
            jsonObject.put("subseed_strength", 1);
            jsonObject.put("seed_resize_from_h", -1);
            jsonObject.put("seed_resize_from_w", -1);
            //jsonObject.put("sampler_name", "string");
            jsonObject.put("batch_size", 1);
            jsonObject.put("n_iter", 1);
            jsonObject.put("steps", 40);
            jsonObject.put("cfg_scale", 7);
            if (aspectRatio.equals("portrait")) {
                jsonObject.put("width", sharedPreferences.getInt("sdImageSize", 512) * 3 / 4);
            } else {
                jsonObject.put("width", sharedPreferences.getInt("sdImageSize", 512));
            }
            if (aspectRatio.equals("landscape")) {
                jsonObject.put("height", sharedPreferences.getInt("sdImageSize", 512) * 3 / 4);
            } else {
                jsonObject.put("height", sharedPreferences.getInt("sdImageSize", 512));
            }
            jsonObject.put("restore_faces", false);
            jsonObject.put("tiling", false);
            jsonObject.put("do_not_save_samples", false);
            jsonObject.put("do_not_save_grid", true);
            jsonObject.put("negative_prompt", sharedPreferences.getString("negativePrompt", ""));
            jsonObject.put("eta", 0);
            jsonObject.put("s_churn", 0);
            jsonObject.put("s_tmax", 0);
            jsonObject.put("s_tmin", 0);
            jsonObject.put("s_noise", 1);
            jsonObject.put("override_settings", new JSONObject());
            jsonObject.put("override_settings_restore_afterwards", true);
            jsonObject.put("script_args", new JSONArray());
            jsonObject.put("sampler_index", "Euler a");
            jsonObject.put("include_init_images", false);
            //jsonObject.put("script_name", "string");
            jsonObject.put("send_images", true);
            jsonObject.put("save_images", false);
            JSONObject alwayson_scripts = new JSONObject();
            JSONObject controlnet = new JSONObject();
            JSONArray args = new JSONArray();
            JSONObject cnArgObject = new JSONObject();
            cnArgObject.put("input_image", Utils.bitmap2Base64String(mCurrentSketch.getImgPreview()));
            //cnArgObject.put("mask", "");
            switch (cnMode) {
                case Sketch.CN_MODE_SCRIBBLE:
                    cnArgObject.put("module", "none");
                    cnArgObject.put("model", sharedPreferences.getString("cnScribbleModel","control_sd15_scribble [fef5e48e]"));
                    cnArgObject.put("weight", 0.2);
                    break;
                case Sketch.CN_MODE_DEPTH:
                    cnArgObject.put("module", "depth");
                    cnArgObject.put("model", sharedPreferences.getString("cnDepthModel","control_sd15_depth [fef5e48e]"));
                    cnArgObject.put("weight", 0.7);
                    break;
                case Sketch.CN_MODE_POSE:
                    cnArgObject.put("module", "openpose");
                    cnArgObject.put("model", sharedPreferences.getString("cnPoseModel","control_sd15_openpose [fef5e48e]"));
                    cnArgObject.put("weight", 1);
                    break;
            }
            cnArgObject.put("resize_mode", "Scale to Fit (Inner Fit)");
            cnArgObject.put("lowvram", false);
            cnArgObject.put("processor_res", 64);
            cnArgObject.put("threshold_a", 64);
            cnArgObject.put("threshold_b", 64);
            cnArgObject.put("guidance", 1);
            cnArgObject.put("guidance_start", 0);
            cnArgObject.put("guidance_end", 1);
            cnArgObject.put("guessmode", false);
            args.put(cnArgObject);
            controlnet.put("args", args);
            alwayson_scripts.put("controlnet", controlnet);
            jsonObject.put("alwayson_scripts", alwayson_scripts);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private void handleResponse(String requestType, String responseBody) throws IOException, JSONException {
        //Log.d("diffusionPaint", responseBody);
        if ("cnimg2img".equals(requestType)) {
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
        }
    }

    private void callSDFailure() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Fail to call Stable Diffusion Server.")
                .setTitle("Magic Failed")
                .setPositiveButton("OK", (dialog, id) -> ViewSdImageActivity.this.onBackPressed());
        AlertDialog alert = builder.create();
        alert.show();
    }
    private void sendPostRequest(String requestType, String url, JSONObject jsonObject) {

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                ViewSdImageActivity.this.runOnUiThread(() -> callSDFailure());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        ViewSdImageActivity.this.runOnUiThread(() -> callSDFailure());
                    }

                    Headers responseHeaders = response.headers();
                    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                        Log.d("diffusionPaint", responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }

                    assert responseBody != null;
                    String responseString = responseBody.string();
                    ViewSdImageActivity.this.runOnUiThread(() -> {
                        try {
                            handleResponse(requestType, responseString);
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                            callSDFailure();
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    ViewSdImageActivity.this.runOnUiThread(() -> callSDFailure());
                }
            }
        });
    }
}