package com.oneup.uplayer.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.obj.Song;

import java.util.ArrayList;

public class SongsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    public static final String ARG_SOURCE = "source";
    public static final String ARG_URI = "uri";
    public static final String ARG_ID_COLUMN = "id_column";
    public static final String ARG_SELECTION = "selection";
    public static final String ARG_SELECTION_ARGS = "selection_args";
    public static final String ARG_ORDER_BY = "order_by";

    public static final int SOURCE_ANDROID = 1;
    public static final int SOURCE_DB = 2;

    private static final String TAG = "UPlayer";

    private DbOpenHelper dbOpenHelper;

    private ListView lvSongs;
    private ArrayList<Song> songs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songs);

        try {
            dbOpenHelper = new DbOpenHelper(this);

            String[] columns = {
                    getIntent().getStringExtra(ARG_ID_COLUMN),
                    Song.TITLE,
                    Song.ARTIST_ID,
                    Song.ARTIST,
                    Song.YEAR
            };
            Cursor cursor;
            switch (getIntent().getIntExtra(ARG_SOURCE, 0)) {
                case SOURCE_ANDROID:
                    cursor = getContentResolver().query(
                            (Uri)getIntent().getParcelableExtra(ARG_URI), columns,
                            getIntent().getStringExtra(ARG_SELECTION),
                            getIntent().getStringArrayExtra(ARG_SELECTION_ARGS),
                            getIntent().getStringExtra(ARG_ORDER_BY));
                    if (cursor == null) {
                        Log.w(TAG, "No cursor");
                        return;
                    }
                    break;
                case SOURCE_DB:
                    try (SQLiteDatabase db = dbOpenHelper.getReadableDatabase()) {
                        cursor = db.query(Song.TABLE_NAME, columns,
                                getIntent().getStringExtra(ARG_SELECTION),
                                getIntent().getStringArrayExtra(ARG_SELECTION_ARGS),
                                null, null, null);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid source");
            }

            try {
                songs = new ArrayList<>();
                int iId = cursor.getColumnIndex(columns[0]);
                int iTitle = cursor.getColumnIndex(columns[1]);
                int iArtistId = cursor.getColumnIndex(columns[2]);
                int iArtist = cursor.getColumnIndex(columns[3]);
                int iYear = cursor.getColumnIndex(columns[4]);
                while (cursor.moveToNext()) {
                    Song song = new Song(cursor.getLong(iId), cursor.getString(iTitle),
                            cursor.getLong(iArtistId), cursor.getString(iArtist),
                            cursor.getInt(iYear));
                    songs.add(song);
                }
            } finally {
                cursor.close();
            }

            Log.d(TAG, "Queried " + songs.size() + " songs");
            setTitle(getResources().getQuantityString(R.plurals.songs, songs.size(), songs.size()));

            lvSongs = (ListView) findViewById(R.id.lvSongs);
            lvSongs.setAdapter(new ListAdapter(this, songs));
            lvSongs.setOnItemClickListener(this);
        } catch (Exception ex) {
            Log.e(TAG, "Error querying songs", ex);
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (dbOpenHelper != null) {
            dbOpenHelper.close();
        }
        super.onDestroy();
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
            ImageButton ibPlayNext = (ImageButton) view.findViewById(R.id.ibPlayNext);
            ibPlayNext.setTag(song);

            ImageButton ibPlayLast = (ImageButton) view.findViewById(R.id.ibPlayLast);
            ibPlayLast.setTag(song);
        }

        @Override
        public void onClick(View v) {
            Song song = (Song) v.getTag();
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
