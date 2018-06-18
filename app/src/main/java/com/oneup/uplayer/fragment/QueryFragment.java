package com.oneup.uplayer.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.DateTimeActivity;
import com.oneup.uplayer.activity.MainActivity;
import com.oneup.uplayer.activity.PlaylistsActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.widget.EditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//TODO: Allow showing stats for tagged/playlist songs.
//TODO: Allow showing songs with tag and on playlist.

public class QueryFragment extends Fragment implements AdapterView.OnItemSelectedListener,
        View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = "UPlayer";

    private static final String PREF_TITLE = "title";
    private static final String PREF_ARTIST = "artist";
    private static final String PREF_MIN_YEAR = "min_year";
    private static final String PREF_MAX_YEAR = "max_year";
    private static final String PREF_MIN_ADDED = "min_added";
    private static final String PREF_MAX_ADDED = "max_added";
    private static final String PREF_BOOKMARKED = "bookmarked";
    private static final String PREF_NOT_BOOKMARKED = "not_bookmarked";
    private static final String PREF_MIN_LAST_PLAYED = "min_last_played";
    private static final String PREF_MAX_LAST_PLAYED = "max_last_played";
    private static final String PREF_MIN_TIMES_PLAYED = "min_times_played";
    private static final String PREF_MAX_TIMES_PLAYED = "max_times_played";
    private static final String PREF_SORT_COLUMN = "sort_column";
    private static final String PREF_SORT_DESC = "sort_desc";
    private static final String PREF_TAGS = "tags";

    private static final int REQUEST_SELECT_MIN_ADDED = 1;
    private static final int REQUEST_SELECT_MAX_ADDED = 2;
    private static final int REQUEST_SELECT_MIN_LAST_PLAYED = 3;
    private static final int REQUEST_SELECT_MAX_LAST_PLAYED = 4;
    private static final int REQUEST_SELECT_PLAYLISTS = 5;

    private SharedPreferences preferences;
    private DbHelper dbHelper;
    private List<Artist> artists;

    private EditText etTitle;
    private EditText etArtist;
    private Spinner sArtist;
    private EditText etMinYear;
    private EditText etMaxYear;
    private Button bMinAdded;
    private Button bMaxAdded;
    private RadioButton rbAll;
    private RadioButton rbBookmarked;
    private RadioButton rbNotBookmarked;
    private Button bMinLastPlayed;
    private Button bMaxLastPlayed;
    private EditText etMinTimesPlayed;
    private EditText etMaxTimesPlayed;
    private Spinner sSortColumn;
    private CheckBox cbSortDesc;
    private Button bQuery;
    private Button bTags;
    private Button bStatistics;
    private Button bPlaylists;
    private Button bSyncDatabase;
    private Button bBackup;

    private long minAdded;
    private long maxAdded;
    private long minLastPlayed;
    private long maxLastPlayed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        dbHelper = new DbHelper(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_query, container, false);

        etTitle = rootView.findViewById(R.id.etTitle);
        etTitle.setString(preferences.getString(PREF_TITLE, null));

        etArtist = rootView.findViewById(R.id.etArtist);
        etArtist.setString(preferences.getString(PREF_ARTIST, null));

        sArtist = rootView.findViewById(R.id.sArtist);
        sArtist.setOnItemSelectedListener(this);
        loadArtists();

        etMinYear = rootView.findViewById(R.id.etMinYear);
        etMinYear.setString(preferences.getString(PREF_MIN_YEAR, null));

        etMaxYear = rootView.findViewById(R.id.etMaxYear);
        etMaxYear.setString(preferences.getString(PREF_MAX_YEAR, null));

        bMinAdded = rootView.findViewById(R.id.bMinAdded);
        bMinAdded.setOnClickListener(this);
        bMinAdded.setOnLongClickListener(this);
        minAdded = preferences.getLong(PREF_MIN_ADDED, 0);
        if (minAdded > 0) {
            bMinAdded.setText(Util.formatDateTime(minAdded));
        }

        bMaxAdded = rootView.findViewById(R.id.bMaxAdded);
        bMaxAdded.setOnClickListener(this);
        bMaxAdded.setOnLongClickListener(this);
        maxAdded = preferences.getLong(PREF_MAX_ADDED, 0);
        if (maxAdded > 0) {
            bMaxAdded.setText(Util.formatDateTime(maxAdded));
        }

        rbAll = rootView.findViewById(R.id.rbAll);
        rbBookmarked = rootView.findViewById(R.id.rbBookmarked);
        rbNotBookmarked = rootView.findViewById(R.id.rbNotBookmarked);
        if (preferences.getBoolean(PREF_BOOKMARKED, false)) {
            rbBookmarked.setChecked(true);
        } else if (preferences.getBoolean(PREF_NOT_BOOKMARKED, false)) {
            rbNotBookmarked.setChecked(true);
        } else {
            rbAll.setChecked(true);
        }

        bMinLastPlayed = rootView.findViewById(R.id.bMinLastPlayed);
        bMinLastPlayed.setOnClickListener(this);
        bMinLastPlayed.setOnLongClickListener(this);
        minLastPlayed = preferences.getLong(PREF_MIN_LAST_PLAYED, 0);
        if (minLastPlayed > 0) {
            bMinLastPlayed.setText(Util.formatDateTime(minLastPlayed));
        }

        bMaxLastPlayed = rootView.findViewById(R.id.bMaxLastPlayed);
        bMaxLastPlayed.setOnClickListener(this);
        bMaxLastPlayed.setOnLongClickListener(this);
        maxLastPlayed = preferences.getLong(PREF_MAX_LAST_PLAYED, 0);
        if (maxLastPlayed > 0) {
            bMaxLastPlayed.setText(Util.formatDateTime(maxLastPlayed));
        }

        etMinTimesPlayed = rootView.findViewById(R.id.etMinTimesPlayed);
        etMinTimesPlayed.setString(preferences.getString(PREF_MIN_TIMES_PLAYED, null));

        etMaxTimesPlayed = rootView.findViewById(R.id.etMaxTimesPlayed);
        etMaxTimesPlayed.setString(preferences.getString(PREF_MAX_TIMES_PLAYED, null));

        sSortColumn = rootView.findViewById(R.id.sSortColumn);
        sSortColumn.setSelection(preferences.getInt(PREF_SORT_COLUMN, 0));

        cbSortDesc = rootView.findViewById(R.id.cbSortDesc);
        cbSortDesc.setChecked(preferences.getBoolean(PREF_SORT_DESC, false));

        bQuery = rootView.findViewById(R.id.bQuery);
        bQuery.setOnClickListener(this);

        bTags = rootView.findViewById(R.id.bTags);
        bTags.setOnClickListener(this);

        bStatistics = rootView.findViewById(R.id.bStatistics);
        bStatistics.setOnClickListener(this);

        bPlaylists = rootView.findViewById(R.id.bPlaylists);
        bPlaylists.setOnClickListener(this);

        bSyncDatabase = rootView.findViewById(R.id.bSyncDatabase);
        bSyncDatabase.setOnClickListener(this);

        bBackup = rootView.findViewById(R.id.bBackup);
        bBackup.setOnClickListener(this);
        bBackup.setOnLongClickListener(this);

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_MIN_ADDED:
                    minAdded = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMinAdded.setText(Util.formatDateTime(minAdded));
                    break;
                case REQUEST_SELECT_MAX_ADDED:
                    maxAdded = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMaxAdded.setText(Util.formatDateTime(maxAdded));
                    break;
                case REQUEST_SELECT_MIN_LAST_PLAYED:
                    minLastPlayed = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMinLastPlayed.setText(Util.formatDateTime(minLastPlayed));
                    break;
                case REQUEST_SELECT_MAX_LAST_PLAYED:
                    maxLastPlayed = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMaxLastPlayed.setText(Util.formatDateTime(maxLastPlayed));
                    break;
                case REQUEST_SELECT_PLAYLISTS:
                    if (data.hasExtra(PlaylistsActivity.EXTRA_PLAYLIST)) {
                        getActivity().startService(new Intent(getActivity(), MainService.class)
                                .putExtra(MainService.EXTRA_ACTION,
                                        MainService.ACTION_PLAY_PLAYLIST)
                                .putExtra(MainService.EXTRA_PLAYLIST,
                                        data.getParcelableExtra(PlaylistsActivity.EXTRA_PLAYLIST)));
                    } else if (data.hasExtra(PlaylistsActivity.EXTRA_PLAYLISTS)) {
                        query(null, data.<Playlist>getParcelableArrayListExtra(
                                PlaylistsActivity.EXTRA_PLAYLISTS));
                    }
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
        saveQueryParams(null);

        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == sArtist) {
            if (position > 0) {
                etArtist.setString(artists.get(position).getArtist());
                sArtist.setSelection(0);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onClick(View v) {
        if (v == bMinAdded) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_min_added);
            if (minAdded > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, minAdded);
            }
            startActivityForResult(intent, REQUEST_SELECT_MIN_ADDED);
        } else if (v == bMaxAdded) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_max_added);
            if (maxAdded > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, maxAdded);
            }
            startActivityForResult(intent, REQUEST_SELECT_MAX_ADDED);
        } else if (v == bMinLastPlayed) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_min_last_played);
            if (minLastPlayed > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, minLastPlayed);
            }
            startActivityForResult(intent, REQUEST_SELECT_MIN_LAST_PLAYED);
        } else if (v == bMaxLastPlayed) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_max_last_played);
            if (maxLastPlayed > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, maxLastPlayed);
            }
            startActivityForResult(intent, REQUEST_SELECT_MAX_LAST_PLAYED);
        } else if (v == bQuery) {
            query(null, null);
        } else if (v == bTags) {
            showTags();
        } else if (v == bStatistics) {
            try {
                List<String> selectionArgs = new ArrayList<>();
                dbHelper.queryStats(true, getSelection(selectionArgs),
                        selectionArgs.toArray(new String[0]))
                        .showDialog(getActivity(), null);
            } catch (Exception ex) {
                Log.e(TAG, "Error querying stats", ex);
                Util.showErrorDialog(getActivity(), ex);
            }
        } else if (v == bPlaylists) {
            startActivityForResult(new Intent(getActivity(), PlaylistsActivity.class)
                            .putExtras(PlaylistsActivity.PlaylistsFragment.getArguments(true)),
                    REQUEST_SELECT_PLAYLISTS);
        } else if (v == bSyncDatabase) {
            syncDatabase();
        } else if (v == bBackup) {
            backup();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == bMinAdded) {
            minAdded = 0;
            bMinAdded.setText(R.string.select_min_added);
        } else if (v == bMaxAdded) {
            maxAdded = 0;
            bMaxAdded.setText(R.string.select_max_added);
        } else if (v == bMinLastPlayed) {
            minLastPlayed = 0;
            bMinLastPlayed.setText(R.string.select_min_last_played);
        } else if (v == bMaxLastPlayed) {
            maxLastPlayed = 0;
            bMaxLastPlayed.setText(R.string.select_max_last_played);
        } else if (v == bBackup) {
            restoreBackup();
        }
        return true;
    }

    private void loadArtists() {
        artists = dbHelper.queryArtists(null);
        Artist nullArtist = new Artist();
        nullArtist.setArtist("");
        artists.add(0, nullArtist);

        sArtist.setAdapter(new
                ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, artists));
    }

    private void query(Set<String> tags, List<Playlist> playlists) {
        List<String> selectionArgs = new ArrayList<>();
        String selection = getSelection(selectionArgs);
        saveQueryParams(tags);

        if (tags != null) {
            String tagSelection;
            if (tags.size() == 0) {
                tagSelection = "IS NULL";
            } else {
                tagSelection = DbHelper.getInClause(tags.size());
                selectionArgs.addAll(tags);
            }
            selection = DbHelper.appendSelection(selection, Song.TAG + " " + tagSelection);
        }

        if (playlists != null) {
            selection = DbHelper.appendSelection(selection,
                    DbHelper.getPlaylistSongsInClause(playlists.size()));
            for (Playlist playlist : playlists) {
                selectionArgs.add(Long.toString(playlist.getId()));
            }
        }

        startActivity(new Intent(getActivity(), SongsActivity.class)
                .putExtras(SongsFragment.getArguments(selection,
                        selection == null ? null : selectionArgs.toArray(new String[0]),
                        sSortColumn.getSelectedItemPosition(), cbSortDesc.isChecked())));
    }

    private String getSelection(List<String> selectionArgs) {
        String selection = null;

        String title = etTitle.getString();
        if (title != null) {
            selection = Song.TITLE + " LIKE ?";
            selectionArgs.add("%" + title + "%");
        }

        String artist = etArtist.getString();
        if (artist != null) {
            selection = DbHelper.appendSelection(selection, Song.ARTIST + " LIKE ?");
            selectionArgs.add("%" + artist + "%");
        }

        String minYear = etMinYear.getString();
        if (minYear != null) {
            selection = DbHelper.appendSelection(selection, Song.YEAR + ">=?");
            selectionArgs.add(minYear);
        }

        String maxYear = etMaxYear.getString();
        if (maxYear != null) {
            selection = DbHelper.appendSelection(selection, Song.YEAR + "<=?");
            selectionArgs.add(maxYear);
        }

        if (minAdded > 0) {
            selection = DbHelper.appendSelection(selection, Song.ADDED + ">=?");
            selectionArgs.add(Long.toString(minAdded));
        }

        if (maxAdded > 0) {
            selection = DbHelper.appendSelection(selection, Song.ADDED + "<=?");
            selectionArgs.add(Long.toString(maxAdded));
        }

        if (rbBookmarked.isChecked()) {
            selection = DbHelper.appendSelection(selection, Song.BOOKMARKED + " IS NOT NULL");
        } else if (rbNotBookmarked.isChecked()) {
            selection = DbHelper.appendSelection(selection, Song.BOOKMARKED + " IS NULL");
        }

        if (minLastPlayed > 0) {
            selection = DbHelper.appendSelection(selection, Song.LAST_PLAYED + ">=?");
            selectionArgs.add(Long.toString(minLastPlayed));
        }

        if (maxLastPlayed > 0) {
            selection = DbHelper.appendSelection(selection, Song.LAST_PLAYED + "<=?");
            selectionArgs.add(Long.toString(maxLastPlayed));
        }

        String minTimesPlayed = etMinTimesPlayed.getString();
        if (minTimesPlayed != null) {
            selection = DbHelper.appendSelection(selection, Song.TIMES_PLAYED + ">=?");
            selectionArgs.add(minTimesPlayed);
        }

        String maxTimesPlayed = etMaxTimesPlayed.getString();
        if (maxTimesPlayed != null) {
            selection = DbHelper.appendSelection(selection, Song.TIMES_PLAYED + "<=?");
            selectionArgs.add(maxTimesPlayed);
        }

        return selection;
    }

    private void showTags() {
        final String[] tags = dbHelper.querySongTags().toArray(new String[0]);
        if (tags.length == 0) {
            Util.showToast(getActivity(), R.string.no_tags);
            return;
        }

        final Set<String> checkedTags = preferences.getStringSet(PREF_TAGS, new ArraySet<String>());
        boolean[] checkedItems = new boolean[tags.length];
        for (int i = 0; i < tags.length; i++) {
            checkedItems[i] = checkedTags.contains(tags[i]);
        }
        final AlertDialog tagsDialog = new AlertDialog.Builder(getActivity())
                .setMultiChoiceItems(tags, checkedItems,
                        new DialogInterface.OnMultiChoiceClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which, boolean isChecked) {
                                if (isChecked) {
                                    checkedTags.add(tags[which]);
                                } else {
                                    checkedTags.remove(tags[which]);
                                }
                            }
                        })
                .setNeutralButton(R.string.none, null)
                .setNegativeButton(R.string.all, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        query(checkedTags, null);
                    }
                })
                .create();
        tagsDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                final ListView listView = tagsDialog.getListView();
                final Button bNone = tagsDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                final Button bAll = tagsDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                View.OnClickListener buttonOnClickListener = new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        for (int i = 0; i < tags.length; i++) {
                            if (v == bNone == checkedTags.contains(tags[i])) {
                                listView.performItemClick(listView, i, i);
                            }
                        }
                    }
                };
                bAll.setOnClickListener(buttonOnClickListener);
                bNone.setOnClickListener(buttonOnClickListener);
            }
        });
        tagsDialog.show();
    }

    private void syncDatabase() {
        Util.showConfirmDialog(getActivity(), R.string.sync_database_confirm,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    DbHelper.SyncResult[] results =
                                            dbHelper.syncWithMediaStore(getActivity());
                                    reload();
                                    Util.showInfoDialog(getActivity(), R.string.sync_completed,
                                            R.string.sync_completed_message,
                                            results[0].getRowCount(),
                                            results[0].getRowsIgnored(),
                                            results[0].getRowsInserted(),
                                            results[0].getRowsUpdated(),
                                            results[0].getRowsDeleted(),
                                            results[1].getRowCount(),
                                            results[1].getRowsIgnored(),
                                            results[1].getRowsInserted(),
                                            results[1].getRowsUpdated(),
                                            results[1].getRowsDeleted());
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error synchronizing database", ex);
                                    Util.showErrorDialog(getActivity(), ex);
                                }
                            }
                        }).start();
                    }
                });
    }

    private void backup() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    dbHelper.backup();
                    Util.showToast(getActivity(), R.string.backup_completed);
                } catch (Exception ex) {
                    Log.e(TAG, "Error running backup", ex);
                    Util.showErrorDialog(getActivity(), ex);
                }
            }
        }).start();
    }

    private void restoreBackup() {
        Util.showConfirmDialog(getActivity(), R.string.restore_backup_confirm,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    dbHelper.restoreBackup();
                                    reload();
                                    Util.showToast(getActivity(), R.string.backup_restored);
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error restoring backup", ex);
                                    Util.showErrorDialog(getActivity(), ex);
                                }
                            }
                        }).start();
                    }
                });
    }

    private void reload() {
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                ((MainActivity) getActivity()).reload();
                loadArtists();
            }
        });
    }

    private void saveQueryParams(Set<String> tags) {
        SharedPreferences.Editor preferences = this.preferences.edit();
        preferences.putString(PREF_TITLE, etTitle.getString());
        preferences.putString(PREF_ARTIST, etArtist.getString());
        preferences.putString(PREF_MIN_YEAR, etMinYear.getString());
        preferences.putString(PREF_MAX_YEAR, etMaxYear.getString());
        preferences.putLong(PREF_MIN_ADDED, minAdded);
        preferences.putLong(PREF_MAX_ADDED, maxAdded);
        preferences.putBoolean(PREF_BOOKMARKED, rbBookmarked.isChecked());
        preferences.putBoolean(PREF_NOT_BOOKMARKED, rbNotBookmarked.isChecked());
        preferences.putLong(PREF_MIN_LAST_PLAYED, minLastPlayed);
        preferences.putLong(PREF_MAX_LAST_PLAYED, maxLastPlayed);
        preferences.putString(PREF_MIN_TIMES_PLAYED, etMinTimesPlayed.getString());
        preferences.putString(PREF_MAX_TIMES_PLAYED, etMaxTimesPlayed.getString());
        preferences.putInt(PREF_SORT_COLUMN, sSortColumn.getSelectedItemPosition());
        preferences.putBoolean(PREF_SORT_DESC, cbSortDesc.isChecked());;
        if (tags != null) {
            preferences.putStringSet(PREF_TAGS, tags);
        }
        preferences.apply();
    }

    public static QueryFragment newInstance() {
        return new QueryFragment();
    }
}
