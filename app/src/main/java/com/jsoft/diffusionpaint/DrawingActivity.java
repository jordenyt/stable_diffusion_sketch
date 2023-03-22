package com.jsoft.diffusionpaint;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.jsoft.diffusionpaint.component.DrawingView;
import com.jsoft.diffusionpaint.helper.CircleView;
import com.jsoft.diffusionpaint.helper.PaintDb;
import com.jsoft.diffusionpaint.helper.Sketch;

import java.io.IOException;

public class DrawingActivity extends AppCompatActivity implements ColorPickerDialogListener
{
    private DrawingView mDrawingView;
    private CircleView circleView;
    private int mCurrentColor;
    private int mCurrentStroke;
    private PaintDb db;
    private Sketch mCurrentSketch;
    private SeekBar seekWidth;
    private String aspectRatio;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        mCurrentSketch = new Sketch();
        db = new PaintDb(this);
        Intent i = getIntent();
        int sketchId = i.getIntExtra("sketchId", -1);
        String bitmapPath = i.getStringExtra("bitmapPath");
        aspectRatio = sharedPreferences.getString("sdImageAspect", "square");
        Bitmap rotatedBitmap = null;
        if (sketchId >= 0) {
            Sketch dbSketch = db.getSketch(sketchId);
            if (dbSketch != null) {
                mCurrentSketch = dbSketch;
                if (mCurrentSketch.getImgPreview().getWidth() * 3 / 4 >= mCurrentSketch.getImgPreview().getHeight()) {
                    aspectRatio = "landscape";
                } else if (mCurrentSketch.getImgPreview().getWidth() <= mCurrentSketch.getImgPreview().getHeight() * 3 / 4) {
                    aspectRatio = "portrait";
                } else {
                    aspectRatio = "square";
                }
            }
        } else if (sketchId == -2) {
            mCurrentSketch.setId(sketchId);
            Bitmap imageBitmap = BitmapFactory.decodeFile(bitmapPath);
            if (imageBitmap != null) {
                int orientation = ExifInterface.ORIENTATION_UNDEFINED;
                try {
                    ExifInterface exif = null;
                    exif = new ExifInterface(bitmapPath);
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                } catch (IOException e) {
                    Log.e("diffusionpaint", "IOException get from returned camera file.");
                }

                // Rotate the bitmap to correct the orientation
                Matrix matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.postRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.postRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.postRotate(270);
                        break;
                    default:
                        break;
                }
                rotatedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);
                if (rotatedBitmap.getWidth() * 3 / 4 >= rotatedBitmap.getHeight()) {
                    aspectRatio = "landscape";
                } else if (rotatedBitmap.getWidth() <= rotatedBitmap.getHeight() * 3 / 4) {
                    aspectRatio = "portrait";
                } else {
                    aspectRatio = "square";
                }
            }
        }

        setScreenRotation();

        setContentView(R.layout.activity_drawing);

        mDrawingView = findViewById(R.id.drawing_view);
        mCurrentColor = Color.BLUE;
        mDrawingView.setPaintColor(mCurrentColor);
        seekWidth = findViewById(R.id.seek_width);
        mCurrentStroke = seekWidth.getProgress();
        mDrawingView.setPaintStrokeWidth(mCurrentStroke);

        circleView = findViewById(R.id.circle_pen);
        circleView.setColor(mCurrentColor);
        circleView.setRadius(mCurrentStroke/2f);


        ConstraintLayout.LayoutParams loParam = (ConstraintLayout.LayoutParams) mDrawingView.getLayoutParams();
        if (aspectRatio.equals("landscape")) {
            loParam.dimensionRatio = "4:3";
        } else if (aspectRatio.equals("portrait")) {
            loParam.dimensionRatio = "3:4";
        } else {
            loParam.dimensionRatio = "1:1";
        }

        if (sketchId >= 0) {
            if (mCurrentSketch.getImgPreview() != null) {
                mDrawingView.setmBaseBitmap(mCurrentSketch.getImgPreview());
            }
        }

        initButtons();

        if (sketchId == -2) {
            mDrawingView.setmBaseBitmap(rotatedBitmap);
        }

    }

    protected void initButtons() {

        FloatingActionButton paletteButton = findViewById(R.id.fab_color);
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
                circleView.setRadius(mCurrentStroke/2f);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        FloatingActionButton undoButton = findViewById(R.id.fab_undo);
        undoButton.setOnClickListener(view -> mDrawingView.undo());

        FloatingActionButton redoButton = findViewById(R.id.fab_redo);
        redoButton.setOnClickListener(view -> mDrawingView.redo());

        FloatingActionButton saveButton = findViewById(R.id.fab_save);
        saveButton.setOnClickListener(view -> {
            saveSketch();
            gotoMainActivity();
        });

        FloatingActionButton deleteButton = findViewById(R.id.fab_delete);
        deleteButton.setOnClickListener(view -> {
            if (mCurrentSketch.getId() >= 0) {
                db.deleteSketch(mCurrentSketch.getId());
            }
            gotoMainActivity();
        });

        FloatingActionButton sdButton = findViewById(R.id.fab_stable_diffusion);
        sdButton.setOnClickListener(view -> showInputDialog());
    }

    public void gotoMainActivity() {
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

        if (aspectRatio.equals("portrait")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else if (aspectRatio.equals("landscape")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_prompt_mode, null);
        builder.setView(dialogView);

        final EditText editText = dialogView.findViewById(R.id.sd_prompt);
        editText.setText(mCurrentSketch.getPrompt());

        Spinner sdMode = dialogView.findViewById(R.id.sd_mode_selection);
        switch (mCurrentSketch.getCnMode()) {
            case Sketch.CN_MODE_DEPTH:
                sdMode.setSelection(1);
                break;
            case Sketch.CN_MODE_POSE:
                sdMode.setSelection(2);
                break;
            case Sketch.CN_MODE_TXT_CANNY:
                sdMode.setSelection(3);
                break;
            case Sketch.CN_MODE_TXT_SCRIBBLE:
                sdMode.setSelection(4);
                break;
            case Sketch.CN_MODE_TXT_DEPTH:
                sdMode.setSelection(5);
                break;
            default:
                sdMode.setSelection(0);
                break;
        }

        builder.setPositiveButton("OK", (dialog, which) -> {
            String inputText = editText.getText().toString();
            mCurrentSketch.setPrompt(inputText);
            switch (sdMode.getSelectedItemPosition()) {
                case 0:
                    mCurrentSketch.setCnMode(Sketch.CN_MODE_SCRIBBLE);
                    break;
                case 1:
                    mCurrentSketch.setCnMode(Sketch.CN_MODE_DEPTH);
                    break;
                case 2:
                    mCurrentSketch.setCnMode(Sketch.CN_MODE_POSE);
                    break;
                case 3:
                    mCurrentSketch.setCnMode(Sketch.CN_MODE_TXT_CANNY);
                    break;
                case 4:
                    mCurrentSketch.setCnMode(Sketch.CN_MODE_TXT_SCRIBBLE);
                    break;
                case 5:
                    mCurrentSketch.setCnMode(Sketch.CN_MODE_TXT_DEPTH);
                    break;
            }
            saveSketch();
            gotoViewSdImageActivity(mCurrentSketch.getId(), mCurrentSketch.getCnMode());
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    ActivityResultLauncher<Intent> sdViewerActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                }
            });

    public void gotoViewSdImageActivity(int sketchID, String cnMode) {
        Intent intent = new Intent(DrawingActivity.this, ViewSdImageActivity.class);
        intent.putExtra("sketchId", sketchID);
        intent.putExtra("cnMode", cnMode);
        sdViewerActivityResultLauncher.launch(intent);
    }

    public void saveSketch()  {
        mCurrentSketch = mDrawingView.getSketch(mCurrentSketch);
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
    public void onDialogDismissed(int dialogId) {

    }
}