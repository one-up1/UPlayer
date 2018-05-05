package com.oneup.uplayer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

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

//TODO: Return ArrayList from queryArtists() but fill it in querySongs() because only songs are reloaded.
//TODO: Check rows affected and moveToFirst() result.

public class DbHelper extends SQLiteOpenHelper {
    private static final String TAG = "UPlayer";

    private static final String DATABASE_NAME = "UPlayer.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_ARTISTS = "artists";
    private static final String TABLE_SONGS = "songs";

    private static final String SQL_CREATE_ARTISTS =
            "CREATE TABLE " + TABLE_ARTISTS + "(" +
                    Artist._ID + " INTEGER PRIMARY KEY," +
                    Artist.ARTIST + " TEXT," +
                    Artist.LAST_SONG_ADDED + " INTEGER," +
                    Artist.LAST_PLAYED + " INTEGER," +
                    Artist.TIMES_PLAYED + " INTEGER DEFAULT 0)";

    private static final String SQL_CREATE_SONGS =
            "CREATE TABLE " + TABLE_SONGS + "(" +
                    Song._ID + " INTEGER PRIMARY KEY," +
                    Song.TITLE + " TEXT," +
                    Song.ARTIST_ID + " INTEGER," +
                    Song.ARTIST + " INTEGER," +
                    Song.DURATION + " INTEGER," +
                    Song.YEAR + " INTEGER," +
                    Song.ADDED + " INTEGER," +
                    Song.TAG + " TEXT," +
                    Song.BOOKMARKED + " INTEGER," +
                    Song.LAST_PLAYED + " INTEGER," +
                    Song.TIMES_PLAYED + " INTEGER DEFAULT 0)";

    private static final String SQL_ID_IS = BaseColumns._ID + "=?";

    private static final String SQL_UPDATE_ARTISTS_STATS =
            "UPDATE " + TABLE_ARTISTS + " SET " +
                    Artist.LAST_SONG_ADDED +
                    "=(SELECT MAX(" + Song.ADDED + ") FROM " + TABLE_SONGS +
                    " WHERE " + Song.ARTIST_ID + "=" + TABLE_ARTISTS + "." + Artist._ID + ")," +

                    Artist.LAST_PLAYED +
                    "=(SELECT MAX(" + Song.LAST_PLAYED + ") FROM " + TABLE_SONGS +
                    " WHERE " + Song.ARTIST_ID + "=" + TABLE_ARTISTS + "." + Artist._ID + ")," +

                    Artist.TIMES_PLAYED +
                    "=(SELECT SUM(" + Song.TIMES_PLAYED + ") FROM " + TABLE_SONGS +
                    " WHERE " + Song.ARTIST_ID + "=" + TABLE_ARTISTS + "." + Artist._ID + ")";

    private static final String SQL_UPDATE_ARTIST_STATS =
            SQL_UPDATE_ARTISTS_STATS + " WHERE " + Artist._ID + "=?";

    private static final String SQL_BOOKMARK_SONG =
            "UPDATE " + TABLE_SONGS + " SET " +
                    Song.BOOKMARKED + "=CASE WHEN " +
                    Song.BOOKMARKED + " IS NULL THEN ? ELSE NULL END " +
                    "WHERE " + Song._ID + "=?";

    private static final File ARTIST_IGNORE_FILE = Util.getMusicFile("ignore.txt");
    private static final File BACKUP_FILE = Util.getMusicFile("UPlayer.json");

    private static final String SQL_QUERY_SONG_COUNT =
            "SELECT COUNT(*) FROM " + TABLE_SONGS;

    private static final String SQL_QUERY_ARTIST_COUNT =
            "SELECT COUNT(*) FROM " + TABLE_ARTISTS;

    private static final String SQL_QUERY_SONGS_DURATION =
            "SELECT SUM(" + Song.DURATION + ") FROM " + TABLE_SONGS;

    private static final String SQL_QUERY_SONGS_PLAYED =
            "SELECT COUNT(*) FROM " + TABLE_SONGS + " WHERE " + Song.TIMES_PLAYED + ">0";

    private static final String SQL_QUERY_SONGS_UNPLAYED =
            "SELECT COUNT(*) FROM " + TABLE_SONGS + " WHERE " + Song.TIMES_PLAYED + "=0";

    private static final String SQL_QUERY_SONGS_TAGGED =
            "SELECT COUNT(*) FROM " + TABLE_SONGS + " WHERE " + Song.TAG + " IS NOT NULL";

    private static final String SQL_QUERY_SONGS_UNTAGGED =
            "SELECT COUNT(*) FROM " + TABLE_SONGS + " WHERE " + Song.TAG + " IS NULL";

    private static final String SQL_QUERY_TIMES_PLAYED =
            "SELECT SUM(" + Song.TIMES_PLAYED + ") FROM " + TABLE_SONGS;

    private static final String SQL_QUERY_PLAYED_DURATION =
            "SELECT SUM(" + Song.DURATION + "*" + Song.TIMES_PLAYED + ") FROM " + TABLE_SONGS;

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "DbHelper.onCreate()");
        db.execSQL(SQL_CREATE_ARTISTS);
        db.execSQL(SQL_CREATE_SONGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void queryArtists(List<Artist> artists, String orderBy) {
        Log.d(TAG, "DbHelper.queryArtists(" + orderBy + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_ARTISTS, new String[]{Artist._ID, Artist.ARTIST,
                            Artist.TIMES_PLAYED},
                    null, null, null, null, orderBy)) {
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
        Log.d(TAG, "DbHelper.queryArtist(" + artist + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = query(db, TABLE_ARTISTS, new String[]{Artist.LAST_SONG_ADDED,
                            Artist.LAST_PLAYED, Artist.TIMES_PLAYED},
                    artist.getId())) {
                artist.setLastSongAdded(c.getLong(0));
                artist.setLastPlayed(c.getLong(1));
                artist.setTimesPlayed(c.getInt(2));
            }
        }
    }

    public void querySongs(List<Song> songs,
                           String selection, String[] selectionArgs, String orderBy) {
        Log.d(TAG, "DbHelper.querySongs(" + selection + "," + orderBy + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_SONGS, new String[]{Song._ID, Song.TITLE,
                            Song.ARTIST_ID, Song.ARTIST, Song.DURATION, Artist.TIMES_PLAYED},
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
        Log.d(TAG, "DbHelper.querySong(" + song + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = query(db, TABLE_SONGS, new String[]{Song.YEAR, Song.ADDED, Song.TAG,
                            Song.BOOKMARKED, Song.LAST_PLAYED, Song.TIMES_PLAYED},
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

    public List<String> querySongTags() {
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(true, TABLE_SONGS, new String[]{Song.TAG},
                    Song.TAG + " IS NOT NULL", null, null, null, Song.TAG, null)) {
                List<String> ret = new ArrayList<>();
                while (c.moveToNext()) {
                    ret.add(c.getString(0));
                }
                Log.d(TAG, ret.size() + " song tags queried");
                return ret;
            }
        }
    }

    public void updateSong(Song song) {
        Log.d(TAG, "DbHelper.updateSong(" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                putValue(values, Song.YEAR, song.getYear());
                putValue(values, Song.ADDED, song.getAdded());
                values.put(Song.TAG, song.getTag());
                putValue(values, Song.BOOKMARKED, song.getBookmarked());

                update(db, TABLE_SONGS, values, song.getId());
                updateArtistStats(db, song);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void bookmarkSong(Song song) {
        Log.d(TAG, "DbHelper.bookmarkSong(" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.execSQL(SQL_BOOKMARK_SONG, new Object[]{Calendar.currentTime(), song.getId()});

            try (Cursor c = query(db, TABLE_SONGS, new String[]{Song.BOOKMARKED}, song.getId())) {
                song.setBookmarked(c.getLong(0));
            }
        }
    }

    public void updateSongPlayed(Song song) {
        Log.d(TAG, "DbHelper.updateSongPlayed(" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            long time = Calendar.currentTime();

            db.beginTransaction();
            try {
                updatePlayed(db, TABLE_SONGS, time, song.getId());
                updatePlayed(db, TABLE_ARTISTS, time, song.getArtistId());
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            song.setLastPlayed(time);
            try (Cursor c = query(db, TABLE_SONGS, new String[]{Song.TIMES_PLAYED}, song.getId())) {
                song.setTimesPlayed(c.getInt(0));
            }
        }
    }

    public void deleteSong(Song song) {
        Log.d(TAG, "DbHelper.deleteSong(" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                delete(db, TABLE_SONGS, song.getId());

                if (db.delete(TABLE_ARTISTS, Artist._ID + " NOT IN (SELECT " + Song.ARTIST_ID +
                        " FROM " + TABLE_SONGS + ")", null) == 0) {
                    updateArtistStats(db, song);
                } else {
                    Log.d(TAG, "Deleted artist: " + song.getArtist());
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }
    
    public Stats queryStats(long artistId) {
        try (SQLiteDatabase db = getReadableDatabase()) {
            Stats stats = new Stats();
            stats.setArtistCount(queryInt(db, SQL_QUERY_ARTIST_COUNT, null));
            stats.setSongCount(queryInt(db, SQL_QUERY_SONG_COUNT, null));
            stats.setSongsDuration(queryLong(db, SQL_QUERY_SONGS_DURATION, null));
            stats.setSongsPlayed(queryInt(db, SQL_QUERY_SONGS_PLAYED, null));
            stats.setSongsUnplayed(queryInt(db, SQL_QUERY_SONGS_UNPLAYED, null));
            stats.setSongsTagged(queryInt(db, SQL_QUERY_SONGS_TAGGED, null));
            stats.setSongsUntagged(queryInt(db, SQL_QUERY_SONGS_UNTAGGED, null));
            stats.setTimesPlayed(queryInt(db, SQL_QUERY_TIMES_PLAYED, null));
            stats.setPlayedDuration(queryLong(db, SQL_QUERY_PLAYED_DURATION, null));
            return stats;
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
                        db, TABLE_ARTISTS, new String[]{Artist._ID, Artist.ARTIST},
                        null, new int[0], 1, artistIgnore, -1, null, null, 0);

                resSongs = syncTable(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        db, TABLE_SONGS, new String[]{Song._ID, Song.TITLE,
                                Song.ARTIST_ID, Song.ARTIST, Song.DURATION, Song.YEAR},
                        new int[]{1, 2, 3, 4}, new int[]{5}, -1, null, 2, resArtists.ids,
                        Song.ADDED, time);

                // Update artists when songs have been inserted or deleted.
                if (resSongs.rowsInserted > 0 || resSongs.rowsDeleted > 0) {
                    db.execSQL(SQL_UPDATE_ARTISTS_STATS);
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
            backupTable(backup, db, TABLE_ARTISTS, new String[]{Artist.ARTIST,
                    Artist.LAST_SONG_ADDED, Artist.LAST_PLAYED, Artist.TIMES_PLAYED});

            backupTable(backup, db, TABLE_SONGS, new String[]{Song.TITLE, Song.ARTIST, Song.YEAR,
                    Song.ADDED, Song.TAG, Song.BOOKMARKED, Song.LAST_PLAYED, Song.TIMES_PLAYED});
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
        //updateBackup(backup);

        // Update database tables from JSONObject.
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                restoreTableBackup(backup, db, TABLE_ARTISTS, new String[]{Artist.LAST_SONG_ADDED,
                                Artist.LAST_PLAYED, Artist.TIMES_PLAYED},
                        Artist.ARTIST + " LIKE ?",
                        new String[]{Artist.ARTIST});

                restoreTableBackup(backup, db, TABLE_SONGS, new String[]{Song.YEAR, Song.ADDED,
                                Song.TAG, Song.BOOKMARKED, Song.LAST_PLAYED, Song.TIMES_PLAYED},
                        Song.TITLE + " LIKE ? AND " + Song.ARTIST + " LIKE ?",
                        new String[]{Song.TITLE, Song.ARTIST});

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public static String[] getWhereArgs(long id) {
        return new String[]{Long.toString(id)};
    }

    private static void putValue(ContentValues values, String key, long value) {
        if (value == 0) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }

    /*private static void updateBackup(JSONObject backup) throws JSONException {
        Log.d(TAG, "updateBackup()");

        JSONArray artists = backup.getJSONArray(TABLE_ARTISTS);
        LongSparseArray<String> artistNames = new LongSparseArray<>();
        for (int i = 0; i < artists.length(); i++) {
            JSONObject artist = artists.getJSONObject(i);
            artistNames.put(artist.getLong(Artist._ID), artist.getString(Artist.ARTIST));

            if (artist.has("date_modified")) {
                artist.put(Artist.LAST_SONG_ADDED, artist.getLong("date_modified"));
                artist.remove("date_modified");
            }
        }

        JSONArray songs = backup.getJSONArray(TABLE_SONGS);
        for (int i = 0; i < songs.length(); i++) {
            JSONObject song = songs.getJSONObject(i);

            String artist = artistNames.get(song.getLong(Song.ARTIST_ID));
            if (artist == null) {
                throw new JSONException("Artist not found");
            }
            song.put(Song.ARTIST, artist);
            song.remove(Song.ARTIST_ID);

            if (song.has(Song.DATE_ADDED)) {
                song.put(Song.ADDED, song.getLong(Song.DATE_ADDED));
                song.remove(Song.DATE_ADDED);
            }
        }
    }*/

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
        Cursor c = db.query(table, columns, SQL_ID_IS, getWhereArgs(id), null, null, null);
        c.moveToFirst();
        return c;
    }

    private static int update(SQLiteDatabase db, String table, ContentValues values, long id) {
        return db.update(table, values, SQL_ID_IS, getWhereArgs(id));
    }

    private static int delete(SQLiteDatabase db, String table, long id) {
        return db.delete(table, SQL_ID_IS, getWhereArgs(id));
    }

    private static void updatePlayed(SQLiteDatabase db, String table, long time, long id) {
        db.execSQL("UPDATE " + table + " SET " +
                PlayedColumns.LAST_PLAYED + "=?," +
                PlayedColumns.TIMES_PLAYED + "=" + PlayedColumns.TIMES_PLAYED + "+1 " +
                        "WHERE " + SQL_ID_IS,
                new Object[]{time, id});
    }

    private static void updateArtistStats(SQLiteDatabase db, Song song) {
        Log.d(TAG, "DbHelper.updateArtistStats(" + song + ")");
        db.execSQL(SQL_UPDATE_ARTIST_STATS, new Object[]{song.getArtistId()});
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

    interface ArtistColumns extends MediaStore.Audio.ArtistColumns {
        String LAST_SONG_ADDED = "last_song_added";
    }

    interface SongColumns extends MediaStore.Audio.AudioColumns {
        String ADDED = "added";
        String TAG = "tag";
        String BOOKMARKED = "bookmarked";
    }

    interface PlayedColumns {
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
