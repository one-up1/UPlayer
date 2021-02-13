package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.fragment.SelectListFragment;
import com.oneup.uplayer.util.Util;
import com.oneup.util.Utils;

import java.util.ArrayList;

public class PlaylistsActivity extends AppCompatActivity {
    public static final String EXTRA_PLAYLIST = "com.oneup.extra.PLAYLIST";
    public static final String EXTRA_PLAYLISTS = "com.oneup.extra.PLAYLISTS";

    private static final String TAG = "UPlayer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.container);
        setContentView(container);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, PlaylistsFragment.newInstance(getIntent().getExtras()))
                    .commit();
        }
    }

    public static class PlaylistsFragment extends SelectListFragment<Playlist> {
        private static final String ARG_CHECKED_PLAYLISTS = "checked_playlists";
        private static final String ARG_SELECT_CONFIRM_ID = "select_confirm_id";

        private int selectConfirmId;

        public PlaylistsFragment() {
            super(R.layout.list_item_playlist, R.menu.list_item_playlist, R.id.checkBox);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle args = getArguments();
            if (args != null) {
                setCheckedListItems(args.getParcelableArrayList(ARG_CHECKED_PLAYLISTS));
                selectConfirmId = args.getInt(ARG_SELECT_CONFIRM_ID);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            reloadData();
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.fragment_playlists, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.add) {
                add();
            } else if (id == R.id.ok) {
                Intent data = new Intent();
                if (isNotVisible()) {
                    data.putExtra(ARG_NOT, isNotChecked());
                }
                data.putExtra(EXTRA_PLAYLISTS, getCheckedListItems());
                getActivity().setResult(AppCompatActivity.RESULT_OK, data);
                getActivity().finish();
            } else {
                return super.onOptionsItemSelected(item);
            }
            return true;
        }

        @Override
        protected ArrayList<Playlist> loadData() {
            return getDbHelper().queryPlaylists(null);
        }

        @Override
        protected String getActivityTitle() {
            return Util.getCountString(getActivity(), R.plurals.playlists, getCount());
        }

        @Override
        protected void setListItemContent(View rootView, int position, Playlist playlist) {
            super.setListItemContent(rootView, position, playlist);

            // Set playlist name.
            TextView tvName = rootView.findViewById(R.id.tvName);
            tvName.setText(playlist.getName());

            // Set play button.
            ImageButton ibPlay = rootView.findViewById(R.id.ibPlay);
            if (selectConfirmId == -1) {
                ibPlay.setVisibility(View.VISIBLE);
                setListItemViewOnClickListener(rootView, R.id.ibPlay);
            } else {
                ibPlay.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onListItemClick(int position, final Playlist playlist) {
            switch (selectConfirmId) {
                case 0:
                    select(playlist);
                    break;
                case -1:
                    super.onListItemClick(position, playlist);
                    break;
                default:
                    Utils.showConfirmDialog(getActivity(), (dialog, which) -> select(playlist),
                            R.string.app_name, selectConfirmId, playlist);
                    break;
            }
        }

        @Override
        protected void onListItemViewClick(int viewId, int position, Playlist playlist) {
            if (viewId == R.id.ibPlay) {
                select(playlist);
            }
        }

        @Override
        protected void onContextItemSelected(int itemId, int position, Playlist playlist) {
            if (itemId == R.id.rename) {
                rename(playlist);
            } else if (itemId == R.id.delete) {
                delete(position, playlist);
            } else {
                super.onContextItemSelected(itemId, position, playlist);
            }
        }

        private void select(Playlist playlist) {
            getActivity().setResult(AppCompatActivity.RESULT_OK, new Intent()
                    .putExtra(EXTRA_PLAYLIST, playlist));
            getActivity().finish();
        }

        private void add() {
            Util.showInputDialog(getActivity(), R.string.add_playlist,
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                    R.string.name, null, view -> {
                        String name = view.getString();
                        if (name != null) {
                            try {
                                Playlist playlist = new Playlist();
                                playlist.setName(name);
                                getDbHelper().insertOrUpdatePlaylist(playlist, null);
                                reloadData();
                            } catch (Exception ex) {
                                Log.e(TAG, "Error adding playlist", ex);
                                Utils.showErrorDialog(getActivity(), ex);
                            }
                        }
                    });
        }

        private void rename(final Playlist playlist) {
            Util.showInputDialog(getActivity(), R.string.rename_playlist,
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                    R.string.name, playlist.getName(), view -> {
                        String name = view.getString();
                        if (name != null) {
                            try {
                                playlist.setName(view.getString());
                                getDbHelper().insertOrUpdatePlaylist(playlist, null);
                                reloadData();
                            } catch (Exception ex) {
                                Log.e(TAG, "Error renaming playlist", ex);
                                Utils.showErrorDialog(getActivity(), ex);
                            }
                        }
                    });
        }

        private void delete(final int position, final Playlist playlist) {
            if (playlist.isDefault()) {
                Utils.showToast(getActivity(), R.string.cannot_delete_default_playlist);
                return;
            }

            Utils.showConfirmDialog(getActivity(),
                    (dialog, which) -> {
                        try {
                            getDbHelper().deletePlaylist(playlist);
                            Utils.showToast(getActivity(), R.string.deleted, playlist);
                            removeListItem(position);
                        } catch (Exception ex) {
                            Log.e(TAG, "Error deleting playlist", ex);
                            Utils.showErrorDialog(getActivity(), ex);
                        }
                    }, R.string.app_name, R.string.delete_confirm, playlist);
        }

        public static Bundle getArguments(ArrayList<Playlist> checkedPlaylists, Boolean not,
                                          int selectConfirmId) {
            Bundle args = new Bundle();
            args.putParcelableArrayList(ARG_CHECKED_PLAYLISTS, checkedPlaylists);
            if (not != null) {
                args.putBoolean(ARG_NOT, not);
            }
            args.putInt(ARG_SELECT_CONFIRM_ID, selectConfirmId);
            return args;
        }

        private static PlaylistsFragment newInstance(Bundle args) {
            PlaylistsFragment fragment = new PlaylistsFragment();
            fragment.setArguments(args);
            return fragment;
        }
    }
}
