package com.oneup.uplayer.fragment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.oneup.uplayer.R;
import com.oneup.uplayer.Util;
import com.oneup.uplayer.activity.DatePickerActivity;
import com.oneup.uplayer.activity.MainActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;

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

public class QueryFragment extends Fragment implements BaseArgs, AdapterView.OnItemSelectedListener,
        View.OnClickListener {
    private static final String TAG = "UPlayer";

    private static final String SQL_QUERY_SONGS_PLAYED =
            "SELECT COUNT(" + Song.TIMES_PLAYED + ") FROM " + Song.TABLE_NAME;

    private static final String SQL_QUERY_TAGGED_SONG_COUNT =
            "SELECT COUNT(" + Song.TAG + ") FROM " + Song.TABLE_NAME;

    private static final String SQL_QUERY_ARTISTS_PLAYED =
            "SELECT COUNT(" + Artist.TIMES_PLAYED + ") FROM " + Artist.TABLE_NAME;

    private static final String SQL_QUERY_TOTAL_SONGS_PLAYED =
            "SELECT SUM(" + Artist.TIMES_PLAYED + ") FROM " + Artist.TABLE_NAME;

    private static final String SQL_QUERY_TOTAL_DURATION =
            "SELECT SUM(" + Song.DURATION + "*" + Song.TIMES_PLAYED + ") FROM " + Song.TABLE_NAME;

    private static final String KEY_JOINED_SORT_BY = "joinedSortBy";
    private static final String KEY_TITLE = "title";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_MIN_YEAR = "minYear";
    private static final String KEY_MAX_YEAR = "maxYear";
    private static final String KEY_MIN_LAST_PLAYED = "minLastPlayed";
    private static final String KEY_MAX_LAST_PLAYED = "maxLastPlayed";
    private static final String KEY_MIN_TIMES_PLAYED = "minTimesPlayed";
    private static final String KEY_MAX_TIMES_PLAYED = "maxTimesPlayed";
    private static final String KEY_DB_ORDER_BY = "dbOrderBy";
    private static final String KEY_DB_ORDER_BY_DESC = "dbOrderByDesc";

    private static final int REQUEST_SELECT_MIN_LAST_PLAYED = 1;
    private static final int REQUEST_SELECT_MAX_LAST_PLAYED = 2;

    private SparseArray<Artist> artists;

    private DbOpenHelper dbOpenHelper;
    private SharedPreferences preferences;

    private TextView tvTotals;
    private Spinner sJoinedSortBy;
    private EditText etTitle;
    private EditText etArtist;
    private EditText etMinYear;
    private EditText etMaxYear;
    private LinearLayout llLastPlayed;
    private Button bMinLastPlayed;
    private Button bMaxLastPlayed;
    private LinearLayout llTimesPlayed;
    private EditText etMinTimesPlayed;
    private EditText etMaxTimesPlayed;
    private LinearLayout llDbOrderBy;
    private Spinner sDbOrderBy;
    private CheckBox cbDbOrderByDesc;
    private Button bQuery;
    private Button bTags;
    private Button bBackup;
    private Button bRestoreBackup;

    private long minLastPlayed;
    private long maxLastPlayed;

    public QueryFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "QueryFragment.onCreateView()");
        artists = getArguments().getSparseParcelableArray(ARG_ARTISTS);

        dbOpenHelper = new DbOpenHelper(getActivity());
        int songsPlayed, taggedSongCount, artistsPlayed, totalPlayed, totalDuration;
        try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
            songsPlayed = DbOpenHelper.queryInt(db, SQL_QUERY_SONGS_PLAYED);
            taggedSongCount = DbOpenHelper.queryInt(db, SQL_QUERY_TAGGED_SONG_COUNT);
            artistsPlayed = DbOpenHelper.queryInt(db, SQL_QUERY_ARTISTS_PLAYED);
            totalPlayed = DbOpenHelper.queryInt(db, SQL_QUERY_TOTAL_SONGS_PLAYED);
            totalDuration = DbOpenHelper.queryInt(db, SQL_QUERY_TOTAL_DURATION);
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        View ret = inflater.inflate(R.layout.fragment_query, container, false);

        tvTotals = ret.findViewById(R.id.tvTotals);
        tvTotals.setText(getString(R.string.totals, songsPlayed, taggedSongCount, artistsPlayed,
                totalPlayed, Util.formatDuration(totalDuration)));

        sJoinedSortBy = ret.findViewById(R.id.sJoinedSortBy);
        sJoinedSortBy.setOnItemSelectedListener(this);
        setSpinnerSelection(KEY_JOINED_SORT_BY, sJoinedSortBy);

        etTitle = ret.findViewById(R.id.etTitle);
        setEditTextText(KEY_TITLE, etTitle);

        etArtist = ret.findViewById(R.id.etArtist);
        setEditTextText(KEY_ARTIST, etArtist);

        etMinYear = ret.findViewById(R.id.etMinYear);
        setEditTextText(KEY_MIN_YEAR, etMinYear);

        etMaxYear = ret.findViewById(R.id.etMaxYear);
        setEditTextText(KEY_MAX_YEAR, etMaxYear);

        llLastPlayed = ret.findViewById(R.id.llLastPlayed);

        bMinLastPlayed = ret.findViewById(R.id.bMinLastPlayed);
        bMinLastPlayed.setOnClickListener(this);
        if (minLastPlayed == 0) {
            minLastPlayed = preferences.getLong(KEY_MIN_LAST_PLAYED, 0);
        }
        if (minLastPlayed > 0) {
            bMinLastPlayed.setText(Util.formatDate(minLastPlayed));
        }

        bMaxLastPlayed = ret.findViewById(R.id.bMaxLastPlayed);
        bMaxLastPlayed.setOnClickListener(this);
        if (maxLastPlayed == 0) {
            maxLastPlayed = preferences.getLong(KEY_MAX_LAST_PLAYED, 0);
        }
        if (maxLastPlayed > 0) {
            bMaxLastPlayed.setText(Util.formatDate(maxLastPlayed));
        }

        llTimesPlayed = ret.findViewById(R.id.llTimesPlayed);

        etMinTimesPlayed = ret.findViewById(R.id.etMinTimesPlayed);
        setEditTextText(KEY_MIN_TIMES_PLAYED, etMinTimesPlayed);

        etMaxTimesPlayed = ret.findViewById(R.id.etMaxTimesPlayed);
        setEditTextText(KEY_MAX_TIMES_PLAYED, etMaxTimesPlayed);

        llDbOrderBy = ret.findViewById(R.id.llDbOrderBy);

        sDbOrderBy = ret.findViewById(R.id.sDbOrderBy);
        setSpinnerSelection(KEY_DB_ORDER_BY, sDbOrderBy);

        cbDbOrderByDesc = ret.findViewById(R.id.cbDbOrderByDesc);
        setCheckBoxChecked(KEY_DB_ORDER_BY_DESC, cbDbOrderByDesc);

        bQuery = ret.findViewById(R.id.bQuery);
        bQuery.setOnClickListener(this);

        bTags = ret.findViewById(R.id.bTags);
        bTags.setOnClickListener(this);

        bBackup = ret.findViewById(R.id.bBackup);
        bBackup.setOnClickListener(this);

        bRestoreBackup = ret.findViewById(R.id.bRestoreBackup);
        bRestoreBackup.setOnClickListener(this);

        return ret;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_MIN_LAST_PLAYED:
                    minLastPlayed = data.getLongExtra(DatePickerActivity.EXTRA_DATE, 0);
                    bMinLastPlayed.setText(Util.formatDate(minLastPlayed));
                    break;
                case REQUEST_SELECT_MAX_LAST_PLAYED:
                    maxLastPlayed = data.getLongExtra(DatePickerActivity.EXTRA_DATE, 0);
                    bMaxLastPlayed.setText(Util.formatDate(maxLastPlayed));
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "QueryFragment.onDestroy()");
        if (dbOpenHelper != null) {
            dbOpenHelper.close();
        }

        super.onDestroy();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == sJoinedSortBy) {
            int dbVisibility = position == 0 ? View.VISIBLE : View.GONE;
            etArtist.setVisibility(dbVisibility);
            llLastPlayed.setVisibility(dbVisibility);
            llTimesPlayed.setVisibility(dbVisibility);
            llDbOrderBy.setVisibility(dbVisibility);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parentw) {
    }

    @Override
    public void onClick(View v) {
        if (v == bMinLastPlayed) {
            startActivityForResult(new Intent(getContext(), DatePickerActivity.class)
                            .putExtra(DatePickerActivity.EXTRA_TITLE, R.string.min_last_played)
                            .putExtra(DatePickerActivity.EXTRA_DATE, minLastPlayed),
                    REQUEST_SELECT_MIN_LAST_PLAYED);
        } else if (v == bMaxLastPlayed) {
            startActivityForResult(new Intent(getContext(), DatePickerActivity.class)
                            .putExtra(DatePickerActivity.EXTRA_TITLE, R.string.max_last_played)
                            .putExtra(DatePickerActivity.EXTRA_DATE, maxLastPlayed),
                    REQUEST_SELECT_MAX_LAST_PLAYED);
        } else if (v == bQuery) {
            int joinedSortBy = sJoinedSortBy.getSelectedItemPosition();
            String title = etTitle.getText().toString();
            String artist = etArtist.getText().toString();
            String minYear = etMinYear.getText().toString();
            String maxYear = etMaxYear.getText().toString();
            String minTimesPlayed = etMinTimesPlayed.getText().toString();
            String maxTimesPlayed = etMaxTimesPlayed.getText().toString();
            int dbOrderByColumn = sDbOrderBy.getSelectedItemPosition();
            boolean dbOrderByDesc = cbDbOrderByDesc.isChecked();

            String selection = null, dbOrderBy = null;

            if (title.length() > 0) {
                selection = Song.TITLE + " LIKE '%" + title + "%'";
            }
            if (minYear.length() > 0) {
                selection = appendSelection(selection, Song.YEAR + ">=" + minYear);
            }
            if (maxYear.length() > 0) {
                selection = appendSelection(selection, Song.YEAR + "<=" + maxYear);
            }

            if (joinedSortBy == 0) {
                if (artist.length() > 0) {
                    selection = appendSelection(selection, Song.ARTIST_ID +
                            " IN(SELECT " + Artist._ID + " FROM " + Artist.TABLE_NAME +
                            " WHERE " + Artist.ARTIST + " LIKE '%" + artist + "%')");
                }
                if (minLastPlayed > 0) {
                    selection = appendSelection(selection, Song.LAST_PLAYED + ">=" + minLastPlayed);
                }
                if (maxLastPlayed > 0) {
                    selection = appendSelection(selection, Song.LAST_PLAYED + "<=" +
                            maxLastPlayed + 86400000);
                }
                if (minTimesPlayed.length() > 0) {
                    selection = appendSelection(selection,
                            Song.TIMES_PLAYED + ">=" + minTimesPlayed);
                }
                if (maxTimesPlayed.length() > 0) {
                    selection = appendSelection(selection,
                            Song.TIMES_PLAYED + "<=" + maxTimesPlayed);
                }
                switch (dbOrderByColumn) {
                    case 1:
                        dbOrderBy = Song.TITLE;
                        break;
                    case 2:
                        dbOrderBy = Song.YEAR;
                        break;
                    case 3:
                        dbOrderBy = Song.LAST_PLAYED;
                        break;
                    case 4:
                        dbOrderBy = Song.TIMES_PLAYED;
                        break;
                }
                if (dbOrderBy != null && dbOrderByDesc) {
                    dbOrderBy += " DESC";
                }
            }

            Log.d(TAG, "joinedSortBy=" + joinedSortBy +
                    ", selection=" + selection + ", dbOrderBy=" + dbOrderBy);
            Bundle args = new Bundle();
            args.putSparseParcelableArray(ARG_ARTISTS, artists);
            args.putInt(ARG_JOINED_SORT_BY, joinedSortBy);
            args.putString(ARG_SELECTION, selection);
            args.putString(ARG_DB_ORDER_BY, dbOrderBy);
            startActivity(new Intent(getContext(), SongsActivity.class).putExtras(args));

            preferences.edit()
                    .putInt(KEY_JOINED_SORT_BY, joinedSortBy)
                    .putString(KEY_TITLE, title)
                    .putString(KEY_ARTIST, artist)
                    .putString(KEY_MIN_YEAR, minYear)
                    .putString(KEY_MAX_YEAR, maxYear)
                    .putLong(KEY_MIN_LAST_PLAYED, minLastPlayed)
                    .putLong(KEY_MAX_LAST_PLAYED, maxLastPlayed)
                    .putString(KEY_MIN_TIMES_PLAYED, minTimesPlayed)
                    .putString(KEY_MAX_TIMES_PLAYED, maxTimesPlayed)
                    .putInt(KEY_DB_ORDER_BY, dbOrderByColumn)
                    .putBoolean(KEY_DB_ORDER_BY_DESC, dbOrderByDesc)
                    .apply();
        } else if (v == bTags) {
            final String[] tags = dbOpenHelper.querySongTags();
            new AlertDialog.Builder(getActivity())
                    .setItems(tags, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Bundle args = new Bundle();
                            args.putSparseParcelableArray(ARG_ARTISTS, artists);
                            args.putString(ARG_SELECTION, Song.TAG + "='" + tags[which] + "'");
                            args.putString(ARG_DB_ORDER_BY, Song.LAST_PLAYED + " DESC");
                            startActivity(new Intent(getContext(), SongsActivity.class)
                                    .putExtras(args));
                        }
                    })
                    .show();
        } else if (v == bBackup) {
            Log.d(TAG, "Running backup");
            try {
                backup();
                Log.d(TAG, "Backup completed");
                Toast.makeText(getContext(), R.string.backup_completed, Toast.LENGTH_SHORT).show();
            } catch (Exception ex) {
                Log.e(TAG, "Error running backup", ex);
                Toast.makeText(getContext(),
                        getString(R.string.error, ex.getMessage()), Toast.LENGTH_LONG).show();
            }
        } else if (v == bRestoreBackup) {
            new AlertDialog.Builder(getContext())
                    .setIcon(R.drawable.ic_dialog_warning)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.restore_backup_confirm)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "Restoring backup");
                            final ProgressDialog progressDialog = ProgressDialog.show(getContext(),
                                    getString(R.string.app_name),
                                    getString(R.string.restoring_backup), true);
                            new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    try {
                                        restoreBackup();
                                        Log.d(TAG, "Backup restored");
                                        getActivity().runOnUiThread(new Runnable() {

                                            @Override
                                            public void run() {
                                                ((MainActivity) getActivity())
                                                        .notifyDataSetChanged();
                                                Toast.makeText(getContext(),
                                                        R.string.backup_restored,
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } catch (final Exception ex) {
                                        Log.e(TAG, "Error restoring backup", ex);
                                        getActivity().runOnUiThread(new Runnable() {

                                            @Override
                                            public void run() {
                                                Toast.makeText(getContext(),
                                                        getString(R.string.error, ex.getMessage()),
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    } finally {
                                        progressDialog.dismiss();
                                    }
                                }
                            }).start();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
    }

    public void setArtists(SparseArray<Artist> artists) {
        getArguments().putSparseParcelableArray(ARG_ARTISTS, artists);
        this.artists = artists;
    }

    private void setSpinnerSelection(String key, Spinner view) {
        if (preferences.contains(key)) {
            view.setSelection(preferences.getInt(key, 0));
        }
    }

    private void setEditTextText(String key, EditText view) {
        if (preferences.contains(key)) {
            view.setText(preferences.getString(key, null));
        }
    }

    private void setCheckBoxChecked(String key, CheckBox view) {
        if (preferences.contains(key)) {
            view.setChecked(preferences.getBoolean(key, false));
        }
    }

    private void backup() throws JSONException, IOException {
        JSONObject backup = new JSONObject();

        try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
            JSONArray artists = new JSONArray();
            try (Cursor c = db.query(Artist.TABLE_NAME,
                    null, null, null, null, null, null)) {
                while (c.moveToNext()) {
                    JSONObject artist = new JSONObject();
                    artist.put(Artist._ID, c.getInt(0));
                    artist.put(Artist.ARTIST, c.getString(1));
                    if (!c.isNull(2)) {
                        artist.put(Artist.LAST_PLAYED, c.getLong(2));
                    }
                    if (!c.isNull(3)) {
                        artist.put(Artist.TIMES_PLAYED, c.getInt(3));
                    }
                    artists.put(artist);
                }
            }
            backup.put(Artist.TABLE_NAME, artists);

            JSONArray songs = new JSONArray();
            try (Cursor c = db.query(Song.TABLE_NAME,
                    new String[]{Song.TITLE, Song.ARTIST_ID, Song.LAST_PLAYED, Song.TIMES_PLAYED,
                            Song.BOOKMARKED, Song.TAG},
                    null, null, null, null, null)) {
                while (c.moveToNext()) {
                    JSONObject song = new JSONObject();
                    song.put(Song.TITLE, c.getString(0));
                    song.put(Song.ARTIST_ID, c.getInt(1));
                    if (!c.isNull(2)) {
                        song.put(Song.LAST_PLAYED, c.getLong(2));
                    }
                    if (!c.isNull(3)) {
                        song.put(Song.TIMES_PLAYED, c.getInt(3));
                    }
                    if (!c.isNull(4)) {
                        song.put(Song.BOOKMARKED, c.getLong(4));
                    }
                    if (!c.isNull(5)) {
                        song.put(Song.TAG, c.getString(5));
                    }
                    songs.put(song);
                }
            }
            backup.put(Song.TABLE_NAME, songs);
        }

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                getBackupFile(), false)))) {
            writer.write(backup.toString());
        }
    }

    private void restoreBackup() throws IOException, JSONException {
        JSONObject backup;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(
                getBackupFile())))) {
            backup = new JSONObject(reader.readLine());
        }

        SparseArray<Artist> artists = new SparseArray<>();

        Log.d(TAG, "Processing artists");
        JSONArray jsaArtists = backup.getJSONArray(Artist.TABLE_NAME);
        for (int i = 0; i < jsaArtists.length(); i++) {
            JSONObject jsoArtist = jsaArtists.getJSONObject(i);
            String name = jsoArtist.getString(Artist.ARTIST);

            try (Cursor c = getContext().getContentResolver().query(
                    MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    new String[]{Artist._ID, Artist.ARTIST},
                    Artist.ARTIST + "=?", new String[]{name}, null)) {
                if (c == null || !c.moveToFirst()) {
                    Log.e(TAG, "Artist '" + name + "' not found");
                    continue;
                }

                Artist artist = new Artist();
                artist.setId(c.getInt(c.getColumnIndex(Artist._ID)));
                artist.setArtist(c.getString(c.getColumnIndex(Artist.ARTIST)));

                if (jsoArtist.has(Artist.LAST_PLAYED)) {
                    artist.setLastPlayed(jsoArtist.getLong(Artist.LAST_PLAYED));
                }
                if (jsoArtist.has(Artist.TIMES_PLAYED)) {
                    artist.setTimesPlayed(jsoArtist.getInt(Artist.TIMES_PLAYED));
                }

                dbOpenHelper.insertOrUpdateArtist(artist);
                artists.put(jsoArtist.getInt(Artist._ID), artist);
            }
        }

        Log.d(TAG, "Processing songs");
        JSONArray jsoSongs = backup.getJSONArray(Song.TABLE_NAME);
        for (int i = 0; i < jsoSongs.length(); i++) {
            JSONObject jsoSong = jsoSongs.getJSONObject(i);
            String title = jsoSong.getString(Song.TITLE);

            Artist artist = artists.get(jsoSong.getInt(Song.ARTIST_ID));
            if (artist == null) {
                Log.e(TAG, "Artist for song '" + title + "' not found");
                continue;
            }

            try (Cursor c = getContext().getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{Song._ID, Song.TITLE, Song.YEAR, Song.DURATION},
                    Song.TITLE + "=? AND " + Song.ARTIST_ID + "=?",
                    new String[]{title, Integer.toString(artist.getId())}, null)) {
                if (c == null || !c.moveToFirst()) {
                    Log.e(TAG, "Song '" + title + "' not found");
                    continue;
                }

                Song song = new Song();
                song.setId(c.getInt(c.getColumnIndex(Song._ID)));
                song.setTitle(c.getString(c.getColumnIndex(Song.TITLE)));
                song.setArtist(artist);
                song.setYear(c.getInt(c.getColumnIndex(Song.YEAR)));
                song.setDuration(c.getInt(c.getColumnIndex(Song.DURATION)));

                if (jsoSong.has(Song.LAST_PLAYED)) {
                    song.setLastPlayed(jsoSong.getLong(Song.LAST_PLAYED));
                }
                if (jsoSong.has(Song.TIMES_PLAYED)) {
                    song.setTimesPlayed(jsoSong.getInt(Song.TIMES_PLAYED));
                }
                if (jsoSong.has(Song.BOOKMARKED)) {
                    song.setBookmarked(jsoSong.getLong(Song.BOOKMARKED));
                }
                if (jsoSong.has(Song.TAG)) {
                    song.setTag(jsoSong.getString(Song.TAG));
                }

                dbOpenHelper.insertOrUpdateSong(song);
            }
        }
    }

    private File getBackupFile() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "UPlayer.json");
    }

    public static QueryFragment newInstance(SparseArray<Artist> artists) {
        QueryFragment fragment = new QueryFragment();
        Bundle args = new Bundle();
        args.putSparseParcelableArray(ARG_ARTISTS, artists);
        fragment.setArguments(args);
        return fragment;
    }

    private static String appendSelection(String selection, String s) {
        return selection == null ? s : selection + " AND " + s;
    }
}
