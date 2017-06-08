package com.oneup.uplayer.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
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
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.obj.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SongsFragment extends Fragment implements BaseArgs, AdapterView.OnItemClickListener,
        SongsListView.OnDataSetChangedListener {
    public static final int SOURCE_MEDIA_STORE = 1;
    public static final int SOURCE_DB = 2;
    public static final int SOURCE_JOINED = 3;

    private static final String TAG = "UPlayer";

    private static final String ARG_SOURCE = "source";
    private static final String ARG_URI = "uri";
    private static final String ARG_ID_COLUMN = "id_column";
    private static final String ARG_SELECTION = "selection";
    private static final String ARG_SELECTION_ARGS = "selection_args";
    private static final String ARG_ORDER_BY = "order_by";

    private SongsListView slvSongs;
    private ArrayList<Song> songs;

    public SongsFragment() {
    }

    public static SongsFragment newInstance(int source, Uri uri, String idColumn, String selection,
                                            String[] selectionArgs, String orderBy, int sortBy) {
        SongsFragment fragment = new SongsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SOURCE, source);
        args.putParcelable(ARG_URI, uri);
        args.putString(ARG_ID_COLUMN, idColumn);
        args.putString(ARG_SELECTION, selection);
        args.putStringArray(ARG_SELECTION_ARGS, selectionArgs);
        args.putString(ARG_ORDER_BY, orderBy);
        args.putInt(ARG_SORT_BY, sortBy);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.fragment_songs, container, false);

        int source = getArguments().getInt(ARG_SOURCE);
        Uri uri = getArguments().getParcelable(ARG_URI);
        String idColumn = getArguments().getString(ARG_ID_COLUMN);
        String selection = getArguments().getString(ARG_SELECTION);
        String[] selectionArgs = getArguments().getStringArray(ARG_SELECTION_ARGS);
        String orderBy = getArguments().getString(ARG_ORDER_BY);
        int sortBy = getArguments().getInt(ARG_SORT_BY);

        Log.d(TAG, "Querying songs");
        songs = new ArrayList<>();
        LongSparseArray<Song> songs;
        Song song;

        if (source == SOURCE_JOINED) {
            songs = new LongSparseArray<>();
        } else {
            songs = null;
        }

        if (source == SOURCE_MEDIA_STORE || source == SOURCE_JOINED) {
            if (uri == null) {
                throw new IllegalArgumentException("No URI");
            }

            try (Cursor c = getContext().getContentResolver().query(uri,
                    new String[]{idColumn, Song.TITLE, Song.ARTIST_ID, Song.ARTIST, Song.YEAR},
                    selection, selectionArgs, orderBy)) {
                if (c == null) {
                    Log.wtf(TAG, "No cursor");
                    return ret;
                }

                int iId = c.getColumnIndex(idColumn);
                int iTitle = c.getColumnIndex(Song.TITLE);
                int iArtistId = c.getColumnIndex(Song.ARTIST_ID);
                int iArtist = c.getColumnIndex(Song.ARTIST);
                int iYear = c.getColumnIndex(Song.YEAR);
                while (c.moveToNext()) {
                    song = new Song();
                    song.setId(c.getLong(iId));
                    song.setTitle(c.getString(iTitle));
                    song.setArtistId(c.getLong(iArtistId));
                    song.setArtist(c.getString(iArtist));
                    song.setYear(c.getInt(iYear));
                    if (songs == null) {
                        this.songs.add(song);
                    } else {
                        songs.put(song.getId(), song);
                    }
                }
            }
        }

        if (source == SOURCE_DB || source == SOURCE_JOINED) {
            DbOpenHelper dbOpenHelper = new DbOpenHelper(getActivity());
            try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
                try (Cursor c = db.query(Song.TABLE_NAME,
                        songs == null ? null : new String[]{Song._ID, Song.LAST_PLAYED,
                                Song.TIMES_PLAYED, Song.STARRED},
                        selection, selectionArgs, null, null, orderBy)) {
                    while (c.moveToNext()) {
                        if (songs == null) {
                            song = new Song();
                            song.setId(c.getLong(0));
                            song.setTitle(c.getString(1));
                            song.setArtistId(c.getLong(2));
                            song.setArtist(c.getString(3));
                            song.setYear(c.getInt(4));
                            song.setLastPlayed(c.getLong(5));
                            song.setTimesPlayed(c.getInt(6));
                            song.setStarred(c.getLong(7));
                            this.songs.add(song);
                        } else {
                            song = songs.get(c.getLong(0));
                            if (song == null) {
                                Log.i(TAG, "Deleting song");
                                //TODO: Delete song from DB.
                            } else {
                                song.setLastPlayed(c.getLong(1));
                                song.setTimesPlayed(c.getInt(2));
                                song.setStarred(c.getLong(3));
                            }
                        }
                    }
                }
            }
        }

        if (songs != null) {
            // Convert SparseArray to ArrayList.
            for (int i = 0; i < songs.size(); i++) {
                this.songs.add(songs.valueAt(i));
            }

            Comparator<? super Song> c;
            switch (sortBy) {
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
                            return Long.compare(song2.getLastPlayed(), song1.getLastPlayed());
                        }
                    };
                    break;
                case SORT_BY_TIMES_PLAYED:
                    c = new Comparator<Song>() {

                        @Override
                        public int compare(Song song1, Song song2) {
                            return Long.compare(song2.getTimesPlayed(), song1.getTimesPlayed());
                        }
                    };
                    break;
                default:
                    c = null;
                    break;
            }

            if (c != null) {
                Collections.sort(this.songs, c);
            }
        }
        Log.d(TAG, "Queried " + this.songs.size() + " songs");

        slvSongs = (SongsListView) ret.findViewById(R.id.slvSongs);
        slvSongs.setAdapter(new ListAdapter(getContext(), this.songs));
        slvSongs.setOnItemClickListener(this);
        slvSongs.setOnDataSetChangedListener(this);

        return ret;
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
