package com.oneup.uplayer.fragment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
        super(listItemResource, listItemContextMenuResource, 0, 0, null, null);
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
        super.onCreateOptionsMenu(menu, inflater);
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
        switch (item.getItemId()) {
            case R.id.select_all:
                if (checkedListItems.size() < getData().size()) {
                    checkedListItems.clear();
                    checkedListItems.addAll(getData());
                } else {
                    checkedListItems.clear();
                }
                notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setListItemChecked(getData().get(getListItemPosition(buttonView)), isChecked);
    }

    @Override
    protected void setListItemContent(View rootView, int position, T item) {
        super.setListItemContent(rootView, position, item);

        CheckBox checkBox = rootView.findViewById(listItemCheckBoxId);
        if (checkedListItems != null) {
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(checkedListItems.contains(item));
            checkBox.setOnCheckedChangeListener(this);
        } else {
            checkBox.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onListItemClick(int position, final T item) {
        setListItemChecked(item, !checkedListItems.contains(item));
    }

    protected boolean isNotVisible() {
        return cbNot != null;
    }

    protected boolean isNotChecked() {
        return cbNot.isChecked();
    }

    protected ArrayList<T> getCheckedListItems() {
        return checkedListItems;
    }

    protected void setCheckedListItems(ArrayList<T> checkedListItems) {
        this.checkedListItems = checkedListItems;
    }

    protected void setListItemChecked(T item, boolean checked) {
        if (checked) {
            checkedListItems.add(item);
        } else {
            checkedListItems.remove(item);
        }
        notifyDataSetChanged();
    }
}
