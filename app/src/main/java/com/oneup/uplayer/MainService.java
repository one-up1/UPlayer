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
import android.view.View;
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
    public static final int ACTION_PLAY_PLAYLIST = 3;
    public static final int ACTION_UPDATE_PLAYLIST_POSITION = 4;

    private static final int ACTION_PREVIOUS = 5;
    private static final int ACTION_PLAY_PAUSE = 6;
    private static final int ACTION_NEXT = 7;
    private static final int ACTION_STOP = 8;
    private static final int ACTION_VOLUME_DOWN = 9;
    private static final int ACTION_VOLUME_UP = 10;

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
    private OnSongChangeListener onSongChangeListener;

    private ArrayList<Song> songs;
    private int songIndex;

    private long playlistId;
    private int songPosition;

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

        //FIXME: Notification icon is always ic_launcher and channel ID must be set.
        // Use NotificationManager to update instead of startForeground()?
        // Start service using startForegroundService()?
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
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(mainReceiver, filter);
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
                play(intent.<Song>getParcelableArrayListExtra(EXTRA_SONGS),
                        intent.getIntExtra(EXTRA_SONG_INDEX, 0));
                break;
            case ACTION_ADD:
                add(intent.<Song>getParcelableArrayListExtra(EXTRA_SONGS),
                        intent.getBooleanExtra(EXTRA_NEXT, false));
                break;
            case ACTION_PLAY_PLAYLIST:
                playPlaylist((Playlist) intent.getParcelableExtra(EXTRA_PLAYLIST));
                break;
            case ACTION_UPDATE_PLAYLIST_POSITION:
                updatePlaylistPosition();
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
        prepared = false;

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
        
        // Seek to the saved song position of a playlist.
        if (songPosition > 0) {
            Log.d(TAG, "Seeking to song position: " + songPosition);
            player.seekTo(songPosition);
            songPosition = 0;
        }

        setVolume();
        player.start();
        prepared = true;

        setPlayPauseResource(R.drawable.ic_notification_pause);
        updateCurrentSong();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "MainService.onError(" + what + ", " + extra + ")");
        player.reset();
        prepared = false;
        songPosition = 0;
        
        setPlayPauseResource(R.drawable.ic_notification_play);
        startForeground(1, notification);

        return true; // Or onCompletion() will be called.
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "MainService.onCompletion()");
        prepared = false;
        
        setPlayPauseResource(R.drawable.ic_notification_play);
        updatePlaylistPosition();

        if (player.getCurrentPosition() == 0) {
            Log.e(TAG, "Current position is 0");
        } else {
            try {
                dbHelper.updateSongPlayed(getSong());
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

    public void setSongIndex(int songIndex) {
        Log.d(TAG, "MainService.setSongIndex(" + songIndex + ")");
        this.songIndex = songIndex;
        play();
    }

    public Song getSong() {
        return songs.get(songIndex);
    }

    public void moveSong(int index, int toIndex) {
        Log.d(TAG, "MainService.moveSong(" + index + ", " + toIndex + "), songIndex=" + songIndex);
        songs.add(toIndex, songs.remove(index));

        if (index == songIndex) {
            // The current song is moved.
            songIndex = toIndex;
        } else if (index < songIndex && toIndex >= songIndex) {
            // A song above the current song is moved to or below the current song.
            songIndex--;
        } else if (index > songIndex && toIndex <= songIndex) {
            // A song below the current song is moved to or above the current song.
            songIndex++;
        }
        updateCurrentSong();
    }

    public void removeSong(int index) {
        Log.d(TAG, "MainService.removeSong(" + index + "), songIndex=" + songIndex +
                ", size=" + songs.size());
        songs.remove(index);

        if (index < songIndex) {
            // A song above the current song is removed.
            songIndex--;
        } else if (index == songIndex) {
            if (index == songs.size()) {
                // Stop playback when the current and last song is removed.
                player.stop();
                prepared = false;
                songIndex--;

                setPlayPauseResource(R.drawable.ic_notification_play);
            } else if (player.isPlaying()) {
                // Play the next song when the current song is removed, only when currently playing,
                // or playback may start when removing songs while paused.
                play();
            }
        }
        updateCurrentSong();
    }

    public void setPlaylist(Playlist playlist) {
        Log.d(TAG, "MainService.setPlaylist(" + playlist + ")");
        playlistId = playlist.getId();

        savePlaylist();
        updateCurrentSong();
    }

    public void updateCurrentSong() {
        Log.d(TAG, "MainService.updateCurrentSong(), songIndex=" + songIndex);
        Song song = getSong();
        dbHelper.querySong(song);

        // Set title and artist.
        notificationViews.setTextViewText(R.id.tvSongTitle, song.getTitle());
        notificationViews.setTextViewText(R.id.tvSongArtist, song.getArtist());

        // Set tag.
        if (song.getTag() == null) {
            notificationViews.setViewVisibility(R.id.tvTag, View.GONE);
        } else {
            notificationViews.setTextViewText(R.id.tvTag, song.getTag());
            notificationViews.setViewVisibility(R.id.tvTag, View.VISIBLE);
        }

        // Set playlist name.
        if (playlistId == 1) {
            notificationViews.setViewVisibility(R.id.tvPlaylistName, View.GONE);
        } else {
            notificationViews.setTextViewText(R.id.tvPlaylistName,
                    dbHelper.queryPlaylistName(playlistId));
            notificationViews.setViewVisibility(R.id.tvPlaylistName, View.VISIBLE);
        }

        updatePlaylistPosition();

        // Update PlaylistActivity.
        if (onSongChangeListener != null) {
            onSongChangeListener.onSongChange();
        }
    }

    public void setOnSongChangeListener(OnSongChangeListener onSongChangeListener) {
        this.onSongChangeListener = onSongChangeListener;
    }

    private void play(ArrayList<Song> songs, int songIndex) {
        Log.d(TAG, "MainService.play(" + songs.size() + ", " + songIndex + ")");
        savePlaylist();
        playlistId = 1;

        this.songs = songs;
        this.songIndex = songIndex;
        play();
    }

    private void add(ArrayList<Song> songs, boolean next) {
        Log.d(TAG, "MainService.add(" + songs.size() + ", " + next + "), songIndex=" + songIndex);
        if (this.songs == null) {
            this.songs = songs;
            playlistId = 1;
        } else if (next) {
            this.songs.addAll(songIndex + 1, songs);
        } else {
            this.songs.addAll(songs);
        }

        // Update current song when playing or paused (prepared), or play the first added song.
        // This will start playback of the added song, if it is the first song
        // or is being added to a playlist of which the last song has completed.
        if (prepared) {
            updateCurrentSong();
        } else {
            songIndex = this.songs.size() - songs.size();
            play();
        }
    }

    private void playPlaylist(Playlist playlist) {
        Log.d(TAG, "MainService.playPlaylist(" + playlist + ")");

        // Save the current playlist only when a different one is going to be played.
        Log.d(TAG, "id=" + playlist.getId() + ", current=" + playlistId);
        if (playlist.getId() != playlistId) {
            savePlaylist();
        }

        songs = dbHelper.queryPlaylistSongs(playlist);
        if (songs.size() == 0) {
            Log.e(TAG, "Playlist is empty");
            stopSelf();
            return;
        }

        songIndex = playlist.getSongIndex();
        songPosition = playlist.getSongPosition();
        playlistId = playlist.getId();

        if (songIndex >= songs.size()) {
            Log.w(TAG, "Invalid song index: " + songIndex);
            songIndex = 0;
            songPosition = 0;
        } else if (songPosition <= RESUME_POSITION_OFFSET * 2) {
            Log.d(TAG, "Ignoring low song position: " + songPosition);
            songPosition = 0;
        } else if (songPosition >= getSong().getDuration() - RESUME_POSITION_OFFSET) {
            Log.d(TAG, "Ignoring high song position: " + songPosition);
            songIndex = songIndex == songs.size() - 1 ? 0 : songIndex + 1;
            songPosition = 0;
        } else {
            Log.d(TAG, "Using song position: " + songPosition);
            songPosition -= RESUME_POSITION_OFFSET;
        }

        play();
    }

    private void previous() {
        Log.d(TAG, "MainService.previous(), songIndex=" + songIndex);
        if (songIndex > 0) {
            songIndex--;
            play();
        }
    }

    private void playPause() {
        Log.d(TAG, "MainService.playPause()");
        if (player.isPlaying()) {
            Log.d(TAG, "Pausing");
            player.pause();

            setPlayPauseResource(R.drawable.ic_notification_play);
            updatePlaylistPosition();
        } else {
            if (prepared) {
                Log.d(TAG, "Resuming");
                player.seekTo(player.getCurrentPosition() <= RESUME_POSITION_OFFSET * 2 ? 0
                        : player.getCurrentPosition() - RESUME_POSITION_OFFSET);
                player.start();

                setPlayPauseResource(R.drawable.ic_notification_pause);
                updatePlaylistPosition();
            } else {
                play();
            }
        }
    }

    private void next() {
        Log.d(TAG, "MainService.next(), songIndex=" + songIndex);
        if (songIndex < songs.size() - 1) {
            songIndex++;
            play();
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

    private void play() {
        Log.d(TAG, "MainService.play(), " + songs.size() + " songs, songIndex=" + songIndex);
        try {
            player.reset();
            player.setDataSource(getApplicationContext(), getSong().getContentUri());
            player.prepareAsync();
        } catch (Exception ex) {
            Log.e(TAG, "Error preparing MediaPlayer", ex);
        }
    }

    private void setVolume() {
        float volume = (float)
                (1 - (Math.log(MAX_VOLUME + 1 - this.volume) / Math.log(MAX_VOLUME)));
        Log.d(TAG, "volume=" + this.volume + " (" + volume + ")");
        player.setVolume(volume, volume);

        notificationViews.setTextViewText(R.id.tvVolume, Integer.toString(this.volume));
        updatePlaylistPosition();
    }

    private void setVolume(int volume) {
        this.volume = volume;
        if (prepared) {
            setVolume();
        }
        preferences.edit().putInt(PREF_VOLUME, volume).apply();
    }

    private void setPlayPauseResource(int srcId) {
        notificationViews.setImageViewResource(R.id.ibPlayPause, srcId);
    }

    private void updatePlaylistPosition() {
        Log.d(TAG, "MainService.updatePlaylistPosition(), songIndex=" + songIndex +
                ", size=" + songs.size());

        // Set song index, song count, songs left and time left.
        String left = Util.formatDuration(
                Song.getDuration(songs, songIndex) - player.getCurrentPosition());
        if (songIndex < songs.size() - 1) {
            left = (songs.size() - songIndex - 1) + " / " + left;
        }
        notificationViews.setTextViewText(R.id.tvPlaylistPosition, getString(
                R.string.playlist_position, songIndex + 1, songs.size(), left));

        startForeground(1, notification);
    }

    private void savePlaylist() {
        Log.d(TAG, "MainService.savePlaylist()");
        if (songs == null || songs.size() == 0) {
            Log.d(TAG, "No playlist to save");
            return;
        }

        try {
            Playlist playlist = new Playlist();
            playlist.setId(playlistId);
            playlist.setModified(Calendar.currentTime());
            playlist.setSongIndex(songIndex);
            playlist.setSongPosition(player.getCurrentPosition());
            Log.d(TAG, "playlistId=" + playlistId + ", songIndex=" + songIndex +
                    ", songPosition=" + songPosition + ", " + songs.size() + " songs");
            dbHelper.insertOrUpdatePlaylist(playlist, songs);
        } catch (Exception ex) {
            Log.e(TAG, "Error saving playlist", ex);
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

    public interface OnSongChangeListener {
        void onSongChange();
    }
}
