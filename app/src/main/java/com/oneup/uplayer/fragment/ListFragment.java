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
    private int listItemHeaderId;
    private int listItemContentId;

    private String[] columns;
    private String[] sortColumns;

    private DbHelper dbHelper;
    private int sortColumn;
    private boolean sortDesc;

    private ListAdapter listAdapter;
    private ArrayList<T> data;

    protected ListFragment(int listItemResource, int listItemHeaderId, int listItemContentId,
                           String[] columns, String[] sortColumns) {
        this.listItemResource = listItemResource;
        this.listItemHeaderId = listItemHeaderId;
        this.listItemContentId = listItemContentId;
        this.columns = columns;
        this.sortColumns = sortColumns;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getActivity());

        Bundle args = getArguments();
        if (args != null) {
            sortColumn = args.getInt(ARG_SORT_COLUMN);
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
        position = getListItemPosition(position);
        onListItemClick(position, data.get(position));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // getUserVisibleHint() or the wrong fragment may receive the onContextItemSelected() call,
        // because there are multiple fragments with the same context menu item ID's.
        if (getUserVisibleHint()) {
            int position = getListItemPosition(
                    ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
            onContextItemSelected(item.getItemId(), position, data.get(position));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        int position = getListItemPosition(getListView().getPositionForView((View) v.getParent()));
        onListItemButtonClick(v.getId(), position, data.get(position));
    }

    public void reverseSortOrder() {
        sortDesc = !sortDesc;
        reloadData();
    }

    protected void reloadData() {
        data = loadData();
        setActivityTitle();

        if (listAdapter == null) {
            listAdapter = new ListAdapter();
            setListAdapter(listAdapter);
        } else {
            listAdapter.notifyDataSetChanged();
        }
    }

    protected void notifyDataSetChanged() {
        Log.d(TAG, "ListFragment.notifyDataSetChanged()");
        setActivityTitle();

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    protected abstract ArrayList<T> loadData();

    protected String getActivityTitle() {
        return null;
    }

    protected void setListItemHeader(View rootView) {
    }

    protected void setListItemContent(View rootView, int position, T item) {
        // Set (or hide) sort column value if sort columns are specified.
        if (columns != null && sortColumns != null) {
            TextView tvSortColumnValue = rootView.findViewById(R.id.tvSortColumnValue);
            String sortColumnValue = getSortColumnValue(sortColumn, item);
            if (sortColumnValue == null) {
                tvSortColumnValue.setVisibility(View.GONE);
            } else {
                tvSortColumnValue.setText(sortColumnValue);
                tvSortColumnValue.setVisibility(View.VISIBLE);
            }
        }
    }

    protected void setListItemButton(View rootView, int buttonId) {
        rootView.findViewById(buttonId).setOnClickListener(this);
    }

    protected String getSortColumnValue(int sortColumn, T item) {
        return null;
    }

    protected void onListItemClick(int position, T item) {
    }

    protected void onContextItemSelected(int itemId, int position, T item) {
    }

    protected void onListItemButtonClick(int buttonId, int position, T item) {
    }

    protected DbHelper getDbHelper() {
        return dbHelper;
    }

    protected void setSortColumns(String[] sortColumns) {
        this.sortColumns = sortColumns;
    }

    protected int getSortColumn() {
        return sortColumn;
    }

    protected void setSortColumn(int sortColumn) {
        this.sortColumn = sortColumn;
    }

    protected boolean isSortDesc() {
        return sortDesc;
    }

    protected void setSortDesc(boolean sortDesc) {
        this.sortDesc = sortDesc;
    }

    protected String getOrderBy() {
        sortColumns[0] = columns[sortColumn];
        boolean sortDesc = this.sortDesc;

        StringBuilder orderBy = new StringBuilder();
        for (String sortColumn : sortColumns) {
            if (sortColumn == null) {
                continue;
            }
            if (orderBy.length() > 0) {
                orderBy.append(',');
            }
            orderBy.append(sortColumn);
            if (sortDesc) {
                orderBy.append(" DESC");
                sortDesc = false;
            }
        }
        return orderBy.toString();
    }

    protected ArrayList<T> getData() {
        return data;
    }

    protected int getCount() {
        return data.size();
    }

    private void setActivityTitle() {
        Activity activity = getActivity();
        if (activity != null && !(activity instanceof MainActivity)) {
            activity.setTitle(getActivityTitle());
        }
    }

    private int getListItemPosition(int position) {
        return listItemHeaderId == 0 ? position : position - 1;
    }

    private class ListAdapter extends BaseAdapter {
        private LayoutInflater layoutInflater;

        private ListAdapter() {
            layoutInflater = LayoutInflater.from(getActivity());
        }

        @Override
        public int getCount() {
            int count = data.size();
            if (listItemHeaderId != 0) {
                count++;
            }
            return count;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView == null ?
                    layoutInflater.inflate(listItemResource, parent, false) : convertView;

            if (listItemHeaderId == 0) {
                setListItemContent(view, position, data.get(position));
            } else {
                View headerView = view.findViewById(listItemHeaderId);
                View contentView = view.findViewById(listItemContentId);

                if (position == 0) {
                    setListItemHeader(headerView);

                    headerView.setVisibility(View.VISIBLE);
                    contentView.setVisibility(View.GONE);
                } else {
                    position--;
                    setListItemContent(view, position, data.get(position));

                    headerView.setVisibility(View.GONE);
                    contentView.setVisibility(View.VISIBLE);
                }
            }

            return view;
        }
    }
}
