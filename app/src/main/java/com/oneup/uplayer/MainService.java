package com.oneup.uplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.oneup.uplayer.activity.PlaylistActivity;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;

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

    private static final int REQUEST_PREVIOUS = 4;
    private static final int REQUEST_PLAY_PAUSE = 5;
    private static final int REQUEST_NEXT = 6;
    private static final int REQUEST_STOP = 7;
    private static final int REQUEST_VOLUME_DOWN = 8;
    private static final int REQUEST_VOLUME_UP = 9;

    private static final String TAG = "UPlayer";

    private static final String KEY_VOLUME = "volume";
    private static final int MAX_VOLUME = 100;

    private final IBinder mainBinder = new MainBinder();

    private SharedPreferences preferences;
    private DbOpenHelper dbOpenHelper;

    private MediaPlayer player;
    private int volume;

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

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        dbOpenHelper = new DbOpenHelper(this);

        player = new MediaPlayer();
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.setOnCompletionListener(this);

        volume = preferences.getInt(KEY_VOLUME, MAX_VOLUME);

        notificationViews = new RemoteViews(getApplicationContext().getPackageName(),
                R.layout.notification);
        setOnClickPendingIntent(notificationViews, R.id.ibPrevious, REQUEST_PREVIOUS);
        setOnClickPendingIntent(notificationViews, R.id.ibPlayPause, REQUEST_PLAY_PAUSE);
        setOnClickPendingIntent(notificationViews, R.id.ibNext, REQUEST_NEXT);
        setOnClickPendingIntent(notificationViews, R.id.ibStop, REQUEST_STOP);
        setOnClickPendingIntent(notificationViews, R.id.ibVolumeDown, REQUEST_VOLUME_DOWN);
        setOnClickPendingIntent(notificationViews, R.id.ibVolumeUp, REQUEST_VOLUME_UP);

        notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCustomContentView(notificationViews)
                .setCustomBigContentView(notificationViews)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, PlaylistActivity.class)
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
                addSong((Song) intent.getParcelableExtra(ARG_SONG), true);
                break;
            case REQUEST_PLAY_LAST:
                addSong((Song) intent.getParcelableExtra(ARG_SONG), false);
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
                PlaylistActivity.finishIfRunning();
                stopSelf();
                break;
            case REQUEST_VOLUME_DOWN:
                volumeDown();
                break;
            case REQUEST_VOLUME_UP:
                volumeUp();
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
        setVolume();
        player.start();

        Song song = songs.get(songIndex);
        notificationViews.setTextViewText(R.id.tvSongTitle, song.getTitle());
        notificationViews.setTextViewText(R.id.tvSongArtist, song.getArtist().getArtist());
        updatePlaylistPosition();
        notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_pause);
        startForeground(1, notification);

        prepared = true;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "MainService.onError(" + what + ", " + extra + ")");
        player.reset();

        notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_play);
        startForeground(1, notification);

        prepared = false;
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "MainService.onCompletion()");
        notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_play);
        startForeground(1, notification);
        prepared = false;

        //FIXME: onCompletion not always called or position 0?
        if (player.getCurrentPosition() == 0) {
            Log.d(TAG, "Current position is 0");
        } else {
            dbOpenHelper.updateSongPlayed(songs.get(songIndex));
            next();
        }
    }

    private void setOnClickPendingIntent(RemoteViews views, int viewId, int requestCode) {
        views.setOnClickPendingIntent(viewId, PendingIntent.getService(this, requestCode,
                new Intent(this, MainService.class)
                        .putExtra(ARG_REQUEST_CODE, requestCode),
                PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private void play() {
        Log.d(TAG, "MainService.play(), " + songs.size() + " songs, songIndex=" + songIndex);
        Song song = songs.get(songIndex);
        try {
            player.reset();
            player.setDataSource(getApplicationContext(), song.getContentUri());
            player.prepareAsync();
        } catch (Exception ex) {
            Log.e(TAG, "Error setting data source", ex);

            // Delete song from DB when it doesn't exist anymore.
            dbOpenHelper.deleteSong(song);
            songs.remove(song);

            if (songIndex < songs.size()) {
                play();
            } else {
                Log.d(TAG, "No more songs to play");
                stopSelf();
            }
        }
    }

    private void setVolume() {
        Log.d(TAG, "MainService.setVolume()");
        float f = (float) (1 - (Math.log(MAX_VOLUME + 1 - volume) / Math.log(MAX_VOLUME)));
        Log.d(TAG, "volume=" + volume + ":" + f);
        player.setVolume(f, f);

        notificationViews.setTextViewText(R.id.tvVolume, Integer.toString(this.volume));
        startForeground(1, notification);
    }

    private void updatePlaylistPosition() {
        String left = Util.formatDuration(Song.getDuration(songs, songIndex));
        if (songIndex < songs.size() - 1) {
            left += " / " + Util.formatDuration(Song.getDuration(songs, songIndex + 1));
        }
        notificationViews.setTextViewText(R.id.tvPlaylistPosition, getString(
                R.string.playlist_position, songIndex + 1, songs.size(), left));
    }

    private void addSong(Song song, boolean next) {
        Log.d(TAG, "MainService.addSong(" + song + ", " + next + ")");
        if (songs == null) {
            songs = new ArrayList<>();
            songs.add(song);

            notificationViews.setTextViewText(R.id.tvSongTitle, song.getTitle());
            notificationViews.setTextViewText(R.id.tvSongArtist, song.getArtist().getArtist());
            notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_play);
        } else if (next) {
            songs.add(songIndex + 1, song);
        } else {
            songs.add(song);
        }

        updatePlaylistPosition();
        startForeground(1, notification);
    }

    private void previous() {
        Log.d(TAG, "MainService.previous()");
        if (songIndex > 0) {
            songIndex--;
            play();
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

    private void next() {
        Log.d(TAG, "MainService.next()");
        if (songIndex < songs.size() - 1) {
            songIndex++;
            play();
        }
    }

    private void volumeDown() {
        Log.d(TAG, "MainService.volumeDown()");
        if (volume > 0) {
            volume--;
            changeVolume();
        }
    }

    private void volumeUp() {
        Log.d(TAG, "MainService.volumeUp()");
        if (volume < MAX_VOLUME) {
            volume++;
            changeVolume();
        }
    }

    private void changeVolume() {
        setVolume();
        preferences.edit().putInt(KEY_VOLUME, volume).apply();
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public void setSongIndex(int songIndex) {
        Log.d(TAG, "MainService.setSongIndex(" + songIndex + ")");
        this.songIndex = songIndex;
        play();
    }

    public void deleteSong(Song song) {
        Log.d(TAG, "MainService.deleteSong(" + song + ")");
        if (songs.size() > 1) {
            int songIndex = songs.indexOf(song);
            songs.remove(songIndex);

            if (songIndex < this.songIndex) {
                this.songIndex--;
            } else if (songIndex == this.songIndex) {
                if (songIndex == songs.size()) {
                    this.songIndex--;
                }
                play();
            }

            updatePlaylistPosition();
            startForeground(1, notification);
        } else {
            stopSelf();
        }
    }

    public class MainBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }
}
