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
import android.widget.ListView;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.activity.MainActivity;
import com.oneup.uplayer.db.DbHelper;

import java.util.ArrayList;

public abstract class ListFragment<T>
        extends androidx.fragment.app.ListFragment
        implements ListView.OnItemLongClickListener, View.OnClickListener {
    protected static final String ARG_SELECTION = "selection";
    protected static final String ARG_SELECTION_ARGS = "selection_args";
    protected static final String ARG_SORT_COLUMN = "sort_column";
    protected static final String ARG_SORT_DESC = "sort_desc";

    private static final String TAG = "UPlayer";

    private final int listItemResource;
    private final int listItemContextMenuResource;
    private final int listItemHeaderId;
    private final int listItemContentId;
    private final int listItemInfoId;

    private final String[] sortColumnValues;
    private String[] sortColumns;

    private DbHelper dbHelper;
    private String selection;
    private String[] selectionArgs;
    private int sortColumn;
    private boolean sortDesc;

    private ListAdapter listAdapter;
    private ArrayList<T> data;

    protected ListFragment(int listItemResource, int listItemContextMenuResource,
                           int listItemHeaderId, int listItemContentId, int listItemInfoId,
                           String[] sortColumnValues, String[] sortColumns) {
        this.listItemResource = listItemResource;
        this.listItemContextMenuResource = listItemContextMenuResource;
        this.listItemHeaderId = listItemHeaderId;
        this.listItemContentId = listItemContentId;
        this.listItemInfoId = listItemInfoId;

        this.sortColumnValues = sortColumnValues;
        this.sortColumns = sortColumns;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getActivity());

        Bundle args = getArguments();
        if (args != null) {
            selection = args.getString(ARG_SELECTION);
            selectionArgs = args.getStringArray(ARG_SELECTION_ARGS);
            sortColumn = args.getInt(ARG_SORT_COLUMN);
            sortDesc = args.getBoolean(ARG_SORT_DESC);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (listItemContextMenuResource != 0) {
            registerForContextMenu(getListView());
        }
        getListView().setOnItemLongClickListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        if (listItemContextMenuResource != 0) {
            getActivity().getMenuInflater().inflate(listItemContextMenuResource, menu);
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // getUserVisibleHint() or the wrong fragment may receive the onContextItemSelected() call,
        // because there are multiple fragments with the same context menu item ID's.
        if (getUserVisibleHint()) {
            int position = getListItemPosition(item.getMenuInfo());
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
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        position = getListItemPosition(position);
        return onListItemLongClick(position, data.get(position));
    }

    @Override
    public void onClick(View v) {
        int position = getListItemPosition(v);
        onListItemViewClick(v.getId(), position, data.get(position));
    }

    public void reloadData() {
        data = loadData();
        setActivityTitle();

        if (listAdapter == null) {
            listAdapter = new ListAdapter();
            setListAdapter(listAdapter);
        } else {
            listAdapter.notifyDataSetChanged();
        }

        getActivity().invalidateOptionsMenu();
    }

    public void reverseSortOrder() {
        sortDesc = !sortDesc;
        reloadData();
    }

    public int getSortColumn() {
        return sortColumn;
    }

    public boolean isSortDesc() {
        return sortDesc;
    }

    protected abstract ArrayList<T> loadData();

    protected String getActivityTitle() {
        return null;
    }

    protected void setListItemHeader(View rootView) {
    }

    protected void setListItemContent(View rootView, int position, T item) {
        // Set (or hide) info when specified.
        if (listItemInfoId != 0) {
            TextView tvInfo = rootView.findViewById(R.id.tvInfo);
            String info = getListItemInfo(item);
            if (info == null) {
                tvInfo.setVisibility(View.GONE);
            } else {
                tvInfo.setText(info);
                tvInfo.setVisibility(View.VISIBLE);
            }
        }
    }

    protected String getListItemInfo(T item) {
        return null;
    }

    protected void setListItemViewOnClickListener(View rootView, int viewId) {
        rootView.findViewById(viewId).setOnClickListener(this);
    }

    protected void onListItemClick(int position, T item) {
    }

    protected boolean onListItemLongClick(int position, T item) {
        return false;
    }

    protected void onContextItemSelected(int itemId, int position, T item) {
    }

    protected void onListItemViewClick(int viewId, int position, T item) {
    }

    protected void removeListItem(int position) {
        getData().remove(position);
        notifyDataSetChanged();
    }

    protected void setSortColumns(String[] sortColumns) {
        this.sortColumns = sortColumns;
    }

    protected DbHelper getDbHelper() {
        return dbHelper;
    }

    protected String getSelection() {
        return selection;
    }

    protected void setSelection(String selection) {
        this.selection = selection;
    }

    protected String[] getSelectionArgs() {
        return selectionArgs;
    }

    protected void setSelectionArgs(String[] selectionArgs) {
        this.selectionArgs = selectionArgs;
    }

    protected void setSortColumn(int sortColumn) {
        this.sortColumn = sortColumn;
        reloadData();
    }

    protected void setSortDesc(boolean sortDesc) {
        this.sortDesc = sortDesc;
        reloadData();
    }

    protected String getOrderBy() {
        sortColumns[0] = sortColumnValues[sortColumn];
        boolean sortDesc = this.sortDesc;

        StringBuilder orderBy = new StringBuilder();
        for (String sortColumn : sortColumns) {
            if (sortColumn != null) {
                if (orderBy.length() > 0) {
                    orderBy.append(',');
                }
                orderBy.append(sortColumn);

                if (sortDesc) {
                    orderBy.append(" DESC");
                    sortDesc = false;
                }
            }
        }
        return orderBy.toString();
    }

    protected void notifyDataSetChanged() {
        Log.d(TAG, "ListFragment.notifyDataSetChanged()");
        setActivityTitle();

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    protected ArrayList<T> getData() {
        return data;
    }

    protected int getCount() {
        return data.size();
    }

    protected T getListItem(int position) {
        return data.get(position);
    }

    protected int getListItemPosition(View v) {
        return getListItemPosition(getListView().getPositionForView((View) v.getParent()));
    }

    protected int getListItemPosition(ContextMenu.ContextMenuInfo menuInfo) {
        return getListItemPosition(((AdapterView.AdapterContextMenuInfo) menuInfo).position);
    }

    private void setActivityTitle() {
        Activity activity = getActivity();
        if (activity != null && !(activity instanceof MainActivity)) {
            String title = getActivityTitle();
            if (title != null) {
                activity.setTitle(getActivityTitle());
            }
        }
    }

    private int getListItemPosition(int position) {
        return listItemHeaderId == 0 ? position : position - 1;
    }

    private class ListAdapter extends BaseAdapter {
        private final LayoutInflater layoutInflater;

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
