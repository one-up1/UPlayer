package com.oneup.uplayer.activity;

import android.app.AlertDialog;
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
        public PlaylistsFragment() {
            super(R.layout.list_item_playlist, R.menu.list_item_playlist,
                    0, 0, R.id.checkBox, null, null);
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
        public void onPrepareOptionsMenu(Menu menu) {
            super.onPrepareOptionsMenu(menu);
            menu.findItem(R.id.select).setVisible(isCheckboxVisible());
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.add:
                    add();
                    return true;
                case R.id.select:
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
            getActivity().setResult(RESULT_OK, new Intent()
                    .putExtra(EXTRA_PLAYLIST, playlist));
            getActivity().finish();
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
            }
        }

        private void add() {
            final EditText etName = new EditText(getActivity());
            etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            etName.setHint(R.string.name);

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.add)
                    .setView(etName)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Playlist playlist = new Playlist();
                            playlist.setName(etName.getString());
                            playlist.setModified(Calendar.currentTime());

                            getDbHelper().insertOrUpdatePlaylist(playlist, true, null);
                            Util.showToast(getActivity(), R.string.ok);
                            reloadData();
                        }
                    })
                    .show();
        }

        private void rename(final Playlist playlist) {
            final EditText etName = new EditText(getActivity());
            etName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            etName.setHint(R.string.name);

            if (playlist.getName() != null) {
                etName.setText(playlist.getName());
            }

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.rename)
                    .setView(etName)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            playlist.setName(etName.getString());
                            getDbHelper().insertOrUpdatePlaylist(playlist, true, null);

                            Util.showToast(getActivity(), R.string.ok);
                            reloadData();
                        }
                    })
                    .show();
        }

        private void delete(final Playlist playlist) {
            if (playlist.getId() == 1) {
                Util.showToast(getActivity(), R.string.cannot_delete_default_playlist);
                return;
            }

            Util.showConfirmDialog(getActivity(),
                    getString(R.string.delete_playlist_confirm,
                            playlist.getName() == null ?
                                    Util.formatDateTime(playlist.getModified())
                                    : playlist.getName()),
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

        public static Bundle getArguments(boolean checkboxVisible) {
            Bundle args = new Bundle();
            args.putBoolean(ARG_CHECKBOX_VISIBLE, checkboxVisible);
            return args;
        }

        private static PlaylistsFragment newInstance(Bundle args) {
            PlaylistsFragment fragment = new PlaylistsFragment();
            fragment.setArguments(args);
            return fragment;
        }
    }
}
