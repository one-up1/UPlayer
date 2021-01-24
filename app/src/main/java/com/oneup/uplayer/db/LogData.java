package com.oneup.uplayer.db;

import android.database.Cursor;

import java.util.ArrayList;

public class LogData implements DbHelper.LogColumns {
    private int count;
    private ArrayList<Long> songIds;
    private ArrayList<Long> artistIds;
    private long duration;

    LogData() {
        songIds = new ArrayList<>();
        artistIds = new ArrayList<>();
    }

    public int getCount() {
        return count;
    }

    public int getSongCount() {
        return songIds.size();
    }

    public int getArtistCount() {
        return artistIds.size();
    }

    public long getDuration() {
        return duration;
    }

    void add(Cursor c) {
        count++;
        addId(songIds, c.getLong(0));
        addId(artistIds, c.getLong(1));
        duration += c.getLong(2);
    }

    private void addId(ArrayList<Long> ids, long id) {
        if (!ids.contains(id)) {
            ids.add(id);
        }
    }
}
