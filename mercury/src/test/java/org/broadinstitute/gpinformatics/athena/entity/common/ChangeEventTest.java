package org.broadinstitute.gpinformatics.athena.entity.common;

import org.testng.Assert;
import junit.framework.TestCase;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 8/15/12
 * Time: 11:04 AM
 */
public class ChangeEventTest extends TestCase {

    @Override
    protected void setUp() throws java.lang.Exception {

    }

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testDatePerson() throws Exception {

        ChangeEvent changeEvent = new ChangeEvent(new Date(), "person1");
        ChangeEvent changeEvent2 = new ChangeEvent(changeEvent.getDate(), "person1");

        Assert.assertEquals( changeEvent, changeEvent2);
    }

}
