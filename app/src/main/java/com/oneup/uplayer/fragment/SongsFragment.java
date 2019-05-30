package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.FilterActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class SongsFragment extends SongsListFragment implements AdapterView.OnItemSelectedListener,
        CompoundButton.OnCheckedChangeListener {
    public static final int SORT_COLUMN_ADDED = 1;
    public static final int SORT_COLUMN_LAST_PLAYED = 2;
    public static final int SORT_COLUMN_TIMES_PLAYED = 3;
    public static final int SORT_COLUMN_DURATION = 4;
    public static final int SORT_COLUMN_YEAR = 5;
    public static final int SORT_COLUMN_TAG = 6;
    public static final int SORT_COLUMN_BOOKMARKED = 7;
    public static final int SORT_COLUMN_ARCHIVED = 8;

    private static final String ARG_ARTIST = "artist";

    private static final String DEFAULT_SORT_COLUMN =
            "(CASE WHEN " + Song.ARCHIVED + " IS NULL THEN 0 ELSE 1 END)";

    private static final int REQUEST_SELECT_FILTER = 100;

    private Artist artist;

    private FilterFragment.Values filterValues;
    private String filterSelection;
    private String[] filterSelectionArgs;

    private Spinner sSortColumn;
    private CheckBox cbSortDesc;

    public SongsFragment() {
        super(R.layout.list_item_song, R.id.llSorting, R.id.llSong, R.id.tvInfo,
                new String[]{
                        null,
                        Song.ADDED,
                        Song.LAST_PLAYED,
                        Song.TIMES_PLAYED,
                        Song.DURATION,
                        Song.YEAR,
                        Song.TAG,
                        Song.BOOKMARKED,
                        Song.ARCHIVED
                },
                DEFAULT_SORT_COLUMN);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

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
        switch (item.getItemId()) {
            case R.id.play_next:
                add(getData(), true);
                Util.showToast(getActivity(), R.string.playing_all_next);
                return true;
            case R.id.play_last:
                add(getData(), false);
                Util.showToast(getActivity(), R.string.playing_all_last);
                return true;
            case R.id.statistics:
                getDbHelper().queryStats(artist == null,
                        filterValues == null || filterValues.getBookmarked() == 0,
                        filterValues == null || filterValues.getArchived() == 0,
                        getSelection(), getSelectionArgs(),
                        filterSelection, filterSelectionArgs)
                        .showDialog(getActivity(), artist == null ? null : artist.getArtist());
                return true;
            case R.id.filter:
                startActivityForResult(new Intent(getActivity(), FilterActivity.class)
                                .putExtra(FilterActivity.EXTRA_SHOW_ARTIST_FILTER, artist == null)
                                .putExtra(FilterActivity.EXTRA_VALUES, filterValues),
                        REQUEST_SELECT_FILTER);
                return true;
            case R.id.clear_filter:
                filterValues = null;
                filterSelection = null;
                filterSelectionArgs = null;
                reloadData();
            default:
                return super.onOptionsItemSelected(item);
        }
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
        outState.putParcelable(FilterActivity.EXTRA_VALUES, filterValues);
        outState.putString(FilterActivity.EXTRA_SELECTION, filterSelection);
        outState.putStringArray(FilterActivity.EXTRA_SELECTION_ARGS, filterSelectionArgs);
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
        return getDbHelper().querySongs(
                DbHelper.concatSelection(getSelection(), filterSelection),
                DbHelper.concatWhereArgs(getSelectionArgs(), filterSelectionArgs),
                getOrderBy());
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
            case SORT_COLUMN_ADDED:
                return song.getAdded() == 0 ? null
                        : Util.formatTimeAgo(song.getAdded());
            case SORT_COLUMN_LAST_PLAYED:
                return song.getLastPlayed() == 0 ? null
                        : Util.formatTimeAgo(song.getLastPlayed());
            case SORT_COLUMN_TIMES_PLAYED:
                return song.getTimesPlayed() == 0 ? null
                        : Integer.toString(song.getTimesPlayed());
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
        switch (viewId) {
            case R.id.ibPlayNext:
                add(song, true);
                Util.showToast(getActivity(), R.string.playing_song_next, song);
                break;
            case R.id.ibPlayLast:
                add(song, false);
                Util.showToast(getActivity(), R.string.playing_song_last, song);
                break;
        }
    }

    @Override
    protected void onSongUpdated(Song song) {
        MainService.update(getActivity(), song);
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
