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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.obj.Artist;
import com.oneup.uplayer.db.obj.Playlist;
import com.oneup.uplayer.db.obj.Song;

import java.util.ArrayList;

//TODO: Favorite.
//TODO: Clean database option to delete songs that don't exist anymore.
//TODO: Recently added.
//TODO: Display total playlist duration.
//FIXME: Error -38 "You seem to try to start the playing before the preparation is complete"

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
                    return StarredFragment.newInstance();
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
            ListView lvPlaylists = (ListView) ret.findViewById(R.id.lvPlaylists);
            lvPlaylists.setOnItemClickListener(this);

            Cursor c = getContext().getContentResolver().query(
                    MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                    new String[]{
                            MediaStore.Audio.Playlists._ID,
                            MediaStore.Audio.Playlists.NAME
                    }, null, null, MediaStore.Audio.Playlists.NAME);
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
                    .putExtra(SongsActivity.ARG_SOURCE, SongsActivity.SOURCE_ANDROID)
                    .putExtra(SongsActivity.ARG_URI, MediaStore.Audio.Playlists.Members
                            .getContentUri("external", playlists.get(position).getId()))
                    .putExtra(SongsActivity.ARG_ID_COLUMN,
                            MediaStore.Audio.Playlists.Members.AUDIO_ID)
                    .putExtra(SongsActivity.ARG_ORDER_BY,
                            MediaStore.Audio.Playlists.Members.PLAY_ORDER));
        }
    }

    public static class StarredFragment extends Fragment {
        public StarredFragment() {
        }

        public static StarredFragment newInstance() {
            StarredFragment fragment = new StarredFragment();
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View ret = inflater.inflate(R.layout.fragment_starred, container, false);
            ListView lvSongs = (ListView) ret.findViewById(R.id.lvSongs);
            return ret;
        }
    }

    public static class ArtistsFragment extends Fragment
            implements AdapterView.OnItemClickListener {
        private static final String ARG_SOURCE = "source";
        private static final String ARG_ORDER_BY = "order_by";
        private static final String ARG_SONGS_ORDER_BY = "songs_order_by";

        private static final int SOURCE_ANDROID = 1;
        private static final int SOURCE_DB = 2;

        private ArrayList<Artist> artists;

        public ArtistsFragment() {
        }

        public static ArtistsFragment newInstance(int source, String orderBy, String songsOrderBy) {
            ArtistsFragment fragment = new ArtistsFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SOURCE, source);
            args.putString(ARG_ORDER_BY, orderBy);
            args.putString(ARG_SONGS_ORDER_BY, songsOrderBy);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View ret = inflater.inflate(R.layout.fragment_artists, container, false);
            ListView lvArtists = (ListView) ret.findViewById(R.id.lvArtists);
            lvArtists.setOnItemClickListener(this);

            DbOpenHelper dbOpenHelper = new DbOpenHelper(getActivity());
            String[] columns = {Artist._ID, Artist.ARTIST};
            Cursor cursor;
            switch (getArguments().getInt(ARG_SOURCE)) {
                case SOURCE_ANDROID:
                    cursor = getContext().getContentResolver().query(
                            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, columns, null, null,
                            getArguments().getString(ARG_ORDER_BY));
                    if (cursor == null) {
                        Log.w(TAG, "No cursor");
                        return ret;
                    }
                    break;
                case SOURCE_DB:
                    cursor = dbOpenHelper.getReadableDatabase().query(Artist.TABLE_NAME, columns,
                            null, null, null, null, getArguments().getString(ARG_ORDER_BY));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid source");
            }

            try {
                artists = new ArrayList<>();
                int iId = cursor.getColumnIndex(Artist._ID);
                int iArtist = cursor.getColumnIndex(Artist.ARTIST);
                while (cursor.moveToNext()) {
                    Artist artist = new Artist();
                    artist.setId(cursor.getLong(iId));
                    artist.setArtist(cursor.getString(iArtist));
                    artists.add(artist);
                }
            } finally {
                cursor.close();
                dbOpenHelper.close();
            }

            Log.d(TAG, "Queried " + artists.size() + " artists");
            lvArtists.setAdapter(new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_list_item_1, artists));

            return ret;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            startActivity(new Intent(getContext(), SongsActivity.class)
                    .putExtra(SongsActivity.ARG_SOURCE, getArguments().getInt(ARG_SOURCE))
                    .putExtra(SongsActivity.ARG_URI, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                    .putExtra(SongsActivity.ARG_ID_COLUMN, Song._ID)
                    .putExtra(SongsActivity.ARG_SELECTION, Song.ARTIST_ID + "=?")
                    .putExtra(SongsActivity.ARG_SELECTION_ARGS,
                            new String[]{Long.toString(artists.get(position).getId())})
                    .putExtra(SongsActivity.ARG_ORDER_BY,
                            getArguments().getString(ARG_SONGS_ORDER_BY)));
        }
    }
}
