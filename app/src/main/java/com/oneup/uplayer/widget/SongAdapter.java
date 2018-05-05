package com.oneup.uplayer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Song;

import java.util.List;

public abstract class SongAdapter extends BaseAdapter {
    private Context context;
    private List<Song> objects;

    private LayoutInflater layoutInflater;

    public SongAdapter(Context context, List<Song> objects) {
        this.context = context;
        this.objects = objects;

        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return objects.size();
    }

    @Override
    public Object getItem(int position) {
        return objects.get(position);
    }

    @Override
    public long getItemId(int position) {
        return objects.get(position).getId();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = layoutInflater.inflate(R.layout.list_item_song, parent, false);
            addButtons((RelativeLayout) view.findViewById(R.id.rlButtons));
        } else {
            view = convertView;
        }

        Song song = objects.get(position);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        tvTitle.setText(song.getTitle());
        tvTitle.setTextColor(song.getTimesPlayed() == 0 ? Color.BLUE : Color.BLACK);

        TextView tvArtist = view.findViewById(R.id.tvArtist);
        tvArtist.setText(song.getArtist());

        setButtons(view, song);
        return view;
    }

    public abstract void addButtons(RelativeLayout rlButtons);

    public abstract void setButtons(View view, Song song);
}
