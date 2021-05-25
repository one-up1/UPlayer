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
        private static final String ARG_DATE = "date";

        private long date;

        public LogDayFragment() {
            super(LogData.TIMESTAMP);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle args = getArguments();
            if (args != null) {
                date = args.getLong(ARG_DATE);
            }
        }

        @Override
        protected ArrayList<Song> loadData() {
            return getDbHelper().queryLogDay(date,
                    getSelection(), getSelectionArgs(),
                    getOrderBy());
        }

        @Override
        protected String getActivityTitle() {
            return getString(R.string.log_title, getCount(), Util.formatDate(date));
        }

        public static Bundle getArguments(long date, String selection, String[] selectionArgs) {
            Bundle args = new Bundle();
            args.putLong(ARG_DATE, date);
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
