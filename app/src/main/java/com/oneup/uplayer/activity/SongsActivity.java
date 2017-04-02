package com.oneup.uplayer.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.SongAdapter;
import com.oneup.uplayer.obj.Song;

import java.util.ArrayList;

public class SongsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    public static final String ARG_URI = "uri";
    public static final String ARG_ID_COLUMN = "id_column";
    public static final String ARG_SELECTION = "selection";
    public static final String ARG_SELECTION_ARGS = "selection_args";
    public static final String ARG_SORT_ORDER = "sort_order";

    private static final String TAG = "UPlayer";

    private ListView lvSongs;
    private ArrayList<Song> songs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songs);

        Intent intent = getIntent();
        Uri uri = intent.getParcelableExtra(ARG_URI);
        String idColumn = intent.getStringExtra(ARG_ID_COLUMN);
        String selection = intent.getStringExtra(ARG_SELECTION);
        String[] selectionArgs = intent.getStringArrayExtra(ARG_SELECTION_ARGS);
        String sortOrder = intent.getStringExtra(ARG_SORT_ORDER);

        try {
            Cursor c = getContentResolver().query(uri, new String[] { idColumn,
                    MediaStore.Audio.AudioColumns.TITLE,
                    MediaStore.Audio.AudioColumns.ARTIST,
                    MediaStore.Audio.AudioColumns.YEAR }, selection, selectionArgs, sortOrder);
            if (c != null) {
                try {
                    songs = new ArrayList<>();
                    int iId = c.getColumnIndex(idColumn);
                    int iTitle = c.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE);
                    int iArtist = c.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
                    int iYear = c.getColumnIndex(MediaStore.Audio.AudioColumns.YEAR);
                    while (c.moveToNext()) {
                        songs.add(new Song(c.getLong(iId), c.getString(iTitle),
                                c.getString(iArtist), c.getInt(iYear)));
                    }
                } finally {
                    c.close();
                }

                Log.d(TAG, "Queried " + songs.size() + " songs");
                setTitle(getResources().getQuantityString(R.plurals.songs, songs.size(), songs.size()));

                lvSongs = (ListView) findViewById(R.id.lvSongs);
                lvSongs.setAdapter(new ListAdapter(this, songs));
                lvSongs.setOnItemClickListener(this);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error querying songs", ex);
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "Playing " + songs.size() + " songs, songIndex=" + position +
                " (" + songs.get(position) + ")");
        startService(new Intent(this, MainService.class)
                .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_START)
                .putExtra(MainService.ARG_SONGS, songs)
                .putExtra(MainService.ARG_SONG_INDEX, position));
    }

    private class ListAdapter extends SongAdapter implements View.OnClickListener {
        private ListAdapter(Context context, ArrayList<Song> songs) {
            super(context, songs);
        }

        @Override
        public void addButtons(LinearLayout llButtons) {
            ImageButton ibPlayNext = new ImageButton(SongsActivity.this);
            ibPlayNext.setId(R.id.ibPlayNext);
            ibPlayNext.setImageResource(R.drawable.ic_play_next);
            ibPlayNext.setContentDescription(getString(R.string.play_next));
            ibPlayNext.setOnClickListener(this);
            llButtons.addView(ibPlayNext);

            ImageButton ibPlayLast = new ImageButton(SongsActivity.this);
            ibPlayLast.setId(R.id.ibPlayLast);
            ibPlayLast.setImageResource(R.drawable.ic_play_last);
            ibPlayLast.setContentDescription(getString(R.string.play_last));
            ibPlayLast.setOnClickListener(this);
            llButtons.addView(ibPlayLast);
        }

        @Override
        public void setButtons(View view, Song song) {
            ImageButton ibPlayNext = (ImageButton)view.findViewById(R.id.ibPlayNext);
            ibPlayNext.setTag(song);

            ImageButton ibPlayLast = (ImageButton)view.findViewById(R.id.ibPlayLast);
            ibPlayLast.setTag(song);
        }

        @Override
        public void onClick(View v) {
            Song song = (Song)v.getTag();
            switch (v.getId()) {
                case R.id.ibPlayNext:
                    Log.d(TAG, "Playing next: " + song);
                    startService(new Intent(SongsActivity.this, MainService.class)
                            .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_PLAY_NEXT)
                            .putExtra(MainService.ARG_SONG, song));
                    Toast.makeText(SongsActivity.this, getString(R.string.playing_next, song),
                            Toast.LENGTH_SHORT).show();
                    break;
                case R.id.ibPlayLast:
                    Log.d(TAG, "Playing last: " + song);
                    startService(new Intent(SongsActivity.this, MainService.class)
                            .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_PLAY_LAST)
                            .putExtra(MainService.ARG_SONG, song));
                    Toast.makeText(SongsActivity.this, getString(R.string.playing_last, song),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}
