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
import android.util.LongSparseArray;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.obj.Artist;
import com.oneup.uplayer.db.obj.Song;
import com.oneup.uplayer.fragment.ArtistsFragment;
import com.oneup.uplayer.fragment.PlaylistsFragment;
import com.oneup.uplayer.fragment.SongsFragment;

import java.util.ArrayList;

//TODO: Delete option.
//TODO: Statistics like total songs played.
//TODO: notifyDataSet not in onResume

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "UPlayer";

    private ArrayList<Artist> artists;

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
        if (sectionsPagerAdapter != null) {
            sectionsPagerAdapter.notifyDataSetChanged();
        }
    }

    private void init() {
        Log.d(TAG, "MainActivity.init()");
        setContentView(R.layout.activity_main);

        // Query artists from MediaStore.
        LongSparseArray<Artist> artists = new LongSparseArray<>();
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
                artist.setId(c.getLong(iId));
                artist.setArtist(c.getString(iArtist));
                artists.put(artist.getId(), artist);
            }
        }

        // Query artists from DB and set values.
        DbOpenHelper dbOpenHelper = new DbOpenHelper(this);
        try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
            try (Cursor c = db.query(Artist.TABLE_NAME,
                    new String[]{Artist._ID, Artist.LAST_PLAYED, Artist.TIMES_PLAYED},
                    null, null, null, null, null)) {
                while (c.moveToNext()) {
                    artist = artists.get(c.getLong(0));
                    if (artist == null) {
                        Log.i(TAG, "Deleting artist");
                        //TODO: Delete artist from DB. and all songs?
                    } else {
                        artist.setLastPlayed(c.getLong(1));
                        artist.setTimesPlayed(c.getInt(2));
                    }
                }
            }
        }

        // Convert SparseArray to ArrayList.
        this.artists = new ArrayList<>();
        for (int i = 0; i < artists.size(); i++) {
            this.artists.add(artists.valueAt(i));
        }
        Log.d(TAG, "Queried " + this.artists.size() + " artists");

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

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // Copy list to prevent fragments from keeping a reference to the original list.
            ArrayList<Artist> artists = new ArrayList<>();
            for (Artist artist : MainActivity.this.artists) {
                artists.add(artist);
            }

            switch (position) {
                case 0:
                    return PlaylistsFragment.newInstance();
                case 1:
                    /*return SongsFragment.newInstance(null, Song._ID, Song.STARRED + " IS NOT NULL",
                            null, 0);*/
                    return SongsFragment.newInstance(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            Song._ID, Song.STARRED + " IS NOT NULL", null, Song.STARRED + " DESC",
                            SongsFragment.SORT_BY_STARRED);
                case 2:
                    return ArtistsFragment.newInstance(artists,
                            ArtistsFragment.SORT_BY_NAME);
                case 3:
                    return ArtistsFragment.newInstance(artists,
                            ArtistsFragment.SORT_BY_LAST_PLAYED);
                case 4:
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
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.playlists);
                case 1:
                    return getString(R.string.starred);
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
