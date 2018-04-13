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
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.widget.EditText;

import java.util.ArrayList;

public class EditSongActivity extends AppCompatActivity implements View.OnClickListener,
        AdapterView.OnItemSelectedListener {
    public static final String EXTRA_SONG =
            "com.oneup.timer.activity.EditSongActivity.SONG";
    public static final String EXTRA_TAGS =
            "com.oneup.timer.activity.EditSongActivity.TAGS";

    private static final int REQUEST_SELECT_DATE_ADDED = 1;

    private Song song;
    private ArrayList<String> tags;

    private EditText etTitle;
    private EditText etArtist;
    private Button bDateAdded;
    private EditText etYear;
    private EditText etDuration;
    private Button bLastPlayed;
    private EditText etTimesPlayed;
    private Button bBookmarked;
    private Spinner sTag;
    private EditText etTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_song);
        setTitle(R.string.edit_song);

        song = getIntent().getParcelableExtra(EXTRA_SONG);
        tags = getIntent().getStringArrayListExtra(EXTRA_TAGS);
        tags.add(0, "");

        etTitle = findViewById(R.id.etTitle);
        etTitle.setString(song.getTitle());

        etArtist = findViewById(R.id.etArtist);
        etArtist.setString(song.getArtist().getArtist());

        bDateAdded = findViewById(R.id.bDateAdded);
        if (song.getDateAdded() > 0) {
            bDateAdded.setText(Util.formatDateTimeAgo(song.getDateAdded()));
        }
        bDateAdded.setOnClickListener(this);

        etYear = findViewById(R.id.etYear);
        if (song.getYear() > 0) {
            etYear.setInt(song.getYear());
        }

        etDuration = findViewById(R.id.etDuration);
        etDuration.setString(Util.formatDuration(song.getDuration()));

        bLastPlayed = findViewById(R.id.bLastPlayed);
        if (song.getLastPlayed() > 0) {
            bLastPlayed.setText(Util.formatDateTimeAgo(song.getLastPlayed()));
        }

        etTimesPlayed = findViewById(R.id.etTimesPlayed);
        etTimesPlayed.setString(song.getArtist().getTimesPlayed() + ":" + song.getTimesPlayed());

        bBookmarked = findViewById(R.id.bBookmarked);
        if (song.getBookmarked() > 0) {
            bBookmarked.setText(Util.formatDateTimeAgo(song.getBookmarked()));
        }

        etTag = findViewById(R.id.etTag);
        if (song.getTag() != null) {
            etTag.setString(song.getTag());
        }

        sTag = findViewById(R.id.sTag);
        sTag.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tags));
        sTag.setOnItemSelectedListener(this);
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
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_DATE_ADDED:
                    song.setDateAdded(data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0));
                    bDateAdded.setText(Util.formatDateTimeAgo(song.getDateAdded()));
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == bDateAdded) {
            Intent intent = new Intent(this, DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.date_added);
            if (song.getDateAdded() > 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, song.getDateAdded());
            }
            startActivityForResult(intent, REQUEST_SELECT_DATE_ADDED);
        }
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
