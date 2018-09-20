package com.oneup.uplayer.fragment;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.EditSongActivity;
import com.oneup.uplayer.activity.PlaylistsActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;

public abstract class SongsListFragment extends ListFragment<Song> {
    private static final String TAG = "UPlayer";

    private static final int REQUEST_SELECT_PLAYLIST = 1;
    private static final int REQUEST_EDIT_SONG = 2;

    protected SongsListFragment(int listItemResource, int listItemHeaderId, int listItemContentId,
                                String[] columns) {
        super(listItemResource, R.menu.list_item_song, listItemHeaderId, listItemContentId,
                columns, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_songs_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.savePlaylist:
                startActivityForResult(new Intent(getActivity(), PlaylistsActivity.class)
                                .putExtras(PlaylistsActivity.PlaylistsFragment.getArguments(
                                        null, null, R.string.save_playlist_confirm)),
                        REQUEST_SELECT_PLAYLIST);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_PLAYLIST:
                    try {
                        Playlist playlist = data.getParcelableExtra(
                                PlaylistsActivity.EXTRA_PLAYLIST);
                        playlist.setSongIndex(0);
                        playlist.setSongPosition(0);
                        playlist.setLastPlayed(0);
                        getDbHelper().insertOrUpdatePlaylist(playlist, getData());
                        onPlaylistSelected(playlist);
                        Util.showToast(getActivity(), R.string.playlist_saved);
                    } catch (Exception ex) {
                        Log.e(TAG, "Error saving playlist", ex);
                    }
                    break;
                case REQUEST_EDIT_SONG:
                    try {
                        Song song = data.getParcelableExtra(EditSongActivity.EXTRA_SONG);
                        getDbHelper().updateSong(song);
                        onSongUpdated(song);
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
        return Util.getCountString(getActivity(), R.plurals.songs, getCount());
    }

    @Override
    protected void setListItemContent(View rootView, int position, Song song) {
        super.setListItemContent(rootView, position, song);

        // Set title, marking unplayed songs.
        TextView tvTitle = rootView.findViewById(R.id.tvTitle);
        tvTitle.setText(song.getTitle());
        tvTitle.setTextColor(song.getTimesPlayed() == 0 ? Color.BLUE : Color.BLACK);

        // Set artist.
        TextView tvArtist = rootView.findViewById(R.id.tvArtist);
        tvArtist.setText(song.getArtist());
    }

    @Override
    protected void onContextItemSelected(int itemId, int position, final Song song) {
        switch (itemId) {
            case R.id.view_artist:
                //FIXME: When just using startActivity(), wrong activity may be returned to.
                startActivityForResult(new Intent(getActivity(), SongsActivity.class)
                        .putExtras(SongsFragment.getArguments(song.getArtistId(),
                                getSortColumn(), isSortDesc())), 0);
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
                    onSongUpdated(song);
                    Util.showToast(getActivity(), song.getBookmarked() == 0 ?
                            R.string.bookmark_cleared : R.string.bookmark_set);
                    reloadData();
                } catch (Exception ex) {
                    Log.e(TAG, "Error bookmarking song", ex);
                    Util.showErrorDialog(getActivity(), ex);
                }
                break;
            case R.id.mark_played:
                Util.showConfirmDialog(getActivity(), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            getDbHelper().updateSongPlayed(song);
                            onSongUpdated(song);
                            Util.showToast(getActivity(), R.string.times_played,
                                    song.getTimesPlayed());
                            reloadData();
                        } catch (Exception ex) {
                            Log.e(TAG, "Error updating song played", ex);
                            Util.showErrorDialog(getActivity(), ex);
                        }
                    }
                }, R.string.mark_played_confirm, song);
                break;
            case R.id.delete:
                deleteSong(position, song);
                break;
            default:
                super.onContextItemSelected(itemId, position, song);
                break;
        }
    }

    protected void onPlaylistSelected(Playlist playlist) {
    }

    protected void onSongUpdated(Song song) {
    }

    private void deleteSong(final int position, final Song song) {
        Util.showConfirmDialog(getActivity(),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Deleting song " + song.getId() + ":" + song);
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

                            Util.showToast(getActivity(), R.string.deleted, song);
                            removeListItem(position);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error deleting song", ex);
                            Util.showErrorDialog(getActivity(), ex);
                        }
                    }
                }, R.string.delete_confirm, song);
    }
}
