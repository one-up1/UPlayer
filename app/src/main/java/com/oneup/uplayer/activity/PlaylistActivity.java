package com.oneup.uplayer.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.Util;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.widget.SongAdapter;
import com.oneup.uplayer.widget.SongsListView;

public class PlaylistActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,
        SongsListView.OnSongDeletedListener {
    private static final String TAG = "UPlayer";

    private SongsListView slvSongs;
    private SongAdapter songsAdapter;

    private MainService mainService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        slvSongs = new SongsListView(this);
        slvSongs.setOnItemClickListener(this);
        slvSongs.setOnSongDeletedListener(this);
        setContentView(slvSongs);
        registerForContextMenu(slvSongs);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v == slvSongs) {
            getMenuInflater().inflate(R.menu.list_item_song, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return slvSongs.onContextItemSelected(item);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "PlaylistActivity.onStart()");
        super.onStart();

        if (mainService == null) {
            Log.d(TAG, "Binding service");
            bindService(new Intent(this, MainService.class), serviceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "PlaylistActivity.onStop()");
        unbindService(serviceConnection);
        mainService = null;

        super.onStop();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == slvSongs) {
            if (mainService != null) {
                mainService.setSongIndex(position);
            }
        }
    }

    @Override
    public void onSongDeleted(Song song) {
        Log.d(TAG, "PlaylistActivity.onSongDeleted(" + song + ")");
        if (mainService != null) {
            mainService.deleteSong(song);
            setTitle();
            songsAdapter.notifyDataSetChanged();
        }
    }

    private void setTitle() {
        setTitle(getString(R.string.song_count_duration, mainService.getSongs().size(),
                Util.formatDuration(Song.getDuration(mainService.getSongs(), 0))));
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "PlaylistActivity.serviceConnection.onServiceConnected()");
            if (isFinishing()) {
                Log.d(TAG, "Finishing");
                return;
            }

            MainService.MainBinder binder = (MainService.MainBinder) service;
            mainService = binder.getService();

            setTitle();
            songsAdapter = new ListAdapter();
            slvSongs.setAdapter(songsAdapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "PlaylistActivity.serviceConnection.onServiceDisconnected()");
            mainService = null;
        }
    };

    private class ListAdapter extends SongAdapter implements View.OnClickListener {
        private ListAdapter() {
            super(PlaylistActivity.this, mainService.getSongs());
        }

        @Override
        public void addButtons(LinearLayout llButtons) {
            ImageButton ibDelete = new ImageButton(PlaylistActivity.this);
            ibDelete.setId(R.id.ibDelete);
            ibDelete.setImageResource(R.drawable.ic_delete);
            ibDelete.setContentDescription(getString(R.string.delete));
            ibDelete.setOnClickListener(this);
            llButtons.addView(ibDelete);
        }

        @Override
        public void setButtons(View view, Song song) {
            ImageButton ibDelete = (ImageButton) view.findViewById(R.id.ibDelete);
            ibDelete.setTag(song);
        }

        @Override
        public void onClick(View v) {
            Song song = (Song) v.getTag();
            switch (v.getId()) {
                case R.id.ibDelete:
                    onSongDeleted(song);
                    break;
            }
        }
    }
}
