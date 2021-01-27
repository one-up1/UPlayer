package com.oneup.uplayer.util;

import java.util.concurrent.TimeUnit;

public class Calendar {
    private java.util.Calendar calendar;

    public Calendar() {
        calendar = java.util.Calendar.getInstance();
    }

    public long getTime() {
        return TimeUnit.MILLISECONDS.toSeconds(calendar.getTimeInMillis());
    }

    public void setTime(long seconds) {
        calendar.setTimeInMillis(TimeUnit.SECONDS.toMillis(seconds));
    }

    public void setDate(int year, int month, int dayOfMonth) {
        calendar.set(java.util.Calendar.YEAR, year);
        calendar.set(java.util.Calendar.MONTH, month);
        calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
    }

    public void addDay() {
        calendar.add(java.util.Calendar.DATE, 1);
    }

    public long getTimeOfDay() {
        return TimeUnit.HOURS.toSeconds(calendar.get(java.util.Calendar.HOUR_OF_DAY)) +
                TimeUnit.MINUTES.toSeconds(calendar.get(java.util.Calendar.MINUTE)) +
                calendar.get(java.util.Calendar.SECOND);
    }

    public void setTimeOfDay(long seconds) {
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, (int) seconds);
    }

    public static long currentTime() {
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    }
}
