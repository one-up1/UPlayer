package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.LogData;
import com.oneup.uplayer.fragment.ListFragment;
import com.oneup.uplayer.util.Settings;
import com.oneup.uplayer.util.Util;
import com.oneup.util.Utils;

import java.util.ArrayList;

public class LogActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnLongClickListener {
    public static final String EXTRA_TITLE = "com.oneup.extra.TITLE";

    public static final String EXTRA_QUERY_ARTIST = "com.oneup.extra.QUERY_ARTIST";
    public static final String EXTRA_SELECTION = "com.oneup.extra.SELECTION";
    public static final String EXTRA_SELECTION_ARGS = "com.oneup.extra.SELECTION_ARGS";

    private static final int REQUEST_SELECT_MIN_DATE = 1;
    private static final int REQUEST_SELECT_MAX_DATE = 2;

    private Button bMinDate;
    private Button bMaxDate;
    private LogFragment logFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        if (getIntent().hasExtra(EXTRA_TITLE)) {
            setTitle(getIntent().getCharSequenceExtra(EXTRA_TITLE));
        }

        Settings settings = Settings.get(this);
        long minDate = settings.getLong(R.string.key_log_min_date, 0);
        long maxDate = settings.getLong(R.string.key_log_max_date, 0);

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

        logFragment = (LogFragment) getSupportFragmentManager().findFragmentById(R.id.logFragment);
        logFragment.setMinDate(minDate);
        logFragment.setMaxDate(maxDate);
        logFragment.setSelection(getIntent().getStringExtra(EXTRA_SELECTION));
        logFragment.setSelectionArgs(getIntent().getStringArrayExtra(EXTRA_SELECTION_ARGS));
        logFragment.setArtist(getIntent().getBooleanExtra(EXTRA_QUERY_ARTIST, true));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_MIN_DATE:
                    long minDate = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMinDate.setText(Util.formatDate(minDate));
                    logFragment.setMinDate(minDate);
                    logFragment.reloadData();
                    break;
                case REQUEST_SELECT_MAX_DATE:
                    long maxDate = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMaxDate.setText(Util.formatDate(maxDate));
                    logFragment.setMaxDate(maxDate);
                    logFragment.reloadData();
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == bMinDate) {
            selectDate(R.string.min_date, logFragment.getMinDate(), REQUEST_SELECT_MIN_DATE);
        } else if (v == bMaxDate) {
            selectDate(R.string.max_date, logFragment.getMaxDate(), REQUEST_SELECT_MAX_DATE);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == bMinDate) {
            bMinDate.setText(R.string.min_date);
            logFragment.setMinDate(0);
            logFragment.reloadData();
        } else if (v == bMaxDate) {
            bMaxDate.setText(R.string.max_date);
            logFragment.setMaxDate(0);
            logFragment.reloadData();
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

    public static class LogFragment extends ListFragment<LogData> {
        private long minDate;
        private long maxDate;
        private String selection;
        private String[] selectionArgs;
        private boolean artist;

        public LogFragment() {
            super(R.layout.list_item_log, 0, 0, 0, 0, null, null);
        }

        @Override
        public void onResume() {
            super.onResume();
            reloadData();
        }

        @Override
        public void onDestroy() {
            Settings.get(getActivity()).edit()
                    .putLong(R.string.key_log_min_date, minDate)
                    .putLong(R.string.key_log_max_date, maxDate)
                    .apply();
            super.onDestroy();
        }

        @Override
        protected ArrayList<LogData> loadData() {
            return getDbHelper().queryLog(minDate, maxDate, selection, selectionArgs);
        }

        @Override
        protected void setListItemContent(View rootView, int position, LogData log) {
            super.setListItemContent(rootView, position, log);

            TextView tvDate = rootView.findViewById(R.id.tvDate);
            if (log.getDate() == 0) {
                tvDate.setVisibility(View.GONE);
            } else {
                tvDate.setText(Util.formatDate(log.getDate()));
                tvDate.setVisibility(View.VISIBLE);
            }

            TextView tvCount = rootView.findViewById(R.id.tvCount);
            if (log.getCount() > 0) {
                String count = log.getCount() + " (" +
                        Util.formatDuration(log.getDuration()) + ")";
                if (log.getTotal() != null) {
                    count += " (" + Util.formatPercent(log.getDuration(),
                            log.getTotal().getDuration()) + ")";
                }
                tvCount.setText(count);
                tvCount.setVisibility(View.VISIBLE);
            } else {
                tvCount.setVisibility(View.GONE);
            }

            TextView tvSongCount = rootView.findViewById(R.id.tvSongCount);
            if (log.getSongCount() > 1) {
                String count = Utils.getCountString(getActivity(),
                        R.plurals.songs, log.getSongCount());
                if (log.getSongCount() != log.getCount()) {
                    count += " (" + Util.formatFraction(
                            log.getCount(), log.getSongCount()) + ")";
                }
                tvSongCount.setText(count);
                tvSongCount.setVisibility(View.VISIBLE);
            } else {
                tvSongCount.setVisibility(View.GONE);
            }

            TextView tvArtistCount = rootView.findViewById(R.id.tvArtistCount);
            if (artist && log.getArtistCount() > 1) {
                String count = Utils.getCountString(getActivity(),
                        R.plurals.artists, log.getArtistCount());
                if (log.getArtistCount() != log.getSongCount()) {
                    count += " (" + Util.formatFraction(
                            log.getSongCount(), log.getArtistCount()) + ")";
                }
                tvArtistCount.setText(count);
                tvArtistCount.setVisibility(View.VISIBLE);
            } else {
                tvArtistCount.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onListItemClick(int position, LogData logData) {
            if (position != 0) {
                startActivity(new Intent(getActivity(), LogDayActivity.class)
                        .putExtras(LogDayActivity.LogDayFragment.getArguments(
                                logData.getDate(), selection, selectionArgs)));
            }
        }

        public long getMinDate() {
            return minDate;
        }

        public void setMinDate(long minDate) {
            this.minDate = minDate;
        }

        public long getMaxDate() {
            return maxDate;
        }

        public void setMaxDate(long maxDate) {
            this.maxDate = maxDate;
        }

        @Override
        public void setSelection(String selection) {
            this.selection = selection;
        }

        @Override
        public void setSelectionArgs(String[] selectionArgs) {
            this.selectionArgs = selectionArgs;
        }

        public void setArtist(boolean artist) {
            this.artist = artist;
        }
    }
}
