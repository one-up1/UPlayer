package com.oneup.uplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.obj.Song;

public class SongsListView extends ListView implements AdapterView.OnItemLongClickListener {
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
            String[] selectionArgs = new String[] { Long.toString(song.getId()) };
            ContentValues values = new ContentValues();
            try (Cursor c = db.query(Song.TABLE_NAME, new String[] { Song.STARRED },
                    selection, selectionArgs, null, null, null)) {
                if (c.moveToNext()) {
                    song.setStarred(c.getInt(0) != 1);
                    values.put(Song.STARRED, song.isStarred() ? 1 : 0);
                    db.update(Song.TABLE_NAME, values, selection, selectionArgs);
                } else {
                    song.setStarred(true);
                    values.put(BaseColumns._ID, song.getId());
                    values.put(MediaStore.MediaColumns.TITLE, song.getTitle());
                    if (song.getArtistId() > 0) {
                        values.put(MediaStore.Audio.AudioColumns.ARTIST_ID, song.getArtistId());
                        values.put(MediaStore.Audio.AudioColumns.ARTIST, song.getArtist());
                    }
                    if (song.getYear() > 0) {
                        values.put(MediaStore.Audio.AudioColumns.YEAR, song.getYear());
                    }
                    values.put(Song.STARRED, true);
                    db.insert(Song.TABLE_NAME, null, values);
                }
            }
        }

        Toast.makeText(getContext(), song.isStarred() ?
                R.string.song_starred : R.string.song_unstarred, Toast.LENGTH_SHORT).show();

        return true;
    }
}
