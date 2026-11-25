package com.jsoft.diffusionpaint.dto;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A gateway workflow that turns images (and optionally text) into text.
 *
 * The list of tools is not hard coded: it is read from the gateway's
 * /api/workflows endpoint, so a new text workflow on the server shows up in
 * the app without an app update.
 */
public class PromptTool {

    /** Payload keys the server fills in itself - the client must not send them. */
    private static final String[] SERVER_KEYS = {"seed", "task", "system_prompt"};

    /** Payload keys that carry the user's own words. */
    private static final String[] TEXT_KEYS = {"prompt", "instruction", "instructions", "text", "query", "positive"};

    /** The keys a workflow may take the background image as. */
    private static final String[] IMAGE1_KEYS = {"image1", "first_image", "background"};
    /** The keys a workflow may take the reference image as. */
    private static final String[] IMAGE2_KEYS = {"image2", "second_image", "reference"};
    /** The keys a workflow may take a third image as. */
    private static final String[] IMAGE3_KEYS = {"image3", "third_image"};
    /** The keys a workflow may take the positive prompt as. */
    private static final String[] PROMPT_KEYS = {"prompt"};

    /** The workflow key on the gateway, e.g. "caption_api". */
    public String workflow = "";
    /** A human readable name. */
    public String title = "";
    /** What the workflow does, as described by the gateway. */
    public String description = "";
    /** Payload keys that take an image, in the order the workflow expects them. */
    public final List<String> imageKeys = new ArrayList<>();
    /** Payload keys that take the user's text. */
    public final List<String> textKeys = new ArrayList<>();
    /** True when the workflow works without any image. */
    public boolean optionalImage = true;

    /** The key the workflow takes the background image as ("image1"). */
    public String image1Key = "";
    /** The key the workflow takes the reference image as ("image2"). */
    public String image2Key = "";
    /** The key the workflow takes a third image as ("image3"). */
    public String image3Key = "";
    /** The key the workflow takes the positive prompt as ("prompt"). */
    public String promptKey = "";

    public static PromptTool fromJson(String name, JSONObject config) {
        PromptTool tool = new PromptTool();
        tool.workflow = name;
        String description = config.optString("descriptions", "");
        tool.description = description;
        tool.title = description.isEmpty() ? name.replace("_api", "") : description;

        JSONObject inputs = config.optJSONObject("inputs");
        JSONObject fields = config.optJSONObject("fields");

        if (inputs != null) {
            Iterator<String> keys = inputs.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject spec = inputs.optJSONObject(key);
                String type = spec == null ? "text" : spec.optString("type", "text");
                if ("image".equals(type)) {
                    tool.imageKeys.add(key);
                    if (spec != null && !spec.optBoolean("optional", false)) {
                        tool.optionalImage = false;
                    }
                } else {
                    tool.textKeys.add(key);
                }
            }
        }

        if (fields != null) {
            Iterator<String> keys = fields.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (inputs != null && inputs.has(key)) {
                    continue;
                }
                if (isServerKey(key) || contains(TEXT_KEYS, key)) {
                    if (!isServerKey(key) && !tool.textKeys.contains(key)) {
                        tool.textKeys.add(key);
                    }
                }
            }
        }

        // The app fills the well known keys by itself: the background image,
        // the reference image and the positive prompt. A workflow only gets
        // what it declares in its "fields".
        tool.image1Key = findKey(fields, IMAGE1_KEYS);
        tool.image2Key = findKey(fields, IMAGE2_KEYS);
        tool.promptKey = findKey(fields, PROMPT_KEYS);
        if (tool.image1Key.isEmpty()) {
            tool.image1Key = findKey(inputs, IMAGE1_KEYS);
        }
        if (tool.image2Key.isEmpty()) {
            tool.image2Key = findKey(inputs, IMAGE2_KEYS);
        }
        tool.image3Key = findKey(fields, IMAGE3_KEYS);
        if (tool.image3Key.isEmpty()) {
            tool.image3Key = findKey(inputs, IMAGE3_KEYS);
        }
        if (tool.promptKey.isEmpty()) {
            tool.promptKey = findKey(inputs, PROMPT_KEYS);
        }

        return tool;
    }

    /** The first of the given keys the workflow declares, empty when it has none. */
    private static String findKey(JSONObject object, String[] keys) {
        if (object == null) {
            return "";
        }
        for (String key : keys) {
            if (object.has(key)) {
                return key;
            }
        }
        return "";
    }

    /**
     * The workflows that can run with the images the app has.
     *
     * @param tools        every text producing workflow of the gateway
     * @param hasBackground true when a background image is available
     * @param hasReference  true when a reference image is available
     */
    public static List<PromptTool> usableFor(List<PromptTool> tools, boolean hasBackground, boolean hasReference) {
        List<PromptTool> usable = new ArrayList<>();
        for (PromptTool tool : tools) {
            // A workflow that wants an image the app does not have cannot run.
            if (!hasBackground && !tool.image1Key.isEmpty()) {
                continue;
            }
            if (!hasReference && !tool.image2Key.isEmpty()) {
                continue;
            }
            // There is no third image to give.
            if (!tool.image3Key.isEmpty()) {
                continue;
            }
            usable.add(tool);
        }
        return usable;
    }

    /**
     * Every text producing workflow of the gateway.
     *
     * @param all the body of GET /api/workflows
     */
    public static List<PromptTool> parseWorkflows(JSONObject all) {
        List<PromptTool> tools = new ArrayList<>();
        if (all == null) {
            return tools;
        }
        Iterator<String> keys = all.keys();
        while (keys.hasNext()) {
            String name = keys.next();
            JSONObject config = all.optJSONObject(name);
            if (config == null) {
                continue;
            }
            if (!config.optBoolean("produces_text", false)) {
                continue;
            }
            tools.add(fromJson(name, config));
        }
        return tools;
    }

    private static boolean isServerKey(String key) {
        return contains(SERVER_KEYS, key);
    }

    private static boolean contains(String[] values, String key) {
        for (String value : values) {
            if (value.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** The label shown in the tool picker. */
    public String label() {
        return description.isEmpty() ? workflow : description;
    }
}
