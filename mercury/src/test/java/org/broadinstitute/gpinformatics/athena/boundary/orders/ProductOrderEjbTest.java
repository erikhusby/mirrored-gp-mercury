package org.broadinstitute.gpinformatics.athena.boundary.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
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
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderTest;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPInterfaceException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImplDBFreeTest;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceProducer;
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
import org.broadinstitute.sap.entity.Condition;
import org.broadinstitute.sap.entity.DeliveryCondition;
import org.broadinstitute.sap.entity.SAPMaterial;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
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
import static org.hamcrest.Matchers.notNullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderEjbTest {
    private static final BSPUserList.QADudeUser qaDudeUser = new BSPUserList.QADudeUser("PM", 2423L);
    private static final UserBean mockUserBean = Mockito.mock(UserBean.class);
    private ProductOrderDao productOrderDaoMock;
    private MercurySampleDao mockMercurySampleDao;
    private QuoteServiceImpl mockQuoteService;
    private SapIntegrationService mockSapService;
    private AppConfig mockAppConfig;
    private SapConfig mockSapConfig;
    private EmailSender mockEmailSender;
    private SAPAccessControlEjb mockAccessController;
    private PriceListCache priceListCache;
    private SAPProductPriceCache productPriceCache;
    private ProductOrderEjb productOrderEjb;

    private static final String[] sampleNames = {"SM-1234", "SM-5678", "SM-9101", "SM-1112"};
    private ProductOrder productOrder;
    private Log logger = LogFactory.getLog(ProductOrderEjbTest.class);

    @BeforeMethod
    public void setUp() throws Exception {

        mockEmailSender = Mockito.mock(EmailSender.class);
        mockSapService = Mockito.mock(SapIntegrationService.class);
        mockQuoteService = Mockito.mock(QuoteServiceImpl.class);
        mockAccessController = Mockito.mock(SAPAccessControlEjb.class);
        Mockito.when(mockAccessController.getCurrentControlDefinitions()).thenReturn(new SAPAccessControl());

        productPriceCache = new SAPProductPriceCache(mockSapService);
        priceListCache = new PriceListCache(mockQuoteService);

        //  this additon is for the temporary support of current processing of recognizing a valid Product.
        //  When the full implementation of the fetchMatarials interface for SAP is completed, this will be removed
        productPriceCache.setQuotesPriceListCache(priceListCache);
        mockMercurySampleDao = Mockito.mock(MercurySampleDao.class);
        productOrderDaoMock = Mockito.mock(ProductOrderDao.class);
        productOrderEjb = new ProductOrderEjb(productOrderDaoMock, null, mockQuoteService,
                JiraServiceProducer.stubInstance(), mockUserBean, null, null, null, mockMercurySampleDao,
                new ProductOrderJiraUtil(JiraServiceProducer.stubInstance(), mockUserBean),
                mockSapService, priceListCache, productPriceCache);
        mockAppConfig = Mockito.mock(AppConfig.class);
        productOrderEjb.setAppConfig(mockAppConfig);
        mockSapConfig = Mockito.mock(SapConfig.class);
        productOrderEjb.setSapConfig(mockSapConfig);
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
        FundingLevel fundingLevel = new FundingLevel("100",Collections.singleton(fundingDefined));

        QuoteFunding quoteFunding = new QuoteFunding(Collections.singleton(fundingLevel));

        testSingleSourceQuote = new Quote(SapIntegrationServiceImplDBFreeTest.SINGLE_SOURCE_PO_QUOTE_ID, quoteFunding, ApprovalStatus.FUNDED);
        testSingleSourceQuote.setExpired(false);

        Quote testSingleSourceQuote2;

        testSingleSourceQuote2 = new Quote(SapIntegrationServiceImplDBFreeTest.SINGLE_SOURCE_PO_QUOTE_ID+"2", quoteFunding, ApprovalStatus.FUNDED);
        testSingleSourceQuote2.setExpired(false);
        Quote testSingleSourceQuote3;

        testSingleSourceQuote3 = new Quote(SapIntegrationServiceImplDBFreeTest.SINGLE_SOURCE_PO_QUOTE_ID+"3", quoteFunding, ApprovalStatus.FUNDED);
        testSingleSourceQuote3.setExpired(false);

        Mockito.when(mockSapService.createOrder(Mockito.any(ProductOrder.class))).thenReturn(SapIntegrationServiceStub.TEST_SAP_NUMBER);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testSingleSourceQuote.getAlphanumericId())).thenReturn(testSingleSourceQuote);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testSingleSourceQuote2.getAlphanumericId())).thenReturn(testSingleSourceQuote2);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testSingleSourceQuote3.getAlphanumericId())).thenReturn(testSingleSourceQuote3);

        String jiraTicketKey= "PDO-SAP-test";
        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);
        conversionPdo.setQuoteId(testSingleSourceQuote.getAlphanumericId() );
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        Mockito.when(productOrderDaoMock.findByBusinessKey(jiraTicketKey)).thenReturn(conversionPdo);

        Set<SAPMaterial> returnMaterials = new HashSet<>();

        returnMaterials.add(new SAPMaterial(conversionPdo.getProduct().getPartNumber(),"10", Collections.<Condition>emptySet(), Collections.<DeliveryCondition>emptySet()));
        priceList.add(new QuotePriceItem(conversionPdo.getProduct().getPrimaryPriceItem().getCategory(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "10", "test",
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
            returnMaterials.add(new SAPMaterial(productOrderAddOn.getAddOn().getPartNumber(),"10", Collections.<Condition>emptySet(), Collections.<DeliveryCondition>emptySet()));
            priceList.add(new QuotePriceItem(productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(), "10", "test",
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
        Mockito.when(mockSapService.findProductsInSap()).thenReturn(returnMaterials);

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

        testSingleSourceQuote3.setExpired(true);

        for (SAPMaterial sapMaterial : returnMaterials) {
            sapMaterial.setBasePrice("10");
        }


        ProductOrderTest.billSampleOut(conversionPdo, conversionPdo.getSamples().iterator().next(), conversionPdo.getSamples().size());

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection, false);
        Assert.assertEquals(conversionPdo.getSapReferenceOrders().size(), 4);


        productOrderEjb.abandon(jiraTicketKey, "testing");

        Mockito.verify(mockEmailSender, Mockito.times(4)).sendHtmlEmail(Mockito.eq(mockAppConfig),
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
        FundingLevel fundingLevel = new FundingLevel("100",Collections.singleton(fundingDefined));

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

    public void testTransitionOrderWithServiceNowTicket() throws Exception {
        String jiraTicketKey= "PDO-SAP-test";
        String childOneJiraTicketKey = "PDO-SAP-Childone";
        String childtwoJiraTicketKey = "PDO-SAP-Childtwo";
        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();


        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);

        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));
        Mockito.when(mockSapService.createOrder(Mockito.any(ProductOrder.class))).thenReturn(SapIntegrationServiceStub.TEST_SAP_NUMBER);

        String testUser = "Scott.G.MATThEws@GMail.CoM";
        Funding fundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        fundingDefined.setPurchaseOrderContact(testUser);
        fundingDefined.setPurchaseOrderNumber("PO00Id8923");
        FundingLevel fundingLevel = new FundingLevel("100", Collections.singleton(fundingDefined));
        QuoteFunding quoteFunding = new QuoteFunding(Collections.singleton(fundingLevel));
        Quote testSingleSourceQuote = new Quote(SapIntegrationServiceImplDBFreeTest.SINGLE_SOURCE_PO_QUOTE_ID, quoteFunding, ApprovalStatus.FUNDED);
        testSingleSourceQuote.setExpired(false);

        Set<SAPMaterial> returnMaterials = new HashSet<>();

        returnMaterials.add(new SAPMaterial(conversionPdo.getProduct().getPartNumber(),"10", Collections.<Condition>emptySet(), Collections.<DeliveryCondition>emptySet()));
        priceList.add(new QuotePriceItem(conversionPdo.getProduct().getPrimaryPriceItem().getCategory(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "10", "test",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform()));
        quoteItems.add(new QuoteItem(testSingleSourceQuote.getAlphanumericId(),conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),"2000", "10","each",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform(),
                conversionPdo.getProduct().getPrimaryPriceItem().getCategory()));
        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            returnMaterials.add(new SAPMaterial(productOrderAddOn.getAddOn().getPartNumber(),"10", Collections.<Condition>emptySet(), Collections.<DeliveryCondition>emptySet()));
            priceList.add(new QuotePriceItem(productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(), "10", "test",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform()));
        }

        testSingleSourceQuote.setQuoteItems(quoteItems);
        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockSapService.findProductsInSap()).thenReturn(returnMaterials);

        Mockito.when(mockQuoteService.getQuoteByAlphaId(testSingleSourceQuote.getAlphanumericId())).thenReturn(testSingleSourceQuote);

        conversionPdo.setQuoteId(testSingleSourceQuote.getAlphanumericId() );
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        MessageCollection messageCollection = new MessageCollection();
        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection, true);
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
        productOrderEjb.publishProductOrderToSAP(childPdoOne, messageCollection, true);

        ProductOrder childPdoTwo = ProductOrder.cloneProductOrder(conversionPdo, false);

        Mockito.when(mockSapService.createOrder(Mockito.any(ProductOrder.class))).thenReturn("Child_sap_test");
        childPdoTwo.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        childPdoTwo.setSamples(ProductOrderSampleTestFactory.createSampleListWithMercurySamples("SM-3BRTST"));
        childPdoTwo.setJiraTicketKey(childtwoJiraTicketKey);
        productOrderEjb.publishProductOrderToSAP(childPdoTwo, messageCollection, true);


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
                Mockito.anyBoolean());
        productOrderEjb.updateOrderStatus(conversionPdo.getBusinessKey(), MessageReporter.UNUSED);
        assertThat(conversionPdo.getOrderStatus(), equalTo(ProductOrder.OrderStatus.Completed));


        Mockito.verify(mockEmailSender, Mockito.times(1)).sendHtmlEmail(Mockito.eq(mockAppConfig),
                Mockito.anyString(),
                Mockito.<String>anyList(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean());
    }

    public void testCreateOrderWithProductNotInSap() throws Exception {
        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();

        String jiraTicketKey = "PDO-TEST";
        Mockito.when(mockUserBean.getBspUser()).thenReturn(new BSPUserList.QADudeUser("PM", 2423L));
        Mockito.when(mockSapService.createOrder(Mockito.any(ProductOrder.class))).thenReturn(SapIntegrationServiceStub.TEST_SAP_NUMBER);

        Quote testSingleSourceQuote;
        String testUser = "Scott.G.MATThEws@GMail.CoM";
        Funding fundingDefined = new Funding(Funding.PURCHASE_ORDER,null, null);
        fundingDefined.setPurchaseOrderContact(testUser);
        fundingDefined.setPurchaseOrderNumber("PO00Id8923");
        FundingLevel fundingLevel = new FundingLevel("100", Collections.singleton(fundingDefined));

        QuoteFunding quoteFunding = new QuoteFunding(Collections.singleton(fundingLevel));

        testSingleSourceQuote = new Quote(SapIntegrationServiceImplDBFreeTest.SINGLE_SOURCE_PO_QUOTE_ID, quoteFunding, ApprovalStatus.FUNDED);
        testSingleSourceQuote.setExpired(false);

        ProductOrder conversionPdo = ProductOrderTestFactory.createDummyProductOrder(10, jiraTicketKey);
        conversionPdo.setPriorToSAP1_5(true);
        conversionPdo.setQuoteId(testSingleSourceQuote.getAlphanumericId() );
        conversionPdo.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        Mockito.when(productOrderDaoMock.findByBusinessKey(jiraTicketKey)).thenReturn(conversionPdo);

        Set<SAPMaterial> returnMaterials = new HashSet<>();

        returnMaterials.add(new SAPMaterial(conversionPdo.getProduct().getPartNumber(),"5", Collections.<Condition>emptySet(), Collections.<DeliveryCondition>emptySet()));
        priceList.add(new QuotePriceItem(conversionPdo.getProduct().getPrimaryPriceItem().getCategory(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(), "10", "test",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform()));
        quoteItems.add(new QuoteItem(testSingleSourceQuote.getAlphanumericId(),conversionPdo.getProduct().getPrimaryPriceItem().getName(),
                conversionPdo.getProduct().getPrimaryPriceItem().getName(),"2000", "5","each",
                conversionPdo.getProduct().getPrimaryPriceItem().getPlatform(),
                conversionPdo.getProduct().getPrimaryPriceItem().getCategory()));
        for (ProductOrderAddOn productOrderAddOn : conversionPdo.getAddOns()) {
            returnMaterials.add(new SAPMaterial(productOrderAddOn.getAddOn().getPartNumber(),"5", Collections.<Condition>emptySet(), Collections.<DeliveryCondition>emptySet()));
            priceList.add(new QuotePriceItem(productOrderAddOn.getAddOn().getPrimaryPriceItem().getCategory(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(),
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getName(), "10", "test",
                    productOrderAddOn.getAddOn().getPrimaryPriceItem().getPlatform()));
        }

        testSingleSourceQuote.setQuoteItems(quoteItems);
        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(Mockito.anyString())).thenReturn(testSingleSourceQuote);

        Mockito.when(mockSapService.findProductsInSap()).thenReturn(returnMaterials);
        try {
            productOrderEjb.isOrderEligibleForSAP(conversionPdo);
            Assert.fail("Differences in prices should have thrown an error");
        } catch (QuoteServerException | QuoteNotFoundException e) {
            Assert.fail("Differences in prices should have thrown an InvalidProductException");
        } catch (InvalidProductException e) {
            e.printStackTrace();
        }

        MessageCollection messageCollection = new MessageCollection();

        productOrderEjb.publishProductOrderToSAP(conversionPdo, messageCollection, true);
        assertThat(conversionPdo.getBusinessKey(), is(equalTo(jiraTicketKey)));
        assertThat(messageCollection.getErrors(), is(not(Matchers.<String>empty())));
        assertThat(conversionPdo.isSavedInSAP(), is(false));

    }
}
