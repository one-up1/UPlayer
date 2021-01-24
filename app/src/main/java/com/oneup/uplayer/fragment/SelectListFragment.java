package com.oneup.uplayer.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import androidx.core.widget.CompoundButtonCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.oneup.uplayer.R;

import java.util.ArrayList;

public abstract class SelectListFragment<T> extends ListFragment<T>
        implements CompoundButton.OnCheckedChangeListener {
    public static final String ARG_NOT = "not";

    private int listItemCheckBoxId;

    private CheckBox cbNot;

    private ArrayList<T> checkedListItems;

    protected SelectListFragment(int listItemResource, int listItemContextMenuResource,
                                 int listItemCheckBoxId) {
        super(listItemResource, listItemContextMenuResource, 0, 0, 0, null, null);
        this.listItemCheckBoxId = listItemCheckBoxId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ARG_NOT)) {
                cbNot = new CheckBox(getActivity());
                ActionBar.LayoutParams params = new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT);
                params.setMargins(getResources().getDimensionPixelSize(
                        R.dimen.action_bar_view_margin_start), 0, 0, 0);
                cbNot.setLayoutParams(params);
                cbNot.setText(R.string.not);
                cbNot.setTextColor(Color.WHITE);
                cbNot.setChecked(args.getBoolean(ARG_NOT));
                CompoundButtonCompat.setButtonTintList(cbNot, new ColorStateList(
                        new int[][]{{android.R.attr.state_checked}, {}},
                        new int[]{Color.WHITE, Color.WHITE}));

                ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
                actionBar.setDisplayShowCustomEnabled(true);
                actionBar.setCustomView(cbNot);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_select_list, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.select_all).setVisible(checkedListItems != null);
        menu.findItem(R.id.ok).setVisible(checkedListItems != null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.select_all) {
            if (checkedListItems.size() < getCount()) {
                checkedListItems = new ArrayList<>(getData());
            } else {
                checkedListItems = new ArrayList<>();
            }
            notifyDataSetChanged();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setListItemChecked(getListItem(getListItemPosition(buttonView)), isChecked);
    }

    @Override
    protected void setListItemContent(View rootView, int position, T item) {
        super.setListItemContent(rootView, position, item);

        CheckBox checkBox = rootView.findViewById(listItemCheckBoxId);
        if (checkedListItems == null) {
            checkBox.setVisibility(View.GONE);
        } else {
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(checkedListItems.contains(item));
            checkBox.setOnCheckedChangeListener(this);

            checkBox.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onListItemClick(int position, final T item) {
        setListItemChecked(item, !checkedListItems.contains(item));
        notifyDataSetChanged();
    }

    protected boolean isNotVisible() {
        return cbNot != null;
    }

    protected boolean isNotChecked() {
        return cbNot.isChecked();
    }

    protected ArrayList<T> getCheckedListItems() {
        ArrayList<T> checkedListItems = new ArrayList<>();
        for (T item : this.checkedListItems) {
            // Get the object from the data queried from the database,
            // as the objects in the checkedListItems ArrayList may not have all fields set.
            int index = getData().indexOf(item);
            if (index != -1) {
                checkedListItems.add(getData().get(index));
            }
        }
        return checkedListItems;
    }

    protected void setCheckedListItems(ArrayList<T> checkedListItems) {
        this.checkedListItems = checkedListItems;
    }

    private void setListItemChecked(T item, boolean checked) {
        if (checked) {
            checkedListItems.add(item);
        } else {
            checkedListItems.remove(item);
        }
    }
}
