package com.oneup.uplayer.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.DateTimeActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.db.Stats;
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.widget.EditText;

import java.util.Set;

//TODO: Review QueryFragment and statistics, starting of DateTimeActivity and getting value, also in EditSongActivity.

public class QueryFragment extends Fragment implements
        View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = "UPlayer";

    private static final String KEY_TITLE = "title";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_MIN_YEAR = "minYear";
    private static final String KEY_MAX_YEAR = "maxYear";
    private static final String KEY_MIN_ADDED = "minAdded";
    private static final String KEY_MAX_ADDED = "maxAdded";
    private static final String KEY_MIN_LAST_PLAYED = "minLastPlayed";
    private static final String KEY_MAX_LAST_PLAYED = "maxLastPlayed";
    private static final String KEY_MIN_TIMES_PLAYED = "minTimesPlayed";
    private static final String KEY_MAX_TIMES_PLAYED = "maxTimesPlayed";
    private static final String KEY_ORDER_BY = "orderBy";
    private static final String KEY_ORDER_BY_DESC = "orderByDesc";
    private static final String KEY_TAGS = "tags";

    private static final int REQUEST_SELECT_MIN_ADDED = 1;
    private static final int REQUEST_SELECT_MAX_ADDED = 2;
    private static final int REQUEST_SELECT_MIN_LAST_PLAYED = 3;
    private static final int REQUEST_SELECT_MAX_LAST_PLAYED = 4;

    private DbHelper dbHelper;
    private SharedPreferences preferences;

    private EditText etTitle;
    private EditText etArtist;
    private EditText etMinYear;
    private EditText etMaxYear;
    private Button bMinAdded;
    private Button bMaxAdded;
    private Button bMinLastPlayed;
    private Button bMaxLastPlayed;
    private EditText etMinTimesPlayed;
    private EditText etMaxTimesPlayed;
    private Spinner sOrderBy;
    private CheckBox cbOrderByDesc;
    private Button bQuery;
    private Button bTags;
    private Button bStatistics;
    private Button bRestorePlaylist;
    private Button bSyncDatabase;
    private Button bBackup;

    private long minAdded;
    private long maxAdded;
    private long minLastPlayed;
    private long maxLastPlayed;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "QueryFragment.onCreateView()");

        dbHelper = new DbHelper(getActivity());
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        View rootView = inflater.inflate(R.layout.fragment_query, container, false);

        etTitle = rootView.findViewById(R.id.etTitle);
        setEditTextString(etTitle, KEY_TITLE);

        etArtist = rootView.findViewById(R.id.etArtist);
        setEditTextString(etArtist, KEY_ARTIST);

        bMinAdded = rootView.findViewById(R.id.bMinAdded);
        bMinAdded.setOnClickListener(this);
        bMinAdded.setOnLongClickListener(this);
        if (minAdded == 0) {
            minAdded = preferences.getLong(KEY_MIN_ADDED, 0);
        }
        if (minAdded > 0) {
            bMinAdded.setText(Util.formatDateTime(minAdded));
        }

        bMaxAdded = rootView.findViewById(R.id.bMaxAdded);
        bMaxAdded.setOnClickListener(this);
        bMaxAdded.setOnLongClickListener(this);
        if (maxAdded == 0) {
            maxAdded = preferences.getLong(KEY_MAX_ADDED, 0);
        }
        if (maxAdded > 0) {
            bMaxAdded.setText(Util.formatDateTime(maxAdded));
        }

        etMinYear = rootView.findViewById(R.id.etMinYear);
        setEditTextString(etMinYear, KEY_MIN_YEAR);

        etMaxYear = rootView.findViewById(R.id.etMaxYear);
        setEditTextString(etMaxYear, KEY_MAX_YEAR);

        bMinLastPlayed = rootView.findViewById(R.id.bMinLastPlayed);
        bMinLastPlayed.setOnClickListener(this);
        bMinLastPlayed.setOnLongClickListener(this);
        if (minLastPlayed == 0) {
            minLastPlayed = preferences.getLong(KEY_MIN_LAST_PLAYED, 0);
        }
        if (minLastPlayed > 0) {
            bMinLastPlayed.setText(Util.formatDateTime(minLastPlayed));
        }

        bMaxLastPlayed = rootView.findViewById(R.id.bMaxLastPlayed);
        bMaxLastPlayed.setOnClickListener(this);
        bMaxLastPlayed.setOnLongClickListener(this);
        if (maxLastPlayed == 0) {
            maxLastPlayed = preferences.getLong(KEY_MAX_LAST_PLAYED, 0);
        }
        if (maxLastPlayed > 0) {
            bMaxLastPlayed.setText(Util.formatDateTime(maxLastPlayed));
        }

        etMinTimesPlayed = rootView.findViewById(R.id.etMinTimesPlayed);
        setEditTextString(etMinTimesPlayed, KEY_MIN_TIMES_PLAYED);

        etMaxTimesPlayed = rootView.findViewById(R.id.etMaxTimesPlayed);
        setEditTextString(etMaxTimesPlayed, KEY_MAX_TIMES_PLAYED);

        sOrderBy = rootView.findViewById(R.id.sOrderBy);
        setSpinnerSelection(sOrderBy, KEY_ORDER_BY);

        cbOrderByDesc = rootView.findViewById(R.id.cbOrderByDesc);
        setCheckBoxChecked(cbOrderByDesc, KEY_ORDER_BY_DESC);

        bQuery = rootView.findViewById(R.id.bQuery);
        bQuery.setOnClickListener(this);

        bTags = rootView.findViewById(R.id.bTags);
        bTags.setOnClickListener(this);

        bStatistics = rootView.findViewById(R.id.bStatistics);
        bStatistics.setOnClickListener(this);

        bRestorePlaylist = rootView.findViewById(R.id.bRestorePlaylist);
        bRestorePlaylist.setOnClickListener(this);

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
        Log.d(TAG, "QueryFragment.onActivityResult(" + requestCode + ", " + resultCode + ")");
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
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "QueryFragment.onDestroy()");
        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == bMinAdded) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.min_added);
            if (minAdded > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, minAdded);
            }
            startActivityForResult(intent, REQUEST_SELECT_MIN_ADDED);
        } else if (v == bMaxAdded) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.max_added);
            if (maxAdded > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, maxAdded);
            }
            startActivityForResult(intent, REQUEST_SELECT_MAX_ADDED);
        } else if (v == bMinLastPlayed) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.min_last_played);
            if (minLastPlayed > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, minLastPlayed);
            }
            startActivityForResult(intent, REQUEST_SELECT_MIN_LAST_PLAYED);
        } else if (v == bMaxLastPlayed) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.max_last_played);
            if (maxLastPlayed > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, maxLastPlayed);
            }
            startActivityForResult(intent, REQUEST_SELECT_MAX_LAST_PLAYED);
        } else if (v == bQuery) {
            query(null);
        } else if (v == bTags) {
            final String[] tags = dbHelper.querySongTags().toArray(new String[0]);
            final Set<String> checkedTags = preferences.getStringSet(KEY_TAGS,
                    new ArraySet<String>());
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
                            query(checkedTags);
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
        } else if (v == bStatistics) {
            Stats stats = dbHelper.queryStats(null);
            Util.showInfoDialog(getActivity(), R.string.statistics, R.string.statistics_message,
                    stats.getSongCount(), Util.formatDuration(stats.getSongsDuration()),
                    stats.getArtistCount(), Math.round(
                            (double) stats.getSongCount() / stats.getArtistCount()),
                    stats.getSongsPlayed(), Util.formatPercent(
                            (double) stats.getSongsPlayed() / stats.getSongCount()),
                    stats.getSongsUnplayed(), Util.formatPercent(
                            (double) stats.getSongsUnplayed() / stats.getSongCount()),
                    stats.getSongsTagged(), Util.formatPercent(
                            (double) stats.getSongsTagged() / stats.getSongCount()),
                    stats.getSongsUntagged(), Util.formatPercent(
                            (double) stats.getSongsUntagged() / stats.getSongCount()),
                    stats.getTimesPlayed(), Util.formatDuration(stats.getPlayedDuration()),
                    Math.round((double) stats.getTimesPlayed() / stats.getSongsPlayed()),
                    Util.formatDuration(stats.getPlayedDuration() / stats.getSongsPlayed()));
        } else if (v == bRestorePlaylist) {
            getActivity().startService(new Intent(getActivity(), MainService.class)
                    .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_RESTORE_PLAYLIST));
        } else if (v == bSyncDatabase) {
            Log.d(TAG, "Syncing database");
            try {
                dbHelper.syncWithMediaStore(getActivity());
            } catch (Exception ex) {
                Log.e(TAG, "Error syncing database", ex);
                Util.showErrorDialog(getActivity(), ex);
            }
        } else if (v == bBackup) {
            Log.d(TAG, "Running backup");
            try {
                dbHelper.backup();
                Toast.makeText(getActivity(), R.string.backup_completed, Toast.LENGTH_SHORT).show();
            } catch (Exception ex) {
                Log.e(TAG, "Error running backup", ex);
                Util.showErrorDialog(getActivity(), ex);
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == bMinAdded) {
            minAdded = 0;
            bMinAdded.setText(R.string.min_added);
        } else if (v == bMaxAdded) {
            maxAdded = 0;
            bMaxAdded.setText(R.string.max_added);
        } else if (v == bMinLastPlayed) {
            minLastPlayed = 0;
            bMinLastPlayed.setText(R.string.min_last_played);
        } else if (v == bMaxLastPlayed) {
            maxLastPlayed = 0;
            bMaxLastPlayed.setText(R.string.max_last_played);
        } else if (v == bBackup) {
            new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.ic_dialog_warning)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.restore_backup_confirm)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                dbHelper.restoreBackup();
                                Toast.makeText(getActivity(), R.string.backup_restored,
                                        Toast.LENGTH_SHORT).show();
                                //((MainActivity) getActivity()).notifyDataSetChanged();
                            } catch (Exception ex) {
                                Log.e(TAG, "Error restoring backup", ex);
                                Util.showErrorDialog(getActivity(), ex);
                            }
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        }
        return true;
    }

    private void setSpinnerSelection(Spinner view, String preferencesKey) {
        if (preferences.contains(preferencesKey)) {
            view.setSelection(preferences.getInt(preferencesKey, 0));
        }
    }

    private void setEditTextString(EditText view, String preferencesKey) {
        if (preferences.contains(preferencesKey)) {
            view.setString(preferences.getString(preferencesKey, null));
        }
    }

    private void setCheckBoxChecked(CheckBox view, String preferencesKey) {
        if (preferences.contains(preferencesKey)) {
            view.setChecked(preferences.getBoolean(preferencesKey, false));
        }
    }

    private void query(Set<String> tags) {
        SharedPreferences.Editor preferences = this.preferences.edit();

        String selection = null, orderBy = null;

        String title = etTitle.getString();
        preferences.putString(KEY_TITLE, title);
        if (title.length() > 0) {
            selection = Song.TITLE + " LIKE '%" + title + "%'";
        }

        String artist = etArtist.getString();
        preferences.putString(KEY_ARTIST, artist);
        if (artist.length() > 0) {
            selection = appendSelection(selection, Song.ARTIST + " LIKE '%" + artist + "%'");
        }

        String minYear = etMinYear.getString();
        preferences.putString(KEY_MIN_YEAR, minYear);
        if (minYear.length() > 0) {
            selection = appendSelection(selection, Song.YEAR + ">=" + minYear);
        }

        String maxYear = etMaxYear.getString();
        preferences.putString(KEY_MAX_YEAR, maxYear);
        if (maxYear.length() > 0) {
            selection = appendSelection(selection, Song.YEAR + "<=" + maxYear);
        }

        preferences.putLong(KEY_MIN_ADDED, minAdded);
        if (minAdded > 0) {
            selection = appendSelection(selection, Song.ADDED + ">=" + minAdded);
        }

        preferences.putLong(KEY_MAX_ADDED, maxAdded);
        if (maxAdded > 0) {
            selection = appendSelection(selection, Song.ADDED + "<=" + maxAdded);
        }

        preferences.putLong(KEY_MIN_LAST_PLAYED, minLastPlayed);
        if (minLastPlayed > 0) {
            selection = appendSelection(selection, Song.LAST_PLAYED + ">=" + minLastPlayed);
        }

        preferences.putLong(KEY_MAX_LAST_PLAYED, maxLastPlayed);
        if (maxLastPlayed > 0) {
            selection = appendSelection(selection, Song.LAST_PLAYED + "<=" + maxLastPlayed);
        }

        String minTimesPlayed = etMinTimesPlayed.getString();
        preferences.putString(KEY_MIN_TIMES_PLAYED, minTimesPlayed);
        if (minTimesPlayed.length() > 0) {
            selection = appendSelection(selection, Song.TIMES_PLAYED + ">=" + minTimesPlayed);
        }

        String maxTimesPlayed = etMaxTimesPlayed.getString();
        preferences.putString(KEY_MAX_TIMES_PLAYED, maxTimesPlayed);
        if (maxTimesPlayed.length() > 0) {
            selection = appendSelection(selection, Song.TIMES_PLAYED + "<=" + maxTimesPlayed);
        }

        if (tags != null) {
            preferences.putStringSet(KEY_TAGS, tags);

            String tagSelection;
            if (tags.size() == 0) {
                tagSelection = "IS NULL";
            } else {
                StringBuilder sbTags = new StringBuilder();
                for (String tag : tags) {
                    sbTags.append(sbTags.length() == 0 ? '(' : ',');
                    sbTags.append('\'');
                    sbTags.append(tag);
                    sbTags.append('\'');
                }
                sbTags.append(')');
                tagSelection = "IN" + sbTags;
            }
            selection = appendSelection(selection,
                    Song.TAG + " " + tagSelection);
        }

        int orderByColumn = sOrderBy.getSelectedItemPosition();
        preferences.putInt(KEY_ORDER_BY, orderByColumn);
        switch (orderByColumn) {
            case 1:
                orderBy = Song.TITLE;
                break;
            case 2:
                orderBy = Song.ARTIST;
                break;
            case 3:
                orderBy = Song.YEAR;
                break;
            case 4:
                orderBy = Song.ADDED;
                break;
            case 5:
                orderBy = Song.LAST_PLAYED;
                break;
            case 6:
                orderBy = Song.TIMES_PLAYED;
                break;
        }

        boolean orderByDesc = cbOrderByDesc.isChecked();
        preferences.putBoolean(KEY_ORDER_BY_DESC, orderByDesc);
        if (orderBy != null) {
            if (orderByDesc) {
                orderBy += " DESC";
            }
            orderBy += "," + Song.TITLE;
        }

        startActivity(new Intent(getActivity(), SongsActivity.class)
                .putExtras(SongsFragment.getArguments(
                        selection, null, orderBy)));

        preferences.apply();
    }

    public static QueryFragment newInstance() {
        return new QueryFragment();
    }

    private static String appendSelection(String selection, String s) {
        return selection == null ? s : selection + " AND " + s;
    }
}
