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
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class ArtistsFragment extends ListFragment<Artist> {
    public static final int SORT_COLUMN_LAST_ADDED = 1;
    public static final int SORT_COLUMN_LAST_PLAYED = 2;
    public static final int SORT_COLUMN_TIMES_PLAYED = 3;

    private static final String TAG = "UPlayer";

    public ArtistsFragment() {
        super(R.layout.list_item_artist, R.menu.list_item_artist, 0, 0, 0,
                new String[]{null, Artist.LAST_ADDED, Artist.LAST_PLAYED, Artist.TIMES_PLAYED},
                new String[]{null, Artist.ARTIST});
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadData();
    }

    @Override
    protected ArrayList<Artist> loadData() {
        return getDbHelper().queryArtists(getOrderBy());
    }

    @Override
    protected void setListItemContent(View rootView, int position, Artist artist) {
        super.setListItemContent(rootView, position, artist);

        // Set artist name.
        TextView tvArtist = rootView.findViewById(R.id.tvArtist);
        tvArtist.setTextColor(artist.getTimesPlayed() == 0 ? Color.BLUE : Color.BLACK);
        tvArtist.setText(artist.getArtist());
    }

    @Override
    protected String getSortColumnValue(int sortColumn, Artist artist) {
        switch (sortColumn) {
            case SORT_COLUMN_LAST_ADDED:
                return artist.getLastAdded() == 0 ? null
                        : Util.formatTimeAgo(artist.getLastAdded());
            case SORT_COLUMN_LAST_PLAYED:
                return artist.getLastPlayed() == 0 ? null
                        : Util.formatTimeAgo(artist.getLastPlayed());
            case SORT_COLUMN_TIMES_PLAYED:
                return artist.getTimesPlayed() == 0 ? null
                        : Integer.toString(artist.getTimesPlayed());
            default:
                return null;
        }
    }

    @Override
    protected void onListItemClick(int position, Artist artist) {
        startActivity(new Intent(getActivity(), SongsActivity.class)
                .putExtras(SongsFragment.getArguments(artist.getId(),
                        getSortColumn(), isSortDesc())));
    }

    @Override
    protected void onContextItemSelected(int itemId, int position, Artist artist) {
        switch (itemId) {
            case R.id.info:
                try {
                    getDbHelper().queryStats(false, Song.ARTIST_ID + "=?",
                            DbHelper.getWhereArgs(artist.getId()))
                            .showDialog(getActivity(), artist.getArtist());
                } catch (Exception ex) {
                    Log.e(TAG, "Error querying artist stats", ex);
                    Util.showErrorDialog(getActivity(), ex);
                }
                break;
        }
    }

    public static ArtistsFragment newInstance(int sortColumn, boolean sortDesc) {
        ArtistsFragment fragment = new ArtistsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SORT_COLUMN, sortColumn);
        args.putBoolean(ARG_SORT_DESC, sortDesc);
        fragment.setArguments(args);
        return fragment;
    }
}