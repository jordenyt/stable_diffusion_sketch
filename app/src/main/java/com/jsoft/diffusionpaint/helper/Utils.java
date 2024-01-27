package com.jsoft.diffusionpaint.helper;

import static java.lang.Math.*;

import android.app.Activity;
import android.content.Context;
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

import androidx.exifinterface.media.ExifInterface;

import com.jsoft.diffusionpaint.dto.Sketch;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

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

    public static String getImageExif(String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);

            JSONObject jsonExif = new JSONObject();
            Field[] fields = ExifInterface.class.getFields();
            for (Field field : fields) {
                if (field.getName().startsWith("TAG_")) {
                    String attribute = field.get(null).toString();
                    String value = exif.getAttribute(attribute);
                    if (attribute.equals(ExifInterface.TAG_ORIENTATION)) {
                        jsonExif.put(attribute, "1");
                    } else if (attribute.equals(ExifInterface.TAG_USER_COMMENT)) {
                        byte[] s = exif.getAttributeBytes(ExifInterface.TAG_USER_COMMENT);
                        value = new String(s, StandardCharsets.UTF_8);
                        jsonExif.put(attribute, value);
                    } else if (value != null) {
                        jsonExif.put(attribute, value);
                    }
                }
            }

            return jsonExif.toString();
            // Use the jsonString as needed
        } catch (IOException | JSONException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveImageExif(String imagePath, String jsonString) {
        if (jsonString == null || jsonString.length() < 2) { return; }
        try {
            JSONObject jsonExif = new JSONObject(jsonString);
            ExifInterface exif = new ExifInterface(imagePath);
            Iterator<String> keys = jsonExif.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = jsonExif.getString(key);
                exif.setAttribute(key, value);
            }

            exif.saveAttributes();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static void saveBitmapToExternalStorage(Activity a, Bitmap bitmap, String filename, String exifJson) {
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
            saveImageExif(file.getAbsolutePath(), exifJson);
            // Notify the media scanner to add the new image to the gallery.
            MediaScannerConnection.scanFile(a, new String[]{file.toString()}, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap getDilationMask(Bitmap sketchBitmap, int expandPixel) {
        // Create a new Bitmap with the same dimensions and a black background

        int[] maskPixels = new int[sketchBitmap.getWidth() * sketchBitmap.getHeight()];
        int[] sketchPixels = new int[sketchBitmap.getWidth() * sketchBitmap.getHeight()];
        sketchBitmap.getPixels(sketchPixels, 0, sketchBitmap.getWidth(), 0, 0, sketchBitmap.getWidth(), sketchBitmap.getHeight());
        // Iterate over each pixel in the original Bitmap and set the color value in the new Bitmap
        for (int i = 0; i < sketchPixels.length; i++) {
            maskPixels[i] = (Color.alpha(sketchPixels[i]) != 0) ? Color.WHITE : Color.TRANSPARENT;
        }
        Bitmap maskBitmap = Bitmap.createBitmap(maskPixels, sketchBitmap.getWidth(), sketchBitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Bitmap dilatedBitmap = Bitmap.createBitmap(sketchBitmap.getWidth(), sketchBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cvMask = new Canvas(dilatedBitmap);

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setStyle(Paint.Style.FILL);
        cvMask.drawRect(0, 0, dilatedBitmap.getWidth(), dilatedBitmap.getHeight(), backgroundPaint);

        cvMask.drawBitmap(maskBitmap, 0, 0, null);

        if (expandPixel > 0) {
            Paint boundaryPaint = new Paint();
            boundaryPaint.setColor(Color.WHITE);
            boundaryPaint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < sketchPixels.length; i++) {
                if (Color.alpha(sketchPixels[i]) != 0) {
                    boolean isBoundary = false;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (!(dx == 0 && dy == 0)) {
                                int x = i % sketchBitmap.getWidth() + dx;
                                int y = i / sketchBitmap.getWidth() + dy;
                                if (x >= 0 && x < sketchBitmap.getWidth() && y >= 0 && y < sketchBitmap.getHeight()) {
                                    if (Color.alpha(sketchPixels[x + y * sketchBitmap.getWidth()]) == 0) {
                                        isBoundary = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (isBoundary) break;
                    }
                    if (isBoundary) {
                        int x = i % sketchBitmap.getWidth();
                        int y = i / sketchBitmap.getWidth();
                        cvMask.drawCircle(x, y, expandPixel, boundaryPaint);
                    }
                }
            }
        }

        return dilatedBitmap;
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
            if (Color.alpha(pixels[i]) != 0) {
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
                String fileName=uri.toString().substring(uri.toString().lastIndexOf("/")+1);
                //String fileName = "temp_file_" + System.currentTimeMillis();
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


    public static Bitmap getOutpaintBmp(Bitmap bm, String cnMode, int fillColor, boolean isPaint, int sdSize) {
        int originalWidth = bm.getWidth();
        int originalHeight = bm.getHeight();
        int newWidth = originalWidth;
        int newHeight = originalHeight;
        int expandPixel;
        if (cnMode.startsWith(Sketch.CN_MODE_OUTPAINT_V)) {
            if (originalHeight * 4 / 3 >= originalWidth) {
                double ratio =  round((double)originalWidth / (originalHeight * 4d / 3d) * sdSize / 64d) * 64d / originalWidth;
                expandPixel = round((round(sdSize / ratio) - originalHeight) / 2f);
            } else {
                double ratio = (double)sdSize / (double)originalWidth;
                expandPixel = (int) round((round((originalHeight * 4d / 3d) * ratio / 64d) * 64d / ratio - originalHeight) / 2f);
            }
            newHeight += 2 * expandPixel;
        } else {
            if (originalWidth * 4 / 3 >= originalHeight) {
                double ratio =  round((double)originalHeight / (originalWidth * 4d / 3d) * sdSize / 64d) * 64d / originalHeight;
                expandPixel = round((round(sdSize / ratio) - originalWidth) / 2f);
            } else {
                double ratio = (double)sdSize / (double)originalHeight;
                expandPixel = (int) round((round((originalWidth * 4d / 3d) * ratio / 64d) * 64d / ratio - originalWidth) / 2f);
            }
            newWidth += 2 * expandPixel;
        }
        Bitmap expandBmp = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(expandBmp);
        Paint paint = new Paint();
        paint.setColor(fillColor); // replace with the desired color
        if (!isPaint) {
            int x = (newWidth - originalWidth) / 2;
            int y = (newHeight - originalHeight) / 2;
            if (cnMode.equals(Sketch.CN_MODE_OUTPAINT_H_RIGHT) || cnMode.equals(Sketch.CN_MODE_OUTPAINT_V_BOTTOM)) {
                x = 0;
                y = 0;
            } else if (cnMode.equals(Sketch.CN_MODE_OUTPAINT_H_LEFT)) {
                x = (newWidth - originalWidth);
                y = 0;
            } else if (cnMode.equals(Sketch.CN_MODE_OUTPAINT_V_TOP)) {
                x = 0;
                y = (newHeight - originalHeight);
            }
            canvas.drawRect(0, 0, expandBmp.getWidth(), expandBmp.getHeight(), paint);
            canvas.drawBitmap(bm, x, y, null);
        } else {
            int margin = expandPixel / 8;
            if (cnMode.equals(Sketch.CN_MODE_OUTPAINT_V)) {
                canvas.drawRect(0, 0, expandBmp.getWidth(), expandPixel + margin, paint);
                canvas.drawRect(0, expandBmp.getHeight() - expandPixel - margin, expandBmp.getWidth(), expandBmp.getHeight(), paint);
            } else if (cnMode.equals(Sketch.CN_MODE_OUTPAINT_V_TOP)) {
                canvas.drawRect(0, 0, expandBmp.getWidth(), expandPixel * 2 + margin, paint);
            } else if (cnMode.equals(Sketch.CN_MODE_OUTPAINT_V_BOTTOM)) {
                canvas.drawRect(0, expandBmp.getHeight() - expandPixel * 2 - margin, expandBmp.getWidth(), expandBmp.getHeight(), paint);
            } else if (cnMode.equals(Sketch.CN_MODE_OUTPAINT_H)){
                canvas.drawRect(0, 0, expandPixel + margin, expandBmp.getHeight(), paint);
                canvas.drawRect(expandBmp.getWidth() - expandPixel - margin, 0, expandBmp.getWidth(), expandBmp.getHeight(), paint);
            } else if (cnMode.equals(Sketch.CN_MODE_OUTPAINT_H_LEFT)) {
                canvas.drawRect(0, 0, 2 * expandPixel + margin, expandBmp.getHeight(), paint);
            } else if (cnMode.equals(Sketch.CN_MODE_OUTPAINT_H_RIGHT)){
                canvas.drawRect(expandBmp.getWidth() - expandPixel * 2 - margin, 0, expandBmp.getWidth(), expandBmp.getHeight(), paint);
            }
        }

        return expandBmp;
    }

    public static long getShortSize(Bitmap bm, int longSize) {
        if (bm.getWidth() >= bm.getHeight()) {
            return round((double)bm.getHeight() / (double)bm.getWidth() * longSize  / 64d) * 64;
        } else {
            return round((double)bm.getWidth() / (double)bm.getHeight() * longSize  / 64d) * 64;
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
