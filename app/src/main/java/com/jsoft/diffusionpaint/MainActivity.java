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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.CursorWindow;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.jsoft.diffusionpaint.adapter.GridViewImageAdapter;
import com.jsoft.diffusionpaint.dto.SdParam;
import com.jsoft.diffusionpaint.helper.PaintDb;
import com.jsoft.diffusionpaint.helper.SdApiHelper;
import com.jsoft.diffusionpaint.helper.SdApiResponseListener;
import com.jsoft.diffusionpaint.dto.Sketch;
import com.jsoft.diffusionpaint.helper.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
    private static int lastStyleSelection = 0;
    private static boolean updateChecked = false;
    private static final int MI_CUSTOM_MODE_BASE = UUID.randomUUID().hashCode();
    private String t_key, t_title, t_hint, t_defaultValue;
    private static final String settingFileName = "SDSketch.json";
    public static boolean isViewingSDImage = false;
    private static Menu mainMenu;
    private TextView txtVRAM;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isViewingSDImage) {
            Intent viewSdIntent = new Intent(this, ViewSdImageActivity.class);
            viewSdIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(viewSdIntent);
            finish();
            return;
        }
        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Intent drawIntent = getIntentFromImage(intent);
            if (drawIntent != null) {
                gotoDrawingActivity(drawIntent);
                finish();
                return;
            }
        }

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
        PopupMenu popupMenu = new PopupMenu(this, menuButton);
        popupMenu.getMenuInflater().inflate(R.menu.sd_setting, popupMenu.getMenu());
        mainMenu = popupMenu.getMenu();
        popupMenu.setOnMenuItemClickListener(this::menuItemClick);

        menuButton.setOnClickListener(v -> {
            popupMenu.show();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { goBack();}
        });

        isPermissionGranted();


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            try {
                Field field = CursorWindow.class.getDeclaredField("sCursorWindowSize");
                field.setAccessible(true);
                field.set(null, 100 * 1024 * 1024); //the 100MB is the new size
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        sdApiHelper = new SdApiHelper(this, this);

        String dflApiAddress = sharedPreferences.getString("dflApiAddress", "");
        if (Utils.isValidServerURL(dflApiAddress) && Sketch.comfyuiModes == null) {
            sdApiHelper.sendRequest("getComfyuiMode", dflApiAddress, "/mode_config", null, "GET");
        } else if (Sketch.comfyuiModes != null) {
            createComfyuiModeConfig();
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                int verCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;//Version Code
                sdApiHelper.sendRequest("getVersionCode", "https://sdsketch.web.app", "/version-info?v=" + verCode + "&s=comfyui", null, "GET");
                return "";
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void createComfyuiModeConfig() {
        MenuItem submenuItem = mainMenu.getItem(2).getSubMenu().getItem(0);
        if (submenuItem.hasSubMenu()) {
            SubMenu subMenu = submenuItem.getSubMenu();
            if (subMenu.size() == 0) {
                int menuID = MI_CUSTOM_MODE_BASE + 1;
                for (int i = 0; i < Sketch.comfyuiModes.length(); i++) {
                    try {
                        JSONObject modeConfig = Sketch.comfyuiModes.getJSONObject(i);
                        if (modeConfig.has("configurable") && modeConfig.getBoolean("configurable")) {
                            subMenu.add(0, menuID + i, 0, modeConfig.getString("title"));
                        }
                    } catch (JSONException ignored) {
                    }
                }
            }
        }
    }

    private boolean validateSettings() {
        if (!sdApiHelper.isValid()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.app_not_configured_title);
            builder.setMessage(R.string.app_not_configured_message);
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            builder.setPositiveButton(R.string.configure, (dialog, which) -> {
                showTextInputDialog("dflApiAddress", "ComfyUI GW Address:", "http://192.168.1.101:5000", "");
                dialog.dismiss();
            });

            AlertDialog dialog = builder.create();
            if (!isFinishing()) dialog.show();

            return false;
        }

        return true;
    }

    public void gotoDrawingActivity(int sketchID) {
        if (!validateSettings()) return;

        Intent intent = new Intent(MainActivity.this, DrawingActivity.class);
        intent.putExtra("sketchId", sketchID);
        gotoDrawingActivity(intent);
    }

    private void gotoDrawingActivity(Intent intent) {
        DrawingActivity.clearPath();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        showGrid(-1);
    }

    public Intent getIntentFromImage(Intent intent) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        String mimeType = this.getContentResolver().getType(uri);
        if (mimeType != null && mimeType.startsWith("image/")) {
            String filePath = Utils.getPathFromUri(uri, this);
            if (filePath != null) {
                Intent drawIntent = new Intent(this, DrawingActivity.class);
                drawIntent.putExtra("sketchId", -2);
                drawIntent.putExtra("bitmapPath", filePath);
                return drawIntent;
            }
        }
        return null;
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
            moveTaskToBack(true);
            //finish();
        }
    }

    private GridView initGridLayout() {
        Resources r = getResources();
        gridView = findViewById(R.id.gridview_sketch_list);
        return gridView;
    }

    public boolean menuItemClick(MenuItem item) {
        // Handle item selection
        AlertDialog.Builder builder;
        AlertDialog alert;
        switch (item.getItemId()) {
            case R.id.mi_start_comfyui:
                sdApiHelper.sendRequest("restart_Server", sharedPreferences.getString("dflApiAddress", ""), "/start_comfyui", null, "GET");
                break;
            case R.id.mi_stop_comfyui:
                sdApiHelper.sendRequest("restart_Server", sharedPreferences.getString("dflApiAddress", ""), "/stop_comfyui", null, "GET");
                break;
            case R.id.mi_comfyui_unload:
                sdApiHelper.sendRequest("restart_Server", sharedPreferences.getString("dflApiAddress", ""), "/comfyui_free", null, "GET");
                break;
            case R.id.mi_dfl_api_address:
                showTextInputDialog("dflApiAddress", "DFL API Address:", "http://192.168.1.101:7860", "");
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
                showAutoCompleteDialog();
                break;
            case R.id.mi_sd_output_dim:
                showOutputDimenDialog();
                break;
            case R.id.mi_canvas_dim:
                showTextInputDialog("canvasDim", "Drawing Canvas Maximum Size (long edge):", "", "3840");
                break;
            case R.id.mi_steps:
                showTextInputDialog("defaultSteps", "Steps:", "Integer from 1 to 150", "30");
                break;
            case R.id.mi_batch_size:
                showTextInputDialog("maxBatchSize", "Maximum Batch Size:", "Integer. Setting this value too high will lead to OOM.", "1");
                break;
            case R.id.mi_folder:
                //File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                showTextInputDialog("picFolder", "Picture folder:", "Name of folder under /Pictures", "sdSketch");
                break;
            case R.id.mi_cfg_scale:
                showTextInputDialog("defaultCfgScale", "CFG Scale:", "Decimal from 1.0 to 30.0", "7.0");
                break;
            case R.id.mi_mask_blur:
                showTextInputDialog("inpaintMaskBlur", "Inpaint Mask Blur Pixels:", "Integer from 0 to 64", "10");
                break;
            case R.id.mi_about:
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://github.com/jordenyt/stable_diffusion_sketch"));
                startActivity(intent);
                break;
            case R.id.mi_export_setting:
                boolean saveSuccess = saveSharedPreferencesToFile();
                builder = new AlertDialog.Builder(this);
                builder.setTitle(saveSuccess ? "Settings has been exported to download folder." : "Failed to export.")
                        .setPositiveButton("OK", (dialog, id) -> {
                        });
                alert = builder.create();
                alert.show();
                break;
            case R.id.mi_import_setting:
                pickJSONFile();
                break;
            default:
                int numComfyUIMode = 0;
                if (Sketch.comfyuiModes != null) {
                    numComfyUIMode = Sketch.comfyuiModes.length();
                }
                if (item.getItemId() > MI_CUSTOM_MODE_BASE && item.getItemId() <= MI_CUSTOM_MODE_BASE + numComfyUIMode) {
                    int i = item.getItemId() - MI_CUSTOM_MODE_BASE - 1;
                    try {
                        JSONObject modeConfig = Sketch.comfyuiModes.getJSONObject(i);
                        showTextInputDialog("modeComyui" + modeConfig.getString("name"), "Parameters for " + modeConfig.getString("title") +  ":", "", modeConfig.getJSONObject("default").toString());
                    } catch (JSONException ignored) {}
                }
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void showOutputDimenDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_frame_dim, null);
        builder.setView(dialogView);

        Spinner spSize = dialogView.findViewById(R.id.sd_size_selection);
        int prefSize = sharedPreferences.getInt("sdImageSize", 768);
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
        final RadioGroup rgSdMode = dialogView.findViewById(R.id.radio_sdmode);
        rgSdMode.setVisibility(View.GONE);
        List<String> acList = new ArrayList<>();
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

        Button btnCaption = dialogView.findViewById(R.id.btnCaption);
        btnCaption.setVisibility(View.GONE);

        Spinner sdMode = dialogView.findViewById(R.id.sd_mode_selection);

        List<String> filteredModes = new ArrayList<>();
        Map<String, String> txt2imgModeMap = Sketch.txt2imgModeMap();
        filteredModes.addAll(txt2imgModeMap.keySet());

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

        SeekBar sdNumGen = dialogView.findViewById(R.id.sd_num_generation);
        TextView sdNumGenVal = dialogView.findViewById(R.id.sd_num_generation_val);
        sdNumGen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sdNumGenVal.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

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
            intent.putExtra("cnMode", txt2imgModeMap.get(selectMode));
            intent.putExtra("prompt", promptText);
            intent.putExtra("negPrompt", negPromptText);
            intent.putExtra("aspectRatio", aspectRatioMap.get(selectAspectRatio));
            int numGen = sdNumGen.getProgress();
            intent.putExtra("numGen", numGen);
            gotoDrawingActivity(intent);
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

        MultiAutoCompleteTextView editText = dialogView.findViewById(R.id.edit_text);
        editText.setHint(hint);
        String defaultText = sharedPreferences.getString(key, defaultValue);
        if (key.startsWith("mode")) {
            defaultText = defaultText.replace(",", ", ");
            defaultText = defaultText.replace("  ", " ");
        }
        editText.setText(defaultText);

        if (key.startsWith("mode")) {
            ArrayAdapter<String> modeKeyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, SdParam.getModelKeyList());
            editText.setAdapter(modeKeyAdapter);
            editText.setThreshold(2);
            editText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        }

        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button buttonOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        buttonOk.setOnClickListener(v -> {
            boolean validated = true;
            String inputText = editText.getText().toString();
            if (inputText.isEmpty()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(key);
                editor.apply();
                dialog.dismiss();
                return;
            }
            inputText = inputText.replace("“", "\"");
            inputText = inputText.replace("”", "\"");
            if (key.startsWith("mode") && !isValidModeJson(inputText)) {
                validated = false;
            }
            if (validated) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(key, inputText);
                editor.apply();
                if ("dflApiAddress".equals(key)) {
                    if (Utils.isValidServerURL(inputText)) {
                        sdApiHelper.sendRequest("getComfyuiMode", inputText, "/mode_config", null, "GET");
                    }
                }
                dialog.dismiss();
            } else {
                Toast.makeText(getApplicationContext(), "Invalid JSON", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidModeJson(String json) {
        try {
            Gson gson = new Gson();
            gson.fromJson(json, SdParam.class);
        } catch (JsonSyntaxException ex) {
            return false;
        }
        return true;
    }

    private void showAutoCompleteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_autocomplete, null);
        builder.setView(dialogView);

        ListView stringListView = dialogView.findViewById(R.id.autocomplete_phrase_list);
        final MultiAutoCompleteTextView newStringInput = dialogView.findViewById(R.id.new_string_input);
        Button addButton = dialogView.findViewById(R.id.add_button);

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
        showPromptDialog();
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
                    gotoDrawingActivity(intent);
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
                    gotoDrawingActivity(intent);
                }
            });

    private void pickJSONFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        pickJSONLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> pickJSONLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData()!= null && result.getData().getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        if (Utils.isJsonUri(this, uri)) {
                            boolean success = readSharedPreferencesFromUri(this, uri);
                            if (success) {
                                Toast.makeText(this, "Settings imported successfully.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to import settings.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "Please select a .json file.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    public boolean readSharedPreferencesFromUri(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            JSONObject jsonObject = new JSONObject(stringBuilder.toString());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();

            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);
                if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                } else if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                }
            }
            editor.apply();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void isPermissionGranted() {
        if (!(checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.POST_NOTIFICATIONS};
            ActivityCompat.requestPermissions(this, permissions, 100);
        }
    }

    public boolean saveSharedPreferencesToFile() {
        try {
            JSONObject jsonObject = new JSONObject(sharedPreferences.getAll());

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, settingFileName);
            //contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            } else {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), settingFileName);
                uri = Uri.fromFile(file);
            }

            if (uri != null) {
                try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                    if (outputStream != null) {
                        outputStream.write(jsonObject.toString().getBytes());
                        outputStream.flush();
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onSdApiFailure(String requestType, String errMessage) {
        if (!"setSDModel1".equals(requestType) && !"getVersionCode".equals(requestType) && !"getVRAM".equals(requestType)) {
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
            if ("getVersionCode".equals(requestType)) {
                int appVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
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
            } else if ("getComfyuiMode".equals(requestType)) {
                JSONArray jsonArray = new JSONArray(responseBody);
                Sketch.comfyuiModes = jsonArray;
                createComfyuiModeConfig();
            } else if ("restart_Server".equals(requestType)) {
                JSONObject jsonObject = new JSONObject(responseBody);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Service Command sent.")
                        .setMessage(jsonObject.getString("message"))
                        .setPositiveButton("OK", (dialog, id) -> {
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
