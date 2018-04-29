package com.oneup.uplayer.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.fragment.ArtistsFragment;
import com.oneup.uplayer.fragment.QueryFragment;
import com.oneup.uplayer.fragment.SongsFragment;

public class MainActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener {
    private static final String TAG = "UPlayer";

    private DbOpenHelper dbOpenHelper;
    private SparseArray<Artist> artists;

    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "MainActivity.onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbOpenHelper = new DbOpenHelper(this);
        //dbOpenHelper.t(this);

        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "MainActivity.onResume()");
        super.onResume();
        //if (true) return;

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            if (sectionsPagerAdapter == null) {
                queryArtists();

                //Toolbar toolbar = findViewById(R.id.toolbar);
                //setSupportActionBar(toolbar);

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
            } else {
                notifyDataSetChanged();
            }
        } else {
            Log.d(TAG, "Requesting permissions");
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "MainActivity.onDestroy()");
        if (dbOpenHelper != null) {
            dbOpenHelper.close();
        }

        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "MainActivity.onRequestPermissionsResult(" +
                requestCode + ", " + permissions[0] + ", " + grantResults[0] + ")");
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Permissions not granted");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "MainActivity.onActivityResult(" + requestCode + "," + resultCode + ")");
        Fragment currentItem = sectionsPagerAdapter.getItem(viewPager.getCurrentItem());
        Log.d(TAG, "Current item: " + viewPager.getCurrentItem() + ":" +
                currentItem.getClass().getSimpleName());
        if (currentItem instanceof SongsFragment) {
            currentItem.onActivityResult(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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

    public void notifyDataSetChanged() {
        Log.d(TAG, "MainActivity.notifyDataSetChanged()");
        queryArtists();

        if (sectionsPagerAdapter.queryFragment != null) {
            sectionsPagerAdapter.queryFragment.setArtists(artists);
        }
        if (sectionsPagerAdapter.bookmarksFragment != null) {
            sectionsPagerAdapter.bookmarksFragment.setArtists(artists);
        }
        if (sectionsPagerAdapter.lastAddedFragment != null) {
            sectionsPagerAdapter.lastAddedFragment.setArtists(artists);
        }
        if (sectionsPagerAdapter.artistsFragment != null) {
            sectionsPagerAdapter.artistsFragment.setArtists(artists);
        }
        if (sectionsPagerAdapter.lastPlayedFragment != null) {
            sectionsPagerAdapter.lastPlayedFragment.setArtists(artists);
        }
        if (sectionsPagerAdapter.mostPlayedFragment != null) {
            sectionsPagerAdapter.mostPlayedFragment.setArtists(artists);
        }
        sectionsPagerAdapter.notifyDataSetChanged();
    }

    private void queryArtists() {
        if (artists == null) {
            artists = new SparseArray<>();
        } else {
            artists.clear();
        }
        Artist artist;

        try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
            try (Cursor c = db.query(Artist.TABLE_NAME,//TODO: Sort artist.
                    null, null, null, null, null, null)) {
                while (c.moveToNext()) {
                    artist = new Artist();
                    int id = c.getInt(0);
                    artist.setId(id);
                    artist.setArtist(c.getString(1));
                    artist.setDateModified(c.getLong(2));
                    artist.setLastPlayed(c.getLong(3));
                    artist.setTimesPlayed(c.getInt(4));
                    artists.put(id, artist);
                }
            }
        }

        Log.d(TAG, "Queried " + artists.size() + " artists");
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
                        queryFragment = QueryFragment.newInstance(artists);
                    }
                    return queryFragment;
                case 1:
                    if (bookmarksFragment == null) {
                        bookmarksFragment = SongsFragment.newInstance(artists, null,
                                Song.BOOKMARKED + " IS NOT NULL", Song.BOOKMARKED + " DESC");
                    }
                    return bookmarksFragment;
                case 2:
                    if (lastAddedFragment == null) {
                        lastAddedFragment = ArtistsFragment.newInstance(artists);
                    }
                    return lastAddedFragment;
                case 3:
                    if (artistsFragment == null) {
                        artistsFragment = ArtistsFragment.newInstance(artists);
                    }
                    return artistsFragment;
                case 4:
                    if (lastPlayedFragment == null) {
                        lastPlayedFragment = ArtistsFragment.newInstance(artists);
                    }
                    return lastPlayedFragment;
                case 5:
                    if (mostPlayedFragment == null) {
                        mostPlayedFragment = ArtistsFragment.newInstance(artists);
                    }
                    return mostPlayedFragment;
            }
            return null;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            // POSITION_NONE makes it possible to reload the PagerAdapter.
            return POSITION_NONE;
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
            }
            return null;
        }
    }
}
