package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class QuoteServiceTest {

    private Quote quote;

    private QuotePriceItem quotePriceItem;
    public final QuoteService service = QuoteServiceProducer.testInstance();

    @BeforeClass(groups = EXTERNAL_INTEGRATION)
    private void setupLargeQuoteAndPriceItem() {
        quote = new Quote("DNA4JC",new QuoteFunding(Collections.singleton(new FundingLevel("100",
                Collections.singleton(new Funding(Funding.FUNDS_RESERVATION, "NHGRI", "NHGRI"))))), ApprovalStatus.FUNDED);
        quotePriceItem = new QuotePriceItem("Illumina Sequencing","1","Illumina Custom Hybrid Selection Library (93 sample batch size)","15","bannanas","DNA Sequencing");
    }

    @Test(groups = {EXTERNAL_INTEGRATION}, enabled = false)
    public void test_register_work() throws Exception {
        Quote fetchedQuote = service.getQuoteByAlphaId(quote.getAlphanumericId());
        System.out.println(fetchedQuote.getQuoteFunding().getFundsRemaining());
        String workBatchId =
            service.registerNewWork(quote, quotePriceItem, null, new Date(), BigDecimal.valueOf(0.0001),
                    "http://www.MercuryTesting", "paramName", "paramValue", null);
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

    // test works but the method under test doesn't seem to be used so why slow down our builds
    @Test(enabled = true)
    public void test_get_a_quote() throws Exception {

        Quote quote = service.getQuoteByAlphaId("DNA23H");

        Assert.assertNotNull(quote);
        Assert.assertEquals("NIAID (CO 5035331)", quote.getName());

        for(FundingLevel level : quote.getQuoteFunding().getFundingLevel()) {

            for (Funding funding :level.getFunding()) {
                if(funding.getCostObject().equals("5035331")) {
                    Assert.assertEquals("GENSEQCTR_(NIH)NIAID",
                            funding.getGrantDescription());
                    Assert.assertEquals(Funding.FUNDS_RESERVATION,
                            funding.getFundingType());
                } else if(funding.getCostObject().equals("5030300")) {
                    Assert.assertEquals("NIAIDSURVEILLANCEOPT",
                            funding.getGrantDescription());
                    Assert.assertEquals(Funding.FUNDS_RESERVATION,
                            funding.getFundingType());
                } else {
                    Assert.fail("Encountered an unrecognized funding level");
                }
            }
        }
        Assert.assertEquals("DNA23H", quote.getAlphanumericId());

        quote = service.getQuoteByAlphaId("DNA3A9");
        for(FundingLevel level : quote.getQuoteFunding().getFundingLevel()) {
            for (Funding funding :level.getFunding()) {
                Assert.assertEquals("HARVARD UNIVERSITY", funding.getInstitute());
                Assert.assertEquals(Funding.PURCHASE_ORDER, funding.getFundingType());
            }
        }
        Assert.assertEquals("DNA3A9", quote.getAlphanumericId());

    }

    // test works but the method under test doesn't seem to be used so why slow down our builds
    @Test(enabled = false)
    public void test_get_all_quotes_for_sequencing() throws Exception {

        Quotes quotes = service.getAllQuotes();
        Set<String> fundingTypes = QuoteServiceDBFreeTest.getFundingTypes(quotes);

        Assert.assertEquals(fundingTypes.size(), 2);   // includes null fundingType
        Assert.assertTrue(fundingTypes.contains(Funding.FUNDS_RESERVATION));
        Assert.assertTrue(fundingTypes.contains(Funding.PURCHASE_ORDER));
    }


    // curl --location --user 'rnordin@broadinstitute.org:Squ1d_us3r' 'http://quoteqa.broadinstitute.org:8080/quotes/ws/portals/private/get_price_list'
    // curl --user 'rnordin@broadinstitute.org:Squ1d_us3r' 'http://quoteqa.broadinstitute.org:8080/quotes/rest/price_list/10
    public void testPriceItems() {
        try {
            PriceList priceItems = service.getAllPriceItems();
            Assert.assertNotNull(priceItems);
            Assert.assertTrue(priceItems.getQuotePriceItems().size() > 10);
        }
        catch (Exception e) {
            Assert.fail(e.toString());
        }
    }




}
