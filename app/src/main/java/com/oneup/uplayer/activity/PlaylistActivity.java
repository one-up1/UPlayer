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
import com.oneup.uplayer.fragment.BaseSongsFragment;

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

    public static class PlaylistFragment extends BaseSongsFragment
            implements MainService.OnSongIndexChangedListener, View.OnClickListener {
        private MainService mainService;

        public PlaylistFragment() {
            super(R.layout.list_item_playlist);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            Log.d(TAG, "PlaylistFragment.onCreate()");
            super.onCreate(savedInstanceState);

            setViewArtistOrderBy(Song.TITLE);
            getActivity().bindService(new Intent(getContext(), MainService.class), serviceConnection,
                    Context.BIND_AUTO_CREATE);
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
        public void onClick(View v) {
            Song song = (Song) v.getTag();
            switch (v.getId()) {
                case R.id.ibMoveUp:
                    moveSong(song, -1);
                    break;
                case R.id.ibMoveDown:
                    moveSong(song, 1);
                    break;
                case R.id.ibDelete:
                    onSongDeleted(song);
                    notifyDataSetChanged();
                    break;
            }
        }

        @Override
        protected void setListViewViews(View rootview, int position, Song song) {
            if (mainService != null && position == mainService.getSongIndex()) {
                TextView tvTitle = rootview.findViewById(R.id.tvTitle);
                SpannableString underlinedText = new SpannableString(tvTitle.getText());
                underlinedText.setSpan(new UnderlineSpan(), 0, underlinedText.length(), 0);
                tvTitle.setText(underlinedText);
            }

            ImageButton ibMoveUp = rootview.findViewById(R.id.ibMoveUp);
            ibMoveUp.setOnClickListener(this);
            ibMoveUp.setTag(song);

            ImageButton ibMoveDown = rootview.findViewById(R.id.ibMoveDown);
            ibMoveDown.setOnClickListener(this);
            ibMoveDown.setTag(song);

            ImageButton ibDelete = rootview.findViewById(R.id.ibDelete);
            ibDelete.setId(R.id.ibDelete);
            ibDelete.setOnClickListener(this);
            ibDelete.setTag(song);
        }

        @Override
        protected void onListViewItemClick(int position) {
            if (mainService != null) {
                mainService.setSongIndex(position);
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

                setSongs(mainService.getSongs());
                setListViewSelection(mainService.getSongIndex());
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
