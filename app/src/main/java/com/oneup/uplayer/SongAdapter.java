package com.oneup.uplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.oneup.uplayer.obj.Song;

import java.util.ArrayList;

public class SongAdapter extends BaseAdapter implements View.OnClickListener {
    private static final String TAG = "UPlayer";

    private Context context;
    private ArrayList<Song> songs;
    private boolean showNextLast;
    private View.OnClickListener deleteOnClickListener;
    private LayoutInflater layoutInflater;

    public SongAdapter(Context context, ArrayList<Song> songs,
                       boolean showNextLast, View.OnClickListener deleteOnClickListener) {
        this.context = context;
        this.songs = songs;
        this.showNextLast = showNextLast;
        this.deleteOnClickListener = deleteOnClickListener;
        this.layoutInflater = LayoutInflater.from(context);
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

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View ret = convertView == null ?
                layoutInflater.inflate(R.layout.row_song, parent, false) : convertView;
        Song song = songs.get(position);

        TextView tvTitle = (TextView)ret.findViewById(R.id.tvTitle);
        tvTitle.setText(song.getTitle());

        TextView tvArtist = (TextView)ret.findViewById(R.id.tvArtist);
        tvArtist.setText(song.getArtist());

        TextView tvYear = (TextView)ret.findViewById(R.id.tvYear);
        tvYear.setText(Integer.toString(song.getYear()));

        ImageButton ibPlayNext = (ImageButton)ret.findViewById(R.id.ibPlayNext);
        ImageButton ibPlayLast = (ImageButton)ret.findViewById(R.id.ibPlayLast);
        if (showNextLast) {
            ibPlayNext.setVisibility(View.VISIBLE);
            ibPlayNext.setTag(song);
            ibPlayNext.setOnClickListener(this);

            ibPlayLast.setVisibility(View.VISIBLE);
            ibPlayLast.setTag(song);
            ibPlayLast.setOnClickListener(this);
        } else {
            ibPlayNext.setVisibility(View.GONE);
            ibPlayLast.setVisibility(View.GONE);
        }

        ImageButton ibDelete = (ImageButton)ret.findViewById(R.id.ibDelete);
        if (deleteOnClickListener == null) {
            ibDelete.setVisibility(View.GONE);
        } else {
            ibDelete.setVisibility(View.VISIBLE);
            ibDelete.setTag(song);
            ibDelete.setOnClickListener(deleteOnClickListener);
        }

        return ret;
    }

    @Override
    public void onClick(View v) {
        Song song = (Song)v.getTag();
        switch (v.getId()) {
            case R.id.ibPlayNext:
                Log.d(TAG, "Playing next: " + song);
                context.startService(new Intent(context, MainService.class)
                        .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_PLAY_NEXT)
                        .putExtra(MainService.ARG_SONG, song));
                Toast.makeText(context, context.getString(R.string.playing_next, song),
                        Toast.LENGTH_SHORT).show();
                break;
            case R.id.ibPlayLast:
                Log.d(TAG, "Playing last: " + song);
                context.startService(new Intent(context, MainService.class)
                        .putExtra(MainService.ARG_REQUEST_CODE, MainService.REQUEST_PLAY_LAST)
                        .putExtra(MainService.ARG_SONG, song));
                Toast.makeText(context, context.getString(R.string.playing_last, song),
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
