package com.oneup.uplayer.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ListView;
import android.widget.MediaController;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;

public class PlayerActivity extends Activity implements MediaController.MediaPlayerControl {
    private static final String TAG = "UPlayer";

    private ListView lvSongs;

    private MainService mainService;
    private MusicController controller;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        lvSongs = (ListView)findViewById(R.id.lvSongs);

        bindService(new Intent(this, MainService.class), mainConnection, Context.BIND_AUTO_CREATE);


    }

    @Override
    protected void onDestroy() {
        controller.hide();
        super.onDestroy();
    }

    @Override
    public void start() {
        mainService.getPlayer().start();
    }

    @Override
    public void pause() {
        mainService.getPlayer().pause();
    }

    @Override
    public int getDuration() {
        return mainService.getPlayer().getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mainService.getPlayer().getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        mainService.getPlayer().seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return mainService.getPlayer().isPlaying();
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
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    private ServiceConnection mainConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainService.MainBinder binder = (MainService.MainBinder)service;
            mainService = binder.getService();

            controller = new MusicController(PlayerActivity.this);
            controller.setMediaPlayer(PlayerActivity.this);
            controller.setAnchorView(lvSongs);
            controller.setEnabled(true);
            controller.show(0);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
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
