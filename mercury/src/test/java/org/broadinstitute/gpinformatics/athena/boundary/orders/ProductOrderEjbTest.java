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
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductOrderJiraUtil;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessItem;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceTestProducer;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImplDBFreeTest;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnector;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.MessageReporter;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.entity.order.SAPOrder;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE, singleThreaded = true)
public class ProductOrderEjbTest {
    private static final BSPUserList.QADudeUser qaDudeUser = new BSPUserList.QADudeUser("PM", 2423L);
    private static final UserBean mockUserBean = Mockito.mock(UserBean.class);
    private ProductOrderDao productOrderDaoMock;
    private MercurySampleDao mockMercurySampleDao;
    private QuoteServiceImpl mockQuoteService;
    private SapIntegrationService sapService;
    private SapIntegrationClientImpl mockSapClient;

    private AppConfig mockAppConfig;
    private SapConfig mockSapConfig;
    private EmailSender mockEmailSender;
    private SAPAccessControlEjb mockAccessController;
    private PriceListCache priceListCache;
    private SAPProductPriceCache productPriceCache;
    private ProductOrderEjb productOrderEjb;

    private static final String[] sampleNames = {"SM-1234", "SM-5678", "SM-9101", "SM-1112"};
    private ProductOrder productOrder;
    SapIntegrationClientImpl.SAPCompanyConfiguration companyCode = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;

    private Log logger = LogFactory.getLog(ProductOrderEjbTest.class);

    @BeforeMethod
    public void setUp() throws Exception {
        mockEmailSender = Mockito.mock(EmailSender.class);
        mockSapConfig = Mockito.mock(SapConfig.class);
        BSPUserList bspUserList = Mockito.mock(BSPUserList.class);
        productPriceCache = new SAPProductPriceCache(sapService);
        priceListCache = new PriceListCache(mockQuoteService);
        mockSapClient = Mockito.mock(SapIntegrationClientImpl.class);
        mockQuoteService = Mockito.mock(QuoteServiceImpl.class);
        mockAccessController = Mockito.mock(SAPAccessControlEjb.class);
        Mockito.when(mockAccessController.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        sapService = new SapIntegrationServiceImpl(mockSapConfig, bspUserList, priceListCache, productPriceCache,
                mockAccessController);
        productPriceCache = new SAPProductPriceCache(sapService);

        productPriceCache = new SAPProductPriceCache(sapService);
        priceListCache = new PriceListCache(mockQuoteService);

        //  this additon is for the temporary support of current processing of recognizing a valid Product.
        //  When the full implementation of the fetchMatarials interface for SAP is completed, this will be removed
        mockMercurySampleDao = Mockito.mock(MercurySampleDao.class);
        productOrderDaoMock = Mockito.mock(ProductOrderDao.class);
        productOrderEjb = new ProductOrderEjb(productOrderDaoMock, null, mockQuoteService,
                JiraServiceTestProducer.stubInstance(), mockUserBean, null, null, null, mockMercurySampleDao,
                new ProductOrderJiraUtil(JiraServiceTestProducer.stubInstance(), mockUserBean),
                sapService, priceListCache, productPriceCache);
        mockAppConfig = Mockito.mock(AppConfig.class);
        productOrderEjb.setAppConfig(mockAppConfig);
        productOrderEjb.setSapConfig(mockSapConfig);
        productOrderEjb.setEmailSender(mockEmailSender);
        productOrderEjb.setAccessController(mockAccessController);
        productOrderEjb.setDeployment(Deployment.DEV);

        ((SapIntegrationServiceImpl)sapService).setWrappedClient(mockSapClient);

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

        final ProductOrderDao productOrderDaoMock = Mockito.mock(ProductOrderDao.class);
        final ProductDao productDaoMock = Mockito.mock(ProductDao.class);
        final QuoteService quoteService= new QuoteServiceStub();
        final JiraService jiraService= Mockito.mock(JiraService.class);
        final UserBean userBean= Mockito.mock(UserBean.class);
        final BSPUserList userList= Mockito.mock(BSPUserList.class);
        final BucketEjb bucketEjb= Mockito.mock(BucketEjb.class);
        final SquidConnector squidConnector= Mockito.mock(SquidConnector.class);
        final MercurySampleDao mercurySampleDao= Mockito.mock(MercurySampleDao.class);
        final ProductOrderJiraUtil productOrderJiraUtil= Mockito.mock(ProductOrderJiraUtil.class);
        final SapIntegrationService sapService= new SapIntegrationServiceStub();
        final PriceListCache priceListCache= Mockito.mock(PriceListCache.class);
        final SAPProductPriceCache productPriceCache= Mockito.mock(SAPProductPriceCache.class);

        ProductOrderEjb pdoEjb =
                new ProductOrderEjb(productOrderDaoMock, productDaoMock, quoteService, jiraService, userBean, userList,
                        bucketEjb, squidConnector, mercurySampleDao, productOrderJiraUtil, sapService, priceListCache,
                        productPriceCache);
        pdoEjb.validateQuote(pdo);
    }

    public void testValidateSapQuote() throws Exception {
        ProductOrder pdo = new ProductOrder();
        pdo.setQuoteId("SAPCASH");

        final ProductOrderDao productOrderDaoMock = Mockito.mock(ProductOrderDao.class);
        final ProductDao productDaoMock = Mockito.mock(ProductDao.class);
        final QuoteService quoteService= new QuoteServiceStub();
        final JiraService jiraService= Mockito.mock(JiraService.class);
        final UserBean userBean= Mockito.mock(UserBean.class);
        final BSPUserList userList= Mockito.mock(BSPUserList.class);
        final BucketEjb bucketEjb= Mockito.mock(BucketEjb.class);
        final SquidConnector squidConnector= Mockito.mock(SquidConnector.class);
        final MercurySampleDao mercurySampleDao= Mockito.mock(MercurySampleDao.class);
        final ProductOrderJiraUtil productOrderJiraUtil= Mockito.mock(ProductOrderJiraUtil.class);
        final SapIntegrationService sapService= new SapIntegrationServiceStub();
        final PriceListCache priceListCache= Mockito.mock(PriceListCache.class);
        final SAPProductPriceCache productPriceCache= Mockito.mock(SAPProductPriceCache.class);

        ProductOrderEjb pdoEjb =
                new ProductOrderEjb(productOrderDaoMock, productDaoMock, quoteService, jiraService, userBean, userList,
                        bucketEjb, squidConnector, mercurySampleDao, productOrderJiraUtil, sapService, priceListCache,
                        productPriceCache);
        pdoEjb.validateQuote(pdo);
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

        MessageCollection testMessaging = new MessageCollection();

        productOrderEjb.abandonSamples(testOrder.getBusinessKey(), testOrder.getSamples(), "To be Abandoned in Test",
                testMessaging);

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
                testMessaging);

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
        Set<FundingLevel> fundingLevels = Collections.singleton(new FundingLevel("100", Collections.singleton(funding)));
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
        Mockito.verify(mockQuoteService, Mockito.times(0)).getQuoteByAlphaId(Mockito.anyString());
    }

    @DataProvider(name = "willQuoteSwapBad")
    public Iterator<Object[]> willQuoteSwapBad() {
        List<Object[]> testScenarios = new ArrayList<>();

        testScenarios.add(new Object[]{true});  //later in the process, swap the quote for a bad quote.
        testScenarios.add(new Object[]{false});

        return testScenarios.iterator();
    }

    @Test(dataProvider = "willQuoteSwapBad")
    public void testCreateOrderInSap(Boolean quoteSwapsForBadQuote) throws Exception {

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();

        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));


        Mockito.when(mockSapClient.createSAPOrder(Mockito.any(SAPOrder.class))).thenReturn(SapIntegrationServiceStub.TEST_SAP_NUMBER);

        String jiraTicketKey= "PDO-SAP-test";
        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);
        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SAP_QUOTE_ID);
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        conversionPdo.setPriorToSAP1_5(Boolean.TRUE);

        Mockito.when(productOrderDaoMock.findByBusinessKey(jiraTicketKey)).thenReturn(conversionPdo);

        Set<SAPMaterial> returnMaterials = new HashSet<>();

        Product conversionPdoProduct = conversionPdo.getProduct();
        final SAPMaterial primaryMaterial =
            new SAPMaterial(conversionPdoProduct.getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                conversionPdoProduct.getDescription(), "10", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,
                new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                companyCode.getSalesOrganization());

        returnMaterials.add(
                primaryMaterial);
        priceList.add(new QuotePriceItem(conversionPdo.getProduct().getPrimaryPriceItem().getCategory(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "10", "test",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform()));
        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            final SAPMaterial addonMaterial =
                new SAPMaterial(productOrderAddOn.getAddOn().getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                    productOrderAddOn.getAddOn().getDescription(), "10", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA,
                    BigDecimal.ONE, new Date(), new Date(),
                    Collections.emptyMap(), Collections.emptyMap(),
                    SAPMaterial.MaterialStatus.ENABLED, companyCode.getSalesOrganization());

            returnMaterials.add(addonMaterial);
            priceList.add(new QuotePriceItem(productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(), "10", "test",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform()));
        }

        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        productPriceCache.refreshCache();

        MessageCollection messageCollection = new MessageCollection();

        Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                .thenReturn(TestUtils.buildTestSapQuote(conversionPdo.getQuoteId(),
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000000),conversionPdo,
                        TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS,companyCode.getSalesOrganization()));

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection);
        conversionPdo.setPriorToSAP1_5(Boolean.TRUE);

        Assert.assertTrue(conversionPdo.getPriorToSAP1_5());

        Assert.assertTrue(CollectionUtils.isNotEmpty(conversionPdo.getSapReferenceOrders()));
        Assert.assertEquals(conversionPdo.getSapReferenceOrders().size(), 1);

        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SAP_QUOTE_ID+"2");

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection);
        Assert.assertEquals(conversionPdo.getSapReferenceOrders().size(), 2);

        Assert.assertFalse(conversionPdo.getPriorToSAP1_5());

        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SAP_QUOTE_ID+"2");

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection);
        Assert.assertEquals(conversionPdo.getSapReferenceOrders().size(), 2);

        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SAP_QUOTE_ID+"3");

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection);
        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyBoolean());
        Assert.assertEquals(conversionPdo.getSapReferenceOrders().size(), 3);

        if(quoteSwapsForBadQuote) {
            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                    .thenReturn(TestUtils.buildTestSapQuote(conversionPdo.getQuoteId(),
                            BigDecimal.valueOf(10000), BigDecimal.valueOf(1000000), conversionPdo,
                            TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER, companyCode.getSalesOrganization()));
        }

        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SAP_QUOTE_ID+"4");

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection);
        Mockito.verify(mockEmailSender, Mockito.times(((quoteSwapsForBadQuote)?1:0))).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyBoolean());
        Assert.assertEquals(conversionPdo.getSapReferenceOrders().size(), 4);


        for (SAPMaterial sapMaterial : returnMaterials) {
            sapMaterial.setBasePrice("10");
        }

        TestUtils.billSampleOut(conversionPdo, conversionPdo.getSamples().iterator().next(), conversionPdo.getSamples().size());

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection);
        Mockito.verify(mockEmailSender, Mockito.times(((quoteSwapsForBadQuote)?1:0))).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyBoolean());
        Assert.assertEquals(conversionPdo.getSapReferenceOrders().size(), 4);


        productOrderEjb.abandon(jiraTicketKey, "testing");

        Mockito.verify(mockEmailSender, Mockito.times((quoteSwapsForBadQuote)?2:0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyBoolean());
    }

    public void testAbandonOrderWithServiceNowTicket() throws Exception {

        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));
        Mockito.when(mockSapClient.createSAPOrder(Mockito.any(SAPOrder.class))).thenReturn(SapIntegrationServiceStub.TEST_SAP_NUMBER);
        
        String jiraTicketKey= "PDO-SAP-test";
        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(1, jiraTicketKey);

        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SAP_QUOTE_ID );
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        Set<SAPMaterial> returnMaterials = new HashSet<>();
        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Product conversionPdoProduct = conversionPdo.getProduct();

        final SAPMaterial primaryMaterial =
            new SAPMaterial(conversionPdoProduct.getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                conversionPdoProduct.getDescription(), "10", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,
                new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                companyCode.getSalesOrganization());

        primaryMaterial.updateCompanyConfiguration(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        returnMaterials.add(
                primaryMaterial);
        priceList.add(new QuotePriceItem(conversionPdo.getProduct().getPrimaryPriceItem().getCategory(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "10", "test",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform()));
        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            final SAPMaterial addonMaterial = new SAPMaterial(productOrderAddOn.getAddOn().getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                                productOrderAddOn.getAddOn().getDescription(), "10", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA,
                                BigDecimal.ONE, new Date(), new Date(),
                                Collections.emptyMap(), Collections.emptyMap(),
                                SAPMaterial.MaterialStatus.ENABLED, companyCode.getSalesOrganization());
            returnMaterials.add(addonMaterial);
            priceList.add(new QuotePriceItem(productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(), "10", "test",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform()));
        }

        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        productPriceCache.refreshCache();

        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                .thenReturn(TestUtils.buildTestSapQuote(conversionPdo.getQuoteId(),
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000000),conversionPdo,
                        TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS,companyCode.getSalesOrganization()));


        productOrderEjb.publishProductOrderToSAP(conversionPdo, new MessageCollection());
        conversionPdo.setPriorToSAP1_5(true);

        Mockito.when(productOrderDaoMock.findByBusinessKey(jiraTicketKey)).thenReturn(conversionPdo);

        productOrderEjb.abandon(jiraTicketKey, "testing");

        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(), 
                Mockito.anyBoolean());

        Mockito.verify(mockQuoteService, Mockito.times(0)).getQuoteByAlphaId(Mockito.anyString());

        productOrderEjb.updateOrderStatus(jiraTicketKey, MessageReporter.UNUSED);
        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(), 
                Mockito.anyBoolean());
    }


    public void testAbandonOrderWithPost1Pt5() throws Exception {

        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));
        Mockito.when(mockSapClient.createSAPOrder(Mockito.any(SAPOrder.class))).thenReturn(SapIntegrationServiceStub.TEST_SAP_NUMBER);

        String jiraTicketKey= "PDO-SAP-test";
        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(1, jiraTicketKey);

        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SAP_QUOTE_ID );
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        Set<SAPMaterial> returnMaterials = new HashSet<>();
        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Product conversionPdoProduct = conversionPdo.getProduct();

        final SAPMaterial primaryMaterial =
            new SAPMaterial(conversionPdoProduct.getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                conversionPdoProduct.getDescription(), "10", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,
                new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                companyCode.getSalesOrganization());

        primaryMaterial.updateCompanyConfiguration(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        returnMaterials.add(
                primaryMaterial);
        priceList.add(new QuotePriceItem(conversionPdo.getProduct().getPrimaryPriceItem().getCategory(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "10", "test",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform()));

        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            final SAPMaterial addonMaterial = new SAPMaterial(productOrderAddOn.getAddOn().getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                                productOrderAddOn.getAddOn().getDescription(), "10", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA,
                                BigDecimal.ONE, new Date(), new Date(),
                                Collections.emptyMap(), Collections.emptyMap(),
                                SAPMaterial.MaterialStatus.ENABLED, companyCode.getSalesOrganization());
            returnMaterials.add(addonMaterial);
            priceList.add(new QuotePriceItem(productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(), "10", "test",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform()));
        }

        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        productPriceCache.refreshCache();

        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                .thenReturn(TestUtils.buildTestSapQuote(conversionPdo.getQuoteId(),
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000000),conversionPdo,
                        TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS,companyCode.getSalesOrganization()));

        productOrderEjb.publishProductOrderToSAP(conversionPdo, new MessageCollection());

        Mockito.when(productOrderDaoMock.findByBusinessKey(jiraTicketKey)).thenReturn(conversionPdo);

        productOrderEjb.abandon(jiraTicketKey, "testing");

        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyBoolean());

        Mockito.verify(mockQuoteService, Mockito.times(0)).getQuoteByAlphaId(Mockito.anyString());

        productOrderEjb.updateOrderStatus(jiraTicketKey, MessageReporter.UNUSED);
        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyBoolean());
    }


    public void testAbandonOrderWithPost1Pt5BlockedProduct() throws Exception {

        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));
        Mockito.when(mockSapClient.createSAPOrder(Mockito.any(SAPOrder.class))).thenReturn(SapIntegrationServiceStub.TEST_SAP_NUMBER);

        String jiraTicketKey= "PDO-SAP-test";
        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(1, jiraTicketKey);

        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SAP_QUOTE_ID );
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        Set<SAPMaterial> returnMaterials = new HashSet<>();
        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Product conversionPdoProduct = conversionPdo.getProduct();

        final SAPMaterial primaryMaterial =
            new SAPMaterial(conversionPdoProduct.getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                conversionPdoProduct.getDescription(), "10", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,
                new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                companyCode.getSalesOrganization());

        returnMaterials.add(
                primaryMaterial);
        priceList.add(new QuotePriceItem(conversionPdo.getProduct().getPrimaryPriceItem().getCategory(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "10", "test",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform()));
;
        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            final SAPMaterial addonMaterial = new SAPMaterial(productOrderAddOn.getAddOn().getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                                productOrderAddOn.getAddOn().getDescription(), "10", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA,
                                BigDecimal.ONE, new Date(), new Date(),
                                Collections.emptyMap(), Collections.emptyMap(),
                                SAPMaterial.MaterialStatus.ENABLED, companyCode.getSalesOrganization());
            returnMaterials.add(addonMaterial);
            priceList.add(new QuotePriceItem(productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(), "10", "test",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform()));
;
        }

        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        productPriceCache.refreshCache();
        Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                .thenReturn(TestUtils.buildTestSapQuote(conversionPdo.getQuoteId(),
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000000),conversionPdo,
                        TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS,companyCode.getSalesOrganization()));


        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);

        productOrderEjb.publishProductOrderToSAP(conversionPdo, new MessageCollection());

        final SAPAccessControl control = new SAPAccessControl();
        AccessItem orderAccessItem = new AccessItem(conversionPdo.getProduct().getPrimaryPriceItem().getName());
        control.setDisabledItems(Collections.singleton(orderAccessItem));


        Mockito.when(productOrderDaoMock.findByBusinessKey(jiraTicketKey)).thenReturn(conversionPdo);

        productOrderEjb.abandon(jiraTicketKey, "testing");

        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(), 
                Mockito.anyBoolean());

        Mockito.verify(mockQuoteService, Mockito.times(0)).getQuoteByAlphaId(Mockito.anyString());

        productOrderEjb.updateOrderStatus(jiraTicketKey, MessageReporter.UNUSED);
        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(), 
                Mockito.anyBoolean());

    }
    public void testTransitionOrderToClosed() throws Exception {
        String jiraTicketKey= "PDO-SAP-test";
        String childOneJiraTicketKey = "PDO-SAP-Childone";
        String childtwoJiraTicketKey = "PDO-SAP-Childtwo";
        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();

        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);
        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));
        Mockito.when(mockSapClient.createSAPOrder(Mockito.any(SAPOrder.class))).thenReturn(SapIntegrationServiceStub.TEST_SAP_NUMBER);

        Set<SAPMaterial> returnMaterials = new HashSet<>();
        Product conversionPdoProduct = conversionPdo.getProduct();

        final SAPMaterial primaryMaterial =
            new SAPMaterial(conversionPdoProduct.getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                conversionPdoProduct.getDescription(), "10", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,
                new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                companyCode.getSalesOrganization());

        returnMaterials.add(
                primaryMaterial);
        priceList.add(new QuotePriceItem(conversionPdo.getProduct().getPrimaryPriceItem().getCategory(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "10", "test",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform()));
        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            final SAPMaterial addonMaterial = new SAPMaterial(productOrderAddOn.getAddOn().getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                                productOrderAddOn.getAddOn().getDescription(), "10", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA,
                                BigDecimal.ONE, new Date(), new Date(),
                                Collections.emptyMap(), Collections.emptyMap(),
                                SAPMaterial.MaterialStatus.ENABLED, companyCode.getSalesOrganization());
            returnMaterials.add(addonMaterial);
            priceList.add(new QuotePriceItem(productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(), "10", "test",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform()));
        }

        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        productPriceCache.refreshCache();

        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SAP_QUOTE_ID );
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        MessageCollection messageCollection = new MessageCollection();

        Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                .thenReturn(TestUtils.buildTestSapQuote(conversionPdo.getQuoteId(),
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000000),conversionPdo,
                        TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS,companyCode.getSalesOrganization()));


        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection);
        assertThat(conversionPdo.getSapOrderNumber(), is(notNullValue()));

        int sampleCount = 0;
        for (ProductOrderSample productOrderSample : conversionPdo.getSamples()) {
            if(sampleCount >=2) {
                break;
            }
            productOrderSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);

            sampleCount++;
        }

        ProductOrder childPdoOne = ProductOrder.cloneProductOrder(conversionPdo, true);
        childPdoOne.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        childPdoOne.setSamples(ProductOrderSampleTestFactory.createSampleListWithMercurySamples("SM-3URTST"));
        childPdoOne.setJiraTicketKey(childOneJiraTicketKey);
        childPdoOne.updateQuoteItems(sapService.findSapQuote(childPdoOne.getQuoteId()));
        productOrderEjb.publishProductOrderToSAP(childPdoOne, messageCollection);

        ProductOrder childPdoTwo = ProductOrder.cloneProductOrder(conversionPdo, false);

        Mockito.when(mockSapClient.createSAPOrder(Mockito.any(SAPOrder.class))).thenReturn("Child_sap_test");
        childPdoTwo.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        childPdoTwo.setSamples(ProductOrderSampleTestFactory.createSampleListWithMercurySamples("SM-3BRTST"));
        childPdoTwo.setJiraTicketKey(childtwoJiraTicketKey);
        childPdoTwo.updateQuoteItems(sapService.findSapQuote(childPdoTwo.getQuoteId()));
        productOrderEjb.publishProductOrderToSAP(childPdoTwo, messageCollection);


        Mockito.when(productOrderDaoMock.findByBusinessKey(jiraTicketKey)).thenReturn(conversionPdo);
        Mockito.when(productOrderDaoMock.findByBusinessKey(childOneJiraTicketKey)).thenReturn(childPdoOne);
        Mockito.when(productOrderDaoMock.findByBusinessKey(childtwoJiraTicketKey)).thenReturn(childPdoTwo);

        assertThat(childPdoOne.getOrderStatus(), equalTo(ProductOrder.OrderStatus.Submitted));
        for (ProductOrderSample childOneSample : childPdoOne.getSamples()) {
            ProductOrderSampleTestFactory.markAsBilled(childOneSample);
        }
        productOrderEjb.updateOrderStatus(childPdoOne.getBusinessKey(), MessageReporter.UNUSED);
        assertThat(childPdoOne.getOrderStatus(), equalTo(ProductOrder.OrderStatus.Completed));


        for (ProductOrderSample child2Sample : childPdoTwo.getSamples()) {
            ProductOrderSampleTestFactory.markAsBilled(child2Sample);
        }

        assertThat(childPdoTwo.getOrderStatus(), equalTo(ProductOrder.OrderStatus.Submitted));
        productOrderEjb.updateOrderStatus(childPdoTwo.getBusinessKey(), MessageReporter.UNUSED);
        assertThat(childPdoTwo.getOrderStatus(), equalTo(ProductOrder.OrderStatus.Completed));


        assertThat(conversionPdo.getOrderStatus(), equalTo(ProductOrder.OrderStatus.Submitted));

        int abandonSampleCount = 1;
        for (ProductOrderSample productOrderSample : conversionPdo.getSamples()) {
            if(abandonSampleCount == 0) {
                break;
            }
            if(!productOrderSample.getDeliveryStatus().isAbandoned()) {
                productOrderSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);
                abandonSampleCount--;
            }
        }


        for (ProductOrderSample productOrderSample : conversionPdo.getSamples()) {
            if(!productOrderSample.getDeliveryStatus().isAbandoned()) {
                ProductOrderSampleTestFactory.markAsBilled(productOrderSample);
            }
        }

        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(), 
                Mockito.anyBoolean());
        productOrderEjb.updateOrderStatus(conversionPdo.getBusinessKey(), MessageReporter.UNUSED);
        assertThat(conversionPdo.getOrderStatus(), equalTo(ProductOrder.OrderStatus.Completed));

        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(), 
                Mockito.anyBoolean());

        // called once for conversionPdo and once for childPdoTwo
        Mockito.verify(mockQuoteService, Mockito.times(0)).getQuoteByAlphaId(Mockito.anyString());
    }

    public void testTransitionOrderToClosedWithLateBlockedProduct() throws Exception {
        String jiraTicketKey= "PDO-SAP-test";
        String childOneJiraTicketKey = "PDO-SAP-Childone";
        String childtwoJiraTicketKey = "PDO-SAP-Childtwo";
        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();

        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);
        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));
        Mockito.when(mockSapClient.createSAPOrder(Mockito.any(SAPOrder.class))).thenReturn(SapIntegrationServiceStub.TEST_SAP_NUMBER);

        Set<SAPMaterial> returnMaterials = new HashSet<>();
        Product conversionPdoProduct = conversionPdo.getProduct();

        final SAPMaterial primaryMaterial =
            new SAPMaterial(conversionPdoProduct.getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                    conversionPdoProduct.getDescription(), "10", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA,
                    BigDecimal.ONE, new Date(), new Date(),
                    Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                    companyCode.getSalesOrganization());

        returnMaterials.add(
                primaryMaterial);
        priceList.add(new QuotePriceItem(conversionPdo.getProduct().getPrimaryPriceItem().getCategory(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "10", "test",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform()));
        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            final SAPMaterial addonMaterial = new SAPMaterial(productOrderAddOn.getAddOn().getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                                productOrderAddOn.getAddOn().getDescription(), "10", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA,
                                BigDecimal.ONE, new Date(), new Date(),
                                Collections.emptyMap(), Collections.emptyMap(),
                                SAPMaterial.MaterialStatus.ENABLED, companyCode.getSalesOrganization());
            returnMaterials.add(addonMaterial);
            priceList.add(new QuotePriceItem(productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(), "10", "test",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform()));
        }

        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        productPriceCache.refreshCache();

        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SAP_QUOTE_ID );
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        MessageCollection messageCollection = new MessageCollection();

        Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                .thenReturn(TestUtils.buildTestSapQuote(conversionPdo.getQuoteId(),
                        BigDecimal.valueOf(10000), BigDecimal.valueOf(1000000),conversionPdo,
                        TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS,companyCode.getSalesOrganization()));

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection);
        assertThat(conversionPdo.getSapOrderNumber(), is(notNullValue()));

        int sampleCount = 0;
        for (ProductOrderSample productOrderSample : conversionPdo.getSamples()) {
            if(sampleCount >=2) {
                break;
            }
            productOrderSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);

            sampleCount++;
        }
        final SAPAccessControl control = new SAPAccessControl();
        AccessItem orderAccessItem = new AccessItem(conversionPdo.getProduct().getPrimaryPriceItem().getName());
        control.setDisabledItems(Collections.singleton(orderAccessItem));


        Mockito.when(productOrderDaoMock.findByBusinessKey(jiraTicketKey)).thenReturn(conversionPdo);
        assertThat(conversionPdo.getOrderStatus(), equalTo(ProductOrder.OrderStatus.Submitted));

        int abandonSampleCount = 1;
        for (ProductOrderSample productOrderSample : conversionPdo.getSamples()) {
            if(abandonSampleCount == 0) {
                break;
            }
            if(!productOrderSample.getDeliveryStatus().isAbandoned()) {
                productOrderSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);
                abandonSampleCount--;
            }
        }


        for (ProductOrderSample productOrderSample : conversionPdo.getSamples()) {
            if(!productOrderSample.getDeliveryStatus().isAbandoned()) {
                ProductOrderSampleTestFactory.markAsBilled(productOrderSample);
            }
        }

        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(), 
                Mockito.anyBoolean());
        productOrderEjb.updateOrderStatus(conversionPdo.getBusinessKey(), MessageReporter.UNUSED);
        assertThat(conversionPdo.getOrderStatus(), equalTo(ProductOrder.OrderStatus.Completed));

        Mockito.verify(mockEmailSender, Mockito.times(0)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(), 
                Mockito.anyBoolean());

        // called once for conversionPdo and once for childPdoTwo
        Mockito.verify(mockQuoteService, Mockito.times(0)).getQuoteByAlphaId(Mockito.anyString());
    }


    public void testCreateOrderWithProductNotInSap() throws Exception {
        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();

        String jiraTicketKey = "PDO-TEST";
        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));
        Mockito.when(mockSapClient.createSAPOrder(Mockito.any(SAPOrder.class))).thenReturn(SapIntegrationServiceStub.TEST_SAP_NUMBER);

        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);
        conversionPdo.setQuoteId(SapIntegrationServiceImplDBFreeTest.SAP_QUOTE_ID );
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        Mockito.when(productOrderDaoMock.findByBusinessKey(jiraTicketKey)).thenReturn(conversionPdo);

        Set<SAPMaterial> returnMaterials = new HashSet<>();
        Product conversionPdoProduct = conversionPdo.getProduct();

        final SAPMaterial primaryMaterial =
            new SAPMaterial(conversionPdoProduct.getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                conversionPdoProduct.getDescription(), "5", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,
                new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                companyCode.getSalesOrganization());

//        returnMaterials.add(primaryMaterial);
        priceList.add(new QuotePriceItem(conversionPdo.getProduct().getPrimaryPriceItem().getCategory(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "10", "test",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform()));
        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            final SAPMaterial addonMaterial = new SAPMaterial(productOrderAddOn.getAddOn().getPartNumber(), companyCode, companyCode.getDefaultWbs(),
                                productOrderAddOn.getAddOn().getDescription(), "5", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA,
                                BigDecimal.ONE, new Date(), new Date(),
                                Collections.emptyMap(), Collections.emptyMap(),
                                SAPMaterial.MaterialStatus.ENABLED, companyCode.getSalesOrganization());
            returnMaterials.add(addonMaterial);

            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                    .thenReturn(TestUtils.buildTestSapQuote(conversionPdo.getQuoteId(),
                            BigDecimal.valueOf(10000), BigDecimal.valueOf(1000000),conversionPdo,
                            TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS,companyCode.getSalesOrganization()));

            priceList.add(new QuotePriceItem(productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(), "10", "test",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform()));
        }

        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);

        Mockito.when(sapService.findProductsInSap()).thenReturn(returnMaterials);

        MessageCollection messageCollection = new MessageCollection();

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection);
        assertThat(conversionPdo.getBusinessKey(), is(equalTo(jiraTicketKey)));
        assertThat(messageCollection.getErrors(), is(not(Matchers.<String>empty())));
        assertThat(conversionPdo.isSavedInSAP(), is(false));
        Mockito.verify(mockQuoteService, Mockito.times(0)).getQuoteByAlphaId(Mockito.anyString());

    }
}
