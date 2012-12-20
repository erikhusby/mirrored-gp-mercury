package org.broadinstitute.gpinformatics.athena.entity.samples;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 12/17/12
 * Time: 10:51 AM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class MaterialTypeTest {

    @Test
    public void testBeaniness() {
        new BeanTester().testBean(MaterialType.class);
        new EqualsMethodTester().testEqualsMethod(MaterialType.class);
    }

    public void testCompareTo() throws Exception {
        MaterialType materialTypeA1 = new MaterialType( "namea", "category");
        MaterialType materialTypeA2 = new MaterialType( "namea", "category");
        Assert.assertEquals(0,materialTypeA1.compareTo( materialTypeA2));

        MaterialType materialTypeB = new MaterialType( "nameb", "category");
        Assert.assertEquals(-1, materialTypeA1.compareTo( materialTypeB));

        try {
            MaterialType materialTypeC1 = new MaterialType( null, "category");
            Assert.fail("expected npe exception");
        } catch ( Exception e ) {
        }

        try {
            MaterialType materialTypeC2 = new MaterialType( "nameb", null);
            Assert.fail("expected npe exception");
        } catch ( Exception e ) {
        }

         Assert.assertEquals(-1, materialTypeA1.compareTo( materialTypeB));

    }

    @Test
    public void testEquals() throws Exception {
        new EqualsMethodTester().testEqualsMethod(MaterialType.class);
    }

    @Test
    public void testHashCode() throws Exception {
        new HashCodeMethodTester().testHashCodeMethod(MaterialType.class);
    }

}
