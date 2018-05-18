package com.oneup.uplayer.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
    protected static final String ARG_SORT_COLUMNS = "sort_columns";
    protected static final String ARG_SORT_DESC = "sort_desc";

    private static final String TAG = "UPlayer";

    private int listItemResource;
    private int listItemHeaderId;
    private int listItemContentId;

    private DbHelper dbHelper;
    private String[] sortColumns;
    private boolean sortDesc;

    private ListAdapter listAdapter;
    private ArrayList<T> data;

    protected ListFragment(int listItemResource) {
        this.listItemResource = listItemResource;
    }

    protected ListFragment(int listItemResource,
                           int listItemHeaderId, int listItemContentId) {
        this.listItemResource = listItemResource;
        this.listItemHeaderId = listItemHeaderId;
        this.listItemContentId = listItemContentId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getActivity());

        Bundle args = getArguments();
        if (args != null) {
            sortColumns = args.getStringArray(ARG_SORT_COLUMNS);
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
        if (listItemHeaderId != 0) {
            position--;
        }
        onListItemClick(position, data.get(position));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Or the wrong fragment may receive the onContextItemSelected() call,
        // because there are multiple fragments with the same context menu item ID's.
        if (getUserVisibleHint()) {
            int position = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position;
            if (listItemHeaderId != 0) {
                position--;
            }
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

    protected String getOrderBy() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sortColumns.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(sortColumns[i]);
            if (sortDesc && i == 0) {
                sb.append(" DESC");
            }
        }
        return sb.toString();
    }

    protected String getActivityTitle() {
        return null;
    }

    protected void setListItemHeader(View rootView) {
    }

    protected void setListItemContent(View rootView, int position, T item) {
        // Set (or hide) info text if sort column is specified.
        if (sortColumns != null) {
            TextView tvSortColumnValue = rootView.findViewById(R.id.tvSortColumnValue);
            String sortColumnValue = getSortColumnValue(item);
            if (sortColumnValue == null) {
                tvSortColumnValue.setVisibility(View.GONE);
            } else {
                tvSortColumnValue.setText(sortColumnValue);
                tvSortColumnValue.setVisibility(View.VISIBLE);
            }
        }
    }

    protected String getSortColumnValue(T item) {
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

    protected String[] getSortColumns() {
        return sortColumns;
    }

    protected boolean isSortDesc() {
        return sortDesc;
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
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = layoutInflater.inflate(listItemResource, parent, false);
            }

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
