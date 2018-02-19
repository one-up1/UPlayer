package com.oneup.uplayer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.oneup.uplayer.util.Calendar;

import java.util.ArrayList;
import java.util.List;

public class DbOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "UPlayer";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "UPlayer.db";

    private static final String SQL_CREATE_ARTISTS =
            "CREATE TABLE " + Artist.TABLE_NAME + "(" +
                    Artist._ID + " INTEGER PRIMARY KEY," +
                    Artist.ARTIST + " TEXT," +
                    Artist.LAST_PLAYED + " INTEGER," +
                    Artist.TIMES_PLAYED + " INTEGER," +
                    Artist.DATE_MODIFIED + " INTEGER)";

    private static final String SQL_CREATE_SONGS =
            "CREATE TABLE " + Song.TABLE_NAME + "(" +
                    Song._ID + " INTEGER PRIMARY KEY," +
                    Song.TITLE + " TEXT," +
                    Song.ARTIST_ID + " INTEGER," +
                    Song.DATE_ADDED + " INTEGER," +
                    Song.YEAR + " INTEGER," +
                    Song.DURATION + " INTEGER," +
                    Song.LAST_PLAYED + " INTEGER," +
                    Song.TIMES_PLAYED + " INTEGER," +
                    Song.BOOKMARKED + " INTEGER," +
                    Song.TAG + " TEXT)";

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
                if (artist.getDateModified() == 0) {
                    values.putNull(Artist.DATE_MODIFIED);
                } else {
                    values.put(Artist.DATE_MODIFIED, artist.getDateModified());
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
                    new String[]{Artist.LAST_PLAYED, Artist.TIMES_PLAYED, Artist.DATE_MODIFIED},
                    Artist._ID + "=" + song.getArtist().getId(), null, null, null, null)) {
                if (c.moveToFirst()) {
                    song.getArtist().setLastPlayed(c.getLong(0));
                    song.getArtist().setTimesPlayed(c.getInt(1));
                    song.getArtist().setDateModified(c.getLong(2));
                } else {
                    Log.d(TAG, "Artist not found");
                }
            }

            try (Cursor c = db.query(Song.TABLE_NAME,
                    new String[]{Song.DATE_ADDED,
                            Song.LAST_PLAYED, Song.TIMES_PLAYED, Song.BOOKMARKED, Song.TAG},
                    Song._ID + "=" + song.getId(), null, null, null, null)) {
                if (c.moveToFirst()) {
                    song.setDateAdded(c.getLong(0));
                    song.setLastPlayed(c.getLong(1));
                    song.setTimesPlayed(c.getInt(2));
                    song.setBookmarked(c.getLong(3));
                    song.setTag(c.getString(4));
                } else {
                    Log.d(TAG, "Song not found");
                }
            }

            Log.d(TAG, "Times played: " + song.getArtist().getTimesPlayed() +
                    ":" + song.getTimesPlayed());
        }
    }

    public  String[] querySongTags() {
        Log.d(TAG, "DbOpenHelper.querySongTags()");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(true, Song.TABLE_NAME, new String[]{Song.TAG},
                    Song.TAG + " IS NOT NULL", null, null, null, Song.TAG, null)) {
                List<String> ret = new ArrayList<>();
                while (c.moveToNext()) {
                    ret.add(c.getString(0));
                }
                Log.d(TAG, "Queried " + ret.size() + " song tags");
                return ret.toArray(new String[0]);
            }
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
                if (song.getDateAdded() == 0) {
                    values.putNull(Song.DATE_ADDED);
                } else {
                    values.put(Song.DATE_ADDED, song.getDateAdded());
                }
                if (song.getYear() == 0) {
                    values.putNull(Song.YEAR);
                } else {
                    values.put(Song.YEAR, song.getYear());
                }
                if (song.getDuration() == 0) {
                    values.putNull(Song.DURATION);
                } else {
                    values.put(Song.DURATION, song.getDuration());
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
                if (song.getTag() == null) {
                    values.putNull(Song.TAG);
                } else {
                    values.put(Song.TAG, song.getTag());
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

        if (song.getDateAdded() > song.getArtist().getDateModified()) {
            Log.d(TAG, "Updating date modified of artist " + song.getArtist());
            song.getArtist().setDateModified(song.getDateAdded());
            insertOrUpdateArtist(song.getArtist());
        }
    }

    public void updateSongPlayed(Song song) {
        querySong(song);
        long lastPlayed = Calendar.currentTime();

        song.getArtist().setLastPlayed(lastPlayed);
        song.getArtist().setTimesPlayed(song.getArtist().getTimesPlayed() + 1);
        insertOrUpdateArtist(song.getArtist());

        song.setLastPlayed(lastPlayed);
        song.setTimesPlayed(song.getTimesPlayed() + 1);
        insertOrUpdateSong(song);
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
                Artist artist;
                try (Cursor c = db.query(Artist.TABLE_NAME, null, Artist.ARTIST + "=?",
                        new String[]{"P!nk"}, null, null, null)) {
                    if (c.moveToNext()) {
                        artist = new Artist();
                        artist.setId(c.getInt(0));
                        artist.setArtist(c.getString(1));
                        artist.setLastPlayed(c.getLong(2));
                        artist.setTimesPlayed(75);
                        artist.setDateModified(1518503574);
                    } else {
                        throw new RuntimeException("Artist not found");
                    }
                }

                Log.d(TAG, "Got artist: " + artist.getId());
                insertOrUpdateArtist(artist);

                Song song;

                song = getSong(context, artist, "So What");
                song.setYear(2008);
                song.setDuration(216900);
                song.setLastPlayed(1508008597);
                song.setTimesPlayed(6);
                insertOrUpdateSong(song);

                song = getSong(context, artist, "Sober");
                song.setDuration(253623);
                song.setLastPlayed(1498851145);
                song.setTimesPlayed(1);
                insertOrUpdateSong(song);

                song = getSong(context, artist, "U & Ur Hand");
                song.setYear(2006);
                song.setDuration(214439);
                song.setLastPlayed(1518627129);
                song.setTimesPlayed(20);
                insertOrUpdateSong(song);

                song = getSong(context, artist, "Blow Me (One Last Kiss)");
                song.setDuration(227587);
                song.setLastPlayed(1498850891);
                song.setTimesPlayed(1);
                insertOrUpdateSong(song);

                song = getSong(context, artist, "Try");
                song.setYear(2012);
                song.setDuration(247954);
                song.setLastPlayed(1518626914);
                song.setTimesPlayed(4);
                insertOrUpdateSong(song);
            }
            Log.d(TAG, "Done!");
        } catch (Exception ex) {
            Log.e(TAG, "Ughh", ex);
        }
    }

    private Song getSong(Context context, Artist artist, String title) {
        try (Cursor c = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{Song._ID, Song.TITLE,
                        Song.ARTIST_ID, Song.DATE_ADDED, Song.YEAR, Song.DURATION},
                Song.TITLE + "=? AND " + Song.ARTIST_ID + "=?",
                new String[]{title, Integer.toString(artist.getId())}, null)) {
            int iId = c.getColumnIndex(Song._ID);
            int iTitle = c.getColumnIndex(Song.TITLE);
            int iDateAdded = c.getColumnIndex(Song.DATE_ADDED);
            int iYear = c.getColumnIndex(Song.YEAR);
            int iDuration = c.getColumnIndex(Song.DURATION);
            if (c.moveToNext()) {
                Song song = new Song();
                song.setTitle(c.getString(iTitle));
                song.setArtist(artist);
                song.setId(c.getInt(iId));
                //song.setDateAdded(c.getLong(iDateAdded));
                song.setYear(c.getInt(iYear));
                song.setDuration(c.getInt(iDuration));
                return song;
            } else {
                throw new RuntimeException("Not found: " + title);
            }
        }
    }*/

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
                            Song.DURATION + "," +
                            Song.LAST_PLAYED + "," +
                            Song.TIMES_PLAYED + "," +
                            Song.BOOKMARKED + "," +
                            Song.TAG + ")" +
                            "SELECT " +
                            Song._ID + "," +
                            Song.TITLE + "," +
                            Song.ARTIST_ID + "," +
                            Song.YEAR + "," +
                            Song.DURATION + "," +
                            Song.LAST_PLAYED + "," +
                            Song.TIMES_PLAYED + "," +
                            Song.BOOKMARKED + "," +
                            Song.TAG + " " +
                            "FROM tmp_" + Song.TABLE_NAME);

                    Log.d(TAG, "Dropping old table");
                    db.execSQL("DROP TABLE tmp_" + Song.TABLE_NAME);

                    Log.d(TAG, "Success!");
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                Log.d(TAG, "Setting dates added");
                db.beginTransaction();
                try {
                    try (android.database.Cursor c = db.query(Song.TABLE_NAME,
                            new String[]{Song._ID}, null, null, null, null, null)) {
                        while (c.moveToNext()) {
                            Song song = new Song();
                            song.setId(c.getInt(0));

                            try (Cursor c2 = context.getContentResolver().query(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    new String[]{Song.DATE_ADDED},
                                    Song._ID + "=?", new String[]{Integer.toString(song.getId())},
                                    null)) {
                                if (c2 != null && c2.moveToFirst()) {
                                    song.setDateAdded(c2.getLong(
                                            c2.getColumnIndex(Song.DATE_ADDED)));
                                }
                                Log.d(TAG, "Setting date added of " + song + ":" + song.getId() +
                                        " to " + Util.formatDateTime(song.getDateAdded()));
                                ContentValues values = new ContentValues();
                                values.put(Song.DATE_ADDED, song.getDateAdded());
                                Log.d(TAG, db.update(Song.TABLE_NAME, values,
                                        Song._ID + "=" + song.getId(), null) + " rows affected");
                            }
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
    }*/
}
