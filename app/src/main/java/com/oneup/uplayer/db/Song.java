package com.oneup.uplayer.db;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

public class Song implements MediaStore.Audio.AudioColumns, DbColumns, Parcelable {
    public static final String TABLE_NAME = "songs";

    public static final String STARRED = "starred";

    private int id;
    private String title;
    private Artist artist;
    private int year;

    private long lastPlayed;
    private int timesPlayed;

    private long starred;

    private int duration;

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
        out.writeInt(id);
        out.writeString(title);
        out.writeParcelable(artist, flags);
        out.writeInt(year);
        out.writeLong(lastPlayed);
        out.writeInt(timesPlayed);
        out.writeLong(starred);
        out.writeInt(duration);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
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

    public long getStarred() {
        return starred;
    }

    public void setStarred(long starred) {
        this.starred = starred;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Uri getContentUri() {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        @Override
        public Object createFromParcel(Parcel in) {
            Song ret = new Song();
            ret.id = in.readInt();
            ret.title = in.readString();
            ret.artist = in.readParcelable(Artist.class.getClassLoader());
            ret.year = in.readInt();
            ret.lastPlayed = in.readLong();
            ret.timesPlayed = in.readInt();
            ret.starred = in.readLong();
            ret.duration = in.readInt();
            return ret;
        }

        @Override
        public Object[] newArray(int size) {
            return new Song[size];
        }
    };
}
