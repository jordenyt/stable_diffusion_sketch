package com.jsoft.diffusionpaint;

import static java.lang.Math.max;
import static java.lang.Math.round;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

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
import android.util.DisplayMetrics;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ViewSdImageActivity extends AppCompatActivity implements SdApiResponseListener {

    public static final String CHANNEL_ID = "foreground_service_channel";
    private static Sketch mCurrentSketch;
    private TouchImageView sdImage;
    private LinearLayout spinner_bg;
    private FloatingActionButton sdButton;
    private FloatingActionButton saveButton;
    private FloatingActionButton editButton;
    private FloatingActionButton prevButton;
    private FloatingActionButton nextButton;
    private FloatingActionButton backButton;
    private TextView txtCount;
    private TextView txtSdStatus;
    public static Bitmap mBitmap = null;
    public static Bitmap inpaintBitmap = null;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault());
    private static SharedPreferences sharedPreferences;
    //private String aspectRatio;
    @SuppressLint("StaticFieldLeak")
    private static SdApiHelper sdApiHelper;
    public static boolean isCallingDFL = false;
    public static String savedImageName = null;
    public static boolean isCallingAPI = false;
    private static List<ApiResult> apiResultList;
    private static int currentResult;
    public static Map<String, String> sdModelList = null;
    private static Handler handler;
    public static int remainGen = 0;
    private boolean isPaused = false;
    private ViewSdImageService mService;
    private boolean mBound = false;
    public static boolean isInterrupted = false;
    public static String rtResultType = null;
    public static List<Bitmap> rtBitmap = null;
    public static List<String> rtInfotext = null;
    public static String rtErrMsg = null;
    public static boolean rtQueued = false;

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
            MainActivity.isViewingSDImage = true;
            rtResultType = null;
            rtBitmap = null;
            rtInfotext = null;
            rtErrMsg = null;
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
        applyImmersiveMode();

        if (isFirstCall) {
            apiResultList = new ArrayList<>();
            currentResult = 0;
            savedImageName = null;
            if (cnMode.equals(Sketch.CN_MODE_ORIGIN)) {
                if (mCurrentSketch.getImgReference() != null) {
                    mBitmap = mCurrentSketch.getImgBgRef(10);
                } else {
                    mBitmap = mCurrentSketch.getImgBackground();
                }
                sdImage.setImageBitmap(mBitmap);
                addResult("original", null);
                remainGen = 0;
            } else {
                SdParam param = sdApiHelper.getSdCnParm(mCurrentSketch.getCnMode());
                if (param.type.equals(SdParam.SD_MODE_TYPE_INPAINT) && param.inpaintPartial == 1) {
                    mBitmap = partialInpaintPreview(mCurrentSketch);
                }
                getSdModel();
            }
        }
        updateScreen();
    }

    /**
     * Hide the system bars (status + navigation / soft key panel) while this fullscreen
     * viewer is in the foreground, so the panel never covers the floating buttons.
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

    private Bitmap partialInpaintPreview(Sketch mCurrentSketch) {
        SdParam param = sdApiHelper.getSdCnParm(mCurrentSketch.getCnMode());
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.getWindowManager()
                .getDefaultDisplay()
                .getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        Bitmap bmEdit = Bitmap.createBitmap(mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvasEdit = new Canvas(bmEdit);
        canvasEdit.drawBitmap(mCurrentSketch.getImgPreview(), null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAlpha(128);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth((float)(15d * (double)mCurrentSketch.getImgBackground().getWidth() / (double)screenWidth));
        canvasEdit.drawRect(mCurrentSketch.getRectInpaint(param.sdSize),paint);
        return bmEdit;
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

        backButton.setOnClickListener(view -> this.goBack());

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

        editButton.setOnClickListener(view -> {
            showSpinner();
            CompletableFuture.supplyAsync(() -> {
                saveImage();
                return "";
            }).thenRun(() -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show();
                    File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    File sdSketchFolder = new File(picturesDirectory, sharedPreferences.getString("picFolder", "sdSketch"));
                    File file = new File(sdSketchFolder, savedImageName);
                    Intent intent = new Intent(ViewSdImageActivity.this, DrawingActivity.class);
                    intent.putExtra("sketchId", -2);
                    intent.putExtra("bitmapPath", file.getAbsolutePath());
                    intent.putExtra("prompt", mCurrentSketch.getPrompt());
                    intent.putExtra("negPrompt", mCurrentSketch.getNegPrompt());
                    if (mCurrentSketch.getId() >= 0) {
                        intent.putExtra("parentId", mCurrentSketch.getId());
                    }
                    //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    setResult(Activity.RESULT_OK, intent);
                    clearStaticVar();
                    MainActivity.isViewingSDImage = false;
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { goBack();}
        });
    }

    public void clearStaticVar() {
        isCallingAPI = false;
        isInterrupted = false;
        isCallingDFL = false;
        rtResultType = null;
        rtBitmap = null;
        rtInfotext = null;
        rtErrMsg = null;
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
            if (exif == null || exif.length() < 2) { exif = "{}"; }
            try {
                JSONObject jsonExif = new JSONObject(exif);
                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                if (!jsonExif.has("DateTimeOriginal")) {
                    jsonExif.put("DateTimeOriginal", sdf.format(date));
                }
                if (!jsonExif.has("CreateDate")) {
                    jsonExif.put("CreateDate", sdf.format(date));
                }
                if (!jsonExif.has("Make")) {
                    jsonExif.put("Make", "samsung");
                }
                if (!jsonExif.has("Model")) {
                    jsonExif.put("Model", "SM-S916B");
                }
                if (!jsonExif.has("Software")) {
                    jsonExif.put("Software", "S916BXXU5CWL5");
                }
                if (!jsonExif.has("ExposureTime")) {
                    jsonExif.put("ExposureTime", "0.005");
                }
                if (!jsonExif.has("FNumber")) {
                    jsonExif.put("FNumber", "1.8");
                }
                if (!jsonExif.has("ISOSpeedRatings")) {
                    jsonExif.put("ISOSpeedRatings", "200");
                }
                if (!jsonExif.has("FocalLength")) {
                    jsonExif.put("FocalLength", "5.4 mm");
                }
                if (!jsonExif.has("FocalLengthIn35mmFilm")) {
                    jsonExif.put("FocalLengthIn35mmFilm", "23 mm");
                }
                if (!jsonExif.has("MaxApertureValue")) {
                    jsonExif.put("MaxApertureValue", "1.8");
                }
                if (!jsonExif.has("ExposureProgram")) {
                    jsonExif.put("ExposureProgram", "2");
                }
                if (!jsonExif.has("ExposureBiasValue")) {
                    jsonExif.put("ExposureBiasValue", "0");
                }
                if (!jsonExif.has("MeteringMode")) {
                    jsonExif.put("MeteringMode", "5");
                }
                if (!jsonExif.has("WhiteBalance")) {
                    jsonExif.put("WhiteBalance", "0");
                }
                if (!jsonExif.has("Flash")) {
                    jsonExif.put("Flash", "0");
                }
                if (!jsonExif.has("SceneCaptureType")) {
                    jsonExif.put("SceneCaptureType", "0");
                }
                exif = jsonExif.toString();
            } catch (JSONException ignored) {}
            Utils.saveBitmapToExternalStorage(this, mBitmap, sharedPreferences.getString("picFolder", "sdSketch"), savedImageName, exif);
            apiResultList.get(currentResult).savedImageName = savedImageName;
        }
    }

    public void goBack() {
        if (isCallingDFL) {
            String sdBaseUrl = sharedPreferences.getString("dflApiAddress", "");
            String runningJobId = (mBound && mService != null) ? mService.getJobId() : null;
            if (runningJobId != null && !runningJobId.isEmpty()) {
                // Cancel this job only - other jobs in the queue keep running.
                sdApiHelper.cancelJob(sdBaseUrl, runningJobId);
            } else {
                // No job id known (very old gateway): fall back to the global interrupt.
                sdApiHelper.sendRequest("interrupt", sdBaseUrl, "/comfyui_interrupt", null, "GET");
            }
            isInterrupted = true;
            // The workflow is interrupted: drop the spinner overlay and show the
            // page that was underneath it again.
            restoreScreen();
        } else if (isCallingAPI) {
            // do Nothing
        } else {
            clearStaticVar();
            Intent intent = new Intent(ViewSdImageActivity.this, DrawingActivity.class);
            intent.putExtra("sketchId", mCurrentSketch.getId());
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            MainActivity.isViewingSDImage = false;
            setResult(Activity.RESULT_CANCELED, intent);
            finish();
        }
    }

    /**
     * Remove the spinner overlay and restore the screen that was visible before
     * the job started. Used when a running job is interrupted.
     */
    private void restoreScreen() {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            isCallingDFL = false;
            isCallingAPI = false;
            txtSdStatus.setText("Cancelled.");
            updateScreen();
        });
    }

    /**
     * The running job was interrupted (cancelled from here or from the gateway).
     * The spinner overlay is removed so the user sees the screen that was
     * underneath it again instead of an endless loading screen.
     */
    public void onJobInterrupted() {
        restoreScreen();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService.
        Intent intent = new Intent(this, ViewSdImageService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connection);
    }

    @Override
    public void onPause() {
        isPaused = true;
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onResume() {
        isPaused = false;
        if (rtResultType != null && rtBitmap != null && !rtQueued) {
            rtQueued = true;
            runOnUiThread(() -> {
                processResultBitmap(rtResultType, rtBitmap, rtInfotext);
                clearStaticVar();
                rtQueued = false;
            });
        } else if (rtResultType != null && rtErrMsg != null) {
            onSdApiFailure(rtResultType, rtErrMsg);
            clearStaticVar();
        } else {
            updateScreen();
        }
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
        public void onBindingDied(ComponentName className) {
            mBound = false;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void getSdModel() {
        callSD4Img();
    }

    private void showSpinner() {
        txtSdStatus.setText("Working...");
        spinner_bg.setVisibility(View.VISIBLE);
        sdButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
        prevButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        editButton.setVisibility(View.GONE);
    }

    private void hideSpinner() {

        spinner_bg.setVisibility(View.GONE);
        sdButton.setVisibility((mCurrentSketch.getCnMode().equals(Sketch.CN_MODE_ORIGIN)) ? View.GONE : View.VISIBLE);

        saveButton.setVisibility((savedImageName != null) || (apiResultList.size() == 0) ? View.GONE : View.VISIBLE);
        backButton.setVisibility(View.VISIBLE);
        prevButton.setVisibility((apiResultList.size() > 1) ? View.VISIBLE : View.GONE);
        nextButton.setVisibility((apiResultList.size() > 1) ? View.VISIBLE : View.GONE);
        
        editButton.setVisibility((apiResultList.size() > 0) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSdApiFailure(String requestType, String errMessage) {
        updateScreen();
        handler.removeCallbacksAndMessages(null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Call Stable Diffusion API failed (" + requestType + ")")
                .setMessage(errMessage)
                .setPositiveButton("OK", (dialog, id) -> {
                    //hideSpinner();
                });
        AlertDialog alert = builder.create();
        if (!isFinishing()) alert.show();
    }

    public void updateStatus(String progress) {
        txtSdStatus.setText(progress);
    }

    public void callSD4Img() {
        clearStaticVar();
        if (mBound) {
            String sdBaseUrl = "";

            Intent intent = new Intent(this, ViewSdImageService.class);
            String requestType;
            JSONObject jsonObject;
            int batchSize = 1;
            try {
                if (remainGen > 0) {
                    batchSize = Math.min(remainGen, Integer.parseInt(sharedPreferences.getString("maxBatchSize", "1")));
                    intent.putExtra("numGen", remainGen);
                }
            } catch (Exception ignored) {}

            if (mCurrentSketch.getCnMode().startsWith(Sketch.CN_MODE_COMFYUI)) {
                requestType = "comfyui";
                jsonObject = sdApiHelper.getComfyuiJSON(mCurrentSketch, batchSize);
                sdBaseUrl = sharedPreferences.getString("dflApiAddress", "");
                mService.setObject(sdBaseUrl, jsonObject);
                intent.putExtra("requestType", requestType);
                startService(intent);
            }
        } else {
            Handler h = new Handler();
            h.postDelayed(this::callSD4Img, 200);
        }
    }

    @Override
    public void onSdApiResponse(String requestType, String responseBody) {

    }

    public static void addResult(String requestType, String infoTexts) {
        ApiResult r = new ApiResult();
        r.requestType = requestType;
        r.mBitmap = mBitmap.copy(mBitmap.getConfig(), true);
        if (inpaintBitmap != null) {
            r.inpaintBitmap = inpaintBitmap.copy(inpaintBitmap.getConfig(), true);
        }
        if (infoTexts == null && apiResultList != null && apiResultList.size() > 0 ) {
            r.infoTexts = apiResultList.get(currentResult).infoTexts;
        } else {
            r.infoTexts = infoTexts;
        }
        //r.savedImageName = savedImageName;
        apiResultList.add(r);
        currentResult = apiResultList.size() - 1;
    }

    public void updateScreen() {
        sdImage.resetView();
        sdImage.setImageBitmap(mBitmap);
        txtCount.setText(apiResultList.size() > 0 ? (currentResult + 1) + "/" + apiResultList.size() : "");

        handler.removeCallbacksAndMessages(null);

        if (isCallingAPI || isCallingDFL) {
            showSpinner();
        } else {
            hideSpinner();
        }
    }

    public void processResultBitmap(String requestType, List<Bitmap> results, List<String> listInfotext) {
        for (int i=0; i<results.size();i++) {
            if (results.get(i) != null) {
                mBitmap = results.get(i);
                if (!"txt2img".equals(requestType)) {
                    updateMBitmap();
                }
                savedImageName = null;
                if (listInfotext != null) {
                    addResult(requestType, listInfotext.get(i));
                } else {
                    addResult(requestType, null);
                }

                if (remainGen > 0) {
                    remainGen--;
                }
            }
        }
        if (remainGen > 0 && !isInterrupted) {
            callSD4Img();
        } else {
            isInterrupted = false;
        }
        updateScreen();
    }

    public static void updateMBitmap() {
        SdParam param = sdApiHelper.getSdCnParm(mCurrentSketch.getCnMode());

        if (param.inpaintPartial == SdParam.INPAINT_PARTIAL) {
            inpaintBitmap = mBitmap.copy(mBitmap.getConfig(), true);
            Bitmap bmEdit = Bitmap.createBitmap(mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvasEdit = new Canvas(bmEdit);
            canvasEdit.drawBitmap(mCurrentSketch.getImgBackground(), null, new RectF(0, 0, bmEdit.getWidth(), bmEdit.getHeight()), null);
            canvasEdit.drawBitmap(mBitmap, null, mCurrentSketch.getRectInpaint(param.sdSize), null);
            RectF partialRect = mCurrentSketch.getRectInpaint(param.sdSize);
            double ratio = (double)max(partialRect.width(), partialRect.height()) / param.sdSize;
            int boundary = (int) round(param.maskBlur * (param.baseImage.equals(SdParam.SD_INPUT_IMAGE_SKETCH)?1:0) * ratio);
            mBitmap = mCurrentSketch.getImgBgMerge(bmEdit, boundary, (int)round((param.maskBlur + 10) * ratio));
        } else if (param.type.equals(SdParam.SD_MODE_TYPE_INPAINT)) {
            inpaintBitmap = mBitmap.copy(mBitmap.getConfig(), true);
            double ratio = (double)max(mCurrentSketch.getImgBackground().getWidth(), mCurrentSketch.getImgBackground().getHeight()) / param.sdSize;
            int boundary = (int) round(param.maskBlur * (param.baseImage.equals(SdParam.SD_INPUT_IMAGE_SKETCH)?1:0) * ratio);
            mBitmap = mCurrentSketch.getImgBgMerge(inpaintBitmap, boundary, (int)round((param.maskBlur + 10) * ratio));
        }
    }
}