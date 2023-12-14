package com.jsoft.diffusionpaint;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import okhttp3.OkHttpClient;

public class ViewSdImageActivity extends AppCompatActivity implements SdApiResponseListener {

    public static final String CHANNEL_ID = "foreground_service_channel";
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
    private TextView txtCount;
    private TextView txtSdStatus;
    public static Bitmap mBitmap = null;
    public static Bitmap inpaintBitmap = null;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault());
    private SharedPreferences sharedPreferences;
    //private String aspectRatio;
    @SuppressLint("StaticFieldLeak")
    private static SdApiHelper sdApiHelper;
    public static boolean isCallingSD = false;
    public static String savedImageName = null;
    public static boolean isCallingAPI = false;
    private static List<ApiResult> apiResultList;
    private static int currentResult;
    private static Map<String, String> sdModelList = null;
    private static Handler handler;
    public static int remainGen = 0;
    private boolean isPaused = false;
    private ViewSdImageService mService;
    private boolean mBound = false;
    public static boolean isInterrupted = false;

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Foreground Service Channel",
                        NotificationManager.IMPORTANCE_DEFAULT);
                channel.setDescription("Channel for foreground service notifications");

                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
            apiResultList = null;
            currentResult = 0;
            remainGen = i.getIntExtra("numGen",1);
            if (sketchId >= 0) {
                Sketch dbSketch = db.getSketch(sketchId);
                if (dbSketch != null) {
                    mCurrentSketch = dbSketch;
                    mCurrentSketch.setCnMode(cnMode);
                    mBitmap = mCurrentSketch.getImgPreview();
                    SdParam param = sdApiHelper.getSdCnParm(mCurrentSketch.getCnMode());
                    if (param.type.equals(SdParam.SD_MODE_TYPE_INPAINT) && param.inpaintPartial == 1) {
                        Bitmap bmEdit = Bitmap.createBitmap(mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvasEdit = new Canvas(bmEdit);
                        canvasEdit.drawBitmap(mCurrentSketch.getImgPreview(), null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
                        Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setAlpha(128);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth((float)(15d * (double)mCurrentSketch.getImgBackground().getHeight() / (double)mCurrentSketch.getImgPreview().getHeight()));
                        canvasEdit.drawRect(mCurrentSketch.getRectInpaint(param.sdSize),paint);
                        mBitmap = bmEdit;
                    }
                }
            } else if (sketchId == -3) {
                mCurrentSketch = new Sketch();
                mCurrentSketch.setPrompt(i.getStringExtra("prompt"));
                mCurrentSketch.setNegPrompt(i.getStringExtra("negPrompt"));
                mCurrentSketch.setCnMode(i.getStringExtra("cnMode"));
                mCurrentSketch.setId(-3);
                String aspectRatio = sharedPreferences.getString("sdImageAspect", Sketch.ASPECT_RATIO_SQUARE);
                if (i.hasExtra("aspectRatio")) aspectRatio = i.getStringExtra("aspectRatio");
                switch (Objects.requireNonNull(aspectRatio)) {
                    case Sketch.ASPECT_RATIO_PORTRAIT:
                        mCurrentSketch.setImgBackground(Bitmap.createBitmap(30, 40, Bitmap.Config.ARGB_8888));
                        break;
                    case Sketch.ASPECT_RATIO_LANDSCAPE:
                        mCurrentSketch.setImgBackground(Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888));
                        break;
                    case Sketch.ASPECT_RATIO_WIDE:
                        mCurrentSketch.setImgBackground(Bitmap.createBitmap(80, 45, Bitmap.Config.ARGB_8888));
                        break;
                    default:
                        mCurrentSketch.setImgBackground(Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888));
                        break;
                }
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
                showSpinner();
                mBitmap = mCurrentSketch.getImgBgRef();
                sdImage.setImageBitmap(mBitmap);
                addResult("original");
                remainGen = 0;
                hideSpinner();
            } else {
                showSpinner();
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
                CompletableFuture.supplyAsync(() -> {
                    getSdModel();
                    return "";
                });
            }
        } else {
            if (isCallingAPI) { showSpinner();}
            else { hideSpinner();}
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initUI(String cnMode) {
        handler = new Handler();
        setContentView(R.layout.activity_view_sd_image);
        sdImage = findViewById(R.id.sd_image);
        txtCount = findViewById(R.id.txtCount);
        spinner_bg = findViewById(R.id.spinner_bg);
        sdButton = findViewById(R.id.fab_stable_diffusion2);
        saveButton = findViewById(R.id.fab_save2);
        backButton = findViewById(R.id.fab_back);
        prevButton = findViewById(R.id.fab_prev);
        nextButton = findViewById(R.id.fab_next);
        expandButton = findViewById(R.id.fab_expand);
        editButton = findViewById(R.id.fab_paint_again);
        txtSdStatus = findViewById(R.id.txtSdStatus);

        if (mBitmap != null) {
            sdImage.setImageBitmap(mBitmap);
        }
        sdImage.setActivity(this);
        txtCount.setText("");
        if (apiResultList != null && apiResultList.size() > 0) {
            txtCount.setText((currentResult + 1) + "/" + apiResultList.size());
        }
        sdButton.setOnClickListener(view -> {
            if (!cnMode.equals(Sketch.CN_MODE_ORIGIN)) callSD4Img();
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
                if (param.inpaintPartial == SdParam.INPAINT_PARTIAL) {
                    double scale = (double) mCurrentSketch.getRectInpaint(param.sdSize).height() / inpaintBitmap.getHeight();
                    jsonObject = sdApiHelper.getExtraSingleImageJSON(inpaintBitmap, scale);
                } else {
                    jsonObject = sdApiHelper.getExtraSingleImageJSON(inpaintBitmap);
                }
            } else {
                jsonObject = sdApiHelper.getExtraSingleImageJSON(mBitmap);
            }
            showSpinner();
            isInterrupted = false;
            if (mBound) {
                mService.setObject(sharedPreferences.getString("sdServerAddress", ""), jsonObject);
                Intent intent = new Intent(this, ViewSdImageService.class);
                intent.putExtra("requestType", "extraSingleImage");
                startService(intent);
            }
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
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                });
            });
        });

        prevButton.setOnClickListener(view -> {
            changeResult(-1);
        });

        nextButton.setOnClickListener(view -> {
            changeResult(1);
        });
    }

    public void changeResult(int change) {
        int newPosition = currentResult + change;
        if (newPosition < 0) {
            newPosition = 0;
        } else if (newPosition > apiResultList.size() - 1) {
            newPosition = apiResultList.size() - 1;
        }
        if (newPosition != currentResult) {
            currentResult = newPosition;
            ApiResult r = apiResultList.get(currentResult);
            savedImageName = r.savedImageName;
            mBitmap = r.mBitmap;
            inpaintBitmap = r.inpaintBitmap;
            saveButton.setVisibility((savedImageName != null) ? View.GONE : View.VISIBLE);
            sdImage.resetView();
            sdImage.setImageBitmap(mBitmap);
            txtCount.setText((currentResult + 1) + "/" + apiResultList.size());
        }
    }

    private void saveImage()  {
        if (savedImageName==null) {
            savedImageName = "sdsketch_" + (mCurrentSketch.getId() >= 0 ? (mCurrentSketch.getId() + "_") : "") + dateFormat.format(new Date()) + ".jpg";
            String exif = mCurrentSketch.getExif();
            SdParam param = sdApiHelper.getSdCnParm(mCurrentSketch.getCnMode());
            if (!mCurrentSketch.getCnMode().equals(Sketch.CN_MODE_ORIGIN) && param.type.equals(SdParam.SD_MODE_TYPE_TXT2IMG)) {
                try {
                    JSONObject jsonExif = new JSONObject();
                    String userComment = String.format("%s\nNegative prompt: %s\nSteps: %d, Sampler: %s, CFG scale: %.1f, Size: %dx%d",
                            mCurrentSketch.getPrompt(), mCurrentSketch.getNegPrompt(), param.steps,
                            sharedPreferences.getString("sdSampler", "Euler a"), param.cfgScale, mBitmap.getWidth(), mBitmap.getHeight());
                    jsonExif.put("UserComment", userComment);
                    Date date = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                    jsonExif.put("DateTimeOriginal", sdf.format(date));
                    jsonExif.put("CreateDate", sdf.format(date));
                    exif = jsonExif.toString();
                } catch (JSONException ignored) {}
            } else if (!mCurrentSketch.getCnMode().equals(Sketch.CN_MODE_ORIGIN)) {
                try {
                    JSONObject jsonExif = new JSONObject(exif);
                    if (!jsonExif.has("UserComment")) {
                        String userComment = String.format("%s\nNegative prompt: %s\nSteps: %d, Sampler: %s, CFG scale: %.1f, Size: %dx%d",
                                mCurrentSketch.getPrompt(), mCurrentSketch.getNegPrompt(), param.steps,
                                sharedPreferences.getString("sdSampler", "Euler a"), param.cfgScale, mBitmap.getWidth(), mBitmap.getHeight());
                        jsonExif.put("UserComment", userComment);
                    }
                    if (!jsonExif.has("DateTimeOriginal")) {
                        Date date = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                        jsonExif.put("DateTimeOriginal", sdf.format(date));
                    }
                    exif = jsonExif.toString();
                } catch (JSONException ignored) {}
            }
            Utils.saveBitmapToExternalStorage(this, mBitmap, savedImageName, exif);
            apiResultList.get(currentResult).savedImageName = savedImageName;
        }
    }

    @Override
    public void onBackPressed() {
        if (isCallingSD && !isInterrupted) {
            sdApiHelper.sendPostRequest("interrupt", "/sdapi/v1/interrupt", new JSONObject());
            isInterrupted = true;
        } else if (isCallingSD || isCallingAPI) {
            // do Nothing
        } else {
            isCallingAPI = false;
            Intent intent = new Intent(ViewSdImageActivity.this, DrawingActivity.class);
            intent.putExtra("sketchId", mCurrentSketch.getId());
            setResult(Activity.RESULT_CANCELED, intent);
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService.
        Intent intent = new Intent(this, ViewSdImageService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        isPaused = true;
        super.onPause();
    }

    @Override
    public void onResume() {
        isPaused = false;
        updateScreen();
        super.onResume();
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            ViewSdImageService.ViewSdImageBinder binder = (ViewSdImageService.ViewSdImageBinder) service;
            mService = binder.getService();
            mService.setActivity(ViewSdImageActivity.this);
            mBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void getSdModel() {
        showSpinner();
        if (sdModelList == null) {
            isCallingAPI = true;
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
            isCallingAPI = true;
            setConfigRequest.put("sd_model_checkpoint", preferredModel);
            setConfigRequest.put("sd_checkpoint_hash", sdModelList.get(preferredModel));
            sdApiHelper.sendPostRequest("setSdModel", "/sdapi/v1/options", setConfigRequest);
        } else {
            callSD4Img();
        }
    }

    private void showSpinner() {
        isCallingAPI = true;
        txtSdStatus.setText("Working...");
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
        handler.removeCallbacksAndMessages(null);
        spinner_bg.setVisibility(View.GONE);
        if (!mCurrentSketch.getCnMode().equals(Sketch.CN_MODE_ORIGIN)) {
            sdButton.setVisibility(View.VISIBLE);
        }
        saveButton.setVisibility((savedImageName != null) ? View.GONE : View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);
        //((currentResult + 1) + "/" + apiResultList.size() : "");)
        if (apiResultList.size() > 0) {
            prevButton.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.VISIBLE);
        }
        expandButton.setVisibility(View.VISIBLE);
        editButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSdApiFailure(String requestType, String errMessage) {
        if ("getProgress".equals(requestType)) {
            if (!isPaused && isCallingSD && !isInterrupted)
                handler.postDelayed(() -> sdApiHelper.sendGetRequest("getProgress", "/sdapi/v1/progress?skip_current_image=false"), 1000);
        } else {
            isCallingSD = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Call Stable Diffusion API failed (" + requestType + ")")
                    .setMessage(errMessage)
                    .setPositiveButton("OK", (dialog, id) -> {
                        hideSpinner();
                    });
            AlertDialog alert = builder.create();
            if (!isFinishing()) alert.show();
        }
    }

    public void callSD4Img() {
        showSpinner();
        if (mBound) {
            String sdBaseUrl = sharedPreferences.getString("sdServerAddress", "");

            isCallingSD = true;
            isInterrupted = false;
            SdParam param = sdApiHelper.getSdCnParm(mCurrentSketch.getCnMode());

            Intent intent = new Intent(this, ViewSdImageService.class);
            String requestType;
            JSONObject jsonObject;
            if (param.type.equals(SdParam.SD_MODE_TYPE_TXT2IMG)) {
                requestType = "txt2img";
                jsonObject = sdApiHelper.getControlnetTxt2imgJSON(param, mCurrentSketch);
            } else {
                requestType = "img2img";
                jsonObject = sdApiHelper.getControlnetImg2imgJSON(param, mCurrentSketch);
            }
            mService.setObject(sdBaseUrl, jsonObject);
            intent.putExtra("requestType", requestType);
            startService(intent);
            if (!isPaused)
                handler.postDelayed(() -> sdApiHelper.sendGetRequest("getProgress", "/sdapi/v1/progress?skip_current_image=false"), 2000);
        } else {
            handler.postDelayed(this::callSD4Img, 100);
        }
    }

    ActivityResultLauncher<Intent> drawingActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {});

    @Override
    public void onSdApiResponse(String requestType, String responseBody) {
        try {
            if ("getSDModel".equals(requestType)) {
                isCallingAPI = false;
                JSONArray jsonArray = new JSONArray(responseBody);
                sdModelList = new HashMap<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject joModel = jsonArray.getJSONObject(i);
                    sdModelList.put(joModel.getString("title"), joModel.getString("sha256"));
                }
                setSdModel();
            } else if ("setSdModel".equals(requestType)) {
                isCallingAPI = false;
                callSD4Img();
            } else if ("interrupt".equals(requestType)) {
                //isCallingSD = false;
                //updateScreen();
            } else if ("getProgress".equals(requestType)) {
                JSONObject jsonObject = new JSONObject(responseBody);
                double progress = jsonObject.getDouble("progress");
                double etaRelative = jsonObject.getDouble("eta_relative");
                if ((etaRelative > 0) && (progress > 0)) {
                    txtSdStatus.setText(String.format("%d%% completed.", Math.round(progress * 100)));
                }
                if (!isPaused && isCallingSD && !isInterrupted)
                    handler.postDelayed(() -> sdApiHelper.sendGetRequest("getProgress", "/sdapi/v1/progress?skip_current_image=false"), 1000);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            onSdApiFailure(requestType, e.getMessage());
        }
    }

    public static void addResult(String requestType) {
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

    public void updateScreen() {
        sdImage.resetView();
        sdImage.setImageBitmap(mBitmap);
        txtCount.setText(apiResultList.size() > 0 ? (currentResult + 1) + "/" + apiResultList.size() : "");

        if (isCallingSD) {
            handler.postDelayed(() -> sdApiHelper.sendGetRequest("getProgress", "/sdapi/v1/progress?skip_current_image=false"), 1000);
        } else {
            handler.removeCallbacksAndMessages(null);
        }

        if (isCallingDFL || isCallingAPI || isCallingSD) {
            showSpinner();
        }

    }

    public static void updateMBitmap() {
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