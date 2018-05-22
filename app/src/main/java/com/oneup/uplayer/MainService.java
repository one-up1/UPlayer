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
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    public static final String EXTRA_ACTION = "com.oneup.extra.ACTION";
    public static final String EXTRA_SONGS = "com.oneup.extra.SONGS";
    public static final String EXTRA_SONG_INDEX = "com.oneup.extra.SONG_INDEX";
    public static final String EXTRA_SONG = "com.oneup.extra.SONG";

    public static final int ACTION_PLAY = 1;
    public static final int ACTION_PLAY_NEXT = 2;
    public static final int ACTION_PLAY_LAST = 3;
    public static final int ACTION_RESTORE_PLAYLIST = 4;

    private static final int ACTION_PREVIOUS = 5;
    private static final int ACTION_PLAY_PAUSE = 6;
    private static final int ACTION_NEXT = 7;
    private static final int ACTION_STOP = 8;
    private static final int ACTION_VOLUME_DOWN = 9;
    private static final int ACTION_VOLUME_UP = 10;

    private static final String TAG = "UPlayer";

    private static final String PREF_VOLUME = "volume";
    private static final String PREF_POSITION = "position";
    private static final String PREF_SONGS = "songs";
    private static final String PREF_SONG_INDEX = "song_index";

    private static final int MAX_VOLUME = 100;
    private static final int MIN_RESTORE_POSITION = 16000;
    private static final int RESTORE_POSITION_OFFSET = 8000;

    private static boolean running;

    private final IBinder mainBinder = new MainBinder();

    private SharedPreferences preferences;
    private DbHelper dbHelper;

    private MediaPlayer player;
    private boolean prepared;
    private int volume;
    private int restorePosition;

    private RemoteViews notificationViews;
    private Notification notification;

    private MainReceiver mainReceiver;
    private OnUpdateListener onUpdateListener;

    private ArrayList<Song> songs;
    private int songIndex;

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
        notification = new NotificationCompat.Builder(this, TAG)
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

        running = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MainService.onStartCommand()");
        if (intent == null) {
            Log.e(TAG, "No intent");
            return START_STICKY;
        }

        int action = intent.getIntExtra(EXTRA_ACTION, 0);
        switch (action) {
            case ACTION_PLAY:
                savePlaylist();
                songs = intent.getParcelableArrayListExtra(EXTRA_SONGS);
                play(intent.getIntExtra(EXTRA_SONG_INDEX, 0));
                break;
            case ACTION_PLAY_NEXT:
                addSong((Song) intent.getParcelableExtra(EXTRA_SONG), true);
                break;
            case ACTION_PLAY_LAST:
                addSong((Song) intent.getParcelableExtra(EXTRA_SONG), false);
                break;
            case ACTION_RESTORE_PLAYLIST:
                restorePlaylist();
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
                Log.e(TAG, "Invalid action: " + action);
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
            Log.d(TAG, "MediaPlayer released");
        }

        if (mainReceiver != null) {
            unregisterReceiver(mainReceiver);
        }

        if (dbHelper != null) {
            dbHelper.close();
        }

        running = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "MainService.onBind()");
        return mainBinder;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "MainService.onPrepared()");
        
        setVolume();
        if (restorePosition > MIN_RESTORE_POSITION) {
            Log.d(TAG, "Seeking to saved position: " + restorePosition);
            player.seekTo(restorePosition - RESTORE_POSITION_OFFSET);
            restorePosition = 0;
        }
        
        player.start();
        prepared = true;

        notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_pause);
        update();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "MainService.onError(" + what + ", " + extra + ")");
        player.reset();
        prepared = false;
        
        notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_play);
        startForeground(1, notification);
        
        return true; // Or onCompletion() will be called.
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "MainService.onCompletion()");
        prepared = false;
        
        notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_play);
        startForeground(1, notification);

        //FIXME: onCompletion not always called or position 0? Only in emulator?
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
        Log.d(TAG, "MainService.play(" + songIndex + ")");
        this.songIndex = songIndex;
        play();
    }

    public void moveSong(int index, int toIndex) {
        Log.d(TAG, "MainService.moveSong(" + index + "," + toIndex + "), songIndex=" + songIndex);
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

    private void addSong(Song song, boolean next) {
        Log.d(TAG, "MainService.addSong(" + song + ", " + next + ")");
        if (songs == null) {
            songs = new ArrayList<>();
            songs.add(song);
        } else if (next) {
            songs.add(songIndex + 1, song);
        } else {
            songs.add(song);
        }

        // Update when playing or paused (prepared), start playing the last song when not.
        // This will start playback of the added song, if it is the first song
        // or is being added to a playlist of which the last song has completed.
        if (prepared) {
            update();
        } else {
            play(songs.size() - 1);
        }
    }

    //TODO: Save playlist to DB and ability to save multiple playlists.
    private void savePlaylist() {
        Log.d(TAG, "MainService.savePlaylist()");
        if (songs == null || songs.size() == 0) {
            Log.d(TAG, "No playlist to save");
            return;
        }

        try {
            JSONArray playlistSongs = new JSONArray();
            for (Song song : songs) {
                JSONObject playlistSong = new JSONObject();
                playlistSong.put(Song._ID, song.getId());
                playlistSong.put(Song.TITLE, song.getTitle());
                playlistSong.put(Song.ARTIST_ID, song.getArtistId());
                playlistSong.put(Song.ARTIST, song.getArtist());
                playlistSong.put(Song.DURATION, song.getDuration());
                playlistSong.put(Song.TIMES_PLAYED, song.getTimesPlayed());
                playlistSongs.put(playlistSong);
            }

            preferences.edit()
                    .putString(PREF_SONGS, playlistSongs.toString())
                    .putInt(PREF_SONG_INDEX, songIndex)
                    .putInt(PREF_POSITION, player.getCurrentPosition())
                    .apply();
            Log.d(TAG, "Playlist saved");
        } catch (Exception ex) {
            Log.e(TAG, "Error saving playlist", ex);
        }
    }

    private void restorePlaylist() {
        Log.d(TAG, "MainService.restorePlaylist()");
        if (!preferences.contains(PREF_SONGS)) {
            Log.e(TAG, "No saved playlist found");
            stop();
            return;
        }

        try {
            //FIXME: Saved playlist could be invalid after syncing media store or manually deleting songs. Saving playlist after deleting last song from PlaylistActivity?
            JSONArray playlistSongs = new JSONArray(preferences.getString(PREF_SONGS, null));
            songs = new ArrayList<>();
            for (int i = 0; i < playlistSongs.length(); i++) {
                JSONObject playlistSong = playlistSongs.getJSONObject(i);
                Song song = new Song();
                song.setId(playlistSong.getLong(Song._ID));
                song.setTitle(playlistSong.getString(Song.TITLE));
                song.setArtistId(playlistSong.getLong(Song.ARTIST_ID));
                song.setArtist(playlistSong.getString(Song.ARTIST));
                song.setDuration(playlistSong.getLong(Song.DURATION));
                song.setTimesPlayed(playlistSong.getInt(Song.TIMES_PLAYED));
                songs.add(song);
            }
            songIndex = preferences.getInt(PREF_SONG_INDEX, 0);
            restorePosition = preferences.getInt(PREF_POSITION, 0);
            
            Log.d(TAG, "Restored playlist with " + songs.size() +
                    " songs, songIndex=" + songIndex + ", position=" + restorePosition);
            if (songIndex >= songs.size()) {
                Log.e(TAG, "Invalid song index");
                songIndex = 0;
            }
            play();

            //TODO: Don't start playing after restoring playlist when already done, go to start.
        } catch (Exception ex) {
            Log.e(TAG, "Error restoring playlist", ex);
            if (songs == null) {
                stop();
            }
        }
    }

    private void previous() {
        Log.d(TAG, "MainService.previous()");
        if (songIndex > 0) {
            play(songIndex - 1);
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
        Log.d(TAG, "MainService.setVolume(" + volume + ")");
        this.volume = volume;

        setVolume();
        preferences.edit().putInt(PREF_VOLUME, volume).apply();
    }

    private void setVolume() {
        Log.d(TAG, "MainService.setVolume()");
        float volume = (float)
                (1 - (Math.log(MAX_VOLUME + 1 - this.volume) / Math.log(MAX_VOLUME)));
        Log.d(TAG, "volume=" + this.volume + " (" + volume + ")");
        player.setVolume(volume, volume);

        notificationViews.setTextViewText(R.id.tvVolume, Integer.toString(this.volume));
        startForeground(1, notification);
    }

    //TODO: Only set needed values in update() and method for setting button icons.
    private void update() {
        Log.d(TAG, "MainService.update()");
        Song song = songs.get(songIndex);

        notificationViews.setTextViewText(R.id.tvSongTitle, song.getTitle());
        notificationViews.setTextViewText(R.id.tvSongArtist, song.getArtist());

        String left = Util.formatDuration(Song.getDuration(songs, songIndex));
        if (songIndex < songs.size() - 1) {
            left += " / " + Util.formatDuration(Song.getDuration(songs, songIndex + 1));
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
        Log.d(TAG, "MainService.stop()");
        PlaylistActivity.finishIfRunning();
        stopSelf();
    }

    public static boolean isRunning() {
        return running;
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
