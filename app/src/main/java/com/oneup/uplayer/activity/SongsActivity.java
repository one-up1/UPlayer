package com.oneup.uplayer.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.SparseArray;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.fragment.BaseArgs;
import com.oneup.uplayer.fragment.SongsFragment;

public class SongsActivity extends FragmentActivity implements BaseArgs {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songs);

        if (savedInstanceState == null) {
            Bundle args = getIntent().getExtras();
            SparseArray<Artist> artists = args.getSparseParcelableArray(ARG_ARTISTS);
            getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainer,
                    SongsFragment.newInstance(
                            artists,
                            args.getInt(ARG_JOINED_SORT_BY),
                            args.getString(ARG_SELECTION),
                            null))
                    .commit();
        }
    }
}