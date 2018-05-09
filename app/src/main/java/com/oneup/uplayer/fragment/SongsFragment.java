package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Song;

import java.util.ArrayList;

public class SongsFragment extends SongsListFragment {
    private static final String TAG = "UPlayer";

    private static final String ARG_ARTIST_ID = "artist_id";
    private static final String ARG_SELECTION = "selection";
    private static final String ARG_SELECTION_ARGS = "selection_args";
    private static final String ARG_ORDER_BY = "order_by";

    private long artistId;
    private String selection;
    private String[] selectionArgs;
    private String orderBy;

    public SongsFragment() {
        super(R.layout.list_item_song);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        artistId = getArguments().getLong(ARG_ARTIST_ID);
        if (artistId == 0) {
            selection = getArguments().getString(ARG_SELECTION);
            selectionArgs = getArguments().getStringArray(ARG_SELECTION_ARGS);
        } else {
            selection = Song.ARTIST_ID + "=?";
            selectionArgs = DbHelper.getWhereArgs(artistId);
        }
        orderBy = getArguments().getString(ARG_ORDER_BY);

        if (artistId == 0) {
            setViewArtistOrderBy(orderBy);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadData();
    }

    @Override
    protected ArrayList<Song> loadData() {
        return getDbHelper().querySongs(selection, selectionArgs, orderBy);
    }

    @Override
    protected void setListItemViews(View rootView, int position, Song song) {
        super.setListItemViews(rootView, position, song);
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
                Toast.makeText(getActivity(), getString(R.string.playing_next, song),
                        Toast.LENGTH_SHORT).show();
                break;
            case R.id.ibPlayLast:
                Log.d(TAG, "Playing last: " + song);
                getActivity().startService(new Intent(getActivity(), MainService.class)
                        .putExtra(MainService.EXTRA_REQUEST_CODE, MainService.REQUEST_PLAY_LAST)
                        .putExtra(MainService.EXTRA_SONG, song));
                Toast.makeText(getActivity(), getString(R.string.playing_last, song),
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public static SongsFragment newInstance(String selection, String[] selectionArgs,
                                            String orderBy) {
        return newInstance(getArguments(selection, selectionArgs, orderBy));
    }

    public static SongsFragment newInstance(Bundle args) {
        SongsFragment fragment = new SongsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static Bundle getArguments(long artistId, String orderBy) {
        Bundle args = new Bundle();
        args.putLong(ARG_ARTIST_ID, artistId);
        args.putString(ARG_ORDER_BY, orderBy);
        return args;
    }

    public static Bundle getArguments(String selection, String[] selectionArgs, String orderBy) {
        Bundle args = new Bundle();
        args.putString(ARG_SELECTION, selection);
        args.putStringArray(ARG_SELECTION_ARGS, selectionArgs);
        args.putString(ARG_ORDER_BY, orderBy);
        return args;
    }
}
