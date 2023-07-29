package com.jsoft.diffusionpaint;

import static com.jsoft.diffusionpaint.dto.Sketch.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.jsoft.diffusionpaint.component.DrawingMode;
import com.jsoft.diffusionpaint.component.DrawingView;
import com.jsoft.diffusionpaint.component.DrawingViewListener;
import com.jsoft.diffusionpaint.component.CircleView;
import com.jsoft.diffusionpaint.helper.PaintDb;
import com.jsoft.diffusionpaint.dto.Sketch;
import com.jsoft.diffusionpaint.helper.SdApiHelper;
import com.jsoft.diffusionpaint.helper.SdApiResponseListener;
import com.jsoft.diffusionpaint.helper.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DrawingActivity extends AppCompatActivity implements ColorPickerDialogListener, DrawingViewListener, SdApiResponseListener {

    private DrawingView mDrawingView;
    private CircleView circleView;
    private int mCurrentColor;
    private int mCurrentStroke;
    private PaintDb db;
    private Sketch mCurrentSketch;
    private SeekBar seekWidth;
    private String aspectRatio;
    private SdApiHelper sdApiHelper;
    MaterialButton eraserButton;
    MaterialButton colorPickerButton;
    MaterialButton switchModeButton;
    FloatingActionButton refButton;
    ExtendedFloatingActionButton generateButton;
    ImageView imgRef;
    Bitmap bmRef;
    public static List<String> loraList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        mCurrentSketch = new Sketch();
        db = new PaintDb(this);
        sdApiHelper = new SdApiHelper(this, this);
        if (loraList == null) {
            sdApiHelper.sendGetRequest("getLoras", "/sdapi/v1/loras");
        }
        Intent i = getIntent();
        int sketchId = i.getIntExtra("sketchId", -1);
        int parentId = i.getIntExtra("parentId", -1);
        String bitmapPath = i.getStringExtra("bitmapPath");
        aspectRatio = sharedPreferences.getString("sdImageAspect", ASPECT_RATIO_SQUARE);
        Bitmap rotatedBitmap = null;
        if (sketchId >= 0) {
            Sketch dbSketch = db.getSketch(sketchId);
            if (dbSketch != null) {
                mCurrentSketch = dbSketch;
                aspectRatio = Utils.getAspectRatio(mCurrentSketch.getImgPreview());
            }
        } else if (sketchId == -2) {
            mCurrentSketch.setId(sketchId);
            mCurrentSketch.setParentId(parentId);
            mCurrentSketch.setPrompt(i.getStringExtra("prompt"));
            mCurrentSketch.setNegPrompt(i.getStringExtra("negPrompt"));
            rotatedBitmap = Utils.getBitmapFromPath(bitmapPath);
            if (rotatedBitmap != null) {
                aspectRatio = Utils.getAspectRatio(rotatedBitmap);
            }
        }

        setScreenRotation();

        setContentView(R.layout.activity_drawing);

        mDrawingView = findViewById(R.id.drawing_view);
        int canvasDim = 2560;
        try {
            canvasDim = Integer.parseInt(sharedPreferences.getString("canvasDim", "2560"));
        } catch (Exception ignored) {
        }
        mCurrentColor = Color.BLUE;
        mDrawingView.setPaintColor(mCurrentColor);
        mDrawingView.setListener(this);
        seekWidth = findViewById(R.id.seek_width);
        mCurrentStroke = seekWidth.getProgress();
        mDrawingView.setPaintStrokeWidth(mCurrentStroke);

        circleView = findViewById(R.id.circle_pen);
        circleView.setColor(mCurrentColor);
        circleView.setRadius(mCurrentStroke / 2f);

        float ratio = 1;

        if (sketchId == -2) {
            ratio = (float)rotatedBitmap.getWidth() / (float)rotatedBitmap.getHeight();
        } else if (sketchId >= 0) {
            ratio = (float)mCurrentSketch.getImgPreview().getWidth() / (float)mCurrentSketch.getImgPreview().getHeight();
        } else {
            if (aspectRatio.equals(ASPECT_RATIO_LANDSCAPE)) {
                ratio = 4f / 3f;
            } else if (aspectRatio.equals(ASPECT_RATIO_PORTRAIT)) {
                ratio = 3f / 4f;
            } else {
                ratio = 1;
            }
        }

        mDrawingView.setCanvasSize(canvasDim, ratio);

        if (sketchId >= 0) {
            if (mCurrentSketch.getImgPreview() != null) {
                mDrawingView.setmBaseBitmap(mCurrentSketch.getImgBackground() == null ? mCurrentSketch.getImgPreview() : mCurrentSketch.getImgBackground());
                mDrawingView.setmPaintBitmap(mCurrentSketch.getImgPaint());
            }
        }

        initButtons();

        if (sketchId == -2) {
            mDrawingView.setmBaseBitmap(rotatedBitmap);
        }

    }

    @Override
    public void onBackPressed() {
        gotoMainActivity();
    }

    protected void initButtons() {

        seekWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mCurrentStroke = i;
                mDrawingView.setPaintStrokeWidth(mCurrentStroke);
                circleView.setRadius(mCurrentStroke / 2f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar_draw);
        toolbar.setNavigationOnClickListener(v -> {
            gotoMainActivity();
        });
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.mi_draw_undo:
                    mDrawingView.undo();
                    break;
                case R.id.mi_draw_redo:
                    mDrawingView.redo();
                    break;
                case R.id.mi_draw_delete:
                    if (mCurrentSketch.getId() >= 0) {
                        db.deleteSketch(mCurrentSketch.getId());
                    }
                    gotoMainActivity();
                    break;
                case R.id.mi_draw_save:
                    saveSketch();
                    gotoMainActivity();
                    break;
            }

            return true;
        });

        generateButton = findViewById(R.id.fab_img_generate);
        generateButton.setOnClickListener(v -> showInputDialog());

        circleView.setOnClickListener(view -> ColorPickerDialog.newBuilder()
                .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                .setAllowPresets(true)
                .setDialogId(0)
                .setColor(mCurrentColor)
                .setShowAlphaSlider(true)
                .show(this));

        colorPickerButton = findViewById(R.id.fab_colorpicker);

        colorPickerButton.setOnClickListener(view -> {
            if (!mDrawingView.getIsEraserMode()) {
                hideTools();
                mDrawingView.setEyedropper(true);
                eraserButton.setVisibility(View.GONE);
            }
        });

        eraserButton = findViewById(R.id.fab_eraser);
        eraserButton.setOnClickListener(view -> {
            if (!mDrawingView.getIsEraserMode()) {
                mDrawingView.setEraserMode();
                eraserButton.setIconResource(R.drawable.ic_brush);
                circleView.setColor(Color.BLACK);
            } else {
                mDrawingView.setPenMode();
                eraserButton.setIconResource(R.drawable.ic_eraser);
                circleView.setColor(mCurrentColor);
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

        switchModeButton = findViewById(R.id.fab_switch_mode);
        switchModeButton.setOnClickListener(v -> {
            if (this.mDrawingView.getMode() == DrawingMode.Draw) {
                this.mDrawingView.setMode(DrawingMode.MoveView);
                switchModeButton.setIconResource(R.drawable.baseline_back_hand_24);
            } else {
                this.mDrawingView.setMode(DrawingMode.Draw);
                switchModeButton.setIconResource(R.drawable.baseline_draw_24);
            }
        });
    }

    public void hideTools() {
        seekWidth.setVisibility(View.GONE);
        circleView.setVisibility(View.GONE);
        imgRef.setVisibility(View.GONE);
        refButton.setVisibility(View.GONE);
    }

    public void showTools() {
        seekWidth.setVisibility(View.VISIBLE);
        circleView.setVisibility(View.VISIBLE);
        if (bmRef != null) {
            imgRef.setVisibility(View.VISIBLE);
        } else {
            refButton.setVisibility(View.VISIBLE);
        }
    }

    public void gotoMainActivity() {
        finish();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Lock the orientation to portrait
        setScreenRotation();
    }

    public void setScreenRotation() {

        if (aspectRatio.equals(ASPECT_RATIO_PORTRAIT)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else if (aspectRatio.equals(ASPECT_RATIO_LANDSCAPE)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_prompt_mode, null);
        builder.setView(dialogView);

        final MultiAutoCompleteTextView promptTV = dialogView.findViewById(R.id.sd_prompt);
        final MultiAutoCompleteTextView negPromptTV = dialogView.findViewById(R.id.sd_negative_prompt);
        promptTV.setText(mCurrentSketch.getPrompt());
        negPromptTV.setText(mCurrentSketch.getNegPrompt());
        if (loraList != null) {
            ArrayAdapter<String> loraAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, loraList);
            promptTV.setAdapter(loraAdapter);
            promptTV.setThreshold(1);
            promptTV.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        }

        Spinner sdMode = dialogView.findViewById(R.id.sd_mode_selection);

        List<String> filteredModes = new ArrayList<>();
        for (String mode : cnModeMap.keySet()) {
            filteredModes.add(mode);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filteredModes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sdMode.setAdapter(adapter);
        for (int i = 0; i < cnModeMap.size(); i++) {
            String modeDesc = sdMode.getItemAtPosition(i).toString();
            if (Objects.equals(cnModeMap.get(modeDesc), mCurrentSketch.getCnMode())) {
                sdMode.setSelection(i);
                break;
            }
        }

        builder.setPositiveButton("OK", (dialog, which) -> {
            String promptText = promptTV.getText().toString();
            mCurrentSketch.setPrompt(promptText);
            String selectMode = sdMode.getSelectedItem().toString();
            mCurrentSketch.setCnMode(cnModeMap.get(selectMode));
            String negPromptText = negPromptTV.getText().toString();
            mCurrentSketch.setNegPrompt(negPromptText);
            saveSketch();
            if (mCurrentSketch.getCnMode().startsWith("inpaint") && mDrawingView.isEmpty() && Utils.isEmptyBitmap(mCurrentSketch.getImgPaint())) {
                gotoViewSdImageActivity(mCurrentSketch.getId(), CN_MODE_ORIGIN);
            } else {
                gotoViewSdImageActivity(mCurrentSketch.getId(), mCurrentSketch.getCnMode());
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        if (!isFinishing()) dialog.show();
    }

    ActivityResultLauncher<Intent> sdViewerActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
            });

    public void gotoViewSdImageActivity(int sketchID, String cnMode) {
        ViewSdImageActivity.mBitmap = null;
        ViewSdImageActivity.inpaintBitmap = null;
        ViewSdImageActivity.isCallingAPI = false;
        Intent intent = new Intent(DrawingActivity.this, ViewSdImageActivity.class);
        intent.putExtra("sketchId", sketchID);
        intent.putExtra("cnMode", cnMode);
        sdViewerActivityResultLauncher.launch(intent);
    }

    public void saveSketch() {
        mCurrentSketch = mDrawingView.prepareBitmap(mCurrentSketch, bmRef);
        if (mCurrentSketch.getId() < 0) {
            long rowId = db.insertSketch(mCurrentSketch);
            int sketchID = db.getId4rowid(rowId);
            mCurrentSketch.setId(sketchID);
        } else {
            db.updateSketch(mCurrentSketch);
        }
    }

    @Override
    public void onColorSelected(int dialogId, int color) {
        if (dialogId == 0) {
            mCurrentColor = color;
            mDrawingView.setPaintColor(mCurrentColor);
            circleView.setColor(mCurrentColor);
        } else {
            throw new IllegalStateException("Unexpected value: " + dialogId);
        }
    }

    @Override
    public void onDialogDismissed(int dialogId) {
    }

    @Override
    public void onEyedropperResult(int color) {
        showTools();
        mDrawingView.setEyedropper(false);
        mCurrentColor = color;
        mDrawingView.setPaintColor(mCurrentColor);
        circleView.setColor(mCurrentColor);
        eraserButton.setVisibility(View.VISIBLE);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
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
                .setPositiveButton("OK", (dialog, id) -> {
                });
        AlertDialog alert = builder.create();
        if (!isFinishing()) alert.show();
    }

    @Override
    public void onSdApiResponse(String requestType, String responseBody) {
        if ("getLoras".equals(requestType)) {
            loraList = sdApiHelper.getLoras(responseBody);
        }
    }
}