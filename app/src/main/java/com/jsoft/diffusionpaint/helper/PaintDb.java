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
                        + ", " + SketchEntry.PROMPT
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
            sketch.setImgPreview(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.PREVIEW))));
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
                        + ", " + SketchEntry.PROMPT
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
            sketch.setImgPreview(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.PREVIEW))));
            sketches.add(sketch);
        }
        c.close();
        if (sketches.size() > 0) {
            return sketches.get(0);
        } else {
            return null;
        }
    }

    public boolean deleteSketch(int sketchId) {
        return db.delete(SketchEntry.TABLE_NAME, SketchEntry._ID + "=" + sketchId, null) > 0;
    }

    public long insertSketch(Sketch sketch) {
        ContentValues values = new ContentValues();
        values.put(SketchEntry.CREATE_DATE, PaintDbHelper.getDateTime(new Date()));
        values.put(SketchEntry.LAST_UPDATE_DATE, PaintDbHelper.getDateTime(new Date()));
        values.put(SketchEntry.PROMPT, sketch.getPrompt());
        values.put(SketchEntry.PREVIEW, Utils.bitmap2Base64String(sketch.getImgPreview()));

        return db.insert(SketchEntry.TABLE_NAME,null,values);
    }

    public long updateSketch(Sketch sketch) {
        ContentValues values = new ContentValues();
        values.put(SketchEntry.LAST_UPDATE_DATE, PaintDbHelper.getDateTime(new Date()));
        values.put(SketchEntry.PROMPT, sketch.getPrompt());
        values.put(SketchEntry.PREVIEW, Utils.bitmap2Base64String(sketch.getImgPreview()));

        // Which row to update, based on the ID
        String selection = SketchEntry._ID + " LIKE ?";
        String[] selectionArgs = { sketch.getId() + "" };

        int count = db.update(
                SketchEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        return sketch.getId();
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
