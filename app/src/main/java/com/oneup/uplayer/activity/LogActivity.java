package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.LogData;
import com.oneup.uplayer.util.Settings;
import com.oneup.uplayer.util.Util;
import com.oneup.util.Utils;

public class LogActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnLongClickListener {
    public static final String EXTRA_TITLE = "com.oneup.extra.TITLE";

    public static final String EXTRA_QUERY_ARTIST = "com.oneup.extra.QUERY_ARTIST";
    public static final String EXTRA_SELECTION = "com.oneup.extra.SELECTION";
    public static final String EXTRA_SELECTION_ARGS = "com.oneup.extra.SELECTION_ARGS";

    private static final int REQUEST_SELECT_MIN_DATE = 1;
    private static final int REQUEST_SELECT_MAX_DATE = 2;

    private Settings settings;
    private long minDate;
    private long maxDate;

    private DbHelper dbHelper;
    private boolean queryArtist;
    private String selection;
    private String[] selectionArgs;

    private Button bMinDate;
    private Button bMaxDate;
    private TextView tvCount;
    private TextView tvSongCount;
    private TextView tvArtistCount;

    private ListView lvDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        if (getIntent().hasExtra(EXTRA_TITLE)) {
            setTitle(getIntent().getStringExtra(EXTRA_TITLE));
        }

        settings = Settings.get(this);
        minDate = settings.getLong(R.string.key_log_min_date, 0);
        maxDate = settings.getLong(R.string.key_log_max_date, 0);

        dbHelper = new DbHelper(this);
        queryArtist = getIntent().getBooleanExtra(EXTRA_QUERY_ARTIST, true);
        selection = getIntent().getStringExtra(EXTRA_SELECTION);
        selectionArgs = getIntent().getStringArrayExtra(EXTRA_SELECTION_ARGS);

        bMinDate = findViewById(R.id.bMinDate);
        if (minDate != 0) {
            bMinDate.setText(Util.formatDate(minDate));
        }
        bMinDate.setOnClickListener(this);
        bMinDate.setOnLongClickListener(this);

        bMaxDate = findViewById(R.id.bMaxDate);
        if (maxDate != 0) {
            bMaxDate.setText(Util.formatDate(maxDate));
        }
        bMaxDate.setOnClickListener(this);
        bMaxDate.setOnLongClickListener(this);

        tvCount = findViewById(R.id.tvCount);
        tvSongCount = findViewById(R.id.tvSongCount);
        tvArtistCount = findViewById(R.id.tvArtistCount);

        lvDays = findViewById(R.id.lvDays);
        query();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_MIN_DATE:
                    minDate = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMinDate.setText(Util.formatDate(minDate));
                    query();
                    break;
                case REQUEST_SELECT_MAX_DATE:
                    maxDate = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMaxDate.setText(Util.formatDate(maxDate));
                    query();
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        settings.edit()
                .putLong(R.string.key_log_min_date, minDate)
                .putLong(R.string.key_log_max_date, maxDate)
                .apply();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == bMinDate) {
            selectDate(R.string.min_date, minDate, REQUEST_SELECT_MIN_DATE);
        } else if (v == bMaxDate) {
            selectDate(R.string.max_date, maxDate, REQUEST_SELECT_MAX_DATE);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == bMinDate) {
            minDate = 0;
            bMinDate.setText(R.string.min_date);
            query();
        } else if (v == bMaxDate) {
            maxDate = 0;
            bMaxDate.setText(R.string.max_date);
            query();
        }
        return true;
    }

    private void selectDate(int titleId, long time, int requestCode) {
        Intent intent = new Intent(this, DateTimeActivity.class);
        intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, titleId);
        if (time != 0) {
            intent.putExtra(DateTimeActivity.EXTRA_TIME, time);
        }
        intent.putExtra(DateTimeActivity.EXTRA_SHOW_EDIT_TIME, false);
        startActivityForResult(intent, requestCode);
    }

    private void query() {
        LogData log = dbHelper.queryLog(minDate, maxDate, selection, selectionArgs);
        if (log.getCount() > 0) {
            tvCount.setText(log.toString());
            tvCount.setVisibility(View.VISIBLE);
        } else {
            tvCount.setVisibility(View.GONE);
        }

        if (log.getSongCount() > 1) {
            String count = Utils.getCountString(this, R.plurals.songs, log.getSongCount());
            if (log.getSongCount() != log.getCount()) {
                count += " (" + Util.formatFraction(
                        log.getCount(), log.getSongCount()) + ")";
            }
            tvSongCount.setText(count);
            tvSongCount.setVisibility(View.VISIBLE);
        } else {
            tvSongCount.setVisibility(View.GONE);
        }

        if (queryArtist && log.getArtistCount() > 1) {
            String count = Utils.getCountString(this, R.plurals.artists, log.getArtistCount());
            if (log.getArtistCount() != log.getSongCount()) {
                count += " (" + Util.formatFraction(
                        log.getSongCount(), log.getArtistCount()) + ")";
            }
            tvArtistCount.setText(count);
            tvArtistCount.setVisibility(View.VISIBLE);
        } else {
            tvArtistCount.setVisibility(View.GONE);
        }

        if (log.getDays() == null) {
            lvDays.setVisibility(View.GONE);
        } else {
            lvDays.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, log.getDays()));
            lvDays.setVisibility(View.VISIBLE);
        }
    }
}
