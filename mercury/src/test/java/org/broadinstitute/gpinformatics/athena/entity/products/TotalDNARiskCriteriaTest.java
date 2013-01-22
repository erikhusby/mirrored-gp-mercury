package org.broadinstitute.gpinformatics.athena.entity.products;

import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/22/13
 * Time: 3:20 PM
 */
public class TotalDNARiskCriteriaTest {
    @Test
    public void testOnRisk() throws Exception {

    }

    @Test
    public void testEquals() throws Exception {
        new EqualsMethodTester().testEqualsMethod(TotalDNARiskCriteria.class );
    }

    @Test
    public void testHashCode() throws Exception {
        new HashCodeMethodTester().testHashCodeMethod(TotalDNARiskCriteria.class);
    }

}
