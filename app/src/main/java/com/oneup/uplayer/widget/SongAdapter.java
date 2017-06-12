package com.oneup.uplayer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Song;

import java.util.ArrayList;

public abstract class SongAdapter extends BaseAdapter {
    private LayoutInflater layoutInflater;
    private ArrayList<Song> songs;

    public SongAdapter(Context context, ArrayList<Song> songs) {
        this.layoutInflater = LayoutInflater.from(context);
        this.songs = songs;
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

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_item_song, parent, false);
            addButtons((LinearLayout) convertView.findViewById(R.id.llButtons));
        }

        Song song = songs.get(position);

        TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTitle);
        tvTitle.setText(song.getTitle());

        TextView tvArtist = (TextView) convertView.findViewById(R.id.tvArtist);
        tvArtist.setText(song.getArtist().getArtist());

        setButtons(convertView, song);
        return convertView;
    }

    public abstract void addButtons(LinearLayout llButtons);

    public abstract void setButtons(View view, Song song);
}
