package com.oneup.uplayer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;

import com.oneup.uplayer.R;
import com.oneup.uplayer.util.Calendar;
import com.oneup.uplayer.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

//TODO: Check rows affected and moveToFirst() result.
//TODO: getDatabase() only in DbOpenHelper?
//TODO: List or ArrayList?
//TODO: Constants for IS NOT NULL/DESC etc?

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
                    Songs.ARTIST_ID + " INTEGER," +
                    Songs.ARTIST + " INTEGER," +
                    Songs.DURATION + " INTEGER," +
                    Songs.YEAR + " INTEGER," +
                    Songs.ADDED + " INTEGER," +
                    Songs.TAG + " TEXT," +
                    Songs.BOOKMARKED + " INTEGER," +
                    Songs.LAST_PLAYED + " INTEGER," +
                    Songs.TIMES_PLAYED + " INTEGER DEFAULT 0)";

    //TODO: Use this everywhere.
    private static final String SQL_ID_ARG = BaseColumns._ID + "=?";
    private static final String SQL_WHERE_ID = "WHERE " + SQL_ID_ARG;

    private static final String SQL_BOOKMARK_SONG =
            "UPDATE " + Songs.TABLE_NAME + " SET " +
                    Songs.BOOKMARKED + "=CASE WHEN " +
                    Songs.BOOKMARKED + " IS NULL THEN ? ELSE NULL END " +
                    SQL_WHERE_ID;

    private static final String SQL_UPDATE_ARTIST_STATS =
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
    private static final File BACKUP_FILE = Util.getMusicFile("UPlayer.json");

    public DbOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "DbOpenHelper.onCreate()");
        db.execSQL(SQL_CREATE_ARTISTS);
        db.execSQL(SQL_CREATE_SONGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void queryArtists(List<Artist> artists,
                             String selection, String[] selectionArgs, String orderBy) {
        Log.d(TAG, "DbOpenHelper.queryArtists(" + selection + "," + orderBy + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(Artists.TABLE_NAME, new String[]{Artists._ID,
                            Artists.ARTIST, Artists.TIMES_PLAYED},
                    selection, selectionArgs, null, null, orderBy)) {
                artists.clear();
                while (c.moveToNext()) {
                    Artist artist = new Artist();
                    artist.setId(c.getLong(0));
                    artist.setArtist(c.getString(1));
                    artist.setTimesPlayed(c.getInt(2));
                    artists.add(artist);
                }
                Log.d(TAG, artists.size() + " artists queried");
            }
        }
    }

    public void queryArtist(Artist artist) {
        Log.d(TAG, "DbOpenHelper.queryArtist(" + artist + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = query(db, Artists.TABLE_NAME, new String[]{Artists.LAST_SONG_ADDED,
                            Artists.LAST_PLAYED, Artists.TIMES_PLAYED},
                    artist.getId())) {
                artist.setLastSongAdded(c.getLong(0));
                artist.setLastPlayed(c.getLong(1));
                artist.setTimesPlayed(c.getInt(2));
            }
        }
    }

    public void querySongs(List<Song> songs,
                           String selection, String[] selectionArgs, String orderBy) {
        Log.d(TAG, "DbOpenHelper.querySongs(" + selection + "," + orderBy + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(Songs.TABLE_NAME, new String[]{Songs._ID, Songs.TITLE,
                            Songs.ARTIST_ID, Songs.ARTIST, Songs.DURATION, Artists.TIMES_PLAYED},
                    selection, selectionArgs, null, null, orderBy)) {
                songs.clear();
                while (c.moveToNext()) {
                    Song song = new Song();
                    song.setId(c.getLong(0));
                    song.setTitle(c.getString(1));
                    song.setArtistId(c.getLong(2));
                    song.setArtist(c.getString(3));
                    song.setDuration(c.getLong(4));
                    song.setTimesPlayed(c.getInt(5));
                    songs.add(song);
                }
                Log.d(TAG, songs.size() + " songs queried");
            }
        }
    }

    public void querySong(Song song) {
        Log.d(TAG, "DbOpenHelper.querySong(" + song + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = query(db, Songs.TABLE_NAME, new String[]{Songs.YEAR, Songs.ADDED,
                            Songs.TAG, Songs.BOOKMARKED, Songs.LAST_PLAYED, Songs.TIMES_PLAYED},
                    song.getId())) {
                song.setYear(c.getInt(0));
                song.setAdded(c.getLong(1));
                song.setTag(c.getString(2));
                song.setBookmarked(c.getLong(3));
                song.setLastPlayed(c.getLong(4));
                song.setTimesPlayed(c.getInt(5));
            }
        }
    }

    public ArrayList<String> querySongTags() {
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(true, Songs.TABLE_NAME, new String[]{Songs.TAG},
                    Songs.TAG + " IS NOT NULL", null, null, null, Songs.TAG, null)) {
                ArrayList<String> ret = new ArrayList<>();
                while (c.moveToNext()) {
                    ret.add(c.getString(0));
                }
                Log.d(TAG, ret.size() + " song tags queried");
                return ret;
            }
        }
    }

    public void updateSong(Song song) {
        Log.d(TAG, "DbOpenHelper.updateSong(" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                if (song.getYear() == 0) {
                    values.putNull(Songs.YEAR);
                } else {
                    values.put(Songs.YEAR, song.getYear());
                }
                if (song.getAdded() == 0) {
                    values.putNull(Songs.ADDED);
                } else {
                    values.put(Songs.ADDED, song.getAdded());
                }
                values.put(Songs.TAG, song.getTag());

                update(db, Songs.TABLE_NAME, values, song.getId());
                updateArtistStats(db, song);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void bookmarkSong(Song song) {
        Log.d(TAG, "DbOpenHelper.bookmarkSong(" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.execSQL(SQL_BOOKMARK_SONG, new Object[]{Calendar.currentTime(), song.getId()});
        }
    }

    public void updateSongPlayed(Song song) {
        Log.d(TAG, "DbOpenHelper.updateSongPlayed(" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                long time = Calendar.currentTime();
                updatePlayed(db, Songs.TABLE_NAME, time, song.getId());
                updatePlayed(db, Artists.TABLE_NAME, time, song.getArtistId());

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void deleteSong(Song song) {
        Log.d(TAG, "DbOpenHelper.deleteSong(" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                delete(db, Songs.TABLE_NAME, song.getId());
                updateArtistStats(db, song);
                //TODO: Delete artist when deleting last song from it?

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
                Log.d(TAG, artistIgnore.size() + " artists on ignore list");
            }
        } else {
            Log.d(TAG, "No artist ignore file");
            artistIgnore = null;
        }

        SyncResult resArtists, resSongs;
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                long time = Calendar.currentTime();

                resArtists = syncTable(context, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                        db, Artists.TABLE_NAME, new String[]{Artists._ID, Artists.ARTIST},
                        null, new int[0], 1, artistIgnore, -1, null, null, 0);

                resSongs = syncTable(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        db, Songs.TABLE_NAME, new String[]{Songs._ID, Songs.TITLE,
                                Songs.ARTIST_ID, Songs.ARTIST, Songs.DURATION, Songs.YEAR},
                        new int[]{1, 2, 3, 4}, new int[]{5}, -1, null, 2, resArtists.ids,
                        Songs.ADDED, time);

                // Update artists when songs have been inserted or deleted.
                if (resSongs.rowsInserted > 0 || resSongs.rowsDeleted > 0) {
                    db.execSQL(SQL_UPDATE_ARTIST_STATS);
                    Log.d(TAG, "Artist stats updated");
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        Util.showInfoDialog(context, R.string.sync_completed, R.string.sync_completed_message,
                resArtists.ids.size(), resArtists.rowsIgnored,
                resArtists.rowsInserted, resArtists.rowsUpdated, resArtists.rowsDeleted,
                resSongs.ids.size(), resSongs.rowsIgnored,
                resSongs.rowsInserted, resSongs.rowsUpdated, resSongs.rowsDeleted);
    }

    public void backup() throws JSONException, IOException {
        JSONObject backup = new JSONObject();

        // Put database tables in JSONObject.
        try (SQLiteDatabase db = getReadableDatabase()) {
            backupTable(backup, db, Artists.TABLE_NAME, new String[]{Artists.ARTIST,
                    Artists.LAST_SONG_ADDED, Artists.LAST_PLAYED, Artists.TIMES_PLAYED});

            backupTable(backup, db, Songs.TABLE_NAME, new String[]{Songs.TITLE, Songs.ARTIST,
                    Songs.YEAR, Songs.ADDED, Songs.TAG, Songs.BOOKMARKED,
                    Songs.LAST_PLAYED, Songs.TIMES_PLAYED});
        }

        // Write JSONObject to file.
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(BACKUP_FILE, false)))) {
            writer.write(backup.toString());
        }
    }

    public void restoreBackup() throws IOException, JSONException {
        // Read JSONObject from file.
        JSONObject backup;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(BACKUP_FILE)))) {
            backup = new JSONObject(reader.readLine());
        }
        updateBackup(backup);

        // Update database tables from JSONObject.
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                restoreTableBackup(backup, db, Artists.TABLE_NAME, new String[]{
                        Artists.LAST_SONG_ADDED, Artists.LAST_PLAYED, Artists.TIMES_PLAYED},
                        Artists.ARTIST + " LIKE ?", new String[]{Artists.ARTIST});

                restoreTableBackup(backup, db, Songs.TABLE_NAME, new String[]{
                        Songs.YEAR, Songs.ADDED, Songs.TAG, Songs.BOOKMARKED,
                                Songs.LAST_PLAYED, Songs.TIMES_PLAYED},
                        Songs.TITLE + " LIKE ? AND " + Songs.ARTIST + " LIKE ?",
                        new String[]{Songs.TITLE, Songs.ARTIST});

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    private static void updateBackup(JSONObject backup) throws JSONException {
        JSONArray artists = backup.getJSONArray(Artists.TABLE_NAME);
        LongSparseArray<String> artistNames = new LongSparseArray<>();
        for (int i = 0; i < artists.length(); i++) {
            JSONObject artist = artists.getJSONObject(i);
            artistNames.put(artist.getLong(Artists._ID), artist.getString(Artists.ARTIST));

            if (artist.has("date_modified")) {
                artist.put(Artists.LAST_SONG_ADDED, artist.getLong("date_modified"));
                artist.remove("date_modified");
            }
        }

        JSONArray songs = backup.getJSONArray(Songs.TABLE_NAME);
        for (int i = 0; i < songs.length(); i++) {
            JSONObject song = songs.getJSONObject(i);

            String artist = artistNames.get(song.getLong(Songs.ARTIST_ID));
            if (artist == null) {
                throw new JSONException("Artist not found");
            }
            song.put(Songs.ARTIST, artist);
            song.remove(Songs.ARTIST_ID);

            if (song.has(Songs.DATE_ADDED)) {
                song.put(Songs.ADDED, song.getLong(Songs.DATE_ADDED));
                song.remove(Songs.DATE_ADDED);
            }
        }
    }

    private static SyncResult syncTable(Context context, Uri contentUri,
                                        SQLiteDatabase db, String table, String[] columns,
                                        int[] updateColumns, int[] insertColumns,
                                        int ignoreColumn, List<String> ignoreValues,
                                        int refIdColumn, List<Long> refIds,
                                        String timeColumn, long time) {
        SyncResult ret = new SyncResult();

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
                        ret.rowsIgnored++;
                        continue;
                    }
                }

                // Ignore rows with a non-existing ref ID.
                if (refIdColumn != -1) {
                    long refId = c.getLong(refIdColumn);
                    if (!refIds.contains(refId)) {
                        Log.d(TAG, table + "." + columns[refIdColumn] + " value " +
                                refId + " ignored: " + id);
                        ret.rowsIgnored++;
                        continue;
                    }
                }

                // Put update column values.
                ContentValues values = new ContentValues();
                putValuesFromCursor(c, values, columns, updateColumns);

                // Update or insert row if it doesn't exist.
                if (update(db, table, values, id) == 0) {
                    values.put(BaseColumns._ID, id);
                    putValuesFromCursor(c, values, columns, insertColumns);
                    if (timeColumn != null) {
                        values.put(timeColumn, time);
                    }
                    db.insert(table, null, values);
                    Log.d(TAG, table + " row inserted: " + id);
                    ret.rowsInserted++;
                } else {
                    Log.d(TAG, table + " row updated: " + id);
                    ret.rowsUpdated++;
                }
                ret.ids.add(id);
            }
        }

        // Delete rows from database.
        try (Cursor c = db.query(table, new String[]{BaseColumns._ID},
                null, null, null, null, null)) {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                if (!ret.ids.contains(id)) {
                    delete(db, table, id);
                    Log.d(TAG, table + " row deleted: " + id);
                    ret.rowsDeleted++;
                }
            }
        }

        return ret;
    }

    private static void putValuesFromCursor(Cursor c, ContentValues values,
                                            String[] columns, int[] putColumns) {
        // Put all columns when putColumns is null or only the specified column indices.
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
                    throw new RuntimeException("Invalid type");
            }
        }
    }

    private static void backupTable(JSONObject backup, SQLiteDatabase db,
                                    String table, String[] columns)
            throws JSONException {
        try (Cursor c = db.query(table, columns, null, null, null, null, null)) {
            JSONArray rows = new JSONArray();
            while (c.moveToNext()) {
                JSONObject row = new JSONObject();
                for (int i = 0; i < columns.length; i++) {
                    switch (c.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL:
                            // Null values are not stored in JSON.
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            row.put(columns[i], c.getLong(i));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            row.put(columns[i], c.getString(i));
                            break;
                        default:
                            throw new RuntimeException("Invalid type");
                    }
                }
                rows.put(row);
            }
            backup.put(table, rows);
            Log.d(TAG, rows.length() + " rows backed up from table " + table);
        }
    }

    private static void restoreTableBackup(JSONObject backup, SQLiteDatabase db,
                                           String table, String[] columns,
                                           String whereClause, String[] whereArgColumns)
            throws JSONException {
        JSONArray rows = backup.getJSONArray(table);
        for (int index = 0; index < rows.length(); index++) {
            // Put values to update from JSONObject.
            JSONObject row = rows.getJSONObject(index);
            ContentValues values = new ContentValues();
            for (String column : columns) {
                if (row.has(column)) {
                    Object value = row.get(column);
                    if (value instanceof Integer) {
                        values.put(column, (Integer) value);
                    } else if (value instanceof Long) {
                        values.put(column, (Long) value);
                    } else if (value instanceof String) {
                        values.put(column, (String) value);
                    } else {
                        throw new RuntimeException("Invalid type");
                    }
                } else {
                    values.putNull(column);
                }
            }

            // Get whereArgs from JSONObject based on whereArgColumns.
            String[] whereArgs = new String[whereArgColumns.length];
            for (int i = 0; i < whereArgs.length; i++) {
                if (row.has(whereArgColumns[i])) {
                    whereArgs[i] = row.get(whereArgColumns[i]).toString();
                } else {
                    throw new RuntimeException("NULL value for where argument column " +
                            table + "." + whereArgColumns[i]);
                }
            }

            // Update row and make sure 1 row is updated.
            switch (db.update(table, values, whereClause, whereArgs)) {
                case 0:
                    throw new RuntimeException(table + " row not found: '" +
                            TextUtils.join(",", whereArgs) + "'");
                case 1:
                    Log.d(TAG, table + " row updated: '" +
                            TextUtils.join(",", whereArgs) + "'");
                    break;
                default:
                    throw new RuntimeException(table + " row has duplicate value: '" +
                            TextUtils.join(",", whereArgs) + "'");
            }
        }
        Log.d(TAG, rows.length() + " rows restored to table " + table);
    }

    private static Cursor query(SQLiteDatabase db, String table, String[] columns, long id) {
        Cursor c = db.query(table, columns, SQL_ID_ARG, getWhereArgs(id), null, null, null);
        c.moveToFirst();
        return c;
    }

    private static int update(SQLiteDatabase db, String table, ContentValues values, long id) {
        return db.update(table, values, SQL_ID_ARG, getWhereArgs(id));
    }

    private static int delete(SQLiteDatabase db, String table, long id) {
        return db.delete(table, SQL_ID_ARG, getWhereArgs(id));
    }

    private static String[] getWhereArgs(long id) {
        return new String[]{Long.toString(id)};
    }

    private static void updatePlayed(SQLiteDatabase db, String table, long time, long id) {
        db.execSQL("UPDATE " + table + " SET " +
                PlayedColumns.LAST_PLAYED + "=?," +
                PlayedColumns.TIMES_PLAYED + "=" + PlayedColumns.TIMES_PLAYED + "+1 " +
                SQL_WHERE_ID,
                new Object[]{time, id});
    }

    private static void updateArtistStats(SQLiteDatabase db, Song song) {
        db.execSQL(SQL_UPDATE_ARTIST_STATS + SQL_WHERE_ID, new Object[]{song.getArtistId()});
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

    public static class Artists implements BaseColumns, ArtistColumns, PlayedColumns {
        private static final String TABLE_NAME = "artists";
    }

    public static class Songs implements SongColumns, PlayedColumns {
        private static final String TABLE_NAME = "songs";
    }

    private interface ArtistColumns extends MediaStore.Audio.ArtistColumns {
        String LAST_SONG_ADDED = "last_song_added";
    }

    private interface SongColumns extends MediaStore.Audio.AudioColumns {
        String ADDED = "added";
        String TAG = "tag";
        String BOOKMARKED = "bookmarked";
    }

    private interface PlayedColumns {
        String LAST_PLAYED = "last_played";
        String TIMES_PLAYED = "times_played";
    }

    private static class SyncResult {
        private List<Long> ids;
        private int rowsIgnored;

        private int rowsInserted;
        private int rowsUpdated;
        private int rowsDeleted;

        private SyncResult() {
            ids = new ArrayList<>();
        }
    }
}
