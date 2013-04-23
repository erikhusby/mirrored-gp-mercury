package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.meanbean.lang.EquivalentFactory;
import org.meanbean.test.*;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.Random;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductTest {

    @Test
    public void test_beaniness() {
        BeanTester tester = new BeanTester();

        // The days setter/getter calculates off the seconds, which seems to stomp between get and set, so just remove
        Configuration configuration = new ConfigurationBuilder()
                .ignoreProperty("expectedCycleTimeDays")
                .ignoreProperty("guaranteedCycleTimeDays").build();

        new BeanTester().testBean(Product.class, configuration);
    }

    @Test
    public void testEquals() throws Exception {
        BeanTester tester = new BeanTester();
        // Ignore everything except product part number
        Configuration configuration = new ConfigurationBuilder()
                .ignoreProperty("expectedCycleTimeSeconds")
                .ignoreProperty("expectedCycleTimeDays")
                .ignoreProperty("pdmOrderableOnly")
                .ignoreProperty("samplesPerWeek")
                .ignoreProperty("minimumOrderSize")
                .ignoreProperty("inputRequirements")
                .ignoreProperty("deliverables")
                .ignoreProperty("workflowName")
                .ignoreProperty("primaryPriceItem")
                .ignoreProperty("description")
                .ignoreProperty("availabilityDate")
                .ignoreProperty("productFamily")
                .ignoreProperty("productName")
                .ignoreProperty("guaranteedCycleTimeSeconds")
                .ignoreProperty("guaranteedCycleTimeDays")
                .ignoreProperty("topLevelProduct")
                .ignoreProperty("discontinuedDate")
                .ignoreProperty("useAutomatedBilling")
                .ignoreProperty("aggregationDataType")
                .build();

        class ProductFactory implements EquivalentFactory<Product> {
            public final long ID = new Random().nextInt(Integer.MAX_VALUE);
            @Override public Product create() {
                Product product = new Product("Exome Express", null, "Exome Express", "P-EX-0002", new Date(), null,
                        1814400, 1814400, 184, null, null, null, true, "Exome Express", false, "agg type");
                return product;
            }
        }
        new EqualsMethodTester().testEqualsMethod(new ProductFactory(), configuration);

    }

    @Test
    public void testHashCode() throws Exception {
        new HashCodeMethodTester().testHashCodeMethod(Product.class);
    }
}
