package com.jsoft.diffusionpaint;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.jsoft.diffusionpaint.component.DrawingView;
import com.jsoft.diffusionpaint.helper.CircleView;
import com.jsoft.diffusionpaint.helper.PaintDb;
import com.jsoft.diffusionpaint.helper.Sketch;

public class DrawingActivity extends AppCompatActivity implements ColorPickerDialogListener
{
    private DrawingView mDrawingView;
    private CircleView circleView;
    private int mCurrentColor;
    private int mCurrentStroke;
    private PaintDb db;
    private Sketch mCurrentSketch;
    private SeekBar seekWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawing);

        db = new PaintDb(this);

        mDrawingView = findViewById(R.id.drawing_view);
        mCurrentColor = Color.BLUE;
        mDrawingView.setPaintColor(mCurrentColor);
        seekWidth = findViewById(R.id.seek_width);
        mCurrentStroke = seekWidth.getProgress();
        mDrawingView.setPaintStrokeWidth(mCurrentStroke);
        mCurrentSketch = new Sketch();
        circleView = findViewById(R.id.circle_pen);
        circleView.setColor(mCurrentColor);
        circleView.setRadius(mCurrentStroke/2);

        Intent i = getIntent();
        int sketchId = i.getIntExtra("sketchId", -1);
        if (sketchId >= 0) {
            Sketch dbSketch = db.getSketch(sketchId);
            if (dbSketch != null) {
                mCurrentSketch = dbSketch;
                mDrawingView.setmBaseBitmap(mCurrentSketch.getImgPreview());
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
                circleView.setRadius(mCurrentStroke/2);
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

    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_textbox, null);
        builder.setView(dialogView);

        final EditText editText = dialogView.findViewById(R.id.edit_text);
        editText.setText(mCurrentSketch.getPrompt());

        builder.setPositiveButton("OK", (dialog, which) -> {
            String inputText = editText.getText().toString();
            mCurrentSketch.setPrompt(inputText);
            saveSketch();
            gotoViewSdImageActivity(mCurrentSketch.getId());
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

    public void gotoViewSdImageActivity(int sketchID) {
        Intent intent = new Intent(DrawingActivity.this, ViewSdImageActivity.class);
        intent.putExtra("sketchId", sketchID);
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