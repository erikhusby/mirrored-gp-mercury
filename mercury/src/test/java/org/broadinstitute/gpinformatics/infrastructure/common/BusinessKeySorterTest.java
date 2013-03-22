package org.broadinstitute.gpinformatics.infrastructure.common;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Test(groups = TestGroups.DATABASE_FREE)
public class BusinessKeySorterTest {

    public void testSimple() {

        assertThat(BusinessKeySorter.sort("AAA-2", "AAA-100"), is(lessThan(0)));
        assertThat(BusinessKeySorter.sort("AAA-100", "AAA-2"), is(greaterThan(0)));
        assertThat(BusinessKeySorter.sort("AAA-2", "AAA-2"), is(equalTo(0)));
    }
}
