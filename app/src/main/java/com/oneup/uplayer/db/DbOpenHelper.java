package com.oneup.uplayer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

//TODO: Store duration in database.

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
                    Song.YEAR + " INTEGER," +
                    Song.LAST_PLAYED + " INTEGER," +
                    Song.TIMES_PLAYED + " INTEGER," +
                    Song.BOOKMARKED + " INTEGER)";

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

    public void insertOrUpdateArtist(Artist artist) {
        Log.d(TAG, "DbOpenHelper.insertOrUpdateArtist(" + artist + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(Artist._ID, artist.getId());
            values.put(Artist.ARTIST, artist.getArtist());
            if (artist.getLastPlayed() == 0) {
                values.putNull(Artist.LAST_PLAYED);
            } else {
                values.put(Artist.LAST_PLAYED, artist.getLastPlayed());
            }
            if (artist.getTimesPlayed() == 0) {
                values.putNull(Artist.TIMES_PLAYED);
            } else {
                values.put(Artist.TIMES_PLAYED, artist.getTimesPlayed());
            }

            int rowsAffected = db.update(Artist.TABLE_NAME, values,
                    Artist._ID + "=" + artist.getId(), null);
            Log.d(TAG, rowsAffected + " artists updated");

            if (rowsAffected == 0) {
                db.insert(Artist.TABLE_NAME, null, values);
                Log.d(TAG, "Artist inserted");
            }
        }
    }


    public void deleteArtist(int id) {
        Log.d(TAG, "DbOpenHelper.deleteArtist(" + id + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            Log.d(TAG, db.delete(Song.TABLE_NAME, Song.ARTIST_ID + "=" + id, null) +
                    " songs deleted");

            Log.d(TAG, db.delete(Artist.TABLE_NAME, Artist._ID + "=" + id, null) +
                    " artists deleted");
        }
    }

    public void insertOrUpdateSong(Song song) {
        Log.d(TAG, "DbOpenHelper.insertOrUpdateSong(" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(Song._ID, song.getId());
            values.put(Song.TITLE, song.getTitle());
            values.put(Song.ARTIST_ID, song.getArtist().getId());
            if (song.getYear() == 0) {
                values.putNull(Song.YEAR);
            } else {
                values.put(Song.YEAR, song.getYear());
            }
            if (song.getLastPlayed() == 0) {
                values.putNull(Song.LAST_PLAYED);
            } else {
                values.put(Song.LAST_PLAYED, song.getLastPlayed());
            }
            if (song.getTimesPlayed() == 0) {
                values.putNull(Song.TIMES_PLAYED);
            } else {
                values.put(Song.TIMES_PLAYED, song.getTimesPlayed());
            }
            if (song.getBookmarked() == 0) {
                values.putNull(Song.BOOKMARKED);
            } else {
                values.put(Song.BOOKMARKED, song.getBookmarked());
            }

            int rowsAffected = db.update(Song.TABLE_NAME, values,
                    Song._ID + "=" + song.getId(), null);
            Log.d(TAG, rowsAffected + " songs updated");

            if (rowsAffected == 0) {
                db.insert(Song.TABLE_NAME, null, values);
                Log.d(TAG, "Song inserted");
            }
        }
    }

    public void deleteSong(Song song) {
        Log.d(TAG, "DbOpenHelper.deleteSong(" + song + ")");
        deleteSong(song.getId());
    }

    public void deleteSong(int id) {
        Log.d(TAG, "DbOpenHelper.deleteSong(" + id + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            Log.d(TAG, db.delete(Song.TABLE_NAME, Song._ID + "=" + id, null) +
                    " songs deleted");
        }
    }
}
