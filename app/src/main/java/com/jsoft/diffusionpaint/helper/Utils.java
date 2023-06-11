package com.jsoft.diffusionpaint.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.exifinterface.media.ExifInterface;

import com.jsoft.diffusionpaint.DrawingActivity;
import com.jsoft.diffusionpaint.dto.Sketch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
            if (ratio >= 7d/6d) {
                aspectRatio = Sketch.ASPECT_RATIO_LANDSCAPE;
            } else if (ratio <= 7d/8d) {
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

        int[] newPixels = new int[originalBitmap.getWidth() * originalBitmap.getHeight()];
        int[] originalPixels = new int[originalBitmap.getWidth() * originalBitmap.getHeight()];
        originalBitmap.getPixels(originalPixels, 0, originalBitmap.getWidth(), 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight());
        // Iterate over each pixel in the original Bitmap and set the color value in the new Bitmap
        for (int i = 0; i < originalPixels.length; i++) {
            int color = originalPixels[i];
            if (Color.alpha(color) != 0) {
                newPixels[i] = Color.WHITE;
            } else {
                newPixels[i] = Color.TRANSPARENT;
            }
        }
        Bitmap newBitmap = Bitmap.createBitmap(newPixels, originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);

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

    public static Bitmap extractBitmap(Bitmap sourceBitmap, RectF r) {
        return Bitmap.createBitmap(sourceBitmap, (int)r.left, (int)r.top, (int)r.width(), (int)r.height());
    }

    public static boolean isEmptyBitmap(Bitmap bitmap) {
        if (bitmap == null) return true;
        boolean allTransparent = true;

        int pixels[] = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int alpha = Color.alpha(pixel);
            if (alpha != 0) {
                allTransparent = false;
                break;
            }
        }

        return allTransparent;
    }

    public static String getPathFromUri(Uri uri, Activity activity) {
        if (uri.getScheme().equals("content")) {
            try {
                InputStream inputStream = activity.getContentResolver().openInputStream(uri);
                String fileName = "temp_file_" + System.currentTimeMillis();
                File tempFile = new File(activity.getCacheDir(), fileName);
                FileOutputStream outputStream = new FileOutputStream(tempFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                outputStream.close();
                inputStream.close();
                return tempFile.getAbsolutePath();
            } catch (IOException e) {
                Log.e("diffusionPaint", "Cannot get Image Path from shared content.");
                return null;
            }
        }

        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, null);
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
    public static void newPaintFromImage(Intent intent, Activity activity, ActivityResultLauncher<Intent> drawingActivityResultLauncher) {
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        String mimeType = activity.getContentResolver().getType(uri);
        if (mimeType != null && mimeType.startsWith("image/")) {
            String filePath = getPathFromUri(uri, activity);
            if (filePath != null) {
                Intent drawIntent = new Intent(activity, DrawingActivity.class);
                drawIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                drawIntent.putExtra("sketchId", -2);
                drawIntent.putExtra("bitmapPath", filePath);
                drawingActivityResultLauncher.launch(drawIntent);
            }
        }
    }

    public static Bitmap getBitmapFromPath(String filePath) {
        if (filePath != null) {
            Bitmap imageBitmap = BitmapFactory.decodeFile(filePath);
            if (imageBitmap != null) {
                int orientation = ExifInterface.ORIENTATION_UNDEFINED;
                try {
                    ExifInterface exif = new ExifInterface(filePath);
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                } catch (IOException e) {
                    Log.e("diffusionpaint", "IOException get from returned file.");
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
                return Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);
            }
        }
        return null;
    }
}
