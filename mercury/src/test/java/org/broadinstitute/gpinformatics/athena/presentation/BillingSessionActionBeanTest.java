package org.broadinstitute.gpinformatics.athena.presentation;

import com.google.common.collect.ArrayListMultimap;
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
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingSessionActionBeanTest {

    private static final String WORK_ITEM_ID = "1234";
    private final String BILLING_SESSION_ID = "BILL-123";

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
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getWorkItemIdToHighlight(),equalTo(
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
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getWorkItemIdToHighlight(), is(nullValue()));
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
        assertThat(roundTrip.getActionBean(BillingSessionActionBean.class).getWorkItemIdToHighlight(),equalTo(
                WORK_ITEM_ID));
    }


    @DataProvider(name = "billingMessageProvider")
    public Iterator<Object[]> billingMessageProvider() throws SAPIntegrationException {
        billingSessionActionBean = new BillingSessionActionBean();
        quoteServerConfig = QuoteConfig.produce(Deployment.DEV);
        QuoteLink quoteLink = new QuoteLink(quoteServerConfig);
        billingSessionActionBean.setQuoteLink(quoteLink);
        sapConfig = SapConfig.produce(Deployment.DEV);
        SapQuoteLink sapQuoteLink = new SapQuoteLink(sapConfig);
        billingSessionActionBean.setSapQuoteLink(sapQuoteLink);
        String qsQuoteId = "GPP1234";
        String sapQuoteId = "2700001";
        String sapQuoteId2 = "2700002";
        LedgerEntry ledgerEntry = Mockito.mock(LedgerEntry.class);

        ProductOrder quoteServerProductOrder = ProductOrderTestFactory.createDummyProductOrder("2345");
        quoteServerProductOrder.setQuoteSource(ProductOrder.QuoteSourceType.QUOTE_SERVER);

        ProductOrder sapProductOrder = ProductOrderTestFactory.createDummyProductOrder("3456");
        sapProductOrder.setQuoteSource(ProductOrder.QuoteSourceType.SAP_SOURCE);

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
}
