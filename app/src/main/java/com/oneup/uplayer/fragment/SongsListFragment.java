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
import android.widget.TextView;
import android.widget.Toast;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.EditSongActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Song;

public abstract class SongsListFragment extends ListFragment<Song> {
    private static final String TAG = "UPlayer";

    private static final int REQUEST_EDIT_SONG = 1;

    private String viewArtistOrderBy;

    public SongsListFragment(int listItemResource) {
        super(listItemResource, R.menu.list_item_song);
    }

    @Override
    protected void setListItemViews(View rootView, int position, Song song) {
        TextView tvTitle = rootView.findViewById(R.id.tvTitle);
        tvTitle.setText(song.getTitle());
        tvTitle.setTextColor(song.getTimesPlayed() == 0 ? Color.BLUE : Color.BLACK);

        TextView tvArtist = rootView.findViewById(R.id.tvArtist);
        tvArtist.setText(song.getArtist());
    }

    @Override
    protected void onContextItemSelected(int itemId, final int position, final Song song) {
        switch (itemId) {
            case R.id.view_artist:
                startActivity(new Intent(getActivity(), SongsActivity.class)
                        .putExtras(SongsFragment.getArguments(
                                song.getArtistId(),
                                viewArtistOrderBy == null ? Song.TITLE : viewArtistOrderBy)));
                break;
            case R.id.edit:
                getDbHelper().querySong(song);
                startActivityForResult(new Intent(getActivity(), EditSongActivity.class)
                                .putExtra(EditSongActivity.EXTRA_SONG, song),
                        REQUEST_EDIT_SONG);
                break;
            case R.id.bookmark:
                getDbHelper().bookmarkSong(song);
                Toast.makeText(getActivity(), song.getBookmarked() > 0 ?
                                R.string.bookmark_set : R.string.bookmark_deleted,
                        Toast.LENGTH_SHORT).show();
                updateList();
                break;
            case R.id.mark_played:
                getDbHelper().updateSongPlayed(song);
                Toast.makeText(getActivity(), getString(
                        R.string.times_played, song.getTimesPlayed()),
                        Toast.LENGTH_SHORT).show();
                updateList();
                break;
            case R.id.delete:
                new AlertDialog.Builder(getActivity())
                        .setIcon(R.drawable.ic_dialog_warning)
                        .setTitle(R.string.delete_song)
                        .setMessage(getString(R.string.delete_confirm, song.getTitle()))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "Deleting song: " + song);
                                ContentResolver resolver = getActivity().getContentResolver();
                                Uri uri = song.getContentUri();

                                // Change type to image, otherwise nothing will be deleted.
                                ContentValues values = new ContentValues();
                                values.put("media_type", 1);
                                resolver.update(uri, values, null, null);

                                // Delete song from MediaStore and database.
                                Log.d(TAG, resolver.delete(uri, null, null) +
                                        " songs deleted from MediaStore");
                                getDbHelper().deleteSong(song);

                                Toast.makeText(getActivity(), R.string.song_deleted,
                                        Toast.LENGTH_SHORT).show();
                                deleteSong(position);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "SongsListFragment.onActivityResult(" + requestCode + ", " + resultCode + ")");
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_EDIT_SONG:
                    Song song = data.getParcelableExtra(EditSongActivity.EXTRA_SONG);
                    getDbHelper().updateSong(song);
                    Toast.makeText(getActivity(), R.string.song_updated, Toast.LENGTH_SHORT).show();
                    // onResume() is called after onActivityResult().
                    break;
            }
        }
    }

    protected void setViewArtistOrderBy(String viewArtistOrderBy) {
        this.viewArtistOrderBy = viewArtistOrderBy;
    }

    protected void deleteSong(int index) {
        Log.d(TAG, "SongsListFragment.deleteSong(" + index + ")");
        getData().remove(index);
        notifyDataSetChanged();
    }
}
