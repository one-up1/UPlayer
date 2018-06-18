package com.oneup.uplayer.db;

import android.app.AlertDialog;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayout;
import android.util.TypedValue;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.util.Util;

public class Stats {
    private Total total;
    private Total bookmarked;
    private Total tagged;
    private Total played;

    private long lastAdded;
    private long lastPlayed;
    private int timesPlayed;
    private long playedDuration;

    Stats() {
        total = new Total();
        bookmarked = new Total();
        tagged = new Total();
        played = new Total();
    }

    Total getTotal() {
        return total;
    }

    Total getBookmarked() {
        return bookmarked;
    }

    Total getTagged() {
        return tagged;
    }

    Total getPlayed() {
        return played;
    }

    void setLastAdded(long lastAdded) {
        this.lastAdded = lastAdded;
    }

    void setLastPlayed(long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    void setTimesPlayed(int timesPlayed) {
        this.timesPlayed = timesPlayed;
    }

    void setPlayedDuration(long playedDuration) {
        this.playedDuration = playedDuration;
    }

    public void showDialog(Context context, String title) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context)
                .setView(R.layout.dialog_stats)
                .setPositiveButton(R.string.ok, null);
        if (title != null) {
            dialogBuilder.setTitle(title);
        }
        AlertDialog dialog = dialogBuilder.show();
        GridLayout grid = dialog.findViewById(R.id.grid);

        total.addRows(context, grid, R.string.stats_count, 0);
        bookmarked.addRows(context, grid, R.string.stats_bookmarked, R.string.stats_unbookmarked);
        tagged.addRows(context, grid, R.string.stats_tagged, R.string.stats_untagged);
        played.addRows(context, grid, R.string.stats_played, R.string.stats_unplayed);

        addRow(context, grid, R.string.stats_last_added, lastAdded);
        addRow(context, grid, R.string.stats_last_played, lastPlayed);

        if (timesPlayed > 0) {
            addRow(context, grid, R.string.stats_times_played,
                    Integer.toString(timesPlayed) +
                            " (" + Util.formatDuration(playedDuration) + ")", null);
        }

        if (played.songCount > 0) {
            addRow(context, grid, R.string.stats_avg_times_played,
                    formatAvgTimesPlayed(timesPlayed, played.songCount),
                    played.artistCount == 0 ? null
                            : formatAvgTimesPlayed(timesPlayed, played.artistCount));
        }
    }

    private String formatAvgTimesPlayed(int timesPlayed, int total) {
        return Util.formatFraction(timesPlayed, total) +
                "\n" + Util.formatDuration(playedDuration / total);
    }

    private static void addRow(Context context, GridLayout grid, int labelId,
                               String songs, String artists) {
        addColumn(context, grid, context.getString(labelId), false);
        addColumn(context, grid, songs, artists == null);
        if (artists != null) {
            addColumn(context, grid, artists, false);
        }
    }

    private static void addRow(Context context, GridLayout grid, int labelId, long time) {
        if (time > 0) {
            addRow(context, grid, labelId,
                    Util.formatDateTimeAgo(context, time, false), null);
        }
    }

    private static void addColumn(Context context, GridLayout grid,
                                  String text, boolean span) {
        TextView view = new TextView(context);

        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
        layoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED,
                span ? 2 : 1, GridLayout.FILL);
        layoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL);
        view.setLayoutParams(layoutParams);

        int paddingHorizontal = context.getResources().getDimensionPixelSize(
                R.dimen.stats_text_padding);
        int paddingVertical = context.getResources().getDimensionPixelSize(
                R.dimen.stats_text_padding);
        view.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

        view.setBackground(ContextCompat.getDrawable(context, R.drawable.border));

        view.setText(text);
        view.setTextColor(ContextCompat.getColor(context, R.color.stats_text));
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimensionPixelSize(R.dimen.stats_text_size));

        grid.addView(view);
    }

    private static String formatCount(int count, int total) {
        String s = Integer.toString(count);
        if (count > 0 && count < total) {
            s += " (" + Util.formatPercent(count, total) + ")";
        }
        return s;
    }

    public class Total {
        private int songCount;
        private long songsDuration;
        private int artistCount;

        private Total() {
        }

        void setSongCount(int songCount) {
            this.songCount = songCount;
        }

        void setSongsDuration(long songsDuration) {
            this.songsDuration = songsDuration;
        }

        void setArtistCount(int artistCount) {
            this.artistCount = artistCount;
        }

        private void addRows(Context context, GridLayout grid,
                             int labelId, int remainderLabelId) {
            addRow(context, grid, labelId);
            if (this != total) {
                Total remainder = new Total();
                remainder.songCount = total.songCount - songCount;
                remainder.songsDuration = total.songsDuration - songsDuration;
                remainder.artistCount = total.artistCount - artistCount;
                remainder.addRow(context, grid, remainderLabelId);
            }
        }

        private void addRow(Context context, GridLayout grid, int labelId) {
            String songs = formatCount(songCount, total.songCount);
            if (songsDuration > 0) {
                songs += "\n" + Util.formatDuration(songsDuration);
            }

            String artists;
            if (total.artistCount == 0) {
                artists = null;
            } else {
                artists = formatCount(artistCount, total.artistCount);
                if (artistCount > 1) {
                    artists += "\n" + context.getString(R.string.stats_avg_songs,
                            Util.formatFraction(songCount, artistCount));
                }
            }

            Stats.addRow(context, grid, labelId, songs, artists);
        }
    }
}
