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

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.DATABASE_FREE)
public class DateRangeTest {
    Calendar beginningOfQuarter;
    Calendar endOfQuarter;

    public void testNewVersionSameResultAsOld() {
        Date[] newDates = DateRange.ThisQuarter.startAndStopDate();

        // This Code is a copy of the original code prior to refactoring, here only to verify new code
        DateRange.ComputeStartAndStopDate computeStartAndStopDate = new DateRange.ComputeStartAndStopDate() {
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
            }
        };

        Date[] oldStyleStartAndStopDate = computeStartAndStopDate.startAndStopDate();

        assertThat(newDates[0], equalTo(oldStyleStartAndStopDate[0]));
        // old style didn't calculate end date!
        // assertThat(newDates[1], equalTo(oldStyleStartAndStopDate[1]));
    }

    private void setupCalendar(int startMonth, int endMonth) {
        beginningOfQuarter = new GregorianCalendar(2014, startMonth, 1, 0, 0, 0);
        beginningOfQuarter.set(Calendar.MILLISECOND, 0);

        endOfQuarter =
                new GregorianCalendar(2014, endMonth, beginningOfQuarter.getActualMaximum(Calendar.DAY_OF_MONTH), 23,
                        59, 59);
        endOfQuarter.set(Calendar.MILLISECOND, 999);
    }

    public void testFirstDayOfQuarter1() throws Exception {
        setupCalendar(Calendar.JANUARY, Calendar.MARCH);

        OneQuarterDateRange dateRange = new OneQuarterDateRange(beginningOfQuarter.getTime());
        validateDateRange(dateRange, beginningOfQuarter.getTime(), endOfQuarter.getTime());
    }


    public void testLastDayOfQuarter1() throws Exception {
        setupCalendar(Calendar.JANUARY, Calendar.MARCH);

        OneQuarterDateRange dateRange = new OneQuarterDateRange(beginningOfQuarter.getTime());
        validateDateRange(dateRange, beginningOfQuarter.getTime(), endOfQuarter.getTime());
    }

    public void testFirstDayOfQuarter2() throws Exception {
        setupCalendar(Calendar.APRIL, Calendar.JUNE);
        OneQuarterDateRange dateRange = new OneQuarterDateRange(beginningOfQuarter.getTime());
        validateDateRange(dateRange, beginningOfQuarter.getTime(), endOfQuarter.getTime());
    }

    public void testLastDayOfQuarter2() throws Exception {
        setupCalendar(Calendar.APRIL, Calendar.JUNE);
        OneQuarterDateRange dateRange = new OneQuarterDateRange(beginningOfQuarter.getTime());
        validateDateRange(dateRange, beginningOfQuarter.getTime(), endOfQuarter.getTime());
    }

    public void testFirstDayOfQuarter3() throws Exception {
        Calendar beginningOfQuarter = new GregorianCalendar(2014, Calendar.JULY, 1, 0, 0, 0);
        Calendar endOfQuarter = new GregorianCalendar(2014, Calendar.SEPTEMBER, 30, 23, 59, 59);

        beginningOfQuarter.set(Calendar.MILLISECOND, 0);
        endOfQuarter.set(Calendar.MILLISECOND, 999);

        OneQuarterDateRange dateRange = new OneQuarterDateRange(beginningOfQuarter.getTime());
        validateDateRange(dateRange, beginningOfQuarter.getTime(), endOfQuarter.getTime());
    }

    public void testLastDayOfQuarter3() throws Exception {
        Calendar beginningOfQuarter = new GregorianCalendar(2014, Calendar.JULY, 1, 0, 0, 0);
        Calendar endOfQuarter = new GregorianCalendar(2014, Calendar.SEPTEMBER, 30, 23, 59, 59);

        beginningOfQuarter.set(Calendar.MILLISECOND, 0);
        endOfQuarter.set(Calendar.MILLISECOND, 999);

        OneQuarterDateRange dateRange = new OneQuarterDateRange(beginningOfQuarter.getTime());
        validateDateRange(dateRange, beginningOfQuarter.getTime(), endOfQuarter.getTime());
    }

    public void testFirstDayOfQuarter4() throws Exception {
        Calendar beginningOfQuarter = new GregorianCalendar(2014, Calendar.OCTOBER, 1, 0, 0, 0);
        Calendar endOfQuarter = new GregorianCalendar(2014, Calendar.DECEMBER, 31, 23, 59, 59);

        beginningOfQuarter.set(Calendar.MILLISECOND, 0);
        endOfQuarter.set(Calendar.MILLISECOND, 999);

        OneQuarterDateRange dateRange = new OneQuarterDateRange(beginningOfQuarter.getTime());
        validateDateRange(dateRange, beginningOfQuarter.getTime(), endOfQuarter.getTime());
    }

    public void testLastDayOfQuarter4() throws Exception {
        Calendar beginningOfQuarter = new GregorianCalendar(2014, Calendar.OCTOBER, 1, 0, 0, 0);
        Calendar endOfQuarter = new GregorianCalendar(2014, Calendar.DECEMBER, 31, 23, 59, 59);

        beginningOfQuarter.set(Calendar.MILLISECOND, 0);
        endOfQuarter.set(Calendar.MILLISECOND, 999);

        OneQuarterDateRange dateRange = new OneQuarterDateRange(beginningOfQuarter.getTime());
        validateDateRange(dateRange, beginningOfQuarter.getTime(), endOfQuarter.getTime());
    }

    public void testEqualDatesRanges() {
        OneQuarterDateRange dateRangeNoArg = new OneQuarterDateRange();
        Date[] datesNoArg = dateRangeNoArg.startAndStopDate();
        OneQuarterDateRange dateRangeWithArg = new OneQuarterDateRange(datesNoArg[0]);
        Date[] datesFromArg = dateRangeWithArg.startAndStopDate();
        assertThat(datesNoArg[0], equalTo(datesFromArg[0]));
        assertThat(datesNoArg[1], equalTo(datesFromArg[1]));
    }

    private void validateDateRange(OneQuarterDateRange calculatedDateRange, Date expectedStartDate,
                                   Date expectedEndDate) {
        Date[] dates = calculatedDateRange.startAndStopDate();

        assertThat(dates[0], equalTo(expectedStartDate));
        assertThat(dates[1], equalTo(expectedEndDate));
    }
}
