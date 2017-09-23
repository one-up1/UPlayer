package com.oneup.uplayer.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.fragment.ArtistsFragment;
import com.oneup.uplayer.fragment.QueryFragment;
import com.oneup.uplayer.fragment.SongsFragment;

//TODO: Improve reversing of sort order.
//TODO: Song/artist count in MainActivity.
//TODO: Remove empty artist directory after deleting song.

public class MainActivity extends AppCompatActivity {
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
        //if (true) return;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setCurrentItem(2);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "MainActivity.onResume()");
        super.onResume();
        //if (true) return;

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            notifyDataSetChanged();
        } else {
            Log.d(TAG, "Requesting permissions");
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reverseSortOrder:
                switch (viewPager.getCurrentItem()) {
                    case 1:
                        ((SongsFragment) sectionsPagerAdapter.getItem(viewPager.getCurrentItem()))
                                .reverseSortOrder();
                        break;
                    case 2:
                    case 3:
                    case 4:
                        ((ArtistsFragment) sectionsPagerAdapter.getItem(viewPager.getCurrentItem()))
                                .reverseSortOrder();
                        break;
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
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

    public void notifyDataSetChanged() {
        Log.d(TAG, "MainActivity.notifyDataSetChanged()");
        if (artists == null) {
            artists = new SparseArray<>();
        } else {
            artists.clear();
        }
        Artist artist;

        try (Cursor c = getContentResolver().query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                new String[]{Artist._ID, Artist.ARTIST}, null, null, null)) {
            if (c == null) {
                Log.wtf(TAG, "No cursor");
                return;
            }

            int iId = c.getColumnIndex(Artist._ID);
            int iArtist = c.getColumnIndex(Artist.ARTIST);
            while (c.moveToNext()) {
                artist = new Artist();
                artist.setId(c.getInt(iId));
                artist.setArtist(c.getString(iArtist));
                artists.put(artist.getId(), artist);
            }
        }

        try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
            try (Cursor c = db.query(Artist.TABLE_NAME,
                    new String[]{Artist._ID, Artist.LAST_PLAYED, Artist.TIMES_PLAYED},
                    null, null, null, null, null)) {
                while (c.moveToNext()) {
                    int id = c.getInt(0);
                    artist = artists.get(id);
                    if (artist == null) {
                        dbOpenHelper.deleteArtist(id);
                    } else {
                        artist.setLastPlayed(c.getLong(1));
                        artist.setTimesPlayed(c.getInt(2));
                    }
                }
            }
        }

        Log.d(TAG, "Queried " + artists.size() + " artists");

        if (sectionsPagerAdapter != null) {
            if (sectionsPagerAdapter.queryFragment != null) {
                sectionsPagerAdapter.queryFragment.setArtists(artists);
            }
            if (sectionsPagerAdapter.songsFragment != null) {
                sectionsPagerAdapter.songsFragment.setArtists(artists);
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
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter {
        private QueryFragment queryFragment;
        private SongsFragment songsFragment;
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
                    if (songsFragment == null) {
                        songsFragment = SongsFragment.newInstance(artists, 0,
                                Song.BOOKMARKED + " IS NOT NULL", Song.BOOKMARKED + " DESC");
                    }
                    return songsFragment;
                case 2:
                    if (artistsFragment == null) {
                        artistsFragment = ArtistsFragment.newInstance(artists,
                                ArtistsFragment.SORT_BY_NAME);
                    }
                    return artistsFragment;
                case 3:
                    if (lastPlayedFragment == null) {
                        lastPlayedFragment = ArtistsFragment.newInstance(artists,
                                ArtistsFragment.SORT_BY_LAST_PLAYED);
                    }
                    return lastPlayedFragment;
                case 4:
                    if (mostPlayedFragment == null) {
                        mostPlayedFragment = ArtistsFragment.newInstance(artists,
                                ArtistsFragment.SORT_BY_TIMES_PLAYED);
                    }
                    return mostPlayedFragment;
            }
            return null;
        }

        @Override
        public int getItemPosition(Object object) {
            // POSITION_NONE makes it possible to reload the PagerAdapter.
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return 5;
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
                    return getString(R.string.last_played);
                case 4:
                    return getString(R.string.most_played);
            }
            return null;
        }
    }
}
