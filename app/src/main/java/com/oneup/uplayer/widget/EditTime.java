package com.oneup.uplayer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.oneup.uplayer.R;
import com.oneup.uplayer.util.Util;

import java.util.concurrent.TimeUnit;

public class EditTime extends LinearLayout implements EditText.OnTextChangeListener {
    private Context context;

    private EditText etHour;
    private EditText etMinute;
    private EditText etSecond;

    public EditTime(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.view_edit_time, this, true);

        etHour = findViewById(R.id.etHour);
        etHour.setFilters(NumberFilter.getInclusiveFilters(24));
        etHour.setMinimumIntegerDigits(2);
        etHour.setOnTextChangeListener(this);

        etMinute = findViewById(R.id.etMinute);
        etMinute.setFilters(NumberFilter.getFilters(60));
        etMinute.setMinimumIntegerDigits(2);
        etMinute.setOnTextChangeListener(this);

        etSecond = findViewById(R.id.etSecond);
        etSecond.setFilters(NumberFilter.getFilters(60));
        etSecond.setMinimumIntegerDigits(2);
        etSecond.setOnTextChangeListener(this);
    }

    @Override
    public void onTextChange(EditText editText, String s) {
        if (editText == etHour) {
            int i = editText.getInt();
            if (i == 24) {
                etMinute.setInt(0);
                etSecond.setInt(0);
                onInputComplete();
            } else if (s.length() > 1) {
                etMinute.requestFocus();
            } else if (i > 2) {
                etHour.setInt(i);
                etMinute.requestFocus();
            }
        } else if (editText == etMinute) {
            if (s.length() > 1) {
                etSecond.requestFocus();
            } else {
                int i = editText.getInt();
                if (i > 5) {
                    etMinute.setInt(i);
                    etSecond.requestFocus();
                }
            }
        } else if (editText == etSecond) {
            if (s.length() > 1) {
                onInputComplete();
            } else {
                int i = editText.getInt();
                if (i > 5) {
                    etSecond.setInt(i);
                    onInputComplete();
                }
            }
        }
    }

    public long getTime() {
        return TimeUnit.HOURS.toSeconds(etHour.getLong()) +
                TimeUnit.MINUTES.toSeconds(etMinute.getLong()) +
                etSecond.getLong();
    }

    public void setTime(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }

        etHour.setLong(TimeUnit.SECONDS.toHours(seconds));
        etMinute.setLong(TimeUnit.SECONDS.toMinutes(seconds) -
                TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(seconds)));
        etSecond.setLong(seconds -
                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)));
    }

    private void onInputComplete() {
        Util.hideSoftInput(context, etSecond);
    }
}