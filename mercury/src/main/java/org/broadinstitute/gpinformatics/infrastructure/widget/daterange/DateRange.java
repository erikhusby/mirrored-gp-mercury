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

    Today("Today", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            Date now = new Date();
            startCalendar.setTime(now);
            stopCalendar.setTime(now);
        }
    }),
    Last24Hours("Last 24 Hours", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.HOUR, -24);
        }}),
    Yesterday("Yesterday", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.DATE, -1);
            stopCalendar.add(Calendar.DATE, -1);
        }}),
    ThisWeek("This Week", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            int currentDayOfWeek = startCalendar.get(Calendar.DAY_OF_WEEK);
            startCalendar.add(Calendar.DAY_OF_WEEK, Calendar.SUNDAY - currentDayOfWeek);
        }
    }),
    Last7Days("Last 7 Days", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.DATE, -7);
        }
    }),
    LastWeek("Last Week", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            int currentDayOfWeek = startCalendar.get(Calendar.DAY_OF_WEEK);
            startCalendar.add(Calendar.DAY_OF_WEEK, Calendar.SUNDAY - currentDayOfWeek - 7);
            stopCalendar.add(Calendar.DAY_OF_WEEK, -currentDayOfWeek);
        }
    }),
    ThisMonth("This Month", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.set(Calendar.DAY_OF_MONTH, 1);
        }
    }),
    Last30Days("Last 30 Days", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.DATE, -30);
        }
    }),
    LastMonth("Last Month", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.MONTH, -1);
            startCalendar.set(Calendar.DAY_OF_MONTH, 1);

            stopCalendar.set(Calendar.DAY_OF_MONTH, 1);
            stopCalendar.add(Calendar.DAY_OF_MONTH, -1);
        }
    }),
    ThisQuarter("This Quarter", new OneQuarterDateRange()),
    Last90Days("Last 90 Days", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.DATE, -90);
        }
    }),
    LastQuarter("Last Quarter", new ComputeStartAndStopDate() {
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
            stopCalendar.add(Calendar.MONTH, 3);
            stopCalendar.set(Calendar.DAY_OF_MONTH, 1);
            stopCalendar.add(Calendar.DAY_OF_MONTH, -1);
        }
    }),
    ThisYear("This Year", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.set(Calendar.MONTH, Calendar.JANUARY);
            startCalendar.set(Calendar.DAY_OF_MONTH, 1);
        }
    }),
    OneYear("One Year", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.set(Calendar.YEAR, -1);
        }
    }),
    LastYear("Last Year", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.add(Calendar.YEAR, -1);
            startCalendar.set(Calendar.MONTH, Calendar.JANUARY);
            startCalendar.set(Calendar.DAY_OF_MONTH, 1);

            stopCalendar.add(Calendar.YEAR, -1);
            stopCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
            stopCalendar.set(Calendar.DAY_OF_MONTH, 31);
        }
    }),
    All("All Time", new ComputeStartAndStopDate() {
        @Override
        public void calcDate(Calendar startCalendar, Calendar stopCalendar) {
            startCalendar.set(Calendar.YEAR, 1970);
            startCalendar.set(Calendar.MONTH, Calendar.JANUARY);
            startCalendar.set(Calendar.DAY_OF_MONTH, 1);
        }
    });

    public static abstract class ComputeStartAndStopDate {
        private Calendar startCalendar=Calendar.getInstance();
        private Calendar stopCalendar=Calendar.getInstance();

        abstract public void calcDate(Calendar start, Calendar stop);

        public Date[] startAndStopDate() {
            startCalendar.setTime(DateUtils.getStartOfDay(startCalendar.getTime()));
            stopCalendar.setTime(DateUtils.getEndOfDay(stopCalendar.getTime()));
            calcDate(startCalendar, stopCalendar);
            return new Date[] { startCalendar.getTime(), stopCalendar.getTime() };
        }

        protected void setStartCalendar(Calendar startCalendar) {
            this.startCalendar = startCalendar;
        }

        protected void setStopCalendar(Calendar stopCalendar) {
            this.stopCalendar = stopCalendar;
        }
    }

    /** Stores a user-friendly version of the enum name. */
    private final String description;

    private final ComputeStartAndStopDate computeStartAndStopDate;

    /** Constructor which accepts the description. */
    private DateRange(String description, ComputeStartAndStopDate computeStartAndStopDate) {
        this.description = description;
        this.computeStartAndStopDate = computeStartAndStopDate;
    }

    /** Gets the user friendly description of the category. */
    public String getDescription() {
        return description;
    }

    /** Delegates to name() to allow javabean compliant access. */
    public String getName() {
        return name();
    }

    /**
     * This method returns a two element array with the 1st
     * element set to the appropriate start date, and the second to the appropriate end
     * date
     *
     * @return Two element array with start and end dates populated.
     */
    public Date[] startAndStopDate() {
        return computeStartAndStopDate.startAndStopDate();
    }
}
