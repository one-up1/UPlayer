package com.oneup.uplayer.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
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

public class QueryFragment extends Fragment implements AdapterView.OnItemSelectedListener,
        View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = "UPlayer";

    private static final String PREF_SORT_COLUMN = "sort_column";
    private static final String PREF_SORT_DESC = "sort_desc";

    private static final int REQUEST_SELECT_MIN_ADDED = 1;
    private static final int REQUEST_SELECT_MAX_ADDED = 2;
    private static final int REQUEST_SELECT_PLAYLISTS = 3;
    private static final int REQUEST_SELECT_MIN_LAST_PLAYED = 4;
    private static final int REQUEST_SELECT_MAX_LAST_PLAYED = 5;

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
    private RadioButton rbBookmarked;
    private RadioButton rbNotBookmarked;
    private Button bTags;
    private Button bPlaylists;
    private Button bMinLastPlayed;
    private Button bMaxLastPlayed;
    private EditText etMinTimesPlayed;
    private EditText etMaxTimesPlayed;
    private Spinner sSortColumn;
    private CheckBox cbSortDesc;
    private Button bQuery;
    private Button bStatistics;
    private Button bSyncDatabase;
    private Button bBackup;

    private long minAdded;
    private long maxAdded;
    private List<String> tags;
    private List<Playlist> playlists;
    private long minLastPlayed;
    private long maxLastPlayed;

    private String selection;
    private List<String> selectionArgs;

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

        etArtist = rootView.findViewById(R.id.etArtist);

        sArtist = rootView.findViewById(R.id.sArtist);
        sArtist.setOnItemSelectedListener(this);
        loadArtists();

        etMinYear = rootView.findViewById(R.id.etMinYear);

        etMaxYear = rootView.findViewById(R.id.etMaxYear);

        bMinAdded = rootView.findViewById(R.id.bMinAdded);
        bMinAdded.setOnClickListener(this);
        bMinAdded.setOnLongClickListener(this);

        bMaxAdded = rootView.findViewById(R.id.bMaxAdded);
        bMaxAdded.setOnClickListener(this);
        bMaxAdded.setOnLongClickListener(this);

        rbBookmarked = rootView.findViewById(R.id.rbBookmarked);
        rbNotBookmarked = rootView.findViewById(R.id.rbNotBookmarked);

        bTags = rootView.findViewById(R.id.bTags);
        bTags.setOnClickListener(this);
        bTags.setOnLongClickListener(this);

        bPlaylists = rootView.findViewById(R.id.bPlaylists);
        bPlaylists.setOnClickListener(this);
        bPlaylists.setOnLongClickListener(this);

        bMinLastPlayed = rootView.findViewById(R.id.bMinLastPlayed);
        bMinLastPlayed.setOnClickListener(this);
        bMinLastPlayed.setOnLongClickListener(this);

        bMaxLastPlayed = rootView.findViewById(R.id.bMaxLastPlayed);
        bMaxLastPlayed.setOnClickListener(this);
        bMaxLastPlayed.setOnLongClickListener(this);

        etMinTimesPlayed = rootView.findViewById(R.id.etMinTimesPlayed);

        etMaxTimesPlayed = rootView.findViewById(R.id.etMaxTimesPlayed);

        sSortColumn = rootView.findViewById(R.id.sSortColumn);
        sSortColumn.setSelection(preferences.getInt(PREF_SORT_COLUMN, 0));

        cbSortDesc = rootView.findViewById(R.id.cbSortDesc);
        cbSortDesc.setChecked(preferences.getBoolean(PREF_SORT_DESC, false));

        bQuery = rootView.findViewById(R.id.bQuery);
        bQuery.setOnClickListener(this);

        bStatistics = rootView.findViewById(R.id.bStatistics);
        bStatistics.setOnClickListener(this);

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
                case REQUEST_SELECT_PLAYLISTS:
                    if (data.hasExtra(PlaylistsActivity.EXTRA_PLAYLIST)) {
                        getActivity().startService(new Intent(getActivity(), MainService.class)
                                .putExtra(MainService.EXTRA_ACTION,
                                        MainService.ACTION_PLAY_PLAYLIST)
                                .putExtra(MainService.EXTRA_PLAYLIST,
                                        data.getParcelableExtra(PlaylistsActivity.EXTRA_PLAYLIST)));
                    } else if (data.hasExtra(PlaylistsActivity.EXTRA_PLAYLISTS)) {
                        playlists = data.getParcelableArrayListExtra(
                                PlaylistsActivity.EXTRA_PLAYLISTS);
                        bPlaylists.setText(Util.getCountString(getActivity(),
                                R.plurals.playlists, playlists.size()));
                    }
                    break;
                case REQUEST_SELECT_MIN_LAST_PLAYED:
                    minLastPlayed = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMinLastPlayed.setText(Util.formatDateTime(minLastPlayed));
                    break;
                case REQUEST_SELECT_MAX_LAST_PLAYED:
                    maxLastPlayed = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMaxLastPlayed.setText(Util.formatDateTime(maxLastPlayed));
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
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
        } else if (v == bTags) {
            showTags();
        } else if (v == bPlaylists) {
            getSelection(this.tags, null);
            startActivityForResult(new Intent(getActivity(), PlaylistsActivity.class)
                            .putExtras(PlaylistsActivity.PlaylistsFragment.getArguments(
                                    selection, getSelectionArgs(), true)),
                    REQUEST_SELECT_PLAYLISTS);
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
            query();
        } else if (v == bStatistics) {
            try {
                getSelection(this.tags, this.playlists);
                dbHelper.queryStats(true, selection, getSelectionArgs())
                        .showDialog(getActivity(), null);
            } catch (Exception ex) {
                Log.e(TAG, "Error querying stats", ex);
                Util.showErrorDialog(getActivity(), ex);
            }
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
        } else if (v == bTags) {
            tags = null;
            bTags.setText(R.string.select_tags);
        } else if (v == bPlaylists) {
            playlists = null;
            bPlaylists.setText(R.string.select_playlists);
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

    private void query() {
        getSelection(this.tags, this.playlists);
        int sortColumn = sSortColumn.getSelectedItemPosition();
        boolean sortDesc = cbSortDesc.isChecked();

        startActivity(new Intent(getActivity(), SongsActivity.class)
                .putExtras(SongsFragment.getArguments(
                        selection, getSelectionArgs(),
                        sortColumn, sortDesc)));

        preferences.edit()
                .putInt(PREF_SORT_COLUMN, sortColumn)
                .putBoolean(PREF_SORT_DESC, sortDesc)
                .apply();
    }

    private void showTags() {
        getSelection(null, this.playlists);
        final String[] tags = dbHelper.querySongTags(selection, getSelectionArgs())
                .toArray(new String[0]);
        if (tags.length == 0) {
            Util.showToast(getActivity(), R.string.no_tags);
            return;
        }

        this.tags = new ArrayList<>();
        final AlertDialog tagsDialog = new AlertDialog.Builder(getActivity())
                .setTitle(Util.getCountString(getActivity(), R.plurals.tags, tags.length))
                .setMultiChoiceItems(tags, null, new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            QueryFragment.this.tags.add(tags[which]);
                        } else {
                            QueryFragment.this.tags.remove(tags[which]);
                        }
                    }
                })
                .setPositiveButton(R.string.select_all, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {

                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        bTags.setText(Util.getCountString(getActivity(),
                                R.plurals.tags, QueryFragment.this.tags.size()));
                    }
                })
                .create();
        tagsDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                final ListView listView = tagsDialog.getListView();
                tagsDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                        new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                for (int i = 0; i < tags.length; i++) {
                                    listView.performItemClick(listView, i, i);
                                }
                            }
                        });
            }
        });
        tagsDialog.show();
    }

    private void getSelection(List<String> tags, List<Playlist> playlists) {
        selection = null;
        selectionArgs = new ArrayList<>();

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
    }

    private String[] getSelectionArgs() {
        return selection == null ? null : selectionArgs.toArray(new String[0]);
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

    public static QueryFragment newInstance() {
        return new QueryFragment();
    }
}
