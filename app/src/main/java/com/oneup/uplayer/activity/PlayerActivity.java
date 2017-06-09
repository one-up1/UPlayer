package com.oneup.uplayer.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.SongAdapter;
import com.oneup.uplayer.SongsListView;
import com.oneup.uplayer.db.Song;

import java.util.ArrayList;

//FIXME: Media controls. Controller not updated when going to next song, occurs when seeking is used?

public class PlayerActivity extends Activity implements
        AdapterView.OnItemClickListener, MediaController.MediaPlayerControl {
    private static final String TAG = "UPlayer";

    private SongsListView slvSongs;
    private SongAdapter songsAdapter;

    private MusicController controller;
    private MainService mainService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        slvSongs = (SongsListView) findViewById(R.id.slvSongs);
        slvSongs.setOnItemClickListener(this);
        registerForContextMenu(slvSongs);

        controller = new MusicController();
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainService != null) {
                    mainService.next();
                }
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainService != null) {
                    mainService.previous();
                }
            }
        });
        controller.setMediaPlayer(PlayerActivity.this);
        controller.setAnchorView(slvSongs);
        controller.setEnabled(true);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.list_item_song, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return slvSongs.onContextItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mainService == null) {
            Log.d(TAG, "Binding service");
            bindService(new Intent(this, MainService.class), serviceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onAttachedToWindow() {
        Log.d(TAG, "onAttachedToWindow()");
        super.onAttachedToWindow();
        controller.show();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Unbinding service");
        controller.setEnabled(false);
        //controller.hide();
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
    public void start() {
        if (mainService != null) {
            mainService.getPlayer().start();
        }
    }

    @Override
    public void pause() {
        if (mainService != null) {
            mainService.getPlayer().pause();
        }
    }

    @Override
    public int getDuration() {
        return mainService == null ? 0 : mainService.getPlayer().getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mainService == null ? 0 : mainService.getPlayer().getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        if (mainService != null) {
            mainService.getPlayer().seekTo(pos);
        }
    }

    @Override
    public boolean isPlaying() {
        return mainService != null && mainService.getPlayer().isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            if (isFinishing()) {
                Log.d(TAG, "finishing");
                return;
            }

            try {
                MainService.MainBinder binder = (MainService.MainBinder) service;
                mainService = binder.getService();

                songsAdapter = new ListAdapter(PlayerActivity.this, mainService.getSongs());
                slvSongs.setAdapter(songsAdapter);
            } catch (Exception ex) {
                Log.e(TAG, "Err", ex);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service diconnected");
            //controller.hide();
            controller.setEnabled(false);
            mainService = null;
        }
    };

    private class ListAdapter extends SongAdapter implements View.OnClickListener {
        private ListAdapter(Context context, ArrayList<Song> songs) {
            super(context, songs);
        }

        @Override
        public void addButtons(LinearLayout llButtons) {
            ImageButton ibDelete = new ImageButton(PlayerActivity.this);
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
                    Log.d(TAG, "Deleting: " + song);
                    mainService.deleteSong(song);
                    songsAdapter.notifyDataSetChanged();
                    break;
            }
        }
    }

    private class MusicController extends MediaController {
        public MusicController() {
            super(PlayerActivity.this);
        }

        @Override
        public void show() {
            super.show(0);
        }

        @Override
        public void show(int timeout) {
            super.show(0);
        }

        @Override
        public void hide() {
            super.hide();
            finish();
        }
    }
}
