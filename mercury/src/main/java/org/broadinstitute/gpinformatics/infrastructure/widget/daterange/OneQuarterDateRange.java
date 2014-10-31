/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.widget.daterange;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This class is used to calculate the beginning and ending date and time for the fiscal quarter based on either
 * current date, or a specified date. Quarters are:
 * <ul>
 * <li>January-March</li>
 * <li>April-June</li>
 * <li>July-September</li>
 * <li>October-December</li>
 * </ul>
 */
public class OneQuarterDateRange extends DateRange.ComputeStartAndStopDate {
    public OneQuarterDateRange() {
        this(new Date());
    }

    public OneQuarterDateRange(Date currentDate) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(DateUtils.getStartOfDay(currentDate));
        setStartCalendar(calendar);
    }

    @Override
    public void calcDate(Calendar startCalendar, Calendar stop) {
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

        // Set the day to 1 so that the startCalendar.get(Calendar.MONTH) + 2 math works.
        stop.set(Calendar.DAY_OF_MONTH, 1);
        stop.set(Calendar.MONTH, startCalendar.get(Calendar.MONTH) + 2);
        stop.set(Calendar.DAY_OF_MONTH, stop.getActualMaximum(Calendar.DAY_OF_MONTH));
        stop.setTime(DateUtils.getEndOfDay(stop.getTime()));
        setStopCalendar(stop);
    }

}
