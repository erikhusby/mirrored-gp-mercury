package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;

@Test(groups = TestGroups.DATABASE_FREE)
public class MathUtilsTest {

    @DataProvider(name = "isSameData")
    public Object[][] isSameDataProvider() {
        // Compute 'almost' one. Due to the way Java represents floating point numbers this value is actually 0.99999...
        // and almostOne == 1 will return false.
        double almostOne = 0;
        for (int i = 0; i < 10; i++) {
            almostOne += 0.1;
        }

        return new Object[][] {
                { almostOne, 1, true },
                { almostOne + 0.00001, 1, false }
        };
    }

    @Test(dataProvider = "isSameData")
    public void testIsSame(double actual, double expected, boolean result) throws Exception {
        Assert.assertEquals(MathUtils.isSame(actual, expected), result);
    }

    public void testScaling(){
        BigDecimal output;

        output = MathUtils.scaleTwoDecimalPlaces( new BigDecimal(".001") );
        Assert.assertEquals( output, new BigDecimal("0.00") );

        output = MathUtils.scaleTwoDecimalPlaces( new BigDecimal("-0.001") );
        Assert.assertEquals( output, new BigDecimal("0.00") );

        output = MathUtils.scaleTwoDecimalPlaces( new BigDecimal(".009") );
        Assert.assertEquals( output, new BigDecimal("0.01") );

        output = MathUtils.scaleTwoDecimalPlaces( new BigDecimal("-0.009") );
        Assert.assertEquals( output, new BigDecimal("-0.01") );
    }
}
