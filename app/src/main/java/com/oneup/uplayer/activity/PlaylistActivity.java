package com.oneup.uplayer.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.widget.SongAdapter;
import com.oneup.uplayer.widget.SongsListView;

public class PlaylistActivity extends AppCompatActivity implements AdapterView.OnItemClickListener,
        SongsListView.OnSongDeletedListener, MainService.OnSongIndexChangedListener {
    private static final String TAG = "UPlayer";

    private static PlaylistActivity instance;

    private SongsListView listView;
    private SongAdapter songsAdapter;

    private MainService mainService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listView = new SongsListView(this);
        listView.setOnItemClickListener(this);
        listView.setOnSongDeletedListener(this);
        setContentView(listView);
        registerForContextMenu(listView);

        instance = this;
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "PlaylistActivity.onStart()");
        super.onStart();

        if (mainService == null) {
            Log.d(TAG, "Binding service");
            bindService(new Intent(this, MainService.class), serviceConnection,
                    Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v == listView) {
            getMenuInflater().inflate(R.menu.list_item_song, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return listView.onContextItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "PlaylistActivity.onActivityResult(" + requestCode + ", " + resultCode + ")");
        listView.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "PlaylistActivity.onStop()");
        if (mainService != null) {
            mainService.setOnSongIndexChangedListener(null);
            unbindService(serviceConnection);
            mainService = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "PlaylistActivity.onDestroy()");
        instance = null;
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == listView) {
            if (mainService != null) {
                mainService.setSongIndex(position);
            }
        }
    }

    @Override
    public void onSongDeleted(Song song) {
        Log.d(TAG, "PlaylistActivity.onSongDeleted(" + song + ")");
        if (mainService != null) {
            mainService.deleteSong(song);
            setTitle();
            songsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onSongIndexChanged() {
        songsAdapter.notifyDataSetChanged();
    }

    private void setTitle() {
        setTitle(getString(R.string.song_count_duration, mainService.getSongs().size(),
                Util.formatDuration(Song.getDuration(mainService.getSongs(), 0))));
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "PlaylistActivity.serviceConnection.onServiceConnected()");
            if (isFinishing()) {
                Log.d(TAG, "Finishing");
                return;
            }

            MainService.MainBinder binder = (MainService.MainBinder) service;
            mainService = binder.getService();
            mainService.setOnSongIndexChangedListener(PlaylistActivity.this);

            setTitle();
            songsAdapter = new ListAdapter();
            listView.setAdapter(songsAdapter);
            listView.setSelection(mainService.getSongIndex());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "PlaylistActivity.serviceConnection.onServiceDisconnected()");
            if (mainService != null) {
                mainService.setOnSongIndexChangedListener(null);
                mainService = null;
            }
        }
    };

    private class ListAdapter extends SongAdapter implements View.OnClickListener {
        private ListAdapter() {
            super(PlaylistActivity.this, mainService.getSongs());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View ret = super.getView(position, convertView, parent);

            if (mainService != null && position == mainService.getSongIndex()) {
                TextView tvSongTitle = ret.findViewById(R.id.tvTitle);
                SpannableString underlinedText = new SpannableString(tvSongTitle.getText());
                underlinedText.setSpan(new UnderlineSpan(), 0, underlinedText.length(), 0);
                tvSongTitle.setText(underlinedText);
            }

            return ret;
        }

        @Override
        public void addButtons(RelativeLayout rlButtons) {
            RelativeLayout.LayoutParams params;

            ImageButton ibMoveUp = new ImageButton(PlaylistActivity.this);
            ibMoveUp.setId(R.id.ibMoveUp);
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 60);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            ibMoveUp.setLayoutParams(params);
            ibMoveUp.setImageResource(R.drawable.ic_move_up);
            ibMoveUp.setContentDescription(getString(R.string.move_up));
            ibMoveUp.setOnClickListener(this);
            rlButtons.addView(ibMoveUp);

            ImageButton ibMoveDown = new ImageButton(PlaylistActivity.this);
            ibMoveDown.setId(R.id.ibMoveDown);
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 60);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            ibMoveDown.setLayoutParams(params);
            ibMoveDown.setImageResource(R.drawable.ic_move_down);
            ibMoveDown.setContentDescription(getString(R.string.move_down));
            ibMoveDown.setOnClickListener(this);
            rlButtons.addView(ibMoveDown);

            ImageButton ibDelete = new ImageButton(PlaylistActivity.this);
            ibDelete.setId(R.id.ibDelete);
            params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.END_OF, R.id.ibMoveUp);
            ibDelete.setLayoutParams(params);
            ibDelete.setImageResource(R.drawable.ic_delete);
            ibDelete.setContentDescription(getString(R.string.delete));
            ibDelete.setOnClickListener(this);
            rlButtons.addView(ibDelete);
        }

        @Override
        public void setButtons(View view, Song song) {
            ImageButton ibMoveUp = view.findViewById(R.id.ibMoveUp);
            ibMoveUp.setTag(song);

            ImageButton ibMoveDown = view.findViewById(R.id.ibMoveDown);
            ibMoveDown.setTag(song);

            ImageButton ibDelete = view.findViewById(R.id.ibDelete);
            ibDelete.setTag(song);
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
                    break;
            }
        }

        private void moveSong(Song song, int i) {
            if (mainService != null && mainService.moveSong(song, i)) {
                songsAdapter.notifyDataSetChanged();
            }
        }
    }

    public static void finishIfRunning() {
        if (instance != null) {
            Log.d(TAG, "Finishing PlaylistActivity");
            instance.finish();
        }
    }
}
