package com.jsoft.diffusionpaint.helper;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.jsoft.diffusionpaint.dto.SdCnParam;
import com.jsoft.diffusionpaint.dto.Sketch;

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
            jsonObject.put("gfpgan_visibility", 0.8);
            jsonObject.put("codeformer_visibility", 0);
            jsonObject.put("codeformer_weight", 0);
            int canvasDim = 2560;
            try {canvasDim = Integer.parseInt(sharedPreferences.getString("canvasDim", "2560")); } catch (Exception e) {}
            jsonObject.put("upscaling_resize", Math.min(4, (double)canvasDim / (double)Math.max(bitmap.getWidth(), bitmap.getHeight())));
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

        Gson gson = new Gson();
        String jsonMode = cnMode.equals(Sketch.CN_MODE_TXT) ? sharedPreferences.getString("modeTxt2img", "{\"type\":\"txt2img\"}") :
                        cnMode.equals(Sketch.CN_MODE_CUSTOM_1) ? sharedPreferences.getString("modeCustom1", "{\"type\":\"txt2img\"}") :
                        cnMode.equals(Sketch.CN_MODE_CUSTOM_2) ? sharedPreferences.getString("modeCustom2", "{\"type\":\"txt2img\"}") :
                        cnMode.equals(Sketch.CN_MODE_CUSTOM_3) ? sharedPreferences.getString("modeCustom3", "{\"type\":\"txt2img\"}") :
                        cnMode.equals(Sketch.CN_MODE_CUSTOM_4) ? sharedPreferences.getString("modeCustom4", "{\"type\":\"txt2img\"}") :
                        cnMode.equals(Sketch.CN_MODE_SCRIBBLE) ? "{\"baseImage\":\"sketch\", \"cnInputImage\":\"sketch\", \"cnModelKey\":\"cnScribbleModel\", \"cnModule\":\"none\", \"cnWeight\":0.7, \"denoise\":0.8, \"type\":\"img2img\"}" :
                        cnMode.equals(Sketch.CN_MODE_DEPTH) ? "{\"baseImage\":\"sketch\", \"cnInputImage\":\"sketch\", \"cnModelKey\":\"cnDepthModel\", \"cnModule\":\"depth_leres\", \"cnWeight\":1.0, \"denoise\":0.8, \"type\":\"img2img\"}" :
                        cnMode.equals(Sketch.CN_MODE_POSE) ? "{\"baseImage\":\"sketch\", \"cnInputImage\":\"sketch\", \"cnModelKey\":\"cnPoseModel\", \"cnModule\":\"openpose_full\", \"cnWeight\":1.0, \"denoise\":0.8, \"type\":\"img2img\"}" :
                        cnMode.equals(Sketch.CN_MODE_TXT_CANNY) ? "{\"cnInputImage\":\"sketch\", \"cnModelKey\":\"cnCannyModel\", \"cnModule\":\"canny\", \"cnWeight\":1.0, \"type\":\"txt2img\"}" :
                        cnMode.equals(Sketch.CN_MODE_TXT_SCRIBBLE) ? "{\"cnInputImage\":\"sketch\", \"cnModelKey\":\"cnScribbleModel\", \"cnModule\":\"scribble_hed\", \"cnWeight\":0.7, \"type\":\"txt2img\"}" :
                        cnMode.equals(Sketch.CN_MODE_TXT_DEPTH) ? "{\"cnInputImage\":\"sketch\", \"cnModelKey\":\"cnDepthModel\", \"cnModule\":\"depth_leres\", \"cnWeight\":1.0, \"type\":\"txt2img\"}" :
                        cnMode.equals(Sketch.CN_MODE_INPAINT) ? "{\"baseImage\":\"background\", \"denoise\":1.0, \"inpaintFill\":2, \"type\":\"inpaint\"}" :
                        cnMode.equals(Sketch.CN_MODE_INPAINT_COLOR) ? "{\"baseImage\":\"sketch\", \"denoise\":0.8, \"inpaintFill\":1, \"type\":\"inpaint\"}" :
                        "{\"baseImage\":\"sketch\", \"cnInputImage\":\"background\", \"cnModelKey\":\"cnDepthModel\", \"cnModule\":\"depth_leres\", \"cnWeight\":1.0, \"denoise\":0.8, \"inpaintFill\":1, \"type\":\"inpaint\"}";
                        //Sketch.CN_MODE_INPAINT_DEPTH

        SdCnParam param = gson.fromJson(jsonMode, SdCnParam.class);
        if (param.sdSize == 0) { param.sdSize = sharedPreferences.getInt("sdImageSize", 512); }
        if (param.cfgScale == 0d) { param.cfgScale = 7.0; }
        if (param.steps == 0) { param.steps = 40; }
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
                jsonObject.put("width", param.sdSize * 3 / 4);
            } else {
                jsonObject.put("width", param.sdSize);
            }
            if (aspectRatio.equals(Sketch.ASPECT_RATIO_LANDSCAPE)) {
                jsonObject.put("height", param.sdSize * 3 / 4);
            } else {
                jsonObject.put("height", param.sdSize);
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
                        param.cnInputImage.equals(SdCnParam.SD_INPUT_IMAGE_SKETCH)?mCurrentSketch.getImgPreview():
                        param.cnInputImage.equals(SdCnParam.SD_INPUT_IMAGE_REF)?mCurrentSketch.getResizedImgReference():
                        mCurrentSketch.getResizedImgBackground()));
                //cnArgObject.put("mask", "");
                cnArgObject.put("module", param.cnModule);
                if (!"None".equals(sharedPreferences.getString(param.cnModelKey, "None"))) {
                    cnArgObject.put("model", sharedPreferences.getString(param.cnModelKey, "None"));
                }
                cnArgObject.put("weight", param.cnWeight);

                cnArgObject.put("resize_mode", "Inner Fit (Scale to Fit)");
                cnArgObject.put("lowvram", false);
                cnArgObject.put("processor_res", param.sdSize);
                cnArgObject.put("threshold_a", 64);
                cnArgObject.put("threshold_b", 64);
                cnArgObject.put("guidance", 1);
                cnArgObject.put("guidance_start", 0);
                cnArgObject.put("guidance_end", 1);
                cnArgObject.put("control_mode", param.cnControlMode == 0 ? SdCnParam.CN_MODE_BALANCED :
                        param.cnControlMode == 1 ? SdCnParam.CN_MODE_PROMPT : SdCnParam.CN_MODE_CONTROLNET);
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

            Bitmap baseImage;
            if (isInpaint && (param.inpaintPartial == SdCnParam.INPAINT_PARTIAL)) {
                if (mCurrentSketch.getRectInpaint() == null) {
                    mCurrentSketch.setRectInpaint(mCurrentSketch.getInpaintRect(param.sdSize));
                }
                Bitmap bg = mCurrentSketch.getImgBackground();
                if (param.baseImage.equals(SdCnParam.SD_INPUT_IMAGE_SKETCH)) {
                    Bitmap bmEdit = Bitmap.createBitmap(mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvasEdit = new Canvas(bmEdit);
                    canvasEdit.drawBitmap(mCurrentSketch.getImgBackground(), null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
                    canvasEdit.drawBitmap(mCurrentSketch.getImgPaint(), null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
                    bg = bmEdit;
                } else if (param.baseImage.equals(SdCnParam.SD_INPUT_IMAGE_BG_REF)) {
                    bg = mCurrentSketch.getImgBgRef();
                }
                baseImage = Utils.extractBitmap(bg, mCurrentSketch.getRectInpaint());
            } else {
                baseImage = param.baseImage.equals(SdCnParam.SD_INPUT_IMAGE_SKETCH) ? mCurrentSketch.getImgPreview() :
                        param.baseImage.equals(SdCnParam.SD_INPUT_IMAGE_BG_REF) ? mCurrentSketch.getResizedImgBgRef() : mCurrentSketch.getResizedImgBackground();
            }
            /*Log.e("diffusionpaint", "ImgBackground=" + mCurrentSketch.getImgBackground().getWidth() + "X" + mCurrentSketch.getImgBackground().getHeight());
            Log.e("diffusionpaint", "baseImage=" + baseImage.getWidth() + "X" + baseImage.getHeight());
            Log.e("diffusionpaint", "Rect=(" + mCurrentSketch.getRectInpaint().left + "," + mCurrentSketch.getRectInpaint().top + ") -> ("
                            + mCurrentSketch.getRectInpaint().right + "," + mCurrentSketch.getRectInpaint().bottom + ")");*/
            init_images.put(Utils.jpg2Base64String(baseImage));
            jsonObject.put("init_images", init_images);
            jsonObject.put("resize_mode", 1);
            jsonObject.put("denoising_strength", param.denoise);
            jsonObject.put("image_cfg_scale", param.cfgScale);
            if (isInpaint) {
                if (mCurrentSketch.getImgInpaintMask() == null) {
                    mCurrentSketch.setImgInpaintMask(Sketch.getInpaintMaskFromPaint(mCurrentSketch));
                }
                Bitmap imgInpaintMask = mCurrentSketch.getImgInpaintMask();
                if (param.inpaintPartial == SdCnParam.INPAINT_PARTIAL) {
                    Bitmap resizedBm = Bitmap.createScaledBitmap(mCurrentSketch.getImgInpaintMask(), mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight(), false);
                    imgInpaintMask = Utils.extractBitmap(resizedBm, mCurrentSketch.getRectInpaint());
                }
                jsonObject.put("mask", Utils.png2Base64String(imgInpaintMask));
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
                jsonObject.put("width", param.sdSize * 3 / 4);
            } else {
                jsonObject.put("width", param.sdSize);
            }
            if (aspectRatio.equals(Sketch.ASPECT_RATIO_LANDSCAPE)) {
                jsonObject.put("height", param.sdSize * 3 / 4);
            } else {
                jsonObject.put("height", param.sdSize);
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

                Bitmap cnImage = null;
                if (param.cnInputImage.equals(SdCnParam.SD_INPUT_IMAGE_REF)) {
                    cnImage = mCurrentSketch.getResizedImgReference();
                } else if (isInpaint && (param.inpaintPartial == SdCnParam.INPAINT_PARTIAL)) {
                    Bitmap bg = mCurrentSketch.getImgBackground();
                    if (param.cnInputImage.equals(SdCnParam.SD_INPUT_IMAGE_SKETCH)) {
                        Bitmap bmEdit = Bitmap.createBitmap(mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvasEdit = new Canvas(bmEdit);
                        canvasEdit.drawBitmap(mCurrentSketch.getImgBackground(), null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
                        canvasEdit.drawBitmap(mCurrentSketch.getImgPaint(), null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
                        bg = bmEdit;
                    }
                    cnImage = Utils.extractBitmap(bg, mCurrentSketch.getRectInpaint());
                } else {
                    cnImage = param.cnInputImage.equals(SdCnParam.SD_INPUT_IMAGE_SKETCH)?mCurrentSketch.getImgPreview():mCurrentSketch.getResizedImgBackground();
                }

                cnArgObject.put("input_image", Utils.jpg2Base64String(cnImage));
                //cnArgObject.put("mask", "");
                cnArgObject.put("module", param.cnModule);
                if (!"None".equals(sharedPreferences.getString(param.cnModelKey, "None"))) {
                    cnArgObject.put("model", sharedPreferences.getString(param.cnModelKey, "None"));
                }
                cnArgObject.put("weight", param.cnWeight);
                cnArgObject.put("resize_mode", "Inner Fit (Scale to Fit)");
                cnArgObject.put("lowvram", false);
                cnArgObject.put("processor_res", param.sdSize);
                cnArgObject.put("threshold_a", 64);
                cnArgObject.put("threshold_b", 64);
                cnArgObject.put("guidance", 1);
                cnArgObject.put("guidance_start", 0);
                cnArgObject.put("guidance_end", 1);
                cnArgObject.put("control_mode", param.cnControlMode == 0 ? SdCnParam.CN_MODE_BALANCED :
                        param.cnControlMode == 1 ? SdCnParam.CN_MODE_PROMPT : SdCnParam.CN_MODE_CONTROLNET);
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
