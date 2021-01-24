package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.fragment.SelectListFragment;
import com.oneup.uplayer.util.Util;

import java.util.ArrayList;

public class TagsActivity extends AppCompatActivity {
    public static final String EXTRA_TAGS = "com.oneup.extra.TAGS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout container = new FrameLayout(this);
        container.setId(R.id.container);
        setContentView(container);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, TagsFragment.newInstance(getIntent().getExtras()))
                    .commit();
        }
    }

    public static class TagsFragment extends SelectListFragment<String> {
        private static final String ARG_CHECKED_TAGS = "checked_tags";

        public TagsFragment() {
            super(R.layout.list_item_tag, 0, R.id.checkBox);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Bundle args = getArguments();
            if (args != null) {
                setCheckedListItems(args.getStringArrayList(ARG_CHECKED_TAGS));
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            reloadData();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.ok) {
                getActivity().setResult(AppCompatActivity.RESULT_OK, new Intent()
                        .putExtra(ARG_NOT, isNotChecked())
                        .putStringArrayListExtra(EXTRA_TAGS, getCheckedListItems()));
                getActivity().finish();
            } else {
                return super.onOptionsItemSelected(item);
            }
            return true;
        }

        @Override
        protected ArrayList<String> loadData() {
            return getDbHelper().querySongTags();
        }

        @Override
        protected String getActivityTitle() {
            return Util.getCountString(getActivity(), R.plurals.tags, getCount());
        }

        @Override
        protected void setListItemContent(View rootView, int position, String tag) {
            super.setListItemContent(rootView, position, tag);

            TextView tvTag = rootView.findViewById(R.id.tvTag);
            tvTag.setText(tag);
        }

        public static Bundle getArguments(ArrayList<String> checkedTags, boolean not) {
            Bundle args = new Bundle();
            args.putStringArrayList(ARG_CHECKED_TAGS, checkedTags);
            args.putBoolean(ARG_NOT, not);
            return args;
        }

        private static TagsFragment newInstance(Bundle args) {
            TagsFragment fragment = new TagsFragment();
            fragment.setArguments(args);
            return fragment;
        }
    }
}
