package com.oneup.uplayer.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.FrameLayout;

import com.oneup.uplayer.R;
import com.oneup.uplayer.fragment.SongsFragment;

public class SongsActivity extends AppCompatActivity {
    private static final String TAG = "UPlayer";

    private SongsFragment songsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "SongsActivity.onCreate()");
        super.onCreate(savedInstanceState);

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