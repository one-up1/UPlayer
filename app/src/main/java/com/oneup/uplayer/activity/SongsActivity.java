package com.oneup.uplayer.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.oneup.uplayer.R;
import com.oneup.uplayer.fragment.BaseArgs;
import com.oneup.uplayer.fragment.SongsFragment;

public class SongsActivity extends FragmentActivity implements BaseArgs {
    public static final String ARG_SOURCE = "source";
    public static final String ARG_URI = "uri";
    public static final String ARG_ID_COLUMN = "id_column";
    public static final String ARG_SELECTION = "selection";
    public static final String ARG_SELECTION_ARGS = "selection_args";
    public static final String ARG_ORDER_BY = "order_by";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_songs);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainer,
                    SongsFragment.newInstance(
                            intent.getIntExtra(ARG_SOURCE, 0),
                            (Uri) intent.getParcelableExtra(ARG_URI),
                            intent.getStringExtra(ARG_ID_COLUMN),
                            intent.getStringExtra(ARG_SELECTION),
                            intent.getStringArrayExtra(ARG_SELECTION_ARGS),
                            intent.getStringExtra(ARG_ORDER_BY),
                            intent.getIntExtra(ARG_SORT_BY, 0)))
                    .commit();
        }
    }
}