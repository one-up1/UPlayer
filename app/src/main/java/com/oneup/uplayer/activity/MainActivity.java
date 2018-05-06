package com.oneup.uplayer.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.oneup.uplayer.fragment.QueryFragment;
import com.oneup.uplayer.fragment.SongsFragment;

//TODO: Setting title from fragments.
//TODO: When/how to reload data from database and recreate/reload fragments.
//TODO: Extra and pref key naming.

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {
    private static final String TAG = "UPlayer";

    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "MainActivity.onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setCurrentItem(3);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.addOnTabSelectedListener(this);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting permissions");
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        switch (tab.getPosition()) {
            case 1:
                ((SongsFragment) sectionsPagerAdapter.getItem(viewPager.getCurrentItem()))
                        .reverseSortOrder();
                break;
            case 2:
            case 3:
            case 4:
            case 5:
                ((ArtistsFragment) sectionsPagerAdapter.getItem(viewPager.getCurrentItem()))
                        .reverseSortOrder();
                break;
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
                        //TODO: Recreate fragments everytime?
                    }
                    return queryFragment;
                case 1:
                    if (bookmarksFragment == null) {
                        bookmarksFragment = SongsFragment.newInstance(
                                Song.BOOKMARKED + " IS NOT NULL", null,
                                Song.BOOKMARKED + " DESC," + Song.TITLE);
                    }
                    return bookmarksFragment;
                case 2:
                    if (lastAddedFragment == null) {
                        lastAddedFragment = ArtistsFragment.newInstance(
                                Artist.LAST_SONG_ADDED + " DESC," + Artist.ARTIST,
                                Song.ADDED + " DESC," + Song.TITLE,
                                ArtistsFragment.INFO_LAST_SONG_ADDED);
                    }
                    return lastAddedFragment;
                case 3:
                    if (artistsFragment == null) {
                        artistsFragment = ArtistsFragment.newInstance(
                                Artist.ARTIST, Song.TITLE, 0);
                    }
                    return artistsFragment;
                case 4:
                    if (lastPlayedFragment == null) {
                        lastPlayedFragment = ArtistsFragment.newInstance(
                                Artist.LAST_PLAYED + " DESC," + Artist.ARTIST,
                                Song.LAST_PLAYED + " DESC," + Song.TITLE,
                                ArtistsFragment.INFO_LAST_PLAYED);
                    }
                    return lastPlayedFragment;
                case 5:
                    if (mostPlayedFragment == null) {
                        mostPlayedFragment = ArtistsFragment.newInstance(
                                Artist.TIMES_PLAYED + " DESC," + Artist.ARTIST,
                                Song.TIMES_PLAYED + " DESC," + Song.TITLE,
                                ArtistsFragment.INFO_TIMES_PLAYED);
                    }
                    return mostPlayedFragment;
            }
            return null;
        }

        /*@Override
        public int getItemPosition(@NonNull Object object) {
            // POSITION_NONE makes it possible to reload the PagerAdapter.
            return POSITION_NONE;
        }*/

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
            }
            return null;
        }
    }
}
