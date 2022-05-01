package com.oneup.uplayer.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Stats;
import com.oneup.uplayer.util.Util;

public class StatisticsActivity extends AppCompatActivity {
    public static final String EXTRA_TITLE = "com.oneup.extra.TITLE";

    public static final String EXTRA_QUERY_ARTIST = "com.oneup.extra.QUERY_ARTIST";
    public static final String EXTRA_QUERY_BOOKMARKED = "com.oneup.extra.QUERY_BOOKMARKED";
    public static final String EXTRA_QUERY_ARCHIVED = "com.oneup.extra.QUERY_ARCHIVED";

    public static final String EXTRA_BASE_SELECTION = "com.oneup.extra.BASE_SELECTION";
    public static final String EXTRA_BASE_SELECTION_ARGS = "com.oneup.extra.BASE_SELECTION_ARGS";

    public static final String EXTRA_SELECTION = "com.oneup.extra.SELECTION";
    public static final String EXTRA_SELECTION_ARGS = "com.oneup.extra.SELECTION_ARGS";

    private Stats stats;

    private GridLayout grid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (getIntent().hasExtra(EXTRA_TITLE)) {
            setTitle(getIntent().getCharSequenceExtra(EXTRA_TITLE));
        }

        stats = new DbHelper(this).queryStats(
                getIntent().getBooleanExtra(EXTRA_QUERY_ARTIST, true),
                getIntent().getBooleanExtra(EXTRA_QUERY_BOOKMARKED, true),
                getIntent().getBooleanExtra(EXTRA_QUERY_ARCHIVED, true),
                getIntent().getStringExtra(EXTRA_BASE_SELECTION),
                getIntent().getStringArrayExtra(EXTRA_BASE_SELECTION_ARGS),
                getIntent().getStringExtra(EXTRA_SELECTION),
                getIntent().getStringArrayExtra(EXTRA_SELECTION_ARGS));

        grid = findViewById(R.id.grid);

        stats.getTotal().addRows(this, grid,
                R.string.stats_total, R.string.stats_rest,
                stats.hasGrandTotal() ? stats.getGrandTotal() : stats.getTotal(), true, true);
        if (stats.hasBookmarked()) {
            stats.getBookmarked().addRows(this, grid,
                    R.string.stats_bookmarked, R.string.stats_unbookmarked,
                    stats.getTotal(), true, true);
        }
        if (stats.hasArchived()) {
            stats.getArchived().addRows(this, grid,
                    R.string.stats_archived, R.string.stats_unarchived,
                    stats.getTotal(), false, true);
        }

        Stats.addRow(this, grid, R.string.stats_last_added, stats.getLastAdded());
        Stats.addRow(this, grid, R.string.stats_last_played, stats.getLastPlayed());

        if (stats.hasTimesPlayed()) {
            Stats.addRow(this, grid, R.string.stats_times_played, stats.getTimesPlayed() +
                    " (" + Util.formatDuration(stats.getPlayedDuration()) + ")", null);
        }

        if (stats.getTotal().getSongCount() > 1) {
            Stats.addRow(this, grid, R.string.stats_avg_times_played,
                    formatAvgTimesPlayed(stats.getTimesPlayed(), stats.getTotal().getSongCount()),
                    stats.getTotal().getArtistCount() > 1 ? formatAvgTimesPlayed(
                            stats.getTimesPlayed(), stats.getTotal().getArtistCount()) : null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private String formatAvgTimesPlayed(int timesPlayed, int total) {
        return Util.formatFraction(timesPlayed, total) +
                "\n" + Util.formatDuration(stats.getPlayedDuration() / total);
    }
}
