package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class SongsFragment extends SongsListFragment {
    private static final String TAG = "UPlayer";

    private static final String ARG_ARTIST_ID = "artist_id";
    private static final String ARG_SELECTION = "selection";
    private static final String ARG_SELECTION_ARGS = "selection_args";

    private long artistId;
    private String selection;
    private String[] selectionArgs;

    public SongsFragment() {
        super(R.layout.list_item_song);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            artistId = args.getLong(ARG_ARTIST_ID);
            if (artistId == 0) {
                selection = args.getString(ARG_SELECTION);
                selectionArgs = args.getStringArray(ARG_SELECTION_ARGS);
            } else {
                selection = Song.ARTIST_ID + "=?";
                selectionArgs = DbHelper.getWhereArgs(artistId);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadData();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.getItem(0).setVisible(artistId == 0);
    }

    @Override
    protected ArrayList<Song> loadData() {
        return getDbHelper().querySongs(selection, selectionArgs, getOrderBy(Song.TITLE));
    }

    @Override
    protected void setListItemViews(View rootView, int position, Song song) {
        super.setListItemViews(rootView, position, song);

        // Set play next and play last buttons.
        setListItemButton(rootView, R.id.ibPlayNext, song);
        setListItemButton(rootView, R.id.ibPlayLast, song);
    }

    @Override
    protected String getInfoText(Song song) {
        switch (getSortColumn()) {
            case Song.ADDED:
                return song.getAdded() == 0 ? null
                        : Util.formatTimeAgo(song.getAdded());
            case Song.LAST_PLAYED:
                return song.getLastPlayed() == 0 ? null
                        : Util.formatTimeAgo(song.getLastPlayed());
            case Song.TIMES_PLAYED:
                return song.getTimesPlayed() == 0 ? null
                        : Integer.toString(song.getTimesPlayed());
            case Song.DURATION:
                return Util.formatDuration(song.getDuration());
            case Song.YEAR:
                return Integer.toString(song.getYear());
            case Song.TAG:
                return song.getTag();
            case Song.BOOKMARKED:
                return song.getBookmarked() == 0 ? null
                        : Util.formatTimeAgo(song.getBookmarked());
            default:
                return null;
        }
    }

    @Override
    protected void onListItemClick(int position, Song song) {
        Log.d(TAG, "Playing " + getData().size() + " songs, songIndex=" + position);
        getActivity().startService(new Intent(getActivity(), MainService.class)
                .putExtra(MainService.EXTRA_REQUEST_CODE, MainService.REQUEST_START)
                .putExtra(MainService.EXTRA_SONGS, getData())
                .putExtra(MainService.EXTRA_SONG_INDEX, position));
    }

    @Override
    protected void onListItemButtonClick(int buttonId, Song song) {
        switch (buttonId) {
            case R.id.ibPlayNext:
                Log.d(TAG, "Playing next: " + song);
                getActivity().startService(new Intent(getActivity(), MainService.class)
                        .putExtra(MainService.EXTRA_REQUEST_CODE, MainService.REQUEST_PLAY_NEXT)
                        .putExtra(MainService.EXTRA_SONG, song));
                Util.showToast(getActivity(), R.string.playing_next, song);
                break;
            case R.id.ibPlayLast:
                Log.d(TAG, "Playing last: " + song);
                getActivity().startService(new Intent(getActivity(), MainService.class)
                        .putExtra(MainService.EXTRA_REQUEST_CODE, MainService.REQUEST_PLAY_LAST)
                        .putExtra(MainService.EXTRA_SONG, song));
                Util.showToast(getActivity(), R.string.playing_last, song);
                break;
        }
    }

    public static SongsFragment newInstance(String selection, String[] selectionArgs,
                                            String sortColumn, boolean sortDesc) {
        return newInstance(getArguments(selection, selectionArgs, sortColumn, sortDesc));
    }

    public static SongsFragment newInstance(Bundle args) {
        SongsFragment fragment = new SongsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static Bundle getArguments(long artistId, String sortColumn, boolean sortDesc) {
        Bundle args = new Bundle();
        args.putLong(ARG_ARTIST_ID, artistId);
        args.putString(ARG_SORT_COLUMN, sortColumn);
        args.putBoolean(ARG_SORT_DESC, sortDesc);
        return args;
    }

    public static Bundle getArguments(String selection, String[] selectionArgs,
                                      String sortColumn, boolean sortDesc) {
        Bundle args = new Bundle();
        args.putString(ARG_SELECTION, selection);
        args.putStringArray(ARG_SELECTION_ARGS, selectionArgs);
        args.putString(ARG_SORT_COLUMN, sortColumn);
        args.putBoolean(ARG_SORT_DESC, sortDesc);
        return args;
    }
}
