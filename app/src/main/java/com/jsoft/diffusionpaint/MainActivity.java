package com.jsoft.diffusionpaint;

import static com.jsoft.diffusionpaint.dto.Sketch.ASPECT_RATIO_SQUARE;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.jsoft.diffusionpaint.adapter.GridViewImageAdapter;
import com.jsoft.diffusionpaint.helper.PaintDb;
import com.jsoft.diffusionpaint.helper.SdApiHelper;
import com.jsoft.diffusionpaint.helper.SdApiResponseListener;
import com.jsoft.diffusionpaint.dto.Sketch;
import com.jsoft.diffusionpaint.helper.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity implements SdApiResponseListener {

    private PaintDb db;
    private GridView gridView;
    private List<Sketch> sketches;
    private SharedPreferences sharedPreferences;
    private static File mImageFile;
    private SdApiHelper sdApiHelper;
    private int currentRootId = -1;
    private static int lastModeSelection = 0;
    private static int lastAspectSelection = -1;
    private static boolean updateChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (mImageFile==null) mImageFile = new File(getExternalFilesDir(null), "captured_image.jpg");
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        db = new PaintDb(this);

        gridView = initGridLayout();

        MaterialButton addSketchButton = findViewById(R.id.fab_add);
        addSketchButton.setOnClickListener(view -> gotoDrawingActivity(-1));

        MaterialButton addCameraButton = findViewById(R.id.fab_add_camera);
        addCameraButton.setOnClickListener(view -> launchCamera());

        MaterialButton addFromFile = findViewById(R.id.fab_add_file);
        addFromFile.setOnClickListener(view -> pickImage());

        MaterialButton addTxt2img = findViewById(R.id.fab_add_txt2img);
        addTxt2img.setOnClickListener(view -> addTxt2img());

        ImageButton menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, menuButton);
            popupMenu.getMenuInflater().inflate(R.menu.sd_setting, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(this::menuItemClick);
            popupMenu.show();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { goBack();}
        });

        isPermissionGranted();

        sdApiHelper = new SdApiHelper(this, this);

        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            newPaintFromImage(intent);
        }

        /*CompletableFuture.supplyAsync(() -> {
            sdApiHelper.sendRequest("getVersionCode", "https://sdsketch.web.app", "/version-info", null, "GET");
            return "";
        });*/

    }

    private void gotoDrawActivity(Intent intent) {
        finish();
        DrawingActivity.clearPath();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean validateSettings() {
        if (!sdApiHelper.isValid()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.app_not_configured_title);
            builder.setMessage(R.string.app_not_configured_message);
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            builder.setPositiveButton(R.string.configure, (dialog, which) -> {
                showServerAddressInput();
                dialog.dismiss();
            });

            AlertDialog dialog = builder.create();
            if(!isFinishing()) dialog.show();

            return false;
        }

        return true;
    }

    public void gotoDrawingActivity(int sketchID) {
        if (!validateSettings()) return;

        Intent intent = new Intent(MainActivity.this, DrawingActivity.class);
        intent.putExtra("sketchId", sketchID);
        gotoDrawActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        showGrid(-1);
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            newPaintFromImage(intent);
        }
    }

    public void newPaintFromImage(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        String mimeType = this.getContentResolver().getType(uri);
        if (mimeType != null && mimeType.startsWith("image/")) {
            String filePath = Utils.getPathFromUri(uri, this);
            if (filePath != null) {
                Intent drawIntent = new Intent(this, DrawingActivity.class);
                drawIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                drawIntent.putExtra("sketchId", -2);
                drawIntent.putExtra("bitmapPath", filePath);
                gotoDrawActivity(drawIntent);
            }
        }
    }

    public void showGrid(int rootSketchId) {

        TextView noRecentImages = findViewById(R.id.no_recent_images);

        noRecentImages.setVisibility(View.INVISIBLE);
        LinearLayout loCreateDrawing = findViewById(R.id.lo_create_drawing);
        TextView projectTitle = findViewById(R.id.textView2);
        if (rootSketchId == -1) {
            currentRootId = -1;
            loCreateDrawing.setVisibility(View.VISIBLE);
            projectTitle.setText(R.string.my_images);
            List<Sketch> dbSketchList = db.getSketchList();
            List<Sketch> showSketches = new ArrayList<>();
            Map<Integer, List<Sketch>> mapSketch = new HashMap<>();

            for (int i = 0; i < dbSketchList.size(); i++) {
                Sketch sketch = dbSketchList.get(i);
                int rootId = getRootId(dbSketchList, sketch.getId());
                mapSketch.computeIfAbsent(rootId, k -> new ArrayList<>());
                Objects.requireNonNull(mapSketch.get(rootId)).add(sketch);
            }
            List<Integer> addedId = new ArrayList<>();
            for (int i = 0; i < dbSketchList.size(); i++) {
                int sketchId = dbSketchList.get(i).getId();
                int rootId = getRootId(dbSketchList, sketchId);
                if (!addedId.contains(rootId)) {
                    List<Sketch> members = mapSketch.get(rootId);
                    assert members != null;
                    if (members.size() > 1) {
                        Sketch sketchGroup = new Sketch();
                        sketchGroup.setId(rootId);
                        sketchGroup.setImgPreview(db.getSketchPreview(members.get(0).getId()));
                        sketchGroup.setChildren(members);
                        sketchGroup.setCreateDate(members.get(0).getCreateDate());
                        sketchGroup.setPrompt(members.get(0).getPrompt());
                        showSketches.add(sketchGroup);
                    } else if (members.size() == 1) {
                        members.get(0).setImgPreview(db.getSketchPreview(members.get(0).getId()));
                        showSketches.add(members.get(0));
                    }
                    addedId.add(rootId);
                }
            }
            sketches = showSketches;
            GridViewImageAdapter adapter = new GridViewImageAdapter(this, showSketches);
            gridView.setAdapter(adapter);

            if (sketches.size() == 0)
                noRecentImages.setVisibility(View.VISIBLE);
        } else {
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                loCreateDrawing.setVisibility(View.GONE);
            } else {
                loCreateDrawing.setVisibility(View.VISIBLE);
            }
            projectTitle.setText("[PROJECT " + rootSketchId + "]");
            for (Sketch sketchGroup : sketches) {
                if (sketchGroup.getId() == rootSketchId) {
                    currentRootId = rootSketchId;
                    for (Sketch s: sketchGroup.getChildren()) {
                        if (s.getImgPreview() == null) {
                            s.setImgPreview(db.getSketchPreview(s.getId()));
                        }
                    }
                    GridViewImageAdapter adapter = new GridViewImageAdapter(this, sketchGroup.getChildren());
                    gridView.setAdapter(adapter);
                    break;
                }
            }
        }
    }

    public void deleteSketch(Sketch sketch) {
        if (sketch.getChildren() == null) {
            db.deleteSketch(sketch.getId());
        } else {
            db.deleteGroup(sketch.getChildren());
        }
        showGrid(-1);
    }

    public int getRootId(List<Sketch> sketches, int sketchId) {
        for (Sketch sketch : sketches) {
            if (sketch.getId() == sketchId) {
                if (sketch.getParentId() >= 0) {
                    int rootId = getRootId(sketches, sketch.getParentId());
                    if (rootId >= 0) {
                        return rootId;
                    } else {
                        return sketchId;
                    }
                } else if (sketch.getParentId() == -1) {
                    return sketchId;
                }
                break;
            }
        }
        return -1;
    }

    public void goBack() {
        if (currentRootId != -1) {
            showGrid(-1);
        } else {
            finish();
        }
    }

    private GridView initGridLayout() {
        Resources r = getResources();
        gridView = findViewById(R.id.gridview_sketch_list);
        return gridView;
    }

    private void showServerAddressInput() {
        showTextInputDialog("sdServerAddress", "Stable Diffusion API Server Address:", "http://192.168.1.101:7860", "");
    }

    public boolean menuItemClick(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.mi_sd_server_address:
                showServerAddressInput();
                break;
            case R.id.mi_restart_a1111:
                sdApiHelper.sendRequest("restart_Server", sharedPreferences.getString("dflApiAddress", ""), "/restart_a1111", null, "GET");
                break;
            case R.id.mi_dfl_api_address:
                showTextInputDialog("dflApiAddress", "DFL API Address:", "http://192.168.1.101:7860", "");
                break;
            case R.id.mi_dfl_model:
                showTextInputDialog("dflModel", "DFL Model:", "", "");
                break;
            case R.id.mi_prompt_prefix:
                showTextInputDialog("promptPrefix", "Prompt Prefix:", "Color drawing of ", "");
                break;
            case R.id.mi_prompt_postfix:
                showTextInputDialog("promptPostfix", "Prompt Postfix:", "colorful background", "");
                break;
            case R.id.mi_negative_prompt:
                showTextInputDialog("negativePrompt", "Negative Prompt:", "nsfw, adult", "");
                break;
            case R.id.mi_autocomplete_phrases:
                if (DrawingActivity.loraList == null) {
                    sdApiHelper.sendGetRequest("getLoras2", "/sdapi/v1/loras");
                } else {
                    showAutoCompleteDialog();
                }
                break;
            case R.id.mi_mode_txt2img:
                showTextInputDialog("modeTxt2img", "Parameters for basic txt2img:", "", Sketch.defaultJSON.get(Sketch.CN_MODE_TXT));
                break;
            case R.id.mi_mode_sdxl:
                showTextInputDialog("modeSDXL", "Parameters for SDXL txt2img:", "", Sketch.defaultJSON.get(Sketch.CN_MODE_TXT_SDXL));
                break;
            case R.id.mi_mode_sdxl_turbo:
                showTextInputDialog("modeSDXLTurbo", "Parameters for SDXL Turbo txt2img:", "", Sketch.defaultJSON.get(Sketch.CN_MODE_TXT_SDXL_TURBO));
                break;
            case R.id.mi_mode_custom1:
                showTextInputDialog("modeCustom1", "Parameters for Custom Mode 1:", "", Sketch.defaultJSON.get(Sketch.CN_MODE_CUSTOM));
                break;
            case R.id.mi_mode_custom2:
                showTextInputDialog("modeCustom2", "Parameters for Custom Mode 2:", "", Sketch.defaultJSON.get(Sketch.CN_MODE_CUSTOM));
                break;
            case R.id.mi_mode_custom3:
                showTextInputDialog("modeCustom3", "Parameters for Custom Mode 3:", "", Sketch.defaultJSON.get(Sketch.CN_MODE_CUSTOM));
                break;
            case R.id.mi_mode_custom4:
                showTextInputDialog("modeCustom4", "Parameters for Custom Mode 4:", "", Sketch.defaultJSON.get(Sketch.CN_MODE_CUSTOM));
                break;
            case R.id.mi_mode_custom5:
                showTextInputDialog("modeCustom5", "Parameters for Custom Mode 5:", "", Sketch.defaultJSON.get(Sketch.CN_MODE_CUSTOM));
                break;
            case R.id.mi_mode_custom6:
                showTextInputDialog("modeCustom6", "Parameters for Custom Mode 6", "", Sketch.defaultJSON.get(Sketch.CN_MODE_CUSTOM));
                break;
            case R.id.mi_mode_custom7:
                showTextInputDialog("modeCustom7", "Parameters for Custom Mode 7:", "", Sketch.defaultJSON.get(Sketch.CN_MODE_CUSTOM));
                break;
            case R.id.mi_mode_custom8:
                showTextInputDialog("modeCustom8", "Parameters for Custom Mode 8:", "", Sketch.defaultJSON.get(Sketch.CN_MODE_CUSTOM));
                break;
            case R.id.mi_cn_scribble:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnScribble", "/controlnet/model_list");
                break;
            case R.id.mi_cn_depth:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnDepth", "/controlnet/model_list");
                break;
            case R.id.mi_cn_pose:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnPose", "/controlnet/model_list");
                break;
            case R.id.mi_cn_canny:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnCanny", "/controlnet/model_list");
                break;
            case R.id.mi_cn_normal:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnNormal", "/controlnet/model_list");
                break;
            case R.id.mi_cn_mlsd:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnMlsd", "/controlnet/model_list");
                break;
            case R.id.mi_cn_lineart:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnLineart", "/controlnet/model_list");
                break;
            case R.id.mi_cn_softedge:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnSoftedge", "/controlnet/model_list");
                break;
            case R.id.mi_cn_seg:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnSeg", "/controlnet/model_list");
                break;
            case R.id.mi_cn_tile:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnTile", "/controlnet/model_list");
                break;
            case R.id.mi_cn_ipadapter:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnIPAdapter", "/controlnet/model_list");
                break;
            case R.id.mi_cnxl_ipadapter:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnxlIPAdapter", "/controlnet/model_list");
                break;
            case R.id.mi_cn_other1:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnOther1", "/controlnet/model_list");
                break;
            case R.id.mi_cn_other2:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnOther2", "/controlnet/model_list");
                break;
            case R.id.mi_cn_other3:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setCnOther3", "/controlnet/model_list");
                break;
            case R.id.mi_sd_output_dim:
                showOutputDimenDialog();
                break;
            case R.id.mi_canvas_dim:
                showTextInputDialog("canvasDim", "Canvas Dimension:", "", "2560");
                break;
            case R.id.mi_steps:
                showTextInputDialog("defaultSteps", "Steps:", "", "50");
                break;
            case R.id.mi_cfg_scale:
                showTextInputDialog("defaultCfgScale", "CFG Scale:", "", "7.0");
                break;
            case R.id.mi_sd_model:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setSDModel", "/sdapi/v1/sd-models");
                break;
            case R.id.mi_sd_inpaint_model:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setSDInpaintModel", "/sdapi/v1/sd-models");
                break;
            case R.id.mi_sdxl_base_model:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setSDXLBaseModel", "/sdapi/v1/sd-models");
                break;
            case R.id.mi_sdxl_turbo_model:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setSDXLTurboModel", "/sdapi/v1/sd-models");
                break;
            case R.id.mi_sd_sampler:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setSampler", "/sdapi/v1/samplers");
                break;
            case R.id.mi_sd_upscaler:
                if (!validateSettings()) break;
                sdApiHelper.sendGetRequest("setUpscaler", "/sdapi/v1/upscalers");
                break;
            case R.id.mi_about:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://github.com/jordenyt/stable_diffusion_sketch"));
                startActivity(intent);
                break;
            case R.id.mi_sd_refresh_loras:
                if (!validateSettings()) break;
                sdApiHelper.sendPostRequest("refreshLoras", "/sdapi/v1/refresh-loras", new JSONObject());
                break;
            case R.id.mi_sd_refresh_ckpt:
                if (!validateSettings()) break;
                sdApiHelper.sendPostRequest("refreshCheckpoints", "/sdapi/v1/refresh-checkpoints", new JSONObject());
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void showSpinnerDialog(JSONArray jsonArray, String jsonKey, String title, String prefKey, String prefDefault, String filter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_spinner, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.dialog_spinner_title);
        tvTitle.setText(title);

        if (jsonKey!=null) {
            List<JSONObject> jsonList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    jsonList.add(jsonArray.getJSONObject(i));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
            jsonList.sort((jsonObject1, jsonObject2) -> {
                String name1 = jsonObject1.optString(jsonKey);
                String name2 = jsonObject2.optString(jsonKey);
                return name1.compareToIgnoreCase(name2);
            });
            jsonArray = new JSONArray(jsonList);
        }

        List<String> options = new ArrayList<>();
        int selectedPosition = 0;
        try {
            String currentModel = sharedPreferences.getString(prefKey, prefDefault);
            for (int i = 0; i < jsonArray.length(); i++) {
                String modelTitle = "";
                if (jsonKey != null) {
                    JSONObject model = (JSONObject) jsonArray.get(i);
                    modelTitle = model.getString(jsonKey);
                } else {
                    modelTitle = jsonArray.getString(i);
                }
                if (modelTitle.toLowerCase().contains(filter)) {
                    if (currentModel.equals(modelTitle)) {
                        selectedPosition = options.size();
                    }
                    options.add(modelTitle);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Spinner spModel = dialogView.findViewById(R.id.dialog_spinner_options);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spModel.setAdapter(adapter);
        spModel.setSelection(selectedPosition);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String selectedModel = (String) spModel.getSelectedItem();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(prefKey,selectedModel);
            editor.apply();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        if(!isFinishing()) dialog.show();
    }

    private void showOutputDimenDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_frame_dim, null);
        builder.setView(dialogView);

        Spinner spSize = dialogView.findViewById(R.id.sd_size_selection);
        int prefSize = sharedPreferences.getInt("sdImageSize", 512);
        spSize.setSelection(prefSize == 768 ? 1 : prefSize == 1024 ? 2 : prefSize == 1280 ? 3 : 0);

        Spinner spAspect = dialogView.findViewById(R.id.sd_aspect_selection);
        String prefAspect = sharedPreferences.getString("sdImageAspect", Sketch.ASPECT_RATIO_SQUARE);
        spAspect.setSelection(prefAspect.equals(Sketch.ASPECT_RATIO_WIDE) ? 3 : prefAspect.equals(Sketch.ASPECT_RATIO_LANDSCAPE) ? 2 : prefAspect.equals(Sketch.ASPECT_RATIO_PORTRAIT) ? 1 : 0);

        builder.setPositiveButton("OK", (dialog, which) -> {
            int ipSize = (spSize.getSelectedItemPosition() == 1 ? 768
                    : spSize.getSelectedItemPosition() == 2 ? 1024
                    : spSize.getSelectedItemPosition() == 3 ? 1280
                    : 512);
            String ipAspect = (spAspect.getSelectedItemPosition() == 1 ? Sketch.ASPECT_RATIO_PORTRAIT
                    : spAspect.getSelectedItemPosition() == 2 ? Sketch.ASPECT_RATIO_LANDSCAPE
                    : spAspect.getSelectedItemPosition() == 3 ? Sketch.ASPECT_RATIO_WIDE
                    : Sketch.ASPECT_RATIO_SQUARE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("sdImageAspect",ipAspect);
            editor.putInt("sdImageSize", ipSize);
            editor.apply();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showPromptDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_prompt_mode, null);
        builder.setView(dialogView);

        final MultiAutoCompleteTextView promptTV = dialogView.findViewById(R.id.sd_prompt);
        final MultiAutoCompleteTextView negPromptTV = dialogView.findViewById(R.id.sd_negative_prompt);
        promptTV.setText(sharedPreferences.getString("txt2imgPrompt", ""));
        negPromptTV.setText(sharedPreferences.getString("txt2imgNegPrompt", ""));
        List<String> acList = new ArrayList<>();
        if (DrawingActivity.loraList != null) {
            acList.addAll(DrawingActivity.loraList);
        }
        String autoCompletePhrases = sharedPreferences.getString("autoCompletePhrases", "[]");
        try {
            JSONArray jsonArray = new JSONArray(autoCompletePhrases);
            for (int i = 0; i < jsonArray.length(); i++) {
                acList.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        ArrayAdapter<String> loraAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, acList);
        promptTV.setAdapter(loraAdapter);
        promptTV.setThreshold(1);
        promptTV.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());


        Button btnInterrogate = dialogView.findViewById(R.id.btnInterrogate);
        btnInterrogate.setVisibility(View.GONE);

        Spinner sdMode = dialogView.findViewById(R.id.sd_mode_selection);

        List<String> filteredModes = new ArrayList<>();
        filteredModes.addAll(Sketch.txt2imgModeMap.keySet());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filteredModes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sdMode.setAdapter(adapter);
        sdMode.setSelection(lastModeSelection);

        Spinner sdAspectRatio = dialogView.findViewById(R.id.sd_aspect_ratio);
        Map<String, String> aspectRatioMap = new LinkedHashMap<>();
        aspectRatioMap.put("Square (1:1)",Sketch.ASPECT_RATIO_SQUARE);
        aspectRatioMap.put("Portrait (3:4)",Sketch.ASPECT_RATIO_PORTRAIT);
        aspectRatioMap.put("Landscape (4:3)",Sketch.ASPECT_RATIO_LANDSCAPE);
        aspectRatioMap.put("Wide Landscape (16:9)",Sketch.ASPECT_RATIO_WIDE);
        List<String> aspectRatioList = new ArrayList<>(aspectRatioMap.keySet());
        ArrayAdapter<String> aspectRatioAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, aspectRatioList);
        aspectRatioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sdAspectRatio.setAdapter(aspectRatioAdapter);

        if (lastAspectSelection >= 0) {
            sdAspectRatio.setSelection(lastAspectSelection);
        } else {
            String defaultAspectRatio = sharedPreferences.getString("sdImageAspect", ASPECT_RATIO_SQUARE);
            for (int i = 0; i < aspectRatioMap.size(); i++) {
                String aspectRatioDesc = sdAspectRatio.getItemAtPosition(i).toString();
                if (Objects.equals(aspectRatioMap.get(aspectRatioDesc), defaultAspectRatio)) {
                    sdAspectRatio.setSelection(i);
                    break;
                }
            }
        }

        Spinner sdNumGen = dialogView.findViewById(R.id.sd_num_generation);
        List<String> sdNumGenList = new ArrayList<>();
        for (int i=1;i<=5;i++) {
            sdNumGenList.add(String.valueOf(i));
        }
        ArrayAdapter<String> sdNumGenAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sdNumGenList);
        sdNumGenAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sdNumGen.setAdapter(sdNumGenAdapter);
        sdNumGen.setSelection(0);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String promptText = promptTV.getText().toString();
            String negPromptText = negPromptTV.getText().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("txt2imgPrompt",promptText);
            editor.putString("txt2imgNegPrompt",negPromptText);
            editor.apply();

            String selectMode = sdMode.getSelectedItem().toString();
            lastModeSelection = sdMode.getSelectedItemPosition();
            String selectAspectRatio = sdAspectRatio.getSelectedItem().toString();
            lastAspectSelection = sdAspectRatio.getSelectedItemPosition();
            Intent intent = new Intent(MainActivity.this, DrawingActivity.class);
            intent.putExtra("sketchId", -3);
            intent.putExtra("cnMode", Sketch.txt2imgModeMap.get(selectMode));
            intent.putExtra("prompt", promptText);
            intent.putExtra("negPrompt", negPromptText);
            intent.putExtra("aspectRatio", aspectRatioMap.get(selectAspectRatio));
            int numGen = sdNumGen.getSelectedItemPosition() + 1;
            intent.putExtra("numGen", numGen);
            gotoDrawActivity(intent);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showTextInputDialog(String key, String title, String hint, String defaultValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_textbox, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.text_title);
        tvTitle.setText(title);

        EditText editText = dialogView.findViewById(R.id.edit_text);
        editText.setHint(hint);
        editText.setText(sharedPreferences.getString(key, defaultValue));

        builder.setPositiveButton("OK", (dialog, which) -> {
            String inputText = editText.getText().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key,inputText);
            editor.apply();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showAutoCompleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_autocomplete, null);
        builder.setView(dialogView);

        ListView stringListView = dialogView.findViewById(R.id.autocomplete_phrase_list);
        final MultiAutoCompleteTextView newStringInput = dialogView.findViewById(R.id.new_string_input);
        Button addButton = dialogView.findViewById(R.id.add_button);

        if (DrawingActivity.loraList != null) {
            ArrayAdapter<String> loraAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, DrawingActivity.loraList);
            newStringInput.setAdapter(loraAdapter);
            newStringInput.setThreshold(1);
            newStringInput.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        }

        List<String> stringList;
        String listString = sharedPreferences.getString("autoCompletePhrases", "[]");

        try {
            JSONArray jsonArray = new JSONArray(listString);
            stringList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                stringList.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        ArrayAdapter<String> stringAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, stringList);
        stringListView.setAdapter(stringAdapter);

        stringListView.setOnItemLongClickListener((parent, view, position, id) -> {
            stringList.remove(position);
            stringListView.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, stringList));
            return true;
        });

        addButton.setOnClickListener(view -> {
            String newString = newStringInput.getText().toString();
            if (!TextUtils.isEmpty(newString)) {
                stringList.add(newString);
                stringListView.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, stringList));
                newStringInput.setText("");
            }
        });

        builder.setPositiveButton("Save List", (dialog, which) -> {
            Gson gson2 = new Gson();
            String listString2 = gson2.toJson(stringList);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("autoCompletePhrases", listString2);
            editor.apply();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

    }



    private void addTxt2img() {
        if (!validateSettings()) return;

        if (DrawingActivity.loraList == null) {
            sdApiHelper.sendGetRequest("getLoras", "/sdapi/v1/loras");
        } else {
            showPromptDialog();
        }
    }

    private void launchCamera() {
        if (!validateSettings()) return;

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            Uri imageUri = FileProvider.getUriForFile(this, "com.jsoft.diffusionpaint.fileprovider", mImageFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            cameraResultLauncher.launch(intent);
        }
    }

    private void pickImage() {
        if (!validateSettings()) return;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData()!= null && result.getData().getData() != null) {
                    Uri uri = result.getData().getData();
                    String mimeType = this.getContentResolver().getType(uri);
                    String filePath = null;
                    if (mimeType != null && mimeType.startsWith("image/")) {
                        filePath = Utils.getPathFromUri(uri, this);
                    }
                    Intent intent = new Intent(MainActivity.this, DrawingActivity.class);
                    intent.putExtra("sketchId", -2);
                    intent.putExtra("bitmapPath", filePath);
                    gotoDrawActivity(intent);
                }
            });

    ActivityResultLauncher<Intent> cameraResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (ActivityResult result) -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    //Intent data = result.getData();
                    Intent intent = new Intent(MainActivity.this, DrawingActivity.class);
                    intent.putExtra("sketchId", -2);
                    intent.putExtra("bitmapPath", mImageFile.getAbsolutePath());
                    gotoDrawActivity(intent);
                }
            });


    public void isPermissionGranted() {
        if (!(checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, 100);
        }
    }

    @Override
    public void onSdApiFailure(String requestType, String errMessage) {
        if (!"setSDModel1".equals(requestType) && !"getVersionCode".equals(requestType)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Call Stable Diffusion Web UI API failed. (" + requestType + ")")
                    .setMessage(errMessage)
                    .setPositiveButton("OK", (dialog, id) -> {
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    public void onSdApiResponse(String requestType, String responseBody) {
        try {
            if ("setSDModel".equals(requestType)) {
                showSpinnerDialog(new JSONArray(responseBody), "title", "Stable Diffusion Model", "sdModelCheckpoint", "", "");
            } else if ("setSDInpaintModel".equals(requestType)) {
                showSpinnerDialog(new JSONArray(responseBody), "title", "SD Inpaint Model", "sdInpaintModel", "", "inpainting.");
            } else if ("setSDXLBaseModel".equals(requestType)) {
                showSpinnerDialog(new JSONArray(responseBody), "title", "SDXL Base Model", "sdxlBaseModel", "", "");
            } else if ("setSDXLTurboModel".equals(requestType)) {
                showSpinnerDialog(new JSONArray(responseBody), "title", "SDXL Turbo Model", "sdxlTurboModel", "", "");
            } else if ("setSampler".equals(requestType)) {
                showSpinnerDialog(new JSONArray(responseBody), "name", "SD Sampling Method", "sdSampler", "Euler a", "");
            } else if ("setUpscaler".equals(requestType)) {
                showSpinnerDialog(new JSONArray(responseBody), "name", "Upscaler", "sdUpscaler", "R-ESRGAN General 4xV3", "");
            } else if ("setCnScribble".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet Scribble Model", "cnScribbleModel", "control_v11p_sd15_scribble [d4ba51ff]", "scribble");
            } else if ("setCnDepth".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet Depth Model", "cnDepthModel", "control_v11f1p_sd15_depth [cfd03158]", "depth");
            } else if ("setCnPose".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet Pose Model", "cnPoseModel", "control_v11p_sd15_openpose [cab727d4]", "pose");
            } else if ("setCnCanny".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet Canny Model", "cnCannyModel", "control_v11p_sd15_canny [d14c016b]", "canny");
            } else if ("setCnTile".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet Tile Model", "cnTileModel", "control_v11f1e_sd15_tile [a371b31b]", "tile");
            } else if ("setCnNormal".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet Normal Model", "cnNormalModel", "control_v11p_sd15_normalbae [316696f1]", "normal");
            } else if ("setCnMlsd".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet MLSD Model", "cnMlsdModel", "control_v11p_sd15_mlsd [aca30ff0]", "mlsd");
            } else if ("setCnLineart".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet Line Art Model", "cnLineartModel", "control_v11p_sd15s2_lineart_anime [3825e83e]", "lineart");
            } else if ("setCnSoftedge".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet Soft Edge Model", "cnSoftedgeModel", "control_v11p_sd15_softedge [a8575a2a]", "softedge");
            } else if ("setCnSeg".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet Seg Model", "cnSegModel", "control_v11p_sd15_seg [e1f51eb9]", "seg");
            } else if ("setCnIPAdapter".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet IP Adapter Model", "cnIPAdapterModel", "", "ip-adapter");
            } else if ("setCnxlIPAdapter".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet IP Adapter SDXL Model", "cnxlIPAdapterModel", "", "ip-adapter");
            } else if ("setCnOther1".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "Other ControlNet Model 1", "cnOther1Model", "", "");
            } else if ("setCnOther2".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "Other ControlNet Model 2", "cnOther2Model", "", "");
            } else if ("setCnOther3".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "Other ControlNet Model 3", "cnOther3Model", "", "");
            } else if ("getLoras".equals(requestType)) {
                DrawingActivity.loraList = sdApiHelper.getLoras(responseBody);
                showPromptDialog();
            } else if ("getLoras2".equals(requestType)) {
                DrawingActivity.loraList = sdApiHelper.getLoras(responseBody);
                showAutoCompleteDialog();
            } else if ("restart_Server".equals(requestType)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Restart Command sent.")
                        .setMessage("Please wait for a minute.")
                        .setPositiveButton("OK", (dialog, id) -> {
                        });
                AlertDialog alert = builder.create();
                alert.show();
            } else if ("refreshLoras".equals(requestType)) {
                DrawingActivity.loraList = null;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Refresh LORAs Command sent.")
                        .setPositiveButton("OK", (dialog, id) -> {
                        });
                AlertDialog alert = builder.create();
                alert.show();
            } else if ("refreshCheckpoints".equals(requestType)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Refresh Checkpoints Command sent.")
                        .setPositiveButton("OK", (dialog, id) -> {
                        });
                AlertDialog alert = builder.create();
                alert.show();
            } else if ("getVersionCode".equals(requestType)) {
                int appVersionCode = BuildConfig.VERSION_CODE;
                JSONObject jsonObject = new JSONObject(responseBody);
                int latestVersionCode = jsonObject.getInt("versionCode");
                String latestVersionName = jsonObject.getString("versionName");

                if (latestVersionCode > appVersionCode && !updateChecked) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("New updates found.")
                            .setMessage("A new version " + latestVersionName + " has been released.  Do you want to get the new APK now?")
                            .setPositiveButton("OK", (dialog, id) -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse("https://github.com/jordenyt/stable_diffusion_sketch/releases/latest"));
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", (dialog, id) -> {

                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                updateChecked = true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
