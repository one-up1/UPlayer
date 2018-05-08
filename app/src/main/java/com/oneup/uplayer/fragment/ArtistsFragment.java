package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.Stats;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class ArtistsFragment extends ListFragment<Artist> {
    public static final int INFO_LAST_SONG_ADDED = 1; //TODO: Only use these and no orderBy and songsOrderBy?
    public static final int INFO_LAST_PLAYED = 2;
    public static final int INFO_TIMES_PLAYED = 3;

    private static final String ARG_ORDER_BY = "order_by";
    private static final String ARG_SONGS_ORDER_BY = "songs_order_by";
    private static final String ARG_INFO = "info";

    public ArtistsFragment() {
        super(R.layout.list_item_artist, R.menu.list_item_artist);
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
            case INFO_LAST_SONG_ADDED:
                info = artist.getLastSongAdded() == 0 ? null :
                        Util.formatTimeAgo(artist.getLastSongAdded());
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
                getDbHelper().queryArtist(artist);
                Stats stats = getDbHelper().queryStats(artist);

                //TODO: Artist stats. Avg prc per artist of total songs, division by 0 stats.getSongPlayed() ?
                Util.showInfoDialog(getActivity(), artist.getArtist(), R.string.artist_message,
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