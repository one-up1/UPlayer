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
import com.oneup.uplayer.fragment.ListFragment;
import com.oneup.uplayer.util.Calendar;
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

    public static class PlaylistsFragment extends ListFragment<Playlist> {
        private static final String ARG_ALLOW_ADD = "allow_add";
        private static final String ARG_CHECKED_ITEMS = "checked_items";
        private static final String ARG_SELECT_PLAYLIST_CONFIRM_ID = "select_playlist_confirm_id";

        private boolean allowAdd;
        private ArrayList<Playlist> checkedItems;
        private int selectPlaylistConfirmId;

        public PlaylistsFragment() {
            super(R.layout.list_item_playlist, R.menu.list_item_playlist, 0, 0, R.id.checkBox,
                    null, null);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);

            Bundle args = getArguments();
            if (args != null) {
                allowAdd = args.getBoolean(ARG_ALLOW_ADD);
                checkedItems = args.getParcelableArrayList(ARG_CHECKED_ITEMS);
                selectPlaylistConfirmId = args.getInt(ARG_SELECT_PLAYLIST_CONFIRM_ID);
            }

            reloadData();
            if (checkedItems != null && checkedItems.size() > 0) {
                setCheckedListItems(checkedItems);
            }
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.fragment_playlists, menu);
        }

        @Override
        public void onPrepareOptionsMenu(Menu menu) {
            super.onPrepareOptionsMenu(menu);
            menu.findItem(R.id.add).setVisible(allowAdd);
            menu.findItem(R.id.select_all).setVisible(isCheckboxVisible());
            menu.findItem(R.id.ok).setVisible(isCheckboxVisible());
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.add:
                    add();
                    return true;
                case R.id.select_all:
                    setCheckedListItems(getData());
                    notifyDataSetChanged();
                    return true;
                case R.id.ok:
                    getActivity().setResult(RESULT_OK, new Intent()
                            .putParcelableArrayListExtra(EXTRA_PLAYLISTS, getCheckedListItems()));
                    getActivity().finish();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        @Override
        protected ArrayList<Playlist> loadData() {
            return getDbHelper().queryPlaylists(!allowAdd, getSelection(), getSelectionArgs());
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
            if (playlist.getName() == null) {
                tvName.setVisibility(View.GONE);
            } else {
                tvName.setText(playlist.getName());
                tvName.setVisibility(View.VISIBLE);
            }

            // Set modified date.
            TextView tvModified = rootView.findViewById(R.id.tvModified);
            tvModified.setText(Util.formatDateTimeAgo(playlist.getModified()));
        }

        @Override
        protected void onListItemClick(int position, final Playlist playlist) {
            if (selectPlaylistConfirmId == 0) {
                selectPlaylist(playlist);
            } else {
                Util.showConfirmDialog(getActivity(),
                        getString(selectPlaylistConfirmId, playlist),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                selectPlaylist(playlist);
                            }
                        });
            }
        }

        @Override
        protected void onContextItemSelected(int itemId, int position, Playlist playlist) {
            switch (itemId) {
                case R.id.rename:
                    rename(playlist);
                    break;
                case R.id.delete:
                    delete(playlist);
                    break;
                default:
                    super.onContextItemSelected(itemId, position, playlist);
                    break;
            }
        }

        private void add() {
            Util.showInputDialog(getActivity(),
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                    R.string.name, null, new Util.InputDialogListener() {

                        @Override
                        public void onOk(EditText view) {
                            Playlist playlist = new Playlist();
                            playlist.setName(view.getString());
                            playlist.setModified(Calendar.currentTime());
                            getDbHelper().insertOrUpdatePlaylist(playlist, null);
                            reloadData();
                        }
                    });
        }

        private void rename(final Playlist playlist) {
            Util.showInputDialog(getActivity(),
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                    R.string.name, playlist.getName(), new Util.InputDialogListener() {

                        @Override
                        public void onOk(EditText view) {
                            playlist.setName(view.getString());
                            getDbHelper().insertOrUpdatePlaylist(playlist, null);
                            reloadData();
                        }
                    });
        }

        private void delete(final Playlist playlist) {
            if (playlist.getId() == 1) {
                Util.showToast(getActivity(), R.string.cannot_delete_default_playlist);
                return;
            }

            Util.showConfirmDialog(getActivity(),
                    getString(R.string.delete_playlist_confirm, playlist),
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
        }

        private void selectPlaylist(Playlist playlist) {
            getActivity().setResult(RESULT_OK, new Intent()
                    .putExtra(EXTRA_PLAYLIST, playlist));
            getActivity().finish();
        }

        public static Bundle getArguments(String selection, String[] selectionArgs,
                                          boolean checkboxVisible, boolean allowAdd,
                                          ArrayList<Playlist> checkedItems,
                                          int selectPlaylistConfirmId) {
            Bundle args = new Bundle();
            args.putString(ARG_SELECTION, selection);
            args.putStringArray(ARG_SELECTION_ARGS, selectionArgs);
            args.putBoolean(ARG_CHECKBOX_VISIBLE, checkboxVisible);
            args.putBoolean(ARG_ALLOW_ADD, allowAdd);
            args.putParcelableArrayList(ARG_CHECKED_ITEMS, checkedItems);
            args.putInt(ARG_SELECT_PLAYLIST_CONFIRM_ID, selectPlaylistConfirmId);
            return args;
        }

        private static PlaylistsFragment newInstance(Bundle args) {
            PlaylistsFragment fragment = new PlaylistsFragment();
            fragment.setArguments(args);
            return fragment;
        }
    }
}
