package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.widget.EditText;

import java.util.List;

public class EditSongActivity extends AppCompatActivity implements View.OnClickListener,
        View.OnLongClickListener, AdapterView.OnItemSelectedListener {
    public static final String EXTRA_SONG =
            "com.oneup.timer.activity.EditSongActivity.SONG";

    private static final int REQUEST_SELECT_ADDED = 1;
    private static final int REQUEST_SELECT_BOOKMARKED = 2;

    private DbHelper dbHelper;

    private Song song;

    private List<String> tags;

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
        tags = dbHelper.querySongTags();
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
            case R.id.ok:
                song.setYear(etYear.getInt());

                String tag = etTag.getString();
                song.setTag(tag.length() == 0 ? null : tag);

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
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.added);
            if (song.getAdded() > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, song.getAdded());
            }
            startActivityForResult(intent, REQUEST_SELECT_ADDED);
        } else if (v == bBookmarked) {
            Intent intent = new Intent(this, DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.bookmarked);
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
            String tag = tags.get(position);
            if (!tag.isEmpty()) {
                etTag.setText(tag);
                sTag.setSelection(0);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }
}
