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
import android.util.Log;

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

public class DbHelper extends SQLiteOpenHelper {
    private static final String TAG = "UPlayer";

    private static final String DATABASE_NAME = "UPlayer.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_ARTISTS = "artists";
    private static final String TABLE_SONGS = "songs";
    private static final String TABLE_PLAYLISTS = "playlists";
    private static final String TABLE_PLAYLIST_SONGS = "playlist_songs";

    private static final String SQL_ID_IS = BaseColumns._ID + "=?";
    private static final String SQL_WHERE_ID_IS = " WHERE " + SQL_ID_IS;

    private static final String SQL_ARTIST_ID_IS = Song.ARTIST_ID + "=?";
    private static final String SQL_WHERE_ARTIST_ID_IS = " WHERE " + SQL_ARTIST_ID_IS;

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
                Artist.LAST_ADDED + " INTEGER," +
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

        db.execSQL("CREATE TABLE " + TABLE_PLAYLISTS + "(" +
                Playlist._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                Playlist.NAME + " TEXT," +
                Playlist.MODIFIED + " INTEGER," +
                Playlist.SONG_INDEX + " INTEGER," +
                Playlist.SONG_POSITION + " INTEGER)");

        db.execSQL("CREATE TABLE " + TABLE_PLAYLIST_SONGS + "(" +
                Playlist._ID + " INTEGER PRIMARY KEY," +
                Playlist.PLAYLIST_ID + " INTEGER," +
                Playlist.SONG_ID + ")");

        ContentValues values = new ContentValues();
        values.put(Playlist._ID, 1);
        db.insert(TABLE_PLAYLISTS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public ArrayList<Artist> queryArtists(String orderBy) {
        Log.d(TAG, "DbHelper.queryArtists('" + orderBy + "')");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_ARTISTS, null,
                    null, null, null, null, orderBy)) {
                ArrayList<Artist> artists = new ArrayList<>();
                while (c.moveToNext()) {
                    Artist artist = new Artist();
                    artist.setId(c.getLong(0));
                    artist.setArtist(c.getString(1));
                    artist.setLastAdded(c.getLong(2));
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
            try (Cursor c = query(db, TABLE_ARTISTS,new String[]{Artist.LAST_ADDED,
                            Artist.LAST_PLAYED, Artist.TIMES_PLAYED},
                    artist.getId())) {
                artist.setLastAdded(c.getLong(0));
                artist.setLastPlayed(c.getLong(1));
                artist.setTimesPlayed(c.getInt(2));
            }
        }
    }

    public ArrayList<Song> querySongs(String selection, String[] selectionArgs, String orderBy) {
        Log.d(TAG, "DbHelper.querySongs(" + selection + ",'" + orderBy + "')");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_SONGS, null,
                    selection, selectionArgs, null, null, orderBy)) {
                ArrayList<Song> songs = new ArrayList<>();
                while (c.moveToNext()) {
                    Song song = new Song();
                    song.setId(c.getLong(0));
                    song.setTitle(c.getString(1));
                    song.setArtistId(c.getLong(2));
                    song.setArtist(c.getString(3));
                    song.setDuration(c.getLong(4));
                    song.setYear(c.getInt(5));
                    song.setAdded(c.getLong(6));
                    song.setTag(c.getString(7));
                    song.setBookmarked(c.getLong(8));
                    song.setLastPlayed(c.getLong(9));
                    song.setTimesPlayed(c.getInt(10));
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

    public ArrayList<String> querySongTags() {
        Log.d(TAG, "DbHelper.querySongTags()");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(true, TABLE_SONGS, new String[]{Song.TAG},
                    Song.TAG + " IS NOT NULL", null, null, null, Song.TAG, null)) {
                ArrayList<String> tags = new ArrayList<>();
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

                update(db, TABLE_SONGS, values, song.getId(), true);
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
            db.beginTransaction();
            try {
                update(db, "UPDATE " + TABLE_SONGS + " SET " +
                                Song.BOOKMARKED + "=CASE WHEN " +
                                Song.BOOKMARKED + " IS NULL THEN ? ELSE NULL END " +
                                SQL_WHERE_ID_IS,
                        new Object[]{Calendar.currentTime(), song.getId()},
                        TABLE_SONGS, song.getId());
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
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

                // Update artist stats if the artist has other songs, delete it otherwise.
                if (queryInt(db, "SELECT COUNT(*) FROM " + TABLE_SONGS + SQL_WHERE_ARTIST_ID_IS,
                        getWhereArgs(song.getArtistId())) > 0) {
                    updateArtistStats(db, song);
                } else {
                    Log.d(TAG, "Deleting artist: '" + song.getArtist() + "'");
                    delete(db, TABLE_ARTISTS, song.getArtistId());
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public ArrayList<Playlist> queryPlaylists() {
        Log.d(TAG, "DbHelper.queryPlaylists()");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_PLAYLISTS, null, Playlist.MODIFIED + " IS NOT NULL",
                    null, null, null, Playlist.MODIFIED + " DESC," + Playlist.NAME)) {
                ArrayList<Playlist> playlists = new ArrayList<>();
                while (c.moveToNext()) {
                    Playlist playlist = new Playlist();
                    playlist.setId(c.getLong(0));
                    playlist.setName(c.getString(1));
                    playlist.setModified(c.getLong(2));
                    playlist.setSongIndex(c.getInt(3));
                    playlist.setSongPosition(c.getInt(4));
                    playlists.add(playlist);
                }
                Log.d(TAG, playlists.size() + " playlists queried");
                return playlists;
            }
        }
    }

    public ArrayList<Song> queryPlaylistSongs(Playlist playlist) {
        Log.d(TAG, "DbHelper.queryPlaylistSongs(" + playlist + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_PLAYLIST_SONGS + "," + TABLE_SONGS, new String[]{
                    Playlist.SONG_ID, Song.TITLE, Song.ARTIST_ID, Song.ARTIST,
                            Song.DURATION, Song.TIMES_PLAYED},
                    Playlist.SONG_ID + "=" + TABLE_SONGS + "." + Song._ID +
                            " AND " + Playlist.PLAYLIST_ID + "=?",
                    getWhereArgs(playlist.getId()), null, null,
                    TABLE_PLAYLIST_SONGS + "." + Playlist._ID)) {
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
                Log.d(TAG, songs.size() + " playlist songs queried");
                return songs;
            }
        }
    }

    public void insertOrUpdatePlaylist(Playlist playlist, List<Song> songs) {
        Log.d(TAG, "DbHelper.insertPlaylist(" + playlist + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues values;

                // Insert or update playlist, deleting any existing playlist songs.
                values = new ContentValues();
                values.put(Playlist.NAME, playlist.getName());
                putValue(values, Playlist.MODIFIED, playlist.getModified());
                putValue(values, Playlist.SONG_INDEX, playlist.getSongIndex());
                putValue(values, Playlist.SONG_POSITION, playlist.getSongPosition());
                if (playlist.getId() == 0) {
                    playlist.setId(db.insert(TABLE_PLAYLISTS, null, values));
                    Log.d(TAG, "Playlist inserted: " + playlist.getId());
                } else {
                    update(db, TABLE_PLAYLISTS, values, playlist.getId(), true);
                    deletePlaylistSongs(db, playlist);
                }

                // Insert playlist songs.
                for (Song song : songs) {
                    values = new ContentValues();
                    values.put(Playlist.PLAYLIST_ID, playlist.getId());
                    values.put(Playlist.SONG_ID, song.getId());
                    db.insert(TABLE_PLAYLIST_SONGS, null, values);
                }
                Log.d(TAG, songs.size() + " playlist songs inserted");

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void deletePlaylist(Playlist playlist) {
        Log.d(TAG, "DbHelper.deletePlaylist(" + playlist + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                deletePlaylistSongs(db, playlist);
                delete(db, TABLE_PLAYLISTS, playlist.getId());
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public Stats queryStats(Artist artist) {
        Log.d(TAG, "DbHelper.queryStats(" + artist + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            Stats stats = new Stats();
            
            String[] artistIdWhereArgs = artist == null ? null : getWhereArgs(artist.getId());

            stats.setSongCount(queryInt(db, appendWhereArtistId(
                    "SELECT COUNT(*) FROM " + TABLE_SONGS,
                    artist, false), artistIdWhereArgs));

            stats.setSongsDuration(queryLong(db, appendWhereArtistId(
                    "SELECT SUM(" + Song.DURATION + ") FROM " + TABLE_SONGS,
                    artist, false), artistIdWhereArgs));

            if (artist == null) {
                stats.setArtistCount(queryInt(db,
                        "SELECT COUNT(*) FROM " + TABLE_ARTISTS, null));
            }

            stats.setSongsPlayed(queryInt(db, appendWhereArtistId(
                    "SELECT COUNT(*) FROM " + TABLE_SONGS + " WHERE " + Song.TIMES_PLAYED + ">0",
                    artist, true), artistIdWhereArgs));

            stats.setSongsTagged(queryInt(db, appendWhereArtistId(
                    "SELECT COUNT(*) FROM " + TABLE_SONGS + " WHERE " + Song.TAG + " IS NOT NULL",
                    artist, true), artistIdWhereArgs));

            if (artist == null) {
                stats.setLastAdded(queryLong(db,
                        "SELECT MAX(" + Song.ADDED + ") FROM " + TABLE_SONGS, null));

                stats.setLastPlayed(queryLong(db,
                        "SELECT MAX(" + Song.LAST_PLAYED + ") FROM " + TABLE_SONGS, null));

                stats.setTimesPlayed(queryInt(db,
                        "SELECT SUM(" + Song.TIMES_PLAYED + ") FROM " + TABLE_SONGS, null));
            } else {
                stats.setLastAdded(artist.getLastAdded());
                stats.setLastPlayed(artist.getLastPlayed());
                stats.setTimesPlayed(artist.getTimesPlayed());
            }

            stats.setPlayedDuration(queryLong(db, appendWhereArtistId(
                    "SELECT SUM(" + Song.DURATION + "*" + Song.TIMES_PLAYED +
                            ") FROM " + TABLE_SONGS,
                    artist, false), artistIdWhereArgs));

            return stats;
        }
    }

    public SyncResult[] syncWithMediaStore(Context context) throws IOException {
        Log.d(TAG, "DbHelper.syncWithMediaStore()");

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

        // Sync artists and songs tables.
        SyncResult[] results = new SyncResult[2];
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                long time = Calendar.currentTime();

                results[0] = syncTable(context, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                        db, TABLE_ARTISTS, new String[]{Artist._ID, Artist.ARTIST},
                        null, new int[0], 1, artistIgnore, -1, null, null, 0);

                results[1] = syncTable(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        db, TABLE_SONGS, new String[]{Song._ID, Song.TITLE,
                                Song.ARTIST_ID, Song.ARTIST, Song.DURATION, Song.YEAR},
                        new int[]{1, 2, 3, 4}, new int[]{5}, -1, null, 2, results[0].ids,
                        Song.ADDED, time);

                // Update artists when songs have been inserted or deleted.
                if (results[1].rowsInserted > 0 || results[1].rowsDeleted > 0) {
                    updateArtistStats(db, null);
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        return results;
    }

    public void backup() throws JSONException, IOException {
        Log.d(TAG, "DbHelper.backup()");
        JSONObject backup = new JSONObject();

        //TODO: Methods for backup up/restoring tables?
        try (SQLiteDatabase db = getReadableDatabase()) {
            // Backup songs table.
            try (Cursor c = db.query(TABLE_SONGS, new String[]{Song.TITLE, Song.ARTIST, Song.YEAR,
                    Song.ADDED, Song.TAG, Song.BOOKMARKED, Song.LAST_PLAYED, Song.TIMES_PLAYED},
                    null, null, null, null, null)) {
                JSONArray songs = new JSONArray();
                while (c.moveToNext()) {
                    JSONObject song = new JSONObject();
                    putValue(c, 0, song, Song.TITLE);
                    putValue(c, 1, song, Song.ARTIST);
                    putValue(c, 2, song, Song.YEAR);
                    putValue(c, 3, song, Song.ADDED);
                    putValue(c, 4, song, Song.TAG);
                    putValue(c, 5, song, Song.BOOKMARKED);
                    putValue(c, 6, song, Song.LAST_PLAYED);
                    putValue(c, 7, song, Song.TIMES_PLAYED);
                    songs.put(song);
                }
                backup.put(TABLE_SONGS, songs);
                Log.d(TAG, songs.length() + " songs backed up");
            }

            // Backup playlists table.
            try (Cursor c = db.query(TABLE_PLAYLISTS, null, null, null, null, null, null)) {
                JSONArray playlists = new JSONArray();
                while (c.moveToNext()) {
                    JSONObject playlist = new JSONObject();
                    putValue(c, 0, playlist, Playlist._ID);
                    putValue(c, 1, playlist, Playlist.NAME);
                    putValue(c, 2, playlist, Playlist.MODIFIED);
                    putValue(c, 3, playlist, Playlist.SONG_INDEX);
                    putValue(c, 4, playlist, Playlist.SONG_POSITION);
                    playlists.put(playlist);
                }
                backup.put(TABLE_PLAYLISTS, playlists);
                Log.d(TAG, playlists.length() + " playlists backed up");
            }

            // Backup playlist_songs table.
            try (Cursor c = db.query(TABLE_PLAYLIST_SONGS, null, null, null, null, null, null)) {
                JSONArray playlistSongs = new JSONArray();
                while (c.moveToNext()) {
                    JSONObject playlistSong = new JSONObject();
                    putValue(c, 0, playlistSong, Playlist._ID);
                    putValue(c, 1, playlistSong, Playlist.PLAYLIST_ID);
                    putValue(c, 2, playlistSong, Playlist.SONG_ID);
                    playlistSongs.put(playlistSong);
                }
                backup.put(TABLE_PLAYLIST_SONGS, playlistSongs);
                Log.d(TAG, playlistSongs.length() + " playlist songs backed up");
            }
        }

        // Write JSONObject to file.
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(BACKUP_FILE, false)))) {
            writer.write(backup.toString());
        }
    }

    public void restoreBackup() throws IOException, JSONException {
        Log.d(TAG, "DbHelper.restoreBackup()");

        // Read JSONObject from file.
        JSONObject backup;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(BACKUP_FILE)))) {
            backup = new JSONObject(reader.readLine());
        }

        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                // Restore songs table.
                JSONArray songs = backup.getJSONArray(TABLE_SONGS);
                for (int i = 0; i < songs.length(); i++) {
                    // Put values to update from JSONObject.
                    JSONObject song = songs.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    putValue(song, values, Song.YEAR);
                    putValue(song, values, Song.ADDED);
                    putValue(song, values, Song.TAG);
                    putValue(song, values, Song.BOOKMARKED);
                    putValue(song, values, Song.LAST_PLAYED);
                    putValue(song, values, Song.TIMES_PLAYED);

                    // Update row and make sure 1 row is updated.
                    switch (db.update(TABLE_SONGS, values,
                            Song.TITLE + " LIKE ? AND " + Song.ARTIST + " LIKE ?", new String[]{
                            song.getString(Song.TITLE), song.getString(Song.ARTIST)})) {
                        case 0:
                            throw new SQLiteException("Song not found: '" +
                                    song.getString(Song.ARTIST) + " - " +
                                    song.getString(Song.TITLE) + "'");
                        case 1:
                            Log.d(TAG, "Song updated: '" +
                                    song.getString(Song.ARTIST) + " - " +
                                    song.getString(Song.TITLE) + "'");
                            break;
                        default:
                            throw new SQLiteException("Duplicate song: '" +
                                    song.getString(Song.ARTIST) + " - " +
                                    song.getString(Song.TITLE) + "'");
                    }
                }
                Log.d(TAG, songs.length() + " songs restored");
                updateArtistStats(db, null);

                // Restore playlists table.
                Log.d(TAG, db.delete(TABLE_PLAYLISTS, null, null) + " playlists deleted");
                JSONArray playlists = backup.getJSONArray(TABLE_PLAYLISTS);
                for (int i = 0; i < playlists.length(); i++) {
                    // Put values to update from JSONObject and insert it.
                    JSONObject playlist = playlists.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    putValue(playlist, values, Playlist._ID);
                    putValue(playlist, values, Playlist.NAME);
                    putValue(playlist, values, Playlist.MODIFIED);
                    putValue(playlist, values, Playlist.SONG_INDEX);
                    putValue(playlist, values, Playlist.SONG_POSITION);
                    db.insert(TABLE_PLAYLISTS, null, values);
                }
                Log.d(TAG, playlists.length() + " playlists restored");

                // Restore playlist songs table.
                Log.d(TAG, db.delete(TABLE_PLAYLIST_SONGS, null, null) + " playlist songs deleted");
                JSONArray playlistSongs = backup.getJSONArray(TABLE_PLAYLIST_SONGS);
                for (int i = 0; i < playlistSongs.length(); i++) {
                    // Put values to update from JSONObject and insert it.
                    JSONObject playlistSong = playlistSongs.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    putValue(playlistSong, values, Playlist._ID);
                    putValue(playlistSong, values, Playlist.PLAYLIST_ID);
                    putValue(playlistSong, values, Playlist.SONG_ID);
                    db.insert(TABLE_PLAYLIST_SONGS, null, values);
                }
                Log.d(TAG, playlistSongs.length() + " playlist songs restored");
                //FIXME: Playlist song ID could be invalid after restoring a backup.

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public static String[] getWhereArgs(long id) {
        return new String[]{Long.toString(id)};
    }

    private static void updatePlayed(SQLiteDatabase db, String table, long time, long id) {
        update(db, "UPDATE " + table + " SET " +
                        PlayedColumns.LAST_PLAYED + "=?," +
                        PlayedColumns.TIMES_PLAYED + "=" + PlayedColumns.TIMES_PLAYED + "+1" +
                        SQL_WHERE_ID_IS,
                new Object[]{time, id}, table, id);
    }

    private static String appendWhereArtistId(String sql, Artist artist, boolean and) {
        return artist == null ? sql
                : sql + " " + (and ? "AND" : "WHERE") + " " + SQL_ARTIST_ID_IS;
    }

    private static SyncResult syncTable(Context context, Uri contentUri,
                                        SQLiteDatabase db, String table, String[] columns,
                                        int[] updateColumns, int[] insertColumns,
                                        int ignoreColumn, List<String> ignoreValues,
                                        int refIdColumn, List<Long> refIds,
                                        String timeColumn, long time) {
        Log.d(TAG, "DbHelper.syncTable(" + table + ")");
        SyncResult result = new SyncResult();

        // Insert/update rows from the MediaStore into the database.
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
                        result.rowsIgnored++;
                        continue;
                    }
                }

                // Ignore rows with a non-existing ref ID.
                if (refIdColumn != -1) {
                    long refId = c.getLong(refIdColumn);
                    if (!refIds.contains(refId)) {
                        Log.d(TAG, table + "." + columns[refIdColumn] + " value " +
                                refId + " ignored: " + id);
                        result.rowsIgnored++;
                        continue;
                    }
                }

                // Put update column values.
                ContentValues values = new ContentValues();
                putValues(c, columns, values, updateColumns);

                // Update or insert row if it doesn't exist.
                if (update(db, table, values, id, false) == 0) {
                    values.put(BaseColumns._ID, id);
                    putValues(c, columns, values, insertColumns);
                    if (timeColumn != null) {
                        values.put(timeColumn, time);
                    }
                    db.insert(table, null, values);
                    Log.d(TAG, table + " row inserted: " + id);
                    result.rowsInserted++;
                } else {
                    Log.d(TAG, table + " row updated: " + id);
                    result.rowsUpdated++;
                }
                result.ids.add(id);
            }
        }

        // Delete rows from the database that don't exist in the MediaStore.
        Log.d(TAG, "Deleting " + table + " rows");
        try (Cursor c = db.query(table, new String[]{BaseColumns._ID},
                null, null, null, null, null)) {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                if (!result.ids.contains(id)) {
                    delete(db, table, id);
                    result.rowsDeleted++;
                }
            }
        }

        Log.d(TAG, result.ids.size() + " " + table + " rows synchronized, " +
                result.rowsIgnored + " ignored, " + result.rowsInserted + " inserted, " +
                result.rowsUpdated + " updated, " + result.rowsDeleted + " deleted");
        return result;
    }

    private static void putValues(Cursor c, String[] columns,
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
                    throw new SQLiteException("Invalid type");
            }
        }
    }

    private static void putValue(Cursor c, int column, JSONObject obj, String key)
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
                throw new SQLiteException("Invalid type");
        }
    }

    private static void putValue(JSONObject obj, ContentValues values, String key)
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
                throw new SQLiteException("Invalid type");
            }
        } else {
            values.putNull(key);
        }
    }

    private static void updateArtistStats(SQLiteDatabase db, Song song) {
        Log.d(TAG, "DbHelper.updateArtistStats(" + song + ")");

        String sql = "UPDATE " + TABLE_ARTISTS + " SET " +
                Artist.LAST_ADDED +
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
            update(db, sql + SQL_WHERE_ID_IS, new Object[]{song.getArtistId()},
                    TABLE_ARTISTS, song.getArtistId());
        }
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

    private static void putValue(ContentValues values, String key, long value) {
        if (value == 0) {
            values.putNull(key);
        } else {
            values.put(key, value);
        }
    }

    private static int update(SQLiteDatabase db, String table, ContentValues values, long id,
                              boolean verify) {
        int rowsAffected = db.update(table, values, SQL_ID_IS, getWhereArgs(id));
        if (verify) {
            verifyUpdate(rowsAffected, table, id);
        }
        return rowsAffected;
    }

    private static void update(SQLiteDatabase db, String sql, Object[] bindArgs,
                               String table, long id) {
        db.execSQL(sql, bindArgs);
        verifyUpdate(queryInt(db, "SELECT changes()", null), table, id);
    }

    private static void delete(SQLiteDatabase db, String table, long id) {
        verifyUpdate(db.delete(table, SQL_ID_IS, getWhereArgs(id)), table, id);
    }

    private static void verifyUpdate(int rowsAffected, String table, long id) {
        switch (rowsAffected) {
            case 0:
                throw new SQLiteException(table + " row not found: " + id);
            case 1:
                Log.d(TAG, table + " row affected: " + id);
                break;
            default:
                throw new SQLiteException("Duplicate " + table + " row: " + id);
        }
    }

    private static void deletePlaylistSongs(SQLiteDatabase db, Playlist playlist) {
        Log.d(TAG, db.delete(TABLE_PLAYLIST_SONGS, Playlist.PLAYLIST_ID + "=?",
                getWhereArgs(playlist.getId())) + " playlist songs deleted");
    }

    interface ArtistColumns extends MediaStore.Audio.ArtistColumns {
        String LAST_ADDED = "last_added";
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

    public static class SyncResult {
        private List<Long> ids;
        private int rowsIgnored;

        private int rowsInserted;
        private int rowsUpdated;
        private int rowsDeleted;

        private SyncResult() {
            ids = new ArrayList<>();
        }

        public int getRowCount() {
            return ids.size();
        }

        public int getRowsIgnored() {
            return rowsIgnored;
        }

        public int getRowsInserted() {
            return rowsInserted;
        }

        public int getRowsUpdated() {
            return rowsUpdated;
        }

        public int getRowsDeleted() {
            return rowsDeleted;
        }
    }
}
