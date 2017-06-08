package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.SongsActivity;
import com.oneup.uplayer.db.obj.Playlist;

import java.util.ArrayList;

public class PlaylistsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "UPlayer";

    private ListView lvPlaylists;
    private ArrayList<Playlist> playlists;

    public PlaylistsFragment() {
    }

    public static PlaylistsFragment newInstance() {
        return new PlaylistsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View ret = inflater.inflate(R.layout.fragment_playlists, container, false);
        lvPlaylists = (ListView) ret.findViewById(R.id.lvPlaylists);
        lvPlaylists.setOnItemClickListener(this);

        try (Cursor c = getContext().getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Audio.Playlists._ID,
                        MediaStore.Audio.Playlists.NAME
                }, null, null, MediaStore.Audio.Playlists.NAME)) {
            if (c == null) {
                Log.wtf(TAG, "No cursor");
                return ret;
            }

            playlists = new ArrayList<>();
            int iId = c.getColumnIndex(MediaStore.Audio.Playlists._ID);
            int iName = c.getColumnIndex(MediaStore.Audio.Playlists.NAME);
            while (c.moveToNext()) {
                Playlist playlist = new Playlist();
                playlist.setId(c.getLong(iId));
                playlist.setName(c.getString(iName));
                playlists.add(playlist);
            }
        }

        Log.d(TAG, "Queried " + playlists.size() + " playlists");
        lvPlaylists.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, playlists));

        return ret;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == lvPlaylists) {
            startActivity(new Intent(getContext(), SongsActivity.class)
                    .putExtra(SongsActivity.ARG_URI,
                            MediaStore.Audio.Playlists.Members.getContentUri("external",
                                    playlists.get(position).getId()))
                    .putExtra(SongsActivity.ARG_ID_COLUMN,
                            MediaStore.Audio.Playlists.Members.AUDIO_ID)
                    .putExtra(SongsActivity.ARG_ORDER_BY,
                            MediaStore.Audio.Playlists.Members.PLAY_ORDER)
            );
        }
    }
}
