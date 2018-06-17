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
    private int songCount;
    private long songsDuration;
    private int artistCount;
    private int songsPlayed;
    private int artistsPlayed;
    private int songsBookmarked;
    private int artistsBookmarked;
    private int songsTagged;
    private int artistsTagged;
    private long lastAdded;
    private long lastPlayed;
    private int timesPlayed;
    private long playedDuration;

    Stats() {
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

    void setSongsPlayed(int songsPlayed) {
        this.songsPlayed = songsPlayed;
    }

    void setArtistsPlayed(int artistsPlayed) {
        this.artistsPlayed = artistsPlayed;
    }

    void setSongsBookmarked(int songsBookmarked) {
        this.songsBookmarked = songsBookmarked;
    }

    void setArtistsBookmarked(int artistsBookmarked) {
        this.artistsBookmarked = artistsBookmarked;
    }

    void setSongsTagged(int songsTagged) {
        this.songsTagged = songsTagged;
    }

    void setArtistsTagged(int artistsTagged) {
        this.artistsTagged = artistsTagged;
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

        String sSongCount = Integer.toString(songCount);
        if (songCount > 0) {
            sSongCount += "\n" + Util.formatDuration(songsDuration);
        }
        addDialogRow(context, grid, R.string.stats_count, sSongCount, artistCount == 0 ? null
                : formatCountAvg(context, songCount, artistCount));

        if (songCount > 0) {
            addDialogRow(context, grid, R.string.stats_played,
                    songsPlayed, artistsPlayed);

            addDialogRow(context, grid, R.string.stats_unplayed,
                    songCount - songsPlayed, artistCount - artistsPlayed);

            addDialogRow(context, grid, R.string.stats_bookmarked,
                    songsBookmarked, artistsBookmarked);

            addDialogRow(context, grid, R.string.stats_unbookmarked,
                    songCount - songsBookmarked, artistCount - artistsBookmarked);

            addDialogRow(context, grid, R.string.stats_tagged,
                    songsTagged, artistsTagged);

            addDialogRow(context, grid, R.string.stats_untagged,
                    songCount - songsTagged, artistCount - artistsTagged);
        }

        addDialogRow(context, grid, R.string.stats_last_added, lastAdded);
        addDialogRow(context, grid, R.string.stats_last_played, lastPlayed);

        if (timesPlayed > 0) {
            addDialogRow(context, grid, R.string.stats_times_played,
                    Integer.toString(timesPlayed) +
                            " (" + Util.formatDuration(playedDuration) + ")", null);
        }

        if (songsPlayed > 0) {
            addDialogRow(context, grid, R.string.stats_avg_times_played,
                    formatAvgTimesPlayed(timesPlayed, songsPlayed),
                    artistCount == 0 ? null : formatAvgTimesPlayed(timesPlayed, artistsPlayed));
        }
    }

    private String formatCountAvg(Context context, int countSongs, int countArtists) {
        String value = formatCount(countArtists, artistCount);
        if (countArtists > 1) {
            value += "\n" + context.getString(R.string.avg_songs,
                    Util.formatFraction(countSongs, countArtists));
        }
        return value;
    }

    private void addDialogRow(Context context, GridLayout grid, int labelId,
                              int countSongs, int countArtists) {
        addDialogRow(context, grid, labelId, formatCount(countSongs, songCount),
                artistCount == 0 ? null : formatCountAvg(context, countSongs, countArtists));
    }

    private String formatAvgTimesPlayed(int timesPlayed, int total) {
        return Util.formatFraction(timesPlayed, total) +
                "\n" + Util.formatDuration(playedDuration / total);
    }

    private static void addDialogRow(Context context, GridLayout grid, int labelId,
                                     String value1, String value2) {
        addDialogColumn(context, grid, context.getString(labelId), false);
        addDialogColumn(context, grid, value1, value2 == null);
        if (value2 != null) {
            addDialogColumn(context, grid, value2, false);
        }
    }

    private static void addDialogRow(Context context, GridLayout grid, int labelId, long time) {
        if (time > 0) {
            addDialogRow(context, grid, labelId,
                    Util.formatDateTimeAgo(context, time, false), null);
        }
    }

    private static void addDialogColumn(Context context, GridLayout grid,
                                        String text, boolean span) {
        TextView view = new TextView(context);
        view.setBackground(ContextCompat.getDrawable(context, R.drawable.border));

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

        view.setText(text);
        view.setTextColor(ContextCompat.getColor(context, R.color.stats_text));
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                context.getResources().getDimensionPixelSize(R.dimen.stats_text_size));

        grid.addView(view);
    }

    private static String formatCount(int count, int total) {
        String value = Integer.toString(count);
        if (count > 0 && count < total) {
            value += " (" + Util.formatPercent(count, total) + ")";
        }
        return value;
    }
}
