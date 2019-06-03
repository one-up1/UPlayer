package com.oneup.uplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.CalendarView;

import com.oneup.uplayer.R;
import com.oneup.uplayer.util.Calendar;
import com.oneup.uplayer.widget.EditTime;

import java.util.concurrent.TimeUnit;

public class DateTimeActivity extends AppCompatActivity
        implements CalendarView.OnDateChangeListener {
    public static final String EXTRA_TITLE_ID = "com.oneup.extra.TITLE_ID";
    public static final String EXTRA_TIME = "com.oneup.extra.TIME";

    private Calendar calendar;

    private CalendarView cvDate;
    private EditTime etTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_time);
        setTitle(getIntent().getIntExtra(EXTRA_TITLE_ID, 0));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        calendar = new Calendar();
        if (getIntent().hasExtra(EXTRA_TIME)) {
            calendar.setTime(getIntent().getLongExtra(EXTRA_TIME, 0));
        }

        cvDate = findViewById(R.id.cvDate);
        cvDate.setOnDateChangeListener(this);
        cvDate.setDate(TimeUnit.SECONDS.toMillis(calendar.getTime()));

        etTime = findViewById(R.id.etTime);
        etTime.setTime(calendar.getTimeOfDay());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_date_time, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ok:
                calendar.setTimeOfDay(etTime.getTime());
                setResult(RESULT_OK, new Intent()
                        .putExtra(EXTRA_TIME, calendar.getTime()));
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
        if (view == cvDate) {
            calendar.setDate(year, month, dayOfMonth);
        }
    }
}
