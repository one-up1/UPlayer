package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.DateTimeActivity;
import com.oneup.uplayer.activity.FilterActivity;
import com.oneup.uplayer.activity.PlaylistsActivity;
import com.oneup.uplayer.activity.TagsActivity;
import com.oneup.uplayer.db.DbHelper;
import com.oneup.uplayer.db.Playlist;
import com.oneup.uplayer.db.Song;
import com.oneup.uplayer.util.Util;
import com.oneup.uplayer.widget.EditText;

import java.util.ArrayList;

public class FilterFragment extends Fragment
        implements View.OnClickListener, View.OnLongClickListener {
    private static final String VAL_TITLE = "title";
    private static final String VAL_ARTIST = "artist";
    private static final String VAL_MIN_YEAR = "min_year";
    private static final String VAL_MAX_YEAR = "max_year";
    private static final String VAL_MIN_ADDED = "min_added";
    private static final String VAL_MAX_ADDED = "max_added";
    private static final String VAL_ALL = "all";
    private static final String VAL_BOOKMARKED = "bookmarked";
    private static final String VAL_NOT_BOOKMARKED = "not_bookmarked";
    private static final String VAL_TAGS = "tags";
    private static final String VAL_TAGS_NOT = "tags_not";
    private static final String VAL_PLAYLISTS = "playlists";
    private static final String VAL_PLAYLISTS_NOT = "playlists_not";
    private static final String VAL_MIN_LAST_PLAYED = "min_last_played";
    private static final String VAL_MAX_LAST_PLAYED = "max_last_played";
    private static final String VAL_MIN_TIMES_PLAYED = "min_times_played";
    private static final String VAL_MAX_TIMES_PLAYED = "max_times_played";

    private static final int REQUEST_SELECT_MIN_ADDED = 1;
    private static final int REQUEST_SELECT_MAX_ADDED = 2;
    private static final int REQUEST_SELECT_TAGS = 3;
    private static final int REQUEST_SELECT_PLAYLISTS = 4;
    private static final int REQUEST_SELECT_MIN_LAST_PLAYED = 5;
    private static final int REQUEST_SELECT_MAX_LAST_PLAYED = 6;

    private EditText etTitle;
    private EditText etArtist;
    private EditText etMinYear;
    private EditText etMaxYear;
    private Button bMinAdded;
    private Button bMaxAdded;
    private RadioButton rbAll;
    private RadioButton rbBookmarked;
    private RadioButton rbNotBookmarked;
    private Button bTags;
    private Button bPlaylists;
    private Button bMinLastPlayed;
    private Button bMaxLastPlayed;
    private EditText etMinTimesPlayed;
    private EditText etMaxTimesPlayed;

    private int selectPlaylistConfirmId;
    private boolean showArtistFilter;
    private Bundle values;
    private String selection;
    private ArrayList<String> selectionArgs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        showArtistFilter = getActivity().getIntent().getBooleanExtra(
                FilterActivity.EXTRA_SHOW_ARTIST_FILTER, true);
        values = getActivity().getIntent().getBundleExtra(FilterActivity.EXTRA_VALUES);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_filter, container, false);

        etTitle = rootView.findViewById(R.id.etTitle);

        etArtist = rootView.findViewById(R.id.etArtist);
        etArtist.setVisibility(showArtistFilter ? View.VISIBLE : View.GONE);

        etMinYear = rootView.findViewById(R.id.etMinYear);

        etMaxYear = rootView.findViewById(R.id.etMaxYear);

        bMinAdded = rootView.findViewById(R.id.bMinAdded);
        bMinAdded.setOnClickListener(this);
        bMinAdded.setOnLongClickListener(this);

        bMaxAdded = rootView.findViewById(R.id.bMaxAdded);
        bMaxAdded.setOnClickListener(this);
        bMaxAdded.setOnLongClickListener(this);

        rbAll = rootView.findViewById(R.id.rbAll);
        rbBookmarked = rootView.findViewById(R.id.rbBookmarked);
        rbNotBookmarked = rootView.findViewById(R.id.rbNotBookmarked);

        bTags = rootView.findViewById(R.id.bTags);
        bTags.setOnClickListener(this);
        bTags.setOnLongClickListener(this);

        bPlaylists = rootView.findViewById(R.id.bPlaylists);
        bPlaylists.setOnClickListener(this);
        bPlaylists.setOnLongClickListener(this);

        bMinLastPlayed = rootView.findViewById(R.id.bMinLastPlayed);
        bMinLastPlayed.setOnClickListener(this);
        bMinLastPlayed.setOnLongClickListener(this);

        bMaxLastPlayed = rootView.findViewById(R.id.bMaxLastPlayed);
        bMaxLastPlayed.setOnClickListener(this);
        bMaxLastPlayed.setOnLongClickListener(this);

        etMinTimesPlayed = rootView.findViewById(R.id.etMinTimesPlayed);

        etMaxTimesPlayed = rootView.findViewById(R.id.etMaxTimesPlayed);

        if (values == null) {
            values = new Bundle();
            values.putStringArrayList(VAL_TAGS, new ArrayList<String>());
            values.putParcelableArrayList(VAL_PLAYLISTS, new ArrayList<Playlist>());

            bMinAdded.setText(R.string.select_min_added);
            bMaxAdded.setText(R.string.select_max_added);
            bTags.setText(R.string.select_tags);
            bPlaylists.setText(R.string.select_playlists);
            bMinLastPlayed.setText(R.string.select_min_last_played);
            bMaxLastPlayed.setText(R.string.select_max_last_played);
        } else {
            etTitle.setString(values.getString(VAL_TITLE));
            if (showArtistFilter) {
                etArtist.setString(values.getString(VAL_ARTIST));
            }
            etMinYear.setString(values.getString(VAL_MIN_YEAR));
            etMaxYear.setString(values.getString(VAL_MAX_YEAR));
            bMinAdded.setText(values.containsKey(VAL_MIN_ADDED)
                    ? Util.formatDateTime(values.getLong(VAL_MIN_ADDED))
                    : getString(R.string.select_min_added));
            bMaxAdded.setText(values.containsKey(VAL_MAX_ADDED)
                    ? Util.formatDateTime(values.getLong(VAL_MAX_ADDED))
                    : getString(R.string.select_max_added));
            rbAll.setChecked(values.getBoolean(VAL_ALL, true));
            rbBookmarked.setChecked(values.getBoolean(VAL_BOOKMARKED));
            rbNotBookmarked.setChecked(values.getBoolean(VAL_NOT_BOOKMARKED));
            setListButton(bTags, values.getStringArrayList(VAL_TAGS),
                    R.string.select_tags, R.string.selected_tags,
                    values.getBoolean(VAL_TAGS_NOT));
            setListButton(bPlaylists, values.getParcelableArrayList(VAL_PLAYLISTS),
                    R.string.select_playlists, R.string.selected_playlists,
                    values.getBoolean(VAL_PLAYLISTS_NOT));
            etMinTimesPlayed.setString(values.getString(VAL_MIN_TIMES_PLAYED));
            etMaxTimesPlayed.setString(values.getString(VAL_MAX_TIMES_PLAYED));
            bMinLastPlayed.setText(values.containsKey(VAL_MIN_LAST_PLAYED)
                    ? Util.formatDateTime(values.getLong(VAL_MIN_LAST_PLAYED))
                    : getString(R.string.select_min_last_played));
            bMaxLastPlayed.setText(values.containsKey(VAL_MAX_LAST_PLAYED)
                    ? Util.formatDateTime(values.getLong(VAL_MAX_LAST_PLAYED))
                    : getString(R.string.select_max_last_played));
        }

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_MIN_ADDED:
                    long minAdded = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMinAdded.setText(Util.formatDateTime(minAdded));
                    values.putLong(VAL_MIN_ADDED, minAdded);
                    break;
                case REQUEST_SELECT_MAX_ADDED:
                    long maxAdded = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMaxAdded.setText(Util.formatDateTime(maxAdded));
                    values.putLong(VAL_MAX_ADDED, maxAdded);
                    break;
                case REQUEST_SELECT_TAGS:
                    ArrayList<String> tags = data.getStringArrayListExtra(TagsActivity.EXTRA_TAGS);
                    boolean tagsNot = data.getBooleanExtra(
                            TagsActivity.TagsFragment.ARG_NOT, false);
                    setListButton(bTags, tags, R.string.select_tags,
                            R.string.selected_tags, tagsNot);
                    values.putStringArrayList(VAL_TAGS, tags);
                    values.putBoolean(VAL_TAGS_NOT, tagsNot);
                case REQUEST_SELECT_PLAYLISTS:
                    if (data.hasExtra(PlaylistsActivity.EXTRA_PLAYLIST)) {
                        getActivity().startService(new Intent(getActivity(), MainService.class)
                                .putExtra(MainService.EXTRA_ACTION,
                                        MainService.ACTION_PLAY)
                                .putExtra(MainService.EXTRA_PLAYLIST,
                                        data.getParcelableExtra(PlaylistsActivity.EXTRA_PLAYLIST)));
                    } else if (data.hasExtra(PlaylistsActivity.EXTRA_PLAYLISTS)) {
                        ArrayList<Playlist> playlists = data.getParcelableArrayListExtra(
                                PlaylistsActivity.EXTRA_PLAYLISTS);
                        boolean playlistsNot = data.getBooleanExtra(
                                TagsActivity.TagsFragment.ARG_NOT, false);
                        setListButton(bPlaylists, playlists, R.string.select_playlists,
                                R.string.selected_playlists, playlistsNot);
                        values.putParcelableArrayList(VAL_PLAYLISTS, playlists);
                        values.putBoolean(VAL_PLAYLISTS_NOT, playlistsNot);
                    }
                    break;
                case REQUEST_SELECT_MIN_LAST_PLAYED:
                    long minLastPlayed = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMinLastPlayed.setText(Util.formatDateTime(minLastPlayed));
                    values.putLong(VAL_MIN_LAST_PLAYED, minLastPlayed);
                    break;
                case REQUEST_SELECT_MAX_LAST_PLAYED:
                    long maxLastPlayed = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMaxLastPlayed.setText(Util.formatDateTime(maxLastPlayed));
                    values.putLong(VAL_MAX_LAST_PLAYED, maxLastPlayed);
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == bMinAdded) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_min_added);
            if (values.containsKey(VAL_MIN_ADDED)) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, values.getLong(VAL_MIN_ADDED));
            }
            startActivityForResult(intent, REQUEST_SELECT_MIN_ADDED);
        } else if (v == bMaxAdded) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_max_added);
            if (values.containsKey(VAL_MAX_ADDED)) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, values.getLong(VAL_MAX_ADDED));
            }
            startActivityForResult(intent, REQUEST_SELECT_MAX_ADDED);
        } else if (v == bTags) {
            startActivityForResult(new Intent(getActivity(), TagsActivity.class)
                            .putExtras(TagsActivity.TagsFragment.getArguments(
                                    values.getBoolean(VAL_TAGS_NOT),
                                    values.getStringArrayList(VAL_TAGS))),
                    REQUEST_SELECT_TAGS);
        } else if (v == bPlaylists) {
            startActivityForResult(new Intent(getActivity(), PlaylistsActivity.class)
                            .putExtras(PlaylistsActivity.PlaylistsFragment.getArguments(
                                    values.getBoolean(VAL_PLAYLISTS_NOT),
                                    values.<Playlist>getParcelableArrayList(VAL_PLAYLISTS),
                                    selectPlaylistConfirmId)),
                    REQUEST_SELECT_PLAYLISTS);
        } else if (v == bMinLastPlayed) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_min_last_played);
            if (values.containsKey(VAL_MIN_LAST_PLAYED)) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, values.getLong(VAL_MIN_LAST_PLAYED));
            }
            startActivityForResult(intent, REQUEST_SELECT_MIN_LAST_PLAYED);
        } else if (v == bMaxLastPlayed) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_max_last_played);
            if (values.containsKey(VAL_MAX_LAST_PLAYED)) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, values.getLong(VAL_MAX_LAST_PLAYED));
            }
            startActivityForResult(intent, REQUEST_SELECT_MAX_LAST_PLAYED);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == bMinAdded) {
            bMinAdded.setText(R.string.select_min_added);
            values.remove(VAL_MIN_ADDED);
        } else if (v == bMaxAdded) {
            bMaxAdded.setText(R.string.select_max_added);
            values.remove(VAL_MAX_ADDED);
        } else if (v == bTags) {
            bTags.setText(R.string.select_tags);
            values.getStringArrayList(VAL_TAGS).clear();
            values.remove(VAL_TAGS_NOT);
        } else if (v == bPlaylists) {
            bPlaylists.setText(R.string.select_playlists);
            values.getParcelableArrayList(VAL_PLAYLISTS).clear();
            values.remove(VAL_PLAYLISTS_NOT);
        } else if (v == bMinLastPlayed) {
            bMinLastPlayed.setText(R.string.select_min_last_played);
            values.remove(VAL_MIN_LAST_PLAYED);
        } else if (v == bMaxLastPlayed) {
            bMaxLastPlayed.setText(R.string.select_max_last_played);
            values.remove(VAL_MAX_LAST_PLAYED);
        }
        return true;
    }

    public void setSelectPlaylistConfirmId(int selectPlaylistConfirmId) {
        this.selectPlaylistConfirmId = selectPlaylistConfirmId;
    }

    public Bundle getValues() {
        values.putString(VAL_TITLE, etTitle.getString());
        if (showArtistFilter) {
            values.putString(VAL_ARTIST, etArtist.getString());
        }
        values.putString(VAL_MIN_YEAR, etMinYear.getString());
        values.putString(VAL_MAX_YEAR, etMaxYear.getString());
        values.putBoolean(VAL_ALL, rbAll.isChecked());
        values.putBoolean(VAL_BOOKMARKED, rbBookmarked.isChecked());
        values.putBoolean(VAL_NOT_BOOKMARKED, rbNotBookmarked.isChecked());
        values.putString(VAL_MIN_TIMES_PLAYED, etMinTimesPlayed.getString());
        values.putString(VAL_MAX_TIMES_PLAYED, etMaxTimesPlayed.getString());
        return values;
    }

    public String getSelection() {
        selection = null;
        selectionArgs = new ArrayList<>();

        String title = etTitle.getString();
        if (title != null) {
            selection = Song.TITLE + " LIKE ?";
            selectionArgs.add("%" + title + "%");
        }

        if (showArtistFilter) {
            String sArtist = etArtist.getString();
            if (sArtist != null) {
                selection = DbHelper.concatSelection(selection, Song.ARTIST + " LIKE ?");
                selectionArgs.add("%" + sArtist + "%");
            }
        }

        String minYear = etMinYear.getString();
        if (minYear != null) {
            selection = DbHelper.concatSelection(selection,
                    DbHelper.getMinSelection(Song.YEAR));
            selectionArgs.add(minYear);
        }

        String maxYear = etMaxYear.getString();
        if (maxYear != null) {
            selection = DbHelper.concatSelection(selection,
                    DbHelper.getMaxSelection(Song.YEAR, minYear != null));
            selectionArgs.add(maxYear);
        }

        if (values.containsKey(VAL_MIN_ADDED)) {
            selection = DbHelper.concatSelection(selection,
                    DbHelper.getMinSelection(Song.ADDED));
            selectionArgs.add(Long.toString(values.getLong(VAL_MIN_ADDED)));
        }

        if (values.containsKey(VAL_MAX_ADDED)) {
            selection = DbHelper.concatSelection(selection,
                    DbHelper.getMaxSelection(Song.ADDED, values.containsKey(VAL_MIN_ADDED)));
            selectionArgs.add(Long.toString(values.getLong(VAL_MAX_ADDED)));
        }

        if (rbBookmarked.isChecked()) {
            selection = DbHelper.concatSelection(selection, Song.BOOKMARKED + " IS NOT NULL");
        } else if (rbNotBookmarked.isChecked()) {
            selection = DbHelper.concatSelection(selection, Song.BOOKMARKED + " IS NULL");
        }

        if (values.containsKey(VAL_MIN_LAST_PLAYED)) {
            selection = DbHelper.concatSelection(selection,
                    DbHelper.getMinSelection(Song.LAST_PLAYED));
            selectionArgs.add(Long.toString(values.getLong(VAL_MIN_LAST_PLAYED)));
        }

        if (values.containsKey(VAL_MAX_LAST_PLAYED)) {
            selection = DbHelper.concatSelection(selection,
                    DbHelper.getMaxSelection(Song.LAST_PLAYED,
                            values.containsKey(VAL_MIN_LAST_PLAYED)));
            selectionArgs.add(Long.toString(values.getLong(VAL_MAX_LAST_PLAYED)));
        }

        String minTimesPlayed = etMinTimesPlayed.getString();
        if (minTimesPlayed != null) {
            selection = DbHelper.concatSelection(selection,
                    DbHelper.getMinSelection(Song.TIMES_PLAYED));
            selectionArgs.add(minTimesPlayed);
        }

        String maxTimesPlayed = etMaxTimesPlayed.getString();
        if (maxTimesPlayed != null) {
            selection = DbHelper.concatSelection(selection,
                    DbHelper.getMaxSelection(Song.TIMES_PLAYED, minTimesPlayed != null));
            selectionArgs.add(maxTimesPlayed);
        }

        ArrayList<String> tags = values.getStringArrayList(VAL_TAGS);
        if (!tags.isEmpty()) {
            String tagSelection = DbHelper.getInClause(tags.size());
            selection = DbHelper.concatSelection(selection, values.getBoolean(VAL_TAGS_NOT)
                    ? DbHelper.getNullOrSelection(Song.TAG, " NOT " + tagSelection)
                    : Song.TAG + " " + tagSelection);
            selectionArgs.addAll(tags);
        }

        ArrayList<Playlist> playlists = values.getParcelableArrayList(VAL_PLAYLISTS);
        if (!playlists.isEmpty()) {
            selection = DbHelper.concatSelection(selection, DbHelper.getPlaylistSongsInClause(
                    playlists.size(), values.getBoolean(VAL_PLAYLISTS_NOT)));
            for (Playlist playlist : playlists) {
                selectionArgs.add(Long.toString(playlist.getId()));
            }
        }

        return selection;
    }

    public String[] getSelectionArgs() {
        return selectionArgs.isEmpty() ? null : selectionArgs.toArray(new String[0]);
    }

    public boolean hasBookmarkedSelection() {
        return !rbAll.isChecked();
    }

    public boolean hasTagSelection() {
        return !values.getStringArrayList(VAL_TAGS).isEmpty();
    }

    public boolean hasPlaylistSelection() {
        return !values.getParcelableArrayList(VAL_PLAYLISTS).isEmpty();
    }

    private void setListButton(Button b, ArrayList<?> list, int defaultId, int otherId,
                               boolean not) {
        String s = Util.getCountString(getActivity(), list, false, defaultId, otherId);
        if (not) {
            s = getString(R.string.not_selected, s);
        }
        b.setText(s);
    }
}
