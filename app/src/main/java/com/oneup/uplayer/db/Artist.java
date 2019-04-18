package com.oneup.uplayer.db;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

public class Artist implements Parcelable,
        BaseColumns, DbHelper.ArtistColumns, DbHelper.PlayedColumns {
    private long id;
    private String artist;
    private long lastAdded;
    private long archived;
    private long lastPlayed;
    private int timesPlayed;

    @Override
    public int hashCode() {
        return Long.valueOf(id).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        Artist artist = (Artist) obj;
        return id == artist.id;
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
        out.writeLong(lastAdded);
        out.writeLong(archived);
        out.writeLong(lastPlayed);
        out.writeInt(timesPlayed);
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

    public long getLastAdded() {
        return lastAdded;
    }

    public void setLastAdded(long lastAdded) {
        this.lastAdded = lastAdded;
    }

    public long getArchived() {
        return archived;
    }

    public void setArchived(long archived) {
        this.archived = archived;
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
            Artist artist = new Artist();
            artist.id = in.readLong();
            artist.artist = in.readString();
            artist.lastAdded = in.readLong();
            artist.archived = in.readLong();
            artist.lastPlayed = in.readLong();
            artist.timesPlayed = in.readInt();
            return artist;
        }

        @Override
        public Object[] newArray(int size) {
            return new Artist[size];
        }
    };
}
