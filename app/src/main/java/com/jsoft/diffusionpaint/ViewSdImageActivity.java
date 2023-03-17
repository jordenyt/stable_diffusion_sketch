package com.jsoft.diffusionpaint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

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
    private ConstraintLayout spinner_bg;
    private FloatingActionButton sdButton;
    private FloatingActionButton saveButton;
    private FloatingActionButton backButton;
    private Bitmap mBitmap;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault());
    OkHttpClient client = new OkHttpClient();
    private String baseUrl;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_sd_image);
        sdImage = findViewById(R.id.sd_image);
        db = new PaintDb(this);
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        baseUrl = sharedPreferences.getString("sdServerAddress", "");

        Intent i = getIntent();
        int sketchId = i.getIntExtra("sketchId", -1);
        if (sketchId >= 0) {
            Sketch dbSketch = db.getSketch(sketchId);
            if (dbSketch != null) {
                mCurrentSketch = dbSketch;
                mBitmap = mCurrentSketch.getImgPreview();
                sdImage.setImageBitmap(mBitmap);
            }
        }
        if (mCurrentSketch==null) {
            mCurrentSketch=new Sketch();
        }

        spinner_bg = findViewById(R.id.spinner_bg);
        sdButton = findViewById(R.id.fab_stable_diffusion2);
        saveButton = findViewById(R.id.fab_save2);
        backButton = findViewById(R.id.fab_back);

        sdButton.setOnClickListener(view -> callSD());

        backButton.setOnClickListener(view -> this.onBackPressed());

        saveButton.setOnClickListener(view -> {
            Utils.saveBitmapToExternalStorage(this,mBitmap,"sdsketch_" + mCurrentSketch.getId() + "_" + dateFormat.format(new Date()) + ".jpg");
            saveButton.setVisibility(View.GONE);
        });

        callSD();
    }

    private void callSD() {
        spinner_bg.setVisibility(View.VISIBLE);
        sdButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);

        JSONObject jsonObject = getControlnetImg2imgJSON(mCurrentSketch.getPrompt());
        sendPostRequest("cnimg2img", baseUrl + "/sdapi/v1/img2img", jsonObject);
    }

    private JSONObject getControlnetImg2imgJSON(String prompt) {
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
            jsonObject.put("width", 512);
            jsonObject.put("height", 512);
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
            cnArgObject.put("module", "scribble");
            cnArgObject.put("model", sharedPreferences.getString("cnScribbleModel","control_sd15_scribble [fef5e48e]"));
            cnArgObject.put("weight", 0);
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
        Log.d("diffusionPaint", responseBody);
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
    private void sendPostRequest(String requestType, String url, JSONObject jsonObject) {

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                ViewSdImageActivity.this.runOnUiThread(ViewSdImageActivity.this::onBackPressed);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        ViewSdImageActivity.this.runOnUiThread(ViewSdImageActivity.this::onBackPressed);
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
                            ViewSdImageActivity.this.onBackPressed();
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    ViewSdImageActivity.this.runOnUiThread(ViewSdImageActivity.this::onBackPressed);
                }
            }
        });
    }
}