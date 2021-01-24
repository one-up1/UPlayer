package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
    public static final String EXTRA_QUERY_BOOKMARKED = "com.oneup.extra.QUERY_BOOKMARKED";
    public static final String EXTRA_QUERY_ARCHIVED = "com.oneup.extra.QUERY_ARCHIVED";

    public static final String EXTRA_BASE_SELECTION = "com.oneup.extra.BASE_SELECTION";
    public static final String EXTRA_BASE_SELECTION_ARGS = "com.oneup.extra.BASE_SELECTION_ARGS";

    public static final String EXTRA_SELECTION = "com.oneup.extra.SELECTION";
    public static final String EXTRA_SELECTION_ARGS = "com.oneup.extra.SELECTION_ARGS";

    private static final int REQUEST_SELECT_MIN_TIME = 1;
    private static final int REQUEST_SELECT_MAX_TIME = 2;

    private Settings settings;
    private long minTime;
    private long maxTime;

    private DbHelper dbHelper;

    private Button bMinTime;
    private Button bMaxTime;
    private TextView tvCount;
    private TextView tvSongCount;
    private TextView tvArtistCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        if (getIntent().hasExtra(EXTRA_TITLE)) {
            setTitle(getIntent().getStringExtra(EXTRA_TITLE));
        }

        settings = Settings.get(this);
        minTime = settings.getLong(R.string.key_log_min_timestamp, 0);
        maxTime = settings.getLong(R.string.key_log_max_timestamp, 0);

        dbHelper = new DbHelper(this);

        bMinTime = findViewById(R.id.bMinTime);
        if (minTime != 0) {
            bMinTime.setText(Util.formatDateTime(minTime));
        }
        bMinTime.setOnClickListener(this);
        bMinTime.setOnLongClickListener(this);

        bMaxTime = findViewById(R.id.bMaxTime);
        if (maxTime != 0) {
            bMaxTime.setText(Util.formatDateTime(maxTime));
        }
        bMaxTime.setOnClickListener(this);
        bMaxTime.setOnLongClickListener(this);

        tvCount = findViewById(R.id.tvCount);
        tvSongCount = findViewById(R.id.tvSongCount);
        tvArtistCount = findViewById(R.id.tvArtistCount);

        query();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_MIN_TIME:
                    minTime = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMinTime.setText(Util.formatDateTime(minTime));
                    query();
                    break;
                case REQUEST_SELECT_MAX_TIME:
                    maxTime = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMaxTime.setText(Util.formatDateTime(maxTime));
                    query();
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        settings.edit()
                .putLong(R.string.key_log_min_timestamp, minTime)
                .putLong(R.string.key_log_max_timestamp, maxTime)
                .apply();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == bMinTime) {
            Intent intent = new Intent(this, DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.min_time);
            if (minTime != 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, minTime);
            }
            startActivityForResult(intent, REQUEST_SELECT_MIN_TIME);
        } else if (v == bMaxTime) {
            Intent intent = new Intent(this, DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.max_time);
            if (maxTime != 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, maxTime);
            }
            startActivityForResult(intent, REQUEST_SELECT_MAX_TIME);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == bMinTime) {
            minTime = 0;
            bMinTime.setText(R.string.min_time);
            query();
        } else if (v == bMaxTime) {
            maxTime = 0;
            bMaxTime.setText(R.string.max_time);
            query();
        }
        return true;
    }

    private void query() {
        LogData log = dbHelper.queryLog(
                getIntent().getBooleanExtra(EXTRA_QUERY_ARTIST, true),
                getIntent().getBooleanExtra(EXTRA_QUERY_BOOKMARKED, true),
                getIntent().getBooleanExtra(EXTRA_QUERY_ARCHIVED, true),
                getIntent().getStringExtra(EXTRA_BASE_SELECTION),
                getIntent().getStringArrayExtra(EXTRA_BASE_SELECTION_ARGS),
                getIntent().getStringExtra(EXTRA_SELECTION),
                getIntent().getStringArrayExtra(EXTRA_SELECTION_ARGS),
                minTime, maxTime);

        tvCount.setText(getString(R.string.log_count_duration,
                log.getCount(), Util.formatDuration(log.getDuration())));
        tvSongCount.setText(Utils.getCountString(this, R.plurals.songs, log.getSongCount()));
        tvArtistCount.setText(Utils.getCountString(this, R.plurals.artists, log.getArtistCount()));
    }
}
