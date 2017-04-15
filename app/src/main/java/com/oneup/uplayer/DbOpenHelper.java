package com.oneup.uplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;

import com.oneup.uplayer.obj.Artist;
import com.oneup.uplayer.obj.Song;

import java.util.ArrayList;

public class DbOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "UPlayer";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "UPlayer.db";

    private static final String SONGS = "songs";
    private static final String ARTISTS = "artists";

    private static final String LAST_PLAYED = "last_played";
    private static final String TIMES_PLAYED = "times_played";

    public DbOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "DbOpenHelper.onCreate()");

        db.execSQL("CREATE TABLE " + SONGS + "(" +
                BaseColumns._ID + " INTEGER PRIMARY KEY," +
                MediaStore.MediaColumns.TITLE + " TEXT," +
                MediaStore.Audio.AudioColumns.ARTIST_ID + " INTEGER," +
                MediaStore.Audio.AudioColumns.ARTIST + " TEXT," +
                MediaStore.Audio.AudioColumns.YEAR + " INTEGER," +
                LAST_PLAYED + " INTEGER," +
                TIMES_PLAYED + " INTEGER);");

        db.execSQL("CREATE TABLE " + ARTISTS + "(" +
                MediaStore.Audio.Artists._ID + " INTEGER PRIMARY KEY," +
                MediaStore.Audio.Artists.ARTIST + " TEXT," +
                LAST_PLAYED + " INTEGER," +
                TIMES_PLAYED + " INTEGER);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public ArrayList<Artist> queryLastPlayedArtists() {
        return queryArtists(LAST_PLAYED);
    }

    public ArrayList<Artist> queryMostPlayedArtists() {
        return queryArtists(TIMES_PLAYED);
    }

    public ArrayList<Song> queryLastPlayedSongs(long artistId) {
        return querySongs(artistId, LAST_PLAYED);
    }

    public ArrayList<Song> queryMostPlayedSongs(long artistId) {
        return querySongs(artistId, TIMES_PLAYED);
    }

    public void updatePlayed(Song song) {
        Log.d(TAG, "DbOpenHelper.updatePlayed(" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            long lastPlayed = System.currentTimeMillis();
            int timesPlayed;
            ContentValues values;

            timesPlayed = queryTimesPlayed(db, SONGS, song.getId());
            Log.d(TAG, "Song timesPlayed=" + timesPlayed);

            values = new ContentValues();
            values.put(LAST_PLAYED, lastPlayed);
            values.put(TIMES_PLAYED, timesPlayed + 1);
            if (timesPlayed == 0) {
                values.put(BaseColumns._ID, song.getId());
                values.put(MediaStore.MediaColumns.TITLE, song.getTitle());
                if (song.getArtistId() > 0) {
                    values.put(MediaStore.Audio.AudioColumns.ARTIST_ID, song.getArtistId());
                    values.put(MediaStore.Audio.AudioColumns.ARTIST, song.getArtist());
                }
                if (song.getYear() > 0) {
                    values.put(MediaStore.Audio.AudioColumns.YEAR, song.getYear());
                }
                db.insert(SONGS, null, values);
                Log.d(TAG, "Song inserted");
            } else {
                db.update(SONGS, values, BaseColumns._ID + "=?",
                        new String[] { Long.toString(song.getId()) });
                Log.d(TAG, "Song updated");
            }

            timesPlayed = queryTimesPlayed(db, ARTISTS, song.getArtistId());
            Log.d(TAG, "Artist timesPlayed=" + timesPlayed);

            values = new ContentValues();
            values.put(LAST_PLAYED, lastPlayed);
            values.put(TIMES_PLAYED, timesPlayed + 1);
            if (timesPlayed == 0) {
                values.put(BaseColumns._ID, song.getArtistId());
                values.put(MediaStore.Audio.Artists.ARTIST, song.getArtist());
                db.insert(ARTISTS, null, values);
                Log.d(TAG, "Artist inserted");
            } else {
                db.update(ARTISTS, values, BaseColumns._ID + "=?",
                        new String[] { Long.toString(song.getArtistId()) });
                Log.d(TAG, "Artist updated");
            }
        }
    }

    private ArrayList<Artist> queryArtists(String orderByColumn) {
        Log.d(TAG, "DbOpenHelper.queryArtists(" + orderByColumn + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(ARTISTS,
                    new String[] {
                            BaseColumns._ID,
                            MediaStore.Audio.ArtistColumns.ARTIST
                    },
                    null, null, null, null, orderByColumn + " DESC")) {
                ArrayList<Artist> ret = new ArrayList<>();
                while (c.moveToNext()) {
                    ret.add(new Artist(
                            c.getLong(0),
                            c.getString(1)));
                }
                Log.d(TAG, ret.size() + " artists queried");
                return ret;
            }
        }
    }

    private ArrayList<Song> querySongs(long artistId, String orderByColumn) {
        Log.d(TAG, "DbOpenHelper.querySongs(" + artistId + "," + orderByColumn + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(SONGS,
                    new String[] {
                            BaseColumns._ID,
                            MediaStore.MediaColumns.TITLE,
                            MediaStore.Audio.AudioColumns.ARTIST_ID,
                            MediaStore.Audio.AudioColumns.ARTIST,
                            MediaStore.Audio.AudioColumns.YEAR
                    },
                    MediaStore.Audio.AudioColumns.ARTIST_ID + "=?",
                    new String[] { Long.toString(artistId) },
                    null, null, orderByColumn + " DESC")) {
                ArrayList<Song> ret = new ArrayList<>();
                while (c.moveToNext()) {
                    ret.add(new Song(
                            c.getLong(0),
                            c.getString(1),
                            c.getLong(2),
                            c.getString(3),
                            c.getInt(4)));
                }
                Log.d(TAG, ret.size() + " songs queried");
                return ret;
            }
        }
    }

    private int queryTimesPlayed(SQLiteDatabase db, String table, long id) {
        Log.d(TAG, "DbOpenHelper.queryTimesPlayed(" + table + "," + id + ")");
        try (Cursor c = db.query(table,
                new String[] {
                        TIMES_PLAYED
                },
                BaseColumns._ID + "=?",
                new String[] { Long.toString(id) },
                null, null, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }
}
