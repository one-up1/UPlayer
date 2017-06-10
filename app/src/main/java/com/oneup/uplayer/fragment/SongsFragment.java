package com.oneup.uplayer.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.oneup.uplayer.SongAdapter;
import com.oneup.uplayer.SongsListView;
import com.oneup.uplayer.activity.MainActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SongsFragment extends Fragment implements BaseArgs, AdapterView.OnItemClickListener,
        SongsListView.OnDataSetChangedListener, SongsListView.OnSongDeletedListener {
    private static final String TAG = "UPlayer";

    private SparseArray<Artist> artists;
    private int joinedSortBy;
    private String selection;
    private String dbOrderBy;

    private ArrayList<Song> songs;
    private ListAdapter listAdapter;
    private SongsListView slvSongs;

    public SongsFragment() {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.fragment_songs, container, false);

        artists = getArguments().getSparseParcelableArray(ARG_ARTISTS);
        joinedSortBy = getArguments().getInt(ARG_JOINED_SORT_BY);
        selection = getArguments().getString(ARG_SELECTION);
        dbOrderBy = getArguments().getString(ARG_DB_ORDER_BY);

        Log.d(TAG, "Querying songs");
        songs = new ArrayList<>();
        SparseArray<Song> songs;
        Song song;

        if (joinedSortBy > 0) {
            try (Cursor c = getContext().getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{Song._ID, Song.TITLE, Song.ARTIST_ID, Song.YEAR},
                    selection, null, null)) {
                if (c == null) {
                    Log.wtf(TAG, "No cursor");
                    return ret;
                }

                int iId = c.getColumnIndex(Song._ID);
                int iTitle = c.getColumnIndex(Song.TITLE);
                int iArtistId = c.getColumnIndex(Song.ARTIST_ID);
                int iYear = c.getColumnIndex(Song.YEAR);
                songs = new SparseArray<>();
                while (c.moveToNext()) {
                    song = new Song();
                    song.setId(c.getInt(iId));
                    song.setTitle(c.getString(iTitle));
                    song.setArtist(artists.get(c.getInt(iArtistId)));
                    song.setYear(c.getInt(iYear));
                    songs.put(song.getId(), song);
                }
            }
        } else {
            songs = null;
        }

        DbOpenHelper dbOpenHelper = new DbOpenHelper(getActivity());
        try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
            try (Cursor c = db.query(Song.TABLE_NAME, songs == null ? null : new String[]{
                            Song._ID, Song.LAST_PLAYED, Song.TIMES_PLAYED, Song.STARRED},
                    selection, null, null, null, dbOrderBy)) {
                while (c.moveToNext()) {
                    int id = c.getInt(0);
                    if (songs == null) {
                        song = new Song();
                        song.setId(id);
                        song.setTitle(c.getString(1));
                        song.setArtist(artists.get(c.getInt(2)));
                        song.setYear(c.getInt(3));
                        song.setLastPlayed(c.getLong(4));
                        song.setTimesPlayed(c.getInt(5));
                        song.setStarred(c.getLong(6));
                        this.songs.add(song);
                    } else {
                        song = songs.get(id);
                        if (song == null) {
                            dbOpenHelper.deleteSong(id);
                        } else {
                            song.setLastPlayed(c.getLong(1));
                            song.setTimesPlayed(c.getInt(2));
                            song.setStarred(c.getLong(3));
                        }
                    }
                }
            }
        }

        if (songs != null) {
            for (int i = 0; i < songs.size(); i++) {
                this.songs.add(songs.valueAt(i));
            }

            Comparator<? super Song> c;
            switch (joinedSortBy) {
                case SORT_BY_NAME:
                    c = new Comparator<Song>() {

                        @Override
                        public int compare(Song song1, Song song2) {
                            return song1.getTitle().compareTo(song2.getTitle());
                        }
                    };
                    break;
                case SORT_BY_LAST_PLAYED:
                    c = new Comparator<Song>() {

                        @Override
                        public int compare(Song song1, Song song2) {
                            int i = Long.compare(song2.getLastPlayed(), song1.getLastPlayed());
                            return i == 0 ? song1.getTitle().compareTo(song2.getTitle()) : i;
                        }
                    };
                    break;
                case SORT_BY_TIMES_PLAYED:
                    c = new Comparator<Song>() {

                        @Override
                        public int compare(Song song1, Song song2) {
                            int i = Integer.compare(song2.getTimesPlayed(), song1.getTimesPlayed());
                            return i == 0 ? song1.getTitle().compareTo(song2.getTitle()) : i;
                        }
                    };
                    break;
                default:
                    throw new IllegalArgumentException("Invalid songs sort by");
            }
            Collections.sort(this.songs, c);
        }

        Log.d(TAG, "Queried " + this.songs.size() + " songs");

        slvSongs = (SongsListView) ret.findViewById(R.id.slvSongs);
        listAdapter = new ListAdapter(getContext(), this.songs);
        slvSongs.setAdapter(listAdapter);
        slvSongs.setOnItemClickListener(this);
        slvSongs.setOnDataSetChangedListener(this);
        slvSongs.setOnSongDeletedListener(this);
        registerForContextMenu(slvSongs);

        return ret;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.list_item_song, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return slvSongs.onContextItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == slvSongs) {
            Log.d(TAG, "Playing " + songs.size() + " songs, songIndex=" + position +
                    " (" + songs.get(position) + ")");
            getContext().startService(new Intent(getContext(), MainService.class)
                    .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_START)
                    .putExtra(MainService.ARG_SONGS, songs)
                    .putExtra(MainService.ARG_SONG_INDEX, position));
        }
    }

    @Override
    public void onDataSetChanged() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).notifyDataSetChanged();
        }
    }

    @Override
    public void onSongDeleted(Song song) {
        songs.remove(song);
        listAdapter.notifyDataSetChanged();
    }

    private class ListAdapter extends SongAdapter implements View.OnClickListener {
        private ListAdapter(Context context, ArrayList<Song> songs) {
            super(context, songs);
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
            ImageButton ibPlayNext = (ImageButton) view.findViewById(R.id.ibPlayNext);
            ibPlayNext.setTag(song);

            ImageButton ibPlayLast = (ImageButton) view.findViewById(R.id.ibPlayLast);
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
}
