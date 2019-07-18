package com.oneup.uplayer.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.BulletSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.oneup.uplayer.R;
import com.oneup.uplayer.widget.EditText;

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

    static {
        TIME_NUMBER_FORMAT.setMinimumIntegerDigits(2);
        FRACTION_FORMAT.setMaximumFractionDigits(1);
        PERCENT_FORMAT.setMaximumFractionDigits(1);
    }

    private Util() {
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

    public static String getCountString(Context context, int id, int count) {
        return context.getResources().getQuantityString(id, count, count);
    }

    public static String getCountString(Context context, ArrayList<?> list,
                                        boolean quotes, int zeroId, int otherId) {
        switch (list.size()) {
            case 0:
                return zeroId == 0 ? null : context.getString(zeroId);
            case 1:
                String s = list.get(0).toString();
                if (quotes) {
                    s = "'" + s + "'";
                }
                return s;
            default:
                return context.getString(otherId, list.size());
        }
    }

    public static SpannableString getStyledText(CharSequence text,
                                                long bookmarked, long archived, long timesPlayed) {
        SpannableString ss = new SpannableString(text);
        if (bookmarked != 0) {
            ss.setSpan(new BulletSpan(10), 0, 0, 0);
        }
        if (archived != 0) {
            ss.setSpan(new StrikethroughSpan(), 0, text.length(), 0);
        }
        if (timesPlayed == 0) {
            ss.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), 0);
        }
        return ss;
    }

    public static SpannableString underline(CharSequence text) {
        SpannableString ss = new SpannableString(text);
        ss.setSpan(new UnderlineSpan(), 0, text.length(), 0);
        return ss;
    }

    public static void hideSoftInput(Context context, View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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