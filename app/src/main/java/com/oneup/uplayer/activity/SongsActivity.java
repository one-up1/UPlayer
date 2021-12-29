package com.oneup.uplayer.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.FrameLayout;

import com.oneup.uplayer.R;
import com.oneup.uplayer.fragment.SongsFragment;

public class SongsActivity extends AppCompatActivity {
    private SongsFragment songsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.container);
        setContentView(container);

        if (savedInstanceState == null) {
            songsFragment = SongsFragment.newInstance(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, songsFragment)
                    .commit();
        }
    }
}