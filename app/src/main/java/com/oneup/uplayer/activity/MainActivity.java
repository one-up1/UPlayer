package com.oneup.uplayer.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.fragment.ArtistsFragment;
import com.oneup.uplayer.fragment.ListFragment;
import com.oneup.uplayer.fragment.QueryFragment;
import com.oneup.uplayer.fragment.SongsFragment;
import com.oneup.uplayer.util.Settings;

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {
    private static final String TAG = "UPlayer";
    
    private static final int TAB_QUERY = 0;
    private static final int TAB_BOOKMARKS = 1;
    private static final int TAB_ARTISTS = 2;
    private static final int TAB_LAST_ADDED = 3;
    private static final int TAB_LAST_PLAYED = 4;
    private static final int TAB_MOST_PLAYED = 5;

    private Settings settings;

    private SectionsPagerAdapter tabAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "MainActivity.onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = Settings.get(this);

        tabAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        viewPager = findViewById(R.id.container);
        viewPager.setAdapter(tabAdapter);
        viewPager.setCurrentItem(settings.getInt(R.string.key_selected_tab, TAB_ARTISTS));

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addOnTabSelectedListener(this);

        if (hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            Log.d(TAG, "Permissions granted");
        } else {
            Log.d(TAG, "Requesting permissions");
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE
            }, 0);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "MainActivity.onDestroy()");

        // Save the current tab position and the sort options of the bookmarks tab.
        Settings.Editor settings = this.settings.edit();
        settings.putInt(R.string.key_selected_tab, viewPager.getCurrentItem());
        SongsFragment bookmarksFragment = (SongsFragment) tabAdapter.items[TAB_BOOKMARKS];
        if (bookmarksFragment != null) {
            settings.putInt(R.string.key_bookmarks_sort_column, bookmarksFragment.getSortColumn());
            settings.putBoolean(R.string.key_bookmarks_sort_desc, bookmarksFragment.isSortDesc());
        }
        settings.apply();

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

    private boolean hasPermission(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter {
        private final Fragment[] items;

        private SectionsPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
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
                                settings.getInt(R.string.key_bookmarks_sort_column, 0),
                                settings.getBoolean(R.string.key_bookmarks_sort_desc, false));
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
