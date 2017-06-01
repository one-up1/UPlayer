package com.oneup.uplayer.db.obj;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import com.oneup.uplayer.db.DbColumns;

public class Artist implements BaseColumns, MediaStore.Audio.ArtistColumns, DbColumns, Parcelable {
    public static final String TABLE_NAME = "artists";

    private long id;
    private String artist;

    private long lastPlayed;
    private int timesPlayed;

    public Artist() {
    }

    @Override
    public String toString() {
        return artist;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(artist);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
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

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        @Override
        public Object createFromParcel(Parcel in) {
            Artist ret = new Artist();
            ret.id = in.readLong();
            ret.artist = in.readString();
            return ret;
        }

        @Override
        public Object[] newArray(int size) {
            return new Artist[size];
        }
    };
}
