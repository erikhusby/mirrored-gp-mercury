package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.bsp.client.workrequest.WorkRequestResponse;
import org.broadinstitute.gpinformatics.athena.boundary.kits.SampleKitRequestDto;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BSPKitRequestServiceIntegrationTest extends Arquillian {

    public static final long BREILLY_BSP_USER_ID = 14038L;
    public static final long BREILLY_DOMAIN_USER_ID = 10619L;
    public static final long ELANDER_DOMAIN_USER_ID = 7062L;

    @Inject
    private BSPKitRequestService bspKitRequestService;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV);
    }

    @Test
    public void testSendKitRequest() {
        SampleKitWorkRequest workRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(
                "BSPKitRequestServiceIntegrationTest.testSendKitRequest " + System.currentTimeMillis(), "breilly",
                "PDO-1", ELANDER_DOMAIN_USER_ID, BREILLY_DOMAIN_USER_ID, 1L, 96L);
        workRequest.setExternalCollaboratorId(BREILLY_DOMAIN_USER_ID);

        WorkRequestResponse result = bspKitRequestService.sendKitRequest(workRequest);
        System.out.println(result.getWorkRequestBarcode());
    }

    @Test(enabled = false)
    public void testSubmitKitRequest() {
        SampleKitWorkRequest workRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(
                "BSPKitRequestServiceIntegrationTest.testSendKitRequest " + System.currentTimeMillis(), "breilly",
                "PDO-1", ELANDER_DOMAIN_USER_ID, BREILLY_DOMAIN_USER_ID, 1L, 96L);
        workRequest.setExternalCollaboratorId(BREILLY_DOMAIN_USER_ID);

        WorkRequestResponse result = bspKitRequestService.sendKitRequest(workRequest);

        WorkRequestResponse submitResult = bspKitRequestService.submitKitRequest(result.getWorkRequestBarcode());
        System.out.println(submitResult.getWorkRequestBarcode());
    }
}
