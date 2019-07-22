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

package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductOrderJiraUtil;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnector;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class QuoteDetailsHelperTest {
    @Mock
    public SapIntegrationServiceImpl mockSAPService;
    @Mock
    public SAPProductPriceCache stubProductPriceCache;
    @Mock
    public PriceListCache mockPriceListCache;
    @Mock
    public ProductOrderDao mockProductOrderDao;
    @Mock
    private BSPUserList mockBspUserList;
    @Mock
    private JiraService mockJiraService;
    @Mock
    private SapIntegrationClientImpl mockSapClient;
    @Mock
    private SAPAccessControlEjb mockAccessController;
    @Mock
    private ProductDao mockProductDao;
    private ProductOrderEjb productOrderEjb;
    private QuoteDetailsHelper quoteDetailsHelper;
    private ProductOrderActionBean actionBean;
    private QuoteService stubbedQuoteService = QuoteServiceProducer.stubInstance();
    private ProductOrder pdo;
    private TemplateEngine templateEngine;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        productOrderEjb = new ProductOrderEjb(mockProductOrderDao, mockProductDao,
            stubbedQuoteService, mockJiraService, Mockito.mock(UserBean.class),
            mockBspUserList, Mockito.mock(BucketEjb.class), Mockito.mock(SquidConnector.class),
            Mockito.mock(MercurySampleDao.class), Mockito.mock(ProductOrderJiraUtil.class),
            mockSAPService, mockPriceListCache, stubProductPriceCache);

        productOrderEjb.setAccessController(mockAccessController);
        templateEngine = new TemplateEngine();
        templateEngine.postConstruct();
        actionBean = new ProductOrderActionBean();
        actionBean.setProductOrderEjb(productOrderEjb);
        actionBean.setProductOrderDao(mockProductOrderDao);
        actionBean.setProductOrderDao(mockProductOrderDao);

        pdo = ProductOrderTestFactory.createDummyProductOrder();

        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString()))
            .thenReturn((Collections.singletonList(pdo)));

        quoteDetailsHelper = new QuoteDetailsHelper(stubbedQuoteService, null, templateEngine);
    }

    public void testQuoteServerActiveCostObject() throws Exception {
        String quoteId = "STC3ZW";// STC3ZW


        Quote quote = stubbedQuoteService.getQuoteByAlphaId(quoteId);
        QuoteItem quoteItem = quote.getQuoteItems().iterator().next();
        String price = quoteItem.getPrice();
        QuotePriceItem quotePriceItem = new QuotePriceItem(quoteItem.getCategoryName(), quoteItem.getPriceItemId(),
            quoteItem.getName(), price, "EA", quoteItem.getPlatform());
        Mockito.when(mockPriceListCache.findByKeyFields(Mockito.any(PriceItem.class))).thenReturn(quotePriceItem);
        Mockito.when(mockPriceListCache.getEffectivePrice(Mockito.any(PriceItem.class), Mockito.any(Quote.class)))
            .thenReturn(price);

        QuoteDetailsHelper.QuoteDetail quoteDetails = quoteDetailsHelper.getQuoteDetails(quoteId, actionBean);
        QuoteDetailsHelper.FundingInfo fundingInfo = quoteDetails.fundingDetails.iterator().next();
        assertThat(fundingInfo.isQuoteWarning(), is(false));
        assertThat(quoteDetails.outstandingEstimate, equalTo(Double.valueOf(price) * 2));
        assertThat(quoteDetails.fundsRemaining, equalTo(66724d));
        assertThat(fundingInfo.getFundingInfoString(), containsString("Funds Reservation [<b>FR -- 1996, CO -- 6010040</b>]"));
        assertThat(fundingInfo.getFundingInfoString(), containsString("Active"));
        assertThat(fundingInfo.getFundingInfoString(), containsString("Expires 12/31/2999"));
        assertThat(fundingInfo.getFundingInfoString(), containsString("funding split percentage = 100%"));

    }

    public void testQuoteServerExpired() throws Exception {
        String quoteId = "MMM2OU";

        double price = 10d;
        Quote quote = stubbedQuoteService.getQuoteByAlphaId(quoteId);

        QuoteItem quoteItem = quote.getQuoteItems().iterator().next();
        QuotePriceItem quotePriceItem = new QuotePriceItem(quoteItem.getCategoryName(), quoteItem.getPriceItemId(),
            quoteItem.getName(), quoteItem.getPrice(), "EA", quoteItem.getPlatform());
        Mockito.when(mockPriceListCache.findByKeyFields(Mockito.any(PriceItem.class))).thenReturn(quotePriceItem);
        Mockito.when(mockPriceListCache.getEffectivePrice(Mockito.any(PriceItem.class), Mockito.any(Quote.class)))
            .thenReturn(Double.toString(price));
        Mockito.when(mockPriceListCache.findByKeyFields(Mockito.any(PriceItem.class))).thenReturn(quotePriceItem);
        Mockito.when(mockPriceListCache.getEffectivePrice(Mockito.any(PriceItem.class), Mockito.any(Quote.class)))
            .thenReturn(Double.toString(price));

        QuoteDetailsHelper.QuoteDetail quoteDetails = quoteDetailsHelper.getQuoteDetails(quoteId, actionBean);
        QuoteDetailsHelper.FundingInfo fundingInfo = quoteDetails.fundingDetails.iterator().next();
        assertThat(fundingInfo.isQuoteWarning(), is(true));
        assertThat(quoteDetails.outstandingEstimate, equalTo(price * 2));
        assertThat(quoteDetails.fundsRemaining, equalTo(86042.5d));
        assertThat(fundingInfo.getFundingInfoString(), containsString("Funds Reservation [<b>FR -- 1383, CO -- 5210500</b>]"));
        assertThat(fundingInfo.getFundingInfoString(), containsString("Active"));
        assertThat(fundingInfo.getFundingInfoString(), containsString("-- Expired"));
        assertThat(fundingInfo.getFundingInfoString(), containsString("funding split percentage = 100%"));
    }

    public void testQuoteServerExpiringSoon() throws Exception {
        String quoteId = "MMM2OU";

        double price = 10d;
        Quote quote = stubbedQuoteService.getQuoteByAlphaId(quoteId);
        quote.getFunding().iterator().next().setGrantEndDate(Date.from(Instant.now().plus(10L, ChronoUnit.DAYS)));

        QuoteService quoteService = Mockito.mock(QuoteService.class);
        Mockito.when(quoteService.getQuoteByAlphaId(Mockito.anyString())).thenReturn(quote);
        quoteDetailsHelper = new QuoteDetailsHelper(quoteService, null, templateEngine);


        QuoteItem quoteItem = quote.getQuoteItems().iterator().next();
        QuotePriceItem quotePriceItem = new QuotePriceItem(quoteItem.getCategoryName(), quoteItem.getPriceItemId(),
            quoteItem.getName(), quoteItem.getPrice(), "EA", quoteItem.getPlatform());
        Mockito.when(mockPriceListCache.findByKeyFields(Mockito.any(PriceItem.class))).thenReturn(quotePriceItem);
        Mockito.when(mockPriceListCache.getEffectivePrice(Mockito.any(PriceItem.class), Mockito.any(Quote.class)))
            .thenReturn(Double.toString(price));
        Mockito.when(mockPriceListCache.findByKeyFields(Mockito.any(PriceItem.class))).thenReturn(quotePriceItem);
        Mockito.when(mockPriceListCache.getEffectivePrice(Mockito.any(PriceItem.class), Mockito.any(Quote.class)))
            .thenReturn(Double.toString(price));

        QuoteDetailsHelper.QuoteDetail quoteDetails = quoteDetailsHelper.getQuoteDetails(quoteId, actionBean);
        QuoteDetailsHelper.FundingInfo fundingInfo = quoteDetails.fundingDetails.iterator().next();
        assertThat(fundingInfo.isQuoteWarning(), is(true));
        assertThat(quoteDetails.outstandingEstimate, equalTo(price * 2));
        assertThat(quoteDetails.fundsRemaining, equalTo(86042.5d));
        assertThat(fundingInfo.getFundingInfoString(), containsString("Funds Reservation [<b>FR -- 1383, CO -- 5210500</b>]"));
        assertThat(fundingInfo.getFundingInfoString(), containsString("Active"));
        assertThat(fundingInfo.getFundingInfoString(), containsString("-- Expires in 10 days. If it is likely this work will not be completed by then, please work on updating the Funding Source so billing errors can be avoided."));

        assertThat(fundingInfo.getFundingInfoString(), containsString("funding split percentage = 100%"));
    }

}
