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
import android.text.SpannableStringBuilder;
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
    public static final String EXTRA_PLAYLIST = "com.oneup.extra.PLAYLIST";
    public static final String EXTRA_NEXT = "com.oneup.extra.NEXT";

    public static final int ACTION_PLAY = 1;
    public static final int ACTION_ADD = 2;
    public static final int ACTION_UPDATE = 3;

    private static final int ACTION_PAUSE_PLAY = 4;
    private static final int ACTION_PREVIOUS = 5;
    private static final int ACTION_NEXT = 6;
    private static final int ACTION_STOP = 7;
    private static final int ACTION_VOLUME_DOWN = 8;
    private static final int ACTION_VOLUME_UP = 9;

    private static final String TAG = "UPlayer";
    private static final String PREF_VOLUME = "volume";

    private static final int MAX_VOLUME = 100;
    private static final int RESUME_POSITION_OFFSET = 8000;

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
        setOnClickPendingIntent(R.id.ibPausePlay, ACTION_PAUSE_PLAY);
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
                        (Playlist) intent.getParcelableExtra(EXTRA_PLAYLIST));
                break;
            case ACTION_ADD:
                add(intent.<Song>getParcelableArrayListExtra(EXTRA_SONGS),
                        intent.getBooleanExtra(EXTRA_NEXT, false));
                break;
            case ACTION_UPDATE:
                update();
                break;
            case ACTION_PAUSE_PLAY:
                pausePlay();
                break;
            case ACTION_PREVIOUS:
                previous();
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
    public void onPrepared(MediaPlayer player) {
        Log.d(TAG, "MainService.onPrepared()");

        // Seek to the saved song position of the playlist.
        Log.d(TAG, "songPosition=" + playlist.getSongPosition());
        if (playlist.getSongPosition() > 0) {
            player.seekTo(playlist.getSongPosition());
            playlist.setSongPosition(0);
        }

        setVolume();
        player.start();

        prepared = true;
        update();
    }

    @Override
    public boolean onError(MediaPlayer player, int what, int extra) {
        Log.d(TAG, "MainService.onError(" + what + ", " + extra + ")");
        player.reset();
        prepared = false;

        playlist.reset();
        update();

        return true; // Or onCompletion() will be called.
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        Log.d(TAG, "MainService.onCompletion()");
        prepared = false;

        if (player.getCurrentPosition() == 0) {
            Log.e(TAG, "Current position is 0");
            update();
        } else {
            try {
                dbHelper.updateSongPlayed(getSong());
                next();
            } catch (Exception ex) {
                Log.e(TAG, "Error updating song played", ex);
            }
        }
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public Song getSong() {
        return songs.get(playlist.getSongIndex());
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist playlist) {
        Log.d(TAG, "MainService.setPlaylist(" + playlist.getId() + ":" + playlist + ")");
        playlist.setSongIndex(this.playlist.getSongIndex());
        this.playlist = playlist;
        update();
    }

    public void play(int songIndex) {
        Log.d(TAG, "MainService.play(" + songIndex + ")");
        playlist.setSongIndex(songIndex);
        prepare();
    }

    public void moveSong(int index, int toIndex) {
        Log.d(TAG, "MainService.moveSong(" + index + ", " + toIndex + ")");
        Log.d(TAG, songs.size() + " songs, songIndex=" + playlist.getSongIndex());
        songs.add(toIndex, songs.remove(index));

        if (index == playlist.getSongIndex()) {
            // The current song is moved.
            playlist.setSongIndex(toIndex);
        } else if (index < playlist.getSongIndex() && toIndex >= playlist.getSongIndex()) {
            // A song above the current song is moved to or below the current song.
            playlist.decrementSongIndex();
        } else if (index > playlist.getSongIndex() && toIndex <= playlist.getSongIndex()) {
            // A song below the current song is moved to or above the current song.
            playlist.incrementSongIndex();
        }
        update();
    }

    public void removeSong(int index) {
        Log.d(TAG, "MainService.removeSong(" + index + ")");
        Log.d(TAG, songs.size() + " songs, songIndex=" + playlist.getSongIndex());
        songs.remove(index);

        if (index < playlist.getSongIndex()) {
            // A song above the current song is removed.
            playlist.decrementSongIndex();
        } else if (index == playlist.getSongIndex()) {
            // The current song is removed.
            if (index == songs.size()) {
                // Stop playback when the current and last song is removed.
                player.stop();
                prepared = false;
                playlist.decrementSongIndex();
            } else if (player.isPlaying()) {
                // Play the next song when the current song is removed, only when currently playing,
                // or playback may start when removing songs while paused.
                prepare();
            }
        }
        update();
    }

    public void update() {
        Log.d(TAG, "MainService.update()");
        Log.d(TAG, "songIndex=" + playlist.getSongIndex());

        // Get song from playlist and query it to refresh any fields that may have changed.
        Song song = getSong();
        dbHelper.querySong(song);

        // Set song title and artist.
        notificationViews.setTextViewText(R.id.tvSongTitle, song.getTitle());
        notificationViews.setTextViewText(R.id.tvSongArtist, song.getArtist());

        // Set playlist position with song index, song count, songs left and time left.
        String left = Util.formatDuration(
                Song.getDuration(songs, playlist.getSongIndex()) - player.getCurrentPosition());
        if (playlist.getSongIndex() < songs.size() - 1) {
            left = (songs.size() - playlist.getSongIndex() - 1) + " / " + left;
        }
        notificationViews.setTextViewText(R.id.tvPlaylistPosition, getString(
                R.string.playlist_position, playlist.getSongIndex() + 1, songs.size(), left));

        // Set song year and tag.
        setOptionalValue(R.id.tvSongYear, song.getYear() == 0 ? null
                : Integer.toString(song.getYear()));
        setOptionalValue(R.id.tvSongTag, song.getTag());

        // Set the names of the playlists the song is on, marking the current playlist.
        SpannableStringBuilder playlistNames = new SpannableStringBuilder();
        for (Playlist playlist : dbHelper.queryPlaylists(song)) {
            if (!playlist.isDefault()) {
                if (playlist.equals(this.playlist)) {
                    if (playlistNames.length() > 0) {
                        playlistNames.insert(0, ", ");
                    }
                    playlistNames.insert(0, Util.underline(playlist.getName()));
                } else {
                    if (playlistNames.length() > 0) {
                        playlistNames.append(", ");
                    }
                    playlistNames.append(playlist.getName());
                }
            }
        }
        setOptionalValue(R.id.tvSongPlaylistNames,
                playlistNames.length() == 0 ? null : playlistNames);

        // Set play/pause image and volume.
        notificationViews.setImageViewResource(R.id.ibPausePlay, prepared && player.isPlaying() ?
                        R.drawable.ic_notification_pause : R.drawable.ic_notification_play);
        notificationViews.setTextViewText(R.id.tvVolume, Integer.toString(volume));

        // Update notification and PlaylistActivity.
        startForeground(1, notification);
        if (onUpdateListener != null) {
            onUpdateListener.onUpdate();
        }
    }

    public void setOnUpdateListener(OnUpdateListener onUpdateListener) {
        this.onUpdateListener = onUpdateListener;
    }

    private void play(ArrayList<Song> songs, Playlist playlist) {
        Log.d(TAG, "MainService.play(" + (songs == null ? "null" : songs.size()) + ", " +
                playlist.getId() + ":" + playlist + ")");

        // Use playlist songs from DB when no songs are specified.
        if (songs == null) {
            songs = dbHelper.queryPlaylistSongs(playlist);
            if (songs.isEmpty()) {
                Log.w(TAG, "Playlist is empty");
                if (this.songs == null) {
                    stopSelf();
                }
                return;
            }

            // Use the saved song index and position of the playlist.
            if (playlist.getSongIndex() >= songs.size()) {
                Log.w(TAG, "Invalid song index: " + playlist.getSongIndex());
                playlist.reset();
            } else if (playlist.getSongPosition() <= RESUME_POSITION_OFFSET * 2) {
                Log.d(TAG, "Ignoring low song position: " + playlist.getSongPosition());
                playlist.setSongPosition(0);
            } else {
                Log.d(TAG, "Using song position: " + playlist.getSongPosition());
                playlist.setSongPosition(playlist.getSongPosition() - RESUME_POSITION_OFFSET);
            }
        }

        savePlaylist();
        this.songs = songs;
        this.playlist = playlist;
        prepare();
    }

    private void add(ArrayList<Song> songs, boolean next) {
        Log.d(TAG, "MainService.add(" + songs.size() + ", " + next + ")");
        if (this.songs == null) {
            this.songs = songs;
            playlist = Playlist.getDefault();
        } else if (next) {
            this.songs.addAll(playlist.getSongIndex() + 1, songs);
        } else {
            this.songs.addAll(songs);
        }

        // Update when playing or paused (prepared), or play the first added song.
        // This will start playback of the added song, if it is the first song
        // or is being added to a playlist of which the last song has completed.
        if (prepared) {
            update();
        } else {
            playlist.setSongIndex(this.songs.size() - songs.size());
            prepare();
        }
    }

    private void pausePlay() {
        Log.d(TAG, "MainService.pausePlay()");
        if (player.isPlaying()) {
            Log.d(TAG, "Pausing");
            player.pause();
            update();
        } else {
            if (prepared) {
                Log.d(TAG, "Resuming");
                player.seekTo(player.getCurrentPosition() <= RESUME_POSITION_OFFSET * 2 ? 0
                        : player.getCurrentPosition() - RESUME_POSITION_OFFSET);
                player.start();
                update();
            } else {
                prepare();
            }
        }
    }

    private void previous() {
        Log.d(TAG, "MainService.previous()");
        Log.d(TAG, songs.size() + " songs, songIndex=" + playlist.getSongIndex());
        if (playlist.getSongIndex() > 0) {
            playlist.decrementSongIndex();
            prepare();
        } else {
            update();
        }
    }

    private void next() {
        Log.d(TAG, "MainService.next()");
        Log.d(TAG, songs.size() + " songs, songIndex=" + playlist.getSongIndex());
        if (playlist.getSongIndex() < songs.size() - 1) {
            playlist.incrementSongIndex();
            prepare();
        } else {
            update();
        }
    }

    private void stop() {
        Log.d(TAG, "MainService.stop()");
        PlaylistActivity.finishIfRunning();
        stopSelf();
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

    private void savePlaylist() {
        Log.d(TAG, "MainService.savePlaylist()");
        if (songs == null || songs.isEmpty()) {
            Log.d(TAG, "No playlist to save");
            return;
        }

        try {
            playlist.setSongPosition(player.getCurrentPosition());
            playlist.setLastPlayed(Calendar.currentTime());
            Log.d(TAG, "songIndex=" + playlist.getSongIndex() +
                    ", songPosition=" + playlist.getSongPosition());
            dbHelper.insertOrUpdatePlaylist(playlist, songs);
        } catch (Exception ex) {
            Log.e(TAG, "Error saving playlist", ex);
        }
    }

    private void prepare() {
        Log.d(TAG, "MainService.prepare()");
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
        Log.d(TAG, "volume=" + this.volume + ":" + volume);
        player.setVolume(volume, volume);
    }

    private void setVolume(int volume) {
        this.volume = volume;
        if (prepared) {
            setVolume();
        }
        update();
        preferences.edit().putInt(PREF_VOLUME, volume).apply();
    }

    private void setOnClickPendingIntent(int viewId, int action) {
        notificationViews.setOnClickPendingIntent(viewId, PendingIntent.getService(this, action,
                new Intent(this, MainService.class)
                        .putExtra(EXTRA_ACTION, action),
                PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private void setOptionalValue(int viewId, CharSequence text) {
        if (text == null) {
            notificationViews.setViewVisibility(viewId, View.GONE);
        } else {
            notificationViews.setTextViewText(viewId, text);
            notificationViews.setViewVisibility(viewId, View.VISIBLE);
        }
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
