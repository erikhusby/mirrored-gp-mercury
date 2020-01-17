/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.ProductLedgerIndex;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPInterfaceException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.sap.entity.SAPDeliveryDocument;
import org.broadinstitute.sap.entity.SAPReturnOrder;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.entity.order.SAPOrderItem;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingCreditDbFreeTest {

    public static final String RETURN_ORDER_ID = "8535937";
    private SapIntegrationClientImpl mockSapClient;
    public static final String DELIVERY_DOCUMENT = "0211403";

    private PriceItem priceItem;
    private QuotePriceItem quotePriceItem;
    private BillingAdaptor billingAdaptor;
    private ProductOrder pdo;

    private EmailSender mockEmailSender = Mockito.mock(EmailSender.class);
    private BillingSessionDao billingSessionDao = Mockito.mock(BillingSessionDao.class);
    private PriceListCache priceListCache = Mockito.mock(PriceListCache.class);
    private QuoteService quoteService = Mockito.mock(QuoteService.class);
    private SapIntegrationServiceImpl sapService;
    private BillingSessionAccessEjb billingSessionAccessEjb = Mockito.mock(BillingSessionAccessEjb.class);
    private ProductOrderEjb productOrderEjb = Mockito.mock(ProductOrderEjb.class);
    private SAPProductPriceCache productPriceCache = Mockito.mock(SAPProductPriceCache.class);
    private SAPAccessControlEjb accessControlEjb = Mockito.mock(SAPAccessControlEjb.class);
    private BillingEjb billingEjb;

    private final Double qtyPositiveTwo = 2D;
    private final Double qtyNegativeTwo = (double) Math.negateExact(qtyPositiveTwo.longValue());

    @BeforeMethod
    public void setUp()

        throws QuoteNotFoundException, QuoteServerException, InvalidProductException, SAPInterfaceException,
        SAPIntegrationException {
        resetMocks();

        final BSPUserList bspUserList = Mockito.mock(BSPUserList.class);
        final SAPProductPriceCache mockProductPriceCache = Mockito.mock(SAPProductPriceCache.class);

        pdo = ProductOrderTestFactory.createDummyProductOrder(2, "PDO-1234");
        pdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        pdo.setProductOrderAddOns(Collections.emptyList());

        String quoteId = pdo.getQuoteId();
        String sapOrderNumber = "sap1234";

        SapOrderDetail sapOrderDetail = new SapOrderDetail(sapOrderNumber, 1, quoteId,
            SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        sapService = new SapIntegrationServiceImpl(SapConfig.produce(Deployment.DEV), bspUserList,
            priceListCache, productPriceCache, accessControlEjb);
        mockSapClient = Mockito.mock(SapIntegrationClientImpl.class);
        sapService.setWrappedClient(mockSapClient);
        pdo.setSapReferenceOrders(Collections.singletonList(sapOrderDetail));

        priceItem = pdo.getProduct().getPrimaryPriceItem();
        priceItem.setPrice("100");
        priceItem.setUnits("uL");
        quotePriceItem = new QuotePriceItem(priceItem.getCategory(), quoteId, priceItem.getName(), priceItem.getPrice(),
                priceItem.getUnits(), priceItem.getPlatform());

        QuoteImportItem quoteImportItem =
            new QuoteImportItem(quotePriceItem.getId(), priceItem, "Quote Type", new ArrayList<>(),
                new Date(), pdo.getProduct(), pdo);
        PriceList price = new PriceList();
        price.getQuotePriceItems().add(quotePriceItem);

        quoteImportItem.setPriceOnWorkDate(price);
        Quote quote = new Quote(quoteImportItem.getQuoteId(), null, ApprovalStatus.FUNDED);
        quote.setQuoteFunding(
            new QuoteFunding(Collections.singleton(
                new FundingLevel("100", Collections.singleton(new Funding(Funding.PURCHASE_ORDER, "foo", "bar"))))));
        quoteImportItem.setQuote(quote);
        final List<QuotePriceItem> quotePriceItems = Collections.singletonList(quotePriceItem);
        Mockito.when(priceListCache.getQuotePriceItems()).thenReturn(quotePriceItems);
        Mockito.when(priceListCache.findByKeyFields(priceItem)).thenReturn(quotePriceItem);

        Mockito.when(quoteService.getPriceItemsForDate(Mockito.anyListOf(QuoteImportItem.class)))
            .thenReturn(quoteImportItem.getPriceOnWorkDate());
        Mockito.when(quoteService.getQuoteByAlphaId(Mockito.anyString())).thenReturn(quoteImportItem.getQuote());
        Mockito.when(quoteService.getQuoteWithPriceItems(Mockito.anyString())).thenReturn(quote);
        Mockito.when(quoteService.registerNewWork(Mockito.any(Quote.class), Mockito.any(QuotePriceItem.class),
            Mockito.any(QuotePriceItem.class), Mockito.any(Date.class), Mockito.anyDouble(), Mockito.anyString(),
            Mockito.anyString(), Mockito.anyString(), Mockito.any(BigDecimal.class))).thenReturn("workId-" + quoteId);
        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.anyString()))
            .thenReturn(new SAPMaterial("test", broad, broad.getDefaultWbs(), "test description", "100",
                SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, new Date(),
                new Date(), Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                broad.getSalesOrganization()));
        TemplateEngine templateEngine = new TemplateEngine();

        templateEngine.postConstruct();
        billingEjb =
            new BillingEjb(priceListCache, billingSessionDao, null, null, null, AppConfig.produce(Deployment.DEV),
                SapConfig.produce(Deployment.DEV), mockEmailSender, templateEngine, bspUserList,
                    mockProductPriceCache);
        billingAdaptor = new BillingAdaptor(billingEjb, priceListCache, quoteService, billingSessionAccessEjb,
            sapService, productPriceCache, accessControlEjb);
        billingAdaptor.setProductOrderEjb(productOrderEjb);
    }

    @DataProvider
    public Object [][] sapOrQuoteProvider() {
        return new Object[][] {
                new Object[]{ProductOrder.QuoteSourceType.SAP_SOURCE},
                new Object[]{ProductOrder.QuoteSourceType.QUOTE_SERVER}
        };
    }

    @Test(dataProvider = "sapOrQuoteProvider")
    public void testCreateBillingCreditRequestDeliveryInvoiceCreated(ProductOrder.QuoteSourceType quoteSourceType)
            throws Exception {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();

        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            pdo.setQuoteId("00029338");
            Mockito.when(mockSapClient.createReturnOrder(Mockito.any(SAPReturnOrder.class)))
                .thenReturn(RETURN_ORDER_ID);
        }

        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            SapQuote sapQuote = TestUtils.buildTestSapQuote(pdo.getQuoteId(), 10000d, 100000d,
                    pdo, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED, "GP01");
            Mockito.when(mockSapClient.createDeliveryDocument(Mockito.any(SAPDeliveryDocument.class)))
                .thenReturn(DELIVERY_DOCUMENT);
            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenReturn(sapQuote);
            setupMocksWhichThrowExceptionsWhenQuoteServerIsCalled();
        } else {
            setupMocksWhichThrowExceptionsWhenSAPCalled();
        }

        HashMap<ProductOrderSample, Pair<ProductLedgerIndex, Double>> billingMap = new HashMap<>();
        billingMap.put(pdoSample, Pair.of(ProductLedgerIndex.create(pdo.getProduct(),priceItem, pdo.hasSapQuote()), qtyPositiveTwo));

        List<BillingEjb.BillingResult> billingResults = bill(billingMap);
        validateBillingResults(pdoSample, billingResults, qtyPositiveTwo);

        billingMap.clear();
        billingMap.put(pdoSample,
                Pair.of(ProductLedgerIndex.create(pdo.getProduct(),priceItem, pdo.hasSapQuote()), qtyNegativeTwo));
        billingResults = bill(billingMap);
        validateBillingResults(pdoSample, billingResults, 0);

        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.verify(mockSapClient, Mockito.times(1)).createReturnOrder(Mockito.any(SAPReturnOrder.class));
        } else {
            Mockito.verify(mockSapClient, Mockito.never()).createReturnOrder(Mockito.any(SAPReturnOrder.class));
        }
    }

    @Test(dataProvider = "sapOrQuoteProvider")
    public void testCreateBillingCreditRequestDeliveryInvoiceNotCreated(ProductOrder.QuoteSourceType quoteSourceType)
            throws Exception {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();

        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            pdo.setQuoteId("00029338");
                Mockito.when(mockSapClient.createReturnOrder(Mockito.any(SAPReturnOrder.class)))
                    .thenThrow(new SAPIntegrationException(
                        "Validating return from SAP yielded E--Invoice not found against the entered delivery"));
        }

        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            SapQuote sapQuote = TestUtils.buildTestSapQuote(pdo.getQuoteId(), 10000d, 100000d,
                    pdo, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED, "GP01");
            Mockito.when(mockSapClient.createDeliveryDocument(Mockito.any(SAPDeliveryDocument.class)))
                .thenReturn(DELIVERY_DOCUMENT);
            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenReturn(sapQuote);

            setupMocksWhichThrowExceptionsWhenQuoteServerIsCalled();

        } else {
            setupMocksWhichThrowExceptionsWhenSAPCalled();
        }

        HashMap<ProductOrderSample, Pair<ProductLedgerIndex, Double>> billingMap = new HashMap<>();
        billingMap.put(pdoSample, Pair.of(ProductLedgerIndex.create(pdo.getProduct(),priceItem, pdo.hasSapQuote()), qtyPositiveTwo));

        List<BillingEjb.BillingResult> billingResults = bill(billingMap);
        validateBillingResults(pdoSample, billingResults, qtyPositiveTwo);

        billingMap.clear();
        billingMap.put(pdoSample,
                Pair.of(ProductLedgerIndex.create(pdo.getProduct(),priceItem, pdo.hasSapQuote()), qtyNegativeTwo));
        try {
            billingResults = bill(billingMap);
        } catch (Exception e) {
            if (quoteSourceType == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
                Assert.fail("Exception should not have been thrown in this case.");
            }
            Mockito.verify(mockEmailSender, Mockito.times(1))
                .sendHtmlEmail(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean());
        }

        if (quoteSourceType != ProductOrder.QuoteSourceType.SAP_SOURCE) {
            validateBillingResults(pdoSample, billingResults, 0);
        }

        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.verify(mockSapClient, Mockito.times(1)).createReturnOrder(Mockito.any(SAPReturnOrder.class));
        } else {
            Mockito.verify(mockSapClient, Mockito.never()).createReturnOrder(Mockito.any(SAPReturnOrder.class));
        }
    }

    @Test(dataProvider = "sapOrQuoteProvider")
    public void testCreateBillingCreditRequestNoFunding(ProductOrder.QuoteSourceType quoteSourceType)
            throws Exception {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();
        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            pdo.setQuoteId("99339288");
        }

        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            SapQuote sapQuote = TestUtils.buildTestSapQuote(pdo.getQuoteId(), 10000d, 100000d,
                    pdo, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED, "GP01");
            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenReturn(sapQuote);
            Mockito.when(mockSapClient.createDeliveryDocument(Mockito.any(SAPDeliveryDocument.class))).thenReturn(DELIVERY_DOCUMENT);
            Mockito.when(mockSapClient.createReturnOrder(Mockito.any(SAPReturnOrder.class)))
                    .thenAnswer((Answer<String>) invocationOnMock -> {
                        final Object[] arguments = invocationOnMock.getArguments();
                        SAPReturnOrder testReturnOrder = (SAPReturnOrder) arguments[0];
                        assertThat(testReturnOrder.getDeliveryId(), is(notNullValue()));
                        assertThat(testReturnOrder.getDeliveryId(), is(equalTo(DELIVERY_DOCUMENT)));

                        for (SAPOrderItem deliveryItem : testReturnOrder.getDeliveryItems()) {
                            assertThat(deliveryItem.getItemQuantity(),is(equalTo(BigDecimal.valueOf(2d))));

                            for (LedgerEntry ledgerItem : pdoSample.getLedgerItems()) {

                                final BillingSession billingSession = ledgerItem.getBillingSession();
                                final List<QuoteImportItem> unBilledQuoteImportItems =
                                        billingSession.getUnBilledQuoteImportItems(priceListCache);
                                for (QuoteImportItem unBilledQuoteImportItem : unBilledQuoteImportItems) {

                                    assertThat(deliveryItem.getProductIdentifier(), is(equalTo(unBilledQuoteImportItem.getProduct().getPartNumber())));
                                }
                            }
                        }
                        return RETURN_ORDER_ID;
                    });


            setupMocksWhichThrowExceptionsWhenQuoteServerIsCalled();

        } else {
            setupMocksWhichThrowExceptionsWhenSAPCalled();
        }

        HashMap<ProductOrderSample, Pair<ProductLedgerIndex, Double>> billingMap = new HashMap<>();
        billingMap.put(pdoSample, Pair.of(ProductLedgerIndex.create(pdo.getProduct(),priceItem,pdo.hasSapQuote()), qtyPositiveTwo));

        List<BillingEjb.BillingResult> billingResults = bill(billingMap);
        validateBillingResults(pdoSample, billingResults, qtyPositiveTwo);

        Quote quote = null;
        if (quoteSourceType == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            quote = pdo.getQuote(quoteService);
            java.sql.Date expirationDate = java.sql.Date.valueOf(LocalDate.now().minus(1, ChronoUnit.MONTHS));
            quote.getFunding().forEach(funding -> {
                funding.setFundingType(Funding.FUNDS_RESERVATION);
                funding.setGrantEndDate(expirationDate);
            });
            assertThat(quote.isFunded(), is(false));
        }


        billingMap.clear();
        billingMap.put(pdoSample, Pair.of(ProductLedgerIndex.create(pdo.getProduct(),priceItem,pdo.hasSapQuote()), qtyNegativeTwo));
        billingResults = bill(billingMap);
        validateBillingResults(pdoSample, billingResults, 0);

        Mockito.verify(mockEmailSender, Mockito.never()).sendHtmlEmail(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.anyBoolean());
        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.verify(mockSapClient, Mockito.times(1)).createReturnOrder(Mockito.any(SAPReturnOrder.class));
        }

    }

    @Test(dataProvider = "sapOrQuoteProvider")
    public void testNegativeBilling(ProductOrder.QuoteSourceType quoteSourceType) throws Exception {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();
        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            pdo.setQuoteId("99339288");
        }

        SapQuote sapQuote = TestUtils.buildTestSapQuote(pdo.getQuoteId(), 10000d, 100000d,
                pdo, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED, "GP01");

        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenReturn(sapQuote);
            setupMocksWhichThrowExceptionsWhenQuoteServerIsCalled();
        } else {
            setupMocksWhichThrowExceptionsWhenSAPCalled();
        }

        HashMap<ProductOrderSample, Pair<ProductLedgerIndex, Double>> billingMap = new HashMap<>();
        billingMap.put(pdoSample, Pair.of(ProductLedgerIndex.create(pdo.getProduct(),priceItem,pdo.hasSapQuote()), qtyNegativeTwo));
        List<BillingEjb.BillingResult> billingResults = bill(billingMap);

        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            billingResults.forEach(
                billingResult -> assertThat(billingResult.getErrorMessage(), endsWith(BillingAdaptor.NEGATIVE_BILL_ERROR)));
        }

        Mockito.verify(mockSapClient, Mockito.never()).createReturnOrder(Mockito.any(SAPReturnOrder.class));
        Mockito.verify(mockEmailSender, Mockito.never())
            .sendHtmlEmail(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test(dataProvider = "sapOrQuoteProvider")
    public void testPositiveBilling(ProductOrder.QuoteSourceType quoteSourceType) throws Exception {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();
        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            pdo.setQuoteId("99339288");
        }

        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            SapQuote sapQuote = TestUtils.buildTestSapQuote(pdo.getQuoteId(), 10000d, 100000d,
                    pdo, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED, "GP01");

            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenReturn(sapQuote);
            Mockito.when(mockSapClient.createDeliveryDocument(Mockito.any(SAPDeliveryDocument.class))).thenReturn(DELIVERY_DOCUMENT);
            Mockito.when(mockSapClient.createReturnOrder(Mockito.any(SAPReturnOrder.class))).thenReturn(RETURN_ORDER_ID);

            setupMocksWhichThrowExceptionsWhenQuoteServerIsCalled();

        } else {
            setupMocksWhichThrowExceptionsWhenSAPCalled();
        }

        HashMap<ProductOrderSample, Pair<ProductLedgerIndex, Double>> billingMap = new HashMap<>();
        billingMap.put(pdoSample, Pair.of(ProductLedgerIndex.create(pdo.getProduct(),priceItem, pdo.hasSapQuote()), qtyPositiveTwo));
        List<BillingEjb.BillingResult> billingResults = bill(billingMap);
        billingResults.forEach(
            billingResult -> {
                assertThat(billingResult.getErrorMessage(), blankOrNullString());
                assertThat(billingResult.getSapBillingId(), pdo.hasSapQuote()?not(blankOrNullString()):is(blankOrNullString()));
            });

        Mockito.verify(mockSapClient, Mockito.never()).createReturnOrder(Mockito.any(SAPReturnOrder.class));
        Mockito.verify(mockEmailSender, Mockito.never())
            .sendHtmlEmail(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    @Test(dataProvider = "sapOrQuoteProvider")
    public void testMoreNegativeThanPositiveBillingPositiveFirst(ProductOrder.QuoteSourceType quoteSourceType)
            throws Exception {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();
        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            pdo.setQuoteId("99339288");
        }

        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            SapQuote sapQuote = TestUtils.buildTestSapQuote(pdo.getQuoteId(), 10000d, 100000d,
                    pdo, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED, "GP01");

            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenReturn(sapQuote);
            Mockito.when(mockSapClient.createDeliveryDocument(Mockito.any(SAPDeliveryDocument.class))).thenReturn(DELIVERY_DOCUMENT);
            Mockito.when(mockSapClient.createReturnOrder(Mockito.any(SAPReturnOrder.class))).thenReturn(RETURN_ORDER_ID);
            setupMocksWhichThrowExceptionsWhenQuoteServerIsCalled();
        } else {
            setupMocksWhichThrowExceptionsWhenSAPCalled();

        }

        HashMap<ProductOrderSample, Pair<ProductLedgerIndex, Double>> billingMap = new HashMap<>();
        billingMap.put(pdoSample, Pair.of(ProductLedgerIndex.create(pdo.getProduct(),priceItem,pdo.hasSapQuote()), 1d));

        List<BillingEjb.BillingResult> billingResults = bill(billingMap);
        validateBillingResults(pdoSample, billingResults, 1);

        billingMap.clear();
        billingMap.put(pdoSample, Pair.of(ProductLedgerIndex.create(pdo.getProduct(),priceItem,pdo.hasSapQuote()), qtyNegativeTwo));
        billingResults = bill(billingMap);

        if (quoteSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            billingResults.forEach(
                billingResult -> assertThat(billingResult.getErrorMessage(), endsWith(BillingAdaptor.NEGATIVE_BILL_ERROR)));
        }

        Mockito.verify(mockSapClient, Mockito.never()).createReturnOrder(Mockito.any(SAPReturnOrder.class));
        Mockito.verify(mockEmailSender, Mockito.never())
            .sendHtmlEmail(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    private void validateBillingResults(ProductOrderSample sample, List<BillingEjb.BillingResult> results,
                                        double quantity) {
        results.forEach(billingResult -> {
            assertThat(billingResult.isError(), is(false));
            assertThat(billingResult.getSapBillingId(),
                sample.getProductOrder().hasSapQuote() ? notNullValue() : nullValue());
        });
        Double totalBilled = sample.getLedgerItems().stream()
            .filter(LedgerEntry.IS_SUCCESSFULLY_BILLED).mapToDouble(LedgerEntry::getQuantity).sum();
        assertThat(totalBilled, equalTo(quantity));
    }

    private List<BillingEjb.BillingResult> bill(Map<ProductOrderSample, Pair<ProductLedgerIndex, Double>> samplePairMap) {
        final Date date = new Date();
        samplePairMap.forEach((productOrderSample, pricItemQtyPair) ->  {
            if(productOrderSample.getProductOrder().hasSapQuote()) {
                productOrderSample.addLedgerItem(date, productOrderSample.getProductOrder().getProduct(),
                        pricItemQtyPair.getValue(), false);
            } else {
                productOrderSample.addLedgerItem(date, pricItemQtyPair.getKey().getPriceItem(), pricItemQtyPair.getValue());
            }
        });
        Set<LedgerEntry> ledgerItems = new HashSet<>();
        samplePairMap.keySet().stream().map(ProductOrderSample::getLedgerItems).forEach(ledgerItems::addAll);

        BillingSession billingSession = new BillingSession(1L, ledgerItems);
        Mockito.reset(billingSessionAccessEjb);
        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString())).thenReturn(billingSession);
        return billingAdaptor.billSessionItems("url", billingSession.getBusinessKey());
    }

    public void testManyPositiveLedgersOneNegativeLedgerCreatesTwoBillingCredits() throws Exception {
        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        SapQuote sapQuote = TestUtils.buildTestSapQuote("1234", 10000d, 100000d,
            pdo, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED, "GP01");
        pdo.setQuoteId(sapQuote.getQuoteHeader().getQuoteNumber());
        SapIntegrationService sapService = Mockito.mock(SapIntegrationService.class);
        Mockito.when(sapService.creditDelivery(Mockito.any(BillingCredit.class)))
            .thenThrow(new SAPIntegrationException(BillingAdaptor.INVOICE_NOT_FOUND));

        Date sameDate = new Date();
        List<ProductOrderSample> samples = pdo.getSamples();
        for (int i = 0; i < samples.size(); i++) {
            Date uniqueDate =
                Date.from(LocalDate.now().minusDays(i + 1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            ProductOrderSample pdoSample = samples.get(i);
            long billQuantity = 1L;

            // for all the samples in the pdo, a ledger is created with unique dates. This will create multiple quoteItems
            LedgerEntry ledgerEntry = new LedgerEntry(pdoSample, pdo.getProduct(), uniqueDate, billQuantity);
            ledgerEntry.setSapDeliveryDocumentId(String.valueOf(uniqueDate.getTime()));
            ledgerEntry.setBillingMessage(BillingSession.SUCCESS);
            pdoSample.getLedgerItems().add(ledgerEntry);

            // for all the samples in the pdo, a ledger for a return is created with the same date. This will create one quoteItem.
            double creditQty = Math.negateExact(billQuantity);
            ledgerEntry = new LedgerEntry(pdoSample, pdo.getProduct(), sameDate, creditQty);
            pdoSample.getLedgerItems().add(ledgerEntry);
            ledgerEntries.add(ledgerEntry);
        }
        BillingEjb billingEjb = Mockito.mock(BillingEjb.class);
        BillingAdaptor billingAdaptor = new BillingAdaptor(billingEjb, null, null, null, sapService, null, null);

        QuoteImportItem quoteImportItem =
            new QuoteImportItem(quotePriceItem.getId(), priceItem, "1234", ledgerEntries, new Date(), pdo.getProduct(),
                pdo);
        Collection<BillingCredit> billingCredits = billingAdaptor.handleBillingCredit(quoteImportItem);
        assertThat(billingCredits, hasSize(2));

        List<BillingCredit> billingCreditsForItem = BillingAdaptor.findCreditsForEmail(billingCredits);
        assertThat(billingCreditsForItem, hasSize(2));

        List<BillingCredit.LineItem> returnLineItems =
            billingCredits.stream().flatMap((BillingCredit billingCredit) -> billingCredit.getReturnLines().stream())
                .collect(Collectors.toList());
        assertThat(returnLineItems, hasSize(2));
        assertThat(Math.abs(quoteImportItem.getQuantity()),
            equalTo(returnLineItems.stream().mapToDouble(lineItem -> lineItem.getQuantity().doubleValue()).sum()));
    }

    public void testOnePositiveLedgerManyNegativeLedgersCreateOneBillingCredit() throws Exception {
        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        SapQuote sapQuote = TestUtils.buildTestSapQuote("1234", 10000d, 100000d,
            pdo, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED, "GP01");
        pdo.setQuoteId(sapQuote.getQuoteHeader().getQuoteNumber());
        SapIntegrationService sapService = Mockito.mock(SapIntegrationService.class);
        Mockito.when(sapService.creditDelivery(Mockito.any(BillingCredit.class)))
            .thenThrow(new SAPIntegrationException(BillingAdaptor.INVOICE_NOT_FOUND));

        Date sameDate = new Date();
        List<ProductOrderSample> samples = pdo.getSamples();
        for (int i = 0; i < samples.size(); i++) {
            Date uniqueDate =
                Date.from(LocalDate.now().minusDays(i + 1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

            ProductOrderSample pdoSample = samples.get(i);
            long billQuantity = 1L;

            // for each sample in the pdo, a ledger is created with the same date. This will create one quoteItem.
            LedgerEntry ledgerEntry = new LedgerEntry(pdoSample, pdo.getProduct(), sameDate, billQuantity);
            ledgerEntry.setSapDeliveryDocumentId(String.valueOf(sameDate.getTime()));
            ledgerEntry.setBillingMessage(BillingSession.SUCCESS);
            pdoSample.getLedgerItems().add(ledgerEntry);

            // for each the sample in the pdo, a ledger for a return is created with unique dates.
            // This will create multiple quoteItems
            double creditQty = Math.negateExact(billQuantity);
            ledgerEntry = new LedgerEntry(pdoSample, pdo.getProduct(), uniqueDate, creditQty);
            pdoSample.getLedgerItems().add(ledgerEntry);
            ledgerEntries.add(ledgerEntry);
        }
        BillingEjb billingEjb = Mockito.mock(BillingEjb.class);
        BillingAdaptor billingAdaptor = new BillingAdaptor(billingEjb, null, null, null, sapService, null, null);

        QuoteImportItem quoteImportItem =
            new QuoteImportItem(quotePriceItem.getId(), priceItem, "1234", ledgerEntries, new Date(), pdo.getProduct(),
                pdo);
        Collection<BillingCredit> billingCredits = billingAdaptor.handleBillingCredit(quoteImportItem);
        assertThat(billingCredits, hasSize(1));
        List<BillingCredit> billingCreditsForItem = BillingAdaptor.findCreditsForEmail(billingCredits);
        assertThat(billingCreditsForItem, hasSize(1));

        List<BillingCredit.LineItem> returnLineItems =
            billingCredits.stream().flatMap((BillingCredit billingCredit) -> billingCredit.getReturnLines().stream())
                .collect(Collectors.toList());
        assertThat(returnLineItems, hasSize(2));
        assertThat(Math.abs(quoteImportItem.getQuantity()),
            equalTo(returnLineItems.stream().mapToDouble(lineItem -> lineItem.getQuantity().doubleValue()).sum()));
    }

    public void testPositiveQtyCredit() throws Exception {
        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        SapQuote sapQuote = TestUtils.buildTestSapQuote("1234", 10000d, 100000d,
            pdo, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED, "GP01");
        pdo.setQuoteId(sapQuote.getQuoteHeader().getQuoteNumber());
        SapIntegrationService sapService = Mockito.mock(SapIntegrationService.class);
        Mockito.when(sapService.creditDelivery(Mockito.any(BillingCredit.class)))
            .thenThrow(new SAPIntegrationException(BillingAdaptor.INVOICE_NOT_FOUND));

        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();
        long billQuantity = 1L;
        Date billDate = new Date();

        // create a ledgerEntry to simulate a billed sample;
        LedgerEntry ledgerEntry = new LedgerEntry(pdoSample, pdo.getProduct(), billDate, billQuantity);
        ledgerEntry.setSapDeliveryDocumentId(String.valueOf(billDate.getTime()));
        ledgerEntry.setBillingMessage(BillingSession.SUCCESS);
        pdoSample.getLedgerItems().add(ledgerEntry);

        // create a billingCredit with a positive value. the UI doesn't allow this but it should be tested.
        double creditQty = 1L;
        ledgerEntry = new LedgerEntry(pdoSample, pdo.getProduct(), new Date(),  creditQty);
        pdoSample.getLedgerItems().add(ledgerEntry);
        ledgerEntries.add(ledgerEntry);
        BillingEjb billingEjb = Mockito.mock(BillingEjb.class);
        BillingAdaptor billingAdaptor = new BillingAdaptor(billingEjb, null, null, null, sapService, null, null);

        QuoteImportItem quoteImportItem =
            new QuoteImportItem(quotePriceItem.getId(), priceItem, "1234", ledgerEntries, new Date(), pdo.getProduct(),
                pdo);
        try {
            billingAdaptor.handleBillingCredit(quoteImportItem);
            Assert.fail("Positive credits aren't a thing. An error should have been thrown.");
        } catch (Exception e) {
            assertThat(e.getLocalizedMessage(), equalTo(BillingAdaptor.POSITIVE_QTY_ERROR_MESSAGE));
        }
    }

    public void testBillingCreditNotSapOrder() throws Exception {
        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        pdo.setQuoteId("ABCDE");

        SapIntegrationService sapService = Mockito.mock(SapIntegrationService.class);
        Mockito.when(sapService.creditDelivery(Mockito.any(BillingCredit.class)))
            .thenThrow(new SAPIntegrationException(BillingAdaptor.INVOICE_NOT_FOUND));

        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();
        long billQuantity = 1L;
        Date billDate = new Date();

        // create a ledgerEntry to simulate a billed sample;
        LedgerEntry ledgerEntry = new LedgerEntry(pdoSample, pdo.getProduct(), billDate, billQuantity);
        ledgerEntry.setSapDeliveryDocumentId(String.valueOf(billDate.getTime()));
        ledgerEntry.setBillingMessage(BillingSession.SUCCESS);
        pdoSample.getLedgerItems().add(ledgerEntry);

        // create a billingCredit with a positive value. the UI doesn't allow this but it should be tested.
        double creditQty = -1L;
        ledgerEntry = new LedgerEntry(pdoSample, pdo.getProduct(), new Date(),  creditQty);
        pdoSample.getLedgerItems().add(ledgerEntry);
        ledgerEntries.add(ledgerEntry);
        BillingEjb billingEjb = Mockito.mock(BillingEjb.class);
        BillingAdaptor billingAdaptor = new BillingAdaptor(billingEjb, null, null, null, sapService, null, null);

        QuoteImportItem quoteImportItem =
            new QuoteImportItem(quotePriceItem.getId(), priceItem, "1234", ledgerEntries, new Date(), pdo.getProduct(),
                pdo);
        try {
            billingAdaptor.handleBillingCredit(quoteImportItem);
            Assert.fail("Positive credits aren't a thing. An error should have been thrown.");
        } catch (Exception e) {
            assertThat(e.getLocalizedMessage(), equalTo(BillingAdaptor.NON_SAP_ITEM_ERROR_MESSAGE));
        }
    }

    private void resetMocks(){
        Mockito.reset(mockEmailSender, billingSessionDao, priceListCache, quoteService, productOrderEjb,
            billingSessionAccessEjb, productPriceCache, accessControlEjb);
    }

    public void setupMocksWhichThrowExceptionsWhenQuoteServerIsCalled() throws QuoteServerException, QuoteNotFoundException {
        Mockito.when(priceListCache.getQuotePriceItems())
            .thenThrow(new RuntimeException("Quote server should not be called in this case"));
        Mockito.when(priceListCache.findByKeyFields(priceItem))
            .thenThrow(new RuntimeException("Quote server should not be called in this case"));
        Mockito.when(quoteService.getPriceItemsForDate(Mockito.anyListOf(QuoteImportItem.class)))
            .thenThrow(new RuntimeException("Quote server should not be called in this case"));
        Mockito.when(quoteService.getQuoteByAlphaId(Mockito.anyString()))
            .thenThrow(new RuntimeException("Quote server should not be called in this case"));
        Mockito.when(quoteService.getQuoteWithPriceItems(Mockito.anyString()))
            .thenThrow(new RuntimeException("Quote server should not be called in this case"));
        Mockito.when(quoteService.registerNewWork(Mockito.any(Quote.class), Mockito.any(QuotePriceItem.class),
            Mockito.any(QuotePriceItem.class), Mockito.any(Date.class), Mockito.anyDouble(), Mockito.anyString(),
            Mockito.anyString(), Mockito.anyString(), Mockito.any(BigDecimal.class)))
            .thenThrow(new RuntimeException("Quote server should not be called in this case"));
    }

    public void setupMocksWhichThrowExceptionsWhenSAPCalled() throws SAPIntegrationException {
        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.anyString()))
                .thenThrow(new RuntimeException("SAP Should not be called in this case"));
        Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                .thenThrow(new RuntimeException("SAP Should not be called in this case"));
        Mockito.when(mockSapClient.createDeliveryDocument(Mockito.any(SAPDeliveryDocument.class)))
                .thenThrow(new RuntimeException("SAP Should not be called in this case"));
        Mockito.when(mockSapClient.createReturnOrder(Mockito.any(SAPReturnOrder.class)))
                .thenThrow(new RuntimeException("SAP Should not be called in this case"));
    }

    public void testMergeSamePart() {
        List<SAPOrderItem> orderItems = Arrays.asList(
            new SAPOrderItem("P-EX", BigDecimal.valueOf(1D)),
            new SAPOrderItem("P-EX", BigDecimal.valueOf(1D)));

        List<SAPOrderItem> merged = BillingCredit.LineItem.merge(orderItems);
        assertThat(merged, hasSize(1));
        assertThat(merged.iterator().next().getItemQuantity(), equalTo(BigDecimal.valueOf(2D)));
    }
    public void testMergeDifferentPart() {
        List<SAPOrderItem> orderItems = Arrays.asList(
            new SAPOrderItem("P-EX", BigDecimal.valueOf(1D)),
            new SAPOrderItem("P-E2", BigDecimal.valueOf(1D)));

        List<SAPOrderItem> merged = BillingCredit.LineItem.merge(orderItems);
        assertThat(merged, hasSize(2));
        merged.forEach(i -> assertThat(i.getItemQuantity(), equalTo(BigDecimal.valueOf(1D))));

    }
}
