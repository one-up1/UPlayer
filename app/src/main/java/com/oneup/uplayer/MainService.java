package com.oneup.uplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.oneup.uplayer.activity.EditSongActivity;
import com.oneup.uplayer.activity.PlaylistActivity;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Settings;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;
import java.util.Collections;

public class MainService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    public static final String EXTRA_ACTION = "com.oneup.extra.ACTION";
    public static final String EXTRA_SONGS = "com.oneup.extra.SONGS";
    public static final String EXTRA_PLAYLIST = "com.oneup.extra.PLAYLIST";
    public static final String EXTRA_NEXT = "com.oneup.extra.NEXT";

    public static final int ACTION_PLAY = 1;
    public static final int ACTION_ADD = 2;

    private static final int ACTION_UPDATE = 3;
    private static final int ACTION_EDIT_SONG = 4;
    private static final int ACTION_PAUSE_PLAY = 5;
    private static final int ACTION_NEXT = 6;
    private static final int ACTION_STOP = 7;
    private static final int ACTION_VOLUME_DOWN = 8;
    private static final int ACTION_VOLUME_UP = 9;

    private static final String TAG = "UPlayer";

    private static final String EXTRA_LOAD_SETTINGS = "com.oneup.extra.LOAD_SETTINGS";
    private static final String EXTRA_EDITED_SONG = "com.oneup.extra.EDITED_SONG";
    private static final String EXTRA_PLAY = "com.oneup.extra.PLAY";

    private static boolean running;

    private final IBinder mainBinder = new MainBinder();

    private Settings settings;
    private int volume;
    private int maxVolume;
    private int resumeOffset;

    private DbHelper dbHelper;

    private MediaPlayer player;
    private boolean prepared;
    private boolean completed;

    private RemoteViews notificationLayout;
    private RemoteViews notificationLayoutExpanded;
    private Notification notification;

    private MainReceiver mainReceiver;
    private OnUpdateListener onUpdateListener;

    private ArrayList<Song> songs;
    private Playlist playlist;

    // Used for the "restore previous" option.
    private int songIndex;
    private int songPosition;

    @Override
    public void onCreate() {
        Log.d(TAG, "MainService.onCreate()");
        super.onCreate();

        settings = Settings.get(this);
        loadSettings();

        dbHelper = new DbHelper(this);

        player = new MediaPlayer();
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
        );
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);

        NotificationChannel notificationChannel = new NotificationChannel(TAG,
                getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationChannel.enableVibration(false);
        notificationChannel.enableLights(false);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(notificationChannel);

        notificationLayout = new RemoteViews(getApplicationContext().getPackageName(),
                R.layout.notification_small);
        notificationLayoutExpanded = new RemoteViews(getApplicationContext().getPackageName(),
                R.layout.notification_large);
        setOnClickPendingIntent(false, R.id.ibEditSong, ACTION_EDIT_SONG);
        setOnClickPendingIntent(false, R.id.ibPausePlay, ACTION_PAUSE_PLAY);
        setOnClickPendingIntent(false, R.id.ibNext, ACTION_NEXT);
        setOnClickPendingIntent(false, R.id.ibStop, ACTION_STOP);
        setOnClickPendingIntent(true, R.id.ibVolumeDown, ACTION_VOLUME_DOWN);
        setOnClickPendingIntent(true, R.id.ibVolumeUp, ACTION_VOLUME_UP);

        notification = new NotificationCompat.Builder(this, TAG)
                .setSmallIcon(R.drawable.ic_notification)
                .setShowWhen(false)
                .setCustomContentView(notificationLayout)
                .setCustomBigContentView(notificationLayoutExpanded)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, PlaylistActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setOngoing(true)
                .build();

        mainReceiver = new MainReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(mainReceiver, filter);

        running = true;
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
                update(intent.getBooleanExtra(EXTRA_LOAD_SETTINGS, false),
                        (Song) intent.getParcelableExtra(EXTRA_EDITED_SONG));
                break;
            case ACTION_EDIT_SONG:
                editSong();
                break;
            case ACTION_PAUSE_PLAY:
                pausePlay(intent.getBooleanExtra(EXTRA_PLAY, true));
                break;
            case ACTION_NEXT:
                next(false, false);
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
        running = false;
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
    public void onPrepared(MediaPlayer player) {
        Log.d(TAG, "MainService.onPrepared()");

        seekTo(playlist.getSongPosition(), true);
        playlist.setSongPosition(0);

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
        update();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer player) {
        Log.d(TAG, "MainService.onCompletion()");
        prepared = false;
        completed = true;

        if (player.getCurrentPosition() > 0) {
            next(true, false);
        } else {
            Log.e(TAG, "Current position is " + player.getCurrentPosition());
            update();
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
        setPrevious();
        playlist.setSongIndex(songIndex);
        prepare();
    }

    public void next(boolean markPlayed, boolean stop) {
        Log.d(TAG, "MainService.next(" + markPlayed + ", " + stop + ")");
        Log.d(TAG, songs.size() + " songs, songIndex=" + playlist.getSongIndex());

        if (markPlayed) {
            dbHelper.updateSongPlayed(getSong());
            songIndex = -1;
        } else {
            setPrevious();
        }

        if (hasSongsLeft()) {
            playlist.incrementSongIndex();
            prepare();
        } else if (stop) {
            stop();
        } else {
            update();
        }
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
        songIndex = -1;
        update();
    }

    public void removeSong(int index) {
        Log.d(TAG, "MainService.removeSong(" + index + ")");
        Log.d(TAG, songs.size() + " songs, songIndex=" + playlist.getSongIndex());
        songs.remove(index);
        songIndex = -1;

        if (index < playlist.getSongIndex()) {
            // A song above the current song is removed.
            playlist.decrementSongIndex();
            update();
        } else if (index == playlist.getSongIndex()) {
            // The current song is removed.
            if (index < songs.size()) {
                // Play the next song when the current song is removed, only when currently playing,
                // or playback may start when removing songs while paused.
                if (player.isPlaying()) {
                    prepare();
                } else {
                    update();
                }
            } else {
                // Stop playback when the current and last song is removed.
                prepared = false;
                completed = true;
                stop();
            }
        } else {
            // A song below the current song is removed.
            update();
        }
    }

    public boolean hasPrevious() {
        return songIndex != -1;
    }

    public void restorePrevious() {
        Log.d(TAG, "MainService.restorePrevious()");
        Log.d(TAG, "songIndex=" + songIndex + ", songPosition=" + songPosition);

        playlist.setSongIndex(songIndex);
        playlist.setSongPosition(songPosition);
        songIndex = -1;
        prepare();
    }

    public void shuffle() {
        Log.d(TAG, "MainService.shuffle()");
        Collections.shuffle(songs);

        playlist.setSongIndex(0);
        songIndex = -1;
        prepare();
    }

    public void update() {
        update(false, null);
    }

    public void stop() {
        Log.d(TAG, "MainService.stop()");
        PlaylistActivity.finishIfRunning();
        stopSelf();
    }

    public void setOnUpdateListener(OnUpdateListener onUpdateListener) {
        this.onUpdateListener = onUpdateListener;
    }

    private void loadSettings() {
        volume = settings.getInt(R.string.key_volume, Settings.DEFAULT_VOLUME);
        maxVolume = settings.getXmlInt(R.string.key_max_volume, Settings.DEFAULT_VOLUME);
        resumeOffset = settings.getXmlInt(R.string.key_resume_offset, 0) * 1000;

        Log.d(TAG, "volume=" + volume +
                ", maxVolume=" + maxVolume +
                ", resumeOffset=" + resumeOffset);
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
                    stop();
                }
                return;
            }

            if (playlist.getSongIndex() >= songs.size()) {
                Log.w(TAG, "Invalid song index: " + playlist.getSongIndex());
                playlist.setSongIndex(0);
                playlist.setSongPosition(0);
            }
        }

        savePlaylist();
        this.songs = songs;
        this.playlist = playlist;
        this.songIndex = -1;
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
        songIndex = -1;

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

    private void editSong() {
        Log.d(TAG, "MainService.editSong()");
        startActivity(new Intent(this, EditSongActivity.class)
                .putExtra(EditSongActivity.EXTRA_SONG, getSong())
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

        // Close the status bar (clicking notification buttons won't).
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    /**
     * @param play Used for pausing playback when an incoming call is detected. It is true by
     *             default for the pause/play button and MainReceiver specifies false so playback is
     *             not resumed when an incoming call occurs while paused.
     */
    private void pausePlay(boolean play) {
        Log.d(TAG, "MainService.pausePlay(" + play + ")");
        if (player.isPlaying()) {
            Log.d(TAG, "Pausing");
            player.pause();
            update();
        } else if (play) {
            if (prepared) {
                Log.d(TAG, "Resuming");
                seekTo(player.getCurrentPosition(), false);
                setVolume(); // Volume is sometimes incorrect after playback paused.
                player.start();
                update();
            } else {
                songIndex = -1;
                prepare();
            }
        }
    }

    private void volumeDown() {
        Log.d(TAG, "MainService.volumeDown()");
        if (volume > 1) {
            setVolume(volume - 1);
        }
    }

    private void volumeUp() {
        Log.d(TAG, "MainService.volumeUp()");
        if (volume < maxVolume) {
            setVolume(volume + 1);
        }
    }

    private void savePlaylist() {
        Log.d(TAG, "MainService.savePlaylist()");
        if (songs == null) {
            Log.d(TAG, "No playlist to save");
            return;
        }

        if (prepared) {
            playlist.setSongPosition(player.getCurrentPosition());
        } else if (completed) {
            playlist.setSongIndex(0);
        }
        dbHelper.insertOrUpdatePlaylist(playlist, songs);
    }

    private void setPrevious() {
        Log.d(TAG, "MainService.setPrevious()");
        songIndex = playlist.getSongIndex();
        songPosition = player.getCurrentPosition();
        Log.d(TAG, "songIndex=" + songIndex + ", songPosition=" + songPosition);
    }

    private void prepare() {
        Log.d(TAG, "MainService.prepare()");
        prepared = completed = false;
        try {
            player.reset();
            player.setDataSource(getApplicationContext(), getSong().getContentUri());
            player.prepareAsync();
        } catch (Exception ex) {
            Log.e(TAG, "Error preparing MediaPlayer", ex);
        }
    }

    private void seekTo(int position, boolean always) {
        Log.d(TAG, "seekTo(" + position + ", " + always + ")");
        if (position > 0) {
            if (resumeOffset != 0 || always) {
                position -= resumeOffset;
                if (position < resumeOffset) {
                    position = 0;
                }

                Log.d(TAG, "Seeking to position: " + position +
                        " (resumeOffset=" + resumeOffset + ")");
                player.seekTo(position);
            }
        }
    }

    private void setVolume() {
        float volume = (float)
                (1 - (Math.log(maxVolume + 1 - this.volume) / Math.log(maxVolume)));
        Log.d(TAG, "volume=" + this.volume + ":" + volume);
        player.setVolume(volume, volume);
    }

    private void setVolume(int volume) {
        this.volume = volume;
        if (prepared) {
            setVolume();
        }
        update();
        settings.edit().putInt(R.string.key_volume, volume).apply();
    }

    private void update(boolean loadSettings, Song editedSong) {
        Log.d(TAG, "MainService.update(" + loadSettings + ", " + editedSong + ")");
        Log.d(TAG, songs.size() + " songs, songIndex=" + playlist.getSongIndex());

        // Reload settings when changed.
        if (loadSettings) {
            loadSettings();
            setVolume();
        }

        // Replace any edited song in the playlist if it is not the current song.
        if (editedSong != null) {
            int songIndex = songs.indexOf(editedSong);
            if (songIndex != -1 && songIndex != playlist.getSongIndex()) {
                Log.d(TAG, "Replacing song: " + editedSong + " (index=" + songIndex + ")");
                dbHelper.querySong(editedSong);
                songs.set(songIndex, editedSong);
            }
        }

        // Get current song from playlist.
        Song song = getSong();
        dbHelper.querySong(song);

        // Set song title, artist and tag.
        setTextViewText(true, R.id.tvSongTitle, song.getStyledTitle());
        setTextViewText(true, R.id.tvSongArtist, song.getArtist());
        setTextViewText(false, R.id.tvSongTag, song.getTag());

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
        setTextViewText(false, R.id.tvSongPlaylistNames,
                playlistNames.length() == 0 ? null : playlistNames);

        // Set playlist position with song index, song count, songs left and time left.
        String left = Util.formatDuration(
                Song.getDuration(songs, playlist.getSongIndex()) - player.getCurrentPosition());
        if (hasSongsLeft()) {
            left = (songs.size() - playlist.getSongIndex() - 1) + " / " + left;
        }
        setTextViewText(false, R.id.tvPlaylistPosition, getString(
                R.string.playlist_position, playlist.getSongIndex() + 1, songs.size(), left));

        // Set song year.
        setTextViewText(false, R.id.tvSongYear,
                song.getYear() == 0 ? null : Integer.toString(song.getYear()));

        // Set play/pause image and volume.
        notificationLayoutExpanded.setImageViewResource(R.id.ibPausePlay,
                prepared && player.isPlaying()
                        ? R.drawable.ic_notification_pause
                        : R.drawable.ic_notification_play);
        setTextViewText(true, R.id.tvVolume, Integer.toString(volume));

        // Update notification and PlaylistActivity.
        startForeground(1, notification);
        if (onUpdateListener != null) {
            onUpdateListener.onServiceUpdated();
        }
    }

    private boolean hasSongsLeft() {
        return playlist.getSongIndex() < songs.size() - 1;
    }

    private void setOnClickPendingIntent(boolean unexpanded, int viewId, int action) {
        PendingIntent pendingIntent = PendingIntent.getService(this, action,
                new Intent(this, MainService.class)
                        .putExtra(EXTRA_ACTION, action),
                PendingIntent.FLAG_UPDATE_CURRENT);
        if (unexpanded) {
            notificationLayout.setOnClickPendingIntent(viewId, pendingIntent);
        }
        notificationLayoutExpanded.setOnClickPendingIntent(viewId, pendingIntent);
    }

    private void setTextViewText(boolean unexpanded, int viewId, CharSequence text) {
        if (unexpanded) {
            setTextViewText(notificationLayout, viewId, text);
        }
        setTextViewText(notificationLayoutExpanded, viewId, text);
    }

    private static void setTextViewText(RemoteViews view, int viewId, CharSequence text) {
        if (text == null) {
            view.setViewVisibility(viewId, View.GONE);
        } else {
            view.setTextViewText(viewId, text);
            view.setViewVisibility(viewId, View.VISIBLE);
        }
    }

    public static void update(Context context, boolean loadSettings, Song editedSong) {
        if (running) {
            context.startForegroundService(new Intent(context, MainService.class)
                    .putExtra(EXTRA_ACTION, ACTION_UPDATE)
                    .putExtra(EXTRA_LOAD_SETTINGS, loadSettings)
                    .putExtra(EXTRA_EDITED_SONG, editedSong));
        }
    }

    public static void pause(Context context) {
        if (running) {
            context.startForegroundService(new Intent(context, MainService.class)
                    .putExtra(EXTRA_ACTION, ACTION_PAUSE_PLAY)
                    .putExtra(EXTRA_PLAY, false));
        }
    }

    public class MainBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }

    public interface OnUpdateListener {
        void onServiceUpdated();
    }
}
