package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.collection.Group;
import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Integration tests for {@link BSPKitRequestService}.
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BSPKitRequestServiceIntegrationTest extends Arquillian {

    private static final Log log = LogFactory.getLog(BSPKitRequestServiceIntegrationTest.class);

    public static final long BREILLY_DOMAIN_USER_ID = 10619;
    public static final long ELANDER_DOMAIN_USER_ID = 7062;
    public static final Site TEST_SITE = new Site(1, "site", "", "", "", false, false);
    public static final Pair<Long, String> HUMAN_ORGANISM = new ImmutablePair(1L, "Animalia : Homo : Homo sapiens");
    public static final SampleCollection TEST_COLLECTION =
            new SampleCollection(1L, "", new Group(1L, "", "", false), "", "", false,
                    Collections.singletonList(HUMAN_ORGANISM));
    public static final long NUMBER_OF_SAMPLES = 96;
    public static final Set<PostReceiveOption> SELECTED_POST_RECEIVE_OPTIONS = new HashSet<PostReceiveOption>() {{
        add(PostReceiveOption.FLUIDIGM_FINGERPRINTING);
        add(PostReceiveOption.DNA_EXTRACTION);
    }};

    private static final String COMMENTS = "This is not a kit";
    private static final boolean IS_EX_EX = true;
    private static final SampleKitWorkRequest.TransferMethod TRANSFER_METHOD =
            SampleKitWorkRequest.TransferMethod.SHIP_OUT;

    @Inject
    private BSPKitRequestService bspKitRequestService;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV);
    }

    @Test
    public void testSendKitRequest() {
        MaterialInfoDto MaterialInfoDto =
                new MaterialInfoDto("DNA Matrix Kit", "DNA Derived from Bucal Cell Tissue and/or Saliva");
        SampleKitWorkRequest workRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(
                "BSPKitRequestServiceIntegrationTest.testSendKitRequest " + System.currentTimeMillis(), "breilly",
                "PDO-1", ELANDER_DOMAIN_USER_ID, BREILLY_DOMAIN_USER_ID,
                ELANDER_DOMAIN_USER_ID, TEST_SITE.getId(), NUMBER_OF_SAMPLES,
                MaterialInfoDto, TEST_COLLECTION.getCollectionId(), "hrafal@broadinstitute.org",
                HUMAN_ORGANISM.getLeft(),
                SELECTED_POST_RECEIVE_OPTIONS, COMMENTS, IS_EX_EX, TRANSFER_METHOD);
        workRequest.setExternalCollaboratorId(BREILLY_DOMAIN_USER_ID);

        WorkRequestResponse result = bspKitRequestService.sendKitRequest(workRequest);
        log.info(result.getWorkRequestBarcode());
    }

    @Test
    public void testSubmitKitRequest() {
        MaterialInfoDto MaterialInfoDto =
                new MaterialInfoDto("DNA Matrix Kit", "DNA Derived from Bucal Cell Tissue and/or Saliva");
        SampleKitWorkRequest workRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(
                "BSPKitRequestServiceIntegrationTest.testSendKitRequest " + System.currentTimeMillis(), "breilly",
                "PDO-1", ELANDER_DOMAIN_USER_ID, BREILLY_DOMAIN_USER_ID, ELANDER_DOMAIN_USER_ID, TEST_SITE.getId(),
                NUMBER_OF_SAMPLES, MaterialInfoDto, TEST_COLLECTION.getCollectionId(), "hrafal@broadinstitute.org",
                HUMAN_ORGANISM.getLeft(), SELECTED_POST_RECEIVE_OPTIONS, COMMENTS, IS_EX_EX, TRANSFER_METHOD);
        workRequest.setExternalCollaboratorId(BREILLY_DOMAIN_USER_ID);

        WorkRequestResponse result = bspKitRequestService.sendKitRequest(workRequest);

        WorkRequestResponse submitResult = bspKitRequestService.submitKitRequest(result.getWorkRequestBarcode());
        log.info(submitResult.getWorkRequestBarcode());
    }
}
