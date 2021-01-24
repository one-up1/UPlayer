package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.LogActivity;
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
    private Button bLog;
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
        fragmentTransaction.commit();

        sSortColumn = rootView.findViewById(R.id.sSortColumn);
        cbSortDesc = rootView.findViewById(R.id.cbSortDesc);

        bQuery = rootView.findViewById(R.id.bQuery);
        bQuery.setOnClickListener(this);
        bQuery.setOnLongClickListener(this);

        bLog = rootView.findViewById(R.id.bLog);
        bLog.setOnClickListener(this);
        bLog.setOnLongClickListener(this);

        bSettings = rootView.findViewById(R.id.bSettings);
        bSettings.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadQueryValues();
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
        } else if (v == bLog) {
            startActivity(new Intent(getActivity(), LogActivity.class));
        } else if (v == bSettings) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == bQuery) {
            PopupMenu pm = new PopupMenu(getActivity(), bQuery, Gravity.END);
            pm.getMenuInflater().inflate(R.menu.pm_query, pm.getMenu());
            pm.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.load_query_values) {
                    loadQueryValues();
                } else if (id == R.id.save_query_values) {
                    saveQueryValues();
                } else if (id == R.id.clear_query_values) {
                    filterFragment.setValues(new FilterFragment.Values());
                } else {
                    return false;
                }
                return true;
            });
            pm.show();
        } else if (v == bLog) {
            startActivity(new Intent(getActivity(), StatisticsActivity.class));
            //dbHelper.t();
        }
        return true;
    }

    private void loadQueryValues() {
        FilterFragment.Values filterValues = new FilterFragment.Values();

        filterValues.setTitle(settings.getString(R.string.key_query_title, null));
        filterValues.setArtist(settings.getString(R.string.key_query_artist, null));
        filterValues.setMinYear(settings.getString(R.string.key_query_min_year, null));
        filterValues.setMaxYear(settings.getString(R.string.key_query_max_year, null));
        filterValues.setMinAdded(settings.getLong(R.string.key_query_min_added, 0));
        filterValues.setMaxAdded(settings.getLong(R.string.key_query_max_added, 0));
        filterValues.setBookmarked(settings.getInt(R.string.key_query_bookmarked, 0));
        filterValues.setArchived(settings.getInt(R.string.key_query_archived, 0));

        Set<String> tags = settings.getStringSet(R.string.key_query_tags, null);
        if (tags != null) {
            filterValues.getTags().addAll(tags);
        }
        filterValues.setTagsNot(settings.getBoolean(R.string.key_query_tags_not, false));

        Set<String> playlistIds = settings.getStringSet(R.string.key_query_playlist_ids, null);
        if (playlistIds != null) {
            for (String playlistId : playlistIds) {
                Playlist playlist = new Playlist();
                playlist.setId(Long.parseLong(playlistId));
                filterValues.getPlaylists().add(playlist);
            }
            if (filterValues.getPlaylists().size() == 1) {
                filterValues.getPlaylists().get(0).setName(settings.getString(
                        R.string.key_query_playlist_name, null));
            }
        }
        filterValues.setPlaylistsNot(settings.getBoolean(R.string.key_query_playlists_not, false));

        filterValues.setMinLastPlayed(settings.getLong(R.string.key_query_min_last_played, 0));
        filterValues.setMaxLastPlayed(settings.getLong(R.string.key_query_max_last_played, 0));
        filterValues.setMinTimesPlayed(settings.getString(R.string.key_query_min_times_played, null));
        filterValues.setMaxTimesPlayed(settings.getString(R.string.key_query_max_times_played, null));

        filterFragment.setValues(filterValues);
        sSortColumn.setSelection(settings.getInt(R.string.key_query_sort_column, 0));
        cbSortDesc.setChecked(settings.getBoolean(R.string.key_query_sort_desc, false));
    }

    private void saveQueryValues() {
        FilterFragment.Values filterValues = filterFragment.getValues();

        Set<String> playlistIds;
        if (filterValues.getPlaylists().isEmpty()) {
            playlistIds = null;
        } else {
            playlistIds = new HashSet<>(filterValues.getPlaylists().size());
            for (Playlist playlist : filterValues.getPlaylists()) {
                playlistIds.add(Long.toString(playlist.getId()));
            }
        }

        settings.edit()
                .putString(R.string.key_query_title, filterValues.getTitle())
                .putString(R.string.key_query_artist, filterValues.getArtist())
                .putString(R.string.key_query_min_year, filterValues.getMinYear())
                .putString(R.string.key_query_max_year, filterValues.getMaxYear())
                .putLong(R.string.key_query_min_added, filterValues.getMinAdded())
                .putLong(R.string.key_query_max_added, filterValues.getMaxAdded())
                .putInt(R.string.key_query_bookmarked, filterValues.getBookmarked())
                .putInt(R.string.key_query_archived, filterValues.getArchived())
                .putStringSet(R.string.key_query_tags, filterValues.getTags().isEmpty() ? null :
                        new HashSet<>(filterValues.getTags()))
                .putBoolean(R.string.key_query_tags_not, filterValues.isTagsNot())
                .putStringSet(R.string.key_query_playlist_ids, playlistIds)
                .putString(R.string.key_query_playlist_name, filterValues.getPlaylists().size() == 1 ?
                        filterValues.getPlaylists().get(0).getName() : null)
                .putBoolean(R.string.key_query_playlists_not, filterValues.isPlaylistsNot())
                .putLong(R.string.key_query_min_last_played, filterValues.getMinLastPlayed())
                .putLong(R.string.key_query_max_last_played, filterValues.getMaxLastPlayed())
                .putString(R.string.key_query_min_times_played, filterValues.getMinTimesPlayed())
                .putString(R.string.key_query_max_times_played, filterValues.getMaxTimesPlayed())
                .putInt(R.string.key_query_sort_column, sSortColumn.getSelectedItemPosition())
                .putBoolean(R.string.key_query_sort_desc, cbSortDesc.isChecked())
                .apply();
        Toast.makeText(getActivity(), R.string.query_values_saved, Toast.LENGTH_SHORT).show();
    }

    public static QueryFragment newInstance() {
        return new QueryFragment();
    }
}
