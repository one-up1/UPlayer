package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.obj.Artist;
import com.oneup.uplayer.db.obj.Song;

import java.util.ArrayList;

public class ArtistsFragment extends Fragment
        implements AdapterView.OnItemClickListener {
    public static final int SOURCE_ANDROID = 1;
    public static final int SOURCE_DB = 2;

    private static final String TAG = "UPlayer";

    private static final String ARG_SOURCE = "source";
    private static final String ARG_ORDER_BY = "order_by";
    private static final String ARG_SONGS_ORDER_BY = "songs_order_by";

    private ArrayList<Artist> artists;

    public ArtistsFragment() {
    }

    public static ArtistsFragment newInstance(int source, String orderBy, String songsOrderBy) {
        ArtistsFragment fragment = new ArtistsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SOURCE, source);
        args.putString(ARG_ORDER_BY, orderBy);
        args.putString(ARG_SONGS_ORDER_BY, songsOrderBy);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.fragment_artists, container, false);
        ListView lvArtists = (ListView) ret.findViewById(R.id.lvArtists);
        lvArtists.setOnItemClickListener(this);

        DbOpenHelper dbOpenHelper = new DbOpenHelper(getActivity());
        String[] columns = {Artist._ID, Artist.ARTIST};
        Cursor cursor;
        switch (getArguments().getInt(ARG_SOURCE)) {
            case SOURCE_ANDROID:
                cursor = getContext().getContentResolver().query(
                        MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, columns, null, null,
                        getArguments().getString(ARG_ORDER_BY));
                if (cursor == null) {
                    Log.w(TAG, "No cursor");
                    return ret;
                }
                break;
            case SOURCE_DB:
                cursor = dbOpenHelper.getReadableDatabase().query(Artist.TABLE_NAME, columns,
                        null, null, null, null, getArguments().getString(ARG_ORDER_BY));
                break;
            default:
                throw new IllegalArgumentException("Invalid source");
        }

        try {
            artists = new ArrayList<>();
            int iId = cursor.getColumnIndex(Artist._ID);
            int iArtist = cursor.getColumnIndex(Artist.ARTIST);
            while (cursor.moveToNext()) {
                Artist artist = new Artist();
                artist.setId(cursor.getLong(iId));
                artist.setArtist(cursor.getString(iArtist));
                artists.add(artist);
            }
        } finally {
            cursor.close();
            dbOpenHelper.close();
        }

        Log.d(TAG, "Queried " + artists.size() + " artists");
        lvArtists.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, artists));

        return ret;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(new Intent(getContext(), SongsActivity.class)
                .putExtra(SongsActivity.ARG_SOURCE, getArguments().getInt(ARG_SOURCE))
                .putExtra(SongsActivity.ARG_URI, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                .putExtra(SongsActivity.ARG_ID_COLUMN, Song._ID)
                .putExtra(SongsActivity.ARG_SELECTION, Song.ARTIST_ID + "=?")
                .putExtra(SongsActivity.ARG_SELECTION_ARGS,
                        new String[]{Long.toString(artists.get(position).getId())})
                .putExtra(SongsActivity.ARG_ORDER_BY,
                        getArguments().getString(ARG_SONGS_ORDER_BY))
        );
    }
}