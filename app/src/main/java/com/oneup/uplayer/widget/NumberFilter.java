package com.oneup.uplayer.widget;

import android.text.InputFilter;
import android.text.Spanned;

class NumberFilter implements InputFilter {
    private final double max;
    private final boolean inclusive;

    private NumberFilter(double max, boolean inclusive) {
        this.max = max;
        this.inclusive = inclusive;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        // Get the new string from source and dest.
        String sDest = dest.toString();
        String s = sDest.substring(0, dstart) + sDest.substring(dend, sDest.length());
        s = s.substring(0, dstart) + source.toString() + s.substring(dstart, s.length());

        // Here the string may contain multiple points,
        // causing Double.parseDouble() to throw an exception.
        try {
            return s.isEmpty() || (inclusive ? Double.parseDouble(s) <= max :
                    Double.parseDouble(s) < max) ? null : "";
        } catch (Exception ex) {
            return "";
        }
    }

    static NumberFilter[] getFilters(double max) {
        return new NumberFilter[]{new NumberFilter(max, false)};
    }

    static NumberFilter[] getInclusiveFilters(double max) {
        return new NumberFilter[]{new NumberFilter(max, true)};
    }
}
