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
    
    private static final int TAB_QUERY = 0;
    private static final int TAB_BOOKMARKS = 1;
    private static final int TAB_ARTISTS = 2;
    private static final int TAB_LAST_ADDED = 3;
    private static final int TAB_LAST_PLAYED = 4;
    private static final int TAB_MOST_PLAYED = 5;

    private static final String PREF_CURRENT_ITEM = "current_item";
    private static final String PREF_BOOKMARKS_SORT_COLUMN = "bookmarks_sort_column";
    private static final String PREF_BOOKMARKS_SORT_DESC = "bookmarks_sort_desc";

    private SharedPreferences preferences;

    private SectionsPagerAdapter tabAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "MainActivity.onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getPreferences(Context.MODE_PRIVATE);

        tabAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        viewPager = findViewById(R.id.container);
        viewPager.setAdapter(tabAdapter);
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

        // Save the current tab position and the sort options of the bookmarks tab.
        SharedPreferences.Editor preferences = this.preferences.edit();
        preferences.putInt(PREF_CURRENT_ITEM, viewPager.getCurrentItem());
        SongsFragment bookmarksFragment = (SongsFragment) tabAdapter.items[TAB_BOOKMARKS];
        if (bookmarksFragment != null) {
            preferences.putInt(PREF_BOOKMARKS_SORT_COLUMN, bookmarksFragment.getSortColumn());
            preferences.putBoolean(PREF_BOOKMARKS_SORT_DESC, bookmarksFragment.isSortDesc());
        }
        preferences.apply();

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
        Fragment fragment = tabAdapter.getItem(tab.getPosition());
        if (fragment instanceof ListFragment) {
            ((ListFragment) fragment).reverseSortOrder();
        }
    }

    public void reload() {
        Log.d(TAG, "MainActivity.reload()");
        for (Fragment fragment : tabAdapter.items) {
            // Reload all ListFragments.
            if (fragment instanceof ListFragment) {
                ((ListFragment) fragment).reloadData();
            }
        }
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter {
        private Fragment[] items;

        private SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            items = new Fragment[6];
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case TAB_QUERY:
                    if (items[position] == null) {
                        items[position] = QueryFragment.newInstance();
                    }
                    break;
                case TAB_BOOKMARKS:
                    if (items[position] == null) {
                        items[position] = SongsFragment.newInstance(
                                Song.BOOKMARKED + " IS NOT NULL", null,
                                preferences.getInt(PREF_BOOKMARKS_SORT_COLUMN,
                                        SongsFragment.SORT_COLUMN_BOOKMARKED),
                                preferences.getBoolean(PREF_BOOKMARKS_SORT_DESC, true));
                    }
                    break;
                case TAB_ARTISTS:
                    if (items[position] == null) {
                        items[position] = ArtistsFragment.newInstance(0, false);
                    }
                    break;
                case TAB_LAST_ADDED:
                    if (items[position] == null) {
                        items[position] = ArtistsFragment.newInstance(
                                ArtistsFragment.SORT_COLUMN_LAST_ADDED, true);
                    }
                    break;
                case TAB_LAST_PLAYED:
                    if (items[position] == null) {
                        items[position] = ArtistsFragment.newInstance(
                                ArtistsFragment.SORT_COLUMN_LAST_PLAYED, true);
                    }
                    break;
                case TAB_MOST_PLAYED:
                    if (items[position] == null) {
                        items[position] = ArtistsFragment.newInstance(
                                ArtistsFragment.SORT_COLUMN_TIMES_PLAYED, true);
                    }
                    break;
            }
            return items[position];
        }

        @Override
        public int getCount() {
            return items.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case TAB_QUERY:
                    return getString(R.string.query);
                case TAB_BOOKMARKS:
                    return getString(R.string.bookmarks);
                case TAB_ARTISTS:
                    return getString(R.string.artists);
                case TAB_LAST_ADDED:
                    return getString(R.string.last_added);
                case TAB_LAST_PLAYED:
                    return getString(R.string.last_played);
                case TAB_MOST_PLAYED:
                    return getString(R.string.most_played);
                default:
                    return null;
            }
        }
    }
}
