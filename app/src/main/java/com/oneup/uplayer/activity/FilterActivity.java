package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.oneup.uplayer.R;
import com.oneup.uplayer.fragment.FilterFragment;

public class FilterActivity extends AppCompatActivity {
    public static final String EXTRA_SHOW_ARTIST_FILTER = "com.oneup.extra.SHOW_ARTIST_FILTER";
    public static final String EXTRA_VALUES = "com.oneup.extra.VALUES";
    public static final String EXTRA_SELECTION = "com.oneup.extra.SELECTION";
    public static final String EXTRA_SELECTION_ARGS = "com.oneup.extra.SELECTION_ARGS";

    private FilterFragment filterFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        filterFragment = (FilterFragment) fragmentManager.findFragmentById(R.id.filterFragment);
        filterFragment.setShowArtistFilter(getIntent().getBooleanExtra(
                EXTRA_SHOW_ARTIST_FILTER, true));
        filterFragment.setSelectPlaylistConfirmId(-1);
        filterFragment.setValues((FilterFragment.Values) getIntent()
                .getParcelableExtra(FilterActivity.EXTRA_VALUES));
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_filter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ok:
                setResult(RESULT_OK, new Intent()
                        .putExtra(EXTRA_VALUES, filterFragment.getValues())
                        .putExtra(EXTRA_SELECTION, filterFragment.getSelection())
                        .putExtra(EXTRA_SELECTION_ARGS, filterFragment.getSelectionArgs()));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
