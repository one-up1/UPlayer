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
import android.view.ContextMenu;
import android.view.View;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.EditSongActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;

import java.util.List;

public abstract class SongsListFragment extends ListFragment<Song> {
    private static final String TAG = "UPlayer";

    private static final int REQUEST_EDIT_SONG = 1;

    protected SongsListFragment(int listItemResource, int listItemHeaderId, int listItemContentId,
                                String[] columns) {
        super(listItemResource, listItemHeaderId, listItemContentId, columns, null);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.list_item_song, menu);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_EDIT_SONG:
                    try {
                        getDbHelper().updateSong((Song)
                                data.getParcelableExtra(EditSongActivity.EXTRA_SONG));
                        Util.showToast(getActivity(), R.string.song_updated);
                        // UI will update in onResume().
                    } catch (Exception ex) {
                        Log.e(TAG, "Error updating song", ex);
                        Util.showErrorDialog(getActivity(), ex);
                    }
                    break;
            }
        }
    }

    @Override
    protected String getActivityTitle() {
        return Util.getCountString(getActivity(), R.plurals.songs, getCount())
                + ", " + Util.formatDuration(Song.getDuration(getData(), 0));
    }

    @Override
    protected void setListItemContent(View rootView, int position, Song song) {
        super.setListItemContent(rootView, position, song);

        // Set title.
        TextView tvTitle = rootView.findViewById(R.id.tvTitle);
        tvTitle.setText(song.getTitle());
        tvTitle.setTextColor(song.getTimesPlayed() == 0 ? Color.BLUE : Color.BLACK);

        // Set artist.
        TextView tvArtist = rootView.findViewById(R.id.tvArtist);
        tvArtist.setText(song.getArtist());
    }

    @Override
    protected void onContextItemSelected(int itemId, int position, Song song) {
        switch (itemId) {
            case R.id.view_artist:
                startActivity(new Intent(getActivity(), SongsActivity.class)
                        .putExtras(SongsFragment.getArguments(song.getArtistId(),
                                getSortColumn(), isSortDesc())));
                break;
            case R.id.edit:
                try {
                    getDbHelper().querySong(song);
                    startActivityForResult(new Intent(getActivity(), EditSongActivity.class)
                                    .putExtra(EditSongActivity.EXTRA_SONG, song),
                            REQUEST_EDIT_SONG);
                } catch (Exception ex) {
                    Log.e(TAG, "Error querying song", ex);
                    Util.showErrorDialog(getActivity(), ex);
                }
                break;
            case R.id.bookmark:
                try {
                    getDbHelper().bookmarkSong(song);
                    Util.showToast(getActivity(), song.getBookmarked() > 0 ?
                                    R.string.bookmark_set : R.string.bookmark_cleared);
                    reloadData();
                } catch (Exception ex) {
                    Log.e(TAG, "Error bookmarking song", ex);
                    Util.showErrorDialog(getActivity(), ex);
                }
                break;
            case R.id.add_to_playlist:
                addToPlaylist(song);
                break;
            case R.id.mark_played:
                try {
                    getDbHelper().updateSongPlayed(song);
                    Util.showToast(getActivity(), R.string.times_played, song.getTimesPlayed());
                    reloadData();
                } catch (Exception ex) {
                    Log.e(TAG, "Error updating song played", ex);
                    Util.showErrorDialog(getActivity(), ex);
                }
                break;
            case R.id.delete:
                deleteSong(position, song);
                break;
        }
    }

    protected void onSongRemoved(int position) {
        getData().remove(position);
        notifyDataSetChanged();
    }

    private void addToPlaylist(final Song song) {
        final List<Playlist> playlists = getDbHelper().queryPlaylists();
        if (playlists.size() == 0) {
            Util.showToast(getActivity(), R.string.no_playlists);
            return;
        }

        final String[] playlistNames = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            Playlist playlist = playlists.get(i);
            playlistNames[i] = playlist.getName() == null ?
                    Util.formatDateTime(playlist.getModified())
                    : playlist.getName() + ": " + Util.formatDateTime(playlist.getModified());
        }
        new AlertDialog.Builder(getActivity())
                .setItems(playlistNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getDbHelper().insertPlaylistSong(playlists.get(which), song);
                        Util.showToast(getActivity(), R.string.ok);
                    }
                })
                .show();
    }

    private void deleteSong(final int position, final Song song) {
        Util.showConfirmDialog(getActivity(),
                getString(R.string.delete_confirm, song.getArtist(), song.getTitle()),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Deleting song: '" + song + "' (" + song.getId() + ")");
                        try {
                            ContentResolver resolver = getActivity().getContentResolver();
                            Uri uri = song.getContentUri();

                            // Change type to image, otherwise nothing will be deleted.
                            ContentValues values = new ContentValues();
                            values.put("media_type", 1);
                            resolver.update(uri, values, null, null);

                            // Delete song from MediaStore and database.
                            int rowsAffected = resolver.delete(uri, null, null);
                            switch (rowsAffected) {
                                case 0:
                                    throw new RuntimeException("Song not found");
                                case 1:
                                    Log.d(TAG, "Song deleted from MediaStore");
                                    break;
                                default:
                                    throw new RuntimeException("Duplicate song");
                            }
                            getDbHelper().deleteSong(song);

                            Util.showToast(getActivity(), R.string.song_deleted);
                            onSongRemoved(position);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error deleting song", ex);
                            Util.showErrorDialog(getActivity(), ex);
                        }
                    }
                });
    }
}
