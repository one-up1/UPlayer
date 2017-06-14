package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.Util;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbComparator;
import com.oneup.uplayer.db.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ArtistsFragment extends Fragment implements BaseArgs, AdapterView.OnItemClickListener {
    private SparseArray<Artist> artists;
    private int joinedSortBy;

    private ArrayList<Artist> objects;
    private ListView lvArtists;

    public ArtistsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
                        return DbComparator.sortByName(artist1.getArtist(), artist2.getArtist());
                    }
                };
                break;
            case SORT_BY_LAST_PLAYED:
                c = new Comparator<Artist>() {

                    @Override
                    public int compare(Artist artist1, Artist artist2) {
                        return DbComparator.sortByLastPlayed(
                                artist1.getLastPlayed(), artist2.getLastPlayed(),
                                artist1.getArtist(), artist2.getArtist());
                    }
                };
                break;
            case SORT_BY_TIMES_PLAYED:
                c = new Comparator<Artist>() {

                    @Override
                    public int compare(Artist artist1, Artist artist2) {
                        return DbComparator.sortByTimesPlayed(
                                artist1.getTimesPlayed(), artist2.getTimesPlayed(),
                                artist1.getLastPlayed(), artist2.getLastPlayed(),
                                artist1.getArtist(), artist2.getArtist());
                    }
                };
                break;
            default:
                throw new IllegalArgumentException("Invalid songs sort by");
        }
        Collections.sort(objects, c);

        lvArtists = new ListView(getContext());
        lvArtists.setAdapter(new ListAdapter());
        lvArtists.setOnItemClickListener(this);
        registerForContextMenu(lvArtists);

        return lvArtists;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v == lvArtists) {
            getActivity().getMenuInflater().inflate(R.menu.list_item_artist, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!getUserVisibleHint()) {
            // Or the wrong fragment may receive the onContextItemSelected() call.
            return false;
        }

        Artist artist = objects.get(((AdapterView.AdapterContextMenuInfo)
                item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.info:
                Util.showInfoDialog(getContext(), artist.getArtist(), getString(
                        R.string.info_message_artist,
                        artist.getLastPlayed() == 0 ?
                                getString(R.string.never) :
                                Util.formatDateTime(artist.getLastPlayed()),
                        artist.getTimesPlayed()));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
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

    public static ArtistsFragment newInstance(SparseArray<Artist> artists, int joinedSortBy) {
        ArtistsFragment fragment = new ArtistsFragment();
        Bundle args = new Bundle();
        args.putSparseParcelableArray(ARG_ARTISTS, artists);
        args.putInt(ARG_JOINED_SORT_BY, joinedSortBy);
        fragment.setArguments(args);
        return fragment;
    }

    private class ListAdapter extends ArrayAdapter<Artist> {
        private ListAdapter() {
            super(ArtistsFragment.this.getContext(), android.R.layout.simple_list_item_1, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView ret = (TextView) super.getView(position, convertView, parent);
            ret.setTextColor(objects.get(position).getTimesPlayed() == 0 ?
                    Color.BLUE : Color.BLACK);
            return ret;
        }
    }
}