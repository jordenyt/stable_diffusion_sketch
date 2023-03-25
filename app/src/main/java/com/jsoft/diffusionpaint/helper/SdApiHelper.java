package com.jsoft.diffusionpaint.helper;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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


    public void sendRequest(String requestType, String url, JSONObject jsonObject, String httpMethod) {
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

    public JSONObject getControlnetTxt2imgJSON(String prompt, String cnMode, Sketch mCurrentSketch, String aspectRatio) {
        JSONObject jsonObject = new JSONObject();
        try {
            //jsonObject.put("enable_hr", false);
            //jsonObject.put("denoising_strength", 0);
            //jsonObject.put("firstphase_width", 0);
            //jsonObject.put("firstphase_height", 0);
            //jsonObject.put("hr_scale", 2);
            //jsonObject.put("hr_upscaler", "R-ESRGAN General 4xV3");
            //jsonObject.put("hr_second_pass_steps", 0);
            //jsonObject.put("hr_resize_x", 0);
            //jsonObject.put("hr_resize_y", 0);
            jsonObject.put("prompt", sharedPreferences.getString("promptPrefix", "") + " " + prompt + ", " + sharedPreferences.getString("promptPostfix", ""));
            //jsonObject.put("styles", new JSONArray());
            jsonObject.put("seed", -1);
            //jsonObject.put("subseed", -1);
            //jsonObject.put("subseed_strength", 0);
            //jsonObject.put("seed_resize_from_h", -1);
            //jsonObject.put("seed_resize_from_w", -1);
            //jsonObject.put("sampler_name", "Euler a");
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
            //jsonObject.put("eta", 0);
            //jsonObject.put("s_churn", 0);
            //jsonObject.put("s_tmax", 0);
            //jsonObject.put("s_tmin", 0);
            //jsonObject.put("s_noise", 1);
            //jsonObject.put("override_settings", new JSONObject());
            //jsonObject.put("override_settings_restore_afterwards", true);
            //jsonObject.put("script_args", new JSONArray());
            jsonObject.put("sampler_index", "Euler a");
            //jsonObject.put("script_name", "string");
            //jsonObject.put("send_images", true);
            jsonObject.put("save_images", false);
            JSONObject alwayson_scripts = new JSONObject();
            JSONObject controlnet = new JSONObject();
            JSONArray args = new JSONArray();
            JSONObject cnArgObject = new JSONObject();
            cnArgObject.put("input_image", Utils.jpg2Base64String(mCurrentSketch.getImgPreview()));
            //cnArgObject.put("mask", "");
            switch (cnMode) {
                case Sketch.CN_MODE_TXT_CANNY:
                    cnArgObject.put("module", "canny");
                    cnArgObject.put("model", sharedPreferences.getString("cnCannyModel","control_sd15_canny [fef5e48e]"));
                    cnArgObject.put("weight", 1);
                    break;
                case Sketch.CN_MODE_TXT_SCRIBBLE:
                    cnArgObject.put("module", "scribble");
                    cnArgObject.put("model", sharedPreferences.getString("cnScribbleModel","control_sd15_scribble [fef5e48e]"));
                    cnArgObject.put("weight", 0.8);
                    break;
                case Sketch.CN_MODE_TXT_DEPTH:
                    cnArgObject.put("module", "depth");
                    cnArgObject.put("model", sharedPreferences.getString("cnDepthModel","control_sd15_depth [fef5e48e]"));
                    cnArgObject.put("weight", 0.8);
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

    public JSONObject getControlnetImg2imgJSON(String prompt, String cnMode, Sketch mCurrentSketch, String aspectRatio) {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONArray init_images = new JSONArray();
            init_images.put(Utils.jpg2Base64String(mCurrentSketch.getImgPreview()));
            jsonObject.put("init_images", init_images);
            jsonObject.put("resize_mode", 1);
            jsonObject.put("denoising_strength", 0.8);
            jsonObject.put("image_cfg_scale", 7);
            //jsonObject.put("mask", "string");
            jsonObject.put("mask_blur", 4);
            //jsonObject.put("inpainting_fill", 0);
            //jsonObject.put("inpaint_full_res", true);
            //jsonObject.put("inpaint_full_res_padding", 0);
            //jsonObject.put("inpainting_mask_invert", 0);
            //jsonObject.put("initial_noise_multiplier", 0);
            jsonObject.put("prompt", sharedPreferences.getString("promptPrefix", "") + " " + prompt + ", " + sharedPreferences.getString("promptPostfix", ""));
            //jsonObject.put("styles", new JSONArray());
            jsonObject.put("seed", -1);
            //jsonObject.put("subseed", -1);
            //jsonObject.put("subseed_strength", 1);
            //jsonObject.put("seed_resize_from_h", -1);
            //jsonObject.put("seed_resize_from_w", -1);
            //jsonObject.put("sampler_name", "string");
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
            //jsonObject.put("eta", 0);
            //jsonObject.put("s_churn", 0);
            //jsonObject.put("s_tmax", 0);
            //jsonObject.put("s_tmin", 0);
            //jsonObject.put("s_noise", 1);
            //jsonObject.put("override_settings", new JSONObject());
            //jsonObject.put("override_settings_restore_afterwards", true);
            //jsonObject.put("script_args", new JSONArray());
            jsonObject.put("steps", 35);
            jsonObject.put("sampler_index", cnMode.equals("depth") ? "DPM++ 2S a Karras": "Euler a");
            //jsonObject.put("include_init_images", false);
            //jsonObject.put("script_name", "string");
            //jsonObject.put("send_images", true);
            jsonObject.put("save_images", false);
            JSONObject alwayson_scripts = new JSONObject();
            JSONObject controlnet = new JSONObject();
            JSONArray args = new JSONArray();
            JSONObject cnArgObject = new JSONObject();
            cnArgObject.put("input_image", Utils.jpg2Base64String(mCurrentSketch.getImgPreview()));
            //cnArgObject.put("mask", "");
            switch (cnMode) {
                case Sketch.CN_MODE_SCRIBBLE:
                    cnArgObject.put("module", "none");
                    cnArgObject.put("model", sharedPreferences.getString("cnScribbleModel","control_sd15_scribble [fef5e48e]"));
                    cnArgObject.put("weight", 0.7);
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

}
