package org.broadinstitute.gpinformatics.infrastructure.common;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.Arrays;

@Test(groups = TestGroups.DATABASE_FREE)
public class BusinessKeyComparatorTest {

    public void testSimple() {

        String [] expected = new String[] {"AAA-2", "AAA-100", "BBB-1"};
        String [] actual = new String[] {"BBB-1", "AAA-100", "AAA-2", };

        Arrays.sort(actual, new BusinessKeyComparator());

        Assert.assertTrue(Arrays.equals(actual, expected));
    }


    public void testJunk() {

        String [] actual = new String[] {"TEST-1", null, "JUNK", "?"};

        Arrays.sort(actual, new BusinessKeyComparator());

        // I actually don't care which end up first, just that the one
        // that looks like a business key is at the end
        Assert.assertEquals("TEST-1", actual[actual.length - 1]);
    }


    public void testSame() {
        // Those AAA-100 literals should be interned to the same objects
        String [] expected = new String[] {"AAA-100", "AAA-100", "BBB-1"};
        String [] actual = new String[] {"BBB-1", "AAA-100", "AAA-100", };

        Arrays.sort(actual, new BusinessKeyComparator());

        Assert.assertTrue(Arrays.equals(actual, expected));
    }

}
