package com.oneup.uplayer.activity;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.LogData;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.fragment.SongsFragment;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class LogDayActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.container);
        setContentView(container);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, LogDayFragment.newInstance(getIntent().getExtras()))
                    .commit();
        }
    }

    public static class LogDayFragment extends SongsFragment {
        private static final String ARG_ACTIVITY_TITLE = "activity_title";
        private static final String ARG_MIN_DATE = "min_date";
        private static final String ARG_MAX_DATE = "max_date";

        private String activityTitle;
        private long minDate;
        private long maxDate;

        public LogDayFragment() {
            super(LogData.TIMESTAMP);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle args = getArguments();
            if (args != null) {
                activityTitle = args.getString(ARG_ACTIVITY_TITLE);
                minDate = args.getLong(ARG_MIN_DATE);
                maxDate = args.getLong(ARG_MAX_DATE);
            }
        }

        @Override
        protected ArrayList<Song> loadData() {
            return getDbHelper().queryLogDay(minDate, maxDate,
                    getSelection(), getSelectionArgs(),
                    getOrderBy());
        }

        @Override
        protected String getActivityTitle() {
            return activityTitle == null ? super.getActivityTitle() : activityTitle;
        }

        public static Bundle getArguments(String activityTitle, long minDate, long maxDate,
                                          String selection, String[] selectionArgs) {
            Bundle args = new Bundle();
            args.putString(ARG_ACTIVITY_TITLE, activityTitle);
            args.putLong(ARG_MIN_DATE, minDate);
            args.putLong(ARG_MAX_DATE, maxDate);
            args.putString(ARG_SELECTION, selection);
            args.putStringArray(ARG_SELECTION_ARGS, selectionArgs);
            args.putBoolean(ARG_SORT_DESC, true);
            return args;
        }

        public static LogDayFragment newInstance(Bundle args) {
            LogDayFragment fragment = new LogDayFragment();
            fragment.setArguments(args);
            return fragment;
        }
    }
}
