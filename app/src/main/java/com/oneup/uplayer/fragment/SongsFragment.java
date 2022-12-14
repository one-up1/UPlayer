package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.FilterActivity;
import com.oneup.uplayer.activity.LogActivity;
import com.oneup.uplayer.activity.LogRecordsActivity;
import com.oneup.uplayer.activity.StatisticsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;
import com.oneup.util.Utils;

import java.util.ArrayList;

public class SongsFragment extends SongsListFragment implements AdapterView.OnItemSelectedListener,
        CompoundButton.OnCheckedChangeListener {
    public static final int SORT_COLUMN_LOG_TIMESTAMP = 0;
    public static final int SORT_COLUMN_ADDED = 1;
    public static final int SORT_COLUMN_LAST_PLAYED = 2;
    public static final int SORT_COLUMN_TIMES_PLAYED = 3;
    public static final int SORT_COLUMN_DURATION = 4;
    public static final int SORT_COLUMN_YEAR = 5;
    public static final int SORT_COLUMN_TAG = 6;
    public static final int SORT_COLUMN_BOOKMARKED = 7;
    public static final int SORT_COLUMN_ARCHIVED = 8;

    private static final String ARG_ARTIST = "artist";

    private static final int REQUEST_SELECT_FILTER = 100;

    private Artist artist;

    private FilterFragment.Values filterValues;
    private String filterSelection;
    private String[] filterSelectionArgs;

    private Spinner sSortColumn;
    private CheckBox cbSortDesc;

    public SongsFragment() {
        this(null);
    }

    protected SongsFragment(String sortColumn) {
        super(R.layout.list_item_song, R.id.llSorting, R.id.llSong, R.id.tvInfo,
                new String[]{
                        sortColumn,
                        Song.ADDED,
                        Song.LAST_PLAYED,
                        Song.TIMES_PLAYED,
                        Song.DURATION,
                        Song.YEAR,
                        Song.TAG,
                        Song.BOOKMARKED,
                        Song.ARCHIVED
                });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            artist = args.getParcelable(ARG_ARTIST);
            if (artist == null) {
                setSortColumns(
                        new String[]{
                                null,
                                Song.ARTIST,
                                Song.TITLE
                        });
            } else {
                setSelection(Song.ARTIST_ID + "=?");
                setSelectionArgs(DbHelper.getWhereArgs(artist.getId()));
                setSortColumns(
                        new String[]{
                                null,
                                Song.TITLE
                        });
            }
        }

        if (savedInstanceState != null) {
            filterValues = savedInstanceState.getParcelable(FilterActivity.EXTRA_VALUES);
            filterSelection = savedInstanceState.getString(FilterActivity.EXTRA_SELECTION);
            filterSelectionArgs = savedInstanceState.getStringArray(
                    FilterActivity.EXTRA_SELECTION_ARGS);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadData();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_songs, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.play_next).setVisible(!getData().isEmpty());
        menu.findItem(R.id.play_last).setVisible(!getData().isEmpty());
        menu.findItem(R.id.clear_filter).setVisible(filterValues != null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.play_next) {
            add(getData(), true);
            Utils.showToast(getActivity(), R.string.playing_all_next);
        } else if (id == R.id.play_last) {
            add(getData(), false);
            Utils.showToast(getActivity(), R.string.playing_all_last);
        } else if (id == R.id.statistics) {
            Intent intent = new Intent(getActivity(), StatisticsActivity.class);
            if (artist != null) {
                intent.putExtra(StatisticsActivity.EXTRA_TITLE, artist.getStyledArtist());
                intent.putExtra(StatisticsActivity.EXTRA_QUERY_ARTIST, false);
            }
            intent.putExtra(StatisticsActivity.EXTRA_QUERY_BOOKMARKED,
                    filterValues == null || filterValues.getBookmarked() == 0);
            intent.putExtra(StatisticsActivity.EXTRA_QUERY_ARCHIVED,
                    filterValues == null || filterValues.getArchived() == 0);
            intent.putExtra(StatisticsActivity.EXTRA_BASE_SELECTION, getSelection());
            intent.putExtra(StatisticsActivity.EXTRA_BASE_SELECTION_ARGS, getSelectionArgs());
            intent.putExtra(StatisticsActivity.EXTRA_SELECTION, filterSelection);
            intent.putExtra(StatisticsActivity.EXTRA_SELECTION_ARGS, filterSelectionArgs);
            startActivity(intent);
        } else if (id == R.id.log) {
            Intent intent = new Intent(getActivity(), LogActivity.class);
            if (artist != null) {
                intent.putExtra(LogActivity.EXTRA_TITLE, artist.getStyledArtist());
                intent.putExtra(LogActivity.EXTRA_QUERY_ARTIST, false);
            }
            intent.putExtra(LogActivity.EXTRA_SELECTION, getSelection());
            intent.putExtra(LogActivity.EXTRA_SELECTION_ARGS, getSelectionArgs());
            startActivity(intent);
        } else if (id == R.id.filter) {
            startActivityForResult(new Intent(getActivity(), FilterActivity.class)
                            .putExtra(FilterActivity.EXTRA_SHOW_ARTIST_FILTER, artist == null)
                            .putExtra(FilterActivity.EXTRA_VALUES, filterValues),
                    REQUEST_SELECT_FILTER);
        } else if (id == R.id.clear_filter) {
            filterValues = null;
            filterSelection = null;
            filterSelectionArgs = null;
            reloadData();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.findItem(R.id.view_artist).setVisible(artist == null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_FILTER:
                    filterValues = data.getParcelableExtra(FilterActivity.EXTRA_VALUES);
                    filterSelection = data.getStringExtra(FilterActivity.EXTRA_SELECTION);
                    filterSelectionArgs = data.getStringArrayExtra(
                            FilterActivity.EXTRA_SELECTION_ARGS);
                    break;
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (filterValues != null) {
            outState.putParcelable(FilterActivity.EXTRA_VALUES, filterValues);
            outState.putString(FilterActivity.EXTRA_SELECTION, filterSelection);
            outState.putStringArray(FilterActivity.EXTRA_SELECTION_ARGS, filterSelectionArgs);
        }
    }

    @Override
    public void reverseSortOrder() {
        cbSortDesc.setChecked(!cbSortDesc.isChecked());
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == sSortColumn) {
            if (position != getSortColumn()) {
                setSortColumn(position);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == cbSortDesc) {
            setSortDesc(cbSortDesc.isChecked());
        }
    }

    @Override
    protected ArrayList<Song> loadData() {
        return getDbHelper().querySongs(getSelection(), getSelectionArgs(), getOrderBy());
    }

    @Override
    protected void setListItemHeader(View rootView) {
        sSortColumn = rootView.findViewById(R.id.sSortColumn);
        sSortColumn.setSelection(getSortColumn());
        sSortColumn.setOnItemSelectedListener(this);

        cbSortDesc = rootView.findViewById(R.id.cbSortDesc);
        cbSortDesc.setChecked(isSortDesc());
        cbSortDesc.setOnCheckedChangeListener(this);
    }

    @Override
    protected void setListItemContent(View rootView, int position, Song song) {
        super.setListItemContent(rootView, position, song);

        // Set play next and play last buttons.
        setListItemViewOnClickListener(rootView, R.id.ibPlayNext);
        setListItemViewOnClickListener(rootView, R.id.ibPlayLast);
    }

    @Override
    protected String getListItemInfo(Song song) {
        switch (getSortColumn()) {
            case SORT_COLUMN_LOG_TIMESTAMP:
                if (song.getLogTimestamp() == 0) {
                    return null;
                }
                return getArguments().getString(
                        LogRecordsActivity.LogRecordsFragment.ARG_ACTIVITY_TITLE) == null
                        ? Util.formatDateTime(song.getLogTimestamp())
                        : Util.formatTimeOfDay(song.getLogTimestamp());
            case SORT_COLUMN_ADDED:
                return song.getAdded() == 0 ? null
                        : Util.formatTimeAgo(song.getAdded());
            case SORT_COLUMN_LAST_PLAYED:
            case SORT_COLUMN_TIMES_PLAYED:
                return song.getLastPlayed() == 0 ? null
                        : Util.formatTimeAgo(song.getLastPlayed());
            /*case SORT_COLUMN_TIMES_PLAYED:
                return song.getTimesPlayed() == 0 ? null
                        : Integer.toString(song.getTimesPlayed());*/
            case SORT_COLUMN_DURATION:
                return Util.formatDuration(song.getDuration());
            case SORT_COLUMN_YEAR:
                return Integer.toString(song.getYear());
            case SORT_COLUMN_TAG:
                return song.getTag();
            case SORT_COLUMN_BOOKMARKED:
                return song.getBookmarked() == 0 ? null : Util.formatTimeAgo(song.getBookmarked());
            case SORT_COLUMN_ARCHIVED:
                return song.getArchived() == 0 ? null : Util.formatTimeAgo(song.getArchived());
            default:
                return null;
        }
    }

    @Override
    protected void onListItemClick(int position, Song song) {
        Playlist playlist = Playlist.getDefault();
        playlist.setSongIndex(position);
        getActivity().startForegroundService(new Intent(getActivity(), MainService.class)
                .putExtra(MainService.EXTRA_ACTION, MainService.ACTION_PLAY)
                .putExtra(MainService.EXTRA_SONGS, getData())
                .putExtra(MainService.EXTRA_PLAYLIST, playlist));
    }

    @Override
    protected void onListItemViewClick(int viewId, int position, Song song) {
        if (viewId == R.id.ibPlayNext) {
            add(song, true);
            Utils.showToast(getActivity(), R.string.playing_song_next, song);
        } else if (viewId == R.id.ibPlayLast) {
            add(song, false);
            Utils.showToast(getActivity(), R.string.playing_song_last, song);
        }
    }

    @Override
    protected String getSelection() {
        return DbHelper.concatSelection(super.getSelection(), filterSelection);
    }

    @Override
    protected String[] getSelectionArgs() {
        return DbHelper.concatWhereArgs(super.getSelectionArgs(), filterSelectionArgs);
    }

    @Override
    protected void onSongUpdated(Song song) {
        MainService.update(getActivity(), false, song);
        reloadData();
    }

    private void add(ArrayList<Song> songs, boolean next) {
        getActivity().startForegroundService(new Intent(getActivity(), MainService.class)
                .putExtra(MainService.EXTRA_ACTION, MainService.ACTION_ADD)
                .putExtra(MainService.EXTRA_SONGS, songs)
                .putExtra(MainService.EXTRA_NEXT, next));
    }

    private void add(Song song, boolean next) {
        ArrayList<Song> songs = new ArrayList<>();
        songs.add(song);
        add(songs, next);
    }

    public static Bundle getArguments(Artist artist, int sortColumn, boolean sortDesc) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_ARTIST, artist);
        args.putInt(ARG_SORT_COLUMN, sortColumn);
        args.putBoolean(ARG_SORT_DESC, sortDesc);
        args.putInt(ARG_SORT_COLUMN, sortColumn);
        return args;
    }

    public static Bundle getArguments(String selection, String[] selectionArgs,
                                      int sortColumn, boolean sortDesc) {
        Bundle args = new Bundle();
        args.putString(ARG_SELECTION, selection);
        args.putStringArray(ARG_SELECTION_ARGS, selectionArgs);
        args.putInt(ARG_SORT_COLUMN, sortColumn);
        args.putBoolean(ARG_SORT_DESC, sortDesc);
        args.putInt(ARG_SORT_COLUMN, sortColumn);
        return args;
    }

    public static SongsFragment newInstance(String selection, String[] selectionArgs,
                                            int sortColumn, boolean sortDesc) {
        return newInstance(getArguments(selection, selectionArgs, sortColumn, sortDesc));
    }

    public static SongsFragment newInstance(Bundle args) {
        SongsFragment fragment = new SongsFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
