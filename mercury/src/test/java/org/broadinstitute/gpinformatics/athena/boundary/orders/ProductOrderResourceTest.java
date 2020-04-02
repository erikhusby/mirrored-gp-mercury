package org.broadinstitute.gpinformatics.athena.boundary.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.MaterialInfo;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.JaxRsUtils;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.AUTO_BUILD;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STANDARD;

@Test(groups = TestGroups.STANDARD)
public class ProductOrderResourceTest extends RestServiceContainerTest {

    private static final String PDO_SAMPLE_STATUS = "pdoSampleStatus";

    private static final String MULTIPLE_RISK_SAMPLE = "SM-3RAE1";

    private static final String VALID_PDO_ID = "PDO-10";
    private static final String WIDELY_USED_QUOTE_ID = "MMMAC1";
    // This RP has a Cohort and has two PMs. Both features are needed for testing.
    private static final String RP_CONTAINING_COHORTS = "RP-40";
    private static final String RP_WITHOUT_COHORTS = "RP-32";
    private static final String EXOME_EXPRESS_V3_PRODUCT_NAME = "Exome Express v3";
    private static final String EXOME_EXPRESS_V3_PART_NUMBER = "P-WG-0092";
    
    private static final String TEST_PDO_NAME = "test";

    @Deployment
    public static WebArchive buildMercuryWar() {
        // need TEST here for now because there's no STUBBY version of ThriftConfig
        return DeploymentBuilder.buildMercuryWar(AUTO_BUILD);
    }

    @Override
    protected String getResourcePath() {
        return "productOrders";
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testCreateProductOrder(@ArquillianResource URL baseUrl) throws Exception {
        Date testDate = new Date();

        ProductOrderData data = new ProductOrderData();
        data.setProductPartNumber(EXOME_EXPRESS_V3_PART_NUMBER);
        data.setTitle(TEST_PDO_NAME + " rest/create " + testDate.getTime());
        data.setQuoteId(WIDELY_USED_QUOTE_ID);
        data.setUsername("scottmat");
        data.setResearchProjectId(RP_WITHOUT_COHORTS);
        List<String> sampleIds = new ArrayList<>();
        Collections.addAll(sampleIds, "SM-41Q94", "SM-41Q95");
        data.setSamples(sampleIds);

        WebTarget resource = makeWebResource(baseUrl, "create");

        ProductOrderData productOrderData = JaxRsUtils.postAndCheck(resource.request(), Entity.xml(data),
                new GenericType<ProductOrderData>() {});
        Assert.assertEquals(productOrderData.getStatus(), ProductOrder.OrderStatus.Pending.name());
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testCreateProductOrderProductNameProvided(@ArquillianResource URL baseUrl) throws Exception {
        Date testDate = new Date();

        ProductOrderData data = new ProductOrderData();
        data.setProductName(EXOME_EXPRESS_V3_PRODUCT_NAME);
        data.setTitle(TEST_PDO_NAME + " rest/create " + testDate.getTime());
        data.setQuoteId(WIDELY_USED_QUOTE_ID);
        data.setUsername("scottmat");
        data.setResearchProjectId(RP_WITHOUT_COHORTS);
        List<String> sampleIds = new ArrayList<>();
        Collections.addAll(sampleIds, "SM-41Q94", "SM-41Q95");
        data.setSamples(sampleIds);

        WebTarget resource = makeWebResource(baseUrl, "create");

        ProductOrderData productOrderData = JaxRsUtils.postAndCheck(resource.request(), Entity.xml(data),
                new GenericType<ProductOrderData>() {});
        Assert.assertEquals(productOrderData.getStatus(), ProductOrder.OrderStatus.Pending.name());
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testCreateProductOrderNoUser(@ArquillianResource URL baseUrl) throws Exception{
        Date testDate = new Date();

        ProductOrderData data = new ProductOrderData();
        data.setProductPartNumber(EXOME_EXPRESS_V3_PART_NUMBER);
        data.setTitle(TEST_PDO_NAME + " rest/create " + testDate.getTime());
        data.setQuoteId(WIDELY_USED_QUOTE_ID);
        data.setResearchProjectId(RP_WITHOUT_COHORTS);
        List<String> sampleIds = new ArrayList<>();
        Collections.addAll(sampleIds, "SM-41Q94", "SM-41Q95");
        data.setSamples(sampleIds);

        WebTarget resource = makeWebResource(baseUrl, "create");

        try {
            resource.request().post(Entity.xml(data), ProductOrderData.class);
            Assert.fail();
        } catch (WebApplicationException e) {
            Assert.assertEquals(e.getResponse().getStatusInfo().getStatusCode(),
                    Response.Status.UNAUTHORIZED.getStatusCode());
        }
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testCreateProductOrderNoUserProductNameProvided(@ArquillianResource URL baseUrl) throws Exception{
        Date testDate = new Date();

        ProductOrderData data = new ProductOrderData();
        data.setProductName(EXOME_EXPRESS_V3_PRODUCT_NAME);
        data.setTitle(TEST_PDO_NAME + " rest/create " + testDate.getTime());
        data.setQuoteId(WIDELY_USED_QUOTE_ID);
        data.setResearchProjectId(RP_WITHOUT_COHORTS);
        List<String> sampleIds = new ArrayList<>();
        Collections.addAll(sampleIds, "SM-41Q94", "SM-41Q95");
        data.setSamples(sampleIds);

        WebTarget resource = makeWebResource(baseUrl, "create");

        try {
            resource.request().post(Entity.xml(data), ProductOrderData.class);
            Assert.fail();
        } catch (WebApplicationException e) {
            Assert.assertEquals(e.getResponse().getStatusInfo().getStatusCode(),
                    Response.Status.UNAUTHORIZED.getStatusCode());
        }
    }

    private static ProductOrderData createTestProductOrderData(String username, String partNumber, String productName) {
        ProductOrderData data = new ProductOrderData();
        data.setProductPartNumber(partNumber);
        data.setProductName(productName);
        data.setTitle(TEST_PDO_NAME + " rest/createWithKitRequest " + new Date().getTime());
        data.setQuoteId(WIDELY_USED_QUOTE_ID);
        // Need to use a research project that has a cohort associated with it.
        data.setResearchProjectId(RP_CONTAINING_COHORTS);
        data.setUsername(username);
        // This is a valid id in the BSP.BSP_SITE table.
        data.setSiteId(1);
        ProductOrderKitDetailData kitDetailData = createTestProductOrderKit();
        data.setKitDetailData(Collections.singletonList(kitDetailData));
        return data;
    }

    private static ProductOrderKitDetailData createTestProductOrderKit() {
        ProductOrderKitDetailData kitDetailData = new ProductOrderKitDetailData();
        kitDetailData.setMaterialInfo(MaterialInfo.DNA_DERIVED_FROM_BLOOD);
        kitDetailData.setMoleculeType(SampleKitWorkRequest.MoleculeType.DNA);
        kitDetailData.setNumberOfSamples(3);
        return kitDetailData;
    }

    private ProductOrderData sendCreateWithKitRequest(URL baseUrl, String username, String partNumber,
                                                      String productName) throws Exception {
        WebTarget resource = makeWebResource(baseUrl, "createWithKitRequest");
        return JaxRsUtils.postAndCheck(resource.request(), Entity.xml(createTestProductOrderData(username,
            partNumber, productName)),
                new GenericType<ProductOrderData>() {});
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testCreateProductOrderWithKit(@ArquillianResource URL baseUrl) throws Exception {
        ProductOrderData data = sendCreateWithKitRequest(baseUrl, "scottmat", EXOME_EXPRESS_V3_PART_NUMBER, null);
        Assert.assertEquals(data.getStatus(), ProductOrder.OrderStatus.Pending.name());

        // Read data from JIRA.
        JiraConfig jiraConfig = new JiraConfig(DEV);
        JiraService jiraService = new JiraServiceImpl(jiraConfig);
        JiraIssue jiraIssue = jiraService.getIssue(data.getProductOrderKey());
        @SuppressWarnings("unchecked")
        Collection<String> projectManagers =
                (Collection<String>) jiraIssue.getField(ProductOrder.JiraField.PMS.getName());
        // There should be two PMs in the PMs field.
        Assert.assertEquals(projectManagers.size(), 2);
    }

    // Test is disabled since it fails for the same reason testCreateProductOrderWithKit fails
    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = false)
    @RunAsClient
    public void testCreateProductOrderWithKitProductNameProvided(@ArquillianResource URL baseUrl) throws Exception {
        ProductOrderData data = sendCreateWithKitRequest(baseUrl, "scottmat",null, EXOME_EXPRESS_V3_PRODUCT_NAME);
        Assert.assertEquals(data.getStatus(), ProductOrder.OrderStatus.Pending.name());

        // Read data from JIRA.
        JiraConfig jiraConfig = new JiraConfig(DEV);
        JiraService jiraService = new JiraServiceImpl(jiraConfig);
        JiraIssue jiraIssue = jiraService.getIssue(data.getProductOrderKey());
        @SuppressWarnings("unchecked")
        Collection<String> projectManagers =
                (Collection<String>) jiraIssue.getField(ProductOrder.JiraField.PMS.getName());
        // There should be two PMs in the PMs field.
        Assert.assertEquals(projectManagers.size(), 2);
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, expectedExceptions = WebApplicationException.class)
    @RunAsClient
    public void testCreateProductOrderWithKitNoUser(@ArquillianResource URL baseUrl) throws Exception {
        sendCreateWithKitRequest(baseUrl, null, EXOME_EXPRESS_V3_PART_NUMBER, null);
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, expectedExceptions = WebApplicationException.class)
    @RunAsClient
    public void testCreateProductOrderWithKitNoGoodUser(@ArquillianResource URL baseUrl) throws Exception {
        sendCreateWithKitRequest(baseUrl, "invalid user name", EXOME_EXPRESS_V3_PART_NUMBER, null);
    }


    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, expectedExceptions = WebApplicationException.class)
    @RunAsClient
    public void testCreateProductOrderWithKitNoUserProductNameProvided(@ArquillianResource URL baseUrl) throws Exception {
        sendCreateWithKitRequest(baseUrl, null, null, EXOME_EXPRESS_V3_PRODUCT_NAME);
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, expectedExceptions = WebApplicationException.class)
    @RunAsClient
    public void testCreateProductOrderWithKitNoGoodUserProductNameProvided(@ArquillianResource URL baseUrl) throws Exception {
        sendCreateWithKitRequest(baseUrl, "invalid user name", null, EXOME_EXPRESS_V3_PRODUCT_NAME);
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testFetchAtRiskPDOSamplesAllAtRisk(@ArquillianResource URL baseUrl) throws Exception {
        PDOSamples pdoSamples = getAtRiskSamples();

        PDOSamples returnedPdoSamples = makeWebResource(baseUrl, PDO_SAMPLE_STATUS)
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.json(pdoSamples), PDOSamples.class);

        Assert.assertEquals(pdoSamples.getPdoSamples().size(), returnedPdoSamples.getPdoSamples().size());
        Assert.assertEquals(returnedPdoSamples.getErrors().size(), pdoSamples.getAtRiskPdoSamples().size());

        RiskCriterion totalDnaCriterion =
                new RiskCriterion(RiskCriterion.RiskCriteriaType.TOTAL_DNA, Operator.LESS_THAN, ".250");
        RiskCriterion ffpeCriterion = new RiskCriterion(RiskCriterion.RiskCriteriaType.FFPE, Operator.IS, null);

        boolean foundSampleWithMultipleRiskFactors = false;
        for (PDOSample pdoSample : returnedPdoSamples.getAtRiskPdoSamples()) {
            if (pdoSample.getSampleName().equals(MULTIPLE_RISK_SAMPLE)) {
                String samplePdoText = pdoSample.getPdoKey() + "/" + pdoSample.getSampleName();
                foundSampleWithMultipleRiskFactors = true;
                Collection<String> riskCategories = pdoSample.getRiskCategories();
                Assert.assertEquals(riskCategories.size(), 2,
                        "Risk categories are not being listed properly.  Check the list of risks associated with "
                        + samplePdoText);
                Assert.assertTrue(riskCategories.contains(ffpeCriterion.getCalculationString()),
                        "Check the risks for " + samplePdoText);
                Assert.assertTrue(riskCategories.contains(totalDnaCriterion.getCalculationString()),
                        "Check the risks for " + samplePdoText);
            }
        }
        Assert.assertTrue(foundSampleWithMultipleRiskFactors,
                "No assertions were done to verify that samples with multiple risk factors are handled properly via " +
                "web service call used by squid to update the risk categorized samples field in LCSET tickets.");
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testFetchAtRiskPDOSamplesNoneAtRisk(@ArquillianResource URL baseUrl) throws Exception {
        PDOSamples pdoSamples = getNonRiskPDOSamples();
        PDOSamples returnedPdoSamples = makeWebResource(baseUrl, PDO_SAMPLE_STATUS)
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(Entity.json(pdoSamples), PDOSamples.class);

        Assert.assertTrue(returnedPdoSamples.getAtRiskPdoSamples().isEmpty());
        Assert.assertTrue(returnedPdoSamples.getErrors().isEmpty());
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindByIds(@ArquillianResource URL baseUrl)
            throws Exception {
        ProductOrders orders = makeWebResource(baseUrl, "pdo/" + VALID_PDO_ID)
                .request(MediaType.APPLICATION_XML)
                .get(ProductOrders.class);
        // The web API returns the PDO ID for both the "id" and "productOrderKey" results.
        Assert.assertEquals(orders.getOrders().get(0).getId(), VALID_PDO_ID);
        Assert.assertEquals(orders.getOrders().get(0).getProductOrderKey(), VALID_PDO_ID);
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testGenotypingInfo(@ArquillianResource URL baseUrl) throws Exception {
        Map<String, String> map = new HashMap<String, String>() {{
            put("PDO-7727", null);
            put("PDO-8470", "Broad_GWAS_supplemental_15061359_A1");
            put("PDO-8350", "Multi-EthnicGlobal-8_A1");
        }};
        ProductOrders orders = makeWebResource(baseUrl, "pdo/" + StringUtils.join(map.keySet(), ","))
                .request(MediaType.APPLICATION_XML)
                .get(ProductOrders.class);
        Assert.assertEquals(orders.getOrders().size(), map.size());
        for (ProductOrderData order : orders.getOrders()) {
            Assert.assertEquals(order.getGenoChipType(), map.get(order.getProductOrderKey()));
        }
    }



    private static PDOSamples getAtRiskSamples() {
        return makeTestPdoSamplePairs("PDO-2350", MULTIPLE_RISK_SAMPLE, "SM-3S1Q8", "SM-4AXN1", "SM-4AXN5", "SM-4AXNJ",
                "SM-4JPLH");
    }

    private static PDOSamples getNonRiskPDOSamples() {
        return makeTestPdoSamplePairs("PDO-2328", "SM-41Q94", "SM-41Q95", "SM-41Q9F", "SM-41Q9G", "SM-41Q9S",
                "SM-41RAL");
    }

    private static PDOSamples makeTestPdoSamplePairs(String pdoKey, String... sampleIds) {
        PDOSamples pdoSamples = new PDOSamples();

        for (String sampleId : sampleIds) {
            pdoSamples.addPdoSample(pdoKey, sampleId, false, false, new Date());
        }
        return pdoSamples;
    }
}
