package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.sap.entity.SAPOrder;
import org.broadinstitute.sap.entity.SAPOrderItem;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;

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

    @BeforeMethod
    public void setUp() throws Exception {

        integrationService = new SapIntegrationServiceImpl();

        testUser = "Scott.G.MATThEws@GMail.CoM";

        Funding fundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        fundingDefined.setPurchaseOrderContact(testUser);
        fundingDefined.setPurchaseOrderNumber("PO00Id8923");
        FundingLevel fundingLevel = new FundingLevel("100",fundingDefined);

        QuoteFunding quoteFunding = new QuoteFunding(Collections.singleton(fundingLevel));

        testSingleSourceQuote = new Quote(SINGLE_SOURCE_PO_QUOTE_ID, quoteFunding, ApprovalStatus.FUNDED);

        Funding costObjectFundingDefined = new Funding(Funding.FUNDS_RESERVATION, "to researchStuff", "8823");
        costObjectFundingDefined.setFundsReservationNumber("FR11293");
        FundingLevel coFundingLevel1 = new FundingLevel("100", costObjectFundingDefined);
        QuoteFunding costObjectQFunding = new QuoteFunding(Collections.singleton(coFundingLevel1));

        testSingleSourceFRQuote = new Quote(SINGLE_SOURCE_FUND_RES_QUOTE_ID, costObjectQFunding, ApprovalStatus.FUNDED);

        Funding test3POFundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        test3POFundingDefined.setPurchaseOrderContact(testUser);
        test3POFundingDefined.setPurchaseOrderNumber("PO002394ID92");
        FundingLevel test3PurchaseOrderFundingLevel = new FundingLevel("50",test3POFundingDefined);

        Funding test3PO2FundingDefined = new Funding(Funding.PURCHASE_ORDER, null, null);
        test3PO2FundingDefined.setPurchaseOrderContact("Second" + testUser);
        test3PO2FundingDefined.setPurchaseOrderNumber("PO3329EEK93");
        FundingLevel test3PO2FundingLevel = new FundingLevel("50", test3PO2FundingDefined);

        QuoteFunding test3Funding = new QuoteFunding(
                Arrays.asList(new FundingLevel[]{test3PurchaseOrderFundingLevel,test3PO2FundingLevel}));

        testMultipleLevelQuote = new Quote(MULTIPLE_SOURCE_QUOTE_ID, test3Funding, ApprovalStatus.FUNDED);

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

        PriceListCache mockPriceListCache = Mockito.mock(PriceListCache.class);
        QuotePriceItem testQuotePriceItem = new QuotePriceItem();
        testQuotePriceItem.setPrice("30.50");
        Mockito.when(mockPriceListCache.findByKeyFields(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).thenReturn(testQuotePriceItem);

        integrationService.setPriceListCache(mockPriceListCache);
    }


    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test
    public void testInitializeSAPOrder() throws Exception {
        ProductOrder conversionPdo = ProductOrderTestFactory.buildExExProductOrder(10);
        conversionPdo.setQuoteId(testSingleSourceQuote.getAlphanumericId());

        SAPOrder convertedOrder = integrationService.initializeSAPOrder(conversionPdo);

        assertThat(convertedOrder.getCompanyCode(), equalTo(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD));
        assertThat(convertedOrder.getSapCustomerNumber(), equalTo(MOCK_CUSTOMER_NUMBER));
        assertThat(convertedOrder.getQuoteNumber(), equalTo(testSingleSourceQuote.getAlphanumericId()));
        assertThat(convertedOrder.getExternalOrderNumber(), equalTo(conversionPdo.getBusinessKey()));
        assertThat(convertedOrder.getSapOrderNumber(), is(nullValue()));
        assertThat(convertedOrder.getCreator(), equalTo(MOCK_USER_NAME));
        assertThat(convertedOrder.getResearchProjectNumber(), equalTo(conversionPdo.getResearchProject().getBusinessKey()));

        assertThat(convertedOrder.getOrderItems().size(), equalTo(conversionPdo.getAddOns().size()+1));

        for(SAPOrderItem item:convertedOrder.getOrderItems()) {
            assertThat(item.getSampleCount(), equalTo(conversionPdo.getSamples().size()));
        }
    }

    @Test
    public void testGetOrderItem() throws Exception {

    }

    @Test
    public void testInitializeSapMaterialObject() throws Exception {

    }
}