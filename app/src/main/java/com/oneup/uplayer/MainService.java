package com.oneup.uplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.oneup.uplayer.obj.Song;

import java.util.ArrayList;

public class MainService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    public static final String ARG_REQUEST_CODE = "request_code";
    public static final String ARG_SONGS = "songs";
    public static final String ARG_SONG_INDEX = "song_index";

    public static final int REQUEST_START = 1;
    public static final int REQUEST_PREVIOUS = 2;
    public static final int REQUEST_PAUSE_PLAY = 3;
    public static final int REQUEST_NEXT = 4;
    public static final int REQUEST_STOP = 5;

    private static final String TAG = "UPlayer";

    private MediaPlayer player;
    private RemoteViews notificationViews;
    private Notification notification;

    private ArrayList<Song> songs;
    private int songIndex;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "MainService.onCreate()");
        super.onCreate();

        player = new MediaPlayer();
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.setOnCompletionListener(this);

        notificationViews = new RemoteViews(getApplicationContext().getPackageName(),
                R.layout.notification);
        setOnClickPendingIntent(notificationViews, R.id.ibPrevious, REQUEST_PREVIOUS);
        setOnClickPendingIntent(notificationViews, R.id.ibPausePlay, REQUEST_PAUSE_PLAY);
        setOnClickPendingIntent(notificationViews, R.id.ibNext, REQUEST_NEXT);
        setOnClickPendingIntent(notificationViews, R.id.ibStop, REQUEST_STOP);

        notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCustomContentView(notificationViews)
                .setCustomBigContentView(notificationViews)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int requestCode = intent.getIntExtra(ARG_REQUEST_CODE, 0);
        Log.d(TAG, "MainService.onStartCommand(), requestCode=" + requestCode);

        switch (requestCode) {
            case REQUEST_START:
                songs = intent.getParcelableArrayListExtra(ARG_SONGS);
                songIndex = intent.getIntExtra(ARG_SONG_INDEX, 0);
                play();
                break;
            case REQUEST_PREVIOUS:
                previous();
                break;
            case REQUEST_PAUSE_PLAY:
                pausePlay();
                break;
            case REQUEST_NEXT:
                next();
                break;
            case REQUEST_STOP:
                stopSelf();
                break;
            default:
                Log.w(TAG, "Invalid request");
                break;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MainService.onDestroy()");
        super.onDestroy();

        if (player != null) {
            player.stop();
            player.release();
            Log.d(TAG, "MediaPlayer released");
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "MainService.onPrepared()");
        mp.start();

        Song song = songs.get(songIndex);
        notificationViews.setTextViewText(R.id.tvSongTitle, song.getTitle());
        notificationViews.setTextViewText(R.id.tvSongArtist, song.getArtist());
        notificationViews.setTextViewText(R.id.tvSong, getString(R.string.song,
                songIndex + 1, songs.size()));
        startForeground(1, notification);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "MainService.onError(), what=" + what + ", extra=" + extra);
        player.reset();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "MainService.onCompletion()");
        if (player.getCurrentPosition() > 0) {
            next();
        }
    }

    private void play() {
        Log.d(TAG, "MainService.play(), " + songs.size() + " songs, songIndex=" + songIndex);
        player.reset();

        try {
            player.setDataSource(getApplicationContext(), ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songs.get(songIndex).getId()));
            player.prepareAsync();
        } catch (Exception ex) {
            Log.e(TAG, "Error setting data source", ex);
        }
    }

    private void previous() {
        Log.d(TAG, "MainService.previous()");
        if (songIndex > 0) {
            songIndex--;
            play();
        }
    }

    private void pausePlay() {
        Log.d(TAG, "MainService.pausePlay()");
        int ibSrcId;
        if (player.isPlaying()) {
            player.pause();
            ibSrcId = R.drawable.ic_play;
        } else {
            player.start();
            ibSrcId = R.drawable.ic_pause;
        }
        notificationViews.setImageViewResource(R.id.ibPausePlay, ibSrcId);
        startForeground(1, notification);
    }

    private void next() {
        Log.d(TAG, "MainService.next()");
        if (songIndex < songs.size() - 1) {
            songIndex++;
            play();
        }
    }

    private void setOnClickPendingIntent(RemoteViews views, int viewId, int requestCode) {
        views.setOnClickPendingIntent(viewId, PendingIntent.getService(this, requestCode,
                new Intent(this, MainService.class)
                        .putExtra(ARG_REQUEST_CODE, requestCode),
                PendingIntent.FLAG_UPDATE_CURRENT));
    }
}
