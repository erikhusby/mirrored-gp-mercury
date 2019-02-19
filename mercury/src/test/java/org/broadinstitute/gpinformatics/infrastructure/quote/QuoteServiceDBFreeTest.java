package org.broadinstitute.gpinformatics.infrastructure.quote;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

@Test(groups = TestGroups.DATABASE_FREE)
public class QuoteServiceDBFreeTest {

    private Quote quote;

    private QuotePriceItem quotePriceItem;


    @BeforeClass(groups = DATABASE_FREE)
    private void setupLargeQuoteAndPriceItem() {
        quote = new Quote("DNA4JD",
                new QuoteFunding(Collections.singleton(new FundingLevel("100",
                        Collections.singleton(new Funding(Funding.FUNDS_RESERVATION, "NHGRI", "NHGRI"))))), ApprovalStatus.FUNDED);
        quotePriceItem = new QuotePriceItem("Illumina Sequencing","1","Illumina HiSeq Run 44 Base","15","bannan","DNA Sequencing");
    }

    /**
     * If this test fails because the quote has been used up, 
     * visit the QA quote server,
     * login with the credentials, run the ALL_SEQUENCING_QUOTES URL,
     * find a quote that doesn't expire for a while, and change {@link #quote} in
     * @{link #setupLargeQuoteAndPriceItem}.
     * @throws Exception
     */
    @Test(groups = {DATABASE_FREE})
    public void test_get_all_price_items() throws Exception {
        QuoteService service = new QuoteServiceStub();
        PriceList priceList = service.getAllPriceItems();
        Assert.assertFalse(priceList.getQuotePriceItems().isEmpty());

        final QuotePriceItem cryovialPriceItem = priceList.findByKeyFields("Biological Samples", "Sample Kit",
                "Cryovials Partial Kit (1 - 40 Samples)");

        Assert.assertNotNull(cryovialPriceItem.getEffectiveDate(), cryovialPriceItem.getName() +
                                                                   " should not have a null effective date");
        Assert.assertNotNull(cryovialPriceItem.getSubmittedDate(), cryovialPriceItem.getName() +
                                                                   " should not have a null submitted date");
    }

    @Test(groups = DATABASE_FREE)
    public void test_bad_response_code() {
        QuoteServiceImpl service = new QuoteServiceImpl(null);
        ClientResponse mockResponse = EasyMock.createMock(ClientResponse.class);

        EasyMock.expect(mockResponse.getClientResponseStatus()).andReturn(ClientResponse.Status.BAD_REQUEST).atLeastOnce();
        EasyMock.replay(mockResponse);
        try {
            service.registerNewWork(mockResponse,quote, quotePriceItem,0.0001);
            Assert.fail("Should have thrown an exception when bad http response returned");
        }
        catch(Exception e) {}
        EasyMock.verify(mockResponse);
    }

    @Test(groups = DATABASE_FREE)
    public void test_null_response() {
        QuoteServiceImpl service = new QuoteServiceImpl(null);
        ClientResponse mockResponse = EasyMock.createMock(ClientResponse.class);
        EasyMock.reset(mockResponse);
        EasyMock.expect(mockResponse.getClientResponseStatus()).andReturn(null).atLeastOnce();
        EasyMock.replay(mockResponse);
        try {
            service.registerNewWork(mockResponse,quote, quotePriceItem,0.0001);
            Assert.fail("Should have thrown an exception when no client response was returned");
        }
        catch(Exception e) {}
        EasyMock.verify(mockResponse);

        EasyMock.reset(mockResponse);
        EasyMock.expect(mockResponse.getClientResponseStatus()).andReturn(ClientResponse.Status.OK).atLeastOnce();
        EasyMock.expect(mockResponse.getEntity(String.class)).andReturn(null).atLeastOnce();

        EasyMock.replay(mockResponse);
        try {
            service.registerNewWork(mockResponse,quote, quotePriceItem,0.0001);
            Assert.fail("Should have thrown an exception when string returned was null");
        }
        catch(Exception e) {}
        EasyMock.verify(mockResponse);
    }

    @Test(groups = DATABASE_FREE)
    public void test_bad_work_unit_return() {
        QuoteServiceImpl service = new QuoteServiceImpl(null);
        ClientResponse mockResponse = EasyMock.createMock(ClientResponse.class);

        EasyMock.reset(mockResponse);
        EasyMock.expect(mockResponse.getClientResponseStatus()).andReturn(ClientResponse.Status.OK).atLeastOnce();
        EasyMock.expect(mockResponse.getEntity(String.class)).andReturn("Oh Crap!").atLeastOnce();

        EasyMock.replay(mockResponse);
        try {
            service.registerNewWork(mockResponse,quote, quotePriceItem,0.0001);
            Assert.fail("Should have thrown an exception when string returned was null");
        }
        catch(Exception e) {}
        EasyMock.verify(mockResponse);

        EasyMock.reset(mockResponse);
        EasyMock.expect(mockResponse.getClientResponseStatus()).andReturn(ClientResponse.Status.OK).atLeastOnce();
        EasyMock.expect(mockResponse.getEntity(String.class)).andReturn(QuoteServiceImpl.WORK_ITEM_ID + QuoteServiceImpl.WORK_ITEM_ID + "Oh\tCrap").atLeastOnce();

        EasyMock.replay(mockResponse);
        try {
            service.registerNewWork(mockResponse,quote, quotePriceItem,0.0001);
            Assert.fail("Should have thrown an exception when string returned was null");
        }
        catch(Exception e) {}
        EasyMock.verify(mockResponse);

        EasyMock.reset(mockResponse);
        EasyMock.expect(mockResponse.getClientResponseStatus()).andReturn(ClientResponse.Status.OK).atLeastOnce();
        EasyMock.expect(mockResponse.getEntity(String.class)).andReturn(QuoteServiceImpl.WORK_ITEM_ID + " ").atLeastOnce();

        EasyMock.replay(mockResponse);
        try {
            service.registerNewWork(mockResponse,quote, quotePriceItem,0.0001);
            Assert.fail("Should have thrown an exception when string returned was null");
        }
        catch(Exception e) {}
        EasyMock.verify(mockResponse);
    }

    @Test(groups = {DATABASE_FREE})
    public void test_get_a_quote() throws Exception {
        QuoteService service = new QuoteServiceStub();
        Quote quote = service.getQuoteByAlphaId("DNA4AA");
        Assert.assertNotNull(quote);
        Assert.assertEquals("Regev Zebrafish RNASeq 2-6-12", quote.getName());
        for(FundingLevel level : quote.getQuoteFunding().getFundingLevel()) {

            for (Funding funding :level.getFunding()) {
                Assert.assertEquals("6820110", funding.getCostObject());
                Assert.assertEquals("ZEBRAFISH_NIH_REGEV", funding.getGrantDescription());
                Assert.assertEquals(Funding.FUNDS_RESERVATION, funding.getFundingType());
            }
        }
        Assert.assertEquals("DNA4AA",quote.getAlphanumericId());

        quote = service.getQuoteByAlphaId("DNA3A9");
        for(FundingLevel level : quote.getQuoteFunding().getFundingLevel()) {

            for (Funding funding :level.getFunding()) {
                Assert.assertEquals("HARVARD UNIVERSITY", funding.getInstitute());
                Assert.assertEquals(Funding.PURCHASE_ORDER, funding.getFundingType());
            }
        }
        Assert.assertEquals("DNA3A9",quote.getAlphanumericId());

    }

    @Test(groups = {DATABASE_FREE})
    public void test_get_all_quotes_for_sequencing() throws Exception {

        boolean caught = false;

        QuoteService service = new QuoteServiceStub();
        Quotes quotes = service.getAllSequencingPlatformQuotes();
        Set<String> fundingTypes = getFundingTypes(quotes);

//        Assert.assertEquals(2,fundingTypes.size(), "Funding Types are: [" + StringUtils.join(fundingTypes,", ") + "]");
        Assert.assertTrue(fundingTypes.contains(Funding.FUNDS_RESERVATION));
        Assert.assertTrue(fundingTypes.contains(Funding.PURCHASE_ORDER));
    }

    @Test(groups = DATABASE_FREE)
    public void testQuotesXmlTransform() throws Exception {
        QuoteService service = new QuoteServiceStub();
        Quote singleTestQuote = service.getQuoteByAlphaId("GAN1MS");

        for (QuoteItem quoteItem : singleTestQuote.getQuoteItems()) {
            Assert.assertTrue(StringUtils.isNotBlank(quoteItem.getPlatform()));
        }
    }

    public static Set<String> getFundingTypes(Quotes quotes) {
        Assert.assertNotNull(quotes);
        Assert.assertFalse(quotes.getQuotes().isEmpty());
        Set<String> fundingTypes = new HashSet<>();
        for (Quote quote : quotes.getQuotes()) {
            if (quote.getQuoteFunding() != null) {
                if (CollectionUtils.isNotEmpty(quote.getQuoteFunding().getFundingLevel())) {
                    for(FundingLevel level : quote.getQuoteFunding().getFundingLevel()) {
                        if (CollectionUtils.isNotEmpty(level.getFunding())) {
                            for (Funding funding :level.getFunding()) {
                                fundingTypes.add(funding.getFundingType());
                            }
                        }
                    }
                }
            }
        }

        return fundingTypes;
    }

}
