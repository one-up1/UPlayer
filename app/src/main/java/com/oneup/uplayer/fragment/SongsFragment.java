package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

    private static final String TAG = "UPlayer";

    private static final String ARG_ARTIST_ID = "artist_id";

    private static final int REQUEST_SELECT_FILTER = 100;

    private long artistId;

    private Bundle filterValues;
    private String filterSelection;
    private String[] filterSelectionArgs;

    private Spinner sSortColumn;
    private CheckBox cbSortDesc;

    public SongsFragment() {
        super(R.layout.list_item_song, R.id.llSorting, R.id.llSong,
                new String[]{
                        null,
                        Song.ADDED,
                        Song.LAST_PLAYED,
                        Song.TIMES_PLAYED,
                        Song.DURATION,
                        Song.YEAR,
                        Song.TAG,
                        Song.BOOKMARKED
                });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle args = getArguments();
        if (args != null) {
            artistId = args.getLong(ARG_ARTIST_ID);
            if (artistId == 0) {
                setSortColumns(
                        new String[]{
                                null,
                                Song.ARTIST,
                                Song.TITLE
                        });
            } else {
                setSelection(Song.ARTIST_ID + "=?");
                setSelectionArgs(DbHelper.getWhereArgs(artistId));
                setSortColumns(
                        new String[]{
                                null,
                                Song.TITLE
                        });
            }
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
        menu.findItem(R.id.artist_info).setVisible(artistId != 0);
        menu.findItem(R.id.clear_filter).setVisible(filterValues != null);
        menu.findItem(R.id.savePlaylist).setVisible(artistId == 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.play_next:
                if (!getData().isEmpty()) {
                    add(getData(), true);
                    Util.showToast(getActivity(), R.string.playing_all_next);
                }
                return true;
            case R.id.play_last:
                if (!getData().isEmpty()) {
                    add(getData(), false);
                    Util.showToast(getActivity(), R.string.playing_all_last);
                }
                return true;
            case R.id.artist_info:
                try {
                    getDbHelper().queryStats(false, true, true, true, Song.ARTIST_ID + "=?",
                            DbHelper.getWhereArgs(artistId))
                            .showDialog(getActivity(), getListItem(0).getArtist());
                } catch (Exception ex) {
                    Log.e(TAG, "Error querying artist stats", ex);
                    Util.showErrorDialog(getActivity(), ex);
                }
                return true;
            case R.id.filter:
                startActivityForResult(new Intent(getActivity(), FilterActivity.class)
                                .putExtra(FilterActivity.EXTRA_VALUES, filterValues)
                                .putExtra(FilterActivity.EXTRA_SELECTION, getSelection())
                                .putExtra(FilterActivity.EXTRA_SELECTION_ARGS, getSelectionArgs()),
                        REQUEST_SELECT_FILTER);
                return true;
            case R.id.clear_filter:
                filterValues = null;
                filterSelection = null;
                filterSelectionArgs = null;
                reloadData();
                getActivity().invalidateOptionsMenu();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.findItem(R.id.view_artist).setVisible(artistId == 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_FILTER:
                    filterValues = data.getBundleExtra(FilterActivity.EXTRA_VALUES);
                    filterSelection = data.getStringExtra(FilterActivity.EXTRA_SELECTION);
                    filterSelectionArgs = data.getStringArrayExtra(
                            FilterActivity.EXTRA_SELECTION_ARGS);
                    getActivity().invalidateOptionsMenu();
                    break;
            }
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
        return getDbHelper().querySongs(
                DbHelper.concatSelection(getSelection(), filterSelection),
                DbHelper.concatSelectionArgs(getSelectionArgs(), filterSelectionArgs),
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
    protected String getSortColumnValue(int sortColumn, Song song) {
        switch (sortColumn) {
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
                return song.getBookmarked() == 0 ? null
                        : Util.formatTimeAgo(song.getBookmarked());
            default:
                return null;
        }
    }

    @Override
    protected void onListItemClick(int position, Song song) {
        Playlist playlist = Playlist.getDefault();
        playlist.setSongIndex(position);
        getActivity().startService(new Intent(getActivity(), MainService.class)
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

    private void add(ArrayList<Song> songs, boolean next) {
        getActivity().startService(new Intent(getActivity(), MainService.class)
                .putExtra(MainService.EXTRA_ACTION, MainService.ACTION_ADD)
                .putExtra(MainService.EXTRA_SONGS, songs)
                .putExtra(MainService.EXTRA_NEXT, next));
    }

    private void add(Song song, boolean next) {
        ArrayList<Song> songs = new ArrayList<>();
        songs.add(song);
        add(songs, next);
    }

    public static Bundle getArguments(long artistId, int sortColumn, boolean sortDesc) {
        Bundle args = new Bundle();
        args.putLong(ARG_ARTIST_ID, artistId);
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
