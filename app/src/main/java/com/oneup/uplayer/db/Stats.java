package com.oneup.uplayer.db;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
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
    private int songsUnplayed;
    private int songsTagged;
    private int songsUntagged;
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
        songsUnplayed = songCount - songsPlayed;
    }

    void setSongsTagged(int songsTagged) {
        this.songsTagged = songsTagged;
        songsUntagged = songCount - songsTagged;
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
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(R.layout.dialog_stats)
                .setPositiveButton(R.string.ok, null)
                .show();

        GridLayout grid = dialog.findViewById(R.id.grid);

        addDialogRow(context, grid, R.string.stats_song_count, songCount +
                " (" + Util.formatDuration(songsDuration) + ")");

        if (artistCount > 0) {
            addDialogRow(context, grid, R.string.stats_artist_count, artistCount +
                    " (avg " + Util.formatFraction(songCount, artistCount) + " songs)");
        }

        if (songCount > 0) {
            addDialogRow(context, grid, R.string.stats_songs_played, songsPlayed +
                    " (" + Util.formatPercent(songsPlayed, songCount) + ")");

            addDialogRow(context, grid, R.string.stats_songs_unplayed, songsUnplayed +
                    " (" + Util.formatPercent(songsUnplayed, songCount) + ")");

            addDialogRow(context, grid, R.string.stats_songs_tagged, songsTagged +
                    " (" + Util.formatPercent(songsTagged, songCount) + ")");

            addDialogRow(context, grid, R.string.stats_songs_untagged, songsUntagged +
                    " (" + Util.formatPercent(songsUntagged, songCount) + ")");
        }

        if (lastAdded > 0) {
            addDialogRow(context, grid, R.string.stats_last_added,
                    Util.formatDateTimeAgo(lastAdded));
        }

        if (lastPlayed > 0) {
            addDialogRow(context, grid, R.string.stats_last_played,
                    Util.formatDateTimeAgo(lastPlayed));
        }

        if (timesPlayed > 0) {
            addDialogRow(context, grid, R.string.stats_times_played, timesPlayed +
                    " (" + Util.formatDuration(playedDuration) + ")");
        }

        if (songsPlayed > 0) {
            addDialogRow(context, grid, R.string.stats_avg_times_played,
                    Util.formatFraction(timesPlayed, songsPlayed) +
                    " (" + Util.formatDuration(playedDuration / songsPlayed) + ")");
        }
    }

    private static void addDialogRow(Context context, GridLayout grid, int labelId, Object value) {
        addDialogColumn(context, grid, context.getString(labelId));
        addDialogColumn(context, grid, value.toString());
    }

    private static void addDialogColumn(Context context, GridLayout grid, String text) {
        TextView view = new TextView(context);
        int padding = context.getResources().getDimensionPixelSize(R.dimen.dialog_text_padding);
        view.setPadding(padding, padding, padding, padding);
        view.setText(text);
        view.setTextColor(Color.BLACK);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        grid.addView(view);
    }
}
