package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.EditSongActivity;
import com.oneup.uplayer.activity.LogActivity;
import com.oneup.uplayer.activity.PlaylistsActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.LogData;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;
import com.oneup.util.Utils;

import java.io.File;

public abstract class SongsListFragment extends ListFragment<Song> {
    private static final String TAG = "UPlayer";

    private static final int REQUEST_SELECT_PLAYLIST = 1;

    protected SongsListFragment(int listItemResource, int listItemHeaderId, int listItemContentId,
                                int listItemInfoId, String[] sortColumnValues) {
        super(listItemResource, R.menu.list_item_song, listItemHeaderId, listItemContentId,
                listItemInfoId, sortColumnValues, null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_songs_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.savePlaylist) {
            startActivityForResult(new Intent(getActivity(), PlaylistsActivity.class)
                            .putExtras(PlaylistsActivity.PlaylistsFragment.getArguments(
                                    null, null, R.string.save_playlist_confirm)),
                    REQUEST_SELECT_PLAYLIST);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        Song song = getListItem(getListItemPosition(menuInfo));
        menu.findItem(R.id.toggle_bookmark).setTitle(song.getBookmarked() == 0 ?
                R.string.bookmark : R.string.clear_bookmark);
        menu.findItem(R.id.toggle_archive).setTitle(song.getArchived() == 0 ?
                R.string.archive : R.string.unarchive);
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

                        Utils.showToast(getActivity(), R.string.playlist_saved);
                        onPlaylistSelected(playlist);
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

        // Set comments.
        /*TextView tvComments = rootView.findViewById(R.id.tvComments);
        String comments = song.getComments();
        if (comments == null) {
            tvComments.setVisibility(View.GONE);
        } else {
            tvComments.setText(comments);
            tvComments.setVisibility(View.VISIBLE);
        }*/
    }

    @Override
    protected void onContextItemSelected(int itemId, int position, final Song song) {
        if (itemId == R.id.view_artist) {
            startActivity(new Intent(getActivity(), SongsActivity.class)
                    .putExtras(SongsFragment.getArguments(getDbHelper().queryArtist(song),
                            getSortColumn(), isSortDesc())));
        } else if (itemId == R.id.edit) {
            startActivity(new Intent(getActivity(), EditSongActivity.class)
                    .putExtra(EditSongActivity.EXTRA_SONG, song));
        } else if (itemId == R.id.toggle_bookmark) {
            try {
                song.setBookmarked(getDbHelper().toggleSongTimestamp(song, Song.BOOKMARKED));
                Utils.showToast(getActivity(), song.getBookmarked() == 0 ?
                        R.string.bookmark_cleared : R.string.bookmark_set);
                onSongUpdated(song);
            } catch (Exception ex) {
                Log.e(TAG, "Error bookmarking song", ex);
                Utils.showErrorDialog(getActivity(), ex);
            }
        } else if (itemId == R.id.toggle_archive) {
            try {
                song.setArchived(getDbHelper().toggleSongTimestamp(song, Song.ARCHIVED));
                Utils.showToast(getActivity(), song.getArchived() == 0 ?
                        R.string.song_unarchived : R.string.song_archived);
                onSongUpdated(song);
            } catch (Exception ex) {
                Log.e(TAG, "Error archiving song", ex);
                Utils.showErrorDialog(getActivity(), ex);
            }
        } else if (itemId == R.id.log) {
            startActivity(new Intent(getActivity(), LogActivity.class)
                    .putExtra(LogActivity.EXTRA_TITLE, song.getStyledTitle())
                    .putExtra(LogActivity.EXTRA_QUERY_ARTIST, false)
                    .putExtra(LogActivity.EXTRA_SELECTION, LogData.SONG_ID + "=?")
                    .putExtra(LogActivity.EXTRA_SELECTION_ARGS,
                            DbHelper.getWhereArgs(song.getId())));
        } else if (itemId == R.id.delete) {
            deleteSong(position, song);
        } else {
            super.onContextItemSelected(itemId, position, song);
        }
    }

    protected void onPlaylistSelected(Playlist playlist) {
    }

    protected void onSongUpdated(Song song) {
    }

    private void deleteSong(final int position, final Song song) {
        Utils.showConfirmDialog(getActivity(),
                (dialog, which) -> {
                    Log.d(TAG, "Deleting song " + song.getId() + ":" + song);
                    try {
                        // Get absolute path of song.
                        String path;
                        try (Cursor c = getActivity().getContentResolver().query(
                                song.getContentUri(), new String[]{Song.DATA}, null, null, null)) {
                            if (c.moveToFirst()) {
                                path = c.getString(0);
                            } else {
                                throw new Exception("Song not found");
                            }
                        }
                        Log.d(TAG, "path=" + path);

                        // Delete song from file system and database.
                        if (new File(path).delete()) {
                            Log.d(TAG, "Song deleted");
                        } else {
                            throw new Exception("Could not delete song");
                        }
                        getDbHelper().deleteSong(song);

                        Utils.showToast(getActivity(), R.string.deleted, song);
                        removeListItem(position);

                        /* Change type to image, otherwise nothing will be deleted.
                        ContentValues values = new ContentValues();
                        values.put("media_type", 1);
                        resolver.update(uri, values, null, null);

                        // Delete song from MediaStore.
                        int rowsAffected = resolver.delete(uri, null, null);
                        switch (rowsAffected) {
                            case 0:
                                throw new RuntimeException("Song not found");
                            case 1:
                                Log.d(TAG, "Song deleted from MediaStore");
                                break;
                            default:
                                throw new RuntimeException("Duplicate song");
                        }*/
                    } catch (Exception ex) {
                        Log.e(TAG, "Error deleting song", ex);
                        Utils.showErrorDialog(getActivity(), ex);
                    }
                }, R.string.app_name, R.string.delete_confirm, song);
    }
}
