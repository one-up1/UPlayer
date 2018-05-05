package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArtistsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "UPlayer";

    private static final String ARG_ORDER_BY = "order_by";
    private static final String ARG_SONGS_ORDER_BY = "songs_order_by";

    /*private static final String SQL_QUERY_PLAYED_DURATION =
            "SELECT SUM(" + Song.DURATION + "*" + Song.TIMES_PLAYED + ") FROM " +
                    Song.TABLE_NAME + " WHERE " + Song.ARTIST_ID + "=?";*/

    private DbHelper dbHelper;
    private List<Artist> artists;

    private ListView listView;
    private ListAdapter listAdapter;
    private Parcelable listViewState;
    private boolean sortOrderReversed;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "ArtistsFragment.onCreate()");
        super.onCreate(savedInstanceState);

        dbHelper = new DbHelper(getActivity());
        artists = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "ArtistsFragment.onCreateView()");
        dbHelper.queryArtists(artists, getArguments().getString(ARG_ORDER_BY));
        if (sortOrderReversed) {
            Collections.reverse(artists);
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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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

        Artist artist = artists.get(((AdapterView.AdapterContextMenuInfo)
                item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.info:
                dbHelper.queryArtist(artist);
                Util.showInfoDialog(getContext(), artist.getArtist(), R.string.info_message_artist,
                        artist.getLastPlayed() == 0 ?
                                getString(R.string.never) :
                                Util.formatDateTimeAgo(artist.getLastPlayed()),
                        artist.getTimesPlayed(),
                        "foo",
                        artist.getLastSongAdded() == 0 ?
                                getString(R.string.na) :
                                Util.formatDateTimeAgo(artist.getLastSongAdded()));
                //TODO: Artist info.
                /*long playedDuration;
                try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
                    playedDuration = DbHelper.queryLong(db, SQL_QUERY_PLAYED_DURATION,
                            new String[]{Integer.toString(artist.getId())});

                    Util.showInfoDialog(getContext(), artist.getArtist(), R.string.info_message_artist,
                            artist.getLastPlayed() == 0 ?
                                    getString(R.string.never) :
                                    Util.formatDateTimeAgo(artist.getLastPlayed()),
                            DbHelper.queryInt(db, "SELECT times_played FROM artists WHERE _id=" + artist.getId(), null),
                            Util.formatDuration(playedDuration),
                            artist.getDateModified() == 0 ?
                                    getString(R.string.na) :
                                    Util.formatDateTimeAgo(artist.getDateModified())
                    );
                }*/
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
    public void onDestroy() {
        Log.d(TAG, "ArtistsFragment.onDestroy()");
        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == listView) {
            startActivity(new Intent(getContext(), SongsActivity.class)
                    .putExtras(SongsFragment.getArguments(
                            artists.get(position).getId(),
                            getArguments().getString(ARG_SONGS_ORDER_BY))));
        }
    }

    public void reverseSortOrder() {
        Collections.reverse(artists);
        listAdapter.notifyDataSetChanged();
        sortOrderReversed = !sortOrderReversed;
    }

    public static ArtistsFragment newInstance(String orderBy, String songsOrderBy) {
        ArtistsFragment fragment = new ArtistsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_BY, orderBy);
        args.putString(ARG_SONGS_ORDER_BY, songsOrderBy);
        fragment.setArguments(args);
        return fragment;
    }

    private class ListAdapter extends ArrayAdapter<Artist> {
        private ListAdapter() {
            super(ArtistsFragment.this.getContext(), android.R.layout.simple_list_item_1, artists);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            view.setTextColor(artists.get(position).getTimesPlayed() == 0 ?
                    Color.BLUE : Color.BLACK);
            return view;
        }
    }
}