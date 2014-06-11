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

import net.sourceforge.stripes.action.Before;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductComparatorTest {
    @DataProvider
    public Object[][] dataProvider() {
        return new Object[][]{
                new Object[]{"A", "B", -1},
                new Object[]{"A", "A", 0},
                new Object[]{"B", "A", 1},
                new Object[]{"A-00000", "A-00001", -1},
                new Object[]{"A-00001", "A-00001", 0},
                new Object[]{"A-00001", "A-00000", 1},
        };
    }

    @Before
    public void setUp() {
    }

    @Test(dataProvider = "dataProvider")
    public void testCompareByPartNumber(String partNumber, String otherPartNumber, int expectedResult) {
        Product standardExomeSequencing = ProductTestFactory.createStandardExomeSequencing();
        Product anotherStandardExomeSequencing = ProductTestFactory.createStandardExomeSequencing();

        standardExomeSequencing.setPartNumber(partNumber);
        anotherStandardExomeSequencing.setPartNumber(otherPartNumber);

        int actualResult = Product.BY_PART_NUMBER.compare(standardExomeSequencing, anotherStandardExomeSequencing);
        Assert.assertEquals(actualResult, expectedResult);
    }
}
