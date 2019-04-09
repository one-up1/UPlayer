package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.oneup.uplayer.R;
import com.oneup.uplayer.fragment.FilterFragment;

public class FilterActivity extends AppCompatActivity {
    public static final String EXTRA_SHOW_ARTIST_FILTER = "com.oneup.extra.SHOW_ARTIST_FILTER";
    public static final String EXTRA_VALUES = "com.oneup.extra.VALUES";
    public static final String EXTRA_SELECTION = "com.oneup.extra.SELECTION";
    public static final String EXTRA_SELECTION_ARGS = "com.oneup.extra.SELECTION_ARGS";
    public static final String EXTRA_HAS_BOOKMARKED_SELECTION =
            "com.oneup.extra.HAS_BOOKMARKED_SELECTION";
    public static final String EXTRA_HAS_ARCHIVED_SELECTION =
            "com.oneup.extra.HAS_ARCHIVED_SELECTION";
    public static final String EXTRA_HAS_TAG_SELECTION = "com.oneup.extra.HAS_TAG_SELECTION";
    public static final String EXTRA_HAS_PLAYLIST_SELECTION =
            "com.oneup.extra.HAS_PLAYLIST_SELECTION";

    private FilterFragment filterFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);
        setTitle(R.string.filter);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        filterFragment = (FilterFragment) fragmentManager.findFragmentById(R.id.filterFragment);
        filterFragment.setSelectPlaylistConfirmId(-1);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_filter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ok:
                setResult(RESULT_OK, new Intent()
                        .putExtra(EXTRA_VALUES, filterFragment.getValues())
                        .putExtra(EXTRA_SELECTION, filterFragment.getSelection())
                        .putExtra(EXTRA_SELECTION_ARGS, filterFragment.getSelectionArgs())
                        .putExtra(EXTRA_HAS_BOOKMARKED_SELECTION,
                                filterFragment.hasBookmarkedSelection())
                        .putExtra(EXTRA_HAS_ARCHIVED_SELECTION,
                                filterFragment.hasArchivedSelection())
                        .putExtra(EXTRA_HAS_TAG_SELECTION, filterFragment.hasTagSelection())
                        .putExtra(EXTRA_HAS_PLAYLIST_SELECTION,
                                filterFragment.hasPlaylistSelection()));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
