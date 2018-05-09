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

public class DbHelper extends SQLiteOpenHelper {
    private static final String TAG = "UPlayer";

    private static final String DATABASE_NAME = "UPlayer.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_ARTISTS = "artists";
    private static final String TABLE_SONGS = "songs";

    private static final String SQL_ID_IS = BaseColumns._ID + "=?";
    private static final String SQL_WHERE_ID_IS = " WHERE " + SQL_ID_IS;

    private static final File ARTIST_IGNORE_FILE = Util.getMusicFile("ignore.txt");
    private static final File BACKUP_FILE = Util.getMusicFile("UPlayer.json");

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "DbHelper.onCreate()");

        db.execSQL("CREATE TABLE " + TABLE_ARTISTS + "(" +
                Artist._ID + " INTEGER PRIMARY KEY," +
                Artist.ARTIST + " TEXT," +
                Artist.LAST_SONG_ADDED + " INTEGER," +
                Artist.LAST_PLAYED + " INTEGER," +
                Artist.TIMES_PLAYED + " INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_SONGS + "(" +
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
                Song.TIMES_PLAYED + " INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public ArrayList<Artist> queryArtists(String orderBy) {
        Log.d(TAG, "DbHelper.queryArtists(" + orderBy + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_ARTISTS, new String[]{Artist._ID, Artist.ARTIST,
                            Artist.LAST_SONG_ADDED, Artist.LAST_PLAYED, Artist.TIMES_PLAYED},
                    null, null, null, null, orderBy)) {
                ArrayList<Artist> artists = new ArrayList<>();
                while (c.moveToNext()) {
                    Artist artist = new Artist();
                    artist.setId(c.getLong(0));
                    artist.setArtist(c.getString(1));
                    artist.setLastSongAdded(c.getLong(2));
                    artist.setLastPlayed(c.getLong(3));
                    artist.setTimesPlayed(c.getInt(4));
                    artists.add(artist);
                }
                Log.d(TAG, artists.size() + " artists queried");
                return artists;
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

    public ArrayList<Song> querySongs(String selection, String[] selectionArgs, String orderBy) {
        Log.d(TAG, "DbHelper.querySongs(" + selection + "," + orderBy + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_SONGS, new String[]{Song._ID, Song.TITLE,
                            Song.ARTIST_ID, Song.ARTIST, Song.DURATION, Song.TIMES_PLAYED},
                    selection, selectionArgs, null, null, orderBy)) {
                ArrayList<Song> songs = new ArrayList<>();
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
                return songs;
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
        Log.d(TAG, "DbOpenHelper.querySongTags()");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(true, TABLE_SONGS, new String[]{Song.TAG},
                    Song.TAG + " IS NOT NULL", null, null, null, Song.TAG, null)) {
                List<String> tags = new ArrayList<>();
                while (c.moveToNext()) {
                    tags.add(c.getString(0));
                }
                Log.d(TAG, tags.size() + " song tags queried");
                return tags;
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
            db.execSQL("UPDATE " + TABLE_SONGS + " SET " +
                            Song.BOOKMARKED + "=CASE WHEN " +
                            Song.BOOKMARKED + " IS NULL THEN ? ELSE NULL END " +
                            SQL_WHERE_ID_IS,
                    new Object[]{Calendar.currentTime(), song.getId()});

            song.setBookmarked(queryLong(db, TABLE_SONGS, Song.BOOKMARKED, song.getId()));
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
            song.setTimesPlayed(queryInt(db, TABLE_SONGS, Song.TIMES_PLAYED, song.getId()));
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

    public Stats queryStats(Artist artist) {
        //TODO: Improve (artist) statistics and fix division by zero.
        Log.d(TAG, "DbOpenHelper.queryStats(" + artist + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            Stats stats = new Stats();
            
            String[] artistIdWhereArgs;
            if (artist == null) {
                stats.setArtistCount(queryInt(db,
                        "SELECT COUNT(*) FROM " + TABLE_ARTISTS, null));

                stats.setTimesPlayed(queryInt(db, addArtistIdWhereClause(
                        "SELECT SUM(" + Song.TIMES_PLAYED + ") FROM " + TABLE_SONGS,
                        null, false), null));

                artistIdWhereArgs = null;
            } else {
                artistIdWhereArgs = getWhereArgs(artist.getId());
            }

            stats.setSongCount(queryInt(db, addArtistIdWhereClause(
                    "SELECT COUNT(*) FROM " + TABLE_SONGS,
                    artist, false), artistIdWhereArgs));

            stats.setSongsDuration(queryLong(db, addArtistIdWhereClause(
                    "SELECT SUM(" + Song.DURATION + ") FROM " + TABLE_SONGS, artist, false),
                    artistIdWhereArgs));

            stats.setSongsPlayed(queryInt(db, addArtistIdWhereClause(
                    "SELECT COUNT(*) FROM " + TABLE_SONGS + " WHERE " + Song.TIMES_PLAYED + ">0",
                    artist, true), artistIdWhereArgs));

            stats.setSongsUnplayed(queryInt(db, addArtistIdWhereClause(
                    "SELECT COUNT(*) FROM " + TABLE_SONGS + " WHERE " + Song.TIMES_PLAYED + "=0",
                    artist, true), artistIdWhereArgs));

            stats.setSongsTagged(queryInt(db, addArtistIdWhereClause(
                    "SELECT COUNT(*) FROM " + TABLE_SONGS + " WHERE " + Song.TAG + " IS NOT NULL",
                    artist, true), artistIdWhereArgs));

            stats.setSongsUntagged(queryInt(db, addArtistIdWhereClause(
                    "SELECT COUNT(*) FROM " + TABLE_SONGS + " WHERE " + Song.TAG + " IS NULL",
                    artist, true), artistIdWhereArgs));

            stats.setPlayedDuration(queryLong(db, addArtistIdWhereClause(
                    "SELECT SUM(" + Song.DURATION + "*" + Song.TIMES_PLAYED +
                            ") FROM " + TABLE_SONGS,
                    artist, false), artistIdWhereArgs));

            return stats;
        }
    }

    public void syncWithMediaStore(Context context) throws IOException {
        // Read artist ignore file.
        List<String> artistIgnore = new ArrayList<>();
        if (ARTIST_IGNORE_FILE.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(ARTIST_IGNORE_FILE)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    artistIgnore.add(line);
                }
            }
        }
        Log.d(TAG, artistIgnore.size() + " artists on ignore list");

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
                    updateArtistStats(db, null);
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
        JSONArray songs = new JSONArray();

        // Put songs table in JSONArray.
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_SONGS, new String[]{Song.TITLE, Song.ARTIST, Song.YEAR,
                    Song.ADDED, Song.TAG, Song.BOOKMARKED, Song.LAST_PLAYED, Song.TIMES_PLAYED},
                    null, null, null, null, null)) {
                while (c.moveToNext()) {
                    JSONObject song = new JSONObject();
                    putValueFromCursor(c, 0, song, Song.TITLE);
                    putValueFromCursor(c, 1, song, Song.ARTIST);
                    putValueFromCursor(c, 2, song, Song.YEAR);
                    putValueFromCursor(c, 3, song, Song.ADDED);
                    putValueFromCursor(c, 4, song, Song.TAG);
                    putValueFromCursor(c, 5, song, Song.BOOKMARKED);
                    putValueFromCursor(c, 6, song, Song.LAST_PLAYED);
                    putValueFromCursor(c, 7, song, Song.TIMES_PLAYED);
                    songs.put(song);
                }
            }
        }

        // Write JSONArray to file.
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(BACKUP_FILE, false)))) {
            writer.write(songs.toString());
        }

        Log.d(TAG, songs.length() + " songs backed up");
    }

    public void restoreBackup() throws IOException, JSONException {
        // Read JSONArray from file.
        JSONArray songs;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(BACKUP_FILE)))) {
            songs = new JSONArray(reader.readLine());
        }

        // Update songs table from JSONArray.
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                for (int index = 0; index < songs.length(); index++) {
                    // Put values to update from JSONObject.
                    JSONObject song = songs.getJSONObject(index);
                    ContentValues values = new ContentValues();
                    putValueFromJSONObject(song, values, Song.YEAR);
                    putValueFromJSONObject(song, values, Song.ADDED);
                    putValueFromJSONObject(song, values, Song.TAG);
                    putValueFromJSONObject(song, values, Song.BOOKMARKED);
                    putValueFromJSONObject(song, values, Song.LAST_PLAYED);
                    putValueFromJSONObject(song, values, Song.TIMES_PLAYED);

                    // Update row and make sure 1 row is updated.
                    switch (db.update(TABLE_SONGS, values,
                            Song.TITLE + " LIKE ? AND " + Song.ARTIST + " LIKE ?", new String[]{
                            song.getString(Song.TITLE), song.getString(Song.ARTIST)})) {
                        case 0:
                            throw new RuntimeException("Song not found: '" +
                                    song.getString(Song.ARTIST) + "' - '" +
                                    song.getString(Song.TITLE) + "'");
                        case 1:
                            Log.d(TAG, "Song updated: '" +
                                    song.getString(Song.ARTIST) + "' - '" +
                                    song.getString(Song.TITLE) + "'");
                            break;
                        default:
                            throw new RuntimeException("Duplicate song: '" +
                                    song.getString(Song.ARTIST) + "' - '" +
                                    song.getString(Song.TITLE) + "'");
                    }
                }
                updateArtistStats(db, null);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        Log.d(TAG, songs.length() + " songs restored");
    }

    public static String[] getWhereArgs(long id) {
        return new String[]{Long.toString(id)};
    }

    private static SyncResult syncTable(Context context, Uri contentUri,
                                        SQLiteDatabase db, String table, String[] columns,
                                        int[] updateColumns, int[] insertColumns,
                                        int ignoreColumn, List<String> ignoreValues,
                                        int refIdColumn, List<Long> refIds,
                                        String timeColumn, long time) {
        SyncResult res = new SyncResult();

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
                        res.rowsIgnored++;
                        continue;
                    }
                }

                // Ignore rows with a non-existing ref ID.
                if (refIdColumn != -1) {
                    long refId = c.getLong(refIdColumn);
                    if (!refIds.contains(refId)) {
                        Log.d(TAG, table + "." + columns[refIdColumn] + " value " +
                                refId + " ignored: " + id);
                        res.rowsIgnored++;
                        continue;
                    }
                }

                // Put update column values.
                ContentValues values = new ContentValues();
                putValuesFromCursor(c, columns, values, updateColumns);

                // Update or insert row if it doesn't exist.
                if (update(db, table, values, id) == 0) {
                    values.put(BaseColumns._ID, id);
                    putValuesFromCursor(c, columns, values, insertColumns);
                    if (timeColumn != null) {
                        values.put(timeColumn, time);
                    }
                    db.insert(table, null, values);
                    Log.d(TAG, table + " row inserted: " + id);
                    res.rowsInserted++;
                } else {
                    Log.d(TAG, table + " row updated: " + id);
                    res.rowsUpdated++;
                }
                res.ids.add(id);
            }
        }

        try (Cursor c = db.query(table, new String[]{BaseColumns._ID},
                null, null, null, null, null)) {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                if (!res.ids.contains(id)) {
                    delete(db, table, id);
                    Log.d(TAG, table + " row deleted: " + id);
                    res.rowsDeleted++;
                }
            }
        }

        return res;
    }

    private static void updateArtistStats(SQLiteDatabase db, Song song) {
        Log.d(TAG, "DbHelper.updateArtistStats(" + song + ")");

        String sql = "UPDATE " + TABLE_ARTISTS + " SET " +
                Artist.LAST_SONG_ADDED +
                "=(SELECT MAX(" + Song.ADDED + ") FROM " + TABLE_SONGS +
                " WHERE " + Song.ARTIST_ID + "=" + TABLE_ARTISTS + "." + Artist._ID + ")," +

                Artist.LAST_PLAYED +
                "=(SELECT MAX(" + Song.LAST_PLAYED + ") FROM " + TABLE_SONGS +
                " WHERE " + Song.ARTIST_ID + "=" + TABLE_ARTISTS + "." + Artist._ID + ")," +

                Artist.TIMES_PLAYED +
                "=(SELECT SUM(" + Song.TIMES_PLAYED + ") FROM " + TABLE_SONGS +
                " WHERE " + Song.ARTIST_ID + "=" + TABLE_ARTISTS + "." + Artist._ID + ")";

        if (song == null) {
            db.execSQL(sql);
        } else {
            db.execSQL(sql + SQL_WHERE_ID_IS, new Object[]{song.getArtistId()});
        }
    }

    private static void updatePlayed(SQLiteDatabase db, String table, long time, long id) {
        db.execSQL("UPDATE " + table + " SET " +
                        PlayedColumns.LAST_PLAYED + "=?," +
                        PlayedColumns.TIMES_PLAYED + "=" + PlayedColumns.TIMES_PLAYED + "+1" +
                        SQL_WHERE_ID_IS,
                new Object[]{time, id});
    }

    private static String addArtistIdWhereClause(String sql, Artist artist, boolean and) {
        return artist == null ? sql : sql + " " +
                (and ? "AND" : "WHERE") + " " + Song.ARTIST_ID + "=?";
    }

    private static Cursor query(SQLiteDatabase db, String table, String[] columns, long id) {
        Cursor c = db.query(table, columns, SQL_ID_IS, getWhereArgs(id), null, null, null);
        c.moveToFirst();
        return c;
    }

    private static int queryInt(SQLiteDatabase db, String table, String column, long id) {
        try (Cursor c = query(db, table, new String[]{column}, id)) {
            return c.getInt(0);
        }
    }

    private static int queryInt(SQLiteDatabase db, String sql, String[] selectionArgs) {
        try (Cursor c = db.rawQuery(sql, selectionArgs)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    private static long queryLong(SQLiteDatabase db, String table, String column, long id) {
        try (Cursor c = query(db, table, new String[]{column}, id)) {
            return c.getLong(0);
        }
    }

    private static long queryLong(SQLiteDatabase db, String sql, String[] selectionArgs) {
        try (Cursor c = db.rawQuery(sql, selectionArgs)) {
            c.moveToFirst();
            return c.getLong(0);
        }
    }

    private static int update(SQLiteDatabase db, String table, ContentValues values, long id) {
        return db.update(table, values, SQL_ID_IS, getWhereArgs(id));
    }

    private static int delete(SQLiteDatabase db, String table, long id) {
        return db.delete(table, SQL_ID_IS, getWhereArgs(id));
    }

    private static void putValue(ContentValues values, String key, long value) {
        if (value == 0) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }

    private static void putValuesFromCursor(Cursor c, String[] columns,
                                            ContentValues values, int[] putColumns) {
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

    private static void putValueFromCursor(Cursor c, int column, JSONObject obj, String key)
            throws JSONException {
        switch (c.getType(column)) {
            case Cursor.FIELD_TYPE_NULL:
                break;
            case Cursor.FIELD_TYPE_INTEGER:
                obj.put(key, c.getLong(column));
                break;
            case Cursor.FIELD_TYPE_STRING:
                obj.put(key, c.getString(column));
                break;
            default:
                throw new RuntimeException("Invalid type");
        }
    }

    private static void putValueFromJSONObject(JSONObject obj, ContentValues values, String key)
            throws JSONException {
        if (obj.has(key)) {
            Object value = obj.get(key);
            if (value instanceof Integer) {
                values.put(key, (Integer) value);
            } else if (value instanceof Long) {
                values.put(key, (Long) value);
            } else if (value instanceof String) {
                values.put(key, (String) value);
            } else {
                throw new RuntimeException("Invalid type");
            }
        } else {
            values.putNull(key);
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
