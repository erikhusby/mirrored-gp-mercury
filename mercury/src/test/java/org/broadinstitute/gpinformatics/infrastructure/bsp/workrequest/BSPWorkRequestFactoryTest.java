package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for factory class for creating {@link SampleKitWorkRequest}s for use by {@link BSPKitRequestService}.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class BSPWorkRequestFactoryTest {

    @Test
    public void testBuildBspKitWorkRequest() throws Exception {
        SampleKitWorkRequest workRequest = BSPWorkRequestFactory.buildBspKitWorkRequest(
                "BSPKitRequestServiceTest.testBuildBspKitWorkRequest", "breilly", "PDO-1", 1L, 2L, 3L, 4, 5,
                new MaterialType("Cells:Pellet frozen, polar extracts"), new MaterialType("Whole Blood:Whole Blood"));

        assertThat(workRequest.getPrimaryInvestigatorId(), equalTo(1L));
        assertThat(workRequest.getProjectManagerId(), equalTo(2L));
        assertThat(workRequest.getExternalCollaboratorId(), equalTo(3L));
        assertThat(workRequest.getBarCode(), nullValue());
        assertThat(workRequest.getWorkRequestName(), equalTo("BSPKitRequestServiceTest.testBuildBspKitWorkRequest"));
        assertThat(workRequest.getRequestUser(), equalTo("breilly"));
        assertThat(workRequest.getPdoId(), equalTo("PDO-1"));
        assertThat(workRequest.getStatus(), nullValue());
        assertThat(workRequest.getNotificationList(), nullValue());
        assertThat(workRequest.getErrors(), nullValue());
        assertThat(workRequest.getWarnings(), nullValue());
        assertThat(workRequest.getInfo(), nullValue());
        assertThat(workRequest.getMoleculeType(), equalTo(SampleKitWorkRequest.MoleculeType.DNA));
        assertThat(workRequest.getSiteId(), equalTo(4L));
        assertThat(workRequest.getNumberOfSamples(), equalTo(5L));
        assertThat(workRequest.getTransferMethod(), equalTo(SampleKitWorkRequest.TransferMethod.SHIP_OUT));
    }
}
