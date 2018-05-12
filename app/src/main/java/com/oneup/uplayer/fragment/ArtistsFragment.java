package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class ArtistsFragment extends ListFragment<Artist> {
    private static final String TAG = "UPlayer";

    public ArtistsFragment() {
        super(R.layout.list_item_artist);
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadData();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(R.menu.list_item_artist, menu);
    }

    @Override
    protected ArrayList<Artist> loadData() {
        String orderBy;
        switch (getOrderBy()) {
            case ORDER_BY_ADDED:
                orderBy = getOrderBy(Artist.LAST_ADDED);
                break;
            case ORDER_BY_LAST_PLAYED:
                orderBy = getOrderBy(Artist.LAST_PLAYED);
                break;
            case ORDER_BY_TIMES_PLAYED:
                orderBy = getOrderBy(Artist.TIMES_PLAYED);
                break;
            default:
                orderBy = null;
                break;
        }

        if (orderBy == null) {
            orderBy = getOrderBy(Artist.ARTIST);
        } else {
            orderBy += "," + Artist.ARTIST;
        }

        return getDbHelper().queryArtists(orderBy);
    }

    @Override
    protected void setListItemViews(View rootView, int position, Artist artist) {
        // Set artist name.
        TextView tvArtist = rootView.findViewById(R.id.tvArtist);
        tvArtist.setTextColor(artist.getTimesPlayed() == 0 ? Color.BLUE : Color.BLACK);
        tvArtist.setText(artist.getArtist());

        // Get info text.
        String info;
        switch (getOrderBy()) {
            case ORDER_BY_ADDED:
                info = artist.getLastAdded() == 0 ? null
                        : Util.formatTimeAgo(artist.getLastAdded());
                break;
            case ORDER_BY_LAST_PLAYED:
                info = artist.getLastPlayed() == 0 ? null
                        : Util.formatTimeAgo(artist.getLastPlayed());
                break;
            case ORDER_BY_TIMES_PLAYED:
                info = artist.getTimesPlayed() == 0 ? null
                        : Integer.toString(artist.getTimesPlayed());
                break;
            default:
                info = null;
                break;
        }

        // Set info or hide the view when no info is available.
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
                        getOrderBy(), isOrderByDesc())));
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

    public static ArtistsFragment newInstance(int orderBy, boolean orderByDesc) {
        ArtistsFragment fragment = new ArtistsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ORDER_BY, orderBy);
        args.putBoolean(ARG_ORDER_BY_DESC, orderByDesc);
        fragment.setArguments(args);
        return fragment;
    }
}