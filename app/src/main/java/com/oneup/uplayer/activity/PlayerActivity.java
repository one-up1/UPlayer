package com.oneup.uplayer.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.MediaController;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.SongAdapter;
import com.oneup.uplayer.obj.Song;

public class PlayerActivity extends Activity implements
        AdapterView.OnItemClickListener, MediaController.MediaPlayerControl {
    private static final String TAG = "UPlayer";

    private ListView lvSongs;
    private SongAdapter songsAdapter;

    private MainService mainService;
    private MusicController controller;
    private boolean paused;
    private boolean controllerShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        lvSongs = (ListView)findViewById(R.id.lvSongs);
        lvSongs.setOnItemClickListener(this);

        setController();
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
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (paused) {
            setController();
            paused = false;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!controllerShown) {
            Log.d(TAG, "Showing controller");
            controller.show(0);
            controllerShown = true;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "Unbinding service");
        unbindService(serviceConnection);
        mainService = null;

        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mainService != null) {
            mainService.setSongIndex(position);
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

    private void setController() {
        controller = new MusicController(PlayerActivity.this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainService != null) {
                    mainService.previous();
                }
                controller.show(0);
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainService != null) {
                    mainService.next();
                }
                controller.show(0);
            }
        });
        controller.setMediaPlayer(PlayerActivity.this);
        controller.setAnchorView(lvSongs);
        controller.setEnabled(true);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            MainService.MainBinder binder = (MainService.MainBinder)service;
            mainService = binder.getService();

            PlayerActivity.this.songsAdapter = new SongAdapter(PlayerActivity.this,
                    mainService.getSongs(), false, new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (mainService.getSongs().size() > 1) {
                                Song song = (Song) v.getTag();
                                Log.d(TAG, "Deleting: " + song);
                                mainService.deleteSong(song);
                                PlayerActivity.this.songsAdapter.notifyDataSetChanged();
                            } else {
                                Log.d(TAG, "Not deleting last song");
                            }
                        }
                    }
            );
            lvSongs.setAdapter(PlayerActivity.this.songsAdapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service diconnected");
            mainService = null;
        }
    };

    private class MusicController extends MediaController {
        public MusicController(Context context) {
            super(context);
        }

        @Override
        public void hide() {
            Log.d(TAG, "MusicController.hide()");
            finish();
        }
    }
}
