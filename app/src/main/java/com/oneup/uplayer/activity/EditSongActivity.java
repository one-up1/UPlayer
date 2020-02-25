package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.widget.EditText;
import com.oneup.util.Utils;

import java.util.ArrayList;

public class EditSongActivity extends AppCompatActivity implements View.OnClickListener,
        View.OnLongClickListener, AdapterView.OnItemSelectedListener {
    public static final String EXTRA_SONG = "com.oneup.uplayer.extra.SONG";

    private static final String TAG = "UPlayer";

    private static final int REQUEST_SELECT_BOOKMARKED = 1;
    private static final int REQUEST_SELECT_ARCHIVED = 2;
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
    private Button bArchived;
    private Button bLastPlayed;
    private EditText etTimesPlayed;
    private EditText etComments;

    private Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_song);

        dbHelper = new DbHelper(this);

        song = getIntent().getParcelableExtra(EXTRA_SONG);
        dbHelper.querySong(song);

        tags = dbHelper.querySongTags();
        tags.add(0, "");

        etTitle = findViewById(R.id.etTitle);
        etTitle.setString(song.getTitle());

        etArtist = findViewById(R.id.etArtist);
        etArtist.setString(song.getArtist());

        etDuration = findViewById(R.id.etDuration);
        etDuration.setString(Util.formatDuration(song.getDuration()));

        etYear = findViewById(R.id.etYear);
        if (song.getYear() != 0) {
            etYear.setInt(song.getYear());
        }

        bAdded = findViewById(R.id.bAdded);
        if (song.getAdded() != 0) {
            bAdded.setText(Util.formatDateTimeAgo(song.getAdded()));
        }

        etTag = findViewById(R.id.etTag);
        if (song.getTag() != null) {
            etTag.setString(song.getTag());
        }

        sTag = findViewById(R.id.sTag);
        sTag.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tags));
        sTag.setOnItemSelectedListener(this);

        bBookmarked = findViewById(R.id.bBookmarked);
        if (song.getBookmarked() != 0) {
            bBookmarked.setText(Util.formatDateTimeAgo(song.getBookmarked()));
        }
        bBookmarked.setOnClickListener(this);
        bBookmarked.setOnLongClickListener(this);

        bArchived = findViewById(R.id.bArchived);
        if (song.getArchived() != 0) {
            bArchived.setText(Util.formatDateTimeAgo(song.getArchived()));
        }
        bArchived.setOnClickListener(this);
        bArchived.setOnLongClickListener(this);

        bLastPlayed = findViewById(R.id.bLastPlayed);
        if (song.getLastPlayed() != 0) {
            bLastPlayed.setText(Util.formatDateTimeAgo(song.getLastPlayed()));
        }

        etTimesPlayed = findViewById(R.id.etTimesPlayed);
        etTimesPlayed.setString(song.getTimesPlayed() +
                " (" + Util.formatDuration(song.getTimesPlayed() * song.getDuration()) + ")");

        etComments = findViewById(R.id.etComments);
        if (song.getComments() != null) {
            etComments.setString(song.getComments());
        }
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
                playlists = dbHelper.queryPlaylists(song);
                startActivityForResult(new Intent(this, PlaylistsActivity.class)
                                .putExtras(PlaylistsActivity.PlaylistsFragment.getArguments(
                                        playlists, null, -1)),
                        REQUEST_SELECT_PLAYLISTS);
                return true;
            case R.id.ok:
                song.setYear(etYear.getInt());
                song.setTag(etTag.getString());
                song.setComments(etComments.getString());
                dbHelper.updateSong(song);

                Utils.showToast(this, R.string.song_updated);
                MainService.update(this, false, song);

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
                case REQUEST_SELECT_BOOKMARKED:
                    song.setBookmarked(data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0));
                    bBookmarked.setText(Util.formatDateTimeAgo(song.getBookmarked()));
                    break;
                case REQUEST_SELECT_ARCHIVED:
                    song.setArchived(data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0));
                    bArchived.setText(Util.formatDateTimeAgo(song.getArchived()));
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
                                true, 0, R.string.playlist_count);
                        String removed = Util.getCountString(this, deleted,
                                true, 0, R.string.playlist_count);

                        String snackbarText = null;
                        if (added != null && removed == null) {
                            snackbarText = getString(R.string.added_to_playlists, added);
                        } else if (added == null && removed != null) {
                            snackbarText = getString(R.string.removed_from_playlists, removed);
                        } else if (added != null) {
                            snackbarText = getString(R.string.added_to_and_removed_from_playlists,
                                    added, removed);
                        }

                        if (snackbarText != null) {
                            snackbar = Snackbar.make(findViewById(android.R.id.content),
                                    snackbarText, Snackbar.LENGTH_INDEFINITE);
                            snackbar.show();
                        }

                        MainService.update(this, false, song);
                    } catch (Exception ex) {
                        Log.e(TAG, "Error modifying playlist songs", ex);
                        Utils.showErrorDialog(this, ex);
                    }
                    break;
            }
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        if (snackbar != null) {
            snackbar.dismiss();
            snackbar = null;
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
        if (v == bBookmarked) {
            Intent intent = new Intent(this, DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_bookmarked);
            if (song.getBookmarked() != 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, song.getBookmarked());
            }
            startActivityForResult(intent, REQUEST_SELECT_BOOKMARKED);
        } else if (v == bArchived) {
            Intent intent = new Intent(this, DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_archived);
            if (song.getArchived() != 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, song.getArchived());
            }
            startActivityForResult(intent, REQUEST_SELECT_ARCHIVED);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == bBookmarked) {
            song.setBookmarked(0);
            bBookmarked.setText("");
        } else if (v == bArchived) {
            song.setArchived(0);
            bArchived.setText("");
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
