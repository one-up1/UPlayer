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

    public static final String formatDate(long millis) {
        return DATE_FORMAT.format(millis);
    }

    public static String formatDuration(long millis) {
        return TIME_NUMBER_FORMAT.format(TimeUnit.MILLISECONDS.toHours(millis)) + ":" +
                TIME_NUMBER_FORMAT.format(TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))) + ":" +
                TIME_NUMBER_FORMAT.format(TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    public static void showInfoDialog(Context context, String title,
                                      int duration, long lastPlayed, int timesPlayed) {
        //TODO: Improve info dialog, show year, remove year from list_item?
        String message = "";
        if (duration > 0) {
            message += formatDuration(duration) + "\n";
        }
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message + context.getString(R.string.info_message,
                        lastPlayed == 0 ?
                                context.getString(R.string.never) :
                                DATE_TIME_FORMAT.format(lastPlayed),
                        timesPlayed))
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
