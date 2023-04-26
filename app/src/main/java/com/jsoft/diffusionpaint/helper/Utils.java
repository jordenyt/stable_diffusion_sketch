package com.jsoft.diffusionpaint.helper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Utils {

    private final Context _context;
    public Utils(Context context) {
        this._context = context;
    }

    /*
     * getting screen width
     */
    public int getScreenWidth() {
        int columnWidth;
        WindowManager wm = (WindowManager) _context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        final Point point = new Point();
        try {
            display.getSize(point);
        } catch (Exception e) { // Older device
            Log.e("diffusionPaint", "Exception caught in getScreenWidth", e);
        }
        columnWidth = point.x;
        return columnWidth;
    }

    public static Bitmap base64String2Bitmap(String s) {
        if (s == null || s.length() == 0) return null;
        byte[] preview = Base64.decode(s, Base64.DEFAULT);
        return (BitmapFactory.decodeByteArray(preview, 0, preview.length));
    }

    public static String jpg2Base64String(Bitmap bm) {
        return bitmap2Base64String(bm, Bitmap.CompressFormat.JPEG, 90);
    }

    public static String png2Base64String(Bitmap bm) {
        return bitmap2Base64String(bm, Bitmap.CompressFormat.PNG, 100);
    }

    public static String bitmap2Base64String(Bitmap bm, Bitmap.CompressFormat format, int quality) {
        if (bm == null) return "";
        ByteArrayOutputStream byteArrayOutputStream  = new ByteArrayOutputStream();
        bm.compress(format, quality, byteArrayOutputStream ); //bm is the bitmap object
        byte[] b = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    public static String getAspectRatio(Bitmap bm) {
        String aspectRatio = Sketch.ASPECT_RATIO_SQUARE;
        double ratio = (double) bm.getWidth() / (double) bm.getHeight();
        if (bm != null) {
            if (ratio >= 1.125) {
                aspectRatio = Sketch.ASPECT_RATIO_LANDSCAPE;
            } else if (ratio <= 0.875) {
                aspectRatio = Sketch.ASPECT_RATIO_PORTRAIT;
            }
        }
        return aspectRatio;
    }
    public static void saveBitmapToExternalStorage(Activity a, Bitmap bitmap, String filename) {
        // Get the directory for the user's public pictures directory.
        File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File sdSketchFolder = new File(picturesDirectory, "sdSketch");
        if (!sdSketchFolder.exists()) {
            sdSketchFolder.mkdirs();
        }
        // Create the file object.
        File file = new File(sdSketchFolder, filename);

        try {
            // Create a file output stream to write the bitmap data to the file.
            FileOutputStream fos = new FileOutputStream(file);
            // Compress the bitmap as a JPEG with 90% quality and write it to the file output stream.
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            // Flush and close the file output stream.
            fos.flush();
            fos.close();
            // Notify the media scanner to add the new image to the gallery.
            MediaScannerConnection.scanFile(a, new String[]{file.toString()}, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap getDilationMask(Bitmap originalBitmap, int expandPixel) {
        // Create a new Bitmap with the same dimensions and a black background
        Bitmap newBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);

        // Iterate over each pixel in the original Bitmap and set the color value in the new Bitmap
        for (int x = 0; x < originalBitmap.getWidth(); x++) {
            for (int y = 0; y < originalBitmap.getHeight(); y++) {
                int color = originalBitmap.getPixel(x,y);
                if (Color.alpha(color) != 0) {
                    newBitmap.setPixel(x,y,Color.WHITE);
                } else {
                    newBitmap.setPixel(x, y, Color.TRANSPARENT);
                }
            }
        }

        Bitmap bmMask = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cvMask = new Canvas(bmMask);
        Paint mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.BLACK);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        cvMask.drawRect(0, 0, bmMask.getWidth(), bmMask.getHeight(), mBackgroundPaint);
        int step = (int) Math.round(Math.max(1,(expandPixel + 0.0)/5));
        for (int i=-expandPixel;i<=expandPixel;i=i+step) {
            for (int j=-expandPixel;j<=expandPixel;j=j+step) {
                double d = Math.sqrt(i^2 + j^2);
                if (d <= expandPixel) {
                    cvMask.drawBitmap(newBitmap, i, j, null);
                }
            }
        }
        return bmMask;
    }

    public static boolean isEmptyBitmap(Bitmap bitmap) {
        if (bitmap == null) return true;
        boolean allTransparent = true;
        for (int x = 0; x < bitmap.getWidth(); x++) {
            for (int y = 0; y < bitmap.getHeight(); y++) {
                int pixel = bitmap.getPixel(x, y);
                int alpha = Color.alpha(pixel);
                if (alpha != 0) {
                    allTransparent = false;
                    break;
                }
            }
            if (!allTransparent) {
                break;
            }
        }
        return allTransparent;
    }
}
