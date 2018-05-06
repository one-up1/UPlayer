package com.oneup.uplayer.fragment;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.EditSongActivity;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Song;

import java.util.ArrayList;

public abstract class BaseSongsFragment extends Fragment
        implements AdapterView.OnItemClickListener {
    private static final String TAG = "UPlayer";

    private static final int REQUEST_EDIT_SONG = 1;

    private int listViewResource;
    private String viewArtistOrderBy;
    private ArrayList<Song> songs;

    private DbHelper dbHelper;

    private ListView listView;
    private ListAdapter listAdapter;

    private Song editSong;

    public BaseSongsFragment(int listViewResource) {
        this.listViewResource = listViewResource;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "BaseSongsFragment.onCreate()");
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "BaseSongsFragment.onCreateView()");
        if (listView == null) {
            Log.d(TAG, "Creating ListView");
            listView = (ListView) inflater.inflate(R.layout.list_view, container, false);
            listView.setOnItemClickListener(this);
            registerForContextMenu(listView);
        }
        return listView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v == listView) {
            getActivity().getMenuInflater().inflate(R.menu.list_item_song, menu);
            menu.getItem(0).setVisible(viewArtistOrderBy != null);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (!getUserVisibleHint()) {
            //TODO: Or the wrong fragment may receive the onContextItemSelected() call?
            return false;
        }

        final Song song = songs.get(
                ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
        switch (item.getItemId()) {
            case R.id.view_artist:
                startActivity(new Intent(getContext(), SongsActivity.class)
                        .putExtras(SongsFragment.getArguments(
                                song.getArtistId(),
                                viewArtistOrderBy == null ? Song.TITLE : viewArtistOrderBy)));
                return true;
            case R.id.edit:
                dbHelper.querySong(song);
                startActivityForResult(new Intent(getContext(), EditSongActivity.class)
                                .putExtra(EditSongActivity.EXTRA_SONG, editSong = song),
                        REQUEST_EDIT_SONG);
                return true;
            case R.id.bookmark:
                dbHelper.bookmarkSong(song);
                dbHelper.querySong(song);
                Toast.makeText(getContext(), song.getBookmarked() > 0 ?
                                R.string.bookmark_set : R.string.bookmark_deleted,
                        Toast.LENGTH_SHORT).show();
                loadSongs();
                return true;
            case R.id.mark_played:
                dbHelper.updateSongPlayed(song);
                dbHelper.querySong(song);
                Toast.makeText(getContext(), getString(
                        R.string.times_played, song.getTimesPlayed()),
                        Toast.LENGTH_SHORT).show();
                loadSongs();
                return true;
            case R.id.delete:
                new AlertDialog.Builder(getContext())
                        .setIcon(R.drawable.ic_dialog_warning)
                        .setTitle(R.string.delete_song)
                        .setMessage(getString(R.string.delete_confirm, song.getTitle()))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "Deleting song: " + song);
                                ContentResolver contentResolver = getContext().getContentResolver();
                                Uri uri = song.getContentUri();

                                // Change type to image, otherwise nothing will be deleted.
                                ContentValues values = new ContentValues();
                                values.put("media_type", 1);
                                contentResolver.update(uri, values, null, null);

                                Log.d(TAG, contentResolver.delete(uri, null, null) +
                                        " songs deleted from MediaStore");
                                dbHelper.deleteSong(song);

                                Toast.makeText(getContext(), R.string.song_deleted,
                                        Toast.LENGTH_SHORT).show();

                                loadSongs();
                                onSongDeleted(song);
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "BaseSongsFragment.onActivityResult(" + requestCode + ", " + resultCode + ")");
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_EDIT_SONG:
                    Song song = data.getParcelableExtra(EditSongActivity.EXTRA_SONG);
                    editSong.setYear(song.getYear());
                    editSong.setAdded(song.getAdded());
                    editSong.setTag(song.getTag());
                    editSong.setBookmarked(song.getBookmarked());

                    dbHelper.updateSong(song);
                    Toast.makeText(getContext(), R.string.song_updated, Toast.LENGTH_SHORT).show();
                    editSong = null;
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "BaseSongsFragment.onDestroy()");
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == listView) {
            onListViewItemClick(position);
        }
    }

    protected void setSongs(ArrayList<Song> songs) {
        this.songs = songs;

        if (listAdapter == null) {
            Log.d(TAG, "Creating ListAdapter");
            listAdapter = new ListAdapter();
            listView.setAdapter(listAdapter);
        } else {
            Log.d(TAG, "Calling ListAdapter.notifyDataSetChanged()");
            listAdapter.notifyDataSetChanged();
        }
    }

    protected ArrayList<Song> getSongs() {
        return songs;
    }

    protected void setViewArtistOrderBy(String viewArtistOrderBy) {
        this.viewArtistOrderBy = viewArtistOrderBy;
    }

    protected void setListViewSelection(int position) {
        listView.setSelection(position);
    }

    protected DbHelper getDbHelper() {
        return dbHelper;
    }

    protected void loadSongs() {
    }

    protected void setListViewViews(View rootView, int position, Song song) {
    }

    protected void onListViewItemClick(int position) {
    }

    protected void onSongDeleted(Song song) {
    }

    protected void notifyDataSetChanged() {
        Log.d(TAG, "BaseSongsFragment.notifyDataSetChanged()");
        listAdapter.notifyDataSetChanged();
    }

    private class ListAdapter extends BaseAdapter {
        private LayoutInflater layoutInflater;

        private ListAdapter() {
            layoutInflater = LayoutInflater.from(BaseSongsFragment.this.getContext());
        }

        @Override
        public int getCount() {
            return songs.size();
        }

        @Override
        public Object getItem(int position) {
            return songs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return songs.get(position).getId();
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            Log.d(TAG, "BaseSongsFragment.getView(" + position + ")");
            Song song = songs.get(position);

            if (view == null) {
                view = layoutInflater.inflate(listViewResource, parent, false);
            }

            TextView tvTitle = view.findViewById(R.id.tvTitle);
            tvTitle.setText(song.getTitle());
            tvTitle.setTextColor(song.getTimesPlayed() == 0 ? Color.BLUE : Color.BLACK);

            TextView tvArtist = view.findViewById(R.id.tvArtist);
            tvArtist.setText(song.getArtist());

            setListViewViews(view, position, song);

            return view;
        }
    }
}
