package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class QuoteTest {

    public void testQuoteIneligibiltySplitFunding() throws Exception{
        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote gp87Uquote = stubbedQuoteService.getQuoteByAlphaId("GP87U");

        boolean quoteEligibility = true;

        try {
            quoteEligibility = gp87Uquote.isEligibleForSAP();
            Assert.assertFalse(quoteEligibility);
        } catch (Exception shouldNotHappen) {
            Assert.fail(shouldNotHappen.toString());
        }

    }

    public void testTrueSplitFundingEligibilty() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote splitFundedQuote = stubbedQuoteService.getQuoteByAlphaId("GXP2B1");

        boolean quoteEligibility = true;

        try {
            quoteEligibility = splitFundedQuote.isEligibleForSAP();
            Assert.assertFalse(quoteEligibility);
        } catch (Exception shouldNotHappen) {
            Assert.fail(shouldNotHappen.toString());
        }
    }
    public void testSplitCostObjectEligibilty() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote splitCostObjectQuote = stubbedQuoteService.getQuoteByAlphaId("STCIL1");

        boolean quoteEligibility = true;

        try {
            quoteEligibility = splitCostObjectQuote.isEligibleForSAP();
            Assert.assertFalse(quoteEligibility);
        } catch (Exception shouldNotHappen) {
            Assert.fail(shouldNotHappen.toString());
        }
    }
    public void testTrueEligibleQuote() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote eligibleQuote = stubbedQuoteService.getQuoteByAlphaId("STC3ZW");

        boolean quoteEligibility = true;

        try {
            quoteEligibility = eligibleQuote.isEligibleForSAP();
            Assert.assertTrue(quoteEligibility);
        } catch (Exception shouldNotHappen) {
            Assert.fail(shouldNotHappen.toString());
        }
    }


    public void testInligibleCostObjectQuote() throws Exception {

        QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();

        Quote eligibleQuote = stubbedQuoteService.getQuoteByAlphaId("MPG20W");

        boolean quoteEligibility = true;

        try {
            quoteEligibility = eligibleQuote.isEligibleForSAP();
            Assert.assertFalse(quoteEligibility);
        } catch (Exception shouldNotHappen) {
            Assert.fail(shouldNotHappen.toString());
        }
    }

}
