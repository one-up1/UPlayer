package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Song;

import java.util.ArrayList;
import java.util.Collections;

public class SongsFragment extends BaseSongsFragment implements View.OnClickListener {
    private static final String TAG = "UPlayer";

    private static final String ARG_ARTIST_ID = "artist_id";
    private static final String ARG_SELECTION = "selection";
    private static final String ARG_SELECTION_ARGS = "selection_args";
    private static final String ARG_ORDER_BY = "order_by";

    private long artistId;
    private String selection;
    private String[] selectionArgs;
    private String orderBy;

    private boolean sortOrderReversed;

    public SongsFragment() {
        super(R.layout.list_item_song);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "SongsFragment.onCreate()");
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
        Log.d(TAG, "SongsFragment.onResume()");
        super.onResume();
        loadSongs();
    }

    @Override
    public void onClick(View v) {
        Song song = (Song) v.getTag();
        switch (v.getId()) {
            case R.id.ibPlayNext:
                Log.d(TAG, "Playing next: " + song);
                getContext().startService(new Intent(getContext(), MainService.class)
                        .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_PLAY_NEXT)
                        .putExtra(MainService.ARG_SONG, song));
                Toast.makeText(getContext(), getString(R.string.playing_next, song),
                        Toast.LENGTH_SHORT).show();
                break;
            case R.id.ibPlayLast:
                Log.d(TAG, "Playing last: " + song);
                getContext().startService(new Intent(getContext(), MainService.class)
                        .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_PLAY_LAST)
                        .putExtra(MainService.ARG_SONG, song));
                Toast.makeText(getContext(), getString(R.string.playing_last, song),
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void reverseSortOrder() {
        Collections.reverse(getSongs());
        notifyDataSetChanged();
        sortOrderReversed = !sortOrderReversed;
    }

    @Override
    protected void loadSongs() {
        ArrayList<Song> songs = getDbHelper().querySongs(selection, selectionArgs, orderBy);
        if (sortOrderReversed) {
            Collections.reverse(songs);
        }
        setSongs(songs);
    }

    @Override
    protected void setListViewViews(View rootView, int position, Song song) {
        ImageButton ibPlayNext = rootView.findViewById(R.id.ibPlayNext);
        ibPlayNext.setOnClickListener(this);
        ibPlayNext.setTag(song);

        ImageButton ibPlayLast = rootView.findViewById(R.id.ibPlayLast);
        ibPlayLast.setOnClickListener(this);
        ibPlayLast.setTag(song);
    }

    @Override
    protected void onListViewItemClick(int position) {
        Log.d(TAG, "Playing " + getSongs().size() + " songs, songIndex=" + position);
        getContext().startService(new Intent(getContext(), MainService.class)
                .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_START)
                .putExtra(MainService.ARG_SONGS, getSongs())
                .putExtra(MainService.ARG_SONG_INDEX, position));
    }

    /*private void setTitle() {
        getActivity().setTitle(getString(R.string.song_count_duration, songs.size(),
                Util.formatDuration(Song.getDuration(songs, 0))));
    }*/

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
