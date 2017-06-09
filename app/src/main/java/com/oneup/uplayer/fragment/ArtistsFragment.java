package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ArtistsFragment extends Fragment implements BaseArgs, AdapterView.OnItemClickListener {
    private SparseArray<Artist> artists;
    private int joinedSortBy;

    private List<Artist> objects;
    private ListView lvArtists;

    public ArtistsFragment() {
    }

    public static ArtistsFragment newInstance(SparseArray<Artist> artists, int joinedSortBy) {
        ArtistsFragment fragment = new ArtistsFragment();
        Bundle args = new Bundle();
        args.putSparseParcelableArray(ARG_ARTISTS, artists);
        args.putInt(ARG_JOINED_SORT_BY, joinedSortBy);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.fragment_artists, container, false);

        artists = getArguments().getSparseParcelableArray(ARG_ARTISTS);
        joinedSortBy = getArguments().getInt(ARG_JOINED_SORT_BY);

        objects = new ArrayList<>();
        for (int i = 0; i < artists.size(); i++) {
            objects.add(artists.valueAt(i));
        }

        Comparator<? super Artist> c;
        switch (joinedSortBy) {
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
                        int i = Long.compare(artist2.getLastPlayed(), artist1.getLastPlayed());
                        return i == 0 ? artist1.getArtist().compareTo(artist2.getArtist()) : i;
                    }
                };
                break;
            case SORT_BY_TIMES_PLAYED:
                c = new Comparator<Artist>() {

                    @Override
                    public int compare(Artist artist1, Artist artist2) {
                        int i = Integer.compare(artist2.getTimesPlayed(), artist1.getTimesPlayed());
                        return i == 0 ? artist1.getArtist().compareTo(artist2.getArtist()) : i;
                    }
                };
                break;
            default:
                throw new IllegalArgumentException("Invalid songs sort by");
        }
        Collections.sort(objects, c);

        lvArtists = (ListView) ret.findViewById(R.id.lvArtists);
        lvArtists.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, objects));
        lvArtists.setOnItemClickListener(this);

        return ret;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == lvArtists) {
            Bundle args = new Bundle();
            args.putSparseParcelableArray(ARG_ARTISTS, artists);
            args.putInt(ARG_JOINED_SORT_BY, joinedSortBy);
            args.putString(ARG_SELECTION, Song.ARTIST_ID + "=" + objects.get(position).getId());
            startActivity(new Intent(getContext(), SongsActivity.class).putExtras(args));
        }
    }
}