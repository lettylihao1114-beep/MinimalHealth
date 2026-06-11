package com.minimalhealth.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class HealthDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "minimalhealth.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_STATS = "health_stats";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_DATE = "record_date";
    public static final String COLUMN_STEPS = "steps";
    public static final String COLUMN_HEART_RATE = "resting_hr";
    public static final String COLUMN_SLEEP = "sleep_hours";
    public static final String COLUMN_WATER = "water_ml";
    public static final String COLUMN_CALORIES = "calories";
    public static final String COLUMN_HEALTH_SCORE = "health_score";

    public HealthDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_STATS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_DATE + " TEXT NOT NULL UNIQUE, "
                + COLUMN_STEPS + " INTEGER DEFAULT 0, "
                + COLUMN_HEART_RATE + " INTEGER DEFAULT 0, "
                + COLUMN_SLEEP + " REAL DEFAULT 0.0, "
                + COLUMN_WATER + " INTEGER DEFAULT 0, "
                + COLUMN_CALORIES + " INTEGER DEFAULT 0, "
                + COLUMN_HEALTH_SCORE + " INTEGER DEFAULT 0"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATS);
        onCreate(db);
    }
}