package com.oneup.uplayer.db;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

public class Playlist implements Parcelable, BaseColumns {
    static final String NAME = "name";
    static final String SONG_INDEX = "song_index";
    static final String SONG_POSITION = "song_position";

    static final String PLAYLIST_ID = "playlist_id";
    static final String SONG_ID = DbHelper.SongColumns.SONG_ID;

    static final int DEFAULT_PLAYLIST_ID = 1;

    private long id;
    private String name;
    private int songIndex;
    private int songPosition;

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

        Playlist playlist = (Playlist) obj;
        return id == playlist.id;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(name);
        out.writeInt(songIndex);
        out.writeInt(songPosition);
    }

    public long getId() {
        return id;
    }

    public boolean isDefault() {
        return id == DEFAULT_PLAYLIST_ID;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSongIndex() {
        return songIndex;
    }

    public void setSongIndex(int songIndex) {
        this.songIndex = songIndex;
    }

    public void incrementSongIndex() {
        songIndex++;
    }

    public void decrementSongIndex() {
        songIndex--;
    }

    public int getSongPosition() {
        return songPosition;
    }

    public void setSongPosition(int songPosition) {
        this.songPosition = songPosition;
    }

    public static Playlist getDefault() {
        Playlist playlist = new Playlist();
        playlist.id = DEFAULT_PLAYLIST_ID;
        return playlist;
    }

    public static final Parcelable.Creator<?> CREATOR = new Parcelable.Creator<Playlist>() {

        @Override
        public Playlist createFromParcel(Parcel in) {
            Playlist playlist = new Playlist();
            playlist.id = in.readLong();
            playlist.name = in.readString();
            playlist.songIndex = in.readInt();
            playlist.songPosition = in.readInt();
            return playlist;
        }

        @Override
        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };
}
