package com.minimalhealth.app;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HealthDataProvider extends ContentProvider {

    private static final String TAG = "HealthDataProvider";
    private static final String AUTHORITY = "com.minimalhealth.app.healthprovider";
    private static final String TABLE_PATH = "stats";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_PATH);

    private static final int STATS_ALL = 1;
    private static final int STATS_SINGLE = 2;

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI(AUTHORITY, TABLE_PATH, STATS_ALL);
        URI_MATCHER.addURI(AUTHORITY, TABLE_PATH + "/#", STATS_SINGLE);
    }

    private HealthDatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new HealthDatabaseHelper(getContext());
        Log.d(TAG, "HealthDataProvider created, URI: " + CONTENT_URI);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;
        switch (URI_MATCHER.match(uri)) {
            case STATS_ALL:
                cursor = db.query(HealthDatabaseHelper.TABLE_STATS,
                        projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case STATS_SINGLE:
                long id = ContentUris.parseId(uri);
                cursor = db.query(HealthDatabaseHelper.TABLE_STATS,
                        projection, HealthDatabaseHelper.COLUMN_ID + "=?",
                        new String[]{String.valueOf(id)}, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case STATS_ALL:
                return "vnd.android.cursor.dir/vnd.com.minimalhealth.stats";
            case STATS_SINGLE:
                return "vnd.android.cursor.item/vnd.com.minimalhealth.stats";
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (URI_MATCHER.match(uri) != STATS_ALL) {
            throw new IllegalArgumentException("Invalid URI for insert: " + uri);
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insertWithOnConflict(HealthDatabaseHelper.TABLE_STATS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
        if (id > 0 && getContext() != null) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, id);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted;
        switch (URI_MATCHER.match(uri)) {
            case STATS_ALL:
                deleted = db.delete(HealthDatabaseHelper.TABLE_STATS, selection, selectionArgs);
                break;
            case STATS_SINGLE:
                long id = ContentUris.parseId(uri);
                deleted = db.delete(HealthDatabaseHelper.TABLE_STATS,
                        HealthDatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(id)});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (deleted > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return deleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int updated;
        switch (URI_MATCHER.match(uri)) {
            case STATS_ALL:
                updated = db.update(HealthDatabaseHelper.TABLE_STATS, values, selection, selectionArgs);
                break;
            case STATS_SINGLE:
                long id = ContentUris.parseId(uri);
                updated = db.update(HealthDatabaseHelper.TABLE_STATS, values,
                        HealthDatabaseHelper.COLUMN_ID + "=?", new String[]{String.valueOf(id)});
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        if (updated > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return updated;
    }
}