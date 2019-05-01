package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOnPriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderPriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.sap.entity.Condition;
import org.broadinstitute.sap.entity.ConditionValue;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.entity.order.SAPOrder;
import org.broadinstitute.sap.entity.order.SAPOrderItem;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService.Option;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;

@Test(groups = TestGroups.DATABASE_FREE)
public class SapIntegrationServiceImplDBFreeTest {

    public static final String MOCK_USER_NAME = "Scott Matthews";
    public static final String SINGLE_SOURCE_PO_QUOTE_ID = "GPTest";
    public static final String SINGLE_SOURCE_FUND_RES_QUOTE_ID = "GPFRQT";
    public static final String MULTIPLE_SOURCE_QUOTE_ID = "GPMultipleQtTest";
    public static final String MOCK_CUSTOMER_NUMBER = "0000123456";
    public static final String SAP_ORDER_NUMBER = "SAPORDER01";
    private Quote testSingleSourceQuote;
    private Quote testMultipleLevelQuote;
    private String testUser;
    private Quote testSingleSourceFRQuote;
    private SapIntegrationServiceImpl integrationService;
    private SAPProductPriceCache productPriceCache;
    private PriceListCache priceListCache;
    SapQuote sapQuote = null;
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

        BSPUserList mockUserList = Mockito.mock(BSPUserList.class);
        Mockito.when(mockUserList.getUserFullName(Mockito.anyLong())).thenReturn(MOCK_USER_NAME);

        integrationService.setBspUserList(mockUserList);

        SapIntegrationClientImpl mockIntegrationClient = Mockito.mock(SapIntegrationClientImpl.class);

        Mockito.when(mockIntegrationClient.findQuoteDetails(Mockito.anyString())).thenAnswer(new Answer<SapQuote>() {
            @Override
            public SapQuote answer(InvocationOnMock invocation) throws Throwable {
                return sapQuote;
            }
        });

        integrationService.setWrappedClient(mockIntegrationClient);

        integrationService.setPriceListCache(priceListCache);

        productPriceCache = new SAPProductPriceCache(integrationService);
        integrationService.setProductPriceCache(productPriceCache);
        final SAPAccessControlEjb mockAccessController = Mockito.mock(SAPAccessControlEjb.class);
        Mockito.when(mockAccessController.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        productPriceCache.setAccessControlEjb(mockAccessController);
    }


    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test(enabled = true)
    public void testInitializeSAPOrderPost1pt5() throws Exception {

        final String primaryMaterialBasePrice = "50.50";
        final String addonMaterialPrice = "40.50";

        PriceList priceList = new PriceList();
        String jiraTicketKey= "PDO-SAP-test";
        Set<SAPMaterial> materials = new HashSet<>();

        Mockito.when(integrationService.findProductsInSap()).thenReturn(materials);

        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);

        conversionPdo.setQuoteId(testSingleSourceQuote.getAlphanumericId());
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        conversionPdo.addSapOrderDetail(new SapOrderDetail(SAP_ORDER_NUMBER, 10, testSingleSourceQuote.getAlphanumericId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode()));

        final Product primaryProduct = conversionPdo.getProduct();
        addTestProductMaterialPrice(primaryMaterialBasePrice, priceList, materials, primaryProduct,
                testSingleSourceQuote.getAlphanumericId());

        for (ProductOrderAddOn addOn : conversionPdo.getAddOns()) {
            addTestProductMaterialPrice(addonMaterialPrice, priceList, materials, addOn.getAddOn(),
                    testSingleSourceQuote.getAlphanumericId());
        }

        final String customProductName = "Test custom material";
        final String customAddonProductName = "Test custom addon material";
        final ProductOrderPriceAdjustment customPriceAdjustment =
                new ProductOrderPriceAdjustment(new BigDecimal("29.50"), null, customProductName);

        conversionPdo.setCustomPriceAdjustment(customPriceAdjustment);

        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            final ProductOrderAddOnPriceAdjustment customAdjustment =
                    new ProductOrderAddOnPriceAdjustment(new BigDecimal("39.50"),1, customAddonProductName);
            productOrderAddOn.setCustomPriceAdjustment(customAdjustment);
        }
        sapQuote = TestUtils.buildTestSapQuote("01234", BigDecimal.TEN, BigDecimal.TEN, conversionPdo,
            TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS, "GP01");

        SAPOrder convertedOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo,
            Option.create(Option.Type.CREATING));

        assertThat(convertedOrder.getCompanyCode(), equalTo(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD));
        assertThat(convertedOrder.getQuoteNumber(), equalTo(testSingleSourceQuote.getAlphanumericId()));
        assertThat(convertedOrder.getExternalOrderNumber(), equalTo(conversionPdo.getBusinessKey()));
        assertThat(convertedOrder.getSapOrderNumber(), emptyOrNullString());
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
                assertThat(foundCondition.getCondition(), equalTo(Condition.PRICE_OVERRIDE));
            }
            else {
                assertThat(item.getItemQuantity().doubleValue(), equalTo((new BigDecimal(1.0d)).doubleValue()));
                final ConditionValue foundCondition = item.getConditions().iterator().next();
                assertThat(foundCondition.getValue(), equalTo(new BigDecimal("39.50")));
                assertThat(foundCondition.getCondition(), equalTo(Condition.PRICE_OVERRIDE));
            }
        }

        conversionPdo.addSapOrderDetail(new SapOrderDetail("testsap001", conversionPdo.getTotalNonAbandonedCount(
                ProductOrder.CountAggregation.SHARE_SAP_ORDER_AND_BILL_READY),
                conversionPdo.getQuoteId(), conversionPdo.getSapCompanyConfigurationForProductOrder().getCompanyCode()));

        SAPOrder convertedOrder2 = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.create(Option.Type.CREATING));
        for(SAPOrderItem item:convertedOrder2.getOrderItems()) {
            if(item.getProductIdentifier().equals(primaryProduct.getPartNumber())) {
                assertThat(item.getItemQuantity().doubleValue(), equalTo(
                        (new BigDecimal(conversionPdo.getSamples().size())).doubleValue()));
            }
            else {
                assertThat(item.getItemQuantity().doubleValue(), equalTo((new BigDecimal(1)).doubleValue()));
            }
        }


        SAPOrder convertedOrder3 = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.create(Option.Type.CREATING));
        for(SAPOrderItem item:convertedOrder3.getOrderItems()) {
            if(item.getProductIdentifier().equals(primaryProduct.getPartNumber())) {
                assertThat(item.getItemQuantity().doubleValue(), equalTo(
                        (new BigDecimal(conversionPdo.getSamples().size())).doubleValue()));
            }
            else {
                assertThat(item.getItemQuantity().doubleValue(), equalTo((new BigDecimal(1)).doubleValue()));
            }
        }

        convertedOrder3 = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.create(Option.Type.CREATING));
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

        SAPOrder sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.NONE);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getProductAlias(), is(equalTo(newProductAlias)));
        }

        ProductOrderPriceAdjustment customAdjustment2 = new ProductOrderPriceAdjustment(new BigDecimal("14.50"), null, null);
        conversionPdo.setCustomPriceAdjustment(customAdjustment2);
        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.NONE);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions().size(), is(equalTo(1)));
            for (ConditionValue conditionValue : sapOrderItem.getConditions()) {
                assertThat(conditionValue.getCondition(), is(equalTo(Condition.PRICE_OVERRIDE)));
                assertThat(conditionValue.getValue(), is(equalTo(new BigDecimal("14.50"))));
            }
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }

        ProductOrderPriceAdjustment customAdjustment3 = new ProductOrderPriceAdjustment(null, 99, null);
        conversionPdo.setCustomPriceAdjustment(customAdjustment3);


        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.NONE);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(99)).doubleValue())));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }


        final SapOrderDetail sapOrderDetail = conversionPdo.latestSapOrderDetail();
        final LedgerEntry ledgerEntry =
                new LedgerEntry(conversionPdo.getSamples().get(0), conversionPdo.getProduct().getPrimaryPriceItem(),
                        new Date(), 3d);
        sapOrderDetail.addLedgerEntry(ledgerEntry);

        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.create(Option.Type.CLOSING));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
                assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(0)).doubleValue())));
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }

        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.NONE);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(99)).doubleValue())));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }

        ledgerEntry.setBillingMessage(BillingSession.SUCCESS);
        ledgerEntry.setSapDeliveryDocumentId("TestDeliveryDocument");

        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.create(Option.Type.CLOSING));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            if(sapOrderItem.getProductIdentifier().equals(conversionPdo.getProduct().getPartNumber())) {
                assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(3)).doubleValue())));
            } else {
            assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(0)).doubleValue())));
            }
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }

        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.NONE);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(99)).doubleValue())));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
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
        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.create(Option.Type.CLOSING));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            if(sapOrderItem.getProductIdentifier().equals(conversionPdo.getProduct().getPartNumber())) {
                assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(3)).doubleValue())));
            } else {
                assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo(
                        (new BigDecimal(productToQuantityMapping.get(sapOrderItem.getProductIdentifier()))).doubleValue())));
            }
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }

        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.NONE);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getItemQuantity().doubleValue(), is(equalTo((new BigDecimal(99)).doubleValue())));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }
    }

    @Test(dataProvider = "orderStatusForSampleCount")
    public void testGetSampleCountFreshOrderNoOverrides(ProductOrder.OrderStatus testOrderStatus, int extraSamples) throws Exception {
        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> materials = new HashSet<>();

        ProductOrder countTestPDO = ProductOrderTestFactory.createDummyProductOrder(10, "PDO-smpcnt");
        countTestPDO.setPriorToSAP1_5(false);
        countTestPDO.setQuoteId(testSingleSourceQuote.getAlphanumericId());
        countTestPDO.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        countTestPDO.addSapOrderDetail(new SapOrderDetail(SAP_ORDER_NUMBER, 10, testSingleSourceQuote.getAlphanumericId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode()));
        System.out.println("The current order status is : " + testOrderStatus.getDisplayName());
        countTestPDO.setOrderStatus(testOrderStatus);

        final Product primaryProduct = countTestPDO.getProduct();
        addTestProductMaterialPrice("50.00", priceList, materials, primaryProduct,
                testSingleSourceQuote.getAlphanumericId());

        for (ProductOrderAddOn addOn : countTestPDO.getAddOns()) {
            addTestProductMaterialPrice("30.00", priceList, materials, addOn.getAddOn(),
                    testSingleSourceQuote.getAlphanumericId());
        }
        testSingleSourceQuote.setQuoteItems(quoteItems);

        double closingCount = 0d;

        while (closingCount <= countTestPDO.getSamples().size()) {
            double primarySampleCount = SapIntegrationServiceImpl
                .getSampleCount(countTestPDO, countTestPDO.getProduct(), extraSamples, Option.NONE).doubleValue();
            double primaryClosingCount = SapIntegrationServiceImpl
                .getSampleCount(countTestPDO, countTestPDO.getProduct(), extraSamples,
                    Option.create(Option.Type.CLOSING)).doubleValue();
            double primaryOrderValueQueryCount = SapIntegrationServiceImpl
                .getSampleCount(countTestPDO, countTestPDO.getProduct(), extraSamples,
                    Option.create(Option.Type.ORDER_VALUE_QUERY)).doubleValue();
            assertThat(primarySampleCount, is(equalTo((double) countTestPDO.getSamples().size()+extraSamples)));
            assertThat(primaryClosingCount, is(equalTo(closingCount)));
            assertThat(primaryOrderValueQueryCount, is(equalTo((double) countTestPDO.getSamples().size()+extraSamples - closingCount)));


            for (ProductOrderAddOn addOn : countTestPDO.getAddOns()) {
                final double addonSampleCount =
                    SapIntegrationServiceImpl.getSampleCount(countTestPDO, addOn.getAddOn(), extraSamples, Option.NONE)
                        .doubleValue();
                final double addonClosingCount = SapIntegrationServiceImpl
                    .getSampleCount(countTestPDO, addOn.getAddOn(), extraSamples, Option.create(Option.Type.CLOSING))
                    .doubleValue();
                final double addOnOrderValueQueryCount = SapIntegrationServiceImpl
                    .getSampleCount(countTestPDO, addOn.getAddOn(), extraSamples,
                        Option.create(Option.Type.ORDER_VALUE_QUERY)).doubleValue();
                assertThat(addonSampleCount, is(equalTo((double) countTestPDO.getSamples().size()+extraSamples)));
                assertThat(addonClosingCount, is(equalTo(closingCount)));
                assertThat(addOnOrderValueQueryCount, is(equalTo((double) countTestPDO.getSamples().size()+extraSamples - closingCount)));
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
                                                   Set<SAPMaterial> materials,
                                                   Product primaryProduct, String quoteId) throws SAPIntegrationException {
        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        SAPMaterial primaryMaterial = new SAPMaterial(primaryProduct.getPartNumber(), broad, broad.getDefaultWbs(), "test description", primaryMaterialBasePrice,
            SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "","",new Date(), new Date(),
            Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
            broad.getSalesOrganization());
        materials.add(primaryMaterial);
        priceList.add(new QuotePriceItem(primaryProduct.getPrimaryPriceItem().getCategory(),
                primaryProduct.getPrimaryPriceItem().getName(),
                primaryProduct.getPrimaryPriceItem().getName(), primaryMaterialBasePrice, "test",
                primaryProduct.getPrimaryPriceItem().getPlatform()));
    }

    @DataProvider(name="orderStatusForSampleCount")
    public Iterator<Object[]> orderStatusForSampleCount() {
        List<Object[]> testScenarios = new ArrayList<>();

        testScenarios.add(new Object[]{ProductOrder.OrderStatus.Submitted, 2});
        testScenarios.add(new Object[]{ProductOrder.OrderStatus.Submitted, 0});
        testScenarios.add(new Object[]{ProductOrder.OrderStatus.Draft, 2});
        testScenarios.add(new Object[]{ProductOrder.OrderStatus.Draft, 0});
        testScenarios.add(new Object[]{ProductOrder.OrderStatus.Abandoned, 2});
        testScenarios.add(new Object[]{ProductOrder.OrderStatus.Abandoned, 0});
        testScenarios.add(new Object[]{ProductOrder.OrderStatus.Completed, 2});
        testScenarios.add(new Object[]{ProductOrder.OrderStatus.Completed, 0});
        testScenarios.add(new Object[]{ProductOrder.OrderStatus.Pending, 2});
        testScenarios.add(new Object[]{ProductOrder.OrderStatus.Pending, 0});

        return testScenarios.iterator();
    }
}
