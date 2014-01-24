package org.broadinstitute.gpinformatics.athena.presentation.orders;


import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.MaterialInfo;
import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpRequest;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.presentation.MockStripesActionRunner;
import org.broadinstitute.gpinformatics.athena.presentation.ResolutionCallback;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.EasyMock;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.jetty.http.HttpResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductOrderActionBeanTest {

    private ProductOrderActionBean actionBean;

    private JSONObject jsonObject;

    private double expectedNumericValue = 9.3;

    private String expectedNonNumericRinScore = "I'm not a number";

    private ProductOrder pdo;

    public static final long BSP_INFORMATICS_TEST_SITE_ID = 1l;
    public static final long HOMO_SAPIENS = 1l;
    public static final long TEST_COLLECTION = 1062L;


    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    private void setUp() {
        actionBean = new ProductOrderActionBean();
        actionBean.setContext(new CoreActionBeanContext());
        jsonObject = new JSONObject();
        pdo = newPdo();
    }

    /**
     * Creates a basic PDO with a few samples.
     * Setting the product is left to individual tests.
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
     * @return
     */
    private Collection<ProductOrderSample> createPdoSamples() {
        List<ProductOrderSample> pdoSamples = new ArrayList<> ();
        BSPSampleDTO sampleWithGoodRin = getSampleDTOWithGoodRinScore();
        BSPSampleDTO sampleWithBadRin = getSamplDTOWithBadRinScore();
        pdoSamples.add(new ProductOrderSample(sampleWithGoodRin.getSampleId(),sampleWithGoodRin));
        pdoSamples.add(new ProductOrderSample(sampleWithBadRin.getSampleId(),sampleWithBadRin));
        pdoSamples.add(new ProductOrderSample("123.0")); // throw in a gssr sample
        return pdoSamples;
    }

    private ProductOrderKit createGoodPdoKit() {
        MaterialInfoDto materialInfoDto =
                new MaterialInfoDto(KitType.DNA_MATRIX.getKitName(), KitType.DNA_MATRIX.getDisplayName());
        ProductOrderKit pdoKit = new ProductOrderKit(96l, KitType.DNA_MATRIX, TEST_COLLECTION, HOMO_SAPIENS,
                BSP_INFORMATICS_TEST_SITE_ID, materialInfoDto);
        pdoKit.getPostReceiveOptions().add(PostReceiveOption.FLUIDIGM_FINGERPRINTING);
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
                new RiskCriterion(RiskCriterion.RiskCriteriaType.RIN, Operator.LESS_THAN,"6.0")
        );
        pdo.setProduct(productThatHasRinRisk);
    }

    private void setNonRinRiskProduct(ProductOrder pdo) {
        pdo.setProduct(new Product());
    }

    /**
     * Tests that non-numeric RIN scores
     * are turned into "N/A" by the action bean
     * @throws JSONException
     */
    @Test(groups = TestGroups.DATABASE_FREE)
    public void testNonNumericRinScore() throws JSONException {
        jsonObject.put(BSPSampleDTO.JSON_RIN_KEY, getSamplDTOWithBadRinScore().getRinScore());
        Assert.assertEquals(jsonObject.get(BSPSampleDTO.JSON_RIN_KEY), expectedNonNumericRinScore);
    }

    /**
     * Tests that numeric RIN scores
     * are handled as real numbers by the action bean
     * @throws JSONException
     */
    @Test(groups = TestGroups.DATABASE_FREE)
    public void testNumericRinScore() throws JSONException {
        jsonObject.put(BSPSampleDTO.JSON_RIN_KEY, getSampleDTOWithGoodRinScore().getRinScore());
        Assert.assertEquals(Double.parseDouble((String) jsonObject.get(BSPSampleDTO.JSON_RIN_KEY)),
                expectedNumericValue);
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testValidateRinScoresWhenProductHasRinRisk() {
        setRinRiskProduct(pdo);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
        actionBean.validateRinScores(pdo);
        Assert.assertEquals(actionBean.getContext().getValidationErrors().size(), 1);
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testValidateRinScoresWhenProductHasNoRinRisk() {
        setNonRinRiskProduct(pdo);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
        actionBean.validateRinScores(pdo);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testIsRinScoreValidationRequired() {
        setRinRiskProduct(pdo);
        Assert.assertTrue(pdo.isRinScoreValidationRequired());
        setNonRinRiskProduct(pdo);
        Assert.assertFalse(pdo.isRinScoreValidationRequired());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testCanBadRinScoreBeUsedForOnRiskCalculation() {
        BSPSampleDTO badRinScoreSample = getSamplDTOWithBadRinScore();
        Assert.assertFalse(badRinScoreSample.canRinScoreBeUsedForOnRiskCalculation());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testCanGoodRinScoreBeUsedForOnRiskCalculation() {
        BSPSampleDTO goodRinScoreSample = getSampleDTOWithGoodRinScore();
        Assert.assertTrue(goodRinScoreSample.canRinScoreBeUsedForOnRiskCalculation());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
        public void testPostReceiveOptions() throws Exception {
        Product product = new Product();
        pdo.setProduct(product);
        pdo.setProductOrderKit(createGoodPdoKit());
        actionBean.setEditOrder(pdo);
        actionBean.setProduct("test product");
        actionBean.setMaterialInfo(MaterialInfo.DNA_DERIVED_FROM_BLOOD.getText());

        StreamingResolution postReceiveOptions = (StreamingResolution) actionBean.getPostReceiveOptions();
        HttpServletRequest request = new MockHttpServletRequest("foo", "bar");
        MockHttpServletResponse response = new MockHttpServletResponse();

        postReceiveOptions.execute(request, response);
        String jsonString = response.getOutputString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readValue(jsonString, JsonNode.class);

        Iterator<JsonNode> resultIterator = jsonNode.getElements();
        String nodeKey = "key";
        String nodeValue = "value";

        while (resultIterator.hasNext()) {
            JsonNode node = resultIterator.next();
            PostReceiveOption option = TestUtils.getFirst(
                    PostReceiveOption.getByText(Arrays.asList(node.get(nodeKey).asText())));
            Assert.assertTrue(option.getDefaultToChecked() == node.get(nodeValue).asBoolean());
            Assert.assertFalse(option.getArchived());
        }
    }

    @Test(groups = TestGroups.DATABASE_FREE)
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

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testSampleKitNoValidationErrors() {
        Product product = new Product();
        pdo.setProduct(product);
        pdo.setProductOrderKit(createGoodPdoKit());
        actionBean.setEditOrder(pdo);
        actionBean.validateTransferMethod(pdo);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testPostReceiveOptionKeys() {
        Product product = new Product();
        pdo.setProduct(product);
        pdo.setProductOrderKit(createGoodPdoKit());
        actionBean.setEditOrder(pdo);
        Assert.assertTrue(actionBean.getPostReceiveOptionKeys().isEmpty());
        actionBean.initPostReceiveOptions();
        Assert.assertFalse(actionBean.getPostReceiveOptionKeys().isEmpty());
    }
    private BSPSampleDTO getSamplDTOWithBadRinScore() {
        Map<BSPSampleSearchColumn, String> dataMap = new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.RIN, expectedNonNumericRinScore);
            put(BSPSampleSearchColumn.SAMPLE_ID,"SM-49M5N");
        }};
        return new BSPSampleDTO(dataMap);
    }

    private BSPSampleDTO getSampleDTOWithGoodRinScore() {
        Map<BSPSampleSearchColumn, String> dataMap = new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.RIN, String.valueOf(expectedNumericValue));
            put(BSPSampleSearchColumn.SAMPLE_ID,"SM-99D2A");
        }};
        return new BSPSampleDTO(dataMap);
    }

    private Product createSimpleProduct(String productPartNumber,String family) {
        Product product = new Product();
        product.setPartNumber(productPartNumber);
        product.setProductFamily(new ProductFamily(family));
        return product;
    }

    @Test(groups = TestGroups.DATABASE_FREE, enabled = false)
    public void testQuoteOptOutAjaxCallStripes() throws Exception {
        Product product = createSimpleProduct("P-EX-0001",ProductFamily.INITIATION_FAMILY_NAME);
        ProductDao productDao = EasyMock.createNiceMock(ProductDao.class);
        EasyMock.expect(productDao.findByBusinessKey((String) EasyMock.anyObject())).andReturn(product).atLeastOnce();
        EasyMock.replay(productDao);

        MockServletContext ctx = new MockServletContext("mercury");
        Map<String,String> params = new HashMap<>();
        // values taken from our web.xml
        params.put("ActionResolver.Packages","org.broadinstitute.gpinformatics.mercury.presentation,org.broadinstitute.gpinformatics.athena.presentation");
        params.put("ActionBeanContext.Class","org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext");
        //params.put("Extension.Packages","com.samaxes.stripes.inject");
        ctx.addFilter(StripesFilter.class,"StripesFilter",params);
        ctx.setServlet(DispatcherServlet.class,"DispatcherServlet",null);
        MockHttpSession session = new MockHttpSession(ctx);

        MockRoundtrip roundtrip = new MockRoundtrip(ctx,ProductOrderActionBean.class,session);
        // we seem to have to make a call once to get a non-null mock bean
        roundtrip.execute("getSupportsSkippingQuote");
        ProductOrderActionBean mockActionBean = roundtrip.getActionBean(ProductOrderActionBean.class);
        // mockActionBean is not null

        mockActionBean.setProductDao(productDao);
        roundtrip.setParameter("product",product.getPartNumber());
        roundtrip.execute("getSupportsSkippingQuote");
        Assert.assertEquals(roundtrip.getOutputString(),"{\"supportsSkippingQuote\":true}");


        product.setProductFamily(new ProductFamily("Something that doesn't support optional quotes"));
        Assert.assertEquals(roundtrip.getOutputString(),"{\"supportsSkippingQuote\":false}");
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testQuoteOptOutAjaxCall() throws Exception {
        Product product = createSimpleProduct("P-EX-0001", ProductFamily.INITIATION_FAMILY_NAME);
        ProductDao productDao = EasyMock.createNiceMock(ProductDao.class);

        actionBean.setProduct(product.getPartNumber());
        actionBean.setProductDao(productDao);

        EasyMock.expect(productDao.findByBusinessKey((String) EasyMock.anyObject())).andReturn(product).atLeastOnce();
        EasyMock.replay(productDao);

        ResolutionCallback resolutionCallback = new ResolutionCallback() {
            @Override
            public Resolution getResolution() throws Exception {
                return actionBean.getSupportsSkippingQuote();
            }
        };

        MockHttpServletResponse response = MockStripesActionRunner.runStripesAction(resolutionCallback);
        Assert.assertEquals(response.getOutputString(),"{\"supportsSkippingQuote\":true}");

        product.setProductFamily(new ProductFamily("Something that doesn't support optional quotes"));
        response = MockStripesActionRunner.runStripesAction(resolutionCallback);
        Assert.assertEquals(response.getOutputString(), "{\"supportsSkippingQuote\":false}");
    }




    @Test(groups = TestGroups.DATABASE_FREE)
    public void testQuoteSkippingValidation() {
        ProductOrder pdo = new ProductOrder();
        actionBean.setEditOrder(pdo);

        actionBean.setSkipQuote(true);
        pdo.setSkipQuoteReason("");
        actionBean.validateQuoteOptions("");
        Assert.assertEquals(actionBean.getValidationErrors().size(), 1);

        actionBean.clearValidationErrors();
        actionBean.setSkipQuote(false);
        pdo.setSkipQuoteReason("");
        actionBean.validateQuoteOptions("");
        Assert.assertEquals(actionBean.getValidationErrors().size(),0);

        actionBean.setSkipQuote(true);
        pdo.setSkipQuoteReason("The dog ate my quote");
        actionBean.validateQuoteOptions("");

        Assert.assertEquals(actionBean.getValidationErrors().size(),0);

        pdo.setQuoteId("SomeQuote");
        actionBean.validateQuoteOptions("");

        Assert.assertEquals(actionBean.getValidationErrors().size(),1);

        // todo arz fix wire up persistence, check save, write UI test.

    }
}
