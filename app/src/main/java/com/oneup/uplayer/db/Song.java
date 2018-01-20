package com.oneup.uplayer.db;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.util.ArrayList;

public class Song implements MediaStore.Audio.AudioColumns, DbColumns, Parcelable {
    public static final String TABLE_NAME = "songs";

    public static final String BOOKMARKED = "bookmarked";
    public static final String TAG = "tag";

    private int id;
    private String title;
    private Artist artist;
    private long dateAdded;
    private int year;
    private int duration;

    private long lastPlayed;
    private int timesPlayed;

    private long bookmarked;
    private String tag;

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
        out.writeLong(dateAdded);
        out.writeInt(year);
        out.writeInt(duration);
        out.writeLong(lastPlayed);
        out.writeInt(timesPlayed);
        out.writeLong(bookmarked);
        out.writeString(tag);
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

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
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

    public long getBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(long bookmarked) {
        this.bookmarked = bookmarked;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Uri getContentUri() {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }

    public static int getDuration(ArrayList<Song> songs, int i) {
        int ret = 0;
        for (; i < songs.size(); i++) {
            ret += songs.get(i).getDuration();
        }
        return ret;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        @Override
        public Object createFromParcel(Parcel in) {
            Song ret = new Song();
            ret.id = in.readInt();
            ret.title = in.readString();
            ret.artist = in.readParcelable(Artist.class.getClassLoader());
            ret.dateAdded = in.readLong();
            ret.year = in.readInt();
            ret.duration = in.readInt();
            ret.lastPlayed = in.readLong();
            ret.timesPlayed = in.readInt();
            ret.bookmarked = in.readLong();
            ret.tag = in.readString();
            return ret;
        }

        @Override
        public Object[] newArray(int size) {
            return new Song[size];
        }
    };
}
