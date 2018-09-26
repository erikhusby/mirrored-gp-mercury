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
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
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
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.sap.entity.SAPMaterial;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingCreditDbFreeTest {
    public BillingCreditDbFreeTest() {
    }

    private PriceItem priceItem;
    private QuotePriceItem quotePriceItem;
    private BillingAdaptor billingAdaptor;
    private ProductOrder pdo;
    private SapIntegrationService sapService = new SapIntegrationServiceStub();

    private EmailSender mockEmailSender = Mockito.mock(EmailSender.class);
    private BillingSessionDao billingSessionDao = Mockito.mock(BillingSessionDao.class);
    private PriceListCache priceListCache = Mockito.mock(PriceListCache.class);
    private QuoteService quoteService = Mockito.mock(QuoteService.class);
    private BillingSessionAccessEjb billingSessionAccessEjb = Mockito.mock(BillingSessionAccessEjb.class);
    private ProductOrderEjb productOrderEjb = Mockito.mock(ProductOrderEjb.class);
    private SAPProductPriceCache productPriceCache = Mockito.mock(SAPProductPriceCache.class);
    private SAPAccessControlEjb accessControlEjb = Mockito.mock(SAPAccessControlEjb.class);
    private BillingEjb billingEjb;

    private final Double qtyPositiveTwo = 2D;
    private final Double qtyNegativeTwo = (double) Math.negateExact(qtyPositiveTwo.longValue());

    @BeforeMethod
    public void setUp() throws QuoteNotFoundException, QuoteServerException, InvalidProductException {
        resetMocks();
        pdo = ProductOrderTestFactory.createDummyProductOrder(2, "PDO-1234");
        pdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        pdo.setProductOrderAddOns(Collections.emptyList());
        String quoteId = pdo.getQuoteId();
        String sapOrderNumber = "sap1234";

        SapOrderDetail sapOrderDetail = new SapOrderDetail(sapOrderNumber, 1, quoteId,
            SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode(), "", "");
        pdo.setSapReferenceOrders(Collections.singletonList(sapOrderDetail));

        priceItem = pdo.getProduct().getPrimaryPriceItem();
        priceItem.setPrice("100");
        priceItem.setUnits("uL");
        quotePriceItem =
            new QuotePriceItem(priceItem.getCategory(), quoteId, priceItem.getName(), priceItem.getPrice(),
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
        Mockito.when(productOrderEjb.areProductsBlocked(Mockito.anySetOf(AccessItem.class))).thenReturn(false);
        Mockito.when(productOrderEjb.isOrderEligibleForSAP(Mockito.any(ProductOrder.class), Mockito.any(Date.class)))
            .thenReturn(true);

        Mockito.when(productPriceCache.findByProduct(Mockito.any(Product.class), Mockito.any(
            SapIntegrationClientImpl.SAPCompanyConfiguration.class)))
            .thenReturn(new SAPMaterial("material", "100", Collections.emptyMap(), Collections.emptyMap()));
        TemplateEngine templateEngine = new TemplateEngine();

        templateEngine.postConstruct();
        billingEjb =
            new BillingEjb(priceListCache, billingSessionDao, null, null, null, AppConfig.produce(Deployment.DEV),
                SapConfig.produce(Deployment.DEV), mockEmailSender, templateEngine);
        billingAdaptor = new BillingAdaptor(billingEjb, priceListCache, quoteService, billingSessionAccessEjb,
            sapService, productPriceCache, accessControlEjb);
        billingAdaptor.setProductOrderEjb(productOrderEjb);
    }

    public void testCreateBillingCreditRequest() {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();

        HashMap<ProductOrderSample, Pair<PriceItem, Double>> billingMap = new HashMap<>();
        billingMap.put(pdoSample, Pair.of(priceItem, qtyPositiveTwo));

        List<BillingEjb.BillingResult> billingResults = bill(billingMap);
        validateBillingResults(pdoSample, billingResults, qtyPositiveTwo);

        billingMap.clear();
        billingMap.put(pdoSample, Pair.of(priceItem, qtyNegativeTwo));
        billingResults = bill(billingMap);
        validateBillingResults(pdoSample, billingResults, 0);

        Mockito.verify(mockEmailSender, Mockito.times(1))
            .sendHtmlEmail(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.anyBoolean());

    }

    public void testNegativeBilling() {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();

        HashMap<ProductOrderSample, Pair<PriceItem, Double>> billingMap = new HashMap<>();
        billingMap.put(pdoSample, Pair.of(priceItem, qtyNegativeTwo));
        List<BillingEjb.BillingResult> billingResults = bill(billingMap);

        billingResults.forEach(
            billingResult -> assertThat(billingResult.getErrorMessage(), endsWith(BillingAdaptor.NEGATIVE_BILL_ERROR)));

        Mockito.verify(mockEmailSender, Mockito.never())
            .sendHtmlEmail(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.anyBoolean());

    }

    public void testPositiveBilling() {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();

        HashMap<ProductOrderSample, Pair<PriceItem, Double>> billingMap = new HashMap<>();
        billingMap.put(pdoSample, Pair.of(priceItem, qtyPositiveTwo));
        List<BillingEjb.BillingResult> billingResults = bill(billingMap);
        billingResults.forEach(
            billingResult -> {
                assertThat(billingResult.getErrorMessage(), blankOrNullString());
                assertThat(billingResult.getSAPBillingId(), not(blankOrNullString()));
            });

        Mockito.verify(mockEmailSender, Mockito.never())
            .sendHtmlEmail(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    public void testMoreNegativeThanPositiveBillingPositiveFirst() {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();
        HashMap<ProductOrderSample, Pair<PriceItem, Double>> billingMap = new HashMap<>();
        billingMap.put(pdoSample, Pair.of(priceItem, 1d));

        List<BillingEjb.BillingResult> billingResults = bill(billingMap);
        validateBillingResults(pdoSample, billingResults, 1);

        billingMap.clear();
        billingMap.put(pdoSample, Pair.of(priceItem, qtyNegativeTwo));
        billingResults = bill(billingMap);

        billingResults.forEach(
            billingResult -> assertThat(billingResult.getErrorMessage(), endsWith(BillingAdaptor.NEGATIVE_BILL_ERROR)));

        Mockito.verify(mockEmailSender, Mockito.never())
            .sendHtmlEmail(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.anyBoolean());
    }

    private void validateBillingResults(ProductOrderSample sample, List<BillingEjb.BillingResult> results,
                                        double quantity) {
        results.stream().filter(result -> !result.isError())
            .forEach(billingResult -> {
                assertThat(billingResult.isError(), is(false));
                assertThat(billingResult.getSAPBillingId(), notNullValue());
            });
        Double totalBilled =
            sample.getLedgerItems().stream().filter(LedgerEntry::isSuccessfullyBilled)
                .map(LedgerEntry::getQuantity).mapToDouble(Double::longValue).sum();
        assertThat(totalBilled, equalTo(quantity));
    }

    private List<BillingEjb.BillingResult> bill(Map<ProductOrderSample, Pair<PriceItem, Double>> samplePairMap) {
        final Date date = new Date();
        samplePairMap.forEach((productOrderSample, pricItemQtyPair) ->  {
            productOrderSample.addLedgerItem(date, pricItemQtyPair.getKey(), pricItemQtyPair.getValue());
        });
        Set<LedgerEntry> ledgerItems = new HashSet<>();
        samplePairMap.keySet().stream().map(ProductOrderSample::getLedgerItems).forEach(ledgerItems::addAll);

        BillingSession billingSession = new BillingSession(1L, ledgerItems);
        Mockito.reset(billingSessionAccessEjb);
        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString())).thenReturn(billingSession);
        return billingAdaptor.billSessionItems("url", billingSession.getBusinessKey());
    }

    private void resetMocks(){
        Mockito.reset(mockEmailSender, billingSessionDao, priceListCache, quoteService, productOrderEjb,
            billingSessionAccessEjb, productPriceCache, accessControlEjb);
    }
}
