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
import java.util.Arrays;

public class DbHelper extends SQLiteOpenHelper {
    private static final String TAG = "UPlayer";

    private static final String DATABASE_NAME = "UPlayer.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_ARTISTS = "artists";
    private static final String TABLE_SONGS = "songs";
    private static final String TABLE_PLAYLISTS = "playlists";
    private static final String TABLE_PLAYLIST_SONGS = "playlist_songs";

    private static final String SQL_CREATE_ARTISTS =
            "CREATE TABLE " + TABLE_ARTISTS + "(" +
                    Artist._ID + " INTEGER PRIMARY KEY," +
                    Artist.ARTIST + " TEXT," +
                    Artist.LAST_ADDED + " INTEGER," +
                    Artist.LAST_PLAYED + " INTEGER," +
                    Artist.TIMES_PLAYED + " INTEGER DEFAULT 0)";

    private static final String SQL_CREATE_SONGS =
            "CREATE TABLE " + TABLE_SONGS + "(" +
                    Song._ID + " INTEGER PRIMARY KEY," +
                    Song.TITLE + " TEXT," +
                    Song.ARTIST_ID + " INTEGER," +
                    Song.ARTIST + " TEXT," +
                    Song.DURATION + " INTEGER," +
                    Song.YEAR + " INTEGER," +
                    Song.ADDED + " INTEGER," +
                    Song.TAG + " TEXT," +
                    Song.BOOKMARKED + " INTEGER," +
                    Song.LAST_PLAYED + " INTEGER," +
                    Song.TIMES_PLAYED + " INTEGER DEFAULT 0)";

    private static final String SQL_CREATE_PLAYLISTS =
            "CREATE TABLE " + TABLE_PLAYLISTS + "(" +
                    Playlist._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    Playlist.NAME + " TEXT," +
                    Playlist.SONG_INDEX + " INTEGER," +
                    Playlist.SONG_POSITION + " INTEGER," +
                    Playlist.LAST_PLAYED + " INTEGER)";

    private static final String SQL_CREATE_PLAYLIST_SONGS =
            "CREATE TABLE " + TABLE_PLAYLIST_SONGS + "(" +
                    Playlist._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    Playlist.PLAYLIST_ID + " INTEGER," +
                    Playlist.SONG_ID + " INTEGER)";

    private static final String SQL_ID_IS = BaseColumns._ID + "=?";

    private static final File BACKUP_FILE = Util.getMusicFile("UPlayer.json");

    private Context context;

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "DbHelper.onCreate()");
        db.execSQL(SQL_CREATE_ARTISTS);
        db.execSQL(SQL_CREATE_SONGS);
        db.execSQL(SQL_CREATE_PLAYLISTS);
        db.execSQL(SQL_CREATE_PLAYLIST_SONGS);

        // Insert the default playlist.
        ContentValues values = new ContentValues();
        values.put(Playlist._ID, Playlist.DEFAULT_PLAYLIST_ID);
        values.put(Playlist.NAME, context.getString(R.string.default_playlist));
        db.insert(TABLE_PLAYLISTS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public ArrayList<Artist> queryArtists(String orderBy) {
        Log.d(TAG, "DbHelper.queryArtists(" + orderBy + ")");
        ArrayList<Artist> artists = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_ARTISTS, null, null, null, null, null, orderBy)) {
                while (c.moveToNext()) {
                    Artist artist = new Artist();
                    artist.setId(c.getLong(0));
                    artist.setArtist(c.getString(1));
                    artist.setLastAdded(c.getLong(2));
                    artist.setLastPlayed(c.getLong(3));
                    artist.setTimesPlayed(c.getInt(4));
                    artists.add(artist);
                }
            }
        }
        Log.d(TAG, artists.size() + " artists queried");
        return artists;
    }

    public ArrayList<Song> querySongs(String selection, String[] selectionArgs, String orderBy) {
        Log.d(TAG, "DbHelper.querySongs(" + selection + ", " + Arrays.toString(selectionArgs) +
                ", " + orderBy + ")");
        ArrayList<Song> songs = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_SONGS, null, selection, selectionArgs,
                    null, null, orderBy)) {
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
            }
        }
        Log.d(TAG, songs.size() + " songs queried");
        return songs;
    }

    public void querySong(Song song) {
        Log.d(TAG, "DbHelper.querySong(" + song.getId() + ":" + song + ")");
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_SONGS,
                    new String[]{
                            Song.YEAR,
                            Song.ADDED,
                            Song.TAG,
                            Song.BOOKMARKED,
                            Song.LAST_PLAYED,
                            Song.TIMES_PLAYED
                    },
                    SQL_ID_IS, getWhereArgs(song.getId()), null, null, null)) {
                c.moveToFirst();
                song.setYear(c.getInt(0));
                song.setAdded(c.getLong(1));
                song.setTag(c.getString(2));
                song.setBookmarked(c.getLong(3));
                song.setLastPlayed(c.getLong(4));
                song.setTimesPlayed(c.getInt(5));
            }
        }
    }

    public ArrayList<String> querySongTags(String selection, String[] selectionArgs) {
        Log.d(TAG, "DbHelper.querySongTags(" + selection + ", " +
                Arrays.toString(selectionArgs) + ")");
        ArrayList<String> tags = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(true, TABLE_SONGS, new String[]{Song.TAG},
                    concatSelection(selection, Song.TAG + " IS NOT NULL"), selectionArgs,
                    null, null, Song.TAG, null)) {
                while (c.moveToNext()) {
                    tags.add(c.getString(0));
                }
            }
        }
        Log.d(TAG, tags.size() + " song tags queried");
        return tags;
    }

    public void updateSong(Song song) {
        Log.d(TAG, "DbHelper.updateSong(" + song.getId() + ":" + song + ")");
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
        Log.d(TAG, "DbHelper.bookmarkSong(" + song.getId() + ":" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                update(db, "UPDATE " + TABLE_SONGS + " SET " +
                                Song.BOOKMARKED + "=CASE WHEN " +
                                Song.BOOKMARKED + " IS NULL THEN ? ELSE NULL END",
                        new Object[]{Calendar.currentTime(), song.getId()},
                        TABLE_SONGS, song.getId());
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            song.setBookmarked(queryLong(db, TABLE_SONGS, Song.BOOKMARKED,
                    SQL_ID_IS, getWhereArgs(song.getId())));
        }
    }

    public void updateSongPlayed(Song song) {
        Log.d(TAG, "DbHelper.updateSongPlayed(" + song.getId() + ":" + song + ")");
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
            song.setTimesPlayed(queryInt(db, TABLE_SONGS, Song.TIMES_PLAYED,
                    SQL_ID_IS, getWhereArgs(song.getId())));
        }
    }

    public void deleteSong(Song song) {
        Log.d(TAG, "DbHelper.deleteSong(" + song.getId() + ":" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                delete(db, TABLE_SONGS, song.getId());

                // Check if artist has songs.
                if (queryInt(db, TABLE_SONGS, "COUNT(*)",
                        Song.ARTIST_ID + "=?", getWhereArgs(song.getArtistId())) > 0) {
                    updateArtistStats(db, song);
                } else {
                    Log.d(TAG, "Deleting artist " + song.getArtistId() + ":" + song.getArtist());
                    delete(db, TABLE_ARTISTS, song.getArtistId());
                }

                deleteOrphanedPlaylistSongs(db, song);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public ArrayList<Playlist> queryPlaylists(Song song) {
        return queryPlaylists(Song._ID + "=?", getWhereArgs(song.getId()));
    }

    public ArrayList<Playlist> queryPlaylists(String songsSelection, String[] selectionArgs) {
        Log.d(TAG, "DbHelper.queryPlaylists(" + songsSelection + ", " +
                Arrays.toString(selectionArgs) + ")");
        String selection;
        String countQuery = "(SELECT COUNT(*) FROM " + TABLE_PLAYLIST_SONGS +
                " WHERE " + Playlist.PLAYLIST_ID + "=" +
                TABLE_PLAYLISTS + "." + Playlist._ID;
        if (songsSelection == null) {
            selection = null;
        } else {
            String playlistSongsSelection = Playlist.SONG_ID + " IN(SELECT " +
                    TABLE_SONGS + "." + Song._ID + " FROM " + TABLE_SONGS +
                    " WHERE " + songsSelection + ")";
            selection = TABLE_PLAYLISTS + "." + Playlist._ID + " IN(SELECT " +
                    Playlist.PLAYLIST_ID + " FROM " + TABLE_PLAYLIST_SONGS + " WHERE " +
                    playlistSongsSelection + ")";
            Log.d(TAG, "selection=" + selection);
            countQuery = concatSelection(countQuery, playlistSongsSelection);
            selectionArgs = concatWhereArgs(selectionArgs, selectionArgs);
        }
        countQuery += ")";
        Log.d(TAG, "countQuery=" + countQuery);

        ArrayList<Playlist> playlists = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_PLAYLISTS,
                    new String[]{
                            Playlist._ID,
                            Playlist.NAME,
                            Playlist.SONG_INDEX,
                            Playlist.SONG_POSITION,
                            Playlist.LAST_PLAYED,
                            countQuery
                    },
                    selection, selectionArgs, null, null,
                    Playlist.LAST_PLAYED + " DESC," + Playlist.NAME)) {
                while (c.moveToNext()) {
                    Playlist playlist = new Playlist();
                    playlist.setId(c.getLong(0));
                    playlist.setName(c.getString(1));
                    playlist.setSongIndex(c.getInt(2));
                    playlist.setSongPosition(c.getInt(3));
                    playlist.setLastPlayed(c.getLong(4));
                    playlist.setSongCount(c.getInt(5));
                    if (playlist.isDefault()) {
                        playlists.add(0, playlist);
                    } else {
                        playlists.add(playlist);
                    }
                }
            }
        }
        Log.d(TAG, playlists.size() + " playlists queried");
        return playlists;
    }

    public void insertOrUpdatePlaylist(Playlist playlist, ArrayList<Song> songs) {
        Log.d(TAG, "DbHelper.insertOrUpdatePlaylist(" + playlist.getId() + ":" + playlist + ", " +
                (songs == null ? "null" : songs.size()) + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                ContentValues values;

                // Insert or update playlist.
                values = new ContentValues();
                if (songs == null) {
                    values.put(Playlist.NAME, playlist.getName());
                } else {
                    putValue(values, Playlist.SONG_INDEX, playlist.getSongIndex());
                    putValue(values, Playlist.SONG_POSITION, playlist.getSongPosition());
                    putValue(values, Playlist.LAST_PLAYED, playlist.getLastPlayed());
                }
                if (playlist.getId() == 0) {
                    playlist.setId(db.insert(TABLE_PLAYLISTS, null, values));
                    Log.d(TAG, "Playlist inserted: " + playlist.getId());
                } else {
                    update(db, TABLE_PLAYLISTS, values, playlist.getId(), true);
                }

                // Insert playlist songs, deleting any existing ones.
                if (songs != null) {
                    deletePlaylistSongs(db, playlist);
                    for (Song song : songs) {
                        insertPlaylistSong(db, playlist, song);
                    }
                    Log.d(TAG, songs.size() + " playlist songs inserted");
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void deletePlaylist(Playlist playlist) {
        Log.d(TAG, "DbHelper.deletePlaylist(" + playlist.getId() + ":" + playlist + ")");
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

    public ArrayList<Song> queryPlaylistSongs(Playlist playlist) {
        Log.d(TAG, "DbHelper.queryPlaylistSongs(" + playlist.getId() + ":" + playlist + ")");
        ArrayList<Song> songs = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase()) {
            try (Cursor c = db.query(TABLE_PLAYLIST_SONGS + "," + TABLE_SONGS,
                    new String[]{
                            Playlist.SONG_ID,
                            Song.TITLE,
                            Song.ARTIST_ID,
                            Song.ARTIST,
                            Song.DURATION,
                            Song.TIMES_PLAYED
                    },
                    Playlist.SONG_ID + "=" + TABLE_SONGS + "." + Song._ID +
                            " AND " + Playlist.PLAYLIST_ID + "=?",
                    getWhereArgs(playlist.getId()), null, null,
                    TABLE_PLAYLIST_SONGS + "." + Playlist._ID)) {
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
            }
        }
        Log.d(TAG, songs.size() + " playlist songs queried");
        return songs;
    }

    public void insertPlaylistSong(Playlist playlist, Song song) {
        Log.d(TAG, "DbHelper.insertPlaylistSong(" + playlist.getId() + ":" + playlist + ", " +
                song.getId() + ":" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                Log.d(TAG, "Playlist song inserted: " + insertPlaylistSong(db, playlist, song));
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public boolean deletePlaylistSong(Playlist playlist, Song song) {
        Log.d(TAG, "DbHelper.deletePlaylistSong(" + playlist.getId() + ":" + playlist + ", " +
                song.getId() + ":" + song + ")");
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                int rowsAffected = db.delete(TABLE_PLAYLIST_SONGS,
                        Playlist.PLAYLIST_ID + "=? AND " + Playlist.SONG_ID + "=?",
                        new String[]{Long.toString(playlist.getId()), Long.toString(song.getId())});
                Log.d(TAG, rowsAffected + " playlist songs deleted");
                db.setTransactionSuccessful();
                return rowsAffected > 0;
            } finally {
                db.endTransaction();
            }
        }
    }

    public Stats queryStats(long artistId) {
        return queryStats(false, true, true, true,
                Song.ARTIST_ID + "=?", getWhereArgs(artistId));
    }

    public Stats queryStats(boolean artist, boolean bookmarked, boolean tagged, boolean playlisted,
                            String selection, String[] selectionArgs) {
        Log.d(TAG, "DbHelper.queryStats(" + artist + ", " + bookmarked + ", " + tagged + ", " +
                playlisted + ", " + selection + ", " + Arrays.toString(selectionArgs) + ")");
        Stats stats = new Stats();
        try (SQLiteDatabase db = getReadableDatabase()) {
            queryTotal(db, stats.getTotal(), artist, selection, selectionArgs);
            queryTotal(db, stats.getPlayed(), artist,
                    concatSelection(selection, Song.LAST_PLAYED + " IS NOT NULL"),
                    selectionArgs);
            if (bookmarked) {
                queryTotal(db, stats.getBookmarked(), artist,
                        concatSelection(selection, Song.BOOKMARKED + " IS NOT NULL"),
                        selectionArgs);
            }
            if (tagged) {
                queryTotal(db, stats.getTagged(), artist,
                        concatSelection(selection, Song.TAG + " IS NOT NULL"),
                        selectionArgs);
            }
            if (playlisted) {
                queryTotal(db, stats.getPlaylisted(), artist, concatSelection(selection,
                        TABLE_SONGS + "." + Song._ID + " IN(SELECT " + Playlist.SONG_ID + " FROM " +
                                TABLE_PLAYLIST_SONGS + " WHERE " + Playlist.PLAYLIST_ID + " != " +
                                Playlist.DEFAULT_PLAYLIST_ID + ")"),
                        selectionArgs);
            }

            try (Cursor c = db.query(TABLE_SONGS,
                    new String[]{
                            "MAX(" + Song.ADDED + ")",
                            "MAX(" + Song.LAST_PLAYED + ")",
                            "SUM(" + Song.TIMES_PLAYED + ")",
                            "SUM(" + Song.DURATION + "*" + Song.TIMES_PLAYED + ")"
                    },
                    selection, selectionArgs, null, null, null)) {
                c.moveToFirst();
                stats.setLastAdded(c.getLong(0));
                stats.setLastPlayed(c.getLong(1));
                stats.setTimesPlayed(c.getInt(2));
                stats.setPlayedDuration(c.getLong(3));
            }
        }
        return stats;
    }

    public SyncResult[] syncWithMediaStore(Context context) {
        Log.d(TAG, "DbHelper.syncWithMediaStore()");

        // Sync artists and songs tables.
        SyncResult[] results = new SyncResult[2];
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.beginTransaction();
            try {
                long time = Calendar.currentTime();

                results[0] = syncTable(context, MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                        db, TABLE_ARTISTS,
                        new String[]{
                                Artist._ID,
                                Artist.ARTIST
                        },
                        null, new int[0], null, null, -1);

                results[1] = syncTable(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        db, TABLE_SONGS,
                        new String[]{
                                Song._ID,
                                Song.TITLE,
                                Song.ARTIST_ID,
                                Song.ARTIST,
                                Song.DURATION,
                                Song.YEAR
                        },
                        new int[]{1, 2, 3, 4}, new int[]{5},
                        Song.IS_MUSIC + "=1", Song.ADDED, time);

                // Update artists when songs have been inserted or deleted.
                if (results[1].rowsInserted > 0 || results[1].rowsDeleted > 0) {
                    int deleted = db.delete(TABLE_ARTISTS, Artist._ID + " NOT IN (SELECT " +
                            Song.ARTIST_ID + " FROM " + TABLE_SONGS + ")", null);
                    Log.d(TAG, "Deleted " + deleted + " artists with 0 songs");
                    results[0].rowCount -= deleted;
                    results[0].rowsInserted -= deleted;
                    updateArtistStats(db, null);
                }

                // Delete orphaned playlist songs when songs have been deleted.
                if (results[1].rowsDeleted > 0) {
                    deleteOrphanedPlaylistSongs(db, null);
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

        try (SQLiteDatabase db = getReadableDatabase()) {
            backupTable(backup, db, TABLE_SONGS,
                    new String[]{
                            Song._ID,
                            Song.TITLE,
                            Song.ARTIST,
                            Song.YEAR,
                            Song.ADDED,
                            Song.TAG,
                            Song.BOOKMARKED,
                            Song.LAST_PLAYED,
                            Song.TIMES_PLAYED
                    });

            backupTable(backup, db, TABLE_PLAYLISTS,
                    new String[]{
                            Playlist._ID,
                            Playlist.NAME,
                            Playlist.SONG_INDEX,
                            Playlist.SONG_POSITION,
                            Playlist.LAST_PLAYED,
                    });

            backupTable(backup, db, TABLE_PLAYLIST_SONGS,
                    new String[]{
                            Playlist._ID,
                            Playlist.PLAYLIST_ID,
                            Playlist.SONG_ID
                    });
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
                LongSparseArray<Long> songIds = new LongSparseArray<>();
                for (int i = 0; i < songs.length(); i++) {
                    JSONObject song = songs.getJSONObject(i);

                    // Query the current ID of the row by artist and title.
                    long id;
                    try (Cursor c = db.query(TABLE_SONGS, new String[]{Song._ID},
                            Song.TITLE + " LIKE ? AND " + Song.ARTIST + " LIKE ?",
                            new String[]{
                                    song.getString(Song.TITLE),
                                    song.getString(Song.ARTIST)
                            },
                            null, null, null)) {
                        if (c.moveToFirst()) {
                            id = c.getLong(0);
                            if (c.moveToNext()) {
                                throw new SQLiteException("Duplicate song: '" +
                                        song.getString(Song.ARTIST) + " - " +
                                        song.getString(Song.TITLE) + "'");
                            }
                        } else {
                            throw new SQLiteException("Song not found: '" +
                                    song.getString(Song.ARTIST) + " - " +
                                    song.getString(Song.TITLE) + "'");
                        }
                    }
                    songIds.put(song.getLong(Song._ID), id);

                    // Get ContentValues from JSONObject and update row.
                    ContentValues values = getValues(song,
                            new String[]{
                                    Song.YEAR,
                                    Song.ADDED,
                                    Song.TAG,
                                    Song.BOOKMARKED,
                                    Song.LAST_PLAYED,
                                    Song.TIMES_PLAYED
                            });
                    update(db, TABLE_SONGS, values, id, false);
                }
                Log.d(TAG, songs.length() + " songs restored");
                updateArtistStats(db, null);

                restoreTable(backup, db, TABLE_PLAYLISTS, SQL_CREATE_PLAYLISTS,
                        new String[]{
                                Playlist._ID,
                                Playlist.NAME,
                                Playlist.SONG_INDEX,
                                Playlist.SONG_POSITION,
                                Playlist.LAST_PLAYED,
                        },
                        null, null);

                restoreTable(backup, db, TABLE_PLAYLIST_SONGS, SQL_CREATE_PLAYLIST_SONGS,
                        new String[]{
                                Playlist._ID,
                                Playlist.PLAYLIST_ID
                        },
                        Playlist.SONG_ID, songIds);

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public static String getMinSelection(String column) {
        return column + ">=?";
    }

    public static String getMaxSelection(String column, boolean hasMin) {
        String s = "<=?";
        return hasMin ? column + s : getNullOrSelection(column, s);
    }

    public static String getNullOrSelection(String column, String s) {
        return "(" + column + " IS NULL OR " + column + s + ")";
    }

    public static String getInClause(int count) {
        StringBuilder sb = new StringBuilder("IN(");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('?');
        }
        sb.append(')');
        return sb.toString();
    }

    public static String getPlaylistSongsInClause(int count, boolean not) {
        String s = "IN(SELECT " + Playlist.SONG_ID + " FROM " + TABLE_PLAYLIST_SONGS +
                " WHERE " + Playlist.PLAYLIST_ID + " " + getInClause(count) + ")";
        if (not) {
            s = "NOT " + s;
        }
        return TABLE_SONGS + "." + Song._ID + " " + s;
    }

    public static String concatSelection(String a, String b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            return a + " AND " + b;
        }
    }

    public static String[] getWhereArgs(long id) {
        return new String[]{Long.toString(id)};
    }

    public static String[] concatWhereArgs(String[] a, String[] b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            String[] selectionArgs = new String[a.length + b.length];
            System.arraycopy(a, 0, selectionArgs, 0, a.length);
            System.arraycopy(b, 0, selectionArgs, a.length, b.length);
            return selectionArgs;
        }
    }

    private static void updatePlayed(SQLiteDatabase db, String table, long time, long id) {
        update(db, "UPDATE " + table + " SET " +
                        PlayedColumns.LAST_PLAYED + "=?," +
                        PlayedColumns.TIMES_PLAYED + "=" + PlayedColumns.TIMES_PLAYED + "+1",
                new Object[]{time, id}, table, id);
    }

    private static long insertPlaylistSong(SQLiteDatabase db, Playlist playlist, Song song) {
        ContentValues values = new ContentValues();
        values.put(Playlist.PLAYLIST_ID, playlist.getId());
        values.put(Playlist.SONG_ID, song.getId());
        return db.insert(TABLE_PLAYLIST_SONGS, null, values);
    }

    private static void deletePlaylistSongs(SQLiteDatabase db, Playlist playlist) {
        Log.d(TAG, db.delete(TABLE_PLAYLIST_SONGS, Playlist.PLAYLIST_ID + "=?",
                getWhereArgs(playlist.getId())) + " playlist songs deleted");
    }

    private static void deleteOrphanedPlaylistSongs(SQLiteDatabase db, Song song) {
        String whereClause;
        String[] whereArgs;
        if (song == null) {
            whereClause = Playlist.SONG_ID + " NOT IN(SELECT " +
                    TABLE_SONGS + "." + Song._ID + " FROM " + TABLE_SONGS + ")";
            whereArgs = null;
        } else {
            whereClause = Playlist.SONG_ID + "=?";
            whereArgs = getWhereArgs(song.getId());
        }
        Log.d(TAG, db.delete(TABLE_PLAYLIST_SONGS, whereClause, whereArgs) +
                " orphaned playlist songs deleted");
    }

    private static void queryTotal(SQLiteDatabase db, Stats.Total total, boolean artist,
                                   String selection, String[] selectionArgs) {
        Log.d(TAG, "queryTotal(" + artist + ", " + selection + ", " +
                Arrays.toString(selectionArgs) + ")");
        try (Cursor c = db.query(TABLE_SONGS,
                new String[]{
                        "COUNT(*)",
                        "SUM(" + Song.DURATION + ")"
                },
                selection, selectionArgs, null, null, null)) {
            c.moveToFirst();
            total.setSongCount(c.getInt(0));
            total.setSongsDuration(c.getLong(1));
        }

        if (artist) {
            total.setArtistCount(queryInt(db, TABLE_SONGS,
                    "COUNT(DISTINCT " + Song.ARTIST_ID + ")",
                    selection, selectionArgs));
        }
    }

    private static SyncResult syncTable(Context context, Uri uri, SQLiteDatabase db, String table,
                                        String[] columns, int[] updateColumns, int[] insertColumns,
                                        String selection, String timeColumn, long time) {
        Log.d(TAG, "DbHelper.syncTable(" + uri + ", " + table + ", " + Arrays.toString(columns) +
                ", " + Arrays.toString(updateColumns) + ", " + Arrays.toString(insertColumns) +
                ", " + selection + ", " + timeColumn + ", " + time + ")");
        SyncResult result = new SyncResult();
        ArrayList<Long> ids = new ArrayList<>();

        // Insert/update rows from the MediaStore into the database.
        try (Cursor c = context.getContentResolver().query(uri, columns, selection, null, null)) {
            if (c == null) {
                throw new RuntimeException("No MediaStore cursor");
            }

            while (c.moveToNext()) {
                long id = c.getLong(0);

                // Put update column values.
                ContentValues values = new ContentValues();
                putValues(values, c, columns, updateColumns);

                // Update or insert row if it doesn't exist.
                if (update(db, table, values, id, false) == 0) {
                    values.put(BaseColumns._ID, id);
                    putValues(values, c, columns, insertColumns);
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
                ids.add(id);
            }
        }

        // Delete rows from the database that don't exist in the MediaStore.
        Log.d(TAG, "Deleting " + table + " rows");
        try (Cursor c = db.query(table, new String[]{BaseColumns._ID},
                null, null, null, null, null)) {
            while (c.moveToNext()) {
                long id = c.getLong(0);
                if (!ids.contains(id)) {
                    delete(db, table, id);
                    result.rowsDeleted++;
                }
            }
        }

        result.rowCount = ids.size();
        Log.d(TAG, result.rowCount + " " + table + " rows synchronized, " +
                result.rowsInserted + " inserted, " + result.rowsUpdated + " updated, " +
                result.rowsDeleted + " deleted");
        return result;
    }

    private static void putValues(ContentValues values, Cursor c,
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
                    throw new SQLiteException("Invalid type");
            }
        }
    }

    private static void backupTable(JSONObject obj, SQLiteDatabase db,
                                    String table, String[] columns) throws JSONException {
        Log.d(TAG, "DbHelper.backupTable(" + table + ", " + Arrays.toString(columns) + ")");
        JSONArray rows = new JSONArray();
        try (Cursor c = db.query(table, columns, null, null, null, null, null)) {
            while (c.moveToNext()) {
                JSONObject row = new JSONObject();
                for (int i = 0; i < columns.length; i++) {
                    switch (c.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL:
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            row.put(columns[i], c.getLong(i));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            row.put(columns[i], c.getString(i));
                            break;
                        default:
                            throw new SQLiteException("Invalid type");
                    }
                }
                rows.put(row);
            }
        }
        obj.put(table, rows);
        Log.d(TAG, rows.length() + " " + table + " rows backed up");
    }

    private static void restoreTable(JSONObject obj, SQLiteDatabase db,
                                     String table, String sqlCreate, String[] columns,
                                     String refIdColumn, LongSparseArray<Long> refIds)
            throws JSONException {
        Log.d(TAG, "DbHelper.restoreTable(" + table + ", " +
                Arrays.toString(columns) + ", " + refIdColumn + ")");

        // Recreate the table.
        db.execSQL("DROP TABLE " + table);
        db.execSQL(sqlCreate);

        // Insert rows from JSONArray.
        JSONArray rows = obj.getJSONArray(table);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            ContentValues values = getValues(row, columns);
            if (refIdColumn != null) {
                long rowRefId = row.getLong(refIdColumn);
                Long refId = refIds.get(rowRefId);
                if (refId == null) {
                    throw new JSONException(table + "." + refIdColumn + " not found: " + rowRefId);
                }
                values.put(refIdColumn, refId);
            }
            db.insert(table, null, values);
        }
        Log.d(TAG, rows.length() + " " + table + " rows restored");
    }

    private static ContentValues getValues(JSONObject obj, String[] keys) throws JSONException {
        ContentValues values = new ContentValues();
        for (String key : keys) {
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
        return values;
    }

    private static void updateArtistStats(SQLiteDatabase db, Song song) {
        Log.d(TAG, "DbHelper.updateArtistStats(" +
                (song == null ? "null" : song.getId() + ":" + song) + ")");

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
            update(db, sql, new Object[]{song.getArtistId()}, TABLE_ARTISTS, song.getArtistId());
        }
    }

    private static int queryInt(SQLiteDatabase db, String table, String column,
                                String selection, String[] selectionArgs) {
        try (Cursor c = db.query(table, new String[]{column},
                selection, selectionArgs, null, null, null)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    private static long queryLong(SQLiteDatabase db, String table, String column,
                                String selection, String[] selectionArgs) {
        try (Cursor c = db.query(table, new String[]{column},
                selection, selectionArgs, null, null, null)) {
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

    private static int update(SQLiteDatabase db, String table, ContentValues values,
                              long id, boolean verify) {
        int rowsAffected = db.update(table, values, SQL_ID_IS, getWhereArgs(id));
        if (verify) {
            verifyUpdate(rowsAffected, table, id);
        }
        return rowsAffected;
    }

    private static void update(SQLiteDatabase db, String sql, Object[] bindArgs,
                               String table, long id) {
        db.execSQL(sql + " WHERE " + SQL_ID_IS, bindArgs);
        try (Cursor c = db.rawQuery("SELECT changes()", null)) {
            c.moveToFirst();
            verifyUpdate(c.getInt(0), table, id);
        }
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
        private int rowCount;
        private int rowsInserted;
        private int rowsUpdated;
        private int rowsDeleted;

        private SyncResult() {
        }

        public int getRowCount() {
            return rowCount;
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
