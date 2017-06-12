package com.oneup.uplayer.widget;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.Log;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.oneup.uplayer.R;
import com.oneup.uplayer.Util;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;

public class SongsListView extends ListView {
    private static final String TAG = "UPlayer";

    private Context context;
    private DbOpenHelper dbOpenHelper;

    private OnDataSetChangedListener onDataSetChangedListener;
    private OnSongDeletedListener onSongDeletedListener;

    public SongsListView(Context context) {
        super(context);

        this.context = context;
        dbOpenHelper = new DbOpenHelper(context);
    }

    public boolean onContextItemSelected(MenuItem item) {
        final Song song = (Song) getItemAtPosition(
                ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.bookmark:
                if (song.getBookmarked() == 0) {
                    song.setBookmarked(System.currentTimeMillis());
                    Toast.makeText(context, R.string.bookmark_set, Toast.LENGTH_SHORT).show();
                } else {
                    song.setBookmarked(0);
                    Toast.makeText(context, R.string.bookmark_deleted, Toast.LENGTH_SHORT).show();
                }
                dbOpenHelper.insertOrUpdateSong(song);

                if (onDataSetChangedListener != null) {
                    onDataSetChangedListener.onDataSetChanged();
                }
                return true;
            case R.id.info:
                Util.showInfoDialog(getContext(), song.getTitle(), context.getString(
                        R.string.info_message_song,
                        song.getYear(),
                        Util.formatDuration(song.getDuration()),
                        song.getLastPlayed() == 0 ?
                                context.getString(R.string.never) :
                                Util.formatDateTime(song.getLastPlayed()),
                        song.getTimesPlayed()));
                return true;
            case R.id.delete:
                new AlertDialog.Builder(context)
                        .setMessage(context.getString(
                                R.string.delete_confirm, song.getTitle()))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "Deleting song: " + song);
                                ContentResolver contentResolver = context.getContentResolver();
                                Uri uri = song.getContentUri();

                                // Change type to image, otherwise nothing will be deleted.
                                ContentValues values = new ContentValues();
                                values.put("media_type", 1);
                                contentResolver.update(uri, values, null, null);

                                Log.d(TAG, contentResolver.delete(uri, null, null) +
                                        " songs deleted");
                                dbOpenHelper.deleteSong(song);
                                Toast.makeText(context, R.string.song_deleted,
                                        Toast.LENGTH_SHORT).show();

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
