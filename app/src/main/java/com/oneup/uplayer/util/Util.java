package com.oneup.uplayer.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.oneup.uplayer.R;
import com.oneup.uplayer.widget.EditText;

import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@SuppressLint("SimpleDateFormat")
public class Util {
    private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private static final DateFormat DATE_TIME_FORMAT_WEEKDAY =
            new SimpleDateFormat("E dd-MM-yyyy HH:mm");
    private static final NumberFormat TIME_NUMBER_FORMAT = NumberFormat.getInstance();
    private static final NumberFormat FRACTION_FORMAT = NumberFormat.getInstance();
    private static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance();

    private static Toast toast;

    static {
        TIME_NUMBER_FORMAT.setMinimumIntegerDigits(2);
        FRACTION_FORMAT.setMaximumFractionDigits(1);
        PERCENT_FORMAT.setMaximumFractionDigits(1);
    }

    private Util() {
    }

    public static File getMusicFile(String name) {
        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), name);
    }

    public static void showToast(final Activity context, final int resId,
                                 final Object... formatArgs) {
        context.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // Cancel any previous toast. This will prevent many toasts from displaying for a
                // long time when repeatingly pressing buttons that show toasts like play next/last.
                if (toast != null) {
                    toast.cancel();
                }

                toast = Toast.makeText(context,
                        context.getString(resId, formatArgs),
                        Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    public static void showSnackbar(Activity context, int resId, Object... formatArgs) {
        Snackbar.make(context.findViewById(R.id.view),
                context.getString(resId, formatArgs),
                Snackbar.LENGTH_INDEFINITE).show();
    }

    public static void showConfirmDialog(Context context, int messageId,
                                         DialogInterface.OnClickListener listener) {
        showConfirmDialog(context, context.getString(messageId), listener);
    }

    public static void showConfirmDialog(Context context, String message,
                                         DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_dialog_warning)
                .setTitle(R.string.app_name)
                .setMessage(message)
                .setPositiveButton(R.string.yes, listener)
                .setNegativeButton(R.string.no, null)
                .show();
    }

    public static void showInfoDialog(Activity context, int titleId,
                                  int messageId, Object... formatArgs) {
        showDialog(context, R.drawable.ic_dialog_info,
                context.getString(titleId),
                context.getString(messageId, formatArgs));
    }

    public static void showErrorDialog(Activity context, Exception ex) {
        showDialog(context, R.drawable.ic_dialog_error,
                ex.getClass().getSimpleName(),
                ex.getMessage());
    }

    public static void showInputDialog(Context context, int titleId, int inputType, int hintId,
                                       Object value, final InputDialogListener listener) {
        final EditText view = new EditText(context);
        view.setInputType(inputType);
        view.setHint(hintId);
        if (value != null) {
            view.setText(value.toString());
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onOk(view);
                    }
                })
                .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    public static String formatDateTime(long seconds) {
        return formatDateTime(seconds, DATE_TIME_FORMAT);
    }

    public static String formatDateTimeAgo(long seconds) {
        return formatDateTime(seconds, DATE_TIME_FORMAT_WEEKDAY) +
                "\n" + formatTimeAgo(seconds);
    }

    public static String formatTimeAgo(long seconds) {
        return formatDuration(Calendar.currentTime() - seconds, false);
    }

    public static String formatDuration(long millis) {
        return formatDuration(TimeUnit.MILLISECONDS.toSeconds(millis), true);
    }

    public static String formatFraction(long value, long total) {
        return formatFraction(value, total, FRACTION_FORMAT);
    }

    public static String formatPercent(long value, long total) {
        return formatFraction(value, total, PERCENT_FORMAT);
    }

    public static String getCountString(Context context, int id, int quantity) {
        return context.getResources().getQuantityString(id, quantity, quantity);
    }

    public static String getCountString(Context context, ArrayList<?> list,
                                        boolean quotes, int resId) {
        String s;
        if (list.size() == 1) {
            s = list.get(0).toString();
            if (quotes) {
                s = "'" + s + "'";
            }
        } else {
            s = context.getString(resId, list.size());
        }
        return s;
    }

    public static void hideSoftInput(Context context, View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private static void showDialog(final Activity context, final int iconId,
                                   final String title, final String message) {
        context.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setIcon(iconId)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(R.string.ok, null)
                        .show();
            }
        });
    }

    private static String formatDateTime(long seconds, DateFormat format) {
        return format.format(TimeUnit.SECONDS.toMillis(seconds));
    }

    private static String formatDuration(long seconds, boolean showSeconds) {
        String s = "";

        // Process years.
        long years = TimeUnit.SECONDS.toDays(seconds) / 365;
        if (years > 0) {
            s += years + "y ";
            seconds -= TimeUnit.DAYS.toSeconds(years * 365);
        }

        // Process weeks.
        long weeks = TimeUnit.SECONDS.toDays(seconds) / 7;
        if (weeks > 0) {
            s += weeks + "w ";
            seconds -= TimeUnit.DAYS.toSeconds(weeks * 7);
        }

        // Process days.
        long days = TimeUnit.SECONDS.toDays(seconds);
        if (days > 0) {
            s += days + "d ";
            seconds -= TimeUnit.DAYS.toSeconds(days);
        }

        // Append HH:mm.
        s += TIME_NUMBER_FORMAT.format(TimeUnit.SECONDS.toHours(seconds)) + ":" +
                TIME_NUMBER_FORMAT.format(TimeUnit.SECONDS.toMinutes(seconds) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(seconds)));

        // Process seconds.
        if (showSeconds) {
            s += ":" + TIME_NUMBER_FORMAT.format(seconds -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(seconds)));
        }

        return s;
    }

    private static String formatFraction(long value, long total, NumberFormat format) {
        return format.format((double) value / total);
    }

    public interface InputDialogListener {
        void onOk(EditText view);
    }
}