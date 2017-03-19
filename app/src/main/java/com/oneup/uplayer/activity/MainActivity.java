package com.oneup.uplayer.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.obj.Artist;
import com.oneup.uplayer.obj.Playlist;

import java.util.ArrayList;

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
            requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 0);
        }
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

    private void init() {
        Log.d(TAG, "MainActivity.init()");
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(sectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return ArtistsFragment.newInstance();
                case 1:
                    return PlaylistsFragment.newInstance();
                case 2:
                    return YearsFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.artists);
                case 1:
                    return getString(R.string.playlists);
                case 2:
                    return getString(R.string.years);
            }
            return null;
        }
    }

    public static class ArtistsFragment extends Fragment
            implements AdapterView.OnItemClickListener {
        private ArrayList<Artist> artists;

        public ArtistsFragment() {
        }

        public static ArtistsFragment newInstance() {
            return new ArtistsFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View ret = inflater.inflate(R.layout.fragment_artists, container, false);
            ListView lvArtists = (ListView)ret.findViewById(R.id.lvArtists);
            lvArtists.setOnItemClickListener(this);

            Cursor c = getActivity().getContentResolver().query(
                    MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                    new String[] {
                            MediaStore.Audio.Artists._ID,
                            MediaStore.Audio.Artists.ARTIST },
                    null, null, null);
            if (c != null) {
                try {
                    artists = new ArrayList<>();
                    int iId = c.getColumnIndex(MediaStore.Audio.Artists._ID);
                    int iArtist = c.getColumnIndex(MediaStore.Audio.Artists.ARTIST);
                    while (c.moveToNext()) {
                        artists.add(new Artist(c.getLong(iId), c.getString(iArtist)));
                    }
                } finally {
                    c.close();
                }

                Log.d(TAG, "Queried " + artists.size() + " artists");
                lvArtists.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_list_item_1, artists));
            }

            return ret;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            startActivity(new Intent(getContext(), SongsActivity.class)
                    .putExtra(SongsActivity.ARG_URI,
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                    .putExtra(SongsActivity.ARG_ID_COLUMN,
                            MediaStore.Audio.Media._ID)
                    .putExtra(SongsActivity.ARG_TITLE_COLUMN,
                            MediaStore.Audio.Media.TITLE)
                    .putExtra(SongsActivity.ARG_ARTIST_COLUMN,
                            MediaStore.Audio.Media.ARTIST)
                    .putExtra(SongsActivity.ARG_YEAR_COLUMN,
                            MediaStore.Audio.Media.YEAR)
                    .putExtra(SongsActivity.ARG_SELECTION,
                            MediaStore.Audio.Media.ARTIST_ID + "=?")
                    .putExtra(SongsActivity.ARG_SELECTION_ARGS,
                            new String[] { Long.toString(artists.get(position).getId()) }));
        }
    }

    public static class PlaylistsFragment extends Fragment
            implements AdapterView.OnItemClickListener {
        private ArrayList<Playlist> playlists;

        public PlaylistsFragment() {
        }

        public static PlaylistsFragment newInstance() {
            return new PlaylistsFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View ret = inflater.inflate(R.layout.fragment_playlists, container, false);
            ListView lvPlaylists = (ListView)ret.findViewById(R.id.lvPlaylists);
            lvPlaylists.setOnItemClickListener(this);

            Cursor c = getActivity().getContentResolver().query(
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    new String[] {
                            MediaStore.Audio.Playlists._ID,
                            MediaStore.Audio.Playlists.NAME
                    },
                    null, null, null);
            if (c != null) {
                try {
                    playlists = new ArrayList<>();
                    int iId = c.getColumnIndex(MediaStore.Audio.Playlists._ID);
                    int iName = c.getColumnIndex(MediaStore.Audio.Playlists.NAME);
                    while (c.moveToNext()) {
                        playlists.add(new Playlist(c.getLong(iId), c.getString(iName)));
                    }
                } finally {
                    c.close();
                }

                Log.d(TAG, "Queried " + playlists.size() + " playlists");
                lvPlaylists.setAdapter(new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_list_item_1, playlists));
            }

            return ret;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            startActivity(new Intent(getContext(), SongsActivity.class)
                    .putExtra(SongsActivity.ARG_URI,
                            MediaStore.Audio.Playlists.Members.getContentUri("external",
                                    playlists.get(position).getId()))
                    .putExtra(SongsActivity.ARG_ID_COLUMN,
                            MediaStore.Audio.Playlists.Members.AUDIO_ID)
                    .putExtra(SongsActivity.ARG_TITLE_COLUMN,
                            MediaStore.Audio.Playlists.Members.TITLE)
                    .putExtra(SongsActivity.ARG_ARTIST_COLUMN,
                            MediaStore.Audio.Playlists.Members.ARTIST)
                    .putExtra(SongsActivity.ARG_YEAR_COLUMN,
                            MediaStore.Audio.Playlists.Members.YEAR));
        }
    }

    public static class YearsFragment extends Fragment
            implements AdapterView.OnItemClickListener {
        public YearsFragment() {
        }

        public static YearsFragment newInstance() {
            return new YearsFragment();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View ret = inflater.inflate(R.layout.fragment_years, container, false);
            ListView lvYears = (ListView)ret.findViewById(R.id.lvYears);
            lvYears.setOnItemClickListener(this);

            return ret;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String yearSelection;
            switch (position) {
                case 0:
                    yearSelection = ">=2017";
                    break;
                case 1:
                    yearSelection = "=2016";
                    break;
                case 2:
                    yearSelection = "=2015";
                    break;
                case 3:
                    yearSelection = "=2014";
                    break;
                case 4:
                    yearSelection = "=2013";
                    break;
                case 5:
                    yearSelection = "=2012";
                    break;
                case 6:
                    yearSelection = "=2011";
                    break;
                case 7:
                    yearSelection = "=2010";
                    break;
                case 8:
                    yearSelection = ">=2005 AND " + MediaStore.Audio.Media.YEAR + "<=2009";
                    break;
                case 9:
                    yearSelection = "BETWEEN 2000 AND 2004";
                    break;
                case 10:
                    yearSelection = "BETWEEN 1990 AND 1999";
                    break;
                case 11:
                    yearSelection = "<1990";
                    break;
                default:
                    yearSelection = null;
                    break;
            }
            startActivity(new Intent(getContext(), SongsActivity.class)
                    .putExtra(SongsActivity.ARG_URI,
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                    .putExtra(SongsActivity.ARG_ID_COLUMN,
                            MediaStore.Audio.Media._ID)
                    .putExtra(SongsActivity.ARG_TITLE_COLUMN,
                            MediaStore.Audio.Media.TITLE)
                    .putExtra(SongsActivity.ARG_ARTIST_COLUMN,
                            MediaStore.Audio.Media.ARTIST)
                    .putExtra(SongsActivity.ARG_YEAR_COLUMN,
                            MediaStore.Audio.Media.YEAR)
                    .putExtra(SongsActivity.ARG_SELECTION,
                            MediaStore.Audio.Media.YEAR + " " + yearSelection));
        }
    }
}
