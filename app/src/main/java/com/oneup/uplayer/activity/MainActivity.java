package com.oneup.uplayer.activity;

import android.Manifest;
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
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.fragment.ArtistsFragment;
import com.oneup.uplayer.fragment.ListFragment;
import com.oneup.uplayer.fragment.QueryFragment;
import com.oneup.uplayer.fragment.SongsFragment;

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {
    private static final String TAG = "UPlayer";

    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "MainActivity.onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        viewPager = findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setCurrentItem(3);

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
    public void onTabSelected(TabLayout.Tab tab) {
        Log.d(TAG, "MainActivity.onTabSelected(" + tab.getPosition() + ")");
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        Log.d(TAG, "MainActivity.onTabReselected(" + tab.getPosition() + ")");

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
            Log.d(TAG, "MainActivity.SectionsPagerAdapter.getItem(" + position + ")");
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
                                Song.BOOKMARKED + " DESC," + Song.TITLE,
                                Song.BOOKMARKED + "," + Song.TITLE + " DESC");
                    }
                    return bookmarksFragment;
                case 2:
                    if (lastAddedFragment == null) {
                        lastAddedFragment = ArtistsFragment.newInstance(
                                Artist.LAST_ADDED + " DESC," + Artist.ARTIST,
                                Artist.LAST_ADDED + "," + Artist.ARTIST + " DESC",
                                Song.ADDED + " DESC," + Song.TITLE,
                                Song.ADDED + "," + Song.TITLE + " DESC",
                                ArtistsFragment.INFO_LAST_ADDED);
                    }
                    return lastAddedFragment;
                case 3:
                    if (artistsFragment == null) {
                        artistsFragment = ArtistsFragment.newInstance(
                                Artist.ARTIST,
                                Artist.ARTIST + " DESC",
                                Song.TITLE,
                                Song.TITLE + " DESC",
                                0);
                    }
                    return artistsFragment;
                case 4:
                    if (lastPlayedFragment == null) {
                        lastPlayedFragment = ArtistsFragment.newInstance(
                                Artist.LAST_PLAYED + " DESC",
                                Artist.LAST_PLAYED,
                                Song.LAST_PLAYED + " DESC",
                                Song.LAST_PLAYED,
                                ArtistsFragment.INFO_LAST_PLAYED);
                    }
                    return lastPlayedFragment;
                case 5:
                    if (mostPlayedFragment == null) {
                        mostPlayedFragment = ArtistsFragment.newInstance(
                                Artist.TIMES_PLAYED + " DESC," + Artist.ARTIST,
                                Artist.TIMES_PLAYED + "," + Artist.ARTIST + " DESC",
                                Song.TIMES_PLAYED + " DESC," + Song.TITLE,
                                Song.TIMES_PLAYED + "," + Song.TITLE + " DESC",
                                ArtistsFragment.INFO_TIMES_PLAYED);
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
                    return getString(R.string.last_added);
                case 3:
                    return getString(R.string.artists);
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
