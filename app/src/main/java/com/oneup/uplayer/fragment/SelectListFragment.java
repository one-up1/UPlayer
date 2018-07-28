package com.oneup.uplayer.fragment;

import android.os.Bundle;
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
    private int listItemCheckBoxId;

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
                    checkedListItems = getData();
                } else {
                    checkedListItems = new ArrayList<>();
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
