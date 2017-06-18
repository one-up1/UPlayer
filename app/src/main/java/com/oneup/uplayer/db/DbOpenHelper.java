package com.oneup.uplayer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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
                    Song.DURATION + " INTEGER," +
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
            db.beginTransaction();
            try {
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

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }


    public void deleteArtist(int id) {
        Log.d(TAG, "DbOpenHelper.deleteArtist(" + id + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                Log.d(TAG, db.delete(Song.TABLE_NAME, Song.ARTIST_ID + "=" + id, null) +
                        " songs deleted");

                Log.d(TAG, db.delete(Artist.TABLE_NAME, Artist._ID + "=" + id, null) +
                        " artists deleted");

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void querySong(Song song) {
        Log.d(TAG, "DbOpenHelper.querySong(" + song + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(Artist.TABLE_NAME,
                    new String[]{Artist.LAST_PLAYED, Artist.TIMES_PLAYED},
                    Artist._ID + "=" + song.getArtist().getId(), null, null, null, null)) {
                if (c.moveToFirst()) {
                    song.getArtist().setLastPlayed(c.getLong(0));
                    song.getArtist().setTimesPlayed(c.getInt(1));
                } else {
                    Log.d(TAG, "Artist not found");
                }
            }

            try (Cursor c = db.query(Song.TABLE_NAME,
                    new String[]{Song.LAST_PLAYED, Song.TIMES_PLAYED, Song.BOOKMARKED},
                    Song._ID + "=" + song.getId(), null, null, null, null)) {
                if (c.moveToFirst()) {
                    song.setLastPlayed(c.getLong(0));
                    song.setTimesPlayed(c.getInt(1));
                    song.setBookmarked(c.getLong(2));
                } else {
                    Log.d(TAG, "Song not found");
                }
            }

            Log.d(TAG, "Times played: " + song.getArtist().getTimesPlayed() +
                    ":" + song.getTimesPlayed());
        }
    }

    public void insertOrUpdateSong(Song song) {
        Log.d(TAG, "DbOpenHelper.insertOrUpdateSong(" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                values.put(Song._ID, song.getId());
                values.put(Song.TITLE, song.getTitle());
                values.put(Song.ARTIST_ID, song.getArtist().getId());
                if (song.getDuration() == 0) {
                    values.putNull(Song.DURATION);
                } else {
                    values.put(Song.DURATION, song.getDuration());
                }
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

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
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
            db.beginTransaction();
            try {
                Log.d(TAG, db.delete(Song.TABLE_NAME, Song._ID + "=" + id, null) +
                        " songs deleted");

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public static int queryInt(SQLiteDatabase db, String sql) {
        try (Cursor c = db.rawQuery(sql, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /*public void t(Context context) {
        Log.d(TAG, "Starting");
        try {
            try (SQLiteDatabase db = getWritableDatabase()) {
                Log.d(TAG, "Altering table");
                db.beginTransaction();
                try {
                    Log.d(TAG, "Renaming table");
                    db.execSQL("ALTER TABLE " + Song.TABLE_NAME +
                            " RENAME TO tmp_" + Song.TABLE_NAME);

                    Log.d(TAG, "Creating new table");
                    db.execSQL(SQL_CREATE_SONGS);

                    Log.d(TAG, "Copying data");
                    db.execSQL("INSERT INTO " + Song.TABLE_NAME + "(" +
                            Song._ID + "," +
                            Song.TITLE + "," +
                            Song.ARTIST_ID + "," +
                            Song.YEAR + "," +
                            Song.LAST_PLAYED + "," +
                            Song.TIMES_PLAYED + "," +
                            Song.BOOKMARKED + ")" +
                            "SELECT " +
                            Song._ID + "," +
                            Song.TITLE + "," +
                            Song.ARTIST_ID + "," +
                            Song.YEAR + "," +
                            Song.LAST_PLAYED + "," +
                            Song.TIMES_PLAYED + "," +
                            Song.BOOKMARKED + " " +
                            "FROM tmp_" + Song.TABLE_NAME);

                    Log.d(TAG, "Dropping old table");
                    db.execSQL("DROP TABLE tmp_" + Song.TABLE_NAME);

                    Log.d(TAG, "Success!");
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                Log.d(TAG, "Setting durations");
                db.beginTransaction();
                try {
                    try (android.database.Cursor c = db.query(Song.TABLE_NAME,
                            new String[]{Song._ID}, null, null, null, null, null)) {
                        while (c.moveToNext()) {
                            Song song = new Song();
                            song.setId(c.getInt(0));

                            if (!setSongDuration(context, song)) {
                                continue;
                            }

                            Log.d(TAG, "Setting duration of " + song.getId() +
                                    " to " + song.getDuration());
                            ContentValues values = new ContentValues();
                            values.put(Song.DURATION, song.getDuration());
                            Log.d(TAG, db.update(Song.TABLE_NAME, values,
                                    Song._ID + "=" + song.getId(), null) + " rows affected");
                        }
                    }

                    Log.d(TAG, "Success!");
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            Log.d(TAG, "Done!");
        } catch (Exception ex) {
            Log.e(TAG, "Ughh", ex);
        }
    }

    private boolean setSongDuration(Context context, Song song) {
        try {
            android.media.MediaMetadataRetriever mediaMetadataRetriever =
                    new android.media.MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(context, song.getContentUri());
            song.setDuration(Integer.parseInt(mediaMetadataRetriever.extractMetadata(
                    android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)));
            mediaMetadataRetriever.release();
            return true;
        } catch (Exception ex) {
            Log.e(TAG, "Error getting duration of " + song, ex);
            return false;
        }
    }*/
}
