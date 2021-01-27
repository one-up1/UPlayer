package com.oneup.uplayer.db;

import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class LogData implements DbHelper.LogColumns {
    private int count;
    private int songCount;
    private int artistCount;
    private long duration;

    private LogData total;
    private ArrayList<LogData> days;
    private long date;

    public LogData() {
    }

    @Override
    public String toString() {
        String s = count + " (" + Util.formatDuration(duration) + ")";
        if (total != null) {
            s += " (" + Util.formatPercent(duration, total.duration) + ")";
        }
        if (date != 0) {
            s = Util.formatDate(date) + "\n" + s;
        }
        return s;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    public int getArtistCount() {
        return artistCount;
    }

    public void setArtistCount(int artistCount) {
        this.artistCount = artistCount;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public LogData getTotal() {
        return total;
    }

    public void setTotal(LogData total) {
        this.total = total;
    }

    public ArrayList<LogData> getDays() {
        return days;
    }

    public void setDays(ArrayList<LogData> days) {
        this.days = days;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
