package org.broadinstitute.sequel.infrastructure.quote;



import com.sun.jersey.api.client.ClientResponse;
import org.broadinstitute.sequel.infrastructure.quote.*;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;
import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

public class QuoteServiceTest {

    private Quote quote;

    private  PriceItem priceItem;

    @BeforeClass
    private void setupLargeQuoteAndPriceItem() {
        quote = new Quote("DNA4JD",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NHGRI"))));
        priceItem = new PriceItem("Illumina Sequencing","1","Illumina HiSeq Run 44 Base","15","bannan","DNA Sequencing");
    }


    /**
     * If this test fails because the quote has been used up, 
     * visit the website {@link QAQuoteConnectionParams#QA_HOST},
     * login with the credentials, run the {@link QuoteConnectionParameters#GET_ALL_SEQUENCING_QUOTES_URL},
     * find a quote that doesn't expire for a while, and change {@link #quote} in
     * @{link #setupLargeQuoteAndPriceItem}.
     * @throws Exception
     */
    @Test(groups = {EXTERNAL_INTEGRATION})
    public void test_get_all_price_items() throws Exception {
        QuoteServiceImpl service = new QuoteServiceImpl(new QAQuoteConnectionParams());
        PriceList priceList = service.getAllPriceItems();
        Assert.assertFalse(priceList.getPriceList().isEmpty());

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

    @Test(groups = DATABASE_FREE)
    public void test_bad_response_code() {
        QuoteServiceImpl service = new QuoteServiceImpl(null);
        ClientResponse mockResponse = EasyMock.createMock(ClientResponse.class);
        
        EasyMock.expect(mockResponse.getClientResponseStatus()).andReturn(ClientResponse.Status.BAD_REQUEST).atLeastOnce();
        EasyMock.replay(mockResponse);
        try {
            service.registerNewWork(mockResponse,quote,priceItem,0.0001,"http://www.SequeLTesting","paramName","paramValue");
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
            service.registerNewWork(mockResponse,quote,priceItem,0.0001,"http://www.SequeLTesting","paramName","paramValue");
            Assert.fail("Should have thrown an exception when no client response was returned");
        }
        catch(Exception e) {}
        EasyMock.verify(mockResponse);

        EasyMock.reset(mockResponse);
        EasyMock.expect(mockResponse.getClientResponseStatus()).andReturn(ClientResponse.Status.OK).atLeastOnce();
        EasyMock.expect(mockResponse.getEntity(String.class)).andReturn(null).atLeastOnce();

        EasyMock.replay(mockResponse);
        try {
            service.registerNewWork(mockResponse,quote,priceItem,0.0001,"http://www.SequeLTesting","paramName","paramValue");
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
            service.registerNewWork(mockResponse,quote,priceItem,0.0001,"http://www.SequeLTesting","paramName","paramValue");
            Assert.fail("Should have thrown an exception when string returned was null");
        }
        catch(Exception e) {}
        EasyMock.verify(mockResponse);

        EasyMock.reset(mockResponse);
        EasyMock.expect(mockResponse.getClientResponseStatus()).andReturn(ClientResponse.Status.OK).atLeastOnce();
        EasyMock.expect(mockResponse.getEntity(String.class)).andReturn(QuoteServiceImpl.WORK_ITEM_ID + QuoteServiceImpl.WORK_ITEM_ID + "Oh\tCrap").atLeastOnce();

        EasyMock.replay(mockResponse);
        try {
            service.registerNewWork(mockResponse,quote,priceItem,0.0001,"http://www.SequeLTesting","paramName","paramValue");
            Assert.fail("Should have thrown an exception when string returned was null");
        }
        catch(Exception e) {}
        EasyMock.verify(mockResponse);

        EasyMock.reset(mockResponse);
        EasyMock.expect(mockResponse.getClientResponseStatus()).andReturn(ClientResponse.Status.OK).atLeastOnce();
        EasyMock.expect(mockResponse.getEntity(String.class)).andReturn(QuoteServiceImpl.WORK_ITEM_ID + " ").atLeastOnce();

        EasyMock.replay(mockResponse);
        try {
            service.registerNewWork(mockResponse,quote,priceItem,0.0001,"http://www.SequeLTesting","paramName","paramValue");
            Assert.fail("Should have thrown an exception when string returned was null");
        }
        catch(Exception e) {}
        EasyMock.verify(mockResponse);



    }


    @Test(groups = {EXTERNAL_INTEGRATION})
    public void test_get_a_quote() throws Exception {
        QuoteServiceImpl service = new QuoteServiceImpl(new QAQuoteConnectionParams());
        Quote quote = service.getQuoteFromQuoteServer("DNA3CD");
        Assert.assertNotNull(quote);
        Assert.assertEquals("Regev ChIP Sequencing 8-1-2011", quote.getName());
        Assert.assertEquals("6820110", quote.getQuoteFunding().getFundingLevel().getFunding().getCostObject());
        Assert.assertEquals("ZEBRAFISH_NIH_REGEV",quote.getQuoteFunding().getFundingLevel().getFunding().getGrantDescription());
        Assert.assertEquals(Funding.FUNDS_RESERVATION,quote.getQuoteFunding().getFundingLevel().getFunding().getFundingType());
        Assert.assertEquals("DNA3CD",quote.getAlphanumericId());

        quote = service.getQuoteFromQuoteServer("DNA3A9");
        Assert.assertEquals("HARVARD UNIVERSITY",quote.getQuoteFunding().getFundingLevel().getFunding().getInstitute());
        Assert.assertEquals(Funding.PURCHASE_ORDER,quote.getQuoteFunding().getFundingLevel().getFunding().getFundingType());
        Assert.assertEquals("DNA3A9",quote.getAlphanumericId());

    }

    @Test(groups = {EXTERNAL_INTEGRATION})
    public void test_get_all_quotes_for_sequencing() throws Exception {

        boolean caught = false;

        QuoteServiceImpl service = new QuoteServiceImpl(new QAQuoteConnectionParams());
        Quotes quotes = service.getAllSequencingPlatformQuotes();
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

}
