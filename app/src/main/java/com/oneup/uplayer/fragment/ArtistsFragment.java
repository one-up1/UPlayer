package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class ArtistsFragment extends ListFragment<Artist> {
    public static final int INFO_LAST_ADDED = 1; //TODO: Only use these and no orderBy and songsOrderBy?
    public static final int INFO_LAST_PLAYED = 2;
    public static final int INFO_TIMES_PLAYED = 3;

    private static final String TAG = "UPlayer";

    private static final String ARG_ORDER_BY = "order_by";
    private static final String ARG_SONGS_ORDER_BY = "songs_order_by";
    private static final String ARG_INFO = "info";

    public ArtistsFragment() {
        super(R.layout.list_item_artist, R.menu.list_item_artist);
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadData();
    }

    @Override
    protected ArrayList<Artist> loadData() {
        return getDbHelper().queryArtists(getArguments().getString(ARG_ORDER_BY));
    }

    @Override
    protected void setListItemViews(View rootView, int position, Artist artist) {
        // Set artist.
        TextView tvArtist = rootView.findViewById(R.id.tvArtist);
        tvArtist.setTextColor(artist.getTimesPlayed() == 0 ? Color.BLUE : Color.BLACK);
        tvArtist.setText(artist.getArtist());

        // Get info.
        //TODO: Artist info in ListView.
        String info;
        switch (getArguments().getInt(ARG_INFO)) {
            case INFO_LAST_ADDED:
                info = artist.getLastAdded() == 0 ? null
                        : Util.formatTimeAgo(artist.getLastAdded());
                break;
            case INFO_LAST_PLAYED:
                info = artist.getLastPlayed() == 0 ? null :
                        Util.formatTimeAgo(artist.getLastPlayed());
                break;
            case INFO_TIMES_PLAYED:
                info = Integer.toString(artist.getTimesPlayed());
                break;
            default:
                info = null;
        }

        // Hide or set info.
        TextView tvInfo = rootView.findViewById(R.id.tvInfo);
        if (info == null) {
            tvInfo.setVisibility(View.GONE);
        } else {
            tvInfo.setText(info);
            tvInfo.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onListItemClick(int position, Artist artist) {
        startActivity(new Intent(getActivity(), SongsActivity.class)
                .putExtras(SongsFragment.getArguments(artist.getId(),
                        getArguments().getString(ARG_SONGS_ORDER_BY))));
    }

    @Override
    protected void onContextItemSelected(int itemId, int position, Artist artist) {
        switch (itemId) {
            case R.id.info:
                try {
                    getDbHelper().queryArtist(artist);
                    getDbHelper().queryStats(artist).showDialog(getActivity(), artist.getArtist());
                } catch (Exception ex) {
                    Log.e(TAG, "Error querying artist stats", ex);
                    Util.showErrorDialog(getActivity(), ex);
                }
                break;
        }
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
}