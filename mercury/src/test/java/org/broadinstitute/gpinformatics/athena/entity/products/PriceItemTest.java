package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class PriceItemTest {

    @Test
    public void test_beaniness() {
        new BeanTester().testBean(PriceItem.class);
        new EqualsMethodTester().testEqualsMethod(PriceItem.class,"price","units");
    }
}
