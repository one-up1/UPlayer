package com.oneup.uplayer.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.fragment.ListFragment;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class PlaylistsActivity extends AppCompatActivity {
    private static final String TAG = "UPlayer";

    public static final String EXTRA_PLAYLISTS = "com.oneup.extra.PLAYLISTS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.container);
        setContentView(container);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, PlaylistsFragment.newInstance())
                    .commit();
        }
    }

    public static class PlaylistsFragment extends ListFragment<Playlist> {
        public PlaylistsFragment() {
            super(R.layout.list_item_playlist, 0, 0, R.id.checkBox, null, null);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            reloadData();
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.fragment_playlists, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.add:
                    Playlist.showSaveDialog(getActivity(), getDbHelper(), null,
                            new Playlist.SaveListener() {

                        @Override
                        public void onSave(Playlist playlist) {
                            reloadData();
                        }
                    });
                    return true;
                case R.id.query:
                    getActivity().setResult(RESULT_OK, new Intent()
                            .putParcelableArrayListExtra(EXTRA_PLAYLISTS, getCheckedListItems()));
                    getActivity().finish();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v,
                                        ContextMenu.ContextMenuInfo menuInfo) {
            getActivity().getMenuInflater().inflate(R.menu.list_item_playlist, menu);
        }

        @Override
        protected ArrayList<Playlist> loadData() {
            return getDbHelper().queryPlaylists();
        }

        @Override
        protected String getActivityTitle() {
            return Util.getCountString(getActivity(), R.plurals.playlists, getCount());
        }

        @Override
        protected void setListItemContent(View rootView, int position, Playlist playlist) {
            super.setListItemContent(rootView, position, playlist);

            TextView tvName = rootView.findViewById(R.id.tvName);
            if (playlist.getName() == null) {
                tvName.setVisibility(View.GONE);
            } else {
                tvName.setText(playlist.getName());
                tvName.setVisibility(View.VISIBLE);
            }

            TextView tvModified = rootView.findViewById(R.id.tvModified);
            tvModified.setText(Util.formatDateTimeAgo(playlist.getModified()));
        }

        @Override
        protected void onListItemClick(int position, Playlist playlist) {
            getActivity().startService(new Intent(getActivity(), MainService.class)
                    .putExtra(MainService.EXTRA_ACTION, MainService.ACTION_PLAY_PLAYLIST)
                    .putExtra(MainService.EXTRA_PLAYLIST, playlist));
        }

        @Override
        protected void onContextItemSelected(int itemId, int position, final Playlist playlist) {
            switch (itemId) {
                case R.id.delete:
                    if (playlist.getId() == 1) {
                        Util.showToast(getActivity(), R.string.cannot_delete_default_playlist);
                        return;
                    }

                    Util.showConfirmDialog(getActivity(),
                            getString(R.string.delete_playlist_confirm, playlist.getName()),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        getDbHelper().deletePlaylist(playlist);
                                        reloadData();
                                    } catch (Exception ex) {
                                        Log.e(TAG, "Error deleting playlist", ex);
                                        Util.showErrorDialog(getActivity(), ex);
                                    }
                                }
                            });
                    break;
            }
        }

        private static PlaylistsFragment newInstance() {
            return new PlaylistsFragment();
        }
    }
}
