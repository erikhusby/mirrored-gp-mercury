package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.collection.Group;
import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequestDefinitionInfo;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;

/**
 * Tests for factory class for creating {@link SampleKitWorkRequest}s for use by {@link BSPKitRequestService}.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class BSPWorkRequestFactoryTest {

    public static final Site TEST_SITE = new Site(4, "site", "", "", "", false, false);
    public static final long PRIMARY_INVESTIGATOR_ID = 1L;
    public static final Pair<Long, String> HUMAN_ORGANISM = new ImmutablePair(1L, "Animalia : Homo : Homo sapiens");
    public static final SampleCollection TEST_COLLECTION =
            new SampleCollection(6L, "", new Group(PRIMARY_INVESTIGATOR_ID, "", "", false), "", "", false,
                    Collections.singletonList(HUMAN_ORGANISM));
    public static final long NUMBER_OF_SAMPLES = 5L;
    public static final long PROJECT_MANAGER_ID = 2L;
    public static final long EXTERNAL_COLLABORATOR_ID = 3L;
    public static final String WORK_REQUEST_NAME = "BSPKitRequestServiceTest.testBuildBspKitWorkRequest";
    public static final String REQUEST_USER = "breilly";
    public static final String PRODUCT_ORDER_ID = "PDO-1";
    public static final String NOTIFICATION_LIST = "hrafal@broadinstitute.org";
    public static final Set<PostReceiveOption> SELECTED_POST_RECEIVE_OPTIONS = new HashSet<PostReceiveOption>() {{
        add(PostReceiveOption.FLUIDIGM_FINGERPRINTING);
        add(PostReceiveOption.DNA_EXTRACTION);
    }};
    private static final String COMMENTS = "This is not a kit";
    private static final boolean IS_EX_EX = true;
    private static final SampleKitWorkRequest.TransferMethod TRANSFER_METHOD =
            SampleKitWorkRequest.TransferMethod.SHIP_OUT;

    @Test
    public void testBuildBspKitWorkRequest() throws Exception {
        MaterialInfoDto MaterialInfoDto =
                new MaterialInfoDto("DNA Matrix Kit", "DNA Derived from Bucal Cell Tissue and/or Saliva");
        SampleKitWorkRequest workRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(WORK_REQUEST_NAME, REQUEST_USER,
                PRODUCT_ORDER_ID, PRIMARY_INVESTIGATOR_ID,
                PROJECT_MANAGER_ID, EXTERNAL_COLLABORATOR_ID,
                TEST_SITE.getId(),
                TEST_COLLECTION.getCollectionId(), NOTIFICATION_LIST,
                COMMENTS, IS_EX_EX, TRANSFER_METHOD,
                BSPWorkRequestFactory.buildBspKitWRDefinitionInfo(NUMBER_OF_SAMPLES, MaterialInfoDto, HUMAN_ORGANISM.getLeft(),SELECTED_POST_RECEIVE_OPTIONS,SampleKitWorkRequest.MoleculeType.DNA)
        );

        assertThat(workRequest.getPrimaryInvestigatorId(), equalTo(PRIMARY_INVESTIGATOR_ID));
        assertThat(workRequest.getProjectManagerId(), equalTo(PROJECT_MANAGER_ID));
        assertThat(workRequest.getExternalCollaboratorId(), equalTo(EXTERNAL_COLLABORATOR_ID));
        assertThat(workRequest.getBarCode(), nullValue());
        assertThat(workRequest.getWorkRequestName(), equalTo(WORK_REQUEST_NAME));
        assertThat(workRequest.getRequestUser(), equalTo(REQUEST_USER));
        assertThat(workRequest.getPdoId(), equalTo(PRODUCT_ORDER_ID));
        assertThat(workRequest.getStatus(), nullValue());
        assertThat(workRequest.getNotificationList(), equalTo(NOTIFICATION_LIST));
        assertThat(workRequest.getErrors(), is(empty()));
        assertThat(workRequest.getWarnings(), is(empty()));
        assertThat(workRequest.getInfo(), is(empty()));
        assertThat(workRequest.getKitDefinitions().iterator().next().getMoleculeType(), equalTo(SampleKitWorkRequest.MoleculeType.DNA));
        assertThat(workRequest.getSiteId(), equalTo(TEST_SITE.getId()));
        assertThat(workRequest.getKitDefinitions().iterator().next().getNumberOfSamples(), equalTo(NUMBER_OF_SAMPLES));
        assertThat(workRequest.getTransferMethod(), equalTo(SampleKitWorkRequest.TransferMethod.SHIP_OUT));
        assertThat(workRequest.getSampleCollectionId(), equalTo(TEST_COLLECTION.getCollectionId()));
        assertThat(workRequest.getNotes(), equalTo(COMMENTS));
        assertThat(workRequest.isExExKit(), equalTo(IS_EX_EX));
        assertThat(workRequest.getTransferMethod(), equalTo(TRANSFER_METHOD));

    }
}
