package com.oneup.uplayer.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.MainActivity;
import com.oneup.uplayer.db.DbHelper;

import java.util.ArrayList;

public abstract class ListFragment<T> extends android.support.v4.app.ListFragment
        implements View.OnClickListener {
    protected static final String ARG_SORT_COLUMN = "sort_column";
    protected static final String ARG_SORT_DESC = "sort_desc";

    private static final String TAG = "UPlayer";

    private int listItemResource;

    private DbHelper dbHelper;
    private String sortColumn;
    private boolean sortDesc;

    private ListAdapter listAdapter;
    private ArrayList<T> data;

    public ListFragment(int listItemResource) {
        this.listItemResource = listItemResource;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getActivity());

        Bundle args = getArguments();
        if (args != null) {
            sortColumn = args.getString(ARG_SORT_COLUMN);
            sortDesc = args.getBoolean(ARG_SORT_DESC);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(getListView());
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
        // Or the wrong fragment may receive the onContextItemSelected() call,
        // because there are multiple fragments with the same context menu item ID's.
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
        sortDesc = !sortDesc;
        reloadData();
    }

    protected void reloadData() {
        data = loadData();

        if (listAdapter == null) {
            listAdapter = new ListAdapter();
            setListAdapter(listAdapter);
        } else {
            listAdapter.notifyDataSetChanged();
        }

        setActivityTitle();
    }

    protected void notifyDataSetChanged() {
        Log.d(TAG, "ListFragment.notifyDataSetChanged()");
        setActivityTitle();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    protected abstract ArrayList<T> loadData();

    protected String getOrderBy(String defaultSortColumn) {
        // Sort by the set sort column (if any) and the default sort column.
        String orderBy = sortColumn == null ? defaultSortColumn : sortColumn;
        if (sortDesc) {
            orderBy += " DESC";
        }
        if (sortColumn != null) {
            orderBy += "," + defaultSortColumn;
        }
        return orderBy;
    }

    protected String getActivityTitle() {
        return null;
    }

    protected void setListItemViews(View rootView, int position, T item) {
        // Set (or hide) info text if sort column is specified.
        if (sortColumn != null) {
            TextView tvInfo = rootView.findViewById(R.id.tvInfo);
            String infoText = getInfoText(item);
            if (infoText == null) {
                tvInfo.setVisibility(View.GONE);
            } else {
                tvInfo.setText(infoText);
                tvInfo.setVisibility(View.VISIBLE);
            }
        }
    }

    protected String getInfoText(T item) {
        return null;
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

    protected String getSortColumn() {
        return sortColumn;
    }

    protected boolean isSortDesc() {
        return sortDesc;
    }

    protected ArrayList<T> getData() {
        return data;
    }

    private void setActivityTitle() {
        Activity activity = getActivity();
        if (activity != null && !(activity instanceof MainActivity)) {
            activity.setTitle(getActivityTitle());
        }
    }

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
