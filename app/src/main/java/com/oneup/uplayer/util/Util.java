package com.oneup.uplayer.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Environment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.oneup.uplayer.R;

import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

@SuppressLint("SimpleDateFormat")
public class Util {
    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private static final DateFormat DATE_TIME_FORMAT_WEEKDAY =
            new SimpleDateFormat("E dd-MM-yyyy HH:mm");
    private static final NumberFormat TIME_NUMBER_FORMAT = NumberFormat.getInstance();
    private static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance();

    static {
        TIME_NUMBER_FORMAT.setMinimumIntegerDigits(2);
        PERCENT_FORMAT.setMaximumFractionDigits(1);
    }

    private Util() {
    }

    public static File getMusicFile(String name) {
        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), name);
    }

    public static String formatDateTime(long seconds) {
        return formatDateTime(DATE_TIME_FORMAT, seconds);
    }

    public static String formatDateTimeAgo(long seconds) {
        return formatDateTime(DATE_TIME_FORMAT_WEEKDAY, seconds) + "\n" +
                formatDuration(Calendar.currentTime() - seconds, false);
    }

    public static String formatDuration(long millis) {
        return formatDuration(TimeUnit.MILLISECONDS.toSeconds(millis), true);
    }

    public static String formatPercent(double number) {
        return PERCENT_FORMAT.format(number);
    }

    public static void showInfoDialog(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    public static void hideSoftInput(Context context, View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private static String formatDateTime(DateFormat format, long seconds) {
        return format.format(TimeUnit.SECONDS.toMillis(seconds));
    }

    private static String formatDuration(long seconds, boolean showSeconds) {
        String ret = "";

        // Process weeks.
        long weeks = TimeUnit.SECONDS.toDays(seconds) / 7;
        if (weeks > 0) {
            ret += Long.toString(weeks) + "w ";
            seconds -= TimeUnit.DAYS.toSeconds(weeks * 7);
        }

        // Process days.
        long days = TimeUnit.SECONDS.toDays(seconds);
        if (days > 0) {
            ret += Long.toString(days) + "d ";
            seconds -= TimeUnit.DAYS.toSeconds(days);
        }

        // Append HH:mm.
        ret += TIME_NUMBER_FORMAT.format(TimeUnit.SECONDS.toHours(seconds)) + ":" +
                TIME_NUMBER_FORMAT.format(TimeUnit.SECONDS.toMinutes(seconds) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(seconds)));

        // Process seconds.
        if (showSeconds) {
            ret += ":" + TIME_NUMBER_FORMAT.format(seconds -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)));
        }

        return ret;
    }
}