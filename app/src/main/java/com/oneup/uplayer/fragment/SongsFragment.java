package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.widget.TextView;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

//TODO: Base method for getting/setting orderBy/info?

public class SongsFragment extends SongsListFragment {
    public static final int ORDER_BY_ARTIST = 4;
    public static final int ORDER_BY_DURATION = 5;
    public static final int ORDER_BY_YEAR = 6;
    public static final int ORDER_BY_TAG = 7;
    public static final int ORDER_BY_BOOKMARKED = 8;

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
        String orderBy;
        switch (getOrderBy()) {
            case ORDER_BY_ADDED:
                orderBy = getOrderBy(Song.ADDED);
                break;
            case ORDER_BY_LAST_PLAYED:
                orderBy = getOrderBy(Song.LAST_PLAYED);
                break;
            case ORDER_BY_TIMES_PLAYED:
                orderBy = getOrderBy(Song.TIMES_PLAYED);
                break;
            case ORDER_BY_ARTIST:
                orderBy = getOrderBy(Song.ARTIST);
                break;
            case ORDER_BY_DURATION:
                orderBy = getOrderBy(Song.DURATION);
                break;
            case ORDER_BY_YEAR:
                orderBy = getOrderBy(Song.YEAR);
                break;
            case ORDER_BY_TAG:
                orderBy = getOrderBy(Song.TAG);
                break;
            case ORDER_BY_BOOKMARKED:
                orderBy = getOrderBy(Song.BOOKMARKED);
                break;
            default:
                orderBy = null;
                break;
        }

        if (orderBy == null) {
            orderBy = getOrderBy(Song.TITLE);
        } else {
            orderBy += "," + Song.TITLE;
        }

        return getDbHelper().querySongs(selection, selectionArgs, orderBy);
    }

    @Override
    protected void setListItemViews(View rootView, int position, Song song) {
        super.setListItemViews(rootView, position, song);

        // Get info text.
        String info;
        switch (getOrderBy()) {
            case ORDER_BY_ADDED:
                info = song.getAdded() == 0 ? null
                        : Util.formatTimeAgo(song.getAdded());
                break;
            case ORDER_BY_LAST_PLAYED:
                info = song.getLastPlayed() == 0 ? null
                        : Util.formatTimeAgo(song.getLastPlayed());
                break;
            case ORDER_BY_TIMES_PLAYED:
                info = song.getTimesPlayed() == 0 ? null
                        : Integer.toString(song.getTimesPlayed());
                break;
            case ORDER_BY_DURATION:
                info = Util.formatDuration(song.getDuration());
                break;
            case ORDER_BY_YEAR:
                info = Integer.toString(song.getYear());
                break;
            case ORDER_BY_TAG:
                info = song.getTag();
                break;
            case ORDER_BY_BOOKMARKED:
                info = song.getBookmarked() == 0 ? null
                        : Util.formatTimeAgo(song.getBookmarked());
                break;
            default:
                info = null;
                break;
        }

        // Set info text or hide the view when no info is available.
        TextView tvInfo = rootView.findViewById(R.id.tvInfo);
        if (info == null) {
            tvInfo.setVisibility(View.GONE);
        } else {
            tvInfo.setText(info);
            tvInfo.setVisibility(View.VISIBLE);
        }

        setListItemButton(rootView, R.id.ibPlayNext, song);
        setListItemButton(rootView, R.id.ibPlayLast, song);
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
                                            int orderBy, boolean orderByDesc) {
        return newInstance(getArguments(selection, selectionArgs, orderBy, orderByDesc));
    }

    public static SongsFragment newInstance(Bundle args) {
        SongsFragment fragment = new SongsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static Bundle getArguments(long artistId, int orderBy, boolean orderByDesc) {
        Bundle args = new Bundle();
        args.putLong(ARG_ARTIST_ID, artistId);
        args.putInt(ARG_ORDER_BY, orderBy);
        args.putBoolean(ARG_ORDER_BY_DESC, orderByDesc);
        return args;
    }

    public static Bundle getArguments(String selection, String[] selectionArgs,
                                      int orderBy, boolean orderByDesc) {
        Bundle args = new Bundle();
        args.putString(ARG_SELECTION, selection);
        args.putStringArray(ARG_SELECTION_ARGS, selectionArgs);
        args.putInt(ARG_ORDER_BY, orderBy);
        args.putBoolean(ARG_ORDER_BY_DESC, orderByDesc);
        return args;
    }
}
