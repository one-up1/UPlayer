package com.oneup.uplayer.db;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.provider.MediaStore;

public class Artist implements BaseColumns, MediaStore.Audio.ArtistColumns, DbColumns, Parcelable {
    public static final String TABLE_NAME = "artists";

    public static final String DATE_MODIFIED = "date_modified";

    private int id;
    private String artist;

    private long lastPlayed;
    private int timesPlayed;

    private long dateModified;

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
        out.writeInt(id);
        out.writeString(artist);
        out.writeLong(lastPlayed);
        out.writeInt(timesPlayed);
        out.writeLong(dateModified);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public long getDateModified() {
        return dateModified;
    }

    public void setDateModified(long dateModified) {
        this.dateModified = dateModified;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        @Override
        public Object createFromParcel(Parcel in) {
            Artist ret = new Artist();
            ret.id = in.readInt();
            ret.artist = in.readString();
            ret.lastPlayed = in.readLong();
            ret.timesPlayed = in.readInt();
            ret.dateModified = in.readLong();
            return ret;
        }

        @Override
        public Object[] newArray(int size) {
            return new Artist[size];
        }
    };
}
