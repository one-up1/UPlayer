package com.oneup.uplayer.fragment;

import android.content.Intent;
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
import com.oneup.uplayer.activity.SettingsActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.util.Settings;
import com.oneup.uplayer.util.Util;

public class QueryFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "UPlayer";

    private Settings settings;
    private DbHelper dbHelper;
    private boolean viewCreated;

    private FilterFragment filterFragment;

    private Spinner sSortColumn;
    private CheckBox cbSortDesc;
    private Button bQuery;
    private Button bStatistics;
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
        filterFragment.setSelectPlaylistConfirmId(-1);
        fragmentTransaction.commit();

        sSortColumn = rootView.findViewById(R.id.sSortColumn);
        if (!viewCreated) {
            sSortColumn.setSelection(settings.getInt(R.string.key_query_sort_column, 0));
        }

        cbSortDesc = rootView.findViewById(R.id.cbSortDesc);
        if (!viewCreated) {
            cbSortDesc.setChecked(settings.getBoolean(R.string.key_query_sort_desc, false));
        }

        bQuery = rootView.findViewById(R.id.bQuery);
        bQuery.setOnClickListener(this);

        bStatistics = rootView.findViewById(R.id.bStatistics);
        bStatistics.setOnClickListener(this);

        bSettings = rootView.findViewById(R.id.bSettings);
        bSettings.setOnClickListener(this);

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
                        null, null,
                        filterFragment.getSelection(), filterFragment.getSelectionArgs())
                        .showDialog(getActivity(), null);
            } catch (Exception ex) {
                Log.e(TAG, "Error querying stats", ex);
                Util.showErrorDialog(getActivity(), ex);
            }
        } else if (v == bSettings) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        }
    }

    private void query() {
        int sortColumn = sSortColumn.getSelectedItemPosition();
        boolean sortDesc = cbSortDesc.isChecked();

        startActivity(new Intent(getActivity(), SongsActivity.class)
                .putExtras(SongsFragment.getArguments(
                        filterFragment.getSelection(), filterFragment.getSelectionArgs(),
                        sortColumn, sortDesc)));

        settings.edit()
                .putInt(R.string.key_query_sort_column, sortColumn)
                .putBoolean(R.string.key_query_sort_desc, sortDesc)
                .apply();
    }

    public static QueryFragment newInstance() {
        return new QueryFragment();
    }
}
