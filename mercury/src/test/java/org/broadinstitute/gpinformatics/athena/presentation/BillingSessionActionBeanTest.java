package org.broadinstitute.gpinformatics.athena.presentation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.sourceforge.stripes.mock.MockRoundtrip;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb;
import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.SapQuoteLink;
import org.broadinstitute.gpinformatics.infrastructure.common.StringMessageReporter;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteConfig;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingSessionActionBeanTest {

    private static final String WORK_ITEM_ID = "1234";
    private final String BILLING_SESSION_ID = "BILL-123";
    private static final String TEST_WORKID = "WI1234";

    MockRoundtrip roundTrip;
    private BillingSessionActionBean billingSessionActionBean;
    private SapConfig sapConfig;
    private QuoteConfig quoteServerConfig;

    @BeforeMethod
    public void setUp() {
        roundTrip = StripesMockTestUtils.createMockRoundtrip(BillingSessionActionBean.class, Mockito.mock(BillingSessionDao.class));
    }

    @Test
    public void testLoadingNonNullWorkItemId() throws Exception {
        roundTrip.addParameter(BillingSessionActionBean.WORK_ITEM_FROM_URL_PARAMETER, WORK_ITEM_ID);
        roundTrip.addParameter(BillingSessionActionBean.BILLING_SESSION_FROM_URL_PARAMETER,
                               BILLING_SESSION_ID);
        roundTrip.execute(BillingSessionActionBean.VIEW_ACTION);
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getHighlightRow(),equalTo(
                WORK_ITEM_ID));
    }

    @Test
    public void testInitDoesntLoseSession() throws Exception {
        roundTrip.addParameter(BillingSessionActionBean.SESSION_KEY_PARAMETER_NAME,BILLING_SESSION_ID);
        roundTrip.execute(BillingSessionActionBean.VIEW_ACTION);
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getSessionKey(),equalTo(
                BILLING_SESSION_ID));
    }

    @Test
    public void testLoadingNullWorkItemId() throws Exception  {
        roundTrip.addParameter(BillingSessionActionBean.WORK_ITEM_FROM_URL_PARAMETER,null);
        roundTrip.execute(BillingSessionActionBean.VIEW_ACTION);
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getHighlightRow(), is(nullValue()));
    }

    @Test
    public void testRedirectFromQuoteServer() throws Exception {
        roundTrip.addParameter(BillingSessionActionBean.WORK_ITEM_FROM_URL_PARAMETER, WORK_ITEM_ID);
        roundTrip.addParameter(BillingSessionActionBean.BILLING_SESSION_FROM_URL_PARAMETER,
                               BILLING_SESSION_ID);
        roundTrip.execute();
        // explicit checks on the redirect url
        assertThat(roundTrip.getRedirectUrl(), containsString(
                BillingSessionActionBean.WORK_ITEM_FROM_URL_PARAMETER + "=" + WORK_ITEM_ID));
        assertThat(roundTrip.getRedirectUrl(),containsString(
                BillingSessionActionBean.SESSION_KEY_PARAMETER_NAME + "=" + BILLING_SESSION_ID));
    }

    @Test
    public void testWorkItemGetter() throws Exception {
        roundTrip.addParameter(BillingSessionActionBean.WORK_ITEM_FROM_URL_PARAMETER, WORK_ITEM_ID);
        roundTrip.addParameter(BillingSessionActionBean.BILLING_SESSION_FROM_URL_PARAMETER,
                               BILLING_SESSION_ID);
        roundTrip.execute(BillingSessionActionBean.VIEW_ACTION);
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getHighlightRow(),equalTo(
                WORK_ITEM_ID));
    }


    @DataProvider(name = "billingMessageProvider")
    public Iterator<Object[]> billingMessageProvider() throws SAPIntegrationException {
        initBillingSessionActionBean();
        String qsQuoteId = "GPP1234";
        String sapQuoteId = "2700001";
        String sapQuoteId2 = "2700002";
        LedgerEntry ledgerEntry = Mockito.mock(LedgerEntry.class);

        ProductOrder quoteServerProductOrder = ProductOrderTestFactory.createDummyProductOrder("2345");
        quoteServerProductOrder.setQuoteId(qsQuoteId);

        ProductOrder sapProductOrder = ProductOrderTestFactory.createDummyProductOrder("3456");
        sapProductOrder.setQuoteId(sapQuoteId);

        QuoteImportItem quoteServerImportItem =
            new QuoteImportItem(qsQuoteId, new PriceItem(qsQuoteId, null, null, null), "",
                Collections.singletonList(ledgerEntry), new Date(), quoteServerProductOrder.getProduct(),
                quoteServerProductOrder);

        QuoteImportItem sapQuoteImportItem =
            new QuoteImportItem(sapQuoteId, new PriceItem(sapQuoteId, null, null, null), "",
                Collections.singletonList(ledgerEntry), new Date(), sapProductOrder.getProduct(), sapProductOrder);
        sapQuoteImportItem.setSapQuote(TestUtils
            .buildTestSapQuote(sapQuoteId, 0d, 0d, sapProductOrder, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization()));
        QuoteImportItem sapQuoteImportItem2 =
            new QuoteImportItem(sapQuoteId2, new PriceItem(sapQuoteId2, null, null, null), "",
                Collections.singletonList(ledgerEntry), new Date(), sapProductOrder.getProduct(), sapProductOrder);
        sapQuoteImportItem2.setSapQuote(TestUtils
            .buildTestSapQuote(sapQuoteId2, 0d, 0d, sapProductOrder, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED,
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization()));

        List<Object[]> testCases = new ArrayList<>();

        testCases.add(new Object[]{Collections.singletonList(quoteServerImportItem)});
        testCases.add(new Object[]{Collections.singletonList(sapQuoteImportItem)});
        testCases.add(new Object[]{Arrays.asList(quoteServerImportItem, sapQuoteImportItem)});
        testCases.add(new Object[]{Arrays.asList(sapQuoteImportItem, sapQuoteImportItem2)});
        testCases.add(new Object[]{Arrays.asList(sapQuoteImportItem, quoteServerImportItem, sapQuoteImportItem2)});

        return testCases.iterator();
    }

    public void initBillingSessionActionBean() {
        billingSessionActionBean = new BillingSessionActionBean();
        quoteServerConfig = QuoteConfig.produce(Deployment.DEV);
        QuoteLink quoteLink = new QuoteLink(quoteServerConfig);
        billingSessionActionBean.setQuoteLink(quoteLink);
        sapConfig = SapConfig.produce(Deployment.DEV);
        SapQuoteLink sapQuoteLink = new SapQuoteLink(sapConfig);
        billingSessionActionBean.setSapQuoteLink(sapQuoteLink);
    }


    @Test(dataProvider = "billingMessageProvider")
    public void testCreateBillingMessageQuoteServerQuote(List<QuoteImportItem> quoteImportItems) throws Exception {
        List<BillingEjb.BillingResult> billingResults = new ArrayList<>();
        quoteImportItems.forEach(quoteImportItem -> {
            String quoteId = quoteImportItem.getQuoteId();
            quoteImportItem.setQuote(new Quote(quoteId, Mockito.mock(QuoteFunding.class), ApprovalStatus.FUNDED));
            billingResults.add(new BillingEjb.BillingResult(quoteImportItem));

        });
        StringMessageReporter stringMessageReporter = new StringMessageReporter();

        billingSessionActionBean.createBillingMessage(billingResults, stringMessageReporter);
        ArrayListMultimap<ProductOrder.QuoteSourceType, String> quotesByType = ArrayListMultimap.create();

        quoteImportItems.forEach(quoteImportItem -> {
            ProductOrder.QuoteSourceType thisQuoteSource;
            if (quoteImportItem.isSapOrder()) {
                thisQuoteSource = ProductOrder.QuoteSourceType.SAP_SOURCE;
            } else {
                thisQuoteSource = ProductOrder.QuoteSourceType.QUOTE_SERVER;
            }
            quotesByType.put(thisQuoteSource, quoteImportItem.getQuoteId());
        });
        quotesByType.asMap().forEach((qsType, quotes) -> quotes.forEach(quote -> {
            assertThat(stringMessageReporter.getMessages(), hasItem(containsString(quote)));
            String messageStartsWith;
            String url;

            if (qsType.isSapType()) {
                messageStartsWith = BillingSessionActionBean.SENT_TO_SAP;
                url = sapConfig.getUrl();
            } else {
                messageStartsWith = BillingSessionActionBean.SENT_TO_QUOTE_SERVER;
                url = quoteServerConfig.getUrl();
            }
            assertThat(stringMessageReporter.getMessages(), hasItem(startsWith(messageStartsWith)));
            assertThat(stringMessageReporter.getMessages(), hasItem(containsString(url)));
        }));
    }

    private void validateQuoteLinks(Collection<String> links, ProductOrder.QuoteSourceType quoteSourceType) {
        links.forEach(link ->{
            if (quoteSourceType == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
                assertThat(link, containsString("workId=" + TEST_WORKID));
                assertThat(link, containsString("QS-1"));
            } else {
                assertThat(link, matchesPattern(".*[1234|2345]+.*"));
            }
        });
    }

    public void testGetQuoteLinkSap() throws Exception {
        initBillingSessionActionBean();
        QuoteImportItem quoteImportItem = getQuoteImportItem("1234", ProductOrder.QuoteSourceType.SAP_SOURCE);
        Multimap<ProductOrder.QuoteSourceType, String> quoteLink =
            billingSessionActionBean.getQuoteLink(quoteImportItem);
        Collection<String> strings = quoteLink.get(ProductOrder.QuoteSourceType.SAP_SOURCE);

        assertThat(strings.size(), is(1));
        validateQuoteLinks(strings, ProductOrder.QuoteSourceType.SAP_SOURCE);
        assertThat(quoteLink.get(ProductOrder.QuoteSourceType.QUOTE_SERVER), emptyCollectionOf(String.class));

    }


    public void testGetQuoteLinkQsQuote() throws Exception {
        initBillingSessionActionBean();
        QuoteImportItem quoteImportItem = getQuoteImportItem("QS-1", ProductOrder.QuoteSourceType.QUOTE_SERVER);
        Multimap<ProductOrder.QuoteSourceType, String> quoteLink = billingSessionActionBean.getQuoteLink(quoteImportItem);
        Collection<String> strings = quoteLink.get(ProductOrder.QuoteSourceType.QUOTE_SERVER);

        assertThat(strings.size(), is(1));
        validateQuoteLinks(strings, ProductOrder.QuoteSourceType.QUOTE_SERVER);
        assertThat(quoteLink.get(ProductOrder.QuoteSourceType.SAP_SOURCE), emptyCollectionOf(String.class));
    }

    public void testGetQuoteLinkQsAndSapQuote() throws Exception {
        initBillingSessionActionBean();
        QuoteImportItem quoteImportItem = getQuoteImportItem("QS-1", ProductOrder.QuoteSourceType.QUOTE_SERVER);

        Multimap<ProductOrder.QuoteSourceType, String> quoteLink =
            billingSessionActionBean.getQuoteLink(quoteImportItem);
        quoteImportItem = getQuoteImportItem("1234", ProductOrder.QuoteSourceType.SAP_SOURCE);

        quoteLink.putAll(billingSessionActionBean.getQuoteLink(quoteImportItem));

        Collection<String> strings = quoteLink.get(ProductOrder.QuoteSourceType.QUOTE_SERVER);
        assertThat(strings.size(), is(1));
        validateQuoteLinks(strings, ProductOrder.QuoteSourceType.QUOTE_SERVER);

        strings = quoteLink.get(ProductOrder.QuoteSourceType.SAP_SOURCE);
        assertThat(strings.size(), is(1));
        validateQuoteLinks(strings, ProductOrder.QuoteSourceType.SAP_SOURCE);
    }

    public void testGetQuoteLinkNoQsAndTwoSapQuote() throws Exception {
        initBillingSessionActionBean();
        QuoteImportItem quoteImportItem = getQuoteImportItem("1234", ProductOrder.QuoteSourceType.SAP_SOURCE);

        Multimap<ProductOrder.QuoteSourceType, String> quoteLink = billingSessionActionBean.getQuoteLink(quoteImportItem);
        quoteImportItem = getQuoteImportItem("2345", ProductOrder.QuoteSourceType.SAP_SOURCE);

        quoteLink.putAll(billingSessionActionBean.getQuoteLink(quoteImportItem));

        assertThat(quoteLink.get(ProductOrder.QuoteSourceType.QUOTE_SERVER), emptyCollectionOf(String.class));
        Collection<String> strings = quoteLink.get(ProductOrder.QuoteSourceType.SAP_SOURCE);

        assertThat(strings.size(), is(2));
        validateQuoteLinks(strings, ProductOrder.QuoteSourceType.SAP_SOURCE);
    }

    public void testGetQuoteLinkNoQsAndTwoSapIdenticalQuote() throws Exception {
        initBillingSessionActionBean();
        QuoteImportItem quoteImportItem = getQuoteImportItem("1234", ProductOrder.QuoteSourceType.SAP_SOURCE);

        Multimap<ProductOrder.QuoteSourceType, String> quoteLink = billingSessionActionBean.getQuoteLink(quoteImportItem);
        quoteImportItem = getQuoteImportItem("1234", ProductOrder.QuoteSourceType.SAP_SOURCE);

        quoteLink.putAll(billingSessionActionBean.getQuoteLink(quoteImportItem));

        assertThat(quoteLink.get(ProductOrder.QuoteSourceType.QUOTE_SERVER), emptyCollectionOf(String.class));
        Collection<String> strings = quoteLink.get(ProductOrder.QuoteSourceType.SAP_SOURCE);

        assertThat(strings.size(), is(1));
        validateQuoteLinks(strings, ProductOrder.QuoteSourceType.SAP_SOURCE);
    }

    public QuoteImportItem getQuoteImportItem(String quoteId, ProductOrder.QuoteSourceType quoteSourceType) {
        ProductOrder pdo = ProductOrderTestFactory.createDummyProductOrder("PDO-1234");
        pdo.setQuoteId(quoteId);
        List<LedgerEntry> ledgerItems = new ArrayList<>();
        LedgerEntry ledgerEntry = null;
        if (!pdo.hasSapQuote()){
            ledgerEntry = new LedgerEntry(null, new PriceItem(), new Date(), 2);
            ledgerEntry.setWorkItem(TEST_WORKID);
        } else {
            ledgerEntry = new LedgerEntry(null, pdo.getProduct(), new Date(), 2);
            ledgerEntry.setWorkItem(TEST_WORKID);
        }
        ledgerItems.add(ledgerEntry);

        return new QuoteImportItem(quoteId, new PriceItem(), null, ledgerItems, new Date(), null, pdo);
    }
}
