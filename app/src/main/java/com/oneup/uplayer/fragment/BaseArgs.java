package com.oneup.uplayer.fragment;

import android.os.Bundle;

//TODO: Extras and pref keys naming.

class BaseArgs {
    static final String SELECTION = "com.oneup.uplayer.extra.SELECTION";
    static final String SELECTION_ARGS = "com.oneup.uplayer.extra.SELECTION_ARGS";
    static final String ORDER_BY = "com.oneup.uplayer.extra.ORDER_BY";

    static Bundle get(String selection, String[] selectionArgs, String orderBy) {
        Bundle args = new Bundle();
        args.putString(SELECTION, selection);
        args.putStringArray(SELECTION_ARGS, selectionArgs);
        args.putString(ORDER_BY, orderBy);
        return args;
    }
}
