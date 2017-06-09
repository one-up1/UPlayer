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
import android.util.Log;
import android.util.SparseArray;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.fragment.ArtistsFragment;
import com.oneup.uplayer.fragment.SongsFragment;

//TODO: getContext()/getActivity()/getApplicationContext()
//TODO: Statistics like total songs played.
//TODO: SDK version.

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "UPlayer";

    private SparseArray<Artist> artists;

    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            init();
        } else {
            Log.d(TAG, "Requesting permissions");
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            init();
        } else {
            Log.w(TAG, "Permissions not granted");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            finish();
        }
    }

    public void notifyDataSetChanged() {
        Log.d(TAG, "Querying artists");
        artists.clear();
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

        DbOpenHelper dbOpenHelper = new DbOpenHelper(this);
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
                        artist.setLastPlayed(id);
                        artist.setTimesPlayed(c.getInt(2));
                    }
                }
            }
        }

        Log.d(TAG, "Queried " + artists.size() + " artists");

        if (sectionsPagerAdapter != null) {
            sectionsPagerAdapter.notifyDataSetChanged();
        }
    }

    private void init() {
        Log.d(TAG, "MainActivity.init()");
        setContentView(R.layout.activity_main);
        artists = new SparseArray<>();

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setCurrentItem(1);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return SongsFragment.newInstance(artists, 0,
                            Song.STARRED + " IS NOT NULL", Song.STARRED + " DESC");
                case 1:
                    return ArtistsFragment.newInstance(artists,
                            ArtistsFragment.SORT_BY_NAME);
                case 2:
                    return ArtistsFragment.newInstance(artists,
                            ArtistsFragment.SORT_BY_LAST_PLAYED);
                case 3:
                    return ArtistsFragment.newInstance(artists,
                            ArtistsFragment.SORT_BY_TIMES_PLAYED);
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
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.starred);
                case 1:
                    return getString(R.string.artists);
                case 2:
                    return getString(R.string.last_played);
                case 3:
                    return getString(R.string.most_played);
            }
            return null;
        }
    }
}
