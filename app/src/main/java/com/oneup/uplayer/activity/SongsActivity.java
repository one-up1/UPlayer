package com.oneup.uplayer.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.obj.Song;

import java.util.ArrayList;

public class SongsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    public static final String ARG_URI = "uri";
    public static final String ARG_ID_COLUMN = "id_column";
    public static final String ARG_TITLE_COLUMN = "title_column";
    public static final String ARG_ARTIST_COLUMN = "artist_column";
    public static final String ARG_YEAR_COLUMN = "year_column";
    public static final String ARG_SELECTION = "selection";
    public static final String ARG_SELECTION_ARGS = "selection_args";

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
        String titleColumn = intent.getStringExtra(ARG_TITLE_COLUMN);
        String artistColumn = intent.getStringExtra(ARG_ARTIST_COLUMN);
        String yearColumn = intent.getStringExtra(ARG_YEAR_COLUMN);
        String selection = intent.getStringExtra(ARG_SELECTION);
        String[] selectionArgs = intent.getStringArrayExtra(ARG_SELECTION_ARGS);

        Cursor c = getContentResolver().query(uri,
                new String[] { idColumn, titleColumn, artistColumn, yearColumn },
                selection, selectionArgs, null);
        if (c != null) {
            try {
                songs = new ArrayList<>();
                int iId = c.getColumnIndex(idColumn);
                int iTitle = c.getColumnIndex(titleColumn);
                int iArtist = c.getColumnIndex(artistColumn);
                int iYear = c.getColumnIndex(yearColumn);
                while (c.moveToNext()) {
                    Log.d(TAG, "Year: " + c.getString(iYear));
                    songs.add(new Song(c.getLong(iId), c.getString(iTitle),
                            c.getString(iArtist), c.getInt(iYear)));
                }
            } finally {
                c.close();
            }

            Log.d(TAG, "Queried " + songs.size() + " songs");
            setTitle(getString(R.string.song_count, songs.size()));

            lvSongs = (ListView)findViewById(R.id.lvSongs);
            lvSongs.setAdapter(new SongAdapter(this, songs));
            lvSongs.setOnItemClickListener(this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Song song = songs.get(position);
        Log.d(TAG, "Playing song: " + song);

        startService(new Intent(this, MainService.class)
                .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_START)
                .putExtra(MainService.ARG_SONGS, songs)
                .putExtra(MainService.ARG_SONG_INDEX, position));
    }

    private class SongAdapter extends BaseAdapter implements View.OnClickListener {
        private Context context;
        private ArrayList<Song> songs;

        private LayoutInflater layoutInflater;

        private SongAdapter(Context context, ArrayList<Song> songs) {
            this.context = context;
            this.songs = songs;

            layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return songs.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout ret = (LinearLayout)(convertView == null ?
                    layoutInflater.inflate(R.layout.row_song, parent, false) : convertView);
            Song song = songs.get(position);

            TextView tvTitle = (TextView)ret.findViewById(R.id.tvTitle);
            tvTitle.setText(song.getTitle());

            TextView tvArtist = (TextView)ret.findViewById(R.id.tvArtist);
            tvArtist.setText(song.getArtist());

            TextView tvYear = (TextView)ret.findViewById(R.id.tvYear);
            tvYear.setText(Integer.toString(song.getYear()));

            ImageButton ibPlayNext = (ImageButton)ret.findViewById(R.id.ibPlayNext);
            ibPlayNext.setTag(song);
            ibPlayNext.setOnClickListener(this);

            ImageButton ibPlayLast = (ImageButton)ret.findViewById(R.id.ibPlayLast);
            ibPlayLast.setTag(song);
            ibPlayLast.setOnClickListener(this);

            return ret;
        }

        @Override
        public void onClick(View v) {
            Song song = (Song)v.getTag();
            switch (v.getId()) {
                case R.id.ibPlayNext:
                    Log.d(TAG, "Play next: " + song);
                    startService(new Intent(SongsActivity.this, MainService.class)
                            .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_PLAY_NEXT)
                            .putExtra(MainService.ARG_SONG, song));
                    break;
                case R.id.ibPlayLast:
                    Log.d(TAG, "Play last: " + song);
                    startService(new Intent(SongsActivity.this, MainService.class)
                            .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_PLAY_LAST)
                            .putExtra(MainService.ARG_SONG, song));
                    break;
            }
        }
    }
}
