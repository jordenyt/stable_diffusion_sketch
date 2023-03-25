package com.jsoft.diffusionpaint.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.jsoft.diffusionpaint.helper.PaintDbHelper.SketchEntry;

public class PaintDb {

    private SQLiteDatabase db;

    public PaintDb(Context context) {
        PaintDbHelper mDbHelper = new PaintDbHelper(context);
        db = mDbHelper.getWritableDatabase();
    }

    public List<Sketch> getSketchList() {
        String queryString =
                "SELECT " + SketchEntry._ID
                        + ", " + SketchEntry.CREATE_DATE
                        + ", " + SketchEntry.LAST_UPDATE_DATE
                        + ", " + SketchEntry.PREVIEW
                        + ", " + SketchEntry.BACKGROUND
                        + ", " + SketchEntry.PAINT
                        + ", " + SketchEntry.MASK
                        + ", " + SketchEntry.PROMPT
                        + ", " + SketchEntry.CN_MODE
                        + " FROM " + SketchEntry.TABLE_NAME
                        + " ORDER BY " + SketchEntry.LAST_UPDATE_DATE + " DESC";
        Cursor c = db.rawQuery(queryString, new String[] {});
        List<Sketch> sketches = new ArrayList<>();
        while (c.moveToNext()) {
            Sketch sketch = new Sketch();
            sketch.setId(c.getInt(c.getColumnIndexOrThrow(SketchEntry._ID)));
            sketch.setCreateDate(PaintDbHelper.parseDateTime(c.getString(c.getColumnIndexOrThrow(SketchEntry.CREATE_DATE))));
            sketch.setLastUpdateDate(PaintDbHelper.parseDateTime(c.getString(c.getColumnIndexOrThrow(SketchEntry.LAST_UPDATE_DATE))));
            sketch.setPrompt(c.getString(c.getColumnIndexOrThrow(SketchEntry.PROMPT)));
            sketch.setCnMode(c.getString(c.getColumnIndexOrThrow(SketchEntry.CN_MODE)));
            sketch.setImgPreview(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.PREVIEW))));
            sketch.setImgBackground(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.BACKGROUND))));
            sketch.setImgPaint(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.PAINT))));
            sketch.setImgInpaint(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.MASK))));
            sketches.add(sketch);
        }
        c.close();
        return sketches;
    }

    public Sketch getSketch(int sketchId) {
        String queryString =
                "SELECT " + SketchEntry._ID
                        + ", " + SketchEntry.CREATE_DATE
                        + ", " + SketchEntry.LAST_UPDATE_DATE
                        + ", " + SketchEntry.PREVIEW
                        + ", " + SketchEntry.BACKGROUND
                        + ", " + SketchEntry.PAINT
                        + ", " + SketchEntry.MASK
                        + ", " + SketchEntry.PROMPT
                        + ", " + SketchEntry.CN_MODE
                        + " FROM " + SketchEntry.TABLE_NAME
                        + " WHERE " + SketchEntry._ID + " = " + sketchId;
        Cursor c = db.rawQuery(queryString, new String[] {});
        List<Sketch> sketches = new ArrayList<>();
        while (c.moveToNext()) {
            Sketch sketch = new Sketch();
            sketch.setId(c.getInt(c.getColumnIndexOrThrow(SketchEntry._ID)));
            sketch.setCreateDate(PaintDbHelper.parseDateTime(c.getString(c.getColumnIndexOrThrow(SketchEntry.CREATE_DATE))));
            sketch.setLastUpdateDate(PaintDbHelper.parseDateTime(c.getString(c.getColumnIndexOrThrow(SketchEntry.LAST_UPDATE_DATE))));
            sketch.setPrompt(c.getString(c.getColumnIndexOrThrow(SketchEntry.PROMPT)));
            sketch.setCnMode(c.getString(c.getColumnIndexOrThrow(SketchEntry.CN_MODE)));
            sketch.setImgPreview(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.PREVIEW))));
            sketch.setImgBackground(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.BACKGROUND))));
            sketch.setImgPaint(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.PAINT))));
            sketch.setImgInpaint(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.MASK))));
            sketches.add(sketch);
        }
        c.close();
        if (sketches.size() > 0) {
            return sketches.get(0);
        } else {
            return null;
        }
    }

    public void deleteSketch(int sketchId) {
        db.delete(SketchEntry.TABLE_NAME, SketchEntry._ID + "=" + sketchId, null);
    }

    public boolean clearSketch() {
        return db.delete(SketchEntry.TABLE_NAME, null, null) > 0;
    }

    public long insertSketch(Sketch sketch) {
        ContentValues values = new ContentValues();
        values.put(SketchEntry.CREATE_DATE, PaintDbHelper.getDateTime(new Date()));
        values.put(SketchEntry.LAST_UPDATE_DATE, PaintDbHelper.getDateTime(new Date()));
        values.put(SketchEntry.PROMPT, sketch.getPrompt());
        values.put(SketchEntry.CN_MODE, sketch.getCnMode());
        values.put(SketchEntry.PREVIEW, Utils.jpg2Base64String(sketch.getImgPreview()));
        values.put(SketchEntry.BACKGROUND, Utils.jpg2Base64String(sketch.getImgBackground()));
        values.put(SketchEntry.PAINT, Utils.png2Base64String(sketch.getImgPaint()));
        values.put(SketchEntry.MASK, Utils.png2Base64String(sketch.getImgInpaint()));
        return db.insert(SketchEntry.TABLE_NAME,null,values);
    }

    public int updateSketch(Sketch sketch) {
        ContentValues values = new ContentValues();
        values.put(SketchEntry.LAST_UPDATE_DATE, PaintDbHelper.getDateTime(new Date()));
        values.put(SketchEntry.PROMPT, sketch.getPrompt());
        values.put(SketchEntry.CN_MODE, sketch.getCnMode());
        values.put(SketchEntry.PREVIEW, Utils.jpg2Base64String(sketch.getImgPreview()));
        values.put(SketchEntry.BACKGROUND, Utils.jpg2Base64String(sketch.getImgBackground()));
        values.put(SketchEntry.PAINT, Utils.png2Base64String(sketch.getImgPaint()));
        values.put(SketchEntry.MASK, Utils.png2Base64String(sketch.getImgInpaint()));
        // Which row to update, based on the ID
        String selection = SketchEntry._ID + " LIKE ?";
        String[] selectionArgs = { sketch.getId() + "" };

        return db.update(
                SketchEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
    }

    public int getId4rowid(long rowid) {
        String queryString =
                "SELECT " + SketchEntry._ID
                        + " FROM " + SketchEntry.TABLE_NAME
                        + " WHERE rowid = ?";
        Cursor c = db.rawQuery(queryString, new String[] {rowid + ""});
        int result = -1;
        if (c.getCount() > 0) {
            c.moveToFirst();
            result = c.getInt(
                    c.getColumnIndexOrThrow(SketchEntry._ID)
            );
        }
        c.close();
        return result;
    }


}
