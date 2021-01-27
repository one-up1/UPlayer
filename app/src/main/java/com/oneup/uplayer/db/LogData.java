package com.oneup.uplayer.db;

public class LogData implements DbHelper.LogColumns {
    private int count;
    private int songCount;
    private int artistCount;
    private long duration;

    private LogData total;
    private long date;

    public LogData() {
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

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}
