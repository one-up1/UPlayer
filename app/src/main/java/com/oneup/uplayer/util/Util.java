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
    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("E dd-MM-yyyy HH:mm");
    private static final NumberFormat TIME_NUMBER_FORMAT = NumberFormat.getInstance();

    static {
        TIME_NUMBER_FORMAT.setMinimumIntegerDigits(2);
    }

    private Util() {
    }

    public static File getMusicFile(String name) {
        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), name);
    }

    public static String formatDateTimeAgo(long seconds) {
        String ret = formatDateTime(seconds) + "\n";
        long timeAgo = Calendar.currentTime() - seconds;

        // Process weeks.
        long weeks = TimeUnit.SECONDS.toDays(timeAgo) / 7;
        if (weeks > 0) {
            ret += Long.toString(weeks) + "w ";
            timeAgo -= TimeUnit.DAYS.toSeconds(weeks * 7);
        }

        // Process days.
        long days = TimeUnit.SECONDS.toDays(timeAgo);
        if (days > 0) {
            ret += Long.toString(days) + "d ";
            timeAgo -= TimeUnit.DAYS.toSeconds(days);
        }

        // Append HH:mm:ss.
        return ret + TIME_NUMBER_FORMAT.format(TimeUnit.SECONDS.toHours(timeAgo)) + ":" +
                TIME_NUMBER_FORMAT.format(TimeUnit.SECONDS.toMinutes(timeAgo) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(timeAgo))) + ":" +
                TIME_NUMBER_FORMAT.format(timeAgo -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(timeAgo)));
    }

    public static String formatDateTime(long seconds) {
        return DATE_TIME_FORMAT.format(TimeUnit.SECONDS.toMillis(seconds));
    }

    public static String formatDuration(long millis) {
        return TIME_NUMBER_FORMAT.format(TimeUnit.MILLISECONDS.toHours(millis)) + ":" +
                TIME_NUMBER_FORMAT.format(TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))) + ":" +
                TIME_NUMBER_FORMAT.format(TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
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
}