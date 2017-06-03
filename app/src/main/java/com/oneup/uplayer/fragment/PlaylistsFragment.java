package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
import com.oneup.uplayer.db.obj.Song;

import java.util.ArrayList;

public class PlaylistsFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "UPlayer";
    
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
        ListView lvPlaylists = (ListView) ret.findViewById(R.id.lvPlaylists);
        lvPlaylists.setOnItemClickListener(this);

        Cursor c = getContext().getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                new String[]{
                        MediaStore.Audio.Playlists._ID,
                        MediaStore.Audio.Playlists.NAME
                }, null, null, MediaStore.Audio.Playlists.NAME);
        if (c != null) {
            try {
                playlists = new ArrayList<>();
                int iId = c.getColumnIndex(MediaStore.Audio.Playlists._ID);
                int iName = c.getColumnIndex(MediaStore.Audio.Playlists.NAME);
                while (c.moveToNext()) {
                    playlists.add(new Playlist(c.getLong(iId), c.getString(iName)));
                }
            } finally {
                c.close();
            }

            Log.d(TAG, "Queried " + playlists.size() + " playlists");
            lvPlaylists.setAdapter(new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_list_item_1, playlists));
        }

        return ret;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startActivity(new Intent(getContext(), SongsActivity.class)
                .putExtra(SongsActivity.ARG_SOURCE, SongsFragment.SOURCE_ANDROID)
                .putExtra(SongsActivity.ARG_URI, MediaStore.Audio.Playlists.Members.getContentUri(
                        "external", playlists.get(position).getId()))
                .putExtra(SongsActivity.ARG_ID_COLUMN, MediaStore.Audio.Playlists.Members.AUDIO_ID)
                .putExtra(SongsActivity.ARG_ORDER_BY,
                        MediaStore.Audio.Playlists.Members.PLAY_ORDER)
        );
    }
}
