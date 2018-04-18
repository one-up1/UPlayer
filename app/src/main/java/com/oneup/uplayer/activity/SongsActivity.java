package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Artist;
import com.oneup.uplayer.fragment.BaseArgs;
import com.oneup.uplayer.fragment.SongsFragment;

public class SongsActivity extends AppCompatActivity implements BaseArgs {
    private static final String TAG = "UPlayer";

    private SongsFragment songsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.container);
        setContentView(container);

        if (savedInstanceState == null) {
            Bundle args = getIntent().getExtras();
            SparseArray<Artist> artists = args.getSparseParcelableArray(ARG_ARTISTS);
            songsFragment = SongsFragment.newInstance(
                    artists,
                    (Artist) args.getParcelable(ARG_ARTIST),
                    args.getInt(ARG_JOINED_SORT_BY),
                    args.getString(ARG_SELECTION),
                    args.getString(ARG_DB_ORDER_BY));
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "SongsActivity.onActivityResult(" + requestCode + ", " + resultCode + ")");
        songsFragment.onActivityResult(requestCode, resultCode, data);
    }
}