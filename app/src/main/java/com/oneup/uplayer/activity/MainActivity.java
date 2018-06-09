package com.oneup.uplayer.activity;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.fragment.ArtistsFragment;
import com.oneup.uplayer.fragment.ListFragment;
import com.oneup.uplayer.fragment.QueryFragment;
import com.oneup.uplayer.fragment.SongsFragment;

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {
    private static final String TAG = "UPlayer";

    private static final String PREF_CURRENT_ITEM = "current_item";
    private static final String PREF_BOOKMARKS_SORT_COLUMN = "bookmarks_sort_column";
    private static final String PREF_BOOKMARKS_SORT_DESC = "bookmarks_sort_desc";

    private SharedPreferences preferences;

    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "MainActivity.onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getPreferences(Context.MODE_PRIVATE);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        viewPager = findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setCurrentItem(preferences.getInt(PREF_CURRENT_ITEM, 2));

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addOnTabSelectedListener(this);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting WRITE_EXTERNAL_STORAGE permission");
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "MainActivity.onDestroy()");
        preferences.edit()
                .putInt(PREF_CURRENT_ITEM, viewPager.getCurrentItem())
                .putInt(PREF_BOOKMARKS_SORT_COLUMN,
                        sectionsPagerAdapter.bookmarksFragment.getSortColumn())
                .putBoolean(PREF_BOOKMARKS_SORT_DESC,
                        sectionsPagerAdapter.bookmarksFragment.isSortDesc())
                .apply();
        super.onDestroy();
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        // Reverse sort order when a ListFragment tab is reselected.
        Fragment fragment = sectionsPagerAdapter.getItem(tab.getPosition());
        if (fragment instanceof ListFragment) {
            ((ListFragment) fragment).reverseSortOrder();
        }
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter {
        private QueryFragment queryFragment;
        private SongsFragment bookmarksFragment;
        private ArtistsFragment lastAddedFragment;
        private ArtistsFragment artistsFragment;
        private ArtistsFragment lastPlayedFragment;
        private ArtistsFragment mostPlayedFragment;

        private SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    if (queryFragment == null) {
                        queryFragment = QueryFragment.newInstance();
                    }
                    return queryFragment;
                case 1:
                    if (bookmarksFragment == null) {
                        bookmarksFragment = SongsFragment.newInstance(
                                Song.BOOKMARKED + " IS NOT NULL", null,
                                preferences.getInt(PREF_BOOKMARKS_SORT_COLUMN,
                                        SongsFragment.SORT_COLUMN_BOOKMARKED),
                                preferences.getBoolean(PREF_BOOKMARKS_SORT_DESC, true));

                    }
                    return bookmarksFragment;
                case 2:
                    if (artistsFragment == null) {
                        artistsFragment = ArtistsFragment.newInstance(0, false);
                    }
                    return artistsFragment;
                case 3:
                    if (lastAddedFragment == null) {
                        lastAddedFragment = ArtistsFragment.newInstance(
                                ArtistsFragment.SORT_COLUMN_LAST_ADDED, true);
                    }
                    return lastAddedFragment;
                case 4:
                    if (lastPlayedFragment == null) {
                        lastPlayedFragment = ArtistsFragment.newInstance(
                                ArtistsFragment.SORT_COLUMN_LAST_PLAYED, true);
                    }
                    return lastPlayedFragment;
                case 5:
                    if (mostPlayedFragment == null) {
                        mostPlayedFragment = ArtistsFragment.newInstance(
                                ArtistsFragment.SORT_COLUMN_TIMES_PLAYED, true);
                    }
                    return mostPlayedFragment;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.query);
                case 1:
                    return getString(R.string.bookmarks);
                case 2:
                    return getString(R.string.artists);
                case 3:
                    return getString(R.string.last_added);
                case 4:
                    return getString(R.string.last_played);
                case 5:
                    return getString(R.string.most_played);
                default:
                    return null;
            }
        }
    }
}
