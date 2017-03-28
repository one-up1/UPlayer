package com.oneup.uplayer.db;

import android.content.ContentValues;
import android.database.Cursor;

public class PlayedSong {
    static final String ID = "id";
    static final String MEDIA_ID = "media_id";
    static final String LAST_PLAYED = "last_played";
    static final String TIMES_PLAYED = "times_played";

    int id;

    private long mediaId;
    private long lastPlayed;
    private int timesPlayed;

    public PlayedSong(long mediaId) {
        this.mediaId = mediaId;
        lastPlayed = System.currentTimeMillis();
        timesPlayed = 1;
    }

    PlayedSong(Cursor c) {
        id = c.getInt(0);
        mediaId = c.getLong(1);
        lastPlayed = c.getLong(2);
        timesPlayed = c.getInt(3);
    }

    public long getMediaId() {
        return mediaId;
    }

    public void setMediaId(long mediaId) {
        this.mediaId = mediaId;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public int getTimesPlayed() {
        return timesPlayed;
    }

    public void setTimesPlayed(int timesPlayed) {
        this.timesPlayed = timesPlayed;
    }

    ContentValues getValues() {
        ContentValues ret = new ContentValues();
        ret.put(MEDIA_ID, mediaId);
        ret.put(LAST_PLAYED, lastPlayed);
        ret.put(TIMES_PLAYED, timesPlayed);
        return ret;
    }
}
