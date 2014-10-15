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
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.StripesMockTestUtils;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.EasyMock;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
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

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderActionBeanTest {

    private ProductOrderActionBean actionBean;

    private JSONObject jsonObject;

    private double expectedNumericValue = 9.3;

    private String expectedNonNumericRinScore = "I'm not a number";

    private ProductOrder pdo;

    public static final long BSP_INFORMATICS_TEST_SITE_ID = 1l;
    public static final long HOMO_SAPIENS = 1l;
    public static final long TEST_COLLECTION = 1062L;


    @BeforeMethod
    private void setUp() {
        actionBean = new ProductOrderActionBean();
        actionBean.setContext(new CoreActionBeanContext());
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
        pdo.setProduct(productThatHasRinRisk);
    }

    private void setNonRinRiskProduct(ProductOrder pdo) {
        pdo.setProduct(new Product());
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
        pdo.setProduct(product);
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
        pdo.setProduct(product);
        pdo.setProductOrderKit(createGoodPdoKit());
        actionBean.setEditOrder(pdo);
        actionBean.validateTransferMethod(pdo);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
    }

    public void testPostReceiveOptionKeys() {
        Product product = new Product();
        pdo.setProduct(product);
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

    private Product createSimpleProduct(String productPartNumber, String family) {
        Product product = new Product();
        product.setPartNumber(productPartNumber);
        product.setProductFamily(new ProductFamily(family));
        return product;
    }

    @Test(enabled = false)
    public void testQuoteOptOutAjaxCallStripes() throws Exception {
        Product product = createSimpleProduct("P-EX-0001",
                ProductFamily.ProductFamilyName.SAMPLE_INITIATION_QUALIFICATION_CELL_CULTURE.getFamilyName());
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
                ProductFamily.ProductFamilyName.SAMPLE_INITIATION_QUALIFICATION_CELL_CULTURE.getFamilyName());
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
                {ProductOrderActionBean.SAVE_ACTION, null, "", true, "Saving any order should succeed."},
                {ProductOrderActionBean.SAVE_ACTION, "", "", true, "Saving any order should succeed."},
                {ProductOrderActionBean.PLACE_ORDER, null, "", false, "No Quote and No reason should fail."},
                {ProductOrderActionBean.SAVE_ACTION, null, testReason, true, "Saving any order should succeed."},
                {ProductOrderActionBean.PLACE_ORDER, null, testReason, true,
                        "No Quote but with reason should succeed."},
                {ProductOrderActionBean.SAVE_ACTION, testQuote, "", true, "Saving any order should succeed."},
                {ProductOrderActionBean.SAVE_ACTION, testQuote, null, true, "Saving any order should succeed."},
                {ProductOrderActionBean.PLACE_ORDER, testQuote, "", true,
                        "A good quote but blank reason should succeed."},
                {ProductOrderActionBean.PLACE_ORDER, testQuote, null, true,
                        "A good quote but null reason should succeed."},
                {ProductOrderActionBean.SAVE_ACTION, testQuote, testReason, true, "Saving any order should succeed."},
                {ProductOrderActionBean.PLACE_ORDER, testQuote, testReason, true,
                        "A good quote and a reason should succeed."},
                {ProductOrderActionBean.VALIDATE_ORDER, testQuote, testReason, true,
                        "A good quote and a reason should succeed."},
                {ProductOrderActionBean.VALIDATE_ORDER, null, testReason, true,
                        "A good quote and a reason should succeed."},
                {ProductOrderActionBean.VALIDATE_ORDER, null, null, false, "No quote or reason should fail."}
        };
    }

    @Test(dataProvider = "quoteOptionsDataProvider")
    public void testQuoteSkippingValidation(String action, String quoteId, String reason,
                                            boolean expectedToPassValidation, String testErrorMessage) {
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
        actionBean.getEditOrder().setProduct(dummyProduct);
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
        for (String action : Arrays.asList(ProductOrderActionBean.PLACE_ORDER, ProductOrderActionBean.VALIDATE_ORDER)) {
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
                ProductOrderActionBean.PLACE_ORDER)) {
            actionBean.validateRegulatoryInformation(action);
            Assert.assertTrue(actionBean.getValidationErrors().isEmpty(), "Validation failed for " + action);
        }
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
}
