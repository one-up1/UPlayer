package com.oneup.uplayer.fragment;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.SongAdapter;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.obj.Song;

import java.util.ArrayList;

public class SongsFragment extends Fragment implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {
    public static final int SOURCE_ANDROID = 1;
    public static final int SOURCE_DB = 2;

    private static final String TAG = "UPlayer";

    private static final String ARG_SOURCE = "source";
    private static final String ARG_URI = "uri";
    private static final String ARG_ID_COLUMN = "id_column";
    private static final String ARG_SELECTION = "selection";
    private static final String ARG_SELECTION_ARGS = "selection_args";
    private static final String ARG_ORDER_BY = "order_by";

    private ListView lvSongs;
    private ArrayList<Song> songs;

    public SongsFragment() {
    }

    public static SongsFragment newInstance(int source, Uri uri, String idColumn, String selection,
                                            String[] selectionArgs, String orderBy) {
        SongsFragment fragment = new SongsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SOURCE, source);
        args.putParcelable(ARG_URI, uri);
        args.putString(ARG_ID_COLUMN, idColumn);
        args.putString(ARG_SELECTION, selection);
        args.putStringArray(ARG_SELECTION_ARGS, selectionArgs);
        args.putString(ARG_ORDER_BY, orderBy);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.fragment_songs, container, false);
        ListView lvSongs = (ListView) ret.findViewById(R.id.lvSongs);
        lvSongs.setOnItemClickListener(this);
        lvSongs.setOnItemLongClickListener(this);

        String idColumn = getArguments().getString(ARG_ID_COLUMN);
        DbOpenHelper dbOpenHelper = new DbOpenHelper(getActivity());
        Cursor cursor;
        switch (getArguments().getInt(ARG_SOURCE, 0)) {
            case SOURCE_ANDROID:
                cursor = getContext().getContentResolver().query(
                        (Uri) getArguments().getParcelable(ARG_URI), new String[] {
                                idColumn,
                                Song.TITLE,
                                Song.ARTIST_ID,
                                Song.ARTIST,
                                Song.YEAR
                        },
                        getArguments().getString(ARG_SELECTION),
                        getArguments().getStringArray(ARG_SELECTION_ARGS),
                        getArguments().getString(ARG_ORDER_BY));
                if (cursor == null) {
                    Log.w(TAG, "No cursor");
                    return ret;
                }
                break;
            case SOURCE_DB:
                cursor = dbOpenHelper.getReadableDatabase().query(Song.TABLE_NAME, null,
                        getArguments().getString(ARG_SELECTION),
                        getArguments().getStringArray(ARG_SELECTION_ARGS),
                        null, null, getArguments().getString(ARG_ORDER_BY));
                break;
            default:
                throw new IllegalArgumentException("Invalid source");
        }

        try {
            songs = new ArrayList<>();
            int iId = cursor.getColumnIndex(idColumn);
            int iTitle = cursor.getColumnIndex(Song.TITLE);
            int iArtistId = cursor.getColumnIndex(Song.ARTIST_ID);
            int iArtist = cursor.getColumnIndex(Song.ARTIST);
            int iYear = cursor.getColumnIndex(Song.YEAR);

            int iLastPlayed = cursor.getColumnIndex(Song.LAST_PLAYED);
            int iTimesPlayed = cursor.getColumnIndex(Song.TIMES_PLAYED);
            int iStarred = cursor.getColumnIndex(Song.STARRED);

            while (cursor.moveToNext()) {
                Song song = new Song();

                song.setId(cursor.getLong(iId));
                song.setTitle(cursor.getString(iTitle));
                song.setArtistId(cursor.getLong(iArtistId));
                song.setArtist(cursor.getString(iArtist));
                song.setYear(cursor.getInt(iYear));

                if (iLastPlayed != -1) {
                    song.setLastPlayed(cursor.getLong(iLastPlayed));
                }
                if (iTimesPlayed != -1) {
                    song.setTimesPlayed(cursor.getInt(iTimesPlayed));
                }
                if (iStarred != -1) {
                    song.setStarred(cursor.getInt(iTimesPlayed) == 1);
                }

                songs.add(song);
            }
        } finally {
            cursor.close();
        }

        Log.d(TAG, "Queried " + songs.size() + " songs");
        lvSongs.setAdapter(new ListAdapter(getContext(), songs));

        return ret;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "Playing " + songs.size() + " songs, songIndex=" + position +
                " (" + songs.get(position) + ")");
        getContext().startService(new Intent(getContext(), MainService.class)
                .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_START)
                .putExtra(MainService.ARG_SONGS, songs)
                .putExtra(MainService.ARG_SONG_INDEX, position));
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        DbOpenHelper dbOpenHelper = new DbOpenHelper(getActivity());
        Song song = songs.get(position);

        try (SQLiteDatabase db = dbOpenHelper.getWritableDatabase()) {
            String selection = Song._ID + "=?";
            String[] selectionArgs = new String[] { Long.toString(song.getId()) };
            ContentValues values = new ContentValues();
            try (Cursor c = db.query(Song.TABLE_NAME, new String[] { Song.STARRED },
                    selection, selectionArgs, null, null, null)) {
                if (c.moveToNext()) {
                    song.setStarred(c.getInt(0) != 1);
                    values.put(Song.STARRED, song.isStarred() ? 1 : 0);
                    db.update(Song.TABLE_NAME, values, selection, selectionArgs);
                } else {
                    song.setStarred(true);
                    values.put(BaseColumns._ID, song.getId());
                    values.put(MediaStore.MediaColumns.TITLE, song.getTitle());
                    if (song.getArtistId() > 0) {
                        values.put(MediaStore.Audio.AudioColumns.ARTIST_ID, song.getArtistId());
                        values.put(MediaStore.Audio.AudioColumns.ARTIST, song.getArtist());
                    }
                    if (song.getYear() > 0) {
                        values.put(MediaStore.Audio.AudioColumns.YEAR, song.getYear());
                    }
                    values.put(Song.STARRED, song.isStarred() ? 1 : 0);
                    db.insert(Song.TABLE_NAME, null, values);
                }
            }
        }

        if (song.isStarred()) {
            Toast.makeText(getContext(), R.string.song_starred, Toast.LENGTH_SHORT).show();
            song.setStarred(false);
        } else {
            Toast.makeText(getContext(), R.string.song_unstarred, Toast.LENGTH_SHORT).show();
            song.setStarred(true);
        }

        return true;
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
