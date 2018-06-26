package com.oneup.uplayer.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.fragment.SongsListFragment;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity {
    private static final String TAG = "UPlayer";

    private static PlaylistActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        private static final int REQUEST_SELECT_PLAYLIST = 100;

        public PlaylistFragment() {
            super(R.layout.list_item_playlist_song, 0, 0, null);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.d(TAG, "PlaylistFragment.onCreate()");
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            getActivity().bindService(new Intent(getActivity(), MainService.class),
                    serviceConnection, BIND_AUTO_CREATE);
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
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.fragment_playlist, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.savePlaylist:
                    startActivityForResult(new Intent(getActivity(), PlaylistsActivity.class)
                                    .putExtras(PlaylistsActivity.PlaylistsFragment.getArguments(
                                            null, null, false, true)),
                            REQUEST_SELECT_PLAYLIST);
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == AppCompatActivity.RESULT_OK) {
                switch (requestCode) {
                    case REQUEST_SELECT_PLAYLIST:
                        service.setPlaylist((Playlist) data.getParcelableExtra(
                                PlaylistsActivity.EXTRA_PLAYLIST));
                        Util.showToast(getActivity(), R.string.playlist_saved);
                        break;
                }
            }
        }

        @Override
        public void onUpdate() {
            notifyDataSetChanged();
        }

        @Override
        protected ArrayList<Song> loadData() {
            Log.d(TAG, "PlaylistFragment.loadData()");
            return service.getSongs();
        }

        @Override
        protected void setListItemContent(View rootView, int position, Song song) {
            super.setListItemContent(rootView, position, song);

            if (service != null && position == service.getSongIndex()) {
                TextView tvTitle = rootView.findViewById(R.id.tvTitle);
                SpannableString underlinedText = new SpannableString(tvTitle.getText());
                underlinedText.setSpan(new UnderlineSpan(), 0, underlinedText.length(), 0);
                tvTitle.setText(underlinedText);
            }

            setListItemViewOnClickListener(rootView, R.id.ibMoveUp);
            setListItemViewOnClickListener(rootView, R.id.ibMoveDown);
            setListItemViewOnClickListener(rootView, R.id.ibRemove);
        }

        @Override
        protected void onListItemClick(int position, Song song) {
            if (service != null) {
                service.setSongIndex(position);
            }
        }

        @Override
        protected void onListItemViewClick(int viewId, int position, Song song) {
            switch (viewId) {
                case R.id.ibMoveUp:
                    moveUp(position);
                    break;
                case R.id.ibMoveDown:
                    moveDown(position);
                    break;
                case R.id.ibRemove:
                    onSongRemoved(position);
                    break;
            }
        }

        @Override
        protected void onSongRemoved(int index) {
            if (service != null) {
                if (getCount() > 1) {
                    service.removeSong(index);
                } else {
                    getActivity().finish();
                    getActivity().stopService(new Intent(getActivity(), MainService.class));
                }
            }
        }

        private void moveUp(int songIndex) {
            if (service != null && songIndex > 0) {
                service.moveSong(songIndex, songIndex - 1);
            }
        }

        private void moveDown(int songIndex) {
            if (service != null && songIndex < getCount() - 1) {
                service.moveSong(songIndex, songIndex + 1);
            }
        }

        private static PlaylistFragment newInstance() {
            return new PlaylistFragment();
        }

        private ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
                Log.d(TAG, "PlaylistFragment.onServiceConnected()");
                if (getActivity().isFinishing()) {
                    Log.w(TAG, "Finishing");
                    return;
                }

                MainService.MainBinder mainBinder = (MainService.MainBinder) serviceBinder;
                service = mainBinder.getService();
                service.setOnUpdateListener(PlaylistFragment.this);

                reloadData();
                setSelection(service.getSongIndex());
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
