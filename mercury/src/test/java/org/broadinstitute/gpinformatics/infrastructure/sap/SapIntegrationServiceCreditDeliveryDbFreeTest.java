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
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.sap.entity.SAPReturnOrder;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class SapIntegrationServiceCreditDeliveryDbFreeTest {
    public static final String QUOTE_ID = "2700103";
    private BSPUserList bspUserList = Mockito.mock(BSPUserList.class);
    private PriceListCache priceListCache = Mockito.mock(PriceListCache.class);
    private SAPProductPriceCache productPriceCache = Mockito.mock(SAPProductPriceCache.class);
    private SAPAccessControlEjb accessControlEjb = Mockito.mock(SAPAccessControlEjb.class);

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
        quantities.stream().sorted(Comparator.naturalOrder()).forEach(i -> {
            String deliveryDocumentId = String.format("00%d", i);
            ProductOrderSample productOrderSample = productOrder.getSamples().get(i);
            double quantity = i + i;
            LedgerEntry ledger = createLedgerEntry(productOrderSample, product, deliveryDocumentId, quantity);
            ledger.setBillingMessage(BillingSession.SUCCESS);
            ledgerEntries.add(ledger);
        });
        new BillingSession(System.currentTimeMillis(), new HashSet<>(ledgerEntries));
        ledgerEntries.clear();

        quantities.stream().sorted(Comparator.reverseOrder()).forEach(i -> {
            String deliveryDocumentId = String.format("00%d", i);
            ProductOrderSample productOrderSample = productOrder.getSamples().get(i);
            double quantity = -(i + i);
            LedgerEntry ledger = createLedgerEntry(productOrderSample, product, deliveryDocumentId, quantity);
            ledgerEntries.add(ledger);
        });
        new BillingSession(System.currentTimeMillis(), new HashSet<>(ledgerEntries));

        QuoteImportItem quoteImportItem =
            new QuoteImportItem(QUOTE_ID, null, null, ledgerEntries, new Date(), product, productOrder);

        Collection<BillingCredit> billingReturns = new ArrayList<>();
        for (BillingCredit billingReturn : BillingCredit.setupSapCredits(quoteImportItem)) {
            billingReturn.setReturnOrderId(sapService.creditDelivery(billingReturn));
            billingReturns.add(billingReturn);
        }
        assertThat(billingReturns.stream().map(BillingCredit::getReturnOrderId).collect(Collectors.toList()),
            Matchers.everyItem(startsWith("RETURN_OF_00")));

        Mockito.verify(sapIntegrationClient, Mockito.times(quantities.size()))
            .createReturnOrder(Mockito.any(SAPReturnOrder.class));
    }

    public void testLedgerEntriesSameSample() throws Exception {
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

        List<LedgerEntry> positiveEntries = new ArrayList<>();
        List<Integer> quantities = Arrays.asList(1, 3, 5, 7, 11, 20);
        quantities.stream().sorted(Comparator.naturalOrder()).forEach(i -> {
            String deliveryDocumentId = String.format("00%d", i);
            ProductOrderSample productOrderSample = productOrder.getSamples().get(i);
            double quantity = i + i;
            LedgerEntry ledger = createLedgerEntry(productOrderSample, product, deliveryDocumentId, quantity);
            ledger.setBillingMessage(BillingSession.SUCCESS);
            positiveEntries.add(ledger);
        });
        List<LedgerEntry> negativeEntries = new ArrayList<>();
        quantities.stream().sorted(Comparator.reverseOrder()).forEach(i -> {
            String deliveryDocumentId = String.format("00%d", i);
            ProductOrderSample productOrderSample = productOrder.getSamples().get(i);
            double quantity = -(i + i);
            LedgerEntry ledger = createLedgerEntry(productOrderSample, product, deliveryDocumentId, quantity);
            negativeEntries.add(ledger);
        });

        negativeEntries.forEach(negativeEntry -> negativeEntry.findCreditSource().forEach((positiveEntry, quantity) -> {
            assertThat(negativeEntry.getProductOrderSample(), equalTo(positiveEntry.getProductOrderSample()));
            assertThat(positiveEntry.getQuantity() + negativeEntry.getQuantity(), equalTo(0.0d));
        }));
        List<LedgerEntry> result = new ArrayList<>();
        positiveEntries.forEach(ledgerEntry -> result.addAll(ledgerEntry.findCreditSource().keySet()));
        assertThat(result, is(Matchers.emptyCollectionOf(LedgerEntry.class)));

    }

    @DataProvider(name = "billingQuantityDataProvider")
    public Iterator<Object[]> billingQuantityDataProvider() {
        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{Collections.singletonList(1d), Collections.singletonList(-1d), null});
        testCases.add(new Object[]{Arrays.asList(.4d, .6d), Collections.singletonList(-1d), null});
        testCases.add(new Object[]{Collections.singletonList(-1d), Arrays.asList(.5d, .5d), BillingAdaptor.NEGATIVE_BILL_ERROR});
        testCases.add(new Object[]{Arrays.asList(.5d, .6d), Collections.singletonList(-1d),
            BillingAdaptor.CREDIT_QUANTITY_INVALID});
        testCases.add(new Object[]{Collections.singletonList(-1d), Collections.singletonList(-2d),
            BillingAdaptor.NEGATIVE_BILL_ERROR});
        testCases.add(new Object[]{Collections.singletonList(0d), Collections.singletonList(-1d),
            BillingAdaptor.NEGATIVE_BILL_ERROR});
        testCases.add(
            new Object[]{Collections.emptyList(), Collections.singletonList(-1d), BillingAdaptor.NEGATIVE_BILL_ERROR});
        return testCases.iterator();
    }


    @Test(dataProvider = "billingQuantityDataProvider")
    public void testLedgerEntriesSameSampleMultiplePositive(List<Double> positiveQuantities,
                                                            List<Double> negativeQuantities, String error)
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
        new BillingSession(System.currentTimeMillis(), new HashSet<>(ledgerEntries));
        ledgerEntries.clear();
        ProductOrderSample productOrderSample = productOrder.getSamples().iterator().next();
        for (int i = 0; i < positiveQuantities.size(); i++) {
            double quantity = positiveQuantities.get(i);
            String deliveryDocumentId = String.format("00%d", i);
            LedgerEntry ledger = createLedgerEntry(productOrderSample, product, deliveryDocumentId, quantity);
            ledger.setBillingMessage(BillingSession.SUCCESS);
            ledgerEntries.add(ledger);
            new BillingSession(System.currentTimeMillis(), new HashSet<>(ledgerEntries));
        }
        for (int i = 0; i < negativeQuantities.size(); i++) {
            double quantity = negativeQuantities.get(i);
            String deliveryDocumentId = String.format("00%d", i);
            LedgerEntry ledger = createLedgerEntry(productOrderSample, product, deliveryDocumentId, quantity);
            ledgerEntries.add(ledger);
            new BillingSession(System.currentTimeMillis(), new HashSet<>(ledgerEntries));
        }

        QuoteImportItem quoteImportItem =
            new QuoteImportItem(QUOTE_ID, null, null, ledgerEntries, new Date(), product, productOrder);

        List<BillingCredit> billingReturns = new ArrayList<>();
        Collection<BillingCredit> billingCredits = new HashSet<>();
        try {
            billingCredits.addAll(BillingCredit.setupSapCredits(quoteImportItem));
            assertThat(error, anyOf(is(emptyOrNullString()), is(not(BillingAdaptor.CREDIT_QUANTITY_INVALID))));
        } catch (Exception e) {
            assertThat(e.getMessage(), equalTo(error));
        }

        for (BillingCredit billingReturn : billingCredits) {
            billingReturn.setReturnOrderId(sapService.creditDelivery(billingReturn));
            billingReturns.add(billingReturn);
        }

        assertThat(billingReturns.stream().map(BillingCredit::getReturnOrderId).collect(Collectors.toList()),
            Matchers.everyItem(startsWith("RETURN_OF_00")));
        if (StringUtils.isBlank(error)) {
            Mockito.verify(sapIntegrationClient, Mockito.times(positiveQuantities.size())).createReturnOrder(Mockito.any(SAPReturnOrder.class));
        }else {
            Mockito.verify(sapIntegrationClient, Mockito.never()).createReturnOrder(Mockito.any(SAPReturnOrder.class));
        }
    }

    public LedgerEntry createLedgerEntry(ProductOrderSample productOrderSample, Product product,
                                         String deliveryDocument, double qty) {
        LedgerEntry ledgerEntry =
            new LedgerEntry(productOrderSample, product, new Date(), qty);
        ledgerEntry.setQuoteId(String.valueOf(System.nanoTime()));
        ledgerEntry.setSapDeliveryDocumentId(deliveryDocument);
        productOrderSample.getLedgerItems().add(ledgerEntry);
        return ledgerEntry;
    }
}
