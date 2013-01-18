package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.meanbean.test.BeanTester;
import org.meanbean.test.Configuration;
import org.meanbean.test.ConfigurationBuilder;
import org.meanbean.test.EqualsMethodTester;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductFamilyTest {

    @Test
    public void test_beaniness() {
        Configuration configuration = new ConfigurationBuilder().ignoreProperty("productFamilyId").build();
        new BeanTester().testBean(ProductFamily.class, configuration);
        new EqualsMethodTester().testEqualsMethod(ProductFamily.class, configuration);
    }
}
