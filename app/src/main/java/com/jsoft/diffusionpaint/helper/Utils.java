package com.jsoft.diffusionpaint.helper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
        byte[] preview = Base64.decode(s, Base64.DEFAULT);
        return (BitmapFactory.decodeByteArray(preview, 0, preview.length));
    }

    public static String bitmap2Base64String(Bitmap bm) {
        ByteArrayOutputStream byteArrayOutputStream  = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream ); //bm is the bitmap object
        byte[] b = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    public static void saveBitmapToExternalStorage(Activity a, Bitmap bitmap, String filename) {
        // Get the directory for the user's public pictures directory.
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        // Create the file object.
        File file = new File(directory, filename);

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
            // Show a toast message indicating that the save was successful.
            Toast.makeText(a, "Image saved successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            // Show a toast message indicating that the save failed.
            Toast.makeText(a, "Error saving image", Toast.LENGTH_SHORT).show();
        }
    }
}
