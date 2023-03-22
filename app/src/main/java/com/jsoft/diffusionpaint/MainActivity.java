package com.jsoft.diffusionpaint;

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
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jsoft.diffusionpaint.adapter.GridViewImageAdapter;
import com.jsoft.diffusionpaint.helper.AppConstant;
import com.jsoft.diffusionpaint.helper.PaintDb;
import com.jsoft.diffusionpaint.helper.Sketch;
import com.jsoft.diffusionpaint.helper.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private PaintDb db;
    private GridView gridView;
    private Utils utils;
    private List<Sketch> sketches;
    private SharedPreferences sharedPreferences;
    private static File mImageFile;

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

        isPermissionGranted();

        Intent intent = getIntent();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            String mimeType = getContentResolver().getType(uri);
            if (mimeType != null && mimeType.startsWith("image/")) {
                String filePath = getPathFromUri(uri);
                if (uri.getScheme().equals("content") && filePath == null) {
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        String fileName = "temp_file_" + System.currentTimeMillis();
                        File tempFile = new File(getCacheDir(), fileName);
                        FileOutputStream outputStream = new FileOutputStream(tempFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        outputStream.flush();
                        outputStream.close();
                        inputStream.close();
                        filePath = tempFile.getAbsolutePath();
                    } catch (IOException e) {
                        Log.e("diffusionPaint", "Cannot get Image Path from shared content.");
                    }
                }
                if (filePath != null) {
                    Intent drawIntent = new Intent(MainActivity.this, DrawingActivity.class);
                    drawIntent.putExtra("sketchId", -2);
                    drawIntent.putExtra("bitmapPath", filePath);
                    drawingActivityResultLauncher.launch(drawIntent);
                }
            }
        }
    }

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            try {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    String path = cursor.getString(column_index);
                    cursor.close();
                    return path;
                }
            } catch (IllegalArgumentException e) {
                cursor.close();
                return null;
            }
            cursor.close();
        }
        return null;
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
        sketches = db.getSketchList();
        GridViewImageAdapter adapter = new GridViewImageAdapter(this, sketches);
        gridView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        finish();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.mi_sd_server_address:
                showTextInputDialog("sdServerAddress", "Stable Diffusion API Server Address:", "http://192.168.1.101:7860", "");
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
            case R.id.mi_cn_scribble:
                showTextInputDialog("cnScribbleModel", "ControlNet Scribble Model:", "control_sd15_scribble [fef5e48e]", "control_sd15_scribble [fef5e48e]");
                break;
            case R.id.mi_cn_depth:
                showTextInputDialog("cnDepthModel", "ControlNet Depth Model:", "control_sd15_depth [fef5e48e]", "control_sd15_depth [fef5e48e]");
                break;
            case R.id.mi_cn_pose:
                showTextInputDialog("cnPoseModel", "ControlNet Pose Model:", "control_sd15_openpose [fef5e48e]", "control_sd15_openpose [fef5e48e]");
                break;
            case R.id.mi_cn_canny:
                showTextInputDialog("cnCannyModel", "ControlNet Canny Model:", "control_sd15_canny [fef5e48e]", "control_sd15_canny [fef5e48e]");
                break;
            case R.id.mi_sd_output_dim:
                showOutputDimenDialog();
                break;
            default:
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
        int prefSize = sharedPreferences.getInt("sdImageSize", 512);
        spSize.setSelection(prefSize == 768 ? 1 : prefSize == 1024 ? 2 : 0);

        Spinner spAspect = dialogView.findViewById(R.id.sd_aspect_selection);
        String prefAspect = sharedPreferences.getString("sdImageAspect", "square");
        spAspect.setSelection(prefAspect.equals("landscape") ? 2 : prefAspect.equals("portrait") ? 1 : 0);

        builder.setPositiveButton("OK", (dialog, which) -> {
            int ipSize = (spSize.getSelectedItemPosition() == 1 ? 768
                    : spSize.getSelectedItemPosition() == 2 ? 1024
                    : 512);
            String ipAspect = (spAspect.getSelectedItemPosition() == 1 ? "portrait"
                    : spAspect.getSelectedItemPosition() == 2 ? "landscape"
                    : "square");
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

}