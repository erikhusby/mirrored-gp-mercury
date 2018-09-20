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
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingCreditDbFreeTest {
    private EmailSender mockEmailSender = Mockito.mock(EmailSender.class);
    private BillingEmailService billingEmailService;
    private PriceItem priceItem;
    private QuotePriceItem quotePriceItem;

    private final long initialQuantity = 2L;
    private final long negativeQuantity = Math.negateExact(initialQuantity);

    public BillingCreditDbFreeTest() {
    }

    private BillingSessionDao billingSessionDao = Mockito.mock(BillingSessionDao.class);
    private PriceListCache priceListCache = Mockito.mock(PriceListCache.class);
    private BillingEjb billingEjb = new BillingEjb(priceListCache, billingSessionDao, null, null, null);
    private QuoteService quoteService = Mockito.mock(QuoteService.class);
    private BillingSessionAccessEjb billingSessionAccessEjb = Mockito.mock(BillingSessionAccessEjb.class);
    private SapIntegrationService sapService = new SapIntegrationServiceStub();
    private ProductOrderEjb productOrderEjb = Mockito.mock(ProductOrderEjb.class);
    private BillingAdaptor billingAdaptor;
    private ProductOrder pdo;
    private SAPProductPriceCache productPriceCache = Mockito.mock(SAPProductPriceCache.class);
    private SAPAccessControlEjb accessControlEjb = Mockito.mock(SAPAccessControlEjb.class);

    @BeforeMethod
    public void setUp() throws QuoteNotFoundException, QuoteServerException, InvalidProductException {
        pdo = ProductOrderTestFactory.createDummyProductOrder(1, "PDO-1234");
        pdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        pdo.setProductOrderAddOns(Collections.emptyList());
        String quoteId = pdo.getQuoteId();
        String sapOrderNumber = "sap1234";

        SapOrderDetail sapOrderDetail = new SapOrderDetail(sapOrderNumber, 1, quoteId,
            SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode(), "", "");
        pdo.setSapReferenceOrders(Collections.singletonList(sapOrderDetail));

        priceItem = pdo.getProduct().getPrimaryPriceItem();
        priceItem.setPrice("100");
        priceItem.setUnits("2");
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

        Mockito.when(priceListCache.getQuotePriceItems()).thenReturn(Collections.singleton(quotePriceItem));
        Mockito.when(priceListCache.findByKeyFields(Mockito.any(PriceItem.class))).thenReturn(quotePriceItem);

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

        billingEmailService =
            new BillingEmailService(AppConfig.produce(Deployment.DEV), SapConfig.produce(Deployment.DEV),
                mockEmailSender, templateEngine);
        billingAdaptor = new BillingAdaptor(billingEjb, priceListCache, quoteService, billingSessionAccessEjb,
            sapService, productPriceCache, accessControlEjb, billingEmailService);
        billingAdaptor.setProductOrderEjb(productOrderEjb);
    }

    public void testCreateBillingCreditRequest() {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();

        List<BillingEjb.BillingResult> billingResults = bill(pdoSample, priceItem, initialQuantity);
        validateBillingResults(pdoSample, billingResults, initialQuantity);

        billingResults = bill(pdoSample, priceItem, negativeQuantity);
        validateBillingResults(pdoSample, billingResults, 0);

        Mockito.verify(mockEmailSender, Mockito.times(1))
            .sendHtmlEmail(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.anyBoolean());

    }

    public void testNegativeBilling() {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();

        List<BillingEjb.BillingResult> billingResults = bill(pdoSample, priceItem, negativeQuantity);
        billingResults.forEach(
            billingResult -> assertThat(billingResult.getErrorMessage(), endsWith(BillingAdaptor.NEGATIVE_BILL_ERROR)));

        Mockito.verify(mockEmailSender, Mockito.never())
            .sendHtmlEmail(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.anyBoolean());

    }

    public void testMoreNegativeThanPositiveBilling() {
        ProductOrderSample pdoSample = pdo.getSamples().iterator().next();
        List<BillingEjb.BillingResult> billingResults = bill(pdoSample, priceItem, 1);
        validateBillingResults(pdoSample, billingResults, 1);

        billingResults = bill(pdoSample, priceItem, -2);
        billingResults.forEach(
            billingResult -> assertThat(billingResult.getErrorMessage(), endsWith(BillingAdaptor.NEGATIVE_BILL_ERROR)));

        Mockito.verify(mockEmailSender, Mockito.never())
            .sendHtmlEmail(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.anyBoolean());

    }

    private void validateBillingResults(ProductOrderSample sample, List<BillingEjb.BillingResult> results,
                                        double quantity) {
        results.forEach(billingResult -> assertThat(billingResult.isError(), is(false)));
        Double totalBilled =
            sample.getLedgerItems().stream().map(LedgerEntry::getQuantity).mapToDouble(Double::longValue).sum();
        assertThat(totalBilled, equalTo(quantity));
    }

    private List<BillingEjb.BillingResult> bill(ProductOrderSample productOrderSample, PriceItem priceItem,
                                                double quantity) {
        productOrderSample.addLedgerItem(new Date(), priceItem, quantity);
        BillingSession billingSession = new BillingSession(1L, productOrderSample.getLedgerItems());
        Mockito.reset(billingSessionAccessEjb);
        Mockito.when(billingSessionAccessEjb.findAndLockSession(Mockito.anyString())).thenReturn(billingSession);
        List<BillingEjb.BillingResult> billingResults =
            billingAdaptor.billSessionItems("url", billingSession.getBusinessKey());
        return billingResults;
    }
}
