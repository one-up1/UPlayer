package com.oneup.uplayer.db;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.SpannableString;

import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class Song implements Parcelable,
        DbHelper.SongColumns, DbHelper.StatColumns {
    private long id;
    private String title;
    private long artistId;
    private String artist;
    private long duration;
    private int year;
    private long added;
    private String tag;
    private long bookmarked;
    private long archived;
    private long lastPlayed;
    private int timesPlayed;
    private String comments;

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

        Song song = (Song) obj;
        return id == song.id;
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
        out.writeLong(archived);
        out.writeLong(lastPlayed);
        out.writeInt(timesPlayed);
        out.writeString(comments);
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

    public SpannableString getStyledTitle() {
        return Util.getStyledText(title, bookmarked, archived, timesPlayed);
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

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Uri getContentUri() {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
    }

    public static long getDuration(ArrayList<Song> songs, int index) {
        long duration = 0;
        for (; index < songs.size(); index++) {
            duration += songs.get(index).duration;
        }
        return duration;
    }

    public static final Parcelable.Creator<?> CREATOR = new Parcelable.Creator<Song>() {

        @Override
        public Song createFromParcel(Parcel in) {
            Song song = new Song();
            song.id = in.readLong();
            song.title = in.readString();
            song.artistId = in.readLong();
            song.artist = in.readString();
            song.duration = in.readLong();
            song.year = in.readInt();
            song.added = in.readLong();
            song.tag = in.readString();
            song.bookmarked = in.readLong();
            song.archived = in.readLong();
            song.lastPlayed = in.readLong();
            song.timesPlayed = in.readInt();
            song.comments = in.readString();
            return song;
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}
