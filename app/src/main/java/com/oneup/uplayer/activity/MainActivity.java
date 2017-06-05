package com.oneup.uplayer.activity;

import android.Manifest;
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
import com.oneup.uplayer.db.obj.Artist;
import com.oneup.uplayer.db.obj.Song;
import com.oneup.uplayer.fragment.ArtistsFragment;
import com.oneup.uplayer.fragment.PlaylistsFragment;
import com.oneup.uplayer.fragment.SongsFragment;

//TODO: Recently added.

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "UPlayer";

    private SectionsPagerAdapter sectionsPagerAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            init();
        } else {
            Log.d(TAG, "Requesting permissions");
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
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
        sectionsPagerAdapter.notifyDataSetChanged();
    }

    private void init() {
        Log.d(TAG, "MainActivity.init()");
        setContentView(R.layout.activity_main);

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
            switch (position) {
                case 0:
                    return PlaylistsFragment.newInstance();
                case 1:
                    return SongsFragment.newInstance(SongsFragment.SOURCE_DB, null, Song._ID,
                            Song.STARRED + " IS NOT NULL", null, Song.STARRED + " DESC");
                case 2:
                    return ArtistsFragment.newInstance(ArtistsFragment.SOURCE_ANDROID,
                            Artist.ARTIST, Song.TITLE);
                case 3:
                    return ArtistsFragment.newInstance(ArtistsFragment.SOURCE_DB,
                            Artist.LAST_PLAYED + " DESC", Song.LAST_PLAYED + " DESC");
                case 4:
                    return ArtistsFragment.newInstance(ArtistsFragment.SOURCE_DB,
                            Artist.TIMES_PLAYED + " DESC", Song.TIMES_PLAYED + " DESC");
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
