package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.widget.EditText;

import java.util.ArrayList;

public class EditSongActivity extends AppCompatActivity implements View.OnClickListener,
        View.OnLongClickListener, AdapterView.OnItemSelectedListener {
    public static final String EXTRA_SONG = "com.oneup.uplayer.extra.SONG";

    private static final String TAG = "UPlayer";

    private static final int REQUEST_SELECT_ADDED = 1;
    private static final int REQUEST_SELECT_BOOKMARKED = 2;
    private static final int REQUEST_SELECT_PLAYLISTS = 3;

    private DbHelper dbHelper;
    private Song song;
    private ArrayList<String> tags;
    private ArrayList<Playlist> playlists;

    private EditText etTitle;
    private EditText etArtist;
    private EditText etDuration;
    private EditText etYear;
    private Button bAdded;
    private EditText etTag;
    private Spinner sTag;
    private Button bBookmarked;
    private Button bLastPlayed;
    private EditText etTimesPlayed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_song);
        setTitle(R.string.edit_song);

        dbHelper = new DbHelper(this);

        song = getIntent().getParcelableExtra(EXTRA_SONG);
        tags = dbHelper.querySongTags(null, null);
        tags.add(0, "");

        etTitle = findViewById(R.id.etTitle);
        etTitle.setString(song.getTitle());

        etArtist = findViewById(R.id.etArtist);
        etArtist.setString(song.getArtist());

        etDuration = findViewById(R.id.etDuration);
        etDuration.setString(Util.formatDuration(song.getDuration()));

        etYear = findViewById(R.id.etYear);
        if (song.getYear() > 0) {
            etYear.setInt(song.getYear());
        }

        bAdded = findViewById(R.id.bAdded);
        if (song.getAdded() > 0) {
            bAdded.setText(Util.formatDateTimeAgo(song.getAdded()));
        }
        bAdded.setOnClickListener(this);
        bAdded.setOnLongClickListener(this);

        etTag = findViewById(R.id.etTag);
        if (song.getTag() != null) {
            etTag.setString(song.getTag());
        }

        sTag = findViewById(R.id.sTag);
        sTag.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tags));
        sTag.setOnItemSelectedListener(this);

        bBookmarked = findViewById(R.id.bBookmarked);
        if (song.getBookmarked() > 0) {
            bBookmarked.setText(Util.formatDateTimeAgo(song.getBookmarked()));
        }
        bBookmarked.setOnClickListener(this);
        bBookmarked.setOnLongClickListener(this);

        bLastPlayed = findViewById(R.id.bLastPlayed);
        if (song.getLastPlayed() > 0) {
            bLastPlayed.setText(Util.formatDateTimeAgo(song.getLastPlayed()));
        }

        etTimesPlayed = findViewById(R.id.etTimesPlayed);
        etTimesPlayed.setString(song.getTimesPlayed() +
                " (" + Util.formatDuration(song.getTimesPlayed() * song.getDuration()) + ")");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_edit_song, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.playlists:
                playlists = dbHelper.queryPlaylists(Song._ID + "=?",
                        DbHelper.getWhereArgs(song.getId()));
                startActivityForResult(new Intent(this, PlaylistsActivity.class)
                        .putExtras(PlaylistsActivity.PlaylistsFragment.getArguments(
                                null, null, -1, playlists)),
                        REQUEST_SELECT_PLAYLISTS);
                return true;
            case R.id.ok:
                song.setYear(etYear.getInt());
                song.setTag(etTag.getString());

                setResult(RESULT_OK, new Intent()
                        .putExtra(EXTRA_SONG, song));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_ADDED:
                    song.setAdded(data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0));
                    bAdded.setText(Util.formatDateTimeAgo(song.getAdded()));
                    break;
                case REQUEST_SELECT_BOOKMARKED:
                    song.setBookmarked(data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0));
                    bBookmarked.setText(Util.formatDateTimeAgo(song.getBookmarked()));
                    break;
                case REQUEST_SELECT_PLAYLISTS:
                    try {
                        ArrayList<Playlist> playlists = data.getParcelableArrayListExtra(
                                PlaylistsActivity.EXTRA_PLAYLISTS);

                        ArrayList<Playlist> inserted = new ArrayList<>();
                        for (Playlist playlist : playlists) {
                            if (!this.playlists.contains(playlist)) {
                                dbHelper.insertPlaylistSong(playlist, song);
                                inserted.add(playlist);
                            }
                        }

                        ArrayList<Playlist> deleted = new ArrayList<>();
                        for (Playlist playlist : this.playlists) {
                            if (!playlists.contains(playlist)) {
                                if (dbHelper.deletePlaylistSong(playlist, song)) {
                                    deleted.add(playlist);
                                }
                            }
                        }

                        String added = Util.getCountString(this, inserted,
                                true, R.string.playlists);
                        String removed = Util.getCountString(this, deleted,
                                true, R.string.playlists);

                        if (added != null && removed == null) {
                            Util.showSnackbar(this, R.string.added_to_playlists, added);
                        } else if (added == null && removed != null) {
                            Util.showSnackbar(this, R.string.removed_from_playlists, removed);
                        } else if (added != null) {
                            Util.showSnackbar(this, R.string.added_to_and_removed_from_playlists,
                                    added, removed);
                        }
                    } catch (Exception ex) {
                        Log.e(TAG, "Error modifying playlist songs", ex);
                        Util.showErrorDialog(this, ex);
                    }
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == bAdded) {
            Intent intent = new Intent(this, DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_added);
            if (song.getAdded() > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, song.getAdded());
            }
            startActivityForResult(intent, REQUEST_SELECT_ADDED);
        } else if (v == bBookmarked) {
            Intent intent = new Intent(this, DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_bookmarked);
            if (song.getBookmarked() > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, song.getBookmarked());
            }
            startActivityForResult(intent, REQUEST_SELECT_BOOKMARKED);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == bAdded) {
            song.setAdded(0);
            bAdded.setText("");
        } else if (v == bBookmarked) {
            song.setBookmarked(0);
            bBookmarked.setText("");
        }
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == sTag) {
            if (position > 0) {
                etTag.setString(tags.get(position));
                sTag.setSelection(0);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
