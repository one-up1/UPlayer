package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.SettingsActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.activity.StatisticsActivity;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.util.Settings;

import java.util.HashSet;
import java.util.Set;

public class QueryFragment extends Fragment
        implements View.OnClickListener, View.OnLongClickListener {
    private Settings settings;
    private DbHelper dbHelper;

    private FilterFragment filterFragment;

    private Spinner sSortColumn;
    private CheckBox cbSortDesc;
    private Button bQuery;
    private Button bStatistics;
    private Button bSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = Settings.get(getActivity());
        dbHelper = new DbHelper(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_query, container, false);

        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        filterFragment = (FilterFragment) fragmentManager.findFragmentById(R.id.filterFragment);
        filterFragment.setShowArtistFilter(true);
        filterFragment.setSelectPlaylistConfirmId(-1);
        filterFragment.setValues(getFilterValues());
        fragmentTransaction.commit();

        sSortColumn = rootView.findViewById(R.id.sSortColumn);
        sSortColumn.setSelection(settings.getInt(R.string.key_query_sort_column, 0));

        cbSortDesc = rootView.findViewById(R.id.cbSortDesc);
        cbSortDesc.setChecked(settings.getBoolean(R.string.key_query_sort_desc, false));

        bQuery = rootView.findViewById(R.id.bQuery);
        bQuery.setOnClickListener(this);
        bQuery.setOnLongClickListener(this);

        bStatistics = rootView.findViewById(R.id.bStatistics);
        bStatistics.setOnClickListener(this);

        bSettings = rootView.findViewById(R.id.bSettings);
        bSettings.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == bQuery) {
            startActivity(new Intent(getActivity(), SongsActivity.class)
                    .putExtras(SongsFragment.getArguments(
                            filterFragment.getSelection(), filterFragment.getSelectionArgs(),
                            sSortColumn.getSelectedItemPosition(), cbSortDesc.isChecked())));
        } else if (v == bStatistics) {
            startActivity(new Intent(getActivity(), StatisticsActivity.class));
        } else if (v == bSettings) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == bQuery) {
            saveQueryValues();
        }
        return true;
    }

    private FilterFragment.Values getFilterValues() {
        FilterFragment.Values values = new FilterFragment.Values();

        values.setTitle(settings.getString(R.string.key_query_title, null));
        values.setArtist(settings.getString(R.string.key_query_artist, null));
        values.setMinYear(settings.getString(R.string.key_query_min_year, null));
        values.setMaxYear(settings.getString(R.string.key_query_max_year, null));
        values.setMinAdded(settings.getLong(R.string.key_query_min_added, 0));
        values.setMaxAdded(settings.getLong(R.string.key_query_max_added, 0));
        values.setBookmarked(settings.getInt(R.string.key_query_bookmarked, 0));
        values.setArchived(settings.getInt(R.string.key_query_archived, 0));

        Set<String> tags = settings.getStringSet(R.string.key_query_tags, null);
        if (tags != null) {
            values.getTags().addAll(tags);
        }
        values.setTagsNot(settings.getBoolean(R.string.key_query_tags_not, false));

        Set<String> playlistIds = settings.getStringSet(R.string.key_query_playlist_ids, null);
        if (playlistIds != null) {
            for (String playlistId : playlistIds) {
                Playlist playlist = new Playlist();
                playlist.setId(Long.parseLong(playlistId));
                values.getPlaylists().add(playlist);
            }
            if (values.getPlaylists().size() == 1) {
                values.getPlaylists().get(0).setName(settings.getString(
                        R.string.key_query_playlist_name, null));
            }
        }
        values.setPlaylistsNot(settings.getBoolean(R.string.key_query_playlists_not, false));

        values.setMinLastPlayed(settings.getLong(R.string.key_query_min_last_played, 0));
        values.setMaxLastPlayed(settings.getLong(R.string.key_query_max_last_played, 0));
        values.setMinTimesPlayed(settings.getString(R.string.key_query_min_times_played, null));
        values.setMaxTimesPlayed(settings.getString(R.string.key_query_max_times_played, null));

        return values;
    }

    private void saveQueryValues() {
        FilterFragment.Values values = filterFragment.getValues();

        Set<String> playlistIds;
        if (values.getPlaylists().isEmpty()) {
            playlistIds = null;
        } else {
            playlistIds = new HashSet<>(values.getPlaylists().size());
            for (Playlist playlist : values.getPlaylists()) {
                playlistIds.add(Long.toString(playlist.getId()));
            }
        }

        settings.edit()
                .putString(R.string.key_query_title, values.getTitle())
                .putString(R.string.key_query_artist, values.getArtist())
                .putString(R.string.key_query_min_year, values.getMinYear())
                .putString(R.string.key_query_max_year, values.getMaxYear())
                .putLong(R.string.key_query_min_added, values.getMinAdded())
                .putLong(R.string.key_query_max_added, values.getMaxAdded())
                .putInt(R.string.key_query_bookmarked, values.getBookmarked())
                .putInt(R.string.key_query_archived, values.getArchived())
                .putStringSet(R.string.key_query_tags, values.getTags().isEmpty() ? null :
                        new HashSet<>(values.getTags()))
                .putBoolean(R.string.key_query_tags_not, values.isTagsNot())
                .putStringSet(R.string.key_query_playlist_ids, playlistIds)
                .putString(R.string.key_query_playlist_name, values.getPlaylists().size() == 1 ?
                        values.getPlaylists().get(0).getName() : null)
                .putBoolean(R.string.key_query_playlists_not, values.isPlaylistsNot())
                .putLong(R.string.key_query_min_last_played, values.getMinLastPlayed())
                .putLong(R.string.key_query_max_last_played, values.getMaxLastPlayed())
                .putString(R.string.key_query_min_times_played, values.getMinTimesPlayed())
                .putString(R.string.key_query_max_times_played, values.getMaxTimesPlayed())
                .putInt(R.string.key_query_sort_column, sSortColumn.getSelectedItemPosition())
                .putBoolean(R.string.key_query_sort_desc, cbSortDesc.isChecked())
                .apply();

        Toast.makeText(getActivity(), R.string.query_values_saved, Toast.LENGTH_SHORT).show();
    }

    public static QueryFragment newInstance() {
        return new QueryFragment();
    }
}
