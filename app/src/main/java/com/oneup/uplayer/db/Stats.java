package com.oneup.uplayer.db;

import android.app.AlertDialog;
import android.content.Context;
import android.support.v7.widget.GridLayout;
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

    void setSongsUnplayed(int songsUnplayed) {
        this.songsUnplayed = songsUnplayed;
    }

    void setSongsTagged(int songsTagged) {
        this.songsTagged = songsTagged;
    }

    void setSongsUntagged(int songsUntagged) {
        this.songsUntagged = songsUntagged;
    }

    void setTimesPlayed(int timesPlayed) {
        this.timesPlayed = timesPlayed;
    }

    void setPlayedDuration(long playedDuration) {
        this.playedDuration = playedDuration;
    }

    public void showDialog(Context context, Artist artist) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(artist == null ?
                        context.getString(R.string.statistics) : artist.getArtist())
                .setView(R.layout.dialog_stats)
                .setPositiveButton(R.string.ok, null)
                .show();

        //TODO: (artist) stats, plurals, string resources, fix division by zero, catch exceptions on query.
        //Util.formatDateTime() auto returns NA when 0?
        //Query unplayed/untagged or just total - played?
        //Dialog layout, padding, etc.

        GridLayout grid = dialog.findViewById(R.id.grid);

        addDialogRow(context, grid, R.string.song_count, songCount +
                " (" + Util.formatDuration(songsDuration) + ")");

        if (artist == null) {
            int avgSongs = (int) Math.round((double) songCount / artistCount);
            addDialogRow(context, grid, R.string.artist_count, artistCount +
                    " (avg " + avgSongs + " songs)");
        }

        addDialogRow(context, grid, R.string.songs_played, songsPlayed +
                " (" + Util.formatPercent((double) songsPlayed / songCount) + ")");

        addDialogRow(context, grid, R.string.songs_unplayed, songsUnplayed +
                " (" + Util.formatPercent((double) songsUnplayed / songCount) + ")");

        addDialogRow(context, grid, R.string.songs_tagged, songsTagged +
                " (" + Util.formatPercent((double) songsTagged / songCount) + ")");

        addDialogRow(context, grid, R.string.songs_untagged, songsUntagged +
                " (" + Util.formatPercent((double) songsUntagged / songCount) + ")");

        if (artist != null) {
            addDialogRow(context, grid, R.string.last_song_added,
                    Util.formatDateTimeAgo(artist.getLastSongAdded()));

            addDialogRow(context, grid, R.string.edit_last_played,
                    Util.formatDateTimeAgo(artist.getLastPlayed()));
        }

        addDialogRow(context, grid, R.string.edit_times_played, timesPlayed +
                " (" + Util.formatDuration(playedDuration) + ")");

        addDialogRow(context, grid, R.string.avg_times_played,
                Math.round((double) timesPlayed / playedDuration) +
                        " (" + Util.formatDuration(playedDuration / songsPlayed));
    }

    private static void addDialogRow(Context context, GridLayout grid, int labelId, Object value) {
        addDialogColumn(context, grid, context.getString(labelId));
        addDialogColumn(context, grid, value.toString());
    }

    private static void addDialogColumn(Context context, GridLayout grid, String text) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextAppearance(android.R.style.TextAppearance_Medium);
        view.setPadding(10, 10, 10, 10);
        grid.addView(view);
    }
}