package com.oneup.uplayer.db;

import android.content.Context;
import android.support.v7.widget.GridLayout;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.util.Util;

public class Stats {
    private Total grandTotal;
    private Total total;
    private Total bookmarked;
    private Total archived;

    private long lastAdded;
    private long lastPlayed;
    private int timesPlayed;
    private long playedDuration;

    Stats() {
        total = new Total();
    }

    public boolean hasGrandTotal() {
        return grandTotal != null;
    }

    public Total getGrandTotal() {
        if (grandTotal == null) {
            grandTotal = new Total();
        }
        return grandTotal;
    }

    public Total getTotal() {
        return total;
    }

    public boolean hasBookmarked() {
        return bookmarked != null;
    }

    public Total getBookmarked() {
        if (bookmarked == null) {
            bookmarked = new Total();
        }
        return bookmarked;
    }

    public boolean hasArchived() {
        return archived != null;
    }

    public Total getArchived() {
        if (archived == null) {
            archived = new Total();
        }
        return archived;
    }

    public long getLastAdded() {
        return lastAdded;
    }

    void setLastAdded(long lastAdded) {
        this.lastAdded = lastAdded;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    void setLastPlayed(long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public boolean hasTimesPlayed() {
        return timesPlayed != 0;
    }

    public int getTimesPlayed() {
        return timesPlayed;
    }

    void setTimesPlayed(int timesPlayed) {
        this.timesPlayed = timesPlayed;
    }

    public long getPlayedDuration() {
        return playedDuration;
    }

    void setPlayedDuration(long playedDuration) {
        this.playedDuration = playedDuration;
    }

    public static void addRow(Context context, GridLayout grid, int labelId,
                               String songs, String artists) {
        addColumn(context, grid, context.getString(labelId), false, true);
        addColumn(context, grid, songs, artists == null, false);
        if (artists != null) {
            addColumn(context, grid, artists, false, false);
        }
    }

    public static void addRow(Context context, GridLayout grid, int labelId, long time) {
        if (time != 0) {
            addRow(context, grid, labelId, Util.formatDateTimeAgo(time), null);
        }
    }

    private static void addColumn(Context context, GridLayout grid,
                                  String text, boolean span, boolean label) {
        TextView view = new TextView(context);

        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams();
        layoutParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED,
                span ? 2 : 1, GridLayout.FILL);
        layoutParams.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL);
        view.setLayoutParams(layoutParams);

        int paddingHorizontal = context.getResources().getDimensionPixelSize(
                R.dimen.stats_text_padding_horizontal);
        int paddingVertical = context.getResources().getDimensionPixelSize(
                R.dimen.stats_text_padding_vertical);
        view.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

        if (label) {
            view.setTextAppearance(android.R.style.TextAppearance_Medium);
        } else {
            view.setGravity(Gravity.CENTER_VERTICAL);
        }
        view.setText(text);

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

        public int getSongCount() {
            return songCount;
        }

        void setSongCount(int songCount) {
            this.songCount = songCount;
        }

        void setSongsDuration(long songsDuration) {
            this.songsDuration = songsDuration;
        }

        public int getArtistCount() {
            return artistCount;
        }

        void setArtistCount(int artistCount) {
            this.artistCount = artistCount;
        }

        public void addRows(Context context, GridLayout grid,
                             int labelId, int remainderLabelId,
                             Total grandTotal, boolean avg, boolean remainderAvg) {
            addRow(context, grid, labelId, grandTotal, avg);
            if (this != grandTotal) {
                Total remainder = new Total();
                remainder.songCount = grandTotal.songCount - songCount;
                remainder.songsDuration = grandTotal.songsDuration - songsDuration;
                remainder.artistCount = grandTotal.artistCount - artistCount;
                remainder.addRow(context, grid, remainderLabelId, grandTotal, remainderAvg);
            }
        }

        private void addRow(Context context, GridLayout grid, int labelId,
                            Total grandTotal, boolean avg) {
            String songs = formatCount(songCount, grandTotal.songCount);
            if (songsDuration > 0) {
                songs += "\n" + Util.formatDuration(songsDuration);
            }

            String artists;
            if (grandTotal.artistCount > 1) {
                artists = formatCount(artistCount, grandTotal.artistCount);
                if (avg && artistCount > 1) {
                    artists += "\n" + context.getString(R.string.stats_avg,
                            Util.formatFraction(songCount, artistCount));
                }
            } else {
                artists = null;
            }

            Stats.addRow(context, grid, labelId, songs, artists);
        }
    }
}
