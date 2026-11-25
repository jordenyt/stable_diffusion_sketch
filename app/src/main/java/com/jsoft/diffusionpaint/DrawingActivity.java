package com.jsoft.diffusionpaint;

import static com.jsoft.diffusionpaint.dto.Sketch.*;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;

import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.jsoft.diffusionpaint.component.DrawingView;
import com.jsoft.diffusionpaint.component.DrawingViewListener;
import com.jsoft.diffusionpaint.component.CircleView;
import com.jsoft.diffusionpaint.dto.PromptTool;
import com.jsoft.diffusionpaint.dto.SdParam;
import com.jsoft.diffusionpaint.dto.SdStyle;
import com.jsoft.diffusionpaint.helper.PaintDb;
import com.jsoft.diffusionpaint.dto.Sketch;
import com.jsoft.diffusionpaint.helper.PromptGenService;
import com.jsoft.diffusionpaint.helper.PromptGenStore;
import com.jsoft.diffusionpaint.helper.PromptToolCatalog;
import com.jsoft.diffusionpaint.helper.SdApiHelper;
import com.jsoft.diffusionpaint.helper.SdApiResponseListener;
import com.jsoft.diffusionpaint.helper.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DrawingActivity extends AppCompatActivity implements ColorPickerDialogListener, DrawingViewListener, SdApiResponseListener {

    private DrawingView mDrawingView;
    private CircleView circleView;
    private int mCurrentColor;
    private int mCurrentStroke;
    private int mCurrentBlur;
    private PaintDb db;
    private Sketch mCurrentSketch;
    private SeekBar seekWidth;
    private SeekBar seekBlur;
    private TextView tvWidth;
    private TextView tvBlur;
    private String aspectRatio;
    private SdApiHelper sdApiHelper;
    private Button btnCaption;
    private Button btnStop;
    FloatingActionButton paletteButton;
    FloatingActionButton undoButton;
    FloatingActionButton redoButton;
    FloatingActionButton sdButton;
    FloatingActionButton eraserButton;
    FloatingActionButton refButton;
    FloatingActionButton moveButton;
    ImageView modeIcon;
    ImageView imgRef;
    Bitmap bmRef;
    private int startIntentSketchId;
    public static ArrayList<Path> mPaths = new ArrayList<>();
    public static ArrayList<Paint> mPaints = new ArrayList<>();
    public static ArrayList<Path> mUndonePaths = new ArrayList<>();
    public static ArrayList<Paint> mUndonePaints = new ArrayList<>();
    private MultiAutoCompleteTextView promptTextView;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new PaintDb(this);
        sdApiHelper = new SdApiHelper(this, this);
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        String dflApiAddress = sharedPreferences.getString("dflApiAddress", "");
        if (Utils.isValidServerURL(dflApiAddress)) {
            if (Sketch.comfyuiModes == null) {
                sdApiHelper.sendRequest("getComfyuiMode", dflApiAddress, "/mode_config", null, "GET");
            }
            if (Sketch.loraList == null) {
                sdApiHelper.sendRequest("getLoras", dflApiAddress, "/loras", null, "GET");
            }
        }
        loadSketch(getIntent());
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        loadSketch(intent);
    }

    public static void clearPath() {
        mPaths = new ArrayList<>();
        mPaints = new ArrayList<>();
        mUndonePaths = new ArrayList<>();
        mUndonePaints = new ArrayList<>();
    }

    private void loadSketch(Intent i) {
        mCurrentSketch = new Sketch();
        int sketchId = i.getIntExtra("sketchId", -1);
        int parentId = i.getIntExtra("parentId", -1);
        String bitmapPath = i.getStringExtra("bitmapPath");
        aspectRatio = sharedPreferences.getString("sdImageAspect", ASPECT_RATIO_SQUARE);
        Bitmap rotatedBitmap = null;
        startIntentSketchId = sketchId;
        if (sketchId >= 0) {
            Sketch dbSketch = db.getSketch(sketchId);
            if (dbSketch != null) {
                mCurrentSketch = dbSketch;
            }
        } else if (sketchId == -2) {
            mCurrentSketch.setId(sketchId);
            mCurrentSketch.setParentId(parentId);
            mCurrentSketch.setPrompt(i.getStringExtra("prompt"));
            mCurrentSketch.setNegPrompt(i.getStringExtra("negPrompt"));

            mCurrentSketch.setExif(Utils.getImageExif(bitmapPath));
            if (!i.hasExtra("prompt")) {
                try {
                    JSONObject jsonExif = new JSONObject(mCurrentSketch.getExif());
                    if (jsonExif.has("UserComment")) {
                        String userComment = jsonExif.getString("UserComment");
                        Matcher matcher = Pattern.compile("(.*)[\\n\\?]Negative prompt: (.*)[\\n\\?]Steps: (.*)").matcher(userComment);
                        if (matcher.find()) {
                            if (mCurrentSketch.getPrompt() == null) {
                                mCurrentSketch.setPrompt(matcher.group(1));
                            }
                            if (mCurrentSketch.getNegPrompt() == null) {
                                mCurrentSketch.setNegPrompt(matcher.group(2));
                            }
                        } else {
                            matcher = Pattern.compile("(.*)[\\n\\?]Steps: (.*)").matcher(userComment);
                            if (matcher.find()) {
                                if (mCurrentSketch.getPrompt() == null) {
                                    mCurrentSketch.setPrompt(matcher.group(1));
                                }
                                if (mCurrentSketch.getNegPrompt() == null) {
                                    mCurrentSketch.setNegPrompt("");
                                }
                            }
                        }
                    }
                    if (!jsonExif.has("ImageDescription")) {
                        String filename = bitmapPath.substring(bitmapPath.lastIndexOf("/") + 1);
                        jsonExif.put("ImageDescription", filename);
                        mCurrentSketch.setExif(jsonExif.toString());
                    }
                } catch (Exception ignored) {
                }
            }
            rotatedBitmap = Utils.getBitmapFromPath(bitmapPath);
        } else if (sketchId == -3) {
            ViewSdImageActivity.mBitmap = null;
            ViewSdImageActivity.inpaintBitmap = null;
            ViewSdImageActivity.isCallingAPI = false;
            String cnMode = i.getStringExtra("cnMode");
            String promptText = i.getStringExtra("prompt");
            String negPromptText = i.getStringExtra("negPrompt");
            String aspectRatioText = i.getStringExtra("aspectRatio");
            String style = i.getStringExtra("style");
            int numGen = i.getIntExtra("numGen", 1);
            Intent intent = new Intent(DrawingActivity.this, ViewSdImageActivity.class);
            intent.putExtra("sketchId", -3);
            intent.putExtra("cnMode", cnMode);
            intent.putExtra("prompt", promptText);
            intent.putExtra("negPrompt", negPromptText);
            intent.putExtra("style", style);
            intent.putExtra("aspectRatio", aspectRatioText);
            intent.putExtra("numGen", numGen);
            sdViewerActivityResultLauncher.launch(intent);
        }

        setContentView(R.layout.activity_drawing);

        mDrawingView = findViewById(R.id.drawing_view);
        try {
            int canvasDim = Integer.parseInt(sharedPreferences.getString("canvasDim", "3840"));
            mDrawingView.setCanvasSize(canvasDim);
        } catch (Exception ignored) {}
        mCurrentColor = Color.argb(127, 0, 0, 255);
        mDrawingView.setPaintColor(mCurrentColor);
        mDrawingView.setListener(this);
        seekWidth = findViewById(R.id.seek_width);
        seekBlur = findViewById(R.id.seek_blur);
        tvWidth = findViewById(R.id.txt_width);
        tvBlur = findViewById(R.id.txt_blur);
        mCurrentStroke = seekWidth.getProgress();
        mCurrentBlur = seekBlur.getProgress();
        mDrawingView.setPaintStrokeWidth(mCurrentStroke);
        mDrawingView.setPaintStrokeBlur(mCurrentBlur);

        circleView = findViewById(R.id.circle_pen);
        circleView.setColor(mCurrentColor);
        circleView.setRadius((float)mCurrentStroke);
        circleView.setBlur((float)mCurrentBlur);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { goBack();}
        });

        if (sketchId >= 0) {
            if (mCurrentSketch.getImgPreview() != null) {
                mDrawingView.setmBaseBitmap(mCurrentSketch.getImgBackground() == null? mCurrentSketch.getImgPreview(): mCurrentSketch.getImgBackground());
                mDrawingView.setmBasePathBitmap(mCurrentSketch.getImgPaint());
            }
        }

        initButtons();

        applyImmersiveMode();

        if (sketchId == -2) {
            mDrawingView.setmBaseBitmap(rotatedBitmap);
            mDrawingView.prepareBitmap(mCurrentSketch, null);
        } else if (sketchId == -1) {
            //try {
                //int canvasDim = Integer.parseInt(sharedPreferences.getString("canvasDim", "3840"));
                Bitmap emptyImg = Utils.getEmptyBackground(1280, aspectRatio);
                mDrawingView.setmBaseBitmap(emptyImg);
                mCurrentSketch.setImgBackground(emptyImg);
                mDrawingView.prepareBitmap(mCurrentSketch, null);
            //} catch (Exception ignored) {}
        }
    }

    public void goBack() {
        gotoMainActivity();
    }

    /**
     * Hide the system bars (status + navigation / soft key panel) while this fullscreen
     * editor is in the foreground, so the panel never covers the bottom controls.
     * The bars reappear transiently when the user swipes from the screen edge.
     */
    private void applyImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveMode();
        }
    }

    protected void initButtons() {

        modeIcon = findViewById(R.id.img_drawing_mode);
        modeIcon.setImageResource(R.drawable.ic_brush);
        paletteButton = findViewById(R.id.fab_color);
        paletteButton.setOnClickListener(view -> ColorPickerDialog.newBuilder()
                .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                .setAllowPresets(true)
                .setDialogId(0)
                .setColor(mCurrentColor)
                .setShowAlphaSlider(true)
                .show(this));

        seekWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mCurrentStroke = i;
                mDrawingView.setPaintStrokeWidth(mCurrentStroke);
                circleView.setRadius((float)mCurrentStroke);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBlur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mCurrentBlur = i;
                mDrawingView.setPaintStrokeBlur(mCurrentBlur);
                circleView.setBlur((float)mCurrentBlur);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        undoButton = findViewById(R.id.fab_undo);
        undoButton.setOnClickListener(view -> mDrawingView.undo());

        redoButton = findViewById(R.id.fab_redo);
        redoButton.setOnClickListener(view -> mDrawingView.redo());

        FloatingActionButton saveButton = findViewById(R.id.fab_save);
        saveButton.setOnClickListener(view -> {
            saveSketch();
            gotoMainActivity();
        });

        moveButton = findViewById(R.id.fab_move);
        moveButton.setOnClickListener(view -> {
            if (!mDrawingView.getIsTranslate()) {
                hideTools();
                mDrawingView.setIsTranslate(true);
                eraserButton.setVisibility(View.GONE);
                modeIcon.setImageResource(R.drawable.magnifying_glass_icon);
                moveButton.setImageResource(R.drawable.ic_edit);
            } else {
                showTools();
                eraserButton.setVisibility(View.VISIBLE);
                moveButton.setImageResource(R.drawable.magnifying_glass_icon);
                if (!mDrawingView.getIsEraserMode()) {
                    modeIcon.setImageResource(R.drawable.ic_brush);
                } else {
                    modeIcon.setImageResource(R.drawable.ic_eraser);
                }
                mDrawingView.setIsTranslate(false);
            }
        });

        sdButton = findViewById(R.id.fab_stable_diffusion);
        sdButton.setOnClickListener(view -> {
            showInputDialog();
        });

        circleView.setOnClickListener(view -> {
            if (!mDrawingView.getIsEraserMode()) {
                hideTools();
                mDrawingView.setEyedropper(true);
                eraserButton.setVisibility(View.GONE);
                moveButton.setVisibility(View.GONE);
                modeIcon.setImageResource(R.drawable.ic_eyedropper);
            }
        });

        eraserButton = findViewById(R.id.fab_eraser);
        eraserButton.setOnClickListener(view -> {
            if (!mDrawingView.getIsEraserMode()) {
                mDrawingView.setEraserMode();
                eraserButton.setImageResource(R.drawable.ic_brush);
                paletteButton.setEnabled(false);
                circleView.setColor(Color.BLACK);
                modeIcon.setImageResource(R.drawable.ic_eraser);
            } else {
                mDrawingView.setPenMode();
                eraserButton.setImageResource(R.drawable.ic_eraser);
                paletteButton.setEnabled(true);
                circleView.setColor(mCurrentColor);
                modeIcon.setImageResource(R.drawable.ic_brush);
            }
        });

        refButton = findViewById(R.id.fab_img_reference);
        refButton.setOnClickListener(view -> {
            pickImage();
        });

        imgRef = findViewById(R.id.img_reference);
        imgRef.setOnClickListener(view -> {
            pickImage();
        });

        bmRef = mCurrentSketch.getImgReference();
        if (bmRef != null) {
            imgRef.setImageBitmap(bmRef);
            refButton.setVisibility(View.GONE);
        } else {
            imgRef.setVisibility(View.GONE);
        }
    }

    public void hideTools() {
        sdButton.setVisibility(View.GONE);
        undoButton.setVisibility(View.GONE);
        redoButton.setVisibility(View.GONE);
        paletteButton.setVisibility(View.GONE);
        seekWidth.setVisibility(View.GONE);
        seekBlur.setVisibility(View.GONE);
        tvWidth.setVisibility(View.GONE);
        tvBlur.setVisibility(View.GONE);
        circleView.setVisibility(View.GONE);
        imgRef.setVisibility(View.GONE);
        refButton.setVisibility(View.GONE);
    }

    public void showTools() {
        sdButton.setVisibility(View.VISIBLE);
        undoButton.setVisibility(View.VISIBLE);
        redoButton.setVisibility(View.VISIBLE);
        paletteButton.setVisibility(View.VISIBLE);
        seekWidth.setVisibility(View.VISIBLE);
        seekBlur.setVisibility(View.VISIBLE);
        tvWidth.setVisibility(View.VISIBLE);
        tvBlur.setVisibility(View.VISIBLE);
        circleView.setVisibility(View.VISIBLE);
        if (bmRef != null) {
            imgRef.setVisibility(View.VISIBLE);
        } else {
            refButton.setVisibility(View.VISIBLE);
        }
    }

    public void gotoMainActivity() {
        clearPath();
        Intent intent = new Intent(this, MainActivity.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_prompt_mode, null);
        builder.setView(dialogView);

        final MultiAutoCompleteTextView promptTV = dialogView.findViewById(R.id.sd_prompt);
        final MultiAutoCompleteTextView negPromptTV = dialogView.findViewById(R.id.sd_negative_prompt);
        final RadioGroup radioGroupMode = dialogView.findViewById(R.id.radio_sdmode);

        promptTV.setText(mCurrentSketch.getPrompt());
        negPromptTV.setText(mCurrentSketch.getNegPrompt());
        List<String> acList = new ArrayList<>();
        String autoCompletePhrases = sharedPreferences.getString("autoCompletePhrases", "[]");
        try {
            JSONArray jsonArray = new JSONArray(autoCompletePhrases);
            for (int i = 0; i < jsonArray.length(); i++) {
                acList.add(jsonArray.getString(i));
            }
            if (Sketch.loraList != null) {
                for (int i = 0; i < Sketch.loraList.length(); i++) {
                    acList.add("<lora:" + Sketch.loraList.getString(i).replace("/", "\\") + ">");
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        ArrayAdapter<String> loraAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, acList);
        promptTV.setAdapter(loraAdapter);
        promptTV.setThreshold(1);
        promptTV.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        promptTextView = promptTV;

        btnCaption = dialogView.findViewById(R.id.btnCaption);
        btnStop = dialogView.findViewById(R.id.btnStop);

        // "AI Prompt" asks the gateway which text generating workflows it
        // offers and lets the user pick one of them.
        btnCaption.setOnClickListener(view -> choosePromptTool());

        // "Stop" ends the request that is running.
        btnStop.setOnClickListener(view -> {
            PromptGenService.cancel(DrawingActivity.this);
            Toast.makeText(DrawingActivity.this, "Stopping...", Toast.LENGTH_SHORT).show();
        });

        updatePromptGenButtons();

        Spinner sdMode = dialogView.findViewById(R.id.sd_mode_selection);
        
        Map<String, String> modeMap = new LinkedHashMap<>();
        Map<String, String> cnModeMap = Sketch.cnModeMap();
        for (String mode : cnModeMap.keySet()) {
            String modeDisplayName = mode;
            modeMap.put(modeDisplayName, cnModeMap.get(mode));
        }

        radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            String modeType = "all";
            switch (checkedId) {
                case R.id.radio_txt2img:
                    modeType = SdParam.SD_MODE_TYPE_TXT2IMG;
                    break;
                case R.id.radio_img2img:
                    modeType = SdParam.SD_MODE_TYPE_IMG2IMG;
                    break;
                case R.id.radio_inpaint:
                    modeType = SdParam.SD_MODE_TYPE_INPAINT;
                    break;
            }
            List<String> modeSelectList = new ArrayList<>();
            for (String displayName : modeMap.keySet()) {
                SdParam sdParam = sdApiHelper.getSdCnParm(modeMap.get(displayName));
                if (modeType.equals(sdParam.type) || "all".equals(modeType)) {
                    modeSelectList.add(displayName);
                }
            }
            ArrayAdapter<String> filteredAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modeSelectList);
            filteredAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sdMode.setAdapter(filteredAdapter);
            for (int i=0; i<modeSelectList.size(); i++) {
                String cnMode = modeMap.get(modeSelectList.get(i));
                if (Objects.equals(cnMode, mCurrentSketch.getCnMode())) {
                    sdMode.setSelection(i);
                    break;
                }
            }
        });
        radioGroupMode.check(R.id.radio_all);

        TextView sdAspectRatioTxt = dialogView.findViewById(R.id.sd_aspect_ratio_txt);
        Spinner sdAspectRatio = dialogView.findViewById(R.id.sd_aspect_ratio);
        sdAspectRatioTxt.setVisibility(View.GONE);
        sdAspectRatio.setVisibility(View.GONE);

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
            mCurrentSketch.setPrompt(promptText);
            String selectMode = sdMode.getSelectedItem().toString();
            mCurrentSketch.setCnMode(modeMap.get(selectMode));
            String negPromptText = negPromptTV.getText().toString();
            mCurrentSketch.setNegPrompt(negPromptText);
            saveSketch();
            int numGen = sdNumGen.getProgress();
            if (mCurrentSketch.getCnMode().startsWith("inpaint") && mDrawingView.isEmpty() && Utils.isEmptyBitmap(mCurrentSketch.getImgPaint())) {
                gotoViewSdImageActivity(mCurrentSketch.getId(), CN_MODE_ORIGIN, numGen);
            } else {
                gotoViewSdImageActivity(mCurrentSketch.getId(), mCurrentSketch.getCnMode(), numGen);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> promptTextView = null);
        if(!isFinishing()) dialog.show();
    }

    // ------------------------------------------------------------------
    // Prompt generation: image (+ text) -> text
    // ------------------------------------------------------------------

    private final PromptGenStore.Listener promptGenListener =
            store -> runOnUiThread(this::onPromptGenChanged);

    /** The last error and state that were shown, so they are not shown twice. */
    private String shownPromptGenError = "";
    private String shownPromptGenState = "";

    @Override
    protected void onResume() {
        super.onResume();
        PromptGenStore.setAppContext(this);
        PromptGenStore.get().restore(this);
        PromptGenStore.get().addListener(promptGenListener);
        onPromptGenChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PromptGenStore.get().removeListener(promptGenListener);
    }

    /** The state of the request changed. */
    private void onPromptGenChanged() {
        updatePromptGenButtons();
        applyPromptGenResult();

        PromptGenStore store = PromptGenStore.get();
        String state = store.getState();
        if (PromptGenStore.STATE_ERROR.equals(state)) {
            String message = store.getError();
            if (!message.isEmpty() && !message.equals(shownPromptGenError)) {
                shownPromptGenError = message;
                showMessage(message);
            }
        } else if (PromptGenStore.STATE_CANCELLED.equals(state)
                && !PromptGenStore.STATE_CANCELLED.equals(shownPromptGenState)) {
            Toast.makeText(this, "The request was stopped.", Toast.LENGTH_SHORT).show();
        }
        shownPromptGenState = state;
    }

    /**
     * Put a generated prompt into the sketch.
     *
     * The result is picked up when it arrives - also when the app was in the
     * background while the request was running.
     */
    private void applyPromptGenResult() {
        String text = PromptGenStore.get().takeResult();
        if (text == null) {
            return;
        }
        if (promptTextView != null) {
            promptTextView.setText(text);
        }
        mCurrentSketch.setPrompt(text);
        saveSketch();
        Toast.makeText(this, "Prompt generated.", Toast.LENGTH_SHORT).show();
    }

    /** Show the state of the request on the buttons. */
    private void updatePromptGenButtons() {
        boolean running = PromptGenStore.get().isRunning();
        if (btnCaption != null) {
            btnCaption.setEnabled(!running);
            btnCaption.setText(running ? "Running..." : "AI Prompt");
        }
        if (btnStop != null) {
            btnStop.setVisibility(running ? View.VISIBLE : View.GONE);
        }
    }

    /** Ask the gateway which text generating workflows it offers. */
    private void choosePromptTool() {
        String baseUrl = sharedPreferences.getString("dflApiAddress", "");
        PromptToolCatalog.load(this, baseUrl, new PromptToolCatalog.Callback() {
            @Override
            public void onTools(List<PromptTool> tools) {
                runOnUiThread(() -> showToolPicker(tools));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showMessage(message));
            }
        });
    }

    /** Let the user pick one of the offered workflows. */
    private void showToolPicker(List<PromptTool> tools) {
        if (isFinishing()) {
            return;
        }
        // A workflow that needs a reference image cannot run without one, and
        // one that needs a third image cannot run with only the background and
        // the reference.
        Bitmap reference = currentReference();
        List<PromptTool> usable = PromptTool.usableFor(tools, true, reference != null);
        if (usable.isEmpty()) {
            showMessage("No workflow fits the images of this sketch.");
            return;
        }        String[] labels = new String[usable.size()];
        for (int i = 0; i < usable.size(); i++) {
            labels[i] = usable.get(i).label();
        }
        new AlertDialog.Builder(this)
                .setTitle("Generate a prompt")
                .setItems(labels, (dialog, which) -> startPromptGen(usable.get(which)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Send the request to the gateway.
     *
     * The images and the text are filled in by the app: "image1" gets the
     * background of the sketch, "image2" the reference image and "prompt" the
     * positive prompt. A workflow only receives the keys it declares.
     *
     * The payload is written to a file: a request with several images is too
     * big for an intent extra.
     */
    private void startPromptGen(PromptTool tool) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("workflow", tool.workflow);

            if (!tool.image1Key.isEmpty()) {
                Bitmap background = mCurrentSketch.getImgBackground() != null
                        ? mCurrentSketch.getImgBackground() : mCurrentSketch.getImgPreview();
                if (background != null) {
                    payload.put(tool.image1Key, Utils.jpg2Base64String(scaleForPrompt(background)));
                }
            }
            if (!tool.image2Key.isEmpty()) {
                Bitmap reference = currentReference();
                if (reference != null) {
                    payload.put(tool.image2Key, Utils.jpg2Base64String(scaleForPrompt(reference)));
                }
            }
            if (!tool.promptKey.isEmpty()) {
                String prompt = currentPrompt();
                if (!prompt.isEmpty()) {
                    payload.put(tool.promptKey, prompt);
                }
            }
        } catch (JSONException e) {
            showMessage("Cannot build the request: " + e.getMessage());
            return;
        }

        File file;
        try {
            file = File.createTempFile("promptgen", ".json", getCacheDir());
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            showMessage("Cannot store the request: " + e.getMessage());
            return;
        }

        PromptGenService.start(this, file.getAbsolutePath(), tool.workflow, tool.label(),
                mCurrentSketch.getId());
        shownPromptGenError = "";
        shownPromptGenState = "";
        Toast.makeText(this, "Generating...", Toast.LENGTH_SHORT).show();
        updatePromptGenButtons();
    }

    /** The reference image of the sketch, the one just picked or the saved one. */
    private Bitmap currentReference() {
        return bmRef != null ? bmRef : mCurrentSketch.getImgReference();
    }

    /** The positive prompt of the sketch, as typed in the prompt dialog. */
    private String currentPrompt() {
        if (promptTextView != null) {
            CharSequence text = promptTextView.getText();
            if (text != null && !text.toString().trim().isEmpty()) {
                return text.toString();
            }
        }
        String prompt = mCurrentSketch.getPrompt();
        return prompt == null ? "" : prompt;
    }

    /** Smaller images are enough for the vision models and keep the request small. */
    private static Bitmap scaleForPrompt(Bitmap bitmap) {
        int maxSide = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (maxSide <= 1280) {
            return bitmap;
        }
        double scale = maxSide / 1280d;
        return Bitmap.createScaledBitmap(bitmap,
                (int) Math.round(bitmap.getWidth() / scale),
                (int) Math.round(bitmap.getHeight() / scale), true);
    }

    private void showMessage(String message) {
        if (isFinishing()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    ActivityResultLauncher<Intent> sdViewerActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    if (startIntentSketchId == -3) {
                        gotoMainActivity();
                    } else {
                        Intent i = result.getData();
                        if (i != null) {
                            if (i.getIntExtra("sketchId", -1) != startIntentSketchId) {
                                clearPath();
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                finish();
                                startActivity(i);
                            }
                        }
                    }
                } else if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent i = result.getData();
                    if (i != null) {
                        int sketchId = i.getIntExtra("sketchId", -1);
                        if (sketchId == -2) {
                            clearPath();
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            finish();
                            startActivity(i);
                        } else {
                            gotoMainActivity();
                        }
                    } else {
                        gotoMainActivity();
                    }
                }
            });

    public void gotoViewSdImageActivity(int sketchID, String cnMode, int numGen) {
        ViewSdImageActivity.mBitmap = null;
        ViewSdImageActivity.inpaintBitmap = null;
        ViewSdImageActivity.isCallingAPI = false;
        Intent intent = new Intent(DrawingActivity.this, ViewSdImageActivity.class);
        intent.putExtra("sketchId", sketchID);
        intent.putExtra("cnMode", cnMode);
        intent.putExtra("numGen", numGen);
        sdViewerActivityResultLauncher.launch(intent);
    }

    public void saveSketch()  {
        mCurrentSketch = mDrawingView.prepareBitmap(mCurrentSketch, bmRef);
        if (mCurrentSketch.getId() < 0) {
            long rowId  = db.insertSketch(mCurrentSketch);
            int sketchID = db.getId4rowid(rowId);
            mCurrentSketch.setId(sketchID);
        } else {
            db.updateSketch(mCurrentSketch);
        }
    }

    @Override public void onColorSelected(int dialogId, int color) {
        if (dialogId == 0) {
            mCurrentColor = color;
            mDrawingView.setPaintColor(mCurrentColor);
            circleView.setColor(mCurrentColor);
        } else {
            throw new IllegalStateException("Unexpected value: " + dialogId);
        }
    }

    @Override
    public void onDialogDismissed(int dialogId) {}

    @Override
    public void onEyedropperResult(int color) {
        showTools();
        mDrawingView.setEyedropper(false);
        mCurrentColor = color;
        mDrawingView.setPaintColor(mCurrentColor);
        circleView.setColor(mCurrentColor);
        eraserButton.setVisibility(View.VISIBLE);
        moveButton.setVisibility(View.VISIBLE);
        modeIcon.setImageResource(R.drawable.ic_brush);
    }

    private void pickImage() {
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
                    bmRef = Utils.getBitmapFromPath(filePath);
                    imgRef.setImageBitmap(bmRef);
                    imgRef.setVisibility(View.VISIBLE);
                    refButton.setVisibility(View.GONE);
                }
            });


    @Override
    public void onSdApiFailure(String requestType, String errMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Request Type: " + requestType)
                .setTitle("Call Stable Diffusion API failed")
                .setMessage(errMessage)
                .setPositiveButton("OK", (dialog, id) -> {});
        AlertDialog alert = builder.create();
        if(!isFinishing()) alert.show();
    }

    @Override
    public void onSdApiResponse(String requestType, String responseBody)  {
        if ("getComfyuiMode".equals(requestType)) {
            try {
                Sketch.comfyuiModes = new JSONArray(responseBody);
            } catch (JSONException ignored) {
            }
        } else if ("getLoras".equals(requestType)) {
            try {
                Sketch.loraList = new JSONArray(responseBody);
            } catch (JSONException ignored) {}
        }
    }
}