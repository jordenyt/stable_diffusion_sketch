package com.jsoft.diffusionpaint;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import com.jsoft.diffusionpaint.dto.SdParam;
import com.jsoft.diffusionpaint.dto.Sketch;
import com.jsoft.diffusionpaint.helper.SdApiHelper;
import com.jsoft.diffusionpaint.helper.SdApiResponseListener;
import com.jsoft.diffusionpaint.helper.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ViewSdImageService extends Service {

    private final IBinder binder = new ViewSdImageBinder();
    private ViewSdImageActivity activity;
    private OkHttpClient client;
    private SdApiHelper sdApiHelper;
    private Sketch mCurrentSketch;
    private String sdBaseUrl;
    public ViewSdImageService() {
    }

    public class ViewSdImageBinder extends Binder {
        ViewSdImageService getService() {
            return ViewSdImageService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setObject(SdApiHelper sdApiHelper, Sketch mCurrentSketch, OkHttpClient postRequestClient, String baseUrl, ViewSdImageActivity activity) {
        this.activity = activity;
        this.client = postRequestClient;
        this.sdApiHelper = sdApiHelper;
        this.mCurrentSketch = mCurrentSketch;
        this.sdBaseUrl = baseUrl;
    }

    public void callSD4Img() {
        ViewSdImageActivity.isCallingSD = true;
        SdParam param = sdApiHelper.getSdCnParm(mCurrentSketch.getCnMode());
        if (param.type.equals(SdParam.SD_MODE_TYPE_TXT2IMG)) {
            JSONObject jsonObject = sdApiHelper.getControlnetTxt2imgJSON(param, mCurrentSketch);
            sendRequest("txt2img", sdBaseUrl,"/sdapi/v1/txt2img", jsonObject);
        } else {
            JSONObject jsonObject = sdApiHelper.getControlnetImg2imgJSON(param, mCurrentSketch);
            sendRequest("img2img", sdBaseUrl, "/sdapi/v1/img2img", jsonObject);
        }
    }

    public void sendRequest(String requestType, String baseUrl, String url, JSONObject jsonObject) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(baseUrl + url);

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
        requestBuilder.post(body);

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                onSdApiFailure(requestType, e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        onSdApiFailure(requestType, "Response Code: " + response.code());
                    }

                    assert responseBody != null;
                    String responseString = responseBody.string();
                    onSdApiResponse(requestType, responseString);

                } catch (IOException e) {
                    e.printStackTrace();
                    onSdApiFailure(requestType, "IOException: " + e.getMessage());
                }
            }
        });
    }

    private void onSdApiResponse(String requestType, String responseBody) {
        //activity.runOnUiThread(() -> activity.onSdApiResponse(requestType, responseString));
        try {
            if (requestType.equals("txt2img") || requestType.equals("img2img")) {

                ViewSdImageActivity.isCallingSD = false;
                JSONObject jsonObject = new JSONObject(responseBody);
                JSONArray images = jsonObject.getJSONArray("images");
                if (images.length() > 0) {
                    ViewSdImageActivity.mBitmap = Utils.base64String2Bitmap((String) images.get(0));
                    if ("img2img".equals(requestType)) {
                        ViewSdImageActivity.updateMBitmap();
                    }
                }
                ViewSdImageActivity.savedImageName = null;
                ViewSdImageActivity.addResult(requestType);

                ViewSdImageActivity.remainGen--;
                if (ViewSdImageActivity.remainGen > 0) {
                    callSD4Img();
                }
                activity.runOnUiThread(() -> activity.updateScreen());

            } else if (requestType.equals("extraSingleImage")) {
                ViewSdImageActivity.isCallingSD = false;
                JSONObject jsonObject = new JSONObject(responseBody);
                String imageStr = jsonObject.getString("image");
                ViewSdImageActivity.mBitmap = Utils.base64String2Bitmap(imageStr);
                ViewSdImageActivity.updateMBitmap();

                ViewSdImageActivity.savedImageName = null;
                ViewSdImageActivity.addResult(requestType);
                activity.runOnUiThread(() -> activity.updateScreen());
            }
        } catch (JSONException ignored) {}
    }

    private void onSdApiFailure(String requestType, String errMsg) {
        activity.runOnUiThread(() -> activity.onSdApiFailure(requestType, errMsg));
    }
}