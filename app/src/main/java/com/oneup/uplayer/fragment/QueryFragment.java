package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

import com.oneup.uplayer.R;
import com.oneup.uplayer.Util;
import com.oneup.uplayer.activity.DatePickerActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;

//TODO: getPrefs or SharedPreferences?

public class QueryFragment extends Fragment implements BaseArgs, AdapterView.OnItemSelectedListener,
        View.OnClickListener {
    private static final String TAG = "UPlayer";

    private static final String SQL_QUERY_SONGS_PLAYED =
            "SELECT COUNT(" + Song.TIMES_PLAYED + ") FROM " + Song.TABLE_NAME;

    private static final String SQL_QUERY_ARTISTS_PLAYED =
            "SELECT COUNT(" + Artist.TIMES_PLAYED + ") FROM " + Artist.TABLE_NAME;

    private static final String SQL_QUERY_TOTAL_SONGS_PLAYED =
            "SELECT SUM(" + Artist.TIMES_PLAYED + ") FROM " + Artist.TABLE_NAME;

    private static final String KEY_JOINED_SORT_BY = "joinedSortBy";
    private static final String KEY_TITLE = "title";
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

    private TextView tvTotalSongsPlayed;
    private Spinner sJoinedSortBy;
    private EditText etTitle;
    private EditText etMinYear;
    private EditText etMaxYear;
    private LinearLayout llLastPlayed;
    private Button bMinLastPlayed;
    private Button bMaxLastPlayed;
    private LinearLayout llTimesPlayed;
    private EditText etMinTimesPlayed;
    private EditText etMaxTimesPlayed;
    private LinearLayout llDbOrderBy;;
    private Spinner sDbOrderBy;
    private CheckBox cbDbOrderByDesc;
    private Button bQuery;

    private long minLastPlayed;
    private long maxLastPlayed;

    public QueryFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        artists = getArguments().getSparseParcelableArray(ARG_ARTISTS);

        dbOpenHelper = new DbOpenHelper(getActivity());
        int songsPlayed, artistsPlayed, totalPlayed;
        try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
            songsPlayed = DbOpenHelper.queryInt(db, SQL_QUERY_SONGS_PLAYED);
            artistsPlayed = DbOpenHelper.queryInt(db, SQL_QUERY_ARTISTS_PLAYED);
            totalPlayed = DbOpenHelper.queryInt(db, SQL_QUERY_TOTAL_SONGS_PLAYED);
        }

        preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        View ret = inflater.inflate(R.layout.fragment_query, container, false);

        tvTotalSongsPlayed = (TextView) ret.findViewById(R.id.tvTotalSongsPlayed);
        tvTotalSongsPlayed.setText(getString(R.string.songs_played,
                songsPlayed, artistsPlayed, totalPlayed));

        sJoinedSortBy = (Spinner) ret.findViewById(R.id.sJoinedSortBy);
        sJoinedSortBy.setOnItemSelectedListener(this);
        setSpinnerSelection(KEY_JOINED_SORT_BY, sJoinedSortBy);

        etTitle = (EditText) ret.findViewById(R.id.etTitle);
        setEditTextText(KEY_TITLE, etTitle);

        etMinYear = (EditText) ret.findViewById(R.id.etMinYear);
        setEditTextText(KEY_MIN_YEAR, etMinYear);

        etMaxYear = (EditText) ret.findViewById(R.id.etMaxYear);
        setEditTextText(KEY_MAX_YEAR, etMaxYear);

        llLastPlayed = (LinearLayout) ret.findViewById(R.id.llLastPlayed);

        bMinLastPlayed = (Button) ret.findViewById(R.id.bMinLastPlayed);
        bMinLastPlayed.setOnClickListener(this);
        if (minLastPlayed == 0) {
            minLastPlayed = preferences.getLong(KEY_MIN_LAST_PLAYED, 0);
        }
        if (minLastPlayed > 0) {
            bMinLastPlayed.setText(Util.formatDate(minLastPlayed));
        }

        bMaxLastPlayed = (Button) ret.findViewById(R.id.bMaxLastPlayed);
        bMaxLastPlayed.setOnClickListener(this);
        if (maxLastPlayed == 0) {
            maxLastPlayed = preferences.getLong(KEY_MAX_LAST_PLAYED, 0);
        }
        if (maxLastPlayed > 0) {
            bMaxLastPlayed.setText(Util.formatDate(maxLastPlayed));
        }

        llTimesPlayed = (LinearLayout) ret.findViewById(R.id.llTimesPlayed);

        etMinTimesPlayed = (EditText) ret.findViewById(R.id.etMinTimesPlayed);
        setEditTextText(KEY_MIN_TIMES_PLAYED, etMinTimesPlayed);

        etMaxTimesPlayed = (EditText) ret.findViewById(R.id.etMaxTimesPlayed);
        setEditTextText(KEY_MAX_TIMES_PLAYED, etMaxTimesPlayed);

        llDbOrderBy = (LinearLayout) ret.findViewById(R.id.llDbOrderBy);

        sDbOrderBy = (Spinner) ret.findViewById(R.id.sDbOrderBy);
        setSpinnerSelection(KEY_DB_ORDER_BY, sDbOrderBy);

        cbDbOrderByDesc = (CheckBox) ret.findViewById(R.id.cbDbOrderByDesc);
        setCheckBoxChecked(KEY_DB_ORDER_BY_DESC, cbDbOrderByDesc);

        bQuery = (Button) ret.findViewById(R.id.bQuery);
        bQuery.setOnClickListener(this);

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
        if (dbOpenHelper != null) {
            dbOpenHelper.close();
        }

        super.onDestroy();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == sJoinedSortBy) {
            int dbVisibility = position == 0 ? View.VISIBLE : View.GONE;
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
                if (minLastPlayed > 0) {
                    selection = appendSelection(selection, Song.LAST_PLAYED + ">=" + minLastPlayed);
                }
                if (maxLastPlayed > 0) {
                    selection = appendSelection(selection, Song.LAST_PLAYED + "<=" +
                            maxLastPlayed + 86400000);
                }
                if (minTimesPlayed.length() > 0) {
                    selection = appendSelection(selection, Song.TIMES_PLAYED + ">=" + minTimesPlayed);
                }
                if (maxTimesPlayed.length() > 0) {
                    selection = appendSelection(selection, Song.TIMES_PLAYED + "<=" + maxTimesPlayed);
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
                    .putString(KEY_MIN_YEAR, minYear)
                    .putString(KEY_MAX_YEAR, maxYear)
                    .putLong(KEY_MIN_LAST_PLAYED, minLastPlayed)
                    .putLong(KEY_MAX_LAST_PLAYED, maxLastPlayed)
                    .putString(KEY_MIN_TIMES_PLAYED, minTimesPlayed)
                    .putString(KEY_MAX_TIMES_PLAYED, maxTimesPlayed)
                    .putInt(KEY_DB_ORDER_BY, dbOrderByColumn)
                    .putBoolean(KEY_DB_ORDER_BY_DESC, dbOrderByDesc)
                    .apply();
        }
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
