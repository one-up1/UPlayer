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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.MainActivity;
import com.oneup.uplayer.db.DbHelper;

import java.util.ArrayList;

public abstract class ListFragment<T> extends android.support.v4.app.ListFragment
        implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    protected static final String ARG_SORT_COLUMN = "sort_column";
    protected static final String ARG_SORT_DESC = "sort_desc";
    protected static final String ARG_CHECKBOX_VISIBLE = "checkbox_visible";

    private static final String TAG = "UPlayer";

    private int listItemResource;
    private int listItemContextMenuResource;
    private int listItemHeaderId;
    private int listItemContentId;
    private int listItemCheckBoxId;

    private String[] columns;
    private String[] sortColumns;

    private DbHelper dbHelper;
    private int sortColumn;
    private boolean sortDesc;
    private boolean checkboxVisible;

    private ListAdapter listAdapter;
    private ArrayList<T> data;
    private boolean[] checkedListItems;

    protected ListFragment(int listItemResource, int listItemContextMenuResource,
                           int listItemHeaderId, int listItemContentId,
                           int listItemCheckBoxId, String[] columns, String[] sortColumns) {
        this.listItemResource = listItemResource;
        this.listItemContextMenuResource = listItemContextMenuResource;
        this.listItemHeaderId = listItemHeaderId;
        this.listItemContentId = listItemContentId;
        this.listItemCheckBoxId = listItemCheckBoxId;

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
            checkboxVisible = args.getBoolean(ARG_CHECKBOX_VISIBLE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (listItemContextMenuResource != 0) {
            registerForContextMenu(getListView());
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (listItemContextMenuResource != 0) {
            getActivity().getMenuInflater().inflate(listItemContextMenuResource, menu);
        }
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
    public void onClick(View v) {
        int position = getListItemPosition(v);
        onListItemViewClick(v.getId(), position, data.get(position));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        checkedListItems[getListItemPosition(buttonView)] = isChecked;
    }

    public int getSortColumn() {
        return sortColumn;
    }

    public void setSortColumn(int sortColumn) {
        this.sortColumn = sortColumn;
    }

    public boolean isSortDesc() {
        return sortDesc;
    }

    public void setSortDesc(boolean sortDesc) {
        this.sortDesc = sortDesc;
    }

    public boolean isCheckboxVisible() {
        return checkboxVisible;
    }

    public void reverseSortOrder() {
        sortDesc = !sortDesc;
        reloadData();
    }

    protected void reloadData() {
        data = loadData();
        if (listItemCheckBoxId != 0) {
            checkedListItems = new boolean[data.size()];
        }

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
        // Set CheckBox if specified.
        if (listItemCheckBoxId != 0) {
            CheckBox checkBox = rootView.findViewById(listItemCheckBoxId);
            if (checkboxVisible) {
                checkBox.setChecked(checkedListItems[position]);
                checkBox.setOnCheckedChangeListener(this);
            } else {
                checkBox.setVisibility(View.GONE);
            }
        }

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

    protected void setListItemViewOnClickListener(View rootView, int viewId) {
        rootView.findViewById(viewId).setOnClickListener(this);
    }

    protected String getSortColumnValue(int sortColumn, T item) {
        return null;
    }

    protected void onListItemClick(int position, T item) {
    }

    protected void onContextItemSelected(int itemId, int position, T item) {
    }

    protected void onListItemViewClick(int viewId, int position, T item) {
    }

    protected DbHelper getDbHelper() {
        return dbHelper;
    }

    protected void setSortColumns(String[] sortColumns) {
        this.sortColumns = sortColumns;
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

    public ArrayList<T> getCheckedListItems() {
        ArrayList<T> items = new ArrayList<>();
        for (int i = 0; i < checkedListItems.length; i++) {
            if (checkedListItems[i]) {
                items.add(data.get(i));
            }
        }
        return items;
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

    private int getListItemPosition(View v) {
        return getListItemPosition(getListView().getPositionForView((View) v.getParent()));
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
