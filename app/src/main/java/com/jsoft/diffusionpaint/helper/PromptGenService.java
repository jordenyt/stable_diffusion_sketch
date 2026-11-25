package com.jsoft.diffusionpaint.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.jsoft.diffusionpaint.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Runs a prompt generation request on the gateway.
 *
 * The request is a normal gateway job: it is submitted with
 * POST /comfyui_workflow, followed with GET /comfyui_status and finished with
 * GET /comfyui_text. Because the job has an id, the user can stop it with
 * POST /comfyui_cancel/{job_id} at any time.
 *
 * The service runs in the foreground, so the request survives the app going to
 * the background, and the result is kept in {@link PromptGenStore} until the
 * user picks it up.
 */
public class PromptGenService extends Service {

    public static final String ACTION_START = "com.jsoft.diffusionpaint.action.START_PROMPT_GEN";
    public static final String ACTION_CANCEL = "com.jsoft.diffusionpaint.action.CANCEL_PROMPT_GEN";

    private static final String EXTRA_REQUEST_PATH = "requestPath";
    private static final String EXTRA_WORKFLOW = "workflow";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_SKETCH_ID = "sketchId";

    private static final String CHANNEL_ID = "prompt_gen";
    private static final int NOTIFICATION_ID = 2;

    /** Give up on a job after this long. */
    private static final long MAX_POLL_DURATION_MS = 30 * 60 * 1000L;
    /** Give up after this many consecutive polling failures. */
    private static final int MAX_POLL_FAILURES = 10;

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences sharedPreferences;
    private String baseUrl = "";
    private long pollStartedAt;
    private int pollFailureCount;
    private boolean running;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        baseUrl = sharedPreferences.getString("dflApiAddress", "");
        PromptGenStore.setAppContext(this);

        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            // A foreground service has to call startForeground before it stops,
            // even when it only lives for a moment.
            startForegroundNotification(PromptGenStore.get().getTitle());
            cancelJob();
            return START_NOT_STICKY;
        }

        if (intent != null && ACTION_START.equals(intent.getAction())) {
            String requestPath = intent.getStringExtra(EXTRA_REQUEST_PATH);
            String workflow = intent.getStringExtra(EXTRA_WORKFLOW);
            String title = intent.getStringExtra(EXTRA_TITLE);
            int sketchId = intent.getIntExtra(EXTRA_SKETCH_ID, -1);
            startForegroundNotification(title == null ? "Generating prompt" : title);
            PromptGenStore.get().begin(workflow, title, requestPath, sketchId);
            submit(requestPath);
            return START_STICKY;
        }

        // The service was restarted by the system: pick the running job up.
        if (resumeRunningJob()) {
            return START_STICKY;
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /** Continue a job that was interrupted by a service restart. */
    private boolean resumeRunningJob() {
        PromptGenStore store = PromptGenStore.get();
        if (!store.isRunning() || store.getJobId().isEmpty()) {
            return false;
        }
        startForegroundNotification(store.getTitle());
        running = true;
        pollStartedAt = System.currentTimeMillis();
        pollFailureCount = 0;
        handler.postDelayed(this::pollStatus, 500);
        return true;
    }

    /** Submit the request payload that the activity wrote to a file. */
    private void submit(String requestPath) {
        if (requestPath == null || requestPath.isEmpty()) {
            PromptGenStore.get().fail("No request payload.");
            stopSelf();
            return;
        }
        String payload;
        try {
            payload = new String(Files.readAllBytes(new File(requestPath).toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            PromptGenStore.get().fail("Cannot read the request payload: " + e.getMessage());
            stopSelf();
            return;
        }
        new File(requestPath).delete();

        Request request = new Request.Builder()
                .url(baseUrl + "/comfyui_workflow")
                // Without this header the gateway answers with an HTML page.
                .header("Accept", "application/json")
                .post(RequestBody.create(payload, MediaType.get("application/json; charset=utf-8")))
                .build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                fail("Cannot reach the gateway: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    String responseBody = body == null ? "" : body.string();
                    if (!response.isSuccessful()) {
                        fail(describeGatewayError(response.code(), responseBody));
                        return;
                    }
                    JSONObject json = new JSONObject(responseBody);
                    String jobId = json.optString("job_id", "");
                    if (jobId.isEmpty()) {
                        fail("The gateway did not return a job id.");
                        return;
                    }
                    PromptGenStore.get().setJobId(jobId);
                    running = true;
                    pollStartedAt = System.currentTimeMillis();
                    pollFailureCount = 0;
                    handler.postDelayed(PromptGenService.this::pollStatus, 500);
                } catch (JSONException | IOException e) {
                    fail("Unexpected answer from the gateway: " + e.getMessage());
                }
            }
        });
    }

    /** Follow the job until it is done. */
    private void pollStatus() {
        if (!running) {
            return;
        }
        PromptGenStore store = PromptGenStore.get();
        String jobId = store.getJobId();
        if (jobId.isEmpty()) {
            running = false;
            stopSelf();
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/comfyui_status?job_id=" + jobId)
                .get()
                .build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.postDelayed(PromptGenService.this::onPollFailure, 1000);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    String responseBody = body == null ? "" : body.string();
                    if (!response.isSuccessful()) {
                        handler.postDelayed(PromptGenService.this::onPollFailure, 1000);
                        return;
                    }
                    JSONObject json = new JSONObject(responseBody);
                    String state = json.optString("state", "");
                    String progress = json.optString("progress", "");
                    String step = json.optString("step", "");
                    String error = json.optString("error", "");
                    pollFailureCount = 0;

                    if ("error".equals(state)) {
                        fail(error.isEmpty() ? "The workflow failed." : error);
                        return;
                    }
                    if ("cancelled".equals(state)) {
                        PromptGenStore.get().cancel();
                        finishService();
                        return;
                    }
                    if ("done".equals(state) || (state.isEmpty() && "Done".equals(progress))) {
                        fetchText(jobId);
                        return;
                    }

                    PromptGenStore.get().setProgress(step.isEmpty() ? progress : progress + " | " + step);
                    updateNotification(progress.isEmpty() ? step : progress);
                    handler.postDelayed(PromptGenService.this::pollStatus, 1000);
                } catch (JSONException | IOException e) {
                    handler.postDelayed(PromptGenService.this::onPollFailure, 1000);
                }
            }
        });
    }

    private void onPollFailure() {
        pollFailureCount++;
        boolean tooLong = (System.currentTimeMillis() - pollStartedAt) > MAX_POLL_DURATION_MS;
        if (tooLong || pollFailureCount > MAX_POLL_FAILURES) {
            fail(tooLong ? "The gateway did not finish the job in time." : "Lost contact with the gateway.");
            return;
        }
        handler.postDelayed(this::pollStatus, 1000);
    }

    /** Read the text the job produced. */
    private void fetchText(String jobId) {
        Request request = new Request.Builder()
                .url(baseUrl + "/comfyui_text?job_id=" + jobId)
                .get()
                .build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                fail("Cannot read the result: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    String responseBody = body == null ? "" : body.string();
                    if (!response.isSuccessful()) {
                        fail(describeGatewayError(response.code(), responseBody));
                        return;
                    }
                    JSONObject json = new JSONObject(responseBody);
                    String text = json.optString("text", "");
                    String error = json.optString("error", "");
                    if (text.trim().isEmpty()) {
                        fail(error.isEmpty() ? "The workflow produced no text." : error);
                        return;
                    }
                    PromptGenStore.get().finish(text);
                    finishService();
                } catch (JSONException | IOException e) {
                    fail("Unexpected answer from the gateway: " + e.getMessage());
                }
            }
        });
    }

    /** Stop the job that is running. */
    private void cancelJob() {
        // Stop polling first: a pending poll must not report a result for a
        // job the user just stopped.
        running = false;
        String jobId = PromptGenStore.get().getJobId();
        if (jobId.isEmpty()) {
            PromptGenStore.get().cancel();
            finishService();
            return;
        }
        Request request = new Request.Builder()
                .url(baseUrl + "/comfyui_cancel/" + jobId)
                .post(RequestBody.create("{}", MediaType.get("application/json; charset=utf-8")))
                .build();
        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // The job is stopped locally even when the gateway cannot be reached.
                PromptGenStore.get().cancel();
                finishService();
            }

            @Override
            public void onResponse(Call call, Response response) {
                PromptGenStore.get().cancel();
                finishService();
            }
        });
    }

    private void fail(String message) {
        PromptGenStore.get().fail(message);
        finishService();
    }

    /**
     * Turn a gateway error body into a message a user can act on.
     *
     * The gateway reports a rejected workflow as
     * {@code {"detail": "... | node_errors: {...}}"}}. The raw JSON is far too
     * noisy for a dialog, so the first node error is picked out and shown
     * instead.
     */
    static String describeGatewayError(int code, String body) {
        String detail = "";
        try {
            detail = new JSONObject(body).optString("detail", "");
        } catch (JSONException e) {
            // Not a JSON body - fall through to the raw text.
        }
        if (detail.isEmpty()) {
            detail = body == null ? "" : body.trim();
        }
        if (detail.isEmpty()) {
            return "The gateway returned " + code;
        }

        String marker = "node_errors: ";
        int index = detail.indexOf(marker);
        if (index < 0) {
            return "The gateway returned " + code + " - " + detail;
        }

        String head = detail.substring(0, index).trim();
        String nodeErrors = detail.substring(index + marker.length()).trim();
        String summary = summariseNodeErrors(nodeErrors);
        if (summary == null) {
            return "The gateway returned " + code + " - " + head;
        }
        return "The gateway returned " + code + " - " + head + ": " + summary;
    }

    /** Extract the first node error of a ComfyUI {@code node_errors} object. */
    private static String summariseNodeErrors(String nodeErrors) {
        try {
            JSONObject errors = new JSONObject(nodeErrors);
            java.util.Iterator<String> nodes = errors.keys();
            while (nodes.hasNext()) {
                JSONObject node = errors.optJSONObject(nodes.next());
                if (node == null) {
                    continue;
                }
                JSONArray list = node.optJSONArray("errors");
                if (list == null || list.length() == 0) {
                    continue;
                }
                JSONObject error = list.optJSONObject(0);
                if (error == null) {
                    continue;
                }
                String message = error.optString("message", "");
                String details = error.optString("details", "");
                String classType = node.optString("class_type", "");
                if (message.isEmpty()) {
                    message = details;
                }
                if (message.isEmpty()) {
                    continue;
                }
                if (!details.isEmpty() && !message.contains(details)) {
                    message = message + " (" + details + ")";
                }
                if (!classType.isEmpty()) {
                    message = classType + ": " + message;
                }
                return message;
            }
        } catch (JSONException e) {
            return null;
        }
        return null;
    }

    private void finishService() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        stopForeground(true);
        stopSelf();
    }

    private void startForegroundNotification(String title) {
        createChannel();
        Notification notification = buildNotification(title, "Starting...");
        startForeground(NOTIFICATION_ID, notification);
    }

    private void updateNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(PromptGenStore.get().getTitle(), text));
        }
    }

    private Notification buildNotification(String title, String text) {
        Intent open = new Intent();
        open.setClassName(this, "com.jsoft.diffusionpaint.DrawingActivity");
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent cancel = new Intent(this, PromptGenService.class);
        cancel.setAction(ACTION_CANCEL);
        PendingIntent cancelPending = PendingIntent.getForegroundService(this, 0, cancel,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_magic_pen)
                .setContentTitle(title.isEmpty() ? "Generating prompt" : title)
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(openPending)
                .addAction(0, "Stop", cancelPending)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Prompt generation",
                NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel(channel);
    }

    /** Start a request. */
    public static void start(Context context, String requestPath, String workflow, String title, int sketchId) {
        Intent intent = new Intent(context, PromptGenService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_REQUEST_PATH, requestPath);
        intent.putExtra(EXTRA_WORKFLOW, workflow);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_SKETCH_ID, sketchId);
        context.startForegroundService(intent);
    }

    /** Stop the request that is running. */
    public static void cancel(Context context) {
        Intent intent = new Intent(context, PromptGenService.class);
        intent.setAction(ACTION_CANCEL);
        context.startForegroundService(intent);
    }
}
