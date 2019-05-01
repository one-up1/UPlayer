package com.oneup.uplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.oneup.uplayer.MainService;
import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.DateTimeActivity;
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
    private RadioGroup rgBookmarked;
    private RadioGroup rgArchived;
    private Button bTags;
    private Button bPlaylists;
    private Button bMinLastPlayed;
    private Button bMaxLastPlayed;

    private boolean showArtistFilter;
    private int selectPlaylistConfirmId;
    private Values values;
    private String selection;
    private ArrayList<String> selectionArgs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_filter, container, false);

        etTitle = rootView.findViewById(R.id.etTitle);

        etArtist = rootView.findViewById(R.id.etArtist);

        etMinYear = rootView.findViewById(R.id.etMinYear);

        etMaxYear = rootView.findViewById(R.id.etMaxYear);

        bMinAdded = rootView.findViewById(R.id.bMinAdded);
        bMinAdded.setOnClickListener(this);
        bMinAdded.setOnLongClickListener(this);

        bMaxAdded = rootView.findViewById(R.id.bMaxAdded);
        bMaxAdded.setOnClickListener(this);
        bMaxAdded.setOnLongClickListener(this);

        rgBookmarked = rootView.findViewById(R.id.rgBookmarked);

        rgArchived = rootView.findViewById(R.id.rgArchived);

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

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == AppCompatActivity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_SELECT_MIN_ADDED:
                    values.minAdded = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMinAdded.setText(Util.formatDateTime(values.minAdded));
                    break;
                case REQUEST_SELECT_MAX_ADDED:
                    values.maxAdded = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMaxAdded.setText(Util.formatDateTime(values.maxAdded));
                    break;
                case REQUEST_SELECT_TAGS:
                    values.tags = data.getStringArrayListExtra(TagsActivity.EXTRA_TAGS);
                    values.tagsNot = data.getBooleanExtra(TagsActivity.TagsFragment.ARG_NOT, false);
                    setListButton(bTags, values.tags, values.tagsNot,
                            R.string.select_tags, R.string.selected_tags);
                case REQUEST_SELECT_PLAYLISTS:
                    if (data.hasExtra(PlaylistsActivity.EXTRA_PLAYLIST)) {
                        getActivity().startService(new Intent(getActivity(), MainService.class)
                                .putExtra(MainService.EXTRA_ACTION,
                                        MainService.ACTION_PLAY)
                                .putExtra(MainService.EXTRA_PLAYLIST,
                                        data.getParcelableExtra(PlaylistsActivity.EXTRA_PLAYLIST)));
                    } else if (data.hasExtra(PlaylistsActivity.EXTRA_PLAYLISTS)) {
                        values.playlists = data.getParcelableArrayListExtra(
                                PlaylistsActivity.EXTRA_PLAYLISTS);
                        values.playlistsNot = data.getBooleanExtra(
                                TagsActivity.TagsFragment.ARG_NOT, false);
                        setListButton(bPlaylists, values.playlists, values.playlistsNot,
                                R.string.select_playlists, R.string.selected_playlists);
                    }
                    break;
                case REQUEST_SELECT_MIN_LAST_PLAYED:
                    values.minLastPlayed = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMinLastPlayed.setText(Util.formatDateTime(values.minLastPlayed));
                    break;
                case REQUEST_SELECT_MAX_LAST_PLAYED:
                    values.maxLastPlayed = data.getLongExtra(DateTimeActivity.EXTRA_TIME, 0);
                    bMaxLastPlayed.setText(Util.formatDateTime(values.maxLastPlayed));
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == bMinAdded) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_min_added);
            if (values.minAdded != 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, values.minAdded);
            }
            startActivityForResult(intent, REQUEST_SELECT_MIN_ADDED);
        } else if (v == bMaxAdded) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_max_added);
            if (values.maxAdded != 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, values.maxAdded);
            }
            startActivityForResult(intent, REQUEST_SELECT_MAX_ADDED);
        } else if (v == bTags) {
            startActivityForResult(new Intent(getActivity(), TagsActivity.class)
                            .putExtras(TagsActivity.TagsFragment.getArguments(
                                    values.tags, values.tagsNot)),
                    REQUEST_SELECT_TAGS);
        } else if (v == bPlaylists) {
            startActivityForResult(new Intent(getActivity(), PlaylistsActivity.class)
                            .putExtras(PlaylistsActivity.PlaylistsFragment.getArguments(
                                    values.playlists, values.playlistsNot,
                                    selectPlaylistConfirmId)),
                    REQUEST_SELECT_PLAYLISTS);
        } else if (v == bMinLastPlayed) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_min_last_played);
            if (values.minLastPlayed != 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, values.minLastPlayed);
            }
            startActivityForResult(intent, REQUEST_SELECT_MIN_LAST_PLAYED);
        } else if (v == bMaxLastPlayed) {
            Intent intent = new Intent(getActivity(), DateTimeActivity.class);
            intent.putExtra(DateTimeActivity.EXTRA_TITLE_ID, R.string.select_max_last_played);
            if (values.maxLastPlayed != 0) {
                intent.putExtra(DateTimeActivity.EXTRA_TIME, values.maxLastPlayed);
            }
            startActivityForResult(intent, REQUEST_SELECT_MAX_LAST_PLAYED);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == bMinAdded) {
            values.minAdded = 0;
            bMinAdded.setText(R.string.select_min_added);
        } else if (v == bMaxAdded) {
            values.maxAdded = 0;
            bMaxAdded.setText(R.string.select_max_added);
        } else if (v == bTags) {
            values.tags.clear();
            values.tagsNot = false;
            bTags.setText(R.string.select_tags);
        } else if (v == bPlaylists) {
            values.playlists.clear();
            values.playlistsNot = false;
            bPlaylists.setText(R.string.select_playlists);
        } else if (v == bMinLastPlayed) {
            values.minAdded = 0;
            bMinLastPlayed.setText(R.string.select_min_last_played);
        } else if (v == bMaxLastPlayed) {
            values.maxAdded = 0;
            bMaxLastPlayed.setText(R.string.select_max_last_played);
        }
        return true;
    }

    public void setShowArtistFilter(boolean showArtistFilter) {
        this.showArtistFilter = showArtistFilter;
        etArtist.setVisibility(showArtistFilter ? View.VISIBLE : View.GONE);
    }

    public void setSelectPlaylistConfirmId(int selectPlaylistConfirmId) {
        this.selectPlaylistConfirmId = selectPlaylistConfirmId;
    }

    public Values getValues() {
        values.title = etTitle.getString();
        if (showArtistFilter) {
            values.artist = etArtist.getString();
        }
        values.minYear = etMinYear.getString();
        values.maxYear = etMaxYear.getString();
        values.bookmarked = getCheckedRadioButtonIndex(rgBookmarked);
        values.archived = getCheckedRadioButtonIndex(rgArchived);
        return values;
    }

    public void setValues(Values values) {
        if (values == null) {
            values = new Values();
        }
        etTitle.setString(values.title);
        if (showArtistFilter) {
            etArtist.setString(values.artist);
        }
        etMinYear.setString(values.minYear);
        etMaxYear.setString(values.maxYear);
        setDateButton(bMinAdded, values.minAdded, R.string.select_min_added);
        setDateButton(bMaxAdded, values.maxAdded, R.string.select_max_added);
        setCheckedRadioButtonIndex(rgBookmarked, values.bookmarked);
        setCheckedRadioButtonIndex(rgArchived, values.archived);
        setListButton(bTags, values.tags, values.tagsNot,
                R.string.select_tags, R.string.selected_tags);
        setListButton(bPlaylists, values.playlists, values.playlistsNot,
                R.string.select_playlists, R.string.selected_playlists);
        setDateButton(bMinLastPlayed, values.minLastPlayed, R.string.select_min_last_played);
        setDateButton(bMaxLastPlayed, values.maxLastPlayed, R.string.select_max_last_played);
        this.values = values;
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

        if (values.minAdded != 0) {
            selection = DbHelper.concatSelection(selection,
                    DbHelper.getMinSelection(Song.ADDED));
            selectionArgs.add(Long.toString(values.minAdded));
        }

        if (values.maxAdded != 0) {
            selection = DbHelper.concatSelection(selection,
                    DbHelper.getMaxSelection(Song.ADDED, values.minAdded != 0));
            selectionArgs.add(Long.toString(values.maxAdded));
        }

        switch (rgBookmarked.getCheckedRadioButtonId()) {
            case R.id.rbBookmarked:
                selection = DbHelper.concatSelection(selection, Song.BOOKMARKED + " IS NOT NULL");
                break;
            case R.id.rbUnbookmarked:
                selection = DbHelper.concatSelection(selection, Song.BOOKMARKED + " IS NULL");
                break;
        }

        switch (rgArchived.getCheckedRadioButtonId()) {
            case R.id.rbArchived:
                selection = DbHelper.concatSelection(selection, Song.ARCHIVED + " IS NOT NULL");
                break;
            case R.id.rbUnarchived:
                selection = DbHelper.concatSelection(selection, Song.ARCHIVED + " IS NULL");
                break;
        }

        if (values.minLastPlayed != 0) {
            selection = DbHelper.concatSelection(selection,
                    DbHelper.getMinSelection(Song.LAST_PLAYED));
            selectionArgs.add(Long.toString(values.minLastPlayed));
        }

        if (values.maxLastPlayed != 0) {
            selection = DbHelper.concatSelection(selection,
                    DbHelper.getMaxSelection(Song.LAST_PLAYED, values.minLastPlayed != 0));
            selectionArgs.add(Long.toString(values.maxLastPlayed));
        }

        if (!values.tags.isEmpty()) {
            String tagSelection = DbHelper.getInClause(values.tags.size());
            selection = DbHelper.concatSelection(selection, values.tagsNot
                    ? DbHelper.getNullOrSelection(Song.TAG, " NOT " + tagSelection)
                    : Song.TAG + " " + tagSelection);
            selectionArgs.addAll(values.tags);
        }

        if (!values.playlists.isEmpty()) {
            selection = DbHelper.concatSelection(selection, DbHelper.getPlaylistSongsInClause(
                    values.playlists.size(), values.playlistsNot));
            for (Playlist playlist : values.playlists) {
                selectionArgs.add(Long.toString(playlist.getId()));
            }
        }

        return selection;
    }

    public String[] getSelectionArgs() {
        return selectionArgs.isEmpty() ? null : selectionArgs.toArray(new String[0]);
    }

    private void setDateButton(Button button, long value, int defaultId) {
        button.setText(value == 0 ? getString(defaultId) : Util.formatDateTime(value));
    }

    private void setListButton(Button button, ArrayList<?> values, boolean not,
                               int defaultId, int otherId) {
        String s = Util.getCountString(getActivity(), values, false, defaultId, otherId);
        if (not) {
            s = getString(R.string.not_selected, s);
        }
        button.setText(s);
    }

    private static int getCheckedRadioButtonIndex(RadioGroup radioGroup) {
        return radioGroup.indexOfChild(radioGroup.findViewById(
                radioGroup.getCheckedRadioButtonId()));
    }

    private static void setCheckedRadioButtonIndex(RadioGroup radioGroup, int index) {
        ((RadioButton) radioGroup.getChildAt(index)).setChecked(true);
    }

    public static class Values implements Parcelable {
        private String title;
        private String artist;

        private String minYear;
        private String maxYear;

        private long minAdded;
        private long maxAdded;

        private int bookmarked;
        private int archived;

        private ArrayList<String> tags;
        private boolean tagsNot;

        private ArrayList<Playlist> playlists;
        private boolean playlistsNot;

        private long minLastPlayed;
        private long maxLastPlayed;

        public Values() {
            tags = new ArrayList<>();
            playlists = new ArrayList<>();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(title);
            out.writeString(artist);
            out.writeString(minYear);
            out.writeString(maxYear);
            out.writeLong(minAdded);
            out.writeLong(maxAdded);
            out.writeInt(bookmarked);
            out.writeInt(archived);
            out.writeStringList(tags);
            out.writeInt(tagsNot ? 1 : 0);
            out.writeTypedList(playlists);
            out.writeInt(playlistsNot ? 1 : 0);
            out.writeLong(minLastPlayed);
            out.writeLong(maxLastPlayed);
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getMinYear() {
            return minYear;
        }

        public void setMinYear(String minYear) {
            this.minYear = minYear;
        }

        public String getMaxYear() {
            return maxYear;
        }

        public void setMaxYear(String maxYear) {
            this.maxYear = maxYear;
        }

        public long getMinAdded() {
            return minAdded;
        }

        public void setMinAdded(long minAdded) {
            this.minAdded = minAdded;
        }

        public long getMaxAdded() {
            return maxAdded;
        }

        public void setMaxAdded(long maxAdded) {
            this.maxAdded = maxAdded;
        }

        public int getBookmarked() {
            return bookmarked;
        }

        public void setBookmarked(int bookmarked) {
            this.bookmarked = bookmarked;
        }

        public int getArchived() {
            return archived;
        }

        public void setArchived(int archived) {
            this.archived = archived;
        }

        public ArrayList<String> getTags() {
            return tags;
        }

        public boolean isTagsNot() {
            return tagsNot;
        }

        public void setTagsNot(boolean tagsNot) {
            this.tagsNot = tagsNot;
        }

        public ArrayList<Playlist> getPlaylists() {
            return playlists;
        }

        public boolean isPlaylistsNot() {
            return playlistsNot;
        }

        public void setPlaylistsNot(boolean playlistsNot) {
            this.playlistsNot = playlistsNot;
        }

        public long getMinLastPlayed() {
            return minLastPlayed;
        }

        public void setMinLastPlayed(long minLastPlayed) {
            this.minLastPlayed = minLastPlayed;
        }

        public long getMaxLastPlayed() {
            return maxLastPlayed;
        }

        public void setMaxLastPlayed(long maxLastPlayed) {
            this.maxLastPlayed = maxLastPlayed;
        }

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {

            @Override
            public Object createFromParcel(Parcel in) {
                Values values = new Values();
                values.title = in.readString();
                values.artist = in.readString();
                values.minYear = in.readString();
                values.maxYear = in.readString();
                values.minAdded = in.readLong();
                values.maxAdded = in.readLong();
                values.bookmarked = in.readInt();
                values.archived = in.readInt();
                in.readStringList(values.tags);
                values.tagsNot = in.readInt() == 1;
                //noinspection unchecked
                in.readTypedList(values.playlists, Playlist.CREATOR);
                values.playlistsNot = in.readInt() == 1;
                values.minLastPlayed = in.readLong();
                values.maxLastPlayed = in.readLong();
                return values;
            }

            @Override
            public Object[] newArray(int size) {
                return new Values[size];
            }
        };
    }
}
