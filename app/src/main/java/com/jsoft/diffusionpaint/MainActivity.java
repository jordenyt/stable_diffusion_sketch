package com.jsoft.diffusionpaint;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jsoft.diffusionpaint.adapter.GridViewImageAdapter;
import com.jsoft.diffusionpaint.dto.AppConstant;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SdApiResponseListener {

    private PaintDb db;
    private GridView gridView;
    private Utils utils;
    private List<Sketch> sketches;
    private SharedPreferences sharedPreferences;
    private static File mImageFile;
    private SdApiHelper sdApiHelper;
    private int currentRootId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (mImageFile==null) mImageFile = new File(getExternalFilesDir(null), "captured_image.jpg");
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        db = new PaintDb(this);
        utils = new Utils(this);

        gridView = initGridLayout();

        FloatingActionButton addSketchButton = findViewById(R.id.fab_add);
        addSketchButton.setOnClickListener(view -> gotoDrawingActivity(-1));

        FloatingActionButton addCameraButton = findViewById(R.id.fab_add_camera);
        addCameraButton.setOnClickListener(view -> launchCamera());

        FloatingActionButton addTxt2img = findViewById(R.id.fab_add_txt2img);
        addTxt2img.setOnClickListener(v -> {
            if (DrawingActivity.loraList == null) {
                sdApiHelper.sendGetRequest("getLoras", "/sdapi/v1/loras");
            } else {
                showPromptDialog();
            }
        });

        isPermissionGranted();

        sdApiHelper = new SdApiHelper(this, this);

        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Utils.newPaintFromImage(intent, this, drawingActivityResultLauncher);
        }
    }

    ActivityResultLauncher<Intent> drawingActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {});

    public void gotoDrawingActivity(int sketchID) {
        Intent intent = new Intent(MainActivity.this, DrawingActivity.class);
        intent.putExtra("sketchId", sketchID);
        drawingActivityResultLauncher.launch(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        showGrid(-1);
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Utils.newPaintFromImage(intent, this, drawingActivityResultLauncher);
        }
    }

    public void showGrid(int rootSketchId) {
        if (rootSketchId == -1) {
            currentRootId = -1;
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
                        sketchGroup.setImgPreview(members.get(0).getImgPreview());
                        sketchGroup.setChildren(members);
                        sketchGroup.setCreateDate(members.get(0).getCreateDate());
                        sketchGroup.setPrompt(members.get(0).getPrompt());
                        showSketches.add(sketchGroup);
                    } else if (members.size() == 1) {
                        showSketches.add(members.get(0));
                    }
                    addedId.add(rootId);
                }
            }
            sketches = showSketches;
            GridViewImageAdapter adapter = new GridViewImageAdapter(this, showSketches);
            gridView.setAdapter(adapter);
        } else {
            for (Sketch sketchGroup : sketches) {
                if (sketchGroup.getId() == rootSketchId) {
                    currentRootId = rootSketchId;
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

    @Override
    public void onBackPressed() {
        if (currentRootId != -1) {
            showGrid(-1);
        } else {
            finish();
        }
    }

    private GridView initGridLayout() {
        Resources r = getResources();
        gridView = findViewById(R.id.gridview_sketch_list);
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                AppConstant.GRID_PADDING, r.getDisplayMetrics());

        int numColumns = 3;
        int columnWidth = (int) ((utils.getScreenWidth() - ((numColumns + 1) * padding)) / numColumns);

        gridView.setNumColumns(numColumns);
        gridView.setColumnWidth(columnWidth);
        gridView.setStretchMode(GridView.NO_STRETCH);
        gridView.setPadding((int) padding, (int) padding, (int) padding,
                (int) padding);
        gridView.setHorizontalSpacing((int) padding);
        gridView.setVerticalSpacing((int) padding);

        return gridView;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sd_setting, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.mi_sd_server_address:
                showTextInputDialog("sdServerAddress", "Stable Diffusion API Server Address:", "http://192.168.1.101:7860", "");
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
            case R.id.mi_mode_txt2img:
                showTextInputDialog("modeTxt2img", "Parameters for basic txt2img:", "", "{\"type\":\"txt2img\",\"steps\":40,\"cfgScale\":7.0}");
                break;
            case R.id.mi_mode_custom1:
                showTextInputDialog("modeCustom1", "Parameters for Custom Mode 1:", "", "{\"type\":\"inpaint\",\"steps\":40,\"denoise\":0.8,\"cfgScale\":7.0,\"baseImage\":\"sketch\",\"inpaintFill\":1,\"cnInputImage\":\"background\",\"cnModelKey\":\"cnDepthModel\",\"cnModule\":\"depth\",\"cnWeight\":1.0}");
                break;
            case R.id.mi_mode_custom2:
                showTextInputDialog("modeCustom2", "Parameters for Custom Mode 2:", "", "{\"type\":\"img2img\",\"steps\":40,\"denoise\":0.8,\"cfgScale\":7.0,\"baseImage\":\"sketch\",\"inpaintFill\":1,\"cnInputImage\":\"background\",\"cnModelKey\":\"cnPoseModel\",\"cnModule\":\"openpose_full\",\"cnWeight\":1.0}");
                break;
            case R.id.mi_mode_custom3:
                showTextInputDialog("modeCustom3", "Parameters for Custom Mode 3:", "", "{\"type\":\"inpaint\",\"steps\":40,\"denoise\":0.8,\"cfgScale\":7.0,\"baseImage\":\"sketch\",\"inpaintFill\":1,\"cnInputImage\":\"background\",\"cnModelKey\":\"cnDepthModel\",\"cnModule\":\"depth\",\"cnWeight\":1.0}");
                break;
            case R.id.mi_mode_custom4:
                showTextInputDialog("modeCustom4", "Parameters for Custom Mode 4:", "", "{\"type\":\"img2img\",\"steps\":40,\"denoise\":0.8,\"cfgScale\":7.0,\"baseImage\":\"sketch\",\"inpaintFill\":1,\"cnInputImage\":\"background\",\"cnModelKey\":\"cnPoseModel\",\"cnModule\":\"openpose_full\",\"cnWeight\":1.0}");
                break;
            case R.id.mi_mode_custom5:
                showTextInputDialog("modeCustom5", "Parameters for Custom Mode 5:", "", "{\"type\":\"img2img\",\"steps\":40,\"denoise\":0.8,\"cfgScale\":7.0,\"baseImage\":\"sketch\",\"inpaintFill\":1,\"cnInputImage\":\"background\",\"cnModelKey\":\"cnPoseModel\",\"cnModule\":\"openpose_full\",\"cnWeight\":1.0}");
                break;
            case R.id.mi_cn_scribble:
                sdApiHelper.sendGetRequest("setCnScribble", "/controlnet/model_list");
                break;
            case R.id.mi_cn_depth:
                sdApiHelper.sendGetRequest("setCnDepth", "/controlnet/model_list");
                break;
            case R.id.mi_cn_pose:
                sdApiHelper.sendGetRequest("setCnPose", "/controlnet/model_list");
                break;
            case R.id.mi_cn_canny:
                sdApiHelper.sendGetRequest("setCnCanny", "/controlnet/model_list");
                break;
            case R.id.mi_cn_normal:
                sdApiHelper.sendGetRequest("setCnNormal", "/controlnet/model_list");
                break;
            case R.id.mi_cn_mlsd:
                sdApiHelper.sendGetRequest("setCnMlsd", "/controlnet/model_list");
                break;
            case R.id.mi_cn_lineart:
                sdApiHelper.sendGetRequest("setCnLineart", "/controlnet/model_list");
                break;
            case R.id.mi_cn_softedge:
                sdApiHelper.sendGetRequest("setCnSoftedge", "/controlnet/model_list");
                break;
            case R.id.mi_cn_seg:
                sdApiHelper.sendGetRequest("setCnSeg", "/controlnet/model_list");
                break;
            case R.id.mi_cn_tile:
                sdApiHelper.sendGetRequest("setCnTile", "/controlnet/model_list");
                break;
            case R.id.mi_sd_output_dim:
                showOutputDimenDialog();
                break;
            case R.id.mi_canvas_dim:
                showTextInputDialog("canvasDim", "Canvas Dimension:", "", "2560");
                break;
            case R.id.mi_sd_model:
                sdApiHelper.sendGetRequest("setSDModel", "/sdapi/v1/sd-models");
                break;
            case R.id.mi_sd_inpaint_model:
                sdApiHelper.sendGetRequest("setSDInpaintModel", "/sdapi/v1/sd-models");
                break;
            case R.id.mi_sd_sampler:
                sdApiHelper.sendGetRequest("setSampler", "/sdapi/v1/samplers");
                break;
            case R.id.mi_sd_upscaler:
                sdApiHelper.sendGetRequest("setUpscaler", "/sdapi/v1/upscalers");
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
        dialog.show();
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
        spAspect.setSelection(prefAspect.equals(Sketch.ASPECT_RATIO_LANDSCAPE) ? 2 : prefAspect.equals(Sketch.ASPECT_RATIO_PORTRAIT) ? 1 : 0);

        builder.setPositiveButton("OK", (dialog, which) -> {
            int ipSize = (spSize.getSelectedItemPosition() == 1 ? 768
                    : spSize.getSelectedItemPosition() == 2 ? 1024
                    : spSize.getSelectedItemPosition() == 3 ? 1280
                    : 512);
            String ipAspect = (spAspect.getSelectedItemPosition() == 1 ? Sketch.ASPECT_RATIO_PORTRAIT
                    : spAspect.getSelectedItemPosition() == 2 ? Sketch.ASPECT_RATIO_LANDSCAPE
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
        View dialogView = inflater.inflate(R.layout.dialog_textbox, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.text_title);
        tvTitle.setText("What do you want me to draw?");
        MultiAutoCompleteTextView editText = dialogView.findViewById(R.id.edit_text);
        editText.setText(sharedPreferences.getString("txt2imgPrompt", ""));
        if (DrawingActivity.loraList != null) {
            ArrayAdapter<String> loraAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, DrawingActivity.loraList);
            editText.setAdapter(loraAdapter);
            editText.setThreshold(1);
            editText.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        }

        builder.setPositiveButton("OK", (dialog, which) -> {
            String prompt = editText.getText().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("txt2imgPrompt",prompt);
            editor.apply();
            ViewSdImageActivity.mBitmap = null;
            ViewSdImageActivity.isCallingAPI = false;
            Intent intent = new Intent(MainActivity.this, ViewSdImageActivity.class);
            intent.putExtra("sketchId", -3);
            intent.putExtra("cnMode", Sketch.CN_MODE_TXT);
            intent.putExtra("prompt", prompt);
            drawingActivityResultLauncher.launch(intent);
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

    private void launchCamera() {
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

    ActivityResultLauncher<Intent> cameraResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            (ActivityResult result) -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    //Intent data = result.getData();
                    Intent intent = new Intent(MainActivity.this, DrawingActivity.class);
                    intent.putExtra("sketchId", -2);
                    intent.putExtra("bitmapPath", mImageFile.getAbsolutePath());
                    drawingActivityResultLauncher.launch(intent);
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
    public void onSdApiFailure(String requestType) {
        if (!"setSDModel1".equals(requestType) ) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Request Type: " + requestType)
                    .setTitle("Call Stable Diffusion API failed.  Please check the server address.")
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
            }else if ("setCnLineart".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet Line Art Model", "cnLineartModel", "control_v11p_sd15s2_lineart_anime [3825e83e]", "lineart");
            } else if ("setCnSoftedge".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet Soft Edge Model", "cnSoftedgeModel", "control_v11p_sd15_softedge [a8575a2a]", "softedge");
            } else if ("setCnSeg".equals(requestType)) {
                showSpinnerDialog((new JSONObject(responseBody)).getJSONArray("model_list"), null, "ControlNet Seg Model", "cnSegModel", "control_v11p_sd15_seg [e1f51eb9]", "seg");
            } else if ("getLoras".equals(requestType)) {
                DrawingActivity.loraList = sdApiHelper.getLoras(responseBody);
                showPromptDialog();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
