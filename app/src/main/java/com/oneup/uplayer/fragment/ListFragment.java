package com.oneup.uplayer.fragment;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.oneup.uplayer.db.DbHelper;

import java.util.ArrayList;
import java.util.Collections;

//TODO: Reversing of sort order.
//TODO: @Nullable / @NotNull anotations?
//TODO: getActivity() vs getContext()

public abstract class ListFragment<T> extends android.support.v4.app.ListFragment {
    private static final String TAG = "UPlayer";

    private DbHelper dbHelper;

    private int listItemResource;
    private int contextMenuResource;

    private ListAdapter listAdapter;
    private ArrayList<T> objects;

    private boolean sortOrderReversed;

    public ListFragment(@LayoutRes int listItemResource, @MenuRes int contextMenuResource) {
        Log.d(TAG, "ListFragment()");
        this.listItemResource = listItemResource;
        this.contextMenuResource = contextMenuResource;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ListFragment.onCreate()");

        dbHelper = new DbHelper(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "ListFragment.onCreateView()");

        if (view != null) {
            registerForContextMenu(view);
        }
        return view;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        Log.d(TAG, "ListFragment.onCreateContextMenu()");
        getActivity().getMenuInflater().inflate(contextMenuResource, menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "ListFragment.onResume()");

        loadData();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (getUserVisibleHint()) { //TODO: Or the wrong fragment may receive the onContextItemSelected() call?
            int position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
            onContextItemSelected(item.getItemId(), position, objects.get(position));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "ListFragment.onDestroy()");
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        onListItemClick(position, objects.get(position));
    }

    public void reverseSortOrder() {
        if (objects != null) {
            Collections.reverse(objects);
            notifyDataSetChanged();
        }
        sortOrderReversed = !sortOrderReversed;
    }

    protected void loadData() {
        Log.d(TAG, "ListFragment.loadData()");
        objects = getData();

        if (sortOrderReversed) {
            Collections.reverse(objects);
        }

        if (listAdapter == null) {
            Log.d(TAG, "Creating ListAdapter");
            listAdapter = new ListAdapter();
            setListAdapter(listAdapter);
        } else {
            Log.d(TAG, "Calling ListAdapter.notifyDataSetChanged()");
            listAdapter.notifyDataSetChanged();
        }
    }

    protected abstract ArrayList<T> getData();

    protected abstract void setRowViews(View rootView, int position, T item);

    protected abstract void onListItemClick(int position, T item);

    protected abstract void onContextItemSelected(int itemId, int position, T item);

    protected DbHelper getDbHelper() {
        return dbHelper;
    }

    protected ArrayList<T> getObjects() {
        return objects;
    }

    protected void notifyDataSetChanged() {
        Log.d(TAG, "ListFragment.notifyDataSetChanged()");
        listAdapter.notifyDataSetChanged();
    }

    private class ListAdapter extends BaseAdapter {
        private LayoutInflater layoutInflater;

        private ListAdapter() {
            layoutInflater = LayoutInflater.from(getContext());
        }

        @Override
        public int getCount() {
            return objects.size();
        }

        @Override
        public Object getItem(int position) {
            return objects.get(position);
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
            setRowViews(view, position, objects.get(position));
            return view;
        }
    }
}
