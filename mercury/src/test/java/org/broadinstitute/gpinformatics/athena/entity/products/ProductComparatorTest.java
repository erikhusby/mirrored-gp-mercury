/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductComparatorTest {
    @DataProvider
    public Object[][] byProductNameDataProvider() {
        return new Object[][]{
                new Object[]{"A", "B", -1},
                new Object[]{"A", "A", 0},
                new Object[]{"B", "A", 1},
                new Object[]{"A-00000", "A-00001", -1},
                new Object[]{"A-00001", "A-00001", 0},
                new Object[]{"A-00001", "A-00000", 1},
        };
    }

    @Test(dataProvider = "byProductNameDataProvider")
    public void testCompareByProductName(String productName, String otherProductName, int expectedResult) {
        Product standardExomeSequencing = ProductTestFactory.createStandardExomeSequencing(null, productName);
        Product anotherStandardExomeSequencing = ProductTestFactory.createStandardExomeSequencing(null, otherProductName);

        int actualResult = Product.BY_PRODUCT_NAME.compare(standardExomeSequencing, anotherStandardExomeSequencing);
        Assert.assertEquals(actualResult, expectedResult);
    }

    @DataProvider
    public Object[][] byFamilyThenProductNameDataProvider() {
        return new Object[][]{
                new Object[]{"A", "B", "Z", "Z", -1},
                new Object[]{"A", "A", "Z", "Z", 0},
                new Object[]{"B", "A", "Z", "Z", 1},
                new Object[]{"A-00000", "A-00001", "Z", "Z", -1},
                new Object[]{"A-00001", "A-00001", "Z", "Z", 0},
                new Object[]{"A-00001", "A-00000", "Z", "Z", 1},

                new Object[]{"X", "X", "A", "B", -1},
                new Object[]{"X", "X", "A", "A", 0},
                new Object[]{"X", "X", "B", "A", 1},
                new Object[]{"X", "X", "A-00000", "A-00001", -1},
                new Object[]{"X", "X", "A-00001", "A-00001", 0},
                new Object[]{"X", "X", "A-00001", "A-00000", 1},

        };
    }

    @Test(dataProvider = "byFamilyThenProductNameDataProvider")
    public void testByFamilyThenProductName(String familyName, String otherFamilyName, String productName,
                                                        String otherProductName, int expectedResult) {
        Product standardExomeSequencing = ProductTestFactory.createStandardExomeSequencing(familyName, productName);
        Product anotherStandardExomeSequencing = ProductTestFactory.createStandardExomeSequencing(otherFamilyName, otherProductName);

        int actualResult = Product.BY_FAMILY_THEN_PRODUCT_NAME.compare(standardExomeSequencing, anotherStandardExomeSequencing);
        Assert.assertEquals(actualResult, expectedResult);
    }
}
