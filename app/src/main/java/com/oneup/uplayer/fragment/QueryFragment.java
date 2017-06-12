package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.oneup.uplayer.R;
import com.oneup.uplayer.Util;
import com.oneup.uplayer.activity.DatePickerActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;

//TODO: More query options, sort by year?

public class QueryFragment extends Fragment implements BaseArgs, View.OnClickListener {
    private static final String SQL_QUERY_TOTAL_SONGS_PLAYED =
            "SELECT SUM(" + Artist.TIMES_PLAYED + ") FROM " + Artist.TABLE_NAME;
    private static final int REQUEST_SELECT_MIN_DATE = 1;
    private static final int REQUEST_SELECT_MAX_DATE = 2;

    private SparseArray<Artist> artists;

    private DbOpenHelper dbOpenHelper;
    private TextView tvTotalSongsPlayed;
    private Button bMinDate;
    private Button bMaxDate;
    private Button bQuery;

    private long minDate;
    private long maxDate;

    public QueryFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.fragment_query, container, false);

        artists = getArguments().getSparseParcelableArray(ARG_ARTISTS);

        dbOpenHelper = new DbOpenHelper(getActivity());
        try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
            try (Cursor c = db.rawQuery(SQL_QUERY_TOTAL_SONGS_PLAYED, null)) {
                if (c.moveToFirst()) {
                    tvTotalSongsPlayed = (TextView) ret.findViewById(R.id.tvTotalSongsPlayed);
                    tvTotalSongsPlayed.setText(getString(R.string.total_songs_played, c.getInt(0)));
                }
            }
        }

        bMinDate = (Button) ret.findViewById(R.id.bMinDate);
        if (minDate > 0) {
            bMinDate.setText(Util.formatDate(minDate));
        }
        bMinDate.setOnClickListener(this);

        bMaxDate = (Button) ret.findViewById(R.id.bMaxDate);
        if (maxDate > 0) {
            bMaxDate.setText(Util.formatDate(maxDate));
        }
        bMaxDate.setOnClickListener(this);

        bQuery = (Button) ret.findViewById(R.id.bQuery);
        bQuery.setOnClickListener(this);

        return ret;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_MIN_DATE:
                    minDate = data.getLongExtra(DatePickerActivity.EXTRA_DATE, 0);
                    bMinDate.setText(Util.formatDate(minDate));
                    break;
                case REQUEST_SELECT_MAX_DATE:
                    maxDate = data.getLongExtra(DatePickerActivity.EXTRA_DATE, 0);
                    bMaxDate.setText(Util.formatDate(maxDate));
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
    public void onClick(View v) {
        if (v == bMinDate) {
            startActivityForResult(new Intent(getContext(), DatePickerActivity.class)
                            .putExtra(DatePickerActivity.EXTRA_TITLE, R.string.min_date)
                            .putExtra(DatePickerActivity.EXTRA_DATE, minDate),
                    REQUEST_SELECT_MIN_DATE);
        } else if (v == bMaxDate) {
            startActivityForResult(new Intent(getContext(), DatePickerActivity.class)
                            .putExtra(DatePickerActivity.EXTRA_TITLE, R.string.max_date)
                            .putExtra(DatePickerActivity.EXTRA_DATE, maxDate),
                    REQUEST_SELECT_MAX_DATE);
        } else if (v == bQuery) {
            if (minDate == 0 || maxDate == 0 || minDate > maxDate) {
                Toast.makeText(getContext(), R.string.invalid_date_range,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle args = new Bundle();
            args.putSparseParcelableArray(ARG_ARTISTS, artists);
            args.putString(ARG_SELECTION, Song.LAST_PLAYED + ">=" + minDate + " AND " +
                    Song.LAST_PLAYED + "<" + (maxDate + 86400000));
            args.putString(ARG_DB_ORDER_BY, Song.LAST_PLAYED);
            startActivity(new Intent(getContext(), SongsActivity.class).putExtras(args));
        }
    }

    public static QueryFragment newInstance(SparseArray<Artist> artists) {
        QueryFragment fragment = new QueryFragment();
        Bundle args = new Bundle();
        args.putSparseParcelableArray(ARG_ARTISTS, artists);
        fragment.setArguments(args);
        return fragment;
    }
}
