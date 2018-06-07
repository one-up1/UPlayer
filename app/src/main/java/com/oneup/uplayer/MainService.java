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
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.oneup.uplayer.activity.PlaylistActivity;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Calendar;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class MainService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    public static final String EXTRA_ACTION = "com.oneup.extra.ACTION";
    public static final String EXTRA_SONGS = "com.oneup.extra.SONGS";
    public static final String EXTRA_SONG_INDEX = "com.oneup.extra.SONG_INDEX";
    public static final String EXTRA_NEXT = "com.oneup.extra.NEXT";
    public static final String EXTRA_PLAYLIST = "com.oneup.extra.PLAYLIST";

    public static final int ACTION_PLAY = 1;
    public static final int ACTION_ADD = 2;
    public static final int ACTION_RESUME_PLAYLIST = 3;

    private static final int ACTION_PREVIOUS = 4;
    private static final int ACTION_PLAY_PAUSE = 5;
    private static final int ACTION_NEXT = 6;
    private static final int ACTION_STOP = 7;
    private static final int ACTION_VOLUME_DOWN = 8;
    private static final int ACTION_VOLUME_UP = 9;

    private static final String TAG = "UPlayer";
    private static final String PREF_VOLUME = "volume";

    private static final int MAX_VOLUME = 100;
    private static final int RESUME_POSITION_OFFSET = 12000;

    private final IBinder mainBinder = new MainBinder();

    private SharedPreferences preferences;
    private DbHelper dbHelper;

    private MediaPlayer player;
    private boolean prepared;
    private int volume;

    private RemoteViews notificationViews;
    private Notification notification;

    private MainReceiver mainReceiver;
    private OnUpdateListener onUpdateListener;

    private ArrayList<Song> songs;
    private int songIndex;
    private Playlist playlist;

    @Override
    public void onCreate() {
        Log.d(TAG, "MainService.onCreate()");
        super.onCreate();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        dbHelper = new DbHelper(this);

        player = new MediaPlayer();
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);

        volume = preferences.getInt(PREF_VOLUME, MAX_VOLUME);

        notificationViews = new RemoteViews(getApplicationContext().getPackageName(),
                R.layout.notification);
        setOnClickPendingIntent(R.id.ibPrevious, ACTION_PREVIOUS);
        setOnClickPendingIntent(R.id.ibPlayPause, ACTION_PLAY_PAUSE);
        setOnClickPendingIntent(R.id.ibNext, ACTION_NEXT);
        setOnClickPendingIntent(R.id.ibStop, ACTION_STOP);
        setOnClickPendingIntent(R.id.ibVolumeDown, ACTION_VOLUME_DOWN);
        setOnClickPendingIntent(R.id.ibVolumeUp, ACTION_VOLUME_UP);

        //FIXME: Notification icon is always ic_launcher.
        notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setCustomContentView(notificationViews)
                .setCustomBigContentView(notificationViews)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, PlaylistActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .build();

        mainReceiver = new MainReceiver();
        registerReceiver(mainReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e(TAG, "No service intent");
            return START_STICKY;
        }

        int action = intent.getIntExtra(EXTRA_ACTION, 0);
        switch (action) {
            case ACTION_PLAY:
                savePlaylist();
                songs = intent.getParcelableArrayListExtra(EXTRA_SONGS);
                play(intent.getIntExtra(EXTRA_SONG_INDEX, 0));
                break;
            case ACTION_ADD:
                ArrayList<Song> songs = intent.getParcelableArrayListExtra(EXTRA_SONGS);
                add(songs, intent.getBooleanExtra(EXTRA_NEXT, false));
                break;
            case ACTION_RESUME_PLAYLIST:
                playlist = intent.getParcelableExtra(EXTRA_PLAYLIST);
                resumePlaylist();
                break;
            case ACTION_PREVIOUS:
                previous();
                break;
            case ACTION_PLAY_PAUSE:
                playPause();
                break;
            case ACTION_NEXT:
                next();
                break;
            case ACTION_STOP:
                stop();
                break;
            case ACTION_VOLUME_DOWN:
                volumeDown();
                break;
            case ACTION_VOLUME_UP:
                volumeUp();
                break;
            default:
                Log.e(TAG, "Invalid service action: " + action);
                break;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MainService.onDestroy()");
        savePlaylist();

        if (player != null) {
            player.stop();
            player.release();
        }

        if (mainReceiver != null) {
            unregisterReceiver(mainReceiver);
        }

        if (dbHelper != null) {
            dbHelper.close();
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mainBinder;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "MainService.onPrepared()");
        
        setVolume();
        if (playlist != null && playlist.getSongPosition() > 0) {
            Log.d(TAG, "Seeking to playlist position: " + playlist.getSongPosition());
            player.seekTo(playlist.getSongPosition());
            playlist.setSongPosition(0);
        }

        player.start();
        prepared = true;

        setPlayPauseResource(R.drawable.ic_pause);
        update();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "MainService.onError(" + what + ", " + extra + ")");
        player.reset();
        prepared = false;
        
        setPlayPauseResource(R.drawable.ic_play);
        startForeground(1, notification);
        
        return true; // Or onCompletion() will be called.
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "MainService.onCompletion()");
        prepared = false;
        
        setPlayPauseResource(R.drawable.ic_play);
        startForeground(1, notification);

        if (player.getCurrentPosition() == 0) {
            Log.e(TAG, "Current position is 0");
        } else {
            try {
                dbHelper.updateSongPlayed(songs.get(songIndex));
                next();
            } catch (Exception ex) {
                Log.e(TAG, "Error updating song played", ex);
                stop();
            }
        }
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public int getSongIndex() {
        return songIndex;
    }

    public void play(int songIndex) {
        this.songIndex = songIndex;
        play();
    }

    public void moveSong(int index, int toIndex) {
        Log.d(TAG, "MainService.moveSong(" + index + ", " + toIndex + "), songIndex=" + songIndex);
        songs.add(toIndex, songs.remove(index));

        if (index == songIndex) {
            songIndex = toIndex;
        } else if (toIndex == songIndex) {
            songIndex = index;
        }

        Log.d(TAG, "songIndex=" + songIndex);
        update();
    }

    public void removeSong(int index) {
        Log.d(TAG, "MainService.removeSong(" + index + "), songIndex=" + songIndex +
                ", size=" + songs.size());
        songs.remove(index);

        if (index < songIndex) {
            songIndex--;
        } else if (index == songIndex) {
            if (index == songs.size()) {
                songIndex--;
            }
            if (player.isPlaying()) {
                // Start playing the next song when the current song is removed, only when
                // currently playing, or playback may start when removing songs while paused.
                play();
            }
        }

        Log.d(TAG, "songIndex=" + songIndex);
        update();
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    public void setOnUpdateListener(OnUpdateListener onUpdateListener) {
        this.onUpdateListener = onUpdateListener;
    }

    private void play() {
        Log.d(TAG, "MainService.play(), " + songs.size() + " songs, songIndex=" + songIndex);
        try {
            player.reset();
            player.setDataSource(getApplicationContext(), songs.get(songIndex).getContentUri());
            player.prepareAsync();
        } catch (Exception ex) {
            Log.e(TAG, "Error preparing MediaPlayer", ex);
        }
    }

    private void add(ArrayList<Song> songs, boolean next) {
        Log.d(TAG, "MainService.add(" + songs.size() + ", " + next + "), songIndex=" + songIndex);
        if (this.songs == null) {
            this.songs = songs;
        } else if (next) {
            this.songs.addAll(songIndex + 1, songs);
        } else {
            this.songs.addAll(songs);
        }

        // Update when playing or paused (prepared), start playing the last song when not.
        // This will start playback of the added song, if it is the first song
        // or is being added to a playlist of which the last song has completed.
        if (prepared) {
            update();
        } else {
            play(this.songs.size() - 1);
        }
    }

    private void savePlaylist() {
        Log.d(TAG, "MainService.savePlaylist()");
        if (songs == null || songs.size() == 0) {
            Log.d(TAG, "No playlist to save");
            return;
        }

        try {
            if (playlist == null) {
                playlist = new Playlist();
                playlist.setId(1);
            }
            playlist.setModified(Calendar.currentTime());
            playlist.setSongIndex(songIndex);
            playlist.setSongPosition(player.getCurrentPosition());
            dbHelper.insertOrUpdatePlaylist(playlist, songs);
        } catch (Exception ex) {
            Log.e(TAG, "Error saving playlist", ex);
        } finally {
            playlist = null;
        }
    }

    private void resumePlaylist() {
        Log.d(TAG, "MainService.resumePlaylist(" + playlist.getName() + "," +
                playlist.getSongIndex() + "," + playlist.getSongPosition() + ")");
        songs = dbHelper.queryPlaylistSongs(playlist);

        if (playlist.getSongIndex() >= songs.size()) {
            Log.w(TAG, "Invalid song index: " + playlist.getSongIndex());
            playlist.setSongIndex(0);
            playlist.setSongPosition(0);
        } else if (playlist.getSongPosition() <= RESUME_POSITION_OFFSET * 2) {
            Log.d(TAG, "Ignoring low song position");
            playlist.setSongPosition(0);
        } else if (playlist.getSongPosition() >=
                songs.get(playlist.getSongIndex()).getDuration() - RESUME_POSITION_OFFSET) {
            Log.d(TAG, "Ignoring high song position");
            playlist.setSongIndex(playlist.getSongIndex() == songs.size() - 1 ? 0
                    : playlist.getSongIndex() + 1);
            playlist.setSongPosition(0);
        } else {
            playlist.setSongPosition(playlist.getSongPosition() - RESUME_POSITION_OFFSET);
        }

        play(playlist.getSongIndex());
    }

    private void previous() {
        Log.d(TAG, "MainService.previous(), songIndex=" + songIndex);
        if (songIndex > 0) {
            play(songIndex - 1);
        }
    }

    private void playPause() {
        Log.d(TAG, "MainService.playPause()");
        if (player.isPlaying()) {
            Log.d(TAG, "Pausing");
            player.pause();
            setPlayPauseResource(R.drawable.ic_play);
        } else {
            if (prepared) {
                Log.d(TAG, "Resuming");
                player.start();
            } else {
                play();
            }
            setPlayPauseResource(R.drawable.ic_pause);
        }
        startForeground(1, notification);
    }

    private void next() {
        Log.d(TAG, "MainService.next(), songIndex=" + songIndex);
        if (songIndex < songs.size() - 1) {
            play(songIndex + 1);
        }
    }

    private void volumeDown() {
        Log.d(TAG, "MainService.volumeDown()");
        if (volume > 0) {
            setVolume(volume - 1);
        }
    }

    private void volumeUp() {
        Log.d(TAG, "MainService.volumeUp()");
        if (volume < MAX_VOLUME) {
            setVolume(volume + 1);
        }
    }

    private void setVolume(int volume) {
        this.volume = volume;

        setVolume();
        preferences.edit().putInt(PREF_VOLUME, volume).apply();
    }

    private void setVolume() {
        float volume = (float)
                (1 - (Math.log(MAX_VOLUME + 1 - this.volume) / Math.log(MAX_VOLUME)));
        Log.d(TAG, "volume=" + this.volume + " (" + volume + ")");
        player.setVolume(volume, volume);

        notificationViews.setTextViewText(R.id.tvVolume, Integer.toString(this.volume));
        startForeground(1, notification);
    }

    private void setPlayPauseResource(int srcId) {
        notificationViews.setImageViewResource(R.id.ibPlayPause, srcId);
    }

    private void update() {
        Log.d(TAG, "MainService.update()");
        Song song = songs.get(songIndex);

        notificationViews.setTextViewText(R.id.tvSongTitle, song.getTitle());
        notificationViews.setTextViewText(R.id.tvSongArtist, song.getArtist());

        String left = Util.formatDuration(Song.getDuration(songs, songIndex));
        if (songIndex < songs.size() - 1) {
            left += " / " + Util.formatDuration(Song.getDuration(songs, songIndex + 1)) +
                    " / " + (songs.size() - songIndex - 1);
        }
        notificationViews.setTextViewText(R.id.tvPlaylistPosition, getString(
                R.string.playlist_position, songIndex + 1, songs.size(), left));

        startForeground(1, notification);

        if (onUpdateListener != null) {
            onUpdateListener.onUpdate();
        }
    }

    private void setOnClickPendingIntent(int viewId, int action) {
        notificationViews.setOnClickPendingIntent(viewId, PendingIntent.getService(this, action,
                new Intent(this, MainService.class)
                        .putExtra(EXTRA_ACTION, action),
                PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private void stop() {
        PlaylistActivity.finishIfRunning();
        stopSelf();
    }

    public class MainBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }

    public interface OnUpdateListener {
        void onUpdate();
    }
}
