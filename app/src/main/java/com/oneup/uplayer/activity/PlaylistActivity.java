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

    private PlaylistFragment playlistFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "PlaylistActivity.onCreate()");
        super.onCreate(savedInstanceState);

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.container);
        setContentView(container);

        if (savedInstanceState == null) {
            playlistFragment = PlaylistFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.container, playlistFragment)
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
        if (instance != null) {
            Log.d(TAG, "Finishing PlaylistActivity");
            instance.finish();
        }
    }

    public static class PlaylistFragment extends SongsListFragment
            implements MainService.OnSongIndexChangedListener {
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
                mainService.setOnSongIndexChangedListener(null);
                getActivity().unbindService(serviceConnection);
                mainService = null;
            }

            super.onDestroy();
        }

        @Override
        public void onSongIndexChanged() {
            Log.d(TAG, "PlaylistActivity.onSongIndexChanged()");
            notifyDataSetChanged();
        }

        @Override
        protected ArrayList<Song> loadData() {
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
            int index = getData().indexOf(song);
            switch (buttonId) {
                case R.id.ibMoveUp:
                    moveSong(index, -1);
                    break;
                case R.id.ibMoveDown:
                    moveSong(index, 1);
                    break;
                case R.id.ibRemove:
                    onSongRemoved(index);
                    setActivityTitle();
                    break;
            }
        }

        @Override
        protected void onSongRemoved(int index) {
            Log.d(TAG, "PlaylistActivity.onSongRemoved(" + index + ")");
            if (mainService != null) {
                mainService.removeSong(index);
            }
        }

        private void moveSong(int position, int i) {
            if (mainService != null && mainService.moveSong(position, i)) {
                notifyDataSetChanged();
            }
        }

        public static PlaylistFragment newInstance() {
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
                mainService.setOnSongIndexChangedListener(PlaylistFragment.this);

                reloadData();
                setSelection(mainService.getSongIndex());
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "PlaylistFragment.onServiceDisconnected()");
                if (mainService != null) {
                    mainService.setOnSongIndexChangedListener(null);
                    mainService = null;
                }
            }
        };
    }
}
