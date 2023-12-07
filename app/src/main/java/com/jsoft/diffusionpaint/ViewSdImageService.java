package com.jsoft.diffusionpaint;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;


import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.jsoft.diffusionpaint.dto.SdParam;
import com.jsoft.diffusionpaint.helper.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ViewSdImageService extends Service {

    private final IBinder binder = new ViewSdImageBinder();
    private ViewSdImageActivity activity;
    private static OkHttpClient client;
    //private SdApiHelper sdApiHelper;
    //private Sketch mCurrentSketch;
    private String sdBaseUrl;

    private static final int FOREGROUND_ID = 1;
    private Notification notification;
    private boolean isRunning;
    public ViewSdImageService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(900, TimeUnit.SECONDS)
                .build();
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        showForegroundNotification();

        // Start long-running API call in a separate thread
        new Thread(() -> {
            try {
                String requestType = intent.getStringExtra("requestType");
                JSONObject jsonObject = new JSONObject(intent.getStringExtra("json"));
                callSD4Img(requestType, jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                isRunning = false;
                stopForeground(true);
                stopSelf();
            }
        }).start();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isRunning) {
            stopForeground(true);
        }
    }

    private void showForegroundNotification() {
        notification = new NotificationCompat.Builder(this, ViewSdImageActivity.CHANNEL_ID)
                .setContentTitle("API Call Running")
                .setContentText("Calling API in background...")
                .setOngoing(true)
                .build();

        startForeground(FOREGROUND_ID, notification);
    }

    public void setObject(String baseUrl, ViewSdImageActivity activity) {
        this.activity = activity;
        this.sdBaseUrl = baseUrl;
    }

    public void callSD4Img(String requestType, JSONObject jsonObject) {
        if (requestType.equals(SdParam.SD_MODE_TYPE_TXT2IMG)) {
            sendRequest("txt2img", sdBaseUrl,"/sdapi/v1/txt2img", jsonObject);
        } else if (requestType.equals(SdParam.SD_MODE_TYPE_IMG2IMG)){
            sendRequest("img2img", sdBaseUrl, "/sdapi/v1/img2img", jsonObject);
        } else {
            sendRequest("extraSingleImage", sdBaseUrl, "/sdapi/v1/extra-single-image", jsonObject);
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
                    onSdApiResponse(requestType, responseString, jsonObject);

                } catch (IOException e) {
                    e.printStackTrace();
                    onSdApiFailure(requestType, "IOException: " + e.getMessage());
                }
            }
        });
    }

    private void onSdApiResponse(String requestType, String responseBody, JSONObject requestJsonObject) {
        try {
            if (requestType.equals("txt2img") || requestType.equals("img2img")) {

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
                    callSD4Img(requestType, requestJsonObject);
                } else {
                    ViewSdImageActivity.isCallingSD = false;
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