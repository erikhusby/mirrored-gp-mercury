package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.sap.entity.SAPOrder;
import org.broadinstitute.sap.entity.SAPOrderItem;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class SapIntegrationServiceImplDBFreeTest {

    public static final String MOCK_USER_NAME = "Scott Matthews";
    public static final String SINGLE_SOURCE_PO_QUOTE_ID = "GPTest";
    public static final String SINGLE_SOURCE_FUND_RES_QUOTE_ID = "GPFRQT";
    public static final String MULTIPLE_SOURCE_QUOTE_ID = "GPMultipleQtTest";
    public static final String MOCK_CUSTOMER_NUMBER = "0000123456";
    public Quote testSingleSourceQuote;
    public Quote testMultipleLevelQuote;
    public String testUser;
    public Quote testSingleSourceFRQuote;
    public SapIntegrationServiceImpl integrationService;
    public QuoteService mockQuoteService;
    public PriceListCache priceListCache;

    @BeforeMethod
    public void setUp() throws Exception {

        integrationService = new SapIntegrationServiceImpl();

        testUser = "Scott.G.MATThEws@GMail.CoM";

        Funding fundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        fundingDefined.setPurchaseOrderContact(testUser);
        fundingDefined.setPurchaseOrderNumber("PO00Id8923");
        FundingLevel fundingLevel = new FundingLevel("100", Collections.singleton(fundingDefined));

        QuoteFunding quoteFunding = new QuoteFunding(Collections.singleton(fundingLevel));

        testSingleSourceQuote = new Quote(SINGLE_SOURCE_PO_QUOTE_ID, quoteFunding, ApprovalStatus.FUNDED);
        testSingleSourceQuote.setExpired(Boolean.FALSE);

        Funding costObjectFundingDefined = new Funding(Funding.FUNDS_RESERVATION, "to researchStuff", "8823");
        costObjectFundingDefined.setFundsReservationNumber("FR11293");
        FundingLevel coFundingLevel1 = new FundingLevel("100", Collections.singleton(costObjectFundingDefined));
        QuoteFunding costObjectQFunding = new QuoteFunding(Collections.singleton(coFundingLevel1));

        testSingleSourceFRQuote = new Quote(SINGLE_SOURCE_FUND_RES_QUOTE_ID, costObjectQFunding, ApprovalStatus.FUNDED);
        testSingleSourceFRQuote.setExpired(Boolean.FALSE);

        Funding test3POFundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        test3POFundingDefined.setPurchaseOrderContact(testUser);
        test3POFundingDefined.setPurchaseOrderNumber("PO002394ID92");
        FundingLevel test3PurchaseOrderFundingLevel = new FundingLevel("50", Collections.singleton(test3POFundingDefined));

        Funding test3PO2FundingDefined = new Funding(Funding.PURCHASE_ORDER, null, null);
        test3PO2FundingDefined.setPurchaseOrderContact("Second" + testUser);
        test3PO2FundingDefined.setPurchaseOrderNumber("PO3329EEK93");
        FundingLevel test3PO2FundingLevel = new FundingLevel("50", Collections.singleton(test3PO2FundingDefined));

        QuoteFunding test3Funding = new QuoteFunding(
                Arrays.asList(new FundingLevel[]{test3PurchaseOrderFundingLevel,test3PO2FundingLevel}));

        testMultipleLevelQuote = new Quote(MULTIPLE_SOURCE_QUOTE_ID, test3Funding, ApprovalStatus.FUNDED);
        testMultipleLevelQuote.setExpired(Boolean.FALSE);

        mockQuoteService = Mockito.mock(QuoteServiceImpl.class);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testSingleSourceQuote.getAlphanumericId())).thenReturn(testSingleSourceQuote);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testSingleSourceFRQuote.getAlphanumericId())).thenReturn(testSingleSourceFRQuote);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testMultipleLevelQuote.getAlphanumericId())).thenReturn(testMultipleLevelQuote);

        integrationService.setQuoteService(mockQuoteService);

        BSPUserList mockUserList = Mockito.mock(BSPUserList.class);
        Mockito.when(mockUserList.getUserFullName(Mockito.anyLong())).thenReturn(MOCK_USER_NAME);

        integrationService.setBspUserList(mockUserList);

        SapIntegrationClientImpl mockIntegrationClient = Mockito.mock(SapIntegrationClientImpl.class);
        Mockito.when(mockIntegrationClient.findCustomerNumber(Mockito.anyString(), Mockito.any(SapIntegrationClientImpl.SAPCompanyConfiguration.class))).thenReturn(
                MOCK_CUSTOMER_NUMBER);

        integrationService.setWrappedClient(mockIntegrationClient);

        priceListCache = new PriceListCache(mockQuoteService);

        integrationService.setPriceListCache(priceListCache);
    }


    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test(enabled = true)
    public void testInitializeSAPOrder() throws Exception {

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();

        String jiraTicketKey= "PDO-SAP-test";
        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);
        conversionPdo.setQuoteId(testSingleSourceQuote.getAlphanumericId());
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        priceList.add(new QuotePriceItem(conversionPdo.getProduct().getPrimaryPriceItem().getCategory(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "50.50", "test",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform()));
        quoteItems.add(new QuoteItem(testSingleSourceQuote.getAlphanumericId(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "10", "30.50", "test",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform(),
                conversionPdo.getProduct().getPrimaryPriceItem().getCategory()));

        for (ProductOrderAddOn addOn : conversionPdo.getAddOns()) {
            priceList.add(new QuotePriceItem(addOn.getAddOn().getPrimaryPriceItem().getCategory(),
                    addOn.getAddOn().getPrimaryPriceItem().getName(),
                    addOn.getAddOn().getPrimaryPriceItem().getName(), "40.50", "test",
                    addOn.getAddOn().getPrimaryPriceItem().getPlatform()));

            quoteItems.add(new QuoteItem(testSingleSourceQuote.getAlphanumericId(),
                    addOn.getAddOn().getPrimaryPriceItem().getName(),
                    addOn.getAddOn().getPrimaryPriceItem().getName(), "10", "20.50", "test",
                    addOn.getAddOn().getPrimaryPriceItem().getPlatform(),
                    addOn.getAddOn().getPrimaryPriceItem().getCategory()));
        }

        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        testSingleSourceQuote.setQuoteItems(quoteItems);
        Map<String, SampleData> mockReturnValue = new HashMap<>();

        for (ProductOrderSample currentSample:conversionPdo.getSamples()) {
            Map<BSPSampleSearchColumn, String> dataMap = new HashMap<>();

            dataMap.put(BSPSampleSearchColumn.SAMPLE_ID, currentSample.getName());
            dataMap.put(BSPSampleSearchColumn.RECEIPT_DATE, "08/16/2016");

            SampleData returnValue =  new BspSampleData(dataMap);
            mockReturnValue.put(currentSample.getName(), returnValue);
        }
        SampleDataFetcher dataFetcher = Mockito.mock(SampleDataFetcher.class);
        Mockito.when(dataFetcher.fetchSampleDataForSamples(Mockito.anyCollectionOf(ProductOrderSample.class),
                Mockito.<BSPSampleSearchColumn>anyVararg())).thenReturn(mockReturnValue);

        SAPOrder convertedOrder = integrationService.initializeSAPOrder(conversionPdo);

        assertThat(convertedOrder.getCompanyCode(), equalTo(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD));
        assertThat(convertedOrder.getSapCustomerNumber(), equalTo(MOCK_CUSTOMER_NUMBER));
        assertThat(convertedOrder.getQuoteNumber(), equalTo(testSingleSourceQuote.getAlphanumericId()));
        assertThat(convertedOrder.getExternalOrderNumber(), equalTo(conversionPdo.getBusinessKey()));
        assertThat(convertedOrder.getSapOrderNumber(), is(nullValue()));
        assertThat(convertedOrder.getCreator(), equalTo(MOCK_USER_NAME));
        assertThat(convertedOrder.getResearchProjectNumber(), equalTo(conversionPdo.getResearchProject().getBusinessKey()));
        assertThat(convertedOrder.getOrderItems().iterator().next().getSampleCount(),equalTo(10));

        assertThat(convertedOrder.getOrderItems().size(), equalTo(conversionPdo.getAddOns().size()+1));

        for(SAPOrderItem item:convertedOrder.getOrderItems()) {
            assertThat(item.getSampleCount(), equalTo(conversionPdo.getSamples().size()));
            if(item.getProductIdentifier().equals(conversionPdo.getProduct().getPartNumber())) {
                assertThat(item.getProductPrice(), equalTo("30.50"));
            } else {
                assertThat(item.getProductPrice(), equalTo("20.50"));
            }
        }

        conversionPdo.addSapOrderDetail(new SapOrderDetail("testsap001", conversionPdo.getTotalNonAbandonedCount(
                ProductOrder.CountAggregation.SHARE_SAP_ORDER_AND_BILL_READY),
                conversionPdo.getQuoteId(), integrationService.determineCompanyCode(conversionPdo).getCompanyCode(), "", ""));

        ProductOrder childOrder = ProductOrder.cloneProductOrder(conversionPdo, false);
        childOrder.setJiraTicketKey("PDO-CLONE1");

        childOrder.addSapOrderDetail(new SapOrderDetail("testchildsap001", childOrder.getTotalNonAbandonedCount(
                ProductOrder.CountAggregation.SHARE_SAP_ORDER_AND_BILL_READY),
                childOrder.getQuoteId(), integrationService.determineCompanyCode(childOrder).getCompanyCode(), "", ""));

        childOrder.setSamples(ProductOrderSampleTestFactory
                .createDBFreeSampleList(MercurySample.MetadataSource.BSP,
                        "SM-2ABDD", "SM-2AB1B", "SM-2ACJC", "SM-2ACGC", "SM-Extra1"));

        SAPOrder convertedOrder2 = integrationService.initializeSAPOrder(conversionPdo);
        for(SAPOrderItem item:convertedOrder2.getOrderItems()) {
            assertThat(item.getSampleCount(), equalTo(conversionPdo.getSamples().size()));
        }


        SAPOrder convertedChildOrder = integrationService.initializeSAPOrder(childOrder);
        for(SAPOrderItem item:convertedChildOrder.getOrderItems()) {
            assertThat(item.getSampleCount(), equalTo(childOrder.getSamples().size()));
        }


        ProductOrder childOrder2 = ProductOrder.cloneProductOrder(conversionPdo, true);
        childOrder2.setJiraTicketKey("PDO-CLONE2");
        childOrder2.setSamples(ProductOrderSampleTestFactory
                .createDBFreeSampleList(MercurySample.MetadataSource.BSP,
                        "SM-2BBDD", "SM-2BB1B", "SM-2BCJC", "SM-2BCGC"));


        SAPOrder convertedOrder3 = integrationService.initializeSAPOrder(conversionPdo);
        for(SAPOrderItem item:convertedOrder3.getOrderItems()) {
            assertThat(item.getSampleCount(),
                    equalTo(conversionPdo.getSamples().size()));
        }

        childOrder2.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        childOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        convertedOrder3 = integrationService.initializeSAPOrder(conversionPdo);
        for(SAPOrderItem item:convertedOrder3.getOrderItems()) {
            assertThat(item.getSampleCount(),
                    equalTo(conversionPdo.getSamples().size() + childOrder2.getSamples().size()));
        }


        SAPOrder convertedChildOrder2 = integrationService.initializeSAPOrder(childOrder2);
        for(SAPOrderItem item:convertedChildOrder2.getOrderItems()) {
            assertThat(item.getSampleCount(), equalTo(conversionPdo.getSamples().size() + childOrder2.getSamples().size()));
        }

    }

    @Test
    public void testGetOrderItem() throws Exception {

    }

    @Test
    public void testInitializeSapMaterialObject() throws Exception {

    }
}