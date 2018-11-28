package org.broadinstitute.gpinformatics.mercury.boundary.run;

import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.net.URL;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test Infinium Run web service.
 */
@Test(groups = TestGroups.STANDARD)
public class InfiniumRunResourceTest extends RestServiceContainerTest {

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @RunAsClient
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER, enabled = true)
    public void testBasics(@ArquillianResource URL baseUrl) throws Exception {
        WebResource resource = makeWebResource(baseUrl, "query");

        InfiniumRunBean response1 = resource.queryParam("chipWellBarcode", "3999582166_R01C01").
                type(MediaType.APPLICATION_JSON_TYPE).
                accept(MediaType.APPLICATION_JSON_TYPE).
                get(InfiniumRunBean.class);
        Assert.assertEquals(response1.getSampleId(), "SM-ATJSM");
        Assert.assertEquals(response1.getCollaboratorSampleId(), "TREDAP123");
        Assert.assertEquals(response1.getCollaboratorParticipantId(), "TREDAP123");
        Assert.assertEquals(response1.getSampleLsid(), "broadinstitute.org:bsp.prod.sample:ATJSM");
        Assert.assertFalse(response1.isPositiveControl());
        Assert.assertFalse(response1.isNegativeControl());
        validateRunBean(response1);

        // Test a control
        InfiniumRunBean response2 = resource.queryParam("chipWellBarcode", "3999582166_R05C01").
                type(MediaType.APPLICATION_JSON_TYPE).
                accept(MediaType.APPLICATION_JSON_TYPE).
                get(InfiniumRunBean.class);
        Assert.assertEquals(response2.getCollaboratorSampleId(), "NA12878");
        Assert.assertTrue(response2.isPositiveControl());
        Assert.assertFalse(response2.isNegativeControl());
        validateRunBean(response2);
    }

    private void validateRunBean(InfiniumRunBean response1) {
        Assert.assertTrue(response1.getRedIDatPath().startsWith("/humgen/illumina_data"));
        Assert.assertTrue(response1.getGreenIDatPath().startsWith("/humgen/illumina_data"));
        Assert.assertEquals(response1.getChipManifestPath(),
                "/humgen/affy_info/GAPProduction/prod/Broad_GWAS_supplemental_15061359_A1/Broad_GWAS_supplemental_15061359_A1.csv");
        Assert.assertEquals(response1.getBeadPoolManifestPath(),
                "/gap/illumina/beadstudio/Autocall/ChipInfo/Broad_GWAS_supplemental/Broad_GWAS_supplemental_15061359_A1.bpm");
        Assert.assertEquals(response1.getCsvBeadPoolManifestPath(),
                "/humgen/illumina_data/Broad_GWAS_supplemental_15061359_A1.bpm.csv");
        Assert.assertEquals(response1.getClusterFilePath(),
                "/gap/illumina/beadstudio/Autocall/ChipInfo/Broad_GWAS_supplemental/Broad_GWAS_supplemental_15061359_A1.egt");
        Assert.assertEquals(response1.getzCallThresholdsPath(),
                "/gap/illumina/beadstudio/Autocall/ChipInfo/Broad_GWAS_supplemental/thresholds.7.txt");
        Assert.assertEquals(response1.getResearchProjectId(), "RP-313");
        Assert.assertEquals(response1.getProductPartNumber(), "P-EX-0017");
        Assert.assertEquals(response1.getProductFamily(), "Exome");
        Assert.assertEquals(response1.getProductName(), "G4L WES + Array v2");
        Assert.assertEquals(response1.getProductOrderId(), "PDO-7783");
    }

    @Override
    protected String getResourcePath() {
        return "infiniumrun";
    }
}