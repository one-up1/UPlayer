package com.oneup.uplayer.activity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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
                lvSongs.setAdapter(new SongAdapter(this, songs, true, null));
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
}
