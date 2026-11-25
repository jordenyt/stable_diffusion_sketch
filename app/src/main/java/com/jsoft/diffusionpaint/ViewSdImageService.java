package com.jsoft.diffusionpaint;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
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

    /** The gateway job id of the job that is currently running. */
    private String jobId;
    /** When the current poll loop started, and how often it failed. */
    private long pollStartedAt;
    private int pollFailureCount;

    private static final int FOREGROUND_ID = 1;
    private Notification notification;
    private boolean isRunning;
    private static Handler handler;

    /** Give up on a job after this long - video workflows can take many minutes. */
    private static final long MAX_POLL_DURATION_MS = 30 * 60 * 1000L;
    /** Give up after this many consecutive polling failures. */
    private static final int MAX_POLL_FAILURES = 10;

    private static final String PREF_JOB_ID = "comfyuiJobId";
    private static final String PREF_REQUEST_JSON = "comfyuiRequestJSON";
    private static final String PREF_BASE_URL = "comfyuiBaseUrl";
    private static final String PREF_NUM_GEN = "comfyuiNumGen";

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
            sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            if (intent == null) {
                // Android restarted the service with a null intent. Pick the job
                // back up instead of losing it.
                if (!resumeSavedJob()) {
                    stopSelf();
                    return START_NOT_STICKY;
                }
                return START_STICKY;
            }
            String requestType = intent.getStringExtra("requestType");
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

    /** The job id of the job that is currently running, or null. */
    public String getJobId() {
        return jobId;
    }

    private void showForegroundNotification() {
        Intent intent = new Intent(this, ViewSdImageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        notification = new NotificationCompat.Builder(this, ViewSdImageActivity.CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_magic_pen)
                .setContentTitle("API Call Running")
                .setContentText("Calling API in background...")
                .setOngoing(true)
                .setContentIntent(pendingIntent)
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
        if (requestType.equals("comfyui")){
            ViewSdImageActivity.isCallingDFL = true;
            jobId = null;
            pollStartedAt = System.currentTimeMillis();
            pollFailureCount = 0;
            sendRequest("comfyui", sdBaseUrl, "/comfyui_workflow", requestJSON);
        } else {
            ViewSdImageActivity.isCallingAPI = true;
            sendRequest("extraSingleImage", sdBaseUrl, "/sdapi/v1/extra-single-image", requestJSON);
        }
        if (activity != null && !activity.isDestroyed() && !activity.isFinishing()) {
            activity.runOnUiThread(() -> activity.updateScreen());
        }
    }

    /** The status url of the job that is currently running. */
    private String statusUrl() {
        if (jobId == null || jobId.isEmpty()) {
            return "/comfyui_status";
        }
        return "/comfyui_status?job_id=" + jobId;
    }

    /** The result url of the job that is currently running. */
    private String resultUrl() {
        if (jobId == null || jobId.isEmpty()) {
            return "/comfyui_result";
        }
        return "/comfyui_result?job_id=" + jobId;
    }

    private void saveJobState() {
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PREF_JOB_ID, jobId);
        editor.putString(PREF_BASE_URL, sdBaseUrl);
        editor.putInt(PREF_NUM_GEN, numGen);
        if (requestJSON != null) {
            editor.putString(PREF_REQUEST_JSON, requestJSON.toString());
        }
        editor.apply();
    }

    private void clearJobState() {
        if (sharedPreferences == null) {
            return;
        }
        sharedPreferences.edit().remove(PREF_JOB_ID).remove(PREF_REQUEST_JSON)
                .remove(PREF_BASE_URL).remove(PREF_NUM_GEN).apply();
    }

    /** Continue a job that was interrupted by a service restart. */
    private boolean resumeSavedJob() {
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        }
        String savedJobId = sharedPreferences.getString(PREF_JOB_ID, null);
        String savedBaseUrl = sharedPreferences.getString(PREF_BASE_URL, null);
        if (savedJobId == null || savedJobId.isEmpty() || savedBaseUrl == null) {
            return false;
        }
        jobId = savedJobId;
        sdBaseUrl = savedBaseUrl;
        numGen = sharedPreferences.getInt(PREF_NUM_GEN, 1);
        String savedJson = sharedPreferences.getString(PREF_REQUEST_JSON, null);
        if (savedJson != null) {
            try {
                requestJSON = new JSONObject(savedJson);
            } catch (JSONException ignored) {}
        }
        isRunning = true;
        listInfotext = new ArrayList<>();
        listBitmap = new ArrayList<>();
        handler = new Handler();
        pollStartedAt = System.currentTimeMillis();
        pollFailureCount = 0;
        showForegroundNotification();
        handler.postDelayed(() -> sendRequest("comfyuiStatus", sdBaseUrl, statusUrl()), 500);
        return true;
    }

    public void sendRequest(String requestType, String baseUrl, String url, JSONObject jsonObject) {
        sendRequest(requestType, baseUrl, url, jsonObject, "POST");
    }

    public void sendRequest(String requestType, String baseUrl, String url) {
        sendRequest(requestType, baseUrl, url, null, "GET");
    }

    public void sendRequest(String requestType, String baseUrl, String url, JSONObject jsonObject, String method) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(baseUrl + url)
                // The gateway answers with a job id only when JSON is accepted.
                .header("Accept", "application/json");

        if ("POST".equals(method)) {
            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(jsonObject == null ? "{}" : jsonObject.toString(), JSON);
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
                        String errorBody = "";
                        try {
                            errorBody = (responseBody == null) ? "" : responseBody.string();
                        } catch (IOException ignored) {}
                        onSdApiFailure(requestType, buildErrorMessage(response.code(), errorBody));
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
                case "comfyui": {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    if (jsonObject.has("status")) {
                        if (jsonObject.has("job_id")) {
                            jobId = jsonObject.getString("job_id");
                            saveJobState();
                        }
                        pollFailureCount = 0;
                        handler.postDelayed(() -> sendRequest("comfyuiStatus", sdBaseUrl, statusUrl()), 1000);
                    } else {
                        isRunning = false;
                        stopForeground(true);
                        clearJobState();
                        onSdApiFailure(requestType, "ComfyUI not started.");
                    }
                    break;
                }
                case "comfyuiStatus": {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    if (jsonObject.has("progress") && jsonObject.has("step")) {
                        pollFailureCount = 0;
                        String progress = jsonObject.getString("progress");
                        String step = jsonObject.getString("step");
                        String state = jsonObject.has("state") ? jsonObject.optString("state", "") : "";
                        String error = jsonObject.has("error") && !jsonObject.isNull("error")
                                ? jsonObject.optString("error", "") : "";

                        if (activity != null && !activity.isDestroyed() && !activity.isFinishing()) {
                            String statusText = statusText(state, progress, step, jsonObject);
                            activity.runOnUiThread(() -> activity.updateStatus(statusText));
                        }

                        // Update notification with progress and step
                        Intent intent = new Intent(this, ViewSdImageActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        notification = new NotificationCompat.Builder(this, ViewSdImageActivity.CHANNEL_ID)
                                .setSmallIcon(R.mipmap.ic_magic_pen)
                                .setContentTitle("ComfyUI Status")
                                .setContentText(progress + " | " + step)
                                .setOngoing(true)
                                .setContentIntent(pendingIntent)
                                .build();
                        startForeground(FOREGROUND_ID, notification);

                        if ("error".equals(state)) {
                            isRunning = false;
                            stopForeground(true);
                            clearJobState();
                            onSdApiFailure(requestType, error.isEmpty() ? "ComfyUI job failed." : error);
                        } else if ("cancelled".equals(state)) {
                            // The user cancelled the job - stop quietly.
                            isRunning = false;
                            stopForeground(true);
                            clearJobState();
                            ViewSdImageActivity.isCallingDFL = false;
                            ViewSdImageActivity.isInterrupted = true;
                        } else if ("done".equals(state) || (state.isEmpty() && "Done".equals(progress))) {
                            sendRequest("comfyuiResult", sdBaseUrl, resultUrl());
                        } else {
                            handler.postDelayed(() -> sendRequest("comfyuiStatus", sdBaseUrl, statusUrl()), 1000);
                        }
                    } else {
                        isRunning = false;
                        stopForeground(true);
                        clearJobState();
                        onSdApiFailure(requestType, "Cannot get ComfyUI Status.");
                    }
                    break;
                }
                case "comfyuiResult": {
                    ViewSdImageActivity.isCallingDFL = false;
                    JSONObject jsonObject = new JSONObject(responseBody);
                    String actualRequestType = "comfyuiResult".equals(requestType) ? "comfyui": requestType;

                    JSONArray images = jsonObject.getJSONArray("processed_image");
                    if (images.length() > 0) {
                        for (int i=0;i<images.length();i++) {
                            listBitmap.add(Utils.base64String2Bitmap(images.getString(i)));
                        }
                    }

                    if (activity != null && !activity.isDestroyed() && !activity.isFinishing() && activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                        isRunning = false;
                        stopForeground(true);
                        clearJobState();
                        activity.runOnUiThread(() -> activity.processResultBitmap(actualRequestType, listBitmap, null));
                    } else {
                        int nextBatchSize = Math.min(numGen - listBitmap.size(), Integer.parseInt(sharedPreferences.getString("maxBatchSize", "1")));
                        if (nextBatchSize > 0) {
                            if (requestJSON.has("batch_size")) {
                                requestJSON.put("batch_size", nextBatchSize);
                            }
                            callSD4Img(actualRequestType);
                        } else {
                            ViewSdImageActivity.rtResultType = actualRequestType;
                            ViewSdImageActivity.rtBitmap = listBitmap;
                            ViewSdImageActivity.rtInfotext = null;
                            isRunning = false;
                            stopForeground(true);
                            clearJobState();
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            onSdApiFailure(requestType, "onSdApiResponse Exception: " + e.getMessage());
        }
    }

    /** The text shown while the job is running. */
    private static String statusText(String state, String progress, String step, JSONObject jsonObject) {
        if ("queued".equals(state)) {
            int position = jsonObject.optInt("queue_position", 0);
            int length = jsonObject.optInt("queue_length", 0);
            if (position > 0) {
                return "Queued (position " + position + " of " + length + ")";
            }
            return "Queued";
        }
        return progress + "\n" + step;
    }

    private void onSdApiFailure(String requestType, String errMsg) {
        if ("comfyuiStatus".equals(requestType)) {
            pollFailureCount++;
            boolean tooLong = (System.currentTimeMillis() - pollStartedAt) > MAX_POLL_DURATION_MS;
            if (tooLong || pollFailureCount > MAX_POLL_FAILURES) {
                isRunning = false;
                stopForeground(true);
                clearJobState();
                ViewSdImageActivity.isCallingAPI = false;
                ViewSdImageActivity.isInterrupted = false;
                ViewSdImageActivity.isCallingDFL = false;
                String message = tooLong
                        ? "ComfyUI did not finish the job in time."
                        : "Cannot reach ComfyUI: " + errMsg;
                if (activity != null && !activity.isDestroyed() && !activity.isFinishing()) {
                    activity.runOnUiThread(() -> activity.onSdApiFailure(requestType, message));
                } else {
                    ViewSdImageActivity.rtResultType = requestType;
                    ViewSdImageActivity.rtErrMsg = message;
                }
                return;
            }
            handler.postDelayed(() -> sendRequest("comfyuiStatus", sdBaseUrl, statusUrl()), 1000);
            return;
        }
        isRunning = false;
        stopForeground(true);
        clearJobState();
        ViewSdImageActivity.isCallingAPI = false;
        ViewSdImageActivity.isInterrupted = false;
        ViewSdImageActivity.isCallingDFL = false;
        if (activity != null && !activity.isDestroyed() && !activity.isFinishing()) {
            activity.runOnUiThread(() -> activity.onSdApiFailure(requestType, errMsg));
        } else {
            ViewSdImageActivity.rtResultType = requestType;
            ViewSdImageActivity.rtErrMsg = errMsg;
        }
    }

    /** Turn an error response into a readable message, keeping the server's detail. */
    private static String buildErrorMessage(int code, String errorBody) {
        String detail = null;
        if (errorBody != null && !errorBody.isEmpty()) {
            try {
                JSONObject json = new JSONObject(errorBody);
                if (json.has("detail")) {
                    detail = json.get("detail").toString();
                } else if (json.has("error")) {
                    detail = json.get("error").toString();
                }
            } catch (JSONException ignored) {
                detail = errorBody;
            }
        }
        if (detail == null || detail.isEmpty()) {
            return "Response Code: " + code;
        }
        return "Response Code: " + code + " - " + detail;
    }
}
