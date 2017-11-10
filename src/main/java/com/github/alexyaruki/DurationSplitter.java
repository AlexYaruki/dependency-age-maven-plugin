package com.github.alexyaruki;

class DurationSplitter {
    private static final long MILLIS_IN_SECOND = 1000L;
    private static final long MILLIS_IN_MINUTE = MILLIS_IN_SECOND * 60;
    private static final long MILLIS_IN_HOUR = MILLIS_IN_MINUTE * 60;
    private static final long MILLIS_IN_DAY = MILLIS_IN_HOUR * 24;
    private static final long MILLIS_IN_YEAR = MILLIS_IN_DAY * 365;
    private transient long current;

    DurationSplitter(final long millis) {
        current = millis;

    }

    long getYears() {
        final long result = current / MILLIS_IN_YEAR;
        current = current % MILLIS_IN_YEAR;
        return result;
    }

    long getDays() {
        final long result = current / MILLIS_IN_DAY;
        current = current % MILLIS_IN_DAY;
        return result;
    }

    long getHours() {
        final long result = current / MILLIS_IN_HOUR;
        current = current % MILLIS_IN_HOUR;
        return result;
    }

    long getMinutes() {
        final long result = current / MILLIS_IN_MINUTE;
        current = current % MILLIS_IN_MINUTE;
        return result;
    }

    long getSeconds() {
        final long result = current / MILLIS_IN_SECOND;
        current = current % MILLIS_IN_SECOND;
        return result;
    }
}
