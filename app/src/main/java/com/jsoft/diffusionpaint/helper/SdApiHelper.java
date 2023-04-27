package com.jsoft.diffusionpaint.helper;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

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
            jsonObject.put("upscaling_resize", 2);
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

    public SdCnParam getSdCnParm(String cnMode) {
        if (cnMode.equals(Sketch.CN_MODE_TXT) || cnMode.startsWith("custom") ) {
            Gson gson = new Gson();
            String jsonMode = cnMode.equals(Sketch.CN_MODE_TXT) ? sharedPreferences.getString("modeTxt2img", "{\"type\":\"txt2img\",\"steps\":40,\"cfgScale\":7.0}") :
                            cnMode.equals(Sketch.CN_MODE_CUSTOM_1) ? sharedPreferences.getString("modeCustom1", "{\"type\":\"txt2img\",\"steps\":40,\"cfgScale\":7.0}") :
                            cnMode.equals(Sketch.CN_MODE_CUSTOM_2) ? sharedPreferences.getString("modeCustom2", "{\"type\":\"txt2img\",\"steps\":40,\"cfgScale\":7.0}") :
                            cnMode.equals(Sketch.CN_MODE_CUSTOM_3) ? sharedPreferences.getString("modeCustom3", "{\"type\":\"txt2img\",\"steps\":40,\"cfgScale\":7.0}") :
                            sharedPreferences.getString("modeCustom4", "{\"type\":\"txt2img\",\"steps\":40,\"cfgScale\":7.0}");
            SdCnParam param = gson.fromJson(jsonMode, SdCnParam.class);
            return param;
        }
        SdCnParam param = new SdCnParam();
        if (cnMode.startsWith("txt")) {
            param.type = SdCnParam.SD_MODE_TYPE_TXT2IMG;
        } else if (cnMode.startsWith("inpaint")) {
            param.type = SdCnParam.SD_MODE_TYPE_INPAINT;
        } else {
            param.type = SdCnParam.SD_MODE_TYPE_IMG2IMG;
        }

        param.cfgScale = 7;
        param.steps = 40;

        if (param.type.equals(SdCnParam.SD_MODE_TYPE_INPAINT)) {
            param.inpaintFill = cnMode.equals(Sketch.CN_MODE_INPAINT)? SdCnParam.SD_INPAINT_FILL_NOISE : SdCnParam.SD_INPAINT_FILL_ORIGINAL;
        }
        if (!param.type.equals(SdCnParam.SD_MODE_TYPE_TXT2IMG)) {
            param.baseImage = cnMode.equals(Sketch.CN_MODE_INPAINT)? SdCnParam.SD_INPUT_IMAGE_BACKGROUND : SdCnParam.SD_INPUT_IMAGE_SKETCH;
            param.denoise = cnMode.equals(Sketch.CN_MODE_INPAINT)?1:0.8;

            if (!param.type.equals(SdCnParam.SD_MODE_TYPE_INPAINT) || cnMode.equals(Sketch.CN_MODE_INPAINT_DEPTH)) {
                param.cnInputImage = cnMode.equals(Sketch.CN_MODE_INPAINT_DEPTH) ? SdCnParam.SD_INPUT_IMAGE_BACKGROUND : SdCnParam.SD_INPUT_IMAGE_SKETCH;
                switch (cnMode) {
                    case Sketch.CN_MODE_SCRIBBLE:
                        param.cnModule = "none";
                        param.cnModelKey = "cnScribbleModel";
                        param.cnWeight = 0.7;
                        break;
                    case Sketch.CN_MODE_DEPTH:
                    case Sketch.CN_MODE_INPAINT_DEPTH:
                        param.cnModule = "depth_leres";
                        param.cnModelKey = "cnDepthModel";
                        param.cnWeight = 1;
                        break;
                    case Sketch.CN_MODE_POSE:
                        param.cnModule = "openpose_full";
                        param.cnModelKey = "cnPoseModel";
                        param.cnWeight = 1;
                        break;
                }
            }
        } else {
            if (!cnMode.equals(Sketch.CN_MODE_TXT)) {
                param.cnInputImage = SdCnParam.SD_INPUT_IMAGE_SKETCH;
                switch (cnMode) {
                    case Sketch.CN_MODE_TXT_CANNY:
                        param.cnModule = "canny";
                        param.cnModelKey = "cnCannyModel";
                        param.cnWeight = 1;
                        break;
                    case Sketch.CN_MODE_TXT_SCRIBBLE:
                        param.cnModule = "scribble_hed";
                        param.cnModelKey = "cnScribbleModel";
                        param.cnWeight = 0.7;
                        break;
                    case Sketch.CN_MODE_TXT_DEPTH:
                        param.cnModule = "depth_leres";
                        param.cnModelKey = "cnDepthModel";
                        param.cnWeight = 1;
                        break;
                }
            }
        }


        return param;
    }

    public JSONObject getControlnetTxt2imgJSON(String prompt, SdCnParam param, Sketch mCurrentSketch, String aspectRatio) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("prompt", sharedPreferences.getString("promptPrefix", "") + " " + prompt + ", " + sharedPreferences.getString("promptPostfix", ""));
            jsonObject.put("seed", -1);
            jsonObject.put("batch_size", 1);
            jsonObject.put("n_iter", 1);
            jsonObject.put("steps", param.steps);
            jsonObject.put("cfg_scale", param.cfgScale);
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

            if (param.cnInputImage != null) {
                // ControlNet Args
                JSONObject controlnet = new JSONObject();
                JSONArray args = new JSONArray();
                JSONObject cnArgObject = new JSONObject();
                cnArgObject.put("input_image", Utils.jpg2Base64String(
                        param.cnInputImage.equals(SdCnParam.SD_INPUT_IMAGE_BACKGROUND)?mCurrentSketch.getImgBackground():mCurrentSketch.getImgPreview()));
                //cnArgObject.put("mask", "");
                cnArgObject.put("module", param.cnModule);
                cnArgObject.put("model", sharedPreferences.getString(param.cnModelKey, ""));
                cnArgObject.put("weight", param.cnWeight);

                cnArgObject.put("resize_mode", "Inner Fit (Scale to Fit)");
                cnArgObject.put("lowvram", false);
                cnArgObject.put("processor_res", sharedPreferences.getInt("sdImageSize", 512));
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

    public JSONObject getControlnetImg2imgJSON(String prompt, SdCnParam param, Sketch mCurrentSketch, String aspectRatio) {
        JSONObject jsonObject = new JSONObject();
        boolean isInpaint = param.type.equals(SdCnParam.SD_MODE_TYPE_INPAINT);
        try {
            JSONArray init_images = new JSONArray();
            init_images.put(Utils.jpg2Base64String(param.baseImage.equals(SdCnParam.SD_INPUT_IMAGE_BACKGROUND)?mCurrentSketch.getImgBackground():mCurrentSketch.getImgPreview()));
            jsonObject.put("init_images", init_images);
            jsonObject.put("resize_mode", 1);
            jsonObject.put("denoising_strength", param.denoise);
            jsonObject.put("image_cfg_scale", param.cfgScale);
            if (isInpaint) {
                if (mCurrentSketch.getImgInpaint() == null) {
                    mCurrentSketch.setImgInpaint(Sketch.getInpaintMaskFromPaint(mCurrentSketch));
                }
                jsonObject.put("mask", Utils.png2Base64String(mCurrentSketch.getImgInpaint()));
                jsonObject.put("mask_blur", 10);
                jsonObject.put("inpainting_fill", param.inpaintFill);
                jsonObject.put("inpaint_full_res", false);
                jsonObject.put("inpaint_full_res_padding", 32);
                //jsonObject.put("inpainting_mask_invert", 0);
                jsonObject.put("initial_noise_multiplier", 1);
            }
            jsonObject.put("prompt", sharedPreferences.getString("promptPrefix", "") + " " + prompt + ", " + sharedPreferences.getString("promptPostfix", ""));
            jsonObject.put("seed", -1);
            jsonObject.put("batch_size", 1);
            jsonObject.put("n_iter", 1);
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
            jsonObject.put("steps", param.steps);
            jsonObject.put("sampler_index", sharedPreferences.getString("sdSampler", "Euler a"));
            jsonObject.put("save_images", false);
            JSONObject alwayson_scripts = new JSONObject();

            // ControlNet Args
            if (param.cnInputImage != null) {
                JSONObject controlnet = new JSONObject();
                JSONArray args = new JSONArray();
                JSONObject cnArgObject = new JSONObject();
                cnArgObject.put("input_image", Utils.jpg2Base64String(
                        param.cnInputImage.equals(SdCnParam.SD_INPUT_IMAGE_BACKGROUND)?mCurrentSketch.getImgBackground():mCurrentSketch.getImgPreview()));
                //cnArgObject.put("mask", "");
                cnArgObject.put("module", param.cnModule);
                cnArgObject.put("model", sharedPreferences.getString(param.cnModelKey, ""));
                cnArgObject.put("weight", param.cnWeight);
                cnArgObject.put("resize_mode", "Inner Fit (Scale to Fit)");
                cnArgObject.put("lowvram", false);
                cnArgObject.put("processor_res", sharedPreferences.getInt("sdImageSize", 512));
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
