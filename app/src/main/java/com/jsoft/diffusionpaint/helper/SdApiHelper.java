package com.jsoft.diffusionpaint.helper;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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

public class SdApiHelper {
    private SharedPreferences sharedPreferences;
    private Activity activity;
    private SdApiResponseListener listener;
    OkHttpClient client = new OkHttpClient();
    private String baseUrl;
    public SdApiHelper(Activity activity, SdApiResponseListener listener) {
        this.activity  = activity;
        this.listener = listener;
        sharedPreferences = activity.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
    }

    public void sendGetRequest(String requestType, String url) {
        sendRequest(requestType, url, null, "GET");
    }

    public void sendPostRequest(String requestType, String url, JSONObject jsonObject) {
        sendRequest(requestType, url, jsonObject, "POST");
    }

    private void sendRequest(String requestType, String url, JSONObject jsonObject, String httpMethod) {
        baseUrl = sharedPreferences.getString("sdServerAddress", "");
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
        Request.Builder requestBuilder = new Request.Builder()
                .url(baseUrl + url);
        if ("GET".equals(httpMethod)) {
            requestBuilder.get();
        } else {
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
            requestBuilder.post(body);
        }
        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> listener.onSdApiFailure(requestType));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        activity.runOnUiThread(() -> listener.onSdApiFailure(requestType));
                    }

                    Headers responseHeaders = response.headers();
                    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                        Log.d("diffusionPaint", responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }

                    assert responseBody != null;
                    String responseString = responseBody.string();
                    activity.runOnUiThread(() -> listener.onSdApiResponse(requestType, responseString));

                } catch (IOException e) {
                    e.printStackTrace();
                    activity.runOnUiThread(() -> listener.onSdApiFailure(requestType));
                }
            }
        });
    }

    public JSONObject getExtraSingleImageJSON(Bitmap bitmap) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("resize_mode", 0);
            //jsonObject.put("show_extras_results", true);
            jsonObject.put("gfpgan_visibility", 1);
            jsonObject.put("codeformer_visibility", 0);
            jsonObject.put("codeformer_weight", 0);
            jsonObject.put("upscaling_resize", 4);
            //jsonObject.put("upscaling_resize_w", 512);
            //jsonObject.put("upscaling_resize_h", 512);
            //jsonObject.put("upscaling_crop", true);
            jsonObject.put("upscaler_1", sharedPreferences.getString("sdUpscaler", "R-ESRGAN General 4xV3"));
            jsonObject.put("upscaler_2", "None");
            jsonObject.put("extras_upscaler_2_visibility", 0);
            jsonObject.put("upscale_first", true);
            jsonObject.put("image", Utils.jpg2Base64String(bitmap));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject getControlnetTxt2imgJSON(String prompt, String cnMode, Sketch mCurrentSketch, String aspectRatio) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("prompt", sharedPreferences.getString("promptPrefix", "") + " " + prompt + ", " + sharedPreferences.getString("promptPostfix", ""));
            jsonObject.put("seed", -1);
            jsonObject.put("batch_size", 1);
            jsonObject.put("n_iter", 1);
            jsonObject.put("steps", 40);
            jsonObject.put("cfg_scale", 7);
            if (aspectRatio.equals(Sketch.ASPECT_RATIO_PORTRAIT)) {
                jsonObject.put("width", sharedPreferences.getInt("sdImageSize", 512) * 3 / 4);
            } else {
                jsonObject.put("width", sharedPreferences.getInt("sdImageSize", 512));
            }
            if (aspectRatio.equals(Sketch.ASPECT_RATIO_LANDSCAPE)) {
                jsonObject.put("height", sharedPreferences.getInt("sdImageSize", 512) * 3 / 4);
            } else {
                jsonObject.put("height", sharedPreferences.getInt("sdImageSize", 512));
            }
            jsonObject.put("restore_faces", false);
            jsonObject.put("tiling", false);
            jsonObject.put("do_not_save_samples", true);
            jsonObject.put("do_not_save_grid", true);
            jsonObject.put("negative_prompt", sharedPreferences.getString("negativePrompt", ""));
            jsonObject.put("sampler_index", sharedPreferences.getString("sdSampler", "Euler a"));
            jsonObject.put("save_images", false);
            JSONObject alwayson_scripts = new JSONObject();

            if (!cnMode.equals(Sketch.CN_MODE_TXT)) {
                // ControlNet Args
                JSONObject controlnet = new JSONObject();
                JSONArray args = new JSONArray();
                JSONObject cnArgObject = new JSONObject();
                cnArgObject.put("input_image", Utils.jpg2Base64String(mCurrentSketch.getImgPreview()));
                //cnArgObject.put("mask", "");
                switch (cnMode) {
                    case Sketch.CN_MODE_TXT_CANNY:
                        cnArgObject.put("module", "canny");
                        cnArgObject.put("model", sharedPreferences.getString("cnCannyModel", "control_sd15_canny [fef5e48e]"));
                        cnArgObject.put("weight", 1);
                        break;
                    case Sketch.CN_MODE_TXT_SCRIBBLE:
                        cnArgObject.put("module", "scribble");
                        cnArgObject.put("model", sharedPreferences.getString("cnScribbleModel", "control_sd15_scribble [fef5e48e]"));
                        cnArgObject.put("weight", 0.7);
                        break;
                    case Sketch.CN_MODE_TXT_DEPTH:
                        cnArgObject.put("module", "depth");
                        cnArgObject.put("model", sharedPreferences.getString("cnDepthModel", "control_sd15_depth [fef5e48e]"));
                        cnArgObject.put("weight", 1);
                        break;
                }
                cnArgObject.put("resize_mode", "Inner Fit (Scale to Fit)");
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

            }
            jsonObject.put("alwayson_scripts", alwayson_scripts);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject getControlnetImg2imgJSON(String prompt, String cnMode, Sketch mCurrentSketch, String aspectRatio) {
        JSONObject jsonObject = new JSONObject();
        boolean isInpaint = cnMode.startsWith("inpaint");
        try {
            JSONArray init_images = new JSONArray();
            init_images.put(Utils.jpg2Base64String(cnMode.equals(Sketch.CN_MODE_INPAINT)?mCurrentSketch.getImgBackground():mCurrentSketch.getImgPreview()));
            jsonObject.put("init_images", init_images);
            jsonObject.put("resize_mode", 1);
            jsonObject.put("denoising_strength", cnMode.equals(Sketch.CN_MODE_INPAINT)?1:0.8);
            jsonObject.put("image_cfg_scale", 7);
            if (isInpaint) {
                if (mCurrentSketch.getImgInpaint() == null) {
                    mCurrentSketch.setImgInpaint(Sketch.getInpaintMaskFromPaint(mCurrentSketch));
                }
                jsonObject.put("mask", Utils.png2Base64String(mCurrentSketch.getImgInpaint()));
                jsonObject.put("mask_blur", 10);
                jsonObject.put("inpainting_fill", cnMode.equals(Sketch.CN_MODE_INPAINT)?2:1);
                jsonObject.put("inpaint_full_res", false);
                jsonObject.put("inpaint_full_res_padding", 32);
                //jsonObject.put("inpainting_mask_invert", 0);
                jsonObject.put("initial_noise_multiplier", 1);
            }
            jsonObject.put("prompt", sharedPreferences.getString("promptPrefix", "") + " " + prompt + ", " + sharedPreferences.getString("promptPostfix", ""));
            jsonObject.put("seed", -1);
            jsonObject.put("batch_size", 1);
            jsonObject.put("n_iter", 1);
            jsonObject.put("cfg_scale", 7);
            if (aspectRatio.equals(Sketch.ASPECT_RATIO_PORTRAIT)) {
                jsonObject.put("width", sharedPreferences.getInt("sdImageSize", 512) * 3 / 4);
            } else {
                jsonObject.put("width", sharedPreferences.getInt("sdImageSize", 512));
            }
            if (aspectRatio.equals(Sketch.ASPECT_RATIO_LANDSCAPE)) {
                jsonObject.put("height", sharedPreferences.getInt("sdImageSize", 512) * 3 / 4);
            } else {
                jsonObject.put("height", sharedPreferences.getInt("sdImageSize", 512));
            }
            jsonObject.put("restore_faces", false);
            jsonObject.put("tiling", false);
            jsonObject.put("do_not_save_samples", true);
            jsonObject.put("do_not_save_grid", true);
            jsonObject.put("negative_prompt", sharedPreferences.getString("negativePrompt", ""));
            jsonObject.put("steps", 50);
            jsonObject.put("sampler_index", sharedPreferences.getString("sdSampler", "Euler a"));
            jsonObject.put("save_images", false);
            JSONObject alwayson_scripts = new JSONObject();

            // ControlNet Args
            if (!isInpaint || cnMode.equals(Sketch.CN_MODE_INPAINT_DEPTH)) {
                JSONObject controlnet = new JSONObject();
                JSONArray args = new JSONArray();
                JSONObject cnArgObject = new JSONObject();
                cnArgObject.put("input_image", Utils.jpg2Base64String(
                        cnMode.equals(Sketch.CN_MODE_INPAINT_DEPTH)?mCurrentSketch.getImgBackground():mCurrentSketch.getImgPreview()));
                //cnArgObject.put("mask", "");
                switch (cnMode) {
                    case Sketch.CN_MODE_SCRIBBLE:
                        cnArgObject.put("module", "none");
                        cnArgObject.put("model", sharedPreferences.getString("cnScribbleModel", "control_sd15_scribble [fef5e48e]"));
                        cnArgObject.put("weight", 0.7);
                        break;
                    case Sketch.CN_MODE_DEPTH:
                    case Sketch.CN_MODE_INPAINT_DEPTH:
                        cnArgObject.put("module", "depth");
                        cnArgObject.put("model", sharedPreferences.getString("cnDepthModel", "control_sd15_depth [fef5e48e]"));
                        cnArgObject.put("weight", 1);
                        break;
                    case Sketch.CN_MODE_POSE:
                        cnArgObject.put("module", "openpose");
                        cnArgObject.put("model", sharedPreferences.getString("cnPoseModel", "control_sd15_openpose [fef5e48e]"));
                        cnArgObject.put("weight", 1);
                        break;
                }
                cnArgObject.put("resize_mode", "Inner Fit (Scale to Fit)");
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
            }
            jsonObject.put("alwayson_scripts", alwayson_scripts);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

}
