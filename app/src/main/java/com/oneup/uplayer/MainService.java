package com.oneup.uplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.oneup.uplayer.activity.PlayerActivity;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.PlayedSong;
import com.oneup.uplayer.obj.Song;

import java.util.ArrayList;

public class MainService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    public static final String ARG_REQUEST_CODE = "request_code";
    public static final String ARG_SONGS = "songs";
    public static final String ARG_SONG_INDEX = "song_index";
    public static final String ARG_SONG = "song";

    public static final int REQUEST_START = 1;
    public static final int REQUEST_PLAY_NEXT = 2;
    public static final int REQUEST_PLAY_LAST = 3;
    public static final int REQUEST_PREVIOUS = 4;
    public static final int REQUEST_PLAY_PAUSE = 5;
    public static final int REQUEST_NEXT = 6;
    public static final int REQUEST_STOP = 7;

    private static final String TAG = "UPlayer";

    private final IBinder mainBinder = new MainBinder();

    private DbOpenHelper dbOpenHelper;

    private MediaPlayer player;
    private RemoteViews notificationViews;
    private Notification notification;

    private ArrayList<Song> songs;
    private int songIndex;
    private boolean prepared;

    private MainReceiver mainReceiver;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mainBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "MainService.onCreate()");
        super.onCreate();

        dbOpenHelper = new DbOpenHelper(this);

        player = new MediaPlayer();
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.setOnCompletionListener(this);

        notificationViews = new RemoteViews(getApplicationContext().getPackageName(),
                R.layout.notification);
        setOnClickPendingIntent(notificationViews, R.id.ibPrevious, REQUEST_PREVIOUS);
        setOnClickPendingIntent(notificationViews, R.id.ibPlayPause, REQUEST_PLAY_PAUSE);
        setOnClickPendingIntent(notificationViews, R.id.ibNext, REQUEST_NEXT);
        setOnClickPendingIntent(notificationViews, R.id.ibStop, REQUEST_STOP);

        notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCustomContentView(notificationViews)
                .setCustomBigContentView(notificationViews)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, PlayerActivity.class)
                                .putExtra(ARG_SONGS, songs)
                                .putExtra(ARG_SONG_INDEX, songIndex),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .build();

        mainReceiver = new MainReceiver();
        registerReceiver(mainReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MainService.onStartCommand()");
        if (intent == null) {
            Log.wtf(TAG, "No intent");
            return START_STICKY;
        }

        int requestCode = intent.getIntExtra(ARG_REQUEST_CODE, 0);
        Log.d(TAG, "requestCode=" + requestCode);
        
        switch (requestCode) {
            case REQUEST_START:
                songs = intent.getParcelableArrayListExtra(ARG_SONGS);
                songIndex = intent.getIntExtra(ARG_SONG_INDEX, 0);
                play();
                break;
            case REQUEST_PLAY_NEXT:
                addSong((Song)intent.getParcelableExtra(ARG_SONG), true);
                break;
            case REQUEST_PLAY_LAST:
                addSong((Song)intent.getParcelableExtra(ARG_SONG), false);
                break;
            case REQUEST_PREVIOUS:
                previous();
                break;
            case REQUEST_PLAY_PAUSE:
                playPause();
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

        if (mainReceiver != null) {
            unregisterReceiver(mainReceiver);
        }

        if (dbOpenHelper != null) {
            dbOpenHelper.close();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "MainService.onPrepared()");
        prepared = true;
        player.start();

        Song song = songs.get(songIndex);
        notificationViews.setTextViewText(R.id.tvSongTitle, song.getTitle());
        notificationViews.setTextViewText(R.id.tvSongArtist, song.getArtist());
        notificationViews.setTextViewText(R.id.tvSong, getString(R.string.song,
                songIndex + 1, songs.size()));
        notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_pause);
        startForeground(1, notification);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "MainService.onError(), what=" + what + ", extra=" + extra);
        player.reset();

        notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_play);
        startForeground(1, notification);
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "MainService.onCompletion()");
        notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_play);
        startForeground(1, notification);

        if (player.getCurrentPosition() > 0) {
            next();
        }
    }

    public void addSong(Song song, boolean next) {
        Log.d(TAG, "MainService.addSong(), song=" + song + ", next=" + next);
        if (songs == null) {
            songs = new ArrayList<>();
            songs.add(song);

            notificationViews.setTextViewText(R.id.tvSongTitle, song.getTitle());
            notificationViews.setTextViewText(R.id.tvSongArtist, song.getArtist());
            notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_play);
        } else if (next) {
            songs.add(songIndex + 1, song);
        } else {
            songs.add(song);
        }

        notificationViews.setTextViewText(R.id.tvSong, getString(R.string.song,
                songIndex + 1, songs.size()));
        startForeground(1, notification);
    }

    public void previous() {
        Log.d(TAG, "MainService.previous()");
        if (songIndex > 0) {
            songIndex--;
            play();
        }
    }

    public void next() {
        Log.d(TAG, "MainService.next()");
        if (songIndex < songs.size() - 1) {
            songIndex++;
            play();
        }
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public void setSongIndex(int songIndex) {
        Log.d(TAG, "MainService.setSongIndex(), songIndex=" + songIndex);
        this.songIndex = songIndex;
        play();
    }

    public void deleteSong(Song song) {
        int songIndex = songs.indexOf(song);
        Log.d(TAG, "MainService.deleteSong(), song=" + songIndex + ":" + song +
                ", current=" + this.songIndex + ":" + songs.get(this.songIndex));
        songs.remove(song);

        if (songIndex < this.songIndex) {
            this.songIndex--;
        } else if (songIndex == this.songIndex) {
            play();
        }

        notificationViews.setTextViewText(R.id.tvSong, getString(R.string.song,
                songIndex + 1, songs.size()));
        startForeground(1, notification);
    }

    private void setOnClickPendingIntent(RemoteViews views, int viewId, int requestCode) {
        views.setOnClickPendingIntent(viewId, PendingIntent.getService(this, requestCode,
                new Intent(this, MainService.class)
                        .putExtra(ARG_REQUEST_CODE, requestCode),
                PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private void play() {
        Log.d(TAG, "MainService.play(), " + songs.size() + " songs, songIndex=" + songIndex);
        player.reset();
        try {
            long mediaId = songs.get(songIndex).getId();
            player.setDataSource(getApplicationContext(), ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId));
            player.prepareAsync();

            PlayedSong playedSong = dbOpenHelper.queryPlayedSong(mediaId);
            if (playedSong == null) {
                playedSong = new PlayedSong(mediaId);
                dbOpenHelper.insertPlayedSong(playedSong);
            } else {
                playedSong.setLastPlayed(System.currentTimeMillis());
                playedSong.setTimesPlayed(playedSong.getTimesPlayed() + 1);
                dbOpenHelper.updatePlayedSong(playedSong);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error setting data source", ex);
        }
    }

    private void playPause() {
        Log.d(TAG, "MainService.playPause()");
        int ibSrcId;
        if (player.isPlaying()) {
            Log.d(TAG, "Pausing");
            player.pause();
            ibSrcId = R.drawable.ic_play;
        } else {
            if (prepared) {
                Log.d(TAG, "Resuming");
                player.start();
            } else {
                play();
            }
            ibSrcId = R.drawable.ic_pause;
        }
        notificationViews.setImageViewResource(R.id.ibPlayPause, ibSrcId);
        startForeground(1, notification);
    }

    public class MainBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }
}
