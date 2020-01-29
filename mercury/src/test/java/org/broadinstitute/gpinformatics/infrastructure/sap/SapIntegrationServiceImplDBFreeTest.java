package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.sap.entity.Condition;
import org.broadinstitute.sap.entity.ConditionValue;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.entity.order.SAPOrder;
import org.broadinstitute.sap.entity.order.SAPOrderItem;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService.Option;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class SapIntegrationServiceImplDBFreeTest {

    private static final String MOCK_USER_NAME = "Scott Matthews";
    public static final String SAP_QUOTE_ID= "99923882";
    private static final String SAP_ORDER_NUMBER = "SAPORDER01";
    private SapIntegrationServiceImpl integrationService;
    private SAPProductPriceCache productPriceCache;
    private PriceListCache priceListCache;
    private SapQuote sapQuote = null;
    private SapIntegrationClientImpl mockIntegrationClient;
    private final static Log log = LogFactory.getLog(SapIntegrationServiceImplDBFreeTest.class);

    @BeforeMethod
    public void setUp() throws Exception {

        integrationService = new SapIntegrationServiceImpl();

        BSPUserList mockUserList = Mockito.mock(BSPUserList.class);
        Mockito.when(mockUserList.getUserFullName(Mockito.anyLong())).thenReturn(MOCK_USER_NAME);

        integrationService.setBspUserList(mockUserList);

        mockIntegrationClient = Mockito.mock(SapIntegrationClientImpl.class);

        Mockito.when(mockIntegrationClient.findQuoteDetails(Mockito.anyString())).thenAnswer(
                (Answer<SapQuote>) invocation -> sapQuote);
        Mockito.when(mockIntegrationClient.createSAPOrder(Mockito.any(SAPOrder.class))).thenAnswer(
                invocationOnMock -> SAP_ORDER_NUMBER);

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

        Mockito.when(mockIntegrationClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(materials);

        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);

        conversionPdo.setQuoteId(SAP_QUOTE_ID);
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        conversionPdo.addSapOrderDetail(new SapOrderDetail(SAP_ORDER_NUMBER, 10, SAP_QUOTE_ID,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode()));

        final Product primaryProduct = conversionPdo.getProduct();
        primaryProduct.setAlternateExternalName(primaryProduct.getProductName() + "_external");
        addTestProductMaterialPrice(primaryMaterialBasePrice, priceList, materials, primaryProduct,
                SAP_QUOTE_ID);

        for (ProductOrderAddOn addOn : conversionPdo.getAddOns()) {
            final Product addOnProduct = addOn.getAddOn();
            addOnProduct.setAlternateExternalName(addOnProduct.getProductName() + "_external");
            addTestProductMaterialPrice(addonMaterialPrice, priceList, materials, addOnProduct,
                    SAP_QUOTE_ID);
        }

        sapQuote = TestUtils.buildTestSapQuote(SAP_QUOTE_ID, BigDecimal.valueOf(10), BigDecimal.valueOf(10), conversionPdo,
                TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS, "GP02");
        conversionPdo.setOrderType(ProductOrder.OrderAccessType.COMMERCIAL);

        SAPOrder convertedOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo,
                Option.create(Option.Type.CREATING));

        assertThat(convertedOrder.getCompanyCode(), equalTo(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES));
        assertThat(convertedOrder.getQuoteNumber(), equalTo(sapQuote.getQuoteHeader().getQuoteNumber()));
        assertThat(convertedOrder.getExternalOrderNumber(), equalTo(conversionPdo.getBusinessKey()));
        assertThat(convertedOrder.getSapOrderNumber(), emptyOrNullString());
        assertThat(convertedOrder.getCreator(), equalTo(MOCK_USER_NAME));
        assertThat(convertedOrder.getResearchProjectNumber(), equalTo(conversionPdo.getResearchProject().getBusinessKey()));

        // HashSet order not deterministic. Tested at line 393 assertThat(convertedOrder.getOrderItems().size(), equalTo(conversionPdo.getAddOns().size()+1));

        for(SAPOrderItem item:convertedOrder.getOrderItems()) {

            if(item.getProductIdentifier().equals(primaryProduct.getPartNumber())) {
                assertThat(item.getItemQuantity(), equalTo(
                        (new BigDecimal(conversionPdo.getSamples().size()))));
                assertThat(item.getProductAlias(), equalTo(primaryProduct.getAlternateExternalName()));
                assertThat(item.getItemQuantity(), equalTo(new BigDecimal(conversionPdo.getSamples().size())));
            }
            else {
                final Map<String, String> alternateNameByPartNumber = conversionPdo.getAddOns().stream().map(ProductOrderAddOn::getAddOn)
                        .collect(Collectors.toMap(Product::getPartNumber, Product::getAlternateExternalName));

                assertThat(item.getProductAlias(), equalTo(alternateNameByPartNumber.get(item.getProductIdentifier())));
                assertThat(item.getItemQuantity(), equalTo(new BigDecimal(conversionPdo.getSamples().size())));
            }
        }

        final String customExternalProductName = "Test custom external material";
        final String customExternalAddonProductName = "Test custom external addon material";
        final ProductOrderPriceAdjustment customExternalPriceAdjustment =
                new ProductOrderPriceAdjustment(null, null, customExternalProductName);

        conversionPdo.setCustomPriceAdjustment(customExternalPriceAdjustment);

        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            final ProductOrderAddOnPriceAdjustment customAdjustment =
                    new ProductOrderAddOnPriceAdjustment(null,BigDecimal.ONE, customExternalAddonProductName);
            productOrderAddOn.setCustomPriceAdjustment(customAdjustment);
        }

        convertedOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo,
                Option.create(Option.Type.ORDER_VALUE_QUERY));


        for(SAPOrderItem item:convertedOrder.getOrderItems()) {

            if(item.getProductIdentifier().equals(primaryProduct.getPartNumber())) {
                assertThat(item.getItemQuantity(), equalTo((new BigDecimal(conversionPdo.getSamples().size()))));
                assertThat(item.getProductAlias(), equalTo(customExternalProductName));
                assertThat(item.getItemQuantity(), equalTo(new BigDecimal(conversionPdo.getSamples().size())));
            }
            else {
                final Map<String, String> alternateNameByPartNumber = conversionPdo.getAddOns().stream().map(ProductOrderAddOn::getAddOn)
                        .collect(Collectors.toMap(Product::getPartNumber, Product::getAlternateExternalName));

                assertThat(item.getProductAlias(), equalTo(customExternalAddonProductName));
                assertThat(item.getItemQuantity(), equalTo(BigDecimal.ONE));
            }
        }

        sapQuote = TestUtils.buildTestSapQuote(SAP_QUOTE_ID, BigDecimal.valueOf(10), BigDecimal.valueOf(10), conversionPdo,
                TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS, "GP01");
        conversionPdo.setOrderType(ProductOrder.OrderAccessType.BROAD_PI_ENGAGED_WORK);
        conversionPdo.clearCustomPriceAdjustment();
        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            final ProductOrderAddOnPriceAdjustment customAdjustment =
                    new ProductOrderAddOnPriceAdjustment(null,BigDecimal.ONE, customExternalAddonProductName);
            productOrderAddOn.clearCustomPriceAdjustment();
        }

        convertedOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo,
                Option.create(Option.Type.CREATING));


        assertThat(convertedOrder.getCompanyCode(), equalTo(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD));
        assertThat(convertedOrder.getQuoteNumber(), equalTo(sapQuote.getQuoteHeader().getQuoteNumber()));
        assertThat(convertedOrder.getExternalOrderNumber(), equalTo(conversionPdo.getBusinessKey()));
        assertThat(convertedOrder.getSapOrderNumber(), emptyOrNullString());
        assertThat(convertedOrder.getCreator(), equalTo(MOCK_USER_NAME));
        assertThat(convertedOrder.getResearchProjectNumber(), equalTo(conversionPdo.getResearchProject().getBusinessKey()));

        // HashSet order not deterministic. Tested at line 393 assertThat(convertedOrder.getOrderItems().size(), equalTo(conversionPdo.getAddOns().size()+1));

        for(SAPOrderItem item:convertedOrder.getOrderItems()) {

            if(item.getProductIdentifier().equals(primaryProduct.getPartNumber())) {
                assertThat(item.getItemQuantity(), equalTo((new BigDecimal(conversionPdo.getSamples().size()))));
            }
            assertThat(item.getProductAlias(), is(nullValue()));
            assertThat(item.getItemQuantity(), equalTo(new BigDecimal(conversionPdo.getSamples().size())));
        }
        final String customProductName = "Test custom material";
        final String customAddonProductName = "Test custom addon material";
        final ProductOrderPriceAdjustment customPriceAdjustment =
                new ProductOrderPriceAdjustment(new BigDecimal("29.50"), null, customProductName);

        conversionPdo.setCustomPriceAdjustment(customPriceAdjustment);

        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            final ProductOrderAddOnPriceAdjustment customAdjustment =
                    new ProductOrderAddOnPriceAdjustment(new BigDecimal("39.50"),BigDecimal.ONE, customAddonProductName);
            productOrderAddOn.setCustomPriceAdjustment(customAdjustment);
        }
        convertedOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo,
                Option.create(Option.Type.CREATING));
        assertThat(convertedOrder.getCompanyCode(), equalTo(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD));
        assertThat(convertedOrder.getQuoteNumber(), equalTo(sapQuote.getQuoteHeader().getQuoteNumber()));
        assertThat(convertedOrder.getExternalOrderNumber(), equalTo(conversionPdo.getBusinessKey()));
        assertThat(convertedOrder.getSapOrderNumber(), emptyOrNullString());
        assertThat(convertedOrder.getCreator(), equalTo(MOCK_USER_NAME));
        assertThat(convertedOrder.getResearchProjectNumber(), equalTo(conversionPdo.getResearchProject().getBusinessKey()));

        // HashSet order not deterministic. Tested at line 393 assertThat(convertedOrder.getOrderItems().size(), equalTo(conversionPdo.getAddOns().size()+1));

        for(SAPOrderItem item:convertedOrder.getOrderItems()) {

            if(item.getProductIdentifier().equals(primaryProduct.getPartNumber())) {
                assertThat(item.getItemQuantity(), equalTo((new BigDecimal(conversionPdo.getSamples().size()))));
                assertThat(item.getProductAlias(), equalTo(customProductName));
                assertThat(item.getItemQuantity(), equalTo(new BigDecimal(conversionPdo.getSamples().size())));
                final ConditionValue foundCondition = item.getConditions().iterator().next();
                assertThat(foundCondition.getValue(), equalTo(new BigDecimal("29.50")));
                assertThat(foundCondition.getCondition(), equalTo(Condition.PRICE_OVERRIDE));
            }
            else {
                assertThat(item.getItemQuantity(), equalTo((BigDecimal.ONE)));
                final ConditionValue foundCondition = item.getConditions().iterator().next();
                assertThat(foundCondition.getValue(), equalTo(new BigDecimal("39.50")));
                assertThat(foundCondition.getCondition(), equalTo(Condition.PRICE_OVERRIDE));
            }
        }

        conversionPdo.addSapOrderDetail(new SapOrderDetail("testsap001",
                conversionPdo.getTotalNonAbandonedCount(
                ProductOrder.CountAggregation.SHARE_SAP_ORDER_AND_BILL_READY).intValue(),
                conversionPdo.getQuoteId(),
                conversionPdo.getSapCompanyConfigurationForProductOrder(sapQuote).getCompanyCode()));

        SAPOrder convertedOrder2 = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.create(Option.Type.CREATING));
        for(SAPOrderItem item:convertedOrder2.getOrderItems()) {
            if(item.getProductIdentifier().equals(primaryProduct.getPartNumber())) {
                assertThat(item.getItemQuantity(), equalTo((new BigDecimal(conversionPdo.getSamples().size()))));
            }
            else {
                assertThat(item.getItemQuantity(), equalTo((new BigDecimal(1))));
            }
        }

        SAPOrder convertedOrder3 = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.create(Option.Type.CREATING));
        for(SAPOrderItem item:convertedOrder3.getOrderItems()) {
            if(item.getProductIdentifier().equals(primaryProduct.getPartNumber())) {
                assertThat(item.getItemQuantity(), equalTo((new BigDecimal(conversionPdo.getSamples().size()))));
            }
            else {
                assertThat(item.getItemQuantity(), equalTo((new BigDecimal(1))));
            }
        }

        convertedOrder3 = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.create(Option.Type.CREATING));
        for(SAPOrderItem item:convertedOrder3.getOrderItems()) {
            if(item.getProductIdentifier().equals(conversionPdo.getProduct().getPartNumber())) {
                assertThat(item.getItemQuantity(), equalTo(new BigDecimal(conversionPdo.getSamples().size())));
            }
            else {
                assertThat((item.getItemQuantity()), equalTo((new BigDecimal(1))));
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

        ProductOrderPriceAdjustment customAdjustment3 = new ProductOrderPriceAdjustment(null, BigDecimal.valueOf(99), null);
        conversionPdo.setCustomPriceAdjustment(customAdjustment3);


        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.NONE);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getItemQuantity(), is(equalTo((new BigDecimal(99)))));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }

        final SapOrderDetail sapOrderDetail = conversionPdo.latestSapOrderDetail();
        final LedgerEntry ledgerEntry;
        if(conversionPdo.hasSapQuote()) {
            ledgerEntry =
                    new LedgerEntry(conversionPdo.getSamples().get(0), conversionPdo.getProduct(),
                            new Date(), BigDecimal.valueOf(3));
        } else {
            ledgerEntry =
                    new LedgerEntry(conversionPdo.getSamples().get(0), conversionPdo.getProduct().getPrimaryPriceItem(),
                            new Date(), BigDecimal.valueOf(3));
        }
        sapOrderDetail.addLedgerEntry(ledgerEntry);

        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.create(Option.Type.CLOSING));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
                assertThat(sapOrderItem.getItemQuantity(), is(equalTo((new BigDecimal(0)))));
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }

        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.NONE);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getItemQuantity(), is(equalTo((new BigDecimal(99)))));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }

        ledgerEntry.setBillingMessage(BillingSession.SUCCESS);
        ledgerEntry.setSapDeliveryDocumentId("TestDeliveryDocument");

        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.create(Option.Type.CLOSING));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            if(sapOrderItem.getProductIdentifier().equals(conversionPdo.getProduct().getPartNumber())) {
                assertThat(sapOrderItem.getItemQuantity(), is(equalTo((new BigDecimal(3)))));
            } else {
            assertThat(sapOrderItem.getItemQuantity(), is(equalTo((new BigDecimal(0)))));
            }
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }

        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.NONE);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getItemQuantity(), is(equalTo((new BigDecimal("99")))));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }

        BigDecimal initialQuantity = BigDecimal.valueOf(4);
        Map<String, BigDecimal> productToQuantityMapping = new HashMap<>();
        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            productToQuantityMapping.put(productOrderAddOn.getAddOn().getPartNumber(), initialQuantity.add(BigDecimal.ONE));
            LedgerEntry addonLedgerEntry;

            if(conversionPdo.hasSapQuote()) {
                addonLedgerEntry = new LedgerEntry(conversionPdo.getSamples().get(0),
                        productOrderAddOn.getAddOn(), new Date(),
                        productToQuantityMapping.get(productOrderAddOn.getAddOn().getPartNumber()));
            } else {
                addonLedgerEntry = new LedgerEntry(conversionPdo.getSamples().get(0),
                        productOrderAddOn.getAddOn().getPrimaryPriceItem(), new Date(),
                        productToQuantityMapping.get(productOrderAddOn.getAddOn().getPartNumber()));
            }

            addonLedgerEntry.setBillingMessage(BillingSession.SUCCESS);
            addonLedgerEntry.setSapDeliveryDocumentId(productOrderAddOn.getAddOn().getPartNumber() + "delivery");
            sapOrderDetail.addLedgerEntry(addonLedgerEntry);
        }
        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.create(Option.Type.CLOSING));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            if(sapOrderItem.getProductIdentifier().equals(conversionPdo.getProduct().getPartNumber())) {
                assertThat(sapOrderItem.getItemQuantity(), is(equalTo((new BigDecimal(3)))));
            } else {
                assertThat(sapOrderItem.getItemQuantity(), is(equalTo(
                        (productToQuantityMapping.get(sapOrderItem.getProductIdentifier())))));
            }
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }

        sapOrder = integrationService.initializeSAPOrder(sapQuote, conversionPdo, Option.NONE);
        assertThat(sapOrder.getOrderItems().size(), is(equalTo(1)));
        for (SAPOrderItem sapOrderItem : sapOrder.getOrderItems()) {
            assertThat(sapOrderItem.getConditions(), is(Collections.<ConditionValue>emptyList()));
            assertThat(sapOrderItem.getItemQuantity(), is(equalTo((new BigDecimal(99)))));
            assertThat(sapOrderItem.getProductAlias(), is(emptyOrNullString()));
        }
    }

    public void testInitializeOrderCountsPost2pt0() throws Exception {

        PriceList priceList = new PriceList();
        String jiraTicketKey= "PDO-SAP-test";
        Set<SAPMaterial> materials = new HashSet<>();

        Mockito.when(mockIntegrationClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(materials);

        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);

        productOrder.setQuoteId(SAP_QUOTE_ID);

        addTestProductMaterialPrice("100", priceList, materials, productOrder.getProduct(),
                SAP_QUOTE_ID);

        for (ProductOrderAddOn addOn : productOrder.getAddOns()) {
            addTestProductMaterialPrice("100", priceList, materials, addOn.getAddOn(),
                    SAP_QUOTE_ID);
        }

        this.sapQuote = TestUtils.buildTestSapQuote(SAP_QUOTE_ID, BigDecimal.valueOf(100000), BigDecimal.valueOf(200000), productOrder,
                TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization());

        SAPOrder sapOrderNew =
                integrationService.initializeSAPOrder(sapQuote, productOrder, Option.create(Option.Type.CREATING));

        assertThat(sapOrderNew.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size()+1)));
        for (SAPOrderItem orderItem : sapOrderNew.getOrderItems()) {
            assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
            assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(productOrder.getSamples().size())),
                    is(equalTo(0)));
        }

        SAPOrder sapOrderNone =
                integrationService.initializeSAPOrder(sapQuote, productOrder, Option.NONE);

        assertThat(sapOrderNone.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size()+1)));
        for (SAPOrderItem orderItem : sapOrderNone.getOrderItems()) {
            assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
            assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(productOrder.getSamples().size())),
                    is(equalTo(0)));
        }

        SAPOrder orderValueQuery =
                integrationService.initializeSAPOrder(sapQuote, productOrder, Option.create(Option.Type.ORDER_VALUE_QUERY));

        assertThat(orderValueQuery.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size()+1)));
        for (SAPOrderItem orderItem : orderValueQuery.getOrderItems()) {
            assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
            assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(productOrder.getSamples().size())),
                    is(equalTo(0)));
        }

        // Now create an SAP order and associate it with the Product Order
        final String orderId = integrationService.createOrder(productOrder);
        productOrder.addSapOrderDetail(new SapOrderDetail(orderId, productOrder.getSampleCount(),
                productOrder.getQuoteId(), productOrder.getSapCompanyConfigurationForProductOrder().getCompanyCode()));

        SAPOrder orderClosing =
                integrationService.initializeSAPOrder(sapQuote, productOrder, Option.create(Option.Type.CLOSING));

        assertThat(orderClosing.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size()+1)));
        for (SAPOrderItem orderItem : orderClosing.getOrderItems()) {
            assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
            assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.ZERO), is(equalTo(0)));
        }

        productOrder.addCustomPriceAdjustment(new ProductOrderPriceAdjustment(BigDecimal.valueOf(80), BigDecimal.valueOf(8), null));


        sapOrderNew = integrationService.initializeSAPOrder(sapQuote, productOrder, Option.create(Option.Type.CREATING));

        assertThat(sapOrderNew.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size()+1)));
        for (SAPOrderItem orderItem : sapOrderNew.getOrderItems()) {
            assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
            if(orderItem.getProductIdentifier().equals(productOrder.getProduct().getPartNumber())) {
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(8)),
                        is(equalTo(0)));
            } else {
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(productOrder.getSamples().size())),
                        is(equalTo(0)));
            }
        }

        sapOrderNone = integrationService.initializeSAPOrder(sapQuote, productOrder, Option.NONE);

        assertThat(sapOrderNone.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size()+1)));
        for (SAPOrderItem orderItem : sapOrderNone.getOrderItems()) {
            assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
            if(orderItem.getProductIdentifier().equals(productOrder.getProduct().getPartNumber())) {
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(8)),
                        is(equalTo(0)));
            } else {
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(productOrder.getSamples().size())),
                        is(equalTo(0)));
            }
        }

        orderValueQuery = integrationService.initializeSAPOrder(sapQuote, productOrder,
                Option.create(Option.Type.ORDER_VALUE_QUERY));

        assertThat(orderValueQuery.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size()+1)));
        for (SAPOrderItem orderItem : orderValueQuery.getOrderItems()) {
            assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
            if(orderItem.getProductIdentifier().equals(productOrder.getProduct().getPartNumber())) {
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(8)), is(equalTo(0)));
            } else {
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(productOrder.getSamples().size())), is(equalTo(0)));
            }
        }

        orderClosing =
                integrationService.initializeSAPOrder(sapQuote, productOrder, Option.create(Option.Type.CLOSING));

        assertThat(orderClosing.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size()+1)));
        for (SAPOrderItem orderItem : orderClosing.getOrderItems()) {
            assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
            assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.ZERO), is(equalTo(0)));
        }

        int ledgerCount = 4;

        IntStream.range(0,ledgerCount).forEach(value -> {
            addLedgerItems(productOrder, BigDecimal.valueOf(1));

            SAPOrder newOrder =
                    integrationService.initializeSAPOrder(sapQuote, productOrder, Option.create(Option.Type.CREATING));

            assertThat(newOrder.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size() + 1)));
            for (SAPOrderItem orderItem : newOrder.getOrderItems()) {
                assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
                if (orderItem.getProductIdentifier().equals(productOrder.getProduct().getPartNumber())) {

                    assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(8 - (value + 1))),
                            is(equalTo(0)));
                } else {
                    assertThat(orderItem.getItemQuantity()
                                    .compareTo(BigDecimal.valueOf(productOrder.getSamples().size() - (value + 1))),
                            is(greaterThanOrEqualTo(0)));
                }
            }

            SAPOrder noneOrder = integrationService.initializeSAPOrder(sapQuote, productOrder, Option.NONE);

            assertThat(noneOrder.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size() + 1)));
            for (SAPOrderItem orderItem : noneOrder.getOrderItems()) {
                assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
                if (orderItem.getProductIdentifier().equals(productOrder.getProduct().getPartNumber())) {

                    assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(8)),
                            is(equalTo(0)));
                } else {
                    assertThat(
                            orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(productOrder.getSamples().size())),
                            is(greaterThanOrEqualTo(0)));
                }
            }

            SAPOrder queryOrder = integrationService.initializeSAPOrder(sapQuote, productOrder,
                    Option.create(Option.Type.ORDER_VALUE_QUERY));

            assertThat(queryOrder.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size() + 1)));
            for (SAPOrderItem orderItem : queryOrder.getOrderItems()) {
                assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
                if (orderItem.getProductIdentifier().equals(productOrder.getProduct().getPartNumber())) {
                    assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(8)),
                            is(equalTo(0)));
                } else {
                    assertThat(
                            orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(productOrder.getSamples().size())),
                            is(equalTo(0)));
                }
            }

            SAPOrder closingOrder =
                    integrationService.initializeSAPOrder(sapQuote, productOrder, Option.create(Option.Type.CLOSING));

            assertThat(closingOrder.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size() + 1)));
            for (SAPOrderItem orderItem : closingOrder.getOrderItems()) {
                assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(value + 1)), is(equalTo(0)));
            }
        }

        //Mimic creating a new order
        productOrder.addSapOrderDetail(new SapOrderDetail(orderId+2, productOrder.getSampleCount(),
                productOrder.getQuoteId(), productOrder.getSapCompanyConfigurationForProductOrder().getCompanyCode()));

        //  There should be a custom quantity of 8 on the primary product with 4 billed to it.
        //  THere should be a standard quantity of 10 on the addon product with 4 billed to it as well.

        sapOrderNew = integrationService.initializeSAPOrder(sapQuote, productOrder, Option.create(Option.Type.CREATING));

        assertThat(sapOrderNew.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size()+1)));
        for (SAPOrderItem orderItem : sapOrderNew.getOrderItems()) {
            assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
            if(orderItem.getProductIdentifier().equals(productOrder.getProduct().getPartNumber())) {
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(8-ledgerCount)),
                        is(equalTo(0)));
            } else {
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(productOrder.getSamples().size() - ledgerCount)),
                        is(equalTo(0)));
            }
        }

        sapOrderNone = integrationService.initializeSAPOrder(sapQuote, productOrder, Option.NONE);

        assertThat(sapOrderNone.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size()+1)));
        for (SAPOrderItem orderItem : sapOrderNone.getOrderItems()) {
            assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
            if(orderItem.getProductIdentifier().equals(productOrder.getProduct().getPartNumber())) {
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(8-ledgerCount)),
                        is(equalTo(0)));
            } else {
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(productOrder.getSamples().size() - ledgerCount)),
                        is(equalTo(0)));
            }
        }

        orderValueQuery = integrationService.initializeSAPOrder(sapQuote, productOrder,
                Option.create(Option.Type.ORDER_VALUE_QUERY));

        assertThat(orderValueQuery.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size()+1)));
        for (SAPOrderItem orderItem : orderValueQuery.getOrderItems()) {
            assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
            if(orderItem.getProductIdentifier().equals(productOrder.getProduct().getPartNumber())) {
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(8-ledgerCount)), is(equalTo(0)));
            } else {
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(productOrder.getSamples().size()-ledgerCount)), is(equalTo(0)));
            }
        }

        orderClosing =
                integrationService.initializeSAPOrder(sapQuote, productOrder, Option.create(Option.Type.CLOSING));

        assertThat(orderClosing.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size()+1)));
        for (SAPOrderItem orderItem : orderClosing.getOrderItems()) {
            assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
            assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(0)), is(equalTo(0)));
        }

        int newLedgerCount = 4;

        IntStream.range(0,newLedgerCount).forEach(value -> {
            addLedgerItems(productOrder, BigDecimal.valueOf(1));

            SAPOrder newOrder =
                    integrationService.initializeSAPOrder(sapQuote, productOrder, Option.create(Option.Type.CREATING));

            assertThat(newOrder.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size() + 1)));
            for (SAPOrderItem orderItem : newOrder.getOrderItems()) {
                assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
                if (orderItem.getProductIdentifier().equals(productOrder.getProduct().getPartNumber())) {
                    assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(8 - ledgerCount - (value + 1))),
                            is(equalTo(0)));
                } else {
                    assertThat(orderItem.getItemQuantity().compareTo(
                            BigDecimal.valueOf(productOrder.getSamples().size() - ledgerCount - (value + 1))),
                            is(greaterThanOrEqualTo(0)));
                }
            }

            SAPOrder noneOrder = integrationService.initializeSAPOrder(sapQuote, productOrder, Option.NONE);

            assertThat(noneOrder.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size() + 1)));
            for (SAPOrderItem orderItem : noneOrder.getOrderItems()) {
                assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
                if (orderItem.getProductIdentifier().equals(productOrder.getProduct().getPartNumber())) {

                    assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(8 - ledgerCount)),
                            is(equalTo(0)));
                } else {
                    assertThat(orderItem.getItemQuantity()
                                    .compareTo(BigDecimal.valueOf(productOrder.getSamples().size() - ledgerCount)),
                            is(greaterThanOrEqualTo(0)));
                }
            }

            SAPOrder queryOrder = integrationService.initializeSAPOrder(sapQuote, productOrder,
                    Option.create(Option.Type.ORDER_VALUE_QUERY));

            assertThat(queryOrder.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size() + 1)));
            for (SAPOrderItem orderItem : queryOrder.getOrderItems()) {
                assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
                if (orderItem.getProductIdentifier().equals(productOrder.getProduct().getPartNumber())) {
                    assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(8 - ledgerCount)),
                            is(equalTo(0)));
                } else {
                    assertThat(orderItem.getItemQuantity()
                                    .compareTo(BigDecimal.valueOf(productOrder.getSamples().size() - ledgerCount)),
                            is(equalTo(0)));
                }
            }

            SAPOrder closingOrder =
                    integrationService.initializeSAPOrder(sapQuote, productOrder, Option.create(Option.Type.CLOSING));

            assertThat(closingOrder.getOrderItems().size(), is(equalTo(productOrder.getAddOns().size() + 1)));
            for (SAPOrderItem orderItem : closingOrder.getOrderItems()) {
                assertThat(orderItem.getItemQuantity(), is(not(lessThan(BigDecimal.ZERO))));
                assertThat(orderItem.getItemQuantity().compareTo(BigDecimal.valueOf(value + 1)), is(equalTo(0)));
            }
        }
    }

    @Test(dataProvider = "orderStatusForSampleCount")
    public void testGetSampleCountFreshOrderNoOverrides(ProductOrder.OrderStatus testOrderStatus, int extraSamples) throws Exception {
        PriceList priceList = new PriceList();
        Set<SAPMaterial> materials = new HashSet<>();

        ProductOrder countTestPDO = ProductOrderTestFactory.createDummyProductOrder(10, "PDO-smpcnt");
        countTestPDO.setPriorToSAP1_5(false);
        countTestPDO.setQuoteId(SAP_QUOTE_ID);
        countTestPDO.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        countTestPDO.addSapOrderDetail(new SapOrderDetail(SAP_ORDER_NUMBER, 10, SAP_QUOTE_ID,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode()));
        countTestPDO.setOrderStatus(testOrderStatus);

        final Product primaryProduct = countTestPDO.getProduct();
        addTestProductMaterialPrice("50.00", priceList, materials, primaryProduct, SAP_QUOTE_ID);

        for (ProductOrderAddOn addOn : countTestPDO.getAddOns()) {
            addTestProductMaterialPrice("30.00", priceList, materials, addOn.getAddOn(),
                    SAP_QUOTE_ID);
        }

        double closingCount = 0d;

        while (closingCount <= countTestPDO.getSamples().size()) {
            double primarySampleCount = SapIntegrationServiceImpl.getSampleCount(countTestPDO,
                    countTestPDO.getProduct(), extraSamples, Option.NONE).doubleValue();
            double primaryClosingCount = SapIntegrationServiceImpl.getSampleCount(countTestPDO,
                    countTestPDO.getProduct(), extraSamples, Option.create(Option.Type.CLOSING)).doubleValue();
            double primaryOrderValueQueryCount = SapIntegrationServiceImpl.getSampleCount(countTestPDO,
                    countTestPDO.getProduct(), extraSamples,
                    Option.create(Option.Type.ORDER_VALUE_QUERY)).doubleValue();
            assertThat(primarySampleCount, is(equalTo((double) countTestPDO.getSamples().size()+extraSamples)));
            assertThat(primaryClosingCount, is(equalTo(closingCount)));
            assertThat(primaryOrderValueQueryCount, is(equalTo((double) countTestPDO.getSamples().size()+extraSamples)));

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
                assertThat(addOnOrderValueQueryCount, is(equalTo((double) countTestPDO.getSamples().size()+extraSamples )));
            }
            addLedgerItems(countTestPDO, BigDecimal.valueOf(1));

            closingCount++;
        }
    }

    public void testFindMaterialsCall() throws Exception {
        Set<SAPMaterial> materials = new HashSet<>();
        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        final String primaryTestMaterialId = "p-test-something";

        SAPMaterial primaryMaterial = new SAPMaterial(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getDefaultWbs(), "test description",
                "1000", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, new Date(), new Date(), Collections.emptyMap(),
                Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization());

        SAPMaterial primaryExternalMaterial = new SAPMaterial(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getDefaultWbs(), "test description",
                "1000", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,new Date(), new Date(), Collections.emptyMap(),
                Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization());

        SAPMaterial primaryPrismMaterial = new SAPMaterial(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.PRISM,
                SapIntegrationClientImpl.SAPCompanyConfiguration.PRISM.getDefaultWbs(), "test description",
                "1000", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,new Date(), new Date(), Collections.emptyMap(),
                Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                SapIntegrationClientImpl.SAPCompanyConfiguration.PRISM.getSalesOrganization());

        SAPMaterial primaryGPPMaterial = new SAPMaterial(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.GPP,
                SapIntegrationClientImpl.SAPCompanyConfiguration.GPP.getDefaultWbs(), "test description",
                "1000", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,new Date(), new Date(), Collections.emptyMap(),
                Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                SapIntegrationClientImpl.SAPCompanyConfiguration.GPP.getSalesOrganization());

        Mockito.when(mockIntegrationClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenAnswer(
                (Answer<Set<SAPMaterial>>) invocationOnMock -> materials);

        materials.add(primaryMaterial);

        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization()),
                is(equalTo(primaryMaterial)));
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization()),
                is(nullValue()));
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.PRISM.getSalesOrganization()),
                is(nullValue()));
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.GPP.getSalesOrganization()),
                is(nullValue()));

        materials.add(primaryExternalMaterial);
        productPriceCache.refreshCache();
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization()),
                is(equalTo(primaryMaterial)));
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization()),
                is(equalTo(primaryExternalMaterial)));
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.PRISM.getSalesOrganization()),
                is(nullValue()));
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.GPP.getSalesOrganization()),
                is(nullValue()));

        materials.add(primaryPrismMaterial);
        productPriceCache.refreshCache();
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization()),
                is(equalTo(primaryMaterial)));
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization()),
                is(equalTo(primaryExternalMaterial)));
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.PRISM.getSalesOrganization()),
                is(equalTo(primaryPrismMaterial)));
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.GPP.getSalesOrganization()),
                is(nullValue()));

        materials.add(primaryGPPMaterial);
        productPriceCache.refreshCache();
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization()),
                is(equalTo(primaryMaterial)));
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization()),
                is(equalTo(primaryExternalMaterial)));
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.PRISM.getSalesOrganization()),
                is(equalTo(primaryPrismMaterial)));
        assertThat(productPriceCache.findByPartNumber(primaryTestMaterialId,
                SapIntegrationClientImpl.SAPCompanyConfiguration.GPP.getSalesOrganization()),
                is(equalTo(primaryGPPMaterial)));
    }

    private void addLedgerItems(ProductOrder order, BigDecimal ledgerCount) {
        for (ProductOrderSample productOrderSample : order.getSamples()) {
            if(!productOrderSample.isCompletelyBilled()) {
                if(order.hasSapQuote()) {
                    productOrderSample.addLedgerItem(new Date(), order.getProduct(), ledgerCount, false);
                    for (ProductOrderAddOn productOrderAddOn : order.getAddOns()) {
                        productOrderSample.addLedgerItem(new Date(), productOrderAddOn.getAddOn(),
                                ledgerCount, false);
                    }
                } else {
                    productOrderSample
                            .addLedgerItem(new Date(), order.getProduct().getPrimaryPriceItem(), ledgerCount);
                    for (ProductOrderAddOn productOrderAddOn : order.getAddOns()) {
                        productOrderSample.addLedgerItem(new Date(), productOrderAddOn.getAddOn().getPrimaryPriceItem(),
                                ledgerCount);
                    }
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

    private static void addTestProductMaterialPrice(String primaryMaterialBasePrice, PriceList priceList,
                                                   Set<SAPMaterial> materials,
                                                   Product primaryProduct, String quoteId) {
        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        SAPMaterial primaryMaterial = new SAPMaterial(primaryProduct.getPartNumber(), broad, broad.getDefaultWbs(), "test description", primaryMaterialBasePrice,
            SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,new Date(), new Date(),
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
