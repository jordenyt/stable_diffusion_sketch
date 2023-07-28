package com.jsoft.diffusionpaint;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jsoft.diffusionpaint.component.TouchImageView;
import com.jsoft.diffusionpaint.dto.ApiResult;
import com.jsoft.diffusionpaint.helper.PaintDb;
import com.jsoft.diffusionpaint.helper.SdApiHelper;
import com.jsoft.diffusionpaint.helper.SdApiResponseListener;
import com.jsoft.diffusionpaint.dto.SdParam;
import com.jsoft.diffusionpaint.dto.Sketch;
import com.jsoft.diffusionpaint.helper.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ViewSdImageActivity extends AppCompatActivity implements SdApiResponseListener {

    private static Sketch mCurrentSketch;
    private TouchImageView sdImage;
    private LinearLayout spinner_bg;
    private FloatingActionButton sdButton;
    private FloatingActionButton saveButton;
    private FloatingActionButton expandButton;
    private FloatingActionButton editButton;
    private FloatingActionButton prevButton;
    private FloatingActionButton nextButton;
    private FloatingActionButton backButton;
    public static Bitmap mBitmap = null;
    public static Bitmap inpaintBitmap = null;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault());
    private SharedPreferences sharedPreferences;
    //private String aspectRatio;
    @SuppressLint("StaticFieldLeak")
    private static SdApiHelper sdApiHelper;
    private boolean isCallingSD = false;
    private static String savedImageName = null;
    public static boolean isCallingAPI = false;
    public static List<ApiResult> apiResultList;
    public static int currentResult;
    public static Map<String, String> sdModelList = null;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        boolean isFirstCall = (mBitmap == null && !isCallingAPI);
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        Intent i = getIntent();
        int sketchId = i.getIntExtra("sketchId", -1);
        String cnMode = i.getStringExtra("cnMode");
        if (sdApiHelper == null) {
            sdApiHelper = new SdApiHelper(this, this);
        } else {
            sdApiHelper.setActivity(this);
            sdApiHelper.setListener(this);
        }
        PaintDb db = new PaintDb(this);

        if (isFirstCall) {
            if (sketchId >= 0) {
                Sketch dbSketch = db.getSketch(sketchId);
                if (dbSketch != null) {
                    mCurrentSketch = dbSketch;
                    mCurrentSketch.setCnMode(cnMode);
                    mBitmap = mCurrentSketch.getImgPreview();
                }
            } else if (sketchId == -3) {
                mCurrentSketch = new Sketch();
                mCurrentSketch.setPrompt(i.getStringExtra("prompt"));
                mCurrentSketch.setNegPrompt(i.getStringExtra("negPrompt"));
                mCurrentSketch.setCnMode(i.getStringExtra("cnMode"));
                mCurrentSketch.setId(-3);
            }
            if (mCurrentSketch == null) {
                mCurrentSketch = new Sketch();
            }
        }

        initUI(cnMode);

        if (isFirstCall) {
            apiResultList = new ArrayList<>();
            currentResult = 0;
            savedImageName = null;
            if (cnMode.equals(Sketch.CN_MODE_ORIGIN)) {
                mBitmap = mCurrentSketch.getImgBgRef();
                sdImage.setImageBitmap(mBitmap);
                addResult("original");
                hideSpinner();
            } else {
                if (cnMode.startsWith(Sketch.CN_MODE_OUTPAINT_V) || cnMode.startsWith(Sketch.CN_MODE_OUTPAINT_H)) {
                    SdParam param = sdApiHelper.getSdCnParm(mCurrentSketch.getCnMode());
                    mCurrentSketch.setImgBackground(Utils.getOutpaintBmp(mCurrentSketch.getImgBackground(), cnMode, Color.BLACK, false, param.sdSize));
                    mCurrentSketch.setImgPreview(Utils.getOutpaintBmp(mCurrentSketch.getImgPreview(), cnMode, Color.BLUE, false, param.sdSize));
                    mCurrentSketch.setImgPaint(Utils.getOutpaintBmp(mCurrentSketch.getImgPaint(), cnMode, Color.BLACK, true, param.sdSize));
                    mBitmap = mCurrentSketch.getImgPreview();
                    sdImage.setImageBitmap(mBitmap);
                } else if (cnMode.equals(Sketch.CN_MODE_MERGE)) {
                    mCurrentSketch.setImgBackground(mCurrentSketch.getImgBgRef());
                    mCurrentSketch.setImgPaint(mCurrentSketch.getImgBgRefPaint(32));
                    mCurrentSketch.setImgPreview(mCurrentSketch.getImgBgRefPreview());
                    mBitmap = mCurrentSketch.getImgPreview();
                    sdImage.setImageBitmap(mBitmap);
                }
                getSdModel();
            }
        } else {
            if (isCallingAPI) { showSpinner();}
            else { hideSpinner();}
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initUI(String cnMode) {
        setContentView(R.layout.activity_view_sd_image);
        sdImage = findViewById(R.id.sd_image);
        spinner_bg = findViewById(R.id.spinner_bg);
        sdButton = findViewById(R.id.fab_stable_diffusion2);
        saveButton = findViewById(R.id.fab_save2);
        backButton = findViewById(R.id.fab_back);
        prevButton = findViewById(R.id.fab_prev);
        nextButton = findViewById(R.id.fab_next);
        expandButton = findViewById(R.id.fab_expand);
        editButton = findViewById(R.id.fab_paint_again);

        if (mBitmap != null) {
            sdImage.setImageBitmap(mBitmap);
        }

        sdButton.setOnClickListener(view -> {
            if (!cnMode.equals(Sketch.CN_MODE_ORIGIN)) getSdModel();
        });

        spinner_bg.setOnTouchListener((v, event) -> true);

        backButton.setOnClickListener(view -> this.onBackPressed());

        saveButton.setOnClickListener(view -> {
            showSpinner();
            CompletableFuture.supplyAsync(() -> {
                saveImage();
                return "";
            }).thenRun(() -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show();
                    hideSpinner();
                    saveButton.setVisibility(View.GONE);
                });
            });
        });

        expandButton.setOnClickListener(view -> {
            JSONObject jsonObject;
            SdParam param = sdApiHelper.getSdCnParm(mCurrentSketch.getCnMode());
            if (param.type.equals(SdParam.SD_MODE_TYPE_INPAINT) && inpaintBitmap != null) {
                jsonObject = sdApiHelper.getExtraSingleImageJSON(inpaintBitmap);
            } else {
                jsonObject = sdApiHelper.getExtraSingleImageJSON(mBitmap);
            }
            showSpinner();
            isCallingSD = true;
            sdApiHelper.sendPostRequest("extraSingleImage", "/sdapi/v1/extra-single-image", jsonObject);
        });

        editButton.setOnClickListener(view -> {
            showSpinner();
            CompletableFuture.supplyAsync(() -> {
                saveImage();
                return "";
            }).thenRun(() -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show();
                    File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    File sdSketchFolder = new File(picturesDirectory, "sdSketch");
                    File file = new File(sdSketchFolder, savedImageName);
                    Intent intent = new Intent(ViewSdImageActivity.this, DrawingActivity.class);
                    intent.putExtra("sketchId", -2);
                    intent.putExtra("bitmapPath", file.getAbsolutePath());
                    intent.putExtra("prompt", mCurrentSketch.getPrompt());
                    intent.putExtra("negPrompt", mCurrentSketch.getNegPrompt());
                    if (mCurrentSketch.getId() >= 0) {
                        intent.putExtra("parentId", mCurrentSketch.getId());
                    }
                    drawingActivityResultLauncher.launch(intent);
                });
            });
        });

        prevButton.setOnClickListener(view -> {
            if (currentResult > 0) {
                currentResult -= 1;
                changeResult();
            }
        });

        nextButton.setOnClickListener(view -> {
            if (currentResult < apiResultList.size() - 1) {
                currentResult += 1;
                changeResult();
            }
        });
    }

    private void changeResult() {
        ApiResult r = apiResultList.get(currentResult);
        savedImageName = r.savedImageName;
        mBitmap = r.mBitmap;
        inpaintBitmap = r.inpaintBitmap;
        saveButton.setVisibility((savedImageName != null)?View.GONE:View.VISIBLE);
        sdImage.resetView();
        sdImage.setImageBitmap(mBitmap);
    }

    private void saveImage() {
        if (savedImageName==null) {
            savedImageName = "sdsketch_" + (mCurrentSketch.getId() >= 0 ? (mCurrentSketch.getId() + "_") : "") + dateFormat.format(new Date()) + ".jpg";
            Utils.saveBitmapToExternalStorage(this, mBitmap, savedImageName);
            apiResultList.get(currentResult).savedImageName = savedImageName;
        }
    }

    @Override
    public void onBackPressed() {
        if (isCallingSD) {
            sdApiHelper.sendPostRequest("interrupt", "/sdapi/v1/interrupt", new JSONObject());
            isCallingSD = false;
        }
        isCallingAPI = false;
        super.onBackPressed();
    }

    private void getSdModel() {
        showSpinner();
        if (sdModelList == null) {
            sdApiHelper.sendGetRequest("getSDModel", "/sdapi/v1/sd-models");
        } else {
            try {
                setSdModel();
            } catch (JSONException e) {
                onSdApiFailure("setConfig", e.getMessage());
            }
        }
    }

    private void setSdModel() throws JSONException {
        SdParam param = sdApiHelper.getSdCnParm(mCurrentSketch.getCnMode());
        String preferredModel = param.model.equals(SdParam.SD_MODEL_INPAINT) ? sharedPreferences.getString("sdInpaintModel", ""):
                param.model.equals(SdParam.SD_MODEL_SDXL_BASE) ? sharedPreferences.getString("sdxlBaseModel", ""):
                param.model.equals(SdParam.SD_MODEL_SDXL_REFINER) ? sharedPreferences.getString("sdxlRefinerModel", ""):
                sharedPreferences.getString("sdModelCheckpoint", "");
        JSONObject setConfigRequest = new JSONObject();
        if (sdModelList !=null && sdModelList.get(preferredModel) != null) {
            setConfigRequest.put("sd_model_checkpoint", preferredModel);
            setConfigRequest.put("sd_checkpoint_hash", sdModelList.get(preferredModel));
            sdApiHelper.sendPostRequest("setSdModel", "/sdapi/v1/options", setConfigRequest);
        } else {
            callSD4Img();
        }
    }

    private void showSpinner() {
        isCallingAPI = true;
        spinner_bg.setVisibility(View.VISIBLE);
        sdButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        prevButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        expandButton.setVisibility(View.GONE);
        editButton.setVisibility(View.GONE);
    }

    private void hideSpinner() {
        isCallingAPI = false;
        spinner_bg.setVisibility(View.GONE);
        sdButton.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);
        prevButton.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);
        expandButton.setVisibility(View.VISIBLE);
        editButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSdApiFailure(String requestType, String errMessage) {
        isCallingSD = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Request Type: " + requestType)
                .setTitle("Call Stable Diffusion API failed (" + requestType + ")")
                .setMessage(errMessage)
                .setPositiveButton("OK", (dialog, id) -> {
                    hideSpinner();
                    //ViewSdImageActivity.this.onBackPressed();
                });
        AlertDialog alert = builder.create();
        if(!isFinishing()) alert.show();
    }

    public void callSD4Img() {
        isCallingSD = true;
        SdParam param = sdApiHelper.getSdCnParm(mCurrentSketch.getCnMode());
        String aspectRatio = (mCurrentSketch.getImgBackground() != null) ?
                Utils.getAspectRatio(mCurrentSketch.getImgBackground()) : sharedPreferences.getString("sdImageAspect", Sketch.ASPECT_RATIO_SQUARE);
        if (param.type.equals(SdParam.SD_MODE_TYPE_TXT2IMG)) {
            JSONObject jsonObject = sdApiHelper.getControlnetTxt2imgJSON(param, mCurrentSketch, aspectRatio);
            sdApiHelper.sendPostRequest("txt2img", "/sdapi/v1/txt2img", jsonObject);
        } else {
            JSONObject jsonObject = sdApiHelper.getControlnetImg2imgJSON(param, mCurrentSketch, aspectRatio);
            sdApiHelper.sendPostRequest("img2img", "/sdapi/v1/img2img", jsonObject);
        }
    }

    ActivityResultLauncher<Intent> drawingActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {});

    @Override
    public void onSdApiResponse(String requestType, String responseBody) {
        try {
            if ("getSDModel".equals(requestType)) {
                JSONArray jsonArray = new JSONArray(responseBody);
                sdModelList = new HashMap<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject joModel = jsonArray.getJSONObject(i);
                    sdModelList.put(joModel.getString("title"), joModel.getString("sha256"));
                }
                setSdModel();
            } else if ("setSdModel".equals(requestType)) {
                callSD4Img();
            } else if ("img2img".equals(requestType) || "txt2img".equals(requestType)) {
                isCallingSD = false;
                JSONObject jsonObject = new JSONObject(responseBody);
                JSONArray images = jsonObject.getJSONArray("images");
                if (images.length() > 0) {
                    mBitmap = Utils.base64String2Bitmap((String) images.get(0));
                    if ("img2img".equals(requestType)) {
                        updateMBitmap();
                    }
                    sdImage.resetView();
                    sdImage.setImageBitmap(mBitmap);
                }
                savedImageName = null;
                addResult(requestType);
                hideSpinner();
            } else if ("extraSingleImage".equals(requestType)) {
                isCallingSD = false;
                JSONObject jsonObject = new JSONObject(responseBody);
                String imageStr = jsonObject.getString("image");
                mBitmap = Utils.base64String2Bitmap(imageStr);
                updateMBitmap();
                sdImage.resetView();
                sdImage.setImageBitmap(mBitmap);
                savedImageName = null;
                addResult(requestType);
                hideSpinner();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            onSdApiFailure(requestType, e.getMessage());
        }
    }

    private void addResult(String requestType) {
        ApiResult r = new ApiResult();
        r.requestType = requestType;
        r.mBitmap = mBitmap.copy(mBitmap.getConfig(), true);
        if (inpaintBitmap != null) {
            r.inpaintBitmap = inpaintBitmap.copy(inpaintBitmap.getConfig(), true);
        }
        //r.savedImageName = savedImageName;
        apiResultList.add(r);
        currentResult = apiResultList.size() - 1;
    }

    private void updateMBitmap() {
        SdParam param = sdApiHelper.getSdCnParm(mCurrentSketch.getCnMode());
        if (param.inpaintPartial == SdParam.INPAINT_PARTIAL) {
            inpaintBitmap = mBitmap.copy(mBitmap.getConfig(), true);
            Bitmap bmEdit = Bitmap.createBitmap(mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvasEdit = new Canvas(bmEdit);
            canvasEdit.drawBitmap(mCurrentSketch.getImgBackground(), null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
            canvasEdit.drawBitmap(mBitmap, null, mCurrentSketch.getRectInpaint(param.sdSize), null);
            int boundary = (int)Math.round(Math.max(mCurrentSketch.getImgPaint().getWidth(), mCurrentSketch.getImgPaint().getHeight()) / 50d);
            mBitmap = mCurrentSketch.getImgBgMerge(bmEdit, boundary);
        } else if (param.type.equals(SdParam.SD_MODE_TYPE_INPAINT)) {
            inpaintBitmap = mBitmap.copy(mBitmap.getConfig(), true);
            int boundary = (int)Math.round(Math.max(mCurrentSketch.getImgPaint().getWidth(), mCurrentSketch.getImgPaint().getHeight()) / 50d);
            mBitmap = mCurrentSketch.getImgBgMerge(inpaintBitmap, boundary);
        }
    }
}