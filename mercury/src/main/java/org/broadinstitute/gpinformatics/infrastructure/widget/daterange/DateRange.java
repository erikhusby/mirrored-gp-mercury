/**
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2007 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.infrastructure.widget.daterange;

import java.util.Calendar;
import java.util.Date;

/**
 * An enumeration of the possible date ranges for user interface input. This is modelled similar to
 * how Quicken shows things.
 *
 * @author George Grant
 */
public enum DateRange {

    Today("Today") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            Date now = new Date();
            startCalendar.setTime(now);
            stopCalendar.setTime(now);
        }
    },
    Last24Hours("Last 24 Hours") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.HOUR, -24);

            // This is one day back from today, so can rely on getInstance using today for stop date.
        }},
    Yesterday("Yesterday") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.DATE, -1);
            stopCalendar.add(Calendar.DATE, -1);
        }},
    ThisWeek("This Week") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            int currentDayOfWeek = startCalendar.get(Calendar.DAY_OF_WEEK);
            startCalendar.add(Calendar.DAY_OF_WEEK, Calendar.SUNDAY - currentDayOfWeek);

            // This is one week back from today, so can rely on getInstance using today for stop date.
        }
    },
    Last7Days("Last 7 Days") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.DATE, -7);

            // This is seven days back from today, so can rely on getInstance using today for stop date.
        }
    },
    LastWeek("Last Week") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            int currentDayOfWeek = startCalendar.get(Calendar.DAY_OF_WEEK);
            startCalendar.add(Calendar.DAY_OF_WEEK, Calendar.SUNDAY - currentDayOfWeek - 7);
            stopCalendar.add(Calendar.DAY_OF_WEEK, -currentDayOfWeek);
        }
    },
    ThisMonth("This Month") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.set(Calendar.DAY_OF_MONTH, startCalendar.getActualMinimum(Calendar.DAY_OF_MONTH));
            stopCalendar.set(Calendar.DAY_OF_MONTH, stopCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
    },
    Last30Days("Last 30 Days") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.DATE, -30);

            // This is one year back from today, so can rely on getInstance using today.
        }
    },
    LastMonth("Last Month") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.MONTH, -1);
            startCalendar.set(Calendar.DAY_OF_MONTH, 1);

            stopCalendar.add(Calendar.MONTH, -1);
            stopCalendar.set(Calendar.DAY_OF_MONTH, stopCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
    },
    ThisQuarter("This Quarter") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            int month = startCalendar.get(Calendar.MONTH);
            if ((month >= Calendar.JANUARY) && (month <= Calendar.MARCH)) {
                startCalendar.set(Calendar.MONTH, Calendar.JANUARY);
            } else if ((month >= Calendar.APRIL) && (month <= Calendar.JUNE)) {
                startCalendar.set(Calendar.MONTH, Calendar.APRIL);
            } else if ((month >= Calendar.JULY) && (month <= Calendar.SEPTEMBER)) {
                startCalendar.set(Calendar.MONTH, Calendar.JULY);
            } else {
                startCalendar.set(Calendar.MONTH, Calendar.OCTOBER);
            }
            startCalendar.set(Calendar.DAY_OF_MONTH, 1);
            startCalendar.setTime(DateUtils.getStartOfDay(startCalendar.getTime()));

            /*
             * Set the day to minimum (effectively 1) for its current month so that startCalendar.get(Calendar.MONTH)
             * doesn't accidentally lead to spill-over into the next month when stop.getActualMaximum() is called.
             */
            stopCalendar.set(Calendar.DAY_OF_MONTH, stopCalendar.getActualMinimum(Calendar.DAY_OF_MONTH));
            stopCalendar.set(Calendar.MONTH, startCalendar.get(Calendar.MONTH) + 2);
            stopCalendar.set(Calendar.DAY_OF_MONTH, stopCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            stopCalendar.setTime(DateUtils.getEndOfDay(stopCalendar.getTime()));
        }
    },
    Last90Days("Last 90 Days") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.DATE, -90);

            // This is 90 days back from today, so can rely on getInstance using today.
        }
    },
    LastQuarter("Last Quarter") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {

            int month = startCalendar.get(Calendar.MONTH);
            if ((month >= Calendar.JANUARY) && (month <= Calendar.MARCH)) {
                startCalendar.add(Calendar.YEAR, -1);
                startCalendar.set(Calendar.MONTH, Calendar.OCTOBER);
            } else if ((month >= Calendar.APRIL) && (month <= Calendar.JUNE)) {
                startCalendar.set(Calendar.MONTH, Calendar.JANUARY);
            } else if ((month >= Calendar.JULY) && (month <= Calendar.SEPTEMBER)) {
                startCalendar.set(Calendar.MONTH, Calendar.APRIL);
            } else {
                startCalendar.set(Calendar.MONTH, Calendar.JULY);
            }
            startCalendar.set(Calendar.DAY_OF_MONTH, 1);

            stopCalendar.setTime(startCalendar.getTime());
            stopCalendar.add(Calendar.MONTH, 2);
            stopCalendar.set(Calendar.DAY_OF_MONTH, stopCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        }
    },
    ThisYear("This Year") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.set(Calendar.DAY_OF_YEAR, 1);
            stopCalendar.set(Calendar.DAY_OF_YEAR, stopCalendar.getActualMaximum(Calendar.DAY_OF_YEAR));
        }
    },
    OneYear("One Year") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.set(Calendar.YEAR, -1);

            // This is one year back from today, so can rely on getInstance using today.
        }
    },
    LastYear("Last Year") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.YEAR, -1);
            stopCalendar.add(Calendar.YEAR, -1);

            // Now that the years are back one, just use 'This Year' logic.
            ThisYear.calcDate(startCalendar, stopCalendar);
        }
    },
    All("All Time") {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.setTimeInMillis(0);
            stopCalendar.setTimeInMillis(Long.MAX_VALUE);
        }
    };

    abstract protected void calcDate(Calendar start, Calendar stop);

    /**
     * This method returns a two element array with the 1st
     * element set to the appropriate start date, and the second to the appropriate end
     * date
     *
     * @return Two element array with start and end dates populated.
     */
    public Date[] startAndStopDate() {
        Calendar startCalendar = Calendar.getInstance();
        Calendar stopCalendar = Calendar.getInstance();
        startCalendar.setTime(DateUtils.getStartOfDay(startCalendar.getTime()));
        stopCalendar.setTime(DateUtils.getEndOfDay(stopCalendar.getTime()));
        calcDate(startCalendar, stopCalendar);
        return new Date[] { startCalendar.getTime(), stopCalendar.getTime() };
    }

    /** Stores a user-friendly version of the enum name. */
    private final String description;


    /** Constructor which accepts the description. */
    private DateRange(String description) {
        this.description = description;
    }

    /** Gets the user friendly description of the category. */
    public String getDescription() {
        return description;
    }

    /** Delegates to name() to allow javabean compliant access. */
    public String getName() {
        return name();
    }
}
