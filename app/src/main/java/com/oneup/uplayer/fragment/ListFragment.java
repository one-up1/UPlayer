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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "ListFragment.onActivityCreated()");

        registerForContextMenu(getListView());
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
        Log.d(TAG, "ListFragment.reverseSortOrder()");
        if (objects == null) {
            Log.e(TAG, "objects null !! !! !!");
        } else {
            Log.d(TAG, "Reversing sort order");
            Collections.reverse(objects);
            notifyDataSetChanged();
        }
        sortOrderReversed = !sortOrderReversed;
    }

    protected void loadData() {
        Log.d(TAG, "ListFragment.loadData()");
        ArrayList<T> data = getData();

        if (data == null) {
            Log.d(TAG, "data == null");
        } else {
            if (sortOrderReversed) {
                Log.d(TAG, "Reversing sort order");
                Collections.reverse(data);
            }
            objects = data;

            if (listAdapter == null) {
                Log.d(TAG, "Creating ListAdapter");
                listAdapter = new ListAdapter();
                setListAdapter(listAdapter);
            } else {
                Log.d(TAG, "Calling ListAdapter.notifyDataSetChanged()");
                listAdapter.notifyDataSetChanged();
            }
        }
    }

    protected ArrayList<T> getData() {
        Log.d(TAG, "ListFragment.getData()");
        return null;
    }

    protected abstract void setRowViews(View rootView, int position, T item);

    protected void onListItemClick(int position, T item) {
    }

    protected void onContextItemSelected(int itemId, int position, T item) {
    }

    protected DbHelper getDbHelper() {
        return dbHelper;
    }

    protected ArrayList<T> getObjects() {
        return objects;
    }

    protected void notifyDataSetChanged() {
        Log.d(TAG, "ListFragment.notifyDataSetChanged()");
        if (listAdapter == null) {
            Log.e(TAG, "listAdapter null !! !! !!");
        } else {
            listAdapter.notifyDataSetChanged();
        }
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
