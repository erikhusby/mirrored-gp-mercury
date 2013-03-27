package org.broadinstitute.gpinformatics.athena.entity.samples;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.testng.Assert;
import org.testng.annotations.Test;


@Test(groups = TestGroups.DATABASE_FREE)
public class MaterialTypeTest {

    @Test
    public void testBeaniness() {
        new BeanTester().testBean(MaterialType.class);
        new EqualsMethodTester().testEqualsMethod(MaterialType.class, "fullName");
    }

    public void testCompareTo() throws Exception {
        MaterialType materialTypeA1 = new MaterialType("category", "namea");
        MaterialType materialTypeA2 = new MaterialType("category", "namea");
        Assert.assertEquals(0, materialTypeA1.compareTo(materialTypeA2));

        MaterialType materialTypeB = new MaterialType("category", "nameb");
        Assert.assertEquals(-1, materialTypeA1.compareTo(materialTypeB));

        try {
            @SuppressWarnings({"UnusedDeclaration", "ConstantConditions"})
            MaterialType materialTypeC1 = new MaterialType("category", null);
            Assert.fail("expected npe exception");
        } catch (Exception e) {
        }

        try {
            @SuppressWarnings({"UnusedDeclaration", "ConstantConditions"})
            MaterialType materialTypeC2 = new MaterialType(null, "nameb");
            Assert.fail("expected npe exception");
        } catch (Exception e) {
        }

        Assert.assertEquals(-1, materialTypeA1.compareTo(materialTypeB));

    }

    @Test
    public void testEquals() throws Exception {
        new EqualsMethodTester().testEqualsMethod(MaterialType.class, "fullName");
    }

    @Test
    public void testHashCode() throws Exception {
        new HashCodeMethodTester().testHashCodeMethod(MaterialType.class);
    }

}
