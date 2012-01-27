package org.broadinstitute.sequel.quotes;


import org.broadinstitute.sequel.quotes.data.Quote;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

public class QuoteServiceTest {

    @Inject
    private QuoteService service;


    @Test(enabled = false)
    public void testBasic() {

        boolean caught = false;

        try {
            Quote quote = service.getQuoteFromQuoteServer("GAN1F8");
            Assert.assertNotNull(quote);
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
