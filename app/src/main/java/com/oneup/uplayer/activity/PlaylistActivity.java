package com.oneup.uplayer.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.fragment.SongsListFragment;
import com.oneup.uplayer.util.Util;
import com.oneup.util.Utils;

import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity {
    private static final String TAG = "UPlayer";

    private static PlaylistActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.container);
        setContentView(container);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, PlaylistFragment.newInstance())
                    .commit();
        }

        instance = this;
    }

    @Override
    protected void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    public static void finishIfRunning() {
        if (instance != null) {
            instance.finish();
        }
    }

    public static class PlaylistFragment extends SongsListFragment
            implements MainService.OnUpdateListener {
        private MainService service;
        private int moveIndex;

        public PlaylistFragment() {
            super(R.layout.list_item_playlist_song, 0, 0, 0, null);
            moveIndex = -1;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.d(TAG, "PlaylistFragment.onCreate()");
            super.onCreate(savedInstanceState);

            getActivity().bindService(new Intent(getActivity(), MainService.class),
                    serviceConnection, BIND_AUTO_CREATE);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.fragment_playlist, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            super.onPrepareOptionsMenu(menu);
            menu.findItem(R.id.restore_previous).setVisible(
                    service != null && service.hasPrevious());
        }

        @Override
        public void onDestroy() {
            Log.d(TAG, "PlaylistActivity.onDestroy()");
            if (service != null) {
                service.setOnUpdateListener(null);
                getActivity().unbindService(serviceConnection);
                service = null;
            }
            super.onDestroy();
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                                        ContextMenu.ContextMenuInfo menuInfo) {
            getActivity().getMenuInflater().inflate(R.menu.list_item_playlist_song, menu);
            super.onCreateContextMenu(menu, v, menuInfo);
        }

        @Override
        public void onServiceUpdated() {
            getActivity().invalidateOptionsMenu();
            notifyDataSetChanged();
        }

        @Override
        protected ArrayList<Song> loadData() {
            Log.d(TAG, "PlaylistFragment.loadData()");
            return service.getSongs();
        }

        @Override
        protected String getActivityTitle() {
            return super.getActivityTitle() + ", " +
                    Util.formatDuration(Song.getDuration(getData(), 0));
        }

        @Override
        protected void setListItemContent(View rootView, int position, Song song) {
            super.setListItemContent(rootView, position, song);

            // Mark the current song.
            if (position == service.getPlaylist().getSongIndex()) {
                TextView tvTitle = rootView.findViewById(R.id.tvTitle);
                tvTitle.setText(Util.underline(tvTitle.getText()));
            }

            // Mark the song that is being moved.
            rootView.setBackgroundResource(position == moveIndex ? R.drawable.border : 0);

            // Set move up/down and remove buttons.
            setListItemViewOnClickListener(rootView, R.id.ibMoveUp);
            setListItemViewOnClickListener(rootView, R.id.ibMoveDown);
            setListItemViewOnClickListener(rootView, R.id.ibRemove);
        }

        @Override
        protected void onListItemClick(int position, Song song) {
            if (moveIndex == -1) {
                service.play(position);
            } else {
                if (moveIndex == position) {
                    Log.d(TAG, "Canceling move");
                    moveIndex = -1;
                    notifyDataSetChanged();
                } else {
                    int moveIndex = this.moveIndex;
                    this.moveIndex = -1;
                    service.moveSong(moveIndex, position);
                }
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.mark_played) {
                final Song song = service.getSong();
                Utils.showConfirmDialog(getActivity(), (dialog, which) -> {
                    try {
                        service.next(true, true);
                        Utils.showToast(getActivity(), R.string.times_played,
                                song.getTimesPlayed());
                    } catch (Exception ex) {
                        Log.e(TAG, "Error marking song played", ex);
                        Utils.showErrorDialog(getActivity(), ex);
                    }
                }, R.string.app_name, R.string.mark_played_confirm, song);
            } else if (id == R.id.restore_previous) {
                service.restorePrevious();
            } else if (id == R.id.shuffle) {
                service.shuffle();
            } else {
                return super.onOptionsItemSelected(item);
            }
            return true;
        }

        @Override
        protected void onContextItemSelected(int itemId, int position, Song song) {
            if (itemId == R.id.move) {
                Log.d(TAG, "Moving " + position + " (" + song + ")");
                moveIndex = position;
                notifyDataSetChanged();
            } else {
                super.onContextItemSelected(itemId, position, song);
            }
        }

        @Override
        protected void onListItemViewClick(int viewId, int position, Song song) {
            moveIndex = -1;
            if (viewId == R.id.ibMoveUp) {
                if (position > 0) {
                    service.moveSong(position, position - 1);
                }
            } else if (viewId == R.id.ibMoveDown) {
                if (position < getCount() - 1) {
                    service.moveSong(position, position + 1);
                }
            } else if (viewId == R.id.ibRemove) {
                removeListItem(position);
            }
        }

        @Override
        protected void removeListItem(int index) {
            moveIndex = -1;
            service.removeSong(index);
        }

        @Override
        protected void onPlaylistSelected(Playlist playlist) {
            service.setPlaylist(playlist);
        }

        @Override
        protected void onSongUpdated(Song song) {
            service.update();
        }

        private static PlaylistFragment newInstance() {
            return new PlaylistFragment();
        }

        private final ServiceConnection serviceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
                Log.d(TAG, "PlaylistFragment.onServiceConnected()");
                if (getActivity().isFinishing()) {
                    Log.w(TAG, "Finishing");
                    return;
                }

                MainService.MainBinder mainBinder = (MainService.MainBinder) serviceBinder;
                service = mainBinder.getService();
                service.update();
                service.setOnUpdateListener(PlaylistFragment.this);

                reloadData();
                setSelection(service.getPlaylist().getSongIndex());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "PlaylistFragment.onServiceDisconnected()");
                if (service != null) {
                    service.setOnUpdateListener(null);
                    service = null;
                }
            }
        };
    }
}
