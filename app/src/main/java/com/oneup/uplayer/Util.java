package com.oneup.uplayer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

@SuppressLint("SimpleDateFormat")
public class Util {
    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private static final NumberFormat TIME_NUMBER_FORMAT = NumberFormat.getInstance();

    static {
        TIME_NUMBER_FORMAT.setMinimumIntegerDigits(2);
    }

    private Util() {
    }

    public static String formatDateTime(long millis) {
        return DATE_TIME_FORMAT.format(millis);
    }

    public static String formatDate(long millis) {
        return DATE_FORMAT.format(millis);
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
}