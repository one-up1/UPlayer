package com.oneup.uplayer.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.util.Util;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "UPlayer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceClickListener  {
        private DbHelper dbHelper;

        private Preference pSyncDatabase;
        private Preference pBackup;
        private Preference pRestoreBackup;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.settings);

            dbHelper = new DbHelper(getActivity());

            pSyncDatabase = findPreference(getString(R.string.key_sync_database));
            pSyncDatabase.setOnPreferenceClickListener(this);

            pBackup = findPreference(getString(R.string.key_backup));
            pBackup.setOnPreferenceClickListener(this);

            pRestoreBackup = findPreference(getString(R.string.key_restore_backup));
            pRestoreBackup.setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (preference == pSyncDatabase) {
                syncDatabase();
            } else if (preference == pBackup) {
                backup();
            } else if (preference == pRestoreBackup) {
                restoreBackup();
            }
            return false;
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
    }
}
