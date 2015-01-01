package org.broadinstitute.gpinformatics.infrastructure.quote;

/**
 * Based on test from Mercury
 */

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Set;


@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class PMBQuoteServiceTest {
    private PMBQuoteService pmbQuoteService=PMBQuoteServiceProducer.testInstance();

    // test works but the method under test doesn't seem to be used so why slow down our builds
    @Test(enabled = false)
    public void test_get_a_quote() throws Exception {

        Quote quote = pmbQuoteService.getQuoteByAlphaId("DNA23H");

        Assert.assertNotNull(quote);
        Assert.assertEquals("NIAID (CO 5035331)", quote.getName());
        Assert.assertEquals("5035331", quote.getQuoteFunding().getFundingLevel().getFunding().getCostObject());
        Assert.assertEquals("GENSEQCTR_(NIH)NIAID",quote.getQuoteFunding().getFundingLevel().getFunding().getGrantDescription());
        Assert.assertEquals(Funding.FUNDS_RESERVATION,quote.getQuoteFunding().getFundingLevel().getFunding().getFundingType());
        Assert.assertEquals("DNA23H",quote.getAlphanumericId());

        quote = pmbQuoteService.getQuoteByAlphaId("DNA3A9");
        Assert.assertEquals("HARVARD UNIVERSITY",quote.getQuoteFunding().getFundingLevel().getFunding().getInstitute());
        Assert.assertEquals(Funding.PURCHASE_ORDER,quote.getQuoteFunding().getFundingLevel().getFunding().getFundingType());
        Assert.assertEquals("DNA3A9",quote.getAlphanumericId());

    }

    // test works but the method under test doesn't seem to be used so why slow down our builds
    @Test(enabled = false)
    public void test_get_all_quotes_for_sequencing() throws Exception {

        Quotes quotes = pmbQuoteService.getAllQuotes();
        Set<String> fundingTypes = QuoteServiceTest.getFundingTypes(quotes);

        Assert.assertEquals(fundingTypes.size(), 2);   // includes null fundingType
        Assert.assertTrue(fundingTypes.contains(Funding.FUNDS_RESERVATION));
        Assert.assertTrue(fundingTypes.contains(Funding.PURCHASE_ORDER));
    }


    // curl --location --user 'rnordin@broadinstitute.org:Squ1d_us3r' 'http://quoteqa.broadinstitute.org:8080/quotes/ws/portals/private/get_price_list'
    // curl --user 'rnordin@broadinstitute.org:Squ1d_us3r' 'http://quoteqa.broadinstitute.org:8080/quotes/rest/price_list/10
    public void testPriceItems() {
        try {
            PriceList priceItems = pmbQuoteService.getAllPriceItems();
            Assert.assertNotNull(priceItems);
            Assert.assertTrue(priceItems.getQuotePriceItems().size() > 10);
        }
        catch (Exception e) {
            Assert.fail(e.toString());
        }
    }
}
