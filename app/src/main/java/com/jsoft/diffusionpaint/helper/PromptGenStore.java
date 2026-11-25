package com.jsoft.diffusionpaint.helper;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The state of the prompt generation the user started.
 *
 * The state lives outside the activity: the request keeps running while the
 * app is in the background and the result is still there when the user comes
 * back. It is written to the device as well, so a result that arrives while
 * the process was killed is not lost.
 */
public class PromptGenStore {

    public static final String STATE_IDLE = "idle";
    public static final String STATE_RUNNING = "running";
    public static final String STATE_DONE = "done";
    public static final String STATE_ERROR = "error";
    public static final String STATE_CANCELLED = "cancelled";

    public interface Listener {
        void onPromptGenChanged(PromptGenStore store);
    }

    private static final String PREFS = "PromptGen";
    private static final String KEY_STATE = "state";
    private static final String KEY_WORKFLOW = "workflow";
    private static final String KEY_TITLE = "title";
    private static final String KEY_JOB_ID = "jobId";
    private static final String KEY_TEXT = "text";
    private static final String KEY_ERROR = "error";
    private static final String KEY_PROGRESS = "progress";
    private static final String KEY_REQUEST_PATH = "requestPath";
    private static final String KEY_SKETCH_ID = "sketchId";
    private static final String KEY_RESULT_TAKEN = "resultTaken";

    private static final PromptGenStore INSTANCE = new PromptGenStore();

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private String state = STATE_IDLE;
    private String workflow = "";
    private String title = "";
    private String jobId = "";
    private String text = "";
    private String error = "";
    private String progress = "";
    private String requestPath = "";
    private int sketchId = -1;
    private boolean resultTaken = false;

    private PromptGenStore() {
    }

    public static PromptGenStore get() {
        return INSTANCE;
    }

    public synchronized String getState() {
        return state;
    }

    public synchronized String getWorkflow() {
        return workflow;
    }

    public synchronized String getTitle() {
        return title;
    }

    public synchronized String getJobId() {
        return jobId;
    }

    public synchronized String getText() {
        return text;
    }

    public synchronized String getError() {
        return error;
    }

    public synchronized String getProgress() {
        return progress;
    }

    public synchronized String getRequestPath() {
        return requestPath;
    }

    public synchronized int getSketchId() {
        return sketchId;
    }

    public synchronized boolean isRunning() {
        return STATE_RUNNING.equals(state);
    }

    /** True when there is a result nobody has applied yet. */
    public synchronized boolean hasResult() {
        return STATE_DONE.equals(state) && !resultTaken && !text.isEmpty();
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /** A new request was started. */
    public synchronized void begin(String workflow, String title, String requestPath, int sketchId) {
        this.state = STATE_RUNNING;
        this.workflow = workflow == null ? "" : workflow;
        this.title = title == null ? "" : title;
        this.requestPath = requestPath == null ? "" : requestPath;
        this.sketchId = sketchId;
        this.jobId = "";
        this.text = "";
        this.error = "";
        this.progress = "Queued";
        this.resultTaken = false;
        save();
        notifyChanged();
    }

    public synchronized void setJobId(String jobId) {
        this.jobId = jobId == null ? "" : jobId;
        save();
        notifyChanged();
    }

    public synchronized void setProgress(String progress) {
        this.progress = progress == null ? "" : progress;
        save();
        notifyChanged();
    }

    /** The request finished and produced text. */
    public synchronized void finish(String text) {
        this.state = STATE_DONE;
        this.text = text == null ? "" : text;
        this.error = "";
        this.progress = "Done";
        this.resultTaken = false;
        save();
        notifyChanged();
    }

    /** The request failed. */
    public synchronized void fail(String error) {
        this.state = STATE_ERROR;
        this.error = error == null ? "Unknown error" : error;
        this.progress = "Done";
        save();
        notifyChanged();
    }

    /** The user stopped the request. */
    public synchronized void cancel() {
        this.state = STATE_CANCELLED;
        this.progress = "Cancelled";
        save();
        notifyChanged();
    }

    /** Forget everything - used when a new request starts. */
    public synchronized void clear() {
        this.state = STATE_IDLE;
        this.workflow = "";
        this.title = "";
        this.jobId = "";
        this.text = "";
        this.error = "";
        this.progress = "";
        this.requestPath = "";
        this.sketchId = -1;
        this.resultTaken = false;
        save();
        notifyChanged();
    }

    /**
     * The generated text, marked as taken.
     *
     * Returns null when there is no pending result.
     */
    public synchronized String takeResult() {
        if (!hasResult()) {
            return null;
        }
        resultTaken = true;
        save();
        return text;
    }

    /** Read the state that was stored on the device. */
    public synchronized void restore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String payload = prefs.getString(KEY_STATE, "");
        if (payload == null || payload.isEmpty()) {
            return;
        }
        try {
            JSONObject json = new JSONObject(payload);
            state = json.optString(KEY_STATE, STATE_IDLE);
            workflow = json.optString(KEY_WORKFLOW, "");
            title = json.optString(KEY_TITLE, "");
            jobId = json.optString(KEY_JOB_ID, "");
            text = json.optString(KEY_TEXT, "");
            error = json.optString(KEY_ERROR, "");
            progress = json.optString(KEY_PROGRESS, "");
            requestPath = json.optString(KEY_REQUEST_PATH, "");
            sketchId = json.optInt(KEY_SKETCH_ID, -1);
            resultTaken = json.optBoolean(KEY_RESULT_TAKEN, false);
        } catch (JSONException ignored) {
            return;
        }
        notifyChanged();
    }

    private void save() {
        Context context = appContext;
        if (context == null) {
            return;
        }
        JSONObject json = new JSONObject();
        try {
            json.put(KEY_STATE, state);
            json.put(KEY_WORKFLOW, workflow);
            json.put(KEY_TITLE, title);
            json.put(KEY_JOB_ID, jobId);
            json.put(KEY_TEXT, text);
            json.put(KEY_ERROR, error);
            json.put(KEY_PROGRESS, progress);
            json.put(KEY_REQUEST_PATH, requestPath);
            json.put(KEY_SKETCH_ID, sketchId);
            json.put(KEY_RESULT_TAKEN, resultTaken);
        } catch (JSONException ignored) {
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_STATE, json.toString()).apply();
    }

    private void notifyChanged() {
        for (Listener listener : listeners) {
            listener.onPromptGenChanged(this);
        }
    }

    /** The application context, needed to write the state to the device. */
    private static Context appContext;

    public static void setAppContext(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }
}
