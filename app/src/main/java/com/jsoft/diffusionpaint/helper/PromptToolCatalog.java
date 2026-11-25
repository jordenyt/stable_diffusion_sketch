package com.jsoft.diffusionpaint.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.jsoft.diffusionpaint.dto.PromptTool;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The list of text generating workflows the gateway offers.
 *
 * The list comes from GET /api/workflows, so it follows whatever is configured
 * on the server. The last successful answer is cached on the device, so the
 * tool picker also works when the gateway cannot be reached.
 */
public class PromptToolCatalog {

    public interface Callback {
        void onTools(List<PromptTool> tools);
        void onError(String message);
    }

    private static final String PREFS = "PromptGen";
    private static final String KEY_TOOLS = "toolCatalog";

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static List<PromptTool> cached;

    /** The cached tools, or an empty list when nothing was cached yet. */
    public static synchronized List<PromptTool> getCached(Context context) {
        if (cached != null) {
            return cached;
        }
        cached = readCache(context);
        return cached;
    }

    /**
     * Fetch the tool list.
     *
     * The server is always asked, so a workflow that was added, changed or
     * removed on the gateway shows up right away. The cached list is only used
     * when the gateway cannot be reached.
     */
    public static void load(Context context, String baseUrl, Callback callback) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            if (callback != null) {
                callback.onError("No gateway address configured.");
            }
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrl + "/api/workflows")
                .get()
                .build();

        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                List<PromptTool> fallback = getCached(context);
                MAIN.post(() -> {
                    if (callback == null) {
                        return;
                    }
                    if (fallback.isEmpty()) {
                        callback.onError("Cannot reach the gateway: " + e.getMessage());
                    } else {
                        callback.onTools(fallback);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    String payload = body == null ? "" : body.string();
                    if (!response.isSuccessful()) {
                        MAIN.post(() -> {
                            if (callback != null) {
                                callback.onError("Gateway returned " + response.code());
                            }
                        });
                        return;
                    }
                    JSONObject json = new JSONObject(payload);
                    List<PromptTool> parsed = PromptTool.parseWorkflows(json);
                    if (parsed.isEmpty()) {
                        MAIN.post(() -> {
                            if (callback != null) {
                                callback.onError("The gateway offers no text generating workflow.");
                            }
                        });
                        return;
                    }
                    writeCache(context, json);
                    synchronized (PromptToolCatalog.class) {
                        cached = parsed;
                    }
                    MAIN.post(() -> {
                        if (callback != null) {
                            callback.onTools(parsed);
                        }
                    });
                } catch (JSONException | IOException e) {
                    MAIN.post(() -> {
                        if (callback != null) {
                            callback.onError("Cannot read the workflow list: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    private static List<PromptTool> readCache(Context context) {
        List<PromptTool> tools = new ArrayList<>();
        try {
            String payload = prefs(context).getString(KEY_TOOLS, "");
            if (payload == null || payload.isEmpty()) {
                return tools;
            }
            tools.addAll(PromptTool.parseWorkflows(new JSONObject(payload)));
        } catch (JSONException ignored) {
        }
        return tools;
    }

    private static void writeCache(Context context, JSONObject json) {
        prefs(context).edit().putString(KEY_TOOLS, json.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
