package com.jsoft.diffusionpaint.helper;

import static com.jsoft.diffusionpaint.dto.Sketch.CN_MODE_ORIGIN;

import static java.lang.Math.*;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.jsoft.diffusionpaint.dto.SdParam;
import com.jsoft.diffusionpaint.dto.Sketch;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
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
                    if (maskImage == null) {
                        double ratio = (double) max(backgroundImage.getWidth(), backgroundImage.getHeight()) / sdParam.sdSize;
                        int boundary = (int) round(sdParam.maskBlur * (sdParam.baseImage.equals(SdParam.SD_INPUT_IMAGE_SKETCH) ? 1 : 0) * ratio);
                        maskImage = Sketch.getInpaintMaskFromPaint(mCurrentSketch, boundary, false);
                        mCurrentSketch.setImgInpaintMask(maskImage);
                        jsonObject.put(key, Utils.jpg2Base64String(maskImage));
                    }
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
        return Sketch.defaultJSON.get(CN_MODE_ORIGIN);
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
        return cnMode.startsWith(Sketch.CN_MODE_COMFYUI) ? sharedPreferences.getString("modeComyui" + getComfyuiModeName(cnMode), getComfyuiDefault(cnMode)) :
                Sketch.defaultJSON.get(cnMode) != null ? Sketch.defaultJSON.get(cnMode) : Sketch.defaultJSON.get(CN_MODE_ORIGIN);
    }

    public SdParam getSdCnParm(String cnMode) {
        Gson gson = new Gson();
        String jsonMode = getSdParmJSON(cnMode);
        SdParam param = gson.fromJson(jsonMode, SdParam.class);
        return param;
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



}
