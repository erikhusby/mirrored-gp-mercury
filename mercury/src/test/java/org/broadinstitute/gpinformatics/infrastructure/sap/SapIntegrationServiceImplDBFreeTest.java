package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOnPriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderPriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
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
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.sap.entity.Condition;
import org.broadinstitute.sap.entity.ConditionValue;
import org.broadinstitute.sap.entity.SAPMaterial;
import org.broadinstitute.sap.entity.SAPOrder;
import org.broadinstitute.sap.entity.SAPOrderItem;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private Quote testSingleSourceQuote;
    private Quote testMultipleLevelQuote;
    private String testUser;
    private Quote testSingleSourceFRQuote;
    private SapIntegrationServiceImpl integrationService;
    private SAPProductPriceCache productPriceCache;
    private QuoteService mockQuoteService;
    private PriceListCache priceListCache;

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

        productPriceCache = new SAPProductPriceCache(integrationService);
        integrationService.setProductPriceCache(productPriceCache);
        final SAPAccessControlEjb mockAccessController = Mockito.mock(SAPAccessControlEjb.class);
        Mockito.when(mockAccessController.getCurrentControlDefinitions()).thenReturn(new SAPAccessControl());
        productPriceCache.setAccessControlEjb(mockAccessController);
    }


    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test(enabled = true)
    public void testInitializeSAPOrderPre1pt5() throws Exception {

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();

        String jiraTicketKey= "PDO-SAP-test";
        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);
        conversionPdo.setPriorToSAP1_5(true);
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

        SAPOrder convertedOrder = integrationService.initializeSAPOrder(conversionPdo, true, false);

        assertThat(convertedOrder.getCompanyCode(), equalTo(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD));
        assertThat(convertedOrder.getSapCustomerNumber(), equalTo(MOCK_CUSTOMER_NUMBER));
        assertThat(convertedOrder.getQuoteNumber(), equalTo(testSingleSourceQuote.getAlphanumericId()));
        assertThat(convertedOrder.getExternalOrderNumber(), equalTo(conversionPdo.getBusinessKey()));
        assertThat(convertedOrder.getSapOrderNumber(), is(nullValue()));
        assertThat(convertedOrder.getCreator(), equalTo(MOCK_USER_NAME));
        assertThat(convertedOrder.getResearchProjectNumber(), equalTo(conversionPdo.getResearchProject().getBusinessKey()));

        assertThat(convertedOrder.getOrderItems().size(), equalTo(conversionPdo.getAddOns().size()+1));

        for(SAPOrderItem item:convertedOrder.getOrderItems()) {
            assertThat(item.getItemQuantity().doubleValue(), equalTo(
                    (new BigDecimal(conversionPdo.getSamples().size())).doubleValue()));
            final ConditionValue conditionValue = item.getConditions().iterator().next();
            assertThat(conditionValue.getCondition(), is(Condition.MATERIAL_PRICE));

            if(item.getProductIdentifier().equals(conversionPdo.getProduct().getPartNumber())) {
                assertThat(conditionValue.getValue(), equalTo(new BigDecimal("30.50")));
            } else {
                assertThat(conditionValue.getValue(), equalTo(new BigDecimal("20.50")));
            }
        }

        conversionPdo.addSapOrderDetail(new SapOrderDetail("testsap001", conversionPdo.getTotalNonAbandonedCount(
                ProductOrder.CountAggregation.SHARE_SAP_ORDER_AND_BILL_READY),
                conversionPdo.getQuoteId(), conversionPdo.getSapCompanyConfigurationForProductOrder().getCompanyCode(), "", ""));

        SAPOrder convertedOrder2 = integrationService.initializeSAPOrder(conversionPdo, true, false);
        for(SAPOrderItem item:convertedOrder2.getOrderItems()) {
            assertThat(item.getItemQuantity().doubleValue(), equalTo(
                    (new BigDecimal(conversionPdo.getSamples().size())).doubleValue()));
            for (ConditionValue conditionValue : item.getConditions()) {
                assertThat(conditionValue.getCondition(), is(Condition.MATERIAL_PRICE));
            }
        }

        SAPOrder convertedOrder3 = integrationService.initializeSAPOrder(conversionPdo, true, false);
        for(SAPOrderItem item:convertedOrder3.getOrderItems()) {
            assertThat(item.getItemQuantity().doubleValue(), equalTo(
                    (new BigDecimal(conversionPdo.getSamples().size())).doubleValue()));
            for (ConditionValue conditionValue : item.getConditions()) {
                assertThat(conditionValue.getCondition(), is(Condition.MATERIAL_PRICE));
            }
        }

        convertedOrder3 = integrationService.initializeSAPOrder(conversionPdo, true, false);
        for(SAPOrderItem item:convertedOrder3.getOrderItems()) {
            assertThat(item.getItemQuantity().doubleValue(), equalTo((new BigDecimal(conversionPdo.getSamples().size())).doubleValue()));

            for (ConditionValue conditionValue : item.getConditions()) {
                assertThat(conditionValue.getCondition(), is(Condition.MATERIAL_PRICE));
            }
        }

    }

    @Test(enabled = true)
    public void testInitializeSAPOrderPost1pt5() throws Exception {

        final String primaryMaterialBasePrice = "50.50";
        final String addonMaterialPrice = "40.50";

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        String jiraTicketKey= "PDO-SAP-test";
        Set<SAPMaterial> materials = new HashSet<>();

        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(integrationService.findProductsInSap()).thenReturn(materials);

        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);
        conversionPdo.setQuoteId(testSingleSourceQuote.getAlphanumericId());
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        conversionPdo.addSapOrderDetail(new SapOrderDetail("testSAPOrder", 10, testSingleSourceQuote.getAlphanumericId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode(), "", ""));

        final Product primaryProduct = conversionPdo.getProduct();
        addTestProductMaterialPrice(primaryMaterialBasePrice, priceList, quoteItems, materials, primaryProduct,
                testSingleSourceQuote.getAlphanumericId());

        for (ProductOrderAddOn addOn : conversionPdo.getAddOns()) {
            addTestProductMaterialPrice(addonMaterialPrice, priceList, quoteItems, materials, addOn.getAddOn(),
                    testSingleSourceQuote.getAlphanumericId());
        }
        testSingleSourceQuote.setQuoteItems(quoteItems);


        final String customProductName = "Test custom material";
        final String customAddonProductName = "Test custom addon material";
        final ProductOrderPriceAdjustment customPriceAdjustment =
                new ProductOrderPriceAdjustment(new BigDecimal(80), null, customProductName);

        customPriceAdjustment.setListPrice(new BigDecimal(priceList.findByKeyFields(primaryProduct.getPrimaryPriceItem()).getPrice()));
        conversionPdo.setCustomPriceAdjustment(customPriceAdjustment);

        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            final ProductOrderAddOnPriceAdjustment customAdjustment = new ProductOrderAddOnPriceAdjustment(new BigDecimal(80),1, customAddonProductName);
            customAdjustment.setListPrice(new BigDecimal(priceList.findByKeyFields(productOrderAddOn.getAddOn().getPrimaryPriceItem()).getPrice()));
            productOrderAddOn.setCustomPriceAdjustment(customAdjustment);
        }

        SAPOrder convertedOrder = integrationService.initializeSAPOrder(conversionPdo, true, false);

        assertThat(convertedOrder.getCompanyCode(), equalTo(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD));
        assertThat(convertedOrder.getSapCustomerNumber(), equalTo(MOCK_CUSTOMER_NUMBER));
        assertThat(convertedOrder.getQuoteNumber(), equalTo(testSingleSourceQuote.getAlphanumericId()));
        assertThat(convertedOrder.getExternalOrderNumber(), equalTo(conversionPdo.getBusinessKey()));
        assertThat(convertedOrder.getSapOrderNumber(), is(nullValue()));
        assertThat(convertedOrder.getCreator(), equalTo(MOCK_USER_NAME));
        assertThat(convertedOrder.getResearchProjectNumber(), equalTo(conversionPdo.getResearchProject().getBusinessKey()));

        // HashSet order not deterministic. Tested at line 393 assertThat(convertedOrder.getOrderItems().size(), equalTo(conversionPdo.getAddOns().size()+1));

        for(SAPOrderItem item:convertedOrder.getOrderItems()) {

            if(item.getProductIdentifier().equals(primaryProduct.getPartNumber())) {
                assertThat(item.getItemQuantity().doubleValue(), equalTo(
                        (new BigDecimal(conversionPdo.getSamples().size())).doubleValue()));
                assertThat(item.getProductAlias(), equalTo(customProductName));

                final ConditionValue foundCondition = item.getConditions().iterator().next();
                assertThat(foundCondition.getValue(), equalTo(new BigDecimal("29.50")));
                assertThat(foundCondition.getCondition(), equalTo(Condition.MARK_UP_LINE_ITEM));
            }
            else {
                assertThat(item.getItemQuantity().doubleValue(), equalTo((new BigDecimal(1.0d)).doubleValue()));
                final ConditionValue foundCondition = item.getConditions().iterator().next();
                assertThat(foundCondition.getValue(), equalTo(new BigDecimal("39.50")));
                assertThat(foundCondition.getCondition(), equalTo(Condition.MARK_UP_LINE_ITEM));
            }
        }

        SAPOrder closedConvertedOrder = integrationService.initializeSAPOrder(conversionPdo, true, true);

        assertThat(closedConvertedOrder.getCompanyCode(), equalTo(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD));
        assertThat(closedConvertedOrder.getSapCustomerNumber(), equalTo(MOCK_CUSTOMER_NUMBER));
        assertThat(closedConvertedOrder.getQuoteNumber(), equalTo(testSingleSourceQuote.getAlphanumericId()));
        assertThat(closedConvertedOrder.getExternalOrderNumber(), equalTo(conversionPdo.getBusinessKey()));
        assertThat(closedConvertedOrder.getSapOrderNumber(), is(nullValue()));
        assertThat(closedConvertedOrder.getCreator(), equalTo(MOCK_USER_NAME));
        assertThat(closedConvertedOrder.getResearchProjectNumber(), equalTo(conversionPdo.getResearchProject().getBusinessKey()));

        assertThat(closedConvertedOrder.getOrderItems().size(), equalTo(conversionPdo.getAddOns().size()+1));

        for(SAPOrderItem item:closedConvertedOrder.getOrderItems()) {

            if(item.getProductIdentifier().equals(primaryProduct.getPartNumber())) {
                assertThat(item.getItemQuantity().doubleValue(), equalTo((new BigDecimal(0)).doubleValue()));
                assertThat(item.getProductAlias(), equalTo(customProductName));

                final ConditionValue foundCondition = item.getConditions().iterator().next();
                assertThat(foundCondition.getValue(), equalTo(new BigDecimal("29.50")));
                assertThat(foundCondition.getCondition(), equalTo(Condition.MARK_UP_LINE_ITEM));
            }
            else {
                assertThat(item.getItemQuantity().doubleValue(), equalTo((new BigDecimal(0)).doubleValue()));
                final ConditionValue foundCondition = item.getConditions().iterator().next();
                assertThat(foundCondition.getValue(), equalTo(new BigDecimal("39.50")));
                assertThat(foundCondition.getCondition(), equalTo(Condition.MARK_UP_LINE_ITEM));
            }
        }


        conversionPdo.addSapOrderDetail(new SapOrderDetail("testsap001", conversionPdo.getTotalNonAbandonedCount(
                ProductOrder.CountAggregation.SHARE_SAP_ORDER_AND_BILL_READY),
                conversionPdo.getQuoteId(), conversionPdo.getSapCompanyConfigurationForProductOrder().getCompanyCode(), "", ""));

        SAPOrder convertedOrder2 = integrationService.initializeSAPOrder(conversionPdo, true, false);
        for(SAPOrderItem item:convertedOrder2.getOrderItems()) {
            if(item.getProductIdentifier().equals(primaryProduct.getPartNumber())) {
                assertThat(item.getItemQuantity().doubleValue(), equalTo(
                        (new BigDecimal(conversionPdo.getSamples().size())).doubleValue()));
            }
            else {
                assertThat(item.getItemQuantity().doubleValue(), equalTo((new BigDecimal(1)).doubleValue()));
            }
        }


        SAPOrder convertedOrder3 = integrationService.initializeSAPOrder(conversionPdo, true, false);
        for(SAPOrderItem item:convertedOrder3.getOrderItems()) {
            if(item.getProductIdentifier().equals(primaryProduct.getPartNumber())) {
                assertThat(item.getItemQuantity().doubleValue(), equalTo(
                        (new BigDecimal(conversionPdo.getSamples().size())).doubleValue()));
            }
            else {
                assertThat(item.getItemQuantity().doubleValue(), equalTo((new BigDecimal(1)).doubleValue()));
            }
        }

        convertedOrder3 = integrationService.initializeSAPOrder(conversionPdo, true, false);
        for(SAPOrderItem item:convertedOrder3.getOrderItems()) {
            if(item.getProductIdentifier().equals(conversionPdo.getProduct().getPartNumber())) {
                assertThat(item.getItemQuantity().doubleValue(), equalTo(new BigDecimal(conversionPdo.getSamples().size()).doubleValue()));
            }
            else {
                assertThat((item.getItemQuantity()).doubleValue(), equalTo((new BigDecimal(1)).doubleValue()));
            }
        }


        String newProductAlias = "testName";
        final ProductOrderPriceAdjustment customPriceAdjustment1 = new ProductOrderPriceAdjustment(null, null,
                newProductAlias);
        conversionPdo.setCustomPriceAdjustment(customPriceAdjustment1);
        conversionPdo.updateAddOnProducts(Collections.<Product>emptyList());

        SAPOrder sapOrder = integrationService.initializeSAPOrder(conversionPdo, false, false);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getProductAlias(), is(equalTo(newProductAlias)));
        }

        ProductOrderPriceAdjustment customAdjustment2 = new ProductOrderPriceAdjustment(new BigDecimal(65), null, null);
        conversionPdo.setCustomPriceAdjustment(customAdjustment2);
        sapOrder = integrationService.initializeSAPOrder(conversionPdo, false, false);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions().size(), is(equalTo(1)));
            for (ConditionValue conditionValue : sapOrderItem.getConditions()) {
                assertThat(conditionValue.getCondition(), is(equalTo(Condition.MARK_UP_LINE_ITEM)));
                assertThat(conditionValue.getValue(), is(equalTo(new BigDecimal("14.50"))));
            }
            assertThat(sapOrderItem.getProductAlias(), is(nullValue()));
        }

        ProductOrderPriceAdjustment customAdjustment3 = new ProductOrderPriceAdjustment(null, 99, null);
        conversionPdo.setCustomPriceAdjustment(customAdjustment3);


        sapOrder = integrationService.initializeSAPOrder(conversionPdo, false, false);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(99)).doubleValue())));
            assertThat(sapOrderItem.getProductAlias(), is(nullValue()));
        }


        final SapOrderDetail sapOrderDetail = conversionPdo.latestSapOrderDetail();
        final LedgerEntry ledgerEntry =
                new LedgerEntry(conversionPdo.getSamples().get(0), conversionPdo.getProduct().getPrimaryPriceItem(),
                        new Date(), 3d);
        sapOrderDetail.addLedgerEntry(ledgerEntry);

        sapOrder = integrationService.initializeSAPOrder(conversionPdo, false, true);
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
                assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(0)).doubleValue())));
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getProductAlias(), is(nullValue()));
        }

        sapOrder = integrationService.initializeSAPOrder(conversionPdo, false, false);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(99)).doubleValue())));
            assertThat(sapOrderItem.getProductAlias(), is(nullValue()));
        }

        ledgerEntry.setBillingMessage(BillingSession.SUCCESS);
        ledgerEntry.setSapDeliveryDocumentId("TestDeliveryDocument");

        sapOrder = integrationService.initializeSAPOrder(conversionPdo, false, true);
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            if(sapOrderItem.getProductIdentifier().equals(conversionPdo.getProduct().getPartNumber())) {
                assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(3)).doubleValue())));
            } else {
            assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(0)).doubleValue())));
            }
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getProductAlias(), is(nullValue()));
        }

        sapOrder = integrationService.initializeSAPOrder(conversionPdo, false, false);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(99)).doubleValue())));
            assertThat(sapOrderItem.getProductAlias(), is(nullValue()));
        }

        double initialQuantity = 4d;
        Map<String, Double> productToQuantityMapping = new HashMap<>();
        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            productToQuantityMapping.put(productOrderAddOn.getAddOn().getPartNumber(), initialQuantity++);
            LedgerEntry addonLedgerEntry = new LedgerEntry(conversionPdo.getSamples().get(0),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem(), new Date(),
                    productToQuantityMapping.get(productOrderAddOn.getAddOn().getPartNumber()));
            addonLedgerEntry.setBillingMessage(BillingSession.SUCCESS);
            addonLedgerEntry.setSapDeliveryDocumentId(productOrderAddOn.getAddOn().getPartNumber() + "delivery");
            sapOrderDetail.addLedgerEntry(addonLedgerEntry);
        }
        sapOrder = integrationService.initializeSAPOrder(conversionPdo, false, true);
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            if(sapOrderItem.getProductIdentifier().equals(conversionPdo.getProduct().getPartNumber())) {
                assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(3)).doubleValue())));
            } else {
                assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo(
                        (new BigDecimal(productToQuantityMapping.get(sapOrderItem.getProductIdentifier()))).doubleValue())));
            }
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getProductAlias(), is(nullValue()));
        }

        sapOrder = integrationService.initializeSAPOrder(conversionPdo, false, false);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(99)).doubleValue())));
            assertThat(sapOrderItem.getProductAlias(), is(nullValue()));
        }
        Mockito.verify(mockQuoteService, Mockito.times(1)).getQuoteByAlphaId(Mockito.anyString());

    }

    public void testTetSampleCountFreshOrderNoOverrides() throws Exception {
        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> materials = new HashSet<>();

        ProductOrder countTestPDO = ProductOrderTestFactory.createDummyProductOrder(10, "PDO-smpcnt");
        countTestPDO.setQuoteId(testSingleSourceQuote.getAlphanumericId());
        countTestPDO.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        countTestPDO.addSapOrderDetail(new SapOrderDetail("testSAPOrder", 10, testSingleSourceQuote.getAlphanumericId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode(), "", ""));

        final Product primaryProduct = countTestPDO.getProduct();
        addTestProductMaterialPrice("50.00", priceList, quoteItems, materials, primaryProduct,
                testSingleSourceQuote.getAlphanumericId());

        for (ProductOrderAddOn addOn : countTestPDO.getAddOns()) {
            addTestProductMaterialPrice("30.00", priceList, quoteItems, materials, addOn.getAddOn(),
                    testSingleSourceQuote.getAlphanumericId());
        }
        testSingleSourceQuote.setQuoteItems(quoteItems);

        double closingCount = 0d;


        while (closingCount <= countTestPDO.getSamples().size()) {
            double primarySampleCount =
                    SapIntegrationServiceImpl.getSampleCount(countTestPDO, countTestPDO.getProduct(), 0, false, false,
                            false).doubleValue();
            double primaryClosingCount =
                    SapIntegrationServiceImpl.getSampleCount(countTestPDO, countTestPDO.getProduct(), 0, false, true,
                            false).doubleValue();
            double primaryOrderValueQueryCount =
                    SapIntegrationServiceImpl.getSampleCount(countTestPDO, countTestPDO.getProduct(), 0, false, true,
                            true).doubleValue();
            assertThat(primarySampleCount, is(equalTo((double) countTestPDO.getSamples().size())));
            assertThat(primaryClosingCount, is(equalTo(closingCount)));
            assertThat(primaryOrderValueQueryCount, is(equalTo((double) countTestPDO.getSamples().size() - closingCount)));


            for (ProductOrderAddOn addOn : countTestPDO.getAddOns()) {
                final double addonSampleCount =
                        SapIntegrationServiceImpl.getSampleCount(countTestPDO, addOn.getAddOn(), 0, false, false,
                                false).doubleValue();
                final double addonClosingCount =
                        SapIntegrationServiceImpl.getSampleCount(countTestPDO, addOn.getAddOn(), 0, false, true,
                                false).doubleValue();
                final double addOnOrderValueQueryCount =
                        SapIntegrationServiceImpl.getSampleCount(countTestPDO, addOn.getAddOn(), 0, false, true,
                                true).doubleValue();
                assertThat(addonSampleCount, is(equalTo((double) countTestPDO.getSamples().size())));
                assertThat(addonClosingCount, is(equalTo(closingCount)));
                assertThat(addOnOrderValueQueryCount, is(equalTo((double) countTestPDO.getSamples().size() - closingCount)));
            }
            addLedgerItems(countTestPDO, 1);

            closingCount++;
        }

    }

    private void addLedgerItems(ProductOrder order, int ledgerCount) {
        for (ProductOrderSample productOrderSample : order.getSamples()) {
            if(!productOrderSample.isCompletelyBilled()) {
                productOrderSample.addLedgerItem(new Date(), order.getProduct().getPrimaryPriceItem(), ledgerCount * 1d);
                for (ProductOrderAddOn productOrderAddOn : order.getAddOns()) {
                    productOrderSample.addLedgerItem(new Date(), productOrderAddOn.getAddOn().getPrimaryPriceItem(), ledgerCount * 1d);
                }

                BillingSession newSession = new BillingSession(1L, productOrderSample.getLedgerItems());
                for (LedgerEntry ledgerEntry : productOrderSample.getLedgerItems()) {
                    ledgerEntry.setPriceItemType(LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM);
                    newSession.setBilledDate(new Date());
                    ledgerEntry.setBillingMessage(BillingSession.SUCCESS);
                    order.latestSapOrderDetail().addLedgerEntry(ledgerEntry);
                }
                break;
            }
        }
    }

    @Test(enabled = false)
    public static void addTestProductMaterialPrice(String primaryMaterialBasePrice, PriceList priceList,
                                            Collection<QuoteItem> quoteItems, Set<SAPMaterial> materials,
                                            Product primaryProduct, String quoteId) {
        SAPMaterial primaryMaterial = new SAPMaterial(primaryProduct.getPartNumber(),
                primaryMaterialBasePrice,null, null);
        primaryMaterial.setCompanyCode(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        materials.add(primaryMaterial);
        priceList.add(new QuotePriceItem(primaryProduct.getPrimaryPriceItem().getCategory(),
                primaryProduct.getPrimaryPriceItem().getName(),
                primaryProduct.getPrimaryPriceItem().getName(), primaryMaterialBasePrice, "test",
                primaryProduct.getPrimaryPriceItem().getPlatform()));
        quoteItems.add(new QuoteItem(quoteId,
                primaryProduct.getPrimaryPriceItem().getName(),
                primaryProduct.getPrimaryPriceItem().getName(), "10",
                (new BigDecimal(primaryMaterialBasePrice)).subtract(new BigDecimal(20)).toString(), "test",
                primaryProduct.getPrimaryPriceItem().getPlatform(),
                primaryProduct.getPrimaryPriceItem().getCategory()));
    }
}
