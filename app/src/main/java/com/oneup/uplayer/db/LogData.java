package com.oneup.uplayer.db;

import android.os.Parcel;
import android.os.Parcelable;

public class LogData implements Parcelable, DbHelper.LogColumns {
    private int count;
    private int songCount;
    private int artistCount;
    private long duration;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(count);
        out.writeInt(songCount);
        out.writeInt(artistCount);
        out.writeLong(duration);
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    public int getArtistCount() {
        return artistCount;
    }

    public void setArtistCount(int artistCount) {
        this.artistCount = artistCount;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public static final Parcelable.Creator<?> CREATOR = new Parcelable.Creator<LogData>() {

        @Override
        public LogData createFromParcel(Parcel in) {
            LogData log = new LogData();
            log.count = in.readInt();
            log.songCount = in.readInt();
            log.artistCount = in.readInt();
            log.duration = in.readLong();
            return log;
        }

        @Override
        public LogData[] newArray(int size) {
            return new LogData[size];
        }
    };
}
