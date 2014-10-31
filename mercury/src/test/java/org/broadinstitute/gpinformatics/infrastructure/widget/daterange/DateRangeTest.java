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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.DATABASE_FREE)
public class DateRangeTest {

    private static final Object[][] QUARTER_DATE_RANGES = new Object[][]{
            {
                    new GregorianCalendar(2014, Calendar.JANUARY, 1, 0, 0, 0),
                    new GregorianCalendar(2014, Calendar.MARCH, 31, 23, 59, 59)
            },
            {
                    new GregorianCalendar(2014, Calendar.APRIL, 1, 0, 0, 0),
                    new GregorianCalendar(2014, Calendar.JUNE, 30, 23, 59, 59)
            },
            {
                    new GregorianCalendar(2014, Calendar.JULY, 1, 0, 0, 0),
                    new GregorianCalendar(2014, Calendar.SEPTEMBER, 30, 23, 59, 59)
            },
            {
                    new GregorianCalendar(2014, Calendar.OCTOBER, 1, 0, 0, 0),
                    new GregorianCalendar(2014, Calendar.DECEMBER, 31, 23, 59, 59)
            }
    };

    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    public void beforeClass() {
        for (Object[] dates : QUARTER_DATE_RANGES) {
            GregorianCalendar beginningOfQuarter = (GregorianCalendar) dates[0];
            GregorianCalendar endOfQuarter = (GregorianCalendar) dates[1];

            beginningOfQuarter.set(Calendar.MILLISECOND, 0);
            endOfQuarter.set(Calendar.MILLISECOND, 999);
        }
    }

    public void testFirstDayOfQuarter() throws Exception {
        for (Object[] dates : QUARTER_DATE_RANGES) {
            GregorianCalendar beginningOfQuarter = (GregorianCalendar) dates[0];
            GregorianCalendar endOfQuarter = (GregorianCalendar) dates[1];

            validateDateRange(DateRange.ThisQuarter, beginningOfQuarter, beginningOfQuarter, endOfQuarter);
        }
    }

    public void testLastDayOfQuarter() throws Exception {
        for (Object[] dates : QUARTER_DATE_RANGES) {
            GregorianCalendar beginningOfQuarter = (GregorianCalendar) dates[0];
            GregorianCalendar endOfQuarter = (GregorianCalendar) dates[1];

            validateDateRange(DateRange.ThisQuarter, endOfQuarter, beginningOfQuarter, endOfQuarter);
        }
    }

    public void testEqualDatesRanges() {
        Calendar now = Calendar.getInstance();
        Calendar start = (Calendar) now.clone();
        Calendar end = (Calendar) now.clone();
        DateRange.ThisQuarter.calcDate(start, end);

        // calcDate above side effects the start date. We are using that for our new start date
        Calendar start2 = (Calendar) start.clone();
        Calendar end2 = (Calendar) end.clone();
        DateRange.ThisQuarter.calcDate(start2, end2);

        assertThat(start, equalTo(start2));
        assertThat(end, equalTo(end2));
    }

    private void validateDateRange(DateRange range, Calendar startAt, Calendar expectedStartDate, Calendar expectedEndDate) {
        Calendar start = startAt != null ? (Calendar) startAt.clone() : Calendar.getInstance();
        Calendar end = (Calendar) start.clone();
        range.calcDate(start, end);

        assertThat(start, equalTo(expectedStartDate));
        assertThat(end, equalTo(expectedEndDate));
    }
}
