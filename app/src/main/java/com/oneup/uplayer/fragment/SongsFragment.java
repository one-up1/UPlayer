package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
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
import android.widget.LinearLayout;
import android.widget.Toast;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.activity.MainActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbComparator;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.widget.SongAdapter;
import com.oneup.uplayer.widget.SongsListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SongsFragment extends Fragment implements BaseArgs, AdapterView.OnItemClickListener,
        SongsListView.OnDataSetChangedListener, SongsListView.OnSongDeletedListener {
    public static final int SORT_BY_DATE_ADDED = 4;

    private static final String TAG = "UPlayer";

    private SparseArray<Artist> artists;
    private int joinedSortBy;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "SongsFragment.onCreateView()");
        artists = getArguments().getSparseParcelableArray(ARG_ARTISTS);
        joinedSortBy = getArguments().getInt(ARG_JOINED_SORT_BY);
        selection = getArguments().getString(ARG_SELECTION);
        dbOrderBy = getArguments().getString(ARG_DB_ORDER_BY);
        Log.d(TAG, artists.size() + " artists, joinedSortBy=" + joinedSortBy +
                ", selection=" + selection + ", dbOrderBy=" + dbOrderBy);

        SparseArray<Song> songs;
        Song song;

        if (joinedSortBy > 0) {
            try (Cursor c = getContext().getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{Song._ID, Song.TITLE,
                            Song.ARTIST_ID, Song.DATE_ADDED, Song.YEAR, Song.DURATION},
                    selection, null, null)) {
                if (c == null) {
                    Log.wtf(TAG, "No cursor");
                    return super.onCreateView(inflater, container, savedInstanceState);
                }

                int iId = c.getColumnIndex(Song._ID);
                int iTitle = c.getColumnIndex(Song.TITLE);
                int iArtistId = c.getColumnIndex(Song.ARTIST_ID);
                int iDateAdded = c.getColumnIndex(Song.DATE_ADDED);
                int iYear = c.getColumnIndex(Song.YEAR);
                int iDuration = c.getColumnIndex(Song.DURATION);
                songs = new SparseArray<>();
                while (c.moveToNext()) {
                    song = new Song();
                    song.setTitle(c.getString(iTitle));
                    if (setArtist(song, c.getInt(iArtistId))) {
                        song.setId(c.getInt(iId));
                        song.setDateAdded(c.getLong(iDateAdded));
                        song.setYear(c.getInt(iYear));
                        song.setDuration(c.getInt(iDuration));
                        songs.put(song.getId(), song);
                    }
                }
            }
        } else {
            songs = null;
        }

        dbOpenHelper = new DbOpenHelper(getActivity());
        if (objects == null) {
            objects = new ArrayList<>();
        } else {
            objects.clear();
        }
        try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
            try (Cursor c = db.query(Song.TABLE_NAME, songs == null ? null :
                            new String[]{Song._ID, Song.DATE_ADDED,
                                    Song.LAST_PLAYED, Song.TIMES_PLAYED, Song.BOOKMARKED, Song.TAG},
                    selection, null, null, null, dbOrderBy)) {
                while (c.moveToNext()) {
                    int id = c.getInt(0);
                    if (songs == null) {
                        song = new Song();
                        song.setTitle(c.getString(1));
                        if (setArtist(song, c.getInt(2))) {
                            song.setId(id);
                            song.setDateAdded(c.getLong(3));
                            song.setYear(c.getInt(4));
                            song.setDuration(c.getInt(5));
                            song.setLastPlayed(c.getLong(6));
                            song.setTimesPlayed(c.getInt(7));
                            song.setBookmarked(c.getLong(8));
                            song.setTag(c.getString(9));
                            objects.add(song);
                        }
                    } else {
                        song = songs.get(id);
                        if (song == null) {
                            dbOpenHelper.deleteSong(id);
                        } else {
                            // Overwrite the DATE_ADDED value from the MediaStore,
                            // as this value may be lost after restoring a backup.
                            song.setDateAdded(c.getLong(1));

                            // Set the other values not present in the MediaStore.
                            song.setLastPlayed(c.getLong(2));
                            song.setTimesPlayed(c.getInt(3));
                            song.setBookmarked(c.getLong(4));
                            song.setTag(c.getString(5));
                        }
                    }
                }
            }
        }

        if (songs != null) {
            for (int i = 0; i < songs.size(); i++) {
                objects.add(songs.valueAt(i));
            }

            Comparator<? super Song> c;
            switch (joinedSortBy) {
                case SORT_BY_NAME:
                    c = new Comparator<Song>() {

                        @Override
                        public int compare(Song song1, Song song2) {
                            return DbComparator.sortByName(song1.getTitle(), song2.getTitle());
                        }
                    };
                    break;
                case SORT_BY_LAST_PLAYED:
                    c = new Comparator<Song>() {

                        @Override
                        public int compare(Song song1, Song song2) {
                            return DbComparator.sortByLastPlayed(
                                    song1.getLastPlayed(), song2.getLastPlayed(),
                                    song1.getTitle(), song2.getTitle());
                        }
                    };
                    break;
                case SORT_BY_TIMES_PLAYED:
                    c = new Comparator<Song>() {

                        @Override
                        public int compare(Song song1, Song song2) {
                            return DbComparator.sortByTimesPlayed(
                                    song1.getTimesPlayed(), song2.getTimesPlayed(),
                                    song1.getLastPlayed(), song2.getLastPlayed(),
                                    song1.getTitle(), song2.getTitle());
                        }
                    };
                    break;
                case SORT_BY_DATE_ADDED:
                    c = new Comparator<Song>() {

                        @Override
                        public int compare(Song song1, Song song2) {
                            return DbComparator.sortByDateAdded(
                                    song1.getDateAdded(), song2.getDateAdded(),
                                    song1.getTitle(), song2.getTitle());
                        }
                    };
                    break;
                default:
                    throw new IllegalArgumentException("Invalid songs sort by");
            }
            Collections.sort(objects, c);
        }
        if (sortOrderReversed) {
            Collections.reverse(objects);
        }

        Log.d(TAG, "Queried " + objects.size() + " songs");
        if (getActivity() instanceof SongsActivity) {
            getActivity().setTitle(getString(R.string.song_count_duration, objects.size(),
                    Util.formatDuration(Song.getDuration(objects, 0))));
        }

        if (listView == null) {
            listView = new SongsListView(getActivity());
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
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
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return getUserVisibleHint() && listView.onContextItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    private boolean setArtist(Song song, int id) {
        Artist artist = artists.get(id);
        if (artist == null) {
            Log.w(TAG, "Artist for song '" + song + "' not found");
            return false;
        }

        song.setArtist(artist);
        return true;
    }

    private class ListAdapter extends SongAdapter implements View.OnClickListener {
        private ListAdapter() {
            super(getContext(), objects);
        }

        @Override
        public void addButtons(LinearLayout llButtons) {
            ImageButton ibPlayNext = new ImageButton(getContext());
            ibPlayNext.setId(R.id.ibPlayNext);
            ibPlayNext.setImageResource(R.drawable.ic_play_next);
            ibPlayNext.setContentDescription(getString(R.string.play_next));
            ibPlayNext.setOnClickListener(this);
            llButtons.addView(ibPlayNext);

            ImageButton ibPlayLast = new ImageButton(getContext());
            ibPlayLast.setId(R.id.ibPlayLast);
            ibPlayLast.setImageResource(R.drawable.ic_play_last);
            ibPlayLast.setContentDescription(getString(R.string.play_last));
            ibPlayLast.setOnClickListener(this);
            llButtons.addView(ibPlayLast);
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

    public static SongsFragment newInstance(SparseArray<Artist> artists, int joinedSortBy,
                                            String selection, String dbOrderBy) {
        SongsFragment fragment = new SongsFragment();
        Bundle args = new Bundle();
        args.putSparseParcelableArray(ARG_ARTISTS, artists);
        args.putInt(ARG_JOINED_SORT_BY, joinedSortBy);
        args.putString(ARG_SELECTION, selection);
        args.putString(ARG_DB_ORDER_BY, dbOrderBy);
        fragment.setArguments(args);
        return fragment;
    }
}
