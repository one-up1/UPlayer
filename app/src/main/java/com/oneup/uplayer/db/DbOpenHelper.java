package com.oneup.uplayer.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DbOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "UPlayer";

    private static final String DATABASE_NAME = "timers";
    private static final int DATABASE_VERSION = 1;

    private static final String PLAYED_SONGS = "played_songs";

    public DbOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "DbOpenHelper.onCreate()");

        db.execSQL("CREATE TABLE " + PLAYED_SONGS + "(" +
                PlayedSong.ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                PlayedSong.MEDIA_ID + " INTEGER NOT NULL," +
                PlayedSong.LAST_PLAYED + " INTEGER NOT NULL," +
                PlayedSong.TIMES_PLAYED + " INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public List<PlayedSong> queryPlayedSongs() {
        Log.d(TAG, "DbOpenHelper.queryPlayedSongs()");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(PLAYED_SONGS, null, null, null, null, null,
                    PlayedSong.LAST_PLAYED + " DESC")) {
                List<PlayedSong> ret = new ArrayList<>();
                while (c.moveToNext()) {
                    ret.add(new PlayedSong(c));
                }
                Log.d(TAG, ret.size() + " played songs queried");
                return ret;
            }
        }
    }

    public PlayedSong queryPlayedSong(long mediaId) {
        Log.d(TAG, "DbOpenHelper.queryPlayedSong(" + mediaId + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(PLAYED_SONGS, null, PlayedSong.MEDIA_ID + "=?",
                    new String[] { Long.toString(mediaId) }, null, null, null)) {
                return c.moveToNext() ? new PlayedSong(c) : null;
            }
        }
    }

    public void insertPlayedSong(PlayedSong playedSong) {
        Log.d(TAG, "DbOpenHelper.insertPlayedSong()");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                playedSong.id = (int)db.insert(PLAYED_SONGS, null, playedSong.getValues());
                Log.d(TAG, "Played song inserted, id=" + playedSong.id);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void updatePlayedSong(PlayedSong playedSong) {
        Log.d(TAG, "DbOpenHelper.updatePlayedSong()");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                db.update(PLAYED_SONGS, playedSong.getValues(), PlayedSong.ID + "=?",
                        new String[] { Integer.toString(playedSong.id) } );
                Log.d(TAG, "Played song updated");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }
}
