package com.oneup.uplayer.db.obj;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.oneup.uplayer.db.DbColumns;

public class Song implements MediaStore.Audio.AudioColumns, DbColumns, Parcelable {
    public static final String TABLE_NAME = "songs";

    public static final String STARRED = "starred";

    private long id;
    private String title;
    private long artistId;
    private String artist;
    private int year;

    private long lastPlayed;
    private int timesPlayed;
    private boolean starred;

    public Song() {
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(title);
        out.writeLong(artistId);
        out.writeString(artist);
        out.writeInt(year);
        out.writeInt(starred ? 1 : 0);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getArtistId() {
        return artistId;
    }

    public void setArtistId(long artistId) {
        this.artistId = artistId;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
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

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        @Override
        public Object createFromParcel(Parcel in) {
            Song ret = new Song();
            ret.id = in.readLong();
            ret.title = in.readString();
            ret.artistId = in.readLong();
            ret.artist = in.readString();
            ret.year = in.readInt();
            ret.starred = in.readInt() == 1;
            return ret;
        }

        @Override
        public Object[] newArray(int size) {
            return new Song[size];
        }
    };
}
