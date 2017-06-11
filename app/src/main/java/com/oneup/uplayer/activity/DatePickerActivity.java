package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CalendarView;

import com.oneup.uplayer.R;

import java.util.Calendar;

public class DatePickerActivity extends AppCompatActivity implements
        CalendarView.OnDateChangeListener {
    public static final String EXTRA_TITLE = "com.oneup.uplayer.activity.DatePickerActivity.TITLE";
    public static final String EXTRA_DATE = "com.oneup.uplayer.activity.DatePickerActivity.DATE";

    private Calendar calendar;
    private CalendarView calendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_picker);
        setTitle(getIntent().getIntExtra(EXTRA_TITLE, 0));

        calendar = Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        calendarView = (CalendarView) findViewById(R.id.calendarView);
        calendarView.setOnDateChangeListener(this);

        long date = getIntent().getLongExtra(EXTRA_DATE, 0);
        if (date > 0) {
            calendar.setTimeInMillis(date);
            calendarView.setDate(date);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_date_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ok:
                setResult(RESULT_OK, new Intent()
                        .putExtra(EXTRA_DATE, calendar.getTimeInMillis()));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSelectedDayChange(@NonNull CalendarView view,
                                    int year, int month, int dayOfMonth) {
        if (view == calendarView) {
            calendar.set(java.util.Calendar.YEAR, year);
            calendar.set(java.util.Calendar.MONTH, month);
            calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
        }
    }
}
