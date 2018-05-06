package com.oneup.uplayer.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.db.DbHelper;

import java.util.ArrayList;
import java.util.Collections;

//TODO: Reversing of sort order.
//TODO: @Nullable / @NotNull anotations?
//TODO: getActivity() vs getContext()

public abstract class ListFragment<T> extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "UPlayer";

    private DbHelper dbHelper;

    private int resource;
    private int contextMenuResource;

    private ListView listView;
    private ListAdapter listAdapter;
    private ArrayList<T> objects;

    private boolean sortOrderReversed;

    public ListFragment(int resource, int contextMenuResource) {
        Log.d(TAG, "ListFragment()");
        this.resource = resource;
        this.contextMenuResource = contextMenuResource;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ListFragment.onCreate()");

        dbHelper = new DbHelper(getActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "ListFragment.onCreateView()");
        if (listView == null) {
            Log.d(TAG, "Creating ListView");
            listView = (ListView) inflater.inflate(R.layout.fragment_list, container, false);
            listView.setOnItemClickListener(this);
            registerForContextMenu(listView);
        }
        return listView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (v == listView) {
            getActivity().getMenuInflater().inflate(contextMenuResource, menu);
        }
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == listView) {
            onItemClick(position, objects.get(position));
        }
    }

    public void reverseSortOrder() {
        if (objects != null) {
            Collections.reverse(objects);
            notifyDataSetChanged();
        }
        sortOrderReversed = !sortOrderReversed;
    }

    protected abstract void setRowViews(View rootView, int position, T item);

    protected abstract void onItemClick(int position, T item);

    protected abstract void onContextItemSelected(int itemId, int position, T item);

    protected DbHelper getDbHelper() {
        return dbHelper;
    }

    protected void setObjects(ArrayList<T> objects) {
        this.objects = objects;

        if (sortOrderReversed) {
            Collections.reverse(objects);
        }

        if (listAdapter == null) {
            Log.d(TAG, "Creating ListAdapter");
            listAdapter = new ListAdapter();
            listView.setAdapter(listAdapter);
        } else {
            Log.d(TAG, "Calling ListAdapter.notifyDataSetChanged()");
            listAdapter.notifyDataSetChanged();
        }
    }

    protected ArrayList<T> getObjects() {
        return objects;
    }

    protected void setSelection(int position) {
        listView.setSelection(position);
    }

    protected void notifyDataSetChanged() {
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
                view = layoutInflater.inflate(resource, parent, false);
            }
            setRowViews(view, position, objects.get(position));
            return view;
        }
    }
}
