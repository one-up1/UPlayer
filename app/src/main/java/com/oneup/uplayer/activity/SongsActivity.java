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
import java.util.Arrays;

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

    private ListView lvSongs;
    private ArrayList<Song> songs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songs);

        String idColumn = getIntent().getStringExtra(ARG_ID_COLUMN);
        try {
            DbOpenHelper dbOpenHelper = new DbOpenHelper(this);
            Cursor cursor;
            switch (getIntent().getIntExtra(ARG_SOURCE, 0)) {
                case SOURCE_ANDROID:
                    cursor = getContentResolver().query(
                            (Uri)getIntent().getParcelableExtra(ARG_URI), new String[] {
                                    idColumn,
                                    Song.TITLE,
                                    Song.ARTIST_ID,
                                    Song.ARTIST,
                                    Song.YEAR
                            },
                            getIntent().getStringExtra(ARG_SELECTION),
                            getIntent().getStringArrayExtra(ARG_SELECTION_ARGS),
                            getIntent().getStringExtra(ARG_ORDER_BY));
                    if (cursor == null) {
                        Log.w(TAG, "No cursor");
                        return;
                    }
                    break;
                case SOURCE_DB:
                    cursor = dbOpenHelper.getReadableDatabase().query(Song.TABLE_NAME, null,
                            getIntent().getStringExtra(ARG_SELECTION),
                            getIntent().getStringArrayExtra(ARG_SELECTION_ARGS),
                            null, null, null);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid source");
            }

            try {
                songs = new ArrayList<>();
                int iId = cursor.getColumnIndex(idColumn);
                int iTitle = cursor.getColumnIndex(Song.TITLE);
                int iArtistId = cursor.getColumnIndex(Song.ARTIST_ID);
                int iArtist = cursor.getColumnIndex(Song.ARTIST);
                int iYear = cursor.getColumnIndex(Song.YEAR);

                int iLastPlayed = cursor.getColumnIndex(Song.LAST_PLAYED);
                int iTimesPlayed = cursor.getColumnIndex(Song.TIMES_PLAYED);

                while (cursor.moveToNext()) {
                    Song song = new Song();

                    song.setId(cursor.getLong(iId));
                    song.setTitle(cursor.getString(iTitle));
                    song.setArtistId(cursor.getLong(iArtistId));
                    song.setArtist(cursor.getString(iArtist));
                    song.setYear(cursor.getInt(iYear));

                    if (iLastPlayed != -1) {
                        song.setLastPlayed(cursor.getLong(iLastPlayed));
                    }
                    if (iTimesPlayed != -1) {
                        song.setTimesPlayed(cursor.getInt(iTimesPlayed));
                    }

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
