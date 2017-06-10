package com.oneup.uplayer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;

public class SongsListView extends ListView {
    private static final String TAG = "UPlayer";

    private OnDataSetChangedListener onDataSetChangedListener;
    private OnSongDeletedListener onSongDeletedListener;

    public SongsListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onContextItemSelected(MenuItem item) {
        final Song song = (Song) getItemAtPosition(
                ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.star_unstar:
                if (song.getStarred() == 0) {
                    song.setStarred(System.currentTimeMillis());
                    Toast.makeText(getContext(), R.string.song_starred,
                            Toast.LENGTH_SHORT).show();
                } else {
                    song.setStarred(0);
                    Toast.makeText(getContext(), R.string.song_unstarred,
                            Toast.LENGTH_SHORT).show();
                }
                new DbOpenHelper(getContext()).insertOrUpdateSong(song);

                if (onDataSetChangedListener != null) {
                    onDataSetChangedListener.onDataSetChanged();
                }
                return true;
            case R.id.delete:
                new AlertDialog.Builder(getContext())
                        .setMessage(getContext().getString(
                                R.string.delete_song_confirm, song.getTitle()))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "Deleting song: " + song);
                                Log.d(TAG, getContext().getContentResolver().delete(
                                        song.getContentUri(), null, null) + " songs deleted");
                                new DbOpenHelper(getContext()).deleteSong(song);
                                //FIXME: Delete doesn't delete from disk.

                                if (onDataSetChangedListener != null) {
                                    onDataSetChangedListener.onDataSetChanged();
                                }
                                if (onSongDeletedListener != null) {
                                    onSongDeletedListener.onSongDeleted(song);
                                }
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
        }
        return true;
    }

    public void setOnDataSetChangedListener(OnDataSetChangedListener onDataSetChangedListener) {
        this.onDataSetChangedListener = onDataSetChangedListener;
    }

    public void setOnSongDeletedListener(OnSongDeletedListener onSongDeletedListener) {
        this.onSongDeletedListener = onSongDeletedListener;
    }

    public interface OnDataSetChangedListener {
        void onDataSetChanged();
    }

    public interface OnSongDeletedListener {
        void onSongDeleted(Song song);
    }
}
