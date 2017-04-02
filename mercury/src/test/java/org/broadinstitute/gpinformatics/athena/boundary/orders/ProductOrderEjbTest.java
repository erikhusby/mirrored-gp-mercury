package org.broadinstitute.gpinformatics.athena.boundary.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderKitTest;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductOrderJiraUtil;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductTestUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImplDBFreeTest;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderEjbTest {
    private static final BSPUserList.QADudeUser qaDudeUser = new BSPUserList.QADudeUser("PM", 2423L);
    private static final UserBean mockUserBean = Mockito.mock(UserBean.class);
    private ProductOrderDao productOrderDaoMock = Mockito.mock(ProductOrderDao.class);
    public final MercurySampleDao mockMercurySampleDao = Mockito.mock(MercurySampleDao.class);
    public final QuoteServiceImpl mockQuoteService = Mockito.mock(QuoteServiceImpl.class);
    public final SapIntegrationService mockSapService = Mockito.mock(SapIntegrationService.class);
    public final AppConfig mockAppConfig = Mockito.mock(AppConfig.class);
    public final EmailSender mockEmailSender = Mockito.mock(EmailSender.class);
    public final SAPAccessControlEjb mockAccessController = Mockito.mock(SAPAccessControlEjb.class);
    public final PriceListCache priceListCache = new PriceListCache(mockQuoteService);
    ProductOrderEjb productOrderEjb = new ProductOrderEjb(productOrderDaoMock, null, mockQuoteService,
            JiraServiceProducer.stubInstance(), mockUserBean, null, null, null, mockMercurySampleDao,
            new ProductOrderJiraUtil(JiraServiceProducer.stubInstance(), mockUserBean),
            mockSapService, priceListCache);

    private static final String[] sampleNames = {"SM-1234", "SM-5678", "SM-9101", "SM-1112"};
    ProductOrder productOrder = null;
    private Log logger = LogFactory.getLog(ProductOrderEjbTest.class);

    @BeforeMethod
    public void setUp() throws Exception {

        Mockito.when(mockAccessController.getCurrentControlDefinitions()).thenReturn(new SAPAccessControl());

        productOrderEjb.setAppConfig(mockAppConfig);
        productOrderEjb.setEmailSender(mockEmailSender);
        productOrderEjb.setAccessController(mockAccessController);
        productOrderEjb.setDeployment(Deployment.DEV);
    }

    public void testUpdateKitInfo() throws Exception {

        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));

        ProductOrder productOrder = ProductOrderDBTestFactory.createTestProductOrder(
                ResearchProjectTestFactory.createTestResearchProject(),
                ProductTestFactory.createDummyProduct(Workflow.NONE, Product.SAMPLE_INITIATION_PART_NUMBER));

        Set<ProductOrderKitDetail> originalKitDetailSet = new HashSet<>();
        ProductOrderKitDetail kitDetail = new ProductOrderKitDetail(5L, KitType.DNA_MATRIX, 87L,
                ProductOrderKitTest.materialInfoDto);
        kitDetail.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.PICO_RECEIVED));
        kitDetail.setProductOrderKitDetailId(4243L);

        ProductOrderKitDetail kitDetailToDelete = new ProductOrderKitDetail(4L, KitType.DNA_MATRIX, 187L,
                ProductOrderKitTest.materialInfoDto);
        kitDetailToDelete.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.PICO_RECEIVED));
        kitDetailToDelete.setProductOrderKitDetailId(2243L);
        originalKitDetailSet.add(kitDetail);
        originalKitDetailSet.add(kitDetailToDelete);

        ProductOrderKit orderKit = new ProductOrderKit(33L, 44L);
        orderKit.setKitOrderDetails(originalKitDetailSet);
        productOrder.setProductOrderKit(orderKit);

        Set<ProductOrderKitDetail> kitDetailSet = new HashSet<>();

        ProductOrderKitDetail kitDetailChange1 = new ProductOrderKitDetail(6L, KitType.DNA_MATRIX, 88L,
                ProductOrderKitTest.materialInfoDto);
        kitDetailChange1.getPostReceiveOptions()
                .addAll(Collections.singleton(PostReceiveOption.FLUIDIGM_FINGERPRINTING));
        kitDetailChange1.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.BIOANALYZER));
//        kitDetail.setProductOrderKitDetaild(3243L);

        ProductOrderKitDetail kitDetailChange2 = new ProductOrderKitDetail(7L, KitType.DNA_MATRIX, 89L,
                ProductOrderKitTest.materialInfoDto);
        kitDetailChange2.getPostReceiveOptions()
                .addAll(Collections.singleton(PostReceiveOption.FLUIDIGM_FINGERPRINTING));
        kitDetailChange2.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.BIOANALYZER));
        kitDetailChange2.setProductOrderKitDetailId(4243L);

        kitDetailSet.add(kitDetailChange1);
        kitDetailSet.add(null);
        kitDetailSet.add(kitDetailChange2);

        productOrderEjb.persistProductOrder(ProductOrder.SaveType.UPDATING, productOrder, Collections.singleton("2243"),
                kitDetailSet);

        Assert.assertEquals(productOrder.getProductOrderKit().getKitOrderDetails().size(), 2);
        for (ProductOrderKitDetail kitDetailToTest : productOrder.getProductOrderKit().getKitOrderDetails()) {

            Assert.assertNotEquals(kitDetailToTest.getProductOrderKitDetailId(), 2243L);

            if (kitDetailToTest.getProductOrderKitDetailId() != null &&
                kitDetailToTest.getProductOrderKitDetailId().equals(4243L)) {
                Assert.assertEquals((Long) kitDetailToTest.getOrganismId(), (Long) 89L);
                Assert.assertEquals((Long) kitDetailToTest.getNumberOfSamples(), (Long) 7L);
            } else {
                Assert.assertEquals((Long) kitDetailToTest.getOrganismId(), (Long) 88L);
                Assert.assertEquals((Long) kitDetailToTest.getNumberOfSamples(), (Long) 6L);
            }

            Assert.assertEquals(kitDetailToTest.getPostReceiveOptions().size(), 2);
        }
    }

    public void testCreatePDOUpdateFieldForQuote() {
        ProductOrder pdo = new ProductOrder();
        pdo.setQuoteId("DOUGH");
        ProductOrderEjb pdoEjb = new ProductOrderEjb();
        PDOUpdateField updateField = PDOUpdateField.createPDOUpdateFieldForQuote(pdo);

        Assert.assertEquals(updateField.getNewValue(), pdo.getQuoteId());

        pdo.setQuoteId(null);

        updateField = PDOUpdateField.createPDOUpdateFieldForQuote(pdo);
        Assert.assertEquals(updateField.getNewValue(), ProductOrder.QUOTE_TEXT_USED_IN_JIRA_WHEN_QUOTE_FIELD_IS_EMPTY);
    }

    public void testValidateQuote() throws Exception {
        ProductOrder pdo = new ProductOrder();
        pdo.setQuoteId("CASH");
        ProductOrderEjb pdoEjb = new ProductOrderEjb();
        pdoEjb.validateQuote(pdo, new QuoteServiceStub());
    }

    public void testJiraCommentString() {
        String jiraComment = productOrderEjb.buildJiraCommentForRiskString("at risk", "QADudeDEV", 2, true);
        Assert.assertEquals(jiraComment, "QADudeDEV set manual on risk to true for 2 samples with comment:\nat risk");
    }

    private List<ProductOrderSample> setupRiskTests() {
        productOrder = ProductOrderTestFactory.createProductOrder(sampleNames);

        Mockito.when(productOrderDaoMock.findByBusinessKey(productOrder.getBusinessKey())).thenReturn(productOrder);
        return productOrder.getSamples().subList(0, 2);
    }

    public void testManualAtRisk() throws IOException {
        List<ProductOrderSample> productOrderSamples = setupRiskTests();
        productOrderEjb
                .addManualOnRisk(qaDudeUser, productOrder.getJiraTicketKey(), productOrderSamples, true, "is risk");
        Assert.assertEquals(productOrder.countItemsOnRisk(), productOrderSamples.size());
    }

    public void testManualNoRisk() throws IOException {
        List<ProductOrderSample> productOrderSamples = setupRiskTests();
        productOrderEjb
                .addManualOnRisk(qaDudeUser, productOrder.getJiraTicketKey(), productOrderSamples, false, "no risk");
        Assert.assertEquals(productOrder.countItemsOnRisk(), 0);
    }

    public void testAbandonAndUnabandonSamples() throws Exception {
        ProductOrder testOrder =
                ProductOrderTestFactory.createProductOrder("SM-toAbandon1", "SM-toAbandon2", "SM-toAbandon3",
                        "SM-toAbandon4", "SM-toAbandon5");

        for (ProductOrderSample sample : testOrder.getSamples()) {
            Assert.assertEquals(sample.getDeliveryStatus(), ProductOrderSample.DeliveryStatus.NOT_STARTED);
        }

        Mockito.when(productOrderDaoMock.findByBusinessKey(Mockito.anyString())).thenReturn(testOrder);
        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));

        productOrderEjb.abandonSamples(testOrder.getBusinessKey(), testOrder.getSamples(), "To be Abandoned in Test");

        for (ProductOrderSample sample : testOrder.getSamples()) {
            Assert.assertEquals(sample.getDeliveryStatus(), ProductOrderSample.DeliveryStatus.ABANDONED);
        }

        MessageReporter mockReporter = Mockito.mock(MessageReporter.class);
        Mockito.when(mockReporter.addMessage(Mockito.anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {

                logger.info((String) invocationOnMock.getArguments()[0]);
                return "";
            }
        });

        ProductOrderSampleDao productOrderSampleDao = Mockito.mock(ProductOrderSampleDao.class);
        Mockito.when(productOrderSampleDao.findListByList(Mockito.eq(ProductOrderSample.class),
                Mockito.eq(ProductOrderSample_.productOrderSampleId), Mockito.anyCollection())).thenReturn(
                testOrder.getSamples());
        productOrderEjb.setProductOrderSampleDao(productOrderSampleDao);

        productOrderEjb.unAbandonSamples(testOrder.getBusinessKey(),
                ProductOrderSample.getSampleIDs(testOrder.getSamples()), "to not be abandoned anymore",
                mockReporter);

        for (ProductOrderSample sample : testOrder.getSamples()) {
            Assert.assertEquals(sample.getDeliveryStatus(), ProductOrderSample.DeliveryStatus.NOT_STARTED);
        }
    }

    public void test_Add_Samples_Without_Bound_Mercury_Samples() throws Exception {
        String jiraTicketKey = "PDO-testMe";
        MessageReporter mockReporter = Mockito.mock(MessageReporter.class);

        ProductOrder order = ProductOrderTestFactory.createDummyProductOrder(0, jiraTicketKey);

        Mockito.when(productOrderDaoMock.findByBusinessKey(Mockito.eq(jiraTicketKey))).thenReturn(order);
        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));
        Funding funding = new Funding(Funding.PURCHASE_ORDER, "238293", null);
        funding.setPurchaseOrderContact("Test@broad.com");
        Set<FundingLevel> fundingLevels = Collections.singleton(new FundingLevel("100", funding));
        QuoteFunding quoteFunding = new QuoteFunding(fundingLevels);
        Quote mockQuote =  new Quote(order.getQuoteId(), quoteFunding, ApprovalStatus.FUNDED);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(Mockito.anyString())).thenReturn(
                mockQuote);

        String[] sampleNames = {"SM-smpl1", "SM-smpl2", "SM-smpl3", "SM-smpl4", "SM-smpl5"};
        List<ProductOrderSample> samples = ProductOrderSampleTestFactory.createDBFreeSampleList(sampleNames);
        Map<String, MercurySample> sampleMap = new HashMap<>();
        for (String sampleName : sampleNames) {
            sampleMap.put(sampleName, new MercurySample(sampleName, MercurySample.MetadataSource.BSP));
        }
        Mockito.when(mockMercurySampleDao.findMapIdToMercurySample(Mockito.eq(sampleMap.keySet()))).thenReturn(sampleMap);

        assertThat(order.getSamples(), is(empty()));

        productOrderEjb.addSamples(jiraTicketKey, samples, mockReporter);
        assertThat(order.getSamples(), is(not(empty())));
        assertThat(order.getSamples().size(), is(Matchers.equalTo(5)));
        for (ProductOrderSample sample : order.getSamples()) {
            assertThat(sample.getMercurySample(), is(not(nullValue())));
            assertThat(sample.getMercurySample().getSampleKey(), is(equalTo(sample.getBusinessKey())));
        }
        Mockito.verify(mockMercurySampleDao).findMapIdToMercurySample(Mockito.eq(sampleMap.keySet()));
    }

    public void testCreateOrderInSap() throws Exception {

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();

        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));

        Quote testSingleSourceQuote;
        String testUser = "Scott.G.MATThEws@GMail.CoM";
        Funding fundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        fundingDefined.setPurchaseOrderContact(testUser);
        fundingDefined.setPurchaseOrderNumber("PO00Id8923");
        FundingLevel fundingLevel = new FundingLevel("100",fundingDefined);

        QuoteFunding quoteFunding = new QuoteFunding(Collections.singleton(fundingLevel));

        testSingleSourceQuote = new Quote(SapIntegrationServiceImplDBFreeTest.SINGLE_SOURCE_PO_QUOTE_ID, quoteFunding, ApprovalStatus.FUNDED);

        Quote testSingleSourceQuote2;

        testSingleSourceQuote2 = new Quote(SapIntegrationServiceImplDBFreeTest.SINGLE_SOURCE_PO_QUOTE_ID+"2", quoteFunding, ApprovalStatus.FUNDED);

        Quote testSingleSourceQuote3;

        testSingleSourceQuote3 = new Quote(SapIntegrationServiceImplDBFreeTest.SINGLE_SOURCE_PO_QUOTE_ID+"3", quoteFunding, ApprovalStatus.FUNDED);

        Mockito.when(mockSapService.createOrder(Mockito.any(ProductOrder.class))).thenReturn(SapIntegrationServiceStub.TEST_SAP_NUMBER);
        Mockito.when(mockSapService.determineCompanyCode(Mockito.any(ProductOrder.class))).thenReturn(
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testSingleSourceQuote.getAlphanumericId())).thenReturn(testSingleSourceQuote);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testSingleSourceQuote2.getAlphanumericId())).thenReturn(testSingleSourceQuote2);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testSingleSourceQuote3.getAlphanumericId())).thenReturn(testSingleSourceQuote3);

        String jiraTicketKey= "PDO-SAP-test";
        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);
        conversionPdo.setQuoteId(testSingleSourceQuote.getAlphanumericId() );
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        priceList.add(new QuotePriceItem(conversionPdo.getProduct().getPrimaryPriceItem().getCategory(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "5", "test",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform()));
        quoteItems.add(new QuoteItem(testSingleSourceQuote.getAlphanumericId(),conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),"2000", "5","each",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform(),
                conversionPdo.getProduct().getPrimaryPriceItem().getCategory()));
        quoteItems.add(new QuoteItem(testSingleSourceQuote2.getAlphanumericId(),conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),"2000", "5","each",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform(),
                conversionPdo.getProduct().getPrimaryPriceItem().getCategory()));
        quoteItems.add(new QuoteItem(testSingleSourceQuote3.getAlphanumericId(),conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),"2000", "5","each",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform(),
                conversionPdo.getProduct().getPrimaryPriceItem().getCategory()));
        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            priceList.add(new QuotePriceItem(productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(), "5", "test",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform()));
            quoteItems.add(new QuoteItem(testSingleSourceQuote.getAlphanumericId(),productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),"2000", "5","each",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory()));
            quoteItems.add(new QuoteItem(testSingleSourceQuote2.getAlphanumericId(),productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),"2000", "5","each",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory()));
            quoteItems.add(new QuoteItem(testSingleSourceQuote3.getAlphanumericId(),productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),"2000", "5","each",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory()));
        }

        testSingleSourceQuote.setQuoteItems(quoteItems);
        testSingleSourceQuote2.setQuoteItems(quoteItems);
        testSingleSourceQuote3.setQuoteItems(quoteItems);

        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);

        MessageCollection messageCollection = new MessageCollection();
        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection, true);

        Assert.assertTrue(CollectionUtils.isNotEmpty(conversionPdo.getSapReferenceOrders()));
        Assert.assertEquals(conversionPdo.getSapReferenceOrders().size(), 1);

        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SINGLE_SOURCE_PO_QUOTE_ID+"2");

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection, false);
        Assert.assertEquals(conversionPdo.getSapReferenceOrders().size(), 2);

        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SINGLE_SOURCE_PO_QUOTE_ID+"2");

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection, true);
        Assert.assertEquals(conversionPdo.getSapReferenceOrders().size(), 2);

        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SINGLE_SOURCE_PO_QUOTE_ID+"3");

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection, false);
        Assert.assertEquals(conversionPdo.getSapReferenceOrders().size(), 3);

        Mockito.when(productOrderDaoMock.findByBusinessKey(jiraTicketKey)).thenReturn(conversionPdo);

        productOrderEjb.abandon(jiraTicketKey, "testing");

        Mockito.verify(mockEmailSender, Mockito.times(3)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean());

    }

    public void testAbandonOrderWithNoServiceNowTicket() throws Exception {

        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));

        Quote testSingleSourceQuote;
        String testUser = "Scott.G.MATThEws@GMail.CoM";
        Funding fundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        fundingDefined.setPurchaseOrderContact(testUser);
        fundingDefined.setPurchaseOrderNumber("PO00Id8923");
        FundingLevel fundingLevel = new FundingLevel("100",fundingDefined);

        QuoteFunding quoteFunding = new QuoteFunding(Collections.singleton(fundingLevel));

        testSingleSourceQuote = new Quote(SapIntegrationServiceImplDBFreeTest.SINGLE_SOURCE_PO_QUOTE_ID, quoteFunding, ApprovalStatus.FUNDED);

        Mockito.when(mockQuoteService.getQuoteByAlphaId(testSingleSourceQuote.getAlphanumericId())).thenReturn(testSingleSourceQuote);

        String jiraTicketKey= "PDO-SAP-test";
        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(1, jiraTicketKey);
        conversionPdo.setQuoteId(testSingleSourceQuote.getAlphanumericId() );
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        Mockito.when(productOrderDaoMock.findByBusinessKey(jiraTicketKey)).thenReturn(conversionPdo);

        productOrderEjb.abandon(jiraTicketKey, "testing");

        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean());

        productOrderEjb.updateOrderStatus(jiraTicketKey, MessageReporter.UNUSED);
        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean());


    }
}
