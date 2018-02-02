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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.DateTimeActivity;
import com.oneup.uplayer.activity.MainActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;
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

public class QueryFragment extends Fragment implements BaseArgs, AdapterView.OnItemSelectedListener,
        View.OnClickListener, View.OnLongClickListener {
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
    private static final String KEY_MIN_DATE_ADDED = "minDateAdded";
    private static final String KEY_MAX_DATE_ADDED = "maxDateAdded";
    private static final String KEY_MIN_YEAR = "minYear";
    private static final String KEY_MAX_YEAR = "maxYear";
    private static final String KEY_MIN_LAST_PLAYED = "minLastPlayed";
    private static final String KEY_MAX_LAST_PLAYED = "maxLastPlayed";
    private static final String KEY_MIN_TIMES_PLAYED = "minTimesPlayed";
    private static final String KEY_MAX_TIMES_PLAYED = "maxTimesPlayed";
    private static final String KEY_TAG_SELECTION = "tag_selection";
    private static final String KEY_DB_ORDER_BY = "dbOrderBy";
    private static final String KEY_DB_ORDER_BY_DESC = "dbOrderByDesc";

    private static final int REQUEST_SELECT_MIN_DATE_ADDED = 1;
    private static final int REQUEST_SELECT_MAX_DATE_ADDED = 2;
    private static final int REQUEST_SELECT_MIN_LAST_PLAYED = 3;
    private static final int REQUEST_SELECT_MAX_LAST_PLAYED = 4;

    private static final int TAG_SELECTION_TAGGED = 1;
    private static final int TAG_SELECTION_UNTAGGED = 2;

    private SparseArray<Artist> artists;

    private DbOpenHelper dbOpenHelper;
    private SharedPreferences preferences;

    private TextView tvTotals;
    private Spinner sJoinedSortBy;
    private EditText etTitle;
    private EditText etArtist;
    private LinearLayout llDateAdded;
    private Button bMinDateAdded;
    private Button bMaxDateAdded;
    private EditText etMinYear;
    private EditText etMaxYear;
    private LinearLayout llLastPlayed;
    private Button bMinLastPlayed;
    private Button bMaxLastPlayed;
    private LinearLayout llTimesPlayed;
    private EditText etMinTimesPlayed;
    private EditText etMaxTimesPlayed;
    private RadioGroup rgTagSelection;
    private LinearLayout llDbOrderBy;
    private Spinner sDbOrderBy;
    private CheckBox cbDbOrderByDesc;
    private Button bQuery;
    private Button bTags;
    private Button bBackup;
    private Button bRestoreBackup;

    private long minDateAdded;
    private long maxDateAdded;
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
        setSpinnerSelection(sJoinedSortBy, KEY_JOINED_SORT_BY);

        etTitle = ret.findViewById(R.id.etTitle);
        setEditTextText(etTitle, KEY_TITLE);

        etArtist = ret.findViewById(R.id.etArtist);
        setEditTextText(etArtist, KEY_ARTIST);

        llDateAdded = ret.findViewById(R.id.llDateAdded);

        bMinDateAdded = ret.findViewById(R.id.bMinDateAdded);
        bMinDateAdded.setOnClickListener(this);
        bMinDateAdded.setOnLongClickListener(this);
        if (minDateAdded == 0) {
            minDateAdded = preferences.getLong(KEY_MIN_DATE_ADDED, 0);
        }
        if (minDateAdded > 0) {
            bMinDateAdded.setText(Util.formatDateTime(minDateAdded));
        }

        bMaxDateAdded = ret.findViewById(R.id.bMaxDateAdded);
        bMaxDateAdded.setOnClickListener(this);
        bMaxDateAdded.setOnLongClickListener(this);
        if (maxDateAdded == 0) {
            maxDateAdded = preferences.getLong(KEY_MAX_DATE_ADDED, 0);
        }
        if (maxDateAdded > 0) {
            bMaxDateAdded.setText(Util.formatDateTime(maxDateAdded));
        }

        etMinYear = ret.findViewById(R.id.etMinYear);
        setEditTextText(etMinYear, KEY_MIN_YEAR);

        etMaxYear = ret.findViewById(R.id.etMaxYear);
        setEditTextText(etMaxYear, KEY_MAX_YEAR);

        llLastPlayed = ret.findViewById(R.id.llLastPlayed);

        bMinLastPlayed = ret.findViewById(R.id.bMinLastPlayed);
        bMinLastPlayed.setOnClickListener(this);
        bMinLastPlayed.setOnLongClickListener(this);
        if (minLastPlayed == 0) {
            minLastPlayed = preferences.getLong(KEY_MIN_LAST_PLAYED, 0);
        }
        if (minLastPlayed > 0) {
            bMinLastPlayed.setText(Util.formatDateTime(minLastPlayed));
        }

        bMaxLastPlayed = ret.findViewById(R.id.bMaxLastPlayed);
        bMaxLastPlayed.setOnClickListener(this);
        bMaxLastPlayed.setOnLongClickListener(this);
        if (maxLastPlayed == 0) {
            maxLastPlayed = preferences.getLong(KEY_MAX_LAST_PLAYED, 0);
        }
        if (maxLastPlayed > 0) {
            bMaxLastPlayed.setText(Util.formatDateTime(maxLastPlayed));
        }

        llTimesPlayed = ret.findViewById(R.id.llTimesPlayed);

        etMinTimesPlayed = ret.findViewById(R.id.etMinTimesPlayed);
        setEditTextText(etMinTimesPlayed, KEY_MIN_TIMES_PLAYED);

        etMaxTimesPlayed = ret.findViewById(R.id.etMaxTimesPlayed);
        setEditTextText(etMaxTimesPlayed, KEY_MAX_TIMES_PLAYED);

        rgTagSelection = ret.findViewById(R.id.rgTagSelection);
        setRadioGroupCheckedRadioButton(rgTagSelection, KEY_TAG_SELECTION);

        llDbOrderBy = ret.findViewById(R.id.llDbOrderBy);

        sDbOrderBy = ret.findViewById(R.id.sDbOrderBy);
        setSpinnerSelection(sDbOrderBy, KEY_DB_ORDER_BY);

        cbDbOrderByDesc = ret.findViewById(R.id.cbDbOrderByDesc);
        setCheckBoxChecked(cbDbOrderByDesc, KEY_DB_ORDER_BY_DESC);

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
                case REQUEST_SELECT_MIN_DATE_ADDED:
                    minDateAdded = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMinDateAdded.setText(Util.formatDateTime(minDateAdded));
                    break;
                case REQUEST_SELECT_MAX_DATE_ADDED:
                    maxDateAdded = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMaxDateAdded.setText(Util.formatDateTime(maxDateAdded));
                    break;
                case REQUEST_SELECT_MIN_LAST_PLAYED:
                    minLastPlayed = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMinLastPlayed.setText(Util.formatDateTime(minLastPlayed));
                    break;
                case REQUEST_SELECT_MAX_LAST_PLAYED:
                    maxLastPlayed = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMaxLastPlayed.setText(Util.formatDateTime(maxLastPlayed));
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
            llDateAdded.setVisibility(dbVisibility);
            llLastPlayed.setVisibility(dbVisibility);
            llTimesPlayed.setVisibility(dbVisibility);
            rgTagSelection.setVisibility(dbVisibility);
            llDbOrderBy.setVisibility(dbVisibility);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parentw) {
    }

    @Override
    public void onClick(View v) {
        if (v == bMinDateAdded) {
            Intent intent = new Intent(getContext(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.min_date_added);
            if (minDateAdded > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, minDateAdded);
            }
            startActivityForResult(intent, REQUEST_SELECT_MIN_DATE_ADDED);
        } else if (v == bMaxDateAdded) {
            Intent intent = new Intent(getContext(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.min_date_added);
            if (maxDateAdded > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, maxDateAdded);
            }
            startActivityForResult(intent, REQUEST_SELECT_MAX_DATE_ADDED);
        } else if (v == bMinLastPlayed) {
            Intent intent = new Intent(getContext(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.min_last_played);
            if (minLastPlayed > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, minLastPlayed);
            }
            startActivityForResult(intent, REQUEST_SELECT_MIN_LAST_PLAYED);
        } else if (v == bMaxLastPlayed) {
            Intent intent = new Intent(getContext(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.min_last_played);
            if (maxLastPlayed > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, maxLastPlayed);
            }
            startActivityForResult(intent, REQUEST_SELECT_MIN_LAST_PLAYED);
        } else if (v == bQuery) {
            query(null);
        } else if (v == bTags) {
            final String[] tags = dbOpenHelper.querySongTags();
            new AlertDialog.Builder(getActivity())
                    .setItems(tags, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            query(tags[which] );
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

    @Override
    public boolean onLongClick(View v) {
        if (v == bMinDateAdded) {
            minDateAdded = 0;
            bMinDateAdded.setText(R.string.min_date_added);
            return true;
        } else if (v == bMaxDateAdded) {
            maxDateAdded = 0;
            bMaxDateAdded.setText(R.string.max_date_added);
            return true;
        } else if (v == bMinLastPlayed) {
            minLastPlayed = 0;
            bMinLastPlayed.setText(R.string.min_last_played);
            return true;
        } else if (v == bMaxLastPlayed) {
            maxLastPlayed = 0;
            bMaxLastPlayed.setText(R.string.max_last_played);
            return true;
        } else {
            return false;
        }
    }

    public void setArtists(SparseArray<Artist> artists) {
        getArguments().putSparseParcelableArray(ARG_ARTISTS, artists);
        this.artists = artists;
    }

    private void setSpinnerSelection(Spinner view, String preferencesKey) {
        if (preferences.contains(preferencesKey)) {
            view.setSelection(preferences.getInt(preferencesKey, 0));
        }
    }

    private void setEditTextText(EditText view, String preferencesKey) {
        if (preferences.contains(preferencesKey)) {
            view.setText(preferences.getString(preferencesKey, null));
        }
    }

    private int getRadioGroupCheckedRadioButton(RadioGroup view) {
        for (int i = 0; i < view.getChildCount(); i++) {
            if (((RadioButton) view.getChildAt(i)).isChecked()) {
                return i;
            }
        }
        return -1;
    }

    private void setRadioGroupCheckedRadioButton(RadioGroup view, String preferencesKey) {
        ((RadioButton) view.getChildAt(preferences.getInt(preferencesKey, 0))).setChecked(true);
    }

    private void setCheckBoxChecked(CheckBox view, String preferencesKey) {
        if (preferences.contains(preferencesKey)) {
            view.setChecked(preferences.getBoolean(preferencesKey, false));
        }
    }

    private void query(String tag) {
        String selection = null, dbOrderBy = null;
        SharedPreferences.Editor preferences = this.preferences.edit();

        String title = etTitle.getText().toString();
        if (title.length() > 0) {
            selection = Song.TITLE + " LIKE '%" + title + "%'";
        }
        preferences.putString(KEY_TITLE, title);

        String minYear = etMinYear.getText().toString();
        if (minYear.length() > 0) {
            selection = appendSelection(selection, Song.YEAR + ">=" + minYear);
        }
        preferences.putString(KEY_MIN_YEAR, minYear);

        String maxYear = etMaxYear.getText().toString();
        if (maxYear.length() > 0) {
            selection = appendSelection(selection, Song.YEAR + "<=" + maxYear);
        }
        preferences.putString(KEY_MAX_YEAR, maxYear);

        int joinedSortBy = sJoinedSortBy.getSelectedItemPosition();
        if (joinedSortBy == 0) {
            String artist = etArtist.getText().toString();
            if (artist.length() > 0) {
                selection = appendSelection(selection, Song.ARTIST_ID +
                        " IN(SELECT " + Artist._ID + " FROM " + Artist.TABLE_NAME +
                        " WHERE " + Artist.ARTIST + " LIKE '%" + artist + "%')");
            }
            preferences.putString(KEY_ARTIST, artist);

            if (minDateAdded > 0) {
                selection = appendSelection(selection,
                        Song.DATE_ADDED + ">=" + minDateAdded);
            }
            preferences.putLong(KEY_MIN_DATE_ADDED, minDateAdded);

            if (maxDateAdded > 0) {
                selection = appendSelection(selection,
                        Song.DATE_ADDED + "<=" + maxDateAdded);
            }
            preferences.putLong(KEY_MAX_DATE_ADDED, maxDateAdded);

            if (minLastPlayed > 0) {
                selection = appendSelection(selection,
                        Song.LAST_PLAYED + ">=" + minLastPlayed);
            }
            preferences.putLong(KEY_MIN_LAST_PLAYED, minLastPlayed);

            if (maxLastPlayed > 0) {
                selection = appendSelection(selection,
                        Song.LAST_PLAYED + "<=" + maxLastPlayed + 86400000);
            }
            preferences.putLong(KEY_MAX_LAST_PLAYED, maxLastPlayed);

            String minTimesPlayed = etMinTimesPlayed.getText().toString();
            if (minTimesPlayed.length() > 0) {
                selection = appendSelection(selection,
                        Song.TIMES_PLAYED + ">=" + minTimesPlayed);
            }
            preferences.putString(KEY_MIN_TIMES_PLAYED, minTimesPlayed);

            String maxTimesPlayed = etMaxTimesPlayed.getText().toString();
            if (maxTimesPlayed.length() > 0) {
                selection = appendSelection(selection,
                        Song.TIMES_PLAYED + "<=" + maxTimesPlayed);
            }
            preferences.putString(KEY_MAX_TIMES_PLAYED, maxTimesPlayed);

            if (tag == null) {
                int tagSelection = getRadioGroupCheckedRadioButton(rgTagSelection);
                switch (tagSelection) {
                    case TAG_SELECTION_TAGGED:
                        selection = appendSelection(selection, Song.TAG + " IS NOT NULL");
                        break;
                    case TAG_SELECTION_UNTAGGED:
                        selection = appendSelection(selection, Song.TAG + " IS NULL");
                        break;
                }
                preferences.putInt(KEY_TAG_SELECTION, tagSelection);
            } else {
                selection = appendSelection(selection, Song.TAG + "='" + tag + "'");
            }

            int dbOrderByColumn = sDbOrderBy.getSelectedItemPosition();
            switch (dbOrderByColumn) {
                case 1:
                    dbOrderBy = Song.TITLE;
                    break;
                case 2:
                    dbOrderBy = Song.DATE_ADDED;
                    break;
                case 3:
                    dbOrderBy = Song.YEAR;
                    break;
                case 4:
                    dbOrderBy = Song.LAST_PLAYED;
                    break;
                case 5:
                    dbOrderBy = Song.TIMES_PLAYED;
                    break;
            }
            preferences.putInt(KEY_DB_ORDER_BY, dbOrderByColumn);

            boolean dbOrderByDesc = cbDbOrderByDesc.isChecked();
            if (dbOrderBy != null && dbOrderByDesc) {
                dbOrderBy += " DESC";
            }
            preferences.putBoolean(KEY_DB_ORDER_BY_DESC, dbOrderByDesc);
        }
        preferences.putInt(KEY_JOINED_SORT_BY, joinedSortBy);

        Bundle args = new Bundle();
        args.putSparseParcelableArray(ARG_ARTISTS, artists);
        args.putInt(ARG_JOINED_SORT_BY, joinedSortBy);
        args.putString(ARG_SELECTION, selection);
        args.putString(ARG_DB_ORDER_BY, dbOrderBy);
        startActivity(new Intent(getContext(), SongsActivity.class).putExtras(args));

        preferences.apply();
    }

    private void backup() throws JSONException, IOException {
        JSONObject backup = new JSONObject();

        try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
            // Process artists.
            JSONArray artists = new JSONArray();
            try (Cursor c = db.query(Artist.TABLE_NAME, null, null, null, null, null, null)) {
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
                    if (!c.isNull(4)) {
                        artist.put(Artist.DATE_MODIFIED, c.getLong(4));
                    }
                    artists.put(artist);
                }
            }
            backup.put(Artist.TABLE_NAME, artists);

            // Process songs.
            JSONArray songs = new JSONArray();
            try (Cursor c = db.query(Song.TABLE_NAME, null, null, null, null, null, null)) {
                while (c.moveToNext()) {
                    JSONObject song = new JSONObject();
                    song.put(Song.TITLE, c.getString(1));
                    song.put(Song.ARTIST_ID, c.getInt(2));
                    if (!c.isNull(3)) {
                        song.put(Song.DATE_ADDED, c.getLong(3));
                    }
                    if (!c.isNull(4)) {
                        song.put(Song.YEAR, c.getInt(4));
                    }
                    if (!c.isNull(5)) {
                        song.put(Song.DURATION, c.getInt(5));
                    }
                    if (!c.isNull(6)) {
                        song.put(Song.LAST_PLAYED, c.getLong(6));
                    }
                    if (!c.isNull(7)) {
                        song.put(Song.TIMES_PLAYED, c.getInt(7));
                    }
                    if (!c.isNull(8)) {
                        song.put(Song.BOOKMARKED, c.getLong(8));
                    }
                    if (!c.isNull(9)) {
                        song.put(Song.TAG, c.getString(9));
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
                    MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, new String[]{Artist._ID},
                    Artist.ARTIST + "=?", new String[]{name}, null)) {
                if (c == null || !c.moveToFirst()) {
                    Log.e(TAG, "Artist '" + name + "' not found");
                    continue;
                }

                Artist artist = new Artist();
                artist.setId(c.getInt(c.getColumnIndex(Artist._ID)));
                //TODO: Just use 0 and not use getColumnIndex() when specifying columns?

                artist.setArtist(name);

                if (jsoArtist.has(Artist.LAST_PLAYED)) {
                    artist.setLastPlayed(jsoArtist.getLong(Artist.LAST_PLAYED));
                }
                if (jsoArtist.has(Artist.TIMES_PLAYED)) {
                    artist.setTimesPlayed(jsoArtist.getInt(Artist.TIMES_PLAYED));
                }
                if (jsoArtist.has(Artist.DATE_MODIFIED)) {
                    artist.setDateModified(jsoArtist.getLong(Artist.DATE_MODIFIED));
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
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{Song._ID},
                    Song.TITLE + "=? AND " + Song.ARTIST_ID + "=?",
                    new String[]{title, Integer.toString(artist.getId())}, null)) {
                if (c == null || !c.moveToFirst()) {
                    Log.e(TAG, "Song '" + title + "' not found");
                    continue;
                }

                Song song = new Song();
                song.setId(c.getInt(c.getColumnIndex(Song._ID)));

                song.setTitle(title);
                song.setArtist(artist);

                if (jsoSong.has(Song.DATE_ADDED)) {
                    song.setDateAdded(jsoSong.getLong(Song.DATE_ADDED));
                }
                if (jsoSong.has(Song.YEAR)) {
                    song.setYear(jsoSong.getInt(Song.YEAR));
                }
                if (jsoSong.has(Song.DURATION)) {
                    song.setDuration(jsoSong.getInt(Song.DURATION));
                }
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
