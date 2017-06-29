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
    private ArrayList<Song> objects;

    private LayoutInflater layoutInflater;

    public SongAdapter(Context context, ArrayList<Song> objects) {
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
        View ret;
        if (convertView == null) {
            ret = layoutInflater.inflate(R.layout.list_item_song, parent, false);
            addButtons((LinearLayout) ret.findViewById(R.id.llButtons));
        } else {
            ret = convertView;
        }

        Song song = objects.get(position);

        TextView tvTitle = (TextView) ret.findViewById(R.id.tvTitle);
        tvTitle.setText(song.getTitle());
        tvTitle.setTextColor(song.getTimesPlayed() == 0 ? Color.BLUE : Color.BLACK);

        TextView tvArtist = (TextView) ret.findViewById(R.id.tvArtist);
        tvArtist.setText(song.getArtist().getArtist());

        setButtons(ret, song);
        return ret;
    }

    public abstract void addButtons(LinearLayout llButtons);

    public abstract void setButtons(View view, Song song);
}
