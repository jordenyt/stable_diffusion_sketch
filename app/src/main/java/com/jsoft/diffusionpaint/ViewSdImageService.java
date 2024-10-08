package com.jsoft.diffusionpaint;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Lifecycle;

import com.jsoft.diffusionpaint.helper.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private JSONObject requestJSON;
    private static OkHttpClient client;
    private String sdBaseUrl;
    private int numGen;
    private List<String> listInfotext = new ArrayList<>();
    private List<Bitmap> listBitmap = new ArrayList<>();
    private static SharedPreferences sharedPreferences;

    private static final int FOREGROUND_ID = 1;
    private Notification notification;
    private boolean isRunning;
    private static Handler handler;
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
        try {
            String requestType = intent.getStringExtra("requestType");
            sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            if (intent.hasExtra("numGen")) {
                numGen = intent.getIntExtra("numGen", 1);
            } else {
                numGen = 1;
            }
            isRunning = true;
            listInfotext = new ArrayList<>();
            listBitmap = new ArrayList<>();
            handler = new Handler();
            showForegroundNotification();
            callSD4Img(requestType);
        } catch (Exception e) {
            isRunning = false;
            stopForeground(true);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isRunning) {
            isRunning = false;
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

    public void setObject(String baseUrl, JSONObject jsonObject) {
        this.sdBaseUrl = baseUrl;
        this.requestJSON = jsonObject;
    }

    public void setActivity(ViewSdImageActivity activity) {
        this.activity = activity;
    }

    public void callSD4Img(String requestType) {
        ViewSdImageActivity.rtResultType = null;
        ViewSdImageActivity.rtBitmap = null;
        ViewSdImageActivity.rtInfotext = null;
        ViewSdImageActivity.rtErrMsg = null;
        if (requestType.equals("txt2img")) {
            ViewSdImageActivity.isCallingSD = true;
            sendRequest("txt2img", sdBaseUrl,"/sdapi/v1/txt2img", requestJSON);
        } else if (requestType.equals("img2img")){
            ViewSdImageActivity.isCallingSD = true;
            sendRequest("img2img", sdBaseUrl, "/sdapi/v1/img2img", requestJSON);
        } else {
            ViewSdImageActivity.isCallingAPI = true;
            sendRequest("extraSingleImage", sdBaseUrl, "/sdapi/v1/extra-single-image", requestJSON);
        }
        if (activity != null && !activity.isDestroyed() && !activity.isFinishing()) {
            activity.runOnUiThread(() -> activity.updateScreen());
        }
    }

    public void sendRequest(String requestType, String baseUrl, String url, JSONObject jsonObject) {
        sendRequest(requestType, baseUrl, url, jsonObject, "POST");
    }

    public void sendRequest(String requestType, String baseUrl, String url) {
        sendRequest(requestType, baseUrl, url, null, "GET");
    }

    public void sendRequest(String requestType, String baseUrl, String url, JSONObject jsonObject, String method) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(baseUrl + url);

        if ("POST".equals(method)) {
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(jsonObject.toString(), JSON);
            requestBuilder.post(body);
        } else if ("GET".equals(method)) {
            requestBuilder.get();
        }

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
                        onSdApiFailure(requestType, responseBody.string());
                        return;
                    }

                    assert responseBody != null;
                    String responseString = responseBody.string();
                    onSdApiResponse(requestType, responseString);

                } catch (Exception e) {
                    e.printStackTrace();
                    onSdApiFailure(requestType, "onResponse Exception: " + e.getMessage());
                }
            }
        });
    }

    private void onSdApiResponse(String requestType, String responseBody) {
        try {
            switch (requestType) {
                case "txt2img":
                case "img2img": {
                    ViewSdImageActivity.isCallingSD = false;
                    JSONObject jsonObject = new JSONObject(responseBody);
                    JSONArray images = jsonObject.getJSONArray("images");
                    String info = jsonObject.getString("info");
                    JSONObject infoObject = new JSONObject(info);
                    JSONArray infotextsArray = infoObject.getJSONArray("infotexts");

                    if (images.length() > 0 && !ViewSdImageActivity.isInterrupted) {
                        int numImage = 1;
                        try {
                            numImage = requestJSON.getInt("batch_size");
                        } catch (JSONException ignored) {}
                        for (int i=0;i<numImage;i++) {
                            listInfotext.add(infotextsArray.getString(i).replaceAll("\\\\n","\n"));
                            listBitmap.add(Utils.base64String2Bitmap((String) images.get(i)));
                        }
                        if (activity != null && !activity.isDestroyed() && !activity.isFinishing() && activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            activity.runOnUiThread(() -> activity.processResultBitmap(requestType, listBitmap, listInfotext));
                            isRunning = false;
                            stopForeground(true);
                        } else {
                            int nextBatchSize = Math.min(numGen - listBitmap.size(), Integer.parseInt(sharedPreferences.getString("maxBatchSize", "1")));
                            if (nextBatchSize > 0 && requestJSON.has("batch_size")) {
                                requestJSON.put("batch_size", nextBatchSize);
                                callSD4Img(requestType);
                            } else {
                                ViewSdImageActivity.rtResultType = requestType;
                                ViewSdImageActivity.rtBitmap = listBitmap;
                                ViewSdImageActivity.rtInfotext = listInfotext;
                                isRunning = false;
                                stopForeground(true);
                            }
                        }
                    } else if (!listBitmap.isEmpty() && ViewSdImageActivity.isInterrupted) {
                        if (activity != null && !activity.isDestroyed() && !activity.isFinishing() && activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            activity.runOnUiThread(() -> activity.processResultBitmap(requestType, listBitmap, listInfotext));
                        } else {
                            ViewSdImageActivity.rtResultType = requestType;
                            ViewSdImageActivity.rtBitmap = listBitmap;
                            ViewSdImageActivity.rtInfotext = listInfotext;
                        }
                        isRunning = false;
                        stopForeground(true);
                    } else {
                        ViewSdImageActivity.isInterrupted = false;
                        if (activity != null && !activity.isDestroyed() && !activity.isFinishing() && activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                            activity.runOnUiThread(() -> activity.updateScreen());
                        }
                        isRunning = false;
                        stopForeground(true);
                    }
                    break;
                }
                case "extraSingleImage": {
                    ViewSdImageActivity.isCallingAPI = false;
                    JSONObject jsonObject = new JSONObject(responseBody);
                    String imageStr = jsonObject.getString("image");
                    List<Bitmap> listBitmap = new ArrayList<>();
                    listBitmap.add(Utils.base64String2Bitmap(imageStr));
                    if (activity != null && !activity.isDestroyed() && !activity.isFinishing() && activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                        activity.runOnUiThread(() -> activity.processResultBitmap(requestType, listBitmap, null));
                    } else {
                        ViewSdImageActivity.rtResultType = requestType;
                        ViewSdImageActivity.rtBitmap = listBitmap;
                        ViewSdImageActivity.rtInfotext = null;
                    }
                    isRunning = false;
                    stopForeground(true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            onSdApiFailure(requestType, "onSdApiResponse Exception: " + e.getMessage());
        }
    }

    private void onSdApiFailure(String requestType, String errMsg) {
        isRunning = false;
        stopForeground(true);
        ViewSdImageActivity.isCallingSD = false;
        ViewSdImageActivity.isCallingAPI = false;
        ViewSdImageActivity.isInterrupted = false;
        if (activity != null && !activity.isDestroyed() && !activity.isFinishing()) {
            activity.runOnUiThread(() -> activity.onSdApiFailure(requestType, errMsg));
        } else {
            ViewSdImageActivity.rtResultType = requestType;
            ViewSdImageActivity.rtErrMsg = errMsg;
        }
    }
}