package com.oneup.uplayer.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
        super.onCreate(savedInstanceState);
        Log.d(TAG, "PlaylistActivity.onCreate()");

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

   /* private void setTitle() {
        setTitle(getString(R.string.song_count_duration, mainService.getSongs().size(),
                Util.formatDuration(Song.getDuration(mainService.getSongs(), 0))));
    }*/

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
            Log.d(TAG, "PlaylistFragment()");
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.d(TAG, "PlaylistFragment.onCreate()");

            setViewArtistOrderBy(Song.TITLE);
            getActivity().bindService(new Intent(getContext(), MainService.class), serviceConnection,
                    Context.BIND_AUTO_CREATE);
        }

        @Override
        public void onDestroy() {
            Log.d(TAG, "PlaylistFragment.onDestroy()");

            if (mainService != null) {
                Log.d(TAG, "Unbinding service");
                mainService.setOnSongIndexChangedListener(null);
                getActivity().unbindService(serviceConnection);
                mainService = null;
            }

            super.onDestroy();
        }

        @Override
        protected ArrayList<Song> getData() {
            Log.d(TAG, "PlaylistFragment.getData()");
            return mainService == null ? null : mainService.getSongs();
        }

        @Override
        protected void setRowViews(View rootView, int position, Song song) {
            super.setRowViews(rootView, position, song);

            if (mainService != null && position == mainService.getSongIndex()) {
                TextView tvTitle = rootView.findViewById(R.id.tvTitle);
                SpannableString underlinedText = new SpannableString(tvTitle.getText());
                underlinedText.setSpan(new UnderlineSpan(), 0, underlinedText.length(), 0);
                tvTitle.setText(underlinedText);
            }

            setButton(rootView, R.id.ibMoveUp, song);
            setButton(rootView, R.id.ibMoveDown, song);
            setButton(rootView, R.id.ibDelete, song);
        }

        @Override
        protected void onListItemClick(int position, Song song) {
            if (mainService != null) {
                mainService.setSongIndex(position);
            }
        }

        @Override
        protected void onButtonClick(int id, Song song) {
            switch (id) {
                case R.id.ibMoveUp:
                    moveSong(song, -1);
                    break;
                case R.id.ibMoveDown:
                    moveSong(song, 1);
                    break;
                case R.id.ibDelete:
                    onSongDeleted(song);
                    break;
            }
        }

        @Override
        protected void onSongDeleted(Song song) {
            Log.d(TAG, "PlaylistActivity.onSongDeleted(" + song + ")");
            if (mainService != null) {
                mainService.deleteSong(song);
                notifyDataSetChanged();
                //setTitle();
            }
        }

        @Override
        public void onSongIndexChanged() {
            Log.d(TAG, "PlaylistActivity.onSongIndexChanged()");
            notifyDataSetChanged();
        }

        private void moveSong(Song song, int i) {
            if (mainService != null && mainService.moveSong(song, i)) {
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

                loadData();
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
