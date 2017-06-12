package com.oneup.uplayer.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.widget.FrameLayout;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.fragment.BaseArgs;
import com.oneup.uplayer.fragment.SongsFragment;

public class SongsActivity extends AppCompatActivity implements BaseArgs {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.container);
        setContentView(container);

        if (savedInstanceState == null) {
            Bundle args = getIntent().getExtras();
            SparseArray<Artist> artists = args.getSparseParcelableArray(ARG_ARTISTS);
            getSupportFragmentManager().beginTransaction().add(R.id.container,
                    SongsFragment.newInstance(
                            artists,
                            args.getInt(ARG_JOINED_SORT_BY),
                            args.getString(ARG_SELECTION),
                            args.getString(ARG_DB_ORDER_BY)))
                    .commit();
        }
    }
}