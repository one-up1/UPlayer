package com.oneup.uplayer.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.oneup.uplayer.db.obj.Artist;
import com.oneup.uplayer.db.obj.Song;

public class DbOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "UPlayer";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "UPlayer.db";

    private static final String SQL_CREATE_ARTISTS =
            "CREATE TABLE " + Artist.TABLE_NAME + "(" +
            Artist._ID + " INTEGER PRIMARY KEY," +
            Artist.ARTIST + " TEXT," +
            Artist.LAST_PLAYED + " INTEGER," +
            Artist.TIMES_PLAYED + " INTEGER)";

    private static final String SQL_CREATE_SONGS =
            "CREATE TABLE " + Song.TABLE_NAME + "(" +
            Song._ID + " INTEGER PRIMARY KEY," +
            Song.TITLE + " TEXT," +
            Song.ARTIST_ID + " INTEGER," +
            Song.ARTIST + " TEXT," +
            Song.YEAR + " INTEGER," +
            Song.LAST_PLAYED + " INTEGER," +
            Song.TIMES_PLAYED + " INTEGER," +
            Song.STARRED + " INTEGER)";

    public DbOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "DbOpenHelper.onCreate()");

        db.execSQL(SQL_CREATE_ARTISTS);
        Log.d(TAG, "Created artists table");

        db.execSQL(SQL_CREATE_SONGS);
        Log.d(TAG, "Created songs table");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
