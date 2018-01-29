package com.oneup.uplayer.widget;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content
        .DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.DateTimeActivity;
import com.oneup.uplayer.db.DbOpenHelper;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Calendar;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SongsListView extends ListView {
    private static final String TAG = "UPlayer";

    private static final int REQUEST_SELECT_DATE_ADDED = 1;

    private Activity context;
    private DbOpenHelper dbOpenHelper;
    private Song editSong;

    private OnDataSetChangedListener onDataSetChangedListener;
    private OnSongDeletedListener onSongDeletedListener;

    public SongsListView(Activity context) {
        super(context);

        this.context = context;
        dbOpenHelper = new DbOpenHelper(context);
    }

    public boolean onContextItemSelected(MenuItem item) {
        final Song song = (Song) getItemAtPosition(
                ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.bookmark:
                dbOpenHelper.querySong(song);
                if (song.getBookmarked() == 0) {
                    Log.d(TAG, "Setting bookmark: " + song);
                    song.setBookmarked(Calendar.currentTime());
                    Toast.makeText(context, R.string.bookmark_set, Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "Deleting bookmark: " + song);
                    song.setBookmarked(0);
                    Toast.makeText(context, R.string.bookmark_deleted, Toast.LENGTH_SHORT).show();
                }
                dbOpenHelper.insertOrUpdateSong(song);

                if (onDataSetChangedListener != null) {
                    onDataSetChangedListener.onDataSetChanged();
                }
                return true;
            case R.id.set_tag:
                dbOpenHelper.querySong(song);
                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setTitle(R.string.set_tag)
                        .setView(R.layout.dialog_set_tag)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                EditText etTag = ((AlertDialog) dialog).findViewById(R.id.etTag);
                                String tag = etTag.getText().toString().trim();
                                if (tag.length() == 0) {
                                    Log.d(TAG, "Clearing tag from: " + song);
                                    song.setTag(null);
                                    Toast.makeText(context, R.string.tag_cleared,
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.d(TAG, "Setting tag '" + tag + "' to: " + song);
                                    song.setTag(tag);
                                    Toast.makeText(context, R.string.tag_set,
                                            Toast.LENGTH_SHORT).show();
                                }
                                dbOpenHelper.insertOrUpdateSong(song);

                                if (onDataSetChangedListener != null) {
                                    onDataSetChangedListener.onDataSetChanged();
                                }
                            }
                        })
                        .create();
                Window window = dialog.getWindow();
                if (window != null) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
                dialog.show();

                Spinner sTag = dialog.findViewById(R.id.sTag);
                final List<String> tags = new ArrayList<>();
                tags.add("");
                tags.addAll(Arrays.asList(dbOpenHelper.querySongTags()));
                sTag.setAdapter(new ArrayAdapter<>(context,
                        android.R.layout.simple_spinner_dropdown_item, tags));

                final EditText etTag = dialog.findViewById(R.id.etTag);
                if (song.getTag() != null) {
                    etTag.setText(song.getTag());
                    etTag.selectAll();
                }

                sTag.setOnItemSelectedListener(new OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        String tag = tags.get(position);
                        if (!tag.isEmpty()) {
                            etTag.setText(tag);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
                return true;
            case R.id.set_date_added:
                editSong = song;
                Intent intent = new Intent(getContext(), DateTimeActivity.class);
                intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.set_date_added);
                if (song.getDateAdded() > 0) {
                    intent.putExtra(DateTimeActivity.EXTRA_TIME, song.getDateAdded());
                }
                context.startActivityForResult(intent, REQUEST_SELECT_DATE_ADDED);
                return true;
            case R.id.mark_played:
                dbOpenHelper.updateSongPlayed(song);
                Toast.makeText(context, R.string.updated, Toast.LENGTH_SHORT).show();

                ((SongAdapter) getAdapter()).notifyDataSetChanged();
                if (onDataSetChangedListener != null) {
                    onDataSetChangedListener.onDataSetChanged();
                }
                return true;
            case R.id.info:
                dbOpenHelper.querySong(song);
                Util.showInfoDialog(context, song.getArtist() + " - " + song.getTitle(),
                        context.getString(
                                R.string.info_message_song,
                                song.getDateAdded() == 0 ?
                                        context.getString(R.string.na) :
                                        Util.formatDateTime(song.getDateAdded()),
                                song.getYear(),
                                Util.formatDuration(song.getDuration()),
                                song.getLastPlayed() == 0 ?
                                        context.getString(R.string.never) :
                                        Util.formatDateTime(song.getLastPlayed()),
                                song.getTimesPlayed(),
                                song.getBookmarked() == 0 ?
                                        context.getString(R.string.no) :
                                        Util.formatDateTime(song.getBookmarked()),
                                song.getTag() == null ? "" : song.getTag())
                );
                return true;
            case R.id.delete:
                new AlertDialog.Builder(context)
                        .setIcon(R.drawable.ic_dialog_warning)
                        .setTitle(R.string.delete_song)
                        .setMessage(context.getString(R.string.delete_confirm, song.getTitle()))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "Deleting song: " + song);
                                ContentResolver contentResolver = context.getContentResolver();
                                Uri uri = song.getContentUri();

                                // Change type to image, otherwise nothing will be deleted.
                                ContentValues values = new ContentValues();
                                values.put("media_type", 1);
                                contentResolver.update(uri, values, null, null);

                                Log.d(TAG, contentResolver.delete(uri, null, null) +
                                        " songs deleted");
                                dbOpenHelper.deleteSong(song);
                                Toast.makeText(context, R.string.song_deleted,
                                        Toast.LENGTH_SHORT).show();

                                if (onDataSetChangedListener != null) {
                                    onDataSetChangedListener.onDataSetChanged();
                                }
                                if (onSongDeletedListener != null) {
                                    onSongDeletedListener.onSongDeleted(song);
                                }
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
        }
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + ", " + resultCode + ")");
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_DATE_ADDED:
                    editSong.setDateAdded(data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0));
                    Log.d(TAG, "Set to " +Util.formatDateTime(editSong.getDateAdded()));
                    dbOpenHelper.insertOrUpdateSong(editSong);
                    editSong = null;
                    break;
            }
        }
    }

    public void setOnDataSetChangedListener(OnDataSetChangedListener onDataSetChangedListener) {
        this.onDataSetChangedListener = onDataSetChangedListener;
    }

    public void setOnSongDeletedListener(OnSongDeletedListener onSongDeletedListener) {
        this.onSongDeletedListener = onSongDeletedListener;
    }

    public interface OnDataSetChangedListener {
        void onDataSetChanged();
    }

    public interface OnSongDeletedListener {
        void onSongDeleted(Song song);
    }
}
