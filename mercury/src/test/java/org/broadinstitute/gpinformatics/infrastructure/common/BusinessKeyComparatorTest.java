package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.apache.commons.lang.ArrayUtils.indexOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class BusinessKeyComparatorTest {

    public void testSimple() {

        String[] expected = new String[]{"AAA-2", "AAA-100", "BBB-1"};
        String[] actual = new String[]{"BBB-1", "AAA-100", "AAA-2",};

        Arrays.sort(actual, new BusinessKeyComparator());

        assertThat(actual, is(equalTo(expected)));
    }


    public void testJunk() {

        String[] actual = new String[]{"TEST-1", null, "JUNK", "?"};

        Arrays.sort(actual, new BusinessKeyComparator());

        // It doesn't matter which end up first, just that the one
        // that looks like a business key is at the end.
        assertThat(indexOf(actual, "TEST-1"), is(equalTo(actual.length - 1)));
    }


    public void testSame() {
        // The literals should be interned to the same objects.
        String[] expected = new String[]{"AAA-100", "AAA-100", "BBB-1"};
        String[] actual = new String[]{"BBB-1", "AAA-100", "AAA-100",};

        Arrays.sort(actual, new BusinessKeyComparator());

        assertThat(actual, is(equalTo(expected)));
    }

}
