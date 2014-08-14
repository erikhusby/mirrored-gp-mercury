package org.broadinstitute.gpinformatics.mercury.integration.infrastructure.quote;

import org.broadinstitute.gpinformatics.infrastructure.quote.*;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Date;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class QuoteServiceTest {

    private Quote quote;

    private QuotePriceItem quotePriceItem;

    @BeforeClass(groups = EXTERNAL_INTEGRATION)
    private void setupLargeQuoteAndPriceItem() {
        quote = new Quote("DNA4JC",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NHGRI", "NHGRI"))), ApprovalStatus.FUNDED);
        quotePriceItem = new QuotePriceItem("Illumina Sequencing","1","Illumina Custom Hybrid Selection Library (93 sample batch size)","15","bannanas","DNA Sequencing");
    }

    @Test(groups = {EXTERNAL_INTEGRATION}, enabled = false)
    public void test_register_work() throws Exception {
        QuoteService service = QuoteServiceProducer.testInstance();
        Quote fetchedQuote = service.getQuoteByAlphaId(quote.getAlphanumericId());
        System.out.println(fetchedQuote.getQuoteFunding().getFundsRemaining());
        String workBatchId =
            service.registerNewWork(quote, quotePriceItem, null, new Date(), 0.0001,
                                    "http://www.MercuryTesting","paramName","paramValue");
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
