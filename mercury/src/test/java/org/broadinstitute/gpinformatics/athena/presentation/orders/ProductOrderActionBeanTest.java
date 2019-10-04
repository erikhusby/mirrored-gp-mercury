package org.broadinstitute.gpinformatics.athena.presentation.orders;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.MaterialInfo;
import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import functions.rfc.sap.document.sap_com.ZESDFUNDINGDET;
import functions.rfc.sap.document.sap_com.ZESDQUOTEHEADER;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.collection.Group;
import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductOrderJiraUtil;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.athena.entity.orders.PriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderPriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.SapOrderDetail;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.StripesMockTestUtils;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.BspGroupCollectionTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.BspShippingLocationTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProjectTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGroupCollectionList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSiteList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnector;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.datatables.Column;
import org.broadinstitute.gpinformatics.mercury.presentation.datatables.Search;
import org.broadinstitute.gpinformatics.mercury.presentation.datatables.State;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.broadinstitute.sap.entity.Condition;
import org.broadinstitute.sap.entity.DeliveryCondition;
import org.broadinstitute.sap.entity.OrderCalculatedValues;
import org.broadinstitute.sap.entity.OrderCriteria;
import org.broadinstitute.sap.entity.OrderValue;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.entity.quote.FundingDetail;
import org.broadinstitute.sap.entity.quote.FundingStatus;
import org.broadinstitute.sap.entity.quote.QuoteHeader;
import org.broadinstitute.sap.entity.quote.QuoteStatus;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.easymock.EasyMock;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderActionBeanTest {

    private ProductOrderActionBean actionBean;

    private JSONObject jsonObject;

    private double expectedNumericValue = 9.3;

    private String expectedNonNumericRinScore = "I'm not a number";

    private ProductOrder pdo;

    private ProductOrderEjb productOrderEjb;

    public static final long BSP_INFORMATICS_TEST_SITE_ID = 1l;
    public static final long HOMO_SAPIENS = 1l;
    public static final long TEST_COLLECTION = 1062L;
    private PriceListCache priceListCache;

    private QuoteService stubQuoteService = new QuoteServiceStub();

    private QuoteService mockQuoteService;
    public SapIntegrationServiceImpl mockSAPService;
    public SAPProductPriceCache stubProductPriceCache;
    public ProductOrderDao mockProductOrderDao;
    public ProductOrder testOrder;
    private BSPUserList mockBspUserList;
    private JiraService mockJiraService;
    private SapIntegrationClientImpl mockSapClient;
    private SAPAccessControlEjb mockAccessController;
    private ProductDao mockProductDao;

    @BeforeMethod
    private void setUp() {
        actionBean = new ProductOrderActionBean();
        actionBean.setContext(new CoreActionBeanContext());

        mockQuoteService = Mockito.mock(QuoteServiceImpl.class);
        priceListCache = new PriceListCache(mockQuoteService);
        actionBean.setPriceListCache(priceListCache);

        mockAccessController = Mockito.mock(SAPAccessControlEjb.class);
        mockSAPService = new SapIntegrationServiceImpl(SapConfig.produce(Deployment.DEV),
                mockQuoteService, Mockito.mock(BSPUserList.class), Mockito.mock(PriceListCache.class),
                stubProductPriceCache, mockAccessController);
        stubProductPriceCache = new SAPProductPriceCache(mockSAPService);
        mockSAPService.setProductPriceCache(stubProductPriceCache);
        mockSapClient = Mockito.mock(SapIntegrationClientImpl.class);
        mockSAPService.setWrappedClient(mockSapClient);

        Mockito.when(mockAccessController.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        stubProductPriceCache.setAccessControlEjb(mockAccessController);
        actionBean.setProductPriceCache(stubProductPriceCache);

        mockProductOrderDao = Mockito.mock(ProductOrderDao.class);
        mockBspUserList = Mockito.mock(BSPUserList.class);
        mockJiraService = Mockito.mock(JiraService.class);
        mockProductDao = Mockito.mock(ProductDao.class);
        productOrderEjb = new ProductOrderEjb(mockProductOrderDao, mockProductDao,
                mockQuoteService, mockJiraService, Mockito.mock(UserBean.class),
                mockBspUserList, Mockito.mock(BucketEjb.class), Mockito.mock(SquidConnector.class),
                Mockito.mock(MercurySampleDao.class), Mockito.mock(ProductOrderJiraUtil.class),
                mockSAPService, priceListCache, stubProductPriceCache);

        productOrderEjb.setAccessController(mockAccessController);

        actionBean.setProductOrderEjb(productOrderEjb);
        actionBean.setProductOrderDao(mockProductOrderDao);
        actionBean.setSapService(mockSAPService);
        actionBean.setBspUserList(mockBspUserList);
        actionBean.setJiraService(mockJiraService);
        actionBean.setQuoteService(mockQuoteService);

        jsonObject = new JSONObject();
        pdo = newPdo();
    }

    /**
     * Creates a basic PDO with a few samples.
     * Setting the product is left to individual tests.
     *
     * @return
     */
    private ProductOrder newPdo() {
        ProductOrder pdo = new ProductOrder();
        pdo.setSamples(createPdoSamples());
        pdo.setTitle("Test PDO");
        return pdo;
    }

    /**
     * Creates a list with two samples: one with a good
     * rin score and one with a bad rin score
     *
     * @return
     */
    private Collection<ProductOrderSample> createPdoSamples() {
        List<ProductOrderSample> pdoSamples = new ArrayList<>();
        SampleData sampleWithGoodRin = getSampleDTOWithGoodRinScore();
        SampleData sampleWithBadRin = getSamplDTOWithBadRinScore();
        pdoSamples.add(new ProductOrderSample(sampleWithGoodRin.getSampleId(), sampleWithGoodRin));
        pdoSamples.add(new ProductOrderSample(sampleWithBadRin.getSampleId(), sampleWithBadRin));
        pdoSamples.add(new ProductOrderSample("123.0")); // throw in a gssr sample
        return pdoSamples;
    }

    private ProductOrderKit createGoodPdoKit() {
        MaterialInfoDto materialInfoDto =
                new MaterialInfoDto(KitType.DNA_MATRIX.getKitName(), KitType.DNA_MATRIX.getDisplayName());
        ProductOrderKit pdoKit = new ProductOrderKit(TEST_COLLECTION, BSP_INFORMATICS_TEST_SITE_ID);
        ProductOrderKitDetail kitDetail = new ProductOrderKitDetail(96l, KitType.DNA_MATRIX, HOMO_SAPIENS,
                materialInfoDto,
                Collections.singleton(PostReceiveOption.FLUIDIGM_FINGERPRINTING));
        pdoKit.setKitOrderDetails(Collections.singleton(kitDetail));
        pdoKit.setTransferMethod(SampleKitWorkRequest.TransferMethod.SHIP_OUT);
        pdoKit.setNotificationIds(Arrays.asList("17255"));
        pdoKit.setExomeExpress(true);
        return pdoKit;
    }

    /**
     * Sets the product for given PDO such that
     * there's rin risk.
     */
    private void setRinRiskProduct(ProductOrder pdo) {
        Product productThatHasRinRisk = new Product();
        productThatHasRinRisk.addRiskCriteria(
                new RiskCriterion(RiskCriterion.RiskCriteriaType.RIN, Operator.LESS_THAN, "6.0")
        );
        try {
            pdo.setProduct(productThatHasRinRisk);
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
    }

    private void setNonRinRiskProduct(ProductOrder pdo) {
        try {
            pdo.setProduct(new Product());
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Tests that non-numeric RIN scores
     * are turned into "N/A" by the action bean
     *
     * @throws JSONException
     */
    public void testNonNumericRinScore() throws JSONException {
        jsonObject.put(BspSampleData.JSON_RIN_KEY, getSamplDTOWithBadRinScore().getRawRin());
        Assert.assertEquals(jsonObject.get(BspSampleData.JSON_RIN_KEY), expectedNonNumericRinScore);
    }

    /**
     * Tests that numeric RIN scores
     * are handled as real numbers by the action bean
     *
     * @throws JSONException
     */
    public void testNumericRinScore() throws JSONException {
        jsonObject.put(BspSampleData.JSON_RIN_KEY, getSampleDTOWithGoodRinScore().getRawRin());
        Assert.assertEquals(Double.parseDouble((String) jsonObject.get(BspSampleData.JSON_RIN_KEY)),
                expectedNumericValue);
    }

    public void testNoRinScore() throws JSONException {
        Map<BSPSampleSearchColumn, String> data = new EnumMap<>(BSPSampleSearchColumn.class);
        data.put(BSPSampleSearchColumn.SAMPLE_ID, "SM-1234");
        SampleData bspSampleData = new BspSampleData(data);

        jsonObject.put(BspSampleData.JSON_RIN_KEY, bspSampleData.getRawRin());

        Assert.assertEquals(jsonObject.get(BspSampleData.JSON_RIN_KEY), "");
    }

    public void testRinRange() throws JSONException {
        Map<BSPSampleSearchColumn, String> data = new EnumMap<>(BSPSampleSearchColumn.class);
        data.put(BSPSampleSearchColumn.SAMPLE_ID, "SM-1234");
        data.put(BSPSampleSearchColumn.RIN, "1.2-3.4");
        SampleData bspSampleData = new BspSampleData(data);

        jsonObject.put(BspSampleData.JSON_RIN_KEY, bspSampleData.getRawRin());

        Assert.assertEquals(jsonObject.get(BspSampleData.JSON_RIN_KEY), "1.2-3.4");
    }

    public void testValidateRinScoresWhenProductHasRinRisk() {
        setRinRiskProduct(pdo);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
        actionBean.validateRinScores(pdo);
        Assert.assertEquals(actionBean.getContext().getValidationErrors().size(), 1);
    }

    public void testValidateRinScoresWhenProductHasNoRinRisk() {
        setNonRinRiskProduct(pdo);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
        actionBean.validateRinScores(pdo);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
    }

    public void testIsRinScoreValidationRequired() {
        setRinRiskProduct(pdo);
        Assert.assertTrue(pdo.isRinScoreValidationRequired());
        setNonRinRiskProduct(pdo);
        Assert.assertFalse(pdo.isRinScoreValidationRequired());
    }

    public void testCanBadRinScoreBeUsedForOnRiskCalculation() {
        SampleData badRinScoreSample = getSamplDTOWithBadRinScore();
        Assert.assertFalse(badRinScoreSample.canRinScoreBeUsedForOnRiskCalculation());
    }

    public void testCanGoodRinScoreBeUsedForOnRiskCalculation() {
        SampleData goodRinScoreSample = getSampleDTOWithGoodRinScore();
        Assert.assertTrue(goodRinScoreSample.canRinScoreBeUsedForOnRiskCalculation());
    }

    public void testCanEmptyRinScoreBeUsedForOnRiskCalculation() {
        SampleData emptyRinScoreSample = getSampleDTOWithEmptyRinScore();
        Assert.assertTrue(emptyRinScoreSample.canRinScoreBeUsedForOnRiskCalculation());
    }

    @DataProvider(name = "regulatorySuggestionSampleInputs")
    public Iterator<Object[]> regulatorySuggestionSampleInputs() {
        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{"SM-1234", true});
        testCases.add(new Object[]{"SM-HJILE ", false});
        testCases.add(new Object[]{String.format("%s%s%s", "\u00a0","x", "\u00a0"), false});
        testCases.add(new Object[]{String.format("%s%s%s", "\u1680","x", "\u1680"), false});
        testCases.add(new Object[]{String.format("%s%s%s", "\u180e","x", "\u180e"), false});
        testCases.add(new Object[]{String.format("%s%s%s", "\u2000","x", "\u2000"), false});
        testCases.add(new Object[]{String.format("%s%s%s", "\u202f","x", "\u202f"), false});
        testCases.add(new Object[]{String.format("%s%s%s", "\u205f","x", "\u205f"), false});
        testCases.add(new Object[]{String.format("%s%s%s", "\u3000","x", "\u3000"), false});
        testCases.add(new Object[]{"SM-HBYE9\nSM-I2QU9\nSM-I43WJ\nSM-HJILE \nSM-GM6ND", false});
        testCases.add(new Object[]{"SM-HOW1Z\nSM-HGTBZ\nSM-HPNGW\nSM-HK6O2\nSM-HJQFA\nSM-HG3GT", true});

        return testCases.iterator();
    }


    @Test(dataProvider = "regulatorySuggestionSampleInputs")
    public void testRegulatorySuggestionInput(String sampleId, boolean inputIsAsciiPrintable) throws Exception {
        Product product = new Product();
        pdo.setProduct(product);
        ResearchProjectDao mockResearchProjectDao = Mockito.mock(ResearchProjectDao.class);
        Mockito.when(mockResearchProjectDao.findByBusinessKey(Mockito.anyString())).thenReturn(new ResearchProject());
        actionBean.setResearchProjectDao(mockResearchProjectDao);
        actionBean.setResearchProjectKey("somekey");
        actionBean.setEditOrder(pdo);
        actionBean.setProduct("test product");

        // Show that the input is not ascii printable. Mimics what is called in the ProductOrderSample constructor.
        final List<String> collect = Arrays.asList(sampleId.split("\\s"))
                .stream().filter(s -> !StringUtils.isAsciiPrintable(s)).collect(Collectors.toList());
        assertThat(CollectionUtils.isEmpty(collect),equalTo(inputIsAsciiPrintable));

        actionBean.setSampleList(sampleId);
        try {
            actionBean.suggestRegulatoryInfo();
        } catch (Exception e) {
            Assert.fail("Calling suggestRegulatoryInfo() should not have resulted in an exception being thrown", e);
        }
    }

    /**
     * Primarily testing the null pointer error found in GPLIM-6072, This test case sets up a Product order and
     * validates that the do Validation method acts as it should.
     *
     * @throws Exception
     * @param quoteSource
     */
    @Test(dataProvider = "quoteSourceProvider")
    public void testDoValidationMethodQuoteOnOrder(String testQuoteIdentifier, ProductOrder.QuoteSourceType quoteSource,
                                                   TestUtils.SapQuoteTestScenario quoteItemsMatchOrderProducts)
            throws Exception {

        pdo = ProductOrderTestFactory.createDummyProductOrder();
        String quoteId = "BSP252";
        if(quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            quoteId = "9999999";
        }
        pdo.setQuoteId(quoteId);
        pdo.addRegulatoryInfo(new RegulatoryInfo("test", RegulatoryInfo.Type.IRB, "test"));
        pdo.setAttestationConfirmed(true);
        pdo.setJiraTicketKey("");
        pdo.setOrderStatus(ProductOrder.OrderStatus.Draft);

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();
        addPriceItemForProduct(quoteId, priceList, quoteItems, pdo.getProduct(), "10", "20", "10");
        addSapMaterial(returnMaterials,pdo.getProduct(), "10", SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);

        for (ProductOrderAddOn addOn : pdo.getAddOns()) {
            addPriceItemForProduct(quoteId, priceList, quoteItems, addOn.getAddOn(), "20", "20", "20");
            addSapMaterial(returnMaterials, addOn.getAddOn(),"20",SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        }

        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        } else {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenThrow(new SAPIntegrationException("Sap Should not be called for Quote Server Source"));

        }
        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn((Collections.singletonList(pdo)));

        SapQuote sapMockQuote = null;
        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            stubProductPriceCache.refreshCache();
            Mockito.when(mockQuoteService.getAllPriceItems()).thenThrow(new QuoteServerException("Quote Server should not be called for SAP Source"));

            BigDecimal potentialOrderValue = BigDecimal.valueOf(pdo.getSamples().size() * 10);
            Mockito
                .when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class), Mockito.any(OrderCriteria.class)))
                .then(invocationOnMock -> {

                    String potentialOrderId = null;
                    BigDecimal fundsRemaining = BigDecimal.ZERO;

                    fundsRemaining = ((SapQuote) invocationOnMock.getArguments()[0]).getQuoteHeader().fundsRemaining();
                    Optional<OrderCriteria> orderCriteria = Optional.ofNullable(((OrderCriteria) invocationOnMock.getArguments()[1]));
                    if(orderCriteria.isPresent()) {
                        potentialOrderId = orderCriteria.get().getSapOrderID();
                    }

                    return new OrderCalculatedValues(potentialOrderValue, Collections.emptySet(),potentialOrderId, fundsRemaining);
                });
            Mockito.when(mockQuoteService.getQuoteByAlphaId(Mockito.anyString()))
                    .thenThrow(new QuoteServerException("Quote Server should not be called for SAP Source"));

            sapMockQuote = TestUtils.buildTestSapQuote(quoteId, BigDecimal.valueOf(30000), BigDecimal.valueOf(37387),
                    pdo,quoteItemsMatchOrderProducts, "GP01");

            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenReturn(sapMockQuote);
        } else {
            Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class), Mockito.any(OrderCriteria.class)))
                    .thenThrow(new SAPIntegrationException("SAP Should not be thrown for SAP source"));
            Mockito.when(mockQuoteService.getQuoteByAlphaId(Mockito.anyString()))
                    .then(new Answer<Quote>() {
                        @Override
                        public Quote answer(InvocationOnMock invocationOnMock) throws Throwable {
                            return stubQuoteService.getQuoteByAlphaId(pdo.getQuoteId());
                        }
                    });

            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenThrow(new SAPIntegrationException("SAP Should not be thrown for SAP source"));
        }

        actionBean.setEditOrder(pdo);
        actionBean.setQuoteService(mockQuoteService);

        Mockito.when(mockBspUserList.getById(Mockito.any(Long.class)))
                .thenReturn(new BspUser(1L, "", "squidUser@broadinstitute.org", "Squid", "User", Collections.<String>emptyList(),1L, "squiduser" ));
        Mockito.when(mockJiraService.isValidUser(Mockito.anyString())).thenReturn(true);

        Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
        actionBean.doValidation(ProductOrderActionBean.PLACE_ORDER_ACTION);

        Assert.assertEquals(actionBean.getValidationErrors().isEmpty(),
                (quoteItemsMatchOrderProducts != TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER ||
                 quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER));
        actionBean.clearValidationErrors();

        if(quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            pdo.setQuoteId("MPG183");
            actionBean.doValidation(ProductOrderActionBean.PLACE_ORDER_ACTION);

            Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            Assert.assertEquals(1, actionBean.getValidationErrors().size());
            actionBean.clearValidationErrors();
        }

        pdo.setQuoteId("ScottInvalid");
        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenReturn(null);
        }
//      todo revisit this:  Not sure why the test case would fail when I re-set a mock with the 'thenThrow' method
//        else {
//            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenThrow(new SAPIntegrationException("Sap should not be called for Quote Server Quote"));
//        }
        actionBean.doValidation(ProductOrderActionBean.PLACE_ORDER_ACTION);

        Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
        Assert.assertEquals(1, actionBean.getValidationErrors().size());
        actionBean.clearValidationErrors();

        pdo.setQuoteId("");
        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenReturn(null);
        }
//      todo revisit this:  Not sure why the test case would fail when I re-set a mock with the 'thenThrow' method
//        else {
//            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenThrow(new SAPIntegrationException("Sap should not be called for Quote Server Quote"));
//        }

        actionBean.doValidation(ProductOrderActionBean.PLACE_ORDER_ACTION);

        Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
        Assert.assertEquals(1, actionBean.getValidationErrors().size());

        //////////////////////////////////////////////////////
        // Now test some of the other validations
        //////////////////////////////////////////////////////
        actionBean.clearValidationErrors();

        pdo.setQuoteId(quoteId);
        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenReturn(sapMockQuote);
        }
//      todo revisit this:  Not sure why the test case would fail when I re-set a mock with the 'thenThrow' method
//        else {
//            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenThrow(new SAPIntegrationException("Sap should not be called for Quote Server Quote"));
//        }

        actionBean.doValidation(ProductOrderActionBean.PLACE_ORDER_ACTION);
        Assert.assertEquals(actionBean.getValidationErrors().isEmpty(),
                quoteItemsMatchOrderProducts != TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER ||
                quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER);

        pdo.setAttestationConfirmed(false);
        actionBean.doValidation(ProductOrderActionBean.PLACE_ORDER_ACTION);
        Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
        Assert.assertEquals(1, actionBean.getValidationErrors().size());
        actionBean.clearValidationErrors();

        pdo.setAttestationConfirmed(true);
        final ResearchProject researchProject = pdo.getResearchProject();
        pdo.setResearchProject(null);

        actionBean.doValidation(ProductOrderActionBean.PLACE_ORDER_ACTION);
        Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
        Assert.assertEquals(1, actionBean.getValidationErrors().size());
        actionBean.clearValidationErrors();

        pdo.setResearchProject(researchProject);
        pdo.setCreatedBy(null);
        actionBean.doValidation(ProductOrderActionBean.PLACE_ORDER_ACTION);
        Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
        Assert.assertEquals(1, actionBean.getValidationErrors().size());
        actionBean.clearValidationErrors();

        pdo.setCreatedBy(1L);
        Mockito.when(mockJiraService.isValidUser(Mockito.anyString())).thenReturn(false);

        actionBean.doValidation(ProductOrderActionBean.PLACE_ORDER_ACTION);
        Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
        Assert.assertEquals(1, actionBean.getValidationErrors().size());
        actionBean.clearValidationErrors();

        Product holdProduct = pdo.getProduct();
        pdo.setProduct(null);

        actionBean.doValidation(ProductOrderActionBean.PLACE_ORDER_ACTION);
        Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
        Assert.assertEquals(1, actionBean.getValidationErrors().size());
        actionBean.clearValidationErrors();

        pdo.setProduct(holdProduct);
        final ProductFamily holdProductFamily = pdo.getProduct().getProductFamily();
        pdo.getProduct().setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.SEQUENCE_ONLY.getFamilyName()));
        pdo.setLaneCount(BigDecimal.ZERO);

        actionBean.doValidation(ProductOrderActionBean.PLACE_ORDER_ACTION);
        Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
        Assert.assertEquals(1, actionBean.getValidationErrors().size());
        actionBean.clearValidationErrors();

        pdo.getProduct().setProductFamily(holdProductFamily);
    }

    public void testPostReceiveOptions() throws Exception {
        Product product = new Product();
        pdo.setProduct(product);
        pdo.setProductOrderKit(createGoodPdoKit());
        actionBean.setEditOrder(pdo);
        actionBean.setProduct("test product");
        actionBean.setMaterialInfo(MaterialInfo.DNA_DERIVED_FROM_BLOOD.getText());
        String testKitQueryIndex = "5";
        actionBean.setKitDefinitionQueryIndex(testKitQueryIndex);

        StreamingResolution postReceiveOptions = (StreamingResolution) actionBean.getPostReceiveOptions();
        HttpServletRequest request = new MockHttpServletRequest("foo", "bar");
        MockHttpServletResponse response = new MockHttpServletResponse();

        postReceiveOptions.execute(request, response);
        String jsonString = response.getOutputString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readValue(jsonString, JsonNode.class);
        String returnedKitIndex = jsonNode.get(actionBean.getKitDefinitionIndexIdentifier()).asText();
        Assert.assertEquals(returnedKitIndex, testKitQueryIndex,
                "The kit index passed in should match the kit index returned");

        Iterator<JsonNode> resultIterator = jsonNode.get("dataList").elements();
        String nodeKey = "key";
        String nodeChecked = "checked";

        while (resultIterator.hasNext()) {
            JsonNode node = resultIterator.next();

            Set<PostReceiveOption> postReceiveOptionSet = new HashSet<>(node.get(nodeKey).size());
            for (String returnedOption : Arrays.asList(node.get(nodeKey).asText())) {
                postReceiveOptionSet.add(PostReceiveOption.valueOf(returnedOption));
            }

            Assert.assertTrue(postReceiveOptionSet.size() > 0);
            PostReceiveOption option = TestUtils.getFirst(postReceiveOptionSet);
            Assert.assertTrue(option.getDefaultToChecked() == node.get(nodeChecked).asBoolean());
            Assert.assertFalse(option.getArchived());
        }
    }

    public void testSampleKitWithValidationErrors() {
        Product product = new Product();
        try {
            pdo.setProduct(product);
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
        ProductOrderKit kitWithValidationProblems = createGoodPdoKit();
        kitWithValidationProblems.setTransferMethod(SampleKitWorkRequest.TransferMethod.PICK_UP);
        kitWithValidationProblems.setNotificationIds(new ArrayList<String>());
        pdo.setProductOrderKit(kitWithValidationProblems);
        actionBean.setEditOrder(pdo);
        actionBean.validateTransferMethod(pdo);
        Assert.assertEquals(actionBean.getContext().getValidationErrors().size(), 1);
    }

    public void testSampleKitNoValidationErrors() {
        Product product = new Product();
        try {
            pdo.setProduct(product);
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
        pdo.setProductOrderKit(createGoodPdoKit());
        actionBean.setEditOrder(pdo);
        actionBean.validateTransferMethod(pdo);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
    }

    public void testPostReceiveOptionKeys() {
        Product product = new Product();
        try {
            pdo.setProduct(product);
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
        pdo.setProductOrderKit(createGoodPdoKit());
        actionBean.setEditOrder(pdo);
        Assert.assertTrue(actionBean.getPostReceiveOptionKeys().isEmpty());
        actionBean.initSampleKitInfo();
        Assert.assertFalse(actionBean.getPostReceiveOptionKeys().isEmpty());
    }

    private SampleData getSamplDTOWithBadRinScore() {
        Map<BSPSampleSearchColumn, String> dataMap = new EnumMap<BSPSampleSearchColumn, String>(
                BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.RIN, expectedNonNumericRinScore);
            put(BSPSampleSearchColumn.SAMPLE_ID, "SM-49M5N");
        }};
        return new BspSampleData(dataMap);
    }

    private SampleData getSampleDTOWithGoodRinScore() {
        Map<BSPSampleSearchColumn, String> dataMap = new EnumMap<BSPSampleSearchColumn, String>(
                BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.RIN, String.valueOf(expectedNumericValue));
            put(BSPSampleSearchColumn.SAMPLE_ID, "SM-99D2A");
        }};
        return new BspSampleData(dataMap);
    }

    private SampleData getSampleDTOWithEmptyRinScore() {
        Map<BSPSampleSearchColumn, String> dataMap = new EnumMap<BSPSampleSearchColumn, String>(
                BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.RIN, "");
            put(BSPSampleSearchColumn.SAMPLE_ID, "SM-99D2A");
        }};
        return new BspSampleData(dataMap);
    }

    private Product createSimpleProduct(String productPartNumber, String family) {
        Product product = new Product();
        product.setPartNumber(productPartNumber);
        product.setProductFamily(new ProductFamily(family));
        return product;
    }

    //todo modify this to coincide with allowing PDMs to opt out of any order
    @Test(enabled = false)
    public void testQuoteOptOutAjaxCallStripes() throws Exception {
        Product product = createSimpleProduct("P-EX-0001",
                ProductFamily.ProductFamilyInfo.SAMPLE_INITIATION_QUALIFICATION_CELL_CULTURE.getFamilyName());
        ProductDao productDao = EasyMock.createNiceMock(ProductDao.class);
        EasyMock.expect(productDao.findByBusinessKey((String) EasyMock.anyObject())).andReturn(product).atLeastOnce();
        EasyMock.replay(productDao);

        MockServletContext ctx = new MockServletContext("mercury");
        Map<String, String> params = new HashMap<>();
        // values taken from our web.xml
        params.put("ActionResolver.Packages",
                "org.broadinstitute.gpinformatics.mercury.presentation,org.broadinstitute.gpinformatics.athena.presentation");
        params.put("ActionBeanContext.Class",
                "org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext");
        //params.put("Extension.Packages","com.samaxes.stripes.inject");
        ctx.addFilter(StripesFilter.class, "StripesFilter", params);
        ctx.setServlet(DispatcherServlet.class, "DispatcherServlet", null);
        MockHttpSession session = new MockHttpSession(ctx);

        MockRoundtrip roundtrip = new MockRoundtrip(ctx, ProductOrderActionBean.class, session);
        // we seem to have to make a call once to get a non-null mock bean
        roundtrip.execute("getSupportsSkippingQuote");
        ProductOrderActionBean mockActionBean = roundtrip.getActionBean(ProductOrderActionBean.class);
        // mockActionBean is not null

        mockActionBean.setProductDao(productDao);
        roundtrip.setParameter("product", product.getPartNumber());
        roundtrip.execute("getSupportsSkippingQuote");
        Assert.assertEquals(roundtrip.getOutputString(), "{\"supportsSkippingQuote\":true}");


        product.setProductFamily(new ProductFamily("Something that doesn't support optional quotes"));
        Assert.assertEquals(roundtrip.getOutputString(), "{\"supportsSkippingQuote\":false}");
    }

    private ProductDao setupMockProductDao(Product product) {
        ProductDao productDao = EasyMock.createNiceMock(ProductDao.class);
        EasyMock.expect(productDao.findByBusinessKey((String) EasyMock.anyObject())).andReturn(product).atLeastOnce();
        EasyMock.replay(productDao);
        return productDao;
    }

    //todo modify this to coincide with allowing PDMs to opt out of any order
    public void testQuoteOptOutAllowed() throws Exception {
        Product product = createSimpleProduct("P-EX-0001",
                ProductFamily.ProductFamilyInfo.SAMPLE_INITIATION_QUALIFICATION_CELL_CULTURE.getFamilyName());
        ProductDao productDao = setupMockProductDao(product);

        MockRoundtrip roundtrip = StripesMockTestUtils.createMockRoundtrip(ProductOrderActionBean.class, productDao);
        roundtrip.addParameter("product", product.getPartNumber());
        roundtrip.execute("getSupportsSkippingQuote");
        Assert.assertEquals(roundtrip.getResponse().getOutputString(), "{\"supportsSkippingQuote\":true}");
    }

    //todo modify this to coincide with allowing PDMs to opt out of any order
    public void testQuoteOptOutNotAllowed() throws Exception {
        Product product = createSimpleProduct("P-EX-0001", "Some product family that doesn't support optional quotes");
        ProductDao productDao = setupMockProductDao(product);

        MockRoundtrip roundtrip = StripesMockTestUtils.createMockRoundtrip(ProductOrderActionBean.class, productDao);
        roundtrip.addParameter("product", product.getPartNumber());
        roundtrip.execute("getSupportsSkippingQuote");
        Assert.assertEquals(roundtrip.getResponse().getOutputString(), "{\"supportsSkippingQuote\":false}");
    }

    @DataProvider(name="quoteSourceProvider")
    public Object[][] quoteSourceProvider() {
        return new Object[][]{
                // source of quote, does the quote items match the order products
                {"GP87U",ProductOrder.QuoteSourceType.QUOTE_SERVER, TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS}, //Second Parameter does not apply to quote Server case
                {"GP87U",ProductOrder.QuoteSourceType.QUOTE_SERVER, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED}, //Second Parameter does not apply to quote Server case
                {"GP87U",ProductOrder.QuoteSourceType.QUOTE_SERVER, TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER}, //Second Parameter does not apply to quote Server case
                {"CRSPEVR",ProductOrder.QuoteSourceType.QUOTE_SERVER, TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS}, //Second Parameter does not apply to quote Server case
                {"CRSPEVR",ProductOrder.QuoteSourceType.QUOTE_SERVER, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED}, //Second Parameter does not apply to quote Server case
                {"CRSPEVR",ProductOrder.QuoteSourceType.QUOTE_SERVER, TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER}, //Second Parameter does not apply to quote Server case
                {"GPSPGR7",ProductOrder.QuoteSourceType.QUOTE_SERVER, TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS}, //Second Parameter does not apply to quote Server case
                {"GPSPGR7",ProductOrder.QuoteSourceType.QUOTE_SERVER, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED}, //Second Parameter does not apply to quote Server case
                {"GPSPGR7",ProductOrder.QuoteSourceType.QUOTE_SERVER, TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER}, //Second Parameter does not apply to quote Server case
                {"testQuote",ProductOrder.QuoteSourceType.QUOTE_SERVER, TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS}, //Second Parameter does not apply to quote Server case
                {"testQuote",ProductOrder.QuoteSourceType.QUOTE_SERVER, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED}, //Second Parameter does not apply to quote Server case
                {"testQuote",ProductOrder.QuoteSourceType.QUOTE_SERVER, TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER}, //Second Parameter does not apply to quote Server case
                {"27000019",ProductOrder.QuoteSourceType.SAP_SOURCE, TestUtils.SapQuoteTestScenario.PRODUCTS_MATCH_QUOTE_ITEMS},
                {"27000019",ProductOrder.QuoteSourceType.SAP_SOURCE, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED},
                {"27000019",ProductOrder.QuoteSourceType.SAP_SOURCE, TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER}
        };
    }

    @DataProvider(name = "quoteOptionsDataProvider")
    public Object[][] quoteOptionsDataProvider() {
        String testReason = "The dog ate my quote.";
        String testQuote = "SomeQuote";
        return new Object[][]{
                {ProductOrderActionBean.SAVE_ACTION, null, "", true, "Saving any order should succeed.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.SAVE_ACTION, null, "", true, "Saving any order should succeed.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.SAVE_ACTION, "", "", true, "Saving any order should succeed.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.SAVE_ACTION, "", "", true, "Saving any order should succeed.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.PLACE_ORDER_ACTION, null, "", false, "No Quote and No reason should fail.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.PLACE_ORDER_ACTION, null, "", false, "No Quote and No reason should fail.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.SAVE_ACTION, null, testReason, true, "Saving any order should succeed.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.SAVE_ACTION, null, testReason, true, "Saving any order should succeed.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.PLACE_ORDER_ACTION, null, testReason, true,
                        "No Quote but with reason should succeed.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.PLACE_ORDER_ACTION, null, testReason, true,
                        "No Quote but with reason should succeed.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.SAVE_ACTION, testQuote, "", true, "Saving any order should succeed.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.SAVE_ACTION, testQuote, "", true, "Saving any order should succeed.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.SAVE_ACTION, testQuote, null, true, "Saving any order should succeed.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.SAVE_ACTION, testQuote, null, true, "Saving any order should succeed.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.PLACE_ORDER_ACTION, testQuote, "", true,
                        "A good quote but blank reason should succeed.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.PLACE_ORDER_ACTION, testQuote, "", true,
                        "A good quote but blank reason should succeed.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.PLACE_ORDER_ACTION, testQuote, null, true,
                        "A good quote but null reason should succeed.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.PLACE_ORDER_ACTION, testQuote, null, true,
                        "A good quote but null reason should succeed.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.SAVE_ACTION, testQuote, testReason, true, "Saving any order should succeed.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.SAVE_ACTION, testQuote, testReason, true, "Saving any order should succeed.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.PLACE_ORDER_ACTION, testQuote, testReason, true,
                        "A good quote and a reason should succeed.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.PLACE_ORDER_ACTION, testQuote, testReason, true,
                        "A good quote and a reason should succeed.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.VALIDATE_ORDER, testQuote, testReason, true,
                        "A good quote and a reason should succeed.",
                    ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.VALIDATE_ORDER, testQuote, testReason, true,
                        "A good quote and a reason should succeed.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.VALIDATE_ORDER, null, testReason, true,
                        "A good quote and a reason should succeed.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.VALIDATE_ORDER, null, testReason, true,
                        "A good quote and a reason should succeed.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE},
                {ProductOrderActionBean.VALIDATE_ORDER, null, null, false, "No quote or reason should fail.",
                        ProductOrder.QuoteSourceType.QUOTE_SERVER},
                {ProductOrderActionBean.VALIDATE_ORDER, null, null, false, "No quote or reason should fail.",
                        ProductOrder.QuoteSourceType.SAP_SOURCE}
        };
    }

    @Test(dataProvider = "quoteOptionsDataProvider")
    public void testQuoteSkippingValidation(String action, String quoteId, String reason,
                                            boolean expectedToPassValidation, String testErrorMessage,
                                            ProductOrder.QuoteSourceType quoteSourceType) {
        ProductOrder pdo = ProductOrderTestFactory.buildSampleInitiationProductOrder(22);
        pdo.setSkipQuoteReason(reason);
        pdo.setQuoteId(quoteId);
        actionBean.clearValidationErrors();
        actionBean.setEditOrder(pdo);
        actionBean.validateQuoteOptions(action);

        Assert.assertEquals(actionBean.getValidationErrors().isEmpty(), expectedToPassValidation, testErrorMessage);
    }

    public void testParentHierarchy() {
        ResearchProject grannyResearchProject = ResearchProjectTestFactory
                .createDummyResearchProject(12, "GrannyResearchProject", "To Study Stuff",
                        ResearchProject.IRB_ENGAGED);

        ResearchProject uncleResearchProject = ResearchProjectTestFactory
                .createDummyResearchProject(120, "UncleResearchProject", "To Study Stuff",
                        ResearchProject.IRB_ENGAGED);

        ResearchProject mamaResearchProject = ResearchProjectTestFactory
                .createDummyResearchProject(1200, "MamaResearchProject", "To Study Stuff",
                        ResearchProject.IRB_ENGAGED);

        ResearchProject babyResearchProject = ResearchProjectTestFactory
                .createDummyResearchProject(12000, "BabyResearchProject", "To Study Stuff",
                        ResearchProject.IRB_ENGAGED);

        babyResearchProject.setParentResearchProject(mamaResearchProject);
        mamaResearchProject.setParentResearchProject(grannyResearchProject);
        uncleResearchProject.setParentResearchProject(grannyResearchProject);

        Map<String, Collection<RegulatoryInfo>> regulatoryInfoByProject
                = actionBean.setupRegulatoryInformation(babyResearchProject);

        Assert.assertEquals(regulatoryInfoByProject.size(), 3);
        List<String> titles = Arrays.asList("MamaResearchProject", "GrannyResearchProject", "BabyResearchProject");
        for (Map.Entry<String, Collection<RegulatoryInfo>> regulatoryCollection : regulatoryInfoByProject.entrySet()) {
            Assert.assertTrue(titles.contains(regulatoryCollection.getKey()));
            Assert.assertFalse(regulatoryCollection.getKey().equals("UncleResearchProject"));
            Assert.assertEquals(regulatoryCollection.getValue().size(), 2);
        }
        Assert.assertEquals(regulatoryInfoByProject.size(), 3, "Should have three projects here.");
    }


    private void getSampleInitiationProductOrder() {
        int numberOfSamples = 4;
        actionBean.setEditOrder(ProductOrderTestFactory.buildSampleInitiationProductOrder(numberOfSamples));
    }

    public void testQuoteRequiredAfterProductChange() {
        boolean hasQuote = false;
        String quoteOrNoQuoteString = "just because";
        getSampleInitiationProductOrder();
        actionBean.clearValidationErrors();
        actionBean.validateQuoteOptions(ProductOrderActionBean.VALIDATE_ORDER);
        Assert.assertTrue(actionBean.getValidationErrors().isEmpty());

        Product dummyProduct =
                ProductTestFactory
                        .createDummyProduct(Workflow.NONE, Product.EXOME_EXPRESS_V2_PART_NUMBER, false, false);
        try {
            actionBean.getEditOrder().setProduct(dummyProduct);
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
        actionBean.getEditOrder().setQuoteId("");
        actionBean.validateQuoteOptions(ProductOrderActionBean.VALIDATE_ORDER);
        Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testGetProductOrderLink() {
        AppConfig testConfig = new AppConfig(Deployment.STUBBY) {
            @Override
            public String getUrl() {
                return "Test URL Magic String";
            }
        };
        ProductOrder order = new ProductOrder();
        order.setJiraTicketKey("PDO-1");
        Assert.assertEquals(ProductOrderActionBean.getProductOrderLink(order, testConfig),
                testConfig.getUrl() + "/orders/order.action?view=&productOrder=" + order.getJiraTicketKey());
    }

    @DataProvider(name = "regulatoryOptionsDataProvider")
    public Iterator<Object[]> regulatoryOptionsDataProvider() throws ParseException {
        Date grandfatheredInDate = DateUtils.parseDate("01/01/2014");
        Date newDate = DateUtils.parseDate(ProductOrder.IRB_REQUIRED_START_DATE_STRING);
        String skipReviewReason = "not human subjects research";
        RegulatoryInfoStub regulatoryInfo = new RegulatoryInfoStub("TEST-1234", RegulatoryInfo.Type.IRB, "12345");
        regulatoryInfo.setId(1L);
        RegulatoryInfo nullRegulatoryInfo = null;
        List<Object[]> testCases = new ArrayList<>();
        for (String action : Arrays.asList(ProductOrderActionBean.PLACE_ORDER_ACTION, ProductOrderActionBean.VALIDATE_ORDER)) {
            testCases.add(new Object[]{action, false, "", regulatoryInfo, false, grandfatheredInDate, true});
            testCases.add(new Object[]{action, false, "", nullRegulatoryInfo, false, grandfatheredInDate, true});
            testCases.add(
                    new Object[]{action, true, skipReviewReason, regulatoryInfo, false, grandfatheredInDate, true});
            testCases.add(
                    new Object[]{action, true, skipReviewReason, nullRegulatoryInfo, false, grandfatheredInDate, true});
            testCases.add(new Object[]{action, true, null, regulatoryInfo, false, grandfatheredInDate, true});
            testCases.add(new Object[]{action, true, null, nullRegulatoryInfo, false, grandfatheredInDate, true});

            testCases.add(new Object[]{action, false, "", regulatoryInfo, false, newDate, false});
            testCases.add(new Object[]{action, false, "", nullRegulatoryInfo, false, newDate, false});
            testCases.add(new Object[]{action, true, skipReviewReason, regulatoryInfo, false, newDate, false});
            testCases.add(new Object[]{action, true, skipReviewReason, nullRegulatoryInfo, false, newDate, false});
            testCases.add(new Object[]{action, true, null, regulatoryInfo, false, newDate, false});
            testCases.add(new Object[]{action, true, null, nullRegulatoryInfo, false, newDate, false});

            testCases.add(new Object[]{action, false, "", regulatoryInfo, true, newDate, true});
            testCases.add(new Object[]{action, false, "", nullRegulatoryInfo, true, newDate, false});
            testCases.add(new Object[]{action, true, skipReviewReason, regulatoryInfo, true, newDate, true});
            testCases.add(new Object[]{action, true, skipReviewReason, nullRegulatoryInfo, true, newDate, true});
            testCases.add(new Object[]{action, true, null, regulatoryInfo, true, newDate, true});
            testCases.add(new Object[]{action, true, null, nullRegulatoryInfo, true, newDate, false});
        }

        testCases.add(
                new Object[]{ProductOrderActionBean.SAVE_ACTION, false, "", regulatoryInfo, false, grandfatheredInDate,
                        true});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, false, "", nullRegulatoryInfo, false,
                grandfatheredInDate, true});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, true, skipReviewReason, regulatoryInfo, false,
                grandfatheredInDate, true});
        testCases.add(
                new Object[]{ProductOrderActionBean.SAVE_ACTION, true, skipReviewReason, nullRegulatoryInfo, false,
                        grandfatheredInDate, true});

        testCases.add(
                new Object[]{ProductOrderActionBean.SAVE_ACTION, false, "", regulatoryInfo, false, newDate, true});
        testCases.add(
                new Object[]{ProductOrderActionBean.SAVE_ACTION, false, "", nullRegulatoryInfo, false, newDate, true});
        testCases.add(
                new Object[]{ProductOrderActionBean.SAVE_ACTION, true, skipReviewReason, regulatoryInfo, false, newDate,
                        true});
        testCases.add(
                new Object[]{ProductOrderActionBean.SAVE_ACTION, true, skipReviewReason, nullRegulatoryInfo, false,
                        newDate, true});

        // skipValidation is checked but no reason is given. This should fail even if the dates are valid
        testCases.add(
                new Object[]{ProductOrderActionBean.SAVE_ACTION, true, null, regulatoryInfo, false, newDate, false});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, true, null, nullRegulatoryInfo, false, newDate,
                false});
        testCases.add(
                new Object[]{ProductOrderActionBean.SAVE_ACTION, true, null, regulatoryInfo, false, grandfatheredInDate,
                        false});
        testCases.add(new Object[]{ProductOrderActionBean.SAVE_ACTION, true, null, nullRegulatoryInfo, false,
                grandfatheredInDate, false});

        return testCases.iterator();
    }


    @Test(dataProvider = "regulatoryOptionsDataProvider")
    public void testRegulatoryInformation(String action, boolean skipRegulatory, String skipRegulatoryReason,
                                          RegulatoryInfo regulatoryInfo, boolean attestationChecked, Date placedDate,
                                          boolean expectedToPass)
            throws ParseException {
        // Set up initial state for objects and validate
        getSampleInitiationProductOrder();
        actionBean.getEditOrder().getResearchProject().getRegulatoryInfos().clear();
        Assert.assertTrue(actionBean.getEditOrder().getRegulatoryInfos().isEmpty());
        actionBean.getEditOrder().getRegulatoryInfos().clear();
        Assert.assertTrue(actionBean.getEditOrder().getRegulatoryInfos().isEmpty());
        actionBean.clearValidationErrors();
        Assert.assertTrue(actionBean.getValidationErrors().isEmpty());


        // Now test test validation using passed-in parameters.
        actionBean.setNotFromHumans(skipRegulatory);
        actionBean.getEditOrder().setSkipRegulatoryReason(skipRegulatoryReason);
        actionBean.getEditOrder().setAttestationConfirmed(attestationChecked);
        actionBean.getEditOrder().setPlacedDate(placedDate);
        if (regulatoryInfo != null) {
            regulatoryInfo.getResearchProjects().add(actionBean.getEditOrder().getResearchProject());
            actionBean.getEditOrder().getResearchProject().getRegulatoryInfos().add(regulatoryInfo);
//            actionBean.getEditOrder().getRegulatoryInfos().add(regulatoryInfo);
        }

        List<Long> regInfoIds = new ArrayList<>();

        if (regulatoryInfo != null) {
            regInfoIds.add(regulatoryInfo.getRegulatoryInfoId());
        }

        RegulatoryInfoDao regulatoryInfoDao = Mockito.mock(RegulatoryInfoDao.class);

        Mockito.when(regulatoryInfoDao.findListByList(Mockito.eq(RegulatoryInfo.class),
                Mockito.eq(RegulatoryInfo_.regulatoryInfoId), Mockito.anyCollection())).thenReturn(
                (regulatoryInfo != null) ? Collections.singletonList(regulatoryInfo) :
                        Collections.<RegulatoryInfo>emptyList());
        actionBean.setRegulatoryInfoDao(regulatoryInfoDao);
        actionBean.setSelectedRegulatoryIds(regInfoIds);

        actionBean.initRegulatoryParameter();

        actionBean.validateRegulatoryInformation(action);
        Assert.assertEquals(actionBean.getValidationErrors().isEmpty(), expectedToPass);
    }

    public void testRegulatoryInformationProjectHasNoIrbButParentDoes()
            throws ParseException {
        // set up two projects, one will be the child of the other.
        ResearchProject dummyParentProject = ResearchProjectTestFactory.createTestResearchProject();
        dummyParentProject.setJiraTicketKey("rp-parent");
        ResearchProject dummyChildProject = ResearchProjectTestFactory.createTestResearchProject();
        dummyChildProject.setJiraTicketKey("rp-child");
        // clear the regulatory infos from both of them
        dummyChildProject.getRegulatoryInfos().clear();
        dummyParentProject.getRegulatoryInfos().clear();
        // create a regulatory info and add it only to the parent.
        RegulatoryInfo regulatoryInfoFromParent =
                new RegulatoryInfo("IRB Consent", RegulatoryInfo.Type.IRB, new Date().toString());
        dummyParentProject.addRegulatoryInfo(regulatoryInfoFromParent);
        dummyChildProject.setParentResearchProject(dummyParentProject);

        // finally create a product order and add the child project to it.
        ProductOrder productOrder = ProductOrderTestFactory.buildSampleInitiationProductOrder(2);
        productOrder.setResearchProject(dummyChildProject);

        actionBean.setEditOrder(productOrder);
        RegulatoryInfoDao regulatoryInfoDao = Mockito.mock(RegulatoryInfoDao.class);

        Mockito.when(regulatoryInfoDao.findListByList(
                Mockito.eq(RegulatoryInfo.class),
                Mockito.eq(RegulatoryInfo_.regulatoryInfoId),
                Mockito.anyCollectionOf(Long.class)))
                .thenReturn(Collections.singletonList(regulatoryInfoFromParent));
        actionBean.setRegulatoryInfoDao(regulatoryInfoDao);
        actionBean.setSelectedRegulatoryIds(Collections.singletonList(1234l));
        actionBean.getEditOrder().setAttestationConfirmed(true);

        actionBean.initRegulatoryParameter();

        // test validation for all pertinent actions.
        for (String action : Arrays.asList(ProductOrderActionBean.SAVE_ACTION, ProductOrderActionBean.VALIDATE_ORDER,
                ProductOrderActionBean.PLACE_ORDER_ACTION)) {
            actionBean.validateRegulatoryInformation(action);
            Assert.assertTrue(actionBean.getValidationErrors().isEmpty(), "Validation failed for " + action);
        }
    }

    /**
     * Test that, when not skipping regulatory information, the regulatory information is retained while any reason for
     * skipping (which may have been copied from the web form in the case where the user is changing to not skipping) is
     * cleared.
     */
    public void testUpdateRegulatoryInformationNotSkipping() {
        setupForChangingRegInfoSkip();
        actionBean.setSkipRegulatoryInfo(false);

        actionBean.updateRegulatoryInformation();

        assertThat(pdo.getRegulatoryInfos(), not(empty()));
        assertThat(pdo.getSkipRegulatoryReason(), isEmptyOrNullString());
    }

    /**
     * Test that, when skipping regulatory information, the reason for skipping is retained while any selected
     * regulatory information (which may have been copied from the web form in the case where the user is changing to
     * skipping) is cleared.
     */
    public void testUpdateRegulatoryInformationSkipping() {
        setupForChangingRegInfoSkip();
        actionBean.setSkipRegulatoryInfo(true);

        actionBean.updateRegulatoryInformation();

        assertThat(pdo.getRegulatoryInfos(), empty());
        assertThat(pdo.getSkipRegulatoryReason(), not(isEmptyOrNullString()));
    }

    /**
     * This test ony applies to the Quote Server
     * @throws Exception
     */
    public void testValueOfOrder() throws Exception {

        final FundingLevel fundingLevel = new FundingLevel();
        Funding funding = new Funding(Funding.FUNDS_RESERVATION, "test", "c333");
        fundingLevel.setFunding(Collections.singleton(funding));
        Collection<FundingLevel> fundingLevelCollection = Collections.singleton(fundingLevel);
        QuoteFunding quoteFunding = new QuoteFunding(fundingLevelCollection);
        final String testQuoteIdentifier = "testQuote";
        final String jiraTicketKey = "PDO-TESTPDOValue";

        Quote testQuote = buildSingleTestQuote(testQuoteIdentifier, null);

        Product primaryProduct = new Product();
        primaryProduct.setPartNumber("P-Test_primary");
        primaryProduct.setPrimaryPriceItem(new PriceItem("primary", "Genomics Platform", "Primary testing size",
                "Thousand dollar Genome price"));
        primaryProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.WHOLE_GENOME.getFamilyName()));


        ProductOrder testOrder = new ProductOrder();
        testOrder.setJiraTicketKey(jiraTicketKey);
        testOrder.setProduct(primaryProduct);
        testOrder.setQuoteId(testQuoteIdentifier);

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();

        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        final Product primaryOrderProduct = testOrder.getProduct();
        returnMaterials.add(new SAPMaterial(primaryOrderProduct.getPartNumber(), broad, broad.getDefaultWbs(),
            "test description", "2000", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,
                null, null, Collections.emptyMap(),
                Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
            broad.getSalesOrganization()));

        final String priceItemPrice = "2000";
        final String quoteItemQuantity = "2000";
        final String quoteItemPrice = "1000";
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, primaryOrderProduct, priceItemPrice,
                quoteItemQuantity, quoteItemPrice);

        Product addonNonSeqProduct = new Product();
        addonNonSeqProduct.setPartNumber("ADD-NON-SEQ");
        addonNonSeqProduct.setPrimaryPriceItem(new PriceItem("Secondary", "Genomics Platform",
                "secondary testing size", "Extraction price"));
        addonNonSeqProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.ALTERNATE_LIBRARY_PREP_DEVELOPMENT.getFamilyName()));

        returnMaterials.add(new SAPMaterial(addonNonSeqProduct.getPartNumber(), broad, broad.getDefaultWbs(), "test description", "1573",
                    SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, new Date(), new Date(),
                    Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                    broad.getSalesOrganization()));

        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, addonNonSeqProduct, "1573", "2000", "573"
        );


        testOrder.updateAddOnProducts(Collections.singletonList(addonNonSeqProduct));

        List<ProductOrderSample> sampleList = new ArrayList<>();

        for (int i = 0; i < 75;i++) {
            sampleList.add(new ProductOrderSample("SM-Test"+i));
        }

        testOrder.setSamples(sampleList);
        testOrder.setLaneCount(BigDecimal.valueOf(5));

        Product seqProduct = new Product();
        seqProduct.setPartNumber("ADD-SEQ");
        seqProduct.setPrimaryPriceItem(new PriceItem("Third", "Genomics Platform", "Seq Testing Size",
                "Put it on the sequencer"));
        seqProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.SEQUENCE_ONLY.getFamilyName()));

        addSapMaterial(returnMaterials, seqProduct, "3500", SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);

        addSapMaterial(returnMaterials, testOrder.getProduct(), "2000",
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        addSapMaterial(returnMaterials, addonNonSeqProduct, "1573",
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);

        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, seqProduct, "3500", "2000", "2500"
        );
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenThrow(new SAPIntegrationException("Sap Client should not be called with quote server quotes"));
        stubProductPriceCache.refreshCache();
        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);

        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote),
                BigDecimal.valueOf(1573 * testOrder.getSamples().size() + 2000 * testOrder.getSamples().size()));
        testQuote.setQuoteItems(quoteItems);

        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote),
                BigDecimal.valueOf((573 * testOrder.getSamples().size() + 1000 * testOrder.getSamples().size())));

        testOrder.updateAddOnProducts(Arrays.asList(addonNonSeqProduct, seqProduct));

        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote),
                BigDecimal.valueOf(573 * testOrder.getSamples().size() + 1000 * testOrder.getSamples().size())
                        .add(BigDecimal.valueOf(2500).multiply(testOrder.getLaneCount())));

        testOrder.setProduct(seqProduct);

        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote),
                BigDecimal.valueOf(573 * testOrder.getSamples().size())
                        .add(BigDecimal.valueOf(2500).multiply(testOrder.getLaneCount()))
                        .add(BigDecimal.valueOf(2500).multiply(testOrder.getLaneCount())));

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 75);
        ProductOrderSample abandonedSample = testOrder.getSamples().get(0);
        abandonedSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);
        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote),
                BigDecimal.valueOf(573 * (testOrder.getSamples().size() - 1))
                        .add(BigDecimal.valueOf(2500).multiply(testOrder.getLaneCount()))
                        .add(BigDecimal.valueOf(2500).multiply(testOrder.getLaneCount())));

        ProductOrder testChildOrder = new ProductOrder();
        testChildOrder.setJiraTicketKey("PDO-ChildTestValue");

        testChildOrder.setSamples(Collections.singletonList(new ProductOrderSample("SM-TestChild1")));
        testOrder.addChildOrder(testChildOrder);

        Assert.assertEquals(testChildOrder.getUnbilledSampleCount(), 1);
        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);

        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote),
                BigDecimal.valueOf(573 * (testOrder.getSamples().size() - 1))
                        .add(BigDecimal.valueOf(2500).multiply(testOrder.getLaneCount()))
                        .add(BigDecimal.valueOf(2500).multiply(testOrder.getLaneCount()))
        );

        testChildOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote),
                BigDecimal.valueOf(573 * (testOrder.getSamples().size() - 1))
                        .add(BigDecimal.valueOf(2500).multiply(testOrder.getLaneCount()))
                        .add(BigDecimal.valueOf(2500).multiply(testOrder.getLaneCount()))
                        .add(BigDecimal.valueOf((573 * testChildOrder.getSamples().size()))));
    }

    /**
     * This order only applies to Quote Server orders
     * @throws Exception
     */
    public void testEstimateNoSAPOrders() throws Exception {
        final String testQuoteIdentifier = "testQuote";
        Quote testQuote = buildSingleTestQuote(testQuoteIdentifier, null);

        Product primaryProduct = new Product();
        primaryProduct.setPartNumber("P-Test_primary");
        primaryProduct.setPrimaryPriceItem(new PriceItem("primary", "Genomics Platform", "Primary testing size",
                "Thousand dollar Genome price"));
        primaryProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.WHOLE_GENOME.getFamilyName()));

        Product addonNonSeqProduct = new Product();
        addonNonSeqProduct.setPartNumber("ADD-NON-SEQ");
        addonNonSeqProduct.setPrimaryPriceItem(new PriceItem("Secondary", "Genomics Platform",
                "secondary testing size", "Extraction price"));
        addonNonSeqProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.ALTERNATE_LIBRARY_PREP_DEVELOPMENT.getFamilyName()));

        Product seqProduct = new Product();
        seqProduct.setPartNumber("ADD-SEQ");
        seqProduct.setPrimaryPriceItem(new PriceItem("Third", "Genomics Platform", "Seq Testing Size",
                "Put it on the sequencer"));
        seqProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.SEQUENCE_ONLY.getFamilyName()));


        ProductOrder testOrder = new ProductOrder();
        testOrder.setJiraTicketKey("PDO-TESTPDOValue");
        testOrder.setProduct(primaryProduct);
        testOrder.setQuoteId(testQuoteIdentifier);
        testOrder.updateAddOnProducts(Collections.singletonList(addonNonSeqProduct));


        List<ProductOrderSample> sampleList = new ArrayList<>();

        for (int i = 0; i < 75;i++) {
            sampleList.add(new ProductOrderSample("SM-Test"+i));
        }

        testOrder.setSamples(sampleList);
        testOrder.setLaneCount(BigDecimal.valueOf(5));

        SapOrderDetail sapReference = new SapOrderDetail("test001", 75, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        testOrder.addSapOrderDetail(sapReference);

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();

        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, testOrder.getProduct(), "2000", "2000",
                "1000");
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, addonNonSeqProduct, "1573", "2000", "573");
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, seqProduct, "3500", "2000", "2500");
        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;

        addSapMaterial(returnMaterials, testOrder.getProduct(), "2000",
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        addSapMaterial(returnMaterials, addonNonSeqProduct, "1573",
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        addSapMaterial(returnMaterials, seqProduct, "3500", SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);


        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenThrow(new SAPIntegrationException("SAP Should not be called for Quote Server quote"));
        stubProductPriceCache.refreshCache();
        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuoteIdentifier)).thenReturn(testQuote);

        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn(Collections.singletonList(testOrder));

        actionBean.setEditOrder(testOrder);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote,0, null),
                getCalculatedOrderValue(priceList, testOrder, ProductOrder.QuoteSourceType.QUOTE_SERVER, testQuote));
        testQuote.setQuoteItems(quoteItems);

        Assert.assertEquals(actionBean.estimateOutstandingOrders( testQuote,0, null),
                getCalculatedOrderValue(priceList, testOrder, ProductOrder.QuoteSourceType.QUOTE_SERVER, testQuote));

        testOrder.updateAddOnProducts(Arrays.asList(addonNonSeqProduct, seqProduct));

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote,0, null),
                getCalculatedOrderValue(priceList, testOrder, ProductOrder.QuoteSourceType.QUOTE_SERVER, testQuote));

        testOrder.setProduct(seqProduct);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote,0, null),
                getCalculatedOrderValue(priceList, testOrder, ProductOrder.QuoteSourceType.QUOTE_SERVER, testQuote));

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 75);
        ProductOrderSample abandonedSample = testOrder.getSamples().get(0);
        abandonedSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);
        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                getCalculatedOrderValue(priceList, testOrder, ProductOrder.QuoteSourceType.QUOTE_SERVER, testQuote));


        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                getCalculatedOrderValue(priceList, testOrder, ProductOrder.QuoteSourceType.QUOTE_SERVER, testQuote));

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                getCalculatedOrderValue(priceList, testOrder, ProductOrder.QuoteSourceType.QUOTE_SERVER, testQuote));
    }

    @Test(dataProvider = "quoteSourceProvider")
    public void testEstimateSomeSAPOrders(String testQuoteIdentifier, ProductOrder.QuoteSourceType quoteSource,
                                          TestUtils.SapQuoteTestScenario quoteItemsMatchOrderProducts) throws Exception {

        Quote testQuote = buildSingleTestQuote(testQuoteIdentifier, null);

        Product primaryProduct = new Product();
        primaryProduct.setPartNumber("P-Test_primary");
        primaryProduct.setProductName("primary product name");
        primaryProduct.setPrimaryPriceItem(new PriceItem("primary", "Genomics Platform", "Primary testing size",
                "Thousand dollar Genome price"));
        primaryProduct
                .setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.WHOLE_GENOME.getFamilyName()));

        Product addonNonSeqProduct = new Product();
        addonNonSeqProduct.setPartNumber("ADD-NON-SEQ");
        addonNonSeqProduct.setProductName("Non Seq Addon Product Name");
        addonNonSeqProduct.setPrimaryPriceItem(new PriceItem("Secondary", "Genomics Platform",
                "secondary testing size", "Extraction price"));
        addonNonSeqProduct.setProductFamily(
                new ProductFamily(ProductFamily.ProductFamilyInfo.ALTERNATE_LIBRARY_PREP_DEVELOPMENT.getFamilyName()));

        Product seqProduct = new Product();
        seqProduct.setPartNumber("ADD-SEQ");
        seqProduct.setProductName("Seq addon product name");
        seqProduct.setPrimaryPriceItem(new PriceItem("Third", "Genomics Platform", "Seq Testing Size",
                "Put it on the sequencer"));
        seqProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.SEQUENCE_ONLY.getFamilyName()));

        Product dummyProduct1 = new Product();
        dummyProduct1.setPartNumber("ADD-DMY-0001");
        dummyProduct1.setProductName("dummy addon product name");
        dummyProduct1.setPrimaryPriceItem(new PriceItem("fourth", "Genomics Platform", "Testing Size",
                "extra 1"));
        dummyProduct1.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.EXOME.getFamilyName()));

        testOrder = new ProductOrder();
        testOrder.setJiraTicketKey("PDO-TESTPDOValue");
        testOrder.setProduct(primaryProduct);
        testOrder.setQuoteId(testQuoteIdentifier);
        testOrder.updateAddOnProducts(Collections.singletonList(addonNonSeqProduct));

        List<ProductOrderSample> sampleList = new ArrayList<>();

        for (int i = 0; i < 75; i++) {
            sampleList.add(new ProductOrderSample("SM-Test" + i));
        }

        testOrder.setSamples(sampleList);
        testOrder.setLaneCount(BigDecimal.valueOf(5));

        SapOrderDetail sapReference = new SapOrderDetail("test001", 75, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        testOrder.addSapOrderDetail(sapReference);

        ProductOrder extraOrder1 = new ProductOrder();
        extraOrder1.setJiraTicketKey("PDO-extra1");
        extraOrder1.setProduct(dummyProduct1);
        extraOrder1.setQuoteId(testQuoteIdentifier);

        IntStream.range(0, 10).forEach(sampleRangeIterator -> {
            extraOrder1.addSample(new ProductOrderSample("extra1-Test" + sampleRangeIterator));
        });
        SapOrderDetail sapReferenceEx1 = new SapOrderDetail("Test_listed_1", 10, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        extraOrder1.addSapOrderDetail(sapReferenceEx1);

        ProductOrder extraOrder2 = new ProductOrder();
        extraOrder2.setJiraTicketKey("PDO-extra2");
        extraOrder2.setProduct(dummyProduct1);
        extraOrder2.setQuoteId(testQuoteIdentifier);

        IntStream.range(0, 23).forEach(sampleRangeIterator -> {
            extraOrder2.addSample(new ProductOrderSample("extra1-Test" + sampleRangeIterator));
        });
        SapOrderDetail sapReferenceEx2 = new SapOrderDetail("Test_listed_2", 23, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        extraOrder2.addSapOrderDetail(sapReferenceEx2);

        ProductOrder extraOrder3 = new ProductOrder();
        extraOrder3.setJiraTicketKey("PDO-extra3");
        extraOrder3.setProduct(dummyProduct1);
        extraOrder3.setQuoteId(testQuoteIdentifier);

        IntStream.range(0, 49).forEach(sampleRangeIterator -> {
            extraOrder3.addSample(new ProductOrderSample("extra1-Test" + sampleRangeIterator));
        });
        SapOrderDetail sapReferenceEx3 = new SapOrderDetail("Test_listed_3", 49, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        extraOrder3.addSapOrderDetail(sapReferenceEx3);

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();

        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, testOrder.getProduct(),
                "2000", "2000", "1000");
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, addonNonSeqProduct,
                "1573", "2000", "573");
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, seqProduct,
                "3500", "2000", "2500");
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, dummyProduct1,
                "1", "100", "1");


        addSapMaterial(returnMaterials, testOrder.getProduct(), "2000",
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        addSapMaterial(returnMaterials, addonNonSeqProduct, "1573",
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        addSapMaterial(returnMaterials, seqProduct, "3500",
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        addSapMaterial(returnMaterials, dummyProduct1, "1",
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);


        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString()))
                    .thenReturn(returnMaterials);
        } else {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString()))
                    .thenThrow(new SAPIntegrationException("Sap should not be called for Quote Server Quote"));
        }
        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            stubProductPriceCache.refreshCache();
            Mockito.when(mockQuoteService.getAllPriceItems())
                    .thenThrow(new QuoteServerException("Quote Server should not be called for SAP Quote"));
            Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuoteIdentifier))
                    .thenThrow(new QuoteServerException("Quote Server should not be called for SAP Quote"));
        } else {
            Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
            Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuoteIdentifier)).thenReturn(testQuote);
        }

        final List<ProductOrder> allOrders = Arrays.asList(
                testOrder, extraOrder1, extraOrder2, extraOrder3);
        final List<ProductOrder> allOrdersMinusTestOrder = Arrays.asList(
                extraOrder1, extraOrder2, extraOrder3);

        final List<ProductOrder> mockPDOListAnswer = new ArrayList<>();
        mockPDOListAnswer.clear();
        mockPDOListAnswer.addAll(allOrders);

        if (!ProductOrderActionBean.EXCLUDED_QUOTES_FROM_VALUE.contains(testQuoteIdentifier)) {
            Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString()))
                    .thenAnswer(new Answer<List<ProductOrder>>() {
                        @Override
                        public List<ProductOrder> answer(InvocationOnMock invocationOnMock) throws Throwable {
                            return mockPDOListAnswer;
                        }
                    });
        } else {
            Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString()))
                    .thenThrow(new RuntimeException("FInd Orders with common quote should not be called at this time"));
        }


        final Set<OrderValue> sapOrderValues = new HashSet<>();
        sapOrderValues.add(new OrderValue("Test_listed_1", BigDecimal.TEN));
        sapOrderValues.add(new OrderValue("Test_listed_2", new BigDecimal(23)));
        sapOrderValues.add(new OrderValue("Test_listed_3", new BigDecimal(49)));

        final BigDecimal extraOrderValues;
        if (!ProductOrderActionBean.EXCLUDED_QUOTES_FROM_VALUE.contains(testQuoteIdentifier)) {
            extraOrderValues = getCalculatedOrderValue(priceList, extraOrder1, quoteSource, testQuote)
                    .add(getCalculatedOrderValue(priceList, extraOrder2, quoteSource, testQuote))
                    .add(getCalculatedOrderValue(priceList, extraOrder3, quoteSource, testQuote));
        } else {
            extraOrderValues = new BigDecimal(0);
        }

        Set<OrderValue> allValues = new HashSet<>(sapOrderValues);
        allValues.add(new OrderValue(testOrder.getSapOrderNumber(), getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote)));
        OrderCalculatedValues calculatedOrderReturnValue = null;
        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenAnswer(new Answer<SapQuote>() {
                @Override
                public SapQuote answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return TestUtils.buildTestSapQuote(testQuoteIdentifier,
                            extraOrderValues.add(getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote)),
                            BigDecimal.valueOf(800000),
                            testOrder, quoteItemsMatchOrderProducts, "GP01");
                }
            });
            String sapOrderNumber = null;
            Optional<ProductOrder> editOrder = Optional.ofNullable(actionBean.getEditOrder());
            if(editOrder.isPresent()) {
                sapOrderNumber = editOrder.get().getSapOrderNumber();
            }
            calculatedOrderReturnValue = new OrderCalculatedValues(BigDecimal.ZERO, allValues,
                    sapOrderNumber,
                    mockSAPService.findSapQuote(testQuoteIdentifier).getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);

        } else {
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class)))
                    .thenThrow(new SAPIntegrationException("Sap Should not be called for Quote Server Quote"));

            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                    .thenThrow(new SAPIntegrationException("Sap Should not be called for Quote Server Quote"));

        }

        actionBean.setEditOrder(testOrder);

        BigDecimal calculatedOrderValue = BigDecimal.ZERO;
        if (!ProductOrderActionBean.EXCLUDED_QUOTES_FROM_VALUE.contains(testQuote.getAlphanumericId())) {
            calculatedOrderValue = getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote);
        }
        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                    calculatedOrderValue.add(extraOrderValues));
        } else {
            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(mockSAPService.findSapQuote(testOrder.getQuoteId()),0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }
        testQuote.setQuoteItems(quoteItems);

        if (!ProductOrderActionBean.EXCLUDED_QUOTES_FROM_VALUE.contains(testQuote.getAlphanumericId())) {
            calculatedOrderValue = getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote);
        }
        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders( testQuote, 0, null),
                    calculatedOrderValue.add(extraOrderValues));
        } else {
            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(mockSAPService.findSapQuote(testOrder.getQuoteId()),0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        testOrder.updateAddOnProducts(Arrays.asList(addonNonSeqProduct, seqProduct));

        if (!ProductOrderActionBean.EXCLUDED_QUOTES_FROM_VALUE.contains(testQuote.getAlphanumericId())) {
            calculatedOrderValue = getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote);
        }
        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                    calculatedOrderValue.add(extraOrderValues));
        } else {
            final SapQuote sapQuote = mockSAPService.findSapQuote(testOrder.getQuoteId());
            allValues = new HashSet<>(sapOrderValues);
            allValues.add(new OrderValue(testOrder.getSapOrderNumber(),
                    getCalculatedOrderValue(priceList, testOrder, ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote)));
            calculatedOrderReturnValue = new OrderCalculatedValues(BigDecimal.ZERO, allValues,
                    actionBean.getEditOrder().getSapOrderNumber(), sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);

            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(sapQuote,0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        testOrder.setProduct(seqProduct);

        if (!ProductOrderActionBean.EXCLUDED_QUOTES_FROM_VALUE.contains(testQuote.getAlphanumericId())) {
            calculatedOrderValue = getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote);
        }
        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                    calculatedOrderValue .add(extraOrderValues));
        } else {
            final SapQuote sapQuote = mockSAPService.findSapQuote(testOrder.getQuoteId());
            allValues = new HashSet<>(sapOrderValues);
            allValues.add(new OrderValue(testOrder.getSapOrderNumber(),
                    getCalculatedOrderValue(priceList, testOrder, ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote)));
            calculatedOrderReturnValue = new OrderCalculatedValues(BigDecimal.ZERO, allValues,
                    actionBean.getEditOrder().getSapOrderNumber(), sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);

            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(sapQuote,0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 75);
        ProductOrderSample abandonedSample = testOrder.getSamples().get(0);
        abandonedSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);
        if (!ProductOrderActionBean.EXCLUDED_QUOTES_FROM_VALUE.contains(testQuote.getAlphanumericId())) {
            calculatedOrderValue = getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote);
        }
        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                    calculatedOrderValue .add(extraOrderValues));
        } else {
            final SapQuote sapQuote = mockSAPService.findSapQuote(testOrder.getQuoteId());
            allValues = new HashSet<>(sapOrderValues);
            allValues.add(new OrderValue("test001", getCalculatedOrderValue(priceList, testOrder,
                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote)));
            calculatedOrderReturnValue = new OrderCalculatedValues(BigDecimal.ZERO, allValues,
                    actionBean.getEditOrder().getSapOrderNumber(), sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);

            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(sapQuote,0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);

        if (!ProductOrderActionBean.EXCLUDED_QUOTES_FROM_VALUE.contains(testQuote.getAlphanumericId())) {
            calculatedOrderValue = getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote);
        }
        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                    calculatedOrderValue.add(extraOrderValues));
        } else {
            final SapQuote sapQuote = mockSAPService.findSapQuote(testOrder.getQuoteId());
            allValues = new HashSet<>(sapOrderValues);
            allValues.add(new OrderValue("test001", getCalculatedOrderValue(priceList, testOrder,
                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote)));
            calculatedOrderReturnValue =
                    new OrderCalculatedValues(BigDecimal.ZERO, allValues, actionBean.getEditOrder().getSapOrderNumber(),
                            sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);

            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(sapQuote,0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        if (!ProductOrderActionBean.EXCLUDED_QUOTES_FROM_VALUE.contains(testQuote.getAlphanumericId())) {
            calculatedOrderValue = getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote);
        }
        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                    calculatedOrderValue.add(extraOrderValues));
        } else {
            allValues = new HashSet<>(sapOrderValues);
            allValues.add(new OrderValue("test001", getCalculatedOrderValue(priceList, testOrder,
                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote)));
            calculatedOrderReturnValue =
                    new OrderCalculatedValues(BigDecimal.ZERO, allValues,
                            actionBean.getEditOrder().getSapOrderNumber(),
                            mockSAPService.findSapQuote(testQuoteIdentifier).getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);
            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(mockSAPService.findSapQuote(testOrder.getQuoteId()),0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }
    }

    /**
     * This unit test primarily tests a pretty basic case:
     * <ol>
     *     <li>Attempt to place a new order where there are no existing orders associated with the Given quote</li>
     *     <li>Validate that the Calculated value of that one order matches what is expected</li>
     *     <li> "Place" the order</li>
     *     <li>attempt to modify that order and estimate the new calculated value</li>
     *     <li>add on more sample to the order and ensure that the Action bean logic rejects this addition since there
     *     is no more money left on the quote to support it.</li>
     * </ol>
     * @param quoteSource
     * @param quoteName Identifier for the Quote to use.  Quote definition is pulled from quoteTestData.xml
     * @throws Exception
     */
    @Test(dataProvider = "quoteDataProvider")
    public void testEstimateOpenOrdersWeirdQuotes(String quoteName, boolean productExistInSap,
                                                  ProductOrder.QuoteSourceType quoteSource)
            throws Exception {

        Quote testQuote = stubQuoteService.getQuoteByAlphaId(quoteName);

        Product primaryProduct = new Product();
        primaryProduct.setPartNumber("P-Test_Prime");
        primaryProduct.setPrimaryPriceItem(new PriceItem("primary", "GP", "primary size", "overfundme"));
        primaryProduct
                .setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.WHOLE_GENOME.getFamilyName()));

        testOrder = new ProductOrder();
        testOrder.setProduct(primaryProduct);
        testOrder.setQuoteId(testQuote.getAlphanumericId());
        actionBean.setQuoteIdentifier(quoteName);

        List<ProductOrderSample> sampleList = new ArrayList<>();
        final int sampleTestSize = 75;
        for (int i = 0; i < sampleTestSize; i++) {
            sampleList.add(new ProductOrderSample("SM-Test" + i));
        }
        testOrder.setSamples(sampleList);

        PriceList priceList = new PriceList();
        Set<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> materials = new HashSet<>();

        final BigDecimal priceItemPrice = (new BigDecimal(testQuote.getQuoteFunding().getFundsRemaining())
                .divide(BigDecimal.valueOf(testOrder.getSamples().size()),2, RoundingMode.FLOOR))
                ;
        final String stringPrice = String.valueOf(priceItemPrice);
        addPriceItemForProduct(testQuote.getAlphanumericId(), priceList, quoteItems, testOrder.getProduct(),
                stringPrice, "1000", stringPrice);
        if(productExistInSap) {
            addSapMaterial(materials, testOrder.getProduct(), stringPrice,
                    SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        }

        final Set<OrderValue> sapOrderValues = new HashSet<>();
        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(materials);
            stubProductPriceCache.refreshCache();
            Mockito.when(mockQuoteService.getAllPriceItems()).thenThrow(new QuoteServerException("Quote Server should not be called for SAP Quote"));
            Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuote.getAlphanumericId())).thenThrow(new QuoteServerException("Quote Server should not be called for SAP Quote"));

            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenAnswer(new Answer<OrderCalculatedValues>() {
                        @Override
                        public OrderCalculatedValues answer(InvocationOnMock invocationOnMock) throws Throwable {
                            BigDecimal fundsRemaining = ((SapQuote) invocationOnMock.getArguments()[0]).getQuoteHeader().fundsRemaining();
                            String potentialOrderId = null;

                            Optional<OrderCriteria> orderCriteria = Optional.ofNullable((OrderCriteria) invocationOnMock.getArguments()[1]);

                            if(orderCriteria.isPresent()) {
                                potentialOrderId = orderCriteria.get().getSapOrderID();
                            }

                            return getTestCalculatedValues(priceList, sapOrderValues, quoteSource, testQuote,
                                    testOrder, fundsRemaining);
                        }
                    });
            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenAnswer(new Answer<SapQuote>() {
                @Override
                public SapQuote answer(InvocationOnMock invocationOnMock) throws Throwable {
                    final BigDecimal totalOpenOrderValue =
                            getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote);
                    final SapQuote sapQuote = TestUtils.buildTestSapQuote(testOrder.getQuoteId(),
                            totalOpenOrderValue,
                            totalOpenOrderValue.add(new BigDecimal(testQuote.getQuoteFunding().getFundsRemaining())),
                            testOrder, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED, "GP01");
                    return sapQuote;
                }
            });
            stubProductPriceCache.refreshCache();
        } else {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString()))
                    .thenThrow(new SAPIntegrationException("SAP should not be called for Quote Server Quote"));
            Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
            Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuote.getAlphanumericId())).thenReturn(testQuote);

            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenThrow(new SAPIntegrationException("SAP should not be called for Quote Server Quote"));
            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                    .thenThrow(new SAPIntegrationException("SAP should not be called for Quote Server Quote"));
        }

        actionBean.setEditOrder(testOrder);

        if(quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                    getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote));
        }

        testOrder.setJiraTicketKey("PDO-1294");

        SapOrderDetail sapReference = new SapOrderDetail("test001", sampleTestSize, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        testOrder.addSapOrderDetail(sapReference);

        sapOrderValues.add(new OrderValue(testOrder.getSapOrderNumber(), getCalculatedOrderValue(priceList, testOrder, ProductOrder.QuoteSourceType.SAP_SOURCE,testQuote)));

        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn(Collections.singletonList(testOrder));

        if(quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER ) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                    getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote));
        }

        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            actionBean.validateQuoteDetails(testQuote, 0);
        } else {
            actionBean.validateSapQuoteDetails(mockSAPService.findSapQuote(testOrder.getQuoteId()), 0);

        }

        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty(),
                "Errors occurred validating Quote details.  Funds remaining is "
                +testQuote.getQuoteFunding().getFundsRemaining()+" and price is "+priceItemPrice);

        testOrder.addSample(new ProductOrderSample("SM-Test" + 76));

        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            actionBean.validateQuoteDetails(testQuote, 0);
        } else {
            actionBean.validateSapQuoteDetails(mockSAPService.findSapQuote(testOrder.getQuoteId()), 0);
        }

        Assert.assertFalse(actionBean.getContext().getValidationErrors().isEmpty(),
                "Errors occurred validating Quote details.  Funds remaining is "
                +testQuote.getQuoteFunding().getFundsRemaining() + " and price is " + priceItemPrice +
                " Calculated Value is " + getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote));
    }

    /**
     *
     * todo For SAP 2.0, this test no longer seems valid
     *
     * This test case sort of does a full gamut of testing order estimation with regards to SAP and not SAP.  It
     * utilizes the same inputs as the prior test case to show that the diverse quote setups will provide the same
     * results.
     *
     * In order to adjust the needs of this test case to the varying setups of the quotes (specifically available funds)
     * the price of each product is dynamically calculated using the size of the orders which will be used with respect
     * to the available funding on the quote.  This way the orders being used will just use up all the remaining funds
     * on the quote.  After that has been determined to calculate according to the estimates, the test will add one
     * more sample to put the total unbilled count just over the funds remaining and thus causing an error.  That error
     * will be considered a successful test.
     *
     * The sequence of events mimicked in this test case are as follows:
     * <ol>
     *     <li>create and validate a product order that has not been placed.  Mimics the validation called when the
     *     order is attempting to be placed</li>
     *     <li>Mimic that the order has been placed and has gone to SAP</li>
     *     <li>Mimic validating the order again after another order has been placed and gone to SAP</li>
     *     <li>Mimic validating the order again after that second order has had a customized price set on it</li>
     *     <li>Force the Quote to no longer be eligible for SAP</li>
     *     <li>Mimic validating the order again after a third order has been placed, but is not in SAP (due to the fact
     *     that the quote is not eligible for SAP</li>
     *     <li>Add one more sample to the main product order</li>
     *     <li>Mimic validating this order again after the sample has been added to it</li>
     *     <li>Ensure that valdiateQuoteDetails returns an error due to the fact that the Quote now is over extended</li>
     * </ol>
     *
     * @param quoteSource
     * @param quoteId Identifier for the Quote to use.  Quote definition is pulled from quoteTestData.xml
     * @throws Exception
     */
    @Deprecated
    @Test(dataProvider = "quoteDataProvider", enabled = false)
    public void testSapEligibleQuoteThenNot(String quoteId, boolean productExistInSap,
                                            ProductOrder.QuoteSourceType quoteSource)
            throws Exception {
        Quote testQuote = stubQuoteService.getQuoteByAlphaId(quoteId);
        final int sampleTestSize = 15;

        final SAPAccessControl enabledControl = new SAPAccessControl();

        Mockito.when(mockAccessController.getCurrentControlDefinitions()).thenThrow(new RuntimeException());
        stubProductPriceCache.setAccessControlEjb(mockAccessController);

        // to derive price, each sample for this product is worth 1
        Product primaryProduct = new Product();
        primaryProduct.setPartNumber("P-Test_Prime");
        primaryProduct.setPrimaryPriceItem(new PriceItem("primary", "GP", "primary size", "overfundme"));
        primaryProduct
                .setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.WHOLE_GENOME.getFamilyName()));

        // to derive price, each sample for this product is worth .5
        Product secondaryProduct = new Product();
        secondaryProduct.setPartNumber("P-TEST-SECOND");
        secondaryProduct.setPrimaryPriceItem(new PriceItem("secondary", "GP", "primary size", "second"));
        secondaryProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.EXOME.getFamilyName()));

        // to derive price, each sample for this product is worth .25
        Product addOnProduct = new Product();
        addOnProduct.setPartNumber("P-ADD-ON");
        addOnProduct.setPrimaryPriceItem(new PriceItem("addon", "GP", "primary size", "addon"));
        addOnProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.SEQUENCE_ONLY.getFamilyName()));

        testOrder = new ProductOrder();
        testOrder.setProduct(primaryProduct);
        testOrder.setQuoteId(testQuote.getAlphanumericId());

        ProductOrder secondOrder = new ProductOrder();
        secondOrder.setProduct(secondaryProduct);
        secondOrder.updateAddOnProducts(Collections.singletonList(addOnProduct));
        secondOrder.setQuoteId(testQuote.getAlphanumericId());
        secondOrder.setJiraTicketKey("PDO-Second");

        SapOrderDetail secondOrderSap = new SapOrderDetail("test002", sampleTestSize, secondOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        secondOrder.addSapOrderDetail(secondOrderSap);
        secondOrder.setLaneCount(BigDecimal.valueOf(7));


        ProductOrder nonSAPOrder = new ProductOrder();
        nonSAPOrder.setProduct(primaryProduct);
        nonSAPOrder.updateAddOnProducts(Collections.singletonList(addOnProduct));
        nonSAPOrder.setQuoteId(testQuote.getAlphanumericId());
        nonSAPOrder.setJiraTicketKey("PDO-NOSAP");
        nonSAPOrder.setLaneCount(BigDecimal.valueOf(3));

        List<ProductOrderSample> sampleList = new ArrayList<>();
        List<ProductOrderSample> secondSampleList = new ArrayList<>();
        List<ProductOrderSample> nonSapSampleList = new ArrayList<>();
        for (int i = 0; i < sampleTestSize; i++) {
            sampleList.add(new ProductOrderSample("SM-Test" + i));
            secondSampleList.add(new ProductOrderSample("SM-second" +i));
            if(i % 2 == 0) {
                nonSapSampleList.add(new ProductOrderSample("SM-NonSAP" +i));
            }
        }
        testOrder.setSamples(sampleList);
        secondOrder.setSamples(secondSampleList);
        nonSAPOrder.setSamples(nonSapSampleList);

        PriceList priceList = new PriceList();
        Set<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> materials = new HashSet<>();

        // Using the size of the orders we plan to use, figure out the price for each product to make the funds
        // remaining just cover our orders.  This way when we add a sample to any order, it will put us over the mark
        // and allow us to test the condition of running out of funds
        final BigDecimal primaryPriceItemPrice = new BigDecimal(testQuote.getQuoteFunding().getFundsRemaining())
                .divide(BigDecimal.valueOf(
                        testOrder.getSamples().size() +
                        secondOrder.getSamples().size() * .5) // Price of the second product will be 1/2 the primary price, so the samples will count as 1/2 a sample
                        .add(secondOrder.getLaneCount() .multiply( BigDecimal.valueOf(.25))) // Price of the addon product will be 1/4 the primary price, so lane count will count as 1/4 of a lane
                        .add(BigDecimal.valueOf(nonSAPOrder.getSamples().size())) // non sap order will use the primary product for its product
                        .add(nonSAPOrder.getLaneCount() .multiply(BigDecimal.valueOf(.25))),2, RoundingMode.FLOOR
                );

        BigDecimal secondaryPrice = primaryPriceItemPrice.multiply(BigDecimal.valueOf(.5)); // Define the price of the second product/price item to be half the price of the primary product/price item
        BigDecimal addonPrice = primaryPriceItemPrice.multiply(BigDecimal.valueOf(.25)); // Define the price of the addon product/price item to be 1/4 the price of the primary product/price item

        final String primaryStringPrice = String.valueOf(primaryPriceItemPrice);
        final String secondaryStringPrice = String.valueOf(secondaryPrice);
        final String addonStringPrice = String.valueOf(addonPrice);
        addPriceItemForProduct(testQuote.getAlphanumericId(), priceList, quoteItems, primaryProduct,
                primaryStringPrice, "1000", primaryStringPrice);
        addPriceItemForProduct(testQuote.getAlphanumericId(), priceList, quoteItems, secondaryProduct,
                secondaryStringPrice, "1000", secondaryStringPrice);
        addPriceItemForProduct(testQuote.getAlphanumericId(), priceList, quoteItems, addOnProduct,
                addonStringPrice, "1000", addonStringPrice);

        if(productExistInSap) {
            addSapMaterial(materials, primaryProduct, primaryStringPrice,
                    SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
            addSapMaterial(materials, secondaryProduct, secondaryStringPrice,
                    SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
            addSapMaterial(materials, addOnProduct, addonStringPrice,
                    SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        }

        BigDecimal calculatedMainOrderValue = BigDecimal.valueOf(testOrder.getSamples().size()) .multiply( primaryPriceItemPrice);
        BigDecimal calculatedSecondOrderValue =
                BigDecimal.valueOf(secondOrder.getSamples().size()).multiply(secondaryPrice)
                        .add(secondOrder.getLaneCount() .multiply(addonPrice));
        BigDecimal calculatedNonSapOrderValue =
                BigDecimal.valueOf(nonSAPOrder.getSamples().size()).multiply(primaryPriceItemPrice)
                        .add(nonSAPOrder.getLaneCount().multiply(addonPrice));
        OrderCalculatedValues testCalculatedValues =
                new OrderCalculatedValues(testOrder.hasSapQuote()?calculatedMainOrderValue:null,
                        Collections.emptySet(), actionBean.getEditOrder().getSapOrderNumber(),
                        mockSAPService.findSapQuote(quoteId).getQuoteHeader().fundsRemaining());

        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(materials);
            stubProductPriceCache.refreshCache();
            Mockito.when(mockQuoteService.getAllPriceItems()).thenThrow(new QuoteServerException("Quote server should not be called for SAP Quote"));
            Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuote.getAlphanumericId())).thenThrow(new QuoteServerException("Quote server should not be called for SAP Quote"));

            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class))).thenReturn(testCalculatedValues);
        } else {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenThrow(new SAPIntegrationException("Sap should not be called for Quote Server Quote"));
            stubProductPriceCache.refreshCache();
            Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
            Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuote.getAlphanumericId())).thenReturn(testQuote);

            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class)))
                    .thenThrow(new SAPIntegrationException("Sap should not be called for Quote Server Quote"));
        }

        actionBean.setEditOrder(testOrder);

        // Estimate the Order now
        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                calculatedMainOrderValue);
        actionBean.validateQuoteDetails(testQuote, 0);

        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty(),
                "Errors occurred validating Quote details.  Funds remaining is "
                +testQuote.getQuoteFunding().getFundsRemaining()+" and price is "+primaryStringPrice);


        //
        // Now the order gets placed in mercury and gets submitted to SAP
        //
        testOrder.setJiraTicketKey("PDO-1294");

        SapOrderDetail sapReference = new SapOrderDetail("test001", sampleTestSize, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        testOrder.addSapOrderDetail(sapReference);

        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn(Collections.singletonList(testOrder));

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                calculatedMainOrderValue);

        actionBean.validateQuoteDetails(testQuote, 0);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty(),
                "Errors occurred validating Quote details.  Funds remaining is "
                +testQuote.getQuoteFunding().getFundsRemaining()+" and price is "+primaryStringPrice);


        //Now the second order is palced and we validate the test order again
        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn(Arrays.asList(testOrder, secondOrder));

        testCalculatedValues =
                new OrderCalculatedValues(testOrder.hasSapQuote()?calculatedMainOrderValue:null,
                        Collections.singleton(new OrderValue(secondOrder.getSapOrderNumber(),
                                calculatedSecondOrderValue)),
                        actionBean.getEditOrder().getSapOrderNumber(),
                        mockSAPService.findSapQuote(quoteId).getQuoteHeader().fundsRemaining());

        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class))).thenReturn(testCalculatedValues);
        } else {
            Mockito
                .when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class), Mockito.any(OrderCriteria.class)))
                .thenThrow(new SAPIntegrationException("Sap should not be called for Quote Server Quote"));
        }

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                (calculatedMainOrderValue.add(calculatedSecondOrderValue)));

        actionBean.validateQuoteDetails(testQuote, 0);

        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty(),
                "Errors occurred validating Quote details.  Funds remaining is "
                +testQuote.getQuoteFunding().getFundsRemaining()+" and price is "+primaryStringPrice);

        // Now set a custom price adjustment for the second order
        final ProductOrderPriceAdjustment customPriceAdjustment =
                new ProductOrderPriceAdjustment(secondaryPrice.subtract(BigDecimal.valueOf(.75d)), null, null);
        secondOrder.setCustomPriceAdjustment(customPriceAdjustment);

        calculatedSecondOrderValue =
                (BigDecimal.valueOf(secondOrder.getSamples().size())
                        .multiply(secondaryPrice.subtract(BigDecimal.valueOf(.75d))))
                        .add(secondOrder.getLaneCount().multiply(addonPrice));

        testCalculatedValues = new OrderCalculatedValues(testOrder.hasSapQuote()?calculatedMainOrderValue:null,
                Collections.singleton(new OrderValue(secondOrder.getSapOrderNumber(),
                        calculatedSecondOrderValue)),
                actionBean.getEditOrder().getSapOrderNumber(),
                mockSAPService.findSapQuote(quoteId).getQuoteHeader().fundsRemaining());

        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class))).thenReturn(testCalculatedValues);
        } else {
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class)))
                    .thenThrow(new SAPIntegrationException("sap should not be called for quote server quote"));
        }

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                (calculatedMainOrderValue.add(calculatedSecondOrderValue)));

        actionBean.validateQuoteDetails(testQuote, 0);

        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty(),
                "Errors occurred validating Quote details.  Funds remaining is "
                +testQuote.getQuoteFunding().getFundsRemaining()+" and price is "+primaryStringPrice);


        //Now set a custom price adjustment for the primary order
        final ProductOrderPriceAdjustment primaryCustomPriceAdjustment =
                new ProductOrderPriceAdjustment(primaryPriceItemPrice.subtract(BigDecimal.valueOf(.25d)), null, null);
        testOrder.setCustomPriceAdjustment(primaryCustomPriceAdjustment);
        calculatedMainOrderValue = BigDecimal.valueOf(testOrder.getSamples().size())
                .multiply(primaryPriceItemPrice.subtract(BigDecimal.valueOf(.25d)));

        testCalculatedValues = new OrderCalculatedValues(testOrder.hasSapQuote()?calculatedMainOrderValue:null,
                Collections.singleton(new OrderValue(secondOrder.getSapOrderNumber(),calculatedSecondOrderValue)),
                actionBean.getEditOrder().getSapOrderNumber(),
                mockSAPService.findSapQuote(quoteId).getQuoteHeader().fundsRemaining());


        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class))).thenReturn(testCalculatedValues);
        } else {
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class)))
                    .thenThrow(new SAPIntegrationException("sap should not be called for quote server quote"));
        }

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                (calculatedMainOrderValue.add(calculatedSecondOrderValue)));

        actionBean.validateQuoteDetails(testQuote, 0);

        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty(),
                "Errors occurred validating Quote details.  Funds remaining is "
                +testQuote.getQuoteFunding().getFundsRemaining()+" and price is "+primaryStringPrice);

        // Now make the Quote split funding so that attempting to go to SAP will fail
        final FundingLevel fundingLevel = new FundingLevel();
        final FundingLevel fundingLevel2 = new FundingLevel();
        Funding funding = new Funding(Funding.FUNDS_RESERVATION, "test", "c333");
        Funding funding2 = new Funding(Funding.FUNDS_RESERVATION, "test", "c333");
        fundingLevel.setFunding(Collections.singleton(funding));
        fundingLevel2.setFunding(Collections.singleton(funding2));
        Collection<FundingLevel> fundingLevelCollection = Arrays.asList(fundingLevel, fundingLevel2);
        QuoteFunding quoteFunding = new QuoteFunding(testQuote.getQuoteFunding().getFundsRemaining(),fundingLevelCollection);

        testQuote.setQuoteFunding(quoteFunding);
        testCalculatedValues =
                new OrderCalculatedValues(null,
                        Collections.singleton(new OrderValue(secondOrder.getSapOrderNumber(),
                                calculatedSecondOrderValue)),
                        actionBean.getEditOrder().getSapOrderNumber(),
                        mockSAPService.findSapQuote(quoteId).getQuoteHeader().fundsRemaining());

        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuote.getAlphanumericId()))
                .thenThrow(new QuoteServerException("Quote server should not be thrown for Quote Server Quote"));
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class))).thenReturn(testCalculatedValues);
        } else {
            Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuote.getAlphanumericId()))
                .thenThrow(new QuoteServerException("Quote server should not be thrown for Quote Server Quote"));
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class)))
                .thenThrow(new SAPIntegrationException("sap should not be called for quote server quote"));
        }


        // Now SAP should be down and we have placed a new order, but it doesn't go to SAP
        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn(Arrays.asList(testOrder, secondOrder, nonSAPOrder));

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                (calculatedMainOrderValue.add(calculatedSecondOrderValue).add(calculatedNonSapOrderValue)));

        actionBean.validateQuoteDetails(testQuote, 0);

        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty(),
                "Errors occurred validating Quote details.  Funds remaining is "
                +testQuote.getQuoteFunding().getFundsRemaining()+" and price is "+primaryStringPrice);


        // Now, add another sample to the primary order
        testOrder.addSample(new ProductOrderSample("SM-test" +(testOrder.getSamples().size()+1)));
        calculatedMainOrderValue = BigDecimal.valueOf(testOrder.getSamples().size()).multiply(primaryPriceItemPrice.subtract(BigDecimal.valueOf(.25d)));
        testCalculatedValues =
                new OrderCalculatedValues(null,
                        Collections.singleton(new OrderValue(secondOrder.getSapOrderNumber(),
                                calculatedSecondOrderValue)),
                        actionBean.getEditOrder().getSapOrderNumber(),
                        mockSAPService.findSapQuote(quoteId).getQuoteHeader().fundsRemaining());

        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class))).thenReturn(testCalculatedValues);
        } else {
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class)))
                .thenThrow(new SAPIntegrationException("sap should not be called for quote server quote"));
        }

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                (calculatedMainOrderValue.add(calculatedSecondOrderValue).add(calculatedNonSapOrderValue)));

        actionBean.validateQuoteDetails(testQuote, 0);

        Assert.assertFalse(actionBean.getContext().getValidationErrors().isEmpty(),
                "Errors occurred validating Quote details.  Funds remaining is "
                +testQuote.getQuoteFunding().getFundsRemaining()+" and price is "+primaryStringPrice);
    }

    @Test(dataProvider = "quoteSourceProvider")
    public void testEstimateSAPOrdersWithUpdateCurrentSapOrder(String testQuoteIdentifier, ProductOrder.QuoteSourceType quoteSource, TestUtils.SapQuoteTestScenario quoteItemsMatchOrderProducts) throws Exception {
        Quote testQuote = buildSingleTestQuote(testQuoteIdentifier, "800000");

        Product primaryProduct = new Product();
        primaryProduct.setPartNumber("P-Test_primary");
//        primaryProduct.setProductName("Primary Product");
        primaryProduct.setPrimaryPriceItem(new PriceItem("primary", "Genomics Platform", "Primary testing size",
                "Thousand dollar Genome price"));
        primaryProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.WHOLE_GENOME.getFamilyName()));

        Product addonNonSeqProduct = new Product();
        addonNonSeqProduct.setPartNumber("ADD-NON-SEQ");
//        addonNonSeqProduct.setProductName("Non-Seq Add On");
        addonNonSeqProduct.setPrimaryPriceItem(new PriceItem("Secondary", "Genomics Platform",
                "secondary testing size", "Extraction price"));
        addonNonSeqProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.ALTERNATE_LIBRARY_PREP_DEVELOPMENT.getFamilyName()));

        Product seqProduct = new Product();
        seqProduct.setPartNumber("ADD-SEQ");
//        seqProduct.setProductName("Seq Add on Product");
        seqProduct.setPrimaryPriceItem(new PriceItem("Third", "Genomics Platform", "Seq Testing Size",
                "Put it on the sequencer"));
        seqProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.SEQUENCE_ONLY.getFamilyName()));

        Product dummyProduct1 = new Product();
        dummyProduct1.setPartNumber("ADD-DMY-0001");
//        dummyProduct1.setProductName("Dummy Product");
        dummyProduct1.setPrimaryPriceItem(new PriceItem("fourth", "Genomics Platform", "Testing Size",
                "extra 1"));
        dummyProduct1.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.EXOME.getFamilyName()));

        testOrder = new ProductOrder();
        testOrder.setProduct(primaryProduct);
        testOrder.setQuoteId(testQuoteIdentifier);
        testOrder.updateAddOnProducts(Collections.singletonList(addonNonSeqProduct));

        List<ProductOrderSample> sampleList = new ArrayList<>();

        IntStream.range(0,75).forEach(sampleRangeIterator -> {
            sampleList.add(new ProductOrderSample("SM-Test"+sampleRangeIterator));
        });

        testOrder.setSamples(sampleList);
        testOrder.setLaneCount(BigDecimal.valueOf(5));

        ProductOrder extraOrder1 = new ProductOrder() ;
        extraOrder1.setJiraTicketKey("PDO-extra1");
        extraOrder1.setProduct(dummyProduct1);
        extraOrder1.setQuoteId(testQuoteIdentifier);

        IntStream.range(0,10).forEach(sampleRangeIterator -> {
            extraOrder1.addSample(new ProductOrderSample("extra1-Test"+sampleRangeIterator));
        });
        SapOrderDetail sapReferenceEx1 = new SapOrderDetail("Test_listed_1", 10, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        extraOrder1.addSapOrderDetail(sapReferenceEx1);

        ProductOrder extraOrder2 = new ProductOrder() ;
        extraOrder2.setJiraTicketKey("PDO-extra2");
        extraOrder2.setProduct(dummyProduct1);
        extraOrder2.setQuoteId(testQuoteIdentifier);

        IntStream.range(0,23).forEach(sampleRangeIterator -> {
            extraOrder2.addSample(new ProductOrderSample("extra1-Test"+sampleRangeIterator));
        });
        SapOrderDetail sapReferenceEx2 = new SapOrderDetail("Test_listed_2", 23, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        extraOrder2.addSapOrderDetail(sapReferenceEx2);

        ProductOrder extraOrder3 = new ProductOrder() ;
        extraOrder3.setJiraTicketKey("PDO-extra3");
        extraOrder3.setProduct(dummyProduct1);
        extraOrder3.setQuoteId(testQuoteIdentifier);

        IntStream.range(0,49).forEach(value -> {

            extraOrder3.addSample(new ProductOrderSample("extra1-Test"+value));
        });
        SapOrderDetail sapReferenceEx3 = new SapOrderDetail("Test_listed_2", 49, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        extraOrder3.addSapOrderDetail(sapReferenceEx3);

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();

        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, testOrder.getProduct(), "2000", "2000",
                "1000");
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, addonNonSeqProduct, "1573", "2000", "573");
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, seqProduct, "3500", "2000", "2500");

        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, dummyProduct1, "1", "100", "1");

        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        addSapMaterial(returnMaterials, testOrder.getProduct(), "2000",
                broad);
        addSapMaterial(returnMaterials, addonNonSeqProduct, "1573",
                broad);
        addSapMaterial(returnMaterials, seqProduct, "3500", broad);
        addSapMaterial(returnMaterials, dummyProduct1, "1", broad);


        final List<ProductOrder> allOrders = Arrays.asList(
                testOrder, extraOrder1, extraOrder2, extraOrder3);
        final List<ProductOrder> allOrdersMinusTestOrder = Arrays.asList(
                extraOrder1, extraOrder2, extraOrder3);

        final List<ProductOrder> mockPDOListAnswer = new ArrayList<>();
        mockPDOListAnswer.clear();
        mockPDOListAnswer.addAll(allOrdersMinusTestOrder);


        if (!ProductOrderActionBean.EXCLUDED_QUOTES_FROM_VALUE.contains(testQuoteIdentifier)) {
            Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenAnswer(
                    new Answer<List<ProductOrder>>() {
                        @Override
                        public List<ProductOrder> answer(InvocationOnMock invocationOnMock) throws Throwable {
                            return mockPDOListAnswer;
                        }
                    });
        } else {
            Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString()))
                    .thenThrow(new RuntimeException("Find orders with common quote should not be thrown at this time"));
        }

        final Set<OrderValue> sapOrderValues = new HashSet<>();
        sapOrderValues.add(new OrderValue("Test_listed_1",getCalculatedOrderValue(priceList, extraOrder1,
                quoteSource, testQuote)));
        sapOrderValues.add(new OrderValue("Test_listed_2", getCalculatedOrderValue(priceList, extraOrder2,
                quoteSource, testQuote)));
        sapOrderValues.add(new OrderValue("Test_listed_3", getCalculatedOrderValue(priceList, extraOrder3,
                quoteSource, testQuote)));


        final BigDecimal extraOrderValues;
        if(!ProductOrderActionBean.EXCLUDED_QUOTES_FROM_VALUE.contains(testQuoteIdentifier)) {
            extraOrderValues = getCalculatedOrderValue(priceList, extraOrder1, quoteSource, testQuote)
                    .add(getCalculatedOrderValue(priceList, extraOrder2, quoteSource, testQuote))
                    .add(getCalculatedOrderValue(priceList, extraOrder3, quoteSource, testQuote));
        } else {
            extraOrderValues = BigDecimal.ZERO;
        }

        BigDecimal overrideCalculatedOrderValue = getCalculatedOrderValue(priceList, testOrder, quoteSource,
                testQuote);


        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
            stubProductPriceCache.refreshCache();
            Mockito.when(mockQuoteService.getAllPriceItems())
                    .thenThrow(new QuoteServerException("Quote server should not be called for SAP Quote"));
            Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuoteIdentifier))
                    .thenThrow(new QuoteServerException("Quote server should not be called for SAP Quote"));

            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                    .thenAnswer(new Answer<SapQuote>() {
                        @Override
                        public SapQuote answer(InvocationOnMock invocationOnMock) throws Throwable {

                            final String quoteId = (String)invocationOnMock.getArguments()[0];
                            final SapQuote quoteObject =
                                    TestUtils.buildTestSapQuote(quoteId , extraOrderValues,
                                            BigDecimal.valueOf(800000), testOrder, quoteItemsMatchOrderProducts, "GP01");
                            System.out.println(String.format("Quote id of %s has: sales org of %s, Total quote amount "
                                                             + "of %s and open quote total of %s",
                                    quoteId, quoteObject.getQuoteHeader().getSalesOrganization(),
                                    quoteObject.getQuoteHeader().getQuoteTotal(),
                                    quoteObject.getQuoteHeader().fundsRemaining()));

                            return quoteObject;
                        }
                    });
            String sapOrderNumber = null;
            Optional<ProductOrder> editOrder = Optional.ofNullable(actionBean.getEditOrder());
            if(editOrder.isPresent()) {
                sapOrderNumber = editOrder.get().getSapOrderNumber();
            }
            OrderCalculatedValues calculatedOrderReturnValue =
                    new OrderCalculatedValues(BigDecimal.ZERO, Collections.emptySet(),
                            sapOrderNumber,
                            mockSAPService.findSapQuote(testQuoteIdentifier).getQuoteHeader().fundsRemaining());

            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);
            System.out.println(String.format("Calculated order values: Open orders of %s, delivery count of %s,"
                                             + " and potential order value of %s",
                    calculatedOrderReturnValue.calculateTotalOpenOrderValue(),
                    calculatedOrderReturnValue.openDeliveryValues(),
                    calculatedOrderReturnValue.getPotentialOrderValue()));
            stubProductPriceCache.refreshCache();
        } else {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString()))
                    .thenThrow(new SAPIntegrationException("SAP should not be called for Quote Server quote"));
            Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
            Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuoteIdentifier)).thenReturn(testQuote);

            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                Mockito.any(OrderCriteria.class)))
                .thenThrow(new SAPIntegrationException("SAP should not be called for Quote Server quote"));

            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                    .thenThrow(new SAPIntegrationException("SAP should not be called for Quote Server quote"));
        }

        actionBean.setEditOrder(testOrder);

        OrderCalculatedValues calculatedOrderReturnValue;
        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                    overrideCalculatedOrderValue.add(extraOrderValues));
            testQuote.setQuoteItems(quoteItems);
        } else {
            final SapQuote sapQuote = mockSAPService.findSapQuote(testOrder.getQuoteId());
            calculatedOrderReturnValue =
                    new OrderCalculatedValues(
                            getCalculatedOrderValue(priceList, testOrder,
                                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote), sapOrderValues,
                            actionBean.getEditOrder().getSapOrderNumber(), sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);
            System.out.println(String.format("Calculated order values: Open orders of %s, delivery count of %s,"
                                             + " and potential order value of %s",
                    calculatedOrderReturnValue.calculateTotalOpenOrderValue(),
                    calculatedOrderReturnValue.openDeliveryValues(),
                    calculatedOrderReturnValue.getPotentialOrderValue()));

            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(sapQuote,0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0,null),
                    extraOrderValues);
        } else {
            final SapQuote sapQuote = mockSAPService.findSapQuote(testOrder.getQuoteId());

            calculatedOrderReturnValue = new OrderCalculatedValues(BigDecimal.ZERO, sapOrderValues,
                    actionBean.getEditOrder().getSapOrderNumber(), sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);
            System.out.println(String.format("Calculated order values: Open orders of %s, delivery count of %s,"
                                             + " and potential order value of %s",
                    calculatedOrderReturnValue.calculateTotalOpenOrderValue(),
                    calculatedOrderReturnValue.openDeliveryValues(),
                    calculatedOrderReturnValue.getPotentialOrderValue()));

            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(sapQuote,0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        // Now mimic saving order to SAP
        mockPDOListAnswer.clear();
        mockPDOListAnswer.addAll(allOrders);

        testOrder.setJiraTicketKey("PDO-TESTPDOValue");
        SapOrderDetail sapReference = new SapOrderDetail("test001", 75, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode());
        testOrder.addSapOrderDetail(sapReference);


        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                    getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote).add(extraOrderValues));
        } else {
            final SapQuote sapQuote = mockSAPService.findSapQuote(testOrder.getQuoteId());

            Set<OrderValue> allValues = new HashSet<>(sapOrderValues);
            allValues.add(new OrderValue("test001", getCalculatedOrderValue(priceList, testOrder,
                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote)));
            calculatedOrderReturnValue =
                    new OrderCalculatedValues(
                            getCalculatedOrderValue(priceList, testOrder,
                                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote),
                            allValues, actionBean.getEditOrder().getSapOrderNumber(), sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);
            System.out.println(String.format("Calculated order values: Open orders of %s, delivery count of %s,"
                                             + " and potential order value of %s",
                    calculatedOrderReturnValue.calculateTotalOpenOrderValue(),
                    calculatedOrderReturnValue.openDeliveryValues(),
                    calculatedOrderReturnValue.getPotentialOrderValue()));

            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(sapQuote,0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        testOrder.updateAddOnProducts(Arrays.asList(addonNonSeqProduct, seqProduct));

        if(quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                    getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote).add(extraOrderValues));
        } else {
            final SapQuote sapQuote = mockSAPService.findSapQuote(testOrder.getQuoteId());

            Set<OrderValue> allValues = new HashSet<>(sapOrderValues);
            allValues.add(new OrderValue("test001", getCalculatedOrderValue(priceList, testOrder,
                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote)));
            calculatedOrderReturnValue =
                    new OrderCalculatedValues(
                            getCalculatedOrderValue(priceList, testOrder,
                                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote),
                            allValues, actionBean.getEditOrder().getSapOrderNumber(), sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);
            System.out.println(String.format("Calculated order values: Open orders of %s, delivery count of %s,"
                                             + " and potential order value of %s",
                    calculatedOrderReturnValue.calculateTotalOpenOrderValue(),
                    calculatedOrderReturnValue.openDeliveryValues(),
                    calculatedOrderReturnValue.getPotentialOrderValue()));

            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(sapQuote,0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        testOrder.setProduct(seqProduct);

        if(quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                    getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote).add(extraOrderValues));
        } else {
            final SapQuote sapQuote = mockSAPService.findSapQuote(testOrder.getQuoteId());

            Set<OrderValue> allValues = new HashSet<>(sapOrderValues);
            allValues.add(new OrderValue("test001", getCalculatedOrderValue(priceList, testOrder,
                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote)));
            calculatedOrderReturnValue =
                    new OrderCalculatedValues(getCalculatedOrderValue(priceList, testOrder,
                            ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote),
                            allValues, actionBean.getEditOrder().getSapOrderNumber(), sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);
            System.out.println(String.format("Calculated order values: Open orders of %s, delivery count of %s,"
                                             + " and potential order value of %s",
                    calculatedOrderReturnValue.calculateTotalOpenOrderValue(),
                    calculatedOrderReturnValue.openDeliveryValues(),
                    calculatedOrderReturnValue.getPotentialOrderValue()));

            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(sapQuote,0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 75);
        ProductOrderSample abandonedSample = testOrder.getSamples().get(0);
        abandonedSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);
        if(quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                    getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote).add(extraOrderValues));
        } else {
            final SapQuote sapQuote = mockSAPService.findSapQuote(testOrder.getQuoteId());
            Set<OrderValue> allValues = new HashSet<>(sapOrderValues);
            allValues.add(new OrderValue("test001", getCalculatedOrderValue(priceList, testOrder,
                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote)));
            calculatedOrderReturnValue =
                    new OrderCalculatedValues(
                            getCalculatedOrderValue(priceList, testOrder,
                                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote),
                            allValues, actionBean.getEditOrder().getSapOrderNumber(), sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);
            System.out.println(String.format("Calculated order values: Open orders of %s, delivery count of %s,"
                                             + " and potential order value of %s",
                    calculatedOrderReturnValue.calculateTotalOpenOrderValue(),
                    calculatedOrderReturnValue.openDeliveryValues(),
                    calculatedOrderReturnValue.getPotentialOrderValue()));

            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(sapQuote,0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);

        if(quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                    getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote).add(extraOrderValues));
        } else {
            final SapQuote sapQuote = mockSAPService.findSapQuote(testOrder.getQuoteId());

            Set<OrderValue> allValues = new HashSet<>(sapOrderValues);
            allValues.add(new OrderValue("test001", getCalculatedOrderValue(priceList, testOrder,
                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote)));
            calculatedOrderReturnValue =
                    new OrderCalculatedValues(
                            getCalculatedOrderValue(priceList, testOrder,
                                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote),
                            allValues, actionBean.getEditOrder().getSapOrderNumber(), sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);
            System.out.println(String.format("Calculated order values: Open orders of %s, delivery count of %s,"
                                             + " and potential order value of %s",
                    calculatedOrderReturnValue.calculateTotalOpenOrderValue(),
                    calculatedOrderReturnValue.openDeliveryValues(),
                    calculatedOrderReturnValue.getPotentialOrderValue()));

            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(sapQuote,0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        if(quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder),
                    getCalculatedOrderValue(priceList, testOrder, quoteSource, testQuote).add(extraOrderValues));
        } else {
            final SapQuote sapQuote = mockSAPService.findSapQuote(testOrder.getQuoteId());

            Set<OrderValue> allValues = new HashSet<>(sapOrderValues);
            allValues.add(new OrderValue("test001", getCalculatedOrderValue(priceList, testOrder,
                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote)));
            calculatedOrderReturnValue =
                    new OrderCalculatedValues(
                            getCalculatedOrderValue(priceList, testOrder,
                                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote),
                            allValues, actionBean.getEditOrder().getSapOrderNumber(), sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);
            System.out.println(String.format("Calculated order values: Open orders of %s, delivery count of %s,"
                                             + " and potential order value of %s",
                    calculatedOrderReturnValue.calculateTotalOpenOrderValue(),
                    calculatedOrderReturnValue.openDeliveryValues(),
                    calculatedOrderReturnValue.getPotentialOrderValue()));

            actionBean.clearValidationErrors();
            actionBean.validateSapQuoteDetails(sapQuote,0);
            if(quoteItemsMatchOrderProducts == TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER) {
                Assert.assertFalse(actionBean.getValidationErrors().isEmpty());
            } else {
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty());
            }
            actionBean.clearValidationErrors();
        }

        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());

        if (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
            actionBean.validateQuoteDetails(testQuote, 0);
        } else {
            final SapQuote sapQuote =
                    TestUtils.buildTestSapQuote(testQuoteIdentifier, extraOrderValues, BigDecimal.valueOf(800000),
                            testOrder, TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED, "GP01");
            Set<OrderValue> allValues = new HashSet<>(sapOrderValues);
            allValues.add(new OrderValue("test001", getCalculatedOrderValue(priceList, testOrder,
                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote)));
            calculatedOrderReturnValue =
                    new OrderCalculatedValues(
                            getCalculatedOrderValue(priceList, testOrder,
                                    ProductOrder.QuoteSourceType.SAP_SOURCE, testQuote),
                            allValues, actionBean.getEditOrder().getSapOrderNumber(), sapQuote.getQuoteHeader().fundsRemaining());
            Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class),
                    Mockito.any(OrderCriteria.class))).thenReturn(calculatedOrderReturnValue);
            System.out.println(String.format("Calculated order values: Open orders of %s, delivery count of %s,"
                                             + " and potential order value of %s",
                    calculatedOrderReturnValue.calculateTotalOpenOrderValue(),
                    calculatedOrderReturnValue.openDeliveryValues(),
                    calculatedOrderReturnValue.getPotentialOrderValue()));

            actionBean.validateSapQuoteDetails(sapQuote, 0);
        }

        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
    }

    @NotNull
    private OrderCalculatedValues getTestCalculatedValues(PriceList priceList, Set<OrderValue> sapOrderValues,
                                                          ProductOrder.QuoteSourceType quoteSource, Quote testQuote,
                                                          ProductOrder productOrder, BigDecimal fundsRemaining) {
        if(productOrder.isSavedInSAP()) {
            sapOrderValues.add(new OrderValue(productOrder.getSapOrderNumber(),
                    getCalculatedOrderValue(priceList, productOrder, quoteSource,
                    testQuote)));
        }
        return new OrderCalculatedValues(
            getCalculatedOrderValue(priceList, productOrder, quoteSource, testQuote), sapOrderValues,
                productOrder.getSapOrderNumber(), fundsRemaining);
    }

    private BigDecimal getCalculatedOrderValue(PriceList priceList, ProductOrder testOrder,
                                              ProductOrder.QuoteSourceType quoteSource, Quote testQuote) {
        BigDecimal overrideCalculatedOrderValue = BigDecimal.ZERO;
        BigDecimal totalNonAbandonedCount;

        if(testOrder != null) {
            if(testOrder.getProduct().getSupportsNumberOfLanes()) {
                totalNonAbandonedCount = testOrder.getLaneCount();
            } else {
                totalNonAbandonedCount = testOrder.getTotalNonAbandonedCount(ProductOrder.CountAggregation.SHARE_SAP_ORDER_AND_BILL_READY);
            }

            String primaryPrice = priceList.findByKeyFields(testOrder.getProduct().getPrimaryPriceItem())
                    .getPrice();

            if(quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
                final QuoteItem cachedQuoteItem =
                        testQuote.findCachedQuoteItem(testOrder.getProduct().getPrimaryPriceItem().getPlatform(),
                                testOrder.getProduct().getPrimaryPriceItem().getCategory(),
                                testOrder.getProduct().getPrimaryPriceItem().getName());
                if(cachedQuoteItem!= null) {
                    primaryPrice = cachedQuoteItem.getPrice();
                }
            }

            overrideCalculatedOrderValue = overrideCalculatedOrderValue.add(totalNonAbandonedCount)
                    .multiply(new BigDecimal(primaryPrice));

            for (ProductOrderAddOn addOn : testOrder.getAddOns()) {
                if(addOn.getAddOn().getSupportsNumberOfLanes()) {
                    totalNonAbandonedCount = testOrder.getLaneCount();
                } else {
                    totalNonAbandonedCount = testOrder.getTotalNonAbandonedCount(ProductOrder.CountAggregation.SHARE_SAP_ORDER_AND_BILL_READY);
                }

                String addOnPrice = priceList.findByKeyFields(addOn.getAddOn().getPrimaryPriceItem()).getPrice();

                if(quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER) {
                    final QuoteItem cachedQuoteItem =
                            testQuote.findCachedQuoteItem(addOn.getAddOn().getPrimaryPriceItem().getPlatform(),
                                    addOn.getAddOn().getPrimaryPriceItem().getCategory(),
                                    addOn.getAddOn().getPrimaryPriceItem().getName());
                    if(cachedQuoteItem!= null) {
                        addOnPrice = cachedQuoteItem.getPrice();
                    }
                }

                overrideCalculatedOrderValue =
                        overrideCalculatedOrderValue.add(totalNonAbandonedCount.multiply(new BigDecimal(addOnPrice)));
            }
        }
        return overrideCalculatedOrderValue;
    }

    @Test(dataProvider = "quoteSourceProvider")
    public void testEstimateCustomHigherThanQuote(String testQuoteIdentifier, ProductOrder.QuoteSourceType quoteSource,
                                                  TestUtils.SapQuoteTestScenario quoteItemsMatchOrderProducts) throws Exception {

        Quote testQuote = buildSingleTestQuote(testQuoteIdentifier, "12000");

        Product primaryProduct = new Product();
        primaryProduct.setPartNumber("P-Test_primary");
        primaryProduct.setProductName("Test primary product");
        primaryProduct.setPrimaryPriceItem(new PriceItem("primary", "Genomics Platform", "Primary testing size",
                "Thousand dollar Genome price"));
        primaryProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.WHOLE_GENOME.getFamilyName()));


        testOrder = new ProductOrder();
        testOrder.setJiraTicketKey("PDO-TESTPDOValue");
        testOrder.setProduct(primaryProduct);
        testOrder.setQuoteId(testQuoteIdentifier);
        List<ProductOrderSample> sampleList = new ArrayList<>();

        actionBean.setEditOrder(testOrder);

        for (int i = 0; i < 75;i++) {
            sampleList.add(new ProductOrderSample("SM-Test"+i));
        }

        testOrder.setSamples(sampleList);

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();

        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, testOrder.getProduct(), "2000", "2000",
                "2000");

        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
            stubProductPriceCache.refreshCache();
            Mockito.when(mockQuoteService.getAllPriceItems())
                    .thenThrow(new QuoteServerException("Quote server should not be called for SAP Quote"));
            Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuoteIdentifier))
                    .thenThrow(new QuoteServerException("Quote server should not be called for SAP Quote"));

            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenAnswer(new Answer<SapQuote>() {
                @Override
                public SapQuote answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return TestUtils.buildTestSapQuote(testOrder.getQuoteId(), BigDecimal.valueOf(100000),
                            BigDecimal.valueOf(1000000), testOrder,quoteItemsMatchOrderProducts, "GP01");
                }
            });
        } else {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString()))
                    .thenThrow(new SAPIntegrationException("Sap should not be called for Quote Server Quote"));
            Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
            Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuoteIdentifier)).thenReturn(testQuote);

            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                    .thenThrow(new SAPIntegrationException("Sap should not be called for Quote Server Quote"));
        }
        if (!ProductOrderActionBean.EXCLUDED_QUOTES_FROM_VALUE.contains(testQuoteIdentifier)) {
            Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn(Collections.singletonList(
                    testOrder));
        } else {
            Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString()))
                    .thenThrow(new RuntimeException("Find orders with common quote should not be called at this point"));
        }

        testOrder.addCustomPriceAdjustment(new ProductOrderPriceAdjustment(new BigDecimal(160.00),null, null));

        if (quoteSource == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            actionBean.validateSapQuoteDetails(mockSAPService.findSapQuote(testOrder.getQuoteId()), 0);
        } else {
            actionBean.validateQuoteDetails(testQuote, 0);
        }

        Assert.assertEquals(actionBean.getContext().getValidationErrors().isEmpty(),
                (quoteSource == ProductOrder.QuoteSourceType.QUOTE_SERVER ||
                 quoteItemsMatchOrderProducts != TestUtils.SapQuoteTestScenario.PRODUCTS_DIFFER));
    }

    private void addSapMaterial(Set<SAPMaterial> returnMaterials, Product product, String basePrice,
                               SapIntegrationClientImpl.SAPCompanyConfiguration broad){
        final SAPMaterial material =
                new SAPMaterial(product.getPartNumber(),broad, broad.getDefaultWbs(),"description",
                        basePrice,SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE,
                        new Date(), new Date(),Collections.<Condition, BigDecimal>emptyMap(),
                        Collections.<DeliveryCondition, BigDecimal>emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                        broad.getSalesOrganization());
        returnMaterials.add(material);
    }

    private void addPriceItemForProduct(String testQuoteIdentifier, PriceList priceList,
                                        Collection<QuoteItem> quoteItems, Product primaryOrderProduct,
                                        String priceItemPrice, String quoteItemQuantity, String quoteItemPrice) {
        priceList.add(new QuotePriceItem(primaryOrderProduct.getPrimaryPriceItem().getCategory(),
                primaryOrderProduct.getPrimaryPriceItem().getName(),
                primaryOrderProduct.getPrimaryPriceItem().getName(), priceItemPrice, "test",
                primaryOrderProduct.getPrimaryPriceItem().getPlatform()));

        quoteItems.add(new QuoteItem(testQuoteIdentifier, primaryOrderProduct.getPrimaryPriceItem().getName(),
                primaryOrderProduct.getPrimaryPriceItem().getName(), quoteItemQuantity, quoteItemPrice,"each",
                primaryOrderProduct.getPrimaryPriceItem().getPlatform(),
                primaryOrderProduct.getPrimaryPriceItem().getCategory()));
    }

    @NotNull
    private Quote buildSingleTestQuote(String testQuoteIdentifier, String fundsRemainingOverride) {
        String fundsRemaining = "100000";
        if(StringUtils.isNotBlank(fundsRemainingOverride) && !fundsRemainingOverride.equals("0")) {
            fundsRemaining = fundsRemainingOverride;
        }
        final FundingLevel fundingLevel = new FundingLevel();
        Funding funding = new Funding(Funding.FUNDS_RESERVATION, "test", "c333");
        fundingLevel.setFunding(Collections.singleton(funding));
        Collection<FundingLevel> fundingLevelCollection = Collections.singleton(fundingLevel);
        QuoteFunding quoteFunding = new QuoteFunding(fundsRemaining,fundingLevelCollection);
        Quote testQuote = new Quote(testQuoteIdentifier, quoteFunding, ApprovalStatus.FUNDED);
        testQuote.setExpired(Boolean.FALSE);
        return testQuote;
    }

    /**
     * Set up the edit PDO such that it has both regulatory information and a reason for skipping regulatory
     * information. This is how the web form will submit the data in the case where one was already saved and the user
     * is editing the PDO to change to the other (and didn't manually clear the form inputs).
     */
    private void setupForChangingRegInfoSkip() {
        // Set both regulatory info and a skip reason.
        pdo.getRegulatoryInfos().add(new RegulatoryInfo("test", RegulatoryInfo.Type.IRB, "test"));
        pdo.setSkipRegulatoryReason("testing");
        actionBean.setEditOrder(pdo);
    }

    public static class RegulatoryInfoStub extends RegulatoryInfo {

        private Long mockId;

        public RegulatoryInfoStub(String name, Type type, String identifier) {
            super(name, type, identifier);
        }

        public RegulatoryInfoStub() {
            super();
        }

        public void setId(Long id) {
            this.mockId = id;
        }

        @Override
        public Long getRegulatoryInfoId() {
            return this.mockId;
        }
    }

    @DataProvider(name = "quoteDataProvider")
    public Iterator<Object[]> quoteDataProvider() {
        List<Object[]> testCases = new ArrayList<>();

        // Using quotes defined in quoteTestData.xml

        // Quote ID, is the product in SAP, quote source type

        testCases.add(new Object[]{"GP87U", true, ProductOrder.QuoteSourceType.SAP_SOURCE});   // common catch all quote used by PDMs
        testCases.add(new Object[]{"GP87U", true, ProductOrder.QuoteSourceType.QUOTE_SERVER});  // Split funded
        testCases.add(new Object[]{"GP87U", false, ProductOrder.QuoteSourceType.SAP_SOURCE});
        testCases.add(new Object[]{"GP87U", false, ProductOrder.QuoteSourceType.QUOTE_SERVER});

        testCases.add(new Object[]{"STCIL1", true, ProductOrder.QuoteSourceType.SAP_SOURCE});  // Single funded quote in which the funding source
        testCases.add(new Object[]{"STCIL1", true, ProductOrder.QuoteSourceType.QUOTE_SERVER}); // is split among 2 cost objects
        testCases.add(new Object[]{"STCIL1", false, ProductOrder.QuoteSourceType.SAP_SOURCE});
        testCases.add(new Object[]{"STCIL1", false, ProductOrder.QuoteSourceType.QUOTE_SERVER});

        testCases.add(new Object[]{"GAN1GX", true,  ProductOrder.QuoteSourceType.SAP_SOURCE});  // Split funded quote in which the funding sources are
        testCases.add(new Object[]{"GAN1GX", true,  ProductOrder.QuoteSourceType.QUOTE_SERVER}); // each fund reservations
        testCases.add(new Object[]{"GAN1GX", false, ProductOrder.QuoteSourceType.SAP_SOURCE});
        testCases.add(new Object[]{"GAN1GX", false, ProductOrder.QuoteSourceType.QUOTE_SERVER});

        testCases.add(new Object[]{"GAN1MB", true,  ProductOrder.QuoteSourceType.SAP_SOURCE});  // Split funded quote in which the funding sources are
        testCases.add(new Object[]{"GAN1MB", true,  ProductOrder.QuoteSourceType.QUOTE_SERVER}); // each purchase orders
        testCases.add(new Object[]{"GAN1MB", false, ProductOrder.QuoteSourceType.SAP_SOURCE});
        testCases.add(new Object[]{"GAN1MB", false, ProductOrder.QuoteSourceType.QUOTE_SERVER});

        testCases.add(new Object[]{"MPG1X6", true,  ProductOrder.QuoteSourceType.SAP_SOURCE});  // Single funded quote in which the funding source is
        testCases.add(new Object[]{"MPG1X6", true,  ProductOrder.QuoteSourceType.QUOTE_SERVER}); // a purchase order
        testCases.add(new Object[]{"MPG1X6", false, ProductOrder.QuoteSourceType.SAP_SOURCE});
        testCases.add(new Object[]{"MPG1X6", false, ProductOrder.QuoteSourceType.QUOTE_SERVER});

        testCases.add(new Object[]{"MPG20W", true,  ProductOrder.QuoteSourceType.SAP_SOURCE});  // Single funded quote in which the funding source
        testCases.add(new Object[]{"MPG20W", true,  ProductOrder.QuoteSourceType.QUOTE_SERVER}); // is a fund reservation.
        testCases.add(new Object[]{"MPG20W", false, ProductOrder.QuoteSourceType.SAP_SOURCE});
        testCases.add(new Object[]{"MPG20W", false, ProductOrder.QuoteSourceType.QUOTE_SERVER});

        testCases.add(new Object[]{"GAN1GX2", true,  ProductOrder.QuoteSourceType.SAP_SOURCE});  // Same setup as GAN1GX only the percentage of each
        testCases.add(new Object[]{"GAN1GX2", true,  ProductOrder.QuoteSourceType.QUOTE_SERVER});
        testCases.add(new Object[]{"GAN1GX2", false, ProductOrder.QuoteSourceType.SAP_SOURCE});
        testCases.add(new Object[]{"GAN1GX2", false, ProductOrder.QuoteSourceType.QUOTE_SERVER});

        testCases.add(new Object[]{"MPG183", true,  ProductOrder.QuoteSourceType.SAP_SOURCE});  // Single funded quote, Cost Object, LOT of money to use.
        testCases.add(new Object[]{"MPG183", true,  ProductOrder.QuoteSourceType.SAP_SOURCE});
        testCases.add(new Object[]{"MPG183", false, ProductOrder.QuoteSourceType.SAP_SOURCE});
        testCases.add(new Object[]{"MPG183", false, ProductOrder.QuoteSourceType.SAP_SOURCE});

        return testCases.iterator();
    }

    @DataProvider(name = "getSamplesForAjaxDataProvider")
    public Iterator<Object[]> getSamplesForAjaxDataProvider() {
        List<Object[]> testCases = new ArrayList<>();

        // The expected number of returned samples is 'pageLength' or 'All' samples whcih is represented as -1 internally.
        testCases.add(new Object[]{/*startRow*/ 0, /*pageLength*/ 1, /*totalSamples*/ 1, /*expectedReturned*/ 1});
        testCases.add(new Object[]{/*startRow*/ 0, /*pageLength*/ 5, /*totalSamples*/ 10, /*expectedReturned*/ 5});

        // But "fetch all" is represented as -1 so here the number of returned samples is 'totalSamples'.
        testCases.add(new Object[]{/*startRow*/ 0, /*pageLength*/ -1, /*totalSamples*/ 10, /*expectedReturned*/ 10});

        // even when the current page is not the first
        testCases.add(new Object[]{/*startRow*/ 2, /*pageLength*/ -1, /*totalSamples*/ 10, /*expectedReturned*/ 10});
        testCases.add(new Object[]{/*startRow*/ 1, /*pageLength*/ 1, /*totalSamples*/ 1, /*expectedReturned*/ 1});
        testCases.add(new Object[]{/*startRow*/ 5, /*pageLength*/ 5, /*totalSamples*/ 5, /*expectedReturned*/ 5});
        testCases.add(new Object[]{/*startRow*/ 1, /*pageLength*/ 5, /*totalSamples*/ 10, /*expectedReturned*/ 5});

        // Test that pagelength > total sample size returns the correct number.
        testCases.add(new Object[]{/*startRow*/ 0, /*pageLength*/ 5, /*totalSamples*/ 2, /*expectedReturned*/ 2});

        return testCases.iterator();
    }

    @Test(dataProvider = "getSamplesForAjaxDataProvider")
    public void testGetPageOneSamples(int startRow, int pageLength, int totalSamples, int expectedReturned)  {
        State tableState = buildTestState(startRow, pageLength);

        actionBean.setState(tableState);
        List<ProductOrderSample> fullSampleList = getInitializedSamples(totalSamples, "SM-");

        List<ProductOrderSample> sampleSubList = ProductOrderActionBean.getPageOneSamples(tableState, fullSampleList);
        assertThat(sampleSubList, hasSize(expectedReturned));

        // all items from subset are included in original list.
        for (int index = 0; index < expectedReturned; index++) {
            assertThat(fullSampleList.get(index), equalTo(sampleSubList.get(index)));
        }
    }

    private State buildTestState(int startRow, int pageLength) {
        Search search = new Search("foo", true, false, false);
        State tableState;
        tableState  = new State(new Date().getTime(), startRow, pageLength, Collections.<Map<Integer, State.Direction>>emptyList(), search,
                Collections.<Column>emptyList());
        return tableState;
    }

    private List<ProductOrderSample> getInitializedSamples(int totalSamples, final String prefix) {
        if (totalSamples == 0) {
            return null;
        }
        List<ProductOrderSample> samples = new ArrayList<>();
        for (int sampleNum = 0; sampleNum < totalSamples; sampleNum++) {
            String sampleName = prefix + sampleNum;
            MercurySample mercurySample = new MercurySample(sampleName, Collections.<Metadata>emptySet());
            ProductOrderSample productOrderSample = new ProductOrderSample(sampleName);
            productOrderSample.setMercurySample(mercurySample);
            productOrderSample.setSampleData(new MercurySampleData(mercurySample));
            samples.add(productOrderSample);
        }
        return samples;
    }


    public void testQuoteOptionsFundsReservation() throws Exception {
        String quoteId = "DNA4JD";
        testOrder = new ProductOrder();
        FundingLevel fundingLevel = new FundingLevel();
        Funding funding = new Funding(Funding.FUNDS_RESERVATION, "test", "c333");
        funding.setGrantNumber("1234");
        funding.setGrantStartDate(new Date());
        Date oneWeek = DateUtils.getOneWeek();
        funding.setGrantEndDate(oneWeek);
        funding.setFundsReservationNumber("CO-1234");
        funding.setGrantStatus("Active");
        fundingLevel.setFunding(Collections.singleton(funding));
        fundingLevel.setPercent("100");
        Collection<FundingLevel> fundingLevelCollection = Collections.singleton(fundingLevel);
        QuoteFunding quoteFunding = new QuoteFunding("100", fundingLevelCollection);
        Quote testQuote = buildSingleTestQuote(quoteId, "2");
        testQuote.setQuoteFunding(quoteFunding);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(quoteId)).thenReturn(testQuote);
        actionBean.setQuoteIdentifier(quoteId);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.postConstruct();

        QuoteDetailsHelper quoteDetailsHelper =
            new QuoteDetailsHelper(mockQuoteService, mockSAPService, templateEngine);
        QuoteDetailsHelper.QuoteDetail quoteDetails = quoteDetailsHelper.getQuoteDetails(quoteId, actionBean);

        QuoteDetailsHelper.FundingInfo fundingInfo = quoteDetails.getFundingDetails().iterator().next();
        String fundingInfoString = fundingInfo.getFundingInfoString();
        assertThat(quoteDetails.quoteIdentifier, equalTo(quoteId));
        assertThat(fundingInfoString, containsString("Active"));
        assertThat(fundingInfoString, containsString("Expires in 7 days. If it is likely this work will not be completed by then, please work on updating the Funding Source so billing errors can be avoided."));
        assertThat(fundingInfo.isQuoteWarning(), is(true));

    }

    public void testQuoteOptionsFundsReservationExpiresAfterLeapYear() throws Exception {
        String quoteId = "DNA4JD";
        testOrder = new ProductOrder();
        FundingLevel fundingLevel = new FundingLevel();
        Funding funding = new Funding(Funding.FUNDS_RESERVATION, "test", "c333");
        funding.setGrantNumber("1234");
        funding.setGrantStartDate(new Date());
        Date oneHeckOfALongTime =
            Date.from(LocalDate.now().plusYears(1000).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        funding.setGrantEndDate(oneHeckOfALongTime);
        funding.setFundsReservationNumber("CO-1234");
        funding.setGrantStatus("Active");
        fundingLevel.setFunding(Collections.singleton(funding));
        fundingLevel.setPercent("100");

        Collection<FundingLevel> fundingLevelCollection = Collections.singleton(fundingLevel);
        QuoteFunding quoteFunding = new QuoteFunding("100", fundingLevelCollection);
        Quote testQuote = buildSingleTestQuote(quoteId, "2");
        testQuote.setQuoteFunding(quoteFunding);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(quoteId)).thenReturn(testQuote);
        actionBean.setQuoteIdentifier(quoteId);
        actionBean.setQuoteService(mockQuoteService);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.postConstruct();

        QuoteDetailsHelper quoteDetailsHelper =
            new QuoteDetailsHelper(mockQuoteService, mockSAPService, templateEngine);
        QuoteDetailsHelper.QuoteDetail quoteDetails = quoteDetailsHelper.getQuoteDetails(quoteId, actionBean);

        QuoteDetailsHelper.FundingInfo fundingInfo = quoteDetails.getFundingDetails().iterator().next();
        String fundingInfoString = fundingInfo.getFundingInfoString();
        assertThat(quoteDetails.quoteIdentifier, equalTo(quoteId));
        assertThat(fundingInfoString, containsString("Active"));
        assertThat(fundingInfoString, containsString(DateUtils.getDate(oneHeckOfALongTime)));
        assertThat(fundingInfo.isQuoteWarning(), is(false));
    }

    public void testSapQuoteOptionsFundsReservation() throws Exception {
        String quoteId = "12345";
        testOrder = new ProductOrder();
        Date oneWeek = DateUtils.getOneWeek();

        Product primaryProduct = new Product();
        primaryProduct.setPartNumber("P-Test_primary");
        primaryProduct.setProductName("Test primary product");
        primaryProduct.setPrimaryPriceItem(new PriceItem("primary", "Genomics Platform", "Primary testing size",
                "Thousand dollar Genome price"));
        primaryProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.WHOLE_GENOME.getFamilyName()));


        testOrder.setProduct(primaryProduct);
        testOrder.addSample(new ProductOrderSample("SM-test1"));

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();

        addPriceItemForProduct(quoteId, priceList, quoteItems, testOrder.getProduct(), "98", "2000",
                "98");

        Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class), Mockito.any(OrderCriteria.class))).then(

                new Answer<OrderCalculatedValues>() {
                    @Override
                    public OrderCalculatedValues answer(InvocationOnMock invocationOnMock) throws Throwable {
                        BigDecimal fundsRemaining = ((SapQuote) invocationOnMock.getArguments()[0]).getQuoteHeader().fundsRemaining();
                        String potentialOrderId = null;

                        Optional<OrderCriteria> orderCriteria = Optional.ofNullable((OrderCriteria) invocationOnMock.getArguments()[1]);

                        if(orderCriteria.isPresent()) {
                            potentialOrderId = orderCriteria.get().getSapOrderID();
                        }

                        return new OrderCalculatedValues(BigDecimal.ZERO,
                                Collections.singleton(new OrderValue("test001",
                                        getCalculatedOrderValue(priceList, testOrder,
                                                ProductOrder.QuoteSourceType.SAP_SOURCE, null))),
                                potentialOrderId, fundsRemaining);
                    }
                });

        Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenAnswer(new Answer<SapQuote>() {
            @Override
            public SapQuote answer(InvocationOnMock invocationOnMock) throws Throwable {

                ZESDQUOTEHEADER sapQHeader = ZESDQUOTEHEADER.Factory.newInstance();
                sapQHeader.setPROJECTNAME("TestProject");
                sapQHeader.setQUOTENAME(quoteId);
                sapQHeader.setQUOTESTATUS(QuoteStatus.Z4.getStatusText());
                sapQHeader.setSALESORG("GP01");
                sapQHeader.setFUNDHEADERSTATUS(FundingStatus.SUBMITTED.name());
                sapQHeader.setQUOTESTATUS(QuoteStatus.Z4.name());
                sapQHeader.setCUSTOMER("");
                sapQHeader.setDISTCHANNEL("GE");
                sapQHeader.setFUNDTYPE(SapIntegrationClientImpl.FundingType.FUNDS_RESERVATION.name());
                sapQHeader.setQUOTESTATUSTXT("");
                sapQHeader.setQUOTETOTAL(BigDecimal.valueOf(198));
                sapQHeader.setSOTOTAL(BigDecimal.valueOf(98));
                sapQHeader.setQUOTEOPENVAL(BigDecimal.valueOf(100));

                QuoteHeader header = new QuoteHeader(sapQHeader);

                final Set<FundingDetail> fundingDetailsCollection = new HashSet<>();

                ZESDFUNDINGDET sapFundDetail = ZESDFUNDINGDET.Factory.newInstance();
                sapFundDetail.setFUNDTYPE(SapIntegrationClientImpl.FundingType.FUNDS_RESERVATION.name());
                sapFundDetail.setSPLITPER(BigDecimal.valueOf(100));
                sapFundDetail.setAPPSTATUS(FundingStatus.APPROVED.name());
                sapFundDetail.setAUTHAMOUNT(BigDecimal.valueOf(100));
                sapFundDetail.setITEMNO("1234");
                sapFundDetail.setCOSTOBJ("c333");
                sapFundDetail.setCOENDDATE(DateUtils.getDate(oneWeek));
                sapFundDetail.setFRDOCU("2341");

                fundingDetailsCollection.add(new FundingDetail(sapFundDetail));

                return new SapQuote(header, fundingDetailsCollection, Collections.emptySet(), Collections.emptySet());
            }
        });


        actionBean.setQuoteIdentifier(quoteId);
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.postConstruct();

        QuoteDetailsHelper quoteDetailsHelper =
            new QuoteDetailsHelper(mockQuoteService, mockSAPService, templateEngine);
        QuoteDetailsHelper.QuoteDetail quoteDetails = quoteDetailsHelper.getQuoteDetails(quoteId, actionBean);

        QuoteDetailsHelper.FundingInfo fundingInfo = quoteDetails.getFundingDetails().iterator().next();
        String fundingInfoString = fundingInfo.getFundingInfoString();
        assertThat(quoteDetails.quoteIdentifier, equalTo(quoteId));
        assertThat(fundingInfoString, containsString(FundingStatus.APPROVED.getStatusText()));
        assertThat(fundingInfoString,
            containsString(SapIntegrationClientImpl.FundingType.FUNDS_RESERVATION.getDisplayName()));
        assertThat(fundingInfoString, containsString("Expires in 7 days."));
        assertThat(fundingInfoString, containsString("funding split percentage = 100%"));
        assertThat(fundingInfo.isQuoteWarning(), is(true));
    }

    public void testQuoteOptionsPurchaseOrder() throws Exception {
        String quoteId = "DNA4JD";
        testOrder = new ProductOrder();
        FundingLevel fundingLevel = new FundingLevel();
        Funding funding = new Funding(Funding.PURCHASE_ORDER, "test", "c333");
        funding.setPurchaseOrderNumber("1234");
        funding.setGrantStartDate(new Date());
        Date oneWeek = DateUtils.getOneWeek();
        funding.setGrantEndDate(oneWeek);
        fundingLevel.setFunding(Collections.singleton(funding));
        fundingLevel.setPercent("100");

        Collection<FundingLevel> fundingLevelCollection = Collections.singleton(fundingLevel);
        QuoteFunding quoteFunding = new QuoteFunding("100", fundingLevelCollection);
        Quote testQuote = buildSingleTestQuote(quoteId, "2");
        testQuote.setQuoteFunding(quoteFunding);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(quoteId)).thenReturn(testQuote);
        actionBean.setQuoteIdentifier(quoteId);
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.postConstruct();

        QuoteDetailsHelper quoteDetailsHelper =
            new QuoteDetailsHelper(mockQuoteService, mockSAPService, templateEngine);
        QuoteDetailsHelper.QuoteDetail quoteDetails = quoteDetailsHelper.getQuoteDetails(quoteId, actionBean);
        QuoteDetailsHelper.FundingInfo fundingInfo = quoteDetails.getFundingDetails().iterator().next();

        assertThat(quoteDetails.getQuoteIdentifier(), equalTo(quoteId));
        assertThat(quoteDetails.getStatus(), is(ApprovalStatus.FUNDED.getValue()));
        assertThat(quoteDetails.getFundsRemaining(), containsString("Funds Remaining: $100.00"));
        String fundingInfoString = fundingInfo.getFundingInfoString();
        assertThat(fundingInfoString, not(containsString("funding split percentage")));
        assertThat(fundingInfo.isQuoteWarning(), is(false));
    }

    public void testSapQuoteOptionsPurchaseOrder() throws Exception {
        String quoteId = "12345";
        testOrder = new ProductOrder();
        Date oneWeek = DateUtils.getOneWeek();


        Product primaryProduct = new Product();
        primaryProduct.setPartNumber("P-Test_primary");
        primaryProduct.setProductName("Test primary product");
        primaryProduct.setPrimaryPriceItem(new PriceItem("primary", "Genomics Platform", "Primary testing size",
                "Thousand dollar Genome price"));
        primaryProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.WHOLE_GENOME.getFamilyName()));


        testOrder.setProduct(primaryProduct);
        testOrder.addSample(new ProductOrderSample("SM-test1"));

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();

        addPriceItemForProduct(quoteId, priceList, quoteItems, testOrder.getProduct(), "98", "2000",
                "98");

        Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class), Mockito.any(OrderCriteria.class))).then(

                new Answer<OrderCalculatedValues>() {
                    @Override
                    public OrderCalculatedValues answer(InvocationOnMock invocationOnMock) throws Throwable {

                        BigDecimal fundsRemaining = ((SapQuote) invocationOnMock.getArguments()[0]).getQuoteHeader().fundsRemaining();
                        String potentialOrderId = null;

                        Optional<OrderCriteria> orderCriteria = Optional.ofNullable((OrderCriteria) invocationOnMock.getArguments()[1]);

                        if(orderCriteria.isPresent()) {
                            potentialOrderId = orderCriteria.get().getSapOrderID();
                        }

                        return new OrderCalculatedValues(BigDecimal.ZERO,
                                Collections.singleton(new OrderValue("test001",
                                        getCalculatedOrderValue(priceList, testOrder,
                                                ProductOrder.QuoteSourceType.SAP_SOURCE, null))),
                                potentialOrderId, fundsRemaining);
                    }
                }
        );

        Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenAnswer(new Answer<SapQuote>() {
            @Override
            public SapQuote answer(InvocationOnMock invocationOnMock) throws Throwable {

                ZESDQUOTEHEADER sapQHeader = ZESDQUOTEHEADER.Factory.newInstance();
                sapQHeader.setPROJECTNAME("TestProject");
                sapQHeader.setQUOTENAME(quoteId);
                sapQHeader.setQUOTESTATUS(QuoteStatus.Z4.getStatusText());
                sapQHeader.setSALESORG("GP01");
                sapQHeader.setFUNDHEADERSTATUS(FundingStatus.APPROVED.name());
                sapQHeader.setQUOTESTATUS(QuoteStatus.Z4.name());
                sapQHeader.setCUSTOMER("");
                sapQHeader.setDISTCHANNEL("GE");
                sapQHeader.setFUNDTYPE(SapIntegrationClientImpl.FundingType.PURCHASE_ORDER.name());
                sapQHeader.setQUOTESTATUSTXT("");
                sapQHeader.setQUOTETOTAL(BigDecimal.valueOf(198));
                sapQHeader.setSOTOTAL(BigDecimal.valueOf(98));
                sapQHeader.setQUOTEOPENVAL(BigDecimal.valueOf(100));

                QuoteHeader header = new QuoteHeader(sapQHeader);

                final Set<FundingDetail> fundingDetailsCollection = new HashSet<>();

                ZESDFUNDINGDET sapFundDetail = ZESDFUNDINGDET.Factory.newInstance();
                sapFundDetail.setFUNDTYPE(SapIntegrationClientImpl.FundingType.PURCHASE_ORDER.name());
                sapFundDetail.setSPLITPER(BigDecimal.valueOf(100));
                sapFundDetail.setAPPSTATUS(FundingStatus.APPROVED.name());
                sapFundDetail.setAUTHAMOUNT(BigDecimal.valueOf(100));
                sapFundDetail.setPONUMBER("1234");
                sapFundDetail.setITEMNO("1234");

                fundingDetailsCollection.add(new FundingDetail(sapFundDetail));

                return new SapQuote(header, fundingDetailsCollection, Collections.emptySet(), Collections.emptySet());
            }
        });

        actionBean.setQuoteIdentifier(quoteId);
        TemplateEngine templateEngine = new TemplateEngine();templateEngine.postConstruct();

        QuoteDetailsHelper quoteDetailsHelper =
             new QuoteDetailsHelper(mockQuoteService, mockSAPService, templateEngine);
         QuoteDetailsHelper.QuoteDetail quoteDetails = quoteDetailsHelper.getQuoteDetails(quoteId, actionBean);

        QuoteDetailsHelper.FundingInfo fundingInfo = quoteDetails.getFundingDetails().iterator().next();
        assertThat(fundingInfo.isQuoteWarning(), is(false));
        assertThat(quoteDetails.getQuoteIdentifier(), equalTo(quoteId));
        assertThat(quoteDetails.getStatus(), equalTo(QuoteStatus.Z4.getStatusText()));
        assertThat(quoteDetails.getFundsRemaining(), containsString("Funds Remaining: $100.00"));
        String fundingInfoString = fundingInfo.getFundingInfoString();
        assertThat(fundingInfoString, not(containsString("funding split percentage")));
        assertThat(fundingInfoString, containsString(SapIntegrationClientImpl.FundingType.PURCHASE_ORDER.getDisplayName()));
        assertThat(fundingInfoString, containsString("<b>1234</b>"));
    }

    public void testQuoteOptionsNoFunding() throws Exception {
        String quoteId = "DNA4JD";
        testOrder = new ProductOrder();
        FundingLevel fundingLevel = new FundingLevel();
        Funding funding = new Funding();
        fundingLevel.setFunding(Collections.singleton(funding));
        QuoteFunding quoteFunding = new QuoteFunding();
        Quote testQuote = buildSingleTestQuote(quoteId, "2");
        testQuote.setQuoteFunding(quoteFunding);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(quoteId)).thenReturn(testQuote);
        actionBean.setQuoteIdentifier(quoteId);
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.postConstruct();

        QuoteDetailsHelper quoteDetailsHelper = new QuoteDetailsHelper(mockQuoteService, mockSAPService, templateEngine);
        QuoteDetailsHelper.QuoteDetail quoteDetails = quoteDetailsHelper.getQuoteDetails(quoteId, actionBean);
        assertThat(quoteDetails.getError(), is("Unable to complete evaluating order values:  null"));
        assertThat(quoteDetails.getQuoteIdentifier(), is(quoteId));
    }

    public void testSapQuoteOptionsNoFunding() throws Exception {
        String quoteId = "12345";
        testOrder = new ProductOrder();

        Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenAnswer(new Answer<SapQuote>() {
            @Override
            public SapQuote answer(InvocationOnMock invocationOnMock) throws Throwable {

                ZESDQUOTEHEADER sapQHeader = ZESDQUOTEHEADER.Factory.newInstance();
                sapQHeader.setPROJECTNAME("TestProject");
                sapQHeader.setQUOTENAME(quoteId);
                sapQHeader.setQUOTESTATUS(QuoteStatus.Z4.getStatusText());
                sapQHeader.setSALESORG("GP01");
                sapQHeader.setFUNDHEADERSTATUS("");
                sapQHeader.setCUSTOMER("");
                sapQHeader.setDISTCHANNEL("GE");
                sapQHeader.setFUNDTYPE(SapIntegrationClientImpl.FundingType.FUNDS_RESERVATION.name());
                sapQHeader.setQUOTESTATUSTXT("");
                sapQHeader.setQUOTEOPENVAL(BigDecimal.valueOf(10000));

                QuoteHeader header = new QuoteHeader(sapQHeader);
                return new SapQuote(header, Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
            }
        });
        Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class), Mockito.any(OrderCriteria.class)))
            .thenAnswer(new Answer<OrderCalculatedValues>() {
                @Override
                public OrderCalculatedValues answer(InvocationOnMock invocationOnMock) throws Throwable {
                    BigDecimal potentialOrderValue = BigDecimal.ZERO;
                    potentialOrderValue = BigDecimal.TEN;
                    for (ProductOrderAddOn addOn : pdo.getAddOns()) {
                        potentialOrderValue = BigDecimal.TEN;
                    }

                    BigDecimal fundsRemaining = ((SapQuote) invocationOnMock.getArguments()[0]).getQuoteHeader().fundsRemaining();
                    String potentialOrderId = null;

                    Optional<OrderCriteria> orderCriteria = Optional.ofNullable((OrderCriteria) invocationOnMock.getArguments()[1]);

                    if(orderCriteria.isPresent()) {
                        potentialOrderId = orderCriteria.get().getSapOrderID();
                    }

                    return new OrderCalculatedValues(potentialOrderValue, Collections.emptySet(), potentialOrderId, fundsRemaining);
                }
            });
        actionBean.setQuoteIdentifier(quoteId);
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.postConstruct();
        QuoteDetailsHelper quoteDetailsHelper = new QuoteDetailsHelper(mockQuoteService, mockSAPService, templateEngine);
        QuoteDetailsHelper.QuoteDetail quoteDetails = quoteDetailsHelper.getQuoteDetails(quoteId, actionBean);

        assertThat(quoteDetails.getError(), is("This quote has no active Funding Sources."));
        assertThat(quoteDetails.getQuoteIdentifier(), is(quoteId));
     }

    /**
     *
     * Tests the validation methods executed when a Product Order is saved
     * @throws Exception
     */
    public void testSaveValidationsQuoteTooSmall() throws Exception {

        // Initialize the Order to be tested

        pdo = ProductOrderTestFactory.createDummyProductOrder();
        pdo.setQuoteId("BSP252");
        pdo.addRegulatoryInfo(new RegulatoryInfo("test", RegulatoryInfo.Type.IRB, "test"));
        pdo.setAttestationConfirmed(true);
        pdo.setJiraTicketKey("");
        pdo.setOrderStatus(ProductOrder.OrderStatus.Draft);
        pdo.setCreatedBy(1L);

        // Initialize the Action Bean to be in such a state that we can mimic calls from the web
        HttpServletRequest request = new MockHttpServletRequest("foo","bar");
        actionBean.setContext(new CoreActionBeanContext());


        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();

        //setup the mock quote server to return a fully defined quote found in our quoteTestData.xml file
        Mockito.when(mockQuoteService.getQuoteByAlphaId(Mockito.anyString()))
                .then(new Answer<Quote>() {
                    @Override
                    public Quote answer(InvocationOnMock invocationOnMock) throws Throwable {
                        return stubQuoteService.getQuoteByAlphaId(pdo.getQuoteId());
                    }
                });

        //Now setup the pricing for the products.  The aim is to have the price set so that the value of the order
        // (without customizations) will be more than the quote.  Later on, we will set the customizations up to have
        // a custom price to make the value of the order will be less than the quote.
        final BigDecimal quoteFundsRemaining = new BigDecimal(mockQuoteService.getQuoteByAlphaId(pdo.getQuoteId())
                        .getQuoteFunding().getFundsRemaining());
        BigDecimal pricedMoreThanQuote =
                quoteFundsRemaining.divide((BigDecimal.valueOf(pdo.getSamples().size()).multiply(
                        BigDecimal.valueOf(pdo.getAddOns().size() + 1))).divide(BigDecimal.valueOf(4), 2, RoundingMode.CEILING),RoundingMode.FLOOR);

        final List<String> addOnKeys = pdo.getAddOns().stream().map(productOrderAddOn -> {
            return productOrderAddOn.getAddOn().getPartNumber();
        }).collect(Collectors.toList());

        addPriceItemForProduct("BSP252", priceList, quoteItems, pdo.getProduct(),
                pricedMoreThanQuote.toString(), "20", pricedMoreThanQuote.toString());
        final SAPMaterial productMaterial =
                new SAPMaterial(pdo.getProduct().getPartNumber(),
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getDefaultWbs(),
                        pdo.getProduct().getName(),pricedMoreThanQuote.toString(),
                        "EA",BigDecimal.ONE,
                        new Date(),new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization());
        productMaterial.updateCompanyConfiguration(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        returnMaterials.add(productMaterial);

        for (ProductOrderAddOn addOn : pdo.getAddOns()) {
            addPriceItemForProduct("BSP252", priceList, quoteItems,
                    addOn.getAddOn(),
                    pricedMoreThanQuote.multiply(BigDecimal.valueOf(2)).toString(),
                    "20",
                    pricedMoreThanQuote.multiply(BigDecimal.valueOf(2)).toString());
            final SAPMaterial addonMaterial =
                    new SAPMaterial(addOn.getAddOn().getPartNumber(),
                            SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                            SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getDefaultWbs(),
                            pdo.getProduct().getName(),
                            pricedMoreThanQuote.multiply(BigDecimal.valueOf(2)).toString(),"EA",
                            BigDecimal.ONE,
                            new Date(), new Date(),
                            Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization());
            addonMaterial.updateCompanyConfiguration(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
            returnMaterials.add(addonMaterial);
        }

        //Set up the rest of the Mocks with values that will refelct the conditions needed
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        stubProductPriceCache.refreshCache();
        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn((Collections.singletonList(pdo)));
        Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class), Mockito.any(OrderCriteria.class)))
            .thenAnswer(new Answer<OrderCalculatedValues>() {
                    @Override
                    public OrderCalculatedValues answer(InvocationOnMock invocationOnMock) throws Throwable {

                        BigDecimal potentialOrderValue = BigDecimal.ZERO;
                        potentialOrderValue = incrementOrderValue(potentialOrderValue, priceList, pdo, pdo.getProduct());

                        for (ProductOrderAddOn addOn : pdo.getAddOns()) {
                            potentialOrderValue = incrementOrderValue(potentialOrderValue, priceList, pdo, addOn.getAddOn());
                        }

                        BigDecimal fundsRemaining = ((SapQuote) invocationOnMock.getArguments()[0]).getQuoteHeader().fundsRemaining();
                        String potentialOrderId = null;

                        Optional<OrderCriteria> orderCriteria = Optional.ofNullable((OrderCriteria) invocationOnMock.getArguments()[1]);

                        if(orderCriteria.isPresent()) {
                            potentialOrderId = orderCriteria.get().getSapOrderID();
                        }

                        return new OrderCalculatedValues(potentialOrderValue, Collections.emptySet(), potentialOrderId, fundsRemaining);
                    }
                });

        actionBean.setEditOrder(pdo);
        actionBean.setQuoteService(mockQuoteService);

        Mockito.when(mockBspUserList.getById(Mockito.any(Long.class)))
                .thenReturn(new BspUser(1L, "", "squidUser@broadinstitute.org", "Squid", "User", Collections.<String>emptyList(),1L, "squiduser" ));
        Mockito.when(mockJiraService.isValidUser(Mockito.anyString())).thenReturn(true);

        Mockito.when(mockProductDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<Product>() {
            @Override
            public Product answer(InvocationOnMock invocationOnMock) throws Throwable {
                return pdo.getProduct();
            }
        });
        Mockito.when(mockProductDao.findByPartNumber(Mockito.anyString())).thenAnswer(new Answer<Product>() {
            @Override
            public Product answer(InvocationOnMock invocationOnMock) throws Throwable {
                return pdo.getProduct();
            }
        });
        ResearchProjectDao mockResearchProjectDao = Mockito.mock(ResearchProjectDao.class);
        Mockito.when(mockResearchProjectDao.findByBusinessKey(Mockito.anyString()))
                .thenAnswer(new Answer<ResearchProject>() {
                    @Override
                    public ResearchProject answer(InvocationOnMock invocationOnMock) throws Throwable {
                        return pdo.getResearchProject();
                    }
                });
        BSPGroupCollectionList mockGroupCollectionList = Mockito.mock(BSPGroupCollectionList.class);
        Mockito.when(mockGroupCollectionList.find(Mockito.anyString()))
                .thenAnswer(new Answer<List<SampleCollection>>() {
                    @Override
                    public List<SampleCollection> answer(InvocationOnMock invocationOnMock) throws Throwable {
                        final Group group = new Group();
                        final SampleCollection sampleCollection =
                                new SampleCollection(1l, "TestName", group,
                                        "testCategory", "Nothing much", true,
                                        Collections.emptyList());
                        return Collections.singletonList(sampleCollection);
                    }
                });
        Mockito.when(mockGroupCollectionList.getById(Mockito.anyLong()))
                .thenAnswer(new Answer<SampleCollection>() {
                    @Override
                    public SampleCollection answer(InvocationOnMock invocationOnMock) throws Throwable {
                        final Group group = new Group();
                        final SampleCollection sampleCollection =
                                new SampleCollection(1l, "TestName", group,
                                "testCategory", "Nothing much", true,
                                Collections.emptyList());
                        return sampleCollection;
                    }
                });

        UserTokenInput userTokenInput = new UserTokenInput(mockBspUserList);

        Mockito.when(mockBspUserList.getById(Mockito.any(Long.class)))
                .thenReturn(new BspUser(1L, "", "squidUser@broadinstitute.org", "Squid", "User", Collections.<String>emptyList(),1L, "squiduser" ));
        Mockito.when(mockBspUserList.find(Mockito.anyString()))
                .thenReturn(Collections.singletonList(new BspUser(1L, "", "squidUser@broadinstitute.org", "Squid", "User", Collections.<String>emptyList(),1L, "squiduser" )));

        // Ensure that there were no Customizations previously set on the Product Order
        Assert.assertTrue(CollectionUtils.isEmpty(pdo.getCustomPriceAdjustments()));

        // The create page will set values with Token input.  Initialize this token input to set values such as the
        //
        ProductTokenInput productTokenInput = new ProductTokenInput();
        productTokenInput.setProductDao(mockProductDao);
        ProjectTokenInput projectTokenInput = new ProjectTokenInput();
        projectTokenInput.setResearchProjectDao(mockResearchProjectDao);
        BspGroupCollectionTokenInput bspGroupCollectionTokenInput = new BspGroupCollectionTokenInput();
        bspGroupCollectionTokenInput.setBspCollectionList(mockGroupCollectionList);
        BspShippingLocationTokenInput locationTokenInput = new BspShippingLocationTokenInput();

        BSPManagerFactory managerFactory = Mockito.mock(BSPManagerFactory.class);
        BSPSiteList siteList = Mockito.mock(BSPSiteList.class);

        locationTokenInput.setBspManagerFactory(managerFactory);
        locationTokenInput.setBspSiteList(siteList);
        actionBean.setProductTokenInput(productTokenInput);
        actionBean.setProjectTokenInput(projectTokenInput);
        actionBean.setBspGroupCollectionTokenInput(bspGroupCollectionTokenInput);
        actionBean.setBspShippingLocationTokenInput(locationTokenInput);
        actionBean.setNotificationListTokenInput(userTokenInput);
        actionBean.setResearchProjectDao(mockResearchProjectDao);
        actionBean.setProductDao(mockProductDao);
        actionBean.setOwner(userTokenInput);

        actionBean.setAddOnKeys(addOnKeys);

        actionBean.populateTokenListsFromObjectData();
        userTokenInput.setup("1");
        // end of definitions for token input

        // call validation methods called during save
        actionBean.saveValidations();
        actionBean.doValidation(actionBean.SAVE_ACTION);

        //Errors should be just related to quote does not have enough value
        Assert.assertFalse(actionBean.getValidationErrors().isEmpty(),
                "validation Errors should not be empty at this stage");
        actionBean.clearValidationErrors();



        // Begin to mimic creating pricing customizations from the user.  Subtract $1 from price so that the value of
        // the order is still over the quoted value
        JSONObject customizationJson = new JSONObject();
        CustomizationValues customPricePrimary = new CustomizationValues(pdo.getProduct().getPartNumber(),
                String.valueOf(pdo.getSamples().size()),
                pricedMoreThanQuote.subtract(BigDecimal.valueOf(1)).toString(), "");
        customizationJson.put(pdo.getProduct().getPartNumber(), customPricePrimary.toJson());
        for (ProductOrderAddOn productOrderAddOn : pdo.getAddOns()) {

            CustomizationValues customPriceAddon = new CustomizationValues(productOrderAddOn.getAddOn().getPartNumber(),
                    String.valueOf(pdo.getSamples().size()),
                    pricedMoreThanQuote.divide(BigDecimal.valueOf(3)).toString(), "");
            customizationJson.put(productOrderAddOn.getAddOn().getPartNumber(), customPriceAddon.toJson());
        }

        //Set the customizations on the action bean
        actionBean.setCustomizationJsonString(customizationJson.toString());
        actionBean.setAddOnKeys(addOnKeys);

        actionBean.populateTokenListsFromObjectData();
        userTokenInput.setup("1");

        // re-call the save validations
        actionBean.saveValidations();
        actionBean.doValidation(actionBean.SAVE_ACTION);

        // Now there are no errors, Quote value compared to Order value is sufficient
        Assert.assertFalse(actionBean.getValidationErrors().isEmpty(),
                "Validation errors should not be empty.");
        Assert.assertTrue(CollectionUtils.isNotEmpty(pdo.getCustomPriceAdjustments()));
        actionBean.clearValidationErrors();




//////////////////////////############################################/////////////////////////////
        // Begin to mimic creating pricing customizations from the user.  Divide the price by 3 to ensure that the
        customizationJson = new JSONObject();
        // order value is lower than the quote
        customPricePrimary = new CustomizationValues(pdo.getProduct().getPartNumber(),
                String.valueOf(pdo.getSamples().size()),
                pricedMoreThanQuote.divide(BigDecimal.valueOf(3), 2, RoundingMode.FLOOR).toString(), "");
        customizationJson.put(pdo.getProduct().getPartNumber(), customPricePrimary.toJson());
        for (ProductOrderAddOn productOrderAddOn : pdo.getAddOns()) {

            CustomizationValues customPriceAddon = new CustomizationValues(productOrderAddOn.getAddOn().getPartNumber(),
                    String.valueOf(pdo.getSamples().size()),
                    pricedMoreThanQuote.divide(BigDecimal.valueOf(3)).toString(), "");
            customizationJson.put(productOrderAddOn.getAddOn().getPartNumber(), customPriceAddon.toJson());
        }

        //Set the customizations on the action bean
        actionBean.setCustomizationJsonString(customizationJson.toString());
        actionBean.setAddOnKeys(addOnKeys);

        actionBean.populateTokenListsFromObjectData();
        userTokenInput.setup("1");

        // re-call the save validations
        actionBean.saveValidations();
        actionBean.doValidation(actionBean.SAVE_ACTION);

        // Now there are no errors, Quote value compared to Order value is sufficient
        Assert.assertTrue(actionBean.getValidationErrors().isEmpty(),
                "Validation errors should be empty.");
        Assert.assertTrue(CollectionUtils.isNotEmpty(pdo.getCustomPriceAdjustments()));
    }

    /**
     *
     * Tests the validation methods executed when a Product Order is saved
     * @throws Exception
     */
    public void testSaveValidationsQuoteHasEnoughFunding() throws Exception {

        // Initialize the Order to be tested

        pdo = ProductOrderTestFactory.createDummyProductOrder();
        final String quoteId = "BSP252";
        pdo.setQuoteId(quoteId);
        pdo.addRegulatoryInfo(new RegulatoryInfo("test", RegulatoryInfo.Type.IRB, "test"));
        pdo.setAttestationConfirmed(true);
        pdo.setJiraTicketKey("");
        pdo.setOrderStatus(ProductOrder.OrderStatus.Draft);
        pdo.setCreatedBy(1L);

        // Initialize the Action Bean to be in such a state that we can mimic calls from the web
        HttpServletRequest request = new MockHttpServletRequest("foo","bar");
        actionBean.setContext(new CoreActionBeanContext());


        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();

        //setup the mock quote server to return a fully defined quote found in our quoteTestData.xml file
        Mockito.when(mockQuoteService.getQuoteByAlphaId(Mockito.anyString()))
                .then(new Answer<Quote>() {
                    @Override
                    public Quote answer(InvocationOnMock invocationOnMock) throws Throwable {
                        return stubQuoteService.getQuoteByAlphaId(pdo.getQuoteId());
                    }
                });

        // Now setup the pricing for the products.  The aim is to have the price set so that the value of the order
        // (without customizations) will be just enough for the quote.  Later on, we will set the customizations up to
        // have a custom price to make the value of the order first greater than the quote, then it will be less than
        // the quote.
        final BigDecimal quoteFundsRemaining = new BigDecimal(mockQuoteService.getQuoteByAlphaId(pdo.getQuoteId())
                .getQuoteFunding().getFundsRemaining());
        final BigDecimal sampleSize = BigDecimal.valueOf(pdo.getSamples().size());
        final BigDecimal productSize = BigDecimal.valueOf(pdo.getAddOns().size() + 1);
        final List<String> addOnKeys = pdo.getAddOns().stream().map(productOrderAddOn -> {
            return productOrderAddOn.getAddOn().getPartNumber();
        }).collect(Collectors.toList());
        BigDecimal pricePerSample =
                quoteFundsRemaining.divide(
                        (sampleSize.multiply(productSize))
                                .multiply(BigDecimal.valueOf(4)),
                        2, RoundingMode.FLOOR);

        System.out.println("Quote funding is " + quoteFundsRemaining.toString() +". Price per sample is " +
                           pricePerSample.toString() + ". sample Size is " + pdo.getSamples().size() +
                           ". number of addons is " + pdo.getAddOns().size());

        addPriceItemForProduct(quoteId, priceList, quoteItems, pdo.getProduct(), pricePerSample.toString(),
                "20", pricePerSample.toString());
        final SAPMaterial productMaterial =
                new SAPMaterial(pdo.getProduct().getPartNumber(),
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getDefaultWbs(),
                        pdo.getProduct().getName(),pricePerSample.toString(),"EA",
                        BigDecimal.ONE,new Date(), new Date(),
                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization());
        productMaterial.updateCompanyConfiguration(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);
        returnMaterials.add(productMaterial);

        for (ProductOrderAddOn addOn : pdo.getAddOns()) {
            addPriceItemForProduct(quoteId, priceList, quoteItems, addOn.getAddOn(),
                    pricePerSample.toString(), "20",
                    pricePerSample.toString());
            final SAPMaterial addonMaterial =
                    new SAPMaterial(addOn.getAddOn().getPartNumber(),
                            SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                            SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getDefaultWbs(),
                            pdo.getProduct().getName(),
                            pricePerSample.toString(), "EA",BigDecimal.ONE,
                            new Date(),new Date(),Collections.emptyMap(), Collections.emptyMap(),
                            SAPMaterial.MaterialStatus.ENABLED,
                            SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization());
            returnMaterials.add(addonMaterial);
        }

        //Set up the rest of the Mocks with values that will refelct the conditions needed
        Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
        stubProductPriceCache.refreshCache();
        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn((Collections.singletonList(pdo)));
        Mockito.when(mockSapClient.calculateOrderValues(Mockito.any(SapQuote.class), Mockito.any(OrderCriteria.class)))
                .thenAnswer(new Answer<OrderCalculatedValues>() {
                    @Override
                    public OrderCalculatedValues answer(InvocationOnMock invocationOnMock) throws Throwable {
                        BigDecimal potentialOrderValue = BigDecimal.ZERO;
                        potentialOrderValue = incrementOrderValue(potentialOrderValue, priceList, pdo, pdo.getProduct());
                        for (ProductOrderAddOn addOn : pdo.getAddOns()) {
                            potentialOrderValue = incrementOrderValue(potentialOrderValue, priceList, pdo, addOn.getAddOn());
                        }

                        BigDecimal fundsRemaining = ((SapQuote) invocationOnMock.getArguments()[0]).getQuoteHeader().fundsRemaining();
                        String potentialOrderId = null;

                        Optional<OrderCriteria> orderCriteria = Optional.ofNullable((OrderCriteria) invocationOnMock.getArguments()[1]);

                        if(orderCriteria.isPresent()) {
                            potentialOrderId = orderCriteria.get().getSapOrderID();
                        }

                        return new OrderCalculatedValues(potentialOrderValue, Collections.emptySet(), potentialOrderId, fundsRemaining);
                    }
                });

        actionBean.setEditOrder(pdo);
        actionBean.setQuoteService(mockQuoteService);

        Mockito.when(mockBspUserList.getById(Mockito.any(Long.class)))
                .thenReturn(new BspUser(1L, "", "squidUser@broadinstitute.org",
                        "Squid", "User", Collections.<String>emptyList(),1L,
                        "squiduser" ));
        Mockito.when(mockJiraService.isValidUser(Mockito.anyString())).thenReturn(true);

        Mockito.when(mockProductDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<Product>() {
            @Override
            public Product answer(InvocationOnMock invocationOnMock) throws Throwable {
                return pdo.getProduct();
            }
        });
        Mockito.when(mockProductDao.findByPartNumber(Mockito.anyString())).thenAnswer(new Answer<Product>() {
            @Override
            public Product answer(InvocationOnMock invocationOnMock) throws Throwable {
                return pdo.getProduct();
            }
        });
        ResearchProjectDao mockResearchProjectDao = Mockito.mock(ResearchProjectDao.class);
        Mockito.when(mockResearchProjectDao.findByBusinessKey(Mockito.anyString()))
                .thenAnswer(new Answer<ResearchProject>() {
                    @Override
                    public ResearchProject answer(InvocationOnMock invocationOnMock) throws Throwable {
                        return pdo.getResearchProject();
                    }
                });
        BSPGroupCollectionList mockGroupCollectionList = Mockito.mock(BSPGroupCollectionList.class);
        Mockito.when(mockGroupCollectionList.find(Mockito.anyString()))
                .thenAnswer(new Answer<List<SampleCollection>>() {
                    @Override
                    public List<SampleCollection> answer(InvocationOnMock invocationOnMock) throws Throwable {
                        final Group group = new Group();
                        final SampleCollection sampleCollection =
                                new SampleCollection(1l, "TestName", group,
                                        "testCategory", "Nothing much", true,
                                        Collections.emptyList());
                        return Collections.singletonList(sampleCollection);
                    }
                });
        Mockito.when(mockGroupCollectionList.getById(Mockito.anyLong()))
                .thenAnswer(new Answer<SampleCollection>() {
                    @Override
                    public SampleCollection answer(InvocationOnMock invocationOnMock) throws Throwable {
                        final Group group = new Group();
                        final SampleCollection sampleCollection =
                                new SampleCollection(1l, "TestName", group,
                                        "testCategory", "Nothing much", true,
                                        Collections.emptyList());
                        return sampleCollection;
                    }
                });

        UserTokenInput userTokenInput = new UserTokenInput(mockBspUserList);

        Mockito.when(mockBspUserList.getById(Mockito.any(Long.class)))
                .thenReturn(new BspUser(1L, "", "squidUser@broadinstitute.org",
                        "Squid", "User", Collections.<String>emptyList(),1L,
                        "squiduser" ));
        Mockito.when(mockBspUserList.find(Mockito.anyString()))
                .thenReturn(Collections.singletonList(new BspUser(1L, "",
                        "squidUser@broadinstitute.org", "Squid", "User",
                        Collections.<String>emptyList(),1L, "squiduser" )));

        // Ensure that there were no Customizations previously set on the Product Order
        Assert.assertTrue(CollectionUtils.isEmpty(pdo.getCustomPriceAdjustments()));

        // The create page will set values with Token input.  Initialize this token input to set values such as the
        //
        ProductTokenInput productTokenInput = new ProductTokenInput();
        productTokenInput.setProductDao(mockProductDao);
        ProjectTokenInput projectTokenInput = new ProjectTokenInput();
        projectTokenInput.setResearchProjectDao(mockResearchProjectDao);
        BspGroupCollectionTokenInput bspGroupCollectionTokenInput = new BspGroupCollectionTokenInput();
        bspGroupCollectionTokenInput.setBspCollectionList(mockGroupCollectionList);
        BspShippingLocationTokenInput locationTokenInput = new BspShippingLocationTokenInput();

        BSPManagerFactory managerFactory = Mockito.mock(BSPManagerFactory.class);
        BSPSiteList siteList = Mockito.mock(BSPSiteList.class);

        locationTokenInput.setBspManagerFactory(managerFactory);
        locationTokenInput.setBspSiteList(siteList);
        actionBean.setProductTokenInput(productTokenInput);
        actionBean.setProjectTokenInput(projectTokenInput);
        actionBean.setBspGroupCollectionTokenInput(bspGroupCollectionTokenInput);
        actionBean.setBspShippingLocationTokenInput(locationTokenInput);
        actionBean.setNotificationListTokenInput(userTokenInput);
        actionBean.setResearchProjectDao(mockResearchProjectDao);
        actionBean.setProductDao(mockProductDao);
        actionBean.setOwner(userTokenInput);

        actionBean.setAddOnKeys(addOnKeys);
        actionBean.populateTokenListsFromObjectData();
        userTokenInput.setup("1");
        // end of definitions for token input

        // call validation methods called during save
        actionBean.saveValidations();
        actionBean.doValidation(actionBean.SAVE_ACTION);

        //Errors should be just related to quote does not have enough value
        Assert.assertTrue(actionBean.getValidationErrors().isEmpty(),
                "validation Errors should be empty at this stage");
        actionBean.clearValidationErrors();



        // Begin to mimic creating pricing customizations from the user.  Subtract $1 from price so that the value of
        // the order is still over the quoted value
        JSONObject customizationJson = new JSONObject();
        CustomizationValues customPricePrimary = new CustomizationValues(pdo.getProduct().getPartNumber(),
                String.valueOf(pdo.getSamples().size()),
                pricePerSample.multiply(BigDecimal.valueOf(4)).toString(), "");
        customizationJson.put(pdo.getProduct().getPartNumber(), customPricePrimary.toJson());
        for (ProductOrderAddOn productOrderAddOn : pdo.getAddOns()) {

            CustomizationValues customPriceAddon = new CustomizationValues(productOrderAddOn.getAddOn().getPartNumber(),
                    String.valueOf(pdo.getSamples().size()),
                    pricePerSample.multiply(BigDecimal.valueOf(4.5)).toString(), "");
            customizationJson.put(productOrderAddOn.getAddOn().getPartNumber(), customPriceAddon.toJson());
        }

        //Set the customizations on the action bean
        actionBean.setAddOnKeys(addOnKeys);
        actionBean.setCustomizationJsonString(customizationJson.toString());

        actionBean.populateTokenListsFromObjectData();
        userTokenInput.setup("1");


        // re-call the save validations
        actionBean.saveValidations();
        actionBean.doValidation(actionBean.SAVE_ACTION);

        // Now there are no errors, Quote value compared to Order value is sufficient
        Assert.assertTrue(actionBean.getValidationErrors().isEmpty(),
                "Validation errors should not be empty.");
        Assert.assertTrue(CollectionUtils.isNotEmpty(pdo.getCustomPriceAdjustments()));
        actionBean.clearValidationErrors();




//////////////////////////############################################/////////////////////////////
        // Begin to mimic creating pricing customizations from the user.  Divide the price by 3 to ensure that the
        customizationJson = new JSONObject();
        // order value is lower than the quote
        customPricePrimary = new CustomizationValues(pdo.getProduct().getPartNumber(),
                String.valueOf(pdo.getSamples().size()),
                pricePerSample.divide(BigDecimal.valueOf(3), 2, RoundingMode.FLOOR).toString(), "");
        customizationJson.put(pdo.getProduct().getPartNumber(), customPricePrimary.toJson());
        for (ProductOrderAddOn productOrderAddOn : pdo.getAddOns()) {

            CustomizationValues customPriceAddon = new CustomizationValues(productOrderAddOn.getAddOn().getPartNumber(),
                    String.valueOf(pdo.getSamples().size()),
                    pricePerSample.divide(BigDecimal.valueOf(3), 2, RoundingMode.FLOOR).toString(), "");
            customizationJson.put(productOrderAddOn.getAddOn().getPartNumber(), customPriceAddon.toJson());
        }

        //Set the customizations on the action bean
        actionBean.setAddOnKeys(addOnKeys);
        actionBean.setCustomizationJsonString(customizationJson.toString());

        actionBean.populateTokenListsFromObjectData();

        userTokenInput.setup("1");

        // re-call the save validations
        actionBean.saveValidations();
        actionBean.doValidation(actionBean.SAVE_ACTION);

        // Now there are no errors, Quote value compared to Order value is sufficient
        Assert.assertTrue(actionBean.getValidationErrors().isEmpty(),
                "Validation errors should be empty.");
        Assert.assertTrue(CollectionUtils.isNotEmpty(pdo.getCustomPriceAdjustments()));
    }

    private enum ProductScenario {LLC, EXTENDS_TO_LLC, JUST_SSF}
    private enum QuoteSalesOrgScenario {MATCHES_PRODUCT, OPPOSITE_PRODUCT, EXTENDED_ORG, NOT_VALID_FOR_PRODUCT}

    @DataProvider(name = "provideAlternateQuoteTypes")
    public Iterator<Object[]> provideAlternateQuoteTypes() {
        List<Object[]> testCases = new ArrayList<>();
        // Quote ID,  expected source type,  Order status,  Will the product be offered in the sales org of the quote, is the quote valid, Scenaro for Product configuration
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"027000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.LLC});





        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.EXTENDED_ORG, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, true, ProductScenario.LLC});

        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.EXTENDS_TO_LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.MATCHES_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.OPPOSITE_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.EXTENDED_ORG, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.LLC});
        testCases.add(new Object[]{"27000001", ProductOrder.QuoteSourceType.SAP_SOURCE, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT, false, ProductScenario.LLC});

        //Quote Server Quotes
        testCases.add(new Object[]{"GPF91", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Draft,      QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"GPF91", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Pending,    QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"GPF91", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Submitted,  QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"GPF91", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Abandoned,  QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"GPF91", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Completed,  QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"0GPF91", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Draft,      QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"0GPF91", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Pending,    QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"0GPF91", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Submitted,  QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"0GPF91", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Abandoned,  QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"0GPF91", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Completed,  QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027oooo1", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Draft,     QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027oooo1", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Pending,   QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027oooo1", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Submitted, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027oooo1", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Abandoned, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});
        testCases.add(new Object[]{"027oooo1", ProductOrder.QuoteSourceType.QUOTE_SERVER, ProductOrder.OrderStatus.Completed, QuoteSalesOrgScenario.MATCHES_PRODUCT, true, ProductScenario.JUST_SSF});



        return testCases.iterator();
    }

    @Test(dataProvider = "provideAlternateQuoteTypes")
    public void testValidateQuoteSoure(String quote, ProductOrder.QuoteSourceType expectedSourceType,
                                       ProductOrder.OrderStatus orderStatus, QuoteSalesOrgScenario quoteMatchSalesOrg,
                                       boolean quoteIsValid, ProductScenario scenarioForProductSalesOrg)
            throws Exception{
        pdo = ProductOrderTestFactory.createDummyProductOrder(3, "");
        pdo.setQuoteId(quote);
        pdo.setOrderStatus(orderStatus);
        String quoteSalesOrgMatch = null;
        String quoteSalesOrgOpposite = null;
        for (Product product : ProductOrder.getAllProductsOrdered(pdo)) {
            switch (scenarioForProductSalesOrg) {
            case LLC:
                product.setExternalOnlyProduct(true);
                quoteSalesOrgMatch =
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization();
                quoteSalesOrgOpposite = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization();
                break;
            case EXTENDS_TO_LLC:
                product.setOfferedAsCommercialProduct(true);
                quoteSalesOrgMatch =
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization();
                quoteSalesOrgOpposite = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization();
                break;
            case JUST_SSF:
            default:
                product.setExternalOnlyProduct(false);
                product.setOfferedAsCommercialProduct(false);
                quoteSalesOrgMatch =
                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getSalesOrganization();
                quoteSalesOrgOpposite = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization();
                break;
            }
        }

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();

        BigDecimal pricedMoreThanQuote = BigDecimal.valueOf(20.00d);
        addPriceItemForProduct(quote, priceList, quoteItems, pdo.getProduct(),
                pricedMoreThanQuote.toString(), "20", pricedMoreThanQuote.toString());

        EnumSet<SapIntegrationClientImpl.SAPCompanyConfiguration> sapCompanyConfigurations =
                EnumSet.copyOf(SapIntegrationServiceImpl.EXTENDED_PLATFORMS);
        sapCompanyConfigurations.add(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD);

        ProductOrder.OrderAccessType testOrderAccessType =
            ProductOrder.determineOrderType(pdo, quoteSalesOrgMatch);
        assertThat(testOrderAccessType.getSalesOrg(), equalTo(quoteSalesOrgMatch));

        for (SapIntegrationClientImpl.SAPCompanyConfiguration companyCode : sapCompanyConfigurations) {

            final Set<SapIntegrationClientImpl.SAPCompanyConfiguration> llcCompanyCodes =
                    Stream.of(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD,
                            SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES).collect(Collectors.toSet());
            if(scenarioForProductSalesOrg == ProductScenario.LLC &&
               !llcCompanyCodes.contains(companyCode )) {
                break;
            }
            Optional<String> defaultWbs = Optional.ofNullable(companyCode.getDefaultWbs());
            final SAPMaterial productMaterial =
                    new SAPMaterial(pdo.getProduct().getPartNumber(),
                            companyCode,
                            defaultWbs.orElse("888"),
                            pdo.getProduct().getName(),pricedMoreThanQuote.toString(),
                            "EA",BigDecimal.ONE,
                            new Date(),new Date(),
                            Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                            companyCode.getSalesOrganization());
            returnMaterials.add(productMaterial);

            for (ProductOrderAddOn addOn : pdo.getAddOns()) {
                addPriceItemForProduct(quote, priceList, quoteItems,
                        addOn.getAddOn(),
                        pricedMoreThanQuote.multiply(BigDecimal.valueOf(2)).toString(),
                        "20",
                        pricedMoreThanQuote.multiply(BigDecimal.valueOf(2)).toString());
                final SAPMaterial addonMaterial =
                        new SAPMaterial(addOn.getAddOn().getPartNumber(),
                                companyCode,
                                defaultWbs.orElse("888"),
                                pdo.getProduct().getName(),
                                pricedMoreThanQuote.multiply(BigDecimal.valueOf(2)).toString(),"EA",
                                BigDecimal.ONE,
                                new Date(), new Date(),
                                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED,
                                companyCode.getSalesOrganization());
                returnMaterials.add(addonMaterial);
            }
        }


        if (expectedSourceType == ProductOrder.QuoteSourceType.SAP_SOURCE) {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString())).thenReturn(returnMaterials);
            stubProductPriceCache.refreshCache();
            if (quoteIsValid) {
                String matchOrg = quoteSalesOrgMatch;
                String oppositeOrg = quoteSalesOrgOpposite;
                Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString())).thenAnswer(new Answer<SapQuote>() {
                    @Override
                    public SapQuote answer(InvocationOnMock invocationOnMock) throws Throwable {
                        String salesOrg = null;
                        switch (quoteMatchSalesOrg) {
                        case EXTENDED_ORG:
                            salesOrg = SapIntegrationClientImpl.SAPCompanyConfiguration.GPP.getSalesOrganization();
                            break;
                        case OPPOSITE_PRODUCT:
                            salesOrg = oppositeOrg;
                            break;
                        case MATCHES_PRODUCT:
                            salesOrg = matchOrg;
                            break;
                        case NOT_VALID_FOR_PRODUCT:
                            salesOrg = "GP03";
                        }
                        return TestUtils.buildTestSapQuote("27000001",
                                BigDecimal.valueOf(30000), BigDecimal.valueOf(37387), pdo,
                                TestUtils.SapQuoteTestScenario.DOLLAR_LIMITED, salesOrg);
                    }
                });
            } else {
                Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                        .thenThrow(new SAPIntegrationException("Quote is Invalid"));
            }
        } else {
            Mockito.when(mockSapClient.findMaterials(Mockito.anyString(), Mockito.anyString()))
                    .thenThrow(new RuntimeException("SAP Should not be searched for here"));
            Mockito.when(mockSapClient.findQuoteDetails(Mockito.anyString()))
                    .thenThrow(new RuntimeException("SAP Should not be searched for here"));
        }

        actionBean.setEditOrder(pdo);

        try {
            actionBean.updateAndValidateQuoteSource(ProductOrder.getAllProductsOrdered(pdo));

            if((quoteMatchSalesOrg == QuoteSalesOrgScenario.OPPOSITE_PRODUCT &&
                scenarioForProductSalesOrg != ProductScenario.EXTENDS_TO_LLC) ||

               quoteMatchSalesOrg == QuoteSalesOrgScenario.NOT_VALID_FOR_PRODUCT ||

               (quoteMatchSalesOrg == QuoteSalesOrgScenario.EXTENDED_ORG &&
                scenarioForProductSalesOrg == ProductScenario.LLC) ||

               !quoteIsValid
            ) {
                Assert.fail("An exception should have been thrown");
            } else {
                Assert.assertEquals(pdo.getQuoteSource(), expectedSourceType);
                Assert.assertTrue(actionBean.getValidationErrors().isEmpty(),
                        "No validations should have been thrown in this scenario");
            }
        } catch (InvalidProductException e) {

        }
    }

    private static BigDecimal incrementOrderValue(BigDecimal potentialOrderValue, PriceList priceList,
                                                  ProductOrder pdo, Product product) {
        BigDecimal primaryMultiplier =
                new BigDecimal(priceList.findByKeyFields(product.getPrimaryPriceItem()).getPrice());
        Optional<PriceAdjustment> primaryPriceAdjustment = Optional.ofNullable(pdo.getAdjustmentForProduct(
                product));
        if (primaryPriceAdjustment.isPresent()){
            if(primaryPriceAdjustment.get().getAdjustmentValue() != null) {
                primaryMultiplier = primaryPriceAdjustment.get().getAdjustmentValue();
            }
        }
        System.out.println("Price multiplier for " + product.getPartNumber() + " is " +
                           primaryMultiplier.toString());
        potentialOrderValue = potentialOrderValue.add(BigDecimal.valueOf(pdo.getSamples().size()).multiply(primaryMultiplier));
        return potentialOrderValue;
    }

    @DataProvider(name = "changeQuoteDataProvider")
    public Iterator<Object[]> changeQuoteDataProvider() {
        final String sapQuote = "1234";
        final String sapQuote2 = "1234567";
        final String qsQuote = "GPP1";
        final String qsQuote2 = "GPP1234";
        List<Object[]> testCases = new ArrayList<>();

        // All of these statuses should not allow changing of quotes
        EnumSet<ProductOrder.OrderStatus> statusesForChangeAllowed = EnumSet
            .of(ProductOrder.OrderStatus.Draft, ProductOrder.OrderStatus.Pending);
        EnumSet<ProductOrder.OrderStatus> statusesForChangeNotAllowed = EnumSet.complementOf(statusesForChangeAllowed);
        boolean quoteRequired = true;
        boolean noQuoteRequired = false;
        statusesForChangeNotAllowed.forEach(orderStatus -> {
            assertThat(orderStatus.canPlace(), is(false));
            testCases.add(new Object[]{orderStatus, quoteRequired, sapQuote, qsQuote, false});
            testCases.add(new Object[]{orderStatus, quoteRequired, qsQuote, sapQuote, false});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, sapQuote, qsQuote, false});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, qsQuote, sapQuote, false});
            // not chaning quote type is OK
            testCases.add(new Object[]{orderStatus, quoteRequired, sapQuote, sapQuote, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, qsQuote, qsQuote, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, sapQuote, sapQuote2, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, qsQuote, qsQuote2, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, sapQuote, sapQuote, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, qsQuote, qsQuote, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, sapQuote, sapQuote2, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, qsQuote, qsQuote2, true});

            testCases.add(new Object[]{orderStatus, quoteRequired, null, qsQuote, false});
            testCases.add(new Object[]{orderStatus, quoteRequired, qsQuote, null, false});
            testCases.add(new Object[]{orderStatus, quoteRequired, null, sapQuote, false});
            testCases.add(new Object[]{orderStatus, quoteRequired, sapQuote, null, false});
            testCases.add(new Object[]{orderStatus, quoteRequired, null, null, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, null, qsQuote, false});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, qsQuote, null, false});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, null, sapQuote, false});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, sapQuote, null, false});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, null, null, true});

            testCases.add(new Object[]{orderStatus, quoteRequired, "", qsQuote, false});
            testCases.add(new Object[]{orderStatus, quoteRequired, qsQuote, "", false});
            testCases.add(new Object[]{orderStatus, quoteRequired, "", sapQuote, false});
            testCases.add(new Object[]{orderStatus, quoteRequired, sapQuote, "", false});
            testCases.add(new Object[]{orderStatus, quoteRequired, "", "", true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, "", qsQuote, false});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, qsQuote, "", false});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, "", sapQuote, false});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, sapQuote, "", false});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, "", "", true});
        });

        // These should always allow changing status
        statusesForChangeAllowed.forEach(orderStatus -> {
            assertThat(orderStatus.canPlace(), is(true));
            testCases.add(new Object[]{orderStatus, quoteRequired, sapQuote, qsQuote, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, qsQuote, sapQuote, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, sapQuote, sapQuote, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, qsQuote, qsQuote, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, sapQuote, sapQuote2, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, qsQuote, qsQuote2, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, sapQuote, qsQuote, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, qsQuote, sapQuote, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, sapQuote, sapQuote, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, qsQuote, qsQuote, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, sapQuote, sapQuote2, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, qsQuote, qsQuote2, true});

            testCases.add(new Object[]{orderStatus, quoteRequired, null, qsQuote, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, qsQuote, null, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, null, sapQuote, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, sapQuote, null, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, null, null, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, null, qsQuote, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, qsQuote, null, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, null, sapQuote, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, sapQuote, null, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, null, null, true});

            testCases.add(new Object[]{orderStatus, quoteRequired, "", qsQuote, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, qsQuote, "", true});
            testCases.add(new Object[]{orderStatus, quoteRequired, "", sapQuote, true});
            testCases.add(new Object[]{orderStatus, quoteRequired, sapQuote, "", true});
            testCases.add(new Object[]{orderStatus, quoteRequired, "", "", true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, "", qsQuote, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, qsQuote, "", true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, "", sapQuote, true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, sapQuote, "", true});
            testCases.add(new Object[]{orderStatus, noQuoteRequired, "", "", true});
        });
        return testCases.iterator();
    }

    @Test(dataProvider = "changeQuoteDataProvider")
    public void testChangeQuote(ProductOrder.OrderStatus orderStatus, boolean quoteRequired, String oldQuote,
                                String newQuote, boolean canChange) throws InvalidProductException {
        pdo.setOrderStatus(orderStatus);
        Product product = Mockito.mock(Product.class);
        Mockito.when(product.getSupportsSkippingQuote()).thenReturn(true);
        pdo.setProduct(product);
        if (!quoteRequired) {
            pdo.setSkipQuoteReason("quote not required");
        }
        assertThat(ProductOrderActionBean.canChangeQuote(pdo, oldQuote, newQuote), is(canChange));
    }

    @Test
    public void testDetermineOrderType() throws InvalidProductException {
        Product product = createSimpleProduct("1234",ProductFamily.WHOLE_GENOME_GENOTYPING);
        product.setExternalOnlyProduct(true);
        product.setClinicalProduct(true);
        pdo.setQuoteId("12345");
        pdo.setProduct(product);
        try {
                ProductOrder.determineOrderType(pdo, ProductOrder.OrderAccessType.BROAD_PI_ENGAGED_WORK.getSalesOrg());
                Assert.fail("An exception should have been thrown");
        } catch (Exception e) {
            assertThat(e.getMessage(), is(ProductOrder.QUOTES_CANNOT_BE_USED_FOR_COMMERCIAL_OR_CLINICAL_PRODUCTS));
        }
    }
}
