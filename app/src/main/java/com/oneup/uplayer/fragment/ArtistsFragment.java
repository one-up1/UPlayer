package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Stats;
import com.oneup.uplayer.util.Util;

import java.util.Collections;
import java.util.List;

//TODO: @Nullable / @NotNull anotations?
//TODO: getActivity() vs getContext()

public class ArtistsFragment extends Fragment implements AdapterView.OnItemClickListener {
    public static final int INFO_LAST_SONG_ADDED = 1;
    public static final int INFO_LAST_PLAYED = 2;
    public static final int INFO_TIMES_PLAYED = 3;

    private static final String TAG = "UPlayer";

    private static final String ARG_ORDER_BY = "order_by";
    private static final String ARG_SONGS_ORDER_BY = "songs_order_by";
    private static final String ARG_INFO = "info";

    private DbHelper dbHelper;
    private List<Artist> artists;

    private ListView listView;
    private ListAdapter listAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "ArtistsFragment.onCreate()");
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "ArtistsFragment.onCreateView()");
        if (listView == null) {
            Log.d(TAG, "Creating ListView");
            listView = (ListView) inflater.inflate(R.layout.list_view, container, false);
            listView.setOnItemClickListener(this);
            registerForContextMenu(listView);
        }
        return listView;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "ArtistsFragment.onResume()");
        super.onResume();

        artists = dbHelper.queryArtists(getArguments().getString(ARG_ORDER_BY));

        if (listAdapter == null) {
            Log.d(TAG, "Creating ListAdapter");
            listAdapter = new ListAdapter();
            listView.setAdapter(listAdapter);
        } else {
            Log.d(TAG, "Calling ListAdapter.notifyDataSetChanged()");
            listAdapter.notifyDataSetChanged();
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
            //TODO: Or the wrong fragment may receive the onContextItemSelected() call?
            return false;
        }

        Artist artist = artists.get(((AdapterView.AdapterContextMenuInfo)
                item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.info:
                dbHelper.queryArtist(artist);
                Stats stats = dbHelper.queryStats(artist);
                //TODO: Avg prc per artist of total songs, / 0 stats.getSongPlayed() ?
                Util.showInfoDialog(getContext(), artist.getArtist(), R.string.artist_message,
                        artist.getLastSongAdded() == 0 ? getString(R.string.na) :
                                Util.formatDateTimeAgo(artist.getLastSongAdded()),
                        stats.getSongCount(), Util.formatDuration(stats.getSongsDuration()),
                        stats.getSongsPlayed(), Util.formatPercent(
                                (double) stats.getSongsPlayed() / stats.getSongCount()),
                        stats.getSongsUnplayed(), Util.formatPercent(
                                (double) stats.getSongsUnplayed() / stats.getSongCount()),
                        stats.getSongsTagged(), Util.formatPercent(
                                (double) stats.getSongsTagged() / stats.getSongCount()),
                        stats.getSongsUntagged(), Util.formatPercent(
                                (double) stats.getSongsUntagged() / stats.getSongCount()),
                        artist.getLastPlayed() == 0 ?
                                getString(R.string.never) :
                                Util.formatDateTimeAgo(artist.getLastPlayed()),
                        artist.getTimesPlayed(), Util.formatDuration(stats.getPlayedDuration()),
                        Math.round((double) artist.getTimesPlayed() / stats.getSongsPlayed()),
                        Util.formatDuration(stats.getPlayedDuration() / stats.getSongsPlayed()));

                return true;
            default:
                return super.onContextItemSelected(item);
        }
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
    }

    public static ArtistsFragment newInstance(String orderBy, String songsOrderBy, int info) {
        ArtistsFragment fragment = new ArtistsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_BY, orderBy);
        args.putString(ARG_SONGS_ORDER_BY, songsOrderBy);
        args.putInt(ARG_INFO, info);
        fragment.setArguments(args);
        return fragment;
    }

    private class ListAdapter extends BaseAdapter {
        private LayoutInflater layoutInflater;

        private ListAdapter() {
            layoutInflater = LayoutInflater.from(ArtistsFragment.this.getContext());
        }

        @Override
        public int getCount() {
            return artists.size();
        }

        @Override
        public Object getItem(int position) {
            return artists.get(position);
        }

        @Override
        public long getItemId(int position) {
            return artists.get(position).getId();
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            Artist artist = artists.get(position);

            if (view == null) {
                view = layoutInflater.inflate(R.layout.list_item_artist, parent, false);
            }

            TextView tvArtist = view.findViewById(R.id.tvArtist);
            tvArtist.setTextColor(artist.getTimesPlayed() == 0 ? Color.BLUE : Color.BLACK);
            tvArtist.setText(artist.getArtist());

            String info;
            switch (getArguments().getInt(ARG_INFO)) {
                case INFO_LAST_SONG_ADDED:
                    info = artist.getLastSongAdded() == 0 ? null :
                            Util.formatDateTimeAgo(artist.getLastSongAdded());
                    break;
                case INFO_LAST_PLAYED:
                    info = artist.getLastPlayed() == 0 ? null :
                            Util.formatDateTimeAgo(artist.getLastPlayed());
                    break;
                case INFO_TIMES_PLAYED:
                    info = Integer.toString(artist.getTimesPlayed());
                    break;
                default:
                    info = null;
            }

            TextView tvInfo = view.findViewById(R.id.tvInfo);
            if (info == null) {
                tvInfo.setVisibility(View.GONE);
            } else {
                tvInfo.setText(info);
                tvInfo.setVisibility(View.VISIBLE);
            }

            return view;
        }
    }
}