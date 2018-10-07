package com.oneup.uplayer.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.MainActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.util.Util;

public class QueryFragment extends Fragment
        implements View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = "UPlayer";

    private static final String PREF_SORT_COLUMN = "sort_column";
    private static final String PREF_SORT_DESC = "sort_desc";

    private SharedPreferences preferences;
    private DbHelper dbHelper;
    private boolean viewCreated;

    private FilterFragment filterFragment;

    private Spinner sSortColumn;
    private CheckBox cbSortDesc;
    private Button bQuery;
    private Button bStatistics;
    private Button bSyncDatabase;
    private Button bBackup;

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

        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        filterFragment = (FilterFragment) fragmentManager.findFragmentById(R.id.filterFragment);
        filterFragment.setSelectPlaylistConfirmId(-1);
        fragmentTransaction.commit();

        sSortColumn = rootView.findViewById(R.id.sSortColumn);
        if (!viewCreated) {
            sSortColumn.setSelection(preferences.getInt(PREF_SORT_COLUMN, 0));
        }

        cbSortDesc = rootView.findViewById(R.id.cbSortDesc);
        if (!viewCreated) {
            cbSortDesc.setChecked(preferences.getBoolean(PREF_SORT_DESC, false));
        }

        bQuery = rootView.findViewById(R.id.bQuery);
        bQuery.setOnClickListener(this);

        bStatistics = rootView.findViewById(R.id.bStatistics);
        bStatistics.setOnClickListener(this);

        bSyncDatabase = rootView.findViewById(R.id.bSyncDatabase);
        bSyncDatabase.setOnClickListener(this);

        bBackup = rootView.findViewById(R.id.bBackup);
        bBackup.setOnClickListener(this);
        bBackup.setOnLongClickListener(this);

        viewCreated = true;
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
            query();
        } else if (v == bStatistics) {
            try {
                dbHelper.queryStats(true,
                        !filterFragment.hasBookmarkedSelection(),
                        !filterFragment.hasTagSelection(),
                        !filterFragment.hasPlaylistSelection(),
                        filterFragment.getSelection(), filterFragment.getSelectionArgs())
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
        if (v == bBackup) {
            restoreBackup();
        }
        return true;
    }

    private void query() {
        int sortColumn = sSortColumn.getSelectedItemPosition();
        boolean sortDesc = cbSortDesc.isChecked();

        startActivity(new Intent(getActivity(), SongsActivity.class)
                .putExtras(SongsFragment.getArguments(
                        filterFragment.getSelection(), filterFragment.getSelectionArgs(),
                        sortColumn, sortDesc)));

        preferences.edit()
                .putInt(PREF_SORT_COLUMN, sortColumn)
                .putBoolean(PREF_SORT_DESC, sortDesc)
                .apply();
    }

    private void syncDatabase() {
        Util.showConfirmDialog(getActivity(),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ProgressDialog progressDialog = ProgressDialog.show(getActivity(),
                                getString(R.string.synchronizing_database), null, true, false);
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
                                            results[0].getRowsInserted(),
                                            results[0].getRowsUpdated(),
                                            results[0].getRowsDeleted(),
                                            results[1].getRowCount(),
                                            results[1].getRowsInserted(),
                                            results[1].getRowsUpdated(),
                                            results[1].getRowsDeleted());
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error synchronizing database", ex);
                                    Util.showErrorDialog(getActivity(), ex);
                                } finally {
                                    progressDialog.dismiss();
                                }
                            }
                        }).start();
                    }
                }, R.string.sync_database_confirm);
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
        Util.showConfirmDialog(getActivity(),
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ProgressDialog progressDialog = ProgressDialog.show(getActivity(),
                                getString(R.string.restoring_backup), null, true, false);
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
                                } finally {
                                    progressDialog.dismiss();
                                }
                            }
                        }).start();
                    }
                }, R.string.restore_backup_confirm);
    }

    private void reload() {
        getActivity().runOnUiThread(new Runnable() {

            @Override
            public void run() {
                ((MainActivity) getActivity()).reload();
            }
        });
    }

    public static QueryFragment newInstance() {
        return new QueryFragment();
    }
}
