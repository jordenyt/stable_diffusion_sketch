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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

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
                        if (s != null) {
                            value = new String(s, StandardCharsets.UTF_8);
                            jsonExif.put(attribute, value);
                        }
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

    public static boolean isJsonUri(Context context, Uri uri) {
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            // Open an input stream from the URI
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return false;
            }

            // Read the input stream into a StringBuilder
            reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            // Try to parse the content as JSON
            new JSONObject(content.toString());
            return true;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        } finally {
            // Close resources
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Bitmap getDilationMask(Bitmap sketchBitmap, int expandPixel, boolean blurBoundary) {
        int width = sketchBitmap.getWidth();
        int height = sketchBitmap.getHeight();

        // Reduce the resolution for processing
        int scaleFactor = max(max(1, expandPixel / 5), max(width, height) / 750); // Adjust this factor for more or less approximation
        int scaledWidth = width / scaleFactor;
        int scaledHeight = height / scaleFactor;

        Bitmap scaledSketchBitmap = Bitmap.createScaledBitmap(sketchBitmap, scaledWidth, scaledHeight, true);

        int[] scaledSketchPixels = new int[scaledWidth * scaledHeight];
        scaledSketchBitmap.getPixels(scaledSketchPixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight);

        // Create a mask bitmap
        int[] scaledMaskPixels = new int[scaledWidth * scaledHeight];
        for (int i = 0; i < scaledSketchPixels.length; i++) {
            scaledMaskPixels[i] = (Color.alpha(scaledSketchPixels[i]) != 0) ? Color.WHITE : Color.TRANSPARENT;
        }

        Bitmap scaledMaskBitmap = Bitmap.createBitmap(scaledMaskPixels, scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);

        Bitmap scaledDilatedBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
        Canvas cvMask = new Canvas(scaledDilatedBitmap);
        cvMask.drawBitmap(scaledMaskBitmap, 0, 0, null);

        if (expandPixel > 0) {
            Paint boundaryPaint = new Paint();
            boundaryPaint.setColor(Color.WHITE);
            boundaryPaint.setStyle(Paint.Style.FILL);

            // Scale the expandPixel value
            int scaledExpandPixel = expandPixel / scaleFactor;

            // Queue for processing boundary pixels
            Queue<int[]> boundaryQueue = new LinkedList<>();

            // Set of processed boundary pixels
            Set<Integer> processedPixels = new HashSet<>();

            // Find initial boundary pixels and add them to the queue
            for (int y = 0; y < scaledHeight; y++) {
                for (int x = 0; x < scaledWidth; x++) {
                    int i = x + y * scaledWidth;
                    if (Color.alpha(scaledSketchPixels[i]) != 0) {
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                if (dx == 0 && dy == 0) continue;
                                int nx = x + dx;
                                int ny = y + dy;
                                if (nx >= 0 && nx < scaledWidth && ny >= 0 && ny < scaledHeight) {
                                    if (Color.alpha(scaledSketchPixels[nx + ny * scaledWidth]) == 0) {
                                        boundaryQueue.add(new int[]{x, y});
                                        processedPixels.add(x + y * scaledWidth);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Process the boundary pixels from the queue
            if (blurBoundary) {
                int[] maxAlphaValues = new int[scaledWidth * scaledHeight];
                // Initialize maxAlphaValues with the alpha values from the mask
                for (int i = 0; i < scaledMaskPixels.length; i++) {
                    maxAlphaValues[i] = Color.alpha(scaledMaskPixels[i]);
                }

                while (!boundaryQueue.isEmpty()) {
                    int[] boundaryPixel = boundaryQueue.poll();
                    int x = boundaryPixel[0];
                    int y = boundaryPixel[1];
                    for (int dx = -scaledExpandPixel; dx <= scaledExpandPixel; dx++) {
                        for (int dy = -scaledExpandPixel; dy <= scaledExpandPixel; dy++) {
                            int bx = x + dx;
                            int by = y + dy;
                            double distance = Math.sqrt(dx * dx + dy * dy);
                            if (bx >= 0 && bx < scaledWidth && by >= 0 && by < scaledHeight && distance < scaledExpandPixel) {
                                int alphaValue = (int) (255 - (distance / scaledExpandPixel) * 255);
                                int index = bx + by * scaledWidth;
                                if (maxAlphaValues[index] < alphaValue) {
                                    maxAlphaValues[index] = alphaValue;
                                }
                                processedPixels.add(index);
                            }
                        }
                    }
                }

                // Apply the calculated max alpha values to the dilated bitmap
                for (int index : processedPixels) {
                    int alphaValue = maxAlphaValues[index];
                    if (alphaValue > 0) {
                        int x = index % scaledWidth;
                        int y = index / scaledWidth;
                        scaledDilatedBitmap.setPixel(x, y, Color.argb(alphaValue, 255, 255, 255));
                    }
                }
            } else {
                while (!boundaryQueue.isEmpty()) {
                    int[] boundaryPixel = boundaryQueue.poll();
                    int x = boundaryPixel[0];
                    int y = boundaryPixel[1];
                    cvMask.drawCircle(x, y, scaledExpandPixel, boundaryPaint);
                }
            }
        }

        // Scale back to original size
        Bitmap dilatedBitmap = Bitmap.createScaledBitmap(scaledDilatedBitmap, width, height, true);

        // Create a result bitmap with a black background
        Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas cvResult = new Canvas(resultBitmap);
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setStyle(Paint.Style.FILL);
        cvResult.drawRect(0, 0, width, height, backgroundPaint);
        cvResult.drawBitmap(dilatedBitmap, 0, 0, null);

        return resultBitmap;
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

    public static Bitmap mergeBitmaps(Bitmap blackImg, Bitmap whiteImg, Bitmap maskImg) {
        int width = blackImg.getWidth();
        int height = blackImg.getHeight();

        if (width != whiteImg.getWidth() || width != maskImg.getWidth() || height != whiteImg.getHeight() || height != maskImg.getHeight()) {
            throw new IllegalArgumentException("All bitmaps must have the same dimensions");
        }

        Bitmap mergedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Define fixed segment dimensions
        int segmentWidth = 1000;
        int segmentHeight = 1000;

        for (int y = 0; y < height; y += segmentHeight) {
            for (int x = 0; x < width; x += segmentWidth) {
                int currentSegmentWidth = Math.min(segmentWidth, width - x);
                int currentSegmentHeight = Math.min(segmentHeight, height - y);

                int[] blackPixels = new int[currentSegmentWidth * currentSegmentHeight];
                int[] whitePixels = new int[currentSegmentWidth * currentSegmentHeight];
                int[] maskPixels = new int[currentSegmentWidth * currentSegmentHeight];
                int[] mergedPixels = new int[currentSegmentWidth * currentSegmentHeight];

                blackImg.getPixels(blackPixels, 0, currentSegmentWidth, x, y, currentSegmentWidth, currentSegmentHeight);
                whiteImg.getPixels(whitePixels, 0, currentSegmentWidth, x, y, currentSegmentWidth, currentSegmentHeight);
                maskImg.getPixels(maskPixels, 0, currentSegmentWidth, x, y, currentSegmentWidth, currentSegmentHeight);

                for (int i = 0; i < currentSegmentWidth * currentSegmentHeight; i++) {
                    int maskPixel = maskPixels[i];
                    int alpha = (int) Math.round(0.299d * Color.red(maskPixel) + 0.587d * Color.green(maskPixel) + 0.114d * Color.blue(maskPixel));

                    if (alpha == 255) {
                        mergedPixels[i] = whitePixels[i];
                    } else if (alpha == 0) {
                        mergedPixels[i] = blackPixels[i];
                    } else {
                        int redA = Color.red(blackPixels[i]);
                        int greenA = Color.green(blackPixels[i]);
                        int blueA = Color.blue(blackPixels[i]);

                        int redB = Color.red(whitePixels[i]);
                        int greenB = Color.green(whitePixels[i]);
                        int blueB = Color.blue(whitePixels[i]);

                        int red = (redA * (255 - alpha) + redB * alpha) / 255;
                        int green = (greenA * (255 - alpha) + greenB * alpha) / 255;
                        int blue = (blueA * (255 - alpha) + blueB * alpha) / 255;

                        mergedPixels[i] = Color.argb(255, red, green, blue);
                    }
                }

                mergedBitmap.setPixels(mergedPixels, 0, currentSegmentWidth, x, y, currentSegmentWidth, currentSegmentHeight);
            }
        }

        return mergedBitmap;
    }

    public static Bitmap changeBlackPixelsToTransparent(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < width * height; i++) {
            if (pixels[i] == Color.BLACK) {
                pixels[i] = Color.argb(0, 0, 0, 0); // Set pixel to transparent black
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    //Outer Fit Image A onto Image B
    public static Bitmap outerFit(Bitmap imageA, Bitmap imageB) {
        int widthB = imageB.getWidth();
        int heightB = imageB.getHeight();

        float aspectRatioA = (float) imageA.getWidth() / imageA.getHeight();
        float aspectRatioB = (float) widthB / heightB;

        int newWidth, newHeight;

        if (aspectRatioA > aspectRatioB) {
            // Image A is wider than Image B
            newWidth = Math.round(heightB * aspectRatioA);
            newHeight = heightB;
        } else {
            // Image A is taller than Image B
            newWidth = widthB;
            newHeight = Math.round(widthB / aspectRatioA);
        }

        Bitmap scaledImageA = Bitmap.createScaledBitmap(imageA, newWidth, newHeight, true);

        Bitmap resultBitmap = Bitmap.createBitmap(widthB, heightB, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);

        // Center the scaled imageA onto imageB
        int offsetX = (widthB - newWidth) / 2;
        int offsetY = (heightB - newHeight) / 2;

        canvas.drawBitmap(imageB, new Matrix(), null);
        canvas.drawBitmap(scaledImageA, offsetX, offsetY, null);

        return resultBitmap;
    }

    public static Bitmap getEmptyBackground(int longSide, String aspectRatio) {
        int baseWidth, baseHeight;
        if (aspectRatio.equals(Sketch.ASPECT_RATIO_SQUARE)) {
            baseWidth = baseHeight = longSide;
        } else if (aspectRatio.equals(Sketch.ASPECT_RATIO_PORTRAIT)) {
            baseWidth = (int)round(longSide * 3 / 4d);
            baseHeight = longSide;
        } else if (aspectRatio.equals(Sketch.ASPECT_RATIO_LANDSCAPE)){
            baseHeight = (int)round(longSide * 3 / 4d);
            baseWidth = longSide;
        } else {
            baseHeight = (int)round(longSide * 9 / 16d);
            baseWidth = longSide;
        }
        Bitmap resultBitmap = Bitmap.createBitmap(baseWidth, baseHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBitmap);
        Paint mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(Color.WHITE);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0f,0f,baseWidth, baseHeight, mBackgroundPaint);
        return resultBitmap;
    }
}
