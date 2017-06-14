package com.oneup.uplayer.db;

public class DbComparator {
    private DbComparator() {
    }

    public static int sortByName(String name1, String name2) {
        return name1.compareTo(name2);
    }

    public static int sortByLastPlayed(long lastPlayed1, long lastPlayed2,
                                       String name1, String name2) {
        return lastPlayed1 == lastPlayed2 ?
                sortByName(name1, name2) :
                Long.compare(lastPlayed2, lastPlayed1);
    }

    public static int sortByTimesPlayed(int timesPlayed1, int timesPlayed2,
                                        long lastPlayed1, long lastPlayed2,
                                        String name1, String name2) {
        return timesPlayed1 == timesPlayed2 ?
                sortByLastPlayed(lastPlayed1, lastPlayed2, name1, name2) :
                Integer.compare(timesPlayed2, timesPlayed1);
    }
}
