package com.github.alexyaruki.pda;

/**
 * Class for splitting milliseconds in desired duration parts.
 */
class DurationSplitter {

    /**
     * Milliseconds in second.
     */
    private static final long MILLIS_IN_SECOND = 1000L;

    /**
     * Milliseconds in minute.
     */
    private static final long MILLIS_IN_MINUTE = MILLIS_IN_SECOND * 60;

    /**
     * Milliseconds in hour.
     */
    private static final long MILLIS_IN_HOUR = MILLIS_IN_MINUTE * 60;

    /**
     * Milliseconds in day.
     */
    private static final long MILLIS_IN_DAY = MILLIS_IN_HOUR * 24;

    /**
     * Milliseconds in year.
     */
    private static final long MILLIS_IN_YEAR = MILLIS_IN_DAY * 365;

    /**
     * Current duration to operate on.
     */
    private transient long current;


    /**
     * Creates splitter with initial duration.
     * @param millis - initial duration
     */
    DurationSplitter(final long millis) {
        current = millis;

    }

    /**
     * Return remaining years in splitter.
     *
     * @return remaining years in splitter
     */
    long getYears() {
        final long result = current / MILLIS_IN_YEAR;
        current = current % MILLIS_IN_YEAR;
        return result;
    }

    /**
     * Return remaining days in splitter.
     *
     * @return remaining days in splitter
     */
    long getDays() {
        final long result = current / MILLIS_IN_DAY;
        current = current % MILLIS_IN_DAY;
        return result;
    }

    /**
     * Return remaining hours in splitter.
     *
     * @return remaining hours in splitter
     */
    long getHours() {
        final long result = current / MILLIS_IN_HOUR;
        current = current % MILLIS_IN_HOUR;
        return result;
    }

    /**
     * Return remaining minutes in splitter.
     *
     * @return remaining minutes in splitter
     */
    long getMinutes() {
        final long result = current / MILLIS_IN_MINUTE;
        current = current % MILLIS_IN_MINUTE;
        return result;
    }

    /**
     * Return remaining seconds in splitter.
     *
     * @return remaining seconds in splitter
     */
    long getSeconds() {
        final long result = current / MILLIS_IN_SECOND;
        current = current % MILLIS_IN_SECOND;
        return result;
    }
}
