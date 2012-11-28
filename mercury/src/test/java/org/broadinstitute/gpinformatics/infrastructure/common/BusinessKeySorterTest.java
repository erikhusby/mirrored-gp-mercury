package org.broadinstitute.gpinformatics.infrastructure.common;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class BusinessKeySorterTest {

    public void testSimple() {

        Assert.assertTrue(BusinessKeySorter.sort("AAA-2", "AAA-100") < 0);
        Assert.assertTrue(BusinessKeySorter.sort("AAA-100", "AAA-2") > 0);
        Assert.assertTrue(BusinessKeySorter.sort("AAA-2", "AAA-2") == 0);

    }
}
