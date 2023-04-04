package com.jsoft.diffusionpaint;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jsoft.diffusionpaint.helper.PaintDb;
import com.jsoft.diffusionpaint.helper.SdApiHelper;
import com.jsoft.diffusionpaint.helper.SdApiResponseListener;
import com.jsoft.diffusionpaint.helper.Sketch;
import com.jsoft.diffusionpaint.helper.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ViewSdImageActivity extends AppCompatActivity implements SdApiResponseListener {

    private Sketch mCurrentSketch;
    private PaintDb db;
    private ImageView sdImage;
    private LinearLayout spinner_bg;
    private FloatingActionButton sdButton;
    private FloatingActionButton saveButton;
    private FloatingActionButton expandButton;
    private FloatingActionButton dflButton;
    private FloatingActionButton editButton;
    private FloatingActionButton backButton;
    private Bitmap mBitmap;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault());
    private SharedPreferences sharedPreferences;
    private String aspectRatio;
    private SdApiHelper sdApiHelper;
    private boolean isCallingSD = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Intent i = getIntent();
        int sketchId = i.getIntExtra("sketchId", -1);
        String cnMode = i.getStringExtra("cnMode");
        this.sdApiHelper = new SdApiHelper(this, this);

        db = new PaintDb(this);

        if (sketchId >= 0) {
            Sketch dbSketch = db.getSketch(sketchId);
            if (dbSketch != null) {
                mCurrentSketch = dbSketch;
                mCurrentSketch.setCnMode(cnMode);
                mBitmap = mCurrentSketch.getImgPreview();
                aspectRatio = Utils.getAspectRatio(mCurrentSketch.getImgPreview());
            }
        } else if (sketchId == -3) {
            mCurrentSketch=new Sketch();
            mCurrentSketch.setPrompt(i.getStringExtra("prompt"));
            mCurrentSketch.setCnMode(Sketch.CN_MODE_TXT);
            mCurrentSketch.setId(-3);
            aspectRatio = sharedPreferences.getString("sdImageAspect", Sketch.ASPECT_RATIO_SQUARE);
        }
        if (mCurrentSketch==null) {
            mCurrentSketch=new Sketch();
        }
        setScreenRotation();

        setContentView(R.layout.activity_view_sd_image);
        sdImage = findViewById(R.id.sd_image);
        spinner_bg = findViewById(R.id.spinner_bg);
        sdButton = findViewById(R.id.fab_stable_diffusion2);
        saveButton = findViewById(R.id.fab_save2);
        backButton = findViewById(R.id.fab_back);
        expandButton = findViewById(R.id.fab_expand);
        editButton = findViewById(R.id.fab_paint_again);
        dflButton = findViewById(R.id.fab_dfl);


        if (mBitmap != null) {
            sdImage.setImageBitmap(mBitmap);
        }

        sdButton.setOnClickListener(view -> {
            if (!cnMode.equals(Sketch.CN_MODE_ORIGIN)) getSdConfig(mCurrentSketch.getCnMode());
        });

        backButton.setOnClickListener(view -> this.onBackPressed());

        saveButton.setOnClickListener(view -> {
            String filename = "sdsketch_" + (mCurrentSketch.getId()>=0?(mCurrentSketch.getId() + "_"):"") + dateFormat.format(new Date()) + ".jpg";
            Utils.saveBitmapToExternalStorage(this,mBitmap,filename);
            saveButton.setVisibility(View.GONE);
        });

        expandButton.setOnClickListener(view -> {
            if (Math.max(mBitmap.getHeight(), mBitmap.getWidth()) <= 1024) {
                JSONObject jsonObject = sdApiHelper.getExtraSingleImageJSON(mBitmap);
                showSpinner();
                isCallingSD = true;
                sdApiHelper.sendPostRequest("extraSingleImage", "/sdapi/v1/extra-single-image", jsonObject);
            }
        });

        dflButton.setOnClickListener(view -> {
            JSONObject jsonObject = sdApiHelper.getDflJSON(mBitmap);
            showSpinner();
            isCallingSD = true;
            sdApiHelper.sendRequest("deepFaceLab", "http://jordentse.asuscomm.com:25000", "/processimage", jsonObject, "POST");
        });

        editButton.setOnClickListener(view -> {
            showSpinner();
            if (mCurrentSketch.getImgInpaint() != null && cnMode.startsWith("inpaint")) {
                Bitmap bmEdit = Bitmap.createBitmap(mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvasEdit = new Canvas(bmEdit);
                canvasEdit.drawBitmap(mBitmap, null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
                Bitmap bmMask = Utils.getDilationMask(mCurrentSketch.getImgPaint(), 20);
                for (int x = 0; x < bmEdit.getWidth(); x++)
                    for (int y = 0; y < bmEdit.getHeight(); y++) {
                        if (bmMask.getPixel(x, y) == Color.BLACK)
                            bmEdit.setPixel(x, y, mCurrentSketch.getImgBackground().getPixel(x, y));
                    }
                mBitmap = bmEdit;
            }

            String fileName = "sdsketch_" + (mCurrentSketch.getId()>=0?(mCurrentSketch.getId() + "_"):"") + dateFormat.format(new Date()) + ".jpg";
            Utils.saveBitmapToExternalStorage(this,mBitmap,fileName);
            File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File sdSketchFolder = new File(picturesDirectory, "sdSketch");
            File file = new File(sdSketchFolder, fileName);
            Intent intent = new Intent(ViewSdImageActivity.this, DrawingActivity.class);
            intent.putExtra("sketchId", -2);
            intent.putExtra("bitmapPath", file.getAbsolutePath());
            intent.putExtra("prompt", mCurrentSketch.getPrompt());
            drawingActivityResultLauncher.launch(intent);
        });

        if (cnMode.equals(Sketch.CN_MODE_ORIGIN)) {
            hideSpinner();
        } else {
            getSdConfig(mCurrentSketch.getCnMode());
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Lock the orientation to portrait
        setScreenRotation();
    }

    @Override
    public void onBackPressed() {
        if (isCallingSD) {
            sdApiHelper.sendPostRequest("interrupt", "/sdapi/v1/interrupt", new JSONObject());
            isCallingSD = false;
        }
        super.onBackPressed();
    }

    public void setScreenRotation() {
        if (aspectRatio.equals(Sketch.ASPECT_RATIO_PORTRAIT)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else if (aspectRatio.equals(Sketch.ASPECT_RATIO_LANDSCAPE)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private void getSdConfig(String cnMode) {
        showSpinner();
        sdApiHelper.sendGetRequest("getConfig", "/sdapi/v1/options");
    }

    private void showSpinner() {
        spinner_bg.setVisibility(View.VISIBLE);
        sdButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        expandButton.setVisibility(View.GONE);
        dflButton.setVisibility(View.GONE);
        editButton.setVisibility(View.GONE);
    }

    private void hideSpinner() {
        spinner_bg.setVisibility(View.GONE);
        sdButton.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);
        expandButton.setVisibility(View.VISIBLE);
        dflButton.setVisibility(View.VISIBLE);
        editButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSdApiFailure(String requestType) {
        isCallingSD = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Request Type: " + requestType)
                .setTitle("Call Stable Diffusion API failed")
                .setPositiveButton("OK", (dialog, id) -> ViewSdImageActivity.this.onBackPressed());
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void callSD4Img() {
        isCallingSD = true;
        String cnMode = mCurrentSketch.getCnMode();
        if (cnMode.startsWith("txt")) {
            JSONObject jsonObject = sdApiHelper.getControlnetTxt2imgJSON(mCurrentSketch.getPrompt(), cnMode, mCurrentSketch, aspectRatio);
            sdApiHelper.sendPostRequest("txt2img", "/sdapi/v1/txt2img", jsonObject);
        } else {
            JSONObject jsonObject = sdApiHelper.getControlnetImg2imgJSON(mCurrentSketch.getPrompt(), cnMode, mCurrentSketch, aspectRatio);
            sdApiHelper.sendPostRequest("img2img", "/sdapi/v1/img2img", jsonObject);
        }
    }

    ActivityResultLauncher<Intent> drawingActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {});

    @Override
    public void onSdApiResponse(String requestType, String responseBody) {
        try {
            if ("getConfig".equals(requestType)) {
                JSONObject getConfigResponse = new JSONObject(responseBody);
                String currentModel = getConfigResponse.getString("sd_model_checkpoint");
                String cnMode = mCurrentSketch.getCnMode();
                String preferredModel = cnMode.startsWith("inpaint")?
                        sharedPreferences.getString("sdInpaintModel", ""):
                        sharedPreferences.getString("sdModelCheckpoint", "");
                if (!currentModel.equals(preferredModel)) {
                    JSONObject setConfigRequest = new JSONObject();
                    setConfigRequest.put("sd_model_checkpoint", preferredModel);
                    sdApiHelper.sendPostRequest("setConfig", "/sdapi/v1/options", setConfigRequest);
                } else {
                    callSD4Img();
                }
            } else if ("setConfig".equals(requestType)) {
                callSD4Img();
            } else if ("img2img".equals(requestType) || "txt2img".equals(requestType)) {
                isCallingSD = false;
                JSONObject jsonObject = new JSONObject(responseBody);
                JSONArray images = jsonObject.getJSONArray("images");
                if (images.length() > 0) {
                    mBitmap = Utils.base64String2Bitmap((String) images.get(0));
                    sdImage.setImageBitmap(mBitmap);
                }
                hideSpinner();
            } else if ("extraSingleImage".equals(requestType)) {
                isCallingSD = false;
                JSONObject jsonObject = new JSONObject(responseBody);
                String imageStr = jsonObject.getString("image");
                mBitmap = Utils.base64String2Bitmap(imageStr);
                sdImage.setImageBitmap(mBitmap);
                hideSpinner();
            } else if ("deepFaceLab".equals(requestType)) {
                isCallingSD = false;
                JSONObject jsonObject = new JSONObject(responseBody);
                String imageStr = jsonObject.getString("processed_image");
                mBitmap = Utils.base64String2Bitmap(imageStr);
                sdImage.setImageBitmap(mBitmap);
                hideSpinner();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            onSdApiFailure(requestType);
        }
    }
}