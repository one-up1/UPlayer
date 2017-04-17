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

    public Artist(long id, String artist) {
        this.id = id;
        this.artist = artist;
    }

    private Artist(Parcel in) {
        id = in.readLong();
        artist = in.readString();
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

    public String getArtist() {
        return artist;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        @Override
        public Object createFromParcel(Parcel in) {
            return new Artist(in);
        }

        @Override
        public Object[] newArray(int size) {
            return new Artist[size];
        }
    };
}
