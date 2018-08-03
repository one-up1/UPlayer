package com.oneup.uplayer.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.fragment.SelectListFragment;
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.widget.EditText;

import java.util.ArrayList;

public class PlaylistsActivity extends AppCompatActivity {
    private static final String TAG = "UPlayer";

    public static final String EXTRA_PLAYLIST = "com.oneup.extra.PLAYLIST";
    public static final String EXTRA_PLAYLISTS = "com.oneup.extra.PLAYLISTS";

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
        private static final String ARG_SELECT_CONFIRM_ID = "select_confirm_id";
        private static final String ARG_CHECKED_PLAYLISTS = "checked_playlists";

        private int selectConfirmId;

        public PlaylistsFragment() {
            super(R.layout.list_item_playlist, R.menu.list_item_playlist, R.id.checkBox);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle args = getArguments();
            if (args != null) {
                selectConfirmId = args.getInt(ARG_SELECT_CONFIRM_ID);
                setCheckedListItems(args.<Playlist>getParcelableArrayList(ARG_CHECKED_PLAYLISTS));
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
            switch (item.getItemId()) {
                case R.id.add:
                    add();
                    return true;
                case R.id.ok:
                    Intent data = new Intent();
                    if (isNotVisible()) {
                        data.putExtra(ARG_NOT, isNotChecked());
                    }
                    data.putExtra(EXTRA_PLAYLISTS, getCheckedListItems());
                    getActivity().setResult(AppCompatActivity.RESULT_OK, data);
                    getActivity().finish();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        @Override
        protected ArrayList<Playlist> loadData() {
            return getDbHelper().queryPlaylists(getSelection(), getSelectionArgs());
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

            // Set song count.
            TextView tvSongCount = rootView.findViewById(R.id.tvSongCount);
            tvSongCount.setText(Util.getCountString(getActivity(),
                    R.plurals.songs, playlist.getSongCount()));

            // Set (or hide) last played.
            TextView tvModified = rootView.findViewById(R.id.tvModified);
            if (playlist.getLastPlayed() == 0) {
                tvModified.setVisibility(View.GONE);
            } else {
                tvModified.setText(Util.formatTimeAgo(playlist.getLastPlayed()));
                tvModified.setVisibility(View.VISIBLE);
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
                    Util.showConfirmDialog(getActivity(),
                            getString(selectConfirmId, playlist),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    select(playlist);
                                }
                            });
                    break;
            }
        }

        @Override
        protected void onContextItemSelected(int itemId, int position, Playlist playlist) {
            switch (itemId) {
                case R.id.rename:
                    rename(playlist);
                    break;
                case R.id.delete:
                    delete(position, playlist);
                    break;
                default:
                    super.onContextItemSelected(itemId, position, playlist);
                    break;
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
                    R.string.name, null, new Util.InputDialogListener() {

                        @Override
                        public void onOk(EditText view) {
                            String name = view.getString();
                            if (name != null) {
                                try {
                                    Playlist playlist = new Playlist();
                                    playlist.setName(name);
                                    getDbHelper().insertOrUpdatePlaylist(playlist, null);
                                    reloadData();
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error adding playlist", ex);
                                    Util.showErrorDialog(getActivity(), ex);
                                }
                            }
                        }
                    });
        }

        private void rename(final Playlist playlist) {
            Util.showInputDialog(getActivity(), R.string.rename_playlist,
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                    R.string.name, playlist.getName(), new Util.InputDialogListener() {

                        @Override
                        public void onOk(EditText view) {
                            String name = view.getString();
                            if (name != null) {
                                try {
                                    playlist.setName(view.getString());
                                    getDbHelper().insertOrUpdatePlaylist(playlist, null);
                                    reloadData();
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error renaming playlist", ex);
                                    Util.showErrorDialog(getActivity(), ex);
                                }
                            }
                        }
                    });
        }

        private void delete(final int position, final Playlist playlist) {
            if (playlist.isDefault()) {
                Util.showToast(getActivity(), R.string.cannot_delete_default_playlist);
                return;
            }

            Util.showConfirmDialog(getActivity(),
                    getString(R.string.delete_confirm, playlist),
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                getDbHelper().deletePlaylist(playlist);

                                Util.showToast(getActivity(), R.string.deleted, playlist);
                                removeListItem(position);
                            } catch (Exception ex) {
                                Log.e(TAG, "Error deleting playlist", ex);
                                Util.showErrorDialog(getActivity(), ex);
                            }
                        }
                    });
        }

        public static Bundle getArguments(String selection, String[] selectionArgs,
                                          Boolean not, ArrayList<Playlist> checkedPlaylists,
                                          int selectConfirmId) {
            Bundle args = new Bundle();
            args.putString(ARG_SELECTION, selection);
            args.putStringArray(ARG_SELECTION_ARGS, selectionArgs);
            if (not != null) {
                args.putBoolean(ARG_NOT, not);
            }
            args.putParcelableArrayList(ARG_CHECKED_PLAYLISTS, checkedPlaylists);
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
