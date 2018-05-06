package com.oneup.uplayer.fragment;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.EditSongActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Song;

public abstract class SongsListFragment extends ListFragment<Song> implements View.OnClickListener {
    private static final String TAG = "UPlayer";

    private static final int REQUEST_EDIT_SONG = 1;

    private String viewArtistOrderBy;
    private Song editSong;

    public SongsListFragment(int resource) {
        super(resource, R.menu.list_item_song);
        Log.d(TAG, "SongsListFragment()");
    }

    @Override
    protected void setRowViews(View rootView, int position, Song song) {
        TextView tvTitle = rootView.findViewById(R.id.tvTitle);
        tvTitle.setText(song.getTitle());
        tvTitle.setTextColor(song.getTimesPlayed() == 0 ? Color.BLUE : Color.BLACK);

        TextView tvArtist = rootView.findViewById(R.id.tvArtist);
        tvArtist.setText(song.getArtist());
    }

    @Override
    protected void onContextItemSelected(int itemId, int position, final Song song) {
        switch (itemId) {
            case R.id.view_artist:
                startActivity(new Intent(getContext(), SongsActivity.class)
                        .putExtras(SongsFragment.getArguments(
                                song.getArtistId(),
                                viewArtistOrderBy == null ? Song.TITLE : viewArtistOrderBy)));
                break;
            case R.id.edit:
                getDbHelper().querySong(song);
                startActivityForResult(new Intent(getContext(), EditSongActivity.class)
                                .putExtra(EditSongActivity.EXTRA_SONG, editSong = song),
                        REQUEST_EDIT_SONG);
                break;
            case R.id.bookmark:
                getDbHelper().bookmarkSong(song);
                getDbHelper().querySong(song);
                Toast.makeText(getContext(), song.getBookmarked() > 0 ?
                                R.string.bookmark_set : R.string.bookmark_deleted,
                        Toast.LENGTH_SHORT).show();
                loadSongs();
                break;
            case R.id.mark_played:
                getDbHelper().updateSongPlayed(song);
                getDbHelper().querySong(song);
                Toast.makeText(getContext(), getString(
                        R.string.times_played, song.getTimesPlayed()),
                        Toast.LENGTH_SHORT).show();
                loadSongs();
                break;
            case R.id.delete:
                new AlertDialog.Builder(getContext())
                        .setIcon(R.drawable.ic_dialog_warning)
                        .setTitle(R.string.delete_song)
                        .setMessage(getString(R.string.delete_confirm, song.getTitle()))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "Deleting song: " + song);
                                ContentResolver contentResolver = getContext().getContentResolver();
                                Uri uri = song.getContentUri();

                                // Change type to image, otherwise nothing will be deleted.
                                ContentValues values = new ContentValues();
                                values.put("media_type", 1);
                                contentResolver.update(uri, values, null, null);

                                Log.d(TAG, contentResolver.delete(uri, null, null) +
                                        " songs deleted from MediaStore");
                                getDbHelper().deleteSong(song);

                                Toast.makeText(getContext(), R.string.song_deleted,
                                        Toast.LENGTH_SHORT).show();

                                loadSongs();
                                onSongDeleted(song);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "SongsListFragment.onActivityResult(" + requestCode + ", " + resultCode + ")");
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_EDIT_SONG:
                    Song song = data.getParcelableExtra(EditSongActivity.EXTRA_SONG);
                    editSong.setYear(song.getYear());
                    editSong.setAdded(song.getAdded());
                    editSong.setTag(song.getTag());
                    editSong.setBookmarked(song.getBookmarked());

                    getDbHelper().updateSong(song);
                    Toast.makeText(getContext(), R.string.song_updated, Toast.LENGTH_SHORT).show();
                    editSong = null;
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        onButtonClick(v.getId(), (Song) v.getTag());
    }

    protected void setButton(View rootView, int id, Song song) {
        ImageButton button = rootView.findViewById(id);
        button.setTag(song);
        button.setOnClickListener(this);
    }

    protected abstract void onButtonClick(int id, Song song);

    protected void setViewArtistOrderBy(String viewArtistOrderBy) {
        this.viewArtistOrderBy = viewArtistOrderBy;
    }

    protected void loadSongs() {
    }

    protected void onSongDeleted(Song song) {
    }
}
