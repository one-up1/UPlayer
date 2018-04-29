package com.oneup.uplayer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;

import com.oneup.uplayer.R;
import com.oneup.uplayer.util.Calendar;
import com.oneup.uplayer.util.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

//TODO: Backup() in DBOpenHelper.

public class DbOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "UPlayer";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "UPlayer.db";

    private static final String SQL_CREATE_ARTISTS =
            "CREATE TABLE " + Artists.TABLE_NAME + "(" +
                    Artists._ID + " INTEGER PRIMARY KEY," +
                    Artists.ARTIST + " TEXT," +
                    Artists.LAST_SONG_ADDED + " INTEGER," +
                    Artists.LAST_PLAYED + " INTEGER," +
                    Artists.TIMES_PLAYED + " INTEGER DEFAULT 0)";

    private static final String SQL_CREATE_SONGS =
            "CREATE TABLE " + Songs.TABLE_NAME + "(" +
                    Songs._ID + " INTEGER PRIMARY KEY," +
                    Songs.TITLE + " TEXT," +
                    Songs.DURATION + " INTEGER," +
                    Songs.ARTIST_ID + " INTEGER," +
                    Songs.YEAR + " INTEGER," +
                    Songs.ADDED + " INTEGER," +
                    Songs.BOOKMARKED + " INTEGER," +
                    Songs.TAG + " TEXT," +
                    Songs.LAST_PLAYED + " INTEGER," +
                    Songs.TIMES_PLAYED + " INTEGER DEFAULT 0)";

    private static final String SQL_WHERE_ID = BaseColumns._ID + "=?"; //TODO: Use this everywhere.

    private static final String SQL_UPDATE_ARTISTS =
            "UPDATE " + Artists.TABLE_NAME + " SET " +
                    Artists.LAST_SONG_ADDED + "=(SELECT MAX(" + Songs.ADDED + ") FROM " +
                    Songs.TABLE_NAME + " WHERE " +
                    Songs.ARTIST_ID + "=" + Artists.TABLE_NAME + "." + Artists._ID + ")," +

                    Artists.LAST_PLAYED + "=(SELECT MAX(" + Songs.LAST_PLAYED + ") FROM " +
                    Songs.TABLE_NAME + " WHERE " +
                    Songs.ARTIST_ID + "=" + Artists.TABLE_NAME + "." + Artists._ID + ")," +

                    Artists.TIMES_PLAYED + "=(SELECT SUM(" + Songs.TIMES_PLAYED + ") FROM " +
                    Songs.TABLE_NAME + " WHERE " +
                    Songs.ARTIST_ID + "=" + Artists.TABLE_NAME + "." + Artists._ID + ")";

    private static final File ARTIST_IGNORE_FILE = Util.getMusicFile("ignore.txt");

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
                values.put(Artist.TIMES_PLAYED, artist.getTimesPlayed());
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
            try (Cursor c = db.query(Artists.TABLE_NAME,
                    new String[]{Artists.LAST_SONG_ADDED,
                            Artists.LAST_PLAYED, Artists.TIMES_PLAYED},
                    SQL_WHERE_ID, new String[]{Long.toString(song.getArtist().getId())},
                    null, null, null)) {
                if (c.moveToFirst()) {
                    song.getArtist().setDateModified(c.getLong(0));
                    song.getArtist().setLastPlayed(c.getLong(1));
                    song.getArtist().setTimesPlayed(c.getInt(2));
                } else {
                    Log.d(TAG, "Artist not found");
                }
            }

            try (Cursor c = db.query(Songs.TABLE_NAME,
                    new String[]{Songs.YEAR, Songs.ADDED, Songs.BOOKMARKED, Songs.TAG,
                            Songs.LAST_PLAYED, Songs.TIMES_PLAYED},
                    Song._ID + "=" + song.getId(), null, null, null, null)) {
                if (c.moveToFirst()) {
                    song.setYear(c.getInt(0));
                    song.setDateAdded(c.getLong(1));
                    song.setBookmarked(c.getLong(2));
                    song.setTag(c.getString(3));
                    song.setLastPlayed(c.getLong(4));
                    song.setTimesPlayed(c.getInt(5));
                } else {
                    Log.d(TAG, "Song not found");
                }
            }

            Log.d(TAG, "Times played: " + song.getArtist().getTimesPlayed() +
                    ":" + song.getTimesPlayed());
        }
    }

    public ArrayList<String> querySongTags() {
        Log.d(TAG, "DbOpenHelper.querySongTags()");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(true, Song.TABLE_NAME, new String[]{Song.TAG},
                    Song.TAG + " IS NOT NULL", null, null, null, Song.TAG, null)) {
                ArrayList<String> ret = new ArrayList<>();
                while (c.moveToNext()) {
                    ret.add(c.getString(0));
                }
                Log.d(TAG, "Queried " + ret.size() + " song tags");
                return ret;
            }
        }
    }

    public void insertOrUpdateSong(Song song) {
        Log.d(TAG, "DbOpenHelper.insertOrUpdateSong(" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                values.put(Songs._ID, song.getId());
                values.put(Songs.TITLE, song.getTitle());
                if (song.getDuration() == 0) {
                    values.putNull(Songs.DURATION);
                } else {
                    values.put(Songs.DURATION, song.getDuration());
                }
                values.put(Songs.ARTIST_ID, song.getArtist().getId());
                if (song.getYear() == 0) {
                    values.putNull(Songs.YEAR);
                } else {
                    values.put(Songs.YEAR, song.getYear());
                }
                if (song.getDateAdded() == 0) {
                    values.putNull(Songs.ADDED);
                } else {
                    values.put(Songs.ADDED, song.getDateAdded());
                }
                if (song.getBookmarked() == 0) {
                    values.putNull(Songs.BOOKMARKED);
                } else {
                    values.put(Songs.BOOKMARKED, song.getBookmarked());
                }
                if (song.getTag() == null) {
                    values.putNull(Songs.TAG);
                } else {
                    values.put(Songs.TAG, song.getTag());
                }
                if (song.getLastPlayed() == 0) {
                    values.putNull(Songs.LAST_PLAYED);
                } else {
                    values.put(Songs.LAST_PLAYED, song.getLastPlayed());
                }
                values.put(Songs.TIMES_PLAYED, song.getTimesPlayed());

                int rowsAffected = db.update(Songs.TABLE_NAME, values,
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

        /*if (song.getDateAdded() > song.getArtist().getDateModified()) {
            Log.d(TAG, "Updating date modified of artist " + song.getArtist());
            song.getArtist().setDateModified(song.getDateAdded());
            insertOrUpdateArtist(song.getArtist());
        }*/
    }

    public void updateSongPlayed(Song song) {
        long lastPlayed = Calendar.currentTime();
        /*querySong(song);
        long lastPlayed = Calendar.currentTime();

        song.getArtist().setLastPlayed(lastPlayed);
        song.getArtist().setTimesPlayed(song.getArtist().getTimesPlayed() + 1);
        insertOrUpdateArtist(song.getArtist());

        song.setLastPlayed(lastPlayed);
        song.setTimesPlayed(song.getTimesPlayed() + 1);
        insertOrUpdateSong(song);*/

        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                db.execSQL("UPDATE " + Artists.TABLE_NAME + " SET " +
                                Artists.LAST_PLAYED + "=?, " +
                                Artists.TIMES_PLAYED + "=" + Artists.TIMES_PLAYED + "+1 " +
                                " WHERE " + Artists._ID + "=?",
                        new String[]{Long.toString(lastPlayed), Long.toString(song.getArtist().getId())});

                db.execSQL("UPDATE " + Songs.TABLE_NAME + " SET " +
                                Songs.LAST_PLAYED + "=?, " +
                                Songs.TIMES_PLAYED + "=" + Songs.TIMES_PLAYED + "+1 " +
                                " WHERE " + Artists._ID + "=?",
                        new String[]{Long.toString(lastPlayed), Long.toString(song.getId())});

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void deleteSong(Song song) {
        Log.d(TAG, "DbOpenHelper.deleteSong(" + song + ")");
        deleteSong(song.getId(), song.getArtist().getId());
    }

    public void deleteSong(int id, int artistId) {
        Log.d(TAG, "DbOpenHelper.deleteSong(" + id + ", " + artistId + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                Log.d(TAG, db.delete(Song.TABLE_NAME, Song._ID + "=" + id, null) +
                        " songs deleted");

                db.execSQL("UPDATE " + Artist.TABLE_NAME + " SET " +
                        Artist.LAST_PLAYED + "=(SELECT MAX(" + Song.LAST_PLAYED + ") FROM " +
                        Song.TABLE_NAME + " WHERE " + Song.ARTIST_ID + "=" + artistId + ")," +
                        Artist.TIMES_PLAYED + "=(SELECT SUM(" + Song.TIMES_PLAYED + ") FROM " +
                        Song.TABLE_NAME + " WHERE " + Song.ARTIST_ID + "=" + artistId + ")," +
                        Artist.DATE_MODIFIED + "=(SELECT MAX(" + Song.DATE_ADDED + ") FROM " +
                        Song.TABLE_NAME + " WHERE " + Song.ARTIST_ID + "=" + artistId + ") " +
                        "WHERE " + Artist._ID + "=" + artistId);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void syncWithMediaStore(Context context) throws IOException {
        // Read artist ignore file.
        List<String> artistIgnore;
        if (ARTIST_IGNORE_FILE.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(ARTIST_IGNORE_FILE)))) {
                artistIgnore = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    artistIgnore.add(line);
                }
            }
            Log.d(TAG, artistIgnore.size() + " artists on ignore list");
        } else {
            Log.d(TAG, "No artist ignore file");
            artistIgnore = null;
        }

        SyncResult resArtists, resSongs;
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                long now = Calendar.currentTime(); //TODO: Millis for time or DEFAULT CURRENT_TIMESTAMP?

                resArtists = syncTable(context, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                        db, Artists.TABLE_NAME, new String[]{Artists._ID, Artists.ARTIST},
                        1, artistIgnore, -1, null, null, new int[0],
                        Artists.LAST_SONG_ADDED, now);

                resSongs = syncTable(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        db, Songs.TABLE_NAME, new String[]{Songs._ID, Songs.TITLE, Songs.DURATION,
                                Songs.ARTIST_ID, Songs.YEAR},
                        -1, null, 3, resArtists.ids, new int[]{1, 2, 3}, new int[]{4},
                        Songs.ADDED, now);

                // Update artists when songs have been inserted or deleted.
                if (resSongs.recordsInserted > 0 || resSongs.recordsDeleted > 0) {
                    db.execSQL(SQL_UPDATE_ARTISTS);
                    Log.d(TAG, "Artists updated");
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        Util.showInfoDialog(context, R.string.sync_completed, R.string.sync_completed_message,
                resArtists.ids.size(), resArtists.recordsIgnored,
                resArtists.recordsInserted, resArtists.recordsUpdated, resArtists.recordsDeleted,
                resSongs.ids.size(), resSongs.recordsIgnored,
                resSongs.recordsInserted, resSongs.recordsUpdated, resSongs.recordsDeleted);
    }
    //TODO: getDatabase() only in DbOpenHelper.

    private SyncResult syncTable(Context context, Uri contentUri, SQLiteDatabase db, String table,
                                 String[] columns, int ignoreColumn, List<String> ignoreValues,
                                 int refIdColumn, List<Long> refIds,
                                 int[] updateColumns, int[] insertColumns,
                                 String addedColumn, long added) {
        Log.d(TAG, "Synchronizing table " + table);
        SyncResult ret = new SyncResult();

        // Insert/update database records from the MediaStore.
        try (Cursor c = context.getContentResolver().query(contentUri, columns, null, null, null)) {
            if (c == null) {
                throw new RuntimeException("No MediaStore cursor");
            }

            while (c.moveToNext()) {
                long id = c.getLong(0);

                // Process ignoring.
                if (ignoreColumn != -1) {
                    String ignoreColumnValue = c.getString(ignoreColumn);
                    if (ignoreValues.contains(ignoreColumnValue)) {
                        Log.d(TAG, table + "." + columns[ignoreColumn] + " value '" +
                                ignoreColumnValue + "' ignored: " + id);
                        ret.recordsIgnored++;
                        continue;
                    }
                }

                // Ignore records with a non-existing ref ID.
                if (refIdColumn != -1) {
                    long refId = c.getLong(refIdColumn);
                    if (!refIds.contains(refId)) {
                        Log.d(TAG, table + "." + columns[refIdColumn] + " value " +
                                refId + " ignored: " + id);
                        ret.recordsIgnored++;
                        continue;
                    }
                }

                // Put update column values.
                ContentValues values = new ContentValues();
                putValues(values, c, columns, updateColumns, table, id);

                // Update or insert the record if it doesn't exist.
                if (db.update(table, values, SQL_WHERE_ID, new String[]{Long.toString(id)}) == 0) {
                    values.put(BaseColumns._ID, id);
                    putValues(values, c, columns, insertColumns, table, id);
                    if (addedColumn != null) {
                        values.put(addedColumn, added);
                    }
                    db.insert(table, null, values);
                    Log.d(TAG, table + " record inserted: " + id);
                    ret.recordsInserted++;
                } else {
                    Log.d(TAG, table + " record updated: " + id);
                    ret.recordsUpdated++;
                }
                ret.ids.add(id);
            }
        }

        // Delete records from the database.
        try (Cursor c = db.query(table, new String[]{BaseColumns._ID},
                null, null, null, null, null)) {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                if (!ret.ids.contains(id)) {
                    db.delete(table, SQL_WHERE_ID, new String[]{Long.toString(id)});
                    Log.d(TAG, table + " record deleted: " + id);
                    ret.recordsDeleted++;
                }
            }
        }

        return ret;
    }

    private void putValues(ContentValues values, Cursor c, String[] columns, int[] putColumns,
                           String table, long id) {
        for (int i = 0; i < (putColumns == null ? columns.length : putColumns.length); i++) {
            int column = putColumns == null ? i : putColumns[i];
            switch (c.getType(column)) {
                case Cursor.FIELD_TYPE_NULL:
                    values.putNull(columns[column]);
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    values.put(columns[column], c.getLong(column));
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    values.put(columns[column], c.getString(column));
                    break;
                default:
                    throw new RuntimeException(table + "." + columns[column] + " field type " +
                            c.getType(i) + " invalid: " + id);
            }
        }
    }

    public static int queryInt(SQLiteDatabase db, String sql, String[] selectionArgs) {
        try (Cursor c = db.rawQuery(sql, selectionArgs)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public static long queryLong(SQLiteDatabase db, String sql, String[] selectionArgs) {
        try (Cursor c = db.rawQuery(sql, selectionArgs)) {
            return c.moveToFirst() ? c.getLong(0) : 0;
        }
    }

    private static class Artists implements BaseColumns, ArtistColumns, DbColumns {
        private static final String TABLE_NAME = "artists";
    }

    private static class Songs implements BaseColumns, SongColumns, DbColumns {
        private static final String TABLE_NAME = "songs";
    }

    private interface ArtistColumns extends MediaStore.Audio.ArtistColumns {
        String LAST_SONG_ADDED = "last_song_added";
    }

    private interface SongColumns extends MediaStore.Audio.AudioColumns {
        String ADDED = "added";
        String BOOKMARKED = "bookmarked";
        String TAG = "tag";
    }

    private interface DbColumns {
        String LAST_PLAYED = "last_played";
        String TIMES_PLAYED = "times_played";
    }

    private static class SyncResult {
        private List<Long> ids;
        private int recordsIgnored;

        private int recordsInserted;
        private int recordsUpdated;
        private int recordsDeleted;

        private SyncResult() {
            ids = new ArrayList<>();
        }
    }
}
