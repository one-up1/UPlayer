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

//TODO: MainService impl and controlling from PlaylistActivity.
//TODO: songIndex, time left calc, null checks, which preferences to use.

public class MainService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    public static final String EXTRA_REQUEST_CODE = "com.oneup.extra.REQUEST_CODE";
    public static final String EXTRA_SONGS = "com.oneup.extra.SONGS";
    public static final String EXTRA_SONG_INDEX = "com.oneup.extra.SONG_INDEX";
    public static final String EXTRA_SONG = "com.oneup.extra.SONG";

    public static final int REQUEST_START = 1;
    public static final int REQUEST_PLAY_NEXT = 2;
    public static final int REQUEST_PLAY_LAST = 3;
    public static final int REQUEST_RESTORE_PLAYLIST = 4;

    private static final int REQUEST_PREVIOUS = 5;
    private static final int REQUEST_PLAY_PAUSE = 6;
    private static final int REQUEST_NEXT = 7;
    private static final int REQUEST_STOP = 8;
    private static final int REQUEST_VOLUME_DOWN = 9;
    private static final int REQUEST_VOLUME_UP = 10;

    private static final String TAG = "UPlayer";

    private static final String PREF_VOLUME = "volume";
    private static final int MAX_VOLUME = 100;

    private static final String PREF_POSITION = "position";
    private static final int POSITION_OFFSET = 8000;

    private static final String PREF_SONGS = "songs";
    private static final String PREF_SONG_INDEX = "song_index";

    private final IBinder mainBinder = new MainBinder();

    private DbHelper dbHelper;
    private SharedPreferences preferences;

    private MediaPlayer player;
    private int volume;
    private int position;

    private RemoteViews notificationViews;
    private Notification notification;

    private ArrayList<Song> songs;
    private int songIndex;
    private boolean prepared;

    private MainReceiver mainReceiver;
    private OnDataChangedListener onDataChangedListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mainBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MainService.onCreate()");

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        dbHelper = new DbHelper(this);

        player = new MediaPlayer();
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnErrorListener(this);
        player.setOnCompletionListener(this);

        volume = preferences.getInt(PREF_VOLUME, MAX_VOLUME);

        notificationViews = new RemoteViews(getApplicationContext().getPackageName(),
                R.layout.notification);
        setOnClickPendingIntent(notificationViews, R.id.ibPrevious, REQUEST_PREVIOUS);
        setOnClickPendingIntent(notificationViews, R.id.ibPlayPause, REQUEST_PLAY_PAUSE);
        setOnClickPendingIntent(notificationViews, R.id.ibNext, REQUEST_NEXT);
        setOnClickPendingIntent(notificationViews, R.id.ibStop, REQUEST_STOP);
        setOnClickPendingIntent(notificationViews, R.id.ibVolumeDown, REQUEST_VOLUME_DOWN);
        setOnClickPendingIntent(notificationViews, R.id.ibVolumeUp, REQUEST_VOLUME_UP);

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MainService.onStartCommand()");
        if (intent == null) {
            Log.e(TAG, "No intent");
            return START_STICKY;
        }

        int requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0);
        Log.d(TAG, "requestCode=" + requestCode);

        switch (requestCode) {
            case REQUEST_START:
                savePlaylist();
                songs = intent.getParcelableArrayListExtra(EXTRA_SONGS);
                songIndex = intent.getIntExtra(EXTRA_SONG_INDEX, 0);
                play();
                break;
            case REQUEST_PLAY_NEXT:
                addSong((Song) intent.getParcelableExtra(EXTRA_SONG), true);
                break;
            case REQUEST_PLAY_LAST:
                addSong((Song) intent.getParcelableExtra(EXTRA_SONG), false);
                break;
            case REQUEST_RESTORE_PLAYLIST:
                if (preferences.contains(PREF_SONGS)) {
                    restorePlaylist();
                } else {
                    Log.w(TAG, "No saved playlist found");
                    stop();
                }
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
                stop();
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

        PlaylistActivity.finishIfRunning();
        super.onDestroy();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "MainService.onPrepared()");
        setVolume();
        if (position > POSITION_OFFSET * 2) {
            Log.d(TAG, "Seeking to saved position:" + position);
            player.seekTo(position - POSITION_OFFSET);
            position = 0;
        }
        player.start();

        notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_pause);
        updateViews();

        prepared = true;
    }

    private void updateViews() {
        Log.d(TAG, "MainService.updateViews()");
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

        if (onDataChangedListener != null) {
            onDataChangedListener.onDataChanged();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "MainService.onError(" + what + ", " + extra + ")");
        player.reset();

        notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_play);
        startForeground(1, notification);

        prepared = false;
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "MainService.onCompletion()");
        notificationViews.setImageViewResource(R.id.ibPlayPause, R.drawable.ic_play);
        startForeground(1, notification);
        prepared = false;

        //FIXME: onCompletion not always called or position 0? Only in emulator?
        if (player.getCurrentPosition() == 0) {
            Log.d(TAG, "Current position is 0");
        } else {
            try {
                dbHelper.updateSongPlayed(songs.get(songIndex));
                next();
            } catch (Exception ex) {
                Log.e(TAG, "Error updating song played", ex);
                stopSelf();
            }
        }
    }

    private void setOnClickPendingIntent(RemoteViews views, int viewId, int requestCode) {
        views.setOnClickPendingIntent(viewId, PendingIntent.getService(this, requestCode,
                new Intent(this, MainService.class)
                        .putExtra(EXTRA_REQUEST_CODE, requestCode),
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

        // If we are playing or paused (prepared) just update the notification, start playing the
        // last song if we are not. This will cause the added song to be played immediately, if it
        // is the first song or is being added to a playlist of which the last song has completed.
        if (prepared) {
            updateViews();
        } else {
            songIndex = songs.size() - 1;
            play();
        }
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
        preferences.edit().putInt(PREF_VOLUME, volume).apply();
    }

    private void savePlaylist() {
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
        //TODO: Save current playlist after restoring so restore can be undone?
        try {
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
            //TODO: Saved playlist could be invalid after syncing media store or manually deleting songs.

            songIndex = preferences.getInt(PREF_SONG_INDEX, 0);
            position = preferences.getInt(PREF_POSITION, 0);
            //TODO: Don't start playing after restoring playlist when already done, go to start.
            Log.d(TAG, "Restored playlist with " + songs.size() +
                    " songs, songIndex=" + songIndex + ", position=" + position);
            if (songIndex >= songs.size()) {
                Log.w(TAG, "Invalid song index");
                songIndex = 0;
            }
            play();
        } catch (Exception ex) {
            Log.e(TAG, "Error restoring playlist", ex);
            if (songs == null) {
                stop();
            }
        }
    }

    private void stop() {
        PlaylistActivity.finishIfRunning();
        stopSelf();
    }

    public void setOnDataChangedListener(OnDataChangedListener onDataChangedListener) {
        this.onDataChangedListener = onDataChangedListener;
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

    public void moveSong(int index, int i) {
        Log.d(TAG, "MainService.moveSong(" + index + "," + i + ")");
        int newIndex = index + i;
        Log.d(TAG, "index=" + index + ", newIndex=" + newIndex + ", songIndex=" + songIndex);
        if (newIndex >= 0 && newIndex < songs.size()) {
            songs.add(newIndex, songs.remove(index));
            if (index == songIndex) {
                songIndex = newIndex;
            } else if (newIndex == songIndex) {
                songIndex = index;
            }

            updateViews();
        }
    }

    public void removeSong(int index) {
        Log.d(TAG, "MainService.removeSong(" + index + ")");
        if (songs.size() > 1) {
            songs.remove(index);
            if (index < songIndex) {
                songIndex--;
            } else if (index == songIndex) {
                if (index == songs.size()) {
                    songIndex--;
                }
                play();
            }
            updateViews();
        } else {
            stop();
        }
    }

    public class MainBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }

    public interface OnDataChangedListener {
        void onDataChanged();
    }
}
