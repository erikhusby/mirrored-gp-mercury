package org.broadinstitute.pmbridge.infrastructure.quote;

/**
 * Based on test from SequeL
 */

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;
import static org.broadinstitute.pmbridge.TestGroups.UNIT;

@Test(groups = {UNIT})
public class QuoteServiceTest {

    @Test
    public void test_get_a_quote() throws Exception {
        QuoteService service = new MockQuoteServiceImpl();
        Quote quote = service.getQuoteByAlphaId("DNA23H");

        Assert.assertNotNull(quote);
        Assert.assertEquals("NIAID (CO 5035331)", quote.getName());
        Assert.assertEquals("5035331", quote.getQuoteFunding().getFundingLevel().getFunding().getCostObject());
        Assert.assertEquals("GENSEQCTR_(NIH)NIAID",quote.getQuoteFunding().getFundingLevel().getFunding().getGrantDescription());
        Assert.assertEquals(Funding.FUNDS_RESERVATION,quote.getQuoteFunding().getFundingLevel().getFunding().getFundingType());
        Assert.assertEquals("DNA23H",quote.getAlphanumericId());

        quote = service.getQuoteByAlphaId("DNA3A9");
        Assert.assertEquals("HARVARD UNIVERSITY",quote.getQuoteFunding().getFundingLevel().getFunding().getInstitute());
        Assert.assertEquals(Funding.PURCHASE_ORDER,quote.getQuoteFunding().getFundingLevel().getFunding().getFundingType());
        Assert.assertEquals("DNA3A9",quote.getAlphanumericId());

    }

    @Test
    public void test_get_all_quotes_for_sequencing() throws Exception {

        QuoteService service = new MockQuoteServiceImpl();
        Quotes quotes = service.getAllQuotes();

        Assert.assertNotNull(quotes);
        Assert.assertFalse(quotes.getQuotes().isEmpty());
        Set<String> grants = new HashSet<String>();
        Set<String> fundingTypes = new HashSet<String>();
        Set<String> pos = new HashSet<String>();
        for (Quote quote : quotes.getQuotes()) {
            if (quote.getQuoteFunding() != null) {
                if (quote.getQuoteFunding().getFundingLevel() != null) {
                    if (quote.getQuoteFunding().getFundingLevel().getFunding() != null) {
                        Funding funding = quote.getQuoteFunding().getFundingLevel().getFunding();
                        fundingTypes.add(funding.getFundingType());
                        //System.out.println(funding.getFundingType());
                        if (Funding.FUNDS_RESERVATION.equals(funding.getFundingType())) {
                            grants.add(funding.getGrantDescription());
                        }
                        else if (Funding.PURCHASE_ORDER.equals(funding.getFundingType())) {
                            pos.add(funding.getPurchaseOrderNumber());
                        }
                    }
                }

            }
        }
        Assert.assertEquals(fundingTypes.size(), 3);   // includes null fundingType
        Assert.assertTrue(fundingTypes.contains(Funding.FUNDS_RESERVATION));
        Assert.assertTrue(fundingTypes.contains(Funding.PURCHASE_ORDER));
    }

}
