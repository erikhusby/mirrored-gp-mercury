/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.jira;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

@Test(groups = TestGroups.DATABASE_FREE)
public class JiraServiceDateParsingTest {

    @DataProvider(name = "dateProvider")
    public Object[][] dateProvider() {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.set(2016, Calendar.JANUARY, 11, 15, 28, 11);

        final Date testDate = calendar.getTime();

        return new Object[][]{
                new Object[]{"2016-01-11", testDate, Calendar.DAY_OF_MONTH},
                new Object[]{"11/Jan/16", testDate, Calendar.DAY_OF_MONTH},
                new Object[]{"2016-01-11T15:28:11.445-0500", testDate, Calendar.SECOND}
        };
    }

    @Test(dataProvider = "dateProvider")
    public void testParseDates(String dateString, Date testDate, int minimumFieldPrecision) {
        Date resultDate = null;
        try {
            resultDate = JiraServiceImpl.parseJiraDate(dateString);
        } catch (ParseException e) {
            Assert.fail("Could not parse date", e);
        }
        Assert.assertEquals(org.apache.commons.lang3.time.DateUtils.truncate(testDate, minimumFieldPrecision),
                org.apache.commons.lang3.time.DateUtils.truncate(resultDate, minimumFieldPrecision));
    }
}
