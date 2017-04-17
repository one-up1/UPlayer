package com.oneup.uplayer.db.obj;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import com.oneup.uplayer.db.DbColumns;

public class Song implements MediaStore.Audio.AudioColumns, DbColumns, Parcelable {
    public static final String TABLE_NAME = "songs";

    private long id;
    private String title;
    private long artistId;
    private String artist;
    private int year;

    public Song(long id, String title, long artistId, String artist, int year) {
        this.id = id;
        this.title = title;
        this.artistId = artistId;
        this.artist = artist;
        this.year = year;
    }

    private Song(Parcel in) {
        id = in.readLong();
        title = in.readString();
        artistId = in.readLong();
        artist = in.readString();
        year = in.readInt();
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
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public long getArtistId() {
        return artistId;
    }

    public String getArtist() {
        return artist;
    }

    public int getYear() {
        return year;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        @Override
        public Object createFromParcel(Parcel in) {
            return new Song(in);
        }

        @Override
        public Object[] newArray(int size) {
            return new Song[size];
        }
    };
}
