package org.broadinstitute.sequel.integration.infrastructure.quote;

import org.broadinstitute.sequel.infrastructure.quote.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

public class QuoteServiceTest {

    private Quote quote;

    private  PriceItem priceItem;

    @BeforeClass
    private void setupLargeQuoteAndPriceItem() {
        quote = new Quote("DNA4JC",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NHGRI"))));
        priceItem = new PriceItem("Illumina Sequencing","1","Illumina Custom Hybrid Selection Library (93 sample batch size)","15","bannanas","DNA Sequencing");
    }

    @Test(groups = {EXTERNAL_INTEGRATION}, enabled = false)
    public void test_register_work() throws Exception {
        QuoteServiceImpl service = new QuoteServiceImpl(new QAQuoteConnectionParams());
        Quote fetchedQuote = service.getQuoteFromQuoteServer(quote.getAlphanumericId());
        System.out.println(fetchedQuote.getQuoteFunding().getFundsRemaining());
        String workBatchId = service.registerNewWork(quote,priceItem,0.0001,"http://www.SequeLTesting","paramName","paramValue");
        System.out.println(fetchedQuote.getQuoteFunding().getFundsRemaining());

        Assert.assertNotNull(workBatchId);

        try {
            long workItemId = Long.parseLong(workBatchId);
            Assert.assertTrue(workItemId > 0);
        }
        catch(NumberFormatException e) {
            Assert.fail(workBatchId + " returned from quote server is not a number");
        }
    }
}
