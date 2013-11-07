package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.collection.Group;
import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.sample.MaterialInfo;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.bsp.client.workrequest.WorkRequestResponse;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * Integration tests for {@link BSPKitRequestService}.
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BSPKitRequestServiceIntegrationTest extends Arquillian {

    private static final Log log = LogFactory.getLog(BSPKitRequestServiceIntegrationTest.class);

    public static final long BREILLY_DOMAIN_USER_ID = 10619;
    public static final long ELANDER_DOMAIN_USER_ID = 7062;
    public static final Site TEST_SITE = new Site(1, "site", "", "", "", false, false);
    public static final SampleCollection TEST_COLLECTION =
            new SampleCollection(1L, "", new Group(1L, "", "", false), "", "", false);
    public static final long NUMBER_OF_SAMPLES = 96;

    @Inject
    private BSPKitRequestService bspKitRequestService;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV);
    }

    @Test
    public void testSendKitRequest() {
        MaterialInfo materialInfo =
                new MaterialInfo("DNA Matrix Kit", "DNA Derived from Bucal Cell Tissue and/or Saliva",
                        new MaterialType("Cells:Pellet frozen, polar extracts"));
        SampleKitWorkRequest workRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(
                "BSPKitRequestServiceIntegrationTest.testSendKitRequest " + System.currentTimeMillis(), "breilly",
                "PDO-1", ELANDER_DOMAIN_USER_ID, BREILLY_DOMAIN_USER_ID,
                ELANDER_DOMAIN_USER_ID, TEST_SITE, NUMBER_OF_SAMPLES,
                materialInfo, TEST_COLLECTION, "hrafal@broadinstitute.org");
        workRequest.setExternalCollaboratorId(BREILLY_DOMAIN_USER_ID);

        WorkRequestResponse result = bspKitRequestService.sendKitRequest(workRequest);
        log.info(result.getWorkRequestBarcode());
    }

    @Test
    public void testSubmitKitRequest() {
        MaterialInfo materialInfo =
                new MaterialInfo("DNA Matrix Kit", "DNA Derived from Bucal Cell Tissue and/or Saliva",
                        new MaterialType("Cells:Pellet frozen, polar extracts"));
        SampleKitWorkRequest workRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(
                "BSPKitRequestServiceIntegrationTest.testSendKitRequest " + System.currentTimeMillis(), "breilly",
                "PDO-1", ELANDER_DOMAIN_USER_ID, BREILLY_DOMAIN_USER_ID, ELANDER_DOMAIN_USER_ID, TEST_SITE,
                NUMBER_OF_SAMPLES, materialInfo, TEST_COLLECTION, "hrafal@broadinstitute.org");
        workRequest.setExternalCollaboratorId(BREILLY_DOMAIN_USER_ID);

        WorkRequestResponse result = bspKitRequestService.sendKitRequest(workRequest);

        WorkRequestResponse submitResult = bspKitRequestService.submitKitRequest(result.getWorkRequestBarcode());
        log.info(submitResult.getWorkRequestBarcode());
    }
}
