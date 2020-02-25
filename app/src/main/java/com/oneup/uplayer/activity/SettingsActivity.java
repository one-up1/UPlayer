package com.oneup.uplayer.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.util.Settings;
import com.oneup.util.Utils;

@SuppressWarnings("deprecation")
public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "UPlayer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    // Use deprecated PreferenceFragment because PreferenceFragmentCompat has issues with EditTextPreference inputType.
    public static class SettingsFragment extends PreferenceFragment
            implements Preference.OnPreferenceClickListener  {
        private Settings settings;
        private int maxVolume;

        private DbHelper dbHelper;

        private Preference pSyncDatabase;
        private Preference pBackup;
        private Preference pRestoreBackup;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            settings = Settings.get(getActivity());
            maxVolume = settings.getXmlInt(R.string.key_max_volume, Settings.DEFAULT_VOLUME);

            dbHelper = new DbHelper(getActivity());

            pSyncDatabase = findPreference(getString(R.string.key_sync_database));
            pSyncDatabase.setOnPreferenceClickListener(this);

            pBackup = findPreference(getString(R.string.key_backup));
            pBackup.setOnPreferenceClickListener(this);

            pRestoreBackup = findPreference(getString(R.string.key_restore_backup));
            pRestoreBackup.setOnPreferenceClickListener(this);
        }

        @Override
        public void onDestroy() {
            // Update saved volume scaled to new maxVolume if changed.
            String sMaxVolume = ((EditTextPreference)
                    findPreference(getString(R.string.key_max_volume))).getText();
            int maxVolume = sMaxVolume.isEmpty() ? 0 : Integer.parseInt(sMaxVolume);
            if (maxVolume != this.maxVolume) {
                int volume = settings.getInt(R.string.key_volume, Settings.DEFAULT_VOLUME);
                Log.d(TAG, "maxVolume updated from " + this.maxVolume + " to " + maxVolume +
                        ", volume=" + volume);

                volume = (int) (((double) volume / this.maxVolume) * maxVolume);
                Log.d(TAG, "volume=" + volume);

                settings.edit().putInt(R.string.key_volume, volume).apply();
            }

            MainService.update(getActivity(), true, null);
            super.onDestroy();
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (preference == pSyncDatabase) {
                syncDatabase();
                return true;
            } else if (preference == pBackup) {
                backup();
                return true;
            } else if (preference == pRestoreBackup) {
                restoreBackup();
                return true;
            }
            return false;
        }

        private void syncDatabase() {
            Utils.showConfirmDialog(getActivity(), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final ProgressDialog progressDialog = ProgressDialog.show(getActivity(),
                            getString(R.string.synchronizing_database), null, true, false);
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                final DbHelper.SyncResult[] results =
                                        dbHelper.syncWithMediaStore(getActivity());
                                getActivity().runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        Utils.showInfoDialog(getActivity(),
                                                R.string.sync_completed,
                                                R.string.sync_completed_message,
                                                results[0].getRowCount(),
                                                results[0].getRowsInserted(),
                                                results[0].getRowsUpdated(),
                                                results[0].getRowsDeleted(),
                                                results[1].getRowCount(),
                                                results[1].getRowsInserted(),
                                                results[1].getRowsUpdated(),
                                                results[1].getRowsDeleted());
                                    }
                                });
                            } catch (final Exception ex) {
                                Log.e(TAG, "Error synchronizing database", ex);
                                getActivity().runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        Utils.showErrorDialog(getActivity(), ex);
                                    }
                                });
                            } finally {
                                progressDialog.dismiss();
                            }
                        }
                    }).start();
                }
            }, R.string.app_name, R.string.sync_database_confirm);
        }

        private void backup() {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        dbHelper.backup();
                        getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                Utils.showToast(getActivity(), R.string.backup_completed);
                            }
                        });
                    } catch (final Exception ex) {
                        Log.e(TAG, "Error running backup", ex);
                        getActivity().runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                Utils.showErrorDialog(getActivity(), ex);
                            }
                        });
                    }
                }
            }).start();
        }

        private void restoreBackup() {
            Utils.showConfirmDialog(getActivity(), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final ProgressDialog progressDialog = ProgressDialog.show(getActivity(),
                            getString(R.string.restoring_backup), null, true, false);
                    new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                dbHelper.restoreBackup();
                                getActivity().runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        Utils.showToast(getActivity(), R.string.backup_restored);
                                    }
                                });
                            } catch (final Exception ex) {
                                Log.e(TAG, "Error restoring backup", ex);
                                getActivity().runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        Utils.showErrorDialog(getActivity(), ex);
                                    }
                                });
                            } finally {
                                progressDialog.dismiss();
                            }
                        }
                    }).start();
                }
            }, R.string.app_name, R.string.restore_backup_confirm);
        }
    }
}
