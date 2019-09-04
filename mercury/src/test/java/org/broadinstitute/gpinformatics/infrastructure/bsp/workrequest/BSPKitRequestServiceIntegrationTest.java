package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.MaterialInfo;
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
import org.broadinstitute.bsp.client.workrequest.kit.KitTypeAllowanceSpecification;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderTest;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryImpl;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Integration tests for {@link BSPKitRequestService}.
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BSPKitRequestServiceIntegrationTest {

    private static final Log log = LogFactory.getLog(BSPKitRequestServiceIntegrationTest.class);

    public static final long BREILLY_DOMAIN_USER_ID = 10619;
    public static final long ELANDER_DOMAIN_USER_ID = 7062;
    public static final Site TEST_SITE = new Site(1, "site", "", "", "", false, false);
    public static final Pair<Long, String> HUMAN_ORGANISM = new ImmutablePair<>(1L, "Animalia : Homo : Homo sapiens");
    public static final SampleCollection TEST_COLLECTION =
            new SampleCollection(1L, "", new Group(1L, "", "", false), "", "", false,
                    Collections.singletonList(HUMAN_ORGANISM));
    public static final long NUMBER_OF_SAMPLES = 96;
    public static final Set<PostReceiveOption> SELECTED_POST_RECEIVE_OPTIONS =
            EnumSet.of(PostReceiveOption.FLUIDIGM_FINGERPRINTING, PostReceiveOption.DNA_EXTRACTION);

    private static final String COMMENTS = "This is not a kit";
    private static final boolean IS_EX_EX = true;
    private static final SampleKitWorkRequest.TransferMethod TRANSFER_METHOD =
            SampleKitWorkRequest.TransferMethod.SHIP_OUT;

    private BSPKitRequestService bspKitRequestService=null;

    @BeforeMethod
    public void setUp() {
        BSPConfig bspConfig = BSPConfig.produce(Deployment.DEV);
        BSPWorkRequestClientService bspWorkRequestClientService = new BSPWorkRequestClientService(bspConfig);
        BSPManagerFactory bspManagerFactory = new BSPManagerFactoryImpl(bspConfig);
        BSPUserList bspUserList = new BSPUserList(bspManagerFactory);
        bspKitRequestService =
                new BSPKitRequestService(bspWorkRequestClientService, bspManagerFactory, bspUserList);
    }
    public void testSendKitRequest() {
        MaterialInfoDto MaterialInfoDto =
                new MaterialInfoDto(KitTypeAllowanceSpecification.DNA_MATRIX_KIT.getText(),
                        MaterialInfo.DNA_DERIVED_FROM_BUCAL_CELLS_OR_SALIVA.getText());
        SampleKitWorkRequest workRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(
                "BSPKitRequestServiceIntegrationTest.testSendKitRequest " + System.currentTimeMillis(), "ktracy",
                ProductOrderTest.PDO_JIRA_KEY, ELANDER_DOMAIN_USER_ID, BREILLY_DOMAIN_USER_ID,
                ELANDER_DOMAIN_USER_ID, TEST_SITE.getId(),
                TEST_COLLECTION.getCollectionId(), "hrafal@broadinstitute.org",
                COMMENTS, IS_EX_EX, TRANSFER_METHOD,
                Collections.singletonList(BSPWorkRequestFactory.buildBspKitWRDefinitionInfo(NUMBER_OF_SAMPLES,
                        MaterialInfoDto, HUMAN_ORGANISM.getLeft(), SELECTED_POST_RECEIVE_OPTIONS,
                        SampleKitWorkRequest.MoleculeType.DNA)));
        workRequest.setExternalCollaboratorId(BREILLY_DOMAIN_USER_ID);

        WorkRequestResponse result = bspKitRequestService.sendKitRequest(workRequest);
        log.info(result.getWorkRequestBarcode());
    }

    public void testSubmitKitRequest() {
        MaterialInfoDto MaterialInfoDto =
                new MaterialInfoDto(KitTypeAllowanceSpecification.DNA_MATRIX_KIT.getText(),
                        MaterialInfo.DNA_DERIVED_FROM_BUCAL_CELLS_OR_SALIVA.getText());
        SampleKitWorkRequest workRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(
                "BSPKitRequestServiceIntegrationTest.testSendKitRequest " + System.currentTimeMillis(), "ktracy",
                ProductOrderTest.PDO_JIRA_KEY, ELANDER_DOMAIN_USER_ID, BREILLY_DOMAIN_USER_ID, ELANDER_DOMAIN_USER_ID,
                TEST_SITE.getId(),
                TEST_COLLECTION.getCollectionId(), "hrafal@broadinstitute.org",
                COMMENTS, IS_EX_EX, TRANSFER_METHOD,
                Collections.singletonList(BSPWorkRequestFactory.buildBspKitWRDefinitionInfo(NUMBER_OF_SAMPLES,
                        MaterialInfoDto, HUMAN_ORGANISM.getLeft(), SELECTED_POST_RECEIVE_OPTIONS,
                        SampleKitWorkRequest.MoleculeType.DNA)));
        workRequest.setExternalCollaboratorId(BREILLY_DOMAIN_USER_ID);

        WorkRequestResponse result = bspKitRequestService.sendKitRequest(workRequest);

        WorkRequestResponse submitResult = bspKitRequestService.submitKitRequest(result.getWorkRequestBarcode());
        log.info(submitResult.getWorkRequestBarcode());
    }
}
