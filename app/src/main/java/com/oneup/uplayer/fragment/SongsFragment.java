package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
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
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.widget.SongAdapter;
import com.oneup.uplayer.widget.SongsListView;

import java.util.ArrayList;
import java.util.Collections;

public class SongsFragment extends Fragment implements BaseArgs, AdapterView.OnItemClickListener,
        SongsListView.OnDataSetChangedListener, SongsListView.OnSongDeletedListener {
    private static final String TAG = "UPlayer";

    private SparseArray<Artist> artists;
    private Artist artist;
    private String selection;
    private String dbOrderBy;

    private DbOpenHelper dbOpenHelper;
    private ArrayList<Song> objects;

    private SongsListView listView;
    private ListAdapter listAdapter;
    private Parcelable listViewState;
    private boolean sortOrderReversed;

    public SongsFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "SongsFragment.onCreateView()");
        artists = getArguments().getSparseParcelableArray(ARG_ARTISTS);
        if (artists == null) {
            artist = getArguments().getParcelable(ARG_ARTIST);
            selection = Song.ARTIST_ID + "=" + artist.getId();
            Log.d(TAG, "artist=" + artist + ", selection=" + selection);
        } else {
            selection = getArguments().getString(ARG_SELECTION);
            dbOrderBy = getArguments().getString(ARG_DB_ORDER_BY);
            Log.d(TAG, artists.size() + " artists, selection=" + selection +
                    ", dbOrderBy=" + dbOrderBy);
        }

        dbOpenHelper = new DbOpenHelper(getActivity());
        if (objects == null) {
            objects = new ArrayList<>();
        } else {
            objects.clear();
        }
        try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
            try (Cursor c = db.query(Song.TABLE_NAME, null, selection, null,
                    null, null, dbOrderBy)) {
                while (c.moveToNext()) {
                    Song song = new Song();
                    song.setId(c.getInt(0));
                    song.setTitle(c.getString(1));
                    song.setDuration(c.getInt(2));
                    song.setArtistId(c.getLong(3));
                    song.setArtist(c.getString(4));
                    song.setYear(c.getInt(5));
                    song.setDateAdded(c.getLong(6));
                    song.setBookmarked(c.getLong(7));
                    song.setTag(c.getString(8));
                    song.setLastPlayed(c.getLong(9));
                    song.setTimesPlayed(c.getInt(9));
                    objects.add(song);
                }
            }
        }

        if (sortOrderReversed) {
            Collections.reverse(objects);
        }

        Log.d(TAG, "Queried " + objects.size() + " songs");
        if (getActivity() instanceof SongsActivity) {
            setTitle();
        }

        if (listView == null) {
            listView = new SongsListView(getActivity());
            if (artist == null) {
                //TODO: listView.setViewArtistSortBy(joinedSortBy);
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
            menu.getItem(0).setVisible(artist == null);
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
            Log.d(TAG, "Playing " + objects.size() + " songs, songIndex=" + position);
            getContext().startService(new Intent(getContext(), MainService.class)
                    .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_START)
                    .putExtra(MainService.ARG_SONGS, objects)
                    .putExtra(MainService.ARG_SONG_INDEX, position));
        }
    }

    @Override
    public void onDataSetChanged() {
        ((MainActivity) getActivity()).notifyDataSetChanged();
    }

    @Override
    public void onSongDeleted(Song song) {
        objects.remove(song);
        setTitle();
        listAdapter.notifyDataSetChanged();
    }

    public void setArtists(SparseArray<Artist> artists) {
        getArguments().putSparseParcelableArray(ARG_ARTISTS, artists);
        this.artists = artists;
    }

    public void reverseSortOrder() {
        Collections.reverse(objects);
        listAdapter.notifyDataSetChanged();
        sortOrderReversed = !sortOrderReversed;
    }

    /*private boolean setArtist(Song song, int id) {
        if (artists == null) {
            song.setArtist(artist);
        } else {
            Artist artist = artists.get(id);
            if (artist == null) {
                Log.w(TAG, "Artist for song '" + song + "' not found");
                return false;
            }
            song.setArtist(artist);
        }
        return true;
    }*/

    private void setTitle() {
        getActivity().setTitle(getString(R.string.song_count_duration, objects.size(),
                Util.formatDuration(Song.getDuration(objects, 0))));
    }

    private class ListAdapter extends SongAdapter implements View.OnClickListener {
        private ListAdapter() {
            super(getContext(), objects);
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

    public static SongsFragment newInstance(SparseArray<Artist> artists, Artist artist,
                                            String selection, String dbOrderBy) {
        //TODO: Song sorting.
        SongsFragment fragment = new SongsFragment();
        Bundle args = new Bundle();
        args.putSparseParcelableArray(ARG_ARTISTS, artists);
        args.putParcelable(ARG_ARTIST, artist);
        args.putString(ARG_SELECTION, selection);
        args.putString(ARG_DB_ORDER_BY, dbOrderBy);
        fragment.setArguments(args);
        return fragment;
    }
}
