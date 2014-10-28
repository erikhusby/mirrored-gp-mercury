package org.broadinstitute.gpinformatics.athena.boundary.orders;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.MaterialInfo;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.AUTO_BUILD;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.STANDARD;

@Test(groups = TestGroups.STANDARD)
public class ProductOrderResourceTest extends RestServiceContainerTest {

    private static final String PDO_SAMPLE_STATUS = "pdoSampleStatus";

    private static final String MULTIPLE_RISK_SAMPLE = "SM-3RAE1";

    private static final String VALID_PDO_ID = "PDO-10";
    private static final String WIDELY_USED_QUOTE_ID = "MMMAC1";
    private static final String RP_CONTAINING_COHORTS = "RP-31";
    private static final String EXOME_EXPRESS_V3_PRODUCT_NAME = "Exome Express v3";

    @Deployment
    public static WebArchive buildMercuryWar() {
        // need TEST here for now because there's no STUBBY version of ThriftConfig
        // see ThriftServiceProducer.produce()
        return DeploymentBuilder.buildMercuryWar(AUTO_BUILD);
    }

    @Override
    protected String getResourcePath() {
        return "productOrders";
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testFetchLibraryDetailsByTubeBarcode(@ArquillianResource URL baseUrl) throws Exception {
        Date testDate = new Date();

        ProductOrderData data = new ProductOrderData();
        data.setProductName(EXOME_EXPRESS_V3_PRODUCT_NAME);
        data.setTitle("test product name" + testDate.getTime());
        data.setQuoteId(WIDELY_USED_QUOTE_ID);
        data.setUsername("scottmat");
        data.setResearchProjectId("RP-32");
        List<String> sampleIds = new ArrayList<>();
        Collections.addAll(sampleIds, "SM-41Q94", "SM-41Q95");
        data.setSamples(sampleIds);

        WebResource resource = makeWebResource(baseUrl, "create");

        resource.post(data);
    }


    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testCreateProductOrderNoUser(@ArquillianResource URL baseUrl) throws Exception{
        Date testDate = new Date();

        ProductOrderData data = new ProductOrderData();
        data.setProductName(EXOME_EXPRESS_V3_PRODUCT_NAME);
        data.setTitle("test product name" + testDate.getTime());
        data.setQuoteId(WIDELY_USED_QUOTE_ID);
        data.setResearchProjectId("RP-32");
        List<String> sampleIds = new ArrayList<>();
        Collections.addAll(sampleIds, "SM-41Q94", "SM-41Q95");
        data.setSamples(sampleIds);

        WebResource resource = makeWebResource(baseUrl, "create");

        try {
            resource.post(data);
            Assert.fail();
        } catch (UniformInterfaceException e) {
//            assertThat(e.getResponse().getStatus(), is(equalTo(Response.Status.UNAUTHORIZED.getStatusCode())));
        }
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testCreateProductOrderWithKit(@ArquillianResource URL baseUrl) throws Exception {
        Date testDate = new Date();

        ProductOrderData data = new ProductOrderData();
        data.setProductName(EXOME_EXPRESS_V3_PRODUCT_NAME);
        data.setTitle("test product name" + testDate.getTime());
        data.setQuoteId(WIDELY_USED_QUOTE_ID);
        data.setUsername("scottmat");
        data.setResearchProjectId(RP_CONTAINING_COHORTS); //RP that has cohorts associated with it.

        ProductOrderKitDetailData kitDetailData = new ProductOrderKitDetailData();

        kitDetailData.setMaterialInfo(MaterialInfo.DNA_DERIVED_FROM_BLOOD);
        kitDetailData.setMoleculeType(SampleKitWorkRequest.MoleculeType.DNA);
        kitDetailData.setNumberOfSamples(3);

        data.setKitDetailData(Collections.singletonList(kitDetailData));

        WebResource resource = makeWebResource(baseUrl, "createWithKitRequest");

        resource.post(data);
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testCreateProductOrderWithKitNoUser(@ArquillianResource URL baseUrl) throws Exception {
        Date testDate = new Date();

        ProductOrderData data = new ProductOrderData();
        data.setProductName(EXOME_EXPRESS_V3_PRODUCT_NAME);
        data.setTitle("test product name" + testDate.getTime());
        data.setQuoteId(WIDELY_USED_QUOTE_ID);
        data.setResearchProjectId(RP_CONTAINING_COHORTS); //RP that has cohorts associated with it.

        ProductOrderKitDetailData kitDetailData = new ProductOrderKitDetailData();

        kitDetailData.setMaterialInfo(MaterialInfo.DNA_DERIVED_FROM_BLOOD);
        kitDetailData.setMoleculeType(SampleKitWorkRequest.MoleculeType.DNA);
        kitDetailData.setNumberOfSamples(3);

        data.setKitDetailData(Collections.singletonList(kitDetailData));

        WebResource resource = makeWebResource(baseUrl, "createWithKitRequest");

        try {
            resource.post(data);
            Assert.fail();
        } catch (UniformInterfaceException e) {

        }
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testCreateProductOrderWithKitNoGoodUser(@ArquillianResource URL baseUrl) throws Exception {
        Date testDate = new Date();
        String purposely_Misspelled_User = "scottmatthewes";

        ProductOrderData data = new ProductOrderData();
        data.setProductName(EXOME_EXPRESS_V3_PRODUCT_NAME);
        data.setTitle("test product name" + testDate.getTime());
        data.setQuoteId(WIDELY_USED_QUOTE_ID);
        data.setUsername(purposely_Misspelled_User);
        data.setResearchProjectId(RP_CONTAINING_COHORTS); //RP that has cohorts associated with it.

        ProductOrderKitDetailData kitDetailData = new ProductOrderKitDetailData();

        kitDetailData.setMaterialInfo(MaterialInfo.DNA_DERIVED_FROM_BLOOD);
        kitDetailData.setMoleculeType(SampleKitWorkRequest.MoleculeType.DNA);
        kitDetailData.setNumberOfSamples(3);

        data.setKitDetailData(Collections.singletonList(kitDetailData));

        WebResource resource = makeWebResource(baseUrl, "createWithKitRequest");

        try {
            resource.post(data);
            Assert.fail();
        } catch (UniformInterfaceException e) {

        }
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true)
    @RunAsClient
    public void testFetchAtRiskPDOSamplesAllAtRisk(@ArquillianResource URL baseUrl) throws Exception {
        PDOSamples pdoSamples = getAtRiskSamples();

        PDOSamples returnedPdoSamples = makeWebResource(baseUrl, PDO_SAMPLE_STATUS)
                .type(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .entity(pdoSamples)
                .post(PDOSamples.class);

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
                .type(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .entity(pdoSamples)
                .post(PDOSamples.class);

        Assert.assertTrue(returnedPdoSamples.getAtRiskPdoSamples().isEmpty());
        Assert.assertTrue(returnedPdoSamples.getErrors().isEmpty());
    }

    @Test(groups = STANDARD, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testFindByIds(@ArquillianResource URL baseUrl)
            throws Exception {
        ProductOrders orders = makeWebResource(baseUrl, "pdo/" + VALID_PDO_ID)
                .accept(MediaType.APPLICATION_XML)
                .get(ProductOrders.class);
        // The web API returns the PDO ID for both the "id" and "productOrderKey" results.
        Assert.assertEquals(orders.getOrders().get(0).getId(), VALID_PDO_ID);
        Assert.assertEquals(orders.getOrders().get(0).getProductOrderKey(), VALID_PDO_ID);
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
            pdoSamples.addPdoSample(pdoKey, sampleId, false, false);
        }
        return pdoSamples;
    }
}
