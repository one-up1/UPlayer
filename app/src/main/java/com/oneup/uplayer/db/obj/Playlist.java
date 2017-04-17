package com.oneup.uplayer.db.obj;

import android.os.Parcel;
import android.os.Parcelable;

public class Playlist implements Parcelable {
    private long id;
    private String name;

    public Playlist(long id, String name) {
        this.id = id;
        this.name = name;
    }

    private Playlist(Parcel in) {
        id = in.readLong();
        name = in.readString();
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
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        @Override
        public Object createFromParcel(Parcel in) {
            return new Playlist(in);
        }

        @Override
        public Object[] newArray(int size) {
            return new Playlist[size];
        }
    };
}
