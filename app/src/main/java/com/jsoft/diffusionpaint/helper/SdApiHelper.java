package com.jsoft.diffusionpaint.helper;

import static com.jsoft.diffusionpaint.ViewSdImageActivity.sdModelList;

import static java.lang.Math.*;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jsoft.diffusionpaint.dto.CnParam;
import com.jsoft.diffusionpaint.dto.SdParam;
import com.jsoft.diffusionpaint.dto.SdStyle;
import com.jsoft.diffusionpaint.dto.Sketch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SdApiHelper {
    private SharedPreferences sharedPreferences = null;
    private Activity activity;
    private SdApiResponseListener listener;
    private OkHttpClient client;

    public SdApiHelper(Activity activity, SdApiResponseListener listener) {
        this.activity  = activity;
        this.listener = listener;
        sharedPreferences = activity.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        client = getClient(10, 120);
    }

    public void setActivity(Activity activity) { this.activity  = activity;}
    public void setListener(SdApiResponseListener listener) { this.listener  = listener;}

    public boolean isValid() {
        String sdAddress = sharedPreferences.getString("dflApiAddress", "");
        return Utils.isValidServerURL(sdAddress);
    }

    public void sendGetRequest(String requestType, String url) {
        sendRequest(requestType, sharedPreferences.getString("sdServerAddress", ""), url, null, "GET");
    }

    public void sendPostRequest(String requestType, String url, JSONObject jsonObject) {
        sendRequest(requestType, sharedPreferences.getString("sdServerAddress", ""), url, jsonObject, "POST");
    }

    public void sendRequest(String requestType, String baseUrl, String url, JSONObject jsonObject, String httpMethod) {
        sendRequest(requestType, baseUrl, url, jsonObject, httpMethod, this.client);
    }

    public static OkHttpClient getClient(long connectTimeout, long readTimeout) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .build();
        return client;
    }

    public void sendRequest(String requestType, String baseUrl, String url, JSONObject jsonObject, String httpMethod, OkHttpClient client) {
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
                activity.runOnUiThread(() -> listener.onSdApiFailure(requestType, e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        activity.runOnUiThread(() -> listener.onSdApiFailure(requestType, "Response Code: " + response.code()));
                        return;
                    }

                    /*Headers responseHeaders = response.headers();
                    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                        Log.d("diffusionPaint", responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }*/

                    assert responseBody != null;
                    String responseString = responseBody.string();
                    activity.runOnUiThread(() -> listener.onSdApiResponse(requestType, responseString));

                } catch (IOException e) {
                    e.printStackTrace();
                    activity.runOnUiThread(() -> listener.onSdApiFailure(requestType, "IOException: " + e.getMessage()));
                }
            }
        });
    }

    public JSONObject getDflJSON(Bitmap bitmap) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("model", sharedPreferences.getString("dflModel", ""));
            jsonObject.put("image", Utils.jpg2Base64String(bitmap));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private String getCleanString(String input) {
        Pattern pattern = Pattern.compile("<\\s*(\\w+)\\s*:\\s*(.*?)\\s*>");
        Matcher matcher = pattern.matcher(input);

        StringBuilder cleanString = new StringBuilder(input);

        while (matcher.find()) {
            cleanString.replace(matcher.start(), matcher.end(), "");
            matcher.reset(cleanString);
        }

        return cleanString.toString().replaceAll("\\s+", " ").trim();
    }

    private JSONObject getEmbeddedJSONObject(String input) {
        Pattern pattern = Pattern.compile("<\\s*(\\w+)\\s*:\\s*(.*?)\\s*>");
        Matcher matcher = pattern.matcher(input);

        JSONObject jsonObject = new JSONObject();

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2); // Capture the entire value, including spaces and colons

            try {
                jsonObject.put(key, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return jsonObject;
    }

    public JSONObject getComfyuiJSON(Sketch mCurrentSketch, int batchSize){
        JSONObject jsonObject = new JSONObject();
        SdParam sdParam = getSdCnParm(mCurrentSketch.getCnMode());
        JSONObject fields = getComfyuiFields(mCurrentSketch.getCnMode());

        Bitmap backgroundImage = mCurrentSketch.getImgBackground();
        if (sdParam.baseImage != null && sdParam.baseImage.equals(SdParam.SD_INPUT_IMAGE_SKETCH)) {
            backgroundImage = Bitmap.createScaledBitmap(mCurrentSketch.getImgPreview(), mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight(), true);
        }

        Bitmap maskImage = null;
        int size = sdParam.sdSize;
        if (sdParam.type.equals(SdParam.SD_MODE_TYPE_INPAINT)) {
            if (sdParam.inpaintPartial == 1) {
                RectF inpaintArea = mCurrentSketch.getRectInpaint(sdParam.sdSize);
                Bitmap baseImage = Utils.extractBitmap(backgroundImage, inpaintArea);
                //mCurrentSketch.setImgInpaintMask(Sketch.getInpaintMaskFromPaint(mCurrentSketch, 0, false));
                backgroundImage = baseImage;
                double ratio = max(inpaintArea.width(), inpaintArea.height()) / sdParam.sdSize;
                int boundary = (int) round(sdParam.maskBlur * (sdParam.baseImage.equals(SdParam.SD_INPUT_IMAGE_SKETCH)?1:0) * ratio);
                mCurrentSketch.setImgInpaintMask(Sketch.getInpaintMaskFromPaint(mCurrentSketch, boundary, false));
                maskImage = Utils.extractBitmap(mCurrentSketch.getImgInpaintMask(), inpaintArea);
                size = min(sdParam.sdSize, (int)max(inpaintArea.width(), inpaintArea.height()));
            } else {
                double ratio = (double)max(backgroundImage.getWidth(), backgroundImage.getHeight()) / sdParam.sdSize;
                int boundary = (int) round(sdParam.maskBlur * (sdParam.baseImage.equals(SdParam.SD_INPUT_IMAGE_SKETCH)?1:0) * ratio);
                maskImage = Sketch.getInpaintMaskFromPaint(mCurrentSketch, boundary, false);
                mCurrentSketch.setImgInpaintMask(maskImage);
            }
        }

        long width = 1024;
        long height = 1024;
        if (backgroundImage != null) {
            if (backgroundImage.getHeight() > backgroundImage.getWidth()) {
                width = Utils.getShortSize(backgroundImage, sdParam.sdSize);
            } else {
                width = sdParam.sdSize;
            }
            if (backgroundImage.getHeight() < backgroundImage.getWidth()) {
                height = Utils.getShortSize(backgroundImage, sdParam.sdSize);
            } else {
                height = sdParam.sdSize;
            }
        }

        String prompt = getPrompt(sdParam, getCleanString(mCurrentSketch.getPrompt()));
        String negPrompt = getNegPrompt(sdParam, getCleanString(mCurrentSketch.getNegPrompt()));
        JSONObject overrideParam = getEmbeddedJSONObject(mCurrentSketch.getPrompt());

        JSONObject modeJSON;
        try {
            modeJSON = new JSONObject(getSdParmJSON(mCurrentSketch.getCnMode()));
        } catch (JSONException e) {
            modeJSON = new JSONObject();
        }
        for (Iterator<String> it = fields.keys(); it.hasNext(); ) {
            String key = it.next();
            try {
                String value = fields.getString(key);
                if (overrideParam.has(key)) {
                    jsonObject.put(key, overrideParam.get(key));
                } else if ("$positive".equals(value)) {
                    jsonObject.put(key, prompt);
                } else if ("$negative".equals(value)) {
                    jsonObject.put(key, negPrompt);
                } else if ("$size".equals(value)) {
                    jsonObject.put(key, size);
                } else if ("$steps".equals(value)) {
                    jsonObject.put(key, sdParam.steps);
                } else if ("$denoise".equals(value)) {
                    jsonObject.put(key, sdParam.denoise);
                } else if ("$cfg".equals(value)) {
                    jsonObject.put(key, sdParam.cfgScale);
                } else if ("$batchSize".equals(value)) {
                    jsonObject.put(key, batchSize);
                } else if ("$width".equals(value)) {
                    jsonObject.put(key, width);
                } else if ("$height".equals(value)) {
                    jsonObject.put(key, height);
                } else if ("$maskBlur".equals(value)) {
                    jsonObject.put(key, sdParam.maskBlur);
                } else if ("$background".equals(value)) {
                    jsonObject.put(key, Utils.jpg2Base64String(backgroundImage));
                } else if ("$mask".equals(value)) {
                    jsonObject.put(key, Utils.jpg2Base64String(maskImage));
                } else if ("$reference".equals(value)) {
                    jsonObject.put(key, Utils.jpg2Base64String(mCurrentSketch.getImgReference()));
                } else if ("$paint".equals(value)) {
                    jsonObject.put(key, Utils.jpg2Base64String(mCurrentSketch.getImgPaint()));
                } else if (modeJSON.has(key)) {
                    jsonObject.put(key, modeJSON.get(key));
                } else {
                    jsonObject.put(key, value);
                }
            } catch (JSONException ignored) {}
        }

        return jsonObject;
    }

    private String getComfyuiModeName(String cnMode) {
        return cnMode.substring(Sketch.CN_MODE_COMFYUI.length());
    }

    private String getComfyuiDefault(String cnMode) {
        for (int i = 0; i < Sketch.comfyuiModes.length(); i ++) {
            try {
                JSONObject cfMode = Sketch.comfyuiModes.getJSONObject(i);
                if (cfMode.get("name").equals(getComfyuiModeName(cnMode))) {
                    return cfMode.getJSONObject("default").toString();
                }
            } catch (JSONException ignored) {}
        }
        return Sketch.defaultJSON.get(Sketch.CN_MODE_TXT);
    }

    private JSONObject getComfyuiFields(String cnMode) {
        for (int i = 0; i < Sketch.comfyuiModes.length(); i ++) {
            try {
                JSONObject cfMode = Sketch.comfyuiModes.getJSONObject(i);
                if (cfMode.get("name").equals(getComfyuiModeName(cnMode))) {
                    return cfMode.getJSONObject("fields");
                }
            } catch (JSONException ignored) {}
        }
        return new JSONObject();
    }

    public JSONObject getComfyuiCaptionJSON(Bitmap bitmap, String mode) {
        JSONObject jsonObject = new JSONObject();
        try {
            if ("tag".equals(mode)) {
                jsonObject.put("workflow", "tag");
            } else {
                jsonObject.put("workflow", "caption");
            }
            double scale = (double) max(bitmap.getHeight(), bitmap.getWidth()) / 1280;
            Bitmap resultBm = bitmap;
            if (scale > 1) {
                resultBm = Bitmap.createScaledBitmap(bitmap, (int) round(bitmap.getWidth() / scale), (int) round(bitmap.getHeight() / scale), true);
            }
            jsonObject.put("background", Utils.jpg2Base64String(resultBm));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject getUpscaleImageJSON(Bitmap bitmap) {
        int canvasDim = 3840;
        try {canvasDim = Integer.parseInt(sharedPreferences.getString("canvasDim", "3840")); } catch (Exception ignored) {}
        return getUpscaleImageJSON(bitmap, canvasDim);
    }

    public JSONObject getUpscaleImageJSON(Bitmap bitmap, int size) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("workflow", "upscale");
            jsonObject.put("size", size);
            jsonObject.put("background", Utils.jpg2Base64String(bitmap));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public String getSdParmJSON(String cnMode) {
        return cnMode.equals(Sketch.CN_MODE_TXT) ? sharedPreferences.getString("modeTxt2img", Sketch.defaultJSON.get(cnMode)) :
                cnMode.equals(Sketch.CN_MODE_TXT_SDXL) ? sharedPreferences.getString("modeSDXL", Sketch.defaultJSON.get(cnMode)) :
                cnMode.equals(Sketch.CN_MODE_TXT_SDXL_TURBO) ? sharedPreferences.getString("modeSDXLTurbo", Sketch.defaultJSON.get(cnMode)) :
                cnMode.equals(Sketch.CN_MODE_INPAINT) ? sharedPreferences.getString("modeInpaint", Sketch.defaultJSON.get(cnMode)) :
                cnMode.equals(Sketch.CN_MODE_INPAINT_SKETCH) ? sharedPreferences.getString("modeInpaintS", Sketch.defaultJSON.get(cnMode)) :
                cnMode.equals(Sketch.CN_MODE_REFINER) ? sharedPreferences.getString("modeRefiner", Sketch.defaultJSON.get(cnMode)) :
                cnMode.equals(Sketch.CN_MODE_PARTIAL_INPAINT) ? sharedPreferences.getString("modePInpaint", Sketch.defaultJSON.get(cnMode)) :
                cnMode.equals(Sketch.CN_MODE_PARTIAL_INPAINT_SKETCH) ? sharedPreferences.getString("modePInpaintS", Sketch.defaultJSON.get(cnMode)) :
                cnMode.equals(Sketch.CN_MODE_PARTIAL_REFINER) ? sharedPreferences.getString("modePRefiner", Sketch.defaultJSON.get(cnMode)) :
                cnMode.startsWith(Sketch.CN_MODE_CUSTOM) ? sharedPreferences.getString("modeCustom" + cnMode.substring(Sketch.CN_MODE_CUSTOM.length()), Sketch.defaultJSON.get(Sketch.CN_MODE_CUSTOM)) :
                cnMode.startsWith(Sketch.CN_MODE_OUTPAINT) ? sharedPreferences.getString("modeOutpaint", Sketch.defaultJSON.get(Sketch.CN_MODE_OUTPAINT)) :
                cnMode.equals(Sketch.CN_MODE_INPAINT_MERGE) ? sharedPreferences.getString("modeMerge", Sketch.defaultJSON.get(cnMode)) :
                cnMode.startsWith(Sketch.CN_MODE_COMFYUI) ? sharedPreferences.getString("modeComyui" + getComfyuiModeName(cnMode), getComfyuiDefault(cnMode)) :
                Sketch.defaultJSON.get(cnMode) != null ? Sketch.defaultJSON.get(cnMode) : Sketch.defaultJSON.get(Sketch.CN_MODE_TXT);
    }

    public SdParam getSdCnParm(String cnMode) {
        Gson gson = new Gson();
        String jsonMode = getSdParmJSON(cnMode);
        JsonObject rootObj = gson.fromJson(jsonMode, JsonObject.class);
        if (rootObj.get("cnInputImage") != null && rootObj.get("cn") == null) {
            JsonObject cnObj = new JsonObject();
            String[] cnProperties = {"cnInputImage", "cnModelKey", "cnModel", "cnModule", "cnControlMode", "cnWeight", "cnModuleParamA", "cnModuleParamB", "cnResizeMode", "cnStart", "cnEnd"};
            for (String cnProp : cnProperties) {
                if (rootObj.get(cnProp) != null) {
                    cnObj.add(cnProp, rootObj.get(cnProp));
                    rootObj.remove(cnProp);
                }
            }
            JsonArray cnArray = new JsonArray();
            cnArray.add(cnObj);
            rootObj.add("cn", cnArray);
            jsonMode = gson.toJson(rootObj);
        }

        SdParam param = gson.fromJson(jsonMode, SdParam.class);
        if (param.model == null) {
            param.model = param.type.equals(SdParam.SD_MODE_TYPE_INPAINT) ? SdParam.SD_MODEL_INPAINT:
                    Sketch.CN_MODE_TXT_SDXL.equals(cnMode) ? SdParam.SD_MODEL_SDXL_BASE :
                    Sketch.CN_MODE_TXT_SD3.equals(cnMode) ? SdParam.SD_MODEL_SD3 :
                    Sketch.CN_MODE_TXT_SDXL_TURBO.equals(cnMode) ? SdParam.SD_MODEL_SDXL_TURBO : SdParam.SD_MODEL_V1;
        }
        if (!param.type.equals(SdParam.SD_MODE_TYPE_INPAINT)) { param.inpaintPartial = 0; }
        if (param.sdSize == 0) { param.sdSize = sharedPreferences.getInt("sdImageSize", 768); }
        if (param.cfgScale == 0d) {
            try {
                param.cfgScale = Double.parseDouble(sharedPreferences.getString("defaultCfgScale", "7.0"));
            } catch (Exception e) { param.cfgScale = 7.0; }
        }
        if (param.steps == 0) {
            try {
                param.steps = Integer.parseInt(sharedPreferences.getString("defaultSteps", "30"));
            } catch (Exception e) { param.steps = 30; }
        }
        if (param.sampler == null) {
            param.sampler = sharedPreferences.getString("sdSampler", "Euler a");
        }
        if (param.scheduler == null) {
            param.scheduler = "Automatic";
        }
        if (param.maskBlur == -1) {
            param.maskBlur = Integer.parseInt(sharedPreferences.getString("inpaintMaskBlur", "10"));
        }
        if (param.clipSkip < 1 || param.clipSkip > 12) {
            try {
                param.clipSkip = Integer.parseInt(sharedPreferences.getString("defaultClipSkip", "1"));
            } catch (Exception e) { param.clipSkip = 1; }
        }
        if (param.cn != null) {
            for (CnParam cnParam : param.cn) {
                if (SdParam.cnModulesResponse != null && cnParam.cnModule != null) {
                    try {
                        JSONObject responseObject = new JSONObject(SdParam.cnModulesResponse);
                        JSONObject moduleDetails = responseObject.getJSONObject("module_detail");
                        if (moduleDetails.has(cnParam.cnModule)) {
                            JSONObject module = moduleDetails.getJSONObject(cnParam.cnModule);
                            if (module.getBoolean("model_free")) {
                                cnParam.cnModel = "None";
                                cnParam.cnModelKey = null;
                            }
                            JSONArray sliders = module.getJSONArray("sliders");
                            if (sliders.length() >= 2) {
                                cnParam.cnModuleParamA = Double.isNaN(cnParam.cnModuleParamA) ? sliders.getJSONObject(1).getDouble("value") : cnParam.cnModuleParamA;
                                if (sliders.length() >= 3) {
                                    cnParam.cnModuleParamB = Double.isNaN(cnParam.cnModuleParamB) ? sliders.getJSONObject(2).getDouble("value") : cnParam.cnModuleParamB;
                                } else {
                                    cnParam.cnModuleParamB = Double.NaN;
                                }
                            } else {
                                cnParam.cnModuleParamA = Double.NaN;
                            }
                        }
                    } catch (JSONException ignored) {}
                }
                if (cnParam.cnResizeMode == -1) {
                    cnParam.cnResizeMode = 2;
                }
                if (cnParam.cnModel == null) {
                    if (cnParam.cnModelKey != null) {
                        cnParam.cnModel = cnParam.cnModelKey = sharedPreferences.getString(cnParam.cnModelKey, cnParam.cnModelKey);
                    } else {
                        cnParam.cnModel = "None";
                    }
                }
                if (cnParam.cnModule == null) {
                    cnParam.cnModule = "none";
                }
            }
        }

        return param;
    }

    public JSONObject getConfig(SdParam param) {
        String preferredModel = param.model.equals(SdParam.SD_MODEL_INPAINT) ? sharedPreferences.getString("sdInpaintModel", ""):
                param.model.equals(SdParam.SD_MODEL_SDXL_BASE) ? sharedPreferences.getString("sdxlBaseModel", ""):
                param.model.equals(SdParam.SD_MODEL_SD3) ? sharedPreferences.getString("sd3Model", ""):
                param.model.equals(SdParam.SD_MODEL_SDXL_TURBO) ? sharedPreferences.getString("sdxlTurboModel", ""):
                param.model.equals(SdParam.SD_MODEL_SDXL_INPAINT) ? sharedPreferences.getString("sdxlInpaintModel", ""):
                sharedPreferences.getString("sdModelCheckpoint", "");
        JSONObject setConfigRequest = new JSONObject();
        if (sdModelList !=null && sdModelList.get(preferredModel) != null) {
            try {
                setConfigRequest.put("CLIP_stop_at_last_layers", param.clipSkip);
                setConfigRequest.put("sd_model_checkpoint", preferredModel);
                setConfigRequest.put("sd_checkpoint_hash", sdModelList.get(preferredModel));
            } catch (JSONException ignored) {
            }
        }
        return setConfigRequest;
    }

    private String getPrompt(SdParam param, String prompt) {
        String promptPrefix = sharedPreferences.getString("promptPrefix", "");
        String promptPostfix = sharedPreferences.getString("promptPostfix", "");
        return (promptPrefix.length() > 0 ? promptPrefix + ", " : "") +
                (prompt.length() > 0 ? prompt + ", " : "") +
                (param.prompt.length() > 0 ? param.prompt + ", " : "") +
                (promptPostfix.length() > 0 ? promptPostfix : "");
    }

    private String getNegPrompt(SdParam param, String negPrompt) {
        String promptNeg = sharedPreferences.getString("negativePrompt", "");
        return (negPrompt.length() > 0 ? negPrompt + ", " : "") +
                (param.negPrompt.length() > 0 ? param.negPrompt + ", " : "") +
                (promptNeg.length() > 0 ? promptNeg : "");
    }

    public JSONObject getControlnetTxt2imgJSON(SdParam param, Sketch mCurrentSketch, int batchSize) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("prompt", getPrompt(param, mCurrentSketch.getPrompt()));
            jsonObject.put("negative_prompt", getNegPrompt(param, mCurrentSketch.getNegPrompt()));
            if (mCurrentSketch.getStyle() != null) {
                jsonObject.put("styles", (new JSONArray()).put(mCurrentSketch.getStyle()));
            }
            jsonObject.put("seed", -1);
            jsonObject.put("batch_size", batchSize);
            jsonObject.put("n_iter", 1);
            jsonObject.put("steps", param.steps);
            jsonObject.put("cfg_scale", param.cfgScale);

            if (mCurrentSketch.getImgBackground().getHeight() > mCurrentSketch.getImgBackground().getWidth()) {
                jsonObject.put("width", Utils.getShortSize(mCurrentSketch.getImgBackground(), param.sdSize));
            } else {
                jsonObject.put("width", param.sdSize);
            }
            if (mCurrentSketch.getImgBackground().getHeight() < mCurrentSketch.getImgBackground().getWidth()) {
                jsonObject.put("height", Utils.getShortSize(mCurrentSketch.getImgBackground(), param.sdSize));
            } else {
                jsonObject.put("height", param.sdSize);
            }

            jsonObject.put("restore_faces", false);
            jsonObject.put("tiling", false);
            jsonObject.put("do_not_save_samples", true);
            jsonObject.put("do_not_save_grid", true);
            jsonObject.put("sampler_name", param.sampler);
            jsonObject.put("scheduler", param.scheduler);
            jsonObject.put("save_images", false);
            jsonObject.put("override_settings", getConfig(param));
            jsonObject.put("override_settings_restore_afterwards", false);

            if (param.cn != null) {
                JSONObject alwayson_scripts = new JSONObject();
                JSONObject controlnet = new JSONObject();
                JSONArray args = new JSONArray();
                for (CnParam cnparam : param.cn) {
                    if (cnparam.cnInputImage != null) {
                        // ControlNet Args
                        JSONObject cnArgObject = new JSONObject();
                        cnArgObject.put("image", Utils.jpg2Base64String(
                                cnparam.cnInputImage.equals(SdParam.SD_INPUT_IMAGE_SKETCH) ? mCurrentSketch.getImgPreview() :
                                cnparam.cnInputImage.equals(SdParam.SD_INPUT_IMAGE_REF) ? mCurrentSketch.getImgReference(param.sdSize) :
                                mCurrentSketch.getImgBackground(param.sdSize)));
                        //cnArgObject.put("mask", "");
                        cnArgObject.put("module", cnparam.cnModule);
                        cnArgObject.put("model",cnparam.cnModel);
                        cnArgObject.put("weight", cnparam.cnWeight);

                        cnArgObject.put("resize_mode", CnParam.CN_RESIZE_MODE[cnparam.cnResizeMode]);
                        cnArgObject.put("low_vram", false);
                        cnArgObject.put("pixel_perfect", true);
                        cnArgObject.put("processor_res", -1);
                        if (!Double.isNaN(cnparam.cnModuleParamA)) {
                            cnArgObject.put("threshold_a", cnparam.cnModuleParamA);
                        } else {
                            cnArgObject.put("threshold_a", -1);
                        }
                        if (!Double.isNaN(cnparam.cnModuleParamB)) {
                            cnArgObject.put("threshold_b", cnparam.cnModuleParamB);
                        } else {
                            cnArgObject.put("threshold_b", -1);
                        }
                        cnArgObject.put("enabled", true);
                        cnArgObject.put("guidance_start", cnparam.cnStart);
                        cnArgObject.put("guidance_end", cnparam.cnEnd);
                        cnArgObject.put("control_mode", CnParam.CN_CONTROL_MODE[cnparam.cnControlMode]);
                        args.put(cnArgObject);
                    }
                }
                controlnet.put("args", args);
                alwayson_scripts.put("controlnet", controlnet);
                jsonObject.put("alwayson_scripts", alwayson_scripts);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject getControlnetImg2imgJSON(SdParam param, Sketch mCurrentSketch, int batchSize) {
        JSONObject jsonObject = new JSONObject();
        boolean isInpaint = param.type.equals(SdParam.SD_MODE_TYPE_INPAINT);
        try {
            JSONArray init_images = new JSONArray();

            jsonObject.put("prompt", getPrompt(param, mCurrentSketch.getPrompt()));
            jsonObject.put("negative_prompt", getNegPrompt(param, mCurrentSketch.getNegPrompt()));
            if (mCurrentSketch.getStyle() != null) {
                jsonObject.put("styles", (new JSONArray()).put(mCurrentSketch.getStyle()));
            }
            jsonObject.put("seed", -1);
            jsonObject.put("batch_size", batchSize);
            jsonObject.put("n_iter", 1);
            if (param.inpaintPartial == SdParam.INPAINT_PARTIAL) {
                RectF inpaintRect = mCurrentSketch.getRectInpaint(param.sdSize);
                if (inpaintRect.width() >= inpaintRect.height()) {
                    jsonObject.put("width", param.sdSize);
                    double h = param.sdSize / (inpaintRect.width() - 1) * (inpaintRect.height() - 1);
                    h = 64 * round(h / 64);
                    jsonObject.put("height", h);
                } else {
                    jsonObject.put("height", param.sdSize);
                    double w = param.sdSize / (inpaintRect.height() - 1) * (inpaintRect.width() - 1);
                    w = 64 * round(w / 64);
                    jsonObject.put("width", w);
                }
                //Log.e("diffusionpaint", "SD Size=" + jsonObject.getDouble("width") + "X" + jsonObject.getDouble("height"));
            } else {
                if (mCurrentSketch.getImgBackground().getHeight() > mCurrentSketch.getImgBackground().getWidth()) {
                    jsonObject.put("width", Utils.getShortSize(mCurrentSketch.getImgBackground(), param.sdSize));
                } else {
                    jsonObject.put("width", param.sdSize);
                }
                if (mCurrentSketch.getImgBackground().getHeight() < mCurrentSketch.getImgBackground().getWidth()) {
                    jsonObject.put("height", Utils.getShortSize(mCurrentSketch.getImgBackground(), param.sdSize));
                } else {
                    jsonObject.put("height", param.sdSize);
                }
            }
            jsonObject.put("restore_faces", false);
            jsonObject.put("tiling", false);
            jsonObject.put("do_not_save_samples", true);
            jsonObject.put("do_not_save_grid", true);
            jsonObject.put("steps", param.steps);
            jsonObject.put("sampler_name", param.sampler);
            jsonObject.put("scheduler", param.scheduler);
            jsonObject.put("save_images", false);
            jsonObject.put("denoising_strength", param.denoise);
            jsonObject.put("cfg_scale", param.cfgScale);
            jsonObject.put("override_settings", getConfig(param));
            jsonObject.put("override_settings_restore_afterwards", false);

            Bitmap baseImage;
            if (isInpaint && (param.inpaintPartial == SdParam.INPAINT_PARTIAL)) {
                Bitmap bg = mCurrentSketch.getImgBackground();
                if (param.baseImage.equals(SdParam.SD_INPUT_IMAGE_SKETCH)) {
                    Bitmap bmEdit = Bitmap.createBitmap(mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvasEdit = new Canvas(bmEdit);
                    canvasEdit.drawBitmap(mCurrentSketch.getImgBackground(), null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
                    canvasEdit.drawBitmap(mCurrentSketch.getImgPaint(), null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
                    bg = bmEdit;
                } else if (param.baseImage.equals(SdParam.SD_INPUT_IMAGE_BG_REF)) {
                    bg = mCurrentSketch.getImgBgRef(0);
                }
                baseImage = Utils.extractBitmap(bg, mCurrentSketch.getRectInpaint(param.sdSize));

            } else {
                baseImage = param.baseImage.equals(SdParam.SD_INPUT_IMAGE_SKETCH) ? mCurrentSketch.getImgPreview() :
                        param.baseImage.equals(SdParam.SD_INPUT_IMAGE_BG_REF) ? mCurrentSketch.getImgBgRef(0) : mCurrentSketch.getImgBackground();
                if (baseImage.getHeight() < jsonObject.getInt("height") || baseImage.getWidth() < jsonObject.getInt("width")) {
                    double scale = max((double)jsonObject.getInt("height") / baseImage.getHeight(), (double)jsonObject.getInt("width") / baseImage.getWidth());
                    baseImage = Bitmap.createScaledBitmap(baseImage, (int) round(baseImage.getWidth() * scale), (int) round(baseImage.getHeight() * scale), true);
                }
            }

            init_images.put(Utils.jpg2Base64String(baseImage));
            jsonObject.put("init_images", init_images);
            jsonObject.put("resize_mode", 1);

            if (isInpaint) {
                if (param.inpaintPartial == SdParam.INPAINT_PARTIAL) {
                    RectF partialRect = mCurrentSketch.getRectInpaint(param.sdSize);
                    double ratio = (double)max(partialRect.width(), partialRect.height()) / param.sdSize;
                    int boundary = SdParam.SD_INPUT_IMAGE_SKETCH.equals(param.baseImage) ? (int) round(param.maskBlur * ratio) : 0;
                    mCurrentSketch.setImgInpaintMask(Sketch.getInpaintMaskFromPaint(mCurrentSketch, boundary, false));
                    Bitmap imgInpaintMask = Utils.extractBitmap(mCurrentSketch.getImgInpaintMask(), partialRect);
                    jsonObject.put("mask", Utils.png2Base64String(imgInpaintMask));
                } else {
                    double ratio = (double)max(mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight()) / param.sdSize;
                    int boundary = SdParam.SD_INPUT_IMAGE_SKETCH.equals(param.baseImage) ? (int) round(param.maskBlur * ratio) : 0;
                    mCurrentSketch.setImgInpaintMask(Sketch.getInpaintMaskFromPaint(mCurrentSketch, boundary, false));
                    jsonObject.put("mask", Utils.png2Base64String(mCurrentSketch.getImgInpaintMask()));
                }
                jsonObject.put("mask_blur", param.maskBlur);
                jsonObject.put("inpainting_fill", param.inpaintFill);
                jsonObject.put("inpaint_full_res", false);
                jsonObject.put("inpaint_full_res_padding", 32);
                //jsonObject.put("inpainting_mask_invert", 0);
                jsonObject.put("initial_noise_multiplier", 1);
            }

            // ControlNet Args
            if (param.cn != null) {
                JSONObject alwayson_scripts = new JSONObject();
                JSONObject controlnet = new JSONObject();
                JSONArray args = new JSONArray();
                for (CnParam cnparam : param.cn) {
                    if (cnparam.cnInputImage != null) {
                        JSONObject cnArgObject = new JSONObject();
                        Bitmap cnImage = null;
                        if (cnparam.cnInputImage.equals(SdParam.SD_INPUT_IMAGE_REF)) {
                            cnImage = mCurrentSketch.getImgReference(param.sdSize);
                        } else if (isInpaint && (param.inpaintPartial == SdParam.INPAINT_PARTIAL)) {
                            Bitmap bg = mCurrentSketch.getImgBackground();
                            if (cnparam.cnInputImage.equals(SdParam.SD_INPUT_IMAGE_SKETCH)) {
                                Bitmap bmEdit = Bitmap.createBitmap(mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight(), Bitmap.Config.ARGB_8888);
                                Canvas canvasEdit = new Canvas(bmEdit);
                                canvasEdit.drawBitmap(mCurrentSketch.getImgBackground(), null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
                                canvasEdit.drawBitmap(mCurrentSketch.getImgPaint(), null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
                                bg = bmEdit;
                            }
                            cnImage = Utils.extractBitmap(bg, mCurrentSketch.getRectInpaint(param.sdSize));
                        } else {
                            cnImage = cnparam.cnInputImage.equals(SdParam.SD_INPUT_IMAGE_SKETCH) ? mCurrentSketch.getImgPreview() : mCurrentSketch.getImgBackground(param.sdSize);
                        }

                        cnArgObject.put("image", Utils.jpg2Base64String(cnImage));
                        //cnArgObject.put("mask", "");
                        cnArgObject.put("module", cnparam.cnModule);
                        cnArgObject.put("model", cnparam.cnModel);
                        cnArgObject.put("weight", cnparam.cnWeight);
                        cnArgObject.put("resize_mode", CnParam.CN_RESIZE_MODE[cnparam.cnResizeMode]);
                        cnArgObject.put("low_vram", false);
                        cnArgObject.put("pixel_perfect", true);
                        cnArgObject.put("processor_res", -1);
                        if (!Double.isNaN(cnparam.cnModuleParamA)) {
                            cnArgObject.put("threshold_a", cnparam.cnModuleParamA);
                        } else {
                            cnArgObject.put("threshold_a", -1);
                        }
                        if (!Double.isNaN(cnparam.cnModuleParamB)) {
                            cnArgObject.put("threshold_b", cnparam.cnModuleParamB);
                        } else {
                            cnArgObject.put("threshold_b", -1);
                        }
                        cnArgObject.put("enabled", true);
                        cnArgObject.put("guidance_start", cnparam.cnStart);
                        cnArgObject.put("guidance_end", cnparam.cnEnd);
                        cnArgObject.put("control_mode", CnParam.CN_CONTROL_MODE[cnparam.cnControlMode]);
                        args.put(cnArgObject);
                    }
                }
                controlnet.put("args", args);
                alwayson_scripts.put("controlnet", controlnet);
                jsonObject.put("alwayson_scripts", alwayson_scripts);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public List<String> getCnModel(String responseBody) {
        try {
            JSONObject responseObject = new JSONObject(responseBody);
            JSONArray modelList = responseObject.getJSONArray("model_list");
            List<String> cnModels = new ArrayList<>();
            for (int i = 0; i < modelList.length(); i++) {
                cnModels.add(modelList.getString(i));
            }
            return cnModels;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }



    public List<String> getSampler(String responseBody) {
        try {
            JSONArray response = new JSONArray(responseBody);
            List<String> samplers = new ArrayList<>();
            for (int i = 0; i < response.length(); i++) {
                JSONObject sampler = response.getJSONObject(i);
                samplers.add(sampler.getString("name"));
            }
            return samplers;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<String> getLoras(String responseBody) {
        try {
            JSONArray loraArray = new JSONArray(responseBody);

            List<JSONObject> jsonList = new ArrayList<>();
            for (int i = 0; i < loraArray.length(); i++) {
                try {
                    jsonList.add(loraArray.getJSONObject(i));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            jsonList.sort((jsonObject1, jsonObject2) -> {
                String name1 = jsonObject1.optString("path");
                String name2 = jsonObject2.optString("path");
                return name1.compareToIgnoreCase(name2);
            });
            loraArray = new JSONArray(jsonList);

            List<String> loraList = new ArrayList<>();
            for (int i = 0; i < loraArray.length(); i++) {
                loraList.add("<lora:" + loraArray.getJSONObject(i).getString("name") + ":0.5>");
            }
            return loraList;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<SdStyle> getStyles(String responseBody) {
        try {
            JSONArray styleArray = new JSONArray(responseBody);

            List<JSONObject> jsonList = new ArrayList<>();
            for (int i = 0; i < styleArray.length(); i++) {
                try {
                    jsonList.add(styleArray.getJSONObject(i));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            jsonList.sort((jsonObject1, jsonObject2) -> {
                String name1 = jsonObject1.optString("name");
                String name2 = jsonObject2.optString("name");
                return name1.compareToIgnoreCase(name2);
            });
            styleArray = new JSONArray(jsonList);

            List<SdStyle> styleList = new ArrayList<>();
            for (int i = 0; i < styleArray.length(); i++) {
                SdStyle style = new SdStyle();
                style.name = styleArray.getJSONObject(i).getString("name");
                style.prompt = styleArray.getJSONObject(i).getString("prompt");
                style.negPrompt = styleArray.getJSONObject(i).getString("negative_prompt");
                styleList.add(style);
            }
            return styleList;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
