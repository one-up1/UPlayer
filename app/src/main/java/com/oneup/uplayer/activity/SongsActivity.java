package com.oneup.uplayer.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.oneup.uplayer.R;
import com.oneup.uplayer.fragment.SongsFragment;

public class SongsActivity extends AppCompatActivity {
    private static final String TAG = "UPlayer";

    private SongsFragment songsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "SongsActivity.onCreate()");

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.container);
        setContentView(container);

        if (savedInstanceState == null) {
            songsFragment = SongsFragment.newInstance(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.container, songsFragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_songs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reverseSortOrder:
                songsFragment.reverseSortOrder();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}