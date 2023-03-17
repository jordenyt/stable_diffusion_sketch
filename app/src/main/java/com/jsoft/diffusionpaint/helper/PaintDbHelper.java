package com.jsoft.diffusionpaint.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaintDbHelper extends SQLiteOpenHelper {

    public static abstract class SketchEntry implements BaseColumns {
        public static final String TABLE_NAME = "SD_SKETCH";
        public static final String _ID = "_id";
        public static final String CREATE_DATE = "create_date";
        public static final String LAST_UPDATE_DATE = "last_update_date";
        public static final String PREVIEW = "img_preview";
        public static final String PROMPT = "prompt";
    }

    // Database Information
    static final String DB_NAME = "DIFFUSION_PAINT.DB";

    // database version
    static final int DB_VERSION = 1;

    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // Creating table query
    private static final String CREATE_TABLE = "create table " + SketchEntry.TABLE_NAME + "("
            + SketchEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + SketchEntry.CREATE_DATE + " TEXT NOT NULL, "
            + SketchEntry.LAST_UPDATE_DATE + " TEXT NOT NULL, "
            + SketchEntry.PREVIEW + " TEXT NOT NULL, "
            + SketchEntry.PROMPT + " TEXT);";

    public PaintDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + SketchEntry.TABLE_NAME);
        onCreate(db);
    }

    public static String getDateTime(Date date) {
        return dateFormat.format(date);
    }

    public static Date parseDateTime(String dbDate) {
        Date d = new Date();
        try {
            d = dateFormat.parse(dbDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return d;
    }
}
