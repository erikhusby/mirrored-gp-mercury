/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingAdaptor;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingCredit;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingSessionAccessEjb;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.sap.entity.SAPDeliveryDocument;
import org.broadinstitute.sap.entity.SAPReturnOrder;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@Test(groups = TestGroups.DATABASE_FREE)
public class SapIntegrationServiceCreditDeliveryDbFreeTest {
    public static final String QUOTE_ID = "2700103";
    public static final String RETURN_PREFIX = "RETURN_OF_";
    private BSPUserList bspUserList = Mockito.mock(BSPUserList.class);
    private PriceListCache priceListCache = Mockito.mock(PriceListCache.class);
    private SAPProductPriceCache productPriceCache = Mockito.mock(SAPProductPriceCache.class);
    private SAPAccessControlEjb accessControlEjb = Mockito.mock(SAPAccessControlEjb.class);
    private BillingAdaptor billingAdaptor;
    private SapIntegrationClientImpl sapIntegrationClient = Mockito.mock(SapIntegrationClientImpl.class);
    private SapIntegrationServiceImpl sapService;
    private BillingSessionAccessEjb billingSessionAccessEjb;

    @BeforeMethod
    public void setUp() throws SAPIntegrationException {
        sapService = new SapIntegrationServiceImpl(SapConfig.produce(Deployment.DEV), bspUserList, priceListCache,
            productPriceCache, accessControlEjb);

        AtomicInteger counter = new AtomicInteger(1);
        Mockito.when(sapIntegrationClient.createDeliveryDocument(Mockito.any(SAPDeliveryDocument.class)))
            .then((Answer<String>) invocation ->
                String.format("%d%d", System.currentTimeMillis(), counter.getAndIncrement()));

        Mockito.when(sapIntegrationClient.createReturnOrder(Mockito.any(SAPReturnOrder.class))).thenAnswer(
            (Answer<String>) invocation -> {
                SAPReturnOrder sapReturnOrder = (SAPReturnOrder) invocation.getArguments()[0];
                if (sapReturnOrder == null) {
                    return null;
                }
                return "RETURN_OF_" + sapReturnOrder.getDeliveryId();
            });
        sapService.setWrappedClient(sapIntegrationClient);

        BillingSessionDao billingSessionDao = Mockito.mock(BillingSessionDao.class);
        EmailSender mockEmailSender = Mockito.mock(EmailSender.class);
        SAPProductPriceCache mockProductPriceCache = Mockito.mock(SAPProductPriceCache.class);
        BillingEjb billingEjb =
            new BillingEjb(priceListCache, billingSessionDao, null, null, null, AppConfig.produce(Deployment.DEV),
                SapConfig.produce(Deployment.DEV), mockEmailSender, bspUserList,
                mockProductPriceCache);
        QuoteService quoteService = Mockito.mock(QuoteService.class);
        billingSessionAccessEjb = Mockito.mock(BillingSessionAccessEjb.class);

        billingAdaptor = new BillingAdaptor(billingEjb, priceListCache, quoteService, billingSessionAccessEjb,
            sapService, productPriceCache, accessControlEjb);


        ProductOrderEjb productOrderEjb = Mockito.mock(ProductOrderEjb.class);
        billingAdaptor.setProductOrderEjb(productOrderEjb);
    }

    public void testCreditDeliveryMultipleDeliveryDocuments() throws Exception {
        SapIntegrationServiceImpl sapService =
            new SapIntegrationServiceImpl(SapConfig.produce(Deployment.DEV), bspUserList, priceListCache,
                productPriceCache, accessControlEjb);
        SapIntegrationClientImpl sapIntegrationClient = Mockito.mock(SapIntegrationClientImpl.class);

        Mockito.when(sapIntegrationClient.createReturnOrder(Mockito.any(SAPReturnOrder.class))).thenAnswer(
            (Answer<String>) invocation -> {
                SAPReturnOrder sapReturnOrder = (SAPReturnOrder) invocation.getArguments()[0];
                return "RETURN_OF_" + sapReturnOrder.getDeliveryId();
            });
        sapService.setWrappedClient(sapIntegrationClient);

        ProductOrder productOrder = ProductOrderTestFactory.buildWholeGenomeProductOrder(100);
        Product product = productOrder.getProduct();

        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        List<Integer> quantities = Arrays.asList(1, 3, 5, 7, 11, 20);
        quantities.forEach(i -> {
            String deliveryDocumentId = String.format("00%d", i);
            ProductOrderSample productOrderSample = productOrder.getSamples().get(i);
            BigDecimal quantity = BigDecimal.valueOf(i).add(BigDecimal.valueOf(i));
            LedgerEntry ledger = createLedgerEntry(productOrderSample, product, deliveryDocumentId, quantity);
            ledger.setBillingMessage(BillingSession.SUCCESS);
            ledgerEntries.add(ledger);
        });

        BillingSession billingSession = new BillingSession(System.currentTimeMillis(), new HashSet<>(ledgerEntries));
        billingSession.getUnBilledQuoteImportItems(priceListCache);

        ledgerEntries.clear();

        quantities.forEach(i -> {
            String deliveryDocumentId = String.format("00%d", i);
            ProductOrderSample productOrderSample = productOrder.getSamples().get(i);
            BigDecimal quantity = BigDecimal.valueOf(i).negate();
            LedgerEntry ledger = createLedgerEntry(productOrderSample, product, deliveryDocumentId, quantity);
            ledgerEntries.add(ledger);
        });
        billingSession = new BillingSession(System.currentTimeMillis(), new HashSet<>(ledgerEntries));
        billingSession.getUnBilledQuoteImportItems(priceListCache);

        QuoteImportItem quoteImportItem =
            new QuoteImportItem(QUOTE_ID, null, null, ledgerEntries, new Date(), product, productOrder);

        Collection<BillingCredit> billingReturns = new ArrayList<>();
        Collection<BillingCredit> billingCredits = BillingCredit.setupSapCredits(quoteImportItem);
        for (BillingCredit billingReturn : billingCredits) {
            billingReturn.setReturnOrderId(sapService.creditDelivery(billingReturn));
            billingReturns.add(billingReturn);
        }
        assertThat(billingReturns.stream().map(BillingCredit::getReturnOrderId).collect(Collectors.toList()),
            Matchers.everyItem(startsWith(RETURN_PREFIX)));

        int invocationCount = (int) billingCredits.stream().map(BillingCredit::getReturnLines).count();
        Mockito.verify(sapIntegrationClient, Mockito.times(invocationCount))
            .createReturnOrder(Mockito.any(SAPReturnOrder.class));
    }

    public void testCreditDeliveryMultipleDeliveryTestOrder() throws Exception {
        ProductOrder productOrder = ProductOrderTestFactory.buildWholeGenomeProductOrder(100);
        String sapQuoteId="1234";
        SapQuote sapQuote = TestUtils
            .buildTestSapQuote(sapQuoteId, BigDecimal.valueOf(0), BigDecimal.valueOf(0), productOrder,
                TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization());

        Mockito.when(sapIntegrationClient.findQuoteDetails(Mockito.anyString())).thenReturn(sapQuote);

        Product product = productOrder.getProduct();
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrder.addSapOrderDetail(new SapOrderDetail("1234", 100, "1234", "GP01"));
        ProductOrderSample sample1 = productOrder.getSamples().get(0);
        ProductOrderSample sample2 = productOrder.getSamples().get(1);
        ProductOrderSample sample3 = productOrder.getSamples().get(2);

        // make sure they are actually unique samples
        assertThat(Stream.of(sample1, sample2, sample3).collect(Collectors.toSet()).size(), is(3));

        BigDecimal TWO = BigDecimal.valueOf(2);
        BigDecimal THREE = BigDecimal.valueOf(3);
        BigDecimal FOUR = BigDecimal.valueOf(4);

        // Bill QTY 1 for one sample1 and QTY 1 for sample2;
        Set<LedgerEntry> session1Ledgers = new HashSet<>();
        LedgerEntry session1Ledger1 = createLedgerEntry(sample1, product, null, BigDecimal.ONE);
        session1Ledgers.add(session1Ledger1);
        LedgerEntry session1Ledger2 = createLedgerEntry(sample2, product, null, BigDecimal.ONE);
        session1Ledgers.add(session1Ledger2);
        BillingSession billingSession = new BillingSession(System.currentTimeMillis(), session1Ledgers);
        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString())).thenReturn(billingSession);
        List<BillingEjb.BillingResult> billingResults =
            billingAdaptor.billSessionItems(StringUtils.EMPTY, StringUtils.EMPTY);
        verifyBillingResults(billingResults);
        verifyRemainingQuantity(billingResults, TWO);

        LedgerEntry session2Ledger = createLedgerEntry(sample1, product, null, TWO);
        billingSession = new BillingSession(System.currentTimeMillis(), Collections.singleton(session2Ledger));
        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString())).thenReturn(billingSession);
        billingResults = billingAdaptor.billSessionItems(StringUtils.EMPTY, StringUtils.EMPTY);
        verifyBillingResults(billingResults);
        verifyRemainingQuantity(billingResults, FOUR);

        // Credit QTY 3.
        LedgerEntry session3Ledger = createLedgerEntry(sample1, product, null, THREE.negate());
        billingSession = new BillingSession(System.currentTimeMillis(), Collections.singleton(session3Ledger));
        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString())).thenReturn(billingSession);
        billingResults = billingAdaptor.billSessionItems(StringUtils.EMPTY, StringUtils.EMPTY);
        verifyBillingResults(billingResults);
        verifyRemainingQuantity(billingResults, BigDecimal.ONE);
        verifyReturnOrderId(billingResults);

        assertThat(session1Ledger1.calculateAvailableQuantity(), is(BigDecimal.ZERO));
        assertThat(session1Ledger2.calculateAvailableQuantity(), is(BigDecimal.ZERO));
        assertThat(session2Ledger.calculateAvailableQuantity(), is(BigDecimal.ONE));

        // Crediting QTY 2 should fail since there is only one available
        LedgerEntry session4Ledger = createLedgerEntry(sample1, product, null, TWO.negate());
        billingSession = new BillingSession(System.currentTimeMillis(), Collections.singleton(session4Ledger));
        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString())).thenReturn(billingSession);

        billingResults = billingAdaptor.billSessionItems(StringUtils.EMPTY, StringUtils.EMPTY);
        String billingError =
            billingResults.stream().map(BillingEjb.BillingResult::getErrorMessage).findFirst().orElse(null);
        assertThat(billingError, equalTo(BillingAdaptor.NEGATIVE_BILL_ERROR));
        try {
            verifyBillingResults(billingResults);
            Assert.fail("Should have failed since there were billing errors");
        } catch (AssertionError e) {
            verifyRemainingQuantity(billingResults, BigDecimal.ONE);
        }

        // No Credit QTY 1 which will succeed.
        session4Ledger.setQuantity(BigDecimal.ONE.negate());
        billingSession = new BillingSession(System.currentTimeMillis(), Collections.singleton(session4Ledger));
        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString())).thenReturn(billingSession);
        billingResults = billingAdaptor.billSessionItems(StringUtils.EMPTY, StringUtils.EMPTY);
        verifyBillingResults(billingResults);
        verifyRemainingQuantity(billingResults, BigDecimal.ZERO);
        verifyReturnOrderId(billingResults);

        // Bill QTY 2 for one sample3;
        LedgerEntry session5Ledger = createLedgerEntry(sample3, product, null, TWO);
        billingSession = new BillingSession(System.currentTimeMillis(), Collections.singleton(session5Ledger));
        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString())).thenReturn(billingSession);
        billingResults = billingAdaptor.billSessionItems(StringUtils.EMPTY, StringUtils.EMPTY);
        verifyBillingResults(billingResults);
        verifyRemainingQuantity(billingResults, TWO);

        // Bill QTY -1
        LedgerEntry session6Ledger = createLedgerEntry(sample3, product, null, BigDecimal.ONE.negate());
        billingSession = new BillingSession(System.currentTimeMillis(), Collections.singleton(session6Ledger));
        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString())).thenReturn(billingSession);
        billingResults = billingAdaptor.billSessionItems(StringUtils.EMPTY, StringUtils.EMPTY);
        verifyBillingResults(billingResults);
        verifyRemainingQuantity(billingResults, BigDecimal.ONE);
        verifyReturnOrderId(billingResults);

        // this ledger has already been billed so simply changing the value and re-billing will fail.
        session6Ledger.setQuantity(BigDecimal.ONE.negate());
        billingSession = new BillingSession(System.currentTimeMillis(), Collections.singleton(session6Ledger));
        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString())).thenReturn(billingSession);
        try {
            billingAdaptor.billSessionItems(StringUtils.EMPTY, StringUtils.EMPTY);
            Assert.fail("Should have failed with " + BillingEjb.NO_ITEMS_TO_BILL_ERROR_TEXT);
        } catch (Exception e) {
            assertThat(e.getMessage(), equalTo(BillingEjb.NO_ITEMS_TO_BILL_ERROR_TEXT));
        }

        // Bill QTY -1 again but on a new Ledger
        LedgerEntry session7Ledger = createLedgerEntry(sample3, product, null, BigDecimal.ONE.negate());
        billingSession = new BillingSession(System.currentTimeMillis(), Collections.singleton(session7Ledger));
        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString())).thenReturn(billingSession);
        billingResults = billingAdaptor.billSessionItems(StringUtils.EMPTY, StringUtils.EMPTY);
        verifyBillingResults(billingResults);
        verifyRemainingQuantity(billingResults, BigDecimal.ZERO);
        verifyReturnOrderId(billingResults);
    }

    private void verifyReturnOrderId(List<BillingEjb.BillingResult> billingResults) {
        billingResults.stream().flatMap(billingResult -> billingResult.getQuoteImportItem().getLedgerItems().stream())
            .forEach(ledgerEntry -> {
                assertThat(ledgerEntry.getSapReturnOrderId(),
                    equalTo(RETURN_PREFIX + ledgerEntry.getSapDeliveryDocumentId()));
            });
    }

    private void verifyRemainingQuantity(List<BillingEjb.BillingResult> billingResults, BigDecimal quantity) {
        Set<LedgerEntry> ledgers = billingResults.stream()
            .flatMap(billingResult -> billingResult.getQuoteImportItem().getPriorLedgersMatchingProduct().stream())
            .collect(Collectors.toSet());
        BigDecimal remaining =
            ledgers.stream().map(LedgerEntry::calculateAvailableQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(remaining, equalTo(quantity));
    }

    private void verifyBillingResults(List<BillingEjb.BillingResult> billingResults) {
        boolean successfullyBilled =
            Optional.of(billingResults.stream().allMatch(BillingEjb.BillingResult::isSuccessfullyBilled)).orElse(false);
        List<String> billingMessages =
            billingResults.stream().map(billingResult -> billingResult.getQuoteImportItem().getBillingMessage())
                .collect(Collectors.toList());
        assertThat(billingMessages.toString(), successfullyBilled, is(true));
        billingResults.stream().map(BillingEjb.BillingResult::getSapBillingId).findFirst()
            .orElseThrow(() -> new AssertionError("SAP Billing ID not found"));
    }

    @DataProvider(name = "billingQuantityDataProvider")
    public Iterator<Object[]> billingQuantityDataProvider() {
        BigDecimal pointFour = BigDecimal.valueOf(.4d);
        BigDecimal pointFive = BigDecimal.valueOf(.5d);
        BigDecimal pointSix = BigDecimal.valueOf(.6d);
        BigDecimal two = BigDecimal.valueOf(2);
        BigDecimal three = BigDecimal.valueOf(3);

        List<Object[]> testCases = new ArrayList<>();
        testCases.add(
            new Object[]{Collections.singletonList(BigDecimal.ONE), Collections.singletonList(BigDecimal.ONE.negate()),
                null});
        testCases.add(
            new Object[]{Collections.singletonList(BigDecimal.ZERO), Collections.singletonList(BigDecimal.ONE.negate()),
                BillingAdaptor.NEGATIVE_BILL_ERROR});
        testCases.add(
            new Object[]{Collections.singletonList(BigDecimal.ONE), Collections.singletonList(BigDecimal.ZERO.negate()),
                BillingAdaptor.CREDIT_QUANTITY_INVALID});
        testCases.add(new Object[]{Collections.singletonList(BigDecimal.ZERO),
            Collections.singletonList(BigDecimal.ZERO.negate()), BillingAdaptor.CREDIT_QUANTITY_INVALID});
        testCases.add(new Object[]{Arrays.asList(pointFour, pointSix, pointFour),
            Collections.singletonList(BigDecimal.ONE.negate()), null});
        testCases.add(new Object[]{Arrays.asList(BigDecimal.ONE.negate(), pointFour), Arrays.asList(pointFive.negate()),
            BillingAdaptor.NEGATIVE_BILL_ERROR});
        testCases.add(
            new Object[]{Arrays.asList(pointFive, BigDecimal.ONE), Collections.singletonList(pointSix.negate()), null});
        testCases.add(
            new Object[]{Collections.singletonList(BigDecimal.ONE.negate()), Collections.singletonList(two.negate()),
                BillingAdaptor.NEGATIVE_BILL_ERROR});
        testCases.add(
            new Object[]{Collections.singletonList(BigDecimal.ZERO), Collections.singletonList(BigDecimal.ONE.negate()),
                BillingAdaptor.NEGATIVE_BILL_ERROR});
        testCases.add(new Object[]{Collections.emptyList(), Collections.singletonList(BigDecimal.ONE.negate()),
            BillingAdaptor.NEGATIVE_BILL_ERROR});
        testCases.add(new Object[]{Arrays.asList(two, BigDecimal.ONE, two),
            Arrays.asList(three.negate(), BigDecimal.ONE.negate()), null});
        testCases.add(new Object[]{Arrays.asList(BigDecimal.ONE, two, three, BigDecimal.ONE),
            Arrays.asList(BigDecimal.ONE.negate(), two.negate(), three.negate()), null});
        testCases.add(new Object[]{Arrays.asList(three, two, BigDecimal.ONE, BigDecimal.ONE),
            Arrays.asList(BigDecimal.ONE.negate(), two.negate(), three.negate()), null});
        testCases.add(new Object[]{Arrays.asList(BigDecimal.ONE, two, three, BigDecimal.ONE),
            Arrays.asList(three.negate(), two.negate(), BigDecimal.ONE.negate()), null});
        testCases.add(new Object[]{Arrays.asList(three, two, BigDecimal.ONE, BigDecimal.ONE),
            Arrays.asList(three.negate(), two.negate(), BigDecimal.ONE.negate()), null});
        return testCases.iterator();
    }


    @Test(dataProvider = "billingQuantityDataProvider")
    public void testLedgerEntriesSameSampleMultiplePositive(List<BigDecimal> positiveQuantities,
                                                            List<BigDecimal> negativeQuantities, String error)
        throws Exception {

        SapIntegrationServiceImpl sapService =
            new SapIntegrationServiceImpl(SapConfig.produce(Deployment.DEV), bspUserList, priceListCache,
                productPriceCache, accessControlEjb);
        SapIntegrationClientImpl sapIntegrationClient = Mockito.mock(SapIntegrationClientImpl.class);

        Mockito.when(sapIntegrationClient.createReturnOrder(Mockito.any(SAPReturnOrder.class))).thenAnswer(
            (Answer<String>) invocation -> {
                SAPReturnOrder sapReturnOrder = (SAPReturnOrder) invocation.getArguments()[0];
                return "RETURN_OF_" + sapReturnOrder.getDeliveryId();
            });
        sapService.setWrappedClient(sapIntegrationClient);

        ProductOrder productOrder = ProductOrderTestFactory.buildWholeGenomeProductOrder(100);
        Product product = productOrder.getProduct();

        List<LedgerEntry> ledgerEntries = new ArrayList<>();

        ProductOrderSample productOrderSample = productOrder.getSamples().iterator().next();
        for (int i = 0; i < positiveQuantities.size(); i++) {
            BigDecimal quantity = positiveQuantities.get(i);
            String deliveryDocumentId = String.format("00%d", i);
            LedgerEntry ledger = createLedgerEntry(productOrderSample, product, deliveryDocumentId, quantity);
            if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                ledger.setSapReturnOrderId(String.format("99%d", i));
            }
            ledger.setBillingMessage(BillingSession.SUCCESS);
        }

        for (int i = 0; i < negativeQuantities.size(); i++) {
            BigDecimal quantity = negativeQuantities.get(i);
            String deliveryDocumentId = String.format("00%d", i);
            LedgerEntry ledger = createLedgerEntry(productOrderSample, product, deliveryDocumentId, quantity);
            ledgerEntries.add(ledger);
        }

        BillingSession billingSession = new BillingSession(System.currentTimeMillis(), new HashSet<>(ledgerEntries));
        billingSession.getUnBilledQuoteImportItems(priceListCache);
        QuoteImportItem quoteImportItem =
            new QuoteImportItem(QUOTE_ID, null, null, ledgerEntries, new Date(), product, productOrder);

        List<BillingCredit> billingReturns = new ArrayList<>();
        Collection<BillingCredit> billingCredits = new HashSet<>();
        boolean expectError = StringUtils.isNotBlank(error);
        try {
            billingCredits.addAll(BillingCredit.setupSapCredits(quoteImportItem));
            assertThat(expectError, is(false));
        } catch (Exception e) {
            assertThat(e.getMessage(), equalTo(error));
        }

        for (BillingCredit billingReturn : billingCredits) {
            billingReturn.setReturnOrderId(sapService.creditDelivery(billingReturn));
            billingReturns.add(billingReturn);
        }

        ledgerEntries.forEach(ledgerEntry -> {
            ledgerEntry.findCreditSource().forEach((ledgerEntry1, bigDecimal) -> {
                if (!expectError) {
                    assertThat(bigDecimal, greaterThanOrEqualTo(ledgerEntry.getQuantity()));
                } else {
                    assertThat(bigDecimal, greaterThanOrEqualTo(ledgerEntry.getQuantity()));
                }
            });
        });

        assertThat(billingReturns.stream().map(BillingCredit::getReturnOrderId).collect(Collectors.toList()),
            Matchers.everyItem(startsWith(RETURN_PREFIX)));
        int invocationCount = (int) billingCredits.stream().map(BillingCredit::getReturnLines).count();
        Mockito.verify(sapIntegrationClient, Mockito.times(invocationCount))
            .createReturnOrder(Mockito.any(SAPReturnOrder.class));
    }

    public LedgerEntry createLedgerEntry(ProductOrderSample productOrderSample, Product product,
                                         String deliveryDocument, BigDecimal qty) {
        LedgerEntry ledgerEntry =
            new LedgerEntry(productOrderSample, product, new Date(), qty);
        ledgerEntry.setQuoteId(String.valueOf(System.nanoTime()));
        if (deliveryDocument != null) {
            ledgerEntry.setSapDeliveryDocumentId(deliveryDocument);
        }
        productOrderSample.getLedgerItems().add(ledgerEntry);
        return ledgerEntry;
    }
}
