package org.broadinstitute.gpinformatics.athena.presentation.orders;


import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.MaterialInfo;
import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;
import org.apache.commons.lang.StringUtils;
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductOrderJiraUtil;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
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
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
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
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.squid.SquidConnector;
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
import org.broadinstitute.sap.entity.OrderCalculatedValues;
import org.broadinstitute.sap.entity.OrderValue;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.EasyMock;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
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

    private QuoteService mockQuoteService;
    public SapIntegrationService mockSAPService;
    public SAPProductPriceCache stubProductPriceCache;
    public ProductOrderDao mockProductOrderDao;
    public ProductOrder testOrder;


    @BeforeMethod
    private void setUp() {
        actionBean = new ProductOrderActionBean();
        actionBean.setContext(new CoreActionBeanContext());

        mockQuoteService = Mockito.mock(QuoteServiceImpl.class);
        priceListCache = new PriceListCache(mockQuoteService);
        actionBean.setPriceListCache(priceListCache);
        mockSAPService = Mockito.mock(SapIntegrationService.class);
        stubProductPriceCache = new SAPProductPriceCache(mockSAPService);
        final SAPAccessControlEjb mockAccessController = Mockito.mock(SAPAccessControlEjb.class);
        Mockito.when(mockAccessController.getCurrentControlDefinitions()).thenReturn(new SAPAccessControl());
        stubProductPriceCache.setAccessControlEjb(mockAccessController);
        actionBean.setProductPriceCache(stubProductPriceCache);

        mockProductOrderDao = Mockito.mock(ProductOrderDao.class);
        productOrderEjb = new ProductOrderEjb(mockProductOrderDao, Mockito.mock(ProductDao.class),
                mockQuoteService, Mockito.mock(JiraService.class),Mockito.mock(UserBean.class),
                Mockito.mock(BSPUserList.class),Mockito.mock(BucketEjb.class),Mockito.mock(SquidConnector.class),
                Mockito.mock(MercurySampleDao.class),Mockito.mock(ProductOrderJiraUtil.class), mockSAPService,priceListCache,
                stubProductPriceCache);

        productOrderEjb.setAccessController(mockAccessController);

        actionBean.setProductOrderEjb(productOrderEjb);
        actionBean.setProductOrderDao(mockProductOrderDao);
        actionBean.setSapService(mockSAPService);

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

        Iterator<JsonNode> resultIterator = jsonNode.get("dataList").getElements();
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

    public void testQuoteOptOutAllowed() throws Exception {
        Product product = createSimpleProduct("P-EX-0001",
                ProductFamily.ProductFamilyInfo.SAMPLE_INITIATION_QUALIFICATION_CELL_CULTURE.getFamilyName());
        ProductDao productDao = setupMockProductDao(product);

        MockRoundtrip roundtrip = StripesMockTestUtils.createMockRoundtrip(ProductOrderActionBean.class, productDao);
        roundtrip.addParameter("product", product.getPartNumber());
        roundtrip.execute("getSupportsSkippingQuote");
        Assert.assertEquals(roundtrip.getResponse().getOutputString(), "{\"supportsSkippingQuote\":true}");
    }

    public void testQuoteOptOutNotAllowed() throws Exception {
        Product product = createSimpleProduct("P-EX-0001", "Some product family that doesn't support optional quotes");
        ProductDao productDao = setupMockProductDao(product);

        MockRoundtrip roundtrip = StripesMockTestUtils.createMockRoundtrip(ProductOrderActionBean.class, productDao);
        roundtrip.addParameter("product", product.getPartNumber());
        roundtrip.execute("getSupportsSkippingQuote");
        Assert.assertEquals(roundtrip.getResponse().getOutputString(), "{\"supportsSkippingQuote\":false}");
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
        pdo.setQuoteSource(quoteSourceType);
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
        actionBean.setSkipRegulatoryInfo(skipRegulatory);
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
            "test description", "2000", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", null,
            null, null, null, Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, ""));

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
                    SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                    Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, ""));

        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, addonNonSeqProduct, "1573", "2000", "573"
        );


        testOrder.updateAddOnProducts(Collections.singletonList(addonNonSeqProduct));

        List<ProductOrderSample> sampleList = new ArrayList<>();

        for (int i = 0; i < 75;i++) {
            sampleList.add(new ProductOrderSample("SM-Test"+i));
        }

        testOrder.setSamples(sampleList);
        testOrder.setLaneCount(5);


        Product seqProduct = new Product();
        seqProduct.setPartNumber("ADD-SEQ");
        seqProduct.setPrimaryPriceItem(new PriceItem("Third", "Genomics Platform", "Seq Testing Size",
                "Put it on the sequencer"));
        seqProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.SEQUENCE_ONLY.getFamilyName()));

        final SAPMaterial seqMaterial= new SAPMaterial(seqProduct.getPartNumber(), broad, broad.getDefaultWbs(), "test description", "3500",
                                    SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                                    Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        returnMaterials.add(seqMaterial);


        final SAPMaterial primaryMaterial =
            new SAPMaterial(testOrder.getProduct().getPartNumber(), broad, broad.getDefaultWbs(), "test description", "2000",
                                        SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                                        Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        returnMaterials.add(primaryMaterial);
        final SAPMaterial nonSeqMaterial =
            new SAPMaterial(addonNonSeqProduct.getPartNumber(), broad, broad.getDefaultWbs(), "test description",
                "1573", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        returnMaterials.add(nonSeqMaterial);


        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, seqProduct, "3500", "2000", "2500"
        );
        Mockito.when(mockSAPService.findProductsInSap()).thenReturn(returnMaterials);
        stubProductPriceCache.refreshCache();
        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);

        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote, Collections.<String>emptySet()),
                (double) (1573 * testOrder.getSamples().size() + 2000 * testOrder.getSamples().size()));
        testQuote.setQuoteItems(quoteItems);

        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote, Collections.<String>emptySet()),
                (double) (573 * testOrder.getSamples().size() + 1000 * testOrder.getSamples().size()));

        testOrder.updateAddOnProducts(Arrays.asList(addonNonSeqProduct, seqProduct));

        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote, Collections.<String>emptySet()),
                (double) (573 * testOrder.getSamples().size() + 1000 * testOrder.getSamples().size() + 2500 * testOrder
                        .getLaneCount()));

        testOrder.setProduct(seqProduct);

        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote, Collections.<String>emptySet()),
                (double) (573 * testOrder.getSamples().size() + 2500 * testOrder.getLaneCount() + 2500 * testOrder
                        .getLaneCount()));

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 75);
        ProductOrderSample abandonedSample = testOrder.getSamples().get(0);
        abandonedSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);
        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote, Collections.<String>emptySet()),
                (double) (573 * (testOrder.getSamples().size() - 1) + 2500 * testOrder.getLaneCount() + 2500 * testOrder
                        .getLaneCount()));


        ProductOrder testChildOrder = new ProductOrder();
        testChildOrder.setJiraTicketKey("PDO-ChildTestValue");

        testChildOrder.setSamples(Collections.singletonList(new ProductOrderSample("SM-TestChild1")));
        testOrder.addChildOrder(testChildOrder);

        Assert.assertEquals(testChildOrder.getUnbilledSampleCount(), 1);
        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);

        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote, Collections.<String>emptySet()),
                (double) (573 * (testOrder.getSamples().size() - 1) + 2500 * testOrder.getLaneCount() + 2500 * testOrder
                        .getLaneCount()
                ));

        testChildOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        Assert.assertEquals(actionBean.getValueOfOpenOrders(Collections.singletonList(testOrder), testQuote, Collections.<String>emptySet()),
                (double) (573 * (testOrder.getSamples().size() - 1) + 2500 * testOrder.getLaneCount() + 2500 * testOrder
                        .getLaneCount()
                          + 573 * testChildOrder.getSamples().size()
                ));
    }

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
        testOrder.setLaneCount(5);

        SapOrderDetail sapReference = new SapOrderDetail("test001", 75, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode(),"","");
        testOrder.addSapOrderDetail(sapReference);



        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();


        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, testOrder.getProduct(), "2000", "2000",
                "1000");
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, addonNonSeqProduct, "1573", "2000", "573");
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, seqProduct, "3500", "2000", "2500");
        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;

        final SAPMaterial primaryMaterial =
            new SAPMaterial(testOrder.getProduct().getPartNumber(), broad, broad.getDefaultWbs(), "test description",
                "2000",
                SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");

        returnMaterials.add(primaryMaterial);
        final SAPMaterial nonSeqMaterial =
            new SAPMaterial(addonNonSeqProduct.getPartNumber(), broad, broad.getDefaultWbs(), "test description",
                            "1573", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                            Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        returnMaterials.add(nonSeqMaterial);
        final SAPMaterial seqMaterial = new SAPMaterial(seqProduct.getPartNumber(), broad, broad.getDefaultWbs(), "test description", "3500",
                                            SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                                            Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        returnMaterials.add(seqMaterial);


        Mockito.when(mockSAPService.findProductsInSap()).thenReturn(returnMaterials);
        stubProductPriceCache.refreshCache();
        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuoteIdentifier)).thenReturn(testQuote);

        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn(Collections.singletonList(testOrder));
        Mockito.when(mockSAPService.calculateOpenOrderValues(Mockito.anyInt(),
                Mockito.anyString(), Mockito.any(ProductOrder.class)
        )).thenReturn(new OrderCalculatedValues(
                BigDecimal.ZERO, Collections.<OrderValue>emptySet()));

        actionBean.setEditOrder(testOrder);


        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote,0, null),
                (double) (1573 * testOrder.getSamples().size() + 2000 * testOrder.getSamples().size()));
        testQuote.setQuoteItems(quoteItems);

        Assert.assertEquals(actionBean.estimateOutstandingOrders( testQuote,0, null),
                (double) (573 * testOrder.getSamples().size() + 1000 * testOrder.getSamples().size()));

        testOrder.updateAddOnProducts(Arrays.asList(addonNonSeqProduct, seqProduct));

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote,0, null),
                (double) (573 * testOrder.getSamples().size() + 1000 * testOrder.getSamples().size() + 2500 * testOrder
                        .getLaneCount()));

        testOrder.setProduct(seqProduct);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote,0, null),
                (double) (573 * testOrder.getSamples().size() + 2500 * testOrder.getLaneCount() + 2500 * testOrder
                        .getLaneCount()));

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 75);
        ProductOrderSample abandonedSample = testOrder.getSamples().get(0);
        abandonedSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);
        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                (double) (573 * (testOrder.getSamples().size() - 1) + 2500 * testOrder.getLaneCount() + 2500 * testOrder
                        .getLaneCount()));


        ProductOrder testChildOrder = new ProductOrder();
        testChildOrder.setJiraTicketKey("PDO-ChildTestValue");

        testChildOrder.setSamples(Collections.singletonList(new ProductOrderSample("SM-TestChild1")));
        testOrder.addChildOrder(testChildOrder);

        Assert.assertEquals(testChildOrder.getUnbilledSampleCount(), 1);
        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                (double) (573 * (testOrder.getSamples().size() - 1) + 2500 * testOrder.getLaneCount() + 2500 * testOrder
                        .getLaneCount()
                ));

        testChildOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                (double) (573 * (testOrder.getSamples().size() - 1) + 2500 * testOrder.getLaneCount() + 2500 * testOrder
                        .getLaneCount()
                          + 573 * testChildOrder.getSamples().size()
                ));
    }


    public void testEstimateSomeSAPOrders() throws Exception {
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

        testOrder = new ProductOrder();
        testOrder.setJiraTicketKey("PDO-TESTPDOValue");
        testOrder.setProduct(primaryProduct);
        testOrder.setQuoteId(testQuoteIdentifier);
        testOrder.updateAddOnProducts(Collections.singletonList(addonNonSeqProduct));

        List<ProductOrderSample> sampleList = new ArrayList<>();

        for (int i = 0; i < 75;i++) {
            sampleList.add(new ProductOrderSample("SM-Test"+i));
        }

        testOrder.setSamples(sampleList);
        testOrder.setLaneCount(5);

        SapOrderDetail sapReference = new SapOrderDetail("test001", 75, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode(),"","");
        testOrder.addSapOrderDetail(sapReference);



        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();


        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, testOrder.getProduct(), "2000", "2000",
                "1000");
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, addonNonSeqProduct, "1573", "2000", "573"
        );
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, seqProduct, "3500", "2000", "2500"
        );

        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        final SAPMaterial primaryMaterial =
            new SAPMaterial(testOrder.getProduct().getPartNumber(), broad, broad.getDefaultWbs(), "test description",
                "2000", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        returnMaterials.add(primaryMaterial);
        final SAPMaterial nonSeqMaterial =
            new SAPMaterial(addonNonSeqProduct.getPartNumber(), broad, broad.getDefaultWbs(), "test description",
                "1573", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        returnMaterials.add(nonSeqMaterial);
        final SAPMaterial seqMaterial =
            new SAPMaterial(seqProduct.getPartNumber(), broad, broad.getDefaultWbs(), "test description", "3500",
                SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        returnMaterials.add(seqMaterial);

        Mockito.when(mockSAPService.findProductsInSap()).thenReturn(returnMaterials);
        stubProductPriceCache.refreshCache();
        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuoteIdentifier)).thenReturn(testQuote);

        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn(Collections.singletonList(
                testOrder));

        final Set<OrderValue> sapOrderValues = new HashSet<>();
        sapOrderValues.add(new OrderValue("Test_listed_1", BigDecimal.TEN));
        sapOrderValues.add(new OrderValue("Test_listed_2", new BigDecimal(23)));
        sapOrderValues.add(new OrderValue("Test_listed_3", new BigDecimal(49)));

        final OrderCalculatedValues testCalculatedValues = new OrderCalculatedValues(
                BigDecimal.ZERO, sapOrderValues);

        Mockito.when(mockSAPService.calculateOpenOrderValues(Mockito.anyInt(),
                Mockito.anyString(), Mockito.any(ProductOrder.class)
        )).thenReturn(
                testCalculatedValues);

        actionBean.setEditOrder(testOrder);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                (double) (1573 * testOrder.getSamples().size() + 2000 * testOrder.getSamples().size()) + 82);
        testQuote.setQuoteItems(quoteItems);

        Assert.assertEquals(actionBean.estimateOutstandingOrders( testQuote, 0, null),
                (double) (573 * testOrder.getSamples().size() + 1000 * testOrder.getSamples().size()) + 82);

        testOrder.updateAddOnProducts(Arrays.asList(addonNonSeqProduct, seqProduct));

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                (double) (573 * testOrder.getSamples().size() + 1000 * testOrder.getSamples().size() + 2500 * testOrder
                        .getLaneCount()) + 82);

        testOrder.setProduct(seqProduct);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                (double) (573 * testOrder.getSamples().size() + 2500 * testOrder.getLaneCount() + 2500 * testOrder
                        .getLaneCount()) + 82);

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 75);
        ProductOrderSample abandonedSample = testOrder.getSamples().get(0);
        abandonedSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);
        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                (double) (573 * (testOrder.getSamples().size() - 1) + 2500 * testOrder.getLaneCount() + 2500 * testOrder
                        .getLaneCount()) + 82);


        ProductOrder testChildOrder = new ProductOrder();
        testChildOrder.setJiraTicketKey("PDO-ChildTestValue");

        testChildOrder.setSamples(Collections.singletonList(new ProductOrderSample("SM-TestChild1")));
        testOrder.addChildOrder(testChildOrder);

        Assert.assertEquals(testChildOrder.getUnbilledSampleCount(), 1);
        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                (double) (573 * (testOrder.getSamples().size() - 1) + 2500 * testOrder.getLaneCount() + 2500 * testOrder
                        .getLaneCount()
                ) + 82);

        testChildOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, null),
                (double) (573 * (testOrder.getSamples().size() - 1) + 2500 * testOrder.getLaneCount() + 2500 * testOrder
                        .getLaneCount()
                          + 573 * testChildOrder.getSamples().size()
                ) + 82);
    }

    public void testEstimateSAPOrdersWithUpdateCurrentSapOrder() throws Exception {
        final String testQuoteIdentifier = "testQuote";
        Quote testQuote = buildSingleTestQuote(testQuoteIdentifier, "71000");

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

        testOrder = new ProductOrder();
        testOrder.setJiraTicketKey("PDO-TESTPDOValue");
        testOrder.setProduct(primaryProduct);
        testOrder.setQuoteId(testQuoteIdentifier);
        testOrder.updateAddOnProducts(Collections.singletonList(addonNonSeqProduct));

        List<ProductOrderSample> sampleList = new ArrayList<>();

        for (int i = 0; i < 75;i++) {
            sampleList.add(new ProductOrderSample("SM-Test"+i));
        }

        testOrder.setSamples(sampleList);
        testOrder.setLaneCount(5);

        SapOrderDetail sapReference = new SapOrderDetail("test001", 75, testOrder.getQuoteId(),
                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode(),"","");
        testOrder.addSapOrderDetail(sapReference);



        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();


        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, testOrder.getProduct(), "2000", "2000",
                "1000");
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, addonNonSeqProduct, "1573", "2000", "573");
        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, seqProduct, "3500", "2000", "2500");

        SapIntegrationClientImpl.SAPCompanyConfiguration broad = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;

        final SAPMaterial primaryMaterial =
            new SAPMaterial(testOrder.getProduct().getPartNumber(), broad, broad.getDefaultWbs(), "test description",
                "2000",
                SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        returnMaterials.add(primaryMaterial);
        final SAPMaterial nonSeqMaterial =
            new SAPMaterial(addonNonSeqProduct.getPartNumber(), broad, broad.getDefaultWbs(), "test description",
                            "1573", SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                            Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        returnMaterials.add(nonSeqMaterial);
        final SAPMaterial seqMaterial =
            new SAPMaterial(seqProduct.getPartNumber(), broad, broad.getDefaultWbs(), "test description", "3500",
                SAPMaterial.DEFAULT_UNIT_OF_MEASURE_EA, BigDecimal.ONE, "description", "", "", new Date(), new Date(),
                Collections.emptyMap(), Collections.emptyMap(), SAPMaterial.MaterialStatus.ENABLED, "");
        returnMaterials.add(seqMaterial);

        Mockito.when(mockSAPService.findProductsInSap()).thenReturn(returnMaterials);
        stubProductPriceCache.refreshCache();
        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuoteIdentifier)).thenReturn(testQuote);

        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn(Collections.singletonList(
                testOrder));

        final Set<OrderValue> sapOrderValues = new HashSet<>();
        sapOrderValues.add(new OrderValue("Test_listed_1", BigDecimal.TEN));
        sapOrderValues.add(new OrderValue("Test_listed_2", new BigDecimal(23)));
        sapOrderValues.add(new OrderValue("Test_listed_3", new BigDecimal(49)));

        final int overrideCalculatedOrderValue = 70000;
        sapOrderValues.add(new OrderValue("test001", new BigDecimal(overrideCalculatedOrderValue)));

        final OrderCalculatedValues testCalculatedValues = new OrderCalculatedValues(
                new BigDecimal(overrideCalculatedOrderValue), sapOrderValues);

        Mockito.when(mockSAPService.calculateOpenOrderValues(Mockito.anyInt(),
                Mockito.anyString(), Mockito.any(ProductOrder.class)
        )).thenReturn(
                testCalculatedValues);

        actionBean.setEditOrder(testOrder);


        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0,
                testOrder), (double) (overrideCalculatedOrderValue + 82));
        testQuote.setQuoteItems(quoteItems);

        Assert.assertEquals(actionBean.estimateOutstandingOrders( testQuote, 0, testOrder), (double) overrideCalculatedOrderValue + 82);

        testOrder.updateAddOnProducts(Arrays.asList(addonNonSeqProduct, seqProduct));

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder), (double) overrideCalculatedOrderValue + 82);

        testOrder.setProduct(seqProduct);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder), (double) overrideCalculatedOrderValue + 82);

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 75);
        ProductOrderSample abandonedSample = testOrder.getSamples().get(0);
        abandonedSample.setDeliveryStatus(ProductOrderSample.DeliveryStatus.ABANDONED);

        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);
        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder), (double) overrideCalculatedOrderValue + 82);


        ProductOrder testChildOrder = new ProductOrder();
        testChildOrder.setJiraTicketKey("PDO-ChildTestValue");

        testChildOrder.setSamples(Collections.singletonList(new ProductOrderSample("SM-TestChild1")));
        testOrder.addChildOrder(testChildOrder);

        Assert.assertEquals(testChildOrder.getUnbilledSampleCount(), 1);
        Assert.assertEquals(testOrder.getUnbilledSampleCount(), 74);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder), (double) overrideCalculatedOrderValue + 82);

        testChildOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

        Assert.assertEquals(actionBean.estimateOutstandingOrders(testQuote, 0, testOrder), (double) overrideCalculatedOrderValue + 82);

        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());

        actionBean.validateQuoteDetails(testQuote, 0);

        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());

    }


    public void testEstimateCustomHigherThanQuote() throws Exception {

        final String testQuoteIdentifier = "testQuote";
        Quote testQuote = buildSingleTestQuote(testQuoteIdentifier, "12000");

        Product primaryProduct = new Product();
        primaryProduct.setPartNumber("P-Test_primary");
        primaryProduct.setPrimaryPriceItem(new PriceItem("primary", "Genomics Platform", "Primary testing size",
                "Thousand dollar Genome price"));
        primaryProduct.setProductFamily(new ProductFamily(ProductFamily.ProductFamilyInfo.WHOLE_GENOME.getFamilyName()));


        testOrder = new ProductOrder();
        testOrder.setJiraTicketKey("PDO-TESTPDOValue");
        testOrder.setProduct(primaryProduct);
        testOrder.setQuoteId(testQuoteIdentifier);
        List<ProductOrderSample> sampleList = new ArrayList<>();

        for (int i = 0; i < 75;i++) {
            sampleList.add(new ProductOrderSample("SM-Test"+i));
        }

        testOrder.setSamples(sampleList);
//        SapOrderDetail sapReference = new SapOrderDetail("test001", 75, testOrder.getQuoteId(),
//                SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD.getCompanyCode(),"","");
//        testOrder.addSapOrderDetail(sapReference);

        PriceList priceList = new PriceList();
        Collection<QuoteItem> quoteItems = new HashSet<>();
        Set<SAPMaterial> returnMaterials = new HashSet<>();


        addPriceItemForProduct(testQuoteIdentifier, priceList, quoteItems, testOrder.getProduct(), "2000", "2000",
                "2000");

        Mockito.when(mockSAPService.findProductsInSap()).thenReturn(returnMaterials);
        stubProductPriceCache.refreshCache();
        Mockito.when(mockQuoteService.getAllPriceItems()).thenReturn(priceList);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(testQuoteIdentifier)).thenReturn(testQuote);

        Mockito.when(mockProductOrderDao.findOrdersWithCommonQuote(Mockito.anyString())).thenReturn(Collections.singletonList(
                testOrder));

        testOrder.addCustomPriceAdjustment(new ProductOrderPriceAdjustment(new BigDecimal(160.00),null, null));

        actionBean.validateQuoteDetails(testQuote, 0);

        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
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
        testOrder.setQuoteSource(ProductOrder.QuoteSourceType.QUOTE_SERVER);
        FundingLevel fundingLevel = new FundingLevel();
        Funding funding = new Funding(Funding.FUNDS_RESERVATION, "test", "c333");
        funding.setGrantNumber("1234");
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
        actionBean.setQuoteService(mockQuoteService);
        actionBean.setQuoteSource(ProductOrder.QuoteSourceType.QUOTE_SERVER.getDisplayName());

        JSONObject quoteFundingJson = actionBean.getQuoteFundingJson();
        JSONObject fundingDetails = (JSONObject) quoteFundingJson.getJSONArray("fundingDetails").get(0);

        assertThat(fundingDetails.get("grantTitle"), equalTo("CO-1234"));
        assertThat(fundingDetails.get("grantEndDate"), equalTo(DateUtils.getDate(oneWeek)));
        assertThat(fundingDetails.get("activeGrant"), is(true));
        assertThat(fundingDetails.get("daysTillExpire"), equalTo(7));
    }

    public void testQuoteOptionsPurchaseOrder() throws Exception {
        String quoteId = "DNA4JD";
        testOrder = new ProductOrder();
        testOrder.setQuoteSource(ProductOrder.QuoteSourceType.QUOTE_SERVER);
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
        actionBean.setQuoteService(mockQuoteService);
        actionBean.setQuoteSource(ProductOrder.QuoteSourceType.QUOTE_SERVER.getDisplayName());

        JSONObject quoteFundingJson = actionBean.getQuoteFundingJson();
        JSONArray fundingDetails = quoteFundingJson.getJSONArray("fundingDetails");
        assertThat(quoteFundingJson.getString("key"), is(quoteId));
        assertThat(quoteFundingJson.getString("fundsRemaining"), equalTo("$100.00"));
        assertThat(quoteFundingJson.getString("status"), equalTo("Funded"));

        // Purchase Orders do not have funding details
        assertThat(fundingDetails.length(), is(0));

    }

    public void testQuoteOptionsNoFunding() throws Exception {
        String quoteId = "DNA4JD";
        testOrder = new ProductOrder();
        testOrder.setQuoteSource(ProductOrder.QuoteSourceType.QUOTE_SERVER);
        FundingLevel fundingLevel = new FundingLevel();
        Funding funding = new Funding();
        fundingLevel.setFunding(Collections.singleton(funding));
        QuoteFunding quoteFunding = new QuoteFunding();
        Quote testQuote = buildSingleTestQuote(quoteId, "2");
        testQuote.setQuoteFunding(quoteFunding);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(quoteId)).thenReturn(testQuote);
        actionBean.setQuoteIdentifier(quoteId);
        actionBean.setQuoteService(mockQuoteService);
        actionBean.setQuoteSource(ProductOrder.QuoteSourceType.QUOTE_SERVER.getDisplayName());

        JSONObject quoteFundingJson = actionBean.getQuoteFundingJson();
        assertThat(quoteFundingJson.getString("error"), is("Unable to complete evaluating order values:  null"));
        assertThat(quoteFundingJson.getString("key"), equalTo(quoteId));
     }

}
