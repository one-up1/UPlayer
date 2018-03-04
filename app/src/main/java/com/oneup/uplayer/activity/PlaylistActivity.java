package com.oneup.uplayer.activity;

import android.app.ActionBar;
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
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.widget.SongAdapter;
import com.oneup.uplayer.widget.SongsListView;

public class PlaylistActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,
        SongsListView.OnSongDeletedListener {
    private static final String TAG = "UPlayer";

    private static PlaylistActivity instance;

    private SongsListView listView;
    private SongAdapter songsAdapter;

    private MainService mainService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listView = new SongsListView(this);
        listView.setOnItemClickListener(this);
        listView.setOnSongDeletedListener(this);
        setContentView(listView);
        registerForContextMenu(listView);

        instance = this;
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
    protected void onResume() {
        Log.d(TAG, "PlaylistActivity.onResume()");
        super.onResume();

        if (songsAdapter != null) {
            songsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v == listView) {
            getMenuInflater().inflate(R.menu.list_item_song, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return listView.onContextItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        listView.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "PlaylistActivity.onStop()");
        unbindService(serviceConnection);
        mainService = null;

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "PlaylistActivity.onDestroy()");
        instance = null;
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == listView) {
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
            listView.setAdapter(songsAdapter);
            listView.setSelection(mainService.getSongIndex());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "PlaylistActivity.serviceConnection.onServiceDisconnected()");
            mainService = null;
        }
    };

    private class ListAdapter extends SongAdapter implements View.OnClickListener,
            View.OnLongClickListener {
        private ListAdapter() {
            super(PlaylistActivity.this, mainService.getSongs());
        }

        @Override
        public void addButtons(LinearLayout llButtons) {
            ImageButton ibMove = new ImageButton(PlaylistActivity.this);
            ibMove.setId(R.id.ibMove);
            ibMove.setImageResource(R.drawable.ic_move);
            ibMove.setContentDescription(getString(R.string.move_up_down));
            ibMove.setOnClickListener(this);
            ibMove.setOnLongClickListener(this);
            llButtons.addView(ibMove);

            ImageButton ibDelete = new ImageButton(PlaylistActivity.this);
            ibDelete.setId(R.id.ibDelete);
            ibDelete.setImageResource(R.drawable.ic_delete);
            ibDelete.setContentDescription(getString(R.string.delete));
            ibDelete.setOnClickListener(this);
            llButtons.addView(ibDelete);
        }

        @Override
        public void setButtons(View view, Song song) {
            ImageButton ibMove = view.findViewById(R.id.ibMove);
            ibMove.setTag(song);

            ImageButton ibDelete = view.findViewById(R.id.ibDelete);
            ibDelete.setTag(song);
        }

        @Override
        public void onClick(View v) {
            Song song = (Song) v.getTag();
            switch (v.getId()) {
                case R.id.ibMove:
                    moveSong(song, 1);
                    break;
                case R.id.ibDelete:
                    onSongDeleted(song);
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            Song song = (Song) v.getTag();
            switch (v.getId()) {
                case R.id.ibMove:
                    moveSong(song, -1);
                    return true;
                default:
                    return false;
            }
        }

        private void moveSong(Song song, int i) {
            if (mainService.moveSong(song, i)) {
                songsAdapter.notifyDataSetChanged();
            }
        }
    }

    public static void finishIfRunning() {
        if (instance != null) {
            Log.d(TAG, "Finishing PlaylistActivity");
            instance.finish();
        }
    }
}
