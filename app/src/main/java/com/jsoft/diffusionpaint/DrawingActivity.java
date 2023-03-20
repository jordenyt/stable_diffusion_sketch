package com.jsoft.diffusionpaint;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DrawingActivity extends AppCompatActivity implements ColorPickerDialogListener
{
    private DrawingView mDrawingView;
    private CircleView circleView;
    private int mCurrentColor;
    private int mCurrentStroke;
    private PaintDb db;
    private Sketch mCurrentSketch;
    private SeekBar seekWidth;
    private File mImageFile;
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
        aspectRatio = sharedPreferences.getString("sdImageAspect", "square");
        if (sketchId >= 0) {
            Sketch dbSketch = db.getSketch(sketchId);
            if (dbSketch != null) {
                mCurrentSketch = dbSketch;
                if (mCurrentSketch.getImgPreview().getWidth() > mCurrentSketch.getImgPreview().getHeight()) {
                    aspectRatio = "landscape";
                } else if (mCurrentSketch.getImgPreview().getWidth() < mCurrentSketch.getImgPreview().getHeight()) {
                    aspectRatio = "portrait";
                } else {
                    aspectRatio = "square";
                }
            }
        } else if (sketchId == -2) {
            mCurrentSketch.setId(sketchId);
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
        } else if (sketchId == -2) {
            mImageFile = new File(getExternalFilesDir(null), "captured_image.jpg");
            // Launch the camera app to capture an image
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) != null) {
                // Get the URI for the saved image file using a FileProvider
                Uri imageUri = FileProvider.getUriForFile(this, "com.jsoft.diffusionpaint.fileprovider", mImageFile);

                // Add the URI to the intent as an extra
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

                // Grant permission to the receiving app to access the saved image file
                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, imageUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                cameraResultLauncher.launch(intent);
            }
        }

        initButtons();
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
            this.onBackPressed();
        });

        FloatingActionButton deleteButton = findViewById(R.id.fab_delete);
        deleteButton.setOnClickListener(view -> {
            if (mCurrentSketch.getId() >= 0) {
                db.deleteSketch(mCurrentSketch.getId());
            }
            this.onBackPressed();
        });

        FloatingActionButton sdButton = findViewById(R.id.fab_stable_diffusion);
        sdButton.setOnClickListener(view -> showInputDialog());
    }

    ActivityResultLauncher<Intent> cameraResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Bitmap imageBitmap = BitmapFactory.decodeFile(mImageFile.getAbsolutePath());

                    ExifInterface exif = null;
                    try {
                        exif = new ExifInterface(mImageFile.getAbsolutePath());
                    } catch (IOException e) {
                        Log.e("diffusionpaint", "IOException get from returned camera file.");
                    }
                    assert exif != null;
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

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
                    Bitmap rotatedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);
                    //mCurrentSketch = dbSketch;
                    mDrawingView.setmBaseBitmap(rotatedBitmap);
                }
            });

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Lock the orientation to portrait
        setScreenRotation();
    }

    public void setScreenRotation() {
        if (aspectRatio.equals("portrait")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (aspectRatio.equals("landscape")) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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
            default:
                sdMode.setSelection(0);
                break;
        }

        builder.setPositiveButton("OK", (dialog, which) -> {
            String inputText = editText.getText().toString();
            mCurrentSketch.setPrompt(inputText);
            switch (sdMode.getSelectedItem().toString()) {
                case "Scribble":
                    mCurrentSketch.setCnMode(Sketch.CN_MODE_SCRIBBLE);
                    break;
                case "Depth":
                    mCurrentSketch.setCnMode(Sketch.CN_MODE_DEPTH);
                    break;
                case "Pose":
                    mCurrentSketch.setCnMode(Sketch.CN_MODE_POSE);
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