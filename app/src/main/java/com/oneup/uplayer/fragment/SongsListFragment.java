package com.oneup.uplayer.fragment;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
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

    protected SongsListFragment(int listItemResource, int listItemHeaderId, int listItemContentId,
                                int listItemInfoId, String[] columns, String defaultSortColumn) {
        super(listItemResource, R.menu.list_item_song, listItemHeaderId, listItemContentId,
                listItemInfoId, columns, null, defaultSortColumn);
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
                        getDbHelper().insertOrUpdatePlaylist(playlist, getData());
                        onPlaylistSelected(playlist);
                        Util.showToast(getActivity(), R.string.playlist_saved);
                    } catch (Exception ex) {
                        Log.e(TAG, "Error saving playlist", ex);
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

        // Set title, marking archived and unplayed songs.
        TextView tvTitle = rootView.findViewById(R.id.tvTitle);
        tvTitle.setText(song.getStyledTitle());

        // Set artist.
        TextView tvArtist = rootView.findViewById(R.id.tvArtist);
        tvArtist.setText(song.getArtist());
    }

    @Override
    protected void onContextItemSelected(int itemId, int position, final Song song) {
        switch (itemId) {
            case R.id.view_artist:
                startActivity(new Intent(getActivity(), SongsActivity.class)
                        .putExtras(SongsFragment.getArguments(getDbHelper().queryArtist(song),
                                getSortColumn(), isSortDesc())));
                break;
            case R.id.edit:
                startActivity(new Intent(getActivity(), EditSongActivity.class)
                        .putExtra(EditSongActivity.EXTRA_SONG, song));
                break;
            case R.id.bookmark:
                try {
                    song.setBookmarked(getDbHelper().toggleSongTimestamp(song, Song.BOOKMARKED));
                    onSongUpdated(song);
                    Util.showToast(getActivity(), song.getBookmarked() == 0 ?
                            R.string.bookmark_cleared : R.string.bookmark_set);
                    reloadData();
                } catch (Exception ex) {
                    Log.e(TAG, "Error bookmarking song", ex);
                    Util.showErrorDialog(getActivity(), ex);
                }
                break;
            case R.id.archive:
                try {
                    song.setArchived(getDbHelper().toggleSongTimestamp(song, Song.ARCHIVED));
                    onSongUpdated(song);
                    Util.showToast(getActivity(), song.getArchived() == 0 ?
                            R.string.song_unarchived : R.string.song_archived);
                    reloadData();
                } catch (Exception ex) {
                    Log.e(TAG, "Error archiving song", ex);
                    Util.showErrorDialog(getActivity(), ex);
                }
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
