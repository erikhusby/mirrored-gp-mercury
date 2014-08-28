package org.broadinstitute.gpinformatics.athena.entity.common;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;

@Test(groups = TestGroups.DATABASE_FREE)
public class ChangeEventTest {
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testDatePerson() throws Exception {

        ChangeEvent changeEvent = new ChangeEvent(new Date(), "person1");
        ChangeEvent changeEvent2 = new ChangeEvent(changeEvent.getDate(), "person1");

        Assert.assertEquals(changeEvent, changeEvent2);
    }

}
