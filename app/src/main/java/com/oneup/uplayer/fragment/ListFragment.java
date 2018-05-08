package com.oneup.uplayer.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import com.oneup.uplayer.activity.MainActivity;
import com.oneup.uplayer.db.DbHelper;

import java.util.ArrayList;
import java.util.Collections;

public abstract class ListFragment<T> extends android.support.v4.app.ListFragment
        implements View.OnClickListener {
    private static final String TAG = "UPlayer";

    private DbHelper dbHelper;

    private int listItemResource;
    private int contextMenuResource;

    private ListAdapter listAdapter;
    private ArrayList<T> data;

    private boolean sortOrderReversed;

    public ListFragment(int listItemResource, int contextMenuResource) {
        this.listItemResource = listItemResource;
        this.contextMenuResource = contextMenuResource;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(getListView());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().getMenuInflater().inflate(contextMenuResource, menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();
    }

    @Override
    public void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        onListItemClick(position, data.get(position));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        //TODO: Or the wrong fragment may receive the onContextItemSelected() call?
        if (getUserVisibleHint()) {
            int position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
            onContextItemSelected(item.getItemId(), position, data.get(position));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        //noinspection unchecked
        onListItemButtonClick(v.getId(), (T) v.getTag());
    }

    public void reverseSortOrder() {
        Log.d(TAG, "ListFragment.reverseSortOrder()");
        if (data != null) {
            Log.d(TAG, "Reversing sort order");
            Collections.reverse(data);
            listAdapter.notifyDataSetChanged();
        }
        sortOrderReversed = !sortOrderReversed;
    }

    protected void updateList() {
        ArrayList<T> data = loadData();
        if (data != null) {
            if (sortOrderReversed) {
                Log.d(TAG, "Reversing sort order");
                Collections.reverse(data);
            }
            this.data = data;

            if (listAdapter == null) {
                listAdapter = new ListAdapter();
                setListAdapter(listAdapter);
            } else {
                listAdapter.notifyDataSetChanged();
            }

            setActivityTitle();
        }
    }

    protected void setActivityTitle() {
        Activity activity = getActivity();
        if (activity != null && !(activity instanceof MainActivity)) {
            String title = getActivityTitle();
            if (title != null) {
                activity.setTitle(title);
            }
        }
    }

    protected String getActivityTitle() {
        return null;
    }

    protected void notifyDataSetChanged() {
        Log.d(TAG, "ListFragment.notifyDataSetChanged()");
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    protected void setListItemViews(View rootView, int position, T item) {
    }

    protected void setListItemButton(View rootView, int buttonId, T item) {
        ImageButton button = rootView.findViewById(buttonId);
        button.setTag(item);
        button.setOnClickListener(this);
    }

    protected void onListItemClick(int position, T item) {
    }

    protected void onContextItemSelected(int itemId, int position, T item) {
    }

    protected void onListItemButtonClick(int buttonId, T item) {
    }

    protected DbHelper getDbHelper() {
        return dbHelper;
    }

    protected ArrayList<T> getData() {
        return data;
    }

    protected abstract ArrayList<T> loadData();

    private class ListAdapter extends BaseAdapter {
        private LayoutInflater layoutInflater;

        private ListAdapter() {
            layoutInflater = LayoutInflater.from(getActivity());
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = layoutInflater.inflate(listItemResource, parent, false);
            }
            setListItemViews(view, position, data.get(position));
            return view;
        }
    }
}
