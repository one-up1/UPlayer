package com.oneup.uplayer.activity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
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
import com.oneup.uplayer.widget.EditText;

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
        private MainService mainService;

        public PlaylistFragment() {
            super(R.layout.list_item_playlist, 0, 0, null);
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
            if (mainService != null) {
                mainService.setOnUpdateListener(null);
                getActivity().unbindService(serviceConnection);
                mainService = null;
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
                case R.id.save:
                    final EditText etName = new EditText(getActivity());
                    etName.setHint(R.string.name);
                    etName.setSelectAllOnFocus(true);

                    Playlist playlist = mainService == null ? null : mainService.getPlaylist();
                    if (playlist != null) {
                        etName.setText(playlist.getName());
                    }

                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.save_playlist)
                            .setView(etName)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (mainService != null) {
                                        Playlist playlist = mainService.getPlaylist();
                                        if (playlist == null) {
                                            playlist = new Playlist();
                                            mainService.setPlaylist(playlist);
                                        }
                                        String name = etName.getString();
                                        playlist.setName(name.length() == 0 ? null : name);
                                        Util.showToast(getActivity(), R.string.ok);
                                    }
                                }
                            })
                            .show();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        @Override
        public void onUpdate() {
            notifyDataSetChanged();
        }

        @Override
        protected ArrayList<Song> loadData() {
            Log.d(TAG, "PlaylistFragment.loadData()");
            return mainService.getSongs();
        }

        @Override
        protected void setListItemContent(View rootView, int position, Song song) {
            super.setListItemContent(rootView, position, song);

            if (mainService != null && position == mainService.getSongIndex()) {
                TextView tvTitle = rootView.findViewById(R.id.tvTitle);
                SpannableString underlinedText = new SpannableString(tvTitle.getText());
                underlinedText.setSpan(new UnderlineSpan(), 0, underlinedText.length(), 0);
                tvTitle.setText(underlinedText);
            }

            setListItemButton(rootView, R.id.ibMoveUp);
            setListItemButton(rootView, R.id.ibMoveDown);
            setListItemButton(rootView, R.id.ibRemove);
        }

        @Override
        protected void onListItemClick(int position, Song song) {
            if (mainService != null) {
                mainService.play(position);
            }
        }

        @Override
        protected void onListItemButtonClick(int buttonId, int position, Song song) {
            switch (buttonId) {
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
            if (mainService != null) {
                if (getCount() > 1) {
                    mainService.removeSong(index);
                } else {
                    getActivity().finish();
                    getActivity().stopService(new Intent(getActivity(), MainService.class));
                }
            }
        }

        private void moveUp(int songIndex) {
            if (mainService != null && songIndex > 0) {
                mainService.moveSong(songIndex, songIndex - 1);
            }
        }

        private void moveDown(int songIndex) {
            if (mainService != null && songIndex < getCount() - 1) {
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
                    Log.w(TAG, "Finishing");
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
