package com.oneup.uplayer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
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
    private Context context;
    private ArrayList<Song> songs;

    private LayoutInflater layoutInflater;

    public SongAdapter(Context context, ArrayList<Song> songs) {
        this.context = context;
        this.songs = songs;

        layoutInflater = LayoutInflater.from(context);
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

        tvTitle.setTextColor(song.getTimesPlayed() == 0 ? Color.BLUE :
                song.getBookmarked() == 0 ?
                        context.getResources().getColor(android.R.color.primary_text_light) :
                        Color.RED);

        setButtons(convertView, song);
        return convertView;
    }

    public abstract void addButtons(LinearLayout llButtons);

    public abstract void setButtons(View view, Song song);
}
