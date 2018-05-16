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
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.fragment.SongsListFragment;

import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity {
    private static final String TAG = "UPlayer";

    private static PlaylistActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "PlaylistActivity.onCreate()");
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
        Log.d(TAG, "PlaylistActivity.onDestroy()");
        instance = null;
        super.onDestroy();
    }

    public static void finishIfRunning() {
        Log.d(TAG, "PlaylistActivity.finishIfRunning()");
        if (instance != null) {
            Log.d(TAG, "Finishing PlaylistActivity");
            instance.finish();
        }
    }

    public static class PlaylistFragment extends SongsListFragment
            implements MainService.OnUpdateListener {
        private MainService mainService;

        public PlaylistFragment() {
            super(R.layout.list_item_playlist);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.d(TAG, "PlaylistFragment.onCreate()");
            super.onCreate(savedInstanceState);

            getActivity().bindService(new Intent(getActivity(), MainService.class),
                    serviceConnection, BIND_AUTO_CREATE);
        }

        @Override
        public void onDestroy() {
            if (mainService != null) {
                Log.d(TAG, "Unbinding service");
                mainService.setOnUpdateListener(null);
                getActivity().unbindService(serviceConnection);
                mainService = null;
            }

            super.onDestroy();
        }

        @Override
        public void onUpdate() {
            Log.d(TAG, "PlaylistFragment.onUpdate()");
            notifyDataSetChanged();
        }

        @Override
        protected ArrayList<Song> loadData() {
            Log.d(TAG, "PlaylistFragment.loadData()");
            return mainService.getSongs();
        }

        @Override
        protected void setListItemViews(View rootView, int position, Song song) {
            super.setListItemViews(rootView, position, song);

            if (mainService != null && position == mainService.getSongIndex()) {
                TextView tvTitle = rootView.findViewById(R.id.tvTitle);
                SpannableString underlinedText = new SpannableString(tvTitle.getText());
                underlinedText.setSpan(new UnderlineSpan(), 0, underlinedText.length(), 0);
                tvTitle.setText(underlinedText);
            }

            setListItemButton(rootView, R.id.ibMoveUp, song);
            setListItemButton(rootView, R.id.ibMoveDown, song);
            setListItemButton(rootView, R.id.ibRemove, song);
        }

        @Override
        protected void onListItemClick(int position, Song song) {
            if (mainService != null) {
                mainService.setSongIndex(position);
            }
        }

        @Override
        protected void onListItemButtonClick(int buttonId, Song song) {
            int songIndex = getData().indexOf(song);
            switch (buttonId) {
                case R.id.ibMoveUp:
                    moveUp(songIndex);
                    break;
                case R.id.ibMoveDown:
                    moveDown(songIndex);
                    break;
                case R.id.ibRemove:
                    onSongRemoved(songIndex);
                    break;
            }
        }

        @Override
        protected void onSongRemoved(int index) {
            Log.d(TAG, "PlaylistFragment.onSongRemoved()");
            if (mainService != null) {
                if (mainService.getSongs().size() > 1) {
                    mainService.removeSong(index);
                } else {
                    getActivity().finish();
                    getActivity().stopService(new Intent(getActivity(), MainService.class));
                }
            }
        }

        private void moveUp(int songIndex) {
            Log.d(TAG, "PlaylistFragment.moveUp(" + songIndex + ")");
            if (mainService != null && songIndex > 0) {
                mainService.moveSong(songIndex, songIndex - 1);
            }
        }

        private void moveDown(int songIndex) {
            Log.d(TAG, "PlaylistFragment.moveDown(" + songIndex + ")");
            if (mainService != null && songIndex < mainService.getSongs().size() - 1) {
                mainService.moveSong(songIndex, songIndex + 1);
            }
        }

        private static PlaylistFragment newInstance() {
            return new PlaylistFragment();
        }

        private ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "PlaylistFragment.onServiceConnected()");
                if (getActivity().isFinishing()) {
                    Log.d(TAG, "Finishing");
                    return;
                }

                MainService.MainBinder binder = (MainService.MainBinder) service;
                mainService = binder.getService();
                mainService.setOnUpdateListener(PlaylistFragment.this);

                reloadData();
                setSelection(mainService.getSongIndex());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "PlaylistFragment.onServiceDisconnected()");
                if (mainService != null) {
                    mainService.setOnUpdateListener(null);
                    mainService = null;
                }
            }
        };
    }
}
