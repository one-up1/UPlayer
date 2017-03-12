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
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;

public class PlayerActivity extends Activity implements MediaController.MediaPlayerControl {
    private static final String TAG = "UPlayer";

    private ListView lvSongs;

    private MainService mainService;
    private MusicController controller;
    private boolean paused;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        lvSongs = (ListView)findViewById(R.id.lvSongs);
        setController();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        controller.show(0);
    }

    @Override
    protected void onStart() {
        super.onStart();

        bindService(new Intent(this, MainService.class), mainConnection, Context.BIND_AUTO_CREATE);

        setController();
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
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unbindService(mainConnection);
        mainConnection = null;
        super.onDestroy();
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
        if (mainService != null) {
            return mainService.getPlayer().getDuration();
        } else {
            return 0;
        }
    }

    @Override
    public int getCurrentPosition() {
        if (mainService != null) {
            return mainService.getPlayer().getCurrentPosition();
        } else {
            return 0;
        }
    }

    @Override
    public void seekTo(int pos) {
        if (mainService != null) {
            mainService.getPlayer().seekTo(pos);
        }
    }

    @Override
    public boolean isPlaying() {
        if (mainService != null) {
            return mainService.getPlayer().isPlaying();
        } else {
            return false;
        }
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

    private ServiceConnection mainConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MainService.MainBinder binder = (MainService.MainBinder)service;
            mainService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
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
