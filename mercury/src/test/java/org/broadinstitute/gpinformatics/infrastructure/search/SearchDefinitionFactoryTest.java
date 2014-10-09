package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;

/**
 * Test features of SearchDefinitionFactory
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SearchDefinitionFactoryTest {

    /**
     * Cover all BigDecimal format inputs.
     */
    @Test
    public void testBigDecimalFormatter() {

        String output;
        BigDecimal input;

        output = SearchDefinitionFactory.formatReportDecimal(null);
        Assert.assertEquals(output, "");

        input = BigDecimal.ZERO;
        output = SearchDefinitionFactory.formatReportDecimal(input);
        Assert.assertEquals(output, "0.00");

        input = new BigDecimal(".004");
        output = SearchDefinitionFactory.formatReportDecimal(input);
        Assert.assertEquals(output, "0.00");

        input = new BigDecimal(".005");
        output = SearchDefinitionFactory.formatReportDecimal(input);
        Assert.assertEquals(output, "0.01");

        input = new BigDecimal("-.004");
        output = SearchDefinitionFactory.formatReportDecimal(input);
        Assert.assertEquals(output, "-0.00");

        input = new BigDecimal("-.005");
        output = SearchDefinitionFactory.formatReportDecimal(input);
        Assert.assertEquals(output, "-0.01");

        input = BigDecimal.TEN;
        output = SearchDefinitionFactory.formatReportDecimal(input);
        Assert.assertEquals(output, "10.00");

        input = BigDecimal.TEN.negate();
        output = SearchDefinitionFactory.formatReportDecimal(input);
        Assert.assertEquals(output, "-10.00");

        input = new BigDecimal("10.995");
        output = SearchDefinitionFactory.formatReportDecimal(input);
        Assert.assertEquals(output, "11.00");

        input = new BigDecimal("10.994");
        output = SearchDefinitionFactory.formatReportDecimal(input);
        Assert.assertEquals(output, "10.99");

        input = new BigDecimal("-10.995");
        output = SearchDefinitionFactory.formatReportDecimal(input);
        Assert.assertEquals(output, "-11.00");

        input = new BigDecimal("-10.994");
        output = SearchDefinitionFactory.formatReportDecimal(input);
        Assert.assertEquals(output, "-10.99");

    }
}
