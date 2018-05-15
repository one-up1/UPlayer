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

    private static final String ARG_SONGS_SORT_COLUMN = "songs_sort_column";

    private String songsSortColumn;

    public ArtistsFragment() {
        super(R.layout.list_item_artist);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            songsSortColumn = args.getString(ARG_SONGS_SORT_COLUMN);
        }
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
        return getDbHelper().queryArtists(getOrderBy(new String[]{Artist.ARTIST}));
    }

    @Override
    protected void setListItemViews(View rootView, int position, Artist artist) {
        super.setListItemViews(rootView, position, artist);

        // Set artist name.
        TextView tvArtist = rootView.findViewById(R.id.tvArtist);
        tvArtist.setTextColor(artist.getTimesPlayed() == 0 ? Color.BLUE : Color.BLACK);
        tvArtist.setText(artist.getArtist());
    }

    @Override
    protected String getSortColumnValue(Artist artist) {
        switch (getSortColumn()) {
            case Artist.LAST_ADDED:
                return artist.getLastAdded() == 0 ? null
                        : Util.formatTimeAgo(artist.getLastAdded());
            case Artist.LAST_PLAYED:
                return artist.getLastPlayed() == 0 ? null
                        : Util.formatTimeAgo(artist.getLastPlayed());
            case Artist.TIMES_PLAYED:
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
                        songsSortColumn, isSortDesc())));
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

    public static ArtistsFragment newInstance(String sortColumn, String songsSortColumn,
                                              boolean sortDesc) {
        ArtistsFragment fragment = new ArtistsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SORT_COLUMN, sortColumn);
        args.putString(ARG_SONGS_SORT_COLUMN, songsSortColumn);
        args.putBoolean(ARG_SORT_DESC, sortDesc);
        fragment.setArguments(args);
        return fragment;
    }
}