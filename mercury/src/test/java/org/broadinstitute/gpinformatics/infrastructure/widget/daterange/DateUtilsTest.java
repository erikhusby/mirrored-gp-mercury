/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.widget.daterange;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class DateUtilsTest {
    @DataProvider(name = "daysBetweenTestProvider")
    public Iterator<Object[]> daysBetweenTestProvider() throws ParseException {
        DateFormat dateInstance = SimpleDateFormat.getDateInstance(DateFormat.SHORT);
        Date d0 = org.apache.commons.lang.time.DateUtils.truncate(dateInstance.parse("3/20/2019"), Calendar.DATE);
        Date d1 = org.apache.commons.lang.time.DateUtils.truncate(dateInstance.parse("3/21/2019"), Calendar.DATE);
        Date d31 = org.apache.commons.lang.time.DateUtils.truncate(dateInstance.parse("4/20/2019"), Calendar.DATE);

        // 2020 is a leap year so there will be an extra day
        Date d367 = org.apache.commons.lang.time.DateUtils.truncate(dateInstance.parse("3/21/2020"), Calendar.DATE);
        Date d358224 = org.apache.commons.lang.time.DateUtils.truncate(dateInstance.parse("12/31/2999"), Calendar.DATE);

        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{d0, d0, 0});
        testCases.add(new Object[]{d0, d1, 1});
        testCases.add(new Object[]{d0, d31, 31});
        testCases.add(new Object[]{d0, d367, 367});
        testCases.add(new Object[]{d0, d358224, 358224});

        return testCases.iterator();
    }

    @Test(dataProvider = "daysBetweenTestProvider")
    public void testGetNumDaysBetween(Date d1, Date d2, long expected) throws Exception {
        long numDaysBetween =
            org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils.getNumDaysBetween(d1, d2);

        assertThat(numDaysBetween, equalTo(expected));
    }
}
