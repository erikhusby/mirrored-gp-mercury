package org.broadinstitute.sequel.quotes;


import org.broadinstitute.sequel.control.quote.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

public class QuoteServiceTest {


    @Test(enabled = true)
    public void test_get_a_quote() {

        boolean caught = false;

        QuoteServiceImpl service = new QuoteServiceImpl(new QuoteConnectionParametersImpl(
                QuoteConnectionParametersImpl.QA_HOST +
                QuoteConnectionParametersImpl.GET_SINGLE_QUOTE_URL));
        try {
            Quote quote = service.getQuoteFromQuoteServer("DNA3CD");
            Assert.assertNotNull(quote);
            Assert.assertEquals("Regev ChIP Sequencing 8-1-2011", quote.getName());
            Assert.assertEquals("6820110", quote.getQuoteFunding().getFundingLevel().getFunding().getCostObject());
            Assert.assertEquals("ZEBRAFISH_NIH_REGEV",quote.getQuoteFunding().getFundingLevel().getFunding().getGrantDescription());
        }
        catch (QuoteNotFoundException nfx) {
            caught = true;
        }
        catch (QuoteServerException sx) {
            caught = true;
        }
        Assert.assertFalse(caught);

    }

    @Test(enabled = true)
    public void test_get_all_quotes_for_sequencing() {

        boolean caught = false;

        QuoteServiceImpl service = new QuoteServiceImpl(new QuoteConnectionParametersImpl(
                QuoteConnectionParametersImpl.QA_HOST +
                QuoteConnectionParametersImpl.GET_ALL_SEQUENCING_QUOTES_URL));
        try {
            Quotes quotes = service.getAllSequencingPlatformQuotes();
            Assert.assertNotNull(quotes);
            Assert.assertFalse(quotes.getQuotes().isEmpty());
            Set<String> grants = new HashSet<String>();
            Set<String> fundingTypes = new HashSet<String>();
            Set<String> pos = new HashSet<String>();
            for (Quote quote : quotes.getQuotes()) {
                System.out.println(quote.getName() + "/" + quote.getAlphanumericId());
                if (quote.getQuoteFunding() != null) {
                    if (quote.getQuoteFunding().getFundingLevel() != null) {
                        if (quote.getQuoteFunding().getFundingLevel().getFunding() != null) {
                            Funding funding = quote.getQuoteFunding().getFundingLevel().getFunding();
                            fundingTypes.add(funding.getFundingType());
                            
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
            Assert.assertEquals(2,fundingTypes.size());
            Assert.assertTrue(fundingTypes.contains(Funding.FUNDS_RESERVATION));
            Assert.assertTrue(fundingTypes.contains(Funding.PURCHASE_ORDER));
        }
        catch (QuoteNotFoundException nfx) {
            caught = true;
        }
        catch (QuoteServerException sx) {
            caught = true;
        }
        Assert.assertFalse(caught);
    }

}
