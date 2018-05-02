package com.oneup.uplayer.db;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.util.List;

public class Song implements Parcelable {
    private long id;
    private String title;
    private long artistId;
    private String artist;
    private long duration;
    private int year;
    private long added;
    private String tag;
    private long bookmarked;
    private long lastPlayed;
    private int timesPlayed;

    Song() {
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
        out.writeLong(duration);
        out.writeInt(year);
        out.writeLong(added);
        out.writeString(tag);
        out.writeLong(bookmarked);
        out.writeLong(lastPlayed);
        out.writeInt(timesPlayed);
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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getAdded() {
        return added;
    }

    public void setAdded(long added) {
        this.added = added;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public long getBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(long bookmarked) {
        this.bookmarked = bookmarked;
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

    public Uri getContentUri() {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }

    public static int getDuration(List<Song> songs, int i) {
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
            ret.id = in.readLong();
            ret.title = in.readString();
            ret.artistId = in.readLong();
            ret.artist = in.readString();
            ret.duration = in.readLong();
            ret.year = in.readInt();
            ret.added = in.readLong();
            ret.tag = in.readString();
            ret.bookmarked = in.readLong();
            ret.lastPlayed = in.readLong();
            ret.timesPlayed = in.readInt();
            return ret;
        }

        @Override
        public Object[] newArray(int size) {
            return new Song[size];
        }
    };
}
