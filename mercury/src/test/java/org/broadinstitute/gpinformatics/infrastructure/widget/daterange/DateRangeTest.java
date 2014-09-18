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

    private static final Calendar BEGINNING_OF_QUARTER1 = new GregorianCalendar(2014, Calendar.JANUARY, 1, 0, 0, 0);
    private static final Calendar END_OF_QUARTER_1 = new GregorianCalendar(2014, Calendar.MARCH, 31, 23, 59, 59);

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

    public void testFirstDayOfQuarter1() throws Exception {
        BEGINNING_OF_QUARTER1.set(Calendar.MILLISECOND, 0);
        END_OF_QUARTER_1.set(Calendar.MILLISECOND, 999);

        OneQuarterDateRange dateRange = new OneQuarterDateRange(BEGINNING_OF_QUARTER1.getTime());
        validateDateRange(dateRange, BEGINNING_OF_QUARTER1.getTime(), END_OF_QUARTER_1.getTime());
    }

    public void testLastDayOfQuarter1() throws Exception {
        Calendar beginningOfQuarter = new GregorianCalendar(2014, Calendar.JANUARY, 1, 0, 0, 0);
        Calendar endOfQuarter = new GregorianCalendar(2014, Calendar.MARCH, 31, 23, 59, 59);

        beginningOfQuarter.set(Calendar.MILLISECOND, 0);
        endOfQuarter.set(Calendar.MILLISECOND, 999);

        OneQuarterDateRange dateRange = new OneQuarterDateRange(beginningOfQuarter.getTime());
        validateDateRange(dateRange, beginningOfQuarter.getTime(), endOfQuarter.getTime());
    }

    public void testFirstDayOfQuarter2() throws Exception {
        Calendar beginningOfQuarter = new GregorianCalendar(2014, Calendar.APRIL, 1, 0, 0, 0);
        Calendar endOfQuarter = new GregorianCalendar(2014, Calendar.JUNE, 30, 23, 59, 59);

        beginningOfQuarter.set(Calendar.MILLISECOND, 0);
        endOfQuarter.set(Calendar.MILLISECOND, 999);

        OneQuarterDateRange dateRange = new OneQuarterDateRange(beginningOfQuarter.getTime());
        validateDateRange(dateRange, beginningOfQuarter.getTime(), endOfQuarter.getTime());
    }

    public void testLastDayOfQuarter2() throws Exception {
        Calendar beginningOfQuarter = new GregorianCalendar(2014, Calendar.APRIL, 1, 0, 0, 0);
        Calendar endOfQuarter = new GregorianCalendar(2014, Calendar.JUNE, 30, 23, 59, 59);

        beginningOfQuarter.set(Calendar.MILLISECOND, 0);
        endOfQuarter.set(Calendar.MILLISECOND, 999);

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

    private void validateDateRange(OneQuarterDateRange calculated, Date expectedStart, Date expectedEnd) {
        Date[] dates = calculated.startAndStopDate();

        assertThat(dates[0], equalTo(expectedStart));
        assertThat(dates[1], equalTo(expectedEnd));
    }
}
