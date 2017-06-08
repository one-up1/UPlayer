package com.oneup.uplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.obj.Song;

public class SongsListView extends ListView implements AdapterView.OnItemLongClickListener {
    private static final String TAG = "UPlayer";

    private OnDataSetChangedListener onDataSetChangedListener;

    public SongsListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnItemLongClickListener(this);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        DbOpenHelper dbOpenHelper = new DbOpenHelper(getContext());
        Song song = (Song) getItemAtPosition(position);

        try (SQLiteDatabase db = dbOpenHelper.getWritableDatabase()) {
            String selection = Song._ID + "=?";
            String[] selectionArgs = new String[]{Long.toString(song.getId())};
            ContentValues values = new ContentValues();
            try (Cursor c = db.query(Song.TABLE_NAME, new String[]{Song.STARRED},
                    selection, selectionArgs, null, null, null)) {
                if (c.moveToNext()) {
                    if (c.isNull(0)) {
                        Log.d(TAG, "Starring song");
                        song.setStarred(System.currentTimeMillis());
                        values.put(Song.STARRED, song.getStarred());
                    } else {
                        Log.d(TAG, "Unstarring song");
                        song.setStarred(0);
                        values.putNull(Song.STARRED);
                    }
                    db.update(Song.TABLE_NAME, values, selection, selectionArgs);
                    Log.d(TAG, "Song updated");
                } else {
                    Log.d(TAG, "Starring song");
                    song.setStarred(System.currentTimeMillis());
                    values.put(Song._ID, song.getId());
                    values.put(Song.TITLE, song.getTitle());
                    if (song.getArtistId() > 0) {
                        values.put(Song.ARTIST_ID, song.getArtistId());
                        values.put(Song.ARTIST, song.getArtist());
                    }
                    if (song.getYear() > 0) {
                        values.put(Song.YEAR, song.getYear());
                    }
                    values.put(Song.STARRED, song.getStarred());
                    db.insert(Song.TABLE_NAME, null, values);
                    Log.d(TAG, "Song inserted");
                }
            }
        }

        if (onDataSetChangedListener != null) {
            onDataSetChangedListener.onDataSetChanged();
        }

        Toast.makeText(getContext(), song.getStarred() > 0 ?
                R.string.song_starred : R.string.song_unstarred, Toast.LENGTH_SHORT).show();

        return true;
    }

    public void setOnDataSetChangedListener(OnDataSetChangedListener onDataSetChangedListener) {
        this.onDataSetChangedListener = onDataSetChangedListener;
    }

    public interface OnDataSetChangedListener {
        void onDataSetChanged();
    }
}
