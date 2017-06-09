package com.oneup.uplayer.fragment;

public interface BaseArgs {
    String ARG_ARTISTS = "artists";
    String ARG_JOINED_SORT_BY = "sort_by";
    String ARG_SELECTION = "selection";
    String ARG_DB_ORDER_BY = "order_by";

    int SORT_BY_NAME = 1;
    int SORT_BY_LAST_PLAYED = 2;
    int SORT_BY_TIMES_PLAYED = 3;
}
