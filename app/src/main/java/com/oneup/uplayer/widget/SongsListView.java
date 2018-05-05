package com.oneup.uplayer.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.EditSongActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.fragment.SongsFragment;

//TODO: How ListViews are implemented. BaseFragment with ListView?

public class SongsListView extends ListView {
    private static final String TAG = "UPlayer";

    private static final int REQUEST_EDIT_SONG = 1;

    private Activity context;
    private DbHelper dbHelper;
    private Song editSong;

    private String viewArtistOrderBy;

    private OnDataSetChangedListener onDataSetChangedListener;
    private OnSongDeletedListener onSongDeletedListener;

    public SongsListView(Activity context) {
        super(context);

        this.context = context;
        dbHelper = new DbHelper(context);
    }

    public boolean onContextItemSelected(MenuItem item) {
        final Song song = (Song) getItemAtPosition(
                ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.view_artist:
                context.startActivity(new Intent(getContext(), SongsActivity.class)
                        .putExtras(SongsFragment.getArguments(
                                song.getArtistId(),
                                viewArtistOrderBy == null ? Song.TITLE : viewArtistOrderBy)));
                return true;
            case R.id.edit:
                dbHelper.querySong(song);
                context.startActivityForResult(new Intent(getContext(), EditSongActivity.class)
                                .putExtra(EditSongActivity.EXTRA_SONG, editSong = song),
                        REQUEST_EDIT_SONG);
                return true;
            case R.id.bookmark:
                dbHelper.bookmarkSong(song);
                dbHelper.querySong(song);
                Toast.makeText(context, song.getBookmarked() > 0 ?
                                R.string.bookmark_set : R.string.bookmark_deleted,
                        Toast.LENGTH_SHORT).show();

                if (onDataSetChangedListener != null) {
                    onDataSetChangedListener.onDataSetChanged();
                }
                return true;
            case R.id.mark_played:
                dbHelper.updateSongPlayed(song);
                dbHelper.querySong(song);
                Toast.makeText(context, context.getString(
                        R.string.times_played, song.getTimesPlayed()),
                        Toast.LENGTH_SHORT).show();

                ((SongAdapter) getAdapter()).notifyDataSetChanged();
                if (onDataSetChangedListener != null) {
                    onDataSetChangedListener.onDataSetChanged();
                }
                return true;
            case R.id.delete:
                new AlertDialog.Builder(context)
                        .setIcon(R.drawable.ic_dialog_warning)
                        .setTitle(R.string.delete_song)
                        .setMessage(context.getString(R.string.delete_confirm, song.getTitle()))
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
                                        " songs deleted from MediaStore");
                                dbHelper.deleteSong(song);
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "SongsListView.onActivityResult(" + requestCode + ", " + resultCode + ")");
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_EDIT_SONG:
                    Song song = data.getParcelableExtra(EditSongActivity.EXTRA_SONG);
                    editSong.setYear(song.getYear());
                    editSong.setAdded(song.getAdded());
                    editSong.setTag(song.getTag());
                    editSong.setBookmarked(song.getBookmarked());

                    dbHelper.updateSong(song);
                    Toast.makeText(context, R.string.song_updated, Toast.LENGTH_SHORT).show();
                    editSong = null;

                    ((SongAdapter) getAdapter()).notifyDataSetChanged();
                    if (onDataSetChangedListener != null) {
                        onDataSetChangedListener.onDataSetChanged();
                    }
                    break;
            }
        }
    }

    public void setViewArtistOrderBy(String viewArtistOrderBy) {
        this.viewArtistOrderBy = viewArtistOrderBy;
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
