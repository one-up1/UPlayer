package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.MainActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.widget.SongAdapter;
import com.oneup.uplayer.widget.SongsListView;

import java.util.ArrayList;
import java.util.Collections;

public class SongsFragment extends Fragment implements AdapterView.OnItemClickListener,
        SongsListView.OnDataSetChangedListener, SongsListView.OnSongDeletedListener {
    private static final String TAG = "UPlayer";

    private static final String ARG_ARTIST_ID = "artist_id";
    private static final String ARG_SELECTION = "selection";
    private static final String ARG_SELECTION_ARGS = "selection_args";
    private static final String ARG_ORDER_BY = "order_by";

    private DbOpenHelper dbOpenHelper;
    private ArrayList<Song> songs;

    private long artistId;
    private String selection;
    private String[] selectionArgs;
    private String orderBy;

    private SongsListView listView;
    private ListAdapter listAdapter;
    private Parcelable listViewState;
    private boolean sortOrderReversed;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "SongsFragment.onCreate()");
        super.onCreate(savedInstanceState);

        dbOpenHelper = new DbOpenHelper(getActivity());
        songs = new ArrayList<>();

        artistId = getArguments().getLong(ARG_ARTIST_ID);
        if (artistId == 0) {
            selection = getArguments().getString(ARG_SELECTION);
            selectionArgs = getArguments().getStringArray(ARG_SELECTION_ARGS);
        } else {
            selection = Song.ARTIST_ID + "=?";;
            selectionArgs = DbOpenHelper.getWhereArgs(artistId);
        }
        orderBy = getArguments().getString(ARG_ORDER_BY);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "SongsFragment.onCreateView()");
        dbOpenHelper.querySongs(songs, selection, selectionArgs, orderBy);
        if (sortOrderReversed) {
            Collections.reverse(songs);
        }

        if (getActivity() instanceof SongsActivity) {
            setTitle();
        }

        if (listView == null) {
            listView = new SongsListView(getActivity());
            if (artistId == 0) {
                listView.setViewArtistOrderBy(orderBy);
            }
            listView.setOnItemClickListener(this);
            if (getActivity() instanceof MainActivity) {
                listView.setOnDataSetChangedListener(this);
            } else {
                listView.setOnSongDeletedListener(this);
            }
            registerForContextMenu(listView);

            listAdapter = new ListAdapter();
            listView.setAdapter(listAdapter);
        } else {
            listAdapter.notifyDataSetChanged();
        }
        return listView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "SongsFragment.onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        if (listViewState != null) {
            listView.onRestoreInstanceState(listViewState);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v == listView) {
            getActivity().getMenuInflater().inflate(R.menu.list_item_song, menu);
            menu.getItem(0).setVisible(artistId == 0);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return getUserVisibleHint() && listView.onContextItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "SongsFragment.onActivityResult(" + requestCode + ", " + resultCode + ")");
        listView.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "SongsFragment.onPause()");
        listViewState = listView.onSaveInstanceState();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "SongsFragment.onDestroy()");
        if (dbOpenHelper != null) {
            dbOpenHelper.close();
        }

        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == listView) {
            Log.d(TAG, "Playing " + songs.size() + " songs, songIndex=" + position);
            getContext().startService(new Intent(getContext(), MainService.class)
                    .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_START)
                    .putExtra(MainService.ARG_SONGS, songs)
                    .putExtra(MainService.ARG_SONG_INDEX, position));
        }
    }

    @Override
    public void onDataSetChanged() {
        ((MainActivity) getActivity()).notifyDataSetChanged();
    }

    @Override
    public void onSongDeleted(Song song) {
        songs.remove(song);
        setTitle();
        listAdapter.notifyDataSetChanged();
    }

    public void reverseSortOrder() {
        Collections.reverse(songs);
        listAdapter.notifyDataSetChanged();
        sortOrderReversed = !sortOrderReversed;
    }

    private void setTitle() {
        getActivity().setTitle(getString(R.string.song_count_duration, songs.size(),
                Util.formatDuration(Song.getDuration(songs, 0))));
    }

    private class ListAdapter extends SongAdapter implements View.OnClickListener {
        private ListAdapter() {
            super(getContext(), songs);
        }

        @Override
        public void addButtons(RelativeLayout rlButtons) {
            RelativeLayout.LayoutParams params;

            ImageButton ibPlayNext = new ImageButton(getContext());
            ibPlayNext.setId(R.id.ibPlayNext);
            ibPlayNext.setImageResource(R.drawable.ic_play_next);
            ibPlayNext.setContentDescription(getString(R.string.play_next));
            ibPlayNext.setOnClickListener(this);
            rlButtons.addView(ibPlayNext);

            ImageButton ibPlayLast = new ImageButton(getContext());
            ibPlayLast.setId(R.id.ibPlayLast);
            params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.END_OF, R.id.ibPlayNext);
            ibPlayLast.setLayoutParams(params);
            ibPlayLast.setImageResource(R.drawable.ic_play_last);
            ibPlayLast.setContentDescription(getString(R.string.play_last));
            ibPlayLast.setOnClickListener(this);
            rlButtons.addView(ibPlayLast);
        }

        @Override
        public void setButtons(View view, Song song) {
            ImageButton ibPlayNext = view.findViewById(R.id.ibPlayNext);
            ibPlayNext.setTag(song);

            ImageButton ibPlayLast = view.findViewById(R.id.ibPlayLast);
            ibPlayLast.setTag(song);
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
