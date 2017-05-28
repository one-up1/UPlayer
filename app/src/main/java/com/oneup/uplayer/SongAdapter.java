package com.oneup.uplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.oneup.uplayer.db.obj.Song;

import java.util.ArrayList;

public class SongAdapter extends BaseAdapter {
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
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
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
        tvArtist.setText(song.getArtist());

        TextView tvYear = (TextView) convertView.findViewById(R.id.tvYear);
        tvYear.setText(Integer.toString(song.getYear()));

        TextView tvTimesPlayed = (TextView) convertView.findViewById(R.id.tvTimesPlayed);
        if (song.getTimesPlayed() == 0) {
            tvTimesPlayed.setVisibility(View.GONE);
        } else {
            tvTimesPlayed.setVisibility(View.VISIBLE);
            tvTimesPlayed.setText(Integer.toString(song.getTimesPlayed()));
        }

        setButtons(convertView, song);
        return convertView;
    }

    public void addButtons(LinearLayout llButtons) {
    }

    public void setButtons(View view, Song song) {
    }
}
