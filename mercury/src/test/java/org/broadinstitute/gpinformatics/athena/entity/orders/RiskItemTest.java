package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.meanbean.test.*;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/22/13
 * Time: 5:00 PM
 */
public class RiskItemTest {

    @Test
    public void test_beaniness() {
        BeanTester tester = new BeanTester();
        Configuration configuration = new ConfigurationBuilder()
                .build();

        new BeanTester().testBean(RiskItem.class, configuration);
    }

    @Test
    public void testEquals() throws Exception {
        new EqualsMethodTester().testEqualsMethod(RiskItem.class,"risk_id");
    }

    @Test
    public void testHashCode() throws Exception {
        new HashCodeMethodTester().testHashCodeMethod(RiskItem.class);
    }

}
