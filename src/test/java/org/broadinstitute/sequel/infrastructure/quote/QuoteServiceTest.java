package org.broadinstitute.sequel.infrastructure.quote;

import com.sun.jersey.api.client.ClientResponse;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

public class QuoteServiceTest {

    private Quote quote;

    private  PriceItem priceItem;

    @BeforeClass
    private void setupLargeQuoteAndPriceItem() {
        quote = new Quote("DNA4JC",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NHGRI"))));
        priceItem = new PriceItem("Illumina Sequencing","1","Illumina Custom Hybrid Selection Library (93 sample batch size)","15","bannanas","DNA Sequencing");
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
}
