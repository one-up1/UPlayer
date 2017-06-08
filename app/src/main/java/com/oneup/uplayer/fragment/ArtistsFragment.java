package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.obj.Artist;
import com.oneup.uplayer.db.obj.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ArtistsFragment extends Fragment implements BaseArgs, AdapterView.OnItemClickListener {
    private static final String ARG_ARTISTS = "artists";

    private ListView lvArtists;
    private ArrayList<Artist> artists;

    public ArtistsFragment() {
    }

    public static ArtistsFragment newInstance(ArrayList<Artist> artists, int sortBy) {
        ArtistsFragment fragment = new ArtistsFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_ARTISTS, artists);
        args.putInt(ARG_SORT_BY, sortBy);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.fragment_artists, container, false);
        lvArtists = (ListView) ret.findViewById(R.id.lvArtists);
        lvArtists.setOnItemClickListener(this);

        artists = getArguments().getParcelableArrayList(ARG_ARTISTS);
        Comparator<? super Artist> c;
        switch (getArguments().getInt(ARG_SORT_BY)) {
            case SORT_BY_NAME:
                c = new Comparator<Artist>() {

                    @Override
                    public int compare(Artist artist1, Artist artist2) {
                        return artist1.getArtist().compareTo(artist2.getArtist());
                    }
                };
                break;
            case SORT_BY_LAST_PLAYED:
                c = new Comparator<Artist>() {

                    @Override
                    public int compare(Artist artist1, Artist artist2) {
                        return Long.compare(artist2.getLastPlayed(), artist1.getLastPlayed());
                    }
                };
                break;
            case SORT_BY_TIMES_PLAYED:
                c = new Comparator<Artist>() {

                    @Override
                    public int compare(Artist artist1, Artist artist2) {
                        return Long.compare(artist2.getTimesPlayed(), artist1.getTimesPlayed());
                    }
                };
                break;
            default:
                c = null;
                break;
        }

        if (c != null) {
            Collections.sort(artists, c);
        }

        lvArtists.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, artists));

        return ret;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == lvArtists) {
            startActivity(new Intent(getContext(), SongsActivity.class)
                    .putExtra(SongsActivity.ARG_SOURCE, SongsFragment.SOURCE_JOINED)
                    .putExtra(SongsActivity.ARG_URI, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                    .putExtra(SongsActivity.ARG_ID_COLUMN, Song._ID)
                    .putExtra(SongsActivity.ARG_SELECTION, Song.ARTIST_ID + "=?")
                    .putExtra(SongsActivity.ARG_SELECTION_ARGS,
                            new String[]{Long.toString(artists.get(position).getId())})
                    .putExtra(SongsActivity.ARG_SORT_BY,
                            getArguments().getInt(ARG_SORT_BY))
            );
        }
    }
}