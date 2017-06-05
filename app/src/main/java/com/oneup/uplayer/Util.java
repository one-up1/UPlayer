package com.oneup.uplayer;

import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

public class Util {
    private static final NumberFormat TIME_NUMBER_FORMAT = NumberFormat.getInstance();

    static {
        TIME_NUMBER_FORMAT.setMinimumIntegerDigits(2);
    }

    private Util() {
    }

    public static String formatDuration(long millis) {
        return TIME_NUMBER_FORMAT.format(TimeUnit.MILLISECONDS.toHours(millis)) + ":" +
                TIME_NUMBER_FORMAT.format(TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis))) + ":" +
                TIME_NUMBER_FORMAT.format(TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }
}
