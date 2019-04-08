package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class QuoteFundingTest {

    public void testMultipleFundingFiltered() throws Exception {
        QuoteService service = QuoteServiceProducer.stubInstance();

        Quote quoteForTesting = service.getQuoteByAlphaId("GP87U");
        Assert.assertEquals(quoteForTesting.getQuoteFunding().getFundingLevel().size(), 3);
        Assert.assertEquals("The filtered number of funding sources for GP87U should now be 1",
                quoteForTesting.getQuoteFunding().getActiveFundingLevel().size(), 1);
    }
}
