package com.oneup.uplayer.db;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

public class Playlist implements Parcelable, BaseColumns {
    public static final String NAME = "name";
    public static final String MODIFIED = "modified";
    public static final String SONG_INDEX = "song_index";
    public static final String SONG_POSITION = "song_position";

    public static final String PLAYLIST_ID = "playlist_id";
    public static final String SONG_ID = "song_id";

    static final int DEFAULT_PLAYLIST_ID = 1;

    private long id;
    private String name;
    private long modified;
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
        out.writeLong(modified);
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

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
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

    public void reset() {
        songIndex = 0;
        songPosition = 0;
    }

    public static Playlist getDefault() {
        Playlist playlist = new Playlist();
        playlist.id = DEFAULT_PLAYLIST_ID;
        return playlist;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        @Override
        public Object createFromParcel(Parcel in) {
            Playlist playlist = new Playlist();
            playlist.id = in.readLong();
            playlist.name = in.readString();
            playlist.modified = in.readLong();
            playlist.songIndex = in.readInt();
            playlist.songPosition = in.readInt();
            return playlist;
        }

        @Override
        public Object[] newArray(int size) {
            return new Playlist[size];
        }
    };
}
