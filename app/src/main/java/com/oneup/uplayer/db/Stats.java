package com.oneup.uplayer.db;

public class Stats {
    private int artistCount;
    private int songCount;
    private long songsDuration;
    private int songsPlayed;
    private int songsUnplayed;
    private int songsTagged;
    private int songsUntagged;
    private int timesPlayed;
    private long playedDuration;

    Stats() {
    }

    public int getArtistCount() {
        return artistCount;
    }

    void setArtistCount(int artistCount) {
        this.artistCount = artistCount;
    }

    public int getSongCount() {
        return songCount;
    }

    void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    public long getSongsDuration() {
        return songsDuration;
    }

    void setSongsDuration(long songsDuration) {
        this.songsDuration = songsDuration;
    }

    public int getSongsPlayed() {
        return songsPlayed;
    }

    void setSongsPlayed(int songsPlayed) {
        this.songsPlayed = songsPlayed;
    }

    public int getSongsUnplayed() {
        return songsUnplayed;
    }

    void setSongsUnplayed(int songsUnplayed) {
        this.songsUnplayed = songsUnplayed;
    }

    public int getSongsTagged() {
        return songsTagged;
    }

    void setSongsTagged(int songsTagged) {
        this.songsTagged = songsTagged;
    }

    public int getSongsUntagged() {
        return songsUntagged;
    }

    void setSongsUntagged(int songsUntagged) {
        this.songsUntagged = songsUntagged;
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
}
