package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.graphics.Color;
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
    private static final String TAG = "UPlayer";

    private SparseArray<Artist> artists;
    private int joinedSortBy;

    private ArrayList<Artist> objects;
    private ListView listView;
    private ListAdapter listAdapter;
    private Parcelable listViewState;
    private boolean sortOrderReversed;

    public ArtistsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "ArtistsFragment.onCreateView()");
        artists = getArguments().getSparseParcelableArray(ARG_ARTISTS);
        joinedSortBy = getArguments().getInt(ARG_JOINED_SORT_BY);

        if (objects == null) {
            objects = new ArrayList<>();
        } else {
            objects.clear();
        }
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
        if (sortOrderReversed) {
            Collections.reverse(objects);
        }

        if (listView == null) {
            listView = new ListView(getContext());
            listView.setOnItemClickListener(this);
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
        Log.d(TAG, "ArtistsFragment.onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        if (listViewState != null) {
            listView.onRestoreInstanceState(listViewState);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v == listView) {
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
                        artist.getTimesPlayed())
                );
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "ArtistsFragment.onPause()");
        listViewState = listView.onSaveInstanceState();
        super.onPause();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == listView) {
            Bundle args = new Bundle();
            args.putSparseParcelableArray(ARG_ARTISTS, artists);
            args.putInt(ARG_JOINED_SORT_BY, joinedSortBy);
            args.putString(ARG_SELECTION, Song.ARTIST_ID + "=" + objects.get(position).getId());
            startActivity(new Intent(getContext(), SongsActivity.class).putExtras(args));
        }
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