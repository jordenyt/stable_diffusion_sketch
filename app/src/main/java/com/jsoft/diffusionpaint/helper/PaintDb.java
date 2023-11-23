package com.jsoft.diffusionpaint.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.jsoft.diffusionpaint.dto.Sketch;
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
                        + ", " + SketchEntry.PARENT_ID
                        + ", " + SketchEntry.CREATE_DATE
                        + ", " + SketchEntry.PROMPT
                        + " FROM " + SketchEntry.TABLE_NAME
                        + " ORDER BY " + SketchEntry.LAST_UPDATE_DATE + " DESC";
        Cursor c = db.rawQuery(queryString, new String[] {});
        List<Sketch> sketches = new ArrayList<>();
        while (c.moveToNext()) {
            Sketch sketch = new Sketch();
            sketch.setId(c.getInt(c.getColumnIndexOrThrow(SketchEntry._ID)));
            sketch.setParentId(c.getInt(c.getColumnIndexOrThrow(SketchEntry.PARENT_ID)));
            sketch.setCreateDate(PaintDbHelper.parseDateTime(c.getString(c.getColumnIndexOrThrow(SketchEntry.CREATE_DATE))));
            sketch.setPrompt(c.getString(c.getColumnIndexOrThrow(SketchEntry.PROMPT)));
            //sketch.setImgPreview(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.PREVIEW))));
            sketches.add(sketch);
        }
        c.close();
        return sketches;
    }

    public Bitmap getSketchPreview(int sketchId) {
        String queryString =
                "SELECT " + SketchEntry._ID
                        + ", " + SketchEntry.PREVIEW
                        + " FROM " + SketchEntry.TABLE_NAME
                        + " WHERE " + SketchEntry._ID + " = " + sketchId;
        Cursor c = db.rawQuery(queryString, new String[] {});
        List<Sketch> sketches = new ArrayList<>();
        while (c.moveToNext()) {
            Sketch sketch = new Sketch();
            sketch.setId(c.getInt(c.getColumnIndexOrThrow(SketchEntry._ID)));
            sketch.setImgPreview(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.PREVIEW))));
            sketches.add(sketch);
        }
        c.close();
        if (sketches.size() > 0) {
            return sketches.get(0).getImgPreview();
        } else {
            return null;
        }
    }

    public Bitmap getSketchRef(int sketchId) {
        String queryString =
                "SELECT " + SketchEntry._ID
                        + ", " + SketchEntry.REF
                        + " FROM " + SketchEntry.TABLE_NAME
                        + " WHERE " + SketchEntry._ID + " = " + sketchId;
        Cursor c = db.rawQuery(queryString, new String[] {});
        List<Sketch> sketches = new ArrayList<>();
        while (c.moveToNext()) {
            Sketch sketch = new Sketch();
            sketch.setId(c.getInt(c.getColumnIndexOrThrow(SketchEntry._ID)));
            sketch.setImgReference(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.REF))));
            sketches.add(sketch);
        }
        c.close();
        if (sketches.size() > 0) {
            return sketches.get(0).getImgReference();
        } else {
            return null;
        }
    }

    public Bitmap getSketchBg(int sketchId) {
        String queryString =
                "SELECT " + SketchEntry._ID
                        + ", " + SketchEntry.BACKGROUND
                        + " FROM " + SketchEntry.TABLE_NAME
                        + " WHERE " + SketchEntry._ID + " = " + sketchId;
        Cursor c = db.rawQuery(queryString, new String[] {});
        CursorWindow cw = new CursorWindow("sketchBg", 5000000);
        AbstractWindowedCursor ac = (AbstractWindowedCursor) c;
        ac.setWindow(cw);

        List<Sketch> sketches = new ArrayList<>();
        while (ac.moveToNext()) {
            Sketch sketch = new Sketch();
            sketch.setId(ac.getInt(ac.getColumnIndexOrThrow(SketchEntry._ID)));
            sketch.setImgBackground(Utils.base64String2Bitmap(ac.getString(ac.getColumnIndexOrThrow(SketchEntry.BACKGROUND))));
            sketches.add(sketch);
        }
        ac.close();
        if (sketches.size() > 0) {
            return sketches.get(0).getImgBackground();
        } else {
            return null;
        }
    }

    public int getSketchParent(int sketchId) {
        String queryString =
                "SELECT " + SketchEntry._ID
                        + ", " + SketchEntry.PARENT_ID
                        + " FROM " + SketchEntry.TABLE_NAME
                        + " WHERE " + SketchEntry._ID + " = " + sketchId;
        Cursor c = db.rawQuery(queryString, new String[] {});
        List<Sketch> sketches = new ArrayList<>();
        while (c.moveToNext()) {
            Sketch sketch = new Sketch();
            sketch.setId(c.getInt(c.getColumnIndexOrThrow(SketchEntry._ID)));
            sketch.setParentId(c.getInt(c.getColumnIndexOrThrow(SketchEntry.PARENT_ID)));
            sketches.add(sketch);
        }
        c.close();
        if (sketches.size() > 0) {
            return sketches.get(0).getParentId();
        } else {
            return -1;
        }
    }

    public Sketch getSketch(int sketchId) {
        String queryString =
                "SELECT " + SketchEntry._ID
                        + ", " + SketchEntry.PARENT_ID
                        + ", " + SketchEntry.CREATE_DATE
                        + ", " + SketchEntry.LAST_UPDATE_DATE
                        + ", " + SketchEntry.PREVIEW
                        + ", " + SketchEntry.PAINT
                        + ", " + SketchEntry.MASK
                        + ", " + SketchEntry.PROMPT
                        + ", " + SketchEntry.NEG_PROMPT
                        + ", " + SketchEntry.CN_MODE
                        + ", " + SketchEntry.EXIF
                        + " FROM " + SketchEntry.TABLE_NAME
                        + " WHERE " + SketchEntry._ID + " = " + sketchId;
        Cursor c = db.rawQuery(queryString, new String[] {});
        List<Sketch> sketches = new ArrayList<>();
        while (c.moveToNext()) {
            Sketch sketch = new Sketch();
            sketch.setId(c.getInt(c.getColumnIndexOrThrow(SketchEntry._ID)));
            sketch.setParentId(c.getInt(c.getColumnIndexOrThrow(SketchEntry.PARENT_ID)));
            sketch.setCreateDate(PaintDbHelper.parseDateTime(c.getString(c.getColumnIndexOrThrow(SketchEntry.CREATE_DATE))));
            sketch.setLastUpdateDate(PaintDbHelper.parseDateTime(c.getString(c.getColumnIndexOrThrow(SketchEntry.LAST_UPDATE_DATE))));
            sketch.setPrompt(c.getString(c.getColumnIndexOrThrow(SketchEntry.PROMPT)));
            sketch.setNegPrompt(c.getString(c.getColumnIndexOrThrow(SketchEntry.NEG_PROMPT)));
            sketch.setCnMode(c.getString(c.getColumnIndexOrThrow(SketchEntry.CN_MODE)));
            sketch.setImgPreview(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.PREVIEW))));
            sketch.setImgPaint(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.PAINT))));
            sketch.setImgInpaintMask(Utils.base64String2Bitmap(c.getString(c.getColumnIndexOrThrow(SketchEntry.MASK))));
            sketch.setExif(c.getString(c.getColumnIndexOrThrow(SketchEntry.EXIF)));
            sketches.add(sketch);
        }
        c.close();
        if (sketches.size() > 0) {
            Sketch result = sketches.get(0);
            result.setImgReference(getSketchRef(sketchId));
            result.setImgBackground(getSketchBg(sketchId));
            return result;
        } else {
            return null;
        }
    }

    private int updateChildUponDelete(int deleteId, int parentId) {
        ContentValues values = new ContentValues();
        values.put(SketchEntry.PARENT_ID, parentId);
        String selection = SketchEntry.PARENT_ID + "=" + deleteId;

        return db.update(
                SketchEntry.TABLE_NAME,
                values,
                selection,
                null);
    }

    public void deleteSketch(int sketchId) {
        int parentId = getSketchParent(sketchId);
        updateChildUponDelete(sketchId, parentId);
        db.delete(SketchEntry.TABLE_NAME, SketchEntry._ID + "=" + sketchId, null);
    }

    public void deleteGroup(List<Sketch> sketchID) {
        String whereClause = "";
        for (int i=0;i<sketchID.size();i++) {
            int id = sketchID.get(i).getId();
            whereClause += SketchEntry._ID + "=" + id;
            if (i < sketchID.size() - 1) {
                whereClause += " OR ";
            }
        }
        db.delete(SketchEntry.TABLE_NAME, whereClause, null);
    }

    public boolean clearSketch() {
        return db.delete(SketchEntry.TABLE_NAME, null, null) > 0;
    }

    public long insertSketch(Sketch sketch) {
        ContentValues values = new ContentValues();
        values.put(SketchEntry.PARENT_ID, sketch.getParentId());
        values.put(SketchEntry.CREATE_DATE, PaintDbHelper.getDateTime(new Date()));
        values.put(SketchEntry.LAST_UPDATE_DATE, PaintDbHelper.getDateTime(new Date()));
        values.put(SketchEntry.PROMPT, sketch.getPrompt());
        values.put(SketchEntry.NEG_PROMPT, sketch.getNegPrompt());
        values.put(SketchEntry.CN_MODE, sketch.getCnMode());
        values.put(SketchEntry.PREVIEW, Utils.jpg2Base64String(sketch.getImgPreview()));
        values.put(SketchEntry.BACKGROUND, Utils.jpg2Base64String(sketch.getImgBackground()));
        values.put(SketchEntry.PAINT, Utils.png2Base64String(sketch.getImgPaint()));
        values.put(SketchEntry.MASK, Utils.png2Base64String(sketch.getImgInpaintMask()));
        values.put(SketchEntry.REF, Utils.jpg2Base64String(sketch.getImgReference()));
        values.put(SketchEntry.EXIF, sketch.getExif());
        return db.insert(SketchEntry.TABLE_NAME,null,values);
    }

    public int updateSketch(Sketch sketch) {
        ContentValues values = new ContentValues();
        values.put(SketchEntry.LAST_UPDATE_DATE, PaintDbHelper.getDateTime(new Date()));
        values.put(SketchEntry.PROMPT, sketch.getPrompt());
        values.put(SketchEntry.NEG_PROMPT, sketch.getNegPrompt());
        values.put(SketchEntry.CN_MODE, sketch.getCnMode());
        values.put(SketchEntry.PREVIEW, Utils.jpg2Base64String(sketch.getImgPreview()));
        values.put(SketchEntry.BACKGROUND, Utils.jpg2Base64String(sketch.getImgBackground()));
        values.put(SketchEntry.PAINT, Utils.png2Base64String(sketch.getImgPaint()));
        values.put(SketchEntry.MASK, Utils.png2Base64String(sketch.getImgInpaintMask()));
        values.put(SketchEntry.REF, Utils.jpg2Base64String(sketch.getImgReference()));
        values.put(SketchEntry.EXIF, sketch.getExif());
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
